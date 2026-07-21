package id.shadowyn.level;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerProfile {
    private final UUID uuid;
    private String name;
    private int level = 1;
    private long exp;
    private int statsPoint;
    private int levelRewardPoints;
    private boolean skillActionbar = true;
    private String clan = "HUMAN";
    private String guildClan = "NONE";
    private int clanRerollsSinceMythic;
    private final Map<Rarity, Integer> raceAutoGet = new EnumMap<>(Rarity.class);
    private final Map<Rarity, Integer> classAutoGet = new EnumMap<>(Rarity.class);
    private final Map<Rarity, Integer> clanAutoGet = new EnumMap<>(Rarity.class);
    private final Map<StatType, Integer> stats = new EnumMap<>(StatType.class);
    private final Map<StatType, StatRank> ranks = new EnumMap<>(StatType.class);

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        for (StatType type : StatType.values()) {
            stats.put(type, 0);
            ranks.put(type, StatRank.F);
        }
        for (Rarity rarity : Rarity.values()) {
            raceAutoGet.put(rarity, 0);
            classAutoGet.put(rarity, 0);
            clanAutoGet.put(rarity, 0);
        }
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public void name(String name) { this.name = name; }
    public int level() { return level; }
    public void level(int level) { this.level = Math.max(1, level); }
    public long exp() { return exp; }
    public void exp(long exp) { this.exp = Math.max(0, exp); }
    public int statsPoint() { return statsPoint; }
    public void statsPoint(int statsPoint) { this.statsPoint = Math.max(0, statsPoint); }
    public int levelRewardPoints() { return levelRewardPoints; }
    public void levelRewardPoints(int levelRewardPoints) { this.levelRewardPoints = Math.max(0, levelRewardPoints); }
    public boolean skillActionbar() { return skillActionbar; }
    public void skillActionbar(boolean skillActionbar) { this.skillActionbar = skillActionbar; }
    public String clan() { return clan; }
    public void clan(String clan) { this.clan = clan == null ? "HUMAN" : clan; }
    public String race() { return clan; }
    public void race(String race) { clan(race); }
    public String guildClan() { return guildClan; }
    public void guildClan(String guildClan) { this.guildClan = guildClan == null || guildClan.isBlank() ? "NONE" : guildClan; }
    public int clanRerollsSinceMythic() { return clanRerollsSinceMythic; }
    public void clanRerollsSinceMythic(int clanRerollsSinceMythic) { this.clanRerollsSinceMythic = Math.max(0, clanRerollsSinceMythic); }
    public int raceAutoGet(Rarity rarity) { return raceAutoGet.getOrDefault(rarity, 0); }
    public void raceAutoGet(Rarity rarity, int value) { raceAutoGet.put(rarity, Math.max(0, value)); }
    public Map<Rarity, Integer> raceAutoGet() { return raceAutoGet; }
    public int classAutoGet(Rarity rarity) { return classAutoGet.getOrDefault(rarity, 0); }
    public void classAutoGet(Rarity rarity, int value) { classAutoGet.put(rarity, Math.max(0, value)); }
    public Map<Rarity, Integer> classAutoGet() { return classAutoGet; }
    public int clanAutoGet(Rarity rarity) { return clanAutoGet.getOrDefault(rarity, 0); }
    public void clanAutoGet(Rarity rarity, int value) { clanAutoGet.put(rarity, Math.max(0, value)); }
    public Map<Rarity, Integer> clanAutoGet() { return clanAutoGet; }
    public int stat(StatType type) { return stats.getOrDefault(type, 0); }
    public void stat(StatType type, int value) { stats.put(type, Math.max(0, value)); }
    public Map<StatType, Integer> stats() { return stats; }
    public StatRank rank(StatType type) { return ranks.getOrDefault(type, StatRank.F); }
    public void rank(StatType type, StatRank rank) { ranks.put(type, rank == null ? StatRank.F : rank); }
    public Map<StatType, StatRank> ranks() { return ranks; }
}
