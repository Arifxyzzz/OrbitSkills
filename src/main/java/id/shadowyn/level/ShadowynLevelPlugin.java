package id.shadowyn.level;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

public final class ShadowynLevelPlugin extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 32371;
    private static final String[] MENU_RESOURCES = {
            "menu/Main.yml",
            "menu/Level.yml",
            "menu/Leaderboard.yml",
            "menu/Clan.yml",
            "menu/Races.yml",
            "menu/Guide.yml",
            "menu/Stats.yml",
            "menu/Points.yml"
    };
    private PlayerDataStore data;
    private LevelService levels;
    private ClanService clans;
    private FragmentService fragments;
    private MenuService menus;
    private String prefix;
    private FileConfiguration levelConfig;
    private FileConfiguration langConfig;
    private FileConfiguration pageConfig;
    private FileConfiguration sourceConfig;
    private FileConfiguration fragmentConfig;
    private FileConfiguration raceConfig;
    private FileConfiguration clanConfig;

    @Override
    public void onEnable() {
        getLogger().info("");
        getLogger().info("   \u2584\u2584\u2584\u2584                       \u2584\u2584\u2584\u2584\u2584            \u2584\u2584 \u2584\u2584       ");
        getLogger().info(" \u2584\u2588\u2580\u2580\u2588\u2588\u2588\u2588\u2584      \u2588\u2584       \u2588\u2584  \u2588\u2588\u2580\u2580\u2580\u2580\u2588\u2584           \u2588\u2588 \u2588\u2588      ");
        getLogger().info(" \u2588\u2588    \u2588\u2588 \u2584     \u2588\u2588    \u2580\u2580\u2584\u2588\u2588\u2584 \u2580\u2588\u2588\u2584  \u2584\u2580 \u2584\u2584     \u2580\u2580 \u2588\u2588 \u2588\u2588      ");
        getLogger().info(" \u2588\u2588    \u2588\u2588 \u2588\u2588\u2588\u2588\u2584 \u2588\u2588\u2588\u2588\u2584 \u2588\u2588 \u2588\u2588    \u2580\u2588\u2588\u2584\u2584  \u2588\u2588 \u2584\u2588\u2580 \u2588\u2588 \u2588\u2588 \u2588\u2588 \u2584\u2588\u2588\u2580\u2588");
        getLogger().info(" \u2588\u2588    \u2588\u2588 \u2588\u2588    \u2588\u2588 \u2588\u2588 \u2588\u2588 \u2588\u2588  \u2584   \u2580\u2588\u2588\u2584 \u2588\u2588\u2588\u2588   \u2588\u2588 \u2588\u2588 \u2588\u2588 \u2580\u2588\u2588\u2588\u2584");
        getLogger().info("  \u2580\u2588\u2588\u2588\u2588\u2580 \u2584\u2588\u2580   \u2584\u2588\u2588\u2588\u2588\u2580\u2584\u2588\u2588\u2584\u2588\u2588  \u2580\u2588\u2588\u2588\u2588\u2588\u2588\u2580\u2584\u2588\u2588 \u2580\u2588\u2584\u2584\u2588\u2588\u2584\u2588\u2588\u2584\u2588\u2588\u2588\u2584\u2584\u2588\u2588\u2580");
        getLogger().info(" Advanced Skills Plugins | ShadowynStudio | v" + getPluginMeta().getVersion());
        getLogger().info("");
        ensureResource("config.yml");
        ensureResource("level.yml");
        ensureResource("lang/LangEN.yml");
        ensureResource("lang/LangID.yml");
        ensureResource("lang/LangMY.yml");
        ensureResource("lang/LangPH.yml");
        ensureResource("lang/LangCN.yml");
        ensureResource("lang/LangES.yml");
        ensureResource("lang/LangJP.yml");
        ensureResource("lang/LangKR.yml");
        ensureResource("lang/LangTH.yml");
        ensureResource("lang/LangVN.yml");
        ensureResource("lang/LangRU.yml");
        ensureResource("lang/LangFR.yml");
        ensureResource("lang/LangDE.yml");
        for (String menuResource : MENU_RESOURCES) ensureResource(menuResource);
        ensureResource("source/Grinding.yml");
        ensureResource("source/Fragments.yml");
        ensureResource("source/Races.yml");
        ensureResource("source/Clans.yml");
        mergeMissingYaml("config.yml");
        mergeMissingYaml("level.yml");
        mergeMissingYaml("lang/LangEN.yml");
        mergeMissingYaml("lang/LangES.yml");
        mergeMissingYaml("lang/LangJP.yml");
        mergeMissingYaml("lang/LangKR.yml");
        mergeMissingYaml("lang/LangTH.yml");
        mergeMissingYaml("lang/LangVN.yml");
        mergeMissingYaml("lang/LangRU.yml");
        mergeMissingYaml("lang/LangFR.yml");
        mergeMissingYaml("lang/LangDE.yml");
        mergeMissingYaml("source/Grinding.yml");
        mergeMissingYaml("source/Fragments.yml");
        mergeMissingYaml("source/Races.yml");
        mergeMissingYaml("source/Clans.yml");
        reloadConfig();
        loadConfigDefaults();
        loadLevelConfig();
        loadLangConfig();
        loadPageConfig();
        loadSourceConfigs();
        data = new PlayerDataStore(this);
        levels = new LevelService(this);
        clans = new ClanService(this);
        fragments = new FragmentService(this);
        menus = new MenuService(this);
        data.load();

        ShadowynCommand command = new ShadowynCommand(this);
        getCommand("orbitskills").setExecutor(command);
        getCommand("orbitskills").setTabCompleter(command);
        getCommand("resetdata").setExecutor(command);
        getCommand("resetdata").setTabCompleter(command);
        getCommand("level").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player player) menus.openLevel(player, args.length > 0 ? parseInt(args[0], 1) : 1);
            return true;
        });
        getCommand("stats").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player player) {
                if (args.length > 0 && args[0].equalsIgnoreCase("profile")) menus.sendChatProfile(player);
                else menus.openStats(player);
            }
            return true;
        });
        getCommand("skills").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player player) {
                if (args.length >= 2 && args[0].equalsIgnoreCase("action")) {
                    boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
                    if (!enabled && !args[1].equalsIgnoreCase("off") && !args[1].equalsIgnoreCase("false")) {
                        player.sendMessage(Text.s("&cUsage: /skills action <on|off>"));
                        return true;
                    }
                    data.get(player).skillActionbar(enabled);
                    data.save();
                    player.sendMessage(Text.s(enabled ? "&aSkills actionbar enabled." : "&cSkills actionbar disabled."));
                } else {
                    menus.openStats(player);
                }
            }
            return true;
        });
        getCommand("clan").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player player) {
                if (args.length > 0 && args[0].equalsIgnoreCase("reroll")) {
                    clans.rerollGuild(player);
                    return true;
                }
                menus.openGuild(player);
            }
            return true;
        });
        getCommand("race").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player player) menus.openClan(player, 1);
            return true;
        });
        getCommand("profile").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player viewer)) return true;
            PlayerProfile target = args.length == 0
                    ? data.get(viewer)
                    : data.findByName(args[0]);
            if (target == null) {
                viewer.sendMessage(Text.s("&cPlayer profile not found."));
                return true;
            }
            menus.sendChatProfile(viewer, target);
            return true;
        });
        getCommand("profile").setTabCompleter((sender, cmd, alias, args) -> {
            if (args.length != 1) return java.util.List.of();
            String lower = args[0].toLowerCase(java.util.Locale.ROOT);
            return data.allProfiles().stream()
                    .map(p -> p.name())
                    .filter(name -> args[0].isBlank() || name.toLowerCase(java.util.Locale.ROOT).startsWith(lower))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        });
        Bukkit.getPluginManager().registerEvents(new GrindListener(this), this);
        Bukkit.getPluginManager().registerEvents(menus, this);
        long saveTicks = 20L * getConfig().getInt("settings.save-interval-seconds", 180);
        Bukkit.getScheduler().runTaskTimer(this, () -> data.save(), saveTicks, saveTicks);
        Bukkit.getScheduler().runTaskTimer(this, this::sendPersistentActionBars, 20L, 20L);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ShadowynPlaceholders(this, "orbitskills", "Shadowyn Studio").register();
        }
        setupMetrics();
        Bukkit.getOnlinePlayers().forEach(this::applyStats);
    }

    @Override
    public void onDisable() {
        if (data != null) data.save();
    }

    public PlayerDataStore data() { return data; }
    public LevelService levels() { return levels; }
    public ClanService clans() { return clans; }
    public FragmentService fragments() { return fragments; }
    public MenuService menus() { return menus; }
    public FileConfiguration levelConfig() { return levelConfig; }
    public FileConfiguration langConfig() { return langConfig; }
    public FileConfiguration pageConfig() { return pageConfig; }
    public FileConfiguration sourceConfig() { return sourceConfig; }
    public FileConfiguration fragmentConfig() { return fragmentConfig; }
    public FileConfiguration raceConfig() { return raceConfig; }
    public FileConfiguration clanConfig() { return clanConfig; }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("language", () -> getConfig().getString("settings.language", "EN").toUpperCase(Locale.ROOT)));
        metrics.addCustomChart(new SimplePie("balance_profile", () -> getConfig().getString("balance.profile", "balanced").toLowerCase(Locale.ROOT)));
        metrics.addCustomChart(new SimplePie("max_level", () -> String.valueOf(maxLevel())));
        metrics.addCustomChart(new SimplePie("stats_module", () -> statsEnabled() ? "enabled" : "disabled"));
        metrics.addCustomChart(new SimplePie("class_module", () -> classesEnabled() ? "enabled" : "disabled"));
        metrics.addCustomChart(new SimplePie("race_module", () -> racesEnabled() ? "enabled" : "disabled"));
        metrics.addCustomChart(new SimplePie("clan_module", () -> clansEnabled() ? "enabled" : "disabled"));
        metrics.addCustomChart(new SimplePie("placeholderapi", () -> Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? "installed" : "missing"));
        metrics.addCustomChart(new SimplePie("vault", () -> Bukkit.getPluginManager().isPluginEnabled("Vault") ? "installed" : "missing"));
    }

    private void ensureResource(String name) {
        File file = new File(getDataFolder(), name);
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        if (!file.exists()) saveResource(name, false);
    }

    private void mergeMissingYaml(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) return;
        try (InputStreamReader reader = new InputStreamReader(getResource(name), StandardCharsets.UTF_8)) {
            YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            boolean changed = mergeSection(current, defaults, "");
            if (changed) current.save(file);
        } catch (Exception e) {
            getLogger().warning("Failed to update missing keys in " + name + ": " + e.getMessage());
        }
    }

    private boolean mergeSection(YamlConfiguration current, YamlConfiguration defaults, String path) {
        boolean changed = false;
        org.bukkit.configuration.ConfigurationSection section = path.isEmpty() ? defaults : defaults.getConfigurationSection(path);
        if (section == null) return false;
        for (String key : section.getKeys(false)) {
            String child = path.isEmpty() ? key : path + "." + key;
            if (defaults.isConfigurationSection(child)) {
                changed |= mergeSection(current, defaults, child);
            } else if (!current.isSet(child)) {
                current.set(child, defaults.get(child));
                changed = true;
            }
        }
        if (getConfig().getInt("settings.balance-version", 1) < 23) {
            getConfig().set("clan-effects.raikage-strike-bonus-damage", 4.0);
            getConfig().set("clan-effects.blazefury-nova-damage", 4.5);
            getConfig().set("clan-effects.wardenborn-roar-bonus-damage", 6.0);
            getConfig().set("clan-effects.void-step-reduction-percent", 45.0);
            getConfig().set("clan-effects.necromancer-heal-percent", 10.0);

            getConfig().set("clans.RAIKAGE.damage-percent", 14);
            getConfig().set("clans.RAIKAGE.health-percent", 2);
            getConfig().set("clans.RAIKAGE.defense-percent", 4);
            getConfig().set("clans.RAIKAGE.assassin-percent", 14);
            getConfig().set("clans.RAIKAGE.description", java.util.List.of(
                    "Ras kilat mythic untuk tempo melee cepat.",
                    "Damage tinggi tanpa mengorbankan durability."));
            getConfig().set("clans.RAIKAGE.passive-skills", java.util.List.of(
                    "&eLightning Step &7- melee hit memberi speed dan bonus damage petir.",
                    "&a+14% &7damage dan &a+14% &7melee damage.",
                    "&a+2% &7health dan &a+4% &7defense."));

            getConfig().set("clans.BLAZEFURY.damage-percent", 18);
            getConfig().set("clans.BLAZEFURY.health-percent", -4);
            getConfig().set("clans.BLAZEFURY.defense-percent", -2);
            getConfig().set("clans.BLAZEFURY.description", java.util.List.of(
                    "Ras panas mythic dengan burst area.",
                    "Sangat agresif, tetap punya drawback ringan."));
            getConfig().set("clans.BLAZEFURY.passive-skills", java.util.List.of(
                    "&6Fire Nova &7- melee hit bisa menyemburkan api ke sekitar dengan cooldown.",
                    "&a+18% &7damage.",
                    "&c-4% &7health dan &c-2% &7defense."));

            getConfig().set("clans.WARDENBORN.damage-percent", 14);
            getConfig().set("clans.WARDENBORN.health-percent", 20);
            getConfig().set("clans.WARDENBORN.defense-percent", 14);
            getConfig().set("clans.WARDENBORN.description", java.util.List.of(
                    "Ras kuno mythic dengan daya tahan besar.",
                    "Tanky, stabil, dan punya burst terkontrol."));
            getConfig().set("clans.WARDENBORN.passive-skills", java.util.List.of(
                    "&3Sonic Roar &7- melee hit memberi sonic boom, darkness, dan bonus damage.",
                    "&a+20% &7health, &a+14% &7defense, &a+14% &7damage."));

            getConfig().set("clans.VOIDWALKER.damage-percent", 12);
            getConfig().set("clans.VOIDWALKER.health-percent", 2);
            getConfig().set("clans.VOIDWALKER.defense-percent", 12);
            getConfig().set("clans.VOIDWALKER.night-damage-percent", 18);
            getConfig().set("clans.VOIDWALKER.description", java.util.List.of(
                    "Ras void mythic yang sulit ditangkap.",
                    "Saat kena hit bisa blink dan mengurangi damage."));
            getConfig().set("clans.VOIDWALKER.passive-skills", java.util.List.of(
                    "&5Void Step &7- saat kena hit bisa teleport pendek, reduce damage, dan speed.",
                    "&a+12% &7damage, &a+12% &7defense, &5+18% &7damage malam.",
                    "&a+2% &7health."));

            getConfig().set("clans.NECROMANCER.damage-percent", 10);
            getConfig().set("clans.NECROMANCER.health-percent", 2);
            getConfig().set("clans.NECROMANCER.defense-percent", 6);
            getConfig().set("clans.NECROMANCER.alchemy-percent", 20);
            getConfig().set("clans.NECROMANCER.night-damage-percent", 12);
            getConfig().set("clans.NECROMANCER.description", java.util.List.of(
                    "Ras kutukan mythic dengan wither dan sustain.",
                    "Kuat untuk duel lama dan alchemy."));
            getConfig().set("clans.NECROMANCER.passive-skills", java.util.List.of(
                    "&2Wither Hex &7- hit memberi wither dan heal dari damage.",
                    "&a+20% &7potion heal, &a+10% &7damage, &a+6% &7defense.",
                    "&a+2% &7health dan &5+12% &7damage malam."));

            getConfig().set("clans.ASTRAL.damage-percent", 12);
            getConfig().set("clans.ASTRAL.health-percent", 8);
            getConfig().set("clans.ASTRAL.defense-percent", 8);
            getConfig().set("clans.ASTRAL.archer-percent", 12);
            getConfig().set("clans.ASTRAL.alchemy-percent", 10);
            getConfig().set("clans.ASTRAL.mining-bonus-drop-chance", 5);
            getConfig().set("clans.ASTRAL.night-damage-percent", 8);
            getConfig().set("clans.ASTRAL.description", java.util.List.of(
                    "Ras bintang mythic dengan mark dan partikel astral.",
                    "Build fleksibel untuk damage, sustain, dan farming."));
            getConfig().set("clans.ASTRAL.passive-skills", java.util.List.of(
                    "&dAstral Mark &7- hit memberi glowing, partikel, dan bonus damage.",
                    "&a+12% &7damage, &a+12% &7projectile, &a+10% &7potion heal.",
                    "&dStar Fortune &7- mining kadang memberi drop bonus.",
                    "&a+8% &7health, &a+8% &7defense, dan &5+8% &7damage malam."));

            getConfig().set("settings.balance-version", 23);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 24) {
            getConfig().set("stats.damage.pvp-max-health-percent-per-hit", 22.0);
            getConfig().set("stats.damage.pvp-min-health-percent-per-hit", 3.0);
            getConfig().set("stats.archery.defense-penetration-per-100-percent", 8.0);
            getConfig().set("stats.fighting.defense-penetration-per-100-percent", 8.0);
            getConfig().set("stats.power.defense-penetration-per-100-percent", 10.0);
            getConfig().set("stats.power.defense-penetration-cap-percent", 70.0);
            getConfig().set("settings.balance-version", 24);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 25) {
            getConfig().set("stats.damage.pvp-level-gap-cap-bonus-percent-per-level", 0.12);
            getConfig().set("stats.damage.pvp-level-gap-max-health-percent-per-hit", 120.0);
            getConfig().set("settings.balance-version", 25);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 26) {
            getConfig().set("stats.damage.pvp-max-health-percent-per-hit", 0.0);
            getConfig().set("stats.damage.pvp-level-gap-cap-bonus-percent-per-level", 0.0);
            getConfig().set("stats.damage.pvp-level-gap-max-health-percent-per-hit", 0.0);
            getConfig().set("actionbar-text", "&#ff1453â¤ {health_formatted}&7/&f{max_health_formatted} &8            &#f3e08dâœ¦ {exp_formatted}&7/&f{needed_exp_formatted}");
            getConfig().set("settings.balance-version", 26);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 27) {
            getConfig().set("stats.damage.raw-damage-per-100-percent", 65.0);
            getConfig().set("stats.defense.effective-cap-percent", 75.0);
            getConfig().set("stats.health.health-per-point", 0.11);
            getConfig().set("stats.health.flat-effective-cap", 900.0);
            getConfig().set("settings.balance-version", 27);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 28) {
            getConfig().set("exp-sources.combat.damage-exp-every", 25.0);
            getConfig().set("exp-sources.combat.max-exp-per-hit", 6);
            getConfig().set("exp-sources.alchemy.brew-potion", 12);
            getConfig().set("exp-sources.fishing.fish", 10);
            getConfig().set("exp-sources.fishing.treasure", 22);
            getConfig().set("exp-sources.fishing.junk", 3);
            getConfig().set("exp-sources.enchanting.base", 3);
            getConfig().set("exp-sources.enchanting.exp-per-level", 0.35);
            getConfig().set("exp-sources.enchanting.max-exp", 20);
            getConfig().set("exp-sources.smelting.exp-per-item", 1);
            getConfig().set("exp-sources.smelting.max-exp-per-extract", 32);
            getConfig().set("exp-sources.breeding.animal", 8);
            getConfig().set("settings.balance-version", 28);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 29) {
            getConfig().set("stats.alchemy.low-health-regen-tier-1-percent", 40.0);
            getConfig().set("stats.alchemy.low-health-regen-tier-2-percent", 65.0);
            getConfig().set("stats.alchemy.low-health-regen-tier-3-percent", 85.0);
            getConfig().set("stats.alchemy.low-health-regen-cooldown-ms", 16000);
            getConfig().set("stats.alchemy.low-health-regen-base-ticks", 35);
            getConfig().set("stats.alchemy.low-health-regen-ticks-per-bonus-percent", 0.10);
            getConfig().set("stats.alchemy.low-health-regen-max-extra-ticks", 35.0);
            getConfig().set("stats.alchemy.low-health-regen-bonus-per-amplifier", 450.0);
            getConfig().set("stats.alchemy.low-health-regen-max-amplifier", 2);
            getConfig().set("settings.balance-version", 29);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 30) {
            getConfig().set("stats.alchemy.max-health-heal-per-100-percent", 0.45);
            getConfig().set("stats.alchemy.low-health-regen-cooldown-ms", 22000);
            getConfig().set("stats.alchemy.low-health-regen-base-ticks", 25);
            getConfig().set("stats.alchemy.low-health-regen-ticks-per-bonus-percent", 0.05);
            getConfig().set("stats.alchemy.low-health-regen-max-extra-ticks", 20.0);
            getConfig().set("stats.alchemy.low-health-regen-bonus-per-amplifier", 9999.0);
            getConfig().set("stats.alchemy.low-health-regen-max-amplifier", 1);
            getConfig().set("clan-effects.alchemy-min-instant-bonus-health", 0.25);
            getConfig().set("settings.balance-version", 30);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 31) {
            getConfig().set("stats.archery.defense-penetration-per-100-percent", 6.0);
            getConfig().set("stats.fighting.defense-penetration-per-100-percent", 6.0);
            getConfig().set("stats.power.defense-penetration-per-100-percent", 8.0);
            getConfig().set("stats.power.defense-penetration-cap-percent", 60.0);
            getConfig().set("stats.defense.health-per-point", 0.01);
            getConfig().set("stats.defense.health-flat-effective-cap", 500.0);
            getConfig().set("stats.defense.health-flat-overflow-efficiency-percent", 10.0);
            getConfig().set("settings.balance-version", 31);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 32) {
            applyGrindingExpansion32();
            getConfig().set("settings.balance-version", 32);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 33) {
            applyGrindingExpansion33();
            getConfig().set("settings.balance-version", 33);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 34) {
            getConfig().set("exp-sources.mob-kill.bosses", 75);
            getConfig().set("exp-sources.mob-kill.types.elder_guardian", 30);
            getConfig().set("exp-sources.mob-kill.types.warden", 120);
            getConfig().set("exp-sources.mob-kill.types.wither", 180);
            getConfig().set("exp-sources.mob-kill.types.ender_dragon", 250);
            getConfig().set("settings.balance-version", 34);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 35) {
            getConfig().set("exp-sources.mob-kill.bosses", 35);
            getConfig().set("exp-sources.mob-kill.types.elder_guardian", 18);
            getConfig().set("exp-sources.mob-kill.types.warden", 45);
            getConfig().set("exp-sources.mob-kill.types.wither", 70);
            getConfig().set("exp-sources.mob-kill.types.ender_dragon", 100);
            getConfig().set("settings.balance-version", 35);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 36) {
            applyMobBalance36();
            getConfig().set("settings.balance-version", 36);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 37) {
            getConfig().set("stats.fighting.mace-damage-per-100-percent", 2.0);
            getConfig().set("stats.fighting.mace-damage-cap-percent", 25.0);
            getConfig().set("settings.balance-version", 37);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 38) {
            getConfig().set("stats.fighting.mace-flat-damage-enabled", false);
            getConfig().set("settings.balance-version", 38);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 39) {
            getConfig().set("stats.damage.stat-percent-multiplier-enabled", true);
            getConfig().set("stats.fighting.mace-flat-damage-enabled", null);
            getConfig().set("stats.fighting.mace-damage-per-100-percent", null);
            getConfig().set("stats.fighting.mace-damage-cap-percent", null);
            getConfig().set("settings.balance-version", 39);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 40) {
            getConfig().set("stats.damage.stat-percent-multiplier-enabled", false);
            getConfig().set("settings.balance-version", 40);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 41) {
            getConfig().set("stats.damage.raw-damage-per-100-percent", 24.0);
            getConfig().set("settings.balance-version", 41);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 42) {
            getConfig().set("stats.archery.overflow-efficiency-percent", 25.0);
            getConfig().set("stats.fighting.overflow-efficiency-percent", 25.0);
            getConfig().set("stats.power.overflow-efficiency-percent", 25.0);
            getConfig().set("settings.balance-version", 42);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 43) {
            getConfig().set("stats.fighting.mace-breach-defense-reduction-percent-per-level", 15.0);
            getConfig().set("settings.balance-version", 43);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 44) {
            getConfig().set("stats.damage.pvp-max-health-percent-per-hit", 0.0);
            getConfig().set("stats.damage.pvp-level-gap-cap-bonus-percent-per-level", 0.0);
            getConfig().set("stats.damage.pvp-level-gap-max-health-percent-per-hit", 0.0);
            getConfig().set("settings.balance-version", 44);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 45) {
            getConfig().set("stats.archery.defense-penetration-per-100-percent", 3.5);
            getConfig().set("stats.fighting.defense-penetration-per-100-percent", 3.5);
            getConfig().set("stats.power.defense-penetration-per-100-percent", 4.5);
            getConfig().set("stats.power.defense-penetration-cap-percent", 40.0);
            getConfig().set("settings.balance-version", 45);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 46) {
            applyMobExpBuff46();
            getConfig().set("settings.balance-version", 46);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 47) {
            applyActivityExpBuff47();
            getConfig().set("settings.balance-version", 47);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 48) {
            getConfig().set("actionbar-text", "&#ff1453â¤ {health_formatted}&7/&f{max_health_formatted} &8            &#f3e08dâœ¦ {exp_formatted}&7/&f{needed_exp_formatted}");
            getConfig().set("settings.balance-version", 48);
            changed = true;
        }
        return changed;
    }

    public void loadConfigDefaults() {
        boolean changed = false;
        changed |= migrateBalanceConfig();
        if (!getConfig().isSet("actionbar")) {
            getConfig().set("actionbar", true);
            changed = true;
        }
        if (!getConfig().isSet("actionbar-text")) {
            getConfig().set("actionbar-text", "&cHP &f{health}&7/&f{max_health} &8        &aEXP &f{exp}&7/&f{needed_exp}");
            changed = true;
        }
        if (changed) saveConfig();
        applyRuntimeDefaults();
    }

    private void applyRuntimeDefaults() {
        String profile = getConfig().getString("balance.profile", "balanced").toLowerCase(Locale.ROOT);
        double scale = switch (profile) {
            case "casual" -> 1.15;
            case "hard" -> 0.85;
            default -> 1.0;
        };
        double rawDamage = 24.0 * scale;
        double previewHp = 1000.0;
        double pvpFloor = 3.0;
        double softCap = 100.0;
        double overflow = profile.equals("hard") ? 20.0 : 25.0;
        getConfig().set("stats.damage.stat-percent-multiplier-enabled", false);
        getConfig().set("stats.damage.raw-damage-per-100-percent", rawDamage);
        getConfig().set("stats.damage.preview-target-health", previewHp);
        getConfig().set("stats.damage.pvp-min-health-percent-per-hit", pvpFloor);
        getConfig().set("stats.damage.effective-cap-percent", 380.0);
        getConfig().set("stats.damage.overflow-efficiency-percent", overflow);

        runtimeStat("archery", 0.24 * scale, softCap, overflow, 10000.0);
        runtimeStat("fighting", 0.24 * scale, softCap, overflow, 10000.0);
        runtimeStat("power", 0.28 * scale, softCap, overflow, 10000.0);
        runtimeStat("defense", 0.22 * scale, softCap, 15.0, 10000.0);
        runtimeStat("health", 0.045 * scale, softCap, 20.0, 10000.0);
        runtimeStat("alchemy", 0.18 * scale, softCap, 12.0, 10000.0);

        getConfig().set("stats.archery.defense-penetration-per-100-percent", 3.5);
        getConfig().set("stats.archery.max-health-damage-per-100-percent", 0.0);
        getConfig().set("stats.fighting.defense-penetration-per-100-percent", 3.5);
        getConfig().set("stats.fighting.max-health-damage-per-100-percent", 0.0);
        getConfig().set("stats.fighting.mace-breach-defense-reduction-percent-per-level", 15.0);
        getConfig().set("stats.power.defense-penetration-per-100-percent", 4.5);
        getConfig().set("stats.power.defense-penetration-cap-percent", 40.0);
        getConfig().set("stats.power.max-health-damage-per-100-percent", 0.0);
        getConfig().set("stats.defense.effective-cap-percent", 75.0);
        getConfig().set("stats.defense.absolute-cap-percent", 92.0);
        getConfig().set("stats.defense.health-per-point", 0.01);
        getConfig().set("stats.defense.health-flat-effective-cap", 500.0);
        getConfig().set("stats.defense.health-flat-overflow-efficiency-percent", 10.0);
        getConfig().set("stats.health.health-per-point", 0.11);
        getConfig().set("stats.health.effective-cap-percent", 1000.0);
        getConfig().set("stats.health.flat-effective-cap", 900.0);
        getConfig().set("stats.health.flat-overflow-efficiency-percent", 20.0);
        getConfig().set("stats.alchemy.max-health-heal-per-100-percent", 0.45);
        getConfig().set("stats.alchemy.low-health-regen-tier-1-percent", 40.0);
        getConfig().set("stats.alchemy.low-health-regen-tier-2-percent", 65.0);
        getConfig().set("stats.alchemy.low-health-regen-tier-3-percent", 85.0);
        getConfig().set("stats.alchemy.low-health-regen-cooldown-ms", 22000);
        getConfig().set("stats.alchemy.low-health-regen-base-ticks", 25);
        getConfig().set("stats.alchemy.low-health-regen-ticks-per-bonus-percent", 0.05);
        getConfig().set("stats.alchemy.low-health-regen-max-extra-ticks", 20.0);
        getConfig().set("stats.alchemy.low-health-regen-bonus-per-amplifier", 9999.0);
        getConfig().set("stats.alchemy.low-health-regen-max-amplifier", 1);
    }

    private void runtimeStat(String key, double gain, double softCap, double overflow, double max) {
        getConfig().set("stats." + key + ".bonus-per-point-percent", gain);
        getConfig().set("stats." + key + ".soft-cap-percent", softCap);
        getConfig().set("stats." + key + ".overflow-efficiency-percent", overflow);
        getConfig().set("stats." + key + ".max-bonus-percent", max);
    }

    private boolean migrateBalanceConfig() {
        boolean changed = false;
        changed |= updateOldDouble("stats.damage.effective-cap-percent", 55.0, 400.0);
        changed |= setMissingDouble("stats.damage.overflow-efficiency-percent", 35.0);
        changed |= updateOldDouble("stats.archery.bonus-per-point-percent", 0.28, 0.38);
        changed |= updateOldDouble("stats.fighting.bonus-per-point-percent", 0.28, 0.38);
        changed |= updateOldDouble("stats.power.bonus-per-point-percent", 0.35, 0.45);
        changed |= updateOldDouble("stats.defense.bonus-per-point-percent", 0.18, 0.16);
        changed |= updateOldDouble("stats.defense.effective-cap-percent", 35.0, 65.0);
        changed |= updateOldDouble("stats.defense.absolute-cap-percent", 95.0, 90.0);
        changed |= updateOldDouble("stats.health.bonus-per-point-percent", 0.22, 0.02);
        changed |= updateOldDouble("stats.health.health-per-point", 0.25, 0.02);
        changed |= updateOldDouble("stats.health.effective-cap-percent", 35.0, 120.0);
        changed |= setMissingDouble("stats.health.flat-effective-cap", 120.0);
        changed |= setMissingDouble("stats.health.flat-overflow-efficiency-percent", 10.0);
        changed |= updateOldDouble("stats.alchemy.bonus-per-point-percent", 0.20, 0.35);

        changed |= updateOldDouble("stats.damage.effective-cap-percent", 400.0, 700.0);
        changed |= updateOldDouble("stats.damage.overflow-efficiency-percent", 35.0, 55.0);
        changed |= updateOldDouble("stats.archery.bonus-per-point-percent", 0.38, 0.50);
        changed |= updateOldDouble("stats.fighting.bonus-per-point-percent", 0.38, 0.50);
        changed |= updateOldDouble("stats.power.bonus-per-point-percent", 0.45, 0.65);
        changed |= updateOldDouble("stats.alchemy.bonus-per-point-percent", 0.35, 0.75);
        changed |= setMissingDouble("stats.alchemy.max-health-heal-per-100-percent", 3.0);
        changed |= setMissingInt("exp-sources.combat.max-exp-per-hit", 30);
        changed |= setMissingDouble("stats.archery.max-health-damage-per-100-percent", 1.4);
        changed |= setMissingDouble("stats.archery.defense-penetration-per-100-percent", 0.0);
        changed |= setMissingDouble("stats.fighting.max-health-damage-per-100-percent", 1.4);
        changed |= setMissingDouble("stats.fighting.defense-penetration-per-100-percent", 0.0);
        changed |= setMissingDouble("stats.power.max-health-damage-per-100-percent", 1.8);
        changed |= setMissingDouble("stats.power.defense-penetration-per-100-percent", 0.0);
        changed |= setMissingDouble("stats.power.defense-penetration-cap-percent", 85.0);
        changed |= setMissingDouble("stats.damage.preview-base-damage", 10.0);
        changed |= setMissingDouble("stats.damage.preview-target-health", 1000.0);
        changed |= setMissingDouble("stats.damage.raw-damage-per-100-percent", 20.0);
        if (!getConfig().isSet("stats.damage.stat-percent-multiplier-enabled")) {
            getConfig().set("stats.damage.stat-percent-multiplier-enabled", false);
            changed = true;
        }
        changed |= updateOldDouble("stats.archery.defense-penetration-per-100-percent", 5.0, 0.0);
        changed |= updateOldDouble("stats.fighting.defense-penetration-per-100-percent", 5.0, 0.0);
        changed |= updateOldDouble("stats.power.defense-penetration-per-100-percent", 7.0, 0.0);
        changed |= updateOldDouble("stats.archery.max-health-damage-per-100-percent", 1.4, 0.0);
        changed |= updateOldDouble("stats.fighting.max-health-damage-per-100-percent", 1.4, 0.0);
        changed |= updateOldDouble("stats.power.max-health-damage-per-100-percent", 1.8, 0.0);

        if (getConfig().getInt("settings.balance-version", 1) < 9) {
            getConfig().set("stats.damage.effective-cap-percent", 380.0);
            getConfig().set("stats.damage.overflow-efficiency-percent", 25.0);
            getConfig().set("stats.damage.raw-damage-per-100-percent", 6.0);
            getConfig().set("stats.archery.bonus-per-point-percent", 0.24);
            getConfig().set("stats.fighting.bonus-per-point-percent", 0.24);
            getConfig().set("stats.power.bonus-per-point-percent", 0.28);
            getConfig().set("stats.defense.bonus-per-point-percent", 0.22);
            getConfig().set("stats.defense.effective-cap-percent", 80.0);
            getConfig().set("stats.defense.absolute-cap-percent", 92.0);
            getConfig().set("stats.health.bonus-per-point-percent", 0.045);
            getConfig().set("stats.health.health-per-point", 0.12);
            getConfig().set("stats.health.effective-cap-percent", 250.0);
            getConfig().set("stats.health.flat-effective-cap", 420.0);
            getConfig().set("stats.health.flat-overflow-efficiency-percent", 25.0);
            getConfig().set("stats.alchemy.bonus-per-point-percent", 0.18);
            getConfig().set("stats.alchemy.max-health-heal-per-100-percent", 1.2);
            getConfig().set("settings.balance-version", 9);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 10) {
            getConfig().set("settings.max-total-stat-points", 100000);
            getConfig().set("stats.archery.soft-cap-percent", 100.0);
            getConfig().set("stats.archery.overflow-efficiency-percent", 10.0);
            getConfig().set("stats.fighting.soft-cap-percent", 100.0);
            getConfig().set("stats.fighting.overflow-efficiency-percent", 10.0);
            getConfig().set("stats.power.soft-cap-percent", 100.0);
            getConfig().set("stats.power.overflow-efficiency-percent", 10.0);
            getConfig().set("stats.defense.soft-cap-percent", 100.0);
            getConfig().set("stats.defense.overflow-efficiency-percent", 15.0);
            getConfig().set("stats.health.soft-cap-percent", 100.0);
            getConfig().set("stats.health.overflow-efficiency-percent", 20.0);
            getConfig().set("stats.health.effective-cap-percent", 1000.0);
            getConfig().set("stats.health.flat-effective-cap", 1000.0);
            getConfig().set("stats.health.flat-overflow-efficiency-percent", 20.0);
            getConfig().set("stats.alchemy.soft-cap-percent", 100.0);
            getConfig().set("stats.alchemy.overflow-efficiency-percent", 12.0);
            getConfig().set("stats.damage.pvp-max-health-percent-per-hit", 18.0);
            getConfig().set("settings.balance-version", 10);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 11) {
            getConfig().set("stats.damage.raw-damage-per-100-percent", 50.0);
            getConfig().set("stats.archery.max-bonus-percent", 10000.0);
            getConfig().set("stats.fighting.max-bonus-percent", 10000.0);
            getConfig().set("stats.power.max-bonus-percent", 10000.0);
            getConfig().set("stats.defense.max-bonus-percent", 10000.0);
            getConfig().set("stats.health.max-bonus-percent", 10000.0);
            getConfig().set("stats.alchemy.max-bonus-percent", 10000.0);
            getConfig().set("settings.balance-version", 11);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 12) {
            getConfig().set("exp-sources.mob-kill.default", 3);
            getConfig().set("exp-sources.mob-kill.bosses", 18);
            getConfig().set("exp-sources.player-kill", 10);
            getConfig().set("exp-sources.combat.damage-exp-every", 12.0);
            getConfig().set("exp-sources.combat.max-exp-per-hit", 18);
            getConfig().set("exp-sources.alchemy.brew-potion", 5);
            getConfig().set("settings.balance-version", 12);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 13) {
            getConfig().set("exp-sources.mob-kill.default", 3);
            getConfig().set("exp-sources.mob-kill.passive-default", 2);
            getConfig().set("exp-sources.mob-kill.bosses", 18);
            getConfig().set("exp-sources.player-kill", 10);
            getConfig().set("exp-sources.combat.damage-exp-every", 12.0);
            getConfig().set("exp-sources.combat.max-exp-per-hit", 18);
            getConfig().set("exp-sources.alchemy.brew-potion", 5);
            getConfig().set("exp-sources.fishing.fish", 3);
            getConfig().set("exp-sources.fishing.treasure", 5);
            getConfig().set("exp-sources.fishing.junk", 1);
            changed |= raisePositiveInts("exp-sources.mob-kill.types", 2);
            changed |= raisePositiveInts("exp-sources.mining", 2);
            changed |= raisePositiveInts("exp-sources.farming", 2);
            changed |= raisePositiveInts("exp-sources.woodcutting", 2);
            changed |= raisePositiveInts("exp-sources.digging", 2);
            getConfig().set("settings.balance-version", 13);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 14) {
            getConfig().set("rank-reroll.weights.F", 2600);
            getConfig().set("rank-reroll.weights.D", 1400);
            getConfig().set("rank-reroll.weights.C", 650);
            getConfig().set("rank-reroll.weights.B", 250);
            getConfig().set("rank-reroll.weights.A", 80);
            getConfig().set("rank-reroll.weights.S", 14);
            getConfig().set("rank-reroll.weights.SS", 4);
            getConfig().set("rank-reroll.weights.SSS", 1);
            getConfig().set("clans.HUMAN.weight", 1400);
            getConfig().set("clans.GUARDIAN.weight", 500);
            getConfig().set("clans.ELF.weight", 450);
            getConfig().set("clans.DWARF.weight", 400);
            getConfig().set("clans.ASSASSIN.weight", 120);
            getConfig().set("clans.ORC.weight", 110);
            getConfig().set("clans.TITAN.weight", 100);
            getConfig().set("clans.CELESTIAL.weight", 35);
            getConfig().set("clans.KITSUNE.weight", 25);
            getConfig().set("clans.VAMPIRE.weight", 30);
            getConfig().set("clans.DRAGONKIN.weight", 6);
            getConfig().set("clans.DEMON.weight", 4);
            getConfig().set("clans.SHADOW.weight", 1);
            getConfig().set("clans.PHOENIX.weight", 1);
            getConfig().set("settings.balance-version", 14);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 15) {
            getConfig().set("rank-reroll.weights.F", 300000);
            getConfig().set("rank-reroll.weights.D", 135000);
            getConfig().set("rank-reroll.weights.C", 50000);
            getConfig().set("rank-reroll.weights.B", 12000);
            getConfig().set("rank-reroll.weights.A", 2500);
            getConfig().set("rank-reroll.weights.S", 400);
            getConfig().set("rank-reroll.weights.SS", 99);
            getConfig().set("rank-reroll.weights.SSS", 1);
            getConfig().set("clans.HUMAN.weight", 300000);
            getConfig().set("clans.GUARDIAN.weight", 85000);
            getConfig().set("clans.ELF.weight", 78000);
            getConfig().set("clans.DWARF.weight", 70000);
            getConfig().set("clans.ASSASSIN.weight", 12000);
            getConfig().set("clans.ORC.weight", 10000);
            getConfig().set("clans.TITAN.weight", 9000);
            getConfig().set("clans.CELESTIAL.weight", 2200);
            getConfig().set("clans.KITSUNE.weight", 1500);
            getConfig().set("clans.VAMPIRE.weight", 1900);
            getConfig().set("clans.DRAGONKIN.weight", 350);
            getConfig().set("clans.DEMON.weight", 48);
            getConfig().set("clans.SHADOW.weight", 1);
            getConfig().set("clans.PHOENIX.weight", 1);
            getConfig().set("settings.balance-version", 15);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 16) {
            getConfig().set("exp-sources.mob-kill.default", 5);
            getConfig().set("exp-sources.mob-kill.bosses", 25);
            getConfig().set("exp-sources.mob-kill.types.zombie", 4);
            getConfig().set("exp-sources.mob-kill.types.skeleton", 4);
            getConfig().set("exp-sources.mob-kill.types.spider", 4);
            getConfig().set("exp-sources.mob-kill.types.creeper", 5);
            getConfig().set("exp-sources.mob-kill.types.enderman", 7);
            getConfig().set("exp-sources.mob-kill.types.blaze", 6);
            getConfig().set("exp-sources.mob-kill.types.witch", 7);
            getConfig().set("exp-sources.mob-kill.types.pillager", 5);
            getConfig().set("exp-sources.mob-kill.types.vindicator", 5);
            getConfig().set("exp-sources.mob-kill.types.ravager", 16);
            getConfig().set("exp-sources.mob-kill.types.guardian", 5);
            getConfig().set("exp-sources.mob-kill.types.elder_guardian", 12);
            getConfig().set("exp-sources.mob-kill.types.wither_skeleton", 6);
            getConfig().set("exp-sources.mob-kill.types.piglin_brute", 6);
            getConfig().set("exp-sources.mob-kill.types.warden", 14);
            getConfig().set("exp-sources.mob-kill.types.wither", 45);
            getConfig().set("exp-sources.mob-kill.types.ender_dragon", 90);
            getConfig().set("exp-sources.fishing.fish", 6);
            getConfig().set("exp-sources.fishing.treasure", 12);
            getConfig().set("exp-sources.fishing.junk", 2);
            getConfig().set("settings.balance-version", 16);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 17) {
            getConfig().set("stats.alchemy.low-health-regen-tier-1-percent", 20.0);
            getConfig().set("stats.alchemy.low-health-regen-tier-2-percent", 50.0);
            getConfig().set("stats.alchemy.low-health-regen-tier-3-percent", 80.0);
            getConfig().set("stats.alchemy.low-health-regen-cooldown-ms", 9000);
            getConfig().set("stats.alchemy.low-health-regen-base-ticks", 45);
            getConfig().set("stats.alchemy.low-health-regen-ticks-per-bonus-percent", 0.18);
            getConfig().set("stats.alchemy.low-health-regen-max-extra-ticks", 55.0);
            getConfig().set("stats.alchemy.low-health-regen-bonus-per-amplifier", 220.0);
            getConfig().set("stats.alchemy.low-health-regen-max-amplifier", 3);
            getConfig().set("clans.DWARF.mining-exp-percent", 18);
            getConfig().set("clans.GOLEM.mining-exp-percent", 25);
            getConfig().set("clans.ASTRAL.mining-exp-percent", 8);
            getConfig().set("clans.MERMAID.fishing-exp-percent", 20);
            getConfig().set("clans.AETHER.damage-percent", 1);
            getConfig().set("clans.AETHER.archer-percent", 14);
            getConfig().set("clans.STORMCALLER.damage-percent", 2);
            getConfig().set("clans.STORMCALLER.archer-percent", 16);
            getConfig().set("clans.RAIKAGE.damage-percent", 8);
            getConfig().set("clans.RAIKAGE.assassin-percent", 4);
            getConfig().set("clans.BLAZEFURY.damage-percent", 9);
            getConfig().set("clans.BLAZEFURY.fire-aspect-hit", false);
            getConfig().set("clans.ONI.damage-percent", 7);
            getConfig().set("clans.ONI.assassin-percent", 6);
            getConfig().set("settings.balance-version", 17);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 18) {
            getConfig().set("clan-effects.shadow-thunder-max-charges", 10);
            getConfig().set("clan-effects.shadow-thunder-damage-per-charge", 1.2);
            getConfig().set("clan-effects.mining-bonus-max-drops", 1);
            getConfig().set("clan-effects.mining-bonus-max-stack", 3);
            getConfig().set("clan-effects.golem-quake-cooldown-ms", 9000);
            getConfig().set("clan-effects.golem-quake-radius", 3.0);
            getConfig().set("clan-effects.golem-quake-slow-ticks", 45);
            getConfig().set("clan-effects.golem-quake-bonus-damage", 1.5);
            getConfig().set("clan-effects.necromancer-command-cooldown-ms", 16000);
            getConfig().set("clan-effects.necromancer-command-radius", 30.0);
            getConfig().set("clan-effects.necromancer-command-limit", 6);
            getConfig().set("clans.DWARF.mining-bonus-drop-chance", 8);
            getConfig().set("clans.GOLEM.mining-bonus-drop-chance", 5);
            getConfig().set("clans.ASTRAL.mining-bonus-drop-chance", 3);
            getConfig().set("clans.DWARF.mining-exp-percent", null);
            getConfig().set("clans.GOLEM.mining-exp-percent", null);
            getConfig().set("clans.ASTRAL.mining-exp-percent", null);
            getConfig().set("settings.balance-version", 18);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 19) {
            getConfig().set("settings.balance-version", 19);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 20) {
            getConfig().set("exp-sources.mining.coal_ore", 1);
            getConfig().set("exp-sources.mining.copper_ore", 1);
            getConfig().set("exp-sources.mining.nether_gold_ore", 1);
            getConfig().set("exp-sources.mining.nether_quartz_ore", 1);
            getConfig().set("exp-sources.farming.wheat", 1);
            getConfig().set("exp-sources.farming.carrots", 1);
            getConfig().set("exp-sources.farming.potatoes", 1);
            getConfig().set("exp-sources.farming.beetroot", 1);
            getConfig().set("exp-sources.farming.melon", 1);
            getConfig().set("exp-sources.farming.sugar_cane", 1);
            getConfig().set("exp-sources.farming.sweet_berry_bush", 1);
            getConfig().set("exp-sources.farming.cactus", 1);
            getConfig().set("exp-sources.farming.bamboo", 1);
            getConfig().set("exp-sources.farming.brown_mushroom", 1);
            getConfig().set("exp-sources.farming.red_mushroom", 1);
            getConfig().set("exp-sources.farming.chorus_flower", 3);
            getConfig().set("exp-sources.farming.pitcher_crop", 3);
            getConfig().set("exp-sources.farming.torchflower_crop", 3);
            getConfig().set("exp-sources.woodcutting.oak_log", 1);
            getConfig().set("exp-sources.woodcutting.spruce_log", 1);
            getConfig().set("exp-sources.woodcutting.birch_log", 1);
            getConfig().set("exp-sources.woodcutting.jungle_log", 1);
            getConfig().set("exp-sources.woodcutting.acacia_log", 1);
            getConfig().set("exp-sources.woodcutting.dark_oak_log", 1);
            getConfig().set("exp-sources.woodcutting.mangrove_log", 1);
            getConfig().set("exp-sources.woodcutting.cherry_log", 1);
            getConfig().set("exp-sources.woodcutting.oak_wood", 1);
            getConfig().set("exp-sources.woodcutting.spruce_wood", 1);
            getConfig().set("exp-sources.woodcutting.birch_wood", 1);
            getConfig().set("exp-sources.woodcutting.jungle_wood", 1);
            getConfig().set("exp-sources.woodcutting.acacia_wood", 1);
            getConfig().set("exp-sources.woodcutting.dark_oak_wood", 1);
            getConfig().set("exp-sources.woodcutting.mangrove_wood", 1);
            getConfig().set("exp-sources.woodcutting.cherry_wood", 1);
            getConfig().set("exp-sources.digging.dirt", 1);
            getConfig().set("exp-sources.digging.grass_block", 1);
            getConfig().set("exp-sources.digging.coarse_dirt", 1);
            getConfig().set("exp-sources.digging.rooted_dirt", 1);
            getConfig().set("exp-sources.digging.podzol", 1);
            getConfig().set("exp-sources.digging.mycelium", 1);
            getConfig().set("exp-sources.digging.sand", 1);
            getConfig().set("exp-sources.digging.red_sand", 1);
            getConfig().set("exp-sources.digging.gravel", 1);
            getConfig().set("exp-sources.digging.mud", 1);
            getConfig().set("exp-sources.digging.netherrack", 1);
            getConfig().set("exp-sources.digging.end_stone", 3);
            getConfig().set("exp-sources.digging.blackstone", 1);
            getConfig().set("exp-sources.digging.basalt", 1);
            getConfig().set("exp-sources.digging.smooth_basalt", 1);
            getConfig().set("exp-sources.digging.soul_sand", 1);
            getConfig().set("exp-sources.digging.soul_soil", 1);
            getConfig().set("settings.balance-version", 20);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 21) {
            getConfig().set("exp-sources.mob-kill.default", 2);
            getConfig().set("exp-sources.mob-kill.passive-default", 1);
            getConfig().set("exp-sources.mob-kill.types.zombie", 1);
            getConfig().set("exp-sources.mob-kill.types.skeleton", 1);
            getConfig().set("exp-sources.mob-kill.types.spider", 1);
            getConfig().set("exp-sources.mob-kill.types.creeper", 2);
            getConfig().set("exp-sources.mob-kill.types.enderman", 3);
            getConfig().set("exp-sources.mob-kill.types.blaze", 3);
            getConfig().set("exp-sources.mob-kill.types.witch", 4);
            getConfig().set("exp-sources.mob-kill.types.pillager", 3);
            getConfig().set("exp-sources.mob-kill.types.vindicator", 4);
            getConfig().set("exp-sources.mob-kill.types.guardian", 3);
            getConfig().set("exp-sources.mob-kill.types.elder_guardian", 8);
            getConfig().set("exp-sources.mob-kill.types.wither_skeleton", 3);
            getConfig().set("exp-sources.mob-kill.types.piglin_brute", 4);
            getConfig().set("exp-sources.mob-kill.types.warden", 20);
            getConfig().set("exp-sources.mob-kill.types.cow", 1);
            getConfig().set("exp-sources.mob-kill.types.sheep", 1);
            getConfig().set("exp-sources.mob-kill.types.pig", 1);
            getConfig().set("exp-sources.mob-kill.types.chicken", 1);
            getConfig().set("exp-sources.mob-kill.types.rabbit", 1);
            getConfig().set("exp-sources.mob-kill.types.cod", 1);
            getConfig().set("exp-sources.mob-kill.types.salmon", 1);
            getConfig().set("settings.balance-version", 21);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 22) {
            getConfig().set("clans.DRAGONKIN.weight", 400);
            getConfig().set("clans.DEMON.weight", 55);
            getConfig().set("clans.SHADOW.weight", 2);
            getConfig().set("clans.PHOENIX.weight", 2);
            getConfig().set("clans.FROSTBORNE.weight", 1350);
            getConfig().set("clans.AETHER.weight", 1120);
            getConfig().set("clans.ONI.weight", 1000);
            getConfig().set("clans.STORMCALLER.weight", 950);
            getConfig().set("clans.RAIKAGE.weight", 135);
            getConfig().set("clans.BLAZEFURY.weight", 90);
            getConfig().set("clans.WARDENBORN.weight", 35);
            getConfig().set("clans.VOIDWALKER.weight", 24);
            getConfig().set("clans.NECROMANCER.weight", 30);
            getConfig().set("clans.ASTRAL.weight", 22);
            getConfig().set("settings.balance-version", 22);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 23) {
            getConfig().set("clan-effects.raikage-strike-bonus-damage", 4.0);
            getConfig().set("clan-effects.blazefury-nova-damage", 4.5);
            getConfig().set("clan-effects.wardenborn-roar-bonus-damage", 6.0);
            getConfig().set("clan-effects.void-step-reduction-percent", 45.0);
            getConfig().set("clan-effects.necromancer-heal-percent", 10.0);

            getConfig().set("clans.RAIKAGE.damage-percent", 14);
            getConfig().set("clans.RAIKAGE.health-percent", 2);
            getConfig().set("clans.RAIKAGE.defense-percent", 4);
            getConfig().set("clans.RAIKAGE.assassin-percent", 14);
            getConfig().set("clans.RAIKAGE.description", java.util.List.of(
                    "Ras kilat mythic untuk tempo melee cepat.",
                    "Damage tinggi tanpa mengorbankan durability."));
            getConfig().set("clans.RAIKAGE.passive-skills", java.util.List.of(
                    "&eLightning Step &7- melee hit memberi speed dan bonus damage petir.",
                    "&a+14% &7damage dan &a+14% &7melee damage.",
                    "&a+2% &7health dan &a+4% &7defense."));

            getConfig().set("clans.BLAZEFURY.damage-percent", 18);
            getConfig().set("clans.BLAZEFURY.health-percent", -4);
            getConfig().set("clans.BLAZEFURY.defense-percent", -2);
            getConfig().set("clans.BLAZEFURY.description", java.util.List.of(
                    "Ras panas mythic dengan burst area.",
                    "Sangat agresif, tetap punya drawback ringan."));
            getConfig().set("clans.BLAZEFURY.passive-skills", java.util.List.of(
                    "&6Fire Nova &7- melee hit bisa menyemburkan api ke sekitar dengan cooldown.",
                    "&a+18% &7damage.",
                    "&c-4% &7health dan &c-2% &7defense."));

            getConfig().set("clans.WARDENBORN.damage-percent", 14);
            getConfig().set("clans.WARDENBORN.health-percent", 20);
            getConfig().set("clans.WARDENBORN.defense-percent", 14);
            getConfig().set("clans.WARDENBORN.description", java.util.List.of(
                    "Ras kuno mythic dengan daya tahan besar.",
                    "Tanky, stabil, dan punya burst terkontrol."));
            getConfig().set("clans.WARDENBORN.passive-skills", java.util.List.of(
                    "&3Sonic Roar &7- melee hit memberi sonic boom, darkness, dan bonus damage.",
                    "&a+20% &7health, &a+14% &7defense, &a+14% &7damage."));

            getConfig().set("clans.VOIDWALKER.damage-percent", 12);
            getConfig().set("clans.VOIDWALKER.health-percent", 2);
            getConfig().set("clans.VOIDWALKER.defense-percent", 12);
            getConfig().set("clans.VOIDWALKER.night-damage-percent", 18);
            getConfig().set("clans.VOIDWALKER.description", java.util.List.of(
                    "Ras void mythic yang sulit ditangkap.",
                    "Saat kena hit bisa blink dan mengurangi damage."));
            getConfig().set("clans.VOIDWALKER.passive-skills", java.util.List.of(
                    "&5Void Step &7- saat kena hit bisa teleport pendek, reduce damage, dan speed.",
                    "&a+12% &7damage, &a+12% &7defense, &5+18% &7damage malam.",
                    "&a+2% &7health."));

            getConfig().set("clans.NECROMANCER.damage-percent", 10);
            getConfig().set("clans.NECROMANCER.health-percent", 2);
            getConfig().set("clans.NECROMANCER.defense-percent", 6);
            getConfig().set("clans.NECROMANCER.alchemy-percent", 20);
            getConfig().set("clans.NECROMANCER.night-damage-percent", 12);
            getConfig().set("clans.NECROMANCER.description", java.util.List.of(
                    "Ras kutukan mythic dengan wither dan sustain.",
                    "Kuat untuk duel lama dan alchemy."));
            getConfig().set("clans.NECROMANCER.passive-skills", java.util.List.of(
                    "&2Wither Hex &7- hit memberi wither dan heal dari damage.",
                    "&a+20% &7potion heal, &a+10% &7damage, &a+6% &7defense.",
                    "&a+2% &7health dan &5+12% &7damage malam."));

            getConfig().set("clans.ASTRAL.damage-percent", 12);
            getConfig().set("clans.ASTRAL.health-percent", 8);
            getConfig().set("clans.ASTRAL.defense-percent", 8);
            getConfig().set("clans.ASTRAL.archer-percent", 12);
            getConfig().set("clans.ASTRAL.alchemy-percent", 10);
            getConfig().set("clans.ASTRAL.mining-bonus-drop-chance", 5);
            getConfig().set("clans.ASTRAL.night-damage-percent", 8);
            getConfig().set("clans.ASTRAL.description", java.util.List.of(
                    "Ras bintang mythic dengan mark dan partikel astral.",
                    "Build fleksibel untuk damage, sustain, dan farming."));
            getConfig().set("clans.ASTRAL.passive-skills", java.util.List.of(
                    "&dAstral Mark &7- hit memberi glowing, partikel, dan bonus damage.",
                    "&a+12% &7damage, &a+12% &7projectile, &a+10% &7potion heal.",
                    "&dStar Fortune &7- mining kadang memberi drop bonus.",
                    "&a+8% &7health, &a+8% &7defense, dan &5+8% &7damage malam."));

            getConfig().set("settings.balance-version", 23);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 24) {
            getConfig().set("stats.damage.pvp-max-health-percent-per-hit", 22.0);
            getConfig().set("stats.damage.pvp-min-health-percent-per-hit", 3.0);
            getConfig().set("stats.archery.defense-penetration-per-100-percent", 8.0);
            getConfig().set("stats.fighting.defense-penetration-per-100-percent", 8.0);
            getConfig().set("stats.power.defense-penetration-per-100-percent", 10.0);
            getConfig().set("stats.power.defense-penetration-cap-percent", 70.0);
            getConfig().set("settings.balance-version", 24);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 25) {
            getConfig().set("stats.damage.pvp-level-gap-cap-bonus-percent-per-level", 0.12);
            getConfig().set("stats.damage.pvp-level-gap-max-health-percent-per-hit", 120.0);
            getConfig().set("settings.balance-version", 25);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 26) {
            getConfig().set("stats.damage.pvp-max-health-percent-per-hit", 0.0);
            getConfig().set("stats.damage.pvp-level-gap-cap-bonus-percent-per-level", 0.0);
            getConfig().set("stats.damage.pvp-level-gap-max-health-percent-per-hit", 0.0);
            getConfig().set("actionbar-text", "&#ff1453â¤ {health_formatted}&7/&f{max_health_formatted} &8            &#f3e08dâœ¦ {exp_formatted}&7/&f{needed_exp_formatted}");
            getConfig().set("settings.balance-version", 26);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 27) {
            getConfig().set("stats.damage.raw-damage-per-100-percent", 65.0);
            getConfig().set("stats.defense.effective-cap-percent", 75.0);
            getConfig().set("stats.health.health-per-point", 0.11);
            getConfig().set("stats.health.flat-effective-cap", 900.0);
            getConfig().set("settings.balance-version", 27);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 28) {
            getConfig().set("exp-sources.mob-kill.default", 2);
            getConfig().set("exp-sources.mob-kill.passive-default", 1);
            getConfig().set("exp-sources.combat.damage-exp-every", 25.0);
            getConfig().set("exp-sources.combat.max-exp-per-hit", 6);
            getConfig().set("exp-sources.mining.coal_ore", 2);
            getConfig().set("exp-sources.mining.deepslate_coal_ore", 2);
            getConfig().set("exp-sources.mining.copper_ore", 2);
            getConfig().set("exp-sources.mining.deepslate_copper_ore", 2);
            getConfig().set("exp-sources.mining.iron_ore", 3);
            getConfig().set("exp-sources.mining.deepslate_iron_ore", 4);
            getConfig().set("exp-sources.mining.gold_ore", 3);
            getConfig().set("exp-sources.mining.deepslate_gold_ore", 4);
            getConfig().set("exp-sources.mining.redstone_ore", 3);
            getConfig().set("exp-sources.mining.deepslate_redstone_ore", 4);
            getConfig().set("exp-sources.mining.lapis_ore", 3);
            getConfig().set("exp-sources.mining.deepslate_lapis_ore", 4);
            getConfig().set("exp-sources.mining.diamond_ore", 8);
            getConfig().set("exp-sources.mining.deepslate_diamond_ore", 10);
            getConfig().set("exp-sources.mining.emerald_ore", 9);
            getConfig().set("exp-sources.mining.deepslate_emerald_ore", 11);
            getConfig().set("exp-sources.mining.ancient_debris", 22);
            getConfig().set("exp-sources.mining.amethyst_cluster", 4);
            getConfig().set("exp-sources.farming.wheat", 2);
            getConfig().set("exp-sources.farming.carrots", 2);
            getConfig().set("exp-sources.farming.potatoes", 2);
            getConfig().set("exp-sources.farming.beetroot", 2);
            getConfig().set("exp-sources.farming.nether_wart", 5);
            getConfig().set("exp-sources.farming.pumpkin", 3);
            getConfig().set("exp-sources.farming.sugar_cane", 2);
            getConfig().set("exp-sources.farming.kelp", 3);
            getConfig().set("exp-sources.farming.chorus_flower", 5);
            getConfig().set("exp-sources.farming.chorus_plant", 3);
            getConfig().set("exp-sources.farming.pitcher_crop", 5);
            getConfig().set("exp-sources.farming.torchflower_crop", 5);
            getConfig().set("exp-sources.woodcutting.oak_log", 2);
            getConfig().set("exp-sources.woodcutting.stripped_oak_log", 2);
            getConfig().set("exp-sources.woodcutting.spruce_log", 2);
            getConfig().set("exp-sources.woodcutting.stripped_spruce_log", 2);
            getConfig().set("exp-sources.woodcutting.birch_log", 2);
            getConfig().set("exp-sources.woodcutting.stripped_birch_log", 2);
            getConfig().set("exp-sources.woodcutting.jungle_log", 2);
            getConfig().set("exp-sources.woodcutting.stripped_jungle_log", 2);
            getConfig().set("exp-sources.woodcutting.acacia_log", 2);
            getConfig().set("exp-sources.woodcutting.stripped_acacia_log", 2);
            getConfig().set("exp-sources.woodcutting.dark_oak_log", 2);
            getConfig().set("exp-sources.woodcutting.stripped_dark_oak_log", 2);
            getConfig().set("exp-sources.woodcutting.mangrove_log", 2);
            getConfig().set("exp-sources.woodcutting.stripped_mangrove_log", 2);
            getConfig().set("exp-sources.woodcutting.cherry_log", 2);
            getConfig().set("exp-sources.woodcutting.stripped_cherry_log", 2);
            getConfig().set("exp-sources.woodcutting.crimson_stem", 3);
            getConfig().set("exp-sources.woodcutting.stripped_crimson_stem", 3);
            getConfig().set("exp-sources.woodcutting.warped_stem", 3);
            getConfig().set("exp-sources.woodcutting.stripped_warped_stem", 3);
            getConfig().set("exp-sources.woodcutting.oak_wood", 2);
            getConfig().set("exp-sources.woodcutting.spruce_wood", 2);
            getConfig().set("exp-sources.woodcutting.birch_wood", 2);
            getConfig().set("exp-sources.woodcutting.jungle_wood", 2);
            getConfig().set("exp-sources.woodcutting.acacia_wood", 2);
            getConfig().set("exp-sources.woodcutting.dark_oak_wood", 2);
            getConfig().set("exp-sources.woodcutting.mangrove_wood", 2);
            getConfig().set("exp-sources.woodcutting.cherry_wood", 2);
            getConfig().set("exp-sources.woodcutting.crimson_hyphae", 3);
            getConfig().set("exp-sources.woodcutting.warped_hyphae", 3);
            getConfig().set("exp-sources.digging.clay", 3);
            getConfig().set("exp-sources.digging.moss_block", 3);
            getConfig().set("exp-sources.digging.calcite", 3);
            getConfig().set("exp-sources.digging.tuff", 3);
            getConfig().set("exp-sources.digging.dripstone_block", 3);
            getConfig().set("exp-sources.digging.end_stone", 4);
            getConfig().set("exp-sources.fishing.fish", 10);
            getConfig().set("exp-sources.fishing.treasure", 22);
            getConfig().set("exp-sources.fishing.junk", 3);
            getConfig().set("exp-sources.alchemy.brew-potion", 12);
            getConfig().set("exp-sources.enchanting.base", 3);
            getConfig().set("exp-sources.enchanting.exp-per-level", 0.35);
            getConfig().set("exp-sources.enchanting.max-exp", 20);
            getConfig().set("exp-sources.smelting.exp-per-item", 1);
            getConfig().set("exp-sources.smelting.max-exp-per-extract", 32);
            getConfig().set("exp-sources.breeding.animal", 8);
            getConfig().set("settings.balance-version", 28);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 29) {
            getConfig().set("stats.alchemy.low-health-regen-tier-1-percent", 40.0);
            getConfig().set("stats.alchemy.low-health-regen-tier-2-percent", 65.0);
            getConfig().set("stats.alchemy.low-health-regen-tier-3-percent", 85.0);
            getConfig().set("stats.alchemy.low-health-regen-cooldown-ms", 16000);
            getConfig().set("stats.alchemy.low-health-regen-base-ticks", 35);
            getConfig().set("stats.alchemy.low-health-regen-ticks-per-bonus-percent", 0.10);
            getConfig().set("stats.alchemy.low-health-regen-max-extra-ticks", 35.0);
            getConfig().set("stats.alchemy.low-health-regen-bonus-per-amplifier", 450.0);
            getConfig().set("stats.alchemy.low-health-regen-max-amplifier", 2);
            getConfig().set("settings.balance-version", 29);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 30) {
            getConfig().set("stats.alchemy.max-health-heal-per-100-percent", 0.45);
            getConfig().set("stats.alchemy.low-health-regen-cooldown-ms", 22000);
            getConfig().set("stats.alchemy.low-health-regen-base-ticks", 25);
            getConfig().set("stats.alchemy.low-health-regen-ticks-per-bonus-percent", 0.05);
            getConfig().set("stats.alchemy.low-health-regen-max-extra-ticks", 20.0);
            getConfig().set("stats.alchemy.low-health-regen-bonus-per-amplifier", 9999.0);
            getConfig().set("stats.alchemy.low-health-regen-max-amplifier", 1);
            getConfig().set("clan-effects.alchemy-min-instant-bonus-health", 0.25);
            getConfig().set("settings.balance-version", 30);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 31) {
            getConfig().set("stats.archery.defense-penetration-per-100-percent", 6.0);
            getConfig().set("stats.fighting.defense-penetration-per-100-percent", 6.0);
            getConfig().set("stats.power.defense-penetration-per-100-percent", 8.0);
            getConfig().set("stats.power.defense-penetration-cap-percent", 60.0);
            getConfig().set("stats.defense.health-per-point", 0.01);
            getConfig().set("stats.defense.health-flat-effective-cap", 500.0);
            getConfig().set("stats.defense.health-flat-overflow-efficiency-percent", 10.0);
            getConfig().set("settings.balance-version", 31);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 32) {
            applyGrindingExpansion32();
            getConfig().set("settings.balance-version", 32);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 33) {
            applyGrindingExpansion33();
            getConfig().set("settings.balance-version", 33);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 34) {
            getConfig().set("exp-sources.mob-kill.bosses", 75);
            getConfig().set("exp-sources.mob-kill.types.elder_guardian", 30);
            getConfig().set("exp-sources.mob-kill.types.warden", 120);
            getConfig().set("exp-sources.mob-kill.types.wither", 180);
            getConfig().set("exp-sources.mob-kill.types.ender_dragon", 250);
            getConfig().set("settings.balance-version", 34);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 35) {
            getConfig().set("exp-sources.mob-kill.bosses", 35);
            getConfig().set("exp-sources.mob-kill.types.elder_guardian", 18);
            getConfig().set("exp-sources.mob-kill.types.warden", 45);
            getConfig().set("exp-sources.mob-kill.types.wither", 70);
            getConfig().set("exp-sources.mob-kill.types.ender_dragon", 100);
            getConfig().set("settings.balance-version", 35);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 36) {
            applyMobBalance36();
            getConfig().set("settings.balance-version", 36);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 37) {
            getConfig().set("stats.fighting.mace-damage-per-100-percent", 2.0);
            getConfig().set("stats.fighting.mace-damage-cap-percent", 25.0);
            getConfig().set("settings.balance-version", 37);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 38) {
            getConfig().set("stats.fighting.mace-flat-damage-enabled", false);
            getConfig().set("settings.balance-version", 38);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 39) {
            getConfig().set("stats.damage.stat-percent-multiplier-enabled", true);
            getConfig().set("stats.fighting.mace-flat-damage-enabled", null);
            getConfig().set("stats.fighting.mace-damage-per-100-percent", null);
            getConfig().set("stats.fighting.mace-damage-cap-percent", null);
            getConfig().set("settings.balance-version", 39);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 40) {
            getConfig().set("stats.damage.stat-percent-multiplier-enabled", false);
            getConfig().set("settings.balance-version", 40);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 41) {
            getConfig().set("stats.damage.raw-damage-per-100-percent", 24.0);
            getConfig().set("settings.balance-version", 41);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 42) {
            getConfig().set("stats.archery.overflow-efficiency-percent", 25.0);
            getConfig().set("stats.fighting.overflow-efficiency-percent", 25.0);
            getConfig().set("stats.power.overflow-efficiency-percent", 25.0);
            getConfig().set("settings.balance-version", 42);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 43) {
            getConfig().set("stats.fighting.mace-breach-defense-reduction-percent-per-level", 15.0);
            getConfig().set("settings.balance-version", 43);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 44) {
            getConfig().set("stats.damage.pvp-max-health-percent-per-hit", 0.0);
            getConfig().set("stats.damage.pvp-level-gap-cap-bonus-percent-per-level", 0.0);
            getConfig().set("stats.damage.pvp-level-gap-max-health-percent-per-hit", 0.0);
            getConfig().set("settings.balance-version", 44);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 45) {
            getConfig().set("stats.archery.defense-penetration-per-100-percent", 3.5);
            getConfig().set("stats.fighting.defense-penetration-per-100-percent", 3.5);
            getConfig().set("stats.power.defense-penetration-per-100-percent", 4.5);
            getConfig().set("stats.power.defense-penetration-cap-percent", 40.0);
            getConfig().set("settings.balance-version", 45);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 46) {
            applyMobExpBuff46();
            getConfig().set("settings.balance-version", 46);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 47) {
            applyActivityExpBuff47();
            getConfig().set("settings.balance-version", 47);
            changed = true;
        }
        if (getConfig().getInt("settings.balance-version", 1) < 48) {
            getConfig().set("actionbar-text", "&#ff1453â¤ {health_formatted}&7/&f{max_health_formatted} &8            &#f3e08dâœ¦ {exp_formatted}&7/&f{needed_exp_formatted}");
            getConfig().set("settings.balance-version", 48);
            changed = true;
        }
        return changed;
    }

    private void applyMobExpBuff46() {
        getConfig().set("exp-sources.mob-kill.default", 3);
        getConfig().set("exp-sources.mob-kill.bosses", 50);
        Map<String, Integer> mobExp = Map.ofEntries(
                Map.entry("zombie", 3), Map.entry("zombie_villager", 4), Map.entry("drowned", 4),
                Map.entry("husk", 4), Map.entry("skeleton", 3), Map.entry("stray", 4),
                Map.entry("bogged", 5), Map.entry("spider", 3), Map.entry("cave_spider", 4),
                Map.entry("creeper", 4), Map.entry("witch", 8), Map.entry("phantom", 6),
                Map.entry("silverfish", 3), Map.entry("endermite", 3), Map.entry("breeze", 12),
                Map.entry("pillager", 6), Map.entry("vindicator", 8), Map.entry("evoker", 16),
                Map.entry("vex", 5), Map.entry("ravager", 25), Map.entry("guardian", 7),
                Map.entry("elder_guardian", 25), Map.entry("warden", 60), Map.entry("blaze", 6),
                Map.entry("ghast", 10), Map.entry("wither_skeleton", 6), Map.entry("piglin", 4),
                Map.entry("piglin_brute", 9), Map.entry("hoglin", 6), Map.entry("zoglin", 9),
                Map.entry("enderman", 6), Map.entry("shulker", 10), Map.entry("wither", 90),
                Map.entry("ender_dragon", 130));
        mobExp.forEach((type, exp) -> getConfig().set("exp-sources.mob-kill.types." + type, exp));
    }
    private void applyActivityExpBuff47() {
        Map<String, Integer> miningExp = Map.ofEntries(
                Map.entry("coal_ore", 3), Map.entry("deepslate_coal_ore", 3),
                Map.entry("copper_ore", 3), Map.entry("deepslate_copper_ore", 3),
                Map.entry("iron_ore", 5), Map.entry("deepslate_iron_ore", 6),
                Map.entry("gold_ore", 5), Map.entry("deepslate_gold_ore", 6),
                Map.entry("redstone_ore", 5), Map.entry("deepslate_redstone_ore", 6),
                Map.entry("lapis_ore", 5), Map.entry("deepslate_lapis_ore", 6),
                Map.entry("diamond_ore", 12), Map.entry("deepslate_diamond_ore", 15),
                Map.entry("emerald_ore", 13), Map.entry("deepslate_emerald_ore", 16),
                Map.entry("nether_gold_ore", 2), Map.entry("nether_quartz_ore", 2),
                Map.entry("ancient_debris", 30), Map.entry("amethyst_cluster", 6));
        miningExp.forEach((block, exp) -> getConfig().set("exp-sources.mining." + block, exp));

        Map<String, Integer> farmingExp = Map.ofEntries(
                Map.entry("wheat", 3), Map.entry("carrots", 3), Map.entry("potatoes", 3),
                Map.entry("beetroots", 3), Map.entry("nether_wart", 7), Map.entry("cocoa", 3),
                Map.entry("pumpkin", 4), Map.entry("melon", 2), Map.entry("sugar_cane", 3),
                Map.entry("cactus", 2), Map.entry("bamboo", 2), Map.entry("kelp", 4),
                Map.entry("kelp_plant", 4), Map.entry("sweet_berry_bush", 3),
                Map.entry("cave_vines", 4), Map.entry("cave_vines_plant", 4),
                Map.entry("brown_mushroom", 2), Map.entry("red_mushroom", 2),
                Map.entry("mushroom_stem", 3), Map.entry("chorus_flower", 7),
                Map.entry("chorus_plant", 4), Map.entry("pitcher_crop", 7),
                Map.entry("torchflower_crop", 7), Map.entry("mangrove_propagule", 3),
                Map.entry("weeping_vines", 3), Map.entry("weeping_vines_plant", 3),
                Map.entry("twisting_vines", 3), Map.entry("twisting_vines_plant", 3),
                Map.entry("sea_pickle", 2));
        farmingExp.forEach((block, exp) -> getConfig().set("exp-sources.farming." + block, exp));

        raiseActivityExp("exp-sources.woodcutting", 3);
        getConfig().set("exp-sources.woodcutting.crimson_stem", 4);
        getConfig().set("exp-sources.woodcutting.stripped_crimson_stem", 4);
        getConfig().set("exp-sources.woodcutting.warped_stem", 4);
        getConfig().set("exp-sources.woodcutting.stripped_warped_stem", 4);
        getConfig().set("exp-sources.woodcutting.crimson_hyphae", 4);
        getConfig().set("exp-sources.woodcutting.warped_hyphae", 4);

        raiseActivityExp("exp-sources.digging", 2);
        getConfig().set("exp-sources.digging.clay", 4);
        getConfig().set("exp-sources.digging.mud_bricks", 3);
        getConfig().set("exp-sources.digging.snow_block", 3);
        getConfig().set("exp-sources.digging.moss_block", 4);
        getConfig().set("exp-sources.digging.calcite", 4);
        getConfig().set("exp-sources.digging.tuff", 4);
        getConfig().set("exp-sources.digging.dripstone_block", 4);
        getConfig().set("exp-sources.digging.end_stone", 6);

        getConfig().set("exp-sources.fishing.fish", 12);
        getConfig().set("exp-sources.fishing.treasure", 28);
        getConfig().set("exp-sources.fishing.junk", 4);
        getConfig().set("exp-sources.alchemy.brew-potion", 16);
        getConfig().set("exp-sources.enchanting.base", 4);
        getConfig().set("exp-sources.enchanting.exp-per-level", 0.45);
        getConfig().set("exp-sources.enchanting.max-exp", 26);
        getConfig().set("exp-sources.smelting.exp-per-item", 2);
        getConfig().set("exp-sources.smelting.max-exp-per-extract", 48);
        getConfig().set("exp-sources.breeding.animal", 10);
        getConfig().set("exp-sources.taming.animal", 18);
        getConfig().set("exp-sources.shearing.entity", 4);
    }

    private void raiseActivityExp(String path, int minimum) {
        org.bukkit.configuration.ConfigurationSection section = getConfig().getConfigurationSection(path);
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String child = path + "." + key;
            if (getConfig().isInt(child) && getConfig().getInt(child) > 0) {
                getConfig().set(child, Math.max(minimum, getConfig().getInt(child)));
            }
        }
    }
    private void applyGrindingExpansion32() {
        Map<String, Integer> mobExp = Map.ofEntries(
                Map.entry("zombie_villager", 2), Map.entry("drowned", 2), Map.entry("husk", 2),
                Map.entry("stray", 2), Map.entry("bogged", 3), Map.entry("cave_spider", 2),
                Map.entry("phantom", 3), Map.entry("slime", 1), Map.entry("silverfish", 2),
                Map.entry("endermite", 2), Map.entry("breeze", 6), Map.entry("evoker", 8),
                Map.entry("vex", 3), Map.entry("ghast", 5), Map.entry("magma_cube", 2),
                Map.entry("piglin", 2), Map.entry("zombified_piglin", 2), Map.entry("hoglin", 3),
                Map.entry("zoglin", 4), Map.entry("shulker", 5), Map.entry("armadillo", 1),
                Map.entry("goat", 1), Map.entry("camel", 1), Map.entry("sniffer", 2),
                Map.entry("turtle", 1), Map.entry("frog", 1), Map.entry("bee", 1),
                Map.entry("axolotl", 1), Map.entry("squid", 1), Map.entry("glow_squid", 1),
                Map.entry("pufferfish", 1), Map.entry("tropical_fish", 1));
        mobExp.forEach((type, exp) -> getConfig().set("exp-sources.mob-kill.types." + type, exp));

        Map<String, Integer> farmingExp = Map.ofEntries(
                Map.entry("beetroots", 2), Map.entry("kelp_plant", 3),
                Map.entry("sweet_berry_bush", 2), Map.entry("cave_vines", 3),
                Map.entry("cave_vines_plant", 3), Map.entry("mangrove_propagule", 2),
                Map.entry("weeping_vines", 2), Map.entry("weeping_vines_plant", 2),
                Map.entry("twisting_vines", 2), Map.entry("twisting_vines_plant", 2),
                Map.entry("sea_pickle", 1));
        farmingExp.forEach((block, exp) -> getConfig().set("exp-sources.farming." + block, exp));
        getConfig().set("exp-sources.farming.beetroot", null);
    }

    private void applyGrindingExpansion33() {
        getConfig().set("exp-sources.taming.animal", 15);
        getConfig().set("exp-sources.shearing.entity", 3);
        getConfig().set("exp-sources.beekeeping.honey-harvest", 8);
        getConfig().set("exp-sources.advancement.task", 15);
        getConfig().set("exp-sources.advancement.goal", 30);
        getConfig().set("exp-sources.advancement.challenge", 60);
        getConfig().set("exp-sources.raid.win", 120);
    }

    private void applyMobBalance36() {
        Map<String, Integer> mobExp = Map.ofEntries(
                Map.entry("zombie", 2), Map.entry("zombie_villager", 3), Map.entry("drowned", 3),
                Map.entry("husk", 3), Map.entry("skeleton", 2), Map.entry("stray", 3),
                Map.entry("bogged", 4), Map.entry("spider", 2), Map.entry("cave_spider", 3),
                Map.entry("creeper", 3), Map.entry("witch", 6), Map.entry("phantom", 4),
                Map.entry("slime", 1), Map.entry("silverfish", 2), Map.entry("endermite", 2),
                Map.entry("breeze", 8), Map.entry("pillager", 4), Map.entry("vindicator", 6),
                Map.entry("evoker", 12), Map.entry("vex", 4), Map.entry("ravager", 18),
                Map.entry("guardian", 5), Map.entry("elder_guardian", 18), Map.entry("warden", 45),
                Map.entry("blaze", 4), Map.entry("ghast", 7), Map.entry("magma_cube", 2),
                Map.entry("wither_skeleton", 4), Map.entry("piglin", 3), Map.entry("piglin_brute", 6),
                Map.entry("zombified_piglin", 2), Map.entry("hoglin", 4), Map.entry("zoglin", 6),
                Map.entry("enderman", 4), Map.entry("shulker", 7));
        mobExp.forEach((type, exp) -> getConfig().set("exp-sources.mob-kill.types." + type, exp));
    }

    private boolean raisePositiveInts(String path, int minimum) {
        org.bukkit.configuration.ConfigurationSection section = getConfig().getConfigurationSection(path);
        if (section == null) return false;
        boolean changed = false;
        for (String key : section.getKeys(false)) {
            String child = path + "." + key;
            if (getConfig().isInt(child)) {
                int value = getConfig().getInt(child);
                if (value > 0 && value < minimum) {
                    getConfig().set(child, minimum);
                    changed = true;
                }
            }
        }
        if (getConfig().getInt("settings.balance-version", 1) < 23) {
            getConfig().set("clan-effects.raikage-strike-bonus-damage", 4.0);
            getConfig().set("clan-effects.blazefury-nova-damage", 4.5);
            getConfig().set("clan-effects.wardenborn-roar-bonus-damage", 6.0);
            getConfig().set("clan-effects.void-step-reduction-percent", 45.0);
            getConfig().set("clan-effects.necromancer-heal-percent", 10.0);

            getConfig().set("clans.RAIKAGE.damage-percent", 14);
            getConfig().set("clans.RAIKAGE.health-percent", 2);
            getConfig().set("clans.RAIKAGE.defense-percent", 4);
            getConfig().set("clans.RAIKAGE.assassin-percent", 14);
            getConfig().set("clans.RAIKAGE.description", java.util.List.of(
                    "Ras kilat mythic untuk tempo melee cepat.",
                    "Damage tinggi tanpa mengorbankan durability."));
            getConfig().set("clans.RAIKAGE.passive-skills", java.util.List.of(
                    "&eLightning Step &7- melee hit memberi speed dan bonus damage petir.",
                    "&a+14% &7damage dan &a+14% &7melee damage.",
                    "&a+2% &7health dan &a+4% &7defense."));

            getConfig().set("clans.BLAZEFURY.damage-percent", 18);
            getConfig().set("clans.BLAZEFURY.health-percent", -4);
            getConfig().set("clans.BLAZEFURY.defense-percent", -2);
            getConfig().set("clans.BLAZEFURY.description", java.util.List.of(
                    "Ras panas mythic dengan burst area.",
                    "Sangat agresif, tetap punya drawback ringan."));
            getConfig().set("clans.BLAZEFURY.passive-skills", java.util.List.of(
                    "&6Fire Nova &7- melee hit bisa menyemburkan api ke sekitar dengan cooldown.",
                    "&a+18% &7damage.",
                    "&c-4% &7health dan &c-2% &7defense."));

            getConfig().set("clans.WARDENBORN.damage-percent", 14);
            getConfig().set("clans.WARDENBORN.health-percent", 20);
            getConfig().set("clans.WARDENBORN.defense-percent", 14);
            getConfig().set("clans.WARDENBORN.description", java.util.List.of(
                    "Ras kuno mythic dengan daya tahan besar.",
                    "Tanky, stabil, dan punya burst terkontrol."));
            getConfig().set("clans.WARDENBORN.passive-skills", java.util.List.of(
                    "&3Sonic Roar &7- melee hit memberi sonic boom, darkness, dan bonus damage.",
                    "&a+20% &7health, &a+14% &7defense, &a+14% &7damage."));

            getConfig().set("clans.VOIDWALKER.damage-percent", 12);
            getConfig().set("clans.VOIDWALKER.health-percent", 2);
            getConfig().set("clans.VOIDWALKER.defense-percent", 12);
            getConfig().set("clans.VOIDWALKER.night-damage-percent", 18);
            getConfig().set("clans.VOIDWALKER.description", java.util.List.of(
                    "Ras void mythic yang sulit ditangkap.",
                    "Saat kena hit bisa blink dan mengurangi damage."));
            getConfig().set("clans.VOIDWALKER.passive-skills", java.util.List.of(
                    "&5Void Step &7- saat kena hit bisa teleport pendek, reduce damage, dan speed.",
                    "&a+12% &7damage, &a+12% &7defense, &5+18% &7damage malam.",
                    "&a+2% &7health."));

            getConfig().set("clans.NECROMANCER.damage-percent", 10);
            getConfig().set("clans.NECROMANCER.health-percent", 2);
            getConfig().set("clans.NECROMANCER.defense-percent", 6);
            getConfig().set("clans.NECROMANCER.alchemy-percent", 20);
            getConfig().set("clans.NECROMANCER.night-damage-percent", 12);
            getConfig().set("clans.NECROMANCER.description", java.util.List.of(
                    "Ras kutukan mythic dengan wither dan sustain.",
                    "Kuat untuk duel lama dan alchemy."));
            getConfig().set("clans.NECROMANCER.passive-skills", java.util.List.of(
                    "&2Wither Hex &7- hit memberi wither dan heal dari damage.",
                    "&a+20% &7potion heal, &a+10% &7damage, &a+6% &7defense.",
                    "&a+2% &7health dan &5+12% &7damage malam."));

            getConfig().set("clans.ASTRAL.damage-percent", 12);
            getConfig().set("clans.ASTRAL.health-percent", 8);
            getConfig().set("clans.ASTRAL.defense-percent", 8);
            getConfig().set("clans.ASTRAL.archer-percent", 12);
            getConfig().set("clans.ASTRAL.alchemy-percent", 10);
            getConfig().set("clans.ASTRAL.mining-bonus-drop-chance", 5);
            getConfig().set("clans.ASTRAL.night-damage-percent", 8);
            getConfig().set("clans.ASTRAL.description", java.util.List.of(
                    "Ras bintang mythic dengan mark dan partikel astral.",
                    "Build fleksibel untuk damage, sustain, dan farming."));
            getConfig().set("clans.ASTRAL.passive-skills", java.util.List.of(
                    "&dAstral Mark &7- hit memberi glowing, partikel, dan bonus damage.",
                    "&a+12% &7damage, &a+12% &7projectile, &a+10% &7potion heal.",
                    "&dStar Fortune &7- mining kadang memberi drop bonus.",
                    "&a+8% &7health, &a+8% &7defense, dan &5+8% &7damage malam."));

            getConfig().set("settings.balance-version", 23);
            changed = true;
        }        return changed;
    }

    private boolean updateOldDouble(String path, double oldValue, double newValue) {
        if (!getConfig().isSet(path) || Math.abs(getConfig().getDouble(path) - oldValue) > 0.0001) return false;
        getConfig().set(path, newValue);
        return true;
    }

    private boolean setMissingDouble(String path, double value) {
        if (getConfig().isSet(path)) return false;
        getConfig().set(path, value);
        return true;
    }

    private boolean setMissingInt(String path, int value) {
        if (getConfig().isSet(path)) return false;
        getConfig().set(path, value);
        return true;
    }

    public void loadLevelConfig() {
        File file = new File(getDataFolder(), "level.yml");
        levelConfig = YamlConfiguration.loadConfiguration(file);
        if (migrateLevelBalance(null)) {
            getLogger().info("Updated level EXP curve.");
        }
    }

    private boolean migrateLevelBalance(File saveFile) {
        if (levelConfig.getConfigurationSection("levels") == null) {
            levelConfig.createSection("levels");
        }
        org.bukkit.configuration.ConfigurationSection levels = levelConfig.getConfigurationSection("levels");
        int version = levelConfig.getInt("settings.exp-balance-version", 1);
        boolean changed = false;
        changed |= setMissingLevelDouble("settings.exp-base", 100.0);
        changed |= setMissingLevelDouble("settings.exp-power", 1.537);
        changed |= setMissingLevelDouble("settings.exp-multiplier", 84.2);
        long level25 = levelConfig.getLong("levels.25.exp-needed", 0L);
        long expected25 = hardExpNeeded(25);

        if (version < 6 || level25 < Math.round(expected25 * 0.75) || level25 > Math.round(expected25 * 1.25)) {
            for (String key : levels.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    levelConfig.set("levels." + level + ".exp-needed", hardExpNeeded(level));
                    changed = true;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        int maxLevel = maxLevel();
        for (int level = 1; level <= maxLevel; level++) {
            String path = "levels." + level + ".exp-needed";
            if (!levelConfig.isSet(path)) {
                levelConfig.set(path, hardExpNeeded(level));
                changed = true;
            }
        }
        if (version < 7) {
            levelConfig.set("settings.exp-balance-version", 7);
            changed = true;
        }
        if (!changed) return false;
        if (saveFile != null) {
            try {
                levelConfig.save(saveFile);
            } catch (Exception e) {
                getLogger().warning("Failed to update levels EXP curve: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    public long hardExpNeeded(int level) {
        double base = levelConfig.getDouble("settings.exp-base", 100.0);
        double power = levelConfig.getDouble("settings.exp-power", 1.537);
        double multiplier = levelConfig.getDouble("settings.exp-multiplier", 84.2);
        return Math.max(1L, Math.round(base + Math.pow(level, power) * multiplier));
    }

    private boolean setMissingLevelDouble(String path, double value) {
        if (levelConfig.isSet(path)) return false;
        levelConfig.set(path, value);
        return true;
    }

    public void loadLangConfig() {
        String language = getConfig().getString("settings.language", "EN").replaceAll("[^A-Za-z0-9_-]", "").toUpperCase(Locale.ROOT);
        File folderFile = new File(getDataFolder(), "lang/Lang" + language + ".yml");
        File fallbackFile = new File(getDataFolder(), "lang/LangEN.yml");
        langConfig = YamlConfiguration.loadConfiguration(folderFile.exists() ? folderFile : fallbackFile);
        prefix = Text.s(lang("messages.prefix", getConfig().getString("messages.prefix", "")));
    }

    public void loadPageConfig() {
        YamlConfiguration merged = new YamlConfiguration();
        File menuFolder = new File(getDataFolder(), "menu");
        File[] menuFiles = menuFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (menuFiles != null && menuFiles.length > 0) {
            java.util.Arrays.sort(menuFiles, java.util.Comparator.comparing(f -> f.getName(), String.CASE_INSENSITIVE_ORDER));
            for (File file : menuFiles) mergeMenuFile(merged, YamlConfiguration.loadConfiguration(file), file);
        }
        pageConfig = merged;
    }

    private void mergeMenuFile(YamlConfiguration target, YamlConfiguration source, File file) {
        if (!source.isConfigurationSection("menu")) {
            mergeInto(target, source, "");
            return;
        }
        String menuId = source.getString("menu.id", menuIdFromFile(file));
        if (source.isSet("menu.size")) target.set("menus." + menuId + ".size", source.getInt("menu.size"));
        if (source.isSet("menu.title")) target.set("menu.titles." + menuId, source.getString("menu.title"));
        if (source.isList("menu.stat-slots")) target.set("menus." + menuId + ".stat-slots", source.getIntegerList("menu.stat-slots"));
        if (source.isList("menu.auto-slots")) target.set("menus." + menuId + ".auto-slots", source.getIntegerList("menu.auto-slots"));
        // Namespace items by menu ID: Main.yml items → menus.stats.items.*, Level.yml → menus.level.items.*
        copySection(source, target, "menu.items", "menus." + menuId + ".items");
        // Copy menu-level config (slots, sizes, etc.) that are already at menu.* level
        for (String key : source.getConfigurationSection("menu").getKeys(false)) {
            if (key.equals("items") || key.equals("id") || key.equals("title") || key.equals("size")
                    || key.equals("stat-slots") || key.equals("auto-slots")) continue;
            target.set("menus." + menuId + "." + key, source.get("menu." + key));
        }
    }

    private String menuIdFromFile(File file) {
        String name = file.getName().replaceFirst("(?i)\\.ya?ml$", "").toLowerCase(Locale.ROOT);
        return switch (name) {
            case "main" -> "stats";
            case "races" -> "clan";
            default -> name;
        };
    }

    private void copySection(YamlConfiguration source, YamlConfiguration target, String sourcePath, String targetPath) {
        org.bukkit.configuration.ConfigurationSection section = source.getConfigurationSection(sourcePath);
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String from = sourcePath + "." + key;
            String to = targetPath + "." + key;
            if (source.isConfigurationSection(from)) {
                copySection(source, target, from, to);
            } else {
                target.set(to, source.get(from));
            }
        }
    }

    private void mergeInto(YamlConfiguration target, YamlConfiguration source, String path) {
        org.bukkit.configuration.ConfigurationSection section = path.isEmpty() ? source : source.getConfigurationSection(path);
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String child = path.isEmpty() ? key : path + "." + key;
            if (source.isConfigurationSection(child)) {
                mergeInto(target, source, child);
            } else {
                target.set(child, source.get(child));
            }
        }
    }

    public void loadSourceConfigs() {
        sourceConfig = loadSplitConfig("source/Grinding.yml");
        fragmentConfig = loadSplitConfig("source/Fragments.yml");
        raceConfig = loadSplitConfig("source/Races.yml");
        clanConfig = loadSplitConfig("source/Clans.yml");
    }

    private FileConfiguration loadSplitConfig(String path) {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), path));
    }

    public boolean sourceIsSet(String path) {
        return sourceConfig != null && sourceConfig.isSet(path) || getConfig().isSet(path);
    }

    public int sourceInt(String path, int fallback) {
        if (sourceConfig != null && sourceConfig.isSet(path)) return sourceConfig.getInt(path, fallback);
        return getConfig().getInt(path, fallback);
    }

    public double sourceDouble(String path, double fallback) {
        if (sourceConfig != null && sourceConfig.isSet(path)) return sourceConfig.getDouble(path, fallback);
        return getConfig().getDouble(path, fallback);
    }

    public boolean sourceBoolean(String path, boolean fallback) {
        if (sourceConfig != null && sourceConfig.isSet(path)) return sourceConfig.getBoolean(path, fallback);
        return getConfig().getBoolean(path, fallback);
    }

    public int maxLevel() {
        return Math.max(1, getConfig().getInt("settings.max-level", 1000));
    }

    public String msg(String key) {
        return prefix + Text.s(lang("messages." + key, getConfig().getString("messages." + key, key)));
    }

    public String lang(String key, String fallback) {
        if (langConfig != null && langConfig.isString(key)) return langConfig.getString(key, fallback);
        return fallback;
    }

    public java.util.List<String> langList(String key, java.util.List<String> fallback) {
        if (langConfig != null && langConfig.isList(key)) return langConfig.getStringList(key);
        return fallback;
    }

    public String langLine() {
        String fullLine = lang("format.line.text", "");
        if (fullLine != null && !fullLine.isBlank()) return fullLine;
        int length = Math.max(1, langConfig == null ? 28 : langConfig.getInt("format.line.length", 28));
        String character = lang("format.line.character", "-");
        if (character == null || character.isBlank()) character = "-";
        return lang("format.line.color", "&8") + character.substring(0, 1).repeat(length);
    }

    public boolean statsEnabled() {
        return getConfig().getBoolean("modules.stats", true);
    }

    public boolean classesEnabled() {
        return getConfig().getBoolean("modules.class", true);
    }

    public boolean racesEnabled() {
        return getConfig().getBoolean("modules.race", true);
    }

    public boolean clansEnabled() {
        return getConfig().getBoolean("modules.clan", true);
    }

    /**
     * Generic module check used by menu visibility.
     * Maps module names (stats, class, race, clan) to their enabled state.
     */
    public boolean isModuleEnabled(String module) {
        return switch (module.toLowerCase(java.util.Locale.ROOT)) {
            case "stats", "stat", "point" -> statsEnabled();
            case "class", "classes", "rank", "ranks" -> classesEnabled();
            case "race", "races", "clan-menu" -> racesEnabled();
            case "clan", "clans", "guild", "guilds" -> clansEnabled();
            default -> true; // unknown module → always visible
        };
    }

    public double classMultiplier(PlayerProfile profile, StatType type) {
        return classesEnabled() ? profile.rank(type).multiplier() : 1.0;
    }

    public void applyStats(Player player) {
        PlayerProfile profile = data.get(player);
        double maxHealth = physicalMaxHealth(profile);
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            double oldMaxHealth = attr.getValue();
            double healthPercent = player.getHealth() / Math.max(1.0, oldMaxHealth);
            try {
                attr.setBaseValue(maxHealth);
            } catch (IllegalArgumentException ex) {
                maxHealth = Math.min(maxHealth, 1024.0);
                attr.setBaseValue(maxHealth);
            }
            if (player.getHealth() > attr.getValue()) {
                player.setHealth(attr.getValue());
            } else if (maxHealth > oldMaxHealth && healthPercent >= 0.99) {
                player.setHealth(attr.getValue());
            }
        }
        player.setHealthScaled(true);
        player.setHealthScale(20.0);
        if (racesEnabled() && raceConfig().getBoolean("clans." + profile.race() + ".water-breathing", getConfig().getBoolean("clans." + profile.race() + ".water-breathing", false))) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 60 * 60, 0, true, false));
        } else {
            player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        }
        if (racesEnabled() && raceConfig().getBoolean("clans." + profile.race() + ".haste", getConfig().getBoolean("clans." + profile.race() + ".haste", false))) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 60 * 60, getConfig().getInt("clan-effects.dwarf-haste-amplifier", 0), true, false));
        } else {
            player.removePotionEffect(PotionEffectType.HASTE);
        }
    }

    public double physicalMaxHealth(PlayerProfile profile) {
        double effective = effectiveMaxHealth(profile);
        double cap = Math.max(20.0, getConfig().getDouble("settings.physical-health-cap", 1024.0));
        return Math.min(effective, cap);
    }

    public double physicalMaxHealth(Player player) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr == null ? Math.max(1.0, player.getHealth()) : Math.max(1.0, attr.getValue());
    }

    public double effectiveCurrentHealth(Player player) {
        PlayerProfile profile = data.get(player);
        double effectiveMax = effectiveMaxHealth(profile);
        double physicalMax = physicalMaxHealth(player);
        return Math.max(0.0, Math.min(effectiveMax, player.getHealth() * effectiveMax / physicalMax));
    }

    public double toPhysicalHealthAmount(Player player, double effectiveAmount) {
        if (effectiveAmount <= 0.0) return 0.0;
        PlayerProfile profile = data.get(player);
        double effectiveMax = effectiveMaxHealth(profile);
        double physicalMax = physicalMaxHealth(player);
        if (effectiveMax <= physicalMax) return effectiveAmount;
        return effectiveAmount * physicalMax / effectiveMax;
    }

    public double maxHealth(PlayerProfile profile) {
        double clanPercent = (racesEnabled() ? clans.value(profile.race(), "health-percent") : 0.0) + (clansEnabled() ? clans.guildValue(profile.guildClan(), "health-percent") : 0.0);
        double cap = getConfig().getDouble("stats.health.effective-cap-percent", 35.0) * classMultiplier(profile, StatType.HEALTH);
        double cappedPercent = clanPercent + Math.max(0, Math.min(cap, levels.statBonusPercent(profile, StatType.HEALTH)));
        return Math.max(1, ((20.0 + levels.healthFlat(profile)) * (1.0 + cappedPercent / 100.0)) + levels.defenseHealthFlat(profile));
    }

    public double effectiveMaxHealth(PlayerProfile profile) {
        double cap = 2000.0
                * classMultiplier(profile, StatType.HEALTH)
                * (1.0 + Math.max(0.0, (racesEnabled() ? clans.value(profile.race(), "health-percent") : 0.0) + (clansEnabled() ? clans.guildValue(profile.guildClan(), "health-percent") : 0.0)) / 100.0);
        return Math.min(Math.max(1.0, cap), maxHealth(profile));
    }

    public double outgoingBonusPercent(Player player, boolean projectile) {
        return (outgoingMultiplier(player, projectile) - 1.0) * 100.0;
    }

    public double outgoingMultiplier(PlayerProfile profile, boolean projectile) {
        return outgoingMultiplier(profile, projectile ? StatType.ARCHERY : StatType.FIGHTING, -1L);
    }

    public double incomingReductionPercent(PlayerProfile profile) {
        double clanPercent = (racesEnabled() ? clans.value(profile.race(), "defense-percent") : 0.0) + (clansEnabled() ? clans.guildValue(profile.guildClan(), "defense-percent") : 0.0);
        double cap = getConfig().getDouble("stats.defense.effective-cap-percent", 35.0) * classMultiplier(profile, StatType.DEFENSE);
        double percent = clanPercent + Math.max(0, Math.min(cap, levels.statBonusPercent(profile, StatType.DEFENSE)));
        double absoluteCap = getConfig().getDouble("stats.defense.absolute-cap-percent", 95.0);
        return Math.max(-20, Math.min(absoluteCap, percent));
    }

    public double outgoingMultiplier(Player player, boolean projectile) {
        PlayerProfile profile = data.get(player);
        return outgoingMultiplier(profile, attackStat(player, projectile), player.getWorld().getTime());
    }

    private double outgoingMultiplier(PlayerProfile profile, StatType attackType, long time) {
        double statPercent = 0.0;
        if (getConfig().getBoolean("stats.damage.stat-percent-multiplier-enabled", false)) {
            statPercent = levels.statBonusPercent(profile, attackType);
        }
        boolean projectile = attackType == StatType.ARCHERY;
        double percent = softCap(statPercent, outgoingCapPercent(profile, attackType),
                getConfig().getDouble("stats.damage.overflow-efficiency-percent", 35.0))
                + (racesEnabled() ? clans.value(profile.race(), "damage-percent") : 0.0)
                + (clansEnabled() ? clans.guildValue(profile.guildClan(), "damage-percent") : 0.0)
                + (projectile ? (racesEnabled() ? clans.value(profile.race(), "archer-percent") : 0.0) + (clansEnabled() ? clans.guildValue(profile.guildClan(), "archer-percent") : 0.0) : attackType == StatType.FIGHTING ? (racesEnabled() ? clans.value(profile.race(), "assassin-percent") : 0.0) + (clansEnabled() ? clans.guildValue(profile.guildClan(), "assassin-percent") : 0.0) : 0.0);
        if (time >= 13000 && time <= 23000) percent += (racesEnabled() ? clans.value(profile.race(), "night-damage-percent") : 0.0) + (clansEnabled() ? clans.guildValue(profile.guildClan(), "night-damage-percent") : 0.0);
        return 1.0 + Math.max(-40, percent) / 100.0;
    }

    public double outgoingCapPercent(PlayerProfile profile, boolean projectile) {
        return outgoingCapPercent(profile, projectile ? StatType.ARCHERY : StatType.FIGHTING);
    }

    public double outgoingCapPercent(PlayerProfile profile, StatType attackType) {
        double baseCap = getConfig().getDouble("stats.damage.effective-cap-percent", 55.0);
        return baseCap * classMultiplier(profile, attackType);
    }

    public double softCap(double value, double cap, double overflowEfficiencyPercent) {
        if (cap <= 0 || value <= cap) return Math.max(0.0, value);
        double overflow = value - cap;
        double efficiency = Math.max(0.0, overflowEfficiencyPercent) / 100.0;
        return Math.max(0.0, cap + (overflow * efficiency));
    }

    public double incomingMultiplier(Player player) {
        PlayerProfile profile = data.get(player);
        return 1.0 - incomingReductionPercent(profile) / 100.0;
    }

    public double incomingMultiplier(Player victim, Player damager, boolean projectile) {
        PlayerProfile victimProfile = data.get(victim);
        double reduction = incomingReductionPercent(victimProfile);
        if (damager != null) {
            reduction -= defensePenetrationPercent(damager, projectile);
            reduction *= 1.0 - maceBreachDefenseBypassPercent(damager, projectile) / 100.0;
        }
        return 1.0 - Math.max(0.0, reduction) / 100.0;
    }

    private double maceBreachDefenseBypassPercent(Player damager, boolean projectile) {
        if (projectile) return 0.0;
        ItemStack hand = damager.getInventory().getItemInMainHand();
        if (hand.getType() != Material.MACE) return 0.0;
        int level = hand.getEnchantmentLevel(Enchantment.BREACH);
        double perLevel = getConfig().getDouble("stats.fighting.mace-breach-defense-reduction-percent-per-level", 15.0);
        return Math.min(100.0, Math.max(0.0, level * perLevel));
    }

    public double defensePenetrationPercent(Player player, boolean projectile) {
        PlayerProfile profile = data.get(player);
        return defensePenetrationPercent(profile, attackStat(player, projectile));
    }

    public double defensePenetrationPercent(PlayerProfile profile, boolean projectile) {
        return defensePenetrationPercent(profile, projectile ? StatType.ARCHERY : StatType.FIGHTING);
    }

    public double defensePenetrationPercent(PlayerProfile profile, StatType attackType) {
        double weapon = levels.statBonusPercent(profile, attackType)
                * getConfig().getDouble("stats." + attackType.configKey() + ".defense-penetration-per-100-percent", 5.0) / 100.0;
        double cap = getConfig().getDouble("stats.power.defense-penetration-cap-percent", 85.0);
        return Math.max(0.0, Math.min(cap, weapon));
    }

    public double maxHealthBonusDamage(Player damager, LivingEntity target, boolean projectile) {
        return rawBonusDamage(data.get(damager), attackStat(damager, projectile));
    }

    public double maxHealthDamagePerThousand(PlayerProfile profile, boolean projectile) {
        return rawBonusDamage(profile, projectile);
    }

    public double damagePreview(PlayerProfile profile, boolean projectile) {
        return damagePreview(profile, projectile ? StatType.ARCHERY : StatType.FIGHTING);
    }

    public double damagePreview(PlayerProfile profile, StatType attackType) {
        return rawBonusDamage(profile, attackType);
    }

    public double defenseBlockPerThousand(PlayerProfile profile) {
        return incomingReductionPercent(profile) * 10.0;
    }

    public double rawBonusDamage(PlayerProfile profile, boolean projectile) {
        return rawBonusDamage(profile, projectile ? StatType.ARCHERY : StatType.FIGHTING);
    }

    public double rawBonusDamage(PlayerProfile profile, StatType attackType) {
        double statPercent = levels.statBonusPercent(profile, attackType);
        return Math.max(0.0, statPercent * getConfig().getDouble("stats.damage.raw-damage-per-100-percent", 20.0) / 100.0);
    }

    private StatType attackStat(Player player, boolean projectile) {
        if (projectile) return StatType.ARCHERY;
        ItemStack hand = player.getInventory().getItemInMainHand();
        Material material = hand == null ? Material.AIR : hand.getType();
        return isMeleeWeapon(material) ? StatType.FIGHTING : StatType.POWER;
    }

    private boolean isMeleeWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("MACE") || name.equals("TRIDENT");
    }

    public boolean actionbarEnabled() {
        return getConfig().getBoolean("actionbar", true);
    }

    private void sendPersistentActionBars() {
        if (!actionbarEnabled()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!data.get(player).skillActionbar()) continue;
            player.sendActionBar(Text.c(actionbarText(player)));
        }
    }

    private String actionbarText(Player player) {
        PlayerProfile profile = data.get(player);
        long needed = levels.neededExp(profile.level());
        String neededText = needed == Long.MAX_VALUE ? "MAX" : String.valueOf(needed);
        String neededFormatted = needed == Long.MAX_VALUE ? "MAX" : formatCompact(needed);
        double expPercent = needed == Long.MAX_VALUE ? 100.0 : (profile.exp() * 100.0 / Math.max(1, needed));
        double maxHealth = effectiveMaxHealth(profile);
        double health = effectiveCurrentHealth(player);
        return getConfig().getString("actionbar-text", "&cHP &f{health}&7/&f{max_health} &8        &aEXP &f{exp}&7/&f{needed_exp}")
                .replace("{player}", player.getName())
                .replace("{level}", String.valueOf(profile.level()))
                .replace("{exp}", String.valueOf(profile.exp()))
                .replace("{exp_formatted}", formatCompact(profile.exp()))
                .replace("{needed_exp}", neededText)
                .replace("{needed_exp_formatted}", neededFormatted)
                .replace("{exp_percent}", oneDecimal(expPercent))
                .replace("{health}", oneDecimal(health))
                .replace("{health_formatted}", formatCompact(health))
                .replace("{max_health}", oneDecimal(maxHealth))
                .replace("{max_health_formatted}", formatCompact(maxHealth))
                .replace("{health_percent}", oneDecimal(health * 100.0 / Math.max(1.0, maxHealth)))
                .replace("{clan}", clans.guildDisplayName(profile.guildClan()))
                .replace("{race}", clans.displayName(profile.race()))
                .replace("{stats_point}", String.valueOf(profile.statsPoint()));
    }

    public String formatCompact(long value) {
        if (value < 10_000L) return String.format(Locale.US, "%,d", value).replace(',', '.');
        if (value < 1_000_000L) return trimCompact(value / 1_000.0) + "K";
        if (value < 1_000_000_000L) return trimCompact(value / 1_000_000.0) + "M";
        return trimCompact(value / 1_000_000_000.0) + "B";
    }

    public String formatCompact(double value) {
        if (value < 0) return "-" + formatCompact(-value);
        if (value < 10_000.0) return oneDecimal(value);
        if (value < 1_000_000.0) return trimCompact(value / 1_000.0) + "K";
        if (value < 1_000_000_000.0) return trimCompact(value / 1_000_000.0) + "M";
        return trimCompact(value / 1_000_000_000.0) + "B";
    }

    private String trimCompact(double value) {
        if (value >= 100 || Math.abs(value - Math.rint(value)) < 0.05) {
            return String.valueOf((int) Math.round(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private String oneDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05) {
            return String.valueOf((int) Math.round(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

