package id.shadowyn.level;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ShadowynPlaceholders extends PlaceholderExpansion {
    private final ShadowynLevelPlugin plugin;
    private final String identifier;
    private final String author;

    public ShadowynPlaceholders(ShadowynLevelPlugin plugin, String identifier, String author) {
        this.plugin = plugin;
        this.identifier = identifier;
        this.author = author;
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public @NotNull String getAuthor() {
        return author;
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) {
            return "0";
        }
        PlayerProfile profile = plugin.data().get(player.getUniqueId());
        if (profile == null) {
            return "0";
        }
        String lower = params.toLowerCase();
        if (lower.startsWith("level_row_") || lower.startsWith("level_pad_")) {
            int width = parseInt(lower.substring(lower.lastIndexOf('_') + 1), 2);
            return String.format(java.util.Locale.US, "%0" + Math.max(1, width) + "d", profile.level());
        }
        return switch (lower) {
            case "level" -> String.valueOf(profile.level());
            case "level_raw" -> String.valueOf(profile.level());
            case "exp" -> String.valueOf(profile.exp());
            case "exp_formatted", "exp_short" -> plugin.formatCompact(profile.exp());
            case "total_exp" -> String.valueOf(plugin.levels().totalExp(profile));
            case "total_exp_formatted", "total_exp_short" -> plugin.formatCompact(plugin.levels().totalExp(profile));
            case "progress_percent", "total_progress_percent" -> String.valueOf(plugin.levels().totalProgressPercent(profile));
            case "exp_needed" -> String.valueOf(plugin.levels().neededExp(profile.level()));
            case "exp_needed_formatted", "exp_needed_short" -> {
                long needed = plugin.levels().neededExp(profile.level());
                yield needed == Long.MAX_VALUE ? "MAX" : plugin.formatCompact(needed);
            }
            case "exp_percent" -> {
                long needed = plugin.levels().neededExp(profile.level());
                yield needed == Long.MAX_VALUE ? "100" : String.valueOf((int) Math.floor(profile.exp() * 100.0 / Math.max(1, needed)));
            }
            case "statspoint" -> String.valueOf(profile.statsPoint());
            case "stats_perlevel" -> String.valueOf(plugin.levels().levelPointReward());
            case "race" -> Text.s(plugin.clans().displayName(profile.race()));
            case "race_key" -> profile.race();
            case "clan" -> Text.s(plugin.clans().guildDisplayName(profile.guildClan()));
            case "clan_key" -> profile.guildClan();
            case "archery" -> String.valueOf(profile.stat(StatType.ARCHERY));
            case "fighting" -> String.valueOf(profile.stat(StatType.FIGHTING));
            case "power" -> String.valueOf(profile.stat(StatType.POWER));
            case "attack" -> String.valueOf(profile.stat(StatType.POWER));
            case "health" -> String.valueOf(profile.stat(StatType.HEALTH));
            case "resistance" -> String.valueOf(profile.stat(StatType.DEFENSE));
            case "defense" -> String.valueOf(profile.stat(StatType.DEFENSE));
            case "assassin" -> String.valueOf(profile.stat(StatType.FIGHTING));
            case "damage" -> String.valueOf(profile.stat(StatType.FIGHTING));
            case "archer" -> String.valueOf(profile.stat(StatType.ARCHERY));
            case "projectile" -> String.valueOf(profile.stat(StatType.ARCHERY));
            case "alchemy" -> String.valueOf(profile.stat(StatType.ALCHEMY));
            case "healing" -> String.valueOf(profile.stat(StatType.ALCHEMY));
            case "archery_rank" -> profile.rank(StatType.ARCHERY).name();
            case "fighting_rank" -> profile.rank(StatType.FIGHTING).name();
            case "power_rank" -> profile.rank(StatType.POWER).name();
            case "defense_rank" -> profile.rank(StatType.DEFENSE).name();
            case "health_rank" -> profile.rank(StatType.HEALTH).name();
            case "alchemy_rank" -> profile.rank(StatType.ALCHEMY).name();
            case "damage_bonus", "damage_value", "melee_damage" -> String.valueOf((int) Math.round(plugin.damagePreview(profile, false)));
            case "projectile_damage_bonus", "projectile_damage" -> String.valueOf((int) Math.round(plugin.damagePreview(profile, true)));
            case "melee_pierce" -> String.valueOf((int) Math.round(plugin.defensePenetrationPercent(profile, false)));
            case "projectile_pierce" -> String.valueOf((int) Math.round(plugin.defensePenetrationPercent(profile, true)));
            case "melee_hp_damage" -> String.valueOf((int) Math.round(plugin.maxHealthDamagePerThousand(profile, false)));
            case "projectile_hp_damage" -> String.valueOf((int) Math.round(plugin.maxHealthDamagePerThousand(profile, true)));
            case "health_bonus", "max_health" -> String.valueOf((int) Math.round(plugin.effectiveMaxHealth(profile)));
            case "health_bonus_formatted", "max_health_formatted", "max_health_short" -> plugin.formatCompact(plugin.effectiveMaxHealth(profile));
            case "current_health", "current_hp" -> onlineHealth(player, false);
            case "current_health_formatted", "current_hp_formatted", "current_health_short", "current_hp_short" -> onlineHealth(player, true);
            case "resistance_bonus" -> String.valueOf((int) Math.round(plugin.levels().statBonusPercent(profile, StatType.DEFENSE)));
            case "defense_bonus", "defense_block" -> String.valueOf((int) Math.round(plugin.defenseBlockPerThousand(profile)));
            case "healing_bonus" -> String.valueOf((int) Math.round(plugin.levels().potionHealBonusPercent(profile)));
            default -> "0";
        };
    }

    private String onlineHealth(OfflinePlayer offlinePlayer, boolean formatted) {
        Player player = offlinePlayer.getPlayer();
        if (player == null) return "0";
        double health = plugin.effectiveCurrentHealth(player);
        return formatted ? plugin.formatCompact(health) : String.valueOf((int) Math.round(health));
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
