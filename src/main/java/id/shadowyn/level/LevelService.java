package id.shadowyn.level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.title.Title;
import java.time.Duration;

public final class LevelService {
    private final ShadowynLevelPlugin plugin;
    private final Set<UUID> activeRankAutoRerolls = ConcurrentHashMap.newKeySet();

    public record AutoRankRerollResult(StatRank rank, Rarity rarity, int attempts, int fragmentsUsed, boolean reachedTarget) {
    }

    public record PointScanResult(String player, int level, int total, int minimum, int maximum, int available, int used, String issue, boolean fixed) {
    }

    public LevelService(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
    }

    public long neededExp(int level) {
        int max = plugin.maxLevel();
        if (level >= max) return Long.MAX_VALUE;
        return plugin.hardExpNeeded(level);
    }

    public long totalExp(PlayerProfile profile) {
        long total = profile.exp();
        for (int level = 1; level < profile.level(); level++) {
            long needed = neededExp(level);
            if (needed == Long.MAX_VALUE) break;
            total += needed;
        }
        return total;
    }

    public long totalExpToMaxLevel() {
        long total = 0L;
        for (int level = 1; level < plugin.maxLevel(); level++) {
            long needed = neededExp(level);
            if (needed == Long.MAX_VALUE) break;
            long add = Math.max(0L, needed);
            if (Long.MAX_VALUE - total <= add) return Long.MAX_VALUE;
            total += add;
        }
        return Math.max(1L, total);
    }

    public int totalProgressPercent(PlayerProfile profile) {
        if (profile.level() >= plugin.maxLevel()) return 100;
        return (int) Math.max(0, Math.min(100, Math.floor(totalExp(profile) * 100.0 / totalExpToMaxLevel())));
    }

    public void addExp(Player player, long amount) {
        addExp(player, amount, true);
    }

    public void addExp(Player player, long amount, boolean applyMultiplier) {
        if (amount <= 0) return;
        if (applyMultiplier) amount = Math.max(1L, Math.round(amount * expMultiplier(player)));
        PlayerProfile profile = plugin.data().get(player);
        int max = plugin.maxLevel();
        if (profile.level() >= max) return;
        profile.exp(profile.exp() + amount);
        while (profile.level() < max && profile.exp() >= neededExp(profile.level())) {
            profile.exp(profile.exp() - neededExp(profile.level()));
            profile.level(profile.level() + 1);
            grantLevelStatsPoint(player, profile);
            reward(player, profile.level());
            plugin.applyStats(player);
            levelUpEffects(player, profile.level());
            player.sendMessage(Text.s(plugin.msg("level-up").replace("%level%", String.valueOf(profile.level()))));
        }
        if (profile.skillActionbar() && plugin.getConfig().getBoolean("settings.actionbar-progress", true) && !plugin.actionbarEnabled()) {
            plugin.sendNoticeActionBar(player, plugin.msg("exp-gain")
                    .replace("%exp%", String.valueOf(amount))
                    .replace("%current%", String.valueOf(profile.exp()))
                    .replace("%needed%", String.valueOf(neededExp(profile.level()))));
        }
    }

    public int giveLevels(Player player, int levels) {
        if (levels <= 0) return 0;
        PlayerProfile profile = plugin.data().get(player);
        int startLevel = profile.level();
        int targetLevel = Math.min(plugin.maxLevel(), startLevel + levels);
        if (targetLevel <= startLevel) return 0;

        long expToAdd = 0L;
        long currentExp = profile.exp();
        for (int level = startLevel; level < targetLevel; level++) {
            long needed = neededExp(level);
            if (needed == Long.MAX_VALUE) break;
            expToAdd = Math.addExact(expToAdd, Math.max(0L, needed - currentExp));
            currentExp = 0L;
        }
        addExp(player, expToAdd, false);
        return plugin.data().get(player).level() - startLevel;
    }

    private void grantLevelStatsPoint(Player player, PlayerProfile profile) {
        int budget = pointBudgetForLevel(profile.level());
        if (totalStats(profile) > budget) {
            setTotalStats(profile, budget);
        }
        int grant = Math.max(0, budget - profile.levelRewardPoints());
        int allowed = Math.max(0, Math.min(grant, budget - profile.levelRewardPoints()));
        if (allowed > 0) {
            profile.statsPoint(profile.statsPoint() + allowed);
            profile.levelRewardPoints(profile.levelRewardPoints() + allowed);
        }
        if (totalStats(profile) > budget) {
            setTotalStats(profile, budget);
        }
    }

    public int levelPointReward() {
        return Math.max(1, pointBudgetForLevel(1));
    }

    public int levelPointReward(Player player) {
        return levelPointReward();
    }

    public int levelPointReward(int level) {
        if (level <= 0) return 0;
        return Math.max(0, pointBudgetForLevel(level) - pointBudgetForLevel(level - 1));
    }

    public int totalStats(PlayerProfile profile) {
        return Math.max(0, profile.statsPoint() + usedStats(profile));
    }

