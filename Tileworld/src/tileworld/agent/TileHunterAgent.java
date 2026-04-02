package tileworld.agent;

import sim.util.Int2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;

/**
 * TileHunterAgent — Aggressive tile collection
 *
 * Specialization: Ignores holes until carrying target capacity (1/3 adaptive by lifetime), aggressive collection
 */
public class TileHunterAgent extends SmartTWAgent {

    public TileHunterAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
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
