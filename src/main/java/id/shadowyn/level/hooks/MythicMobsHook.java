package id.shadowyn.level.hooks;

import id.shadowyn.level.ShadowynLevelPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Reads MythicMobs mob identity straight off the entity.
 *
 * <p>MythicMobs stamps every mob it spawns with its own persistent data — the internal name
 * under {@code mythicmobs:type} and the spawn level under {@code mythicmobs:level} — and its
 * own {@code ActiveMob} class reads them back the same way. That means this hook needs no
 * MythicMobs class, no reflection, and no compile-time dependency: it asks Bukkit for a
 * container that MythicMobs happens to have filled in. Nothing here can fail to link, so the
 * class is safe to reference from anywhere in the plugin.
 *
 * <p>The one thing it does need is the MythicMobs plugin instance, purely to build a key in
 * the right namespace. No instance, no hook — {@link HookManager} handles that.
 */
public final class MythicMobsHook extends Hook {
    private final NamespacedKey typeKey;
    private final NamespacedKey levelKey;

    public MythicMobsHook(ShadowynLevelPlugin plugin) {
        super(plugin);
        Plugin mythic = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (mythic == null) throw new IllegalStateException("MythicMobs is not loaded");
        this.typeKey = new NamespacedKey(mythic, "type");
        this.levelKey = new NamespacedKey(mythic, "level");
    }

    @Override
    public String pluginName() {
        return "MythicMobs";
    }

    public boolean isMythicMob(Entity entity) {
        return mobType(entity) != null;
    }

    /**
     * Returns the mob's internal name — the key it is written under in MythicMobs' own mob
     * files — or {@code null} for a vanilla entity.
     */
    public String mobType(Entity entity) {
        if (entity == null) return null;
        try {
            String type = entity.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
            return type == null || type.isBlank() ? null : type;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Returns the level the mob spawned at, or {@code 1.0} when it carries none.
     *
     * <p>The level is read tolerantly across numeric types. MythicMobs is free to change how
     * it stores the value between releases, and a wrong guess here should cost the level
     * scaling, not the EXP reward that depends on it.
     */
    public double mobLevel(Entity entity) {
        if (entity == null) return 1.0;
        PersistentDataContainer container;
        try {
            container = entity.getPersistentDataContainer();
        } catch (Throwable ignored) {
            return 1.0;
        }
        Double asDouble = read(container, PersistentDataType.DOUBLE);
        if (asDouble != null) return asDouble;
        Integer asInt = read(container, PersistentDataType.INTEGER);
        if (asInt != null) return asInt;
        Float asFloat = read(container, PersistentDataType.FLOAT);
        if (asFloat != null) return asFloat;
        String asText = read(container, PersistentDataType.STRING);
        if (asText != null) {
            try {
                return Double.parseDouble(asText.trim());
            } catch (NumberFormatException ignored) {
                return 1.0;
            }
        }
        return 1.0;
    }

    private <T> T read(PersistentDataContainer container, PersistentDataType<?, T> type) {
        try {
            return container.get(levelKey, type);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
