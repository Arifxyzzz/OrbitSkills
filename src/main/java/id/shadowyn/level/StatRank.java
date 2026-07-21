package id.shadowyn.level;

public enum StatRank {
    F(1.00),
    D(1.08),
    C(1.16),
    B(1.28),
    A(1.42),
    S(1.60),
    SS(1.85),
    SSS(2.15);

    private final double multiplier;

    StatRank(double multiplier) {
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return multiplier;
    }

    public static StatRank from(String raw) {
        if (raw == null) return F;
        for (StatRank rank : values()) {
            if (rank.name().equalsIgnoreCase(raw)) return rank;
        }
        return F;
    }
}
