# TileWorld Multi-Agent System - How to Run Each Phase

## Prerequisites

- **Java 17** (tested with Java 17.0.7 LTS)
- **MASON 14** library (included at `lib/MASON_14.jar`)
- No build tool required - compiles with `javac` directly

## Quick Start (Current Code - Phase 4)

```bash
# Compile
mkdir -p Tileworld/bin
find Tileworld/src -name "*.java" | xargs javac -cp lib/MASON_14.jar -d Tileworld/bin

# Run (10 iterations, random seeds, prints average reward)
java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TileworldMain
```

## Running Individual Phases

Each phase corresponds to a git commit. Since phases are cumulative (each builds on the previous), you check out the specific commit to get that phase's code.

### Phase 1 - Fuel Management, Memory, Exploration
**Commit:** `ac290a0`

```bash
git stash              # save any uncommitted changes
git checkout ac290a0
mkdir -p Tileworld/bin
find Tileworld/src -name "*.java" | xargs javac -cp lib/MASON_14.jar -d Tileworld/bin
java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TileworldMain
git checkout main      # return to latest
git stash pop          # restore changes
```

**What it includes:**
- SmartTWAgent with lawnmower exploration pattern
- SmartTWAgentMemory with fuel station tracking and memory decay
- Opportunistic tile pickup/drop only (no planning)
- Fuel emergency management

### Phase 2 - Goal-Directed Planning
**Commit:** `b870f85`

```bash
git stash
git checkout b870f85
mkdir -p Tileworld/bin
find Tileworld/src -name "*.java" | xargs javac -cp lib/MASON_14.jar -d Tileworld/bin
java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TileworldMain
git checkout main
git stash pop
```

**What it adds:**
- SmartTWPlanner with A* pathfinding
- Tile seeking with affordability checks
- Hole delivery planning
- Path caching and reuse

### Phase 3 - Multi-Agent Coordination
**Commit:** `fab8b03`

```bash
git stash
git checkout fab8b03
mkdir -p Tileworld/bin
find Tileworld/src -name "*.java" | xargs javac -cp lib/MASON_14.jar -d Tileworld/bin
java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TileworldMain
git checkout main
git stash pop
```

**What it adds:**
- Zone-based exploration (6 agents in 3x2 grid partition)
- Message-based communication (fuel station, tile, hole, claim broadcasts)
- Claim tracking to prevent duplicate work
- Shared entity tracking from other agents

### Phase 4 - Adaptive Optimization (Current HEAD)
**Commit:** `72a943c` (or `main` branch)

```bash
mkdir -p Tileworld/bin
find Tileworld/src -name "*.java" | xargs javac -cp lib/MASON_14.jar -d Tileworld/bin
java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TileworldMain
```

**What it adds:**
- Observation-based environment detection (density from avg objects per sensor step, lifetime from disappearance tracking, grid size from actual environment dimensions) — no hardcoded `Parameters` reads for classification
- Adaptive fuel safety margins
- Opportunistic refueling near fuel stations
- Tile batching in dense environments
- Dynamic replanning for closer targets
- Adaptive lawnmower step size
- Path validity checking before execution (avoids wasted steps on newly blocked cells)
- Stale claim cleanup when claimed objects disappear from memory
- Hole lifetime validation to skip holes/tiles that will expire before the agent arrives

## Running Without Checkout (Isolated)

To run a specific phase without affecting your working directory:

```bash
# Extract phase source to a temp directory
mkdir -p /tmp/tileworld-phase/src /tmp/tileworld-phase/bin
git archive <COMMIT_HASH> -- Tileworld/src/ | tar -x -C /tmp/tileworld-phase/

# Compile and run
find /tmp/tileworld-phase/Tileworld/src -name "*.java" | \
  xargs javac -cp lib/MASON_14.jar -d /tmp/tileworld-phase/bin
java -cp /tmp/tileworld-phase/bin:lib/MASON_14.jar tileworld.TileworldMain

# Cleanup
rm -rf /tmp/tileworld-phase
```

Replace `<COMMIT_HASH>` with:
| Phase | Commit Hash |
|-------|-------------|
| 1 | `ac290a0` |
| 2 | `b870f85` |
| 3 | `fab8b03` |
| 4 | `72a943c` |

## Switching Configurations

The default configuration is **Config 1** (50x50 sparse). To test with **Config 2** (80x80 dense), edit `Tileworld/src/tileworld/Parameters.java`:

| Parameter | Config 1 (Default) | Config 2 (Dense) |
|-----------|-------------------|-----------------|
| `xDimension` | 50 | 80 |
| `yDimension` | 50 | 80 |
| `tileMean` | 0.2 | 2.0 |
| `holeMean` | 0.2 | 2.0 |
| `obstacleMean` | 0.2 | 2.0 |
| `lifeTime` | 100 | 30 |

