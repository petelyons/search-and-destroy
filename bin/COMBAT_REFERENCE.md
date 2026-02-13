# Combat Effectiveness Reference Guide

## Overview

Combat in Search & Destroy uses a rock-paper-scissors system where different unit types have advantages and disadvantages against each other. This guide explains how combat resolution works and which units are effective against which targets.

---

## Core Combat Mechanics

### Basic Combat System

1. **Turn-Based Resolution**: Combat continues in rounds until one unit is destroyed
2. **50% Hit Chance**: Each round, both attacker and defender have a 50% chance to score a hit
3. **Damage Calculation**: When a hit occurs, damage = `Base Attack × Effectiveness Multiplier`
4. **Simultaneous Combat**: Both units can strike in the same round
5. **Health System**: Each unit has hit points - when reduced to 0, the unit is destroyed

### Combat Flow

```
Each Combat Round:
  1. Attacker attempts to hit (50% chance)
     - If successful: Apply (Attack × Effectiveness) damage to defender
  2. Defender attempts to counterattack (50% chance)
     - If successful: Apply (Attack × Effectiveness) damage to attacker
  3. Repeat until one unit is destroyed
```

---

## Effectiveness Multipliers

Units receive damage modifiers based on their matchup with the enemy:

| Matchup Type | Multiplier | Effect | Example |
|--------------|-----------|---------|---------|
| **Primary Target** | 1.5x | +50% damage | Fighter vs Bomber |
| **Secondary Target** | 1.2x | +20% damage | Fighter vs Destroyer |
| **Poor Matchup** | 0.75x | -25% damage | Infantry vs Battleship |
| **Non-Combat Unit** | 0.5x | -50% damage | Transport vs anything |

### Important Notes

- Effectiveness applies to **BOTH** attacking and defending
- If Fighter (Attack=1) fights Bomber (Attack=3):
  - Fighter deals 1.5 damage per hit (primary target)
  - Bomber deals 2.25 damage per hit (primary target)
- The multiplier is **bidirectional** - both units use their effectiveness when they strike

---

## Unit Statistics

| Unit | Move | HP | Attack | Cost | Vision | Special Abilities |
|------|------|----|----|------|--------|-------------------|
| **Infantry** | 1 | 2 | 1 | 5 | 1 (Surface) | Cheap garrison unit |
| **Armor** | 2 | 4 | 2 | 10 | 1 (Surface) | Strong ground unit |
| **Fighter** | 5 | 2 | 1 | 10 | 3 (Surface) | Air superiority |
| **Bomber** | 4 | 2 | 3 | 15 | 3 (Surface) | Ground attack |
| **Cargo Plane** | 3 | 2 | 0 | 15 | 5 (Surface) | Carries 1 Infantry |
| **Destroyer** | 3 | 3 | 3 | 20 | 2 (Complete) | Sub hunter, sees submarines |
| **Transport** | 2 | 2 | 0 | 30 | 1 (Surface) | Carries 6 land units |
| **Submarine** | 2 | 4 | 4 | 30 | 2 (Water only) | Stealth attacker |
| **Cruiser** | 2 | 8 | 3 | 40 | 3 (Complete) | Powerful AA, sees submarines |
| **Carrier** | 2 | 6 | 1 | 50 | 2 (Surface) | Carries 6 Fighters |
| **Battleship** | 2 | 12 | 4 | 50 | 2 (Surface) | Heaviest firepower |

### Vision Types
- **Surface**: Sees land and sea units (NOT submarines)
- **Complete**: Sees everything including submarines
- **Water**: Only sees water/sea hexes and units (submarines use this)

---

## Unit Matchup Tables

### Land Units

#### Infantry (Attack: 1)

| vs Target | Effectiveness | Damage/Hit | Reasoning |
|-----------|--------------|------------|-----------|
| **Infantry** | Primary (1.5x) | 1.5 | Squad tactics, equal combat |
| **Armor** | Secondary (1.2x) | 1.2 | AT weapons, mines |
| **Transport** | Secondary (1.2x) | 1.2 | Vulnerable cargo ship |
| Bomber | Poor (0.75x) | 0.75 | Hard to hit aircraft |
| Fighter | Poor (0.75x) | 0.75 | Hard to hit aircraft |
| Destroyer | Poor (0.75x) | 0.75 | Naval unit, poor matchup |
| Cruiser | Poor (0.75x) | 0.75 | Naval unit, poor matchup |
| Battleship | Poor (0.75x) | 0.75 | Naval unit, poor matchup |
| Carrier | Poor (0.75x) | 0.75 | Naval unit, poor matchup |
| Submarine | Poor (0.75x) | 0.75 | Naval unit, poor matchup |
| Cargo | Poor (0.75x) | 0.75 | Air unit, poor matchup |

