package tileworld.agent;

import java.awt.Color;
import sim.display.GUIState;
import sim.portrayal.Inspector;
import sim.portrayal.LocationWrapper;
import sim.portrayal.Portrayal;
import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEntity;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;
import tileworld.exceptions.CellBlockedException;
import tileworld.planners.AstarPathGenerator;
import tileworld.planners.TWPath;
import tileworld.planners.TWPathStep;

/**
 * SmartTWAgent — Phase 1 intelligent agent for TileWorld.
 *
 * Decision priority:
 * 1. FUEL EMERGENCY — navigate to fuel station if fuel below safety margin
 * 2. REFUEL — if standing on fuel station and not full
 * 3. OPPORTUNISTIC DROP — standing on hole while carrying tile
 * 4. OPPORTUNISTIC PICKUP — standing on tile while carrying < 3
 * 5. EXPLORE — systematic lawnmower pattern to cover the grid
 * 6. WAIT — no valid action
 */
public class SmartTWAgent extends TWAgent {

    private final String name;
    private SmartTWAgentMemory smartMemory;
    private AstarPathGenerator pathGenerator;

    // Current path being followed
    private TWPath currentPath;
    private Int2D currentGoalPos;
    private String currentGoalType; // "fuel", "explore"

    // Lawnmower exploration state machine
    private enum SweepState { HORIZONTAL, SHIFTING }
    private SweepState sweepState = SweepState.HORIZONTAL;
    private boolean sweepGoingRight;
    private int shiftRemaining = 0;
    private boolean shiftGoingDown;
    private static final int LAWNMOWER_STEP = 7;

    public SmartTWAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(xpos, ypos, env, fuelLevel);
        this.name = name;

        // Replace default memory with our enhanced version
        this.smartMemory = new SmartTWAgentMemory(this, env.schedule,
                env.getxDimension(), env.getyDimension());
        this.memory = this.smartMemory;

        // A* path generator with max search distance across the grid
        int maxSearch = env.getxDimension() + env.getyDimension();
        this.pathGenerator = new AstarPathGenerator(env, this, maxSearch);

