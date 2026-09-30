# Robot AI Commitments: Results vs Baseline

Spec: `2026-09-30-robot-ai-commitments-design.md`.

**Method.** `AITestCLI metrics --games 20 --turn-limit 150 --seed 1000`, with both players
running the default AI. Every game reached the turn limit. Baselines were recorded before any AI
change, in `ai-configs/baselines/2026-09-baseline-{map}.json`. Seeded runs are not
bit-reproducible, so the results compare means and standard errors.

## Success criteria

| Criterion | MedMap | BorderLand |
|---|---|---|
| LAND oscillation down ≥ 50% | −55% ✅ | −51% ✅ |
| LAND retarget down ≥ 50% | −62% ✅ | −60% ✅ |
| LAND idle up ≤ 10% | −23% ✅ | −28% ✅ |
| Landings down ≤ 10% | +7% ✅ | +4% ✅ |
| Overseas captures down ≤ 10% | +12% ✅ | 0% ✅ |
| SEA/AIR within ±2 se | ✅ (no row flagged) | ✅ (no row flagged; SEA.oscillation and AIR.oscillation close to the edge) |
| Mean turn ms up ≤ 25% | 8.1 → 0.8 ✅ | 16.9 → 1.9 ✅ |

The comparison tool flags a row with `!` when it moved the wrong way by more than two combined
standard errors. No row was flagged on either map.

## What changed along the way

- **Operation-driven production.** Sharing one `StrategyMemory` across turns exposed that
  operations never staff themselves after creation. Every coastal city then built transports
  forever: landings fell from 6.6 to 1.0 in 100-turn runs, found by bisecting to the
  shared-memory commit.
  - Operation-driven production is disabled until sub-project 2 fixes staffing. Production is
    now what the old AI effectively had, because its memory was wiped every turn.
- **Transport commitments were removed.** Committed transports kept sailing to the beach they
  first picked instead of the nearest one.
  - Landings were 11.5 ± 1.4 with transport commitments and 16.2 ± 1.6 without them.
  - Transports go back to planning every turn. Invasions are reworked in sub-project 2.
- **Land targets.** Land units only target cities on their own continent. Previously, paths
  through unexplored hexes let them chase cities across water.
- **Exploring before staging.** Land units explore their home continent before staging for an
  overseas target.
- **Operation pre-emption.** An operation only pre-empts a commitment when it has an order for
  the unit. Without this, units flipped between STAGE and OPERATION every turn.

## Findings for later sub-projects

- **Operations** only claim units when first planned, so they can't be relied on to produce or
  staff anything yet (sub-project 2).
- **Fuel losses** are high and noisy: about 7 aircraft per player per game on both maps
  (sub-project 3).
- **Naval attacks** are about 0.1–0.4 per 100 sea moves (sub-project 3).

## Raw tables

### MedMap

```
metric                       mean      ±se   baseline        Δ%  
AIR.idle                    0.099    0.023      0.062      +62%  
AIR.oscillation             0.008    0.003      0.010      -24%  
AIR.retarget                0.931    0.211      0.958       -3%  
LAND.idle                   0.218    0.033      0.284      -23%  
LAND.oscillation            0.134    0.016      0.294      -55%  
LAND.retarget               0.187    0.020      0.492      -62%  
SEA.idle                    0.126    0.015      0.124       +2%  
SEA.oscillation             0.161    0.014      0.158       +2%  
SEA.retarget                1.188    0.082      1.290       -8%  
emptySailing                0.475    0.039      0.488       -3%  
fuelLosses                  6.900    1.547      5.500      +25%  
landings                   16.750    1.774     15.625       +7%  
navalAttacksPer100          0.327    0.071      0.393      -17%  
overseasCaptures            2.075    0.179      1.850      +12%  
seaLoad                     2.002    0.175      1.683      +19%  
mean turn ms: 0.8
```

### BorderLand

```
metric                       mean      ±se   baseline        Δ%  
AIR.idle                    0.019    0.014      0.001    +1507%  
AIR.oscillation             0.014    0.008      0.000    +2703%  
AIR.retarget                0.350    0.138      0.157     +123%  
LAND.idle                   0.150    0.046      0.209      -28%  
LAND.oscillation            0.083    0.012      0.169      -51%  
LAND.retarget               0.219    0.037      0.543      -60%  
SEA.idle                    0.104    0.022      0.085      +22%  
SEA.oscillation             0.158    0.019      0.117      +35%  
SEA.retarget                0.995    0.104      1.053       -5%  
emptySailing                0.444    0.048      0.522      -15%  
fuelLosses                  7.550    2.171      3.300     +129%  
landings                    6.875    0.807      6.600       +4%  
navalAttacksPer100          0.115    0.046      0.137      -16%  
overseasCaptures            0.975    0.123      0.975       +0%  
seaLoad                     2.108    0.218      1.723      +22%  
mean turn ms: 1.9
```

