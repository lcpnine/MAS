# Phase Development History

This document summarizes how the project evolved across Phases 1 to 4.

The score progression below is for **Config 1**. Current validation reruns are tracked separately in [`MULTI_RUN_RESULTS.md`](/Users/lcpnine/mas/MULTI_RUN_RESULTS.md).

The commit shown in the table is the primary anchor commit for each phase. Each phase also includes related follow-up commits listed in the sections below.

| Phase | Avg Reward | Min | Max |
|---|---:|---:|---:|
| **Phase 1** | 32.1 | 0 | 55 |
| **Phase 2** | 160.3 | 0 | 249 |
| **Phase 3** | 541.5 | 469 | 601 |
| **Phase 4** | 864.3 | 801 | 959 |

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

**Primary anchor:** `72a943`

**Related commits:** `348fb1`, `72a943`, `5a58db`, `091d70`, `7b78fd`, `cd81f80`, `d9cef11`, `f571aae`, `8e208cd`, `3b64708`, `e3a4a01`, `efe94e`

Phase 4 made two distinct contributions. The first was architectural: the system moved from a single shared `SmartTWAgent` to a team of role-specific agents — `TileHunterAgent`, `HoleFillerAgent`, `FuelScoutAgent`, `DeliveryOptimizerAgent`, and `ExplorerAgent`. Each agent owns a narrow responsibility and executes it without needing to reason about the full task space, which eliminated the conditional branching that had accumulated inside `SmartTWAgent`.

The second contribution was refinement of that new architecture: runtime environment detection, adaptive fuel safety margins, tighter target freshness handling, smarter replanning, and tuned behavior for dense or short-lifetime maps. These changes strengthened each specialised agent individually and made the team more robust across different map conditions.

## Summary

The development pattern across the four phases is clear:

1. `Phase 1` built survival and memory.
2. `Phase 2` added planning.
3. `Phase 3` added coordination.
4. `Phase 4` introduced specialised agents, then refined them with adaptive and robustness improvements.

That progression matches the score trajectory. The largest structural jump came in Phase 3, while Phase 4 introduced a second architectural shift — role-specific agents — and then made that new architecture more reliable and context-aware.