**Best Used Against**: Other infantry, secondary effectiveness vs armor
**Vulnerable To**: Armor, bombers, fighters

---

#### Armor (Attack: 2)

| vs Target | Effectiveness | Damage/Hit | Reasoning |
|-----------|--------------|------------|-----------|
| **Infantry** | Primary (1.5x) | 3 | Tank superiority vs foot soldiers |
| **Armor** | Secondary (1.2x) | 2.4 | Tank vs tank warfare |
| **Transport** | Secondary (1.2x) | 2.4 | Easy naval target |
| Bomber | Poor (0.75x) | 1.5 | Vulnerable to air attack |
| Fighter | Poor (0.75x) | 1.5 | Vulnerable to air attack |
| Destroyer | Poor (0.75x) | 1.5 | Can't engage naval effectively |
| Cruiser | Poor (0.75x) | 1.5 | Can't engage naval effectively |
| Battleship | Poor (0.75x) | 1.5 | Can't engage naval effectively |
| Carrier | Poor (0.75x) | 1.5 | Can't engage naval effectively |
| Submarine | Poor (0.75x) | 1.5 | Can't engage naval effectively |
| Cargo | Poor (0.75x) | 1.5 | Air unit, poor matchup |

**Best Used Against**: Infantry (devastating), other armor
**Vulnerable To**: Bombers (3x primary target damage!), fighters

---

### Air Units

#### Fighter (Attack: 1)

| vs Target | Effectiveness | Damage/Hit | Reasoning |
|-----------|--------------|------------|-----------|
| **Transport** | Primary (1.5x) | 1.5 | Strafing undefended cargo |
| **Armor** | Primary (1.5x) | 1.5 | Ground attack capability |
| **Infantry** | Primary (1.5x) | 1.5 | Strafing ground forces |
| **Bomber** | Primary (1.5x) | 1.5 | Air superiority fighter |
| **Cargo** | Primary (1.5x) | 1.5 | Defenseless air target |
| **Fighter** | Primary (1.5x) | 1.5 | Dogfighting |
| **Destroyer** | Secondary (1.2x) | 1.2 | Can strafe small ships |
| **Cruiser** | Secondary (1.2x) | 1.2 | Can attack but risky (AA fire) |
| **Battleship** | Secondary (1.2x) | 1.2 | Can attack but risky (AA fire) |
| **Carrier** | Secondary (1.2x) | 1.2 | Can attack but risky (AA fire) |
| Submarine | Poor (0.75x) | 0.75 | Hard to spot underwater |

**Best Used Against**: Almost everything! Air superiority and ground attack
**Vulnerable To**: Other fighters, AA from cruisers/destroyers

---

#### Bomber (Attack: 3)

| vs Target | Effectiveness | Damage/Hit | Reasoning |
|-----------|--------------|------------|-----------|
| **Transport** | Primary (1.5x) | 4.5 | Bombing undefended ships |
| **Armor** | Primary (1.5x) | 4.5 | Designed to destroy armor |
| **Infantry** | Primary (1.5x) | 4.5 | Carpet bombing ground forces |
| **Destroyer** | Secondary (1.2x) | 3.6 | Can bomb naval targets |
| **Cruiser** | Secondary (1.2x) | 3.6 | Can bomb naval targets |
| **Battleship** | Secondary (1.2x) | 3.6 | Can bomb naval targets |
| **Carrier** | Secondary (1.2x) | 3.6 | Can bomb naval targets |
| Bomber | Poor (0.75x) | 2.25 | Not designed for dogfighting |
| Cargo | Poor (0.75x) | 2.25 | Not a priority target |
| Fighter | Poor (0.75x) | 2.25 | Vulnerable to fighters |
| Submarine | Poor (0.75x) | 2.25 | Hard to spot underwater |

**Best Used Against**: Ground forces (infantry/armor), transports - devastating damage!
**Vulnerable To**: Fighters (poor dogfighting ability)

---

#### Cargo Plane (Attack: 0)

| vs Target | Effectiveness | Damage/Hit | Reasoning |
|-----------|--------------|------------|-----------|
| ALL | Non-Combat (0.5x) | 0 | Unarmed transport aircraft |

**Best Used For**: Transport only - avoid all combat!
**Vulnerable To**: Everything - no offensive capability

---

### Naval Units

#### Destroyer (Attack: 3)

