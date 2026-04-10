# Speaker Script — TileWorld Multi-Agent System
AI6125 · NTU · 2026 · 15–20 minute slot

**Format per slide:**
- **Transition** — how you hand off from the previous speaker
- **Message** — the single thing this slide proves
- **Points** — what to say (aim for ~90 seconds per slide)
- **Cut** — one sentence to say if you're running short on time

---

## Speaker 1 — Slides 1–3 (Problem + Challenge)
*~4 minutes*

---

### Slide 1 · Title

**Transition:** *(opening)*

**Message:** Set the context — what we built and the headline result.

**Points:**
> "We built a six-agent TileWorld system for the NTU AI6125 competition.
> The core design uses a shared base with specialist roles layered on top.
> On Config 1, our system achieved an average reward of 883.8 over ten runs with a zero percent failure rate.
> I'll give you the problem context, then my teammates will walk through how we got there."

**Cut:** "We built a six-agent coordinated system. 883.8 average on Config 1, zero failures."

---

### Slide 2 · Problem Setting

**Transition:** *(continuing from title)*

**Message:** TileWorld looks simple but has several compounding constraints.

**Points:**
> "TileWorld is a grid-based simulation running for 5000 steps.
> Each agent sees only a 5-by-5 patch of a grid that can be up to 80 by 80.
> The task is to pick up tiles and deliver them to holes before either expires.
> There is a single fuel station on the grid that starts hidden — every move drains fuel.
> We competed on three configurations: Config 1 is sparse and stable, Config 2 is dense and volatile, and Config 3 is unknown at competition time.
> Every tick follows the Sense, Communicate, Plan, Act loop defined in the assignment."

**Cut:** "Limited vision, expiring objects, hidden fuel station — three configs including an unknown one."

---

### Slide 3 · Why This Is Hard

**Transition:** *(continuing)*

**Message:** Three problems combine to make naive approaches fail.

**Points:**
> "First: partial observability. Each agent sees 5 by 5 in a grid up to 80 by 80. Without sharing, most of the grid is dark at any given moment.
>
> Second: object expiry. In Config 2, tiles and holes disappear in as few as 30 steps. An agent can plan a delivery, navigate halfway, and arrive at an empty cell.
>
> Third: fuel and coordination overhead. Refueling costs time and the station isn't visible until someone finds it. Without coordination, six agents redundantly cover the same ground, waste fuel on dying targets, and miss opportunities."

**Cut:** "Partial vision, fast expiry, and fuel cost — these three problems compound each other."

---

## Speaker 2 — Slide 4 (Architecture)
*~2.5 minutes*

---

### Slide 4 · Core Architecture

**Transition:** "So how did we address that? Here is the design we built."

**Message:** The key insight is separating shared infrastructure from specialist behavior.

**Points:**
> "Every agent runs the same loop: sense the environment, merge incoming messages into shared memory, plan, then act.
>
> Three shared components carry all six agents:
> SmartTWAgent provides the decision loop and a fuel-safety override that cannot be disabled.
> SmartTWAgentMemory handles time-decay, claim tracking, and runtime environment detection.
> SmartTWPlanner runs A-star with a round-trip fuel budget check — it won't commit to a goal the agent can't afford.
>
> The critical design choice is that all three are shared. If we improve the pathfinder, every agent benefits. Specialization sits on top, not in parallel."

**Cut:** "Shared backbone — A*, memory, fuel safety — with specialist behavior layered over it."

---

## Speaker 3 — Slide 5 (Evolution)
*~2.5 minutes*

---

### Slide 5 · Development Story

**Transition:** "We didn't build all of this at once. Here is how the system evolved across four phases."

**Message:** Each phase produced a measurable jump, and each one built directly on the last.

**Points:**
> "Phase 1 was survival: fuel management and a lawnmower exploration pattern. Average reward: 32.
>
> Phase 2 added goal-directed planning with A-star. Average reward jumped to 160 — a 5x improvement just from replacing random exploration with deliberate pathfinding.
>
> Phase 3 was the biggest single jump: zone assignment, message-based communication, and a claim system to prevent duplicate work. Reward went from 160 to 542.
>
> Phase 4 added the six specialist agents and a round of reliability fixes. That brought us to 884 on Config 1.
>
> The key point: we could measure the impact of every decision. If Phase 3 had hurt performance, we would have known immediately."

**Cut:** "32 → 160 → 542 → 884. Each phase measurable, each one additive."

---

## Speaker 4 — Slides 6–8 (Design + Agents + Communication)
*~4 minutes*

---

### Slide 6 · Final Team Architecture

**Transition:** "Let me show you the structure that produced those numbers."

**Message:** Two layers: a rock-solid shared base, and six specialists that override only what they need to.

**Points:**
> "Layer 1 is the shared base — SmartTWAgent. It contains A-star navigation, the fuel safety override, memory decay, the claim system, and all message handling.
>
> Layer 2 is the six specialist agents. Each one extends SmartTWAgent and overrides only the think method. Everything else is inherited.
>
> This is important: specialization means changing what the agent *prioritizes*, not rebuilding how it moves, communicates, or manages fuel. That means a bug fix in the base class benefits all six agents at once."

**Cut:** "Shared base does all the heavy lifting. Specialists only override priority logic."

---

### Slide 7 · The 6 Agents

**Transition:** *(continuing)*

**Message:** Each agent has a distinct role; together they cover the full problem space.

**Points:**
> "FuelScout aggressively searches for the fuel station and broadcasts its position the moment it finds it.
>
> TileHunter adapts its batching strategy — it collects one tile in short-lifetime environments and three tiles in stable ones, to match delivery feasibility to object longevity.
>
> HoleFiller projects expiry before committing to a delivery, and broadcasts EXPIRING signals to the team.
>
> Explorer divides the grid into six vertical strips, one per agent, and broadcasts a SWAP signal when its zone is fully covered.
>
> DeliveryOptimizer uses a seven-gate cascade to pick the freshest, most feasible goal each tick.
>
> SmarterReplanning runs three checks before committing to any goal: has a teammate signalled expiry? Will the agent arrive before the object dies? Can it afford the round trip?"

**Cut:** "Each agent has one primary job. Together they cover fuel, collection, delivery, coverage, throughput, and reliability."

---

### Slide 8 · Communication & Coordination

**Transition:** *(continuing)*

**Message:** Agents can't act for each other — but message-passing replicates the effect.

**Points:**
> "We use seven message types. FUEL lets every agent know the station's position the moment FuelScout finds it — without this, other agents might run dry before they discover it themselves.
>
> TILE and HOLE messages populate each agent's memory with observations from the full team, effectively multiplying each 5-by-5 sensor across the whole grid.
>
> CLAIM prevents duplicate work — when an agent targets a tile, it broadcasts a claim and others skip that tile.
>
> EXPIRING and LOW are safety signals. HOTSPOT and SWAP are coordination signals for routing and zone management.
>
> The underlying principle from MAS theory: agents have individual goals but can't act for each other. Shared information is the only mechanism available. We built a message schema that covers every situation where one agent's knowledge would change another agent's decision."

**Cut:** "Seven message types covering fuel, discovery, claims, and coordination. Information sharing replicates physical cooperation."

---

## Speaker 5 — Slide 9 (Robustness)
*~2 minutes*

---

### Slide 9 · Adaptation & Robustness

**Transition:** "Communication alone isn't enough. Here are the four mechanisms that keep the system reliable."

**Message:** Reliability is built in, not bolted on.

**Points:**
> "First: the fuel safety override. It fires at the very top of every think call, before any specialist logic runs. An agent heading to a fuel station cannot be redirected by any other priority.
>
> Second: runtime environment detection. We don't read the Parameters file for classification — we infer isDense, isShortLifetime, and isLargeGrid from live observations. This is what lets us generalize to Config 3 without hardcoded thresholds.
>
> Third: expiry projection. Before any agent commits to a goal, it checks whether the cost to reach it exceeds 80 percent of the object's remaining lifetime. If not affordable, skip.
>
> Fourth: fallback movement guards. Null-direction checks ensure an agent that finds no valid path can still take a safe step rather than freezing."

**Cut:** "Fuel override, live env detection, expiry projection, and null-direction guards. Four layers of reliability."

---

## Speaker 6 — Slides 10–12 (Results + Demo + Takeaways)
*~4 minutes*

---

### Slide 10 · Results & Analysis

**Transition:** "Let's look at what these design decisions actually produced."

**Message:** Config 1 improved significantly and reliably. Config 2 exposes a remaining bottleneck.

**Points:**
> "On Config 1: 883.8 average over 10 runs, ranging from 801 to 1003. Zero failures. That's a 60 percent improvement over the pre-specialization baseline of 548.
>
> On Config 2: the successful runs averaged 2566 — a 42 percent improvement on those runs. But the failure rate rose from 20 percent to 40 percent.
>
> We want to be honest about that. The Config 2 failures are fuel deaths on large-grid seeds where the fuel station takes too long to discover. Our specialists optimize aggressively, but aggressive behavior backfires when the fuel station is still unknown.
>
> The isShortLifetime detection is also a known limitation — the metric relies on observed object disappearances, which are rare in the first few hundred steps. Detection accuracy is only 20 to 30 percent early in the simulation."

**Cut:** "Config 1: +61%, zero failures. Config 2: higher peaks but more failures — fuel station discovery on large grids is the bottleneck."

---

### Slide 11 · Demo

**Transition:** "Let me show you the system running."

**Message:** Watch the coordination in action — zone separation, fuel broadcast, claim deconfliction, replanning.

**Script:**
> "This is Config 1 with a fixed seed so we see consistent behavior.
>
> At the start, watch the six agents — they spread out into their assigned vertical strips rather than clustering. That's the zone assignment from Phase 3.
>
> A few hundred steps in, one agent discovers the fuel station and immediately broadcasts a FUEL message. Watch how the other agents update their paths.
>
> Mid-simulation, two agents approach the same tile. The second one receives the CLAIM message and diverts to its next-best target without any explicit negotiation.
>
> Finally, watch what happens when an agent's current goal disappears — it replans within one tick and continues without stalling."

---

### Slide 12 · Takeaways

**Transition:** "To summarize."

**Message:** Three things worked, two limitations remain, and we know what to fix next.

**Points:**
> "What worked: the shared-base specialization approach. Every improvement to the base class lifted all six agents. Communication multiplied each agent's 5-by-5 sensor to cover the full grid. And building in phases meant we could measure every decision.
>
> What we'd fix: the Config 2 failure rate. The fuel station discovery problem on large grids needs a more aggressive early-game fuel scouting strategy. The isShortLifetime detection also needs a faster bootstrap — perhaps using object density as a proxy in the first 100 steps.
>
> If we had more time, we'd add online learning for environment classification and a negotiation protocol for dynamic zone rebalancing when one zone runs out of objects and another is overloaded."

**Cut:** "Shared base plus communication worked. Config 2 fuel deaths are the next problem to solve."

---

## Demo Checklist

Before the presentation:
- [ ] Compile current `main` branch: `bash run-tests.sh` (or `cd Tileworld && javac ... && java ...`)
- [ ] Run a single fixed-seed preview: edit `TileworldMain.java` to call `main4()` with `Parameters.seed = 4162012`
- [ ] Record a 90-second screen capture as fallback
- [ ] Confirm Config 1 parameters are active (not Config 2) in `Parameters.java`

Demo seed: use `Parameters.seed = 4162012` (existing default)

---

## Timing Guide

| Speaker | Slides | Target |
|---------|--------|--------|
| 1 | 1–3 | 4 min |
| 2 | 4 | 2.5 min |
| 3 | 5 | 2.5 min |
| 4 | 6–8 | 4 min |
| 5 | 9 | 2 min |
| 6 | 10–12 + demo | 5 min |
| **Total** | | **20 min** |

*For 15-minute slot: cut Speaker 4's Slide 6 (architecture overview) to 1 minute, and compress demo to 2 minutes.*
