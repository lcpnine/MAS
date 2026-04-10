# Speaker Script — TileWorld Multi-Agent System
AI6125 · NTU · 2026 · 15–20 minute slot

**Format per slide:**
- **Transition** — how you hand off from the previous speaker
- **Message** — the single thing this slide proves
- **Points** — what to say (60–75 seconds for most slides; Slides 7, 8, and 9 must be compressed to cut)
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
> "Phase 1 was survival: fuel management and a lawnmower exploration pattern. Average reward: 32.1.
>
> Phase 2 added goal-directed planning with A-star. Average reward jumped to 160.3 — about 5x improvement just from replacing random exploration with deliberate pathfinding.
>
> Phase 3 was the biggest single jump: zone assignment, message-based communication, and a claim system to prevent duplicate work. Reward went from 160.3 to 541.5.
>
> Phase 4 introduced adaptive optimization — observation-based environment detection, opportunistic refueling, and adaptive lawnmower step size. That reached 556.1.
>
> Finally, adding the six specialist agents on top of that Phase 4 base brought the system to 883.8 — the bar on the far right.
>
> The key point: we could measure the impact of every decision. If Phase 3 had hurt performance, we would have known immediately."

**Cut:** "32.1 → 160.3 → 541.5 → 556.1 → 883.8. Five steps, each measurable, each additive."

---

## Speaker 4 — Slides 6–8 (Design + Agents + Communication)
*~4 minutes*

---

### Slide 6 · Final Team Architecture

**Transition:** "Let me show you the structure that produced those numbers."

**Message:** Two layers: a rock-solid shared base, and six specialists that override only what they need to.

**Points:**
> "Two layers: a shared base that every agent inherits — navigation, fuel safety, memory, messaging — and six specialists that override only what they need. A fix to the base lifts all six at once."

**Cut:** "Shared base does all the heavy lifting. Specialists only override priority logic."

---

### Slide 7 · The 6 Agents

**Transition:** *(continuing)*

**Message:** Each agent has a distinct role; together they cover the full problem space.

**Points:**
> "Exploration: FuelScout locates the hidden station; Explorer covers the grid in a 3×2 zone partition.
>
> Delivery: TileHunter and HoleFiller handle collection and delivery; DeliveryOptimizer picks the freshest feasible goal each tick.
>
> Reliability: SmarterReplanning validates every goal against expiry signal, arrival time, and fuel budget before committing."

**Cut:** "Three groups: exploration, delivery, reliability. Each agent owns one job."

---

### Slide 8 · Communication & Coordination

**Transition:** *(continuing)*

**Message:** Agents can't act for each other — but message-passing replicates the effect.

*Speaker note: The slide displays all 8 message labels as a reference table — you only need to speak one example per group. Do not read the labels aloud.*

**Points:**
> "Discovery: when FuelScout finds the station, a FUEL message reaches every agent within one tick — a 5-by-5 sensor becomes team-wide coverage instantly.
>
> Deconfliction: a CLAIM message stops two agents pursuing the same target. A LOW message triggers early refueling before an agent goes critical.
>
> Coordination: HOTSPOT and SWAP messages reallocate effort when part of the grid runs dry.
>
> The practical result: agents that have never seen the same cell can still avoid duplicating each other's work."

**Cut:** "Discovery, deconfliction, coordination — eight messages, three jobs."

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
> Second: runtime environment detection. We don't read the Parameters file for classification — we infer isDense, isShortLifetime, and isLargeGrid from live observations. Across five hypothetical Config 3 scenarios, isDense and isLargeGrid were 100% correct; only short-lifetime detection lagged in the early steps.
>
> Third and fourth: expiry projection skips any goal the agent can't reach before 80 percent of its lifetime expires; fallback movement guards ensure agents never freeze when no valid path exists."

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
> On Config 2: the overall average is 1539.6 across all 10 runs — 4 runs failed with zero score. The successful runs averaged 2566.0, a +42.6 percent improvement over the baseline successful-run average of 1799.9. But the failure rate rose from 20 percent to 40 percent.
>
> We want to be honest about that. The Config 2 failures are fuel deaths on large-grid seeds where the fuel station takes too long to discover. Our specialists optimize aggressively, but aggressive behavior backfires when the fuel station is still unknown."

**Cut:** "Config 1: +61%, zero failures. Config 2: successful runs +42.6% but failure rate doubled — fuel station discovery on large grids is the bottleneck."

---

### Slide 11 · Demo

**Transition:** "Let me show you the system running."

**Message:** Watch the coordination in action — zone separation, fuel broadcast, claim deconfliction, replanning.

**Script:**
> "This is Config 1 with a fixed seed so we see consistent behavior.
>
> At the start, watch the six agents — they spread out into their assigned 3×2 zones rather than clustering. That's the zone assignment from Phase 3.
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
> What we'd fix: the Config 2 failure rate. The fuel station discovery problem on large grids needs a more aggressive early-game fuel scouting strategy. The `isShortLifetime` detection also needs a faster bootstrap — it classifies long-lifetime environments correctly, but short-lifetime detection is only ~20–30% by step 50, then improves over time. A density-based proxy in the first 100 steps could fix this.
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
| 1 | 1–3 | 3.5 min |
| 2 | 4 | 2.5 min |
| 3 | 5 | 2.5 min |
| 4 | 6–8 | 4 min |
| 5 | 9 | 2 min |
| 6 | 10–12 + demo | 5.5 min |
| **Total** | | **20 min** |

*Target 18–19 min of spoken content — five speaker handoffs will consume the remaining 1–2 min. If running over, pre-cut one beat each from Slide 5 (drop the "key point" sentence), Slide 8 (drop the Coordination example), and the demo narration (drop the replanning beat).*

*Slide 13 (Thank You) is a non-speaking Q&A visual — no script needed, but account for it when advancing slides at the end of Slide 12.*

*For 15-minute slot:*
- *Speaker 1 (slides 1–3): 3 min — cut one bullet per slide*
- *Speaker 2 (slide 4): 2 min*
- *Speaker 3 (slide 5): 2 min*
- *Speaker 4 (slides 6–8): 3 min — skip Slide 6 verbal walkthrough (show diagram only); on Slide 7 name the three groups only; on Slide 8 read one example per group only*
- *Speaker 5 (slide 9): 1.5 min — drop fallback movement guards from spoken script*
- *Speaker 6 (slides 10–12 + demo): 3.5 min — 1 min results, 2 min pre-recorded demo (zone separation + fuel broadcast only), 0.5 min takeaways*
