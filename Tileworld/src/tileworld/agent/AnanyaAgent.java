package tileworld.agent;

import tileworld.environment.TWEnvironment;

public class AnanyaAgent extends SmartTWAgent {
    public AnanyaAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(name, xpos, ypos, env, fuelLevel, 2);
    }
}
