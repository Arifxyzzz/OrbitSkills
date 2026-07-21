package id.shadowyn.level;

import java.util.Locale;

public enum Rarity {
    COMMON(0),
    UNCOMMON(1),
    RARE(2),
    EPIC(3),
    MYTHIC(4);

    private final int weight;

    Rarity(int weight) {
        this.weight = weight;
    }

    public boolean atLeast(Rarity target) {
        return weight >= target.weight;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Rarity from(String raw, Rarity fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String normalized = raw.replace("-", "").replace("_", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (normalized.equals("COMON")) return COMMON;
        if (normalized.equals("UNCOMON")) return UNCOMMON;
        for (Rarity rarity : values()) {
            if (rarity.name().replace("_", "").equalsIgnoreCase(normalized)) return rarity;
        }
        return fallback;
    }
}
