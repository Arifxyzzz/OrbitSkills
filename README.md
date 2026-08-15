<div align="center">

# OrbitSkills

**RPG progression for Paper servers — levels, stat points, races, clans, and class ranks.**

Players don't just grind EXP. They build a character.

[![Version](https://img.shields.io/badge/version-1.2.2-1E6F9F?style=flat-square)](CHANGELOG.md)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.x-237A4B?style=flat-square)](https://papermc.io/)
[![Java](https://img.shields.io/badge/java-21-A66A00?style=flat-square)](https://adoptium.net/)
[![Platform](https://img.shields.io/badge/platform-Paper-5B3F99?style=flat-square)](https://papermc.io/)
[![bStats](https://img.shields.io/badge/bStats-32371-A33A55?style=flat-square)](https://bstats.org/plugin/bukkit/OrbitSkills/32371)

</div>

---

## Table of Contents

- [About](#about)
- [Features](#features)
- [Installation](#installation)
- [Systems](#systems)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Level Rewards](#level-rewards)
- [Placeholders](#placeholders)
- [Integrations](#integrations)
- [Languages](#languages)
- [Building from Source](#building-from-source)
- [Changelog](#changelog)

---

## About

OrbitSkills is an RPG progression plugin for Minecraft servers. Leveling is only the entry point — each system feeds a different part of the character:

| System | Role |
| :-- | :-- |
| **Level** | Main progression. Grants SP and unlocks level rewards. |
| **Stat Points** | Spend on damage, defense, HP, and healing. |
| **Race** | Character origin. Gives stats plus passive effects. |
| **Clan** | Bloodline or guild. Stat bonuses and clan-only skills. |
| **Class Rank** | Quality multiplier per stat, from F to SSS. |
| **Fragments** | Currency for rerolling Race, Clan, and Class Rank. |

---

## Features

- **Leveling** from a wide set of activities — mob kills, mining, farming, woodcutting, digging, fishing, combat, archery, alchemy, enchanting, smelting, breeding, taming, shearing, beekeeping, advancements, and raids.
- **6 stats** — Archery, Fighting, Power, Defense, Health, Alchemy — each with its own scaling curve and soft cap.
- **14 races** with weighted rarity, roll chance, passives, and reroll fragments.
- **14 clans** with stat bonuses and clan-exclusive skills.
- **Class Rank F → SSS** as a per-stat multiplier, so two players with the same SP can still build differently.
- **Auto-reroll** until a target rarity is hit, with **Mythic pity** so bad luck can't run forever.
- **Scaled HP** — RPG values like 4000+ HP display on a locked heart bar instead of a wall of hearts.
- **Actionbar** HP and EXP progress with a notice window so one-off messages aren't wiped.
- **GUI menus** for levels, stats, races, classes, clan, guide, and leaderboard — fully slot-configurable.
- **PlaceholderAPI** support for scoreboards, tab lists, and third-party menus.
- **Additive YAML updates** — new keys merge in on update, your edits are never overwritten.

---

## Installation

1. Drop `OrbitSkills-1.2.2.jar` into your server's `plugins/` folder.
2. Restart the server (not `/reload`).
3. Edit the generated files in `plugins/OrbitSkills/`.
4. Apply changes with `/os reload`.

### Requirements

| | |
| :-- | :-- |
| **Java** | 21 |
| **Server** | Paper 1.21.x (recommended) |
| **API** | 1.21 |

> [!WARNING]
> CraftBukkit and older server versions are not supported. Spigot forks work, but see [Integrations](#integrations) for the heart-bar note.

### Optional dependencies

| Plugin | What it adds |
| :-- | :-- |
| [PlaceholderAPI](https://www.spigotmc.org/resources/6245/) | `%orbitskills_*%` placeholders |
| [Vault](https://www.spigotmc.org/resources/34315/) | Money rewards through your economy plugin |
| [MythicMobs](https://www.spigotmc.org/resources/5702/) | Per-mob EXP for custom mobs |
| [MMOItems](https://www.spigotmc.org/resources/39267/) | Custom weapons scored by their MMOItems type |
| [packetevents](https://www.spigotmc.org/resources/80279/) / [ProtocolLib](https://www.spigotmc.org/resources/1997/) | Packet-level heart bar lock on Spigot forks |

All of them are soft dependencies — nothing breaks if they're absent.

---

## Systems

### Stats

Six stats, allocated from SP earned per level. Default cap is **10,000 total SP** across **100 levels**.

| Stat | Effect |
| :-- | :-- |
| **Archery** | Projectile damage |
| **Fighting** | Melee damage with swords, axes, pickaxes, shovels, hoes, maces, tridents, shears |
| **Power** | Unarmed and non-tool attack damage |
| **Defense** | Incoming damage reduction |
| **Health** | Max HP |
| **Alchemy** | Potion and food healing efficiency, plus emergency regeneration |

An even endgame split is roughly **1,667 SP per stat** — about 100% base stat power before race, clan, and class bonuses. Past each stat's soft cap, points keep working at reduced efficiency (25% for damage, 20% Health, 15% Defense, 12% Alchemy) so specialist builds stay viable without scaling forever.

### Rarity tiers

`COMMON` → `UNCOMMON` → `RARE` → `EPIC` → `MYTHIC`

Used by both races and clans, with configurable roll weights per entry.

### Class ranks

| Rank | F | D | C | B | A | S | SS | SSS |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| **Multiplier** | 1.00× | 1.08× | 1.16× | 1.28× | 1.42× | 1.60× | 1.85× | 2.15× |

Each stat carries its own rank, rerolled independently with Stats Class Fragments.

### Races

`HUMAN` · `SLIME` · `GUARDIAN` · `DWARF` · `MERMAID` · `ORC` · `ELF` · `TITAN` · `GOLEM` · `KITSUNE` · `FROSTBORNE` · `VAMPIRE` · `DRAGONKIN` · `DEMON`

### Clans

`ASSASSIN` · `CELESTIAL` · `SHADOW` · `PHOENIX` · `PLAGUE` · `AETHER` · `ONI` · `STORMCALLER` · `RAIKAGE` · `BLAZEFURY` · `WARDENBORN` · `VOIDWALKER` · `NECROMANCER` · `ASTRAL`

### Fragments

| Fragment | Item | Default cost |
| :-- | :-- | :-- |
| Stats Class | `NETHER_STAR` | 1 per reroll |
| Race | `AMETHYST_SHARD` | 3 per reroll |
| Clan | `ECHO_SHARD` | 3 per reroll |

Fragments drop from mobs (configurable in `source/Fragments.yml`) or are handed out via commands and level rewards.

---

## Commands

### Player

| Command | Description |
| :-- | :-- |
| `/skills`, `/stats` | Open the stats menu |
| `/level` | Open level rewards |
| `/race` | Show or reroll your race |
| `/clan` | Show your clan |
| `/profile [player]` | Show a player's chat profile |
| `/os guide` | Open the player guide |
| `/os leaderboard` | Open the leaderboard |
| `/os class` | Open the stats class menu |
| `/orbitskills` | Command help |

**Aliases for `/orbitskills`:** `os`, `orbit`, `oskills`, `shadowynlevel`, `slv`, `slc`, `slevel`

### Admin

<details>
<summary><b>Give</b></summary>

```bash
/os give point <player|*> <amount>
/os give level <player|*> <amount>
/os give exp <player|*> <amount>
/os give fragment race <player|*> <amount>
/os give fragment class <player|*> <amount>
/os give fragment clan <player|*> <amount>
```

</details>

<details>
<summary><b>Set</b></summary>

```bash
/os set level <player> <value>
/os set race <player> <race>
/os set clan <player> <clan>
/os set class <player> <stat> <F-SSS>
```

</details>

<details>
<summary><b>Modules & maintenance</b></summary>

```bash
/os disable <stats|class|race|clan|all>   # e.g. during PvP events
/os enable  <stats|class|race|clan|all>
/os scanall [fix]                         # check or repair player SP totals
/os placeholder                           # list every placeholder
/os reload
/resetdata <player>
```

</details>

---

## Permissions

| Node | Default | Grants |
| :-- | :-- | :-- |
| `orbitskills.use` | `true` | Normal plugin access |
| `orbitskills.admin` | `op` | All admin commands |
| `orbitskills.multiplier.2` | `false` | 2× EXP multiplier |
| `orbitskills.multiplier.3` | `false` | 3× EXP multiplier |
| `orbitskills.multiplier.4` | `false` | 4× EXP multiplier |

Legacy nodes `shadowyn.admin`, `shadowynlevel.use`, and `shadowynlevel.2/3/4` are still honored.

---

## Configuration

```
plugins/OrbitSkills/
├── config.yml              # global settings and balance knobs
├── level.yml               # level rewards and EXP curve
├── lang/
│   └── Lang<CODE>.yml      # 13 languages, EN by default
├── menu/
│   ├── Main.yml            # titles, slots, item visuals
│   ├── Stats.yml
│   ├── Points.yml
│   ├── Races.yml
│   ├── Clan.yml
│   ├── Level.yml
│   ├── Guide.yml
│   └── Leaderboard.yml
└── source/
    ├── Grinding.yml        # EXP per activity
    ├── Races.yml           # race entries and passives
    ├── Clans.yml           # clan stat bonuses
    └── Fragments.yml       # fragment items and mob drops
```

### Key settings

| Key | Default | Description |
| :-- | :-- | :-- |
| `settings.language` | `EN` | Language file to load |
| `settings.max-level` | `100` | Highest reachable level |
| `settings.max-total-stat-points` | `10000` | SP cap per player |
| `settings.stat-point-curve-linear-weight` | `0.5` | `0.5` = SP per level rises gradually; `1.0` = flat |
| `settings.physical-health-cap` | `1024` | Real HP stored by Minecraft |
| `settings.health-display-scaling` | `true` | Lock the heart bar to a fixed size |
| `settings.health-display-hearts` | `20.0` | Hearts shown on the locked bar (2–40) |
| `settings.health-display-keep-ratio` | `true` | Keep the bar still when max health changes |
| `settings.actionbar-notice-ms` | `2500` | How long a one-off notice holds the actionbar |
| `settings.save-interval-seconds` | `180` | Player data autosave interval |
| `modules.stats/class/race/clan` | `true` | Master toggles per system |

> [!NOTE]
> Updates are **additive**. Missing keys are merged in automatically, and customized values are preserved. Old defaults are migrated to the new balance where it's safe to do so.

---

## Level Rewards

`level.yml` is command-first so it works with any economy, crate, or rank plugin. Use `rewards.commands` for money, keys, fragments, or third-party integrations. Legacy direct rewards (`money`, `items`, `clan-fragments`, `rank-fragments`) still work.

```yml
levels:
  10:
    reward-description:
      - "{line}"
      - "&#FFB84DMilestone Rewards"
      - "&8- &7Race Fragment: &#C77DFF8x"
      - "&8- &7Stats Class Fragment: &#5CC8FF3x"
    rewards:
      commands:
        - "orbitskills give racefragment %player% 8"
        - "orbitskills give classfragment %player% 3"
```

Reaching level 100 takes roughly **30 million total EXP** on the default curve — early levels stay accessible while late levels get progressively more demanding.

---

## Placeholders

Requires PlaceholderAPI. Run `/os placeholder` in-game for the complete list.

<details>
<summary><b>Progression</b></summary>

| Placeholder | Returns |
| :-- | :-- |
| `%orbitskills_level%` | Current level |
| `%orbitskills_exp%` | Current EXP |
| `%orbitskills_exp_formatted%` | Compact EXP (e.g. `1.2M`) |
| `%orbitskills_exp_needed%` | EXP to next level |
| `%orbitskills_exp_percent%` | Progress to next level, % |
| `%orbitskills_total_exp%` | Lifetime EXP |
| `%orbitskills_progress_percent%` | Progress to max level, % |
| `%orbitskills_statspoint%` | Unspent SP |
| `%orbitskills_stats_perlevel%` | SP granted per level |

</details>

<details>
<summary><b>Identity</b></summary>

| Placeholder | Returns |
| :-- | :-- |
| `%orbitskills_race%` | Race display name |
| `%orbitskills_race_key%` | Race internal key |
| `%orbitskills_clan%` | Clan display name |
| `%orbitskills_clan_key%` | Clan internal key |

</details>

<details>
<summary><b>Stat points and ranks</b></summary>

| Placeholder | Aliases |
| :-- | :-- |
| `%orbitskills_archery%` | `archery_point`, `archer`, `projectile` |
| `%orbitskills_fighting%` | `fight`, `fighting_point`, `assassin`, `damage` |
| `%orbitskills_power%` | `attack` |
| `%orbitskills_defense%` | `defense_point`, `resistance` |
| `%orbitskills_health%` | `health_point` |
| `%orbitskills_alchemy%` | `alchemy_point`, `healing` |

Ranks: `%orbitskills_archery_rank%`, `fighting_rank`, `power_rank`, `defense_rank`, `health_rank`, `alchemy_rank`

</details>

<details>
<summary><b>Combat values</b></summary>

| Placeholder | Returns |
| :-- | :-- |
| `%orbitskills_melee_damage%` | Melee damage preview |
| `%orbitskills_projectile_damage%` | Projectile damage preview |
| `%orbitskills_melee_pierce%` | Melee defense penetration, % |
| `%orbitskills_projectile_pierce%` | Projectile defense penetration, % |
| `%orbitskills_max_health%` | Effective max HP |
| `%orbitskills_current_health%` | Effective current HP |
| `%orbitskills_defense_bonus%` | Damage blocked per 1000 |
| `%orbitskills_healing_bonus%` | Healing bonus, % |

Most numeric placeholders also have a `_formatted` / `_short` variant for compact display.

</details>

---

## Integrations

### MythicMobs

Custom mobs can be worth their own EXP instead of inheriting from their base entity, so a scripted boss built on a zombie doesn't pay out like a zombie. Map each mob by internal name under `exp-sources.mob-kill.mythic` in `source/Grinding.yml`. Unlisted mobs keep the vanilla amount. Set `exp-sources.mob-kill.mythic-exp-percent-per-level` to scale EXP with mob level.

### MMOItems

MMOItems weapons are scored by their author-assigned type rather than by material — a custom staff built on a stick counts as a Power weapon instead of reading as an empty hand. The mapping lives in `hooks.MMOItems.weapon-stats` and covers the common types out of the box.

> [!IMPORTANT]
> Both hooks are optional and read-only. OrbitSkills keeps its own stats and leveling either way — no stat values are read from either plugin, only *which* mob or item something is. Toggle with `hooks.MythicMobs.enabled` / `hooks.MMOItems.enabled`.

### packetevents / ProtocolLib

On Spigot forks that leak the raw heart count after armor swaps or hits, either plugin locks the heart bar at the packet level. Both are picked up automatically when installed. Paper servers don't need them.

---

## Languages

`EN` · `ID` · `MY` · `PH` · `CN` · `ES` · `JP` · `KR` · `TH` · `VN` · `RU` · `FR` · `DE`

Set `settings.language` in `config.yml` to load the matching `lang/Lang<CODE>.yml`.

---

## Building from Source

```bash
git clone https://github.com/Arifxyzzz/OrbitSkills.git
cd OrbitSkills
mvn clean package
```

The shaded jar lands at `target/OrbitSkills-1.2.2.jar`. Requires JDK 21.

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for the full history.

**Latest — v1.2.2 · Heart Display Compatibility**
- Fixed phantom hit effects when equipping armor, healing, eating, or changing stats
- Alchemy healing now scales off max health, so it stays meaningful at high HP
- Clan skills now fire correctly — 11 clan-only skills were previously unreachable
- Added MythicMobs and MMOItems support
- Added `health-display-*` and `actionbar-notice-ms` settings

---

<div align="center">

**Shadowyn Studio** · [shadowyn.id](https://www.shadowyn.id)

*OrbitSkills turns normal leveling into a complete RPG character system.*

</div>
