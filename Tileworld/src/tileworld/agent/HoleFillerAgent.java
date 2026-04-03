package tileworld.agent;

import sim.util.Int2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;

/**
 * HoleFillerAgent — Hole discovery and patrolling
 *
 * Specialization: Prioritizes holes, patrols near them (dense envs only), skips patrol in sparse envs
 */
public class HoleFillerAgent extends SmartTWAgent {

    public HoleFillerAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
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
