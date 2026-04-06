package tileworld.agent;

import sim.util.Int2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;

/**
 * TileHunterAgent — Aggressive tile collection specialist.
 *
 * Behavioural focus: Suppresses hole-seeking until a tile capacity target is
 * reached, then delivers. This batching strategy reduces the number of
 * tile→hole trips, making each fuel unit more productive.
 *
 * Improvements over base SmartTWAgent:
 *  1. Adaptive tile target   — batch 3 in stable envs, deliver immediately in
 *                              volatile envs (short lifetime / Config 2)
 *  2. Aggressive claiming    — claims the target tile immediately to reduce
 *                              conflict with other agents
 *  3. Expiring-target skip   — skips tiles and holes flagged as expiring via
 *                              EXPIRING broadcasts (parsed by base class)
 *  4. Hotspot-biased explore — when no tile is known, moves toward the nearest
 *                              HOTSPOT broadcast rather than random sweeping
 *
 * Everything else (fuel emergency, opportunistic pickup/drop, A* nav,
 * message parsing, zone sweeping) is inherited from SmartTWAgent.
 */
public class TileHunterAgent extends SmartTWAgent {

    public TileHunterAgent(String name, int xpos, int ypos, TWEnvironment env,
                           double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        TWThought fuelSafety = fuelSafetyOverride();
        if (fuelSafety != null) {
            return fuelSafety;
        }

        int carried = getCarriedTileCount();

        // ADAPTIVE TILE TARGET
        // Short-lifetime envs (Config 2): objects expire in ~30 steps, so batching
        // risks the hole disappearing before we arrive. Deliver at 1.
        // Long-lifetime envs (Config 1): batch 3 tiles for fuel efficiency.
        int tileTarget = getSmartMemory().isShortLifetime() ? 1 : 3;

        // ── PRIORITY 0: TILE HUNTING ──────────────────────────────────────────
        // While below capacity, bypass the base agent's deliver/seek balance and
        // commit fully to collecting tiles.
        if (carried < tileTarget) {
            Int2D tile = getSmartMemory().getClosestTilePosition();

            // Skip if claimed by a teammate or flagged as expiring
            if (tile != null
                    && !getSmartMemory().isClaimed(tile.x, tile.y)
                    && !isExpiring(tile)) {

                // Claim immediately so teammates skip this tile
                getSmartMemory().addClaim(
                        tile.x, tile.y,
                        getEnvironment().schedule.getTime());

                TWDirection dir = navigateTo(tile.x, tile.y, "tile");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }

            // No viable tile in memory — explore toward nearest hotspot if known,
            // otherwise fall back to the base lawnmower sweep.
            TWDirection dir = exploreTowardHotspot();
            if (dir != null) {
                return new TWThought(TWAction.MOVE, dir);
            }
            dir = exploreGreedy();
            if (dir != null) {
                return new TWThought(TWAction.MOVE, dir);
            }
        }

        // ── PRIORITY 1: DELIVERY ─────────────────────────────────────────────
        // Reached target — find the nearest unclaimed, non-expiring hole.
        if (carried >= tileTarget) {
            Int2D hole = getSmartMemory().getClosestHolePosition();

            if (hole != null
                    && !getSmartMemory().isClaimed(hole.x, hole.y)
                    && !isExpiring(hole)) {

                // Claim the hole immediately so teammates skip it (mirrors tile claiming)
                getSmartMemory().addClaim(
                        hole.x, hole.y,
                        getEnvironment().schedule.getTime());

                TWDirection dir = navigateTo(hole.x, hole.y, "hole");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
            // No viable hole found — fall through to base agent which will
            // explore until a hole appears in memory.
        }

        // ── PRIORITY 2: BASE FALLTHROUGH ─────────────────────────────────────
        // SmartTWAgent handles: fuel emergency, refuel, opportunistic pickup/drop,
        // opportunistic refuel near station, standard planner, explore, wait.
        return super.think();
    }

    @Override
    public void communicate() {
        // Base class broadcasts: FUEL, TILE, HOLE, GONE, CLAIM.
        // TileHunterAgent has no additional messages to send —
        // aggressive claiming via addClaim() already reduces team-wide conflict.
        super.communicate();
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

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

    /**
     * When no tile is in memory, navigate toward the nearest known hotspot.
     * Hotspots are tile-dense regions broadcast by DeliveryOptimizerAgent.
     * Returns null if no hotspots are known, so caller falls back to exploreGreedy().
     */
    private TWDirection exploreTowardHotspot() {
        if (hotspots.isEmpty()) return null;

        HotspotEntry nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (HotspotEntry entry : hotspots) {
            double dist = Math.abs(entry.position.x - getX())
                        + Math.abs(entry.position.y - getY());
            if (dist < bestDist) {
                bestDist = dist;
                nearest = entry;
            }
        }

        return (nearest != null)
                ? navigateTo(nearest.position.x, nearest.position.y, "hotspot")
                : null;
    }
}