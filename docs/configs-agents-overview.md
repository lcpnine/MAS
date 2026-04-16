# TileWorld Configs, Agents, and Team Cooperation

## Project Overview

This project is a six-agent **TileWorld multi-agent system** built for **NTU AI6125** using the **MASON** simulation framework. The team competes in a dynamic grid world where agents must:

- explore the map with limited vision
- find the hidden fuel station
- collect tiles
- fill holes to score reward
- avoid wasting fuel, time, and duplicated effort

The system is built around a **shared base architecture** plus **specialist agents**.

- The shared base is implemented in `SmartTWAgent`, `SmartTWAgentMemory`, and `SmartTWPlanner`.
- The specialists extend that base and change decision priorities for different jobs.

This design matters because one improvement to the base layer improves all six agents at once.

## What Are Config 1 and Config 2?

The repo defines two known competition configurations.

| Config | Grid | Object Rate | Lifetime | What it means in practice |
|---|---:|---:|---:|---|
| **Config 1** | 50x50 | 0.2 | 100 | Sparse and stable. Objects are rarer but last longer, so careful planning works well. |
| **Config 2** | 80x80 | 2.0 | 30 | Dense and volatile. Many objects appear, but they disappear quickly and the larger map makes fuel much harder. |

### Config 1

`Config 1` comes from `Parameters.java`.

- Grid size: `50 x 50`
- Tile mean: `0.2`
- Hole mean: `0.2`
- Obstacle mean: `0.2`
- Object lifetime: `100`

This is the easier environment for long-term planning. Agents have more time to reach remembered targets, and stale information is less dangerous.

What those words mean:

- **Tile mean `0.2`**: on average, about `0.2` new tiles are generated per simulation step. The environment uses a random normal distribution, so some steps create `0`, some create `1`, and occasionally more, but the long-run average is around `0.2`.
- **Hole mean `0.2`**: on average, about `0.2` new holes are generated per step.
- **Obstacle mean `0.2`**: on average, about `0.2` new obstacles are generated per step.
- **Object lifetime `100`**: once a tile, hole, or obstacle is created, it stays in the world for about `100` simulation steps before disappearing.

### Config 2

`Config 2` comes from `Parameters2.java`.

- Grid size: `80 x 80`
- Tile mean: `2.0`
- Hole mean: `2.0`
- Obstacle mean: `2.0`
- Object lifetime: `30`

This is the harder environment for reliability. The map is larger, fuel is tighter, and objects often disappear before an agent arrives.

What those words mean here:

- **Tile mean `2.0`**: around `2` new tiles are created per step on average.
- **Hole mean `2.0`**: around `2` new holes are created per step on average.
- **Obstacle mean `2.0`**: around `2` new obstacles are created per step on average.
- **Object lifetime `30`**: objects disappear much faster, so an agent may remember a target that is already gone by the time it reaches it.

This is why Config 2 feels crowded but unstable: many things appear, but they do not stay long.

## How the Project Co-Works

The team uses a **two-layer architecture**.

### Layer 1: Shared Foundation

All agents inherit the same core capabilities:

- **A* pathfinding** for navigation
- **fuel-aware planning** so agents avoid impossible trips
- **shared memory updates** for tiles, holes, and fuel station knowledge
- **claim tracking** to reduce duplicate targeting
- **message parsing and broadcasting**
- **environment detection** for dense, short-lifetime, and large-grid conditions

This shared base gives every agent the same core survival and planning skills.

### Layer 2: Specialist Agents

Each agent then focuses on a different role. Instead of making six completely separate systems, the project combines:

- one common infrastructure
- six role-specific decision styles

That balance improves both maintainability and performance. The team gets specialization without losing consistency.

## Common Features of All Agents

All six specialist agents inherit the same common features from the shared base classes. This is the part that makes them behave like one family of agents instead of six unrelated programs.

### Shared Agent Loop

Every step follows the same basic loop:

1. **Sense**
2. **Communicate**
3. **Think**
4. **Act**

In practice, this means:

- the agent observes nearby cells
- it updates memory with what it saw
- it reads team messages and sends its own messages
- it decides the next action
- it moves, picks up a tile, drops a tile into a hole, refuels, or waits

### Shared Memory

All agents use `SmartTWAgentMemory`.

This memory stores:

- remembered tiles
- remembered holes
- discovered fuel station location
- observation times for freshness checking
- claimed targets from teammates
- recently visited cells for exploration
- runtime signals used to classify the environment

The memory is important because agents cannot see the whole map. They must act based on remembered objects and teammate broadcasts.

### Shared Navigation and Planning

