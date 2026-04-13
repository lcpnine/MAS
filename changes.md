# Agent Optimization — Changes & Rationale

## Results

| Config | Baseline Avg | Baseline Best | After Avg | After Best |
|--------|-------------|---------------|-----------|------------|
| Config 1 (50×50, lifetime=100) | 800 | 900 | **~840** | **984** |
| Config 2 (80×80, lifetime=30)  | 700 | 800 | **~846** | **943** |

---

## Change 1 — `opportunisticAction()` in all 6 specialist agents

**What:** Added a call to `opportunisticAction()` at the top of every specialist `think()` method, immediately after `fuelSafetyOverride()`.

**Why:** Each specialist overrides `think()` with custom priority logic that returns a `MOVE` action before the base class's priority 3 (drop tile in hole) and priority 4 (pick up tile) can execute. This meant agents were **walking over tiles and holes without interacting with them** — losing free points every step. The `opportunisticAction()` check inserts a zero-cost pickup or drop at the current cell before any movement decision is made.

**Affected agents:** FuelScoutAgent, TileHunterAgent, HoleFillerAgent, ExplorerAgent, DeliveryOptimizerAgent, SmarterReplanningAgent.

---

## Change 2 — Tighter expiry thresholds

**What:** Changed the threshold fraction used to decide whether a target will expire before arrival from `0.7 / 0.9` (short/long lifetime) to `0.65 / 0.85`.

**Why:** The original thresholds were too lenient — agents would commit to targets already near the end of their life, arrive to find them gone, and wasted fuel on the trip. Tighter thresholds cause earlier plan abandonment and faster replanning to fresher targets, especially critical in Config 2 where objects only live 30 steps.

**Affected agents:** HoleFillerAgent, SmarterReplanningAgent, DeliveryOptimizerAgent, TileHunterAgent.

---

## Change 3 — Reachability checks on tile targets (TileHunterAgent)

**What:** Added `isReachableBeforeExpiry()` check on both tile and hole targets before committing.

**Why:** TileHunterAgent was navigating toward tiles it sensed much earlier in memory that had likely expired by the time it arrived. The check compares manhattan distance to remaining lifetime and aborts early if arrival is implausible, freeing the agent to explore or pick a fresher target.

---

## Change 4 — FuelScoutAgent refuel threshold lowered (70% → 60%)

**What:** After discovering the fuel station, the scout now only returns to refuel when below 60% fuel (down from 70%), unless it wants to refuel immediately after first discovery.

**Why:** At 70% the scout was spending too many steps cycling back to the fuel station even when it had plenty of fuel left. Lowering to 60% keeps it productive in the field longer after initial discovery.

---

## Change 5 — DeliveryOptimizerAgent: reduced freshness penalty weight (15 → 10)

**What:** Lowered `FRESHNESS_WEIGHT` from `15.0` to `10.0` in the hole scoring function.

**Why:** The original weight was over-penalising holes that were only slightly older, causing the agent to ignore perfectly viable nearby holes in favour of very fresh but distant ones. The dual-threshold expiry check already filters truly unreachable holes, so this penalty can be lighter.

---

## Change 6 — DeliveryOptimizerAgent: throttled hotspot broadcasts (every step → every 5 steps)

**What:** Hotspot broadcast messages are now only sent every 5 simulation steps.

**Why:** Broadcasting every step flooded the shared message channel and caused other agents to constantly re-evaluate their paths based on marginally updated cluster locations. Throttling to every 5 steps reduces noise while still propagating useful tile-density information.

---

## Change 7 — HoleFillerAgent: adaptive hole rebroadcast interval and freshness

**What:** Hole rebroadcast interval is now 10 steps (short-lifetime env) or 15 steps (long-lifetime env), and freshness cutoff is 0.5× or 0.7× of object lifetime respectively.

**Why:** In Config 2 (lifetime=30), rebroadcasting at the original 15-step interval meant most entries being sent were already quite stale. A tighter 10-step cadence and 0.5× freshness cutoff ensures only useful hole information is shared.

---

## Change 8 — ExplorerAgent: adaptive hotspot distance threshold (fixed 15 → 8/15)

**What:** The minimum distance required before the explorer moves toward a hotspot is now `8` on small grids (50×50) and `15` on large grids (80×80), instead of a fixed `15`.

**Why:** On a 50×50 grid a threshold of 15 was ignoring hotspots that were right in the agent's zone. Reducing it to 8 for small grids makes the explorer react to nearby tile clusters rather than bypassing them.

---

## What was NOT changed

- `Parameters.java`, `Parameters2.java` — no environment parameters were modified.
- `SmartTWAgent`, `SmartTWAgentMemory`, `SmartTWPlanner`, `TWEnvironment` — core infrastructure untouched.
- The base planner's affordability and replanning logic was preserved. An earlier attempt to add aggressive active tile/hole seeking *inside* specialist `think()` methods caused severe regression (avg dropped to ~445 on Config 1) because it bypassed the planner's carefully tuned cost checks. All navigation still delegates to `SmartTWPlanner` and `super.think()`.
