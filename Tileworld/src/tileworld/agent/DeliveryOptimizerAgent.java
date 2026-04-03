package tileworld.agent;

import sim.util.Int2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;

/**
 * DeliveryOptimizerAgent — Cluster-density routing
 *
 * Specialization: Routes to holes near high-density hotspots, broadcasts HOTSPOT messages
 */
public class DeliveryOptimizerAgent extends SmartTWAgent {

    public DeliveryOptimizerAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        return super.think();
    }

    @Override
    public void communicate() {
        super.communicate();
    }
}
