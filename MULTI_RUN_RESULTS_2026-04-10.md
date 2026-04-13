# Multi-Run Test Results

**Date:** 2026-04-14
**Project:** TileWorld Multi-Agent System
**Runner:** `bash run-config1.sh` / `bash run-config2.sh`

---

## run-config1.sh Validation Run (2026-04-14)

**Context:** `run-config1.sh` was added as a headless counterpart to `run-gui-config1.sh`. This is the first run to verify the script works correctly.

**Script:** `bash run-config1.sh`
**Seed:** 1264286830

| Metric | Value |
|---|---:|
| Final reward | **916** |
| Average reward | **902.0** |
| Failures | 0 |

Script executed successfully — compiles, applies Config 1 parameters, and runs `TileworldMain` without errors.

---

## run-config2.sh Validation Run (2026-04-14)

**Context:** Companion run to the `run-config1.sh` validation above, verifying `run-config2.sh` still works correctly after the bug-fix.

**Script:** `bash run-config2.sh`

| Metric | Value |
|---|---:|
| Final reward | **780** |
| Average reward | **758.0** |
| Failures | 0 |

Script executed successfully — applies Config 2 parameters, runs `TileworldMain`, and restores Config 1 on exit.
