package tileworld.environment;

import sim.util.Int2D;
import tileworld.Parameters;
import tileworld.agent.SmartTWAgent;

/**
 * BaselineEnvironment
 *
 * Runs 6 plain SmartTWAgent instances (no specialization) for baseline testing.
 * Extends TWEnvironment and overrides only createAgents() — all environment
 * setup, object creation, and scheduling remain identical to the normal run.
 */
public class BaselineEnvironment extends TWEnvironment {

    public BaselineEnvironment(long seed) {
        super(seed);
    }

    @Override
    protected void createAgents() {
        for (int i = 0; i < 6; i++) {
            Int2D pos = this.generateRandomLocation();
            createAgent(new SmartTWAgent("Agent" + i, pos.getX(), pos.getY(), this, Parameters.defaultFuelLevel, i));
        }
    }
}
