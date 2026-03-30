# TileWorld Multi-Agent System - AI Handover Document

## Project Overview

NTU AI6125 group project implementing intelligent agents for TileWorld (MASON simulation).
Agents pick up tiles and deposit them in holes for score on a dynamic grid.

**Grading:** 40% report + 20% presentation/demo + 40% live competition (10 runs per config, averaged)

## Current State (Phase 1 Complete)

Phase 1 implements a solo-capable agent with fuel management, memory, and systematic exploration.

### Files Created

| File | Purpose |
|------|---------|
| `Tileworld/src/tileworld/agent/SmartTWAgent.java` | Main agent class extending TWAgent |
| `Tileworld/src/tileworld/agent/SmartTWAgentMemory.java` | Enhanced memory with fuel station tracking, decay, exploration state |

### Files Modified

| File | Change |
|------|--------|
| `Tileworld/src/tileworld/environment/TWEnvironment.java` | Lines 18-19: added SmartTWAgent import. Lines 112-114: replaced SimpleTWAgent with SmartTWAgent. Line 14: removed unused sun.font import. |

### Architecture

```
SmartTWAgent extends TWAgent
  |-- SmartTWAgentMemory extends TWAgentWorkingMemory
  |-- AstarPathGenerator (reused from framework)
  |-- Lawnmower state machine (HORIZONTAL / SHIFTING)
```

### How SmartTWAgent.think() Works

```
if fuel_station_unknown:
    EXPLORE (pure lawnmower sweep to find fuel station ASAP)

if on_fuel_station and fuel < max:
    REFUEL

if fuel_emergency (fuel <= safety_margin):
    NAVIGATE to fuel station (A* path, greedy fallback)

if carrying_tile and standing_on_hole:
    DROP tile

if not_full and standing_on_tile:
    PICKUP tile

else:
    EXPLORE (lawnmower sweep)
```

### Key Design Decisions

1. **Fuel station detection**: `TWFuelStation` extends `TWEntity` (NOT `TWObject`). The parent's `updateMemory()` skips non-TWObject entities. We override `updateMemory()` in `SmartTWAgentMemory` to intercept fuel stations from the sensed objects bag.

2. **Parallel state arrays**: Parent's `objects[][]` is private. We maintain `rememberedEntities[][]` and `observationTimes[][]` in `SmartTWAgentMemory`. On decay, we call the public `removeAgentPercept(x, y)` to sync parent state.

3. **Memory decay**: Deterministic removal when `age >= Parameters.lifeTime`. Called every step inside `updateMemory()`.

4. **Fuel safety margin**: When station is known: `(int)(manhattanDist * 1.5) + 30`. When unknown: `defaultFuelLevel * 0.5` (agent prioritizes finding station).

5. **Lawnmower exploration**: State machine with HORIZONTAL (sweep East/West) and SHIFTING (move North/South by 7 cells). Sweeps toward closer horizontal edge first. Shifts toward further vertical edge, reverses at grid boundaries.

6. **Sensor cleared cells**: When sensor observes empty cells, parallel arrays are cleared too, preventing stale "phantom objects."

### Known Limitations

- **Fuel station finding**: With 500 fuel on a 50x50 grid, ~5-10% of random seeds result in the agent not finding the fuel station in time. Phase 3 (communication) fixes this — once ANY agent finds the station, all agents know.
- **No goal-directed planning**: Agent only does opportunistic pickups/drops (standing on the object). Phase 2 adds A*-based planning to deliberately seek tiles and deliver to holes.
- **No team coordination**: Currently 2 agents operate independently. Phase 3 adds 6 agents with communication.

### Performance

Config 1 (50x50, sparse, lifetime=100): Average team reward ~34 per run (10 runs averaged).
Agents survive 5000 steps in most runs, refuel successfully, and score through opportunistic pickups.

---

## Phase 2: Solo Scoring (Next)

**Goal:** Agent actively seeks tiles, plans deliveries, and maximizes individual score.

### What to Build

1. **`Tileworld/src/tileworld/planners/SmartTWPlanner.java`** — Implements `TWPlanner` interface

   Use existing `AstarPathGenerator` for pathfinding. The planner holds:
   - Reference to SmartTWAgent and SmartTWAgentMemory
   - Current `TWPath` (sequence of steps)
   - Current goal position and type (tile/hole/fuel)

   Key methods:
   - `generatePlan()` — Select best goal and compute A* path
   - `hasPlan()` — Is there an active plan?
   - `voidPlan()` — Discard current plan
   - `execute()` — Pop and return next step direction

   Plan invalidation triggers:
   - Target object expired from memory (age >= lifeTime)
   - Fuel drops below safety margin (switch to fuel plan)
   - Closer/better opportunity appears in sensor range

