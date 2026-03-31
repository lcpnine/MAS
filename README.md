# TileWorld Multi-Agent System — AI6125 Project Guide

## 1. Project Overview

This is the group project for AI6125: Multi-agent System at NTU. Your team of 6 students must each design and implement one agent in Java using the MASON simulation toolkit. The 6 agents operate together in a shared TileWorld environment, competing against other teams' agent squads across multiple configurations.

TileWorld was originally introduced by Pollack and Ringuette (1990) as a testbed for experimentally evaluating agent architectures in dynamic, unpredictable environments. It has since become one of the most widely used benchmarks in multi-agent systems research, valued for its high parameterisability and conceptual simplicity (Lees, 2002). In the NTU version, agents must also manage a finite fuel supply and discover hidden fuel stations — features inspired by the 1994 revision by Pollack et al. that added maintenance goals and resource management to the original design.

The grade depends on the **report** (40% of the course), the **presentation and demonstration** (20% of the course), and a live competition. Design quality, analysis, and communication matter more than winning.

---

## 2. Environment Specification (from `group-project.pdf`)

The TileWorld is a grid containing five entity types:

| Entity | Behaviour |
|---|---|
| **Agent** | Moves one cell per step (up/down/left/right). Can pick up tiles, drop tiles into holes, and refuel. |
| **Tile** | A unit square that can be carried by an agent (max 3 tiles at once). |
| **Hole** | A cell that can be filled by a tile. When filled, the tile and hole disappear, leaving a blank cell, and the agent earns 1 reward. |
| **Obstacle** | Immovable. Agents cannot occupy the same cell. |
| **Fuel Station** | Fixed position, randomly generated at simulation start. Position is **unknown** to agents — must be discovered. |

**Dynamic environment:** Tiles, holes, and obstacles appear randomly according to a normal distribution and persist for a configurable lifetime before vanishing. Fuel stations do not disappear.

**Fuel:** Every movement action consumes 1 fuel. Starting fuel is 500. At zero fuel, the agent is permanently stuck. The REFUEL action restores fuel when the agent is at a discovered fuel station.

**Perception:** Limited to a 7×7 neighbourhood. An object at position (X, Y) is visible to an agent at (x, y) only when `max(abs(x − X), abs(y − Y)) ≤ 3`.

**Actions available:** `{WAIT, MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, PICK_UP, DROP, REFUEL}`

**Agent loop:** Each time step follows the cycle: **Sense → Communicate (optional) → Plan → Act**

---

## 3. Competition Configurations

Each experiment runs for **5,000 steps**. Ten experiments per configuration, averaged. Two configurations are known; one is secret.

| Parameter | Config 1 | Config 2 | Config 3 |
|---|---|---|---|
| Grid size | 50 × 50 | 80 × 80 | ? × ? |
| Object creation rate | Normal(μ=0.2, σ=0.05) | Normal(μ=2, σ=0.5) | ? |
| Object lifetime | 100 steps | 30 steps | ? |

**Config 1** is a sparse, stable environment — few objects appear per step, and they persist for a long time. Agents have breathing room to plan carefully.

**Config 2** is dense and volatile — objects flood the grid but vanish quickly. Agents must react fast and cannot rely on stale memory. The larger grid (80×80) also means more ground to cover with the same 500 fuel.

**Config 3** is the wildcard. Your agents must generalise across different grid sizes, spawn rates, and lifetimes. Avoid hardcoding thresholds to any single configuration.

---

## 4. Implementation Constraints

These are explicitly stated in the project brief and the lecture slides. Violations lead to grade penalties.

**You MUST:**
- Extend `TWAgent` to create your own agent class.
- Override only `communicate()`, `think()`, and `act()`.
- Implement a planning module (required). Memory and communication modules are optional but strongly recommended.
- Call `receiveMessage()` inside `communicate()` so the message is added to the environment's broadcast list.

**You MUST NOT:**
- Override other methods of `TWAgent` (e.g., `sense()`, `putTileInHole()`, or any method in `TWAgentSensor`).
- Modify anything in the `environment` package.
- Call `increaseReward()` outside of `putTileInHole()`.

**Codebase structure (from lecture slides):**

```
tileworld/
├── tileworld.agent/
│   ├── TWAgent.java          ← extend this
│   ├── Message.java          ← extend for custom messages
│   ├── TWAgentWorkingMemory.java  ← extend for custom memory
│   ├── SimpleTWAgent.java    ← reference example
│   └── ...
├── tileworld.planners/
│   ├── TWPlanner.java        ← interface to implement
│   ├── DefaultTWPlanner.java ← reference planner
│   ├── AstarPathGenerator.java
│   ├── TWPath.java / TWPathStep.java
│   └── ...
├── tileworld.environment/    ← DO NOT MODIFY
└── tileworld.exceptions/
```

---

## 5. The Three Modules

### 5.1 Planning Module (Required)

The planning module is called inside `think()`. It reads the agent's updated memory and decides the next action.

**Recommended approach: Rule-based priority planning.** The original Pollack & Ringuette (1990) experiments showed that even simple deliberation strategies — such as scoring holes by a subjective expected utility (SEU) function — significantly outperformed naive approaches. Their SEU estimator for a hole *h* was:

```
SEU(h) = score(h) / (dist(agent, h) + tileavail(h))
```

where `tileavail(h)` sums twice the distance from each of the *n* nearest tiles to the hole (accounting for the round-trip of fetching each tile). In our NTU variant, every hole is worth 1 reward, so the scoring simplifies to minimising delivery cost.

**Suggested decision hierarchy (highest to lowest priority):**

1. **Fuel emergency** — If fuel is below a safe threshold (enough to reach the nearest known fuel station + margin), navigate to refuel immediately.
2. **Drop tile** — If carrying a tile and standing on or adjacent to a known hole, execute DROP.
3. **Deliver** — If carrying a tile and a reachable hole exists within fuel budget, move toward that hole.
4. **Pick up** — If a nearby tile exists and a viable tile→hole→fuel-station round trip is affordable, move to pick it up.
5. **Respond to teammate request** — If a help message has been received and the agent can assist, move toward the requested location.
6. **Explore** — Move toward unexplored regions, prioritising the agent's assigned zone.
7. **Wait** — If no productive action exists, conserve fuel.

**Reactivity:** Before executing any planned step, verify the target still exists in memory. If the target tile or hole has expired (exceeded its lifetime) or has been invalidated by a teammate message, abandon the plan and re-evaluate from rule 1. Additionally, path validity is checked before each move — if the next cell on the A* path is now blocked by a new obstacle, the plan is voided and replanning is triggered immediately instead of wasting a step on a `CellBlockedException`. Hole lifetime is also validated during planning: candidates whose estimated remaining lifetime is too short relative to travel cost are skipped, avoiding wasted trips in volatile environments like Config 2.

**Commitment vs. reactivity tradeoff:** Pollack & Ringuette's Experiment 2 showed that in slower environments, less filtering (more willingness to reconsider plans) yielded better results. In faster environments, the benefit of filtering was ambiguous. For Config 1 (slow, stable), lean toward cautious replanning. For Config 2 (fast, volatile), lean toward committing to short plans and finishing them.

### 5.2 Memory Module (Optional but Recommended)

Extend `TWAgentWorkingMemory` to build a richer world model.

**What to store:**
- Tile positions with the time step they were last observed.
- Hole positions with the time step they were last observed.
- Fuel station locations (permanent once discovered).
- Obstacle positions (may expire — track lifetime).
- Teammate positions and states (from messages).
- Regions already explored (to avoid redundant coverage).

**Staleness policy:** Given that objects have a finite lifetime (100 steps in Config 1, 30 in Config 2), any memory entry older than the configured lifetime should be treated as expired. For Config 3, where the lifetime is unknown, use a conservative decay — trust recent observations and discount older ones.

**Memory from communication:** When a teammate broadcasts a tile or hole sighting, add it to memory with the sender's observation timestamp. This effectively extends each agent's 7×7 vision to a team-wide shared map.

### 5.3 Communication Module (Optional but Recommended)

Extend the `Message` class to encode richer information. In `communicate()`, call `receiveMessage()` on the `TWEnvironment` to add your message to the broadcast list. All messages are broadcast to every agent — there is no range limit — though you can specify an intended recipient in the message payload.

To retrieve all messages from the current step, call `getMessages()` on the `TWEnvironment` class during the planning phase.

**Recommended message types:**

| Type | Payload | When to Send |
|---|---|---|
| `FUEL_STATION_FOUND` | (x, y) coordinates | Agent steps on or senses a fuel station |
| `TILE_SIGHTED` | (x, y), observation timestamp | Agent sees a new tile |
| `HOLE_SIGHTED` | (x, y), observation timestamp | Agent sees a new hole |
| `OBJECT_GONE` | (x, y), entity type | Agent arrives at a remembered location and finds it empty |
| `HELP_REQUEST` | tile (x, y), agent fuel level, nearest known hole | Agent finds a tile but cannot deliver it |
| `CLAIMING_TARGET` | target (x, y), entity type | Agent commits to a tile or hole, preventing duplicated effort |
| `ZONE_ASSIGNMENT` | agent ID, zone boundaries | At startup or when redistributing exploration areas |

