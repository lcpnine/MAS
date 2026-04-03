package tileworld.agent;

import java.awt.Color;
import java.util.ArrayList;

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
import tileworld.planners.SmartTWPlanner;
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
 * 5. DELIVER — carrying tile, navigate to remembered hole via planner
 * 6. SEEK TILE — not full, navigate to affordable remembered tile via planner
 * 7. EXPLORE — systematic lawnmower pattern to cover the grid
 * 8. WAIT — no valid action
 */
public class SmartTWAgent extends TWAgent {

    private final String name;
    private final int agentIndex;
    private SmartTWAgentMemory smartMemory;
    private AstarPathGenerator pathGenerator;
    private SmartTWPlanner planner;

    // Current path being followed
    private TWPath currentPath;
    private Int2D currentGoalPos;
    private String currentGoalType; // "fuel", "explore"

    // Lawnmower exploration state machine
    private enum SweepState {
        HORIZONTAL, SHIFTING
    }

    private SweepState sweepState = SweepState.HORIZONTAL;
    private boolean sweepGoingRight;
    private int shiftRemaining = 0;
    private boolean shiftGoingDown;
    private int LAWNMOWER_STEP = 7; // default conservative; updated after warmup

    // Zone-based exploration
    private int zoneStartX, zoneEndX, zoneStartY, zoneEndY;
    private boolean zoneExplored = false;

    // Custom message data (populated in communicate(), used in think())
    protected final ArrayList<String> lowFuelAgents = new ArrayList<>();
    protected final ArrayList<Int2D> expiringTargets = new ArrayList<>();
    protected final ArrayList<HotspotEntry> hotspots = new ArrayList<>();
    protected final ArrayList<String> zoneSwapRequests = new ArrayList<>();

    // Inner class for hotspot data (position + density together, no parallel lists)
    protected static class HotspotEntry {
        final Int2D position;
        final double density;

        HotspotEntry(Int2D position, double density) {
            this.position = position;
            this.density = density;
        }
    }

    public SmartTWAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(xpos, ypos, env, fuelLevel);
        this.name = name;
        this.agentIndex = agentIndex;

        // Replace default memory with our enhanced version
        this.smartMemory = new SmartTWAgentMemory(this, env.schedule,
                env.getxDimension(), env.getyDimension());
        this.memory = this.smartMemory;

        // A* path generator with max search distance across the grid
        int maxSearch = env.getxDimension() + env.getyDimension();
        this.pathGenerator = new AstarPathGenerator(env, this, maxSearch);

        // Zone assignment: 3 columns x 2 rows for agents 0-5
        int col = agentIndex % 3;
        int row = agentIndex / 3;
        int colWidth = env.getxDimension() / 3;
        int rowHeight = env.getyDimension() / 2;
        zoneStartX = col * colWidth;
        zoneEndX = (col == 2) ? env.getxDimension() - 1 : (col + 1) * colWidth - 1;
        zoneStartY = row * rowHeight;
        zoneEndY = (row == 1) ? env.getyDimension() - 1 : (row + 1) * rowHeight - 1;

        // On large grids, skip zone exploration for faster fuel station discovery
        if (env.getxDimension() >= 70) {
            this.zoneExplored = true;
        }

        // Sweep toward closer zone edge first
        int zoneMidX = (zoneStartX + zoneEndX) / 2;
        int zoneMidY = (zoneStartY + zoneEndY) / 2;
        this.sweepGoingRight = (xpos >= zoneMidX);
        this.shiftGoingDown = (ypos < zoneMidY);

        // Phase 2: goal-directed planner
        this.planner = new SmartTWPlanner(this, this.smartMemory, this.pathGenerator);
    }

    @Override
    public void communicate() {
        double now = getEnvironment().schedule.getTime();

        // Clear previous step's custom message data BEFORE parsing
        // This ensures we start with fresh data from this step's messages
        lowFuelAgents.clear();
        expiringTargets.clear();
        hotspots.clear();
        zoneSwapRequests.clear();

        // 1. Read and process messages from other agents
        smartMemory.clearAllClaims();
        ArrayList<Message> messages = getEnvironment().getMessages();
        for (Message msg : messages) {
            if (msg.getFrom().equals(name))
                continue; // skip own messages
            String content = msg.getMessage();
            if (content == null)
                continue;

            int colonIdx = content.indexOf(':');
            if (colonIdx < 0)
                continue;
            String type = content.substring(0, colonIdx);
            String payload = content.substring(colonIdx + 1);

            try {
                // --- EXISTING BASE CLASS PARSING (keep this unchanged) ---
                if ("FUEL".equals(type)) {
                    String[] parts = payload.split(",");
                    int fx = Integer.parseInt(parts[0]);
                    int fy = Integer.parseInt(parts[1]);
                    smartMemory.setFuelStation(fx, fy);
                } else if ("TILE".equals(type)) {
                    String[] parts = payload.split(",");
                    int tx = Integer.parseInt(parts[0]);
                    int ty = Integer.parseInt(parts[1]);
                    double time = Double.parseDouble(parts[2]);
                    smartMemory.addSharedTile(tx, ty, time);
                } else if ("HOLE".equals(type)) {
                    String[] parts = payload.split(",");
                    int hx = Integer.parseInt(parts[0]);
                    int hy = Integer.parseInt(parts[1]);
                    double time = Double.parseDouble(parts[2]);
                    smartMemory.addSharedHole(hx, hy, time);
                } else if ("GONE".equals(type)) {
                    String[] parts = payload.split(",");
                    int gx = Integer.parseInt(parts[0]);
                    int gy = Integer.parseInt(parts[1]);
                    smartMemory.removeSharedEntity(gx, gy);
                } else if ("CLAIM".equals(type)) {
                    String[] parts = payload.split(",");
                    int cx = Integer.parseInt(parts[0]);
                    int cy = Integer.parseInt(parts[1]);
                    smartMemory.addClaim(cx, cy, now);

                // --- NEW CUSTOM MESSAGE TYPES (add these) ---
                } else if ("LOW".equals(type)) {
                    // Parse: "LOW:agentId,fuel"
                    String[] parts = payload.split(",");
                    if (parts.length >= 1) {
                        lowFuelAgents.add(parts[0]); // Store agentId with low fuel
                    }
                } else if ("EXPIRING".equals(type)) {
                    // Parse: "EXPIRING:x,y,reason"
                    String[] parts = payload.split(",");
                    if (parts.length >= 2) {
                        int ex = Integer.parseInt(parts[0]);
                        int ey = Integer.parseInt(parts[1]);
                        expiringTargets.add(new Int2D(ex, ey));
                    }
                } else if ("HOTSPOT".equals(type)) {
                    // Parse: "HOTSPOT:x,y,density"
                    String[] parts = payload.split(",");
                    if (parts.length >= 3) {
                        Int2D pos = new Int2D(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1])
                        );
                        double density = Double.parseDouble(parts[2]);
                        hotspots.add(new HotspotEntry(pos, density));
                    }
                } else if ("SWAP".equals(type)) {
                    // Parse: "SWAP:agentName,status" e.g., "SWAP:Explorer3,ZONE_DONE"
                    String[] parts = payload.split(",");
                    if (parts.length >= 2) {
                        zoneSwapRequests.add(parts[0] + "," + parts[1]); // Store as "agentName,status"
                    }
                }
            } catch (Exception e) {
                // Malformed message, skip
            }
        }

        // 2. Broadcast fuel station location
        if (smartMemory.isFuelStationKnown()) {
            Int2D fp = smartMemory.getKnownFuelStation();
            sendMsg("FUEL:" + fp.x + "," + fp.y);
        }

        // 3. Broadcast new tile/hole sightings and gone positions
        for (Int2D t : smartMemory.getNewTiles()) {
            sendMsg("TILE:" + t.x + "," + t.y + "," + now);
        }
        for (Int2D h : smartMemory.getNewHoles()) {
            sendMsg("HOLE:" + h.x + "," + h.y + "," + now);
        }
        for (Int2D g : smartMemory.getGoneEntities()) {
            sendMsg("GONE:" + g.x + "," + g.y);
        }

        // 4. Broadcast current planner target as claim
        Int2D goal = planner.getCurrentGoal();
        if (goal != null) {
            sendMsg("CLAIM:" + goal.x + "," + goal.y);
        }
    }

    private void sendMsg(String content) {
        getEnvironment().receiveMessage(new Message(name, "", content));
    }

    @Override
    protected TWThought think() {
        boolean fuelKnown = smartMemory.isFuelStationKnown();

        // 0. FUEL STATION UNKNOWN — explore to find it, but conserve fuel if low
        if (!fuelKnown) {
            // On large grids, conserve fuel when low to wait for FUEL broadcasts
            if (smartMemory.isLargeGrid() && fuelLevel < Parameters.defaultFuelLevel * 0.35) {
                return new TWThought(TWAction.MOVE, TWDirection.Z);
            }
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

        // 2. FUEL EMERGENCY — navigate to fuel station (MUST return, never fall
        // through)
        if (isFuelEmergency()) {
            planner.voidPlan(); // cancel any tile/hole seeking
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

        // 4.5 OPPORTUNISTIC REFUEL — near fuel station and fuel < 70%
        if (fuelLevel < Parameters.defaultFuelLevel * 0.7) {
            Int2D fp = smartMemory.getKnownFuelStation();
            if (fp != null) {
                int distToFuel = Math.abs(getX() - fp.x) + Math.abs(getY() - fp.y);
                if (distToFuel <= 3 && distToFuel > 0) {
                    TWDirection dir = navigateTo(fp.x, fp.y, "fuel");
                    if (dir != null) {
                        return new TWThought(TWAction.MOVE, dir);
                    }
                }
            }
        }

        // 5. TILE BATCHING — if carrying < 3 tiles, pick up nearby tile before
        // delivering (dense only)
        if (smartMemory.isDense() && !smartMemory.isShortLifetime() && hasTile() && carriedTiles.size() < 3) {
            Int2D nearbyTile = findNearbyTile(5);
            if (nearbyTile != null && isAffordableDetour(nearbyTile)) {
                if (!"tile".equals(planner.getGoalType())
                        || planner.getCurrentGoal() == null
                        || (planner.getCurrentGoal().x != nearbyTile.x || planner.getCurrentGoal().y != nearbyTile.y)) {
                    planner.voidPlan();
                }
                TWDirection dir = navigateTo(nearbyTile.x, nearbyTile.y, "batch");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // 6. DELIVER — carrying tile, seek hole via planner
        if (hasTile()) {
            // Void planner if it's pursuing a tile (we already have one, prioritize
            // delivery)
            if ("tile".equals(planner.getGoalType())) {
                planner.voidPlan();
            }
            // Try existing hole plan or generate new one
            if (!planner.hasPlan() || !"hole".equals(planner.getGoalType())) {
                planner.planToHole();
            }
            if (planner.hasPlan()) {
                TWDirection planDir = planner.execute();
                if (planDir != null) {
                    return new TWThought(TWAction.MOVE, planDir);
                }
            }
            // No hole in memory — fall through to explore
        }

        // 6. SEEK TILE — not full, seek affordable tile via planner
        if (carriedTiles.size() < 3) {
            if (!planner.hasPlan() || !"tile".equals(planner.getGoalType())) {
                planner.planToTile();
            }
            if (planner.hasPlan()) {
                TWDirection planDir = planner.execute();
                if (planDir != null) {
                    return new TWThought(TWAction.MOVE, planDir);
                }
            }
            // No affordable tile — fall through to explore
        }

        // Update lawnmower step once environment is classified
        if (smartMemory.isDense() && LAWNMOWER_STEP != 5) {
            LAWNMOWER_STEP = 5;
        }

        // 7. EXPLORE — systematic coverage with persistent target
        TWDirection dir = exploreDirection();
        if (dir != null) {
            return new TWThought(TWAction.MOVE, dir);
        }

        // 8. WAIT
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
            // Large grid + dense obstacles: need extra margin for detours
            if (smartMemory.isLargeGrid()) {
                return (int) (manhattanDist * 2.5) + 50;
            } else {
                return (int) (manhattanDist * 2.0) + 40;
            }
        } else {
            // Unknown fuel station — very conservative
            return (int) (Parameters.defaultFuelLevel * 0.5);
        }
    }

    // ---- Navigation ----

    /**
     * Navigate toward a target using cached A* paths.
     * Returns next direction, or null if no path found.
     */
    protected TWDirection navigateTo(int tx, int ty, String goalType) {
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
     * SHIFTING: move North/South by LAWNMOWER_STEP cells, then reverse horizontal
     * direction.
     * Pure greedy — no A* overhead.
     */
    private TWDirection exploreDirection() {
        return sweepStep();
    }

    /**
     * Greedy exploration for when fuel station is unknown — same state machine.
     */
    protected TWDirection exploreGreedy() {
        return sweepStep();
    }

    /**
     * One step of the lawnmower state machine. Returns a direction to move.
     */
    private TWDirection sweepStep() {
        int xDim = smartMemory.getXDim();
        int yDim = smartMemory.getYDim();

        // Use zone boundaries when zone not yet explored, full grid otherwise
        int sweepMinX = zoneExplored ? 1 : zoneStartX;
        int sweepMaxX = zoneExplored ? xDim - 2 : zoneEndX;
        int sweepMinY = zoneExplored ? 0 : zoneStartY;
        int sweepMaxY = zoneExplored ? yDim - 1 : zoneEndY;

        if (sweepState == SweepState.HORIZONTAL) {
            TWDirection moveDir = sweepGoingRight ? TWDirection.E : TWDirection.W;

            boolean atTargetEdge = (sweepGoingRight && getX() >= sweepMaxX)
                    || (!sweepGoingRight && getX() <= sweepMinX);
            if (atTargetEdge) {
                sweepState = SweepState.SHIFTING;
                sweepGoingRight = !sweepGoingRight;
                shiftRemaining = LAWNMOWER_STEP;
                return sweepStep();
            }

            if (canMove(moveDir))
                return moveDir;
            TWDirection detour = shiftGoingDown ? TWDirection.S : TWDirection.N;
            if (canMove(detour))
                return detour;
            detour = shiftGoingDown ? TWDirection.N : TWDirection.S;
            if (canMove(detour))
                return detour;
            return getRandomSafeDirection();
        }

        if (sweepState == SweepState.SHIFTING) {
            if (shiftRemaining <= 0) {
                // Check if zone exploration is complete
                if (!zoneExplored && getY() >= sweepMaxY && shiftGoingDown) {
                    zoneExplored = true;
                } else if (!zoneExplored && getY() <= sweepMinY && !shiftGoingDown) {
                    zoneExplored = true;
                }
                sweepState = SweepState.HORIZONTAL;
                return sweepStep();
            }

            TWDirection shiftDir = shiftGoingDown ? TWDirection.S : TWDirection.N;
            int ny = getY() + shiftDir.dy;

            // Hit boundary — reverse direction
            boolean hitBoundary = !getEnvironment().isInBounds(getX(), ny)
                    || (!zoneExplored && (ny > sweepMaxY || ny < sweepMinY));
            if (hitBoundary) {
                if (!zoneExplored) {
                    zoneExplored = true; // reached zone edge, switch to full grid
                }
                shiftGoingDown = !shiftGoingDown;
                shiftDir = shiftGoingDown ? TWDirection.S : TWDirection.N;
                ny = getY() + shiftDir.dy;
                if (!getEnvironment().isInBounds(getX(), ny)) {
                    sweepState = SweepState.HORIZONTAL;
                    return sweepStep();
                }
            }

            if (canMove(shiftDir)) {
                shiftRemaining--;
                return shiftDir;
            }
            TWDirection detour = sweepGoingRight ? TWDirection.E : TWDirection.W;
            if (canMove(detour))
                return detour;
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

        if (primary != null && canMove(primary))
            return primary;
        if (secondary != null && canMove(secondary))
            return secondary;

        // Try perpendicular directions to get around obstacles
        if (primary != null) {
            // Try both perpendicular directions
            if (primary == TWDirection.E || primary == TWDirection.W) {
                if (canMove(TWDirection.N))
                    return TWDirection.N;
                if (canMove(TWDirection.S))
                    return TWDirection.S;
            } else {
                if (canMove(TWDirection.E))
                    return TWDirection.E;
                if (canMove(TWDirection.W))
                    return TWDirection.W;
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
        TWDirection[] dirs = { TWDirection.N, TWDirection.S, TWDirection.E, TWDirection.W };
        int start = getEnvironment().random.nextInt(4);
        for (int i = 0; i < 4; i++) {
            TWDirection d = dirs[(start + i) % 4];
            if (canMove(d))
                return d;
        }
        return TWDirection.Z;
    }

    /**
     * Find the closest unclaimed tile within maxDist manhattan distance.
     */
    private Int2D findNearbyTile(int maxDist) {
        java.util.List<Int2D> tiles = smartMemory.getAllTilePositions();
        Int2D best = null;
        int bestDist = maxDist + 1;
        for (Int2D t : tiles) {
            if (smartMemory.isClaimed(t.x, t.y))
                continue;
            int d = Math.abs(getX() - t.x) + Math.abs(getY() - t.y);
            if (d > 0 && d <= maxDist && d < bestDist) {
                bestDist = d;
                best = t;
            }
        }
        return best;
    }

    /**
     * Check if detouring to a tile is fuel-affordable (tile → nearest hole → fuel
     * station).
     */
    private boolean isAffordableDetour(Int2D tile) {
        Int2D fuelPos = smartMemory.getKnownFuelStation();
        if (fuelPos == null)
            return false;
        int costToTile = Math.abs(getX() - tile.x) + Math.abs(getY() - tile.y);
        Int2D nearestHole = smartMemory.getClosestHolePosition(tile.x, tile.y);
        int costTileToHole = (nearestHole != null)
                ? Math.abs(tile.x - nearestHole.x) + Math.abs(tile.y - nearestHole.y)
                : 20;
        int hx = (nearestHole != null) ? nearestHole.x : tile.x;
        int hy = (nearestHole != null) ? nearestHole.y : tile.y;
        int costHoleToFuel = Math.abs(hx - fuelPos.x) + Math.abs(hy - fuelPos.y);
        int totalCost = costToTile + costTileToHole + costHoleToFuel;
        double affordMult = smartMemory.isDense() ? 1.3 : 1.5;
        int affordBuffer = smartMemory.isDense() ? 20 : 30;
        return fuelLevel > totalCost * affordMult + affordBuffer;
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

    public int getAgentIndex() {
        return agentIndex;
    }

    public int getCarriedTileCount() {
        return carriedTiles.size();
    }

    public SmartTWAgentMemory getSmartMemory() {
        return smartMemory;
    }

    protected SmartTWPlanner getPlanner() {
        return planner;
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
