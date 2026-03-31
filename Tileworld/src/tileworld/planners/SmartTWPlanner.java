package tileworld.planners;

import java.util.List;

import sim.util.Int2D;
import tileworld.agent.SmartTWAgent;
import tileworld.agent.SmartTWAgentMemory;
import tileworld.environment.TWDirection;

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
     * Skips holes claimed by other agents.
     */
    public TWPath planToHole() {
        voidPlan();
        Int2D fuelPos = memory.getKnownFuelStation();
        List<Int2D> holes = memory.getAllHolePositions();
        if (holes.isEmpty())
            return null;

        // Adaptive affordability based on environment
        double affordMult = memory.isShortLifetime() ? 1.3 : 1.5;
        int affordBuffer = memory.isShortLifetime() ? 20 : 30;
        int maxDist = memory.isShortLifetime() ? (int) (memory.getEstimatedLifetime() * 0.6) : Integer.MAX_VALUE;

        Int2D bestHole = null;
        double bestDist = Double.MAX_VALUE;

        for (Int2D hole : holes) {
            if (memory.isClaimed(hole.x, hole.y))
                continue;

            int costToHole = manhattan(agent.getX(), agent.getY(), hole.x, hole.y);
            if (costToHole > maxDist)
                continue;

            int costHoleToFuel = (fuelPos != null)
                    ? manhattan(hole.x, hole.y, fuelPos.x, fuelPos.y)
                    : 25;

            int totalCost = costToHole + costHoleToFuel;
            boolean affordable = agent.getFuelLevel() > totalCost * affordMult + affordBuffer;
            if (!affordable)
                continue;

            if (costToHole < bestDist) {
                bestDist = costToHole;
                bestHole = hole;
            }
        }

        if (bestHole == null)
            return null;

        TWPath path = pathGenerator.findPath(agent.getX(), agent.getY(), bestHole.x, bestHole.y);
        if (path != null && path.hasNext()) {
            currentPath = path;
            currentGoal = new Int2D(bestHole.x, bestHole.y);
            goalType = "hole";
            return path;
        }
        return null;
    }

    /**
     * Plan to pick up the best affordable tile.
     * Skips tiles claimed by other agents.
     */
    public TWPath planToTile() {
        voidPlan();
        List<Int2D> tiles = memory.getAllTilePositions();
        if (tiles.isEmpty())
            return null;

        Int2D fuelPos = memory.getKnownFuelStation();
        if (fuelPos == null)
            return null;

        // Adaptive affordability based on environment
        double affordMult = memory.isShortLifetime() ? 1.3 : 1.5;
        int affordBuffer = memory.isShortLifetime() ? 20 : 30;
        int maxDist = memory.isShortLifetime() ? (int) (memory.getEstimatedLifetime() * 0.6) : Integer.MAX_VALUE;

        Int2D bestTile = null;
        double bestScore = Double.MAX_VALUE;

        for (Int2D tile : tiles) {
            if (memory.isClaimed(tile.x, tile.y))
                continue;

            int costToTile = manhattan(agent.getX(), agent.getY(), tile.x, tile.y);
            if (costToTile > maxDist)
                continue;

            Int2D nearestHole = memory.getClosestHolePosition(tile.x, tile.y);
            int costTileToHole;
            int holeX, holeY;
            if (nearestHole != null) {
                costTileToHole = manhattan(tile.x, tile.y, nearestHole.x, nearestHole.y);
                holeX = nearestHole.x;
                holeY = nearestHole.y;
            } else {
                costTileToHole = 20;
                holeX = tile.x;
                holeY = tile.y;
            }

            int costHoleToFuel = manhattan(holeX, holeY, fuelPos.x, fuelPos.y);
            int totalCost = costToTile + costTileToHole + costHoleToFuel;

            boolean affordable = agent.getFuelLevel() > totalCost * affordMult + affordBuffer;
            if (!affordable)
                continue;

            // Hole proximity scoring: only in dense environments where holes are plentiful
            double score = memory.isDense() ? costToTile + 0.5 * costTileToHole : costToTile;
            if (score < bestScore) {
                bestScore = score;
                bestTile = tile;
            }
        }

        if (bestTile == null)
            return null;

        TWPath path = pathGenerator.findPath(agent.getX(), agent.getY(), bestTile.x, bestTile.y);
        if (path != null && path.hasNext()) {
            currentPath = path;
            currentGoal = new Int2D(bestTile.x, bestTile.y);
            goalType = "tile";
            return path;
        }
        return null;
    }

    @Override
    public TWDirection execute() {
        if (!hasPlan())
            return null;

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

        // Dynamic replanning: switch to significantly closer target (dense envs only)
        if (memory.isDense() && currentGoal != null) {
            int currentDist = manhattan(agent.getX(), agent.getY(), currentGoal.x, currentGoal.y);
            if (currentDist > 6) { // only replan if current target isn't already close
                Int2D closer = null;
                if ("tile".equals(goalType)) {
                    closer = memory.getClosestTilePosition();
                } else if ("hole".equals(goalType)) {
                    closer = memory.getClosestHolePosition();
                }
                if (closer != null && !memory.isClaimed(closer.x, closer.y)
                        && (closer.x != currentGoal.x || closer.y != currentGoal.y)) {
                    int newDist = manhattan(agent.getX(), agent.getY(), closer.x, closer.y);
                    if (newDist * 3 < currentDist) { // >66% closer to avoid thrashing
                        voidPlan();
                        return null; // trigger replan in think()
                    }
                }
            }
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