2. **Modify `SmartTWAgent.think()`** — Add planning priorities between fuel emergency and explore:

   ```
   FUEL EMERGENCY (existing)
   REFUEL (existing)
   DROP TILE — carrying tile & on hole (existing)
   >>> NEW: DELIVER — carrying tile & hole in memory? Plan path to hole
   PICKUP NEARBY — tile in sensor range (existing)
   >>> NEW: SEEK TILE — tile in memory & affordable round-trip? Plan path to tile
   EXPLORE (existing)
   ```

3. **Affordable round-trip check** — Before committing to a tile:
   ```java
   int cost = dist(me, tile) + dist(tile, nearestHole) + dist(nearestHole, fuelStation);
   boolean affordable = fuelLevel > cost * 1.5 + 30;
   ```
   If no hole is known, only pick up tiles in immediate sensor range.

### Key Methods in SmartTWAgentMemory to Reuse
- `getAllRememberedTiles()` — Returns List<TWTile> of non-expired tiles
- `getAllRememberedHoles()` — Returns List<TWHole> of non-expired holes
- `getClosestRememberedTile()` — Nearest tile to agent
- `getClosestRememberedHole()` / `getClosestRememberedHole(int fromX, int fromY)` — Nearest hole

### Testing
- Config 1: Target 40+ team reward per run (vs ~34 currently)
- Config 2: Run with `Parameters2.java` settings. Agents should adapt to shorter lifetimes.
- Zero fuel deaths

---

## Phase 3: Team Coordination

**Goal:** 6 agents communicate to share discoveries, avoid duplicate work, and partition the grid.

### What to Build

1. **`Tileworld/src/tileworld/agent/TWMessage.java`** — Typed message class

   Encode message types in the `message` string field as `"TYPE:data"`:

   | Type | Payload | Purpose |
   |------|---------|---------|
   | `FUEL_FOUND` | `x,y` | Broadcast fuel station location |
   | `TILE_SEEN` | `x,y,time` | Share tile discovery |
   | `HOLE_SEEN` | `x,y,time` | Share hole discovery |
   | `CLAIMING` | `x,y` | I'm heading to this target |
   | `GONE` | `x,y` | Object no longer exists |

2. **Override `SmartTWAgent.communicate()`**:
   ```java
   // 1. Read all messages from getEnvironment().getMessages()
   // 2. Process each (update memory with shared info)
   // 3. Broadcast new discoveries
   // 4. Broadcast current target claim
   ```

   Note: `TWEnvironment.step()` calls `messages.clear()` every step (line 175), so messages only last one step. The scheduling order is: env.step (ordering 1) -> sense+communicate (ordering 2) -> think+act (ordering 3). So messages sent in step N are available to other agents in step N+1.

3. **Zone assignment** in `SmartTWAgent`:
   - Assign each agent a "home zone" based on its name/index (1-6)
   - Partition grid into 6 zones (3 columns × 2 rows, or 2×3)
   - Agents prioritize exploring their home zone but can cross boundaries
   - This requires modifying the lawnmower to prefer the assigned zone

4. **Target deconfliction** in `SmartTWAgentMemory`:
   - Store a set of claimed positions (from CLAIMING messages)
   - When `SmartTWPlanner` selects a target, skip if claimed by another agent
   - Clear claims after `Parameters.lifeTime` steps (object may have expired)

5. **Modify `TWEnvironment.java` `start()`** to create 6 SmartTWAgents

### Critical Architecture Note

The fuel station discovery sharing is the single highest-value communication feature. Currently ~5-10% of runs fail because an individual agent can't find the station in 500 moves. With 6 agents exploring, the probability of at least one finding it quickly is very high. Broadcasting `FUEL_FOUND` immediately eliminates this failure mode.

### Testing
- 6 agents should collectively score more than 6 independent agents
- Fuel station shared within ~100 steps (6 agents exploring = 6× coverage)
- Zero fuel deaths (with station sharing)
- No two agents pursuing the same tile

---

## Phase 4: Optimization & Adaptability

**Goal:** Tune for competition across all 3 configs, including unknown Config 3.

### What to Build

1. **Adaptive parameters** in SmartTWAgent/SmartTWPlanner:
   - Track runtime statistics: objects seen per step (rolling avg), estimated object lifetime
   - Adjust thresholds dynamically:
     - High object rate → greedy mode (grab nearest, less exploration)
     - Low object rate → exploration mode (cover more ground)
     - Short lifetime → reduce max travel distance
     - Long lifetime → willing to travel further

2. **Scoring optimizations**:
   - Tile batching: when carrying < 3 tiles and another tile is very close, grab it before seeking a hole
   - Hole proximity bias: prefer tiles near known holes
   - Opportunistic refueling: refuel when passing near station even if not critical
   - Delivery chains: after dropping tile, immediately check for nearby tiles at hole location

3. **Config 2 testing** (80×80, mean=2.0, lifetime=30):
   - Use `Parameters2.java` — may need to adjust `Parameters.java` or add config switching
   - Key differences: much more objects (grab quickly), shorter lifetime (don't travel far), larger grid (zone assignment more important)

4. **Experiment collection**:
   - Modify `TileworldMain.java` to log per-agent scores
   - Run each config 10+ times, compute mean and std deviation
   - Compare: with/without communication, different zone strategies, different safety margins

### Testing
- Config 1: Team score target 100+ per run
- Config 2: Team score target 150+ per run
- Config 3: Should perform reasonably (within 80% of best config-specific score)

---

## Critical Constraints (DO NOT VIOLATE)

1. **DO NOT modify** anything in `tileworld.environment` package (except `TWEnvironment.start()` for agent creation)
2. **DO NOT override** `sense()`, `putTileInHole()`, `pickUpTile()`, `refuel()` in TWAgent
3. **DO NOT call** `increaseReward()` — only `putTileInHole()` does this internally
4. **ONLY override** `communicate()`, `think()`, and `act()` in TWAgent
5. **Sensor range is 3** (7×7 area) — this is fixed, don't try to change it
6. **Max 3 tiles** carried at once
7. **Fuel: 500 start, -1 per move, 0 = permanently stuck**

## Build & Run

```bash
# Compile
javac -cp lib/MASON_14.jar -d Tileworld/bin -sourcepath Tileworld/src \
  Tileworld/src/tileworld/TileworldMain.java

# Run headless (10 iterations)
java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TileworldMain

# Run with GUI
java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TWGUI
```

## File Map

```
Tileworld/src/tileworld/
├── agent/
│   ├── TWAgent.java              # Base class — DO NOT MODIFY
│   ├── SmartTWAgent.java         # OUR AGENT (Phase 1 done)
│   ├── SmartTWAgentMemory.java   # OUR MEMORY (Phase 1 done)
│   ├── SimpleTWAgent.java        # Reference naive agent
│   ├── TWAgentWorkingMemory.java # Base memory — extend only
│   ├── TWAgentSensor.java        # Sensor — DO NOT MODIFY
│   ├── TWAgentPercept.java       # Memory fact (entity + time)
│   ├── Message.java              # Base message class
│   ├── TWAction.java             # Enum: MOVE, PICKUP, PUTDOWN, REFUEL
│   └── TWThought.java            # Think result (action + direction)
├── planners/
│   ├── TWPlanner.java            # Interface to implement (Phase 2)
│   ├── AstarPathGenerator.java   # A* pathfinding — REUSE THIS
│   ├── TWPath.java               # Path = LinkedList<TWPathStep>
│   ├── TWPathStep.java           # Step = (x, y, direction)
│   └── DefaultTWPlanner.java     # Skeleton — reference only
├── environment/                  # DO NOT MODIFY (except TWEnvironment.start())
│   ├── TWEnvironment.java        # Main sim — agent creation in start()
│   ├── TWEntity.java             # Base entity (x, y, env)
│   ├── TWObject.java             # Entity with lifetime (tile, hole, obstacle)
│   ├── TWTile.java, TWHole.java  # Pickable / fillable
│   ├── TWObstacle.java           # Blocks movement
│   ├── TWFuelStation.java        # NOT a TWObject! Extends TWEntity directly
│   └── TWDirection.java          # Enum: N(0,-1), S(0,1), E(1,0), W(-1,0), Z(0,0)
├── Parameters.java               # Config 1: 50×50, mean=0.2, lifetime=100
├── Parameters2.java              # Config 2: 80×80, mean=2.0, lifetime=30
├── TileworldMain.java            # Headless runner (10 iterations)
└── TWGUI.java                    # GUI runner
```
