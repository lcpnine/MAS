package tileworld.agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sim.util.Int2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;

/**
 * DeliveryOptimizerAgent — Cluster-density routing specialist.
 *
 * Two behavioural improvements over SmartTWAgent:
 *
 *  1. HOTSPOT broadcasting — scans remembered tiles each step and broadcasts
 *     HOTSPOT messages for tile clusters. TileHunterAgent uses these to bias
 *     exploration toward tile-dense regions, reducing wasted sweep steps.
 *
 *  2. Cluster-aware hole selection — when delivering tiles, scores candidate
 *     holes by proximity to nearby tile clusters. A hole adjacent to many tiles
 *     is preferred over a marginally closer isolated hole, because the agent can
 *     immediately pick up more tiles after the drop instead of travelling far.
 *
 * All other behaviour (fuel management, opportunistic pickup/drop, tile seeking,
 * exploration, A* pathfinding) is inherited unchanged from SmartTWAgent.
 */
public class DeliveryOptimizerAgent extends SmartTWAgent {

    // --- Tuning constants ---

    /** Manhattan radius around a hole used when scoring tile proximity. */
    private static final int CLUSTER_RADIUS = 10;

    /**
     * Weight applied to tile-proximity bonus when scoring holes.
     * Higher = stronger preference for holes near tile clusters.
     */
    private static final double CLUSTER_BONUS_WEIGHT = 3.0;

    /**
     * Spatial cell size for clustering tiles into hotspots (matches sensor range).
     * Tiles within the same CELL×CELL block are grouped into one HOTSPOT broadcast.
     */
    private static final int CLUSTER_CELL = 7;

    // -------------------------------------------------------------------------

    public DeliveryOptimizerAgent(String name, int xpos, int ypos,
                                  TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    // ── THINK ─────────────────────────────────────────────────────────────────

    /**
     * Intercepts the delivery decision to apply cluster-aware hole selection.
     * All other priorities (fuel emergency, refuel, opportunistic drop/pickup,
     * tile seeking, exploration) fall through to SmartTWAgent.
     */
    @Override
    protected TWThought think() {
        int carried = getCarriedTileCount();

        if (carried > 0) {
            Int2D bestHole = findClusterAwareHole();
            if (bestHole != null
                    && !getSmartMemory().isClaimed(bestHole.x, bestHole.y)
                    && !isExpiring(bestHole)) {

                getSmartMemory().addClaim(
                        bestHole.x, bestHole.y,
                        getEnvironment().schedule.getTime());

                TWDirection dir = navigateTo(bestHole.x, bestHole.y, "hole");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // Fuel safety, opportunistic actions, tile seeking, exploration — all handled here
        return super.think();
    }

    // ── COMMUNICATE ───────────────────────────────────────────────────────────

    /**
     * After base message handling, detect tile clusters in memory and broadcast
     * HOTSPOT messages so teammates (TileHunterAgent) can navigate toward them.
     */
    @Override
    public void communicate() {
        super.communicate();
        broadcastHotspots();
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    /**
     * Scores every remembered hole and returns the position of the one with the
     * best delivery value: nearest hole, biased toward holes adjacent to tile clusters.
     *
     * Score = dist(agent → hole) − CLUSTER_BONUS_WEIGHT × tileProximityBonus
     *
     * A lower score is better. The tile-proximity bonus is the sum of
     * 1/(1+dist) for each remembered tile within CLUSTER_RADIUS of the hole.
     */
    private Int2D findClusterAwareHole() {
        List<TWHole> holes = getSmartMemory().getAllRememberedHoles();
        if (holes.isEmpty()) return null;

        List<TWTile> tiles = getSmartMemory().getAllRememberedTiles();

        TWHole best = null;
        double bestScore = Double.MAX_VALUE;

        for (TWHole h : holes) {
            if (getSmartMemory().isClaimed(h.getX(), h.getY())) continue;

            double dist = Math.abs(getX() - h.getX()) + Math.abs(getY() - h.getY());

            // Proximity bonus: tiles near this hole make it a better delivery target
            double bonus = 0;
            for (TWTile t : tiles) {
                double td = Math.abs(h.getX() - t.getX()) + Math.abs(h.getY() - t.getY());
                if (td <= CLUSTER_RADIUS) {
                    bonus += 1.0 / (1.0 + td);
                }
            }

            double score = dist - CLUSTER_BONUS_WEIGHT * bonus;
            if (score < bestScore) {
                bestScore = score;
                best = h;
            }
        }

        return (best != null) ? new Int2D(best.getX(), best.getY()) : null;
    }

    /**
     * Groups remembered tiles into CLUSTER_CELL×CLUSTER_CELL spatial blocks,
     * computes a centroid for each block with ≥ 2 tiles, and broadcasts the
     * two densest blocks as HOTSPOT messages.
     */
    private void broadcastHotspots() {
        List<TWTile> tiles = getSmartMemory().getAllRememberedTiles();
        if (tiles.size() < 2) return;

        // Accumulate per-cell: [sumX, sumY, count]
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

        // Collect clusters with at least 2 tiles, sort descending by count
        clusters.values().stream()
                .filter(c -> c[2] >= 2)
                .sorted((a, b) -> b[2] - a[2])
                .limit(2)
                .forEach(c -> {
                    int cx = c[0] / c[2]; // centroid X
                    int cy = c[1] / c[2]; // centroid Y
                    double density = (double) c[2] / (CLUSTER_CELL * CLUSTER_CELL);
                    getEnvironment().receiveMessage(
                            new Message(getName(), "", "HOTSPOT:" + cx + "," + cy + "," + density));
                });
    }

    /**
     * Returns true if this position was flagged as expiring by a teammate.
     * Uses manual x/y comparison — MASON's Int2D.equals() is unreliable.
     */
    private boolean isExpiring(Int2D pos) {
        for (Int2D exp : expiringTargets) {
            if (exp.x == pos.x && exp.y == pos.y) return true;
        }
        return false;
    }
}
