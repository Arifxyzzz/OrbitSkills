package id.shadowyn.level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class FragmentService {
    public static final String RACES = "Races";
    public static final String STATS_CLASS = "Stats_Class";
    public static final String CLANS = "Clans";

    private final ShadowynLevelPlugin plugin;
    private final NamespacedKey fragmentKey;
    private final NamespacedKey legacyRaceKey;
    private final NamespacedKey legacyClassKey;

    public FragmentService(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
        this.fragmentKey = new NamespacedKey(plugin, "fragment");
        this.legacyRaceKey = new NamespacedKey(plugin, "reroll_fragment");
        this.legacyClassKey = new NamespacedKey(plugin, "rank_fragment");
    }

    public ItemStack create(int amount) {
        return createRaces(amount);
    }

    public ItemStack createClan(int amount) {
        return createRaces(amount);
    }

    public ItemStack createRank(int amount) {
        return createStatsClass(amount);
    }

    public ItemStack createRaces(int amount) {
        return create(RACES, amount);
    }

    public ItemStack createStatsClass(int amount) {
        return create(STATS_CLASS, amount);
    }

    public ItemStack create(String id, int amount) {
        String key = resolveId(id);
        ConfigurationSection section = section(key);
        Material material = Material.matchMaterial(section == null ? null : section.getString("material", "AMETHYST_SHARD"));
        ItemStack item = new ItemStack(material == null ? Material.AMETHYST_SHARD : material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        String name = section == null ? "&#C7B8FF" + key + " Fragment" : section.getString("name", "&#C7B8FF" + key + " Fragment");
        meta.displayName(Text.item(name));
        List<String> lore = section == null ? List.of() : section.getStringList("lore");
        meta.lore(lore.stream().map(Text::item).toList());
        int model = section == null ? 0 : section.getInt("custom-model-data", 0);
        if (model > 0) meta.setCustomModelData(model);
        meta.getPersistentDataContainer().set(fragmentKey, PersistentDataType.STRING, key);
        if (same(key, RACES)) meta.getPersistentDataContainer().set(legacyRaceKey, PersistentDataType.BYTE, (byte) 1);
        if (same(key, STATS_CLASS)) meta.getPersistentDataContainer().set(legacyClassKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isFragment(ItemStack item) {
        return fragmentId(item) != null || isRaces(item) || isStatsClass(item);
    }

    public boolean isClanFragment(ItemStack item) {
        return isRaces(item);
    }

    public boolean isRankFragment(ItemStack item) {
        return isStatsClass(item);
    }

    public boolean isRaces(ItemStack item) {
        String id = fragmentId(item);
        return same(id, RACES) || hasLegacy(item, legacyRaceKey);
    }

    public boolean isStatsClass(ItemStack item) {
        String id = fragmentId(item);
        return same(id, STATS_CLASS) || hasLegacy(item, legacyClassKey);
    }

    public boolean isFragment(ItemStack item, String id) {
        String resolved = resolveId(id);
        if (same(resolved, RACES)) return isRaces(item);
        if (same(resolved, STATS_CLASS)) return isStatsClass(item);
        return same(fragmentId(item), resolved);
    }

    public boolean take(Player player, int amount) {
        return takeRaces(player, amount);
    }

    public boolean takeClan(Player player, int amount) {
        return takeRaces(player, amount);
    }

    public boolean takeRank(Player player, int amount) {
        return takeStatsClass(player, amount);
    }

    public boolean takeRaces(Player player, int amount) {
        return take(player, RACES, amount);
    }

    public boolean takeStatsClass(Player player, int amount) {
        return take(player, STATS_CLASS, amount);
    }

    public boolean take(Player player, String id, int amount) {
        int left = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (!isFragment(item, id)) continue;
            left -= item.getAmount();
            if (left <= 0) break;
        }
        if (left > 0) return false;
        left = amount;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!isFragment(item, id)) continue;
            int remove = Math.min(left, item.getAmount());
            item.setAmount(item.getAmount() - remove);
            if (item.getAmount() <= 0) player.getInventory().setItem(slot, null);
            left -= remove;
            if (left <= 0) return true;
        }
        return true;
    }

    public int countClan(Player player) {
        return countRaces(player);
    }

    public int countRank(Player player) {
        return countStatsClass(player);
    }

    public int countRaces(Player player) {
        return count(player, RACES);
    }

    public int countStatsClass(Player player) {
        return count(player, STATS_CLASS);
    }

    public int count(Player player, String id) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isFragment(item, id)) total += item.getAmount();
        }
        return total;
    }

    public int cost(String id, int fallback) {
        ConfigurationSection section = section(resolveId(id));
        return section == null ? fallback : Math.max(1, section.getInt("cost", fallback));
    }

    public List<String> ids() {
        ConfigurationSection section = plugin.fragmentConfig().getConfigurationSection("fragments");
        if (section == null) return List.of(RACES, STATS_CLASS, CLANS);
        return new ArrayList<>(section.getKeys(false));
    }

    public String resolveId(String raw) {
        if (raw == null || raw.isBlank()) return RACES;
        String normalized = normalize(raw);
        for (String id : ids()) {
            if (normalize(id).equals(normalized)) return id;
        }
        return switch (normalized) {
            case "race", "races", "reroll", "racefragment" -> RACES;
            case "class", "classes", "statsclass", "rank", "rankfragment", "classfragment" -> STATS_CLASS;
            case "clan", "clans", "guild" -> CLANS;
            default -> raw;
        };
    }

    private ConfigurationSection section(String id) {
        return plugin.fragmentConfig() == null ? null : plugin.fragmentConfig().getConfigurationSection("fragments." + id);
    }

    private String fragmentId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(fragmentKey, PersistentDataType.STRING);
    }

    private boolean hasLegacy(ItemStack item, NamespacedKey key) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private boolean same(String a, String b) {
        return a != null && b != null && normalize(a).equals(normalize(b));
    }

    private String normalize(String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
