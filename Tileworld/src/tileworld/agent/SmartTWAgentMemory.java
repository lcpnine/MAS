package tileworld.agent;

import java.util.ArrayList;
import java.util.List;
import sim.engine.Schedule;
import sim.util.Bag;
import sim.util.Int2D;
import sim.util.IntBag;
import tileworld.Parameters;
import tileworld.environment.TWEntity;
import tileworld.environment.TWFuelStation;
import tileworld.environment.TWHole;
import tileworld.environment.TWTile;
import tileworld.environment.TWObstacle;
import tileworld.environment.TWObject;

/**
 * Enhanced memory system for SmartTWAgent.
 *
 * Extends the base working memory with:
 * - Fuel station tracking (TWFuelStation is not a TWObject, so the parent skips it)
 * - Parallel state arrays (parent's objects[][] is private)
 * - Time-based memory decay aligned with object lifetime
 * - Exploration tracking via lastVisitedTime grid
 */
public class SmartTWAgentMemory extends TWAgentWorkingMemory {

    private final TWAgent me;
    private final Schedule schedule;
    private final int xDim;
    private final int yDim;

    // Fuel station location — once found, never forgotten
    private Int2D fuelStationPos = null;

    // Parallel tracking arrays (parent's objects[][] is private)
    private final TWEntity[][] rememberedEntities;
    private final double[][] observationTimes;

    // Exploration tracking — when did we last visit/see each cell
    private final double[][] lastVisitedTime;

