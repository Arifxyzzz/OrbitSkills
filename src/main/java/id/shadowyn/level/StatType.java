package id.shadowyn.level;

import org.bukkit.Material;

public enum StatType {
    ALCHEMY(Material.BREWING_STAND),
    DEFENSE(Material.SHIELD),
    HEALTH(Material.GOLDEN_APPLE),
    FIGHTING(Material.NETHERITE_SWORD),
    ARCHERY(Material.BOW),
    POWER(Material.PLAYER_HEAD);

    private final Material icon;

    StatType(Material icon) {
        this.icon = icon;
    }

    public Material icon() {
        return icon;
    }

    public String configKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static StatType from(String raw) {
        if (raw == null) return null;
        String normalized = raw.replace("_", "").replace("-", "").replace(" ", "");
        if (normalized.equalsIgnoreCase("archer") || normalized.equalsIgnoreCase("projectile")) return ARCHERY;
        if (normalized.equalsIgnoreCase("assassin") || normalized.equalsIgnoreCase("damage")) return FIGHTING;
        if (normalized.equalsIgnoreCase("resistance")) return DEFENSE;
        if (normalized.equalsIgnoreCase("hp") || normalized.equalsIgnoreCase("maxhealth")) return HEALTH;
        for (StatType type : values()) {
            if (type.name().equalsIgnoreCase(raw) || type.configKey().equalsIgnoreCase(raw) || type.name().replace("_", "").equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }
}
