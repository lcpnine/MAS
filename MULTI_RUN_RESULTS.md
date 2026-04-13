# Multi-Run Test Results

**Date:** 2026-04-14
**Project:** TileWorld Multi-Agent System
**Runner:** `bash run-config1.sh` / `bash run-config2.sh` / `bash run-baseline.sh`

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

---

## Baseline Retest (2026-04-14)

**Context:** Fresh baseline run using 6× plain `SmartTWAgent` (no specialization) via `run-baseline.sh` + `BaselineTileworldMain`. Both configs run in the same session.

**Log files:**
- `test-logs/baseline-config1-20260414-014118.log`
- `test-logs/baseline-config2-20260414-014136.log`

### Config 1 Baseline

| Seed | Score |
|------|------:|
| 1285218896 | 570 |
| 184095952 | 562 |
| 765561672 | 536 |
| 1345180370 | 593 |
| 325606911 | 571 |
| 1771649336 | 560 |
| 2070713174 | 530 |
| 2144141961 | 541 |
| 1503745347 | 525 |

| Metric | Value |
|---|---:|
| Average | **547.9** |
| Min | 525 |
| Max | 593 |
| Failures | 0 |
| Success rate | **100%** |

### Config 2 Baseline

| Seed | Score |
|------|------:|
| 671880803 | 359 |
| 463683310 | 316 |
| 1389702881 | 309 |
| 1800456472 | 260 |
| 1559321292 | 312 |
| 431464988 | 374 |
| 1819497296 | 376 |
| 924748438 | 410 |
| 1145037886 | 361 |
| 1053401864 | 321 |

| Metric | Value |
|---|---:|
| Average | **339.8** |
| Min | 260 |
| Max | 410 |
| Failures | 0 |
| Success rate | **100%** |
