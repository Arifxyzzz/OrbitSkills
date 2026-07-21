package id.shadowyn.level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class ClanService {
    private final ShadowynLevelPlugin plugin;
    private final Set<UUID> activeAutoRerolls = ConcurrentHashMap.newKeySet();

    public record AutoRerollResult(String clan, Rarity rarity, int attempts, int fragmentsUsed, boolean reachedTarget) {
    }

    public ClanService(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
    }

    public String roll() {
        ConfigurationSection clans = raceSection();
        if (clans == null) return "HUMAN";
        return rollFrom(new ArrayList<>(clans.getKeys(false)));
    }

    private String rollFrom(List<String> keys) {
        ConfigurationSection clans = raceSection();
        if (clans == null || keys.isEmpty()) return "HUMAN";
        int total = keys.stream().mapToInt(key -> Math.max(0, clans.getInt(key + ".weight", 0))).sum();
        int roll = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        int cursor = 0;
        for (String key : keys) {
            cursor += Math.max(0, clans.getInt(key + ".weight", 0));
            if (roll < cursor) return key;
        }
        return "HUMAN";
    }

    private String rollAtLeast(Rarity target) {
        List<String> mythics = keys().stream()
                .filter(key -> rarity(key).atLeast(target))
                .toList();
        return mythics.isEmpty() ? roll() : rollFrom(mythics);
    }

    public boolean reroll(Player player) {
        int need = plugin.fragments().cost(FragmentService.RACES, 3);
        if (!plugin.fragments().takeRaces(player, need)) {
            player.sendMessage(Text.s(plugin.msg("need-fragment").replace("%amount%", String.valueOf(need))));
            return false;
        }
        PlayerProfile profile = plugin.data().get(player);
        profile.race(rollWithPity(profile));
        plugin.applyStats(player);
        player.sendMessage(Text.s(plugin.msg("clan-rerolled")
                .replace("%clan%", displayName(profile.race()))
                .replace("%rarity%", rarityDisplay(rarity(profile.race())))));
        return true;
    }

    public boolean rerollGuild(Player player) {
        int need = plugin.fragments().cost(FragmentService.CLANS, 3);
        if (!plugin.fragments().take(player, FragmentService.CLANS, need)) {
            player.sendMessage(Text.s(plugin.msg("need-fragment").replace("%amount%", String.valueOf(need))));
            return false;
        }
        PlayerProfile profile = plugin.data().get(player);
        String rolled = rollGuildWithAutoGet(profile);
        profile.guildClan(rolled);
        plugin.applyStats(player);
        plugin.data().save();
        player.sendMessage(Text.s("&#FFD6A5Clan &#6D7890› " + guildDisplayName(rolled)
                + " &#6D7890( " + rarityDisplay(guildRarity(rolled)) + " &#6D7890)"));
        return true;
    }

    public AutoRerollResult autoReroll(Player player, Rarity target) {
        if (!activeAutoRerolls.add(player.getUniqueId())) {
            player.sendMessage(plugin.msg("auto-reroll-busy"));
            return null;
        }
        int cost = plugin.fragments().cost(FragmentService.RACES, 3);
        int maxFragments = plugin.getConfig().getInt("auto-reroll.max-fragments", 100);
        int budget = Math.min(maxFragments, plugin.fragments().countRaces(player));
        int maxAttempts = budget / Math.max(1, cost);
        if (maxAttempts <= 0) {
            activeAutoRerolls.remove(player.getUniqueId());
            player.sendMessage(Text.s(plugin.msg("need-fragment").replace("%amount%", String.valueOf(cost))));
            return null;
        }
        PlayerProfile profile = plugin.data().get(player);
        player.sendMessage(Text.s(plugin.msg("auto-reroll-start")
                .replace("%target%", rarityDisplay(target))
                .replace("%max_fragments%", String.valueOf(maxFragments))));
        long delay = Math.max(1L, plugin.getConfig().getLong("auto-reroll.delay-ticks", 8L));
        new BukkitRunnable() {
            private int attempts;
            private String rolled = profile.race();
            private Rarity rolledRarity = rarity(rolled);
            private boolean reached;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    finish(false);
                    return;
                }
                if (attempts >= maxAttempts || !plugin.fragments().takeRaces(player, cost)) {
                    finish(false);
                    return;
                }
                attempts++;
                rolled = rollWithPity(profile);
                rolledRarity = rarity(rolled);
                profile.race(rolled);
                plugin.applyStats(player);
                player.sendMessage(Text.s(plugin.msg("clan-auto-roll")
                        .replace("%attempt%", String.valueOf(attempts))
                        .replace("%max_attempts%", String.valueOf(maxAttempts))
                        .replace("%clan%", displayName(rolled))
                        .replace("%rarity%", rarityDisplay(rolledRarity))));
                playAutoSound(player, rolledRarity);
                if (rolledRarity.atLeast(target)) {
                    reached = true;
                    finish(true);
                }
            }

            private void finish(boolean targetReached) {
                activeAutoRerolls.remove(player.getUniqueId());
                if (player.isOnline() && attempts > 0) {
                    player.sendMessage(Text.s(plugin.msg("clan-auto-rerolled")
                            .replace("%clan%", displayName(rolled))
                            .replace("%rarity%", rarityDisplay(rolledRarity))
                            .replace("%target%", rarityDisplay(target))
                            .replace("%attempts%", String.valueOf(attempts))
                            .replace("%fragments%", String.valueOf(attempts * cost))
                            .replace("%status%", (targetReached || reached)
                                    ? plugin.lang("messages.auto-reroll-reached", "&aTarget tercapai")
                                    : plugin.lang("messages.auto-reroll-limit", "&cLimit fragment tercapai"))));
                }
                cancel();
            }
        }.runTaskTimer(plugin, 0L, delay);
        return new AutoRerollResult(profile.race(), rarity(profile.race()), 0, 0, false);
    }

    public AutoRerollResult autoRerollGuild(Player player, Rarity target) {
        if (!activeAutoRerolls.add(player.getUniqueId())) {
            player.sendMessage(plugin.msg("auto-reroll-busy"));
            return null;
        }
        int cost = plugin.fragments().cost(FragmentService.CLANS, 3);
        int maxFragments = plugin.getConfig().getInt("auto-reroll.max-fragments", 100);
        int budget = Math.min(maxFragments, plugin.fragments().count(player, FragmentService.CLANS));
        int maxAttempts = budget / Math.max(1, cost);
        if (maxAttempts <= 0) {
            activeAutoRerolls.remove(player.getUniqueId());
            player.sendMessage(Text.s(plugin.msg("need-fragment").replace("%amount%", String.valueOf(cost))));
            return null;
        }
        PlayerProfile profile = plugin.data().get(player);
        player.sendMessage(Text.s(plugin.msg("auto-reroll-start")
                .replace("%target%", rarityDisplay(target))
                .replace("%max_fragments%", String.valueOf(maxFragments))));
        long delay = Math.max(1L, plugin.getConfig().getLong("auto-reroll.delay-ticks", 8L));
        new BukkitRunnable() {
            private int attempts;
            private String rolled = profile.guildClan();
            private Rarity rolledRarity = guildRarity(rolled);
            private boolean reached;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    finish(false);
                    return;
                }
                if (attempts >= maxAttempts || !plugin.fragments().take(player, FragmentService.CLANS, cost)) {
                    finish(false);
                    return;
                }
                attempts++;
                rolled = rollGuildWithAutoGet(profile);
                rolledRarity = guildRarity(rolled);
                profile.guildClan(rolled);
                plugin.applyStats(player);
                plugin.data().save();
                player.sendMessage(Text.s(plugin.msg("clan-auto-roll")
                        .replace("%attempt%", String.valueOf(attempts))
                        .replace("%max_attempts%", String.valueOf(maxAttempts))
                        .replace("%clan%", guildDisplayName(rolled))
                        .replace("%rarity%", rarityDisplay(rolledRarity))));
                playAutoSound(player, rolledRarity);
                if (rolledRarity.atLeast(target)) {
                    reached = true;
                    finish(true);
                }
            }

            private void finish(boolean targetReached) {
                activeAutoRerolls.remove(player.getUniqueId());
                if (player.isOnline() && attempts > 0) {
                    player.sendMessage(Text.s(plugin.msg("clan-auto-rerolled")
                            .replace("%clan%", guildDisplayName(rolled))
                            .replace("%rarity%", rarityDisplay(rolledRarity))
                            .replace("%target%", rarityDisplay(target))
                            .replace("%attempts%", String.valueOf(attempts))
                            .replace("%fragments%", String.valueOf(attempts * cost))
                            .replace("%status%", (targetReached || reached)
                                    ? plugin.lang("messages.auto-reroll-reached", "&aTarget reached")
                                    : plugin.lang("messages.auto-reroll-limit", "&cFragment limit reached"))));
                }
                cancel();
            }
        }.runTaskTimer(plugin, 0L, delay);
        return new AutoRerollResult(profile.guildClan(), guildRarity(profile.guildClan()), 0, 0, false);
    }

    private String rollWithPity(PlayerProfile profile) {
        Rarity due = dueAutoGet("auto-get.Races", profile.raceAutoGet());
        String rolled = due == null ? roll() : rollAtLeast(due);
        updateAutoGet("auto-get.Races", profile.raceAutoGet(), rarity(rolled));
        profile.clanRerollsSinceMythic(profile.raceAutoGet(Rarity.MYTHIC));
        return rolled;
    }

    private String rollGuildWithAutoGet(PlayerProfile profile) {
        Rarity due = dueAutoGet("auto-get.Clans", profile.clanAutoGet());
        String rolled = due == null ? rollGuild() : rollGuildAtLeast(due);
        updateAutoGet("auto-get.Clans", profile.clanAutoGet(), guildRarity(rolled));
        return rolled;
    }

    private String rollGuild() {
        ConfigurationSection clans = plugin.clanConfig().getConfigurationSection("clans");
        if (clans == null) return "NONE";
        List<String> keys = clans.getKeys(false).stream()
                .filter(key -> !key.equalsIgnoreCase("NONE"))
                .toList();
        return rollGuildFrom(keys);
    }

    private String rollGuildAtLeast(Rarity target) {
        ConfigurationSection clans = plugin.clanConfig().getConfigurationSection("clans");
        if (clans == null) return "NONE";
        List<String> keys = clans.getKeys(false).stream()
                .filter(key -> !key.equalsIgnoreCase("NONE"))
                .filter(key -> guildRarity(key).atLeast(target))
                .toList();
        return keys.isEmpty() ? rollGuild() : rollGuildFrom(keys);
    }

    private String rollGuildFrom(List<String> keys) {
        if (keys.isEmpty()) return "NONE";
        int total = keys.stream().mapToInt(key -> Math.max(0, plugin.clanConfig().getInt("clans." + key + ".weight", 1))).sum();
        int roll = ThreadLocalRandom.current().nextInt(Math.max(1, total));
        int cursor = 0;
        for (String key : keys) {
            cursor += Math.max(0, plugin.clanConfig().getInt("clans." + key + ".weight", 1));
            if (roll < cursor) return key;
        }
        return keys.get(0);
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

    public double value(String clan, String key) {
        String path = "clans." + clan + "." + key;
        if (plugin.raceConfig().isSet(path)) return plugin.raceConfig().getDouble(path, 0);
        return plugin.getConfig().getDouble(path, 0);
    }

    public double guildValue(String clan, String key) {
        String path = "clans." + clan + ".stats." + key;
        if (plugin.clanConfig().isSet(path)) return plugin.clanConfig().getDouble(path, 0);
        return plugin.clanConfig().getDouble("clans." + clan + "." + key, 0);
    }

    public String description(String clan) {
        return String.join(" ", descriptionLines(clan));
    }

    public List<String> descriptionLines(String clan) {
        String path = "clans." + clan + ".description";
        if (plugin.raceConfig().isList(path)) return plugin.raceConfig().getStringList(path);
        if (plugin.getConfig().isList(path)) return plugin.getConfig().getStringList(path);
        String description = plugin.raceConfig().getString(path, plugin.getConfig().getString(path, ""));
        return description.isBlank() ? List.of() : List.of(description);
    }

    public List<String> passiveLines(String clan) {
        String path = "clans." + clan + ".passive-skills";
        if (plugin.raceConfig().isList(path)) return plugin.raceConfig().getStringList(path);
        if (plugin.getConfig().isList(path)) return plugin.getConfig().getStringList(path);
        List<String> lines = new ArrayList<>();
        addPercentLine(lines, clan, "damage-percent", "Semua damage");
        addPercentLine(lines, clan, "assassin-percent", "Damage melee");
        addPercentLine(lines, clan, "archer-percent", "Damage projectile");
        addPercentLine(lines, clan, "health-percent", "Max health");
        addPercentLine(lines, clan, "defense-percent", "Defense");
        addPercentLine(lines, clan, "alchemy-percent", "Potion heal");
        addPercentLine(lines, clan, "fishing-exp-percent", "EXP fishing");
        addPercentLine(lines, clan, "night-damage-percent", "Damage malam");
        addPercentLine(lines, clan, "mining-bonus-drop-chance", "Chance bonus ore/drop mining");
        if (plugin.raceConfig().getBoolean("clans." + clan + ".water-breathing", plugin.getConfig().getBoolean("clans." + clan + ".water-breathing", false))) {
            lines.add("&bWater Breathing &7- bisa berenang tanpa takut kehabisan napas.");
        }
        int extraArrows = plugin.raceConfig().getInt("clans." + clan + ".extra-arrows", plugin.getConfig().getInt("clans." + clan + ".extra-arrows", 0));
        if (extraArrows > 0) lines.add("&eExtra Arrow &7- menembakkan &f" + extraArrows + " &7panah tambahan.");
        if (plugin.raceConfig().getBoolean("clans." + clan + ".fire-aspect-hit", plugin.getConfig().getBoolean("clans." + clan + ".fire-aspect-hit", false))) {
            lines.add("&cFire Hit &7- serangan membuat target terbakar.");
        }
        if (lines.isEmpty()) lines.add("&7Tidak ada efek pasif khusus.");
        return lines;
    }

    private void addPercentLine(List<String> lines, String clan, String key, String label) {
        double value = value(clan, key);
        if (Math.abs(value) < 0.01) return;
        String color = value > 0 ? "&a+" : "&c";
        lines.add(color + oneDecimal(value) + "% &7" + label);
    }

    private String oneDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05) return String.valueOf((int) Math.round(value));
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    public String displayName(String clan) {
        String path = "clans." + clan + ".display-name";
        return plugin.raceConfig().getString(path, plugin.getConfig().getString(path, clan));
    }

    public String guildDisplayName(String clan) {
        return plugin.clanConfig().getString("clans." + clan + ".display-name", clan == null || clan.equalsIgnoreCase("NONE") ? "N/A" : clan);
    }

    public Rarity guildRarity(String clan) {
        return Rarity.from(plugin.clanConfig().getString("clans." + clan + ".rarity"), Rarity.COMMON);
    }

    public Rarity rarity(String clan) {
        String path = "clans." + clan + ".rarity";
        return Rarity.from(plugin.raceConfig().getString(path, plugin.getConfig().getString(path)), Rarity.COMMON);
    }

    public String rarityDisplay(Rarity rarity) {
        String base = plugin.getConfig().getString("rarity-display." + rarity.key() + ".name", prettyRarity(rarity));
        String color = plugin.getConfig().getString("rarity-display." + rarity.key() + ".color", "&f");
        return color + base;
    }

    private String prettyRarity(Rarity rarity) {
        String lower = rarity.key();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public int totalWeight() {
        ConfigurationSection clans = raceSection();
        if (clans == null) return 0;
        return clans.getKeys(false).stream().mapToInt(key -> Math.max(0, clans.getInt(key + ".weight", 0))).sum();
    }

    public double chance(String clan) {
        int total = totalWeight();
        if (total <= 0) return 0;
        return (plugin.raceConfig().isSet("clans." + clan + ".weight")
                ? plugin.raceConfig().getInt("clans." + clan + ".weight", 0)
                : plugin.getConfig().getInt("clans." + clan + ".weight", 0)) * 100.0 / total;
    }

    public int totalGuildWeight() {
        ConfigurationSection clans = plugin.clanConfig().getConfigurationSection("clans");
        if (clans == null) return 0;
        return clans.getKeys(false).stream()
                .filter(key -> !key.equalsIgnoreCase("NONE"))
                .mapToInt(key -> Math.max(0, clans.getInt(key + ".weight", 0)))
                .sum();
    }

    public double guildChance(String clan) {
        if (clan == null || clan.equalsIgnoreCase("NONE")) return 0;
        int total = totalGuildWeight();
        if (total <= 0) return 0;
        return Math.max(0, plugin.clanConfig().getInt("clans." + clan + ".weight", 0)) * 100.0 / total;
    }

    public List<String> keys() {
        ConfigurationSection clans = raceSection();
        return clans == null ? List.of() : new ArrayList<>(clans.getKeys(false));
    }

    public List<String> guildKeys() {
        ConfigurationSection clans = plugin.clanConfig().getConfigurationSection("clans");
        return clans == null ? List.of("NONE") : new ArrayList<>(clans.getKeys(false));
    }

    private ConfigurationSection raceSection() {
        ConfigurationSection split = plugin.raceConfig().getConfigurationSection("clans");
        return split != null ? split : plugin.getConfig().getConfigurationSection("clans");
    }
}
