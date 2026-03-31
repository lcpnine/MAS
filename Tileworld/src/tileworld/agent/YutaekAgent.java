package tileworld.agent;

import tileworld.environment.TWEnvironment;

public class YutaekAgent extends SmartTWAgent {
    public YutaekAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(name, xpos, ypos, env, fuelLevel, 1);
    }
}
