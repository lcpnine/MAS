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
- Runtime environment detection (dense/sparse, large/small grid, short/long lifetime)
- Adaptive fuel safety margins
- Opportunistic refueling near fuel stations
- Tile batching in dense environments
- Dynamic replanning for closer targets
- Adaptive lawnmower step size

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

## Other Run Modes

```bash
# Single run with fixed seed
# (Edit TileworldMain.java: change main() to call main4())
java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TileworldMain

# GUI mode (requires display)
java -cp Tileworld/bin:lib/MASON_14.jar tileworld.TWGUI
```
