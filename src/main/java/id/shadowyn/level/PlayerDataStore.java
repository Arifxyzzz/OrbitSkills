package id.shadowyn.level;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class PlayerDataStore {
    private final ShadowynLevelPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerProfile> cache = new HashMap<>();
    private YamlConfiguration yaml;

    public PlayerDataStore(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/players.yml");
    }

    public void load() {
        file.getParentFile().mkdirs();
        yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            PlayerProfile profile = new PlayerProfile(uuid, sec.getString("name", "Unknown"));
            profile.level(sec.getInt("level", 1));
            profile.exp(sec.getLong("exp", 0));
            profile.statsPoint(sec.getInt("stats-point", 0));
            profile.levelRewardPoints(sec.getInt("level-reward-points", 0));
            profile.skillActionbar(sec.getBoolean("skill-actionbar", true));
            String savedRace = sec.getString("race", null);
            if (savedRace == null) {
                profile.race(sec.getString("clan", "HUMAN"));
                profile.guildClan(sec.getString("guild-clan", "NONE"));
            } else {
                profile.race(savedRace);
                profile.guildClan(sec.getString("clan", sec.getString("guild-clan", "NONE")));
            }
            profile.clanRerollsSinceMythic(sec.getInt("clan-rerolls-since-mythic", 0));
            ConfigurationSection autoGet = sec.getConfigurationSection("auto-get");
            if (autoGet != null) {
                loadAutoGet(autoGet.getConfigurationSection("races"), profile.raceAutoGet());
                loadAutoGet(autoGet.getConfigurationSection("stats-class"), profile.classAutoGet());
                loadAutoGet(autoGet.getConfigurationSection("clans"), profile.clanAutoGet());
            } else {
                profile.raceAutoGet(Rarity.MYTHIC, profile.clanRerollsSinceMythic());
            }
            ConfigurationSection stats = sec.getConfigurationSection("stats");
            if (stats != null) {
                for (StatType type : StatType.values()) {
                    profile.stat(type, stats.getInt(type.name(), 0));
                }
                migrateOldStats(stats, profile);
            }
            ConfigurationSection ranks = sec.getConfigurationSection("stat-ranks");
            if (ranks != null) {
                for (StatType type : StatType.values()) {
                    profile.rank(type, StatRank.from(ranks.getString(type.name(), "F")));
                }
            }
            normalizeProfile(profile);
            cache.put(uuid, profile);
        }
        save();
    }

    private void migrateOldStats(ConfigurationSection stats, PlayerProfile profile) {
        if (!stats.isSet(StatType.ARCHERY.name())) {
            profile.stat(StatType.ARCHERY, stats.getInt("ARCHER", 0));
        }
        if (!stats.isSet(StatType.FIGHTING.name())) {
            profile.stat(StatType.FIGHTING, stats.getInt("ASSASSIN", 0));
        }
        if (!stats.isSet(StatType.DEFENSE.name())) {
            profile.stat(StatType.DEFENSE, stats.getInt("RESISTANCE", 0));
        }
    }

    private void normalizeProfile(PlayerProfile profile) {
        int cap = plugin.getConfig().getInt("settings.max-total-stat-points", 5000);
        int total = Math.max(0, profile.statsPoint() + plugin.levels().usedStats(profile));
        if (total <= cap) {
            profile.levelRewardPoints(Math.min(profile.levelRewardPoints(), total));
            return;
        }
        plugin.levels().setTotalStats(profile, cap);
    }

    public PlayerProfile get(Player player) {
        PlayerProfile profile = cache.computeIfAbsent(player.getUniqueId(), id -> new PlayerProfile(id, player.getName()));
        profile.name(player.getName());
        return profile;
    }

    public PlayerProfile get(UUID uuid) {
        return cache.get(uuid);
    }

    public java.util.List<PlayerProfile> allProfiles() {
        return new java.util.ArrayList<>(cache.values());
    }

    public PlayerProfile findByName(String name) {
        if (name == null || name.isBlank()) return null;
        for (PlayerProfile profile : cache.values()) {
            if (profile.name().equalsIgnoreCase(name)) return profile;
        }
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return cache.values().stream()
                .filter(profile -> profile.name().toLowerCase(java.util.Locale.ROOT).startsWith(lower))
                .findFirst()
                .orElse(null);
    }

    public boolean reset(Player player) {
        cache.put(player.getUniqueId(), new PlayerProfile(player.getUniqueId(), player.getName()));
        return true;
    }

    public boolean resetByName(String name) {
        UUID found = null;
        for (PlayerProfile profile : cache.values()) {
            if (profile.name().equalsIgnoreCase(name)) {
                found = profile.uuid();
                break;
            }
        }
        if (found == null) {
            return false;
        }
        cache.remove(found);
        yaml.set("players." + found, null);
        save();
        return true;
    }

    public void save() {
        yaml.set("players", null);
        for (PlayerProfile profile : cache.values()) {
            String path = "players." + profile.uuid();
            yaml.set(path + ".name", profile.name());
            yaml.set(path + ".level", profile.level());
            yaml.set(path + ".exp", profile.exp());
            yaml.set(path + ".stats-point", profile.statsPoint());
            yaml.set(path + ".level-reward-points", profile.levelRewardPoints());
            yaml.set(path + ".skill-actionbar", profile.skillActionbar());
            yaml.set(path + ".race", profile.race());
            yaml.set(path + ".clan", profile.guildClan());
            yaml.set(path + ".clan-rerolls-since-mythic", profile.clanRerollsSinceMythic());
            saveAutoGet(path + ".auto-get.races", profile.raceAutoGet());
            saveAutoGet(path + ".auto-get.stats-class", profile.classAutoGet());
            saveAutoGet(path + ".auto-get.clans", profile.clanAutoGet());
            for (Map.Entry<StatType, Integer> entry : profile.stats().entrySet()) {
                yaml.set(path + ".stats." + entry.getKey().name(), entry.getValue());
            }
            for (Map.Entry<StatType, StatRank> entry : profile.ranks().entrySet()) {
                yaml.set(path + ".stat-ranks." + entry.getKey().name(), entry.getValue().name());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save player data: " + e.getMessage());
        }
    }

    private void loadAutoGet(ConfigurationSection section, Map<Rarity, Integer> target) {
        if (section == null) return;
        for (Rarity rarity : Rarity.values()) {
            target.put(rarity, Math.max(0, section.getInt(rarity.name(), section.getInt(rarity.key(), 0))));
        }
    }

    private void saveAutoGet(String path, Map<Rarity, Integer> source) {
        for (Rarity rarity : Rarity.values()) {
            int value = source.getOrDefault(rarity, 0);
            if (value > 0) yaml.set(path + "." + rarity.name(), value);
        }
    }
}
