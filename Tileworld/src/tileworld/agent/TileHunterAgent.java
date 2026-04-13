package tileworld.agent;

import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;

/**
 * TileHunterAgent — Aggressive tile collection specialist.
 *
 * Behavioural focus: Suppresses hole-seeking until a tile capacity target is
 * reached, then delivers. This batching strategy reduces the number of
 * tile→hole trips, making each fuel unit more productive.
 *
 * Key behaviours:
 *  1. Adaptive tile target   — batch 3 in stable envs, deliver immediately in
 *                              volatile envs (short lifetime / Config 2)
 *  2. Aggressive claiming    — claims the target tile immediately to reduce
 *                              conflict with other agents
 *  3. Expiring-target skip   — skips tiles and holes flagged as expiring via
 *                              EXPIRING broadcasts (parsed by base class)
 *  4. Hotspot-biased explore — when no tile is known, moves toward the nearest
 *                              HOTSPOT broadcast rather than random sweeping
 *  5. Reachability check     — skips tiles that will likely expire before arrival
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

        // Free pickup/drop at current cell — critical when specialist logic
        // would otherwise return MOVE and skip the base class's priority 3-4.
        TWThought opp = opportunisticAction();
        if (opp != null) {
            return opp;
        }

        int carried = getCarriedTileCount();

        // ADAPTIVE TILE TARGET
        // Short-lifetime envs (Config 2): objects expire in ~30 steps, deliver at 1.
        // Long-lifetime envs (Config 1): batch 3 tiles for fuel efficiency.
        int tileTarget = getSmartMemory().isShortLifetime() ? 1 : 3;

        // ── PRIORITY 0: TILE HUNTING ──────────────────────────────────────────
        if (carried < tileTarget) {
            Int2D tile = getSmartMemory().getClosestTilePosition();

            if (tile != null
                    && !getSmartMemory().isClaimed(tile.x, tile.y)
                    && !isExpiring(tile)
                    && isReachableBeforeExpiry(tile)) {

                getSmartMemory().addClaim(
                        tile.x, tile.y,
                        getEnvironment().schedule.getTime());

                TWDirection dir = navigateTo(tile.x, tile.y, "tile");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }

            // No viable tile — explore toward nearest hotspot or sweep
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
        if (carried >= tileTarget) {
            Int2D hole = getSmartMemory().getClosestHolePosition();

            if (hole != null
                    && !getSmartMemory().isClaimed(hole.x, hole.y)
                    && !isExpiring(hole)
                    && isReachableBeforeExpiry(hole)) {

                getSmartMemory().addClaim(
                        hole.x, hole.y,
                        getEnvironment().schedule.getTime());

                TWDirection dir = navigateTo(hole.x, hole.y, "hole");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // ── PRIORITY 2: BASE FALLTHROUGH ─────────────────────────────────────
        return super.think();
    }

    @Override
    public void communicate() {
        super.communicate();
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private boolean isReachableBeforeExpiry(Int2D target) {
        double now = getEnvironment().schedule.getTime();
        double obsTime = getSmartMemory().getObservationTime(target.x, target.y);
        if (obsTime < 0) return true;
        double age = now - obsTime;
        double remaining = Parameters.lifeTime - age;
        int stepsToArrival = Math.abs(getX() - target.x) + Math.abs(getY() - target.y);
        double threshold = getSmartMemory().isShortLifetime() ? 0.7 : 0.85;
        return stepsToArrival <= remaining * threshold;
    }

    private boolean isExpiring(Int2D pos) {
        for (Int2D exp : expiringTargets) {
            if (exp.x == pos.x && exp.y == pos.y) return true;
        }
        return false;
    }

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