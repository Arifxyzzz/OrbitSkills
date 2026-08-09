# Changelog

## v1.2.2 — Heart Display Compatibility

### Fixed
- **Extra hearts on non-Paper software** — On forks such as UniverseSpigot, getting hit could briefly draw a long row of hearts instead of the fixed 10-heart row before snapping back. The heart bar lock is now re-applied after damage, healing, respawn, world change, and join, so the display stays at the configured size.
- **Extra hearts from armor and armor enchants** — Equipping, swapping, damaging, or breaking armor resends the player's health, which dropped the heart bar lock on the same forks. Armor changes, durability loss, and item breaks now refresh the lock. Health Boost and Absorption are covered too, since both change max health directly.
- **Armor changes still showing real health and a phantom hit** — Equipment is not synced to the client on the tick it changes, and the packet that eventually carries it also carries the raw max health the heart bar is drawn from, undoing a lock applied before it. The lock is now re-applied on every tick of the window after an armor change, so a correction follows that packet whichever tick it lands on.
- **Phantom hit effect when healing, gaining health, or eating** — The number of hearts drawn comes from the max-health attribute the client holds. With scaling on, the server substitutes the heart scale for the real maximum; when that substitution is missed, the client stretches the bar to match real HP and then reads the corrected packet as a large drop, playing the red tint and hurt sound. Alchemy healing showed it most clearly, since it raises health the most at once. The lock is now re-applied so the client is never left holding the raw maximum.
- **Refresh being skipped when the lock looked intact** — The refresh returned early when the server still reported the heart scale as set, even though the client had already lost it. The lock is now re-applied unconditionally, which is what re-sends the substituted attribute.
- **Heart bar showing values one step behind** — Damage and healing events fire before the server applies their result, so refreshing during the event sent the health the player had a moment ago. Refreshes now run on the following tick, when the real values are settled.
- **Clan skills never firing** — Effects were matched against the player's race only, so the eleven clan-only skills (Assassin, Shadow, Phoenix, Plague, Oni, Stormcaller, Raikage, Blazefury, Wardenborn, Astral, Necromancer) could never trigger, and the defensive Celestial, Aether, and Voidwalker effects were unreachable as well. Race and guild clan are now both checked.
- **Clan skills ignoring the clan module** — Clan effects were gated behind `modules.race`, so disabling races also disabled every clan skill while leaving race skills gated correctly. Each identity now respects its own module toggle.

- **Actionbar notices being wiped instantly** — The persistent HP/EXP bar rewrites every second, which was fast enough to erase a notice before it could be read. Alchemy heal, EXP gain, Shadow Thunder, and Necromancer messages now hold the slot for a moment before the bar returns.

### Added
- **`settings.actionbar-notice-ms`** — How long a one-off actionbar notice holds the slot before the persistent HP/EXP bar resumes. Defaults to `2500`. Set `0` to restore the old behaviour where notices are overwritten immediately.
- **`settings.health-display-scaling`** — Turns the fixed heart bar on or off. Set it to `false` when another plugin already controls the heart display.
- **`settings.health-display-hearts`** — Number of hearts the locked bar shows. Defaults to `20.0`, the vanilla row. Accepted range is 2 to 40.
- **`settings.health-display-refresh-ticks`** — How often the lock is re-applied, in ticks. Defaults to `40` (2 seconds). Set `0` to disable the repeating refresh, or raise it to refresh less often.
- **`settings.health-display-equipment-ticks`** — How many ticks after an armor change the lock keeps being re-applied. Defaults to `10`. Raise it if equipping armor still flickers on your server software.

### Notes
- Paper servers are unaffected in behaviour; the extra refresh is idempotent and only resends the existing heart scale.
- Armor tracking uses Paper's `PlayerArmorChangeEvent`. On server software without it, the listener is skipped safely and the repeating refresh keeps the heart bar correct.
- Missing keys are added automatically on update, and existing customized values are preserved.

## v1.2.1 — Progression & Balance Overhaul