| vs Target | Effectiveness | Damage/Hit | Reasoning |
|-----------|--------------|------------|-----------|
| **Transport** | Primary (1.5x) | 4.5 | Easy target for destroyer |
| **Submarine** | Primary (1.5x) | 4.5 | Dedicated sub hunter with sonar |
| **Destroyer** | Secondary (1.2x) | 3.6 | Ship vs ship combat |
| **Bomber** | Secondary (1.2x) | 3.6 | AA defense capability |
| **Fighter** | Secondary (1.2x) | 3.6 | AA defense capability |
| Armor | Poor (0.75x) | 2.25 | Can't engage land effectively |
| Infantry | Poor (0.75x) | 2.25 | Can't engage land effectively |
| Cruiser | Poor (0.75x) | 2.25 | Outgunned by larger ships |
| Battleship | Poor (0.75x) | 2.25 | Outgunned by larger ships |
| Carrier | Poor (0.75x) | 2.25 | Not primary role |
| Cargo | Poor (0.75x) | 2.25 | Not a priority target |

**Best Used Against**: Submarines (primary role!), transports
**Vulnerable To**: Battleships, cruisers (larger ships)

---

#### Cruiser (Attack: 3)

| vs Target | Effectiveness | Damage/Hit | Reasoning |
|-----------|--------------|------------|-----------|
| **Transport** | Primary (1.5x) | 4.5 | Powerful vs cargo ships |
| **Destroyer** | Primary (1.5x) | 4.5 | Superior firepower vs smaller ships |
| **Submarine** | Primary (1.5x) | 4.5 | Sonar and depth charges |
| **Cruiser** | Secondary (1.2x) | 3.6 | Ship vs ship combat |
| **Bomber** | Secondary (1.2x) | 3.6 | Strong AA battery |
| **Fighter** | Secondary (1.2x) | 3.6 | Strong AA battery |
| Armor | Poor (0.75x) | 2.25 | Can't engage land effectively |
| Infantry | Poor (0.75x) | 2.25 | Can't engage land effectively |
| Battleship | Poor (0.75x) | 2.25 | Outgunned by battleship |
| Carrier | Poor (0.75x) | 2.25 | Not primary role |
| Cargo | Poor (0.75x) | 2.25 | Not a priority target |

**Best Used Against**: Destroyers, submarines, aircraft (excellent AA)
**Vulnerable To**: Battleships, submarines (if surprised)

---

#### Battleship (Attack: 4)

| vs Target | Effectiveness | Damage/Hit | Reasoning |
|-----------|--------------|------------|-----------|
| **Transport** | Primary (1.5x) | 6 | Devastating firepower |
| **Carrier** | Primary (1.5x) | 6 | Designed to sink capital ships |
| **Battleship** | Primary (1.5x) | 6 | Ship-of-the-line combat |
| **Destroyer** | Secondary (1.2x) | 4.8 | Can engage smaller ships |
| **Cruiser** | Secondary (1.2x) | 4.8 | Can engage smaller ships |
| Armor | Poor (0.75x) | 3 | Can't engage land effectively |
| Infantry | Poor (0.75x) | 3 | Can't engage land effectively |
| Submarine | Poor (0.75x) | 3 | Vulnerable to torpedo attack |
| Bomber | Poor (0.75x) | 3 | Limited AA capability |
| Fighter | Poor (0.75x) | 3 | Limited AA capability |
| Cargo | Poor (0.75x) | 3 | Not a priority target |

**Best Used Against**: Other capital ships (carriers, battleships)
**Vulnerable To**: Submarines (torpedoes!), bombers

---

#### Submarine (Attack: 4)

| vs Target | Effectiveness | Damage/Hit | Reasoning |
|-----------|--------------|------------|-----------|
| **Transport** | Primary (1.5x) | 6 | Torpedo attack on cargo |
| **Carrier** | Primary (1.5x) | 6 | Torpedo attack on capital ships |
| **Battleship** | Primary (1.5x) | 6 | Torpedo attack on capital ships |
| **Cruiser** | Secondary (1.2x) | 4.8 | Can engage but risky (sonar) |
| **Submarine** | Secondary (1.2x) | 4.8 | Sub vs sub warfare |
| Armor | Poor (0.75x) | 3 | Can't engage land |
| Infantry | Poor (0.75x) | 3 | Can't engage land |
| Destroyer | Poor (0.75x) | 3 | Destroyers designed to hunt subs! |
| Bomber | Poor (0.75x) | 3 | Limited AA while surfaced |
| Fighter | Poor (0.75x) | 3 | Limited AA while surfaced |
| Cargo | Poor (0.75x) | 3 | Not a priority target |

**Best Used Against**: Capital ships (stealth torpedo attacks)
**Vulnerable To**: Destroyers and cruisers (sonar detection)
**Special**: Only visible to units with "Complete" vision (destroyers, cruisers)

