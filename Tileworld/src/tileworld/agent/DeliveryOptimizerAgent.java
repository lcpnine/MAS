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
 * Strategy: a strict 7-gate cascade in think():
 *
 *   GATE 0 (FUEL DANGER)        — replicate parent's fuel-emergency check;
 *                                  if in danger, delegate entirely to super.think().
 *   GATE 1 (VALIDATE COMMITMENT)— check committed target against EXPIRING broadcasts,
 *                                  arrival-time projection, and fuel affordability.
 *   GATE 2 (DELIVERY)           — when carrying >= adaptive tile target, select hole
 *                                  using composite score with fuel affordability and
 *                                  dual-threshold expiry; immediately claim and commit.
 *   GATE 3 (EN-ROUTE BATCHING)  — when carrying some tiles but below target, batch
 *                                  en-route tiles with adaptive detour budget.
 *   GATE 4 (TILE HUNTING)       — when carrying 0 tiles, seek closest unclaimed tile
 *                                  with immediate claiming.
 *   GATE 5 (HOTSPOT HARVEST)    — navigate toward highest-value hotspot from teammates.
 *   GATE 6 (SUPER FALLBACK)     — opportunistic pickup/drop, tile seeking, exploration.
 *
 * Improvements over previous version (borrowed from sibling agents):
 *   1. Adaptive tile target: 1 in short-lifetime, 3 in long-lifetime (TileHunterAgent)
 *   2. Dual-threshold arrival-time expiry projection (SmarterReplanningAgent)
 *   3. Fuel affordability check for delivery round-trip (SmarterReplanningAgent)
 *   4. Immediate claiming of chosen targets (TileHunterAgent)
 *   5. Committed navigation to prevent oscillation (FuelScoutAgent)
 *   6. EXPIRING broadcast to warn teammates (SmarterReplanningAgent + HoleFillerAgent)
 *   7. Periodic hole rebroadcasting (HoleFillerAgent)
 *   8. Adaptive en-route detour budget (2 for short-life, 4 for long-life)
 *   9. Dedicated tile-hunting gate when carrying 0 tiles
 *  10. LOW-aware fuel station rebroadcasting
 */
public class DeliveryOptimizerAgent extends SmartTWAgent {

    // --- Existing tuning constants ---

    /** Weight for tile-cluster proximity bonus in dense environments. */
    private static final double DENSE_BONUS_WEIGHT  = 4.5;

    /** Weight for tile-cluster proximity bonus in sparse environments. */
    private static final double SPARSE_BONUS_WEIGHT = 1.5;

    /** Cluster radius (manhattan) used when scoring holes in dense environments. */
    private static final int DENSE_CLUSTER_RADIUS  = 8;

    /** Cluster radius (manhattan) used when scoring holes in sparse environments. */
    private static final int SPARSE_CLUSTER_RADIUS = 12;

    /**
     * Freshness penalty weight applied to hole age fraction (0-1).
     */
    private static final double FRESHNESS_WEIGHT = 15.0;

    /**
     * Maximum extra Manhattan steps accepted to pick up a tile en-route (long-life).
     */
    private static final int BATCH_DETOUR_BUDGET = 4;

    /**
     * Spatial cell size for grouping tiles into HOTSPOT broadcasts.
     */
    private static final int CLUSTER_CELL = 7;

    // --- New constants (from sibling agents) ---

    /** Adaptive tile target: deliver immediately in short-lifetime envs. (TileHunterAgent) */
    private static final int SHORT_LIFE_TILE_TARGET = 1;
    private static final int LONG_LIFE_TILE_TARGET  = 3;

    /** Periodic hole rebroadcasting interval in simulation steps. (HoleFillerAgent) */
    private static final int HOLE_REBROADCAST_INTERVAL = 15;
    /** Only rebroadcast holes younger than this fraction of lifetime. */
    private static final double HOLE_REBROADCAST_FRESHNESS = 0.7;

    /** Dual expiry thresholds for arrival-time projection. (SmarterReplanningAgent) */
    private static final double SHORT_EXPIRY_THRESHOLD = 0.7;
    private static final double LONG_EXPIRY_THRESHOLD  = 0.9;

    /** Minimum commitment steps before reconsidering target. (FuelScoutAgent) */
    private static final int COMMIT_STEPS = 6;

    // --- Instance fields for commitment and EXPIRING tracking ---

    private Int2D committedTarget = null;
    private int committedStepsRemaining = 0;
    private String committedGoalType = null;  // "hole", "batch", "tile", "hotspot"
    private Int2D lastBroadcastExpiring = null;

    // -------------------------------------------------------------------------

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

        // == GATE 0: FUEL DANGER ==================================================
        if (isInFuelDanger()) {
            voidCommitment();
            return super.think();
        }

        int carried = getCarriedTileCount();
        int tileTarget = computeAdaptiveTileTarget();

