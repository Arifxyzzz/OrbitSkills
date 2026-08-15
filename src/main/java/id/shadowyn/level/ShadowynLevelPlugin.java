package id.shadowyn.level;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
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
    private final java.util.Set<java.util.UUID> pendingHealthDisplay = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile Boolean paperBasedServer;
    private final java.util.Map<java.util.UUID, Long> actionbarHold = new java.util.concurrent.ConcurrentHashMap<>();
    /** Fixed identity so a modifier from an earlier session is found and replaced, never stacked. */
    private static final String HEALTH_MODIFIER_NAME = "orbitskills_health";
    private static final java.util.UUID HEALTH_MODIFIER_ID = java.util.UUID.fromString("2f9a1c47-6b3e-4d18-9f52-0a7c3e5d8b41");
    /** Below this, a stat change is not worth a packet the client could read as a hit. */
    private static final double HEALTH_MODIFIER_EPSILON = 0.1;
    private PlayerDataStore data;
    private LevelService levels;
    private ClanService clans;
    private FragmentService fragments;
    private MenuService menus;
    private id.shadowyn.level.hooks.HookManager hooks;
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
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(name -> args[0].isBlank() || name.toLowerCase(java.util.Locale.ROOT).startsWith(lower))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        });
        hooks = new id.shadowyn.level.hooks.HookManager(this);
        hooks.registerAll();
        Bukkit.getPluginManager().registerEvents(new GrindListener(this), this);
        Bukkit.getPluginManager().registerEvents(menus, this);
        registerArmorDisplayListener();
        long saveTicks = 20L * getConfig().getInt("settings.save-interval-seconds", 180);
        Bukkit.getScheduler().runTaskTimer(this, () -> data.save(), saveTicks, saveTicks);
        Bukkit.getScheduler().runTaskTimer(this, this::sendPersistentActionBars, 20L, 20L);
        long healthDisplayTicks = Math.max(0L, getConfig().getLong("settings.health-display-refresh-ticks", 40L));
        if (healthDisplayTicks > 0) {
            Bukkit.getScheduler().runTaskTimer(this, this::refreshHealthDisplays, healthDisplayTicks, healthDisplayTicks);
        }
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
    public id.shadowyn.level.hooks.HookManager hooks() { return hooks; }
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

    /**
     * Registers the armor listener only when the server provides Paper's
     * PlayerArmorChangeEvent. On servers without it the class fails to link, so the
     * failure is caught here instead of taking down the other listeners with it.
     */
    private void registerArmorDisplayListener() {
        try {
            Class.forName("com.destroystokyo.paper.event.player.PlayerArmorChangeEvent");
            Bukkit.getPluginManager().registerEvents(new ArmorDisplayListener(this), this);
        } catch (Throwable ignored) {
            // No Paper armor event on this server: fall back to the Bukkit-only paths
            // (inventory clicks, right-click equips, dispensers) so armor swaps still
            // resync the heart bar immediately instead of waiting for the sweep.
            Bukkit.getPluginManager().registerEvents(new LegacyArmorDisplayListener(this), this);
            getLogger().info("Paper armor event unavailable; using Bukkit fallback for the heart-bar refresh.");
        }
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("language", () -> getConfig().getString("settings.language", "EN").toUpperCase(Locale.ROOT)));
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
        return changed;
    }

    public void loadConfigDefaults() {
        boolean changed = false;
        int bundledBalanceVersion = 52;
        if (getConfig().getInt("settings.balance-version", 1) < bundledBalanceVersion) {
            changed |= updateOldDouble("stats.health.health-per-point", 0.11, 0.0588);
            changed |= updateOldDouble("stats.health.flat-effective-cap", 900.0, 980.0);
            changed |= updateOldDouble("stats.defense.effective-cap-percent", 75.0, 40.0);
            changed |= updateOldInt("settings.max-total-stat-points", 100000, 10000);
            changed |= updateOldInt("settings.max-level", 1000, 100);
            changed |= updateOldDouble("stats.archery.bonus-per-point-percent", 0.006, 0.06);
            changed |= updateOldDouble("stats.fighting.bonus-per-point-percent", 0.006, 0.06);
            changed |= updateOldDouble("stats.power.bonus-per-point-percent", 0.006, 0.06);
            changed |= updateOldDouble("stats.defense.bonus-per-point-percent", 0.006, 0.06);
            changed |= updateOldDouble("stats.health.bonus-per-point-percent", 0.006, 0.06);
            changed |= updateOldDouble("stats.alchemy.bonus-per-point-percent", 0.006, 0.06);
            changed |= updateOldDouble("stats.defense.health-per-point", 0.01, 0.1);
            changed |= updateOldDouble("stats.health.health-per-point", 0.0588, 0.588);
        }
        // The old value healed 0.45% of max health per 100% Alchemy, which a maxed build
        // could not feel. Only the untouched default is raised; a customised value stays.
        changed |= updateOldDouble("stats.alchemy.max-health-heal-per-100-percent", 0.45, 4.0);
        if (getConfig().getInt("settings.balance-version", 1) < bundledBalanceVersion) {
            getConfig().set("settings.balance-version", bundledBalanceVersion);
            changed = true;
        }
        if (!getConfig().isSet("actionbar")) {
            getConfig().set("actionbar", true);
            changed = true;
        }
        if (!getConfig().isSet("actionbar-text")) {
            getConfig().set("actionbar-text", "&cHP &f{health}&7/&f{max_health} &8        &aEXP &f{exp}&7/&f{needed_exp}");
            changed = true;
        }
        if (changed) saveConfig();
    }

    private boolean updateOldDouble(String path, double oldValue, double newValue) {
        if (!getConfig().isSet(path) || Math.abs(getConfig().getDouble(path) - oldValue) > 0.0001) return false;
        getConfig().set(path, newValue);
        return true;
    }

    private boolean updateOldInt(String path, int oldValue, int newValue) {
        if (!getConfig().isSet(path) || getConfig().getInt(path) != oldValue) return false;
        getConfig().set(path, newValue);
        return true;
    }

    public void loadLevelConfig() {
        File file = new File(getDataFolder(), "level.yml");
        levelConfig = YamlConfiguration.loadConfiguration(file);
        if (migrateLevelBalance(file)) {
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
        changed |= setMissingLevelDouble("settings.exp-power", 1.5);
        changed |= setMissingLevelDouble("settings.exp-multiplier", 759.2195);

        double currentPower = levelConfig.getDouble("settings.exp-power", 1.5);
        double currentMultiplier = levelConfig.getDouble("settings.exp-multiplier", 759.2195);
        boolean oldDefaultCurve = version < 12
            && Math.abs(levelConfig.getDouble("settings.exp-base", 100.0) - 100.0) < 0.0001
            && Math.abs(currentPower - 1.537) < 0.0001
            && (Math.abs(currentMultiplier - 84.2) < 0.0001 || Math.abs(currentMultiplier - 1.55) < 0.0001);
        if (oldDefaultCurve) {
            levelConfig.set("settings.exp-power", 1.5);
            levelConfig.set("settings.exp-multiplier", 759.2195);
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
        if (version < 12) {
            levelConfig.set("settings.exp-balance-version", 12);
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
        double power = levelConfig.getDouble("settings.exp-power", 1.5);
        double multiplier = levelConfig.getDouble("settings.exp-multiplier", 759.2195);
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
        applyMaxHealth(player, profile);
        refreshHealthDisplay(player);
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

    /**
     * Applies this plugin's share of max health as a named attribute modifier.
     *
     * <p>The previous approach wrote {@code setBaseValue} on every call. That overwrote
     * whatever other plugins had contributed, and — because it ran unconditionally — it
     * rewrote the attribute even when the value had not changed. Each rewrite is a
     * client-visible max-health change, and a client that sees max health move while
     * current health stays put renders the difference as a hit. That is the phantom
     * damage on armor swaps, healing, and eating, and why it fired in creative too.
     *
     * <p>So the value is contributed as an additive modifier under a fixed key, and the
     * modifier is only replaced when the amount actually differs. An unchanged stat costs
     * nothing and sends nothing. The base value is left alone, which also means armor and
     * other plugins keep their own contributions.
     */
    private void applyMaxHealth(Player player, PlayerProfile profile) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;
        // Our share is everything above the vanilla 20; the rest stays the server's own.
        double bonus = Math.max(0.0, physicalMaxHealth(profile) - 20.0);
        double oldMax = attr.getValue();

        AttributeModifier existing = null;
        for (AttributeModifier modifier : attr.getModifiers()) {
            if (isOwnHealthModifier(modifier)) {
                existing = modifier;
                break;
            }
        }
        if (existing != null && Math.abs(existing.getAmount() - bonus) <= HEALTH_MODIFIER_EPSILON) {
            return;
        }
        if (existing != null) attr.removeModifier(existing);
        if (bonus > 0.0) {
            try {
                attr.addModifier(new AttributeModifier(healthModifierKey(), bonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            } catch (IllegalArgumentException | NoSuchMethodError ex) {
                attr.addModifier(new AttributeModifier(HEALTH_MODIFIER_ID, HEALTH_MODIFIER_NAME, bonus, AttributeModifier.Operation.ADD_NUMBER));
            }
        }

        double newMax = attr.getValue();
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        } else if (Math.abs(newMax - oldMax) > HEALTH_MODIFIER_EPSILON && oldMax > 0.0
                && getConfig().getBoolean("settings.health-display-keep-ratio", true)) {
            // Raising max health alone lowers the fraction a scaled bar draws, and the
            // client reads any fall as a hit. Holding the ratio keeps the bar where it was.
            player.setHealth(Math.max(0.5, Math.min(newMax, player.getHealth() / oldMax * newMax)));
        }
    }

    private NamespacedKey healthModifierKey() {
        return new NamespacedKey(this, HEALTH_MODIFIER_NAME);
    }

    /**
     * Recognises our modifier across the 1.21 key migration and the legacy UUID form, so a
     * modifier left by an older build is replaced rather than stacked on top of.
     */
    private boolean isOwnHealthModifier(AttributeModifier modifier) {
        if (HEALTH_MODIFIER_NAME.equals(modifier.getName())) return true;
        try {
            NamespacedKey key = modifier.getKey();
            return key != null
                    && (getName().toLowerCase(java.util.Locale.ROOT).equals(key.getNamespace()) || "minecraft".equals(key.getNamespace()))
                    && (HEALTH_MODIFIER_NAME.equals(key.getKey()) || HEALTH_MODIFIER_ID.toString().equals(key.getKey()));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean healthDisplayScalingEnabled() {
        return getConfig().getBoolean("settings.health-display-scaling", true);
    }

    /**
     * Whether a packet-level heart bar (packetevents or ProtocolLib hook) is live.
     *
     * <p>When it is, Bukkit's own health scaling must stay OFF. Two engines writing the
     * same bar is exactly the fight the phantom hits came from: the fork's scaling code
     * recomputes the bar from values it reads mid-change and sends its own packets, which
     * race the rewritten ones. With scaling off the server only ever emits raw health,
     * and the packet hook is the single place that converts it for the client.
     */
    public boolean packetHeartBarActive() {
        var manager = hooks;
        return manager != null
                && (manager.isRegistered(id.shadowyn.level.hooks.PacketEventsHook.class)
                        || manager.isRegistered(id.shadowyn.level.hooks.ProtocolLibHook.class));
    }

    public double healthDisplayHearts() {
        double hearts = getConfig().getDouble("settings.health-display-hearts", 20.0);
        return Math.max(2.0, Math.min(40.0, hearts));
    }

    /**
     * Locks the heart bar to a fixed number of hearts.
     *
     * <p>The client plays the hurt flash and sound whenever a health packet reports less
     * health than it is already showing — it does not check whether any damage occurred.
     * That is why the effect fires in creative, where the player cannot be hurt at all:
     * the only thing sending those packets is this plugin. Anything that briefly changes
     * the ratio behind {@code health / maxHealth * scale} — armor attribute modifiers
     * going on or off, a resend arriving mid-change — lands as a dip, and the dip reads
     * as a hit.
     *
     * <p>So this sends exactly one packet and no more. {@code setHealthScale} already
     * implies {@code setHealthScaled(true)} and resends the bar by itself; the
     * {@code sendHealthUpdate} that used to run alongside it was a second, independent
     * health packet, and the extra packet was the hit. Nothing here is conditional: a
     * scale the server still considers correct is exactly the case where the client has
     * lost it, so skipping the write is what leaves the raw maximum on screen.
     */
    public void refreshHealthDisplay(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!healthDisplayScalingEnabled() || packetHeartBarActive()) {
            // With a packet hook live, Bukkit scaling must be off — see
            // packetHeartBarActive(). Turning it off also resends raw health once,
            // which the hook immediately rewrites, so the bar never blinks.
            if (player.isHealthScaled()) player.setHealthScaled(false);
            return;
        }
        player.setHealthScale(healthDisplayHearts());
    }

    /**
     * Whether the periodic sweep should resend the bar without asking the server first.
     *
     * <p>{@code isHealthScaled()} and {@code getHealthScale()} read a server-side field
     * that is written once and never dropped, so they cannot see the client losing the
     * bar — which is exactly how Spigot-based forks break it: they resend raw health to
     * the client and the server-side flag stays "correct" forever. On those servers the
     * conditional sweep never fires and the broken bar stays until relog. Paper and its
     * forks (Purpur, Leaf) keep the client in sync themselves, so the cheap conditional
     * sweep is enough there and forcing would only add packets that can land mid-change.
     */
    private boolean healthDisplayForceRefresh() {
        String mode = getConfig().getString("settings.health-display-force-refresh", "auto");
        if (mode == null) mode = "auto";
        if (mode.equalsIgnoreCase("true") || mode.equalsIgnoreCase("always")) return true;
        if (mode.equalsIgnoreCase("false") || mode.equalsIgnoreCase("off")) return false;
        return !isPaperBasedServer();
    }

    /**
     * Detects Paper and its descendants by probing for a Paper-only API method rather
     * than parsing the server brand string, which forks rename freely.
     */
    private boolean isPaperBasedServer() {
        Boolean cached = paperBasedServer;
        if (cached != null) return cached;
        boolean found;
        try {
            Player.class.getMethod("sendHealthUpdate");
            found = true;
        } catch (NoSuchMethodException ex) {
            found = false;
        }
        paperBasedServer = found;
        return found;
    }

    /**
     * Queues a heart-bar refresh for the next tick, at most once per player per tick.
     *
     * <p>Every caller goes through here rather than refreshing inline. Damage, healing,
     * and equipment events all fire before the server has applied their result, so an
     * inline resync would broadcast the pre-event health — which the client then
     * reconciles against the real value as a second, phantom hit. Waiting one tick means
     * the numbers being sent are the ones the player actually has.
     */
    public void scheduleHealthDisplayRefresh(Player player) {
        if (player == null || !healthDisplayScalingEnabled()) return;
        java.util.UUID uuid = player.getUniqueId();
        if (!pendingHealthDisplay.add(uuid)) return;
        Bukkit.getScheduler().runTask(this, () -> {
            pendingHealthDisplay.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.isOnline() && !online.isDead()) refreshHealthDisplay(online);
        });
    }

    /**
     * Re-applies the heart bar after an equipment change.
     *
     * <p>This deliberately refreshes once, on the next tick, rather than repeatedly across
     * the following ticks. Each refresh is a packet, and a packet that reports less health
     * than the client is drawing produces the hurt flash on its own — so sweeping a whole
     * window of ticks turned one badly-timed packet into ten chances to fire the effect.
     * Armor is not special enough to need more than the single correction every other
     * caller gets.
     */
    public void scheduleHealthDisplayRefreshAfterEquipment(Player player) {
        if (player == null || !healthDisplayScalingEnabled()) {
            scheduleHealthDisplayRefresh(player);
            return;
        }
        double maxBefore = maxHealthValue(player);
        double healthBefore = player.getHealth();
        java.util.UUID uuid = player.getUniqueId();
        if (!pendingHealthDisplay.add(uuid)) return;
        Bukkit.getScheduler().runTask(this, () -> {
            pendingHealthDisplay.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online == null || !online.isOnline() || online.isDead()) return;
            keepHealthRatio(online, healthBefore, maxBefore);
            refreshHealthDisplay(online);
        });
        // Spigot-based forks apply the armor's attribute modifiers and resend raw
        // health on their own schedule, which can land after the tick-one resync and
        // wipe it — that is the visible flip between the raw and the fixed bar. One
        // late follow-up out-waits that resend. Paper applies attributes in-tick, so
        // there the extra packet would only be another chance to flash the hurt effect.
        if (!isPaperBasedServer()) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null && online.isOnline() && !online.isDead()) refreshHealthDisplay(online);
            }, 3L);
        }
    }

    /**
     * Keeps the heart bar still when a piece of armor changes max health.
     *
     * <p>A scaled bar draws {@code health / maxHealth * scale}. Armor that carries a max
     * health modifier raises the denominator and leaves the numerator alone, so the number
     * the client is told to draw falls even though nothing hurt the player — and a falling
     * health value is the one and only thing the client needs to play the hurt flash. No
     * damage is involved, which is why it fired in creative.
     *
     * <p>Holding the ratio steady keeps the drawn value identical across the swap, so there
     * is no fall to react to. Health is only touched when max health actually moved and
     * nothing else changed health in the same tick; real damage landing at the same moment
     * is left alone to resolve normally.
     */
    private void keepHealthRatio(Player player, double healthBefore, double maxBefore) {
        if (!getConfig().getBoolean("settings.health-display-keep-ratio", true)) return;
        if (maxBefore <= 0.0) return;
        double maxAfter = maxHealthValue(player);
        if (Math.abs(maxAfter - maxBefore) <= HEALTH_MODIFIER_EPSILON) return;
        if (Math.abs(player.getHealth() - healthBefore) > 0.01) return;
        double target = Math.max(0.5, Math.min(maxAfter, healthBefore / maxBefore * maxAfter));
        if (Math.abs(target - player.getHealth()) <= 0.01) return;
        player.setHealth(target);
    }

    private double maxHealthValue(Player player) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr == null ? 0.0 : attr.getValue();
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
        return Math.max(1, ((20.0 + levels.healthFlat(profile)) * (1.0 + clanPercent / 100.0)) + levels.defenseHealthFlat(profile));
    }

    public double effectiveMaxHealth(PlayerProfile profile) {
        double cap = getConfig().getDouble("stats.health.absolute-effective-cap", 10000.0)
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
        double damagePer100 = getConfig().getDouble("stats." + attackType.configKey() + ".raw-damage-per-100-percent",
                getConfig().getDouble("stats.damage.raw-damage-per-100-percent", 20.0));
        return Math.max(0.0, statPercent * damagePer100 / 100.0);
    }

    private StatType attackStat(Player player, boolean projectile) {
        if (projectile) return StatType.ARCHERY;
        ItemStack hand = player.getInventory().getItemInMainHand();
        Material material = hand == null ? Material.AIR : hand.getType();
        StatType custom = customWeaponStat(hand);
        if (custom != null) return custom;
        return isMeleeWeapon(material) ? StatType.FIGHTING : StatType.POWER;
    }

    /**
     * Classifies an MMOItems weapon by the type its author gave it rather than by material.
     *
     * <p>Without this, a custom staff or dagger built on a stick reads as an empty hand and
     * scores as an unarmed Power hit. Asking MMOItems what kind of weapon it is keeps the
     * player's stat choice meaningful. Returns {@code null} for anything MMOItems does not
     * claim, which leaves the vanilla material check in charge.
     */
    private StatType customWeaponStat(ItemStack hand) {
        if (hooks == null || hand == null) return null;
        var mmoItems = hooks.get(id.shadowyn.level.hooks.MmoItemsHook.class);
        if (mmoItems == null) return null;
        String type = mmoItems.itemType(hand);
        if (type == null) return null;
        String mapped = getConfig().getString("hooks.MMOItems.weapon-stats." + type.toUpperCase(Locale.ROOT));
        if (mapped == null) return null;
        try {
            return StatType.valueOf(mapped.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            getLogger().warning("Unknown stat '" + mapped + "' in hooks.MMOItems.weapon-stats." + type + ".");
            return null;
        }
    }

    private boolean isMeleeWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || name.equals("MACE")
                || name.equals("TRIDENT")
                || name.equals("SHEARS");
    }

    public boolean actionbarEnabled() {
        return getConfig().getBoolean("actionbar", true);
    }

    /**
     * Periodic safety net. On Paper-based servers it skips anyone whose bar the server
     * already reports as locked — re-sending a correct scale is a packet that can land
     * mid-change and flash the hurt effect. On Spigot-based forks that server-side
     * report is meaningless (see {@link #healthDisplayForceRefresh()}), so the sweep
     * resends unconditionally there; a lost bar the server cannot see is worse than
     * the occasional redundant packet.
     */
    private void refreshHealthDisplays() {
        if (!healthDisplayScalingEnabled()) return;
        boolean force = healthDisplayForceRefresh();
        double hearts = healthDisplayHearts();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead()) continue;
            if (!force && player.isHealthScaled() && Math.abs(player.getHealthScale() - hearts) < 0.001) continue;
            refreshHealthDisplay(player);
        }
    }

    private void sendPersistentActionBars() {
        if (!actionbarEnabled()) return;
        long now = System.currentTimeMillis();
        // Also drops holds belonging to players who logged out mid-notice.
        actionbarHold.values().removeIf(until -> now >= until);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!data.get(player).skillActionbar()) continue;
            if (actionbarHold.containsKey(player.getUniqueId())) continue;
            player.sendActionBar(Text.c(actionbarText(player)));
        }
    }

    /**
     * Sends a one-off actionbar message and keeps the persistent HP/EXP bar off it
     * until it has been read.
     *
     * <p>The persistent bar is rewritten every second, which is short enough to wipe a
     * notification before the player notices it. Anything worth telling the player —
     * an Alchemy heal, a race skill firing — goes through here so it holds the slot for
     * {@code settings.actionbar-notice-ms} instead of losing a race against the sweep.
     */
    public void sendNoticeActionBar(Player player, String message) {
        if (player == null || !player.isOnline()) return;
        long hold = Math.max(0L, getConfig().getLong("settings.actionbar-notice-ms", 2500L));
        if (hold > 0) actionbarHold.put(player.getUniqueId(), System.currentTimeMillis() + hold);
        player.sendActionBar(Text.c(message));
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

