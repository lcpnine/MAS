---
marp: true
theme: default
paginate: true
size: 16:9
footer: 'AI6125 Multi-Agent Systems · NTU 2026'
---

<style>
:root {
  --bg: #0f1117;
  --bg2: #1a1d27;
  --fg: #e2e8f0;
  --fg-dim: #94a3b8;
  --cyan: #61dafb;
  --purple: #bb86fc;
  --green: #4ade80;
  --yellow: #fbbf24;
  --red: #f87171;
  --border: #2d3748;
}

section {
  background-color: var(--bg);
  color: var(--fg);
  font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
  font-size: 21px;
  line-height: 1.65;
  padding: 52px 64px 64px;
  box-sizing: border-box;
}

h1 {
  font-size: 54px;
  font-weight: 800;
  color: var(--cyan);
  line-height: 1.2;
  margin: 0 0 16px;
  text-shadow: 0 0 40px rgba(97,218,251,0.25);
}

h2 {
  font-size: 32px;
  font-weight: 700;
  color: var(--cyan);
  margin: 0 0 28px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--border);
  position: relative;
}

h2::before {
  content: '';
  position: absolute;
  left: 0;
  bottom: -2px;
  width: 56px;
  height: 2px;
  background: linear-gradient(90deg, var(--cyan), var(--purple));
}

h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--purple);
  margin: 20px 0 8px;
}

ul, ol {
  padding-left: 28px;
  margin: 0;
}

li {
  margin-bottom: 10px;
  color: var(--fg);
}

li::marker {
  color: var(--cyan);
}

strong {
  color: var(--yellow);
  font-weight: 700;
}

em {
  color: var(--fg-dim);
  font-style: normal;
}

code {
  background-color: #1e2333;
  color: var(--cyan);
  padding: 2px 8px;
  border-radius: 4px;
  font-family: 'Consolas', 'Fira Code', monospace;
  font-size: 0.88em;
  border: 1px solid var(--border);
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 19px;
  margin-top: 8px;
}

th {
  background-color: #1e2333;
  color: var(--cyan);
  font-weight: 700;
  padding: 10px 14px;
  text-align: left;
  border: 1px solid var(--border);
}

td {
  padding: 9px 14px;
  border: 1px solid var(--border);
  vertical-align: top;
}

tr:nth-child(even) td {
  background-color: #161924;
}

tr:nth-child(odd) td {
  background-color: var(--bg);
}

footer {
  font-size: 13px;
  color: var(--fg-dim);
  position: absolute;
  bottom: 18px;
  left: 64px;
  right: 64px;
  border-top: 1px solid var(--border);
  padding-top: 8px;
}

/* Page number */
section::after {
  color: var(--fg-dim);
  font-size: 13px;
}

/* Lead (title) slide */
section.lead {
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: radial-gradient(ellipse at 70% 30%, #1a2035 0%, var(--bg) 70%);
}

section.lead h1 {
  font-size: 52px;
}

section.lead p {
  color: var(--fg-dim);
  font-size: 20px;
  line-height: 1.7;
  margin: 8px 0;
}

section.lead footer {
  display: none;
}

/* Divider / chapter slides */
section.chapter {
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: radial-gradient(ellipse at 30% 70%, #1a1535 0%, var(--bg) 65%);
}

section.chapter h2 {
  font-size: 46px;
  border: none;
  margin-bottom: 16px;
  color: var(--purple);
}

section.chapter h2::before { display: none; }

/* Flow diagram box */
.flow {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 20px 0;
  font-size: 20px;
  font-weight: 600;
}

.flow-box {
  background: #1e2333;
  border: 1px solid var(--cyan);
  border-radius: 8px;
  padding: 12px 20px;
  color: var(--cyan);
  white-space: nowrap;
}

.flow-box.wide {
  flex: 1;
  text-align: center;
}

.flow-arrow {
  color: var(--fg-dim);
  font-size: 22px;
}

/* Layer boxes */
.layer {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px 20px;
  margin: 10px 0;
}

.layer-label {
  font-size: 14px;
  font-weight: 700;
  color: var(--fg-dim);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 8px;
}

.layer.shared { border-color: var(--cyan); }
.layer.specialist { border-color: var(--purple); }

.agent-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 8px;
}

.agent-chip {
  background: #1e2333;
  border: 1px solid var(--purple);
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 17px;
  color: var(--purple);
  font-weight: 600;
  text-align: center;
}

/* Phase bar chart */
.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  height: 180px;
  padding: 0 20px;
  margin: 16px 0;
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  height: 100%;
  justify-content: flex-end;
}

