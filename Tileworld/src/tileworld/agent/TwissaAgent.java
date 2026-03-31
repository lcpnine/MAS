package tileworld.agent;

import tileworld.environment.TWEnvironment;

public class TwissaAgent extends SmartTWAgent {
    public TwissaAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(name, xpos, ypos, env, fuelLevel, 4);
    }
}