        // Sweep toward the CLOSER horizontal edge first (less fuel waste on first half-row)
        this.sweepGoingRight = (xpos >= env.getxDimension() / 2);
        // Shift toward the FURTHER vertical edge (covers more ground before reversing)
        this.shiftGoingDown = (ypos < env.getyDimension() / 2);
    }

    @Override
    protected TWThought think() {
        boolean fuelKnown = smartMemory.isFuelStationKnown();

        // 0. FUEL STATION UNKNOWN — focus entirely on finding it
        if (!fuelKnown) {
            TWDirection dir = exploreGreedy();
            if (dir != null) {
                return new TWThought(TWAction.MOVE, dir);
            }
            return new TWThought(TWAction.MOVE, TWDirection.Z);
        }

        // From here, fuel station is known.

        // 1. REFUEL — if on fuel station and not at max
        Int2D fuelPos = smartMemory.getKnownFuelStation();
        if (getX() == fuelPos.x && getY() == fuelPos.y
                && fuelLevel < Parameters.defaultFuelLevel) {
            return new TWThought(TWAction.REFUEL, TWDirection.Z);
        }

        // 2. FUEL EMERGENCY — navigate to fuel station (MUST return, never fall through)
        if (isFuelEmergency()) {
            if (!"fuel".equals(currentGoalType)) {
                voidCurrentPath();
            }
            TWDirection dir = navigateTo(fuelPos.x, fuelPos.y, "fuel");
            if (dir != null) {
                return new TWThought(TWAction.MOVE, dir);
            }
            dir = greedyDirection(fuelPos.x, fuelPos.y);
            if (dir != null) {
                return new TWThought(TWAction.MOVE, dir);
            }
            // Last resort — any movement toward fuel station
            return new TWThought(TWAction.MOVE, getRandomSafeDirection());
        }

        // 3. OPPORTUNISTIC DROP — on a hole while carrying tile
        if (hasTile()) {
            TWEntity objHere = smartMemory.getObjectAtCurrentPos();
            if (objHere instanceof TWHole) {
                return new TWThought(TWAction.PUTDOWN, TWDirection.Z);
            }
        }

        // 4. OPPORTUNISTIC PICKUP — on a tile while carrying < 3
        if (carriedTiles.size() < 3) {
            TWEntity objHere = smartMemory.getObjectAtCurrentPos();
            if (objHere instanceof TWTile) {
                return new TWThought(TWAction.PICKUP, TWDirection.Z);
            }
        }

        // 5. EXPLORE — systematic coverage with persistent target
        TWDirection dir = exploreDirection();
        if (dir != null) {
            return new TWThought(TWAction.MOVE, dir);
        }

        // 6. WAIT
        return new TWThought(TWAction.MOVE, TWDirection.Z);
    }

    @Override
    protected void act(TWThought thought) {
        switch (thought.getAction()) {
            case MOVE:
                try {
                    this.move(thought.getDirection());
                } catch (CellBlockedException ex) {
                    // Path blocked — void current plan so we replan next step
                    voidCurrentPath();
                }
                break;
            case PICKUP:
                TWEntity tileHere = smartMemory.getObjectAtCurrentPos();
                if (tileHere instanceof TWTile) {
                    pickUpTile((TWTile) tileHere);
                    smartMemory.removeObject(tileHere);
                }
                break;
            case PUTDOWN:
                TWEntity holeHere = smartMemory.getObjectAtCurrentPos();
                if (holeHere instanceof TWHole) {
                    putTileInHole((TWHole) holeHere);
                    smartMemory.removeObject(holeHere);
                }
                break;
            case REFUEL:
                refuel();
                break;
        }
    }

    // ---- Fuel safety ----

    private boolean isFuelEmergency() {
        return fuelLevel <= computeSafetyMargin();
    }

    private int computeSafetyMargin() {
        if (smartMemory.isFuelStationKnown()) {
            Int2D fuelPos = smartMemory.getKnownFuelStation();
            int manhattanDist = Math.abs(getX() - fuelPos.x) + Math.abs(getY() - fuelPos.y);
            // 1.5x for obstacle detours + 30 buffer for replanning overhead
            return (int)(manhattanDist * 1.5) + 30;
        } else {
            // Unknown fuel station — very conservative
            return (int)(Parameters.defaultFuelLevel * 0.5);
        }
    }

    // ---- Navigation ----

    /**
     * Navigate toward a target using cached A* paths.
     * Returns next direction, or null if no path found.
     */
    private TWDirection navigateTo(int tx, int ty, String goalType) {
        // If we have a valid cached path for this goal, follow it
        if (currentPath != null && currentPath.hasNext()
                && goalType.equals(currentGoalType)
                && currentGoalPos != null && currentGoalPos.x == tx && currentGoalPos.y == ty) {
            TWPathStep step = currentPath.popNext();
            return step.getDirection();
        }

        // Generate new A* path
        TWPath path = pathGenerator.findPath(getX(), getY(), tx, ty);
        if (path != null && path.hasNext()) {
            currentPath = path;
            currentGoalType = goalType;
            currentGoalPos = new Int2D(tx, ty);
            TWPathStep step = currentPath.popNext();
            return step.getDirection();
        }

        return null; // caller handles fallback
    }

    /**
     * Systematic exploration using a state-machine lawnmower.
     * HORIZONTAL: sweep East/West until hitting the edge.
     * SHIFTING: move North/South by LAWNMOWER_STEP cells, then reverse horizontal direction.
     * Pure greedy — no A* overhead.
     */
    private TWDirection exploreDirection() {
        return sweepStep();
    }

    /**
     * Greedy exploration for when fuel station is unknown — same state machine.
     */
    private TWDirection exploreGreedy() {
        return sweepStep();
    }

    /**
     * One step of the lawnmower state machine. Returns a direction to move.
     */
    private TWDirection sweepStep() {
        int xDim = smartMemory.getXDim();
        int yDim = smartMemory.getYDim();

        if (sweepState == SweepState.HORIZONTAL) {
            TWDirection moveDir = sweepGoingRight ? TWDirection.E : TWDirection.W;

            // Check if we've reached the edge we're sweeping toward
            boolean atTargetEdge = (sweepGoingRight && getX() >= xDim - 2)
                                || (!sweepGoingRight && getX() <= 1);
            if (atTargetEdge) {
                sweepState = SweepState.SHIFTING;
                sweepGoingRight = !sweepGoingRight;
                shiftRemaining = LAWNMOWER_STEP;
                return sweepStep();
            }

            // Normal horizontal movement
            if (canMove(moveDir)) return moveDir;
            // Obstacle — try to go around vertically (prefer shift direction)
            TWDirection detour = shiftGoingDown ? TWDirection.S : TWDirection.N;
            if (canMove(detour)) return detour;
            detour = shiftGoingDown ? TWDirection.N : TWDirection.S;
            if (canMove(detour)) return detour;
            return getRandomSafeDirection();
        }

        // SHIFTING state: move vertically by LAWNMOWER_STEP cells, reversing at edges
        if (sweepState == SweepState.SHIFTING) {
            if (shiftRemaining <= 0) {
                sweepState = SweepState.HORIZONTAL;
                return sweepStep();
            }

            TWDirection shiftDir = shiftGoingDown ? TWDirection.S : TWDirection.N;
            int ny = getY() + shiftDir.dy;

            // Hit vertical edge — reverse direction
            if (!getEnvironment().isInBounds(getX(), ny)) {
                shiftGoingDown = !shiftGoingDown;
                shiftDir = shiftGoingDown ? TWDirection.S : TWDirection.N;
                ny = getY() + shiftDir.dy;
                if (!getEnvironment().isInBounds(getX(), ny)) {
                    // Stuck at corner — go horizontal
                    sweepState = SweepState.HORIZONTAL;
                    return sweepStep();
                }
            }

            if (canMove(shiftDir)) {
                shiftRemaining--;
                return shiftDir;
            }
            // Obstacle during shift — try horizontal to get around
            TWDirection detour = sweepGoingRight ? TWDirection.E : TWDirection.W;
            if (canMove(detour)) return detour;
            return getRandomSafeDirection();
        }

        return getRandomSafeDirection();
    }

    /**
     * Simple greedy movement toward target.
     */
    private TWDirection greedyDirection(int tx, int ty) {
        int dx = tx - getX();
        int dy = ty - getY();

        TWDirection primary = null;
        TWDirection secondary = null;

        if (Math.abs(dx) >= Math.abs(dy)) {
            primary = dx > 0 ? TWDirection.E : (dx < 0 ? TWDirection.W : null);
            secondary = dy > 0 ? TWDirection.S : (dy < 0 ? TWDirection.N : null);
        } else {
            primary = dy > 0 ? TWDirection.S : (dy < 0 ? TWDirection.N : null);
            secondary = dx > 0 ? TWDirection.E : (dx < 0 ? TWDirection.W : null);
        }

        if (primary != null && canMove(primary)) return primary;
        if (secondary != null && canMove(secondary)) return secondary;

        // Try perpendicular directions to get around obstacles
        if (primary != null) {
            // Try both perpendicular directions
            if (primary == TWDirection.E || primary == TWDirection.W) {
                if (canMove(TWDirection.N)) return TWDirection.N;
                if (canMove(TWDirection.S)) return TWDirection.S;
            } else {
                if (canMove(TWDirection.E)) return TWDirection.E;
                if (canMove(TWDirection.W)) return TWDirection.W;
            }
        }

        return getRandomSafeDirection();
    }

    private boolean canMove(TWDirection dir) {
        int nx = getX() + dir.dx;
        int ny = getY() + dir.dy;
        return getEnvironment().isInBounds(nx, ny)
                && !getEnvironment().isCellBlocked(nx, ny);
    }

    private TWDirection getRandomSafeDirection() {
        TWDirection[] dirs = {TWDirection.N, TWDirection.S, TWDirection.E, TWDirection.W};
        int start = getEnvironment().random.nextInt(4);
        for (int i = 0; i < 4; i++) {
            TWDirection d = dirs[(start + i) % 4];
            if (canMove(d)) return d;
        }
        return TWDirection.Z;
    }

    private void voidCurrentPath() {
        currentPath = null;
        currentGoalType = null;
        currentGoalPos = null;
    }

    @Override
    public String getName() {
        return name;
    }

    public SmartTWAgentMemory getSmartMemory() {
        return smartMemory;
    }

    public static Portrayal getPortrayal() {
        return new TWAgentPortrayal(Color.red, Parameters.defaultSensorRange) {
            @Override
            public Inspector getInspector(LocationWrapper wrapper, GUIState state) {
                return new AgentInspector(super.getInspector(wrapper, state), wrapper, state);
            }
        };
    }
}
