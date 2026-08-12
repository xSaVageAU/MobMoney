# Mob Money: Dimension & Attribute Pricing

An addon for [Mob Money](../README.md) that lets kill payouts vary by **which dimension** a mob
died in and **how buffed its attributes are** (max health, attack damage, etc.), on top of Mob
Money's normal per-mob price list. It's built entirely on Mob Money's public
`MobMoneyEvents.PRICE_MODIFIER` extension hook - Mob Money's own core has no opinion on this at
all, so if you don't install this addon, nothing changes.

Requires **Mob Money** and **Fabric API** to already be installed.

## The one rule to hold in your head

> **Listed = that multiplier applies. Unlisted = untouched.**

There's no separate "everything else" setting anywhere in this config. If a dimension or
attribute isn't mentioned, it simply has no effect (multiplier of `1.0`).

## Where the config lives

`config/mobmoney-testaddon.json`, generated automatically the first time the server starts with
this addon installed. Edit it and restart (or reload, if your server supports config reloading)
to apply changes.

## Config reference

```json
{
  "dimensionMultipliers": {
    "minecraft:the_end": 2.0
  },
  "attributeScaling": [
    { "attribute": "minecraft:max_health", "weight": 1.0 }
  ],
  "perMobOverrides": {
    "minecraft:enderman": {
      "dimensionMultipliers": { "minecraft:the_end": 3.0 }
    }
  }
}
```

### `dimensionMultipliers` (object, dimension ID → number)

Applies to **every mob** unless a mob has its own override (see below). The killed mob's current
dimension is looked up in this map; if found, price is multiplied by that number. If not found,
nothing happens.

### `attributeScaling` (array of `{ "attribute": ..., "weight": ... }`)

Applies to **every mob** unless overridden. For each rule:

1. The mob's *current* value for that attribute is read.
2. It's compared against *that entity type's own default value* for the same attribute - not a
   fixed number. A zombie's baseline is 20 HP, a wither's is 300 HP; both are handled correctly
   automatically, because the comparison is always "this specific mob vs. what this specific mob
   type normally has."
3. `multiplier = (current / baseline) ^ weight`

`weight` controls sensitivity:

| weight | effect |
|---|---|
| `1.0` | Full linear scaling - 2x the baseline value gives 2x price from this rule |
| `0.5` | Dampened (square-root-like) scaling - 2x baseline gives ~1.41x price |
| `2.0` | Exaggerated scaling - 2x baseline gives 4x price |
| `0.0` | Rule has no effect (same as removing it) |

Multiple rules in the same list all multiply together. Using the power formula means the result
is always positive, no matter how small or large the ratio gets.

If an attribute ID is misspelled, unregistered (e.g. from a mod that isn't installed), or the
specific mob type doesn't have that attribute at all, that rule is silently skipped rather than
breaking the payout - check the server log for a warning if a rule doesn't seem to be firing.

### `perMobOverrides` (object, entity ID → override)

```json
"perMobOverrides": {
  "minecraft:enderman": {
    "dimensionMultipliers": { "minecraft:the_end": 3.0 },
    "attributeScaling": [ { "attribute": "minecraft:max_health", "weight": 1.5 } ]
  }
}
```

Lets a specific mob use completely different rules than the global ones above. This is a
**replacement, not a merge**, and it applies **per axis independently**:

- If a mob's override includes `dimensionMultipliers`, that map is used *instead of* the global
  one for that mob - the global one is ignored entirely for that mob's dimension pricing.
- If a mob's override omits `dimensionMultipliers` (or omits the whole override), it just uses
  the global map.
- The same independent rule applies to `attributeScaling`.

So you can override just one axis for a mob and still inherit the global rule for the other.

### How dimension and attribute pricing combine

They always **both** apply and multiply together:

```
final price = base price × dimensionMultiplier × attributeMultiplier
```

This means you never need "if/or" logic to express "bonus if X or Y or both" - each condition
that's true contributes its own factor, and a condition that doesn't apply is just `1.0` and
vanishes from the multiplication. An enderman killed in the End (dimension factor fires) that's
also been buffed to 40 HP (attribute factor fires) gets both bonuses multiplied together
automatically.

## Finding valid IDs

All IDs are `namespace:path`, same as any Minecraft identifier (item IDs, block IDs, etc.).

**Dimension IDs** - vanilla: `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end`.
For a modded dimension, check that mod's documentation, or stand in it and check the F3 debug
screen (top-left, "Dimension:" or similar).

**Entity IDs** - vanilla examples: `minecraft:zombie`, `minecraft:enderman`,
`minecraft:creeper`, `minecraft:wither`. Tab-complete works in `/summon` in-game to browse valid
IDs, including modded ones.

**Attribute IDs** - the full vanilla list (all `minecraft:` namespace):

`max_health`, `attack_damage`, `attack_speed`, `attack_knockback`, `armor`, `armor_toughness`,
`knockback_resistance`, `movement_speed`, `follow_range`, `step_height`, `jump_strength`,
`gravity`, `scale`, `luck`, `max_absorption`, `safe_fall_distance`, `fall_damage_multiplier`,
`spawn_reinforcements`, `sweeping_damage_ratio`, `block_interaction_range`,
`entity_interaction_range`, `mining_efficiency`, `sneaking_speed`, `submerged_mining_speed`,
`water_movement_efficiency`, `movement_efficiency`, `oxygen_bonus`, `burning_time`,
`explosion_knockback_resistance`, `block_break_speed`, `flying_speed`, `tempt_range`,
`camera_distance`, `friction_modifier`, `air_drag_modifier`, `bounciness`,
`below_name_distance`, `name_tag_distance`, `waypoint_transmit_range`,
`waypoint_receive_range`.

The most useful ones for pricing are `max_health` and `attack_damage`; the rest exist mostly for
completeness or apply mainly to players. Mob-scaling or RPG mods may register their own
attribute IDs under their own namespace (e.g. `somemod:mob_level`) - those work here too, as
long as the mod that registers them is installed.

## Worked examples

**Flat dimension bonus, no attribute scaling:**
```json
{
  "dimensionMultipliers": { "minecraft:the_nether": 1.5, "minecraft:the_end": 2.0 },
  "attributeScaling": [],
  "perMobOverrides": {}
}
```

**Tougher mobs pay more, everywhere, regardless of dimension:**
```json
{
  "dimensionMultipliers": {},
  "attributeScaling": [ { "attribute": "minecraft:max_health", "weight": 1.0 } ],
  "perMobOverrides": {}
}
```

**Only endermen get a bonus, and only in the End or when buffed:**
```json
{
  "dimensionMultipliers": {},
  "attributeScaling": [],
  "perMobOverrides": {
    "minecraft:enderman": {
      "dimensionMultipliers": { "minecraft:the_end": 2.0 },
      "attributeScaling": [ { "attribute": "minecraft:max_health", "weight": 1.0 } ]
    }
  }
}
```

**Global Nether bonus for everyone, but blazes get an even bigger one:**
```json
{
  "dimensionMultipliers": { "minecraft:the_nether": 1.5 },
  "attributeScaling": [],
  "perMobOverrides": {
    "minecraft:blaze": {
      "dimensionMultipliers": { "minecraft:the_nether": 3.0 }
    }
  }
}
```

## Using an AI assistant to write your config

This file is written to be self-contained enough to hand to an LLM directly. To get a config
generated for you, paste this entire README into your assistant of choice along with a plain
description of what you want, for example:

> Here's the documentation for a Minecraft mod config. Using it, write me a
> `mobmoney-testaddon.json` where: creepers are worth triple in the Nether and the End, any mob
> with more than double its normal max health pays 2x regardless of where it's killed, and
> withers specifically always pay a flat 5x no matter what.

The schema, matching rules, and ID formats above are everything needed to produce a valid file -
double-check dimension/entity/attribute IDs against the lists above (or your installed mods) if
the assistant guesses at a modded one it isn't certain of.

## Troubleshooting

If a rule doesn't seem to be firing, check the server log for a line from `mobmoney-testaddon`:

```
minecraft:creeper - dimension x1.5, attribute x1.0 - 10.0 -> 15.0
```

This only logs when at least one multiplier isn't `1.0`, so a normal, unaffected kill logs
nothing - that's expected, not a sign something's broken. If you expected a multiplier to apply
and don't see this line at all, double check the entity ID, dimension ID, and attribute ID in
your config are spelled exactly as the game registers them (see "Finding valid IDs" above).
