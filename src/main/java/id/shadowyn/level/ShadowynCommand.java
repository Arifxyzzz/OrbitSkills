package id.shadowyn.level;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class ShadowynCommand implements CommandExecutor, TabCompleter {
    private final ShadowynLevelPlugin plugin;

    public ShadowynCommand(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("orbitskills") && openPublicMenu(sender, args)) {
            return true;
        }
        if (!hasAdmin(sender)) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (command.getName().equalsIgnoreCase("resetdata")) {
            return resetData(sender, args);
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        if (args.length == 0) {
            String line = "&#6D7890â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”";
            sender.sendMessage(Text.s(line));
            sender.sendMessage(Text.s("&#C7B8FFâœ¦ &#F7F3FFOriginSkills Help"));
            sender.sendMessage(Text.s(line));
            sender.sendMessage(Text.s("&#FFD6A5Give:"));
            sender.sendMessage(Text.s("  &#8FA0B7/os give point <player|*> <amount>"));
            sender.sendMessage(Text.s("  &#8FA0B7/os give level <player|*> <amount>"));
            sender.sendMessage(Text.s("  &#8FA0B7/os give exp <player|*> <amount>"));
            sender.sendMessage(Text.s("  &#8FA0B7/os give fragment <race|class|clan> <player|*> <amount>"));
            sender.sendMessage(Text.s("&#FFD6A5Set:"));
            sender.sendMessage(Text.s("  &#8FA0B7/os set level <player> <value>"));
            sender.sendMessage(Text.s("  &#8FA0B7/os set exp <player> <value>"));
            sender.sendMessage(Text.s("  &#8FA0B7/os set point <player> <value>"));
            sender.sendMessage(Text.s("  &#8FA0B7/os set race <player> <race>"));
            sender.sendMessage(Text.s("  &#8FA0B7/os set clan <player> <clan>"));
            sender.sendMessage(Text.s("  &#8FA0B7/os set class [player] <stat> <F-SSS>"));
            sender.sendMessage(Text.s("  &#8FA0B7/os disable <stats|class|race|clan|all>"));
            sender.sendMessage(Text.s("  &#8FA0B7/os enable <stats|class|race|clan|all>"));
            sender.sendMessage(Text.s("&#FFD6A5Tools:"));
            sender.sendMessage(Text.s("  &#8FA0B7/os scanall [fix] &8â€” &7Check SP issues"));
            sender.sendMessage(Text.s("  &#8FA0B7/os reload &8â€” &7Reload all configs"));
            sender.sendMessage(Text.s("  &#8FA0B7/os placeholder &8â€” &7List placeholders"));
            sender.sendMessage(Text.s("  &#8FA0B7/os resetdata <player> &8â€” &7Wipe player data"));
            sender.sendMessage(Text.s(line));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.loadConfigDefaults();
            plugin.loadLevelConfig();
            plugin.loadLangConfig();
            plugin.loadPageConfig();
            plugin.loadSourceConfigs();
            sender.sendMessage(plugin.msg("reload"));
            return true;
        }
        if (args[0].equalsIgnoreCase("placeholder")) {
            sender.sendMessage(Text.s("&#C7B8FFOriginSkills Placeholders:"));
            placeholders().forEach(p -> sender.sendMessage(Text.s("&7- &f" + p)));
            return true;
        }
        if (args[0].equalsIgnoreCase("resetdata")) {
            return resetData(sender, args.length >= 2 ? new String[] {args[1]} : new String[0]);
        }
        if (args[0].equalsIgnoreCase("scanall")) {
            return scanAll(sender, args);
        }
        if (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) {
            return toggleModule(sender, args, args[0].equalsIgnoreCase("enable"));
        }
        if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("add")) {
            return give(sender, args);
        }
        if (args[0].equalsIgnoreCase("set")) {
            return set(sender, args);
        }
        if (args.length < 3) {
            sender.sendMessage(Text.s("&cUsage: /os " + args[0] + " <player> <amount>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Text.s("&cPlayer not found."));
            return true;
        }
        int amount = parseInt(args[2], 0);
        PlayerProfile profile = plugin.data().get(target);
        switch (args[0].toLowerCase()) {
            case "givepoint" -> {
                profile.statsPoint(profile.statsPoint() + amount);
                sender.sendMessage(Text.s("&aGave " + amount + " StatsPoint to " + target.getName() + "."));
            }
            case "givefragment" -> {
                target.getInventory().addItem(plugin.fragments().create(FragmentService.RACES, amount));
                sender.sendMessage(Text.s("&aGave " + amount + " Races fragment to " + target.getName() + "."));
            }
            case "addexp" -> {
                plugin.levels().addExp(target, amount, false);
                sender.sendMessage(Text.s("&aAdded EXP."));
            }
            default -> sender.sendMessage(Text.s("&cUnknown subcommand."));
        }
        return true;
    }

    private boolean openPublicMenu(CommandSender sender, String[] args) {
        if (args.length == 0 || !(sender instanceof Player player)) return false;
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "class", "classes", "rank", "ranks" -> plugin.menus().openRank(player);
            case "clan", "clans", "guild", "guilds" -> plugin.menus().openGuild(player);
            case "leaderboard", "leaderboards", "top" -> plugin.menus().openLeaderboard(player);
            case "race", "races", "ras", "rass" -> plugin.menus().openClan(player, 1);
            case "guide", "help-menu" -> plugin.menus().openGuide(player);
            default -> {
                return false;
            }
        }
        return true;
    }
    private void sendHelp(CommandSender sender) {
        String line = "&#6D7890â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”â”";
        sender.sendMessage(Text.s(line));
        sender.sendMessage(Text.s("&#C7B8FFâœ¦ &#F7F3FFOriginSkills Help"));
        sender.sendMessage(Text.s(line));
        sender.sendMessage(Text.s("&#FFD6A5â—† Give"));
        sender.sendMessage(Text.s("&#6D7890â€º &#A7E8FF/os give point <player|*> <amount> &#8FA0B7- Add SP"));
        sender.sendMessage(Text.s("&#6D7890â€º &#A7E8FF/os give level <player|*> <amount> &#8FA0B7- Add level"));
        sender.sendMessage(Text.s("&#6D7890â€º &#A7E8FF/os give exp <player|*> <amount> &#8FA0B7- Add EXP"));
        sender.sendMessage(Text.s("&#6D7890â€º &#A7E8FF/os give fragment <id> <player|*> <amount> &#8FA0B7- Give fragment"));
        sender.sendMessage(Text.s("&#FFD6A5â—† Set"));
        sender.sendMessage(Text.s("&#6D7890â€º &#B8F7D4/os set level <player> <value> &#8FA0B7- Set level + SP"));
        sender.sendMessage(Text.s("&#6D7890â€º &#B8F7D4/os set point <player> <value> &#8FA0B7- Set total SP"));
        sender.sendMessage(Text.s("&#6D7890â€º &#B8F7D4/os set race <player> <race> &#8FA0B7- Set race"));
        sender.sendMessage(Text.s("&#6D7890â€º &#B8F7D4/os set clan <player> <clan> &#8FA0B7- Set clan"));
        sender.sendMessage(Text.s("&#6D7890â€º &#B8F7D4/os set class [player] <stat> <F-SSS> &#8FA0B7- Set class rank"));
        sender.sendMessage(Text.s("&#FFD6A5â—† Control"));
        sender.sendMessage(Text.s("&#6D7890â€º &#FFB6C8/os disable <stats|class|race|clan|all> &#8FA0B7- Lock module"));
        sender.sendMessage(Text.s("&#6D7890â€º &#B8F7D4/os enable <stats|class|race|clan|all> &#8FA0B7- Unlock module"));
        sender.sendMessage(Text.s("&#6D7890â€º &#C7B8FF/os scanall [fix] &#8FA0B7- Check SP issues"));
        sender.sendMessage(Text.s("&#6D7890â€º &#C7B8FF/os reload &#8FA0B7- Reload files"));
        sender.sendMessage(Text.s("&#6D7890â€º &#C7B8FF/os placeholder &#8FA0B7- Show placeholders"));
        sender.sendMessage(Text.s("&#6D7890â€º &#C7B8FF/os resetdata <player> &#8FA0B7- Reset profile"));
        sender.sendMessage(Text.s(line));
    }

    private boolean toggleModule(CommandSender sender, String[] args, boolean enabled) {
        if (args.length < 2) {
            sender.sendMessage(Text.s("&cUsage: /os " + args[0].toLowerCase() + " <stats|class|race|clan|all>"));
            return true;
        }
        List<String> modules = modules(args[1]);
        if (modules.isEmpty()) {
            sender.sendMessage(Text.s("&cUnknown module. Use stats, class, race, clan, or all."));
            return true;
        }
        for (String module : modules) plugin.getConfig().set("modules." + module, enabled);
        plugin.saveConfig();
        for (Player player : Bukkit.getOnlinePlayers()) plugin.applyStats(player);
        sender.sendMessage(Text.s((enabled ? "&#B8F7D4Enabled " : "&#FFB6C8Disabled ")
                + "&#F7F3FF" + String.join(", ", modules)));
        return true;
    }

    private List<String> modules(String raw) {
        return switch (raw.toLowerCase()) {
            case "all", "*" -> List.of("stats", "class", "race", "clan");
            case "stat", "stats", "point", "points" -> List.of("stats");
            case "class", "classes", "rank", "ranks" -> List.of("class");
            case "race", "races", "ras", "rass" -> List.of("race");
            case "clan", "clans", "guild", "guilds" -> List.of("clan");
            default -> List.of();
        };
    }

    private boolean set(CommandSender sender, String[] args) {
        if (args.length >= 2 && (args[1].equalsIgnoreCase("stats")
                || args[1].equalsIgnoreCase("rank")
                || args[1].equalsIgnoreCase("class"))) {
            return setStatRank(sender, args);
        }
        if (args.length < 4) {
            sender.sendMessage(Text.s("&cUsage: /os set <level|exp|point|race|clan> <player> <value>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        PlayerProfile profile = target == null ? plugin.data().findByName(args[2]) : plugin.data().get(target);
        if (profile == null) {
            sender.sendMessage(Text.s("&cPlayer data not found."));
            return true;
        }
        String targetName = target == null ? profile.name() : target.getName();
        int amount = parseInt(args[3], 0);
        switch (args[1].toLowerCase()) {
            case "level", "lvl" -> {
                profile.level(Math.min(plugin.maxLevel(), Math.max(1, amount)));
                profile.exp(0);
                plugin.levels().syncLevelStats(profile);
                if (target != null) plugin.applyStats(target);
                plugin.data().save();
                int used = plugin.levels().usedStats(profile);
                sender.sendMessage(Text.s("&aSet level of " + targetName + " to " + profile.level()
                        + " &7(SP " + (profile.statsPoint() + used) + ", available " + profile.statsPoint() + ")."));
            }
            case "exp" -> {
                profile.exp(amount);
                plugin.data().save();
                sender.sendMessage(Text.s("&aSet EXP of " + targetName + " to " + profile.exp() + "."));
            }
            case "point", "points", "statpoint", "statpoints" -> {
                if (target != null) {
                    plugin.levels().setTotalStats(target, amount);
                } else {
                    plugin.levels().setTotalStats(profile, amount);
                    plugin.data().save();
                }
                int used = plugin.levels().usedStats(profile);
                sender.sendMessage(Text.s("&aSet total StatsPoint of " + targetName + " to " + (profile.statsPoint() + used)
                        + " &7(available " + profile.statsPoint() + ", used " + used + ")."));
            }
            case "race", "ras" -> {
                String clan = findClan(args[3]);
                if (clan == null) {
                    sender.sendMessage(Text.s("&cRace not found. Use tab complete to see available races."));
                    return true;
                }
                profile.race(clan);
                if (plugin.clans().rarity(clan) == Rarity.MYTHIC) profile.clanRerollsSinceMythic(0);
                if (target != null) plugin.applyStats(target);
                plugin.data().save();
                sender.sendMessage(Text.s("&aSet race " + targetName + " to " + plugin.clans().displayName(clan) + "&a."));
            }
            case "clan", "guild" -> {
                String clan = findGuildClan(args[3]);
                if (clan == null) {
                    sender.sendMessage(Text.s("&cClan not found. Use tab complete to see available clans."));
                    return true;
                }
                profile.guildClan(clan);
                if (target != null) plugin.applyStats(target);
                plugin.data().save();
                sender.sendMessage(Text.s("&aSet clan " + targetName + " to " + plugin.clans().guildDisplayName(clan) + "&a."));
            }
            default -> sender.sendMessage(Text.s("&cUnknown set type. Use level, exp, point, race, or clan."));
        }
        return true;
    }

    private boolean setStatRank(CommandSender sender, String[] args) {
        Player target;
        PlayerProfile profile;
        String statRaw;
        String rankRaw;

        if (args.length == 4) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Text.s("&cConsole must specify a player: /os set class <player> <stat> <F-SSS>"));
                return true;
            }
            target = player;
            profile = plugin.data().get(player);
            statRaw = args[2];
            rankRaw = args[3];
        } else if (args.length >= 5) {
            target = Bukkit.getPlayerExact(args[2]);
            profile = target == null ? plugin.data().findByName(args[2]) : plugin.data().get(target);
            if (profile == null) {
                sender.sendMessage(Text.s("&cPlayer data not found."));
                return true;
            }
            statRaw = args[3];
            rankRaw = args[4];
        } else {
            sender.sendMessage(Text.s("&cUsage: /os set class [player] <stat> <F-SSS>"));
            return true;
        }

        StatType type = StatType.from(statRaw);
        if (type == null) {
            sender.sendMessage(Text.s("&cStat not found. Choose: alchemy, defense, health, fighting, archery, power."));
            return true;
        }
        StatRank rank = findRank(rankRaw);
        if (rank == null) {
            sender.sendMessage(Text.s("&cInvalid class. Choose: F, D, C, B, A, S, SS, SSS."));
            return true;
        }

        profile.rank(type, rank);
        if (target != null) plugin.applyStats(target);
        plugin.data().save();
        String targetName = target == null ? profile.name() : target.getName();
        sender.sendMessage(Text.s("&aSet class " + plugin.levels().statName(type) + " of " + targetName
                + " to " + plugin.levels().rankDisplay(rank) + "&a."));
        return true;
    }

    private boolean scanAll(CommandSender sender, String[] args) {
        boolean fix = args.length >= 2 && args[1].equalsIgnoreCase("fix");
        List<LevelService.PointScanResult> results = plugin.levels().scanUnusualPoints(fix);
        if (results.isEmpty()) {
            sender.sendMessage(Text.s("&aScan complete. No unusual point totals found."));
            return true;
        }
            sender.sendMessage(Text.s((fix ? "&aFixed " : "&eFound ") + results.size() + " player with unusual point totals:"));
        int shown = 0;
        for (LevelService.PointScanResult result : results) {
            if (shown++ >= 10) break;
            sender.sendMessage(Text.s("&7- &f" + result.player() + " &8Lv " + result.level()
                    + " &7issue &e" + result.issue()
                    + " &7total &c" + result.total() + " &7valid &a" + result.minimum() + "&7-&a" + result.maximum()
                    + " &8(used " + result.used() + ", available " + result.available() + ")"));
        }
        if (results.size() > shown) {
            sender.sendMessage(Text.s("&7... dan " + (results.size() - shown) + " data lain."));
        }
        if (!fix) {
            sender.sendMessage(Text.s("&eUse &f/os scanall fix &eto repair every result."));
        }
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Text.s("&cUsage: /os give <point|level|exp> <player|*> <amount> &7or &c/os give fragment <fragment> <player|*> <amount>"));
            return true;
        }
        if (args[1].equalsIgnoreCase("fragment") || args[1].equalsIgnoreCase("fragments")) {
            return giveFragment(sender, args);
        }
        List<Player> targets = giveTargets(args[2]);
        if (targets.isEmpty()) {
            sender.sendMessage(Text.s("&cPlayer not found."));
            return true;
        }
        int amount = parseInt(args[3], 0);
        if (amount <= 0) {
            sender.sendMessage(Text.s("&cAmount must be greater than 0."));
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "point", "points", "statpoint", "statpoints" -> {
                for (Player target : targets) {
                    PlayerProfile profile = plugin.data().get(target);
                    profile.statsPoint(profile.statsPoint() + amount);
                }
                plugin.data().save();
                sender.sendMessage(Text.s("&aAdded " + amount + " StatsPoint to " + targetSummary(targets) + "."));
            }
            case "level", "levels", "lvl", "lv" -> {
                int totalGiven = 0;
                for (Player target : targets) {
                    totalGiven += plugin.levels().giveLevels(target, amount);
                }
                plugin.data().save();
                sender.sendMessage(Text.s("&aGave up to " + amount + " level to " + targetSummary(targets)
                        + " &8(total level gained " + totalGiven + ")."));
            }
            case "fragment", "fragments", "clanfragment", "clanfragments", "clan", "racefragment", "racefragments", "race" -> {
                String fragment = args[1].toLowerCase().contains("class") || args[1].toLowerCase().contains("rank")
                        ? FragmentService.STATS_CLASS
                        : args[1].toLowerCase().contains("clan") || args[1].toLowerCase().contains("guild")
                        ? FragmentService.CLANS
                        : FragmentService.RACES;
                for (Player target : targets) target.getInventory().addItem(plugin.fragments().create(fragment, amount));
                sender.sendMessage(Text.s("&aAdded " + amount + " " + plugin.fragments().resolveId(fragment) + " fragment to " + targetSummary(targets) + "."));
            }
            case "rankfragment", "rankfragments", "rank", "classfragment", "classfragments", "class" -> {
                for (Player target : targets) target.getInventory().addItem(plugin.fragments().create(FragmentService.STATS_CLASS, amount));
                sender.sendMessage(Text.s("&aAdded " + amount + " Stats_Class fragment to " + targetSummary(targets) + "."));
            }
            case "exp" -> {
                for (Player target : targets) plugin.levels().addExp(target, amount, false);
                sender.sendMessage(Text.s("&aAdded " + amount + " EXP to " + targetSummary(targets) + "."));
            }
            default -> sender.sendMessage(Text.s("&cUnknown give type. Use point, level, fragment, or exp."));
        }
        return true;
    }

    private boolean giveFragment(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Text.s("&cUsage: /os give fragment <fragment> <player|*> <amount>"));
            return true;
        }
        List<Player> targets = giveTargets(args[3]);
        if (targets.isEmpty()) {
            sender.sendMessage(Text.s("&cPlayer not found."));
            return true;
        }
        int amount = parseInt(args[4], 0);
        if (amount <= 0) {
            sender.sendMessage(Text.s("&cAmount must be greater than 0."));
            return true;
        }
        String fragment = plugin.fragments().resolveId(args[2]);
        for (Player target : targets) target.getInventory().addItem(plugin.fragments().create(fragment, amount));
        sender.sendMessage(Text.s("&aAdded " + amount + " " + fragment + " fragment to " + targetSummary(targets) + "."));
        return true;
    }

    private boolean hasAdmin(CommandSender sender) {
        return sender.hasPermission("orbitskills.admin") || sender.hasPermission("shadowyn.admin");
    }

    private List<Player> giveTargets(String raw) {
        if (raw.equals("*")) return new ArrayList<>(Bukkit.getOnlinePlayers());
        Player target = Bukkit.getPlayerExact(raw);
        return target == null ? List.of() : List.of(target);
    }

    private String targetSummary(List<Player> targets) {
        if (targets.size() == 1) return targets.get(0).getName();
        return targets.size() + " online players";
    }

    private boolean resetData(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Text.s("&cUsage: /resetdata <player>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null) {
            plugin.data().reset(target);
            plugin.applyStats(target);
            sender.sendMessage(Text.s("&aReset data for " + target.getName() + "."));
            return true;
        }
        if (plugin.data().resetByName(args[0])) {
            sender.sendMessage(Text.s("&aReset offline data for " + args[0] + "."));
        } else {
            sender.sendMessage(Text.s("&cPlayer data not found."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("resetdata") && args.length == 1) {
            return filter(onlinePlayers(), args[0]);
        }
        if (args.length == 1) return filter(List.of("class", "clan", "leaderboard", "race", "guide", "help", "give", "set", "enable", "disable", "scanall", "resetdata", "placeholder", "reload"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("scanall")) return filter(List.of("fix"), args[1]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable"))) return filter(List.of("stats", "class", "race", "clan", "all"), args[1]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("add"))) return filter(List.of("point", "level", "fragment", "exp"), args[1]);
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("add")) && args[1].equalsIgnoreCase("fragment")) return filter(plugin.fragments().ids(), args[2]);
        if (args.length == 4 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("add")) && args[1].equalsIgnoreCase("fragment")) return filter(onlinePlayersWithAll(), args[3]);
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("add"))) return filter(onlinePlayersWithAll(), args[2]);
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) return filter(List.of("level", "exp", "point", "race", "clan", "class", "stats"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("set") && isClassArg(args[1])) {
            List<String> values = new ArrayList<>(statNames());
            values.addAll(onlinePlayers());
            return filter(values, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("set") && isClassArg(args[1])) {
            return StatType.from(args[2]) == null ? filter(statNames(), args[3]) : filter(rankNames(), args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("set") && isClassArg(args[1])) return filter(rankNames(), args[4]);
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) return filter(onlinePlayers(), args[2]);
        if (args.length == 4 && args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("race")) return filter(plugin.clans().keys(), args[3]);
        if (args.length == 4 && args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("clan")) return filter(plugin.clans().guildKeys(), args[3]);
        if (args.length == 2 && !args[0].equalsIgnoreCase("placeholder") && !args[0].equalsIgnoreCase("reload")) {
            return filter(onlinePlayers(), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        if (prefix == null || prefix.isBlank()) return values;
        String lower = prefix.toLowerCase(java.util.Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(java.util.Locale.ROOT).startsWith(lower))
                .toList();
    }

    private List<String> onlinePlayers() {
        List<String> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) players.add(player.getName());
        return players;
    }

    private List<String> onlinePlayersWithAll() {
        List<String> players = new ArrayList<>();
        players.add("*");
        players.addAll(onlinePlayers());
        return players;
    }

    private String findClan(String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (String clan : plugin.clans().keys()) {
            if (clan.equalsIgnoreCase(raw)) return clan;
        }
        String lower = raw.toLowerCase(java.util.Locale.ROOT);
        return plugin.clans().keys().stream()
                .filter(clan -> clan.toLowerCase(java.util.Locale.ROOT).startsWith(lower))
                .findFirst()
                .orElse(null);
    }

    private String findGuildClan(String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (String clan : plugin.clans().guildKeys()) {
            if (clan.equalsIgnoreCase(raw)) return clan;
        }
        String lower = raw.toLowerCase(java.util.Locale.ROOT);
        return plugin.clans().guildKeys().stream()
                .filter(clan -> clan.toLowerCase(java.util.Locale.ROOT).startsWith(lower))
                .findFirst()
                .orElse(null);
    }

    private StatRank findRank(String raw) {
        if (raw == null) return null;
        for (StatRank rank : StatRank.values()) {
            if (rank.name().equalsIgnoreCase(raw)) return rank;
        }
        return null;
    }

    private List<String> statNames() {
        return List.of("alchemy", "defense", "health", "fighting", "archery", "power");
    }

    private List<String> rankNames() {
        return List.of("F", "D", "C", "B", "A", "S", "SS", "SSS");
    }

    private boolean isClassArg(String raw) {
        return raw.equalsIgnoreCase("stats") || raw.equalsIgnoreCase("rank") || raw.equalsIgnoreCase("class");
    }

    private List<String> placeholders() {
        return List.of(
                "%orbitskills_level%", "%orbitskills_exp%", "%orbitskills_exp_needed%", "%orbitskills_exp_percent%",
                "%orbitskills_level_raw%", "%orbitskills_level_row_2%", "%orbitskills_level_row_3%", "%orbitskills_total_exp%",
                "%orbitskills_progress_percent%",
                "%orbitskills_statspoint%", "%orbitskills_race%", "%orbitskills_clan%", "%orbitskills_power%", "%orbitskills_health%",
                "%orbitskills_stats_perlevel%",
                "%orbitskills_resistance%", "%orbitskills_assassin%", "%orbitskills_archer%", "%orbitskills_alchemy%",
                "%orbitskills_damage_bonus%", "%orbitskills_health_bonus%", "%orbitskills_resistance_bonus%"
        );
    }
    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