.bar {
  width: 100%;
  border-radius: 4px 4px 0 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 4px;
  font-size: 15px;
  font-weight: 700;
  color: #0f1117;
}

.bar-label {
  font-size: 15px;
  color: var(--fg-dim);
  text-align: center;
  white-space: nowrap;
}

/* Result cards */
.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.result-card {
  background: #1e2333;
  border-radius: 8px;
  padding: 16px 20px;
  border-left: 3px solid var(--cyan);
}

.result-card.warn {
  border-left-color: var(--red);
}

.result-card .metric {
  font-size: 32px;
  font-weight: 800;
  color: var(--cyan);
  line-height: 1;
}

.result-card.warn .metric {
  color: var(--red);
}

.result-card .sublabel {
  font-size: 14px;
  color: var(--fg-dim);
  margin-top: 4px;
}

.tag {
  display: inline-block;
  background: #1e2333;
  border: 1px solid var(--border);
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 14px;
  color: var(--fg-dim);
  margin-right: 6px;
}
</style>

---

<!-- _class: lead -->
<!-- _paginate: false -->

# TileWorld Multi-Agent System

**AI6125 Multi-Agent Systems · NTU · 2026**

6 coordinated agents with shared memory,
fuel-aware A* planning, and specialist roles

*883.8 avg reward on Config 1 · 0% failure rate*

---

## Problem Setting

**TileWorld** is a competitive multi-agent simulation with 5000 steps and limited visibility

| Property | Value |
|----------|-------|
| Grid size | 50×50 (Config 1) · 80×80 (Config 2) · unknown (Config 3) |
| Sensor range | 5×5 cells per agent |
| Entities | Tiles, holes, obstacles, fuel stations, agents |
| Fuel | Drains every step · single hidden station |
| Object lifetime | 100 steps (Config 1) · 30 steps (Config 2) |
| Competition configs | Config 1 sparse/stable · Config 2 dense/volatile · Config 3 unknown |

**Agent loop (each tick):** `Sense → Communicate → Plan → Act`

**Scoring:** place a tile into a hole before either expires

---

## Why This Is Hard

<br>

### 1 · Partial Observability
Each agent sees only a **5×5 patch** of a grid up to **80×80**.
Unknown grid state must be built up incrementally through exploration and communication.

### 2 · Object Expiry
Tiles and holes disappear in as few as **30 steps**.
An agent may plan a delivery that becomes impossible before arrival.

### 3 · Fuel + Coordination Overhead
Refueling costs steps and the station is **initially hidden**.
Without coordination, agents duplicate work, waste fuel, and miss expiring targets.

---

## Core Architecture

<div class="flow">
  <div class="flow-box">Sense</div>
  <div class="flow-arrow">→</div>
  <div class="flow-box wide">Shared Memory<br><span style="font-size:15px;color:#94a3b8">+ Incoming Messages</span></div>
  <div class="flow-arrow">→</div>
  <div class="flow-box">Planner</div>
  <div class="flow-arrow">→</div>
  <div class="flow-box">Act</div>
</div>

| Component | Responsibility |
|-----------|---------------|
| `SmartTWAgent` | 8-priority decision loop + `fuelSafetyOverride()` |
| `SmartTWAgentMemory` | Time-decay, claim tracking, runtime env detection |
| `SmartTWPlanner` | A* pathfinding with round-trip fuel budget check |

All three are **shared by all 6 agents** — specialization adds behavioral priorities on top, not separate implementations.

---

## Development Story

