# Team Strategy: Shared Foundation + Individual Specialisation + Information Broadcast

## Core Idea

Our architecture has two layers:

**Layer 1: Shared Infrastructure** (implemented in `SmartTWAgent` and utility classes)
- All algorithmic improvements go here so all 6 agents benefit
- Examples: A* pathfinding, fuel-aware planning, memory decay, claim system, message schema
- Implemented collaboratively — each team member "owns" one subsystem but codes it in the shared base class

**Layer 2: Agent Specialisation** (implemented in personal agent files via `think()` override)
- Each agent has distinct behavioral priorities and decision logic
- Examples: Fuel Scout explores aggressively, Tile Hunter ignores holes until carrying 3 tiles
- Uses all shared capabilities from Layer 1 but prioritizes actions differently

This is grounded in MAS theory: agents have individual goals (fill holes, don't run out of fuel) but cannot always achieve those goals alone. So they share knowledge and request help. In Tileworld specifically, agents cannot act *for* each other — Agent B cannot physically refuel Agent A — but Agent B can send Agent A the information it needs to act better in its own planning cycle.

---

## Current State & What Needs to Be Done

### ✅ Already Implemented (Layer 1 — Shared Foundation)

**Most of the foundation is complete.** However, small visibility changes are needed before implementing specialists.

- `SmartTWAgent` — Full base agent with 8-priority decision hierarchy
- `SmartTWAgentMemory` — Enhanced memory with time-decay, claims, environment detection
- `SmartTWPlanner` — Goal-directed planning with A* pathfinding and fuel checks
- `AstarPathGenerator` — A* navigation with obstacle avoidance
- `TWPath` / `TWPathStep` — Path representation classes
- `Message.java` — String-based messaging (5/8 types working)
- Zone-based exploration — 3×2 grid assignment with lawnmower pattern
- Claim system — Embedded in memory with auto-expiry
- Runtime environment detection — `isDense()`, `isShortLifetime()`, `isLargeGrid()`

### ⚠️ Required Before Starting: Base Class Visibility Changes

**Three small changes needed to `SmartTWAgent.java` before implementing specialists:**

1. **Add getter method** (~3 lines):
   ```java
   protected SmartTWPlanner getPlanner() {
       return planner;
   }
   ```

2. **Change `navigateTo()` from `private` to `protected`** (~1 line change)

3. **Change `exploreGreedy()` from `private` to `protected`** (~1 line change)

**Why these are needed:** The specialist examples use these methods, but they're currently private. Changing them to `protected` allows subclasses to access them. This doesn't violate assignment constraints (you're not overriding forbidden methods, just changing visibility).

See **"Base Class Update Required"** section below for exact locations and code.

---

### ❌ To Be Implemented (Layer 2 — Agent Specializations)

**Each team member creates their own agent file (e.g., `FuelScoutAgent.java`) extending `SmartTWAgent`:**

1. **Fuel Scout** — Lower fuel threshold (200 vs 125), aggressive station discovery, broadcasts LOW messages
2. **Tile Hunter** — Ignores holes until carrying 3 tiles, aggressive tile collection
3. **Hole Filler** — Patrols near holes, prioritizes hole discovery
4. **Explorer** — Systematic zone coverage, broadcasts SWAP messages when zone explored
5. **Delivery Optimizer** — Cluster-density routing, broadcasts HOTSPOT messages
6. **Smarter Replanning** — Predictive failure detection, broadcasts EXPIRING messages

### ✅ COMPLETED: Shared Infrastructure Updates

**Update 1: Method visibility changes** ✅ DONE
- Added `getPlanner()` getter
- Made `navigateTo()` protected
- Made `exploreGreedy()` protected
- Added `agentIndex` field and getter

**Update 2: Custom message parsing** ✅ DONE
- Added 4 protected fields for custom message data
- Added `HotspotEntry` inner class
- Extended `communicate()` to parse `LOW`, `EXPIRING`, `HOTSPOT`, `SWAP` messages
- Clears custom message data at start of each step

**Implementation:** All changes completed in `SmartTWAgent.java`.

---

## How It Works in Practice

Every time step, each agent runs this cycle:

```
Sense → Communicate (publish) → Think (subscribe + plan) → Act
```

**Example at Step 547:**
```
Time Step 547:
├─ Agent A.sense() → sees fuel station at (34, 12)
├─ Agent A.communicate() → broadcasts STATION_FOUND(34, 12)
├─ Agent B.think() → reads getMessages(), sees station, updates memory
├─ Agent B.act() → moves toward (34, 12) using this new knowledge
└─ (parallel for all 6 agents)
```

- In `communicate()`, the agent packages whatever it discovered into a `Message` object and calls `environment.receiveMessage(msg)` — this puts it into the environment's broadcast list.
- Every other agent reads all messages via `environment.getMessages()` during their own `think()` phase.
- There are no private channels — the broadcast is global, so one message reaches all 6 agents in the same step.

---

## Base Class Update Required (One-Time Task)

**CRITICAL:** Before implementing specialists, you MUST make these small changes to `SmartTWAgent.java`. The specialist examples below depend on these methods being accessible.

**Why these changes are needed:**
- The assignment allows overriding `communicate()`, `think()`, and `act()` only
- Adding NEW protected methods is allowed (not overriding)
- Changing visibility from `private` to `protected` is allowed (not modifying behavior)

---

### Step 0: Add Protected Methods to SmartTWAgent ✅ DONE

**Added ONE getter method** to `SmartTWAgent.java`:

```java
// ✅ IMPLEMENTED (line 629)
protected SmartTWPlanner getPlanner() {
    return planner;
}
```

**Changed visibility** of two existing methods from `private` to `protected`:

```java
// ✅ IMPLEMENTED (line 385)
protected TWDirection navigateTo(int tx, int ty, String goalType) {
    // ... existing implementation unchanged ...
}

// ✅ IMPLEMENTED (line 421)
protected TWDirection exploreGreedy() {
    // ... existing implementation unchanged ...
}
```

**Result:** Specialists can now:
- Access the planner via `getPlanner()`
- Call `navigateTo()` for direct A* navigation
- Call `exploreGreedy()` for fuel station search

**No behavior was modified** — only visibility was changed from `private` to `protected`, and a getter was added.

---

### Step 1: Add Protected Fields (for custom message parsing) ✅ DONE

```java
// ✅ IMPLEMENTED (lines 65-79)
// Custom message data (populated in communicate(), used in think())
protected final ArrayList<String> lowFuelAgents = new ArrayList<>();
protected final ArrayList<Int2D> expiringTargets = new ArrayList<>();
protected final ArrayList<HotspotEntry> hotspots = new ArrayList<>();
protected final ArrayList<String> zoneSwapRequests = new ArrayList<>();

// Inner class for hotspot data (position + density together, no parallel lists)
protected static class HotspotEntry {
    final Int2D position;
    final double density;

    HotspotEntry(Int2D position, double density) {
        this.position = position;
        this.density = density;
    }
}
```

### Step 2: Update communicate() Method ✅ DONE

```java
// ✅ IMPLEMENTED (lines 120-216)
// Extended communicate() to parse 4 new message types: LOW, EXPIRING, HOTSPOT, SWAP
// Clears custom message data at start of each step
```

    for (Message msg : messages) {
        if (msg.getFrom().equals(name))
            continue; // skip own messages
        String content = msg.getMessage();
        if (content == null)
            continue;

        int colonIdx = content.indexOf(':');
        if (colonIdx < 0)
            continue;
        String type = content.substring(0, colonIdx);
        String payload = content.substring(colonIdx + 1);

        try {
            // --- EXISTING BASE CLASS PARSING (keep this unchanged) ---
            if ("FUEL".equals(type)) {
                String[] parts = payload.split(",");
                int fx = Integer.parseInt(parts[0]);
                int fy = Integer.parseInt(parts[1]);
                smartMemory.setFuelStation(fx, fy);
            } else if ("TILE".equals(type)) {
                String[] parts = payload.split(",");
                int tx = Integer.parseInt(parts[0]);
                int ty = Integer.parseInt(parts[1]);
                double time = Double.parseDouble(parts[2]);
                smartMemory.addSharedTile(tx, ty, time);
            } else if ("HOLE".equals(type)) {
                String[] parts = payload.split(",");
                int hx = Integer.parseInt(parts[0]);
                int hy = Integer.parseInt(parts[1]);
                double time = Double.parseDouble(parts[2]);
                smartMemory.addSharedHole(hx, hy, time);
            } else if ("GONE".equals(type)) {
                String[] parts = payload.split(",");
                int gx = Integer.parseInt(parts[0]);
                int gy = Integer.parseInt(parts[1]);
                smartMemory.removeSharedEntity(gx, gy);
            } else if ("CLAIM".equals(type)) {
                String[] parts = payload.split(",");
                int cx = Integer.parseInt(parts[0]);
                int cy = Integer.parseInt(parts[1]);
                smartMemory.addClaim(cx, cy, now);

            // --- NEW CUSTOM MESSAGE TYPES (add these) ---
            } else if ("LOW".equals(type)) {
                // Parse: "LOW:agentId,fuel"
                String[] parts = payload.split(",");
                if (parts.length >= 1) {
                    lowFuelAgents.add(parts[0]); // Store agentId with low fuel
                }
            }
            else if ("EXPIRING".equals(type)) {
                // Parse: "EXPIRING:x,y,reason"
                String[] parts = payload.split(",");
                if (parts.length >= 2) {
                    int ex = Integer.parseInt(parts[0]);
                    int ey = Integer.parseInt(parts[1]);
                    expiringTargets.add(new Int2D(ex, ey));
                }
            }
            else if ("HOTSPOT".equals(type)) {
                // Parse: "HOTSPOT:x,y,density"
                String[] parts = payload.split(",");
                if (parts.length >= 3) {
                    Int2D pos = new Int2D(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1])
                    );
                    double density = Double.parseDouble(parts[2]);
                    hotspots.add(new HotspotEntry(pos, density));
                }
            }
            else if ("SWAP".equals(type)) {
                // Parse: "SWAP:agentName,status" e.g., "SWAP:Explorer3,ZONE_DONE"
                String[] parts = payload.split(",");
                if (parts.length >= 2) {
                    zoneSwapRequests.add(parts[0] + "," + parts[1]); // Store as "agentName,status"
                }
            }
        } catch (Exception e) {
            // Malformed message, skip
        }
    }

    // 2. Broadcast fuel station location
    if (smartMemory.isFuelStationKnown()) {
        Int2D fp = smartMemory.getKnownFuelStation();
        sendMsg("FUEL:" + fp.x + "," + fp.y);
    }

    // 3. Broadcast new tile/hole sightings and gone positions
    for (Int2D t : smartMemory.getNewTiles()) {
        sendMsg("TILE:" + t.x + "," + t.y + "," + now);
    }
    for (Int2D h : smartMemory.getNewHoles()) {
        sendMsg("HOLE:" + h.x + "," + h.y + "," + now);
    }
    for (Int2D g : smartMemory.getGoneEntities()) {
        sendMsg("GONE:" + g.x + "," + g.y);
    }

    // 4. Broadcast current planner target as claim
    Int2D goal = planner.getCurrentGoal();
    if (goal != null) {
        sendMsg("CLAIM:" + goal.x + "," + goal.y);
    }
}
```

### That's It!

Now all specialists can access these fields in their `think()` method:
- `lowFuelAgents` — List of agent IDs with low fuel
- `expiringTargets` — List of (x,y) positions that will expire soon
- `hotspots` — List of `HotspotEntry` objects (each contains position + density)
- `zoneSwapRequests` — Agents requesting zone reassignment

**Important notes:**
- **Int2D.equals() warning:** `expiringTargets.contains(hole)` relies on `Int2D.equals()` working correctly. MASON's `Int2D` class might not override `equals()` properly. **Test this** when implementing. If it doesn't work, use manual comparison:
  ```java
  boolean isExpiring = false;
  for (Int2D exp : expiringTargets) {
      if (exp.x == hole.x && exp.y == hole.y) {
          isExpiring = true;
          break;
      }
  }
  if (isExpiring) { /* skip this hole */ }
  ```
- **HotspotEntry access:** Use `entry.position` and `entry.density` to access hotspot data:
  ```java
  for (HotspotEntry entry : hotspots) {
      Int2D pos = entry.position;
      double density = entry.density;
      // ...
  }
  ```
- **Null safety:** Always check if lists are empty before accessing:
  ```java
  if (!hotspots.isEmpty()) {
      // Safe to iterate
  }
  ```
- **SWAP messages:** See Explorer section for how agents can respond to `zoneSwapRequests`

---

## Shared Infrastructure (Layer 1) — CURRENT IMPLEMENTATION STATUS

These are implemented in the shared `SmartTWAgent` base class and utility classes. All 6 agents use these capabilities.

| Component | Location | Purpose | Status |
|---|---|---|---|
| **A* Pathfinding** | `AstarPathGenerator.java` + `SmartTWAgent.navigateTo()` | Fuel-aware A* navigation with obstacle avoidance | ✅ **Implemented** |
| **SmartTWPlanner** | `SmartTWPlanner.java` | Goal-directed planning with lifetime validation, affordability checks | ✅ **Implemented** |
| **Memory System** | `SmartTWAgentMemory.java` | Time-based decay, observation tracking, runtime environment detection | ✅ **Implemented** |
| **Message System** | `Message.java` (string-based) | 5/8 message types working (FUEL, TILE, HOLE, GONE, CLAIM), 4 custom types to add | ⚠️ **Needs 4 more types** |
| **Claim System** | `SmartTWAgentMemory` (embedded) | `addClaim()`, `isClaimed()`, auto-expiry in `decayMemory()` | ✅ **Implemented** |
| **Path Representation** | `TWPath.java` | Linked list with `peekNext()`, `popNext()`, `hasNext()` | ✅ **Implemented** |
| **Zone Management** | `SmartTWAgent` constructor | 3×2 grid zone assignment with lawnmower exploration | ✅ **Implemented** |
| **Base Agent Logic** | `SmartTWAgent.think()` | 8-priority decision hierarchy (fuel emergency → deliver → explore) | ✅ **Implemented** |

**Key point**: Algorithmic improvements (like A* pathfinding or claim systems) are implemented HERE so all 6 agents benefit. If only one agent had better pathfinding, the other 5 would still move inefficiently.

---

## What Each Specialist Inherits (No Code Needed)

Every agent specialization starts with these capabilities by extending `SmartTWAgent`:

### Memory Access
```java
SmartTWAgentMemory mem = getSmartMemory();

// Query objects
mem.getAllTilePositions()          → List<Int2D>
mem.getAllHolePositions()          → List<Int2D>
mem.getClosestTilePosition()       → Int2D
mem.getClosestHolePosition()        → Int2D
mem.getKnownFuelStation()          → Int2D (null if unknown)
mem.isFuelStationKnown()           → boolean

// Claim system
mem.isClaimed(x, y)                → boolean
mem.addClaim(x, y, time)           → void (auto-clears each step)
mem.clearAllClaims()               → void

// Observation timestamps (for predictive replanning)
mem.getObservationTime(x, y)       → double (time when object was seen)

// Runtime environment detection
mem.isDense()                      → boolean (Config 2: true)
mem.isShortLifetime()              → boolean (Config 2: true)
mem.isLargeGrid()                  → boolean (Config 2: true)
mem.getEstimatedLifetime()         → int (from observations)

// Exploration tracking
mem.getLeastVisitedTarget(radius)  → Int2D (for systematic exploration)
```

### Planning & Navigation
```java
SmartTWPlanner planner = getPlanner();

// Goal-directed planning (fuel-aware, checks affordability)
planner.planToTile()               → TWPath (null if none affordable)
planner.planToHole()               → TWPath (null if none affordable)
planner.execute()                  → TWDirection (next step)
planner.hasPlan()                  → boolean
planner.getCurrentGoal()           → Int2D
planner.voidPlan()                 → void