    public SmartTWAgentMemory(TWAgent agent, Schedule schedule, int x, int y) {
        super(agent, schedule, x, y);
        this.me = agent;
        this.schedule = schedule;
        this.xDim = x;
        this.yDim = y;

        this.rememberedEntities = new TWEntity[x][y];
        this.observationTimes = new double[x][y];
        this.lastVisitedTime = new double[x][y];

        // Initialize all observation times to -1 (never seen)
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                observationTimes[i][j] = -1;
                lastVisitedTime[i][j] = -1;
            }
        }
    }

    /**
     * Override to intercept fuel station sightings and maintain parallel arrays.
     * TWFuelStation extends TWEntity but NOT TWObject, so parent's updateMemory skips it.
     */
    @Override
    public void updateMemory(Bag sensedObjects, IntBag objectXCoords, IntBag objectYCoords,
                             Bag sensedAgents, IntBag agentXCoords, IntBag agentYCoords) {

        double now = schedule.getTime();

        // Mark sensor range cells as visited
        int sensorRange = Parameters.defaultSensorRange;
        for (int dx = -sensorRange; dx <= sensorRange; dx++) {
            for (int dy = -sensorRange; dy <= sensorRange; dy++) {
                int cx = me.getX() + dx;
                int cy = me.getY() + dy;
                if (isInBounds(cx, cy)) {
                    lastVisitedTime[cx][cy] = now;
                }
            }
        }

        // Scan sensed objects for fuel stations and update parallel arrays
        for (int i = 0; i < sensedObjects.size(); i++) {
            Object obj = sensedObjects.get(i);
            if (obj instanceof TWFuelStation) {
                TWFuelStation fs = (TWFuelStation) obj;
                fuelStationPos = new Int2D(fs.getX(), fs.getY());
            }
            if (obj instanceof TWObject) {
                TWEntity e = (TWEntity) obj;
                rememberedEntities[e.getX()][e.getY()] = e;
                observationTimes[e.getX()][e.getY()] = now;
            }
        }

        // Also clear parallel arrays for cells in sensor range that are now empty
        for (int dx = -sensorRange; dx <= sensorRange; dx++) {
            for (int dy = -sensorRange; dy <= sensorRange; dy++) {
                int cx = me.getX() + dx;
                int cy = me.getY() + dy;
                if (isInBounds(cx, cy) && rememberedEntities[cx][cy] != null) {
                    // Check if the object is still in sensedObjects at this position
                    boolean stillThere = false;
                    for (int i = 0; i < sensedObjects.size(); i++) {
                        Object obj = sensedObjects.get(i);
                        if (obj instanceof TWEntity) {
                            TWEntity e = (TWEntity) obj;
                            if (e.getX() == cx && e.getY() == cy) {
                                stillThere = true;
                                break;
                            }
                        }
                    }
                    if (!stillThere) {
                        rememberedEntities[cx][cy] = null;
                        observationTimes[cx][cy] = -1;
                    }
                }
            }
        }

        // Call parent to handle standard memory updates
        super.updateMemory(sensedObjects, objectXCoords, objectYCoords,
                          sensedAgents, agentXCoords, agentYCoords);

        // Decay old memories
        decayMemory();
    }

    /**
     * Remove memories older than the configured object lifetime.
     */
    @Override
    public void decayMemory() {
        double now = schedule.getTime();
        int lifetime = Parameters.lifeTime;

        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                if (rememberedEntities[x][y] != null && observationTimes[x][y] >= 0) {
                    double age = now - observationTimes[x][y];
                    if (age >= lifetime) {
                        rememberedEntities[x][y] = null;
                        observationTimes[x][y] = -1;
                        // Sync with parent's state
                        removeAgentPercept(x, y);
                    }
                }
            }
        }
    }

    // ---- Fuel station ----

    public Int2D getKnownFuelStation() {
        return fuelStationPos;
    }

    public boolean isFuelStationKnown() {
        return fuelStationPos != null;
    }

    // ---- Query remembered objects ----

    public List<TWTile> getAllRememberedTiles() {
        List<TWTile> tiles = new ArrayList<TWTile>();
        double now = schedule.getTime();
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                if (rememberedEntities[x][y] instanceof TWTile) {
                    double age = now - observationTimes[x][y];
                    if (age < Parameters.lifeTime) {
                        tiles.add((TWTile) rememberedEntities[x][y]);
                    }
                }
            }
        }
        return tiles;
    }

    public List<TWHole> getAllRememberedHoles() {
        List<TWHole> holes = new ArrayList<TWHole>();
        double now = schedule.getTime();
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                if (rememberedEntities[x][y] instanceof TWHole) {
                    double age = now - observationTimes[x][y];
                    if (age < Parameters.lifeTime) {
                        holes.add((TWHole) rememberedEntities[x][y]);
                    }
                }
            }
        }
        return holes;
    }

    /**
     * Get the closest remembered tile to the agent.
     */
    public TWTile getClosestRememberedTile() {
        List<TWTile> tiles = getAllRememberedTiles();
        TWTile closest = null;
        double minDist = Double.MAX_VALUE;
        for (TWTile t : tiles) {
            double d = me.getDistanceTo(t);
            if (d < minDist) {
                minDist = d;
                closest = t;
            }
        }
        return closest;
    }

    /**
     * Get the closest remembered hole to a given position.
     */
    public TWHole getClosestRememberedHole(int fromX, int fromY) {
        List<TWHole> holes = getAllRememberedHoles();
        TWHole closest = null;
        double minDist = Double.MAX_VALUE;
        for (TWHole h : holes) {
            double d = Math.abs(h.getX() - fromX) + Math.abs(h.getY() - fromY);
            if (d < minDist) {
                minDist = d;
                closest = h;
            }
        }
        return closest;
    }

    /**
     * Get the closest remembered hole to the agent.
     */
    public TWHole getClosestRememberedHole() {
        return getClosestRememberedHole(me.getX(), me.getY());
    }

    // ---- Exploration ----

    /**
     * Find the direction toward the least-recently-visited area within a search radius.
     * Evaluates each cardinal direction by averaging lastVisitedTime in a cone.
     */
    public Int2D getLeastVisitedTarget(int searchRadius) {
        double now = schedule.getTime();
        int bestX = me.getX();
        int bestY = me.getY();
        double bestScore = Double.MAX_VALUE; // lower = older = more interesting

        for (int dx = -searchRadius; dx <= searchRadius; dx += 3) {
            for (int dy = -searchRadius; dy <= searchRadius; dy += 3) {
                int tx = me.getX() + dx;
                int ty = me.getY() + dy;
                if (!isInBounds(tx, ty) || (dx == 0 && dy == 0)) continue;

                double visitTime = lastVisitedTime[tx][ty];
                double score = (visitTime < 0) ? -1 : visitTime; // never visited = lowest score

                if (score < bestScore) {
                    bestScore = score;
                    bestX = tx;
                    bestY = ty;
                }
            }
        }
        return new Int2D(bestX, bestY);
    }

    /**
     * Get the entity at a remembered position (may be stale).
     */
    public TWEntity getRememberedEntity(int x, int y) {
        if (isInBounds(x, y)) {
            return rememberedEntities[x][y];
        }
        return null;
    }

    /**
     * Get the object at the agent's current position from environment's object grid.
     */
    public TWEntity getObjectAtCurrentPos() {
        Object obj = me.getEnvironment().getObjectGrid().get(me.getX(), me.getY());
        if (obj instanceof TWEntity) {
            return (TWEntity) obj;
        }
        return null;
    }

    private boolean isInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < xDim && y < yDim;
    }

    public int getXDim() { return xDim; }
    public int getYDim() { return yDim; }
}
