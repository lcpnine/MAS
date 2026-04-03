package tileworld.agent;

import sim.util.Int2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;

/**
 * ExplorerAgent — Systematic zone coverage
 *
 * Specialization: Zone-based exploration (6 strips), hotspot bias, SWAP broadcasts when zone done
 */
public class ExplorerAgent extends SmartTWAgent {

    public ExplorerAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
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
