package tileworld.agent;

import sim.util.Int2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;
import tileworld.Parameters;

/**
 * SmarterReplanningAgent — Predictive replanning to avoid wasted trips
 *
 * Checks three things before pursuing a goal:
 * 1. Did someone else say this target is expiring? Agent avoids it.
 * 2. Will it expire before agent arrives? Agent broadcasts and switches.
 * 3. Will agent run out of fuel doing the round trip? Agent goes to refuel.
 */
public class SmarterReplanningAgent extends SmartTWAgent {

    private Int2D lastBroadcastExpiring = null;

    public SmarterReplanningAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        if (!getPlanner().hasPlan()) {
            return super.think();
        }

        Int2D currentGoal = getPlanner().getCurrentGoal();
        if (currentGoal == null) {
            return super.think();
        }

        // Reset broadcast flag when switching targets
        if (lastBroadcastExpiring != null
                && (lastBroadcastExpiring.x != currentGoal.x || lastBroadcastExpiring.y != currentGoal.y)) {
            lastBroadcastExpiring = null;
        }

        // 1. Others marked this as expiring? Skip it.
        boolean isExpiring = false;
        for (Int2D exp : expiringTargets) {
            if (exp.x == currentGoal.x && exp.y == currentGoal.y) {
                isExpiring = true;
                break;
            }
        }
        if (isExpiring) {
            getPlanner().voidPlan();
            return super.think();
        }

        // 2. Will target expire before agent arrives?
        double currentTime = getEnvironment().schedule.getTime();
        double obsTime = getSmartMemory().getObservationTime(currentGoal.x, currentGoal.y);
        if (obsTime >= 0) {
            double age = currentTime - obsTime;
            double remainingLifetime = Parameters.lifeTime - age;
            int stepsToArrival = Math.abs(getX() - currentGoal.x) + Math.abs(getY() - currentGoal.y);

            // Tighter threshold for short-lived objects
            double expiryThreshold = getSmartMemory().isShortLifetime() ? 0.7 : 0.9;
            if (stepsToArrival > remainingLifetime * expiryThreshold) {
                // Broadcast once so teammates avoid it too
                if (lastBroadcastExpiring == null
                        || lastBroadcastExpiring.x != currentGoal.x
                        || lastBroadcastExpiring.y != currentGoal.y) {
                    getEnvironment().receiveMessage(
                        new Message(getName(), "", "EXPIRING:" + currentGoal.x + "," + currentGoal.y + ",lifetime")
                    );
                    lastBroadcastExpiring = new Int2D(currentGoal.x, currentGoal.y);
                }

                getPlanner().voidPlan();
                return super.think();
            }
        }

        // 3. Can agent afford the round trip to goal + back to fuel?
        Int2D fuelPos = getSmartMemory().getKnownFuelStation();
        if (fuelPos != null) {
            int stepsToGoal = Math.abs(getX() - currentGoal.x) + Math.abs(getY() - currentGoal.y);
            int stepsToFuel = Math.abs(currentGoal.x - fuelPos.x) + Math.abs(currentGoal.y - fuelPos.y);
            int safetyMargin = Math.max(50, getSmartMemory().getXDim() / 4);
            int totalCost = stepsToGoal + stepsToFuel + safetyMargin;

            if (getFuelLevel() < totalCost) {
                getPlanner().voidPlan();
                TWDirection dir = navigateTo(fuelPos.x, fuelPos.y, "fuel");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // All checks passed - execute the plan
        TWDirection planDir = getPlanner().execute();
        if (planDir != null) {
            return new TWThought(TWAction.MOVE, planDir);
        }

        return super.think();
    }

    @Override
    public void communicate() {
        super.communicate(); // Base handles parsing
        // EXPIRING broadcasts sent from think() to catch failures early
    }
}