All agents use A* pathfinding through the shared planner stack.

The common planning logic includes:

- path generation toward tiles, holes, and fuel
- affordability checks before committing to a trip
- path invalidation if a target disappears
- replanning when a path becomes blocked
- shorter planning horizons in short-lifetime environments

So even before specialization, every agent already knows how to survive, route, and avoid obviously bad trips.

### What A* Pathfinding Means

**A\***, pronounced “A star,” is a pathfinding algorithm used to find a good route from one cell to another.

In this project, A* is used when an agent wants to move toward:

- a tile
- a hole
- the fuel station
- an exploration target

How it works at a high level:

1. it starts from the agent’s current position
2. it explores possible neighboring cells
3. it prefers routes that are both:
   - cheap so far
   - estimated to be close to the goal
4. it avoids blocked cells such as obstacles
5. it returns a step-by-step path if one exists

Why it is useful:

- it is much better than random walking
- it avoids obstacles automatically
- it produces direct and efficient routes
- it lets agents compare whether a target is affordable before committing to it

So when the document says the system uses A* pathfinding, it means the agents are not just wandering. They are computing concrete routes through the grid.

## Turn Order and Sequence

Even though everything happens inside one simulation step, there is still a strict sequence.

### Global Order in One Simulation Step

The environment scheduler runs in this order:

1. **Environment step**
2. **Agents sense and communicate**
3. **Agents think and act**

This comes directly from `TWEnvironment`, where:

- the environment is scheduled with ordering `1`
- `sense()` and `communicate()` are scheduled with ordering `2`
- each agent’s `step()` method, which does `think()` then `act()`, is scheduled with ordering `3`

### What Happens First

At the start of a turn:

1. the environment creates new objects
2. the environment removes expired objects
3. the environment clears the message list

Only after that do the agents begin their own work for that turn.

### What Agents Do Next

After the environment update:

1. each agent senses its nearby area
2. each agent communicates using the shared broadcast list
3. each agent thinks
4. each agent acts

### Which Agent Works First?

The specialist agents are created in this order in `TWEnvironment`:

1. `FuelScoutAgent`
2. `TileHunterAgent`
3. `HoleFillerAgent`
4. `ExplorerAgent`
5. `DeliveryOptimizerAgent`
6. `SmarterReplanningAgent`

That is the creation order. Conceptually, however, all agents are intended to operate within the same phase of the turn:

- all do sensing and communication in the scheduling slot for order `2`
- all do thinking and acting in the scheduling slot for order `3`

So the important idea is not “one agent finishes the whole game logic before the next begins.” The important idea is:

- the environment updates first
- then the team gathers information
- then the team decides
- then the team acts

That phase structure is what makes communication useful.

## Basic Rules of the World

The agents are not free to do anything. They operate under a small set of basic rules.

### Movement Rules

- agents move one grid cell at a time
- movement is up, down, left, right, or stay still
- moving costs `1` fuel
- agents cannot move through obstacles

### Pickup and Delivery Rules

- an agent must be on the same cell as a tile to pick it up
- an agent can carry up to `3` tiles
- an agent must be on the same cell as a hole to drop a tile into it
- filling a hole increases reward

### Fuel Rules

- every agent starts with `500` fuel
- refueling only works at the fuel station
- agents must stand on the fuel station cell to refuel
- if fuel reaches zero, the agent can no longer move effectively

### Visibility Rules

- agents can only see a limited local area around themselves
- they do not know the whole map at once
- they must use memory and communication to act beyond what they currently see

### Object Rules

- tiles, holes, and obstacles are created dynamically during the simulation
- these objects disappear after their lifetime expires
- the fuel station does not disappear

### Scoring Rule

- reward is earned when a tile is successfully dropped into a hole
- collecting tiles alone does not score
- moving alone does not score

This is why the system must solve the full chain:

1. find objects
2. collect tiles
3. find holes
4. deliver in time
5. survive fuel constraints

### Shared Safety Logic

All agents share fuel and risk management logic.

This includes:

- emergency fuel checks
- refueling when standing on the station
- refusing trips that are too expensive
- avoiding targets likely to expire before arrival
- clearing stale or invalid plans

This is why specialization does not completely break reliability. The base class still enforces basic survival rules.

### Shared Environment Adaptation

All agents use the same runtime environment classification:

- `isDense()`
- `isShortLifetime()`
- `isLargeGrid()`

Those signals are estimated from observation patterns, not hardcoded to a specific config. That lets the team adjust behavior when the environment changes.

## What Fuel, Tiles, Holes, and Obstacles Do

The TileWorld environment contains a few core object types, and each one changes how the agents behave.

### Fuel Station

The fuel station is a fixed location on the map.

- agents do not know its position at the start
- moving costs fuel
- when fuel reaches zero, an agent is effectively finished
- refueling is only possible at the fuel station

This makes fuel station discovery one of the most important early events in the whole system.

### Fuel

Fuel is the team’s main survival resource.

- each move consumes fuel
- long routes are risky on large maps
- path planning must include the return cost to fuel
- low fuel can override other goals

In short, fuel limits how ambitious agents can be. Even a good scoring opportunity is bad if the agent cannot survive the trip.

### Tiles

Tiles are the objects agents pick up and carry.

- an agent can carry tiles up to capacity
- tiles are needed to score
- tiles disappear after their lifetime expires
- some agents collect tiles aggressively, while others focus more on delivery timing

Tiles represent opportunity, but only if the team can turn them into completed deliveries in time.

### Holes

Holes are the scoring targets.

- dropping a tile into a hole produces reward
- holes also expire after a limited lifetime
- a remembered hole may already be gone by the time an agent arrives

This is why hole freshness matters so much, especially in Config 2.

### Obstacles

Obstacles block movement.

- agents cannot move through them
- they make direct routes impossible
- pathfinding must go around them
- newly blocked paths can invalidate current plans

Obstacles mainly hurt efficiency. They increase travel cost and make timing less predictable.

### Other Agents

Teammates are not just helpers. They are also moving entities that must be coordinated around.

- two agents should not chase the same target
- they should not waste coverage by clustering too much
- they should share discoveries as quickly as possible

So the multi-agent problem is not only about the map. It is also about avoiding interference between teammates.

## Roles of Each Agent

| Agent | Main role | How it improves the result |
|---|---|---|
| **FuelScoutAgent** | Fuel station discovery | Explores aggressively to find fuel early, which helps the entire team survive and plan better. |
| **TileHunterAgent** | Tile collection | Batches tile pickup in stable settings and switches to faster delivery in short-lifetime settings, improving fuel efficiency and responsiveness. |
| **HoleFillerAgent** | Hole delivery reliability | Avoids expiring targets and rebroadcasts holes so the team keeps fresh delivery options. |
| **ExplorerAgent** | Systematic map coverage | Covers assigned zones and pushes the team toward unseen areas, improving discovery and reducing overlap. |
| **DeliveryOptimizerAgent** | High-value routing | Scores delivery choices, uses hotspot logic, and commits to good routes, improving reward efficiency. |
| **SmarterReplanningAgent** | Failure prevention | Cancels bad plans early when a target is expiring or fuel is insufficient, reducing wasted trips. |

### How Their Roles Work Together

- **FuelScoutAgent** gives the team early fuel knowledge. Once the fuel station is found and broadcast, the whole system becomes safer.
- **ExplorerAgent** improves coverage, which increases the chance of discovering objects and the fuel station faster.
- **TileHunterAgent** and **DeliveryOptimizerAgent** convert discoveries into reward efficiently.
- **HoleFillerAgent** keeps delivery information fresh and useful.
- **SmarterReplanningAgent** acts as a quality-control layer by preventing plans that are likely to fail.

The result is not just six agents doing different things. It is six agents covering different failure modes of TileWorld.

## How Each Agent Works

Each specialist keeps the shared base behavior, but changes the decision order to solve a particular problem better.

### FuelScoutAgent

`FuelScoutAgent` is designed to make fuel station discovery happen earlier.

How it works:

- explores aggressively while the fuel station is still unknown
- uses committed exploration targets so it does not keep changing direction too often
- prefers broad scouting behavior on large maps
- broadcasts `LOW` messages when fuel is becoming risky before station discovery
- once fuel is known, it can quickly transition back into the shared base logic

Its value is highest at the start of the run, when the whole team depends on finding fuel.

### TileHunterAgent

`TileHunterAgent` focuses on collecting tiles efficiently.

How it works:

- chooses an adaptive tile target
- in long-lifetime settings, it batches up to a fuller load before delivery
- in short-lifetime settings, it delivers earlier to avoid losing opportunities
- immediately claims tile or hole targets to reduce collisions with teammates
- uses `HOTSPOT` information to move toward promising tile-dense areas

Its strength is converting map discoveries into carried resources efficiently.

### HoleFillerAgent

`HoleFillerAgent` is a delivery-reliability specialist.

How it works:

- checks whether a currently planned target is likely to expire before arrival
- voids plans that became bad
- sends `EXPIRING` warnings so teammates also avoid those targets
- rebroadcasts fresh hole locations at intervals to keep the team’s delivery memory updated

