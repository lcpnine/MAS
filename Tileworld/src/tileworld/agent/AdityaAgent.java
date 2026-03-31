package tileworld.agent;

import tileworld.environment.TWEnvironment;

public class AdityaAgent extends SmartTWAgent {
    public AdityaAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(name, xpos, ypos, env, fuelLevel, 0);
    }
}
