# OrbitSkills

Advanced RPG skills plugin for Paper servers.

## ASCI TEXT - BUAT LOG CONSOLE, KLO BISA PAKE IN WARNA
                                                           
   ____                       _____            __ __       
 _¦¯¯¦¦¦¦_      ¦_       ¦_  ¦¦¯¯¯¯¦_           ¦¦ ¦¦      
 ¦¦    ¦¦ _     ¦¦    ¯¯_¦¦_ ¯¦¦_  _¯ __     ¯¯ ¦¦ ¦¦      
 ¦¦    ¦¦ ¦¦¦¦_ ¦¦¦¦_ ¦¦ ¦¦    ¯¦¦__  ¦¦ _¦¯ ¦¦ ¦¦ ¦¦ _¦¦¯¦
 ¦¦    ¦¦ ¦¦    ¦¦ ¦¦ ¦¦ ¦¦  _   ¯¦¦_ ¦¦¦¦   ¦¦ ¦¦ ¦¦ ¯¦¦¦_
  ¯¦¦¦¦¯ _¦¯   _¦¦¦¦¯_¦¦_¦¦  ¯¦¦¦¦¦¦¯_¦¦ ¯¦__¦¦_¦¦_¦¦¦__¦¦¯
                                                           
 Advanced Skills Plugins | ShadowynStudio | v1.2.0  

## Features

- Player leveling with configurable EXP sources.
- StatsPoint system for Archery, Fighting, Power, Defense, Health, and Alchemy.
- Race reroll system with weighted rarity, Mythic pity, and passive effects.
- Clan/guild system that is stat-only and does not run passive skill logic.
- Stats Class reroll system from F to SSS.
- GUI menus for levels, stats, races, stats classes, guide, and leaderboard.
- PlaceholderAPI support with `%orbitskills_*%` placeholders.
- Additive YAML updates: missing keys are merged without deleting server edits.

## Commands

- `/os class` opens the stats class menu.
- `/os clan` opens the clan menu.
- `/os leaderboard` opens the leaderboard.
- `/os race` opens the race menu.
- `/os guide` opens the player guide.
- `/skills` opens the stats menu.
- `/level` opens level rewards.
- `/race` opens the race menu.
- `/clan` shows your current clan/guild.
- `/profile [player]` shows a chat profile.
- `/orbitskills` or `/os` opens command help.

## Admin Examples

- `/os give point <player|*> <amount>`
- `/os give level <player|*> <amount>`
- `/os give exp <player|*> <amount>`
- `/os give fragment race <player|*> <amount>`
- `/os give fragment class <player|*> <amount>`
- `/os give fragment clan <player|*> <amount>`
- `/os set level <player> <value>`
- `/os set race <player> <race>`
- `/os set clan <player> <clan>`
- `/os set class <player> <stat> <F-SSS>`
- `/os disable <stats|class|race|clan|all>`
- `/os enable <stats|class|race|clan|all>`
- `/os scanall fix`
- `/os reload`

## Permissions

- `orbitskills.admin` for admin commands.
- `orbitskills.use` for general use.
- `orbitskills.multiplier.2` and higher for reward multiplier tiers.

## Level Rewards

`level.yml` is command-first for public compatibility. Use `rewards.commands` for money, crates, keys, fragments, or third-party integrations. Legacy direct rewards (`money`, `items`, `clan-fragments`, `rank-fragments`) still work.

Example:

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

## File Layout

- `config.yml` keeps global settings and balance knobs.
- `lang/LangEN.yml` is the default language. Set `settings.language` to load another `lang/Lang<CODE>.yml`.
- Available languages: EN, ID, MY, PH, CN, ES, JP, KR, TH, VN, RU, FR, DE.
- `menu/*.yml` stores menu titles, slots, and item visuals.
- `source/Grinding.yml` stores EXP and fragment sources.
- `source/Races.yml` stores race reroll entries and passive race effects.
- `source/Clans.yml` stores stat-only clans/guilds.
