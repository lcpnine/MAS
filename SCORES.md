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
| **Avg Score** | **548.9** | **1439.9** |
| Failures | 0% | 20% |

**Key Insight:** Config 2 has higher scoring potential but is much more challenging due to rapid object expiration, larger search space, and information overload.

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

**Last Updated:** 2026-04-02