After editing, recompile before running.

## Performance Results (Config 1, 10 runs each)

| Phase | Avg Reward | Min | Max | Description |
|-------|-----------|-----|-----|-------------|
| **Phase 1** | 32.1 | 0 | 55 | Exploration + opportunistic pickup only |
| **Phase 2** | 160.3 | 0 | 249 | + A* planning and delivery |
| **Phase 3** | 541.5 | 469 | 601 | + Zone coordination and communication |
| **Phase 4** | 556.1 | 481 | 596 | + Adaptive strategies |

### Progression Chart

```
Avg Reward
600 |                              ████  ████
550 |                              ████  ████
500 |                              ████  ████
450 |                              ████  ████
400 |                              ████  ████
350 |                              ████  ████
300 |                              ████  ████
250 |                              ████  ████
200 |                              ████  ████
150 |          ████                ████  ████
100 |          ████                ████  ████
 50 |  ████    ████                ████  ████
  0 +------------------------------------------
      Phase1  Phase2  Phase3  Phase4
       32.1   160.3   541.5   556.1
```

## Config 3 Simulation Results

Since Config 3 is unknown in the competition, we tested 5 hypothetical "Config 3" scenarios to validate that our runtime environment detection adapts correctly across diverse parameter combinations.

### Results Table (10 runs each, with observation-based detection)

| Scenario | Grid | Spawn Rate (μ) | Lifetime | Avg Reward | Min | Max | Fuel Deaths | isDense | isShortLifetime | isLargeGrid |
|----------|------|----------------|----------|------------|-----|-----|-------------|---------|-----------------|-------------|
| **Config 1** | 50×50 | 0.2 | 100 | 546.5 | 466 | 606 | 0/10 | false (correct) | false (correct) | false (correct) |
| **Config 2** | 80×80 | 2.0 | 30 | 230.9 | 0 | 377 | 3/10 | true (correct) | partial (~30%) | true (correct) |
| **A: Small Dense** | 30×30 | 1.5 | 50 | 3182.1 | 3081 | 3225 | 0/10 | true (correct) | false (correct) | false (correct) |
| **B: Large Sparse** | 100×100 | 0.1 | 150 | 46.7 | 0 | 83 | 3/10 | false (correct) | false (correct) | true (correct) |
| **C: Med High Turnover** | 60×60 | 3.0 | 15 | 170.4 | 127 | 221 | 0/10 | true (correct) | partial (~20%) | false (correct) |
| **D: Med Balanced** | 65×65 | 0.5 | 75 | 661.1 | 522 | 763 | 0/10 | false (correct) | false (correct) | false (correct) |
| **E: Large Dense Long** | 80×80 | 2.0 | 100 | 1252.9 | 0 | 1861 | 3/10 | true (correct) | false (correct) | true (correct) |

### Detection Accuracy Summary

- **isDense**: 100% correct after threshold fix (normalized density = avgObj/step × gridArea/sensorArea, threshold >50)
- **isShortLifetime**: Correct for long-lifetime environments; partially detects short lifetime (~20-30% of agents by step 50, improves over time). Fundamental limitation: agents on large grids observe few object lifecycles early in simulation
- **isLargeGrid**: 100% correct (simple dimension check, threshold ≥70)

### Key Observations

1. **Scenario A (Small Dense)** achieved the highest scores (3182 avg) — small grid means fast fuel station discovery and high object density per cell
2. **Scenario B (Large Sparse)** had 30% fuel death rate — extremely sparse objects on a huge grid make fuel station discovery unreliable
3. **Scenario E (Large Dense Long)** had 30% fuel deaths despite abundant objects — large grid fuel station discovery is the bottleneck
4. **Scenario D (Medium Balanced)** outperformed Config 1 — moderate spawn rate on a slightly larger grid gives good balance
5. **isShortLifetime detection** remains a known limitation — the metric tracks observed disappearances which are rare early in simulation. The detection improves as the simulation progresses (cumulative counters)

### Running Custom Config 3 Scenarios

To test your own Config 3:

1. Edit `Tileworld/src/tileworld/Parameters.java` — change `xDimension`, `yDimension`, `tileMean`, `holeMean`, `obstacleMean`, `tileDev`, `holeDev`, `obstacleDev`, and `lifeTime`
2. Recompile: `find Tileworld/src -name "*.java" | xargs javac -cp lib/MASON_14.jar -d Tileworld/bin`
3. Run: `java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TileworldMain`
4. Remember to revert Parameters.java after testing

## Other Run Modes

```bash
# Single run with fixed seed
# (Edit TileworldMain.java: change main() to call main4())
java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TileworldMain

# GUI mode (requires display)
java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TWGUI
```
