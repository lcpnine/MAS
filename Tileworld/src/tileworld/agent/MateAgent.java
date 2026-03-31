package tileworld.agent;

import tileworld.environment.TWEnvironment;

public class MateAgent extends SmartTWAgent {
    public MateAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(name, xpos, ypos, env, fuelLevel, 3);
    }
}