Its main job is protecting the team from wasting trips to dead scoring opportunities.

### ExplorerAgent

`ExplorerAgent` focuses on search coverage.

How it works:

- operates with a zone-based exploration bias
- looks for least recently visited targets
- can move toward strong hotspots inside its own region
- broadcasts `SWAP` once a zone is sufficiently covered

Its strength is making sure the map gets seen instead of leaving large regions dark.

### DeliveryOptimizerAgent

`DeliveryOptimizerAgent` is the most scoring-focused specialist.

How it works:

- uses a multi-gate decision cascade
- scores holes using distance, freshness, and surrounding tile density
- adapts tile batching to environment conditions
- uses commitment logic to avoid oscillating between many near-equal goals
- broadcasts `HOTSPOT` information to help other agents exploit dense regions
- rebroadcasts holes and shares fuel knowledge when low-fuel teammates need it

This agent is strong because it combines several good ideas from other specialists into one high-value delivery policy.

### SmarterReplanningAgent

`SmarterReplanningAgent` is a failure-prevention specialist.

How it works:

- checks if a current goal has been flagged as expiring
- estimates whether the goal will still exist by arrival time
- checks whether the full trip is fuel-affordable
- voids plans early and forces replanning instead of following bad routes

This reduces wasted movement and helps the team recover faster when the environment changes.

## How Communication Improves the Result

Each agent only sees a small local area, so communication is critical. The shared message system lets agents broadcast:

- `FUEL` for fuel station discovery
- `TILE` and `HOLE` for new targets
- `GONE` for removed targets
- `CLAIM` to stop teammates chasing the same target
- `LOW` for low-fuel context
- `EXPIRING` for targets that will likely disappear soon
- `HOTSPOT` for promising dense regions
- `SWAP` for zone-completion coordination

This improves the result in four main ways:

1. **Faster discovery sharing**. One agent finding fuel benefits all six.
2. **Less duplication**. Claims reduce multiple agents chasing the same tile or hole.
3. **Better freshness**. Expiring-target warnings stop wasted movement.
4. **Better coverage**. Zone-based exploration spreads the team across the map.

Without communication, each agent is limited by its own sensor. With communication, the team behaves more like a distributed shared-intelligence system.

## Strong Points of the Project

### 1. Shared-base specialization

The strongest design choice is the shared base plus specialist layer.

- Navigation, fuel safety, memory, and messaging are implemented once.
- Specialized agents build on top of that instead of duplicating infrastructure.

This is a strong engineering choice because improvements scale across the whole team.

### 2. Strong coordination under partial observability

TileWorld is hard because no single agent sees enough of the map. The project addresses that directly through memory sharing, claims, and broadcasts.

This turns six local agents into one coordinated system.

### 3. Measured improvement in results

The broader same-day rerun evidence shows that the architecture improved performance substantially.

| Config | Evidence |
|---|---|
| **Config 1** | Averaged **890.35** over 40 runs with **0% failures** |
| **Config 2** | Averaged **1924.72** over 40 runs with **75% success rate** |

Config 1 is the clearest success case: the system is both strong and reliable there.

### 4. Quality improvements through cross-agent borrowing

One of the best signs of maturity in the code is that good ideas were reused across agents. For example, `DeliveryOptimizerAgent` explicitly incorporates mechanisms borrowed from sibling agents, such as:

- adaptive tile targets
- expiry projection
- fuel affordability checks
- commitment logic
- rebroadcasting

That means the project improved not just by adding more logic, but by combining the best ideas into better integrated behavior.

## Config 1 Phase Score History

The phase scores below are the historical progression for the Config 1 runs. The current Phase 4 validation runs are recorded separately in [`MULTI_RUN_RESULTS.md`](/Users/lcpnine/mas/MULTI_RUN_RESULTS.md).

| Phase | Avg Reward | Min | Max | Description |
|-------|-----------:|----:|----:|-------------|
| **Phase 1** | 32.1 | 0 | 55 | Exploration + opportunistic pickup only |
| **Phase 2** | 160.3 | 0 | 249 | + A* planning and delivery |
| **Phase 3** | 541.5 | 469 | 601 | + Zone coordination and communication |
| **Phase 4** | 864.3 | 801 | 959 | Latest Config 1 rerun |

## How the Team Improved the Result

The project improved through stages.

### Early improvement

The team first built a solid foundation:

- smarter memory
- fuel handling
- A* planning
- goal-directed delivery

This made agents capable, but not yet strongly coordinated.