    public int pointBudgetForLevel(int level) {
        int cap = plugin.getConfig().getInt("settings.max-total-stat-points", 5000);
        int completedLevels = Math.max(0, Math.min(plugin.maxLevel(), level));
        double progress = completedLevels / (double) Math.max(1, plugin.maxLevel());
        double linearWeight = plugin.getConfig().getDouble("settings.stat-point-curve-linear-weight", 0.5);
        double curved = linearWeight * progress + (1.0 - linearWeight) * progress * progress;
        return Math.min(cap, (int) Math.round(cap * curved));
    }

    public int pointBudgetFor(Player player, PlayerProfile profile) {
        return pointBudgetForLevel(profile.level());
    }

    public void syncLevelStats(PlayerProfile profile) {
        int budget = pointBudgetForLevel(profile.level());
        setTotalStats(profile, budget);
        profile.levelRewardPoints(budget);
    }

    public List<PointScanResult> scanUnusualPoints(boolean fix) {
        List<PointScanResult> results = new ArrayList<>();
        for (PlayerProfile profile : plugin.data().allProfiles()) {
            Player online = Bukkit.getPlayer(profile.uuid());
            int minimum = pointBudgetForLevel(profile.level());
            int maximum = minimum;
            int total = totalStats(profile);
            String issue;
            int target;
            if (total > maximum) {
                issue = "OVER";
                target = maximum;
            } else if (total < minimum) {
                issue = "UNDER";
                target = minimum;
            } else {
                continue;
            }
            int available = profile.statsPoint();
            int used = usedStats(profile);
            if (fix) {
                setTotalStats(profile, target);
                if (online != null) plugin.applyStats(online);
            }
            results.add(new PointScanResult(profile.name(), profile.level(), total, minimum, maximum, available, used, issue, fix));
        }
        if (fix && !results.isEmpty()) plugin.data().save();
        return results;
    }

    private void reward(Player player, int level) {
        giveConfiguredReward(player, "every-level.", level);
        giveConfiguredReward(player, "levels." + level + ".rewards.", level);
        giveMilestoneRewards(player, level);
    }

    private void giveConfiguredReward(Player player, String path, int level) {
        runCommands(player, plugin.levelConfig().getStringList(path + "commands"));
        giveItems(player, plugin.levelConfig().getStringList(path + "items"));
        int money = plugin.levelConfig().getInt(path + "money", 0);
        if (money > 0) giveMoney(player, money);
        giveRewardFragments(player, path, level);
    }

    private void giveRewardFragments(Player player, String path, int level) {
        if (plugin.levelConfig().getBoolean(path + "random-fragment.enabled", false)) {
            for (var entry : randomFragmentPlan(path, level).entrySet()) {
                if (entry.getValue() > 0) player.getInventory().addItem(plugin.fragments().create(entry.getKey(), entry.getValue()));
            }
            return;
        }
        ConfigurationSection fragments = plugin.levelConfig().getConfigurationSection(path + "fragments");
        if (fragments != null) {
            for (String id : fragments.getKeys(false)) {
                int amount = clampFragmentAmount(fragments.getInt(id, 0));
                if (amount > 0) player.getInventory().addItem(plugin.fragments().create(id, amount));
            }
            return;
        }
        int clanFragments = clampFragmentAmount(plugin.levelConfig().getInt(path + "clan-fragments", plugin.levelConfig().getInt(path + "fragments", 0)));
        if (clanFragments > 0) player.getInventory().addItem(plugin.fragments().create(FragmentService.RACES, clanFragments));
        int rankFragments = clampFragmentAmount(plugin.levelConfig().getInt(path + "rank-fragments", 0));
        if (rankFragments > 0) player.getInventory().addItem(plugin.fragments().create(FragmentService.STATS_CLASS, rankFragments));
    }

    private void giveMilestoneRewards(Player player, int level) {
        for (String key : matchingMilestones(level)) {
            String path = "milestone-rewards." + key + ".";
            runCommands(player, plugin.levelConfig().getStringList(path + "commands"));
            int money = plugin.levelConfig().getInt(path + "money", 0);
            if (money > 0) giveMoney(player, money);
            giveItems(player, plugin.levelConfig().getStringList(path + "items"));
        }
    }

    public double expMultiplier(Player player) {
        int best = 1;
        for (int i = 2; i <= 20; i++) {
            if (hasExplicitRewardPermission(player, "orbitskills.multiplier." + i)
                    || hasExplicitRewardPermission(player, "shadowynlevel." + i)) best = i;
        }
        return 1.0 + ((best - 1) * 0.5);
    }

    private boolean hasExplicitRewardPermission(Player player, String permission) {
        return player.getEffectivePermissions().stream()
                .anyMatch(info -> info.getValue() && info.getPermission().equalsIgnoreCase(permission));
    }

