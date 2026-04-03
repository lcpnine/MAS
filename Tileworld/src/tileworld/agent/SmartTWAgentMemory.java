package tileworld.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
<<<<<<< HEAD
=======

>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
import sim.engine.Schedule;
import sim.util.Bag;
import sim.util.Int2D;
import sim.util.IntBag;
import tileworld.Parameters;
import tileworld.environment.TWEntity;
import tileworld.environment.TWFuelStation;
import tileworld.environment.TWHole;
<<<<<<< HEAD
import tileworld.environment.TWTile;
import tileworld.environment.TWObstacle;
import tileworld.environment.TWObject;
=======
import tileworld.environment.TWObject;
import tileworld.environment.TWTile;
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611

/**
 * Enhanced memory system for SmartTWAgent.
 *
 * Extends the base working memory with:
<<<<<<< HEAD
 * - Fuel station tracking (TWFuelStation is not a TWObject, so the parent skips it)
=======
 * - Fuel station tracking (TWFuelStation is not a TWObject, so the parent skips
 * it)
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
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

    // Shared entity tracking (from communication, can't create real TWTile/TWHole)
    // 0 = none, 1 = tile, 2 = hole
    private final int[][] sharedEntityType;

    // Claimed targets by other agents: key="x,y", value=claim time
    private final HashMap<String, Double> claimedTargets = new HashMap<String, Double>();

    // New discoveries this step (for broadcasting in communicate())
    private final ArrayList<Int2D> newTiles = new ArrayList<Int2D>();
    private final ArrayList<Int2D> newHoles = new ArrayList<Int2D>();
    private final ArrayList<Int2D> goneEntities = new ArrayList<Int2D>();

    // Exploration tracking — when did we last visit/see each cell
    private final double[][] lastVisitedTime;

<<<<<<< HEAD
=======
    // Runtime observation counters for environment classification
    private int totalObjectsSensed = 0;
    private int senseStepCount = 0;
    private int objectDisappearances = 0;
    private long totalObservedLifetime = 0;

>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
    public SmartTWAgentMemory(TWAgent agent, Schedule schedule, int x, int y) {
        super(agent, schedule, x, y);
        this.me = agent;
        this.schedule = schedule;
        this.xDim = x;
        this.yDim = y;

        this.rememberedEntities = new TWEntity[x][y];
        this.observationTimes = new double[x][y];
        this.sharedEntityType = new int[x][y];
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
<<<<<<< HEAD
     * TWFuelStation extends TWEntity but NOT TWObject, so parent's updateMemory skips it.
     */
    @Override
    public void updateMemory(Bag sensedObjects, IntBag objectXCoords, IntBag objectYCoords,
                             Bag sensedAgents, IntBag agentXCoords, IntBag agentYCoords) {

        double now = schedule.getTime();

=======
     * TWFuelStation extends TWEntity but NOT TWObject, so parent's updateMemory
     * skips it.
     */
    @Override
    public void updateMemory(Bag sensedObjects, IntBag objectXCoords, IntBag objectYCoords,
            Bag sensedAgents, IntBag agentXCoords, IntBag agentYCoords) {

        double now = schedule.getTime();

        // Observation counting for runtime environment classification
        senseStepCount++;
        int objectCount = 0;
        for (int i = 0; i < sensedObjects.size(); i++) {
            if (sensedObjects.get(i) instanceof TWObject)
                objectCount++;
        }
        totalObjectsSensed += objectCount;

>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
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

        // Clear discovery lists for this step
        newTiles.clear();
        newHoles.clear();
        goneEntities.clear();

        // Scan sensed objects for fuel stations and update parallel arrays
        for (int i = 0; i < sensedObjects.size(); i++) {
            Object obj = sensedObjects.get(i);
            if (obj instanceof TWFuelStation) {
                TWFuelStation fs = (TWFuelStation) obj;
                fuelStationPos = new Int2D(fs.getX(), fs.getY());
            }
            if (obj instanceof TWObject) {
                TWEntity e = (TWEntity) obj;
                // Track new discoveries for broadcasting
                if (rememberedEntities[e.getX()][e.getY()] == null
                        && sharedEntityType[e.getX()][e.getY()] == 0) {
                    if (e instanceof TWTile) {
                        newTiles.add(new Int2D(e.getX(), e.getY()));
                    } else if (e instanceof TWHole) {
                        newHoles.add(new Int2D(e.getX(), e.getY()));
                    }
                }
                rememberedEntities[e.getX()][e.getY()] = e;
                sharedEntityType[e.getX()][e.getY()] = 0; // direct observation supersedes shared
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
<<<<<<< HEAD
=======
                        // Track observed lifetime for environment classification
                        if (observationTimes[cx][cy] > 0) {
                            double observedLife = now - observationTimes[cx][cy];
                            totalObservedLifetime += (long) observedLife;
                            objectDisappearances++;
                        }
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
                        goneEntities.add(new Int2D(cx, cy));
                        rememberedEntities[cx][cy] = null;
                        sharedEntityType[cx][cy] = 0;
                        observationTimes[cx][cy] = -1;
                    }
                }
            }
        }

        // Call parent to handle standard memory updates
        super.updateMemory(sensedObjects, objectXCoords, objectYCoords,
<<<<<<< HEAD
                          sensedAgents, agentXCoords, agentYCoords);
=======
                sensedAgents, agentXCoords, agentYCoords);
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611

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
                if (observationTimes[x][y] >= 0) {
                    double age = now - observationTimes[x][y];
                    if (age >= lifetime) {
                        if (rememberedEntities[x][y] != null) {
                            rememberedEntities[x][y] = null;
                            removeAgentPercept(x, y);
                        }
                        sharedEntityType[x][y] = 0;
                        observationTimes[x][y] = -1;
                    }
                }
            }
        }

        // Decay old claims
        Iterator<Map.Entry<String, Double>> it = claimedTargets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Double> entry = it.next();
            if (now - entry.getValue() >= lifetime) {
                it.remove();
<<<<<<< HEAD
=======
            } else {
                // Remove stale claims where the object no longer exists in memory
                String[] parts = entry.getKey().split(",");
                int cx = Integer.parseInt(parts[0]);
                int cy = Integer.parseInt(parts[1]);
                if (isInBounds(cx, cy)
                        && rememberedEntities[cx][cy] == null
                        && sharedEntityType[cx][cy] == 0) {
                    it.remove();
                }
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
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
<<<<<<< HEAD
     * Find the direction toward the least-recently-visited area within a search radius.
=======
     * Find the direction toward the least-recently-visited area within a search
     * radius.
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
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
<<<<<<< HEAD
                if (!isInBounds(tx, ty) || (dx == 0 && dy == 0)) continue;
=======
                if (!isInBounds(tx, ty) || (dx == 0 && dy == 0))
                    continue;
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611

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
<<<<<<< HEAD
     * Get the object at the agent's current position from environment's object grid.
=======
     * Get the object at the agent's current position from environment's object
     * grid.
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
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

<<<<<<< HEAD
    public int getXDim() { return xDim; }
    public int getYDim() { return yDim; }
=======
    public int getXDim() {
        return xDim;
    }

    public int getYDim() {
        return yDim;
    }
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611

    // ---- Communication support ----

    public void setFuelStation(int x, int y) {
        if (fuelStationPos == null) {
            fuelStationPos = new Int2D(x, y);
        }
    }

    public void addSharedTile(int x, int y, double time) {
<<<<<<< HEAD
        if (!isInBounds(x, y)) return;
=======
        if (!isInBounds(x, y))
            return;
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
        // Only update if we don't already have a direct observation here
        if (rememberedEntities[x][y] == null) {
            sharedEntityType[x][y] = 1;
            // Only update time if newer
            if (time > observationTimes[x][y]) {
                observationTimes[x][y] = time;
            }
        }
    }

    public void addSharedHole(int x, int y, double time) {
<<<<<<< HEAD
        if (!isInBounds(x, y)) return;
=======
        if (!isInBounds(x, y))
            return;
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
        if (rememberedEntities[x][y] == null) {
            sharedEntityType[x][y] = 2;
            if (time > observationTimes[x][y]) {
                observationTimes[x][y] = time;
            }
        }
    }

    public void removeSharedEntity(int x, int y) {
<<<<<<< HEAD
        if (!isInBounds(x, y)) return;
=======
        if (!isInBounds(x, y))
            return;
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
        rememberedEntities[x][y] = null;
        sharedEntityType[x][y] = 0;
        observationTimes[x][y] = -1;
        removeAgentPercept(x, y);
    }

    // ---- Claim tracking ----

    public void addClaim(int x, int y, double time) {
        claimedTargets.put(x + "," + y, time);
    }

    public void clearAllClaims() {
        claimedTargets.clear();
    }

    public boolean isClaimed(int x, int y) {
        return claimedTargets.containsKey(x + "," + y);
    }

    // ---- Position-based queries (combines sensed + shared) ----

    public List<Int2D> getAllTilePositions() {
        List<Int2D> tiles = new ArrayList<Int2D>();
        double now = schedule.getTime();
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
<<<<<<< HEAD
                if (observationTimes[x][y] < 0) continue;
                double age = now - observationTimes[x][y];
                if (age >= Parameters.lifeTime) continue;
=======
                if (observationTimes[x][y] < 0)
                    continue;
                double age = now - observationTimes[x][y];
                if (age >= Parameters.lifeTime)
                    continue;
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
                if (rememberedEntities[x][y] instanceof TWTile || sharedEntityType[x][y] == 1) {
                    tiles.add(new Int2D(x, y));
                }
            }
        }
        return tiles;
    }

    public List<Int2D> getAllHolePositions() {
        List<Int2D> holes = new ArrayList<Int2D>();
        double now = schedule.getTime();
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
<<<<<<< HEAD
                if (observationTimes[x][y] < 0) continue;
                double age = now - observationTimes[x][y];
                if (age >= Parameters.lifeTime) continue;
=======
                if (observationTimes[x][y] < 0)
                    continue;
                double age = now - observationTimes[x][y];
                if (age >= Parameters.lifeTime)
                    continue;
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
                if (rememberedEntities[x][y] instanceof TWHole || sharedEntityType[x][y] == 2) {
                    holes.add(new Int2D(x, y));
                }
            }
        }
        return holes;
    }

    public Int2D getClosestTilePosition() {
        List<Int2D> tiles = getAllTilePositions();
        Int2D closest = null;
        double minDist = Double.MAX_VALUE;
        for (Int2D t : tiles) {
            double d = Math.abs(t.x - me.getX()) + Math.abs(t.y - me.getY());
            if (d < minDist) {
                minDist = d;
                closest = t;
            }
        }
        return closest;
    }

    public Int2D getClosestHolePosition() {
        return getClosestHolePosition(me.getX(), me.getY());
    }

    public Int2D getClosestHolePosition(int fromX, int fromY) {
        List<Int2D> holes = getAllHolePositions();
        Int2D closest = null;
        double minDist = Double.MAX_VALUE;
        for (Int2D h : holes) {
            double d = Math.abs(h.x - fromX) + Math.abs(h.y - fromY);
            if (d < minDist) {
                minDist = d;
                closest = h;
            }
        }
        return closest;
    }

    // ---- Discovery lists for communication ----

<<<<<<< HEAD
    public ArrayList<Int2D> getNewTiles() { return newTiles; }
    public ArrayList<Int2D> getNewHoles() { return newHoles; }
    public ArrayList<Int2D> getGoneEntities() { return goneEntities; }
=======
    public ArrayList<Int2D> getNewTiles() {
        return newTiles;
    }

    public ArrayList<Int2D> getNewHoles() {
        return newHoles;
    }

    public ArrayList<Int2D> getGoneEntities() {
        return goneEntities;
    }
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611

    public void removeObject(TWEntity entity) {
        if (entity != null && isInBounds(entity.getX(), entity.getY())) {
            rememberedEntities[entity.getX()][entity.getY()] = null;
            sharedEntityType[entity.getX()][entity.getY()] = 0;
            observationTimes[entity.getX()][entity.getY()] = -1;
        }
    }

<<<<<<< HEAD
    // ---- Environment profile (runtime detection for Config 3 adaptability) ----

    public boolean isDense() {
        return Parameters.tileMean >= 1.0;
    }

    public boolean isShortLifetime() {
        return Parameters.lifeTime <= 50;
    }

    public boolean isLargeGrid() {
        return Parameters.xDimension >= 70;
=======
    // ---- Observation time access ----

    public double getObservationTime(int x, int y) {
        if (isInBounds(x, y)) {
            return observationTimes[x][y];
        }
        return -1;
    }

    // ---- Environment profile (runtime observation-based detection) ----

    public boolean isDense() {
        if (senseStepCount < 30)
            return false; // warmup: assume sparse
        double avgObjectsPerStep = (double) totalObjectsSensed / senseStepCount;
        // Normalize by grid coverage: sensor sees (2*range+1)^2 cells out of xDim*yDim
        int sensorSide = 2 * Parameters.defaultSensorRange + 1;
        double sensorArea = sensorSide * sensorSide;
        double gridArea = xDim * yDim;
        double normalizedDensity = avgObjectsPerStep * (gridArea / sensorArea);
        return normalizedDensity > 50.0;
    }

    public boolean isShortLifetime() {
        if (objectDisappearances < 2)
            return false; // warmup: assume long lifetime
        double avgLifetime = (double) totalObservedLifetime / objectDisappearances;
        return avgLifetime < 50;
    }

    public boolean isLargeGrid() {
        return xDim >= 70; // xDim from env.getxDimension(), already correct
    }

    /**
     * Estimated object lifetime from observations, or Parameters.lifeTime as
     * fallback.
     */
    public int getEstimatedLifetime() {
        if (objectDisappearances >= 5) {
            return (int) ((double) totalObservedLifetime / objectDisappearances);
        }
        return Parameters.lifeTime;
>>>>>>> f571aae19ae0809e71e7bad4a3916fc45e46e611
    }
}