### Major improvement

The largest jump came from:

- multi-agent communication
- zone-based exploration
- claim-based deconfliction

These changes made the six agents work as a real team instead of six isolated individuals.

### Later quality improvement

The next improvement came from robustness features:

- replanning when targets disappear
- skipping goals that will expire before arrival
- checking path validity before execution
- clearing stale claims
- adapting behavior to dense or short-lifetime environments

These changes improved quality by reducing wasted actions and making the system less brittle.

## Weaknesses of the Project

### 1. Config 2 is still unstable

The main weakness is that **Config 2 remains volatile**, even after improvements.

Why:

- the grid is much larger
- fuel is limited
- the fuel station may be found late
- objects expire quickly
- long trips become risky

So even though successful Config 2 runs can score very high, the system still has failure risk.

### 2. Lifetime detection is imperfect early on

The environment classifier can identify density and grid size well, but short-lifetime detection is harder early in a run because agents need enough observed disappearances first.

That means early decisions in fast-changing environments can still be based on incomplete information.

### 3. Large-grid fuel discovery remains a bottleneck

A major structural weakness is that many other decisions depend on finding the fuel station soon enough. If that discovery is delayed on a large map, the whole team is forced into more conservative or riskier behavior.

## How the Team Fixed Weaknesses to Improve Quality

The codebase already includes several direct fixes for these weaknesses.

### Fuel-related fixes

- **fuel safety override** makes fuel protection happen before specialist logic
- **adaptive fuel floors** help scouting behave differently on large short-lifetime maps
- **opportunistic refueling** reduces unnecessary risk near known fuel stations

These changes improve survivability and reduce preventable fuel deaths.

### Expiry-related fixes

- **expiry projection** skips goals that are unlikely to survive until arrival
- **EXPIRING broadcasts** warn teammates away from bad targets
- **hole rebroadcasting** helps keep delivery knowledge fresh

These changes reduce wasted movement toward disappearing objects.

### Coordination fixes

- **CLAIM messages** reduce duplicated effort
- **stale-claim cleanup** prevents the team from avoiding targets that no longer exist
- **zone-based exploration** spreads the team out more effectively

These changes improve team efficiency and map coverage.

### Planning-quality fixes

- **path validity checks** stop agents from committing to newly blocked paths
- **commitment logic** reduces oscillation between competing goals
- **dynamic replanning** helps agents switch away from bad plans early

These changes improve consistency and action quality.

## Best and Worst Scenarios for the Multi-Agent System

The multi-agent system performs best when the environment rewards coordination and coverage.

### Best Scenarios

The best scenarios are:

- **stable environments** where objects live long enough for planning to matter
- **moderately sparse maps** where finding objects is difficult enough that shared discovery helps a lot
- **maps where early fuel discovery happens quickly**
- **situations with many distributed opportunities**, so different agents can work in parallel without interfering

Why these scenarios are good:

- communication has time to pay off
- claims reduce duplication effectively
- specialization can spread across exploration, collection, and delivery
- the shared planner has enough time to turn memory into reward

This is why Config 1 is the strongest scenario for this system. It rewards coordination, planning, and reliability at the same time.

### Worst Scenarios

The worst scenarios are:

- **large maps with delayed fuel discovery**
- **short-lifetime environments** where targets vanish before agents arrive
- **high-volatility settings** where memory becomes stale very quickly
- **situations where long routes are required through obstacles**
- **cases where many agents are forced into the same narrow area**

Why these scenarios are bad:

- communication may arrive too late to save a plan
- the planner can make decisions using information that expires quickly
- fuel becomes the dominant constraint
- coordination overhead matters more because the environment changes too fast

This is why Config 2 is the hardest scenario. It has the two most dangerous properties at once: a larger map and shorter object lifetime.

### Multi-Agent Tradeoff

A multi-agent system is strongest when parallelism beats coordination cost. It is weakest when the environment changes so quickly that even shared information becomes old before the team can use it.

That is the central tradeoff in this project.

## Final Assessment

This project is strong because it combines:

- a reusable shared architecture
- complementary specialist roles
- active communication
- iterative quality fixes based on observed weaknesses

Its biggest success is **reliable high performance in Config 1** and a clearly coordinated multi-agent design. Its main remaining weakness is **Config 2 reliability**, where the system shows high upside but still suffers from fuel and volatility pressure.

Overall, the team improved the result not by relying on one clever trick, but by combining:

- shared infrastructure
- specialization
- communication
- repeated robustness fixes

That is the main reason the agents co-work effectively and achieve stronger results than a simpler independent-agent approach.
