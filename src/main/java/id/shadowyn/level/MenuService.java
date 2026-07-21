package id.shadowyn.level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class MenuService implements Listener {
    private static final String LEVEL_TITLE = "menu.titles.level";
    private static final String STATS_TITLE = "menu.titles.stats";
    private static final String STAT_DETAIL_TITLE = "menu.titles.points";
    private static final String CLAN_TITLE = "menu.titles.clan";
    private static final String GUILD_TITLE = "menu.titles.guild";
    private static final String GUIDE_TITLE = "menu.titles.guide";
    private static final String LEADERBOARD_TITLE = "menu.titles.leaderboard";
    private static final String RANK_TITLE = "menu.titles.rank";
    private static final int[] LEVEL_PATH = {1, 2, 3, 4, 5, 6, 7, 16, 15, 14, 13, 12, 11, 10};
    private static final int[] STAT_SLOTS = {10, 11, 12, 14, 15, 16};
    private static final int[] RANK_AUTO_SLOTS = {19, 20, 21, 23, 24, 25};
    private final ShadowynLevelPlugin plugin;
    private final Map<UUID, Rarity> clanAutoTargets = new HashMap<>();
    private final Map<UUID, Rarity> guildAutoTargets = new HashMap<>();
    private final Map<UUID, Rarity> rankAutoTargets = new HashMap<>();
    // Tracks the highest priority placed at each slot during menu building.
    private final Map<Integer, Integer> slotPriority = new HashMap<>();
    private String currentMenuId;

    public MenuService(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
    }

    private void beginMenu(String menuId) {
        slotPriority.clear();
        this.currentMenuId = menuId;
    }

    /** Returns the namespaced config path for an item in the current menu. */
    private String ipath(String key) {
        return "menus." + currentMenuId + ".items." + key;
    }

    /**
     * Places an item into the inventory slot if its priority is higher than
     * what's already there. Priority is read from items.<key>.priority in config.
     * Default: filler = 0, all other items = 1.
     */
    private boolean placeItem(Inventory inv, int slot, ItemStack item, String configKey) {
        if (slot < 0 || slot >= inv.getSize()) return false;
        int priority = plugin.pageConfig().getInt(ipath(configKey) + ".priority", 1);
        int current = slotPriority.getOrDefault(slot, -1);
        if (priority <= current) return false;
        slotPriority.put(slot, priority);
        inv.setItem(slot, item);
        return true;
    }

    /** Call at the end of every menu-open method. Auto-places remaining YAML items. */
    private void finishMenu(Inventory inv, Player player) {
        placeAllSlotItems(inv);
        player.openInventory(inv);
    }

    public void openLevel(Player player, int page) {
        PlayerProfile profile = plugin.data().get(player);
        int max = plugin.maxLevel();
        int[] levelSlots = menuSlots("level", "level-slots", LEVEL_PATH);
        int maxPage = Math.max(1, (int) Math.ceil(max / (double) levelSlots.length));
        page = Math.max(1, Math.min(maxPage, page));
        Map<String, String> vars = commonVars(player, profile);
        vars.put("page", String.valueOf(page));
        vars.put("max_page", String.valueOf(maxPage));
        Inventory inv = Bukkit.createInventory(null, menuSize("level", 27), Text.item(t(LEVEL_TITLE, "Levels {page}", vars)));
        beginMenu("level");
        fill(inv);
        int start = (page - 1) * levelSlots.length + 1;
        int end = Math.min(max, start + levelSlots.length - 1);
        for (int level = start; level <= end; level++) {
            boolean done = profile.level() >= level;
            boolean current = profile.level() == level;
            Material icon = done ? Material.LIME_STAINED_GLASS_PANE : current ? Material.YELLOW_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            Map<String, String> itemVars = commonVars(player, profile);
            itemVars.put("level", String.valueOf(level));
            itemVars.put("status", done ? t("menu.words.unlocked", "&aUnlocked") : t("menu.words.locked", "&cLocked"));
            itemVars.put("need_exp", String.valueOf(plugin.levels().neededExp(Math.max(1, level - 1))));
            List<String> lore = new ArrayList<>(tl("menu.level.level-item.lore", List.of(
                    "&7Status: {status}",
                    "&7EXP: &f{need_exp}"
            ), itemVars));
            lore.addAll(plugin.levels().rewardLore(level));
            inv.setItem(levelSlots[level - start], item(icon, t("menu.level.level-item.name", "&dLevel {level}", itemVars), lore));
        }
        int progressSlot = menuSlot("level", "progress-slot", 22);
        inv.setItem(progressSlot, item(Material.EXPERIENCE_BOTTLE, t("menu.level.progress.name", "&dYour Progress", vars),
                tl("menu.level.progress.lore", List.of(
                        "&7Lv &f{level} &8| &7Page &f{page}/{max_page}",
                        "&7EXP &f{exp}&8/&f{needed_exp}",
                        "&7SP &f{stats_point}"
                ), vars)));
        if (page > 1) {
            int prevSlot = pageItemSlot("level-nav-previous", "menus.level.prev-slot", 18);
            Map<String, String> navVars = Map.of("page", String.valueOf(page - 1));
            inv.setItem(prevSlot, item(Material.ARROW, t("menu.nav.previous.name", "&dPrevious", navVars), tl("menu.nav.previous.lore", List.of("&7Page {page}"), navVars)));
        }
        if (page < maxPage) {
            int nextSlot = pageItemSlot("level-nav-next", "menus.level.next-slot", 26);
            Map<String, String> navVars = Map.of("page", String.valueOf(page + 1));
            inv.setItem(nextSlot, item(Material.ARROW, t("menu.nav.next.name", "&dNext", navVars), tl("menu.nav.next.lore", List.of("&7Page {page}"), navVars)));
        }
        finishMenu(inv, player);
    }

    public void openStats(Player player) {
        PlayerProfile profile = plugin.data().get(player);
        Map<String, String> vars = commonVars(player, profile);
        Inventory inv = Bukkit.createInventory(null, menuSize("stats", 36), Text.item(t(STATS_TITLE, "Stats", vars)));
        beginMenu("stats");
        fill(inv);
        inv.setItem(itemSlot("stats-profile", "menus.stats.items.profile.slot", 4), profileItem(profile, t("menu.stats.profile.name", "&dProfile", vars),
                tl("menu.stats.profile.lore", List.of(
                        "{line}",
                        "&dProfile &8| &f{player}",
                        "&7Lv &f{level} &8| &7EXP &f{exp}/{needed_exp}",
                        "&7Total &f{progress_percent}%",
                        "{progress_bar}",
                        "&7SP &f{stats_point} &8| &7Used &f{used_point}",
                        "&7Race &f{race} &8| &7Clan &f{clan}",
                        "{line}",
                        "&cDMG &f+{melee_damage} &8| &eArrow &f+{projectile_damage}",
                        "&bHP &f{max_health} &8| &9DEF &f{defense}",
                        "&aHeal &f{potion_heal}",
                        "{line}"
                ), vars)));
        int index = 0;
        for (StatType type : StatType.values()) {
            Map<String, String> statVars = statVars(player, profile, type);
            inv.setItem(statSlots()[index++], configItem("stat-" + type.configKey(), t("menu.stats.stat.name", "&d{stat}", statVars),
                    tl("menu.stats.stat.lore", List.of(
                            "&7Point &f{stat_point} &8| &7{rank_colored}",
                            "&7Progress &f{stat_percent}%",
                            "{stat_bar}",
                            "&7Now &f{stat_bonus}",
                            "&7Each &f{stat_per_point}"
                    ), statVars)));
        }
        // Navigation items with visibility check + open-command support
        placeIfVisible(inv, "race-menu", "menus.stats.items.race-menu.slot", 31,
                () -> configItem("race-menu", t("menu.stats.race.name", "&dRaces", vars), tl("menu.stats.race.lore", List.of("&7Race passives and traits."), vars)));
        placeIfVisible(inv, "guild-menu", "menus.stats.items.guild-menu.slot", 32,
                () -> configItem("guild-menu", t("menu.stats.guild.name", "&aClans", vars), tl("menu.stats.guild.lore", List.of("&7Clan stat bonus."), vars)));
        placeIfVisible(inv, "rank-menu", "menus.stats.items.rank.slot", 30,
                () -> configItem("rank-menu", t("menu.stats.rank.name", "&bClasses", vars), tl("menu.stats.rank.lore", List.of("&7Class reroll."), vars)));
        placeIfVisible(inv, "leaderboard", "menus.stats.items.leaderboard.slot", 33,
                () -> configItem("leaderboard", t("menu.stats.leaderboard.name", "&eTop", vars), tl("menu.stats.leaderboard.lore", List.of("&7Lv + EXP."), vars)));
        placeIfVisible(inv, "stats-guide", "menus.stats.items.guide.slot", 35,
                () -> configItem("stats-guide", t("menu.stats.guide.name", "&dGuide", vars), tl("menu.stats.guide.lore", List.of("&7Info."), vars)));
        finishMenu(inv, player);
    }

    /** Places an item into the inventory only if it is visible, respecting priority. */
    private void placeIfVisible(Inventory inv, String itemKey, String oldPath, int fallbackSlot, java.util.function.Supplier<ItemStack> itemSupplier) {
        if (!isItemVisible(itemKey)) return;
        placeItem(inv, itemSlot(itemKey, oldPath, fallbackSlot), itemSupplier.get(), itemKey);
    }

    /** Places a config-based item with visibility + priority checks. */
    private void placeConfigItem(Inventory inv, String itemKey, String oldPath, int fallbackSlot, String name, List<String> lore) {
        placeIfVisible(inv, itemKey, oldPath, fallbackSlot, () -> configItem(itemKey, name, lore));
    }

    /**
     * Auto-places ALL items from the current menu's YAML that have a "slot" field.
     * Items with higher priority override lower ones. No hardcoded keys needed —
     * any item in the YAML is automatically rendered.
     */
    private void placeAllSlotItems(Inventory inv) {
        String sectionPath = "menus." + currentMenuId + ".items";
        org.bukkit.configuration.ConfigurationSection itemsSection = plugin.pageConfig().getConfigurationSection(sectionPath);
        if (itemsSection == null) return;
        Map<String, String> vars = Map.of(); // minimal vars map; replace() auto-adds "line"
        for (String key : itemsSection.getKeys(false)) {
            if (!isItemVisible(key)) continue;
            int slot = plugin.pageConfig().getInt(ipath(key) + ".slot", Integer.MIN_VALUE);
            if (slot == Integer.MIN_VALUE) continue; // no slot = skip (filler items use "slots")
            // Read name/lore from YAML; null name = don't set display name
            String rawName = plugin.pageConfig().getString(ipath(key) + ".name", null);
            String name = rawName == null || rawName.isBlank() ? null : replace(rawName, vars);
            List<String> rawLore = plugin.pageConfig().getStringList(ipath(key) + ".lore");
            List<String> lore = new ArrayList<>();
            for (String line : rawLore) lore.add(replace(line, vars));
            ItemStack item = configItem(key, name, lore);
            placeItem(inv, slot, item, key);
        }
    }

    public void openGuide(Player player) {
        Inventory inv = Bukkit.createInventory(null, menuSize("guide", 27), Text.item(t(GUIDE_TITLE, "Guide")));
        beginMenu("guide");
        fill(inv);
        Map<String, String> vars = guideVars();
        placeConfigItem(inv, "guide-grinding", "menus.guide.items.grinding.slot", 10, t("menu.guide.grinding.name", "&eLeveling", vars), pageLore("guide-grinding", vars));
        placeConfigItem(inv, "guide-point", "menus.guide.items.point.slot", 11, t("menu.guide.point.name", "&aSP", vars), pageLore("guide-point", vars));
        placeConfigItem(inv, "guide-fragment", "menus.guide.items.fragment.slot", 12, t("menu.guide.fragment.name", "&dFragment", vars), pageLore("guide-fragment", vars));
        placeConfigItem(inv, "guide-clan", "menus.guide.items.clan.slot", 14, t("menu.guide.clan.name", "&dRaces", vars), pageLore("guide-clan", vars));
        placeConfigItem(inv, "guide-guild", "menus.guide.items.guild.slot", 15, t("menu.guide.guild.name", "&aClans", vars), pageLore("guide-guild", vars));
        placeConfigItem(inv, "guide-rank", "menus.guide.items.rank.slot", 16, t("menu.guide.rank.name", "&bClasses", vars), pageLore("guide-rank", vars));
        placeConfigItem(inv, "guide-leaderboard", "menus.guide.items.leaderboard.slot", 22, t("menu.guide.leaderboard.name", "&eLeaderboard", vars), pageLore("guide-leaderboard", vars));
        placeConfigItem(inv, "guide-back", "menus.guide.items.back.slot", 31, t("menu.nav.back.name", "&dBack"), List.of(replace("&7Stats", vars)));
        finishMenu(inv, player);
    }

    private List<String> pageLore(String key, Map<String, String> vars) {
        return plugin.pageConfig().getStringList(ipath(key) + ".lore").stream()
                .map(line -> replace(line, vars))
                .toList();
    }

    public void openStatDetail(Player player, StatType type) {
        PlayerProfile profile = plugin.data().get(player);
        Map<String, String> vars = statVars(player, profile, type);
        Inventory inv = Bukkit.createInventory(null, menuSize("points", 36), Text.item(t(STAT_DETAIL_TITLE, "Stat: {stat}", vars)));
        beginMenu("points");
        fill(inv);
        List<String> infoLore = tl("menu.stat-detail.info.lore", List.of(
                "&7Point &f{stat_point} &8| &7SP &f{stats_point}",
                "&7Now &f{stat_bonus}",
                "&7Each &f{stat_per_point}"
        ), vars);
        placeConfigItem(inv, "stat-detail-info", null, menuSlot("points", "info-slot", 4), t("menu.stat-detail.info.name", "&d{stat}", vars), infoLore);
        int[] minusSlots = menuSlots("points", "minus-slots", new int[]{10, 11, 12, 13, 14, 15, 16});
        int[] plusSlots = menuSlots("points", "plus-slots", new int[]{19, 20, 21, 22, 23, 24, 25});
        int[] amounts = {1, 50, 100, 200, 500, 1000, 5000};
        for (int i = 0; i < amounts.length; i++) {
            Map<String, String> amountVars = Map.of("amount", String.valueOf(amounts[i]));
            placeConfigItem(inv, "stat-minus", null, minusSlots[i], t("menu.stat-detail.minus.name", "&c-{amount}", amountVars), tl("menu.stat-detail.minus.lore", List.of("&7Refund."), amountVars));
            placeConfigItem(inv, "stat-plus", null, plusSlots[i], t("menu.stat-detail.plus.name", "&a+{amount}", amountVars), tl("menu.stat-detail.plus.lore", List.of("&7Spend."), amountVars));
        }
        placeConfigItem(inv, "points-back", null, menuSlot("points", "back-slot", 31), t("menu.nav.back.name", "&dBack"), tl("menu.nav.back.lore", List.of("&7Stats")));
        finishMenu(inv, player);
    }

    public void openRank(Player player) {
        PlayerProfile profile = plugin.data().get(player);
        Map<String, String> vars = commonVars(player, profile);
        Inventory inv = Bukkit.createInventory(null, menuSize("rank", 36), Text.item(t(RANK_TITLE, "Classes", vars)));
        beginMenu("rank");
        fill(inv);
        List<String> rankInfoLore = tl("menu.rank.info.lore", List.of(
                "{line}",
                "&7Cost &f{rank_cost}",
                "{line}"
        ), vars);
        placeConfigItem(inv, "rank-info", "menus.rank.items.info.slot", 4, t("menu.rank.info.name", "&bRank Stats", vars), rankInfoLore);
        int index = 0;
        for (StatType type : StatType.values()) {
            Map<String, String> statVars = statVars(player, profile, type);
            inv.setItem(statSlots()[index++], configItem("stat-" + type.configKey(), t("menu.rank.stat.name", "&b{stat} &8[&f{rank}&8]", statVars),
                    tl("menu.rank.stat.lore", List.of(
                            "&7Rank &f{rank} &8| &7x{rank_multiplier}",
                            "&7Now &f{stat_bonus}",
                            "&7Cost &f{rank_cost}"
                    ), statVars)));
        }
        addRankAutoStatControls(player, inv, profile);
        addRankTargetToggle(player, inv, vars);
        placeConfigItem(inv, "rank-back", "menus.rank.items.back.slot", 31, t("menu.nav.back.name", "&dBack"), tl("menu.nav.back.lore", List.of("&7Stats")));
        finishMenu(inv, player);
    }

    public void openLeaderboard(Player player) {
        openLeaderboard(player, 1);
    }

    public void openLeaderboard(Player player, int page) {
        List<PlayerProfile> sorted = plugin.data().allProfiles().stream()
                .sorted((a, b) -> {
                    int level = Integer.compare(b.level(), a.level());
                    if (level != 0) return level;
                    return Long.compare(plugin.levels().totalExp(b), plugin.levels().totalExp(a));
                })
                .toList();
        int[] lbSlots = menuSlots("leaderboard", "entry-slots", new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34});
        int maxPage = Math.max(1, (int) Math.ceil(sorted.size() / (double) lbSlots.length));
        page = Math.max(1, Math.min(maxPage, page));
        Map<String, String> pageVars = new HashMap<>();
        pageVars.put("page", String.valueOf(page));
        pageVars.put("max_page", String.valueOf(maxPage));
        Inventory inv = Bukkit.createInventory(null, menuSize("leaderboard", 54), Text.item(t(LEADERBOARD_TITLE, "Top {page}", pageVars)));
        beginMenu("leaderboard");
        fill(inv);
        int start = (page - 1) * lbSlots.length;
        int end = Math.min(sorted.size(), start + lbSlots.length);
        for (int i = start; i < end; i++) {
            PlayerProfile target = sorted.get(i);
            Map<String, String> vars = leaderboardVars(target, i + 1);
            inv.setItem(lbSlots[i - start], playerHead(target, "leaderboard-entry", t("menu.leaderboard.entry.name", "&e#{rank} &f{player}", vars),
                    tl("menu.leaderboard.entry.lore", List.of(
                            "&7Lv &f{level} &8| &7EXP &f{total_exp}",
                            "&7Race &f{race} &8| &6{progress_percent}%",
                            "&cFighting {fighting_rank} &8| &eArchery {archery_rank}",
                            "&dPower {power_rank} &8| &bHealth {health_rank}",
                            "&aAlchemy {alchemy_rank} &8| &9DEF {defense_rank}"
                    ), vars)));
        }
        PlayerProfile self = plugin.data().get(player);
        Map<String, String> infoVars = leaderboardVars(self, 0);
        infoVars.put("page", String.valueOf(page));
        infoVars.put("max_page", String.valueOf(maxPage));
        int infoSlot = menuSlot("leaderboard", "info-slot", 4);
        inv.setItem(infoSlot, playerHead(self, "leaderboard-info", t("menu.leaderboard.info.name", "&eTop", infoVars), tl("menu.leaderboard.info.lore", List.of("&7Lv + EXP."), infoVars)));
        placeConfigItem(inv, "leaderboard-back", "menus.leaderboard.back-slot", 49, t("menu.nav.back.name", "&dBack"), tl("menu.nav.back.lore", List.of("&7Stats")));
        if (page > 1) {
            int prevSlot = pageItemSlot("leaderboard-nav-previous", "menus.leaderboard.prev-slot", 46);
            Map<String, String> navVars = Map.of("page", String.valueOf(page - 1));
            inv.setItem(prevSlot, item(Material.ARROW, t("menu.nav.previous.name", "&dPrevious", navVars), tl("menu.nav.previous.lore", List.of("&7Page {page}"), navVars)));
        }
        if (page < maxPage) {
            int nextSlot = pageItemSlot("leaderboard-nav-next", "menus.leaderboard.next-slot", 52);
            Map<String, String> navVars = Map.of("page", String.valueOf(page + 1));
            inv.setItem(nextSlot, item(Material.ARROW, t("menu.nav.next.name", "&dNext", navVars), tl("menu.nav.next.lore", List.of("&7Page {page}"), navVars)));
        }
        finishMenu(inv, player);
    }

    public void sendChatProfile(Player player) {
        sendChatProfile(player, plugin.data().get(player));
    }

    public void sendChatProfile(Player viewer, PlayerProfile profile) {
        Map<String, String> vars = Bukkit.getPlayer(profile.uuid()) instanceof Player online
                ? commonVars(online, profile)
                : profileVars(profile);
        for (String line : tl("chat-profile.lines", List.of(
                "{line}",
                "&#C7B8FF✦ &#F7F3FF{player} &#6D7890| &#FFD6A5Lv {level_padded}",
                "&#A7E8FF◆ &#8FA0B7EXP &#F7F3FF{exp}&#6D7890/&#F7F3FF{needed_exp} &#6D7890( &#B8F7D4{exp_percent}% &#6D7890)",
                "{progress_bar}",
                "{line}",
                "&#C7B8FF◆ &#8FA0B7Race &#F7F3FF{race} &#6D7890| &#B8F7D4Clan &#F7F3FF{clan}",
                "&#B8F7D4◆ &#8FA0B7SP &#F7F3FF{stats_point} &#6D7890| &#8FA0B7Used &#F7F3FF{used_point}",
                "{line}",
                "&#FFB6C8◆ &#8FA0B7Damage &#F7F3FF{melee_damage} &#6D7890| &#FFE7A3Archery &#F7F3FF{projectile_damage}",
                "&#C7B8FF◆ &#8FA0B7Power &#F7F3FF{unarmed_damage} &#6D7890| &#A7E8FFHP &#F7F3FF{max_health}",
                "&#C7B8FF◆ &#8FA0B7DEF &#F7F3FF{defense} &#6D7890| &#B8F7D4Heal &#F7F3FF{potion_heal}",
                "{line}"
        ), vars)) {
            viewer.sendMessage(Text.s(line));
        }
    }

    public void openClan(Player player, int page) {
        PlayerProfile profile = plugin.data().get(player);
        List<String> clans = plugin.clans().keys();
        int perPage = 14;
        int maxPage = Math.max(1, (int) Math.ceil(clans.size() / (double) perPage));
        page = Math.max(1, Math.min(maxPage, page));
        Map<String, String> vars = commonVars(player, profile);
        vars.put("page", String.valueOf(page));
        vars.put("max_page", String.valueOf(maxPage));
        Inventory inv = Bukkit.createInventory(null, menuSize("clan", 36), Text.item(t(CLAN_TITLE, "Races {page}", vars)));
        beginMenu("clan");
        fill(inv);
        int currentSlot = menuSlot("clan", "current-slot", 4);
        Map<String, String> currentVars = clanVars(profile.race());
        inv.setItem(currentSlot, clanItem(profile.race(), "clan-current", t("menu.clan.current.name", "&dCurrent Race: &f{race}", currentVars), currentClanLore(profile.race())));
        int start = (page - 1) * perPage;
        int[] cSlots = menuSlots("clan", "entry-slots", new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25});
        for (int i = 0; i < perPage && start + i < clans.size(); i++) {
            String clan = clans.get(start + i);
            inv.setItem(cSlots[i], clanItem(clan, "clan-entry", t("menu.clan.entry.name", "&d{clan}", clanVars(clan)), clanLore(clan)));
        }
        int need = plugin.fragments().cost(FragmentService.RACES, 3);
        Map<String, String> rerollVars = new HashMap<>(vars);
        rerollVars.put("cost", String.valueOf(need));
        PlayerProfile playerProfile = plugin.data().get(player);
        rerollVars.put("epic_pity", String.valueOf(autoGetEvery("auto-get.Races", Rarity.EPIC)));
        rerollVars.put("epic_pity_left", String.valueOf(autoGetLeft("auto-get.Races", playerProfile.raceAutoGet(Rarity.EPIC), Rarity.EPIC)));
        rerollVars.put("mythic_pity", String.valueOf(autoGetEvery("auto-get.Races", Rarity.MYTHIC)));
        rerollVars.put("mythic_pity_left", String.valueOf(autoGetLeft("auto-get.Races", playerProfile.raceAutoGet(Rarity.MYTHIC), Rarity.MYTHIC)));
        placeConfigItem(inv, "race-reroll", "menus.clan.items.reroll.slot", 29, t("menu.clan.reroll.name", "&dReroll", rerollVars), tl("menu.clan.reroll.lore", List.of("&7Cost &f{cost}", "&7Mythic &f{mythic_pity_left}/{mythic_pity}"), rerollVars));
        addAutoClanControls(player, inv, rerollVars);
        placeConfigItem(inv, "clan-back", "menus.clan.items.back.slot", 31, t("menu.nav.back.name", "&dBack"), tl("menu.nav.back.lore", List.of("&7Stats")));
        if (page > 1) {
            int prevSlot = pageItemSlot("clan-nav-previous", "menus.clan.prev-slot", 28);
            Map<String, String> navVars = Map.of("page", String.valueOf(page - 1));
            inv.setItem(prevSlot, item(Material.FIREWORK_ROCKET, t("menu.nav.previous.name", "&dPrevious", navVars), tl("menu.nav.previous.lore", List.of("&7Page {page}"), navVars)));
        }
        if (page < maxPage) {
            int nextSlot = pageItemSlot("clan-nav-next", "menus.clan.next-slot", 34);
            Map<String, String> navVars = Map.of("page", String.valueOf(page + 1));
            inv.setItem(nextSlot, item(Material.FIREWORK_ROCKET, t("menu.nav.next.name", "&dNext", navVars), tl("menu.nav.next.lore", List.of("&7Page {page}"), navVars)));
        }
        finishMenu(inv, player);
    }

    public void openGuild(Player player) {
        PlayerProfile profile = plugin.data().get(player);
        List<String> guilds = plugin.clans().guildKeys();
        Map<String, String> vars = commonVars(player, profile);
        Inventory inv = Bukkit.createInventory(null, menuSize("guild", 36), Text.item(t(GUILD_TITLE, "Clans", vars)));
        beginMenu("guild");
        fill(inv);
        int currentSlot = menuSlot("guild", "current-slot", 4);
        Map<String, String> currentVars = guildVars(profile.guildClan());
        inv.setItem(currentSlot, guildItem(profile.guildClan(), "guild-current", t("menu.guild.current.name", "&aCurrent Clan: &f{clan}", currentVars), currentGuildLore(profile.guildClan())));
        int[] gSlots = menuSlots("guild", "entry-slots", new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25});
        for (int i = 0; i < Math.min(guilds.size(), gSlots.length); i++) {
            String guild = guilds.get(i);
            inv.setItem(gSlots[i], guildItem(guild, "guild-entry", t("menu.guild.entry.name", "&a{clan}", guildVars(guild)), guildLore(guild)));
        }
        int need = plugin.fragments().cost(FragmentService.CLANS, 3);
        Map<String, String> rerollVars = new HashMap<>(vars);
        rerollVars.put("cost", String.valueOf(need));
        rerollVars.put("epic_pity", String.valueOf(autoGetEvery("auto-get.Clans", Rarity.EPIC)));
        rerollVars.put("epic_pity_left", String.valueOf(autoGetLeft("auto-get.Clans", profile.clanAutoGet(Rarity.EPIC), Rarity.EPIC)));
        rerollVars.put("mythic_pity", String.valueOf(autoGetEvery("auto-get.Clans", Rarity.MYTHIC)));
        rerollVars.put("mythic_pity_left", String.valueOf(autoGetLeft("auto-get.Clans", profile.clanAutoGet(Rarity.MYTHIC), Rarity.MYTHIC)));
        placeConfigItem(inv, "guild-reroll", "menus.guild.items.reroll.slot", 29, t("menu.guild.reroll.name", "&aReroll", rerollVars), tl("menu.guild.reroll.lore", List.of("&7Cost &f{cost}"), rerollVars));
        addAutoGuildControls(player, inv, rerollVars);
        placeConfigItem(inv, "guild-back", "menus.guild.items.back.slot", 31, t("menu.nav.back.name", "&dBack"), tl("menu.nav.back.lore", List.of("&7Stats")));
        finishMenu(inv, player);
    }

    private List<String> guildLore(String guild) {
        return tl("menu.guild.lore", List.of(
                "&7{clan_rarity} &8| &f{clan_chance}%",
                "&cDMG &f{damage}% &8| &bHP &f{health}%",
                "&9DEF &f{defense}% &8| &aHeal &f{potion_heal}%",
                "&eArchery &f{projectile}% &8| &cFighting &f{melee}%"
        ), guildVars(guild));
    }

    private List<String> currentGuildLore(String guild) {
        return tl("menu.guild.current.lore", guildLore(guild), guildVars(guild));
    }

    private ItemStack guildItem(String guild, String templateKey, String name, List<String> lore) {
        String fallbackPath = ipath(templateKey) + ".";
        Material material = guild == null || guild.equalsIgnoreCase("NONE")
                ? Material.matchMaterial(plugin.pageConfig().getString(fallbackPath + "material", "BARRIER"))
                : Material.matchMaterial(plugin.pageConfig().getString(fallbackPath + "material", "TOTEM_OF_UNDYING"));
        ItemStack item = item(material == null ? Material.STONE : material, name, new ArrayList<>(lore));
        applyPageMeta(item, templateKey);
        return item;
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String levelTitle = plainTitle(LEVEL_TITLE, "Levels");
        String statsTitle = plainTitle(STATS_TITLE, "Stats");
        String statDetailTitle = plainTitle(STAT_DETAIL_TITLE, "Stat:");
        String clanTitle = plainTitle(CLAN_TITLE, "Races");
        String guildTitle = plainTitle(GUILD_TITLE, "Clans");
        String guideTitle = plainTitle(GUIDE_TITLE, "Guide");
        String leaderboardTitle = plainTitle(LEADERBOARD_TITLE, "Top");
        String rankTitle = plainTitle(RANK_TITLE, "Classes");
        if (!title.contains(levelTitle) && !title.contains(statsTitle) && !title.contains(statDetailTitle) && !title.contains(clanTitle) && !title.contains(guildTitle) && !title.contains(guideTitle) && !title.contains(leaderboardTitle) && !title.contains(rankTitle)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int clickedSlot = event.getRawSlot();
        if (clickedSlot < 0 || clickedSlot >= event.getView().getTopInventory().getSize()) return;

        // ── Stats menu: open-command for nav, stat entries for detail ──
        if (title.contains(statsTitle)) {
            if (tryOpenCommand(player, clickedSlot)) return;
            StatType type = statFromSlot(clickedSlot);
            if (type != null) openStatDetail(player, type);

        // ── Stat Detail: open-command for back, dynamic logic for +/- ──
        } else if (title.contains(statDetailTitle)) {
            if (tryOpenCommand(player, clickedSlot)) return;
            StatType type = statFromTitle(title, statDetailTitle);
            if (type == null) return;
            int amount = amountFromSlot(clickedSlot);
            if (amount <= 0) return;
            if (isStatRemoveSlot(clickedSlot)) plugin.levels().removeStat(player, type, amount);
            else plugin.levels().addStat(player, type, amount);
            openStatDetail(player, type);

        // ── Clan/Races menu: open-command for back, dynamic for reroll/auto/nav ──
        } else if (title.contains(clanTitle)) {
            if (tryOpenCommand(player, clickedSlot)) return;
            int page = titlePage(title);
            if (isItemVisible("race-reroll") && clickedSlot == itemSlot("race-reroll", "menus.clan.items.reroll.slot", 29) && plugin.clans().reroll(player)) openClan(player, page);
            else if (isItemVisible("clan-auto") && clickedSlot == itemSlot("clan-auto", "menus.clan.items.auto.slot", 32)) {
                if (plugin.clans().autoReroll(player, clanAutoTarget(player)) != null) openClan(player, page);
            } else if (isItemVisible("clan-auto-toggle") && clickedSlot == itemSlot("clan-auto-toggle", "menus.clan.items.auto-toggle.slot", 33)) {
                clanAutoTargets.put(player.getUniqueId(), nextAutoTarget(clanAutoTarget(player)));
                openClan(player, page);
            } else if (clickedSlot == pageItemSlot("clan-nav-previous", "menus.clan.prev-slot", 28)) openClan(player, page - 1);
            else if (clickedSlot == pageItemSlot("clan-nav-next", "menus.clan.next-slot", 34)) openClan(player, page + 1);

        // ── Level menu: only page navigation ──
        } else if (title.contains(levelTitle)) {
            int page = titlePage(title);
            if (clickedSlot == pageItemSlot("level-nav-previous", "menus.level.prev-slot", 18)) openLevel(player, page - 1);
            else if (clickedSlot == pageItemSlot("level-nav-next", "menus.level.next-slot", 26)) openLevel(player, page + 1);

        // ── Guide menu: open-command only ──
        } else if (title.contains(guideTitle)) {
            tryOpenCommand(player, clickedSlot);

        // ── Guild/Clans menu: open-command for back, dynamic for reroll/auto ──
        } else if (title.contains(guildTitle)) {
            if (tryOpenCommand(player, clickedSlot)) return;
            if (isItemVisible("guild-reroll") && clickedSlot == itemSlot("guild-reroll", "menus.guild.items.reroll.slot", 29) && plugin.clans().rerollGuild(player)) openGuild(player);
            else if (isItemVisible("guild-auto") && clickedSlot == itemSlot("guild-auto", "menus.guild.items.auto.slot", 32)) {
                if (plugin.clans().autoRerollGuild(player, guildAutoTarget(player)) != null) openGuild(player);
            } else if (isItemVisible("guild-auto-toggle") && clickedSlot == itemSlot("guild-auto-toggle", "menus.guild.items.auto-toggle.slot", 33)) {
                guildAutoTargets.put(player.getUniqueId(), nextAutoTarget(guildAutoTarget(player)));
                openGuild(player);
            }

        // ── Leaderboard: open-command for back, dynamic for page nav ──
        } else if (title.contains(leaderboardTitle)) {
            if (tryOpenCommand(player, clickedSlot)) return;
            int page = titlePage(title);
            if (clickedSlot == pageItemSlot("leaderboard-nav-previous", "menus.leaderboard.prev-slot", 46)) openLeaderboard(player, page - 1);
            else if (clickedSlot == pageItemSlot("leaderboard-nav-next", "menus.leaderboard.next-slot", 52)) openLeaderboard(player, page + 1);

        // ── Rank/Classes menu: open-command for back, dynamic for reroll/auto ──
        } else if (title.contains(rankTitle)) {
            if (tryOpenCommand(player, clickedSlot)) return;
            if (isItemVisible("rank-auto-toggle") && clickedSlot == itemSlot("rank-auto-toggle", "menus.rank.items.auto-toggle.slot", 33)) {
                rankAutoTargets.put(player.getUniqueId(), nextAutoTarget(rankAutoTarget(player)));
                openRank(player);
                return;
            }
            StatType autoType = rankAutoStatFromSlot(clickedSlot);
            if (autoType != null && isItemVisible("rank-auto")) {
                if (plugin.levels().autoRerollRank(player, autoType, rankAutoTarget(player)) != null) openRank(player);
                return;
            }
            StatType type = statFromSlot(clickedSlot);
            if (type != null && plugin.levels().rerollRank(player, type)) openRank(player);
        }
    }

    /**
     * Scans ALL items in the current menu for an open-command at the clicked slot.
     * open:<id> = internal routing (open:stats, open:clan, etc.)
     * anything else = executed as player command (/warp, /shop, etc.)
     * Menu is closed after execution to avoid interfering (e.g. teleport GUIs).
     */
    private boolean tryOpenCommand(Player player, int clickedSlot) {
        String sectionPath = "menus." + currentMenuId + ".items";
        org.bukkit.configuration.ConfigurationSection itemsSection = plugin.pageConfig().getConfigurationSection(sectionPath);
        if (itemsSection == null) return false;
        for (String key : itemsSection.getKeys(false)) {
            if (!isItemVisible(key)) continue;
            String cmd = plugin.pageConfig().getString(ipath(key) + ".open-command", null);
            if (cmd == null || cmd.isBlank()) continue;
            int slot = plugin.pageConfig().getInt(ipath(key) + ".slot", -1);
            if (slot == clickedSlot) {
                player.closeInventory();
                if (cmd.startsWith("open:")) {
                    routeOpenCommand(player, cmd.substring(5));
                } else {
                    player.performCommand(cmd);
                }
                return true;
            }
        }
        return false;
    }

    /** Routes internal open:<id> commands to the correct menu. */
    private void routeOpenCommand(Player player, String target) {
        switch (target.toLowerCase(java.util.Locale.ROOT)) {
            case "stats" -> openStats(player);
            case "races", "clan" -> openClan(player, 1);
            case "guild", "clans" -> openGuild(player);
            case "rank", "classes" -> openRank(player);
            case "leaderboard" -> openLeaderboard(player);
            case "guide" -> openGuide(player);
            case "level" -> openLevel(player, 1);
            default -> plugin.getLogger().warning("Unknown open target: " + target);
        }
    }

    private void addAutoClanControls(Player player, Inventory inv, Map<String, String> vars) {
        addAutoControls(inv, clanAutoTarget(player), "clan-auto", "clan-auto-toggle", itemSlot("clan-auto", "menus.clan.items.auto.slot", 32), itemSlot("clan-auto-toggle", "menus.clan.items.auto-toggle.slot", 33), vars);
    }

    private void addAutoGuildControls(Player player, Inventory inv, Map<String, String> baseVars) {
        Map<String, String> vars = new HashMap<>(baseVars);
        vars.put("target_rarity", plugin.clans().rarityDisplay(guildAutoTarget(player)));
        vars.put("max_fragments", String.valueOf(plugin.getConfig().getInt("auto-reroll.max-fragments", 100)));
        if (isItemVisible("guild-auto")) placeItem(inv, itemSlot("guild-auto", "menus.guild.items.auto.slot", 32),
                configItem("guild-auto", t("menu.guild.auto.name", "&dAuto Clan", vars),
                tl("menu.guild.auto.lore", List.of("&7Target &f{target_rarity}", "&7Limit &f{max_fragments}"), vars)), "guild-auto");
        if (isItemVisible("guild-auto-toggle")) placeItem(inv, itemSlot("guild-auto-toggle", "menus.guild.items.auto-toggle.slot", 33),
                configItem("guild-auto-toggle", t("menu.guild.auto-toggle.name", "&b{target_rarity}", vars),
                tl("menu.guild.auto-toggle.lore", List.of("&7Rare -> Epic -> Mythic"), vars)), "guild-auto-toggle");
    }

    private void addRankAutoStatControls(Player player, Inventory inv, PlayerProfile profile) {
        int[] slots = rankAutoSlots();
        int index = 0;
        for (StatType type : StatType.values()) {
            if (index >= slots.length) break;
            Map<String, String> vars = statVars(player, profile, type);
            vars.put("target_rarity", plugin.clans().rarityDisplay(rankAutoTarget(player)));
            vars.put("max_fragments", String.valueOf(plugin.getConfig().getInt("auto-reroll.max-fragments", 100)));
            if (isItemVisible("rank-auto")) placeItem(inv, slots[index++], configItem("rank-auto", t("menu.rank.auto-stat.name", "&#FFB84DAuto {stat}", vars),
                    tl("menu.rank.auto-stat.lore", List.of(
                            "{line}",
                            "&8- &7Auto reroll khusus &f{stat}&7.",
                            "&8- &7Stop saat dapat {target_rarity}&7 atau lebih.",
                            "&8- &7Limit &f{max_fragments}",
                            "{line}",
                            "&7Start."
                    ), vars)), "rank-auto");
        }
    }

    private void addRankTargetToggle(Player player, Inventory inv, Map<String, String> vars) {
        addAutoToggle(inv, rankAutoTarget(player), "rank-auto-toggle", itemSlot("rank-auto-toggle", "menus.rank.items.auto-toggle.slot", 33), vars);
    }

    private void addAutoControls(Inventory inv, Rarity target, String autoKey, String toggleKey, int autoSlot, int toggleSlot, Map<String, String> baseVars) {
        Map<String, String> vars = new HashMap<>(baseVars);
        vars.put("target_rarity", plugin.clans().rarityDisplay(target));
        vars.put("max_fragments", String.valueOf(plugin.getConfig().getInt("auto-reroll.max-fragments", 100)));
        if (isItemVisible(autoKey)) placeItem(inv, autoSlot, configItem(autoKey, t("menu.auto-reroll.auto.name", "&dAuto Reroll", vars),
                tl("menu.auto-reroll.auto.lore", List.of(
                        "&7Pakai fragment sampai dapat {target_rarity}&7.",
                        "&7Limit: &f{max_fragments} fragment",
                        "&7Start."
                ), vars)), autoKey);
        addAutoToggle(inv, target, toggleKey, toggleSlot, baseVars);
    }

    private void addAutoToggle(Inventory inv, Rarity target, String toggleKey, int toggleSlot, Map<String, String> baseVars) {
        Map<String, String> vars = new HashMap<>(baseVars);
        vars.put("target_rarity", plugin.clans().rarityDisplay(target));
        vars.put("max_fragments", String.valueOf(plugin.getConfig().getInt("auto-reroll.max-fragments", 100)));
        if (isItemVisible(toggleKey)) placeItem(inv, toggleSlot, configItem(toggleKey, t("menu.auto-reroll.toggle.name", "&bTarget: {target_rarity}", vars),
                tl("menu.auto-reroll.toggle.lore", List.of(
                        "&7Mode stop auto reroll.",
                        "&7Next."
                ), vars)), toggleKey);
    }

    private List<String> clanLore(String clan) {
        return expandClanLore(tl("menu.clan.lore", List.of(
                "&7{clan_description}",
                "&7Chance: &f{chance}%",
                "&cDMG &f{damage}% &8| &bHP &f{health}%",
                "&9DEF &f{defense}% &8| &aHeal &f{potion_heal}%",
                "&eArrow &f{projectile}% &8| &cMelee &f{melee}%",
                "&5Night &f{night_damage}%"
        ), clanVars(clan)), clan);
    }

    private List<String> currentClanLore(String clan) {
        return expandClanLore(tl("menu.clan.current.lore", clanLore(clan), clanVars(clan)), clan);
    }

    private List<String> expandClanLore(List<String> lore, String clan) {
        List<String> expanded = new ArrayList<>();
        for (String line : lore) {
            if (line.contains("{clan_description_lines}")) {
                List<String> description = plugin.clans().descriptionLines(clan);
                if (description.isEmpty()) expanded.add(line.replace("{clan_description_lines}", ""));
                else for (String descriptionLine : description) expanded.add(line.replace("{clan_description_lines}", descriptionLine));
            } else if (line.contains("{clan_passive_lines}") || line.contains("{race_passive_lines}")) {
                for (String passiveLine : plugin.clans().passiveLines(clan)) {
                    expanded.add(line.replace("{clan_passive_lines}", passiveLine).replace("{race_passive_lines}", passiveLine));
                }
            } else {
                expanded.add(line);
            }
        }
        return expanded;
    }

    private StatType statFromSlot(int slot) {
        int[] slots = statSlots();
        for (int i = 0; i < slots.length && i < StatType.values().length; i++) {
            if (slots[i] == slot) return StatType.values()[i];
        }
        return null;
    }

    private StatType rankAutoStatFromSlot(int slot) {
        int[] slots = rankAutoSlots();
        for (int i = 0; i < slots.length && i < StatType.values().length; i++) {
            if (slots[i] == slot) return StatType.values()[i];
        }
        return null;
    }

    private Rarity clanAutoTarget(Player player) {
        return clanAutoTargets.getOrDefault(player.getUniqueId(), Rarity.RARE);
    }

    private Rarity guildAutoTarget(Player player) {
        return guildAutoTargets.getOrDefault(player.getUniqueId(), Rarity.RARE);
    }

    private Rarity rankAutoTarget(Player player) {
        return rankAutoTargets.getOrDefault(player.getUniqueId(), Rarity.RARE);
    }

    private Rarity nextAutoTarget(Rarity current) {
        return switch (current) {
            case RARE -> Rarity.EPIC;
            case EPIC -> Rarity.MYTHIC;
            default -> Rarity.RARE;
        };
    }

    private StatType statFromTitle(String title, String titlePrefix) {
        String raw = title.substring(title.indexOf(titlePrefix) + titlePrefix.length()).trim();
        StatType internal = StatType.from(raw.replace(" ", ""));
        if (internal != null) return internal;
        for (StatType type : StatType.values()) {
            if (plugin.levels().statName(type).equalsIgnoreCase(raw)) return type;
        }
        return null;
    }

    private String oneDecimal(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private String whole(double value) {
        return String.valueOf((int) Math.round(value));
    }

    private void combatVars(Map<String, String> vars, PlayerProfile profile) {
        vars.put("melee_damage", whole(plugin.damagePreview(profile, false)));
        vars.put("projectile_damage", whole(plugin.damagePreview(profile, true)));
        vars.put("unarmed_damage", whole(plugin.damagePreview(profile, StatType.POWER)));
        vars.put("melee_pierce", "0");
        vars.put("projectile_pierce", "0");
        vars.put("melee_hp_damage", whole(plugin.rawBonusDamage(profile, false)));
        vars.put("projectile_hp_damage", whole(plugin.rawBonusDamage(profile, true)));
        vars.put("unarmed_hp_damage", whole(plugin.rawBonusDamage(profile, StatType.POWER)));
        vars.put("damage_preview_target", whole(plugin.getConfig().getDouble("stats.damage.preview-target-health", 1000.0)));
        vars.put("max_health", oneDecimal(plugin.effectiveMaxHealth(profile)));
        vars.put("defense", whole(plugin.defenseBlockPerThousand(profile)));
        vars.put("potion_heal", whole(potionHealPreview(profile)));
    }

    private double potionHealPreview(PlayerProfile profile) {
        double targetHealth = plugin.getConfig().getDouble("stats.damage.preview-target-health", 1000.0);
        double bonus = plugin.levels().potionHealBonusPercent(profile);
        double base = 8.0;
        double maxHealthHealPer100 = plugin.getConfig().getDouble("stats.alchemy.max-health-heal-per-100-percent", 3.0);
        return base + (base * bonus / 100.0) + (targetHealth * (bonus / 100.0) * (Math.max(0.0, maxHealthHealPer100) / 100.0));
    }

    private String progressBar(int percent, int size) {
        int filled = (int) Math.round(Math.max(0, Math.min(100, percent)) / 100.0 * size);
        return "&#6D7890[" + "&#B8F7D4" + "|".repeat(filled) + "&#3F4B5F" + "|".repeat(Math.max(0, size - filled)) + "&#6D7890] &#F7F3FF" + Math.max(0, Math.min(100, percent)) + "%";
    }

    private int amountFromSlot(int slot) {
        return switch (slot) {
            case 10, 19 -> 1;
            case 11, 20 -> 50;
            case 12, 21 -> 100;
            case 13, 22 -> 200;
            case 14, 23 -> 500;
            case 15, 24 -> 1000;
            case 16, 25 -> 5000;
            default -> 0;
        };
    }

    private boolean isStatRemoveSlot(int slot) {
        return slot >= 10 && slot <= 16;
    }

    private int titlePage(String title) {
        String[] split = title.split(" ");
        try {
            return Integer.parseInt(split[split.length - 1]);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private Map<String, String> profileVars(PlayerProfile profile) {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", profile.name());
        vars.put("level", String.valueOf(profile.level()));
        vars.put("level_padded", String.format(java.util.Locale.US, "%02d", profile.level()));
        vars.put("exp", String.valueOf(profile.exp()));
        long needed = plugin.levels().neededExp(profile.level());
        int expPercent = needed == Long.MAX_VALUE ? 100 : (int) Math.max(0, Math.min(100, Math.floor(profile.exp() * 100.0 / Math.max(1, needed))));
        vars.put("needed_exp", needed == Long.MAX_VALUE ? "MAX" : String.valueOf(needed));
        vars.put("exp_percent", String.valueOf(expPercent));
        vars.put("exp_bar", progressBar(expPercent, 24));
        int progressPercent = plugin.levels().totalProgressPercent(profile);
        vars.put("progress_percent", String.valueOf(progressPercent));
        vars.put("progress_bar", progressBar(progressPercent, 24));
        vars.put("total_exp", String.valueOf(plugin.levels().totalExp(profile)));
        vars.put("stats_point", String.valueOf(profile.statsPoint()));
        vars.put("used_point", String.valueOf(plugin.levels().usedStats(profile)));
        vars.put("clan", plugin.clans().guildDisplayName(profile.guildClan()));
        vars.put("race", plugin.clans().displayName(profile.race()));
        combatVars(vars, profile);
        vars.put("exp_multiplier", "1.0");
        vars.put("line", plugin.langLine());
        return vars;
    }
    private Map<String, String> commonVars(Player player, PlayerProfile profile) {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", player.getName());
        vars.put("level", String.valueOf(profile.level()));
        vars.put("level_padded", String.format(java.util.Locale.US, "%02d", profile.level()));
        vars.put("exp", String.valueOf(profile.exp()));
        long needed = plugin.levels().neededExp(profile.level());
        int expPercent = needed == Long.MAX_VALUE ? 100 : (int) Math.max(0, Math.min(100, Math.floor(profile.exp() * 100.0 / Math.max(1, needed))));
        vars.put("needed_exp", needed == Long.MAX_VALUE ? "MAX" : String.valueOf(needed));
        vars.put("exp_percent", String.valueOf(expPercent));
        vars.put("exp_bar", progressBar(expPercent, 24));
        int progressPercent = plugin.levels().totalProgressPercent(profile);
        vars.put("progress_percent", String.valueOf(progressPercent));
        vars.put("progress_bar", progressBar(progressPercent, 24));
        vars.put("total_exp", String.valueOf(plugin.levels().totalExp(profile)));
        vars.put("stats_point", String.valueOf(profile.statsPoint()));
        vars.put("used_point", String.valueOf(plugin.levels().usedStats(profile)));
        vars.put("clan", plugin.clans().guildDisplayName(profile.guildClan()));
        vars.put("race", plugin.clans().displayName(profile.race()));
        combatVars(vars, profile);
        vars.put("exp_multiplier", String.format(java.util.Locale.US, "%.1f", plugin.levels().expMultiplier(player)));
        vars.put("line", plugin.langLine());
        return vars;
    }

    private Map<String, String> statVars(Player player, PlayerProfile profile, StatType type) {
        Map<String, String> vars = commonVars(player, profile);
        vars.put("stat", plugin.levels().statName(type));
        vars.put("stat_key", type.configKey());
        vars.put("stat_point", String.valueOf(profile.stat(type)));
        vars.put("stat_bonus", statEffect(profile, type));
        vars.put("stat_per_point", statPerPointEffect(profile, type));
        int pointTarget = Math.max(1, plugin.getConfig().getInt("settings.max-total-stat-points", 5000) / Math.max(1, StatType.values().length));
        int statPercent = (int) Math.max(0, Math.min(100, Math.round(profile.stat(type) * 100.0 / pointTarget)));
        vars.put("stat_percent", String.valueOf(statPercent));
        vars.put("stat_bar", progressBar(statPercent, 24));
        vars.put("rank", profile.rank(type).name());
        vars.put("rank_colored", plugin.levels().rankDisplay(profile.rank(type)));
        vars.put("rank_rarity", plugin.clans().rarityDisplay(plugin.levels().rankRarity(profile.rank(type))));
        vars.put("rank_multiplier", String.format(java.util.Locale.US, "%.2f", profile.rank(type).multiplier()));
        vars.put("rank_cost", String.valueOf(plugin.fragments().cost(FragmentService.STATS_CLASS, 1)));
        vars.put("stat_description", plugin.levels().statDescription(type));
        return vars;
    }

    private Map<String, String> leaderboardVars(PlayerProfile profile, int rank) {
        Map<String, String> vars = new HashMap<>();
        vars.put("rank", String.valueOf(rank));
        vars.put("player", profile.name());
        vars.put("level", String.valueOf(profile.level()));
        vars.put("level_padded", String.format(java.util.Locale.US, "%02d", profile.level()));
        vars.put("exp", String.valueOf(profile.exp()));
        vars.put("total_exp", String.valueOf(plugin.levels().totalExp(profile)));
        int progressPercent = plugin.levels().totalProgressPercent(profile);
        vars.put("progress_percent", String.valueOf(progressPercent));
        vars.put("progress_bar", progressBar(progressPercent, 24));
        vars.put("clan", plugin.clans().guildDisplayName(profile.guildClan()));
        vars.put("race", plugin.clans().displayName(profile.race()));
        combatVars(vars, profile);
        vars.put("alchemy", vars.get("potion_heal"));
        vars.put("defense_stat", vars.get("defense"));
        vars.put("health_stat", vars.get("max_health"));
        vars.put("fighting", vars.get("melee_damage"));
        vars.put("archery", vars.get("projectile_damage"));
        vars.put("power", vars.get("unarmed_damage"));
        vars.put("alchemy_point", String.valueOf(profile.stat(StatType.ALCHEMY)));
        vars.put("defense_point", String.valueOf(profile.stat(StatType.DEFENSE)));
        vars.put("health_point", String.valueOf(profile.stat(StatType.HEALTH)));
        vars.put("fighting_point", String.valueOf(profile.stat(StatType.FIGHTING)));
        vars.put("archery_point", String.valueOf(profile.stat(StatType.ARCHERY)));
        vars.put("power_point", String.valueOf(profile.stat(StatType.POWER)));
        vars.put("alchemy_rank", plugin.levels().rankDisplay(profile.rank(StatType.ALCHEMY)));
        vars.put("defense_rank", plugin.levels().rankDisplay(profile.rank(StatType.DEFENSE)));
        vars.put("health_rank", plugin.levels().rankDisplay(profile.rank(StatType.HEALTH)));
        vars.put("fighting_rank", plugin.levels().rankDisplay(profile.rank(StatType.FIGHTING)));
        vars.put("archery_rank", plugin.levels().rankDisplay(profile.rank(StatType.ARCHERY)));
        vars.put("power_rank", plugin.levels().rankDisplay(profile.rank(StatType.POWER)));
        vars.put("line", plugin.langLine());
        return vars;
    }

    private String statEffect(PlayerProfile profile, StatType type) {
        return switch (type) {
            case ARCHERY -> "Arrow +" + whole(plugin.damagePreview(profile, true));
            case FIGHTING -> "DMG +" + whole(plugin.damagePreview(profile, false));
            case POWER -> "Hand +" + whole(plugin.damagePreview(profile, StatType.POWER));
            case DEFENSE -> "DEF " + whole(plugin.defenseBlockPerThousand(profile));
            case HEALTH -> "HP " + oneDecimal(plugin.effectiveMaxHealth(profile));
            case ALCHEMY -> "Heal " + whole(potionHealPreview(profile));
        };
    }

    private String statPerPointEffect(PlayerProfile profile, StatType type) {
        double perPoint = plugin.levels().statBonusPerPointPercent(profile, type);
        return switch (type) {
            case ARCHERY -> "+" + oneDecimal(perPoint) + "%";
            case FIGHTING -> "+" + oneDecimal(perPoint) + "%";
            case POWER -> "+" + oneDecimal(perPoint) + "%";
            case DEFENSE -> "+" + oneDecimal(perPoint * 10.0);
            case HEALTH -> "+" + oneDecimal(plugin.getConfig().getDouble("stats.health.health-per-point", 0.02) * profile.rank(type).multiplier()) + " HP";
            case ALCHEMY -> "+" + oneDecimal(perPoint) + "%";
        };
    }

    private Map<String, String> guideVars() {
        Map<String, String> vars = new HashMap<>();
        vars.put("ender_dragon", String.valueOf(fragmentDrop("ENDER_DRAGON", FragmentService.RACES, 30)));
        vars.put("wither", String.valueOf(fragmentDrop("WITHER", FragmentService.RACES, 10)));
        vars.put("warden", String.valueOf(fragmentDrop("WARDEN", FragmentService.RACES, 6)));
        vars.put("stats_per_level", String.valueOf(plugin.levels().levelPointReward()));
        vars.put("stat_point_cap", String.valueOf(plugin.getConfig().getInt("settings.max-total-stat-points", 5000)));
        vars.put("clan_cost", String.valueOf(plugin.fragments().cost(FragmentService.CLANS, 3)));
        vars.put("race_cost", String.valueOf(plugin.fragments().cost(FragmentService.RACES, 3)));
        vars.put("rank_cost", String.valueOf(plugin.fragments().cost(FragmentService.STATS_CLASS, 1)));
        vars.put("leaderboard_size", String.valueOf(plugin.getConfig().getInt("leaderboard.size", 15)));
        vars.put("line", plugin.langLine());
        return vars;
    }

    private int fragmentDrop(String mob, String fragment, int fallback) {
        return plugin.fragmentConfig() == null
                ? fallback
                : plugin.fragmentConfig().getInt("mob-drops.mobs." + mob + "." + fragment, fallback);
    }

    private int autoGetEvery(String path, Rarity rarity) {
        if (!plugin.getConfig().getBoolean(path + ".enabled", false)) return 0;
        return plugin.getConfig().getInt(path + ".targets." + rarity.key(), 0);
    }

    private int autoGetLeft(String path, int current, Rarity rarity) {
        int every = autoGetEvery(path, rarity);
        return every <= 0 ? 0 : Math.max(0, every - current);
    }

    private Map<String, String> clanVars(String clan) {
        Map<String, String> vars = new HashMap<>();
        vars.put("clan", plugin.clans().displayName(clan));
        vars.put("race", plugin.clans().displayName(clan));
        vars.put("clan_key", clan);
        vars.put("race_key", clan);
        vars.put("clan_description", plugin.clans().description(clan));
        vars.put("clan_rarity", plugin.clans().rarityDisplay(plugin.clans().rarity(clan)));
        vars.put("race_rarity", vars.get("clan_rarity"));
        vars.put("race_chance", formatChance(plugin.clans().chance(clan)));
        vars.put("chance", formatChance(plugin.clans().chance(clan)));
        vars.put("damage", oneDecimal(plugin.clans().value(clan, "damage-percent")));
        vars.put("health", oneDecimal(plugin.clans().value(clan, "health-percent")));
        vars.put("defense", oneDecimal(plugin.clans().value(clan, "defense-percent")));
        vars.put("melee", oneDecimal(plugin.clans().value(clan, "assassin-percent")));
        vars.put("projectile", oneDecimal(plugin.clans().value(clan, "archer-percent")));
        vars.put("potion_heal", oneDecimal(plugin.clans().value(clan, "alchemy-percent")));
        vars.put("night_damage", oneDecimal(plugin.clans().value(clan, "night-damage-percent")));
        vars.put("line", plugin.langLine());
        return vars;
    }

    private Map<String, String> guildVars(String guild) {
        Map<String, String> vars = new HashMap<>();
        vars.put("clan", plugin.clans().guildDisplayName(guild));
        vars.put("clan_key", guild == null ? "NONE" : guild);
        vars.put("clan_rarity", plugin.clans().rarityDisplay(plugin.clans().guildRarity(guild)));
        vars.put("clan_chance", formatChance(plugin.clans().guildChance(guild)));
        vars.put("chance", vars.get("clan_chance"));
        vars.put("damage", oneDecimal(plugin.clans().guildValue(guild, "damage-percent")));
        vars.put("health", oneDecimal(plugin.clans().guildValue(guild, "health-percent")));
        vars.put("defense", oneDecimal(plugin.clans().guildValue(guild, "defense-percent")));
        vars.put("melee", oneDecimal(plugin.clans().guildValue(guild, "assassin-percent")));
        vars.put("projectile", oneDecimal(plugin.clans().guildValue(guild, "archer-percent")));
        vars.put("potion_heal", oneDecimal(plugin.clans().guildValue(guild, "alchemy-percent")));
        vars.put("night_damage", oneDecimal(plugin.clans().guildValue(guild, "night-damage-percent")));
        vars.put("line", plugin.langLine());
        return vars;
    }

    private String formatChance(double chance) {
        if (chance > 0.0 && chance < 0.01) {
            return String.format(java.util.Locale.US, "%.4f", chance);
        }
        return String.format(java.util.Locale.US, "%.2f", chance);
    }
    private String t(String key, String fallback) {
        String itemKey = itemTextKey(key);
        if (itemKey != null) {
            String namePath = ipath(itemKey) + ".name";
            if (plugin.pageConfig().isString(namePath)) {
                return replace(plugin.pageConfig().getString(namePath, fallback), Map.of());
            }
        }
        return replace(plugin.pageConfig().getString(key, plugin.lang(key, fallback)), Map.of());
    }

    private String t(String key, String fallback, Map<String, String> vars) {
        String itemKey = itemTextKey(key);
        if (itemKey != null) {
            String namePath = ipath(itemKey) + ".name";
            if (plugin.pageConfig().isString(namePath)) {
                return replace(plugin.pageConfig().getString(namePath, fallback), vars);
            }
        }
        return replace(plugin.pageConfig().getString(key, plugin.lang(key, fallback)), vars);
    }

    private List<String> tl(String key, List<String> fallback) {
        return tl(key, fallback, Map.of());
    }

    private List<String> tl(String key, List<String> fallback, Map<String, String> vars) {
        String itemKey = itemTextKey(key);
        List<String> raw;
        if (itemKey != null) {
            String lorePath = ipath(itemKey) + ".lore";
            raw = plugin.pageConfig().isList(lorePath)
                    ? plugin.pageConfig().getStringList(lorePath)
                    : plugin.pageConfig().isList(key) ? plugin.pageConfig().getStringList(key) : plugin.langList(key, fallback);
        } else {
            raw = plugin.pageConfig().isList(key) ? plugin.pageConfig().getStringList(key) : plugin.langList(key, fallback);
        }
        List<String> lines = new ArrayList<>();
        for (String line : raw) lines.add(replace(line, vars));
        return lines;
    }

    private String itemTextKey(String key) {
        return switch (key) {
            case "menu.nav.previous.name", "menu.nav.previous.lore" -> "nav-previous";
            case "menu.nav.next.name", "menu.nav.next.lore" -> "nav-next";
            case "menu.nav.back.name", "menu.nav.back.lore" -> "back";
            case "menu.level.level-item.name", "menu.level.level-item.lore" -> "level-item";
            case "menu.level.progress.name", "menu.level.progress.lore" -> "level-progress";
            case "menu.stats.profile.name", "menu.stats.profile.lore" -> "stats-profile";
            case "menu.stats.stat.name", "menu.stats.stat.lore" -> "stat-entry";
            case "menu.stats.clan.name", "menu.stats.clan.lore" -> "clan-menu";
            case "menu.stats.race.name", "menu.stats.race.lore" -> "race-menu";
            case "menu.stats.guild.name", "menu.stats.guild.lore" -> "guild-menu";
            case "menu.stats.rank.name", "menu.stats.rank.lore" -> "rank-menu";
            case "menu.stats.leaderboard.name", "menu.stats.leaderboard.lore" -> "leaderboard";
            case "menu.stats.guide.name", "menu.stats.guide.lore" -> "stats-guide";
            case "menu.guide.grinding.name", "menu.guide.grinding.lore" -> "guide-grinding";
            case "menu.guide.point.name", "menu.guide.point.lore" -> "guide-point";
            case "menu.guide.fragment.name", "menu.guide.fragment.lore" -> "guide-fragment";
            case "menu.guide.clan.name", "menu.guide.clan.lore" -> "guide-clan";
            case "menu.guide.rank.name", "menu.guide.rank.lore" -> "guide-rank";
            case "menu.guide.leaderboard.name", "menu.guide.leaderboard.lore" -> "guide-leaderboard";
            case "menu.stat-detail.info.name", "menu.stat-detail.info.lore" -> "stat-detail-info";
            case "menu.stat-detail.minus.name", "menu.stat-detail.minus.lore" -> "stat-minus";
            case "menu.stat-detail.plus.name", "menu.stat-detail.plus.lore" -> "stat-plus";
            case "menu.rank.info.name", "menu.rank.info.lore" -> "rank-info";
            case "menu.rank.stat.name", "menu.rank.stat.lore" -> "rank-stat";
            case "menu.rank.auto-stat.name", "menu.rank.auto-stat.lore" -> "rank-auto";
            case "menu.leaderboard.info.name", "menu.leaderboard.info.lore" -> "leaderboard-info";
            case "menu.leaderboard.entry.name", "menu.leaderboard.entry.lore" -> "leaderboard-entry";
            case "menu.clan.current.name", "menu.clan.current.lore" -> "clan-current";
            case "menu.clan.entry.name", "menu.clan.lore" -> "clan-entry";
            case "menu.clan.reroll.name", "menu.clan.reroll.lore" -> "race-reroll";
            case "menu.guild.current.name", "menu.guild.current.lore" -> "guild-current";
            case "menu.guild.entry.name", "menu.guild.lore" -> "guild-entry";
            case "menu.guild.reroll.name", "menu.guild.reroll.lore" -> "guild-reroll";
            case "menu.guild.auto.name", "menu.guild.auto.lore" -> "guild-auto";
            case "menu.guild.auto-toggle.name", "menu.guild.auto-toggle.lore" -> "guild-auto-toggle";
            case "menu.auto-reroll.auto.name", "menu.auto-reroll.auto.lore" -> "clan-auto";
            case "menu.auto-reroll.toggle.name", "menu.auto-reroll.toggle.lore" -> "clan-auto-toggle";
            default -> null;
        };
    }

    private String replace(String text, Map<String, String> vars) {
        String result = text == null ? "" : text;
        Map<String, String> all = new HashMap<>(vars);
        all.putIfAbsent("line", plugin.langLine());
        all.putIfAbsent("rank_cost", String.valueOf(plugin.fragments().cost(FragmentService.STATS_CLASS, 1)));
        for (Map.Entry<String, String> entry : all.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    private String plainTitle(String key, String fallback) {
        String title = t(key, fallback);
        int placeholder = title.indexOf('{');
        if (placeholder >= 0) title = title.substring(0, placeholder);
        return stripLegacy(Text.s(title)).trim();
    }

    private String stripLegacy(String text) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == 167 && i + 1 < text.length()) {
                i++;
                continue;
            }
            builder.append(text.charAt(i));
        }
        return builder.toString();
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (name != null && !name.isBlank()) {
            net.kyori.adventure.text.Component cmp = Text.item(name);
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(cmp);
            meta.displayName(plain.isBlank() ? net.kyori.adventure.text.Component.text(" ") : cmp);
        }
        if (lore != null && !lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> lines = new ArrayList<>();
            for (String line : lore) {
                if (line == null || line.isBlank()) continue;
                lines.add(Text.item(line));
            }
            if (!lines.isEmpty()) meta.lore(lines);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack configItem(String key, String name, List<String> lore) {
        String path = ipath(key) + ".";
        Material material = Material.matchMaterial(plugin.pageConfig().getString(path + "material", "STONE"));
        ItemStack item = item(material == null ? Material.STONE : material, name, lore);
        applyPageMeta(item, key);
        return item;
    }

    private ItemStack clanItem(String clan, String templateKey, String name, List<String> lore) {
        String path = "clans." + clan + ".";
        String fallbackPath = ipath(templateKey) + ".";
        Material material = Material.matchMaterial(plugin.getConfig().getString(path + "material", plugin.pageConfig().getString(fallbackPath + "material", "STONE")));
        ItemStack item = item(material == null ? Material.STONE : material, name, lore);
        int model = plugin.getConfig().getInt(path + "custom-model-data", plugin.pageConfig().getInt(fallbackPath + "custom-model-data", 0));
        if (model > 0) {
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(model);
            item.setItemMeta(meta);
        }
        applyPageMeta(item, templateKey);
        return item;
    }

    private ItemStack profileItem(PlayerProfile profile, String name, List<String> lore) {
        String path = ipath("stats-profile") + ".";
        Material material = Material.matchMaterial(plugin.pageConfig().getString(path + "material", "PLAYER_HEAD"));
        ItemStack item = material == Material.PLAYER_HEAD || material == null
                ? playerHead(profile, "stats-profile", name, lore)
                : item(material, name, lore);
        applyPageMeta(item, "stats-profile");
        return item;
    }

    private ItemStack playerHead(PlayerProfile profile, String templateKey, String name, List<String> lore) {
        ItemStack item = item(Material.PLAYER_HEAD, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            Player online = Bukkit.getPlayer(profile.uuid());
            OfflinePlayer offline = online == null ? Bukkit.getOfflinePlayer(profile.uuid()) : online;
            skullMeta.setOwningPlayer(offline);
            item.setItemMeta(skullMeta);
        }
        applyPageMeta(item, templateKey);
        return item;
    }

    private void applyPageMeta(ItemStack item, String key) {
        String path = ipath(key) + ".";
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        int model = plugin.pageConfig().getInt(path + "custom-model-data", 0);
        if (model > 0) meta.setCustomModelData(model);
        // Skull texture support (base64 or player name)
        if (item.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta skull) {
            String texture = plugin.pageConfig().getString(path + "skull-texture", null);
            String owner = plugin.pageConfig().getString(path + "skull-owner", null);
            if (texture != null && !texture.isBlank()) {
                setSkullTexture(skull, texture);
            } else if (owner != null && !owner.isBlank()) {
                skull.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            }
            item.setItemMeta(skull);
        }
        boolean hasGlint = plugin.pageConfig().getBoolean(path + "enchantment_glint_override",
                plugin.pageConfig().getBoolean(path + "enchanted", true));
        if (hasGlint) {
            try {
                meta.getClass().getMethod("setEnchantmentGlintOverride", Boolean.class).invoke(meta, Boolean.TRUE);
            } catch (ReflectiveOperationException e) {
                try {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } catch (NoSuchMethodError ignored) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }
        }
        item.setItemMeta(meta);
    }

    private void setSkullTexture(SkullMeta skull, String base64) {
        try {
            com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(java.util.UUID.randomUUID());
            profile.getProperties().add(new com.destroystokyo.paper.profile.ProfileProperty("textures", base64));
            skull.setPlayerProfile(profile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set skull texture: " + e.getMessage());
        }
    }

    /**
     * Checks whether a menu item should be visible.
     * Respects the "visible" field in the item config, and automatically hides items
     * whose linked module is disabled (e.g. race-menu hidden when modules.race is false).
     */
    private boolean isItemVisible(String key) {
        String path = ipath(key) + ".";
        if (!plugin.pageConfig().isSet(path + "material") && !plugin.pageConfig().isSet(path + "slot") && !plugin.pageConfig().isSet(path + "slots")) {
            return false;
        }
        if (plugin.pageConfig().isSet(path + "visible")) {
            return plugin.pageConfig().getBoolean(path + "visible");
        }
        String module = plugin.pageConfig().getString(path + "module", null);
        if (module != null) return plugin.isModuleEnabled(module);
        return true;
    }

    /**
     * Scans the current menu's items for any with a "slots" field and places them
     * as fill layers, sorted by priority (lowest first). No hardcoded key names —
     * any item with "slots" is treated as a filler layer.
     */
    private void fill(Inventory inv) {
        String sectionPath = "menus." + currentMenuId + ".items";
        org.bukkit.configuration.ConfigurationSection itemsSection = plugin.pageConfig().getConfigurationSection(sectionPath);
        if (itemsSection == null) return;

        record FillEntry(String key, int priority, java.util.List<?> rawSlots) {}
        java.util.List<FillEntry> entries = new java.util.ArrayList<>();
        for (String key : itemsSection.getKeys(false)) {
            java.util.List<?> rawSlots = plugin.pageConfig().getList(ipath(key) + ".slots");
            if (rawSlots == null || rawSlots.isEmpty()) continue;
            int priority = plugin.pageConfig().getInt(ipath(key) + ".priority", 0);
            entries.add(new FillEntry(key, priority, rawSlots));
        }
        if (entries.isEmpty()) return;

        entries.sort(java.util.Comparator.comparingInt(e -> e.priority));
        for (FillEntry entry : entries) {
            String name = plugin.pageConfig().getString(ipath(entry.key) + ".name", null);
            List<String> lore = plugin.pageConfig().getStringList(ipath(entry.key) + ".lore");
            if (name == null) name = " ";
            ItemStack fillItem = configItem(entry.key, name, lore);
            for (int slot : expandSlots(entry.rawSlots)) {
                placeItem(inv, slot, fillItem.clone(), entry.key);
            }
        }
    }

    /** Expands a mixed list of Integers and range Strings ("1-8") into a set of slots. */
    private java.util.Set<Integer> expandSlots(java.util.List<?> rawSlots) {
        java.util.Set<Integer> expanded = new java.util.LinkedHashSet<>();
        for (Object entry : rawSlots) {
            if (entry instanceof Integer i) {
                expanded.add(i);
            } else if (entry instanceof String s) {
                int dash = s.indexOf('-');
                if (dash > 0) {
                    try {
                        int from = Integer.parseInt(s.substring(0, dash).trim());
                        int to = Integer.parseInt(s.substring(dash + 1).trim());
                        for (int slot = Math.min(from, to); slot <= Math.max(from, to); slot++) {
                            expanded.add(slot);
                        }
                    } catch (NumberFormatException ignored) {}
                } else {
                    try {
                        expanded.add(Integer.parseInt(s.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return expanded;
    }

    private int slot(String path, int fallback) {
        if (path == null) return fallback;
        return plugin.pageConfig().getInt(path, fallback);
    }

    private int menuSize(String menuId, int fallback) {
        int size = plugin.pageConfig().getInt("menus." + menuId + ".size", fallback);
        size = Math.max(9, Math.min(54, size));
        return size - (size % 9);
    }

    private int itemSlot(String itemKey, String oldPath, int fallback) {
        String path = ipath(itemKey) + ".slot";
        if (plugin.pageConfig().isSet(path)) return plugin.pageConfig().getInt(path, fallback);
        return slot(oldPath, fallback);
    }

    private int pageItemSlot(String itemKey, String menuPath, int fallback) {
        String itemPath = ipath(itemKey) + ".slot";
        if (plugin.pageConfig().isSet(itemPath)) return plugin.pageConfig().getInt(itemPath, fallback);
        return slot(menuPath, fallback);
    }

    private int[] statSlots() {
        return slots("menus.stats.stat-slots", STAT_SLOTS);
    }

    private int[] rankAutoSlots() {
        return slots("menus.rank.auto-slots", RANK_AUTO_SLOTS);
    }

    private int[] slots(String path, int[] fallback) {
        List<Integer> values = plugin.pageConfig().getIntegerList(path);
        if (values.isEmpty()) return fallback;
        int[] result = new int[Math.max(fallback.length, values.size())];
        for (int i = 0; i < result.length; i++) result[i] = i < values.size() ? values.get(i) : fallback[Math.min(i, fallback.length - 1)];
        return result;
    }

    /** Reads a single slot from menu config, e.g. menus.level.progress-slot → 22 */
    private int menuSlot(String menuId, String key, int fallback) {
        return plugin.pageConfig().getInt("menus." + menuId + "." + key, fallback);
    }

    /** Reads a slot array from menu config, e.g. menus.clan.entry-slots */
    private int[] menuSlots(String menuId, String key, int[] fallback) {
        return slots("menus." + menuId + "." + key, fallback);
    }
}