**Avoiding duplicated work:** The `CLAIMING_TARGET` message is particularly important. Without it, multiple agents may pathfind toward the same tile-hole pair, wasting fuel. When an agent claims a target, others should remove it from their candidate list.

---

## 6. Design Strategies

### 6.1 Spatial Partitioning

Divide the grid into 6 zones (one per agent). In Config 1 (50×50), each agent covers roughly 417 cells. In Config 2 (80×80), roughly 1,067 cells.

**Simple approach:** Divide into 6 horizontal strips. Agent *i* explores rows `[i * H/6, (i+1) * H/6)`.

**Better approach:** Use a Voronoi-like partition based on agent spawn positions, reassigned periodically via `ZONE_ASSIGNMENT` messages. The closest agent to each region "owns" it.

Partitioning ensures that the team covers the grid efficiently and discovers fuel stations faster.

### 6.2 Fuel-Aware Pathfinding

Use the provided `AstarPathGenerator` but wrap every pathfinding call with a fuel feasibility check:

```
fuel_needed = cost(current → target) + cost(target → nearest_known_fuel_station) + SAFETY_MARGIN
if agent.fuel < fuel_needed:
    navigate to fuel station instead
```

The `SAFETY_MARGIN` should account for obstacles that may lengthen the actual path beyond the Manhattan distance estimate.

### 6.3 Adapting to Configuration Differences

| Aspect | Config 1 (Sparse/Stable) | Config 2 (Dense/Volatile) |
|---|---|---|
| **Planning horizon** | Longer plans are viable (objects last 100 steps) | Short plans only (objects last 30 steps) |
| **Memory trust** | High — entries stay valid longer | Low — entries expire quickly |
| **Fuel urgency** | Lower — fewer moves needed, objects nearby | Higher — larger grid, more movement per reward |
| **Communication value** | Moderate — fewer sightings to share | High — rapid discovery-sharing prevents wasted trips |
| **Exploration priority** | Higher — objects are rare, must find them | Lower — objects are everywhere, focus on delivery |

Our agents detect the configuration at runtime using observation-based classification rather than reading `Parameters` directly (which would fail for Config 3). Density is estimated by averaging objects seen per sensor step (threshold: >8.0 = dense). Object lifetime is estimated by tracking how long observed objects persist before disappearing (threshold: <50 = short). Grid size is read from the actual environment dimensions. During a warmup period (~30 steps for density, ~5 disappearances for lifetime), agents default to conservative sparse/long-lifetime behavior.

---

## 7. Theoretical Grounding for the Report

The report should include a background section on TileWorld. Key concepts from the literature to reference:

**From Pollack & Ringuette (1990):**
- TileWorld as a parameterised testbed for evaluating agent reasoning in dynamic environments.
- The distinction between **filtering** (restricting which options the agent considers) and **deliberation** (choosing among surviving options). Their experiments showed that how much an agent reconsiders its plans should depend on the rate of environmental change.
- The **bold vs. cautious** spectrum: a bold agent sticks to its current plan; a cautious agent reconsiders frequently. Their results suggested caution is generally better when deliberation is cheap.

**From Lees (2002):**
- The evolution of TileWorld from a single-agent testbed to MA-TileWorld (Ephrati et al., 1995), which introduced multi-agent coordination and filtering strategies for teams.
- The four properties of a good simulated testbed (Kinny, 1990): rich objects/events, convenient performance metrics, tunable parameters mapping to real-world properties, and the ability to generate statistically similar worlds.
- The criticism that TileWorld's simplicity can be a limitation — strong performance in TileWorld does not guarantee strong general-purpose agent design. Acknowledge this in your report's limitations section.

**From Pollack et al. (1994):**
- The addition of fuel and maintenance goals, which is directly relevant to our NTU variant. This forced agents to balance task completion with self-preservation, making the environment more realistic.

---

## 8. Assessment Deliverables

### Report (40% of course grade)
- Maximum **2,000 words and 8 pages** (including images/plots).
- Must include: background on TileWorld, detailed agent design descriptions, communication and collaboration strategy, experimental results with analysis.
- Due: Sunday 11:59 PM, Week 13. **5 marks deducted per calendar day late.**

### Presentation & Demonstration (20% of course grade)
- **15–20 minutes**, team-based.
- Scheduled in Week 12 and Week 13.
- Includes a live demonstration of your agents in the competition.

