package tileworld.agent;

import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;

/**
 * FuelScoutAgent — v4 exact restore for benchmark comparison.
 */
public class FuelScoutAgent extends SmartTWAgent {

    private static final int MAX_EXPLORE_RADIUS = 20;
    private static final int MIN_EXPLORE_RADIUS = 5;
    private static final double SAFETY_BUFFER_FRACTION = 0.15;
    private static final int COMMIT_STEPS = 8;
    private static final int MAX_COMMIT_MULTIPLIER = 3;
    private static final double POST_DISCOVERY_REFUEL_THRESHOLD = 0.70;
    private static final int LOW_BROADCAST_INTERVAL = 10;

    private Int2D committedTarget = null;
    private int committedStepsRemaining = 0;
    private boolean centerVisited = false;
    private double lastLowBroadcastStep = -1;
    private boolean wasStationKnown = false;

    public FuelScoutAgent(String name, int xpos, int ypos, TWEnvironment env,
                          double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        boolean fuelKnown = getSmartMemory().isFuelStationKnown();
        boolean justDiscoveredStation = fuelKnown && !wasStationKnown;
        wasStationKnown = fuelKnown;

        int safetyBuffer = (int) (Parameters.defaultFuelLevel * SAFETY_BUFFER_FRACTION);

        if (!fuelKnown) {
            int fuelFloor = getSmartMemory().getXDim() + getSmartMemory().getYDim() + safetyBuffer;
            if (fuelLevel <= fuelFloor) {
                return new TWThought(TWAction.MOVE, TWDirection.Z);
            }

            double fuelFraction = fuelLevel / Parameters.defaultFuelLevel;
            int effectiveRadius = Math.max(MIN_EXPLORE_RADIUS,
                    (int) (MAX_EXPLORE_RADIUS * fuelFraction));

            if (!centerVisited) {
                int cx = getSmartMemory().getXDim() / 2;
                int cy = getSmartMemory().getYDim() / 2;
                Int2D center = new Int2D(cx, cy);
                if (isWithinSensorRange(center)) {
                    centerVisited = true;
                } else {
                    if (committedTarget == null || committedStepsRemaining <= 0
                            || isWithinSensorRange(committedTarget)) {
                        committedTarget = center;
                        committedStepsRemaining = commitLengthFor(center);
                    }
                    committedStepsRemaining--;
                    TWDirection dir = navigateTo(committedTarget.x, committedTarget.y, "scout");
                    if (dir != null) return new TWThought(TWAction.MOVE, dir);
                    centerVisited = true;
                }
            }

            if (committedTarget == null || committedStepsRemaining <= 0
                    || isWithinSensorRange(committedTarget)) {
                committedTarget = getSmartMemory().getLeastVisitedTarget(effectiveRadius);
                committedStepsRemaining = commitLengthFor(committedTarget);
            }
            committedStepsRemaining--;

            if (committedTarget != null
                    && (committedTarget.x != getX() || committedTarget.y != getY())) {
                TWDirection dir = navigateTo(committedTarget.x, committedTarget.y, "scout");
                if (dir != null) return new TWThought(TWAction.MOVE, dir);
                committedTarget = null;
            }

            TWDirection dir = exploreGreedy();
            if (dir != null) return new TWThought(TWAction.MOVE, dir);
            return new TWThought(TWAction.MOVE, TWDirection.Z);
        }

        boolean wantsRefuel = justDiscoveredStation
                || (fuelLevel < Parameters.defaultFuelLevel * POST_DISCOVERY_REFUEL_THRESHOLD
                        && !hasTile());
        if (wantsRefuel) {
            Int2D fp = getSmartMemory().getKnownFuelStation();
            if (fp != null && !(getX() == fp.x && getY() == fp.y)) {
                TWDirection dir = navigateTo(fp.x, fp.y, "fuel");
                if (dir != null) {
                    committedTarget = null;
                    committedStepsRemaining = 0;
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        committedTarget = null;
        committedStepsRemaining = 0;
        return super.think();
    }

    @Override
    public void communicate() {
        super.communicate();
        if (!getSmartMemory().isFuelStationKnown()) {
            int baseFloor = getSmartMemory().getXDim() + getSmartMemory().getYDim()
                    + (int) (Parameters.defaultFuelLevel * SAFETY_BUFFER_FRACTION);
            int lowWarningThreshold = (int) (baseFloor * 1.2);
            if (fuelLevel <= lowWarningThreshold) {
                double now = getEnvironment().schedule.getTime();
                if (now - lastLowBroadcastStep >= LOW_BROADCAST_INTERVAL) {
                    sendScoutMsg("LOW:" + getName() + "," + (int) fuelLevel);
                    lastLowBroadcastStep = now;
                }
            }
        }
    }

    private int commitLengthFor(Int2D target) {
        if (target == null) return COMMIT_STEPS;
        int dist = Math.abs(getX() - target.x) + Math.abs(getY() - target.y);
        return Math.min(COMMIT_STEPS * MAX_COMMIT_MULTIPLIER, Math.max(COMMIT_STEPS, dist));
    }

    private boolean isWithinSensorRange(Int2D target) {
        int dist = Math.abs(getX() - target.x) + Math.abs(getY() - target.y);
        return dist <= Parameters.defaultSensorRange;
    }

    private void sendScoutMsg(String content) {
        getEnvironment().receiveMessage(new Message(getName(), "", content));
    }
}