<div class="bar-chart">
  <div class="bar-col">
    <div class="bar" style="height:4%;background:#4a5568;">32.1</div>
    <div class="bar-label">Phase 1<br><em style="font-size:12px">Survive</em></div>
  </div>
  <div class="bar-col">
    <div class="bar" style="height:18%;background:#4a7c59;">160.3</div>
    <div class="bar-label">Phase 2<br><em style="font-size:12px">Plan</em></div>
  </div>
  <div class="bar-col">
    <div class="bar" style="height:61%;background:#2563eb;">541.5</div>
    <div class="bar-label">Phase 3<br><em style="font-size:12px">Coordinate</em></div>
  </div>
  <div class="bar-col">
    <div class="bar" style="height:63%;background:#3b82f6;">556.1</div>
    <div class="bar-label">Phase 4<br><em style="font-size:12px">Adaptive</em></div>
  </div>
  <div class="bar-col">
    <div class="bar" style="height:100%;background:#61dafb;color:#0f1117;">883.8</div>
    <div class="bar-label"><strong style="color:var(--cyan)">Final</strong><br><em style="font-size:12px">Specialists</em></div>
  </div>
</div>

| Phase | Key Addition | Config 1 Avg |
|-------|-------------|-------------|
| 1 · Survive | Fuel management + lawnmower exploration | 32.1 |
| 2 · Plan | A* pathfinding + tile/hole delivery | 160.3 |
| 3 · Coordinate | Zone assignment + messaging + claim system | 541.5 |
| 4 · Adaptive | Env detection, adaptive margins, opportunistic refuel | 556.1 |
| **Final · Specialists** | **6 specialist agents added on Phase 4 base** | **883.8** |

---

## Final Team Architecture

<div class="layer shared">
  <div class="layer-label">Layer 1 · Shared Base (all agents inherit)</div>
  <strong style="color:var(--cyan)">SmartTWAgent</strong> &nbsp;·&nbsp; A* pathfinding &nbsp;·&nbsp; <code>fuelSafetyOverride()</code> &nbsp;·&nbsp; memory decay &nbsp;·&nbsp; claim system &nbsp;·&nbsp; messaging &nbsp;·&nbsp; env detection
</div>

<div class="layer specialist" style="margin-top:16px">
  <div class="layer-label">Layer 2 · Specialist Agents (override <code>think()</code>)</div>
  <div class="agent-grid">
    <div class="agent-chip">FuelScout</div>
    <div class="agent-chip">TileHunter</div>
    <div class="agent-chip">HoleFiller</div>
    <div class="agent-chip">Explorer</div>
    <div class="agent-chip">DeliveryOptimizer</div>
    <div class="agent-chip">SmarterReplanning</div>
  </div>
</div>

<br>

Each specialist **reuses all shared infrastructure** and adds its own behavioral priority stack.
Specialization changes *what an agent prioritizes*, not how it navigates or communicates.

---

## The 6 Agents

### Exploration
| Agent | Focus |
|-------|-------|
| **FuelScout** | Finds the hidden fuel station · broadcasts `FUEL` + `LOW` to the team |
| **Explorer** | Systematic zone coverage in 3×2 grid partition · broadcasts `SWAP` when done |

### Delivery
| Agent | Focus |
|-------|-------|
| **TileHunter** | Collects tiles · batches adaptively (1 short-life / 3 long-life) |
| **HoleFiller** | Delivers tiles · validates expiry before committing · broadcasts `EXPIRING` |
| **DeliveryOptimizer** | Maximises throughput · freshness-first selection · cluster routing |

### Reliability
| Agent | Focus |
|-------|-------|
| **SmarterReplanning** | Validates every goal: expiry signal? arrival time? fuel budget? |

---

## Communication & Coordination

**Discovery** — multiply each agent's 5×5 sensor across the full grid

| `FUEL` | Station position shared instantly on discovery |
|--------|-----------------------------------------------|
| `TILE` / `HOLE` | Remote sightings populate every agent's memory |

**Deconfliction** — prevent wasted work

| `CLAIM` | Skip already-targeted objects |
|---------|-------------------------------|
| `EXPIRING` | Skip objects about to vanish |
| `LOW` | Alert team to a fuel-critical agent |

**Coordination** — optimise collective coverage