// Direct A* navigation (for custom targets)
navigateTo(tx, ty, goalType)       → TWDirection (uses cached paths)
```

### Agent State
```java
getFuelLevel()                     → int
getCarriedTileCount()              → int (0-3)
hasTile()                          → boolean
getX(), getY()                      → int
getName()                          → String
getEnvironment()                   → TWEnvironment (for schedule time, etc.)
getAgentIndex()                    → int (✅ **Implemented**)
```

**✅ `getAgentIndex()` is now implemented in `SmartTWAgent`:**

The Explorer specialist calls `getAgentIndex()` for zone assignment. This method has been added to `SmartTWAgent` as:
- Field: `private final int agentIndex;`
- Getter: `public int getAgentIndex()`
- Initialized in constructor from the `agentIndex` parameter

### Communication (Already Implemented in Base Class)
The base `SmartTWAgent.communicate()` **already broadcasts**:
- `"FUEL:x,y"` — when fuel station is known
- `"TILE:x,y,time"` — for new tile sightings
- `"HOLE:x,y,time"` — for new hole sightings
- `"GONE:x,y"` — for disappeared objects
- `"CLAIM:x,y"` — for current planner target

Specialists **only need to add** their custom message types (LOW, EXPIRING, HOTSPOT, SWAP).

---

## Agent Specialisations (Layer 2)

Each team member implements one of these behavioral specializations in their personal agent file. All specializations use the shared capabilities from Layer 1 but prioritize different actions.

### 1. Fuel Scout
**Behavioral focus:** Aggressive fuel station discovery and lower refuel threshold

```java
public class FuelScoutAgent extends SmartTWAgent {

    public FuelScoutAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        // PRIORITY 0: Fuel is EXTRA critical for me (40% vs base ~25% emergency)
        // NOTE: This threshold triggers NAVIGATION toward the station, not the REFUEL action itself.
        //       The actual REFUEL action fires when standing on the station (base agent PRIORITY 2).
        // ADAPTIVE: Scale refuel threshold with grid size for Config 3 robustness
        int refuelThreshold = getSmartMemory().isLargeGrid() ? 250 : 175;
        if (getFuelLevel() < refuelThreshold && getSmartMemory().isFuelStationKnown()) {
            Int2D fuelPos = getSmartMemory().getKnownFuelStation();
            TWDirection dir = navigateTo(fuelPos.x, fuelPos.y, "fuel");
            if (dir != null) {
                return new TWThought(TWAction.MOVE, dir);
            }
        }

        // PRIORITY 1: When station unknown, explore with LARGER search radius
        // This differentiates from base exploration — we cast a wider net
        if (!getSmartMemory().isFuelStationKnown()) {
            Int2D target = getSmartMemory().getLeastVisitedTarget(20); // Wider: 20 vs base ~7-10
            if (target != null) {
                TWDirection dir = navigateTo(target.x, target.y, "explore");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
            // Fallback to base exploration
            TWDirection dir = exploreGreedy();
            if (dir != null) {
                return new TWThought(TWAction.MOVE, dir);
            }
        }

        // PRIORITY 2: Normal tasks (fall through to base)
        return super.think();
    }

    @Override
    public void communicate() {
        super.communicate(); // Base broadcasts FUEL/TILE/HOLE/GONE/CLAIM

        // Broadcast fuel warning early (at 30% vs base emergency threshold)
        if (getFuelLevel() < 150) {
            getEnvironment().receiveMessage(
                new Message(getName(), "", "LOW:" + getName() + "," + getFuelLevel())
            );
        }
    }
}
```

**What makes this different:**
- **Lower fuel threshold (200 vs ~125 base emergency)** — Refuels before crisis
- **Wider exploration radius when station unknown** — Uses `getLeastVisitedTarget(20)` for broader search vs base's focused lawnmower pattern
- **Broadcasts LOW messages** — Teammates can adjust zone coverage to avoid overloading fuel-scarce areas

**Key differentiation:** The base agent also explores when station is unknown, but uses a systematic lawnmower pattern within its assigned zone. Fuel Scout uses `getLeastVisitedTarget()` with a **20-radius search** (vs base's ~7-10 sensor range), allowing it to jump between zones and discover the station faster.

---

### 2. Tile Hunter
**Behavioral focus:** Ignores holes until carrying 3 tiles, aggressively collects tiles

```java
public class TileHunterAgent extends SmartTWAgent {

    public TileHunterAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        int carriedTiles = getCarriedTileCount();

        // ADAPTIVE: In short-lifetime environments, deliver immediately to avoid expiration
        // In long-lifetime environments, batch 3 tiles for efficiency
        int tileTarget = getSmartMemory().isShortLifetime() ? 1 : 3;

