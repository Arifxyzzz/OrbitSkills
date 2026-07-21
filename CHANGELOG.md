# Changelog

## v1.2.0

**BBCode for forum post:**

```
[HEADING=2]v1.2.0[/HEADING]

[HEADING=3][COLOR=#4CAF50]Added[/COLOR][/HEADING]
[LIST]
[*][B]Priority system[/B] — Items now have a [ICODE]priority[/ICODE] field. Filler defaults to [ICODE]0[/ICODE], items to [ICODE]1[/ICODE]. Higher priority always wins. Customizable per item.
[*][B]Open Command[/B] — Menu items support [ICODE]open-command[/ICODE]. Clicking runs any player command. Example: [ICODE]open-command: "race"[/ICODE], [ICODE]open-command: "warp pvp"[/ICODE], [ICODE]open-command: "os class"[/ICODE].
[*][B]Show/Hide items[/B] — [ICODE]visible: true/false[/ICODE] to fully hide items. [ICODE]module: race|clan|class|stats[/ICODE] auto-hides items when the module is disabled ([ICODE]/os disable race[/ICODE]).
[*][B]Filler slot ranges[/B] — [ICODE]slots[/ICODE] in filler now supports ranges: [ICODE]["0-8", 17, "27-35"][/ICODE].
[/LIST]

[HEADING=3][COLOR=#F44336]Fixed[/COLOR][/HEADING]
[LIST]
[*][B]Enchantment glint not showing on MC 1.20.5+[/B] — Replaced [ICODE]enchanted: true[/ICODE] with [ICODE]enchantment_glint_override: true[/ICODE]. Automatic fallback for older versions.
[*][B]Menu items could not be moved to different slots[/B] — All positions are now config-driven: [ICODE]entry-slots[/ICODE], [ICODE]level-slots[/ICODE], [ICODE]minus-slots[/ICODE], [ICODE]plus-slots[/ICODE], [ICODE]prev-slot[/ICODE], [ICODE]next-slot[/ICODE], [ICODE]back-slot[/ICODE], [ICODE]current-slot[/ICODE], [ICODE]info-slot[/ICODE], [ICODE]progress-slot[/ICODE]. Editable via [ICODE]menu/*.yml[/ICODE].
[*][B]Deleted items appeared as stone[/B] — Removed or hidden items now fully disappear instead of showing as stone blocks.
[*][B]Filler format was inconsistent[/B] — Filler now uses the same expanded format as other items.
[/LIST]

---

[HEADING=2]v1.1.0[/HEADING]
[LIST]
[*]Leveling system with configurable EXP sources
[*]StatsPoint system (Archery, Fighting, Power, Defense, Health, Alchemy)
[*]Race reroll with weighted rarity and Mythic pity
[*]Clan/guild stat bonuses
[*]Stats Class reroll from F to SSS
[*]GUI menus (levels, stats, races, classes, guide, leaderboard)
[*]PlaceholderAPI support
[*]Additive YAML updates (merge missing keys)
[/LIST]
```

---

## v1.2.0

### Added
- **Priority system** — Items now have a `priority` field. Filler defaults to `0`, items to `1`. Higher priority always wins. Customizable per item.
- **Open Command** — Menu items support `open-command`. Clicking runs any player command. Example: `open-command: "race"`, `open-command: "warp pvp"`, `open-command: "os class"`.
- **Show/Hide items** — `visible: true/false` to fully hide items. `module: race|clan|class|stats` auto-hides items when the module is disabled (`/os disable race`).
- **Filler slot ranges** — `slots` in filler now supports ranges: `["0-8", 17, "27-35"]`.

### Fixed
- **Enchantment glint not showing on MC 1.20.5+** — Replaced `enchanted: true` with `enchantment_glint_override: true`. Automatic fallback for older versions.
- **Menu items could not be moved to different slots** — All positions are now config-driven: `entry-slots`, `level-slots`, `minus-slots`, `plus-slots`, `prev-slot`, `next-slot`, `back-slot`, `current-slot`, `info-slot`, `progress-slot`. Editable via `menu/*.yml`.
- **Deleted items appeared as stone** — Removed or hidden items now fully disappear instead of showing as stone blocks.
- **Filler format was inconsistent** — Filler now uses the same expanded format as other items.

---

## v1.1.0
- Leveling system with configurable EXP sources
- StatsPoint system (Archery, Fighting, Power, Defense, Health, Alchemy)
- Race reroll with weighted rarity and Mythic pity
- Clan/guild stat bonuses
- Stats Class reroll from F to SSS
- GUI menus (levels, stats, races, classes, guide, leaderboard)
- PlaceholderAPI support
- Additive YAML updates (merge missing keys)