### Presentation Tips
- Lead with architecture: show the Sense→Communicate→Plan→Act cycle and how your three modules interact.
- Walk through a concrete scenario (e.g., agent discovers fuel station → broadcasts → teammate refuels → delivers tile).
- Show adaptability: demonstrate what happens when a target hole vanishes mid-plan.
- Compare your results across Config 1 and Config 2 and explain why performance differs.

### Report Tips
- Justify every design decision with reasoning, not just description. Why rule-based over utility-based? Why this fuel threshold? Reference the Pollack & Ringuette findings where applicable.
- Include a communication protocol table (message types, payloads, triggers).
- Discuss failure modes and limitations honestly. What happens when no fuel station is found early? When all agents cluster in one area?
- Show quantitative results: average reward across 10 runs per configuration, with variance.

---

## 9. Implementation Roadmap

### Phase 1 — Solo Agent Survival
- Set up Java JDK 1.8, Java3D 1.5, MASON_14.jar, Eclipse IDE.
- Extend `TWAgent`, override `think()` and `act()`.
- Implement basic movement with obstacle avoidance.
- Build memory module to record discovered tiles, holes, fuel stations.
- Implement fuel-check rule: always refuel before running dry.
- **Milestone:** A single agent that explores, finds fuel stations, and survives the full 5,000 steps.

### Phase 2 — Solo Scoring
- Implement the planning module with the priority rule hierarchy.
- Integrate `AstarPathGenerator` for pathfinding with fuel-awareness.
- Agent picks up tiles and delivers them to holes.
- Add reactivity: abandon plan if target disappears from memory.
- **Milestone:** A single agent that reliably scores rewards in both Config 1 and Config 2.

### Phase 3 — Team Communication
- Implement the communication module with custom `Message` subclass.
- Broadcast fuel station discoveries and tile/hole sightings.
- Implement zone-based exploration so 6 agents spread out.
- Add `CLAIMING_TARGET` messages to prevent duplicated effort.
- **Milestone:** Six agents coordinate exploration and share discoveries.

### Phase 4 — Polish and Experiment
- Implement help-request protocol for cooperative tile delivery.
- Tune fuel thresholds, staleness decay, and safety margins for both known configurations.
- Run 10-experiment batches for Config 1 and Config 2, collect average reward and variance.
- Add runtime configuration detection for Config 3 adaptability.
- Prepare presentation and write report.
- **Milestone:** Competition-ready team with documented results.

---

## 10. Quick-Reference Decision Flowchart

```
START (each time step)
  │
  ├─ SENSE: observe 7×7 neighbourhood, update memory
  ├─ COMMUNICATE: broadcast discoveries, read teammate messages
  │
  ▼
PLAN:
  │
  Is fuel critically low?
  ├─ YES → Path to nearest known fuel station → ACT: move or REFUEL
  │         (If none known → explore toward unvisited area)
  │
  Am I on a hole and carrying a tile?
  ├─ YES → ACT: DROP
  │
  Am I carrying a tile with a reachable hole?
  ├─ YES → ACT: move toward hole
  │
  Is there a tile I can pick up, deliver, and still refuel?
  ├─ YES → Is a teammate already claiming it?
  │         ├─ YES → skip, find another
  │         └─ NO  → Broadcast CLAIMING_TARGET → ACT: move toward tile / PICK_UP
  │
  Is there a pending HELP_REQUEST I can fulfil?
  ├─ YES → ACT: move to assist
  │
  ▼
  Explore assigned zone → ACT: move toward nearest unvisited area
```

---

## 11. References

- Pollack, M. E. & Ringuette, M. (1990). Introducing the Tileworld: Experimentally evaluating agent architectures. *AAAI National Conference on Artificial Intelligence*, 183–189.
- Lees, M. (2002). A history of the Tileworld agent testbed. *University of Nottingham Technical Report NOTTCS-WP-2002-1*.
- Pollack, M. E., Joslin, D., Nunes, A., Ur, S., & Ephrati, E. (1994). Experimental investigation of an agent commitment strategy. *Technical Report 94-31*, University of Pittsburgh.
- Ephrati, E., Pollack, M., & Ur, S. (1995). Deriving multi-agent coordination through filtering strategies. *IJCAI-95*, 679–685.
- Kinny, D. & Georgeff, M. (1991). Commitment and effectiveness of situated agents. *IJCAI-91*, 82–88.
- Bratman, M. E., Israel, D. J., & Pollack, M. E. (1988). Plans and resource-bounded practical reasoning. *Computational Intelligence*, 4(4), 349–355.