        // PRIORITY 0: Ignore holes unless at target capacity
        if (carriedTiles < tileTarget) {
            Int2D nearestTile = getSmartMemory().getClosestTilePosition();
            if (nearestTile != null && !getSmartMemory().isClaimed(nearestTile.x, nearestTile.y)) {
                // Claim this tile
                getSmartMemory().addClaim(nearestTile.x, nearestTile.y, getEnvironment().schedule.getTime());

                TWDirection dir = navigateTo(nearestTile.x, nearestTile.y, "tile");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
            // No tile found — explore (use base exploration)
            TWDirection dir = exploreGreedy();
            if (dir != null) {
                return new TWThought(TWAction.MOVE, dir);
            }
        }

        // PRIORITY 1: At target capacity — find nearest hole
        if (carriedTiles >= tileTarget) {
            Int2D nearestHole = getSmartMemory().getClosestHolePosition();
            if (nearestHole != null && !getSmartMemory().isClaimed(nearestHole.x, nearestHole.y)) {
                TWDirection dir = navigateTo(nearestHole.x, nearestHole.y, "hole");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // PRIORITY 2: Fall through to base logic
        return super.think();
    }
}
```

**What makes this different:** Won't consider holes until carrying 3 tiles, claims tiles aggressively, fills delivery queue before delivering. This is effective in dense environments where tiles are plentiful.

---

### 3. Hole Filler
**Behavioral focus:** Prioritizes finding holes, waits near holes for tiles

```java
public class HoleFillerAgent extends SmartTWAgent {

    public HoleFillerAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        // PRIORITY 0: If carrying a tile, deliver to nearest hole immediately
        if (getCarriedTileCount() > 0) {
            Int2D nearestHole = getSmartMemory().getClosestHolePosition();
            if (nearestHole != null) {
                // Skip expiring holes (use manual comparison, see Int2D.equals() warning)
                boolean isExpiring = false;
                for (Int2D exp : expiringTargets) {
                    if (exp.x == nearestHole.x && exp.y == nearestHole.y) {
                        isExpiring = true;
                        break;
                    }
                }

                if (isExpiring) {
                    getPlanner().voidPlan();
                    return super.think(); // Find different hole
                }

                TWDirection dir = navigateTo(nearestHole.x, nearestHole.y, "hole");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // PRIORITY 1: If not carrying, patrol near known holes
        // ENVIRONMENT AWARENESS: Only patrol in dense environments where holes are plentiful
        // In sparse environments, camping near holes wastes fuel — skip patrol entirely
        if (!getSmartMemory().isDense()) {
            // Sparse environment — skip patrol, go collect tiles
            return super.think();
        }

        Int2D nearestHole = getSmartMemory().getClosestHolePosition();
        if (nearestHole != null) {
            // Check if hole is expiring
            boolean isExpiring = false;
            for (Int2D exp : expiringTargets) {
                if (exp.x == nearestHole.x && exp.y == nearestHole.y) {
                    isExpiring = true;
                    break;
                }
            }

            if (!isExpiring) {
                int distToHole = Math.abs(getX() - nearestHole.x) + Math.abs(getY() - nearestHole.y);

                // If far from hole, move toward it
                if (distToHole > 5) {
                    TWDirection dir = navigateTo(nearestHole.x, nearestHole.y, "patrol");
                    if (dir != null) {
                        return new TWThought(TWAction.MOVE, dir);
                    }
                }

                // If near hole, patrol in small area (within 5 cells)
                // IMPORTANT: Add fuel guard to prevent burning fuel indefinitely
                if (distToHole <= 5 && getFuelLevel() > 150) {
                    // Move perpendicular to hole direction to circle it
                    TWDirection[] perp = getPerpendicularDirections(nearestHole);
                    for (TWDirection d : perp) {
                        if (canMove(d)) {
                            return new TWThought(TWAction.MOVE, d);
                        }
                    }
                } else if (distToHole <= 5 && getFuelLevel() <= 150) {
                    // Low fuel and circling hole — go get a tile instead
                    return super.think();
                }
            }
        }

        // PRIORITY 2: No holes known — explore for holes
        return super.think();
    }

    private TWDirection[] getPerpendicularDirections(Int2D target) {
        // Get directions perpendicular to line to target
        int dx = target.x - getX();
        int dy = target.y - getY();

        if (Math.abs(dx) > Math.abs(dy)) {
            // Moving mostly horizontal, return vertical directions
            return new TWDirection[] { TWDirection.N, TWDirection.S };
        } else {
            // Moving mostly vertical, return horizontal directions
            return new TWDirection[] { TWDirection.E, TWDirection.W };
        }
    }

    private boolean canMove(TWDirection dir) {
        int nx = getX() + dir.dx;
        int ny = getY() + dir.dy;
        return getEnvironment().isInBounds(nx, ny) && !getEnvironment().isCellBlocked(nx, ny);
    }

    @Override
    public void communicate() {
        super.communicate(); // Base handles ALL parsing + FUEL/TILE/HOLE/GONE/CLAIM broadcast

        // No custom broadcasts needed — base already broadcasts hole discoveries
    }
}
```

**What makes this different:** Focuses on holes first, patrols near holes (within 5 cells) instead of wandering off, avoids expiring holes, stays near holes to receive tiles from teammates.

---

### 4. Explorer
**Behavioral focus:** Systematic zone coverage, maintains shared map

```java
public class ExplorerAgent extends SmartTWAgent {

    private boolean zoneFullyExplored = false;

    public ExplorerAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        // PRIORITY 0: Use hotspot data to bias exploration toward productive areas
        // IMPORTANT: Only bias toward hotspots WITHIN assigned zone to avoid leaving zone uncovered
        if (!hotspots.isEmpty()) {
            HotspotEntry bestHotspot = getBestHotspot();
            if (bestHotspot != null) {
                Int2D pos = bestHotspot.position;
                // Check if hotspot is within this agent's assigned zone
                if (isInAssignedZone(pos.x, pos.y)) {
                    int dist = Math.abs(getX() - pos.x) + Math.abs(getY() - pos.y);
                    if (dist > 15) { // If far from hotspot, bias exploration that way
                        TWDirection dir = navigateTo(pos.x, pos.y, "explore_hotspot");
                        if (dir != null) {
                            return new TWThought(TWAction.MOVE, dir);
                        }
                    }
                }
            }
        }

        // PRIORITY 1: Explore assigned zone systematically
        // ADAPTIVE: Scale search radius with grid size for Config 3 robustness
        int searchRadius = getSmartMemory().isLargeGrid() ? 20 : 10;
        Int2D target = getSmartMemory().getLeastVisitedTarget(searchRadius);
        if (target != null) {
            int dist = Math.abs(getX() - target.x) + Math.abs(getY() - target.y);
            if (dist > 2) { // Not already there
                TWDirection dir = navigateTo(target.x, target.y, "explore");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // PRIORITY 2: Zone fully explored — request swap
        if (!zoneFullyExplored && isZoneFullyExplored()) {
            zoneFullyExplored = true;
            // Broadcast SWAP request (will be sent in communicate())
            // Zone ID is based on agent index (0-5 map to 6 zones)
        }

        // PRIORITY 3: Normal tasks (collect tiles encountered during exploration)
        return super.think();
    }

    private boolean isZoneFullyExplored() {
        // Check if all cells in assigned zone have been visited recently
        // Zone is assigned in SmartTWAgent constructor (zoneStartX, zoneEndX, etc.)
        // Use actual exploration state from memory, not received hotspots
        double now = getEnvironment().schedule.getTime();
        double expirationThreshold = now - Parameters.lifeTime; // Cells visited before this are stale

        // Count visited cells in our assigned zone
        int visitedCount = 0;
        int totalZoneCells = 0;
        int xDim = getSmartMemory().getXDim();
        int yDim = getSmartMemory().getYDim();

        // Simple zone division: 6 vertical strips (agentIndex 0-5)
        int zoneWidth = xDim / 6;
        int zoneStartX = getAgentIndex() * zoneWidth;
        int zoneEndX = (getAgentIndex() == 5) ? xDim - 1 : zoneStartX + zoneWidth - 1;

        for (int x = zoneStartX; x <= zoneEndX; x++) {
            for (int y = 0; y < yDim; y++) {
                totalZoneCells++;
                double lastVisit = getSmartMemory().getObservationTime(x, y);
                if (lastVisit >= expirationThreshold) {
                    visitedCount++;
                }
            }
        }

        // Zone is fully explored if 80%+ of cells have been visited recently
        return totalZoneCells > 0 && (double) visitedCount / totalZoneCells >= 0.8;
    }

    private HotspotEntry getBestHotspot() {
        HotspotEntry best = null;
        double bestDensity = 0;
        for (HotspotEntry entry : hotspots) {
            if (entry.density > bestDensity) {
                bestDensity = entry.density;
                best = entry;
            }
        }
        return best;
    }

    private boolean isInAssignedZone(int x, int y) {
        // Calculate zone boundaries (same logic as isZoneFullyExplored)
        int xDim = getSmartMemory().getXDim();
        int yDim = getSmartMemory().getYDim();
        int zoneWidth = xDim / 6;
        int zoneStartX = getAgentIndex() * zoneWidth;
        int zoneEndX = (getAgentIndex() == 5) ? xDim - 1 : zoneStartX + zoneWidth - 1;

        // Check if position is within this agent's vertical zone strip
        return x >= zoneStartX && x <= zoneEndX && y >= 0 && y < yDim;
    }

    @Override
    public void communicate() {
        super.communicate(); // Base handles ALL parsing + FUEL/TILE/HOLE/GONE/CLAIM broadcast

        // Broadcast SWAP request when zone is fully explored
        if (zoneFullyExplored) {
            // Agent index is passed to constructor, use it to identify zone
            // Format: "SWAP:agentId,zoneId"
            getEnvironment().receiveMessage(
                new Message(getName(), "", "SWAP:" + getName() + ",ZONE_DONE")
            );
        }
    }
}
```

**What makes this different:** Systematic zone coverage using `getLeastVisitedTarget()`, biases exploration toward hotspots, broadcasts SWAP when zone is done. Uses hotspot data from Delivery Optimizer to focus on productive areas.

**How SWAP messages actually work:**

When Explorer broadcasts `"SWAP:Explorer3,ZONE_DONE"`, other agents see it in `zoneSwapRequests`. Here's how to respond:

```java
// In any specialist's think() - example response to SWAP:
if (!zoneSwapRequests.isEmpty() && !getPlanner().hasPlan() && getFuelLevel() > 250) {
    // Someone finished their zone - I can help by exploring their area
    // In practice: Base exploration (super.think()) will naturally expand
    // toward unvisited areas, which includes the done zone
    return super.think();
}
```

**In practice:** SWAP messages are primarily for **debugging/observation** - log when zones complete to verify exploration is balanced across agents. The base agent's natural exploration (lawnmower + `getLeastVisitedTarget()`) will automatically expand into uncovered areas. Full zone reassignment would require a coordinator agent with distributed negotiation, which is out of scope.


---

### 5. Delivery Optimizer
**Behavioral focus:** Computes optimal tile→hole pairings, minimizes delivery distance

```java
public class DeliveryOptimizerAgent extends SmartTWAgent {

    public DeliveryOptimizerAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        // PRIORITY 1: If carrying tile, find optimal hole (not just nearest)
        if (getCarriedTileCount() > 0) {
            Int2D bestHole = findOptimalHoleForTile();
            if (bestHole != null) {
                TWDirection dir = navigateTo(bestHole.x, bestHole.y, "hole");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        return super.think();
    }

    private Int2D findOptimalHoleForTile() {
        List<Int2D> holes = getSmartMemory().getAllHolePositions();
        if (holes.isEmpty()) return null;

        Int2D bestHole = null;
        double bestScore = Double.MAX_VALUE;

        for (Int2D hole : holes) {
            // Skip expiring holes (use manual comparison, see Int2D.equals() warning)
            boolean isExpiring = false;
            for (Int2D exp : expiringTargets) {
                if (exp.x == hole.x && exp.y == hole.y) {
                    isExpiring = true;
                    break;
                }
            }
            if (isExpiring) continue;

            int dist = Math.abs(getX() - hole.x) + Math.abs(getY() - hole.y);

            // Bonus for holes near hotspots (cluster density)
            double densityBonus = 0;
            for (HotspotEntry entry : hotspots) {
                Int2D hotspot = entry.position;
                int distToHotspot = Math.abs(hole.x - hotspot.x) + Math.abs(hole.y - hotspot.y);
                if (distToHotspot <= 10) {
                    densityBonus += entry.density * 5;
                }
            }

            double score = dist - densityBonus;
            if (score < bestScore) {
                bestScore = score;
                bestHole = hole;
            }
        }
        return bestHole;
    }

    private HotspotEntry getBestHotspot() {
        HotspotEntry best = null;
        double bestDensity = 0;
        for (HotspotEntry entry : hotspots) {
            if (entry.density > bestDensity) {
                bestDensity = entry.density;
                best = entry;
            }
        }
        return best;
    }

    @Override
    public void communicate() {
        super.communicate(); // Base handles ALL parsing

        // Broadcast your own hotspots
        for (HotspotEntry hotspot : findHotspots()) {
            getEnvironment().receiveMessage(
                new Message(getName(), "", "HOTSPOT:" + hotspot.position.x + "," + hotspot.position.y + "," + hotspot.density)
            );
        }
    }

    // Helper methods to find hotspots and calculate density
    private ArrayList<HotspotEntry> findHotspots() {
        // TODO: Implement hotspot scanning algorithm
        // Suggested approach:
        // 1. Scan grid cells within a reasonable radius (e.g., 10-15 cells) of current position
        // 2. For each cell, count tiles + holes within radius R (e.g., 5 cells)
        // 3. Return cells where density > threshold (e.g., 3+ objects)
        // 4. Each HotspotEntry should contain: cell position (Int2D) + calculated density (double)
        // Note: This requires access to memory's getAllTilePositions() and getAllHolePositions()
        // IMPORTANT: Without this implementation, no HOTSPOT messages are broadcast and
        //            Explorer agents receive no hotspot data to bias their exploration.
        return new ArrayList<>(); // Placeholder - MUST IMPLEMENT
    }
}
```

**What makes this different:** Uses `hotspots` data from base class to bias routing toward dense areas, avoids `expiringTargets`, considers cluster density when selecting holes.

---

### 6. Smarter Replanning
**Behavioral focus:** Predictive failure detection, backup targets, adaptive replanning

```java
public class SmarterReplanningAgent extends SmartTWAgent {

    private Int2D lastBroadcastExpiring = null;

    public SmarterReplanningAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        // Check if we have an active plan
        if (!getPlanner().hasPlan()) {
            return super.think();
        }

        Int2D currentGoal = getPlanner().getCurrentGoal();
        if (currentGoal == null) {
            return super.think();
        }

        // Reset lastBroadcastExpiring when we switch to a new target
        if (lastBroadcastExpiring != null
                && (lastBroadcastExpiring.x != currentGoal.x || lastBroadcastExpiring.y != currentGoal.y)) {
            lastBroadcastExpiring = null;
        }

        // PREDICTIVE 1: Check if someone else detected my target as expiring
        // Use manual comparison (see Int2D.equals() warning)
        boolean isExpiring = false;
        for (Int2D exp : expiringTargets) {
            if (exp.x == currentGoal.x && exp.y == currentGoal.y) {
                isExpiring = true;
                break;
            }
        }
        if (isExpiring) {
            getPlanner().voidPlan();
            return super.think();  // Replan immediately
        }

        // PREDICTIVE 2: Will target expire before I arrive? (my own detection)
        double currentTime = getEnvironment().schedule.getTime();
        double obsTime = getSmartMemory().getObservationTime(currentGoal.x, currentGoal.y);
        if (obsTime >= 0) {
            double age = currentTime - obsTime;
            double remainingLifetime = Parameters.lifeTime - age;
            int stepsToArrival = Math.abs(getX() - currentGoal.x) + Math.abs(getY() - currentGoal.y);

            // ADAPTIVE: Scale expiry threshold with lifetime for Config 3 robustness
            // Short lifetime → tighter threshold (0.7), Long lifetime → looser (0.9)
            double expiryThreshold = getSmartMemory().isShortLifetime() ? 0.7 : 0.9;
            if (stepsToArrival > remainingLifetime * expiryThreshold) {
                // Target will expire — broadcast ONCE and switch NOW
                // Check if we already broadcast this target to avoid duplicates
                // Note: We can't use expiringTargets because it skips own messages
                boolean alreadyBroadcast = (lastBroadcastExpiring != null
                        && lastBroadcastExpiring.x == currentGoal.x
                        && lastBroadcastExpiring.y == currentGoal.y);

                if (!alreadyBroadcast) {
                    getEnvironment().receiveMessage(
                        new Message(getName(), "", "EXPIRING:" + currentGoal.x + "," + currentGoal.y + ",lifetime")
                    );
                    lastBroadcastExpiring = new Int2D(currentGoal.x, currentGoal.y);
                }

                getPlanner().voidPlan();
                // NOTE: Don't clear lastBroadcastExpiring here — it persists to prevent re-broadcast
                //       It will be reset when a new plan is formed (see below)
                return super.think();
            }
        }

        // PREDICTIVE 3: Will I run out of fuel before completing round trip?
        Int2D fuelPos = getSmartMemory().getKnownFuelStation();
        if (fuelPos != null) {
            int stepsToGoal = Math.abs(getX() - currentGoal.x) + Math.abs(getY() - currentGoal.y);
            int stepsToFuel = Math.abs(currentGoal.x - fuelPos.x) + Math.abs(currentGoal.y - fuelPos.y);
            // ADAPTIVE: Scale safety margin with grid size for Config 3 robustness
            int safetyMargin = Math.max(50, getSmartMemory().getXDim() / 4);
            int totalCost = stepsToGoal + stepsToFuel + safetyMargin;

            if (getFuelLevel() < totalCost) {
                getPlanner().voidPlan();
                TWDirection dir = navigateTo(fuelPos.x, fuelPos.y, "fuel");
                if (dir != null) {
                    return new TWThought(TWAction.MOVE, dir);
                }
            }
        }

        // Execute the plan
        TWDirection planDir = getPlanner().execute();
        if (planDir != null) {
            return new TWThought(TWAction.MOVE, planDir);
        }

        return super.think();
    }

    @Override
    public void communicate() {
        super.communicate(); // Base handles ALL parsing

        // EXPIRING messages are sent in think() when targets are predicted to expire
        // Broadcasting in think() means EXPIRING arrives ONE STEP LATE to teammates
        // This is acceptable — agents avoid expiring targets starting next step
    }
}
```

**What makes this different:** Uses `expiringTargets` from base class (both to avoid others' expiring targets and to broadcast own detections), predicts failures before they happen, connected to Pollack & Ringuette's "adaptive cautious agent" research.

---

## ⚠️ Adaptive Threshold Strategy (Don't Overfit to Config 2!)

**Critical design principle:** Thresholds must be **adaptive based on runtime observations**, not hardcoded for specific configs. Config 3 is unknown — your agents must generalize.

### Current Adaptive Approach (Good Foundation)

The system already has runtime environment detection in `SmartTWAgentMemory`:

```java
memory.isShortLifetime()     // Detects if objects decay quickly (Config 2)
memory.isDense()             // Detects if objects are plentiful (Config 2)
memory.isLargeGrid()         // Detects grid size (Config 2: 80×80)
memory.getEstimatedLifetime() // Learns actual object lifetime from observations
```

These already drive adaptive behavior in `SmartTWPlanner`:
```java
// Short lifetime environments get tighter margins
double affordMult = memory.isShortLifetime() ? 1.3 : 1.5;
int affordBuffer = memory.isShortLifetime() ? 20 : 30;
int maxDist = memory.isShortLifetime() ? (int)(memory.getEstimatedLifetime() * 0.6) : Integer.MAX_VALUE;
```

### ✅ Implemented Adaptive Thresholds

The following hardcoded thresholds have been replaced with adaptive formulas in the specialist implementations:

**1. Fuel Scout's Refuel Threshold** (line 465)
```java
// ADAPTIVE: Scale with grid size
int refuelThreshold = getSmartMemory().isLargeGrid() ? 250 : 175;
```

**2. Smarter Replanning's Safety Margin** (line 1024)
```java
// ADAPTIVE: Scale with grid dimensions
int safetyMargin = Math.max(50, getSmartMemory().getXDim() / 4);
```

**3. Smarter Replanning's Expiry Threshold** (line 998)
```java
// ADAPTIVE: Scale with lifetime characteristics
double expiryThreshold = getSmartMemory().isShortLifetime() ? 0.7 : 0.9;
```

**4. Explorer's Search Radius** (line 721)
```java
// ADAPTIVE: Scale with grid size
int searchRadius = getSmartMemory().isLargeGrid() ? 20 : 10;
```

**5. Tile Hunter's Batching Target** (line 534)
```java
// ADAPTIVE: Scale with lifetime (avoid expiration in short-lived envs)
int tileTarget = getSmartMemory().isShortLifetime() ? 1 : 3;
```

**6. Hole Filler's Patrol Behavior** (line 616)
```java
// ADAPTIVE: Only patrol in dense environments
if (!getSmartMemory().isDense()) {
    return super.think(); // Skip patrol in sparse envs
}
```

### Generalization Strategy

**Don't tune for Config 2.** Instead:

1. **Make thresholds scale with observed characteristics:**
   - Grid size → scales distance/fuel thresholds
   - Object lifetime → scales expiration thresholds
   - Object density → affects exploration vs. exploitation tradeoff

2. **Use runtime learning to adjust (OPTIONAL - theoretical extension):**
   ```java
   // NOTE: This is a CONCEPTUAL EXAMPLE of runtime learning, NOT a required implementation.
   // Measuring "actuallyExpired" requires post-hoc observation (check if object still exists
   // after arriving), which adds complexity. The adaptive thresholds above are sufficient
   // for Config 3 robustness without this additional layer.

   // Example: Track expiration success rate and adjust threshold
   private int expirationPredictions = 0;
   private int expirationSuccesses = 0;

   if (predictedExpiring && actuallyExpired) {
       expirationSuccesses++;
   }
   expirationPredictions++;

   double successRate = (double) expirationSuccesses / expirationPredictions;
   if (successRate < 0.7) {
       // Predictions are too optimistic → lower threshold
       expirationThreshold -= 0.05;
   } else if (successRate > 0.9) {
       // Predictions are too conservative → raise threshold
       expirationThreshold += 0.05;
   }
   ```

3. **Test on BOTH configs during development:**
   - Config 1: Long lifetime, small grid (50×50), 500 fuel
   - Config 2: Short lifetime, large grid (80×80), 500 fuel
   - **If it works on both, it will likely generalize to Config 3**

### Calibration for Generalization

When testing, don't optimize for one config. Instead:

1. **Run both configs** and compare performance
2. **Look for threshold values that work reasonably well on BOTH** — not perfectly on one
3. **Prefer adaptive formulas over hardcoded values**
4. **Add runtime learning** if agents consistently mispredict in a new environment

**Target:** Agents that achieve ≥ 80% of optimal performance on BOTH Config 1 and Config 2, without config-specific tuning.

---

## Specialist Environment Awareness

**Core principle:** Each specialist should ask: *"Is my specialization profitable in this environment?"*

Custom logic should only activate when the environment matches the scenario it was designed for. If the environment doesn't match, fall through to `super.think()` rather than burning fuel on irrelevant behavior.

### General Pattern

```java
// In any specialist's think() method:
if (!getSmartMemory().isDense() && /* my logic only helps in dense envs */) {
    return super.think(); // Not my environment — defer to base agent
}
```

### Per-Specialist Guidance

**1. Fuel Scout** — Always relevant
- Fuel discovery is critical in all environments
- Search radius already adapts: `isLargeGrid() ? 20 : 10`
- Refuel threshold already adapts: `isLargeGrid() ? 250 : 175`

**2. Tile Hunter** — Adapts batching strategy
- **Short lifetime (`isShortLifetime()`):** Deliver immediately with 1 tile to avoid expiration
- **Long lifetime:** Batch 3 tiles for efficiency (3 deliveries worth of fuel for 1 trip)
- Implementation: `int tileTarget = isShortLifetime() ? 1 : 3;`

**3. Hole Filler** — Skip patrol in sparse environments
- **Dense environments (`isDense()`):** Patrol near holes (PRIORITY 1 patrol logic)
- **Sparse environments:** Skip patrol — holes are too far apart, camping wastes fuel
- **Implementation:** Wrap the patrol block with `if (isDense())` check
- **Important:** Delivery logic (carrying tile → find hole) should ALWAYS run, regardless of density

**4. Explorer** — Adapts search radius
- **Large grids:** Use `searchRadius = 20` to find next unvisited area
- **Small grids:** Use `searchRadius = 10` (sufficient, faster execution)
- Already implemented: `int searchRadius = isLargeGrid() ? 20 : 10;`

**5. Delivery Optimizer** — Naturally degrades in sparse environments
- **Dense environments:** Hotspot routing provides cluster-density bonus
- **Sparse environments:** No HOTSPOT messages arrive → `hotspots` list stays empty → density bonus = 0 → naturally degrades to nearest-hole routing
- **No guard needed** — the logic self-adapts without explicit check

**6. Smarter Replanning** — Already fully adaptive
- Expiry threshold adapts: `isShortLifetime() ? 0.7 : 0.9`
- Safety margin adapts: `Math.max(50, getXDim() / 4)`
- All thresholds scale with runtime observations

### Why This Matters for Config 3

Config 3 is unknown. It could be:
- A tiny dense grid with very short lifetimes
- A massive sparse grid with long lifetimes
- Something we haven't considered

By making specialists **environment-aware**, we ensure they don't execute counter-productive specialization logic when the environment doesn't reward it. Each specialist degrades gracefully to base agent behavior when their specialization isn't profitable.

---

## Why the Improvements Compound

### Shared Infrastructure Benefits All Agents

Because algorithmic improvements are implemented in **Layer 1 (shared code)**:

- The fuel-aware A* pathfinding improves movement efficiency for **all 6 agents**
- The zone management system helps **everyone** avoid redundant exploration
- The persistent claim system prevents **all 6 agents** from wasting steps on the same target
- The memory decay system ensures **everyone** operates on fresh information

### Information Broadcast Amplifies Individual Discoveries

Because each specialist **broadcasts their findings**:

- The Fuel Scout's station discovery gives **everyone** a refueling target
- The Explorer's tile/hole sightings populate the **shared map** that all agents use
- The Delivery Optimizer's hotspot broadcasts help **everyone** bias movement toward productive zones
- The Smarter Replanning's failure predictions help **everyone** avoid wasted trips

### Result: Shared Algorithms + Individual Information Discovery → Team Performance

The combination is multiplicative, not additive. Each agent's individual improvements feed into the shared infrastructure, which raises the baseline performance for everyone. Then everyone's individual discoveries broadcast through the message bus, compounding the effect.

---

## The Message Schema (LOCKED — String-Based Format)

**This is shared infrastructure — agreed upon before implementing specializations.**

The current `Message.java` uses **string-based messages**. The format is:

```
"TYPE:payload1,payload2,..."
```

### All 8 Message Types

| Message Type | String Format | Example | Status | Who Broadcasts | Who Consumes |
|---|---|---|---|---|---|
| **FUEL** | `"FUEL:x,y"` | `"FUEL:34,12"` | ✅ **Implemented** | Everyone (when station found) | Everyone (updates memory) |
| **TILE** | `"TILE:x,y,time"` | `"TILE:15,7,234.5"` | ✅ **Implemented** | Everyone (new tile sighting) | Everyone (updates memory) |
| **HOLE** | `"HOLE:x,y,time"` | `"HOLE:20,8,234.5"` | ✅ **Implemented** | Everyone (new hole sighting) | Everyone (updates memory) |
| **GONE** | `"GONE:x,y"` | `"GONE:15,7"` | ✅ **Implemented** | Everyone (object vanished) | Everyone (clears memory) |
| **CLAIM** | `"CLAIM:x,y"` | `"CLAIM:15,7"` | ✅ **Implemented** | Everyone (current planner target) | Everyone (skips claimed) |
| **LOW** | `"LOW:agentId,fuel"` | `"LOW:Agent3,120"` | ⚠️ **To be added** | Fuel Scout specialist | Teammates (adjust coverage) |
| **EXPIRING** | `"EXPIRING:x,y,reason"` | `"EXPIRING:15,7,lifetime"` | ⚠️ **To be added** | Smarter Replanning specialist | Everyone (avoid that target) |
| **HOTSPOT** | `"HOTSPOT:x,y,density"` | `"HOTSPOT:25,15,3.5"` | ⚠️ **To be added** | Delivery Optimizer specialist | Explorers (bias movement) |
| **SWAP** | `"SWAP:agentName,status"` | `"SWAP:Explorer3,ZONE_DONE"` | ⚠️ **To be added** | Explorer specialist | Teammates (reassign zones) |

### How to Send Messages

In your agent's `communicate()` method:

```java
@Override
public void communicate() {
    super.communicate(); // Always call base first (broadcasts FUEL/TILE/HOLE/GONE/CLAIM)

    // Add your custom messages
    if (condition) {
        getEnvironment().receiveMessage(new Message(getName(), "", "LOW:" + getName() + "," + getFuelLevel()));
    }
}
```

### How to Receive Messages

The base `SmartTWAgent.communicate()` parses ALL 8 message types and stores them in protected fields:

| Message Type | Parsed By | Stored In | Available To |
|---|---|---|---|
| FUEL, TILE, HOLE, GONE, CLAIM | Base class | Memory (via `smartMemory`) | Everyone (via memory methods) |
| LOW, EXPIRING, HOTSPOT, SWAP | Base class (after update) | `lowFuelAgents`, `expiringTargets`, `hotspots` (HotspotEntry list), `zoneSwapRequests` | Everyone (direct field access) |

**Specialists don't need to parse anything — just use the data:**

```java
public class YourSpecialistAgent extends SmartTWAgent {

    public YourSpecialistAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        // Use parsed message data directly — no need to re-parse!

        // Example: Avoid expiring targets (use manual comparison for Int2D)
        if (!expiringTargets.isEmpty()) {
            Int2D myTarget = getPlanner().getCurrentGoal();
            if (myTarget != null) {
                boolean isExpiring = false;
                for (Int2D exp : expiringTargets) {
                    if (exp.x == myTarget.x && exp.y == myTarget.y) {
                        isExpiring = true;
                        break;
                    }
                }
                if (isExpiring) {
                    getPlanner().voidPlan(); // Avoid expiring target
                }
            }
        }

        // Example: Check for low-fuel teammates
        if (!lowFuelAgents.isEmpty()) {
            // Consider helping fuel-scarce teammates
            // ...
        }

        // Example: Use hotspot data (HotspotEntry contains position + density)
        if (!hotspots.isEmpty()) {
            for (HotspotEntry entry : hotspots) {
                Int2D pos = entry.position;
                double density = entry.density;
                // Use this data for routing decisions
            }
        }

        return super.think();
    }

    @Override
    public void communicate() {
        super.communicate(); // Base handles ALL parsing automatically

        // Just broadcast your custom messages
        if (yourCondition) {
            getEnvironment().receiveMessage(new Message(getName(), "", "YOUR_TYPE:payload"));
        }
    }
}
```

**That's it — no parsing code needed in specialists!**

---

## Why This Satisfies the Assignment Requirement

From `group-project.pdf`:
> "Each student in a team needs to design one agent"

Our approach satisfies this because:

1. **Each student has their own agent file** (FuelScoutAgent.java, TileHunterAgent.java, etc.)
2. **Each agent has genuinely different behavioral logic** implemented via `think()` override
3. **The designs are different algorithms** solving different subproblems:
   - Fuel Scout → aggressive station discovery
   - Tile Hunter → capacity-based collection
   - Hole Filler → hole-patrol strategy
   - Explorer → systematic zone coverage
   - Delivery Optimizer → cluster-density routing
   - Smarter Replanning → predictive failure detection

4. **The designs are built to work together** through shared infrastructure and information broadcast
5. **The report can show 6 distinct designs** with clear behavioral differences and performance contributions

---

## Implementation Order

### Step 1: Team Alignment + Base Class Updates (One Meeting)

**CRITICAL:** Base class updates must be completed FIRST before anyone starts implementing specialists.

**Two required base class updates:**
1. **Step 0** (above): Add `getPlanner()` getter, make `navigateTo()` and `exploreGreedy()` protected (~3 lines changed, 1 method added)
2. **Step 1** (below): Add custom message parsing fields and logic (~40 lines)

**Workflow:**
- Person 1 completes BOTH Step 0 and Step 1 (~5 minutes total) → commits to repository
- Everyone verifies the commit → then starts implementing specialists in parallel

**Why both updates are needed:**
- Step 0: Specialist examples use `navigateTo()`, `exploreGreedy()`, and `getPlanner()` — these must be accessible
- Step 1: Custom message types (LOW, EXPIRING, HOTSPOT, SWAP) must be parsed so specialists can use them

All team members review and agree on:
1. **Message Schema** — The 8 message types are LOCKED (see "The Message Schema" section). Do not change formats without team consensus.
2. **Agent File Naming** — Decide naming convention (e.g., `FuelScoutAgent.java`, `TileHunterAgent.java`, etc.)
3. **Specialization Assignment** — Each person chooses one of the 6 specializations
4. **Base class updater** — ONE person volunteers to complete Steps 0 and 1

**After BOTH base class updates are committed, everyone can work in parallel.**

### Step 2: Parallel Implementation

Each team member creates their agent file (e.g., `FuelScoutAgent extends SmartTWAgent`):

| Person | Specialization | Override `think()` | Broadcast | Use Parsed Data |
|---|---|---|---|---|
| Person 1 | Fuel Scout | Lower fuel threshold (200) | `LOW` | Optional |
| Person 2 | Tile Hunter | Ignore holes until 3 tiles | (None) | Optional |
| Person 3 | Hole Filler | Prioritize holes, patrol | (None) | Optional |
| Person 4 | Explorer | Systematic zone coverage | `SWAP` | `hotspots` (HotspotEntry) for routing bias |
| Person 5 | Delivery Optimizer | Cluster-density routing | `HOTSPOT` | `hotspots` (HotspotEntry) for density bonus |
| Person 6 | Smarter Replanning | Predictive failure detection | `EXPIRING` | `expiringTargets` (manual comparison) |

**Data usage:**
- **Everyone can access:** `lowFuelAgents`, `expiringTargets`, `zoneSwapRequests`
- **Only Explorer/DeliveryOptimizer need:** `hotspots` (HotspotEntry list for routing)
- **Tile Hunter/Hole Filler:** May ignore message data (simple agents)
- **All agents should use manual comparison for Int2D** (see Int2D.equals() warning)

**Implementation pattern for everyone:**

```java
public class YourSpecialistAgent extends SmartTWAgent {

    public YourSpecialistAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel, int agentIndex) {
        super(name, xpos, ypos, env, fuelLevel, agentIndex);
    }

    @Override
    protected TWThought think() {
        // PRIORITY 0: Your specialist logic (inserts BEFORE base logic)
        if (yourSpecialCondition()) {
            return yourSpecialAction();
        }

        // PRIORITY 1: Use parsed message data (OPTIONAL)
        if (!expiringTargets.isEmpty()) {
            // Example: Avoid expiring targets (use manual comparison)
            Int2D myTarget = getPlanner().getCurrentGoal();
            if (myTarget != null) {
                for (Int2D exp : expiringTargets) {
                    if (exp.x == myTarget.x && exp.y == myTarget.y) {
                        getPlanner().voidPlan(); // Avoid expiring target
                        break;
                    }
                }
            }
        }

        // Fall through to base agent logic
        return super.think();
    }

    @Override
    public void communicate() {
        super.communicate(); // Base handles ALL parsing + FUEL/TILE/HOLE/GONE/CLAIM broadcast

        // Only add your custom broadcasts
        if (yourCondition) {
            getEnvironment().receiveMessage(new Message(getName(), "", "YOUR_TYPE:payload"));
        }
    }
}
```

### Step 3: Integration and Testing

1. **Register all 6 agents** in `TWEnvironment.main()` or your runner class
2. **Run Config 1** — Check for conflicts, verify all agents score points
3. **Run Config 2** — Verify adaptivity to dense/volatile environment
4. **Debug message conflicts** — Use logging to trace message flow if issues arise
5. **Tune thresholds** — Adjust fuel levels, safety margins based on performance
6. **Document** — Each person writes their agent's design section for the report

---

## Quick Reference: Base Agent Decision Flow

The `SmartTWAgent.think()` priority order (your specialist can insert logic at any point):

1. **FUEL STATION UNKNOWN** — Explore to find it (conserve fuel on large grids)
2. **REFUEL** — If on fuel station and not full
3. **FUEL EMERGENCY** — Navigate to fuel station (must succeed, never falls through)
4. **OPPORTUNISTIC DROP** — On a hole while carrying tile
5. **OPPORTUNISTIC PICKUP** — On a tile while carrying < 3
6. **OPPORTUNISTIC REFUEL** — Near fuel station and fuel < 70%
7. **TILE BATCHING** — Pick up nearby tile before delivering (dense envs only)
8. **DELIVER** — Carrying tile, seek hole via planner
9. **SEEK TILE** — Not full, seek affordable tile via planner
10. **EXPLORE** — Systematic lawnmower pattern
11. **WAIT** — No valid action

**To insert your specialist logic:** Add checks BEFORE the priority you want to override. For example, Fuel Scout inserts at step 2 (before REFUEL) to refuel at 40% instead of waiting for emergency.

---

## Research Connection (For Report)

This strategy connects directly to Pollack & Ringuette (1990) on **bold vs. cautious** agents:

- **Bold agent**: Commits to plans, sticks with them
- **Cautious agent**: Reconsiders frequently, abandons plans easily

Our **Smarter Replanning** specialist embodies the **adaptive cautious agent**:
- Config 1 (stable, long lifetime): More bold, commits to longer plans
- Config 2 (volatile, short lifetime): More cautious, replans aggressively
- **Predictive switching**: Doesn't wait for plans to fail — switches before they fail

This is exactly the kind of environment-aware deliberation strategy the original TileWorld research was exploring.
