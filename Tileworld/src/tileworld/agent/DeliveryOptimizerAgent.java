package tileworld.agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;

/**
 * DeliveryOptimizerAgent — Enhanced Freshness-Aware Cluster Delivery.
 *
 * Strategy: a strict gate cascade in think():
 *
 *   GATE 0 (FUEL DANGER)        — delegate to super.think().
 *   GATE 1 (VALIDATE COMMITMENT)— check committed target against EXPIRING,
 *                                  arrival-time, and fuel affordability.
 *   GATE 2 (DELIVERY)           — when carrying >= adaptive tile target, select hole.
 *   GATE 3 (EN-ROUTE BATCHING)  — batch en-route tiles with adaptive detour budget.
 *   GATE 4 (TILE HUNTING)       — when carrying 0, seek closest unclaimed tile.
 *   GATE 5 (HOTSPOT HARVEST)    — navigate toward highest-value hotspot.
 *   GATE 6 (SUPER FALLBACK)     — standard planner, explore, wait.
 *
 * Key improvements over previous version:
 *   - Reachability checks on tiles AND holes (both gates)
 *   - Tighter expiry thresholds (0.65/0.85)
 *   - Throttled hotspot broadcasts (every 5 steps)
 *   - Fresher tile preference in GATE 4
 */
public class DeliveryOptimizerAgent extends SmartTWAgent {

    // --- Tuning constants ---

    private static final double DENSE_BONUS_WEIGHT  = 4.5;
    private static final double SPARSE_BONUS_WEIGHT = 1.5;
    private static final int DENSE_CLUSTER_RADIUS  = 8;
    private static final int SPARSE_CLUSTER_RADIUS = 12;
    private static final double FRESHNESS_WEIGHT = 10.0;
    private static final int BATCH_DETOUR_BUDGET = 4;
    private static final int CLUSTER_CELL = 7;

    private static final int SHORT_LIFE_TILE_TARGET = 1;
    private static final int LONG_LIFE_TILE_TARGET  = 3;

    private static final int HOLE_REBROADCAST_INTERVAL = 15;
    private static final double HOLE_REBROADCAST_FRESHNESS = 0.7;

    private static final double SHORT_EXPIRY_THRESHOLD = 0.65;
    private static final double LONG_EXPIRY_THRESHOLD  = 0.85;

    private static final int COMMIT_STEPS = 6;
    private static final int HOTSPOT_BROADCAST_INTERVAL = 5;

    // --- Instance fields ---

    private Int2D committedTarget = null;
    private int committedStepsRemaining = 0;
    private String committedGoalType = null;
    private Int2D lastBroadcastExpiring = null;