    public String rewardDescription(int level) {
        String description = plugin.levelConfig().getString("levels." + level + ".description", "");
        if (!description.isBlank()) return description;
        String path = "levels." + level + ".rewards.";
        List<String> items = plugin.levelConfig().getStringList(path + "items");
        if (items.isEmpty()) items = milestoneItems(level);
        int clanFragments = fragmentRewardAmount(path, FragmentService.RACES,
                plugin.levelConfig().getInt(path + "clan-fragments", plugin.levelConfig().getInt(path + "fragments", 0)));
        int rankFragments = fragmentRewardAmount(path, FragmentService.STATS_CLASS,
                plugin.levelConfig().getInt(path + "rank-fragments", 0));
        int money = plugin.levelConfig().getInt(path + "money", 0) + milestoneMoney(level);
        if (!items.isEmpty()) {
            String[] split = items.get(0).split(":");
            return prettyMaterial(split[0]) + " " + (split.length > 1 ? split[1] : "1") + "x";
        }
        List<String> commands = new ArrayList<>(plugin.levelConfig().getStringList(path + "commands"));
        commands.addAll(milestoneCommands(level));
        List<String> descriptions = plugin.levelConfig().getStringList("levels." + level + ".reward-description");
        if (!descriptions.isEmpty()) return Text.s(descriptions.get(0));
        if (!commands.isEmpty()) return "Custom commands";
        if (clanFragments > 0) return "Race Reroll Fragment " + clanFragments + "x";
        if (rankFragments > 0) return "Stats Class Fragment " + rankFragments + "x";
        if (money > 0) return "Money " + money;
        return "SP";
    }

    public List<String> rewardLore(int level) {
        List<String> configuredLore = plugin.levelConfig().getStringList("levels." + level + ".lore");
        if (!configuredLore.isEmpty()) {
            return configuredLore.stream()
                    .map(line -> line.replace("%orbitskills_stats_perlevel%", String.valueOf(levelPointReward(level))))
                    .toList();
        }
        String path = "levels." + level + ".rewards.";
        List<String> description = plugin.levelConfig().getStringList("levels." + level + ".reward-description");
        if (!description.isEmpty()) return description.stream()
                .map(line -> rewardPlaceholders(line, level, 1.0))
                .toList();
        List<String> template = plugin.levelConfig().getStringList("settings.default-reward-lore");
        if (!template.isEmpty()) {
            List<String> lore = new ArrayList<>();
            List<String> items = plugin.levelConfig().getStringList("levels." + level + ".rewards.items");
            if (items.isEmpty()) items = milestoneItems(level);
            for (String line : template) {
                if (!line.contains("{items}")) {
                    lore.add(rewardPlaceholders(line, level, 1.0));
                    continue;
                }
                String prefix = line.substring(0, line.indexOf("{items}"));
                if (items.isEmpty()) {
                    lore.add(rewardPlaceholders(prefix + "None", level, 1.0));
                } else {
                    boolean first = true;
                    for (String item : items) {
                        lore.add(rewardPlaceholders((first ? prefix : "&#6D7890  › &#F7F3FF") + itemSummary(List.of(item)), level, 1.0));
                        first = false;
                    }
                }
            }
            return lore;
        }
        int money = plugin.levelConfig().getInt("every-level.money", 0) + plugin.levelConfig().getInt(path + "money", 0) + milestoneMoney(level);
        int clanFragments = fragmentRewardAmount("every-level.", FragmentService.RACES,
                plugin.levelConfig().getInt("every-level.clan-fragments", plugin.levelConfig().getInt("every-level.fragments", 0)))
                + fragmentRewardAmount(path, FragmentService.RACES,
                plugin.levelConfig().getInt(path + "clan-fragments", plugin.levelConfig().getInt(path + "fragments", 0)));
        int rankFragments = fragmentRewardAmount("every-level.", FragmentService.STATS_CLASS,
                plugin.levelConfig().getInt("every-level.rank-fragments", 0))
                + fragmentRewardAmount(path, FragmentService.STATS_CLASS,
                plugin.levelConfig().getInt(path + "rank-fragments", 0));
        List<String> items = plugin.levelConfig().getStringList(path + "items");
        if (items.isEmpty()) items = milestoneItems(level);
        List<String> lore = new ArrayList<>(List.of(
                plugin.langLine(),
                "&#FFB84DReward",
                "&8- &7Money: &f" + money,
                "&8- &7Race Frag: &#C77DFF" + clanFragments + "x",
                "&8- &7Class Frag: &#5CC8FF" + rankFragments + "x",
            "&8- &7SP: &#63F29B" + levelPointReward(level),
            "&8- &7Items:"
        ));
        if (items.isEmpty()) lore.add("&8  › &fNone");
        else for (String item : items) lore.add("&8  › &f" + itemSummary(List.of(item)));
        return lore;
    }

