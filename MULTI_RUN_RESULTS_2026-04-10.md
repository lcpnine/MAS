# Multi-Run Test Results

**Date:** 2026-04-10  
**Project:** TileWorld Multi-Agent System  
**Code state:** current workspace HEAD  
**Runner:** `bash run-tests.sh`

## Purpose

The presentation reports:

- **Config 1 average:** 883.8
- **Config 2 average:** 1539.6

Those values match the first 10-run batch already stored in:

- `test-logs/config1-20260410-182951.log`
- `test-logs/config2-20260410-183007.log`

This document adds **3 new batches** on the same codebase, which gives:

- **30 fresh runs for Config 1**
- **30 fresh runs for Config 2**

It also shows the **combined 40-run total** when the original presentation batch is included.

## Method

Each batch runs:

- 10 random-seed games on **Config 1**
- 10 random-seed games on **Config 2**

New rerun batches collected for this document:

- `test-logs/config1-20260410-193717.log`
- `test-logs/config1-20260410-193759.log`
- `test-logs/config1-20260410-193840.log`
- `test-logs/config2-20260410-193734.log`
- `test-logs/config2-20260410-193815.log`
- `test-logs/config2-20260410-193856.log`

## Batch Results

### Config 1

| Batch | Log | Average |
|---|---|---:|
| Presentation batch | `test-logs/config1-20260410-182951.log` | 883.8 |
| Rerun 1 | `test-logs/config1-20260410-193717.log` | 893.4 |
| Rerun 2 | `test-logs/config1-20260410-193759.log` | 915.8 |
| Rerun 3 | `test-logs/config1-20260410-193840.log` | 868.4 |

### Config 2

| Batch | Log | Average |
|---|---|---:|
| Presentation batch | `test-logs/config2-20260410-183007.log` | 1539.6 |
| Rerun 1 | `test-logs/config2-20260410-193734.log` | 2092.1 |
| Rerun 2 | `test-logs/config2-20260410-193815.log` | 2022.6 |
| Rerun 3 | `test-logs/config2-20260410-193856.log` | 2044.6 |

## Fresh Rerun Summary

These statistics use only the **3 new batches** collected for this document.

### Config 1: 30 fresh runs

| Metric | Value |
|---|---:|
| Runs | 30 |
| Average score | **892.53** |
| Standard deviation | 52.04 |
| Min | 757 |
| Max | 1012 |
| Failures | 0 |
| Success rate | **100%** |

### Config 2: 30 fresh runs

| Metric | Value |
|---|---:|
| Runs | 30 |
| Average score | **2053.10** |
| Standard deviation | 1033.81 |
| Min | 0 |
| Max | 2778 |
| Failures | 6 |
| Success rate | **80%** |
| Average on successful runs only | **2566.38** |
| Successful-run standard deviation | 136.70 |

## Combined Same-Day Summary

These statistics combine the **presentation batch** plus the **3 new rerun batches**.

### Config 1: 40 total runs

| Metric | Value |
|---|---:|
| Runs | 40 |
| Average score | **890.35** |
| Standard deviation | 57.36 |
| Min | 757 |
| Max | 1012 |
| Failures | 0 |
| Success rate | **100%** |

### Config 2: 40 total runs

| Metric | Value |
|---|---:|
| Runs | 40 |
| Average score | **1924.72** |
| Standard deviation | 1116.57 |
| Min | 0 |
| Max | 2778 |
| Failures | 10 |
| Success rate | **75%** |
| Average on successful runs only | **2566.30** |
| Successful-run standard deviation | 125.82 |

## Comparison Against Presentation Numbers

### Config 1

- Presentation value: **883.8**
- Fresh rerun average over 30 runs: **892.53**
- Combined 40-run average: **890.35**

Interpretation: the presentation value is consistent with later reruns. Config 1 remains stable and stays near **890 average** with **no failures**.

### Config 2

- Presentation value: **1539.6**
- Fresh rerun average over 30 runs: **2053.10**
- Combined 40-run average: **1924.72**
- Successful-run average across 40 runs: **2566.30**

Interpretation: Config 2 is highly volatile. The presentation batch was a lower-performing sample because it had **4 failures out of 10**. In the new reruns, the failure rate improved to **20%**, so the overall average increased sharply even though successful-run scores stayed in roughly the same **2500-2700** band.

## Main Takeaways

- **Config 1 is reliable.** The system repeatedly scores around **890** with zero failures.
- **Config 2 has much higher upside but still unstable outcomes.** Successful runs are strong, but failures still dominate the spread.
- The presentation averages are valid for that original 10-run sample, but they do **not** fully represent the broader same-day rerun performance, especially for Config 2.

## Recommended Reporting Line

If you want a stronger evidence-based statement for report or presentation:

> Across 4 same-day 10-run batches, Config 1 averaged **890.35** over 40 runs with **0% failures**. Config 2 averaged **1924.72** over 40 runs with a **75% success rate**, and its successful runs averaged **2566.30**.
