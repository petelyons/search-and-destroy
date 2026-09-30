# Robot AI Commitments: Results vs Baseline

Spec: `2026-09-30-robot-ai-commitments-design.md`.

**Method.** `AITestCLI metrics --games 20 --turn-limit 150 --seed 1000`, with both players
running the default AI. Every game reached the turn limit. Baselines were recorded before any AI
change, in `ai-configs/baselines/2026-09-baseline-{map}.json`. Seeded runs are not
bit-reproducible, so the results compare means and standard errors. The numbers below are from
the final branch state, after the review fixes.

## Land criteria

| Criterion | MedMap | BorderLand |
|---|---|---|
| LAND oscillation down ≥ 50% | −58% ✅ | −55% ✅ |
| LAND retarget down ≥ 50% | −67% ✅ | −54% ✅ |
| LAND idle up ≤ 10% | −25% ✅ | −28% ✅ |
| Landings down ≤ 10% | −4% ✅ | +18% ✅ |
| Overseas captures down ≤ 10% | 0% ✅ | **−18% ❌** (Δ −0.18 against a combined se of 0.17, about 1σ; the previous 20-game run showed 0%) |
| Mean turn ms up ≤ 25% | 8.1 → 0.7 ✅ | 16.9 → 2.2 ✅ |

Part of the LAND idle improvement is a measurement effect.
- **Before:** staged land units re-issued move-to-self orders, which counted as idle.
- **Now:** they sentry at the loading point, and sentried units are exempt from the idle metric.

## SEA/AIR "unchanged within noise": needs your call

The spec says "±2 standard errors" without saying which standard error. Two readings:

- **Combined:** |Δ| > 2·√(se² + se_baseline²). This is what `MetricsTable` flags. No SEA or AIR
  row fails on either map.
- **Strict:** |Δ| > 2·se_baseline, counting only moves in the bad direction.
  - MedMap: every row passes.
  - BorderLand: five rows fail:

    | Metric | Baseline | Now |
    |---|---|---|
    | AIR.idle | 0.001 | 0.019 |
    | AIR.oscillation | 0.000 | 0.002 |
    | AIR.retarget | 0.157 | 0.431 |
    | SEA.oscillation | 0.117 | 0.150 |
    | fuelLosses | 3.3 | 6.9 per player |

    The AIR baselines on BorderLand are close to zero, so tiny absolute changes fail. The
    fuel-loss increase is the one that matters.

The spec assumed navy and air would be untouched. They aren't, for four reasons:
- Memory now persists, so the destroyer ESCORT role never clears.
- `shouldDefend` is deterministic and feeds cargo-plane zones.
- Targets are explored-only.
- Disabling operation-driven production changes what gets built.

Sub-project 3 (navy and air) is where these get addressed.

## What changed along the way

- **Operation-driven production is disabled.** Shared memory exposed that operations never
  staff themselves, so every coastal city built transports forever. Landings fell from 6.6 to
  1.0, found by bisecting to the shared-memory commit. Re-enable in sub-project 2.
- **Transport commitments were removed.** Committed transports sailed to their first-chosen
  beach instead of the nearest one. Over 20 games, landings were 11.5 ± 1.4 with them and
  16.2 ± 1.6 without.
- **Land targets.** Land units target only cities on their own continent. Paths through
  unexplored hexes had let them chase cities across water. Units aboard transports are exempt,
  so they still pick a landing target.
- **Exploring before staging.** Land units explore their home continent before staging for an
  overseas target.
- **Operation pre-emption.** An operation pre-empts a commitment only when it has an order for
  the unit.
- **Abandoned targets.** After giving up on a target, a unit avoids it for 10 turns. This stops
  a loop of release, recommit, release.

## Findings for later sub-projects

- **Operations** only claim units when first planned (sub-project 2).
- **Fuel losses** run at about 7 aircraft per player per game, with a rise on BorderLand
  (sub-project 3).
- **Naval attacks** are about 0.15–0.26 per 100 sea moves (sub-project 3).

## Raw tables (final run)

### MedMap

```
metric                       mean      ±se   baseline        Δ%  
AIR.idle                    0.079    0.015      0.062      +28%  
AIR.oscillation             0.016    0.008      0.010      +59%  
AIR.retarget                0.948    0.329      0.958       -1%  
LAND.idle                   0.212    0.035      0.284      -25%  
LAND.oscillation            0.123    0.013      0.294      -58%  
LAND.retarget               0.164    0.014      0.492      -67%  
SEA.idle                    0.111    0.016      0.124      -11%  
SEA.oscillation             0.167    0.016      0.158       +5%  
SEA.retarget                1.366    0.134      1.290       +6%  
emptySailing                0.441    0.037      0.488      -10%  
fuelLosses                  7.650    1.742      5.500      +39%  
landings                   14.925    1.431     15.625       -4%  
navalAttacksPer100          0.262    0.075      0.393      -33%  
overseasCaptures            1.850    0.199      1.850       +0%  
seaLoad                     2.216    0.153      1.683      +32%  
mean turn ms: 0.7
```

### BorderLand

```
metric                       mean      ±se   baseline        Δ%  
AIR.idle                    0.019    0.014      0.001    +1560%  
AIR.oscillation             0.002    0.001      0.000     +375%  
AIR.retarget                0.431    0.259      0.157     +175%  
LAND.idle                   0.151    0.042      0.209      -28%  
LAND.oscillation            0.076    0.011      0.169      -55%  
LAND.retarget               0.250    0.041      0.543      -54%  
SEA.idle                    0.111    0.019      0.085      +30%  
SEA.oscillation             0.150    0.017      0.117      +29%  
SEA.retarget                0.936    0.090      1.053      -11%  
emptySailing                0.482    0.041      0.522       -8%  
fuelLosses                  6.900    2.119      3.300     +109%  
landings                    7.775    1.270      6.600      +18%  
navalAttacksPer100          0.154    0.053      0.137      +12%  
overseasCaptures            0.800    0.099      0.975      -18%  
seaLoad                     2.010    0.168      1.723      +17%  
mean turn ms: 2.2
```

