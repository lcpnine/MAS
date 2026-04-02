package tileworld.agent;

import sim.util.Int2D;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEnvironment;

/**
 * FuelScoutAgent — Aggressive fuel station discovery
 *
 * Specialization: Lower fuel threshold (175/250 adaptive), wider search radius (20), LOW broadcasts
 */
public class FuelScoutAgent extends SmartTWAgent {

    public FuelScoutAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
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