| `HOTSPOT` | Redirect exploration toward dense areas |
|-----------|----------------------------------------|
| `SWAP` | Reassign zones when a zone is exhausted |

> Agents **cannot act for each other** — but shared information replicates the effect.

---

## Adaptation & Robustness

### Fuel Safety Override
`fuelSafetyOverride()` fires at the **top of every `think()`** call.
Fuel emergency overrides all specialist logic — no agent can starve itself.

### Runtime Environment Detection
`isDense()` · `isShortLifetime()` · `isLargeGrid()` inferred from **live observations only** — no hardcoded `Parameters` reads. Validated across 5 hypothetical Config 3 scenarios:

| Detector | Accuracy | Note |
|----------|----------|------|
| `isDense()` | **100%** | Normalised object density per sensor step |
| `isLargeGrid()` | **100%** | Direct dimension threshold (≥70) |
| `isShortLifetime()` | **~20–30%** early | Improves over time; few disappearances observed early |

### Expiry Projection
Before committing to a goal: `costToGoal > remainingLifetime × 0.8` → skip.
Prevents wasted trips to targets that will expire before arrival.

### Fallback Movement Guards
Null-direction safety and fallback movement prevent agents from locking up when no valid path exists.

---

## Results & Analysis

<div class="result-grid">
  <div class="result-card">
    <div class="metric">883.8</div>
    <div class="sublabel">Config 1 avg reward · 10 runs</div>
    <div style="margin-top:10px;font-size:17px">Range: 801–1003 &nbsp;·&nbsp; <strong style="color:var(--green)">0% failures</strong></div>
    <div style="font-size:15px;color:var(--fg-dim);margin-top:4px">+61.0% vs pre-specialization baseline (548.9)</div>
  </div>
  <div class="result-card warn">
    <div class="metric">1539.6</div>
    <div class="sublabel">Config 2 avg reward · 10 runs</div>
    <div style="margin-top:10px;font-size:17px">Success avg: 2566.0 &nbsp;·&nbsp; <strong style="color:var(--red)">40% failures</strong></div>
    <div style="font-size:15px;color:var(--fg-dim);margin-top:4px">Successful-run avg: +42.6% vs baseline successful-run avg (1799.9) · failure rate 20% → 40%</div>
  </div>
</div>

<br>

**Root cause of Config 2 failures:** fuel depletion before station discovery on large-grid seeds.
**Known limitation:** `isShortLifetime` correctly classifies long-lifetime environments; in short-lifetime environments, early detection is only ~20–30%.

---

## Live Demo

**Config:** Config 1 · fixed seed · 5000 steps

<br>

Watch for these events:

1. **Zone separation** at start — 6 agents diverge into their assigned 3×2 zones
2. **Fuel station broadcast** — `FUEL` message propagates to all agents the moment it is found
3. **Claim deconfliction** — second agent approaching a claimed tile diverts to the next best target
4. **Automatic replanning** — agent's goal disappears mid-path; it replans without intervention

<br>

*Pre-recorded fallback available if live demo fails*

---

<!-- _class: lead -->
<!-- _paginate: false -->

## Takeaways

<br>

**What worked**
- Shared-base specialization: all agents stay **reliable** while adding individual strengths
- Communication multiplied individual 5×5 sensor range across the full grid
- Phase-by-phase evolution made every gain **measurable**

**Limitations**
- Config 2: 40% failure rate from fuel depletion on large-grid seeds
- `isShortLifetime` detects long-lifetime correctly; short-lifetime detection is only ~20–30% in early steps

**Future work**
- Online learning for environment classification
- Negotiation protocols for dynamic zone rebalancing

---

<!-- _paginate: false -->

<div style="display:flex;flex-direction:column;justify-content:center;align-items:center;height:100%;text-align:center;">
  <h1 style="font-size:48px;margin-bottom:24px">Thank you</h1>
  <p style="font-size:20px;color:var(--fg-dim)">AI6125 Multi-Agent Systems · NTU · 2026</p>
  <p style="font-size:18px;color:var(--fg-dim);margin-top:32px">
    Code: <code>github.com/lcpnine/mas</code>
  </p>
</div>
