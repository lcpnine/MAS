# Score Tracking - TileWorld Multi-Agent System

**Project:** NTU AI6125 - Multi-Agent Systems
**Team:** 6 Agents
**Base Framework:** SmartTWAgent

---

## Baseline Scores (No Specializations)

**Date:** 2026-04-02
**Configuration:** All agents using base `SmartTWAgent` behavior (specialists call `super.think()`)

---

### Config 1: Stable Environment (Parameters.java)

**Environment:** 50×50 grid, lifetime 100, sparse objects

| Run | Seed | Score |
|-----|------|-------|
| 1 | 485233351 | 507 |
| 2 | 730251616 | 558 |
| 3 | 273278053 | 563 |
| 4 | 1333566655 | 549 |
| 5 | 1446739245 | 507 |
| 6 | 804898994 | 530 |
| 7 | 1700916724 | 560 |
| 8 | 1641181468 | 555 |
| 9 | 1520419261 | 566 |
| 10 | 1744548123 | 594 |

**Config 1 Baseline:**
- **Average:** 548.9
- **Range:** 507-594
- **Status:** ✅ Consistent performance, no failures

---

### Config 2: Volatile Environment (Parameters2.java)

**Environment:** 80×80 grid, lifetime 30, dense objects (10× more)

| Run | Seed | Score |
|-----|------|-------|
| 1 | 2057143826 | 0 ❌ |
| 2 | 1601653137 | 1829 |
| 3 | 953376910 | 1869 |
| 4 | 1543768039 | 1833 |
| 5 | 1371462797 | 0 ❌ |
| 6 | 592638203 | 1728 |
| 7 | 946709628 | 1764 |
| 8 | 530795097 | 1871 |
| 9 | 869897579 | 1804 |
| 10 | 486521294 | 1701 |

**Config 2 Baseline:**
- **Average:** 1439.9 (excluding failures: 1799.9)
- **Range:** 0-1871
- **Success Rate:** 80% (8/10 runs)
- **Status:** ⚠️ High volatility, 20% failure rate

---

## Config Comparison

| Metric | Config 1 (Stable) | Config 2 (Volatile) |
|--------|-------------------|---------------------|
| Grid Size | 50×50 | 80×80 |
| Object Lifetime | 100 | 30 |
| Object Density | 0.2 mean (sparse) | 2.0 mean (dense) |
| Fuel Capacity | 500 | 500 |
| **Baseline Avg** | **548.9** | **1439.9** |
| **Final Avg (pre-fix)** | **883.8** | **1539.6** |
| **Bug-Fixed Avg** | **813.7** | **1479.3** |
| Baseline Failures | 0% | 20% |
| Final Failures (pre-fix) | 0% | 40% |
| Bug-Fixed Failures | 0% | 10% |

**Key Insight:** Config 1 improved significantly (+61.0% vs baseline) with zero failures. Config 2 failure rate improved from 40% to 10% after the bug fix (object lifetime was incorrectly set to 100 steps instead of 30 in the test script). Bug-fixed Config 2 successful runs average 1643.7.

---

## Final Results (After All Agent Specializations Implemented)

**Date:** 2026-04-10
**Configuration:** All 6 specialist agents active (FuelScout, TileHunter, HoleFiller, Explorer, DeliveryOptimizer, SmarterReplanning)

### Config 1: Final Scores

| Run | Seed | Score |
|-----|------|-------|
| 1 | 1780338918 | 1003 |
| 2 | 1231410861 | 821 |
| 3 | 101695460 | 847 |
| 4 | 41646663 | 858 |
| 5 | 595275871 | 801 |
| 6 | 647214690 | 847 |
| 7 | 250373607 | 977 |
| 8 | 942406077 | 825 |
| 9 | 1457024394 | 980 |
| 10 | 1521039410 | 879 |

**Config 1 Final:**
- **Average:** 883.8 (+61.0% vs baseline 548.9)
- **Range:** 801–1003
- **Failure Rate:** 0%
- **Status:** ✅ Consistent improvement, zero failures

### Config 2: Final Scores