### Added
- **Food-based Alchemy healing** — Alchemy now boosts healing from full-hunger regeneration in addition to instant-heal potions. Food healing efficiency is configurable through `stats.alchemy.food-heal-efficiency-percent`.
- **Emergency Alchemy regeneration** — Taking damage at low health can trigger tiered Regeneration with configurable health thresholds, duration, amplifier, and cooldown.
- **Progressive SP rewards** — SP gained per level now increases gradually. With the default curve, players gain about 50 SP in early levels, 100 SP around level 50, and 150 SP at level 100.
- **Per-stat damage targets** — Archery, Fighting, and Power now have independent `raw-damage-per-100-percent` values for clearer combat specialization.
- **Expanded Fighting tool support** — Fighting applies to swords, axes, pickaxes, shovels, hoes, maces, tridents, and shears. Power remains focused on unarmed and non-tool attacks.
- **Placeholder aliases** — Added common stat aliases including `fight`, `fighting_point`, `archery_point`, `health_point`, `defense_point`, and `alchemy_point`.

### Changed
- **Level cap redesigned** — The default maximum level changed from 1,000 to 100 so every level is more meaningful and easier to understand.
- **Total SP redesigned** — The default total SP changed from 100,000 to 10,000. Stat strength remains equivalent because all per-point values were rescaled accordingly.
- **Balanced SP milestone** — An even endgame allocation is about 1,667 SP per stat, equal to approximately 100% base stat power before class, race, and clan bonuses.
- **Long-term EXP curve** — Reaching level 100 now requires approximately 30 million total EXP. Early levels remain accessible while late levels become progressively more demanding.
- **Default combat balance** — At an even rank-F allocation, Fighting targets about 120 damage, Archery 105 damage, Power 130 damage, and Defense is capped at 40% reduction before external systems.
- **Health progression** — Health grows strongly before its main milestone, then continues at 20% overflow efficiency instead of stopping completely.
- **Soft-cap scaling** — Damage stats use 25% overflow efficiency, Health 20%, Defense 15%, and Alchemy 12%, allowing specialist builds without unrestricted linear scaling.
- **Config formatting** — Inline YAML maps were expanded into consistent readable sections.
- **Enchant compatibility preserved** — OrbitSkills adds its RPG damage on top of the existing Paper damage pipeline without limiting vanilla or custom enchant levels.

### Fixed
- **Stat bonuses returning zero** — Restored required `stats.*` defaults so damage, healing, Defense, and related placeholders calculate correctly.
- **Fighting placeholder returning zero** — `%orbitskills_fight%` now resolves correctly instead of falling through to `0`.
- **Health using outdated fallback values** — Health now reads the current configured curve rather than legacy defaults that produced unexpectedly low values.
- **Defense reaching 100% too early** — Removed the old effective scaling where roughly 1,000 points could already display maximum Defense.
- **Config values overwritten at startup** — Removed runtime balance code that replaced customized server values whenever the plugin started.
- **Unsafe legacy migration logic** — Removed duplicated and misplaced balance migrations from YAML merge helpers.
- **EXP migration not being saved** — Level-curve migrations now save correctly to `level.yml`.
- **Race/Clan migration side effects** — YAML merging is now strictly additive and no longer executes unrelated balance changes.

### Configuration and Migration
- `settings.balance-version` is now `51`.
- `settings.max-level` defaults to `100`.
- `settings.max-total-stat-points` defaults to `10000`.
- `settings.stat-point-curve-linear-weight` controls progressive SP distribution.
- `level.yml` uses EXP balance version `12`, power `1.5`, and multiplier `759.2195` by default.
- Missing YAML keys are added automatically.
- Existing customized values are preserved.
- Exact previous default values are migrated safely to the new balance.
- Existing player totals above the new SP cap are normalized by the player-data safety logic.

### Technical
- Removed obsolete `applyRuntimeDefaults`, `runtimeStat`, and legacy `migrateBalanceConfig` code.
- Simplified YAML merging to additive-only behavior.
- Cleaned duplicate migration helpers and unused imports.
- Verified Java 21/Paper 1.21.1 compilation with Maven.

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