    public DeliveryOptimizerAgent(String name, int xpos, int ypos,
                                  TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    // == THINK ================================================================

    @Override
    protected TWThought think() {
        TWThought fuelSafety = fuelSafetyOverride();
        if (fuelSafety != null) {
            voidCommitment();
            return fuelSafety;
        }

        // Free pickup/drop at current cell
        TWThought opp = opportunisticAction();
        if (opp != null) {
            return opp;
        }

        // == GATE 0: FUEL DANGER ==================================================
        if (isInFuelDanger()) {
            voidCommitment();
            return super.think();
        }

        int carried = getCarriedTileCount();
        int tileTarget = computeAdaptiveTileTarget();

        // == GATE 1: VALIDATE COMMITMENT ==========================================
        if (committedTarget != null && committedStepsRemaining > 0) {
            if (isExpiring(committedTarget)) {
                voidCommitment();
            }
            else if (!isReachableBeforeExpiry(committedTarget)) {
                broadcastExpiringIfNeeded(committedTarget);
                voidCommitment();
            }
            else if ("hole".equals(committedGoalType) && !isDeliveryAffordable(committedTarget)) {
                voidCommitment();
            }

            if (committedTarget != null && committedStepsRemaining > 0) {
                committedStepsRemaining--;
                TWDirection dir = navigateTo(committedTarget.x, committedTarget.y, committedGoalType);
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
                voidCommitment();
            }
        }

        // == GATE 2: DELIVERY =====================================================
        if (carried >= tileTarget) {
            Int2D bestHole = findScoredHole();
            if (bestHole != null) {
                getSmartMemory().addClaim(bestHole.x, bestHole.y,
                        getEnvironment().schedule.getTime());
                commitTo(bestHole, "hole");
                TWDirection dir = navigateTo(bestHole.x, bestHole.y, "hole");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // == GATE 3: EN-ROUTE BATCHING ============================================
        if (carried > 0 && carried < tileTarget) {
            Int2D bestHole = findScoredHole();
            if (bestHole != null) {
                Int2D batchTile = findEnRouteTileToward(bestHole);
                if (batchTile != null) {
                    getSmartMemory().addClaim(batchTile.x, batchTile.y,
                            getEnvironment().schedule.getTime());
                    commitTo(batchTile, "batch");
                    TWDirection dir = navigateTo(batchTile.x, batchTile.y, "batch");
                    if (dir != null) {
                        return new TWThought(TWAction.MOVE, dir);
                    }
                }
                getSmartMemory().addClaim(bestHole.x, bestHole.y,
                        getEnvironment().schedule.getTime());
                commitTo(bestHole, "hole");
                TWDirection dir = navigateTo(bestHole.x, bestHole.y, "hole");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // == GATE 4: TILE HUNTING =================================================
        if (carried == 0) {
            Int2D tile = findBestTile();
            if (tile != null) {
                getSmartMemory().addClaim(tile.x, tile.y,
                        getEnvironment().schedule.getTime());
                commitTo(tile, "tile");
                TWDirection dir = navigateTo(tile.x, tile.y, "tile");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // == GATE 5: HOTSPOT HARVEST ==============================================
        if (carried < tileTarget) {
            TWDirection dir = navigateTowardHotspot();
            if (dir != null) {
                return new TWThought(TWAction.MOVE, dir);
            }
        }

        // == GATE 6: SUPER FALLBACK ===============================================
        return super.think();
    }

    // == COMMUNICATE ===========================================================

    @Override
    public void communicate() {
        super.communicate();

        double now = getEnvironment().schedule.getTime();

        if (((int) now) % HOTSPOT_BROADCAST_INTERVAL == 0) {
            broadcastHotspots();
        }

        if (((int) now) % HOLE_REBROADCAST_INTERVAL == 0) {
            List<Int2D> holePositions = getSmartMemory().getAllHolePositions();
            for (Int2D h : holePositions) {
                double obsTime = getSmartMemory().getObservationTime(h.x, h.y);
                if (obsTime >= 0 && (now - obsTime) < Parameters.lifeTime * HOLE_REBROADCAST_FRESHNESS) {
                    getEnvironment().receiveMessage(
                            new Message(getName(), "", "HOLE:" + h.x + "," + h.y + "," + (int) obsTime));
                }
            }
        }

        if (committedTarget != null) {
            getEnvironment().receiveMessage(
                    new Message(getName(), "", "CLAIM:" + committedTarget.x + "," + committedTarget.y));
        }

        if (!lowFuelAgents.isEmpty() && getSmartMemory().isFuelStationKnown()) {
            Int2D fp = getSmartMemory().getKnownFuelStation();
            getEnvironment().receiveMessage(
                    new Message(getName(), "", "FUEL:" + fp.x + "," + fp.y));
        }
    }

    // == PRIVATE HELPERS =======================================================

    private boolean isInFuelDanger() {
        if (!getSmartMemory().isFuelStationKnown()) {
            return fuelLevel <= Parameters.defaultFuelLevel * 0.5;
        }
        Int2D fp = getSmartMemory().getKnownFuelStation();
        int dist = Math.abs(getX() - fp.x) + Math.abs(getY() - fp.y);
        int margin = getSmartMemory().isLargeGrid()
                ? (int)(dist * 2.5) + 50
                : (int)(dist * 2.0) + 40;
        return fuelLevel <= margin;
    }

    private int computeAdaptiveTileTarget() {
        return getSmartMemory().isShortLifetime() ? SHORT_LIFE_TILE_TARGET : LONG_LIFE_TILE_TARGET;
    }

    private boolean isDeliveryAffordable(Int2D hole) {
        if (!getSmartMemory().isFuelStationKnown()) return true;
        Int2D fp = getSmartMemory().getKnownFuelStation();
        int stepsToHole = Math.abs(getX() - hole.x) + Math.abs(getY() - hole.y);
        int stepsHoleToFuel = Math.abs(hole.x - fp.x) + Math.abs(hole.y - fp.y);
        int safetyMargin = Math.max(40, getSmartMemory().getXDim() / 4);
        int totalCost = stepsToHole + stepsHoleToFuel + safetyMargin;
        return fuelLevel >= totalCost;
    }

    private boolean isReachableBeforeExpiry(Int2D target) {
        double now = getEnvironment().schedule.getTime();
        double obsTime = getSmartMemory().getObservationTime(target.x, target.y);
        if (obsTime < 0) return true;
        double age = now - obsTime;
        double remaining = Parameters.lifeTime - age;
        int stepsToArrival = Math.abs(getX() - target.x) + Math.abs(getY() - target.y);
        double threshold = getSmartMemory().isShortLifetime()
                ? SHORT_EXPIRY_THRESHOLD : LONG_EXPIRY_THRESHOLD;
        return stepsToArrival <= remaining * threshold;
    }

    private void broadcastExpiringIfNeeded(Int2D target) {
        if (lastBroadcastExpiring != null
                && lastBroadcastExpiring.x == target.x
                && lastBroadcastExpiring.y == target.y) {
            return;
        }
        getEnvironment().receiveMessage(
                new Message(getName(), "", "EXPIRING:" + target.x + "," + target.y + ",lifetime"));
        lastBroadcastExpiring = new Int2D(target.x, target.y);
    }

    private void commitTo(Int2D target, String goalType) {
        this.committedTarget = target;
        this.committedGoalType = goalType;
        int dist = Math.abs(getX() - target.x) + Math.abs(getY() - target.y);
        this.committedStepsRemaining = Math.min(COMMIT_STEPS * 3, Math.max(COMMIT_STEPS, dist));
    }

    private void voidCommitment() {
        committedTarget = null;
        committedStepsRemaining = 0;
        committedGoalType = null;
    }

    /**
     * Find the best tile, preferring fresh tiles close to the agent.
     */
    private Int2D findBestTile() {
        List<Int2D> tiles = getSmartMemory().getAllTilePositions();
        if (tiles.isEmpty()) return null;

        double now = getEnvironment().schedule.getTime();
        Int2D best = null;
        double bestScore = Double.MAX_VALUE;

        for (Int2D t : tiles) {
            if (getSmartMemory().isClaimed(t.x, t.y)) continue;
            Int2D tPos = new Int2D(t.x, t.y);
            if (isExpiring(tPos)) continue;
            if (!isReachableBeforeExpiry(tPos)) continue;

            double dist = Math.abs(getX() - t.x) + Math.abs(getY() - t.y);

            double obsTime = getSmartMemory().getObservationTime(t.x, t.y);
            double ageFraction = 0;
            if (obsTime >= 0) {
                ageFraction = (now - obsTime) / (double) Parameters.lifeTime;
            }

            // Prefer nearby + fresh tiles
            double score = dist + ageFraction * 5.0;
            if (score < bestScore) {
                bestScore = score;
                best = t;
            }
        }

        return best;
    }

    private Int2D findScoredHole() {
        List<TWHole> holes = getSmartMemory().getAllRememberedHoles();
        if (holes.isEmpty()) return null;

        List<TWTile> tiles = getSmartMemory().getAllRememberedTiles();
        double now = getEnvironment().schedule.getTime();

        boolean dense = getSmartMemory().isDense();
        double bonusWeight   = dense ? DENSE_BONUS_WEIGHT  : SPARSE_BONUS_WEIGHT;
        int    clusterRadius = dense ? DENSE_CLUSTER_RADIUS : SPARSE_CLUSTER_RADIUS;

        double expiryThreshold = getSmartMemory().isShortLifetime()
                ? SHORT_EXPIRY_THRESHOLD : LONG_EXPIRY_THRESHOLD;

        TWHole best = null;
        double bestScore = Double.MAX_VALUE;

        for (TWHole h : holes) {
            if (getSmartMemory().isClaimed(h.getX(), h.getY())) continue;
            Int2D holePos = new Int2D(h.getX(), h.getY());
            if (isExpiring(holePos)) continue;

            double dist = Math.abs(getX() - h.getX()) + Math.abs(getY() - h.getY());

            double ageFraction = 0.0;
            double obsTime = getSmartMemory().getObservationTime(h.getX(), h.getY());
            if (obsTime >= 0) {
                double age = now - obsTime;
                double remaining = Parameters.lifeTime - age;
                if (dist > remaining * expiryThreshold) continue;
                ageFraction = age / (double) Parameters.lifeTime;
            }

            if (!isDeliveryAffordable(holePos)) continue;

            double bonus = 0;
            for (TWTile t : tiles) {
                double td = Math.abs(h.getX() - t.getX()) + Math.abs(h.getY() - t.getY());
                if (td <= clusterRadius) {
                    bonus += 1.0 / (1.0 + td);
                }
            }

            double score = dist - bonusWeight * bonus + FRESHNESS_WEIGHT * ageFraction;
            if (score < bestScore) {
                bestScore = score;
                best = h;
            }
        }

        return (best != null) ? new Int2D(best.getX(), best.getY()) : null;
    }

    private Int2D findEnRouteTileToward(Int2D hole) {
        List<TWTile> tiles = getSmartMemory().getAllRememberedTiles();
        if (tiles.isEmpty()) return null;

        int detourBudget = getSmartMemory().isShortLifetime() ? 2 : BATCH_DETOUR_BUDGET;
        double direct = Math.abs(getX() - hole.x) + Math.abs(getY() - hole.y);
        Int2D best = null;
        double bestDetour = detourBudget + 1;

        for (TWTile t : tiles) {
            if (getSmartMemory().isClaimed(t.getX(), t.getY())) continue;
            Int2D tilePos = new Int2D(t.getX(), t.getY());
            if (isExpiring(tilePos)) continue;
            if (!isReachableBeforeExpiry(tilePos)) continue;

            double dAgentTile = Math.abs(getX() - t.getX()) + Math.abs(getY() - t.getY());
            double dTileHole  = Math.abs(t.getX() - hole.x)  + Math.abs(t.getY() - hole.y);
            double detour = dAgentTile + dTileHole - direct;
            if (detour >= 0 && detour <= detourBudget && detour < bestDetour) {
                bestDetour = detour;
                best = new Int2D(t.getX(), t.getY());
            }
        }

        return best;
    }

    private TWDirection navigateTowardHotspot() {
        if (hotspots.isEmpty()) return null;

        HotspotEntry best = null;
        double bestValue = -1;

        for (HotspotEntry entry : hotspots) {
            int dist = Math.abs(entry.position.x - getX())
                     + Math.abs(entry.position.y - getY());
            if (dist <= Parameters.defaultSensorRange) continue;
            double value = entry.density / (1.0 + dist);
            if (value > bestValue) {
                bestValue = value;
                best = entry;
            }
        }

        if (best == null) return null;
        return navigateTo(best.position.x, best.position.y, "hotspot");
    }

    private void broadcastHotspots() {
        List<TWTile> tiles = getSmartMemory().getAllRememberedTiles();
        if (tiles.size() < 2) return;

        Map<String, int[]> clusters = new HashMap<>();
        for (TWTile t : tiles) {
            int bx = t.getX() / CLUSTER_CELL;
            int by = t.getY() / CLUSTER_CELL;
            String key = bx + "," + by;
            int[] existing = clusters.get(key);
            if (existing == null) {
                clusters.put(key, new int[]{t.getX(), t.getY(), 1});
            } else {
                existing[0] += t.getX();
                existing[1] += t.getY();
                existing[2]++;
            }
        }

        clusters.values().stream()
                .filter(c -> c[2] >= 2)
                .sorted((a, b) -> b[2] - a[2])
                .limit(2)
                .forEach(c -> {
                    int cx = c[0] / c[2];
                    int cy = c[1] / c[2];
                    double density = (double) c[2] / (CLUSTER_CELL * CLUSTER_CELL);
                    getEnvironment().receiveMessage(
                            new Message(getName(), "", "HOTSPOT:" + cx + "," + cy + "," + density));
                });
    }

    private boolean isExpiring(Int2D pos) {
        for (Int2D exp : expiringTargets) {
            if (exp.x == pos.x && exp.y == pos.y) return true;
        }
        return false;
    }
}
