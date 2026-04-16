# Phase Development History

This document summarizes how the project evolved across Phases 1 to 4.

The score progression below is for **Config 1**. Phases 1–3 scores are frozen historical results from earlier codebase states. The Phase 4 score is the current validation rerun on the active codebase; full rerun details are in [`MULTI_RUN_RESULTS.md`](/Users/lcpnine/mas/MULTI_RUN_RESULTS.md).

The primary anchor commit for each phase is noted in the section below. Each phase also includes related follow-up commits listed in those sections.

| Phase | Avg Reward | Min | Max |
|---|---:|---:|---:|
| **Phase 1** | 32.1 | 0 | 55 |
| **Phase 2** | 160.3 | 0 | 249 |
| **Phase 3** | 541.5 | 469 | 601 |
| **Phase 4** | 864.3 | 801 | 959 (current rerun) |

## Phase 1

**Primary anchor:** `ac290a`

**Related commits:** `126128`, `ac290a`

Phase 1 established the survival baseline. The project introduced `SmartTWAgent` and `SmartTWAgentMemory`, replacing the simpler default agent behavior with fuel-aware movement, memory decay, fuel station tracking, and systematic lawnmower exploration.

At this stage, the agents could survive longer and pick up or drop tiles opportunistically, but they still lacked deliberate long-distance planning. That is why performance remained low: the team could react locally, but not yet route intelligently toward high-value work.

## Phase 2

**Primary anchor:** `b870f8`

**Related commits:** `88818a`, `b870f8`

Phase 2 added `SmartTWPlanner` and moved the system from reactive movement to goal-directed behavior. Agents could now use A* pathfinding to seek remembered tiles, deliver carried tiles to holes, and reject trips that were too expensive relative to fuel.

This phase produced the first major quality jump because the agents stopped wandering and started executing explicit plans. Even so, the behavior was still mostly individual. Planning improved efficiency, but the team still lacked coordination.

## Phase 3

**Primary anchor:** `fab8b0`

**Related commits:** `705f7d`, `fab8b0`

Phase 3 turned the system into a coordinated multi-agent team. The main additions were zone-based exploration, message broadcasts, shared tile and hole knowledge, and claim tracking to reduce duplicate work across agents.

This is the phase where the score jump became structural rather than incremental. Instead of six agents exploring independently, the project began to benefit from coverage, task deconfliction, and faster reuse of discoveries such as fuel station knowledge and candidate targets.

## Phase 4

**Primary anchor:** `d9cef11`

**Related commits:** `348fb1`, `72a943`, `5a58db`, `091d70`, `cd81f80`, `d9cef11`, `da869a4`, `f571aae`, `8e208cd`, `3b64708`, `e3a4a01`, `7b78fd`, `efe94e`

Phase 4 unfolded in three steps.

The first was adaptive improvements to `SmartTWAgent` and `SmartTWAgentMemory` directly (`348fb1`, `72a943`, `5a58db`): runtime environment detection, adaptive fuel safety margins, and smarter planning heuristics.

The second was a two-stage architectural transition. `cd81f80` broke the uniform agent loop — previously six identical `SmartTWAgent` instances — into individually instantiated per-slot subclasses (person-named: `AdityaAgent`, `YutaekAgent`, etc.), each a thin wrapper with no overridden behavior. `091d70` added a further `SmartTWAgentMemory` improvement during this window. `d9cef11` then replaced those with role-named subclasses — `TileHunterAgent`, `HoleFillerAgent`, `FuelScoutAgent`, `DeliveryOptimizerAgent`, `ExplorerAgent`, and `SmarterReplanningAgent` — still mostly delegating to `super` at that point.

The third step was where the actual role-specific behavior landed (`da869a4`, `f571aae`, `8e208cd`, `3b64708`, `e3a4a01`, `7b78fd`, `efe94e`): tighter expiry thresholds, freshness-aware delivery clustering, reachability checks on tile targets, adaptive hotspot distances, and throttled broadcasts. `SmartTWAgent` remained the shared execution base throughout; each subclass layered its specialisation on top.

## Summary

The development pattern across the four phases is clear:

1. `Phase 1` built survival and memory.
2. `Phase 2` added planning.
3. `Phase 3` added coordination.
4. `Phase 4` first improved `SmartTWAgent` adaptively, then split the team into per-slot subclasses (`cd81f80`), then renamed those to role-named classes (`d9cef11`), then added the actual per-role behavior in follow-up commits.

That progression matches the score trajectory. The largest structural jump came in Phase 3, while Phase 4 combined adaptive base improvements with a two-stage architectural transition to role-specific subclasses, with substantive specialisation landing in the follow-up commits rather than at the architectural switch itself.