| Run | Seed | Score |
|-----|------|-------|
| 1 | 348125492 | 0 ❌ |
| 2 | 1679860796 | 2605 |
| 3 | 1133029664 | 2593 |
| 4 | 1674742454 | 2645 |
| 5 | 1577092082 | 0 ❌ |
| 6 | 939021519 | 2496 |
| 7 | 961758693 | 0 ❌ |
| 8 | 464230032 | 2600 |
| 9 | 446586957 | 0 ❌ |
| 10 | 916130063 | 2457 |

**Config 2 Final:**
- **Average:** 1539.6 (excluding failures: 2566.0)
- **Range:** 0–2645
- **Failure Rate:** 40% (4/10 runs) — worse than baseline 20%
- **Status:** ⚠️ Higher peak scores but increased failure rate vs baseline

---

## Agent Specializations

| Agent | Specialization | Focus Area |
|-------|---------------|------------|
| Agent 0 | FuelScoutAgent | Lower fuel threshold, wider search radius |
| Agent 1 | TileHunterAgent | Capacity-based batching (1/3 tiles) |
| Agent 2 | HoleFillerAgent | Hole patrolling (dense envs only) |
| Agent 3 | ExplorerAgent | Zone-based exploration (6 strips) |
| Agent 4 | DeliveryOptimizerAgent | Cluster-density routing |
| Agent 5 | SmarterReplanningAgent | Predictive failure detection |

---

## Tracking Your Results

After implementing your agent specialization, run tests and track your improvements:

### How to Test Config 1
```bash
cd Tileworld
javac -cp "../lib/MASON_14.jar" -d bin -sourcepath src src/tileworld/TileworldMain.java
java -cp "bin:../lib/MASON_14.jar" tileworld.TileworldMain
```

### How to Test Config 2
1. Edit `TWEnvironment.java`: Change `import tileworld.Parameters;` → `import tileworld.Parameters2;`
2. Replace all `Parameters.` → `Parameters2.` in `TWEnvironment.java`
3. Replace all `Parameters.` → `Parameters2.` in `TileworldMain.java`
4. Recompile and run

### What to Track
- Average score over 10 runs
- Failure rate (especially for Config 2)
- Score consistency (variance)
- Compare against baseline:
  - **Config 1 baseline:** 548.9
  - **Config 2 baseline:** 1439.9

### Goals
- Reduce Config 2 failure rate from 20% → 0%
- Maintain or improve Config 1 consistency
- Achieve good performance on BOTH configs (generalization)

---

---

## Bug-Fixed Results (After run-tests.sh Fix)

**Date:** 2026-04-14
**Fix:** `run-tests.sh` was not switching object lifetime to 30 steps for Config 2 in `TWObjectCreator.java` — objects were living 100 steps instead of 30, flooding the grid. Fixed by patching the correct environment-layer files only.

### Config 1: Bug-Fixed Scores

| Run | Seed | Score |
|-----|------|-------|
| 1 | 1871076827 | 838 |
| 2 | 1040844671 | 753 |
| 3 | 874603391 | 868 |
| 4 | 1404597876 | 920 |
| 5 | 1989114278 | 894 |
| 6 | 1932730136 | 892 |
| 7 | 2086761665 | 315 |
| 8 | 494487007 | 857 |
| 9 | 1877723601 | 903 |
| 10 | 1394466126 | 897 |

**Config 1 Bug-Fixed:**
- **Average:** 813.7
- **Range:** 315–920
- **Failure Rate:** 0%
- **Log:** `test-logs/config1-20260414-001703.log`

### Config 2: Bug-Fixed Scores

| Run | Seed | Score |
|-----|------|-------|
| 1 | 2133795667 | 1675 |
| 2 | 1506701698 | 1691 |
| 3 | 803048268 | 1560 |
| 4 | 205262730 | 1514 |
| 5 | 1579995097 | 1629 |
| 6 | 1563481405 | 1434 |
| 7 | 2077485139 | 1716 |
| 8 | 1710251302 | 0 ❌ |
| 9 | 2058411423 | 1757 |
| 10 | 534566726 | 1817 |

**Config 2 Bug-Fixed:**
- **Average:** 1479.3
- **Range:** 0–1817
- **Failure Rate:** 10% (1/10)
- **Average (successful runs only):** 1643.7
- **Log:** `test-logs/config2-20260414-001719.log`

---

**Last Updated:** 2026-04-14