        // == GATE 1: VALIDATE COMMITMENT ==========================================
        if (committedTarget != null && committedStepsRemaining > 0) {
            // 1a: Teammate flagged target as EXPIRING
            if (isExpiring(committedTarget)) {
                voidCommitment();
            }
            // 1b: Arrival-time projection (dual-threshold)
            else if (!isReachableBeforeExpiry(committedTarget)) {
                broadcastExpiringIfNeeded(committedTarget);
                voidCommitment();
            }
            // 1c: Fuel affordability (only for hole deliveries)
            else if ("hole".equals(committedGoalType) && !isDeliveryAffordable(committedTarget)) {
                voidCommitment();
            }

            // If commitment survived all checks, continue pursuing it
            if (committedTarget != null && committedStepsRemaining > 0) {
                committedStepsRemaining--;
                TWDirection dir = navigateTo(committedTarget.x, committedTarget.y, committedGoalType);
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
                // Navigation failed (blocked/consumed) — void and re-evaluate
                voidCommitment();
            }
        }

        // == GATE 2: DELIVERY (carrying >= adaptive tile target) ==================
        if (carried >= tileTarget) {
            Int2D bestHole = findScoredHole();
            if (bestHole != null) {
                // Immediate claiming (from TileHunterAgent)
                getSmartMemory().addClaim(bestHole.x, bestHole.y,
                        getEnvironment().schedule.getTime());
                // Commit to prevent oscillation (from FuelScoutAgent)
                commitTo(bestHole, "hole");

                TWDirection dir = navigateTo(bestHole.x, bestHole.y, "hole");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
            // No viable hole — fall through to explore
        }

        // == GATE 3: EN-ROUTE BATCHING (carrying some, below target) ==============
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
                // No viable batch tile — deliver what we have
                getSmartMemory().addClaim(bestHole.x, bestHole.y,
                        getEnvironment().schedule.getTime());
                commitTo(bestHole, "hole");
                TWDirection dir = navigateTo(bestHole.x, bestHole.y, "hole");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // == GATE 4: TILE HUNTING (carrying 0 tiles) ==============================
        if (carried == 0) {
            Int2D tile = getSmartMemory().getClosestTilePosition();
            if (tile != null
                    && !getSmartMemory().isClaimed(tile.x, tile.y)
                    && !isExpiring(tile)
                    && isReachableBeforeExpiry(tile)) {
                getSmartMemory().addClaim(tile.x, tile.y,
                        getEnvironment().schedule.getTime());
                commitTo(tile, "tile");
                TWDirection dir = navigateTo(tile.x, tile.y, "tile");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // == GATE 5: HOTSPOT-GUIDED TILE HARVESTING ===============================
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
        broadcastHotspots();

        double now = getEnvironment().schedule.getTime();

        // Periodic hole rebroadcasting (from HoleFillerAgent)
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

        // Broadcast committed target as CLAIM for teammate awareness
        if (committedTarget != null) {
            getEnvironment().receiveMessage(
                    new Message(getName(), "", "CLAIM:" + committedTarget.x + "," + committedTarget.y));
        }

        // LOW-aware fuel station rebroadcast: if teammates are low on fuel, share station
        if (!lowFuelAgents.isEmpty() && getSmartMemory().isFuelStationKnown()) {
            Int2D fp = getSmartMemory().getKnownFuelStation();
            getEnvironment().receiveMessage(
                    new Message(getName(), "", "FUEL:" + fp.x + "," + fp.y));
        }
    }

    // == PRIVATE HELPERS =======================================================

    /**
     * Replicate SmartTWAgent's private isFuelEmergency() + computeSafetyMargin().
     * MUST MATCH the base class computation exactly to avoid safety inconsistencies.
     */
    private boolean isInFuelDanger() {
        if (!getSmartMemory().isFuelStationKnown()) {
            // Must match SmartTWAgent: 50% of tank when station unknown
            return fuelLevel <= Parameters.defaultFuelLevel * 0.5;
        }
        Int2D fp = getSmartMemory().getKnownFuelStation();
        int dist = Math.abs(getX() - fp.x) + Math.abs(getY() - fp.y);
        // Match SmartTWAgent safety computation exactly.
        int margin = getSmartMemory().isLargeGrid()
                ? (int)(dist * 2.5) + 50
                : (int)(dist * 2.0) + 40;
        return fuelLevel <= margin;
    }

    /**
     * Adaptive tile capacity target. In short-lifetime environments deliver
     * immediately with 1 tile since holes expire fast. In long-lifetime
     * environments batch 3 for fuel efficiency. (From TileHunterAgent)
     */
    private int computeAdaptiveTileTarget() {
        return getSmartMemory().isShortLifetime() ? SHORT_LIFE_TILE_TARGET : LONG_LIFE_TILE_TARGET;
    }

    /**
     * Check if the agent can afford to reach the target hole AND return to
     * the fuel station afterward. (From SmarterReplanningAgent)
     */
    private boolean isDeliveryAffordable(Int2D hole) {
        if (!getSmartMemory().isFuelStationKnown()) return true; // can't check, assume ok
        Int2D fp = getSmartMemory().getKnownFuelStation();
        int stepsToHole = Math.abs(getX() - hole.x) + Math.abs(getY() - hole.y);
        int stepsHoleToFuel = Math.abs(hole.x - fp.x) + Math.abs(hole.y - fp.y);
        int safetyMargin = Math.max(50, getSmartMemory().getXDim() / 4);
        int totalCost = stepsToHole + stepsHoleToFuel + safetyMargin;
        return fuelLevel >= totalCost;
    }

    /**
     * Check if the agent can reach the target before it expires, using
     * dual-threshold arrival-time projection. (From SmarterReplanningAgent)
     */
    private boolean isReachableBeforeExpiry(Int2D target) {
        double now = getEnvironment().schedule.getTime();
        double obsTime = getSmartMemory().getObservationTime(target.x, target.y);
        if (obsTime < 0) return true; // unknown observation time, assume ok
        double age = now - obsTime;
        double remaining = Parameters.lifeTime - age;
        int stepsToArrival = Math.abs(getX() - target.x) + Math.abs(getY() - target.y);
        double threshold = getSmartMemory().isShortLifetime()
                ? SHORT_EXPIRY_THRESHOLD : LONG_EXPIRY_THRESHOLD;
        return stepsToArrival <= remaining * threshold;
    }

    /**
     * Broadcast an EXPIRING message for the given target, deduplicated to
     * avoid message flooding. (From SmarterReplanningAgent + HoleFillerAgent)
     */
    private void broadcastExpiringIfNeeded(Int2D target) {
        if (lastBroadcastExpiring != null
                && lastBroadcastExpiring.x == target.x
                && lastBroadcastExpiring.y == target.y) {
            return; // already broadcast for this target
        }
        getEnvironment().receiveMessage(
                new Message(getName(), "", "EXPIRING:" + target.x + "," + target.y + ",lifetime"));
        lastBroadcastExpiring = new Int2D(target.x, target.y);
    }

    /**
     * Commit to navigating toward a target for a number of steps proportional
     * to its distance. Prevents oscillation between targets. (From FuelScoutAgent)
     */
    private void commitTo(Int2D target, String goalType) {
        this.committedTarget = target;
        this.committedGoalType = goalType;
        int dist = Math.abs(getX() - target.x) + Math.abs(getY() - target.y);
        this.committedStepsRemaining = Math.min(COMMIT_STEPS * 3, Math.max(COMMIT_STEPS, dist));
    }

    /**
     * Clear the current committed target.
     */
    private void voidCommitment() {
        committedTarget = null;
        committedStepsRemaining = 0;
        committedGoalType = null;
    }

    /**
     * Score every remembered unclaimed hole and return the best position.
     *
     * Score = dist(agent -> hole)
     *       - bonusWeight x tile_proximity_bonus   [reward: tiles near hole]
     *       + FRESHNESS_WEIGHT x age_fraction       [penalty: stale hole]
     *
     * Lower is better. Enhanced with:
     *   - Dual-threshold arrival-time expiry projection (SmarterReplanningAgent)
     *   - Fuel affordability check (SmarterReplanningAgent)
     *   - EXPIRING message consumption (HoleFillerAgent)
     */
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

            // Dual-threshold arrival-time expiry projection (replaces fixed 15%)
            double ageFraction = 0.0;
            double obsTime = getSmartMemory().getObservationTime(h.getX(), h.getY());
            if (obsTime >= 0) {
                double age = now - obsTime;
                double remaining = Parameters.lifeTime - age;
                if (dist > remaining * expiryThreshold) continue; // unreachable before expiry
                ageFraction = age / (double) Parameters.lifeTime;
            }

            // Fuel affordability check
            if (!isDeliveryAffordable(holePos)) continue;

            // Tile proximity bonus: tiles near this hole mean more pickups after drop
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

    /**
     * Find a tile whose collection adds at most an adaptive detour budget of
     * extra steps compared to going directly to the target hole.
     * Budget: 2 (short-lifetime) or 4 (long-lifetime).
     *
     * Enhanced with EXPIRING and arrival-time checks on candidate tiles.
     */
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

    /**
     * Navigate toward the highest-value hotspot broadcast by teammates that is
     * outside our current sensor range.
     */
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

    /**
     * Groups remembered tiles into CLUSTER_CELL x CLUSTER_CELL spatial blocks,
     * computes a centroid for each block with >= 2 tiles, and broadcasts the
     * two densest blocks as HOTSPOT messages for teammates to consume.
     */
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

    /**
     * Returns true if this position was flagged as expiring by a teammate.
     */
    private boolean isExpiring(Int2D pos) {
        for (Int2D exp : expiringTargets) {
            if (exp.x == pos.x && exp.y == pos.y) return true;
        }
        return false;
    }
}
