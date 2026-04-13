package tileworld.agent;

import java.util.List;

import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;

/**
 * HoleFillerAgent — Hole-centric delivery specialist.
 *
 * Improvements over base SmartTWAgent:
 *  1. Expiring-target avoidance — voids plans to targets flagged as expiring
 *     or that will likely expire before arrival (tighter thresholds).
 *  2. EXPIRING broadcasts — warns teammates about soon-to-expire targets.
 *  3. Periodic hole rebroadcasting — re-shares known hole positions.
 *  4. Reachability checks — tighter arrival-time projections.
 */
public class HoleFillerAgent extends SmartTWAgent {

    private Int2D lastBroadcastExpiring = null;

    public HoleFillerAgent(String name, int xpos, int ypos, TWEnvironment env,
                           double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        TWThought fuelSafety = fuelSafetyOverride();
        if (fuelSafety != null) {
            return fuelSafety;
        }

        TWThought opp = opportunisticAction();
        if (opp != null) {
            return opp;
        }

        // Only intercept when planner has an active plan to validate
        if (getPlanner().hasPlan()) {
            Int2D goal = getPlanner().getCurrentGoal();
            if (goal != null) {
                // Reset broadcast flag when switching targets
                if (lastBroadcastExpiring != null
                        && (lastBroadcastExpiring.x != goal.x || lastBroadcastExpiring.y != goal.y)) {
                    lastBroadcastExpiring = null;
                }

                // CHECK 1: Teammate flagged this as expiring → void and replan
                for (Int2D exp : expiringTargets) {
                    if (exp.x == goal.x && exp.y == goal.y) {
                        getPlanner().voidPlan();
                        return super.think();
                    }
                }

                // CHECK 2: Will target expire before arrival? (tighter thresholds)
                SmartTWAgentMemory mem = getSmartMemory();
                double now = getEnvironment().schedule.getTime();
                double obsTime = mem.getObservationTime(goal.x, goal.y);
                if (obsTime >= 0) {
                    double remaining = Parameters.lifeTime - (now - obsTime);
                    int dist = Math.abs(getX() - goal.x) + Math.abs(getY() - goal.y);
                    double threshold = mem.isShortLifetime() ? 0.65 : 0.85;

                    if (dist > remaining * threshold) {
                        // Broadcast EXPIRING so teammates avoid it too
                        if (lastBroadcastExpiring == null
                                || lastBroadcastExpiring.x != goal.x
                                || lastBroadcastExpiring.y != goal.y) {
                            getEnvironment().receiveMessage(
                                new Message(getName(), "",
                                    "EXPIRING:" + goal.x + "," + goal.y + ",lifetime"));
                            lastBroadcastExpiring = new Int2D(goal.x, goal.y);
                        }
                        getPlanner().voidPlan();
                        return super.think();
                    }
                }
            }
        }

        return super.think();
    }

    @Override
    public void communicate() {
        super.communicate();

        // Periodically rebroadcast all known holes to improve team awareness.
        double now = getEnvironment().schedule.getTime();
        int interval = getSmartMemory().isShortLifetime() ? 10 : 15;
        double freshness = getSmartMemory().isShortLifetime() ? 0.5 : 0.7;
        if (((int) now) % interval == 0) {
            List<Int2D> holes = getSmartMemory().getAllHolePositions();
            for (Int2D h : holes) {
                double obsTime = getSmartMemory().getObservationTime(h.x, h.y);
                if (obsTime >= 0 && (now - obsTime) < Parameters.lifeTime * freshness) {
                    getEnvironment().receiveMessage(
                        new Message(getName(), "", "HOLE:" + h.x + "," + h.y + "," + obsTime));
                }
            }
        }
    }
}
