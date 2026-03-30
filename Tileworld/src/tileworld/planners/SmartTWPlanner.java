package tileworld.planners;

import java.util.List;
import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.agent.SmartTWAgent;
import tileworld.agent.SmartTWAgentMemory;
import tileworld.environment.TWDirection;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;

/**
 * SmartTWPlanner — Phase 2 goal-directed planner for SmartTWAgent.
 *
 * Uses A* pathfinding to navigate agents toward tiles and holes.
 * Plans are invalidated when targets decay from memory or fuel runs low.
 */
public class SmartTWPlanner implements TWPlanner {

    private final SmartTWAgent agent;
    private final SmartTWAgentMemory memory;
    private final AstarPathGenerator pathGenerator;

    private TWPath currentPath;
    private Int2D currentGoal;
    private String goalType; // "tile", "hole"

    public SmartTWPlanner(SmartTWAgent agent, SmartTWAgentMemory memory, AstarPathGenerator pathGenerator) {
        this.agent = agent;
        this.memory = memory;
        this.pathGenerator = pathGenerator;
    }

    @Override
    public TWPath generatePlan() {
        voidPlan();

        if (agent.hasTile()) {
            return planToHole();
        } else if (agent.getCarriedTileCount() < 3) {
            return planToTile();
        }
        return null;
    }

    /**
     * Plan to deliver a carried tile to the nearest affordable hole.
     * "Affordable" means the agent can reach the hole and still get back to fuel.
     */
    public TWPath planToHole() {
        voidPlan();
        Int2D fuelPos = memory.getKnownFuelStation();
        List<TWHole> holes = memory.getAllRememberedHoles();
        if (holes.isEmpty()) return null;

        TWHole bestHole = null;
        double bestDist = Double.MAX_VALUE;

        for (TWHole hole : holes) {
            int costToHole = manhattan(agent.getX(), agent.getY(), hole.getX(), hole.getY());
            int costHoleToFuel = (fuelPos != null)
                    ? manhattan(hole.getX(), hole.getY(), fuelPos.x, fuelPos.y)
                    : 25; // estimate if fuel unknown

            int totalCost = costToHole + costHoleToFuel;
            boolean affordable = agent.getFuelLevel() > totalCost * 1.5 + 30;
            if (!affordable) continue;

            if (costToHole < bestDist) {
                bestDist = costToHole;
                bestHole = hole;
            }
        }

        if (bestHole == null) return null;

        TWPath path = pathGenerator.findPath(agent.getX(), agent.getY(), bestHole.getX(), bestHole.getY());
        if (path != null && path.hasNext()) {
            currentPath = path;
            currentGoal = new Int2D(bestHole.getX(), bestHole.getY());
            goalType = "hole";
            return path;
        }
        return null;
    }

    /**
     * Plan to pick up the best affordable tile.
     * "Affordable" means the agent has enough fuel for the round trip:
     * agent -> tile -> nearest hole -> fuel station.
     */
    public TWPath planToTile() {
        voidPlan();
        List<TWTile> tiles = memory.getAllRememberedTiles();
        if (tiles.isEmpty()) return null;

        Int2D fuelPos = memory.getKnownFuelStation();
        if (fuelPos == null) return null; // can't estimate fuel cost without fuel station

        TWTile bestTile = null;
        double bestScore = Double.MAX_VALUE;

        for (TWTile tile : tiles) {
            int costToTile = manhattan(agent.getX(), agent.getY(), tile.getX(), tile.getY());

            // Estimate cost from tile to nearest hole (or default if no holes known)
            TWHole nearestHole = memory.getClosestRememberedHole(tile.getX(), tile.getY());
            int costTileToHole;
            int holeX, holeY;
            if (nearestHole != null) {
                costTileToHole = manhattan(tile.getX(), tile.getY(), nearestHole.getX(), nearestHole.getY());
                holeX = nearestHole.getX();
                holeY = nearestHole.getY();
            } else {
                costTileToHole = 20; // estimated average
                holeX = tile.getX();
                holeY = tile.getY();
            }

            int costHoleToFuel = manhattan(holeX, holeY, fuelPos.x, fuelPos.y);
            int totalCost = costToTile + costTileToHole + costHoleToFuel;

            // Check if the round trip is affordable with safety margin
            boolean affordable = agent.getFuelLevel() > totalCost * 1.5 + 30;
            if (!affordable) continue;

            // Score by distance to tile (prefer closer tiles)
            if (costToTile < bestScore) {
                bestScore = costToTile;
                bestTile = tile;
            }
        }

        if (bestTile == null) return null;

        TWPath path = pathGenerator.findPath(agent.getX(), agent.getY(), bestTile.getX(), bestTile.getY());
        if (path != null && path.hasNext()) {
            currentPath = path;
            currentGoal = new Int2D(bestTile.getX(), bestTile.getY());
            goalType = "tile";
            return path;
        }
        return null;
    }

    @Override
    public TWDirection execute() {
        if (!hasPlan()) return null;

        // Invalidate if target no longer in memory
        if (currentGoal != null) {
            Object remembered = memory.getRememberedEntity(currentGoal.x, currentGoal.y);
            if (remembered == null) {
                // Target decayed or was consumed — replan
                voidPlan();
                return null;
            }
        }

        // Check if arrived at goal
        if (currentGoal != null && agent.getX() == currentGoal.x && agent.getY() == currentGoal.y) {
            voidPlan();
            return null; // arrived — let think() handle pickup/putdown
        }

        TWPathStep step = currentPath.popNext();
        return step.getDirection();
    }

    @Override
    public boolean hasPlan() {
        return currentPath != null && currentPath.hasNext();
    }

    @Override
    public void voidPlan() {
        currentPath = null;
        currentGoal = null;
        goalType = null;
    }

    @Override
    public Int2D getCurrentGoal() {
        return currentGoal;
    }

    public String getGoalType() {
        return goalType;
    }

    private static int manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }
}
