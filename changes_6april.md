## Changes by File

### `SmartTWAgent.java` (around line 415)
- Added shared fuel safety hook for specialist agents via `fuelSafetyOverride`.
- New behavior:
    - Refuels immediately when on a known station and tank is not full.
    - On fuel emergency, clears current plan/path and forces return-to-fuel navigation.
    - Uses fallback greedy movement toward fuel if planned route is unavailable.
- Added `opportunisticAction` helper for zero-cost pickup/drop opportunities at current cell (around line 393).

### `DeliveryOptimizerAgent.java` (around line 112)
- `think` now starts with `fuelSafetyOverride`.
- Clears commitment before honoring forced fuel behavior.
- Fuel danger comments/logic aligned with `SmartTWAgent` safety assumptions (around line 263).
- **Net effect:** strategy-level commitments no longer override emergency fuel return.

### `ExplorerAgent.java` (around line 29)
- Added early `fuelSafetyOverride` in `think`.
- **Net effect:** zone exploration/hotspot pursuit no longer bypasses emergency refuel behavior.

### `HoleFillerAgent.java` (around line 33)
- Added early `fuelSafetyOverride` in `think`.
- **Net effect:** hole-focused replanning remains fuel-safe under emergency thresholds.

### `TileHunterAgent.java` (around line 36)
- Added early `fuelSafetyOverride` in `think`.
- **Net effect:** aggressive tile collection cannot starve fuel return logic.

### `SmarterReplanningAgent.java` (around line 26)
- Added early `fuelSafetyOverride` in `think`.
- **Net effect:** predictive replanning checks run only when fuel safety permits.

### `FuelScoutAgent.java` (around line 14)
- Added `LARGE_GRID_MIN_FUEL_FLOOR`.
- Reduced `LOW_BROADCAST_INTERVAL` from `10` to `4` (around line 18).
- Added early `fuelSafetyOverride` in `think` (around line 33).
- Improved unknown-fuel-station behavior:
    - Adaptive low-fuel floor for short-lifetime + large-grid setups.
    - One last greedy exploration attempt before waiting near floor.
    - Larger effective exploration radius floor on large maps.
- LOW broadcasts now occur more frequently for faster team reaction (around line 136).

### `SmartTWAgentMemory.java` (around line 245)
- `getAllRememberedTiles` now includes synthetic tile candidates from teammate-shared tile entries (`sharedEntityType == 1`) if within lifetime.
- `getAllRememberedHoles` now includes synthetic hole candidates from teammate-shared hole entries (`sharedEntityType == 2`) if within lifetime (around line 269).
- **Net effect:** planners consume both directly observed and communicated map knowledge.

### `TWAgent.java` (around line 101)
- `move` now normalizes null direction to stay action (around line 102).
- When fuel is depleted, `move` returns safely instead of printing and attempting unstable behavior (around line 106).
- **Net effect:** stronger runtime robustness for edge cases.

---

## Current Strategy of All 6 Active Agents

Active team composition is instantiated in:
- `TWEnvironment.java:120`
- `TWEnvironment.java:122`
- `TWEnvironment.java:124`
- `TWEnvironment.java:126`
- `TWEnvironment.java:128`
- `TWEnvironment.java:130`

### FuelScout
- **Source:** `FuelScoutAgent.java:32`
- **Primary role:** discover fuel station quickly and improve coverage under uncertain fuel.
- **Strategy:**
    - Enforces shared fuel safety override first.
    - If station unknown: adaptive committed exploration (center-first, then least-visited), with radius scaled by remaining fuel and map size.
    - In short-lifetime large grids: lowers conservative fuel floor to avoid premature retreat.
    - After station discovery: may return to refuel (especially right after discovery or below threshold).
    - Periodically communicates LOW fuel to trigger teammate fuel-info rebroadcasts.

### TileHunter
- **Source:** `TileHunterAgent.java:35`
- **Primary role:** aggressive tile collection with conflict reduction.
- **Strategy:**
    - Enforces shared fuel safety override first.
    - Adaptive tile batching target:
        - Short-lifetime: deliver at 1 tile.
        - Long-lifetime: batch up to 3 tiles.
    - Immediately claims selected tile/hole targets to reduce teammate duplication.
    - Skips claimed and expiring targets.
    - If no local tile candidate: explores hotspot hints, then fallback exploration.

### HoleFiller
- **Source:** `HoleFillerAgent.java:32`
- **Primary role:** reliable hole delivery with expiry-aware replanning.
- **Strategy:**
    - Enforces shared fuel safety override first.
    - Validates active plan goal:
        - Drops plan if teammate flagged goal as expiring.
        - Predicts expiry risk from lifetime vs arrival distance and abandons risky targets.
    - Broadcasts `EXPIRING` warnings for failing goals.
    - Periodically rebroadcasts fresh known holes.

### Explorer
- **Source:** `ExplorerAgent.java:28`
- **Primary role:** zone-based map coverage and hotspot assistance.
- **Strategy:**
    - Enforces shared fuel safety override first.
    - Operates in assigned strip zone; prefers dense in-zone hotspot targets when worthwhile.
    - Otherwise visits least recently visited cells (radius scaled by grid size).
    - Marks zone complete after sufficient coverage and emits `SWAP`.
    - Falls through to `SmartTWAgent` for pickup/delivery/fuel fundamentals.

### DeliveryOptimizer
- **Source:** `DeliveryOptimizerAgent.java:111`
- **Primary role:** freshness-aware, commitment-driven tile-to-hole throughput optimization.
- **Strategy:**
    - Enforces shared fuel safety override first and clears stale commitment if fuel intervention occurs.
    - Multi-gate pipeline:
        - Validate commitment (expiry flags, projected expiry, fuel affordability).
        - Prioritized delivery when carrying enough tiles.
        - En-route batching when partially loaded.
        - Tile hunting when empty.
        - Hotspot-guided harvesting.
        - Base fallback.
    - Uses claim broadcasts, commitment windows, hotspot sharing, and periodic hole rebroadcasting.

### SmarterReplanning
- **Source:** `SmarterReplanningAgent.java:25`
- **Primary role:** predictive plan validation before execution.
- **Strategy:**
    - Enforces shared fuel safety override first.
    - If planner has a goal, performs 3 checks:
        1. Teammate-expiring signal check.
        2. Arrival-before-expiry projection using environment-sensitive threshold.
        3. Fuel affordability for round trip to goal and then station.
    - If checks pass: executes plan.
    - Otherwise: replans or reroutes to fuel.
    - Broadcasts `EXPIRING` when likely failed targets are detected.