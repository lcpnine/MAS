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
> Phase 4 introduced adaptive optimization — observation-based environment detection, opportunistic refueling, and adaptive lawnmower step size. That reached 556.
>
> Finally, adding the six specialist agents on top of that Phase 4 base brought the system to 883.8 — the bar on the far right.
>
> The key point: we could measure the impact of every decision. If Phase 3 had hurt performance, we would have known immediately."

**Cut:** "32 → 160 → 542 → 556 → 884. Five steps, each measurable, each additive."

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
> "The slide groups the six agents into three categories.
>
> Exploration: FuelScout finds the hidden station and broadcasts its position. Explorer divides the grid into six vertical strips and systematically covers them.
>
> Delivery: TileHunter collects tiles — batching one or three depending on object lifetime. HoleFiller validates expiry before every delivery and warns teammates when targets are dying. DeliveryOptimizer picks the freshest, most feasible goal each tick.
>
> Reliability: SmarterReplanning checks three conditions before committing to any goal — has a teammate signalled expiry, will the agent arrive in time, and can it afford the fuel round trip. If any check fails, it finds the next best option."

**Cut:** "Three groups: exploration, delivery, reliability. Each agent owns one job."

---

### Slide 8 · Communication & Coordination

**Transition:** *(continuing)*

**Message:** Agents can't act for each other — but message-passing replicates the effect.

**Points:**
> "The messages fall into three groups.
>
> Discovery: FUEL and TILE and HOLE messages turn each agent's 5-by-5 sensor into team-wide coverage. When FuelScout finds the station, every agent knows within one tick.
>
> Deconfliction: CLAIM prevents two agents pursuing the same target. EXPIRING warns the team that a target is dying — drop it. LOW alerts teammates that an agent is fuel-critical.
>
> Coordination: HOTSPOT redirects exploration toward dense areas. SWAP reassigns zones when one strip is exhausted.
>
> The practical result: agents that have never seen the same cell can still avoid duplicating each other's work."

**Cut:** "Discovery, deconfliction, coordination — seven messages, three jobs."

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
*~5 minutes — budget: 1.5 min results · 2 min demo (pre-recorded by default) · 1 min takeaways · 0.5 min buffer*

---

### Slide 10 · Results & Analysis

**Transition:** "Let's look at what these design decisions actually produced."

**Message:** Config 1 improved significantly and reliably. Config 2 exposes a remaining bottleneck.

**Points:**
> "On Config 1: 883.8 average over 10 runs, ranging from 801 to 1003. Zero failures. That's a +61.0 percent improvement over the pre-specialization baseline of 548.9.
>
> On Config 2: the successful runs averaged 2566 — a +42.6 percent improvement on those runs. But the failure rate rose from 20 percent to 40 percent.
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

*For 15-minute slot:*
- *Speaker 1 (slides 1–3): 3 min — cut one bullet per slide*
- *Speaker 2 (slide 4): 2 min*
- *Speaker 3 (slide 5): 2 min*
- *Speaker 4 (slides 6–8): 3 min — skip Slide 6 verbal walkthrough (show diagram only); on Slide 7 name the three groups only, no per-agent detail; on Slide 8 read one example per group only*
- *Speaker 5 (slide 9): 1.5 min — drop fallback movement guards bullet*
- *Speaker 6 (slides 10–12 + demo): 3.5 min — 1 min results, 2 min pre-recorded demo, 0.5 min takeaways*