    private String rewardPlaceholders(String line, int level, double multiplier) {
        String path = "levels." + level + ".rewards.";
        int money = plugin.levelConfig().getInt("every-level.money", 0) + plugin.levelConfig().getInt(path + "money", 0) + milestoneMoney(level);
        java.util.Map<String, Integer> randomFragments = randomFragmentPlan("every-level.", level);
        int raceFragments = randomFragments.getOrDefault(FragmentService.RACES, 0) + fragmentRewardAmount("every-level.", FragmentService.RACES,
                plugin.levelConfig().getInt("every-level.clan-fragments", plugin.levelConfig().getInt("every-level.fragments", 0)))
                + fragmentRewardAmount(path, FragmentService.RACES,
                plugin.levelConfig().getInt(path + "clan-fragments", plugin.levelConfig().getInt(path + "fragments", 0)));
        int classFragments = randomFragments.getOrDefault(FragmentService.STATS_CLASS, 0) + fragmentRewardAmount("every-level.", FragmentService.STATS_CLASS,
                plugin.levelConfig().getInt("every-level.rank-fragments", 0))
                + fragmentRewardAmount(path, FragmentService.STATS_CLASS,
                plugin.levelConfig().getInt(path + "rank-fragments", 0));
        int clanFragments = randomFragments.getOrDefault(FragmentService.CLANS, 0) + fragmentRewardAmount("every-level.", FragmentService.CLANS, 0)
                + fragmentRewardAmount(path, FragmentService.CLANS, 0);
        List<String> items = plugin.levelConfig().getStringList(path + "items");
        if (items.isEmpty()) items = milestoneItems(level);
        int fragmentMin = plugin.levelConfig().getInt("every-level.random-fragment.min", 3);
        int fragmentMax = plugin.levelConfig().getInt("every-level.random-fragment.max", 25);
        List<String> fragmentTypes = plugin.levelConfig().getStringList("every-level.random-fragment.types");
        return line
                .replace("{line}", plugin.langLine())
                .replace("{level}", String.valueOf(level))
                .replace("{exp_needed}", String.valueOf(neededExp(level)))
                .replace("{money}", String.valueOf(money))
                .replace("{race_fragments}", String.valueOf(raceFragments))
                .replace("{races_fragments}", String.valueOf(raceFragments))
                .replace("{clan_fragments}", String.valueOf(clanFragments))
                .replace("{clans_fragments}", String.valueOf(clanFragments))
                .replace("{class_fragments}", String.valueOf(classFragments))
                .replace("{rank_fragments}", String.valueOf(classFragments))
                .replace("{stats_class_fragments}", String.valueOf(classFragments))
                .replace("{fragment_min}", String.valueOf(fragmentMin))
                .replace("{fragment_max}", String.valueOf(fragmentMax))
                .replace("{fragment_types}", fragmentTypes.isEmpty() ? "Races | Class | Clans" : String.join(" | ", fragmentTypes))
                .replace("{stats_points}", String.valueOf(levelPointReward(level)))
                .replace("{items}", items.isEmpty() ? "None" : itemSummary(items))
                .replace("%orbitskills_stats_perlevel%", String.valueOf(levelPointReward(level)));
    }

    private int fragmentRewardAmount(String path, String fragment, int fallback) {
        if (plugin.levelConfig().isConfigurationSection(path + "fragments")) {
            return clampFragmentAmount(plugin.levelConfig().getInt(path + "fragments." + fragment, fallback));
        }
        return clampFragmentAmount(fallback);
    }

    private int clampFragmentAmount(int amount) {
        if (amount <= 0) return 0;
        int min = Math.max(3, plugin.levelConfig().getInt("every-level.random-fragment.min", 3));
        return Math.max(min, amount);
    }

    private java.util.Map<String, Integer> randomFragmentPlan(String path, int level) {
        java.util.Map<String, Integer> plan = new java.util.LinkedHashMap<>();
        if (!plugin.levelConfig().getBoolean(path + "random-fragment.enabled", false)) return plan;
        List<String> types = plugin.levelConfig().getStringList(path + "random-fragment.types");
        if (types.isEmpty()) types = List.of(FragmentService.RACES, FragmentService.STATS_CLASS, FragmentService.CLANS);
        int min = Math.max(3, plugin.levelConfig().getInt(path + "random-fragment.min", 3));
        int max = Math.max(min, plugin.levelConfig().getInt(path + "random-fragment.max", 25));
        if (plugin.levelConfig().getBoolean(path + "random-fragment.scale-with-level", true)) {
            int maxAtLevel = Math.max(1, plugin.levelConfig().getInt(path + "random-fragment.max-at-level", Math.min(100, plugin.maxLevel())));
            double progress = Math.max(0.0, Math.min(1.0, level / (double) maxAtLevel));
            max = Math.max(min, min + (int) Math.round((max - min) * progress));
        }
        java.util.Random random = new java.util.Random((level * 1103515245L) + 12345L);
        for (String type : types) {
            String id = plugin.fragments().resolveId(type);
            int amount = min + random.nextInt((max - min) + 1);
            plan.merge(id, amount, (a, b) -> a + b);
        }
        return plan;
    }

    private List<String> milestoneItems(int level) {
        List<String> items = new ArrayList<>();
        for (String key : matchingMilestones(level)) {
            items.addAll(plugin.levelConfig().getStringList("milestone-rewards." + key + ".items"));
        }
        return items;
    }

    private List<String> milestoneCommands(int level) {
        List<String> commands = new ArrayList<>();
        for (String key : matchingMilestones(level)) {
            commands.addAll(plugin.levelConfig().getStringList("milestone-rewards." + key + ".commands"));
        }
        return commands;
    }

    private int milestoneMoney(int level) {
        int money = 0;
        for (String key : matchingMilestones(level)) {
            money += Math.max(0, plugin.levelConfig().getInt("milestone-rewards." + key + ".money", 0));
        }
        return money;
    }

