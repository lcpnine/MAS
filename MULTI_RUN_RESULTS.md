# Multi-Run Test Results

**Date:** 2026-04-16
**Project:** TileWorld Multi-Agent System
**Current Phase:** Phase 4
**Runner:** `bash run-config1.sh` / `bash run-config2.sh`

---

## Config 1 Phase Score History

These are the historical Phase 1-4 scores recorded for Config 1 only. They are kept here for comparison with the current validation runs below.

A phase-by-phase narrative summary is recorded in [`docs/phase-history.md`](/Users/lcpnine/mas/docs/phase-history.md).

| Phase | Avg Reward | Min | Max | Description |
|---|---:|---:|---:|---|
| **Phase 1** | 32.1 | 0 | 55 | Exploration + opportunistic pickup only |
| **Phase 2** | 160.3 | 0 | 249 | + A* planning and delivery |
| **Phase 3** | 541.5 | 469 | 601 | + Zone coordination and communication |
| **Phase 4** | 864.3 | 801 | 959 | Latest Config 1 rerun |

---

## Phase 4 Validation Runs

These are the current Phase 4 reruns on the active codebase. They are separate from the historical Config 1 phase scores above and include both Config 1 and Config 2.

### Config 1

**Script:** `bash run-config1.sh`
**Seed:** 32690122

| Metric | Value |
|---|---:|
| Final reward | **959** |
| Average reward | **864.3** |
| Failures | 0 |

Script executed successfully — compiles, applies Config 1 parameters, and runs `TileworldMain` without errors.

### Config 2

**Script:** `bash run-config2.sh`
**Seed:** 502922639

| Metric | Value |
|---|---:|
| Final reward | **981** |
| Average reward | **888.6** |
| Failures | 0 |

Script executed successfully — applies Config 2 parameters, runs `TileworldMain`, and restores Config 1 on exit.
