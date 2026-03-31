package tileworld.agent;

import tileworld.environment.TWEnvironment;

public class NikAgent extends SmartTWAgent {
    public NikAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(name, xpos, ypos, env, fuelLevel, 5);
    }
}