    private List<String> matchingMilestones(int level) {
        List<String> keys = new ArrayList<>();
        ConfigurationSection section = plugin.levelConfig().getConfigurationSection("milestone-rewards");
        if (section == null) return keys;
        for (String key : section.getKeys(false)) {
            if (!milestoneMatches(key, level)) continue;
            keys.add(key);
        }
        return keys;
    }

    private boolean milestoneMatches(String key, int level) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("every-")) {
            try {
                int interval = Integer.parseInt(normalized.substring("every-".length()));
                return interval > 0 && level % interval == 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        try {
            return Integer.parseInt(normalized) == level;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void runCommands(Player player, List<String> commands) {
        for (String raw : commands) {
            String command = cleanCommand(raw
                    .replace("%player%", player.getName())
                    .replace("{player}", player.getName())
                    .replace("%level%", String.valueOf(plugin.data().get(player).level()))
                    .replace("{level}", String.valueOf(plugin.data().get(player).level()))
                    .replace("%stats_per_level%", String.valueOf(levelPointReward(player)))
                    .replace("{stats_points}", String.valueOf(levelPointReward(player)))
                    .replace("{exp_multiplier}", String.format(Locale.US, "%.2f", expMultiplier(player)))
                    .replace("%random_eco_10_1000%", String.valueOf(ThreadLocalRandom.current().nextInt(10, 1001))));
            if (!command.isBlank()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private void giveMoney(Player player, int amount) {
        if (depositVaultMoney(player, amount)) return;
        String command = cleanCommand(plugin.levelConfig().getString("settings.money-command", "eco give %player% %amount%")
                .replace("%player%", player.getName())
                .replace("%amount%", String.valueOf(amount)));
        if (!command.isBlank()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private String cleanCommand(String command) {
        String cleaned = command == null ? "" : command.trim();
        if (cleaned.startsWith("\\\"") && cleaned.endsWith("\\\"") && cleaned.length() >= 4) {
            cleaned = cleaned.substring(2, cleaned.length() - 2).trim();
        }
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        if (cleaned.startsWith("/")) cleaned = cleaned.substring(1).trim();
        return cleaned;
    }

    private boolean depositVaultMoney(Player player, int amount) {
        Object economy = vaultEconomy();
        if (economy == null) return false;
        try {
            Method deposit = economy.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class);
            Object response = deposit.invoke(economy, player, (double) amount);
            return transactionSuccess(response);
        } catch (NoSuchMethodException ignored) {
            return depositVaultMoneyByName(economy, player, amount);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to give money through Vault: " + e.getMessage());
            return false;
        }
    }

    private boolean depositVaultMoneyByName(Object economy, Player player, int amount) {
        try {
            Method deposit = economy.getClass().getMethod("depositPlayer", String.class, double.class);
            Object response = deposit.invoke(economy, player.getName(), (double) amount);
            return transactionSuccess(response);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to give money through Vault: " + e.getMessage());
            return false;
        }
    }

    private Object vaultEconomy() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return null;
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            var registration = Bukkit.getServicesManager().getRegistration(economyClass);
            return registration == null ? null : registration.getProvider();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private boolean transactionSuccess(Object response) {
        if (response == null) return true;
        try {
            Method success = response.getClass().getMethod("transactionSuccess");
            Object result = success.invoke(response);
            return result instanceof Boolean value && value;
        } catch (Exception e) {
            return true;
        }
    }

    private void giveItems(Player player, List<String> items) {
        for (String raw : items) {
            String[] split = raw.split(":");
            Material material = Material.matchMaterial(split[0].toUpperCase(Locale.ROOT));
            if (material == null) continue;
            int amount = split.length > 1 ? Math.max(1, parseInt(split[1], 1)) : 1;
            player.getInventory().addItem(new ItemStack(material, amount));
        }
    }

    public boolean addStat(Player player, StatType type) {
        return addStat(player, type, 1);
    }

    public boolean addStat(Player player, StatType type, int amount) {
        PlayerProfile profile = plugin.data().get(player);
        int allowed = Math.min(amount, profile.statsPoint());
        if (allowed <= 0) {
            player.sendMessage(plugin.msg("not-enough-points"));
            return false;
        }
        profile.statsPoint(profile.statsPoint() - allowed);
        profile.stat(type, profile.stat(type) + allowed);
        plugin.applyStats(player);
        plugin.data().save();
        player.sendMessage(plugin.msg("stat-added")
            .replace("%stat%", statName(type))
            .replace("%amount%", String.valueOf(allowed)));
        return true;
    }

    public boolean removeStat(Player player, StatType type, int amount) {
        PlayerProfile profile = plugin.data().get(player);
        int removed = Math.min(amount, profile.stat(type));
        if (removed <= 0) return false;
        profile.stat(type, profile.stat(type) - removed);
        profile.statsPoint(profile.statsPoint() + removed);
        plugin.applyStats(player);
        plugin.data().save();
        player.sendMessage(plugin.msg("stat-refunded")
            .replace("%stat%", statName(type))
            .replace("%amount%", String.valueOf(removed)));
        return true;
    }

    public int usedStats(PlayerProfile profile) {
        int total = 0;
        for (int val : profile.stats().values()) total += val;
        return total;
    }

    public void setTotalStats(Player player, int total) {
        PlayerProfile profile = plugin.data().get(player);
        setTotalStats(profile, total);
        plugin.applyStats(player);
        plugin.data().save();
    }

    public void setTotalStats(PlayerProfile profile, int total) {
        int targetTotal = Math.max(0, total);
        profile.levelRewardPoints(Math.min(profile.levelRewardPoints(), targetTotal));
        int used = usedStats(profile);
        if (used <= targetTotal) {
            profile.statsPoint(targetTotal - used);
            return;
        }
        if (used <= 0) {
            profile.statsPoint(targetTotal);
            return;
        }

        StatType[] types = StatType.values();
        int[] scaled = new int[types.length];
        double[] remainders = new double[types.length];
        int assigned = 0;
        for (int i = 0; i < types.length; i++) {
            double exact = profile.stat(types[i]) * targetTotal / (double) used;
            scaled[i] = (int) Math.floor(exact);
            remainders[i] = exact - scaled[i];
            assigned += scaled[i];
        }

        int missing = targetTotal - assigned;
        while (missing > 0) {
            int best = 0;
            for (int i = 1; i < remainders.length; i++) {
                if (remainders[i] > remainders[best]) best = i;
            }
            scaled[best]++;
            remainders[best] = -1;
            missing--;
        }

        for (int i = 0; i < types.length; i++) {
            profile.stat(types[i], scaled[i]);
        }
        profile.statsPoint(0);
    }

    public double statBonusPercent(PlayerProfile profile, StatType type) {
        if (!plugin.statsEnabled()) return 0.0;
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("stats." + type.configKey());
        if (sec == null) return 0;
        double perPoint = sec.getDouble("bonus-per-point-percent", 0);
        double ranked = profile.stat(type) * perPoint * plugin.classMultiplier(profile, type);
        double softCap = sec.getDouble("soft-cap-percent", sec.getDouble("max-bonus-percent", 25));
        double overflow = sec.getDouble("overflow-efficiency-percent", 0.0);
        double effective = plugin.softCap(ranked, softCap, overflow);
        return Math.min(sec.getDouble("max-bonus-percent", 25), effective);
    }

    public double statBonusPerPointPercent(PlayerProfile profile, StatType type) {
        if (!plugin.statsEnabled()) return 0.0;
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("stats." + type.configKey());
        if (sec == null) return 0;
        return sec.getDouble("bonus-per-point-percent", 0) * plugin.classMultiplier(profile, type);
    }

    public double healthFlat(PlayerProfile profile) {
        if (!plugin.statsEnabled()) return 0.0;
        double raw = plugin.getConfig().getDouble("stats.health.health-per-point", 0.02)
                * profile.stat(StatType.HEALTH)
                * plugin.classMultiplier(profile, StatType.HEALTH);
        double cap = plugin.getConfig().getDouble("stats.health.flat-effective-cap", 120.0)
                * plugin.classMultiplier(profile, StatType.HEALTH);
        double overflowEfficiency = plugin.getConfig().getDouble("stats.health.flat-overflow-efficiency-percent", 10.0);
        return plugin.softCap(raw, cap, overflowEfficiency);
    }

    public double defenseHealthFlat(PlayerProfile profile) {
        if (!plugin.statsEnabled()) return 0.0;
        double raw = plugin.getConfig().getDouble("stats.defense.health-per-point", 0.0)
                * profile.stat(StatType.DEFENSE)
                * plugin.classMultiplier(profile, StatType.DEFENSE);
        double cap = plugin.getConfig().getDouble("stats.defense.health-flat-effective-cap", 500.0)
                * plugin.classMultiplier(profile, StatType.DEFENSE);
        double overflowEfficiency = plugin.getConfig().getDouble("stats.defense.health-flat-overflow-efficiency-percent", 10.0);
        return plugin.softCap(raw, cap, overflowEfficiency);
    }

    public double potionHealBonusPercent(PlayerProfile profile) {
        return statBonusPercent(profile, StatType.ALCHEMY)
                + (plugin.racesEnabled() ? plugin.clans().value(profile.race(), "alchemy-percent") : 0.0)
                + (plugin.clansEnabled() ? plugin.clans().guildValue(profile.guildClan(), "alchemy-percent") : 0.0);
    }

    public String statName(StatType type) {
        return plugin.lang("stats.names." + type.configKey(), plugin.getConfig().getString("stat-display." + type.configKey(), defaultStatName(type)));
    }

    public String statDescription(StatType type) {
        String configured = plugin.lang("stats.descriptions." + type.configKey(), plugin.getConfig().getString("stat-descriptions." + type.configKey(), ""));
        if (configured != null && !configured.isBlank()) return configured;
        return switch (type) {
            case ARCHERY -> "Meningkatkan damage bow dan projectile.";
            case FIGHTING -> "Meningkatkan damage melee; smash Mace juga mendapat scaling khusus.";
            case POWER -> "Meningkatkan damage tangan kosong.";
            case DEFENSE -> "Mengurangi damage masuk dan menambah sedikit Guard HP.";
            case HEALTH -> "Menambah max health.";
            case ALCHEMY -> "Memperkuat heal dari potion.";
        };
    }

    private String defaultStatName(StatType type) {
        return switch (type) {
            case ARCHERY -> "Archery";
            case FIGHTING -> "Fighting";
            case POWER -> "Power";
            case DEFENSE -> "Defense";
            case HEALTH -> "Health";
            case ALCHEMY -> "Healing";
        };
    }

    public boolean rerollRank(Player player, StatType type) {
        int cost = plugin.fragments().cost(FragmentService.STATS_CLASS, 1);
        if (!plugin.fragments().takeStatsClass(player, cost)) {
            player.sendMessage(Text.s(plugin.msg("need-rank-fragment").replace("%amount%", String.valueOf(cost))));
            return false;
        }
        PlayerProfile profile = plugin.data().get(player);
        StatRank rank = rollRankWithAutoGet(profile);
        profile.rank(type, rank);
        plugin.applyStats(player);
        player.sendMessage(Text.s(plugin.msg("rank-rerolled")
                .replace("%stat%", statName(type))
                .replace("%rank%", rankDisplay(rank))
                .replace("%rarity%", plugin.clans().rarityDisplay(rankRarity(rank)))));
        return true;
    }

    public AutoRankRerollResult autoRerollRank(Player player, StatType type, Rarity target) {
        if (!activeRankAutoRerolls.add(player.getUniqueId())) {
            player.sendMessage(plugin.msg("auto-reroll-busy"));
            return null;
        }
        int cost = plugin.fragments().cost(FragmentService.STATS_CLASS, 1);
        int maxFragments = plugin.getConfig().getInt("auto-reroll.max-fragments", 100);
        int budget = Math.min(maxFragments, plugin.fragments().countStatsClass(player));
        int maxAttempts = budget / Math.max(1, cost);
        if (maxAttempts <= 0) {
            activeRankAutoRerolls.remove(player.getUniqueId());
            player.sendMessage(Text.s(plugin.msg("need-rank-fragment").replace("%amount%", String.valueOf(cost))));
            return null;
        }
        PlayerProfile profile = plugin.data().get(player);
        player.sendMessage(Text.s(plugin.msg("auto-reroll-start")
                .replace("%target%", plugin.clans().rarityDisplay(target))
                .replace("%max_fragments%", String.valueOf(maxFragments))));
        long delay = Math.max(1L, plugin.getConfig().getLong("auto-reroll.delay-ticks", 8L));
        new BukkitRunnable() {
            private int attempts;
            private StatRank rank = profile.rank(type);
            private Rarity rarity = rankRarity(rank);
            private boolean reached;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    finish(false);
                    return;
                }
                if (attempts >= maxAttempts || !plugin.fragments().takeStatsClass(player, cost)) {
                    finish(false);
                    return;
                }
                attempts++;
                rank = rollRankWithAutoGet(profile);
                rarity = rankRarity(rank);
                profile.rank(type, rank);
                plugin.applyStats(player);
                player.sendMessage(Text.s(plugin.msg("rank-auto-roll")
                        .replace("%attempt%", String.valueOf(attempts))
                        .replace("%max_attempts%", String.valueOf(maxAttempts))
                        .replace("%stat%", statName(type))
                        .replace("%rank%", rankDisplay(rank))
                        .replace("%rarity%", plugin.clans().rarityDisplay(rarity))));
                playAutoSound(player, rarity);
                if (rarity.atLeast(target)) {
                    reached = true;
                    finish(true);
                }
            }

            private void finish(boolean targetReached) {
                activeRankAutoRerolls.remove(player.getUniqueId());
                if (player.isOnline() && attempts > 0) {
                    player.sendMessage(Text.s(plugin.msg("rank-auto-rerolled")
                            .replace("%stat%", statName(type))
                            .replace("%rank%", rankDisplay(rank))
                            .replace("%rarity%", plugin.clans().rarityDisplay(rarity))
                            .replace("%target%", plugin.clans().rarityDisplay(target))
                            .replace("%attempts%", String.valueOf(attempts))
                            .replace("%fragments%", String.valueOf(attempts * cost))
                            .replace("%status%", (targetReached || reached)
                                    ? plugin.lang("messages.auto-reroll-reached", "&aTarget tercapai")
                                    : plugin.lang("messages.auto-reroll-limit", "&cLimit fragment tercapai"))));
                }
                cancel();
            }
        }.runTaskTimer(plugin, 0L, delay);
        return new AutoRankRerollResult(profile.rank(type), rankRarity(profile.rank(type)), 0, 0, false);
    }

    public StatRank rollRank() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("rank-reroll.weights");
        if (sec == null) return StatRank.F;
        int total = 0;
        for (StatRank rank : StatRank.values()) total += Math.max(0, sec.getInt(rank.name(), 0));
        int roll = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        int cursor = 0;
        for (StatRank rank : StatRank.values()) {
            cursor += Math.max(0, sec.getInt(rank.name(), 0));
            if (roll < cursor) return rank;
        }
        return StatRank.F;
    }

    private StatRank rollRankWithAutoGet(PlayerProfile profile) {
        Rarity due = dueAutoGet("auto-get.Stats_Class", profile.classAutoGet());
        StatRank rank = due == null ? rollRank() : rollRankAtLeast(due);
        updateAutoGet("auto-get.Stats_Class", profile.classAutoGet(), rankRarity(rank));
        return rank;
    }

    private StatRank rollRankAtLeast(Rarity target) {
        List<StatRank> ranks = new ArrayList<>();
        for (StatRank rank : StatRank.values()) {
            if (rankRarity(rank).atLeast(target)) ranks.add(rank);
        }
        if (ranks.isEmpty()) return rollRank();
        int total = 0;
        for (StatRank rank : ranks) total += Math.max(0, plugin.getConfig().getInt("rank-reroll.weights." + rank.name(), 0));
        int roll = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        int cursor = 0;
        for (StatRank rank : ranks) {
            cursor += Math.max(0, plugin.getConfig().getInt("rank-reroll.weights." + rank.name(), 0));
            if (roll < cursor) return rank;
        }
        return ranks.get(0);
    }

    private Rarity dueAutoGet(String path, java.util.Map<Rarity, Integer> counters) {
        if (!plugin.getConfig().getBoolean(path + ".enabled", false)) return null;
        for (Rarity rarity : new Rarity[] {Rarity.MYTHIC, Rarity.EPIC, Rarity.RARE, Rarity.UNCOMMON}) {
            int every = plugin.getConfig().getInt(path + ".targets." + rarity.key(), 0);
            if (every > 0 && counters.getOrDefault(rarity, 0) + 1 >= every) return rarity;
        }
        return null;
    }

    private void updateAutoGet(String path, java.util.Map<Rarity, Integer> counters, Rarity rolled) {
        if (!plugin.getConfig().getBoolean(path + ".enabled", false)) return;
        for (Rarity rarity : Rarity.values()) {
            int every = plugin.getConfig().getInt(path + ".targets." + rarity.key(), 0);
            if (every <= 0) continue;
            if (rolled.atLeast(rarity)) counters.put(rarity, 0);
            else counters.put(rarity, counters.getOrDefault(rarity, 0) + 1);
        }
    }

    public Rarity rankRarity(StatRank rank) {
        return Rarity.from(plugin.getConfig().getString("rank-reroll.rarity." + rank.name()), switch (rank) {
            case F, D -> Rarity.COMMON;
            case C -> Rarity.UNCOMMON;
            case B -> Rarity.RARE;
            case A -> Rarity.EPIC;
            case S, SS, SSS -> Rarity.MYTHIC;
        });
    }

    public String rankDisplay(StatRank rank) {
        return rankColor(rank) + rank.name();
    }

    public String rankColor(StatRank rank) {
        return plugin.getConfig().getString("rank-reroll.colors." + rank.name(), "&f");
    }

    private void playAutoSound(Player player, Rarity rarity) {
        String configured = plugin.getConfig().getString("auto-reroll.sounds." + rarity.key(), "BLOCK_NOTE_BLOCK_PLING");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(configured), 1.0f, soundPitch(rarity));
        } catch (IllegalArgumentException ignored) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, soundPitch(rarity));
        }
    }

    private float soundPitch(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> 0.8f;
            case UNCOMMON -> 1.0f;
            case RARE -> 1.2f;
            case EPIC -> 1.45f;
            case MYTHIC -> 1.8f;
        };
    }

    private void levelUpEffects(Player player, int level) {
        if (plugin.levelConfig().getBoolean("level-up-effects.title.enabled", true)) {
            String title = plugin.levelConfig().getString("level-up-effects.title.title", "&dLevel Up!");
            String subtitle = plugin.levelConfig().getString("level-up-effects.title.subtitle", "&fYou reached level &d%level%");
            player.showTitle(Title.title(
                    Text.c(title.replace("%level%", String.valueOf(level))),
                    Text.c(subtitle.replace("%level%", String.valueOf(level))),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(1800), Duration.ofMillis(700))
            ));
        }
        if (plugin.levelConfig().getBoolean("level-up-effects.sound.enabled", true)) {
            try {
                Sound sound = Sound.valueOf(plugin.levelConfig().getString("level-up-effects.sound.name", "ENTITY_PLAYER_LEVELUP"));
                float volume = (float) plugin.levelConfig().getDouble("level-up-effects.sound.volume", 1.0);
                float pitch = (float) plugin.levelConfig().getDouble("level-up-effects.sound.pitch", 1.2);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException ignored) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            }
        }
        if (plugin.levelConfig().getBoolean("level-up-effects.firework.enabled", true)) {
            Location location = player.getLocation().add(0, 1.0, 0);
            Firework firework = player.getWorld().spawn(location, Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.setPower(0);
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL)
                    .withColor(org.bukkit.Color.FUCHSIA)
                    .withFade(org.bukkit.Color.PURPLE)
                    .trail(true)
                    .build());
            firework.setFireworkMeta(meta);
            firework.detonate();
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String prettyMaterial(String raw) {
        String[] words = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    private String itemSummary(List<String> items) {
        List<String> parts = new ArrayList<>();
        for (String raw : items) {
            String[] split = raw.split(":");
            parts.add(prettyMaterial(split[0]) + " x" + (split.length > 1 ? split[1] : "1"));
        }
        return String.join(", ", parts);
    }
}