---

#### Carrier (Attack: 1)

| vs Target | Effectiveness | Damage/Hit | Reasoning |
|-----------|--------------|------------|-----------|
| **Transport** | Secondary (1.2x) | 1.2 | Can engage defenseless ships |
| ALL OTHERS | Poor (0.75x) | 0.75 | Limited self-defense capability |

**Best Used For**: Fighter platform - carriers themselves are weak in direct combat
**Vulnerable To**: Everything except transports - protect with escorts!

---

#### Transport (Attack: 0)

| vs Target | Effectiveness | Damage/Hit | Reasoning |
|-----------|--------------|------------|-----------|
| ALL | Non-Combat (0.5x) | 0 | Unarmed cargo vessel |

**Best Used For**: Transport only - avoid all combat!
**Vulnerable To**: Everything - most valuable target for enemy submarines and aircraft

---

## Strategic Implications

### Rock-Paper-Scissors Relationships

**Land Warfare:**
- Armor > Infantry (3 damage vs 1.5 damage)
- Infantry = Infantry (even matchup)
- Both vulnerable to air attack

**Air Warfare:**
- Fighter > Bomber (1.5 vs 2.25, but fighter has initiative)
- Bomber > Ground Units (devastating 4.5 damage)
- Cargo = Defenseless

**Naval Warfare:**
- Battleship > Carrier/Battleship (6 damage to capital ships)
- Submarine > Battleship/Carrier (stealth 6 damage torpedoes)
- Destroyer/Cruiser > Submarine (counter with sonar, 4.5 damage)
- All Ships > Transport (easy kills)

**Combined Arms:**
- Fighters counter bombers (air superiority)
- Bombers counter armor (4.5 damage!)
- Destroyers counter submarines (dedicated hunters)
- Submarines counter battleships (stealth advantage)

---

## Combat Examples

### Example 1: Infantry vs Armor

**Infantry attacks Armor:**
- Infantry: Attack 1, HP 2
- Armor: Attack 2, HP 4

Each round:
- Infantry hits (50%): deals 1.2 damage (secondary target)
- Armor hits (50%): deals 3 damage (primary target)

**Result**: Armor heavily favored - deals 2.5x more damage per hit

---

### Example 2: Bomber vs Infantry

**Bomber attacks Infantry:**
- Bomber: Attack 3, HP 2
- Infantry: Attack 1, HP 2

Each round:
- Bomber hits (50%): deals 4.5 damage (primary target) - **KILLS IN ONE HIT**
- Infantry hits (50%): deals 0.75 damage (poor matchup)

**Result**: Bomber can one-shot infantry, infantry needs 3 hits to kill bomber

---

### Example 3: Submarine vs Destroyer

**Submarine attacks Destroyer:**
- Submarine: Attack 4, HP 4
- Destroyer: Attack 3, HP 3

Each round:
- Submarine hits (50%): deals 3 damage (poor matchup vs dedicated hunter)
- Destroyer hits (50%): deals 4.5 damage (primary target - sub hunter)

**Result**: Destroyer favored despite lower base attack - effectiveness matters!

---

## Design Philosophy

1. **Specialization Matters**: Units excel at their designed role
2. **No Universal Counter**: Every unit has weaknesses
3. **Combined Arms**: Best armies use multiple unit types
4. **Cost Efficiency**: Expensive units aren't always better (context matters)
5. **Positioning**: Vision and terrain can overcome raw stats
6. **Stealth Advantage**: Submarines effective because hard to detect
7. **Non-Combat Units**: Transport/Cargo should always be escorted

---

## Quick Reference: Best Counters

| Unit Type | Best Counter | Why |
|-----------|--------------|-----|
| Infantry | Armor, Bomber | 3+ damage per hit |
| Armor | Bomber, Fighter | Air units devastating vs armor |
| Fighter | Fighter | Dogfighting specialists |
| Bomber | Fighter | Bombers poor at air combat |
| Destroyer | Battleship, Cruiser | Larger ships outgun it |
| Cruiser | Battleship, Submarine | Battleship outguns, sub torpedoes |
| Battleship | Submarine | Stealth torpedo attack |
| Submarine | Destroyer, Cruiser | Sonar detection + depth charges |
| Carrier | Submarine, Bomber | Weak self-defense |
| Transport | Anything | Defenseless |
| Cargo | Anything | Defenseless |

---

**Version**: 1.0  
**Last Updated**: Based on UnitMatchups.java and Type.java  
**Combat System**: 50% hit chance, effectiveness multipliers, simultaneous strikes
