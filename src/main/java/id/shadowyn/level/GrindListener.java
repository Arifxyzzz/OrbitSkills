package id.shadowyn.level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class GrindListener implements Listener {
    private final ShadowynLevelPlugin plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> shadowCharges = new HashMap<>();

    public GrindListener(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        plugin.data().get(event.getPlayer());
        plugin.applyStats(event.getPlayer());
        plugin.scheduleHealthDisplayRefresh(event.getPlayer());
    }

    @EventHandler
    public void respawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.applyStats(event.getPlayer()));
    }

    /**
     * A world change resends the player's health packets, so the fixed heart bar has to be
     * re-applied afterwards or the client falls back to one heart per real HP point.
     */
    @EventHandler
    public void changeWorld(PlayerChangedWorldEvent event) {
        plugin.scheduleHealthDisplayRefresh(event.getPlayer());
    }

    /**
     * Armor with max-health modifiers makes the server resend health when it breaks or
     * loses durability, which drops the heart scale on some forks. Only armor slots
     * matter here, so held tools are ignored to keep this off the hot path.
     */
    @EventHandler(ignoreCancelled = true)
    public void itemDamage(PlayerItemDamageEvent event) {
        if (!isArmor(event.getItem().getType())) return;
        plugin.scheduleHealthDisplayRefreshAfterEquipment(event.getPlayer());
    }

    @EventHandler
    public void itemBreak(PlayerItemBreakEvent event) {
        if (!isArmor(event.getBrokenItem().getType())) return;
        plugin.scheduleHealthDisplayRefreshAfterEquipment(event.getPlayer());
    }

    /**
     * Health Boost and Absorption change max health directly, so gaining or losing one
     * resends health the same way armor does.
     */
    @EventHandler(ignoreCancelled = true)
    public void potionEffectChange(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PotionEffectType type = event.getModifiedType();
        if (type == null) return;
        if (!type.equals(PotionEffectType.HEALTH_BOOST) && !type.equals(PotionEffectType.ABSORPTION)) return;
        plugin.scheduleHealthDisplayRefresh(player);
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || name.equals("TURTLE_HELMET")
                || name.equals("ELYTRA");
    }

    @EventHandler(ignoreCancelled = true)
    public void kill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        int exp;
        if (event.getEntity() instanceof Player) {
            exp = plugin.sourceInt("exp-sources.player-kill", 35);
        } else {
            String typeKey = event.getEntityType().name();
            String expPath = "exp-sources.mob-kill.types." + typeKey;
            if (!plugin.sourceIsSet(expPath)) expPath = "exp-sources.mob-kill.types." + typeKey.toLowerCase(java.util.Locale.ROOT);
            exp = plugin.sourceIsSet(expPath)
                    ? plugin.sourceInt(expPath, 1)
                    : event.getEntity() instanceof Monster
                    ? plugin.sourceInt("exp-sources.mob-kill.default", 9)
                    : plugin.sourceInt("exp-sources.mob-kill.passive-default", Math.max(1, plugin.sourceInt("exp-sources.mob-kill.default", 9) / 2));
            if (event.getEntity().getScoreboardTags().contains("boss") || event.getEntity().getName().toLowerCase().contains("boss")) {
                exp = plugin.sourceInt("exp-sources.mob-kill.bosses", 60);
            }
        }
        dropFragments(event);
        plugin.levels().addExp(killer, exp);
    }

    private void dropFragments(EntityDeathEvent event) {
        if (plugin.fragmentConfig() == null || !plugin.fragmentConfig().getBoolean("mob-drops.enabled", true)) return;
        String mob = event.getEntityType().name();
        ConfigurationSection drops = plugin.fragmentConfig().getConfigurationSection("mob-drops.mobs." + mob);
        if (drops == null) drops = plugin.fragmentConfig().getConfigurationSection("mob-drops.mobs." + mob.toLowerCase(java.util.Locale.ROOT));
        if (drops == null) return;
        boolean natural = plugin.fragmentConfig().getBoolean("mob-drops.drop-naturally", true);
        for (String fragment : drops.getKeys(false)) {
            int amount = Math.max(0, drops.getInt(fragment, 0));
            if (amount <= 0) continue;
            ItemStack item = plugin.fragments().create(fragment, amount);
            if (natural) event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), item);
            else if (event.getEntity().getKiller() != null) event.getEntity().getKiller().getInventory().addItem(item);
            else event.getDrops().add(item);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void mine(BlockBreakEvent event) {
        Block block = event.getBlock();
        String materialName = block.getType().name();
        String key = "exp-sources.mining." + materialName;
        if (!plugin.sourceIsSet(key)) key = "exp-sources.mining." + materialName.toLowerCase();
        boolean mining = plugin.sourceIsSet(key);
        if (!mining) {
            key = "exp-sources.woodcutting." + materialName;
            if (!plugin.sourceIsSet(key)) key = "exp-sources.woodcutting." + materialName.toLowerCase();
        }
        if (!plugin.sourceIsSet(key)) {
            key = "exp-sources.farming." + materialName;
            if (!plugin.sourceIsSet(key)) key = "exp-sources.farming." + materialName.toLowerCase();
        }
        if (!plugin.sourceIsSet(key)) {
            key = "exp-sources.digging." + materialName;
            if (!plugin.sourceIsSet(key)) key = "exp-sources.digging." + materialName.toLowerCase();
        }
        if (key.startsWith("exp-sources.farming.") && block.getBlockData() instanceof Ageable crop
                && crop.getAge() < crop.getMaximumAge()) return;
        int exp = plugin.sourceInt(key, 0);
        if (exp > 0) {
            plugin.levels().addExp(event.getPlayer(), exp);
            if (mining) tryBonusMiningDrop(event, plugin.data().get(event.getPlayer()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void harvest(PlayerHarvestBlockEvent event) {
        String materialName = event.getHarvestedBlock().getType().name();
        String key = "exp-sources.farming." + materialName;
        if (!plugin.sourceIsSet(key)) key = "exp-sources.farming." + materialName.toLowerCase();
        int exp = plugin.sourceInt(key, 0);
        if (exp > 0) plugin.levels().addExp(event.getPlayer(), exp);
    }

    private void tryBonusMiningDrop(BlockBreakEvent event, PlayerProfile profile) {
        if (!plugin.racesEnabled()) return;
        double chance = plugin.clans().value(profile.race(), "mining-bonus-drop-chance");
        if (chance <= 0 || ThreadLocalRandom.current().nextDouble(100.0) >= chance) return;
        int maxDrops = Math.max(1, plugin.getConfig().getInt("clan-effects.mining-bonus-max-drops", 1));
        int dropped = 0;
        for (ItemStack drop : event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand())) {
            if (dropped++ >= maxDrops) break;
            ItemStack copy = drop.clone();
            copy.setAmount(Math.max(1, Math.min(copy.getAmount(), plugin.getConfig().getInt("clan-effects.mining-bonus-max-stack", 3))));
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), copy);
        }
        event.getBlock().getWorld().spawnParticle(Particle.HAPPY_VILLAGER, event.getBlock().getLocation().add(0.5, 0.7, 0.5), 6, 0.25, 0.25, 0.25, 0.02);
    }

    @EventHandler(ignoreCancelled = true)
    public void fish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        String path = "exp-sources.fishing.fish";
        if (event.getCaught() instanceof Item item) {
            Material material = item.getItemStack().getType();
            if (material != Material.COD && material != Material.SALMON
                    && material != Material.PUFFERFISH && material != Material.TROPICAL_FISH) {
                path = "exp-sources.fishing.treasure";
            }
        }
        int exp = plugin.sourceInt(path, plugin.sourceInt("exp-sources.fishing.fish", 3));
        if (exp > 0) {
            PlayerProfile profile = plugin.data().get(event.getPlayer());
            double bonus = plugin.racesEnabled() ? plugin.clans().value(profile.race(), "fishing-exp-percent") : 0.0;
            int finalExp = Math.max(1, (int) Math.round(exp * (1.0 + bonus / 100.0)));
            plugin.levels().addExp(event.getPlayer(), finalExp);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void damage(EntityDamageByEntityEvent event) {
        Player damager = damager(event);
        if (damager != null) {
            boolean projectile = event.getDamager() instanceof AbstractArrow;
            event.setDamage(event.getDamage() * plugin.outgoingMultiplier(damager, projectile));
            if (event.getEntity() instanceof LivingEntity target) {
                event.setDamage(event.getDamage() + plugin.maxHealthBonusDamage(damager, target, projectile));
            }
            PlayerProfile profile = plugin.data().get(damager);
            if (plugin.racesEnabled() && plugin.raceConfig().getBoolean("clans." + profile.race() + ".fire-aspect-hit", plugin.getConfig().getBoolean("clans." + profile.race() + ".fire-aspect-hit", false))) {
                event.getEntity().setFireTicks(Math.max(event.getEntity().getFireTicks(), plugin.getConfig().getInt("clan-effects.fire-aspect-ticks", 80)));
            }
            if (plugin.racesEnabled()) applyOffensiveRaceEffects(event, damager, profile, projectile);
            // Gates per identity inside: race effects need the race module, clan effects the clan module.
            if (plugin.racesEnabled() || plugin.clansEnabled()) applyOffensiveClanEffects(event, damager, profile, projectile);
            double every = plugin.sourceDouble("exp-sources.combat.damage-exp-every", 10.0);
            int baseExp = plugin.sourceInt("exp-sources.combat.exp", 2);
            if (event.getFinalDamage() >= every) {
                int multiplier = plugin.sourceBoolean("exp-sources.combat.exp-per-threshold", true)
                        ? Math.max(1, (int) Math.floor(event.getFinalDamage() / Math.max(0.1, every)))
                        : 1;
                long exp = (long) baseExp * multiplier;
                int maxExp = plugin.sourceInt("exp-sources.combat.max-exp-per-hit", 30);
                if (maxExp > 0) exp = Math.min(exp, maxExp);
                plugin.levels().addExp(damager, exp);
            }
            if (projectile) plugin.levels().addExp(damager, plugin.sourceInt("exp-sources.archer.projectile-hit", 3));
        }
        if (event.getEntity() instanceof Player victim) {
            boolean projectile = event.getDamager() instanceof AbstractArrow;
            event.setDamage(event.getDamage() * plugin.incomingMultiplier(victim, damager, projectile));
            if (plugin.racesEnabled()) applyDefensiveRaceEffects(event, victim, damager);
            if (plugin.racesEnabled() || plugin.clansEnabled()) applyDefensiveClanEffects(event, victim, damager);
            if (damager != null) {
                event.setDamage(Math.max(event.getDamage(), minPvpHitDamage(victim, damager, projectile)));
            }
            event.setDamage(plugin.toPhysicalHealthAmount(victim, event.getDamage()));
        }
    }

    /**
     * Queues a heart-bar resync after damage.
     *
     * <p>The resync must not run inside the event: the damage has not been applied to the
     * player yet, so it would push the pre-hit health and the client would render the
     * real hit a moment later as a second, phantom one. Next tick the server values are
     * settled and the resync reports the truth.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void refreshHeartsOnDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        plugin.scheduleHealthDisplayRefresh(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void scaleEnvironmentDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;
        if (!(event.getEntity() instanceof Player player)) return;
        event.setDamage(plugin.toPhysicalHealthAmount(player, event.getDamage()));
    }

    private double minPvpHitDamage(Player victim, Player damager, boolean projectile) {
        double floorPercent = plugin.getConfig().getDouble("stats.damage.pvp-min-health-percent-per-hit", 0.0);
        if (floorPercent <= 0) return 0.0;
        double rawStatDamage = plugin.rawBonusDamage(plugin.data().get(damager), projectile);
        if (rawStatDamage <= 0) return 0.0;
        double maxHealth = plugin.effectiveMaxHealth(plugin.data().get(victim));
        return Math.min(rawStatDamage, Math.max(0.0, maxHealth * floorPercent / 100.0));
    }

    @EventHandler(ignoreCancelled = true)
    public void alchemyLowHealthRegen(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> applyAlchemyRegen(player));
    }

    private void applyAlchemyRegen(Player player) {
        if (!player.isOnline() || player.isDead()) return;
        PlayerProfile profile = plugin.data().get(player);
        double bonus = plugin.levels().potionHealBonusPercent(profile);
        if (bonus <= 0) return;
        double max = plugin.effectiveMaxHealth(profile);
        double missingPercent = (1.0 - (plugin.effectiveCurrentHealth(player) / Math.max(1.0, max))) * 100.0;
        double tier1 = plugin.getConfig().getDouble("stats.alchemy.low-health-regen-tier-1-percent", 20.0);
        double tier2 = plugin.getConfig().getDouble("stats.alchemy.low-health-regen-tier-2-percent", 50.0);
        double tier3 = plugin.getConfig().getDouble("stats.alchemy.low-health-regen-tier-3-percent", 80.0);
        if (missingPercent < tier1) return;
        if (!ready(player.getUniqueId(), "alchemy_regen", plugin.getConfig().getLong("stats.alchemy.low-health-regen-cooldown-ms", 9000))) return;
        int baseAmplifier = missingPercent >= tier3 ? 2 : missingPercent >= tier2 ? 1 : 0;
        int bonusAmplifier = (int) Math.floor(Math.min(2.0, bonus / Math.max(1.0, plugin.getConfig().getDouble("stats.alchemy.low-health-regen-bonus-per-amplifier", 220.0))));
        int amplifier = Math.min(plugin.getConfig().getInt("stats.alchemy.low-health-regen-max-amplifier", 3), baseAmplifier + bonusAmplifier);
        int ticks = plugin.getConfig().getInt("stats.alchemy.low-health-regen-base-ticks", 45)
                + (int) Math.round(Math.min(plugin.getConfig().getDouble("stats.alchemy.low-health-regen-max-extra-ticks", 55.0),
                bonus * plugin.getConfig().getDouble("stats.alchemy.low-health-regen-ticks-per-bonus-percent", 0.18)));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticks, amplifier, true, false));
        if (plugin.getConfig().getBoolean("clan-effects.alchemy-heal-visual", true)) {
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.0, 0), 10, 0.35, 0.35, 0.35, 0.02);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void shoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.racesEnabled()) return;
        PlayerProfile profile = plugin.data().get(player);
        int extra = plugin.raceConfig().getInt("clans." + profile.race() + ".extra-arrows", plugin.getConfig().getInt("clans." + profile.race() + ".extra-arrows", 0));
        if (extra <= 0 || !(event.getProjectile() instanceof AbstractArrow arrow)) return;
        double spread = Math.max(0.0, plugin.getConfig().getDouble("clan-effects.extra-arrow-spread", 0.12));
        Vector base = arrow.getVelocity();
        for (int i = 0; i < extra; i++) {
            double side = (i % 2 == 0 ? 1 : -1) * spread * ((i / 2) + 1);
            Arrow extraArrow = player.launchProjectile(Arrow.class);
            extraArrow.setVelocity(base.clone().add(new Vector(side, spread * 0.35, 0)));
            extraArrow.setDamage(arrow.getDamage() * plugin.getConfig().getDouble("clan-effects.extra-arrow-damage-multiplier", 0.45));
            extraArrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void brew(BrewEvent event) {
        if (event.getBlock().getLocation().getNearbyPlayers(6).stream().findFirst().orElse(null) instanceof Player player) {
            plugin.levels().addExp(player, plugin.sourceInt("exp-sources.alchemy.brew-potion", 8));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void enchant(EnchantItemEvent event) {
        int base = plugin.sourceInt("exp-sources.enchanting.base", 0);
        double perLevel = plugin.sourceDouble("exp-sources.enchanting.exp-per-level", 0.0);
        int max = plugin.sourceInt("exp-sources.enchanting.max-exp", 0);
        int exp = base + (int) Math.round(event.getExpLevelCost() * Math.max(0.0, perLevel));
        if (max > 0) exp = Math.min(exp, max);
        if (exp > 0) plugin.levels().addExp(event.getEnchanter(), exp);
    }

    @EventHandler(ignoreCancelled = true)
    public void smelt(FurnaceExtractEvent event) {
        int perItem = plugin.sourceInt("exp-sources.smelting.exp-per-item", 0);
        if (perItem <= 0) return;
        int exp = Math.max(0, event.getItemAmount()) * perItem;
        int max = plugin.sourceInt("exp-sources.smelting.max-exp-per-extract", 0);
        if (max > 0) exp = Math.min(exp, max);
        if (exp > 0) plugin.levels().addExp(event.getPlayer(), exp);
    }

    @EventHandler(ignoreCancelled = true)
    public void breed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;
        int exp = plugin.sourceInt("exp-sources.breeding.animal", 0);
        if (exp > 0) plugin.levels().addExp(player, exp);
    }

    @EventHandler(ignoreCancelled = true)
    public void tame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) return;
        int exp = plugin.sourceInt("exp-sources.taming.animal", 0);
        if (exp > 0) plugin.levels().addExp(player, exp);
    }

    @EventHandler(ignoreCancelled = true)
    public void shear(PlayerShearEntityEvent event) {
        int exp = plugin.sourceInt("exp-sources.shearing.entity", 0);
        if (exp > 0) plugin.levels().addExp(event.getPlayer(), exp);
    }

    @EventHandler(ignoreCancelled = true)
    public void harvestHoney(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || event.getItem() == null) return;
        Material tool = event.getItem().getType();
        if (tool != Material.GLASS_BOTTLE && tool != Material.SHEARS) return;
        if (!(event.getClickedBlock().getBlockData() instanceof org.bukkit.block.data.type.Beehive hive)
                || hive.getHoneyLevel() < hive.getMaximumHoneyLevel()) return;
        Block block = event.getClickedBlock();
        String key = "honey_" + block.getX() + "_" + block.getY() + "_" + block.getZ();
        if (!ready(block.getWorld().getUID(), key, 1000L)) return;
        int exp = plugin.sourceInt("exp-sources.beekeeping.honey-harvest", 0);
        if (exp > 0) plugin.levels().addExp(event.getPlayer(), exp);
    }

    @EventHandler
    public void advancement(PlayerAdvancementDoneEvent event) {
        var advancement = event.getAdvancement();
        var display = advancement.getDisplay();
        if (display == null) return;
        String key = advancement.getKey().getKey();
        if (key.startsWith("recipes/") || key.endsWith("/root")) return;
        String frame = display.frame().name().toLowerCase(java.util.Locale.ROOT);
        int exp = plugin.sourceInt("exp-sources.advancement." + frame, 0);
        if (exp > 0) plugin.levels().addExp(event.getPlayer(), exp);
    }

    @EventHandler
    public void raidFinish(RaidFinishEvent event) {
        int exp = plugin.sourceInt("exp-sources.raid.win", 0);
        if (exp <= 0) return;
        for (Player winner : event.getWinners()) plugin.levels().addExp(winner, exp);
    }

    @EventHandler(ignoreCancelled = true)
    public void heal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isAlchemyHeal(event.getRegainReason())) return;
        PlayerProfile profile = plugin.data().get(player);
        double bonus = plugin.levels().potionHealBonusPercent(profile);
        double base = event.getAmount();
        double sourceEfficiency = event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
                ? plugin.getConfig().getDouble("stats.alchemy.food-heal-efficiency-percent", 50.0) / 100.0
                : 1.0;
        double extra = bonus <= 0 ? 0.0 : base * bonus / 100.0 * Math.max(0.0, sourceEfficiency);
        if (bonus > 0 && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.MAGIC) {
            double maxHealthHealPer100 = plugin.getConfig().getDouble("stats.alchemy.max-health-heal-per-100-percent", 3.0);
            extra += plugin.effectiveMaxHealth(profile) * (bonus / 100.0) * (Math.max(0.0, maxHealthHealPer100) / 100.0);
            extra = Math.max(extra, plugin.getConfig().getDouble("clan-effects.alchemy-min-instant-bonus-health", 0.5));
        }
        event.setAmount(plugin.toPhysicalHealthAmount(player, base + extra));
        if (bonus > 0 && plugin.getConfig().getBoolean("clan-effects.alchemy-heal-visual", true)
                && ready(player.getUniqueId(), "alchemy_heal_visual", plugin.getConfig().getLong("clan-effects.alchemy-heal-visual-cooldown-ms", 700))) {
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.2, 0), 2, 0.35, 0.25, 0.35, 0.0);
            String source = event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED ? "Food Heal" : "Potion Heal";
            plugin.sendNoticeActionBar(player, "&a" + source + " +" + oneDecimal(extra) + " HP &8(&f" + oneDecimal(bonus) + "% Alchemy&8)");
        }
    }

    /** Queues a heart-bar resync after healing; see {@link #refreshHeartsOnDamage}. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void refreshHeartsOnHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        plugin.scheduleHealthDisplayRefresh(player);
    }

    private boolean isAlchemyHeal(EntityRegainHealthEvent.RegainReason reason) {
        return reason == EntityRegainHealthEvent.RegainReason.MAGIC
                || reason == EntityRegainHealthEvent.RegainReason.SATIATED;
    }

    @EventHandler(ignoreCancelled = true)
    public void sprint(PlayerToggleSprintEvent event) {
        if (!event.isSprinting()) return;
        Player player = event.getPlayer();
        if (!plugin.racesEnabled()) return;
        PlayerProfile profile = plugin.data().get(player);
        if (!profile.race().equalsIgnoreCase("VAMPIRE")) return;
        int duration = plugin.getConfig().getInt("clan-effects.vampire-sprint-speed-ticks", 80);
        int amplifier = plugin.getConfig().getInt("clan-effects.vampire-sprint-speed-amplifier", 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier, true, false));
    }

    private void applyOffensiveRaceEffects(EntityDamageByEntityEvent event, Player damager, PlayerProfile profile, boolean projectile) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        String race = profile.race().replaceAll("[^A-Za-z0-9_]", "").toUpperCase(java.util.Locale.ROOT);
        switch (race) {
            case "DRAGONKIN" -> dragonkinFireball(event, damager, target, projectile);
            case "DEMON" -> demonLightning(event, damager, target, projectile);
            case "ELF" -> { if (projectile) elfExplosiveArrow(event, damager, target); }
            case "ORC" -> orcBleed(event, damager, target, projectile);
            default -> {}
        }
    }

    private void dragonkinFireball(EntityDamageByEntityEvent event, Player damager, LivingEntity target, boolean projectile) {
        if (projectile || !ready(damager.getUniqueId(), "dragonkin_fireball", plugin.getConfig().getLong("race-effects.dragonkin-fireball-cooldown-ms", 5500))) return;
        target.setFireTicks(Math.max(target.getFireTicks(), plugin.getConfig().getInt("race-effects.dragonkin-fire-ticks", 100)));
        org.bukkit.entity.Fireball fireball = damager.launchProjectile(org.bukkit.entity.Fireball.class);
        fireball.setShooter(damager);
        fireball.setIsIncendiary(false);
        fireball.setYield(0);
        Vector direction = target.getLocation().toVector().subtract(damager.getEyeLocation().toVector()).normalize();
        fireball.setVelocity(direction.multiply(plugin.getConfig().getDouble("race-effects.dragonkin-fireball-speed", 1.0)));
        event.setDamage(event.getDamage() + plugin.getConfig().getDouble("race-effects.dragonkin-fireball-bonus-damage", 4.0));
        damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.5f, 1.3f);
    }

    private void demonLightning(EntityDamageByEntityEvent event, Player damager, LivingEntity target, boolean projectile) {
        if (projectile || !ready(damager.getUniqueId(), "demon_lightning", plugin.getConfig().getLong("race-effects.demon-lightning-cooldown-ms", 6000))) return;
        target.getWorld().strikeLightningEffect(target.getLocation());
        double bonusDamage = plugin.getConfig().getDouble("race-effects.demon-lightning-bonus-damage", 30.0);
        event.setDamage(event.getDamage() + bonusDamage);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, plugin.getConfig().getInt("race-effects.demon-slow-ticks", 40), 2, true, false));
        damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.0f);
    }

    private void elfExplosiveArrow(EntityDamageByEntityEvent event, Player damager, LivingEntity target) {
        if (!ready(damager.getUniqueId(), "elf_explosive", plugin.getConfig().getLong("race-effects.elf-explosive-cooldown-ms", 8000))) return;
        float power = (float) plugin.getConfig().getDouble("race-effects.elf-explosive-power", 2.0);
        target.getWorld().createExplosion(target.getLocation(), power, false, false, damager);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.2f);
        target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1, 0, 0, 0, 0);
    }

    private void orcBleed(EntityDamageByEntityEvent event, Player damager, LivingEntity target, boolean projectile) {
        if (projectile || !ready(damager.getUniqueId(), "orc_bleed", plugin.getConfig().getLong("race-effects.orc-bleed-cooldown-ms", 5000))) return;
        int ticks = plugin.getConfig().getInt("race-effects.orc-bleed-ticks", 60);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, ticks, 0, true, false));
        double bonus = plugin.getConfig().getDouble("race-effects.orc-bleed-bonus-damage", 5.0);
        event.setDamage(event.getDamage() + bonus);
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.0, 0), 12, 0.3, 0.4, 0.3, 0.04);
    }

    private void applyDefensiveRaceEffects(EntityDamageByEntityEvent event, Player victim, Player damager) {
        PlayerProfile profile = plugin.data().get(victim);
        String race = profile.race().replaceAll("[^A-Za-z0-9_]", "").toUpperCase(java.util.Locale.ROOT);
        switch (race) {
            case "TITAN" -> titanThorns(event, victim, damager);
            default -> {}
        }
    }

    private void titanThorns(EntityDamageByEntityEvent event, Player victim, Player damager) {
        if (damager == null || !ready(victim.getUniqueId(), "titan_thorns", plugin.getConfig().getLong("race-effects.titan-thorns-cooldown-ms", 4000))) return;
        double reflect = event.getFinalDamage() * plugin.getConfig().getDouble("race-effects.titan-thorns-reflect-percent", 15.0) / 100.0;
        if (reflect > 0) damager.damage(reflect, victim);
        victim.getWorld().spawnParticle(Particle.BLOCK, victim.getLocation().add(0, 1.0, 0), 18, 0.4, 0.5, 0.4, 0.02, Material.IRON_BLOCK.createBlockData());
    }

    private void applyOffensiveClanEffects(EntityDamageByEntityEvent event, Player damager, PlayerProfile profile, boolean projectile) {
        if (hasIdentity(profile, "DWARF") && isTool(damager.getInventory().getItemInMainHand().getType())) {
            double percent = plugin.getConfig().getDouble("clan-effects.dwarf-tool-damage-percent", 10.0);
            event.setDamage(event.getDamage() * (1.0 + percent / 100.0));
        }
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        for (String name : identities(profile)) {
            switch (name) {
                case "ASSASSIN" -> assassinStun(damager, target, projectile);
                case "KITSUNE" -> kitsuneSpiritTrick(event, damager, target);
                case "VAMPIRE" -> vampireLifesteal(event, damager);
                case "PHOENIX" -> phoenixFireball(event, damager, target, projectile);
                case "SHADOW" -> shadowThunder(event, damager, target, projectile);
                case "RAIKAGE" -> raikageStrike(event, damager, target, projectile);
                case "FROSTBORNE" -> frostborneChill(damager, target);
                case "ONI" -> oniCleave(event, damager, target, projectile);
                case "PLAGUE" -> plagueTouch(damager, target);
                case "STORMCALLER" -> stormcallerBolt(event, damager, target);
                case "BLAZEFURY" -> blazeFuryNova(event, damager, target, projectile);
                case "WARDENBORN" -> wardenbornRoar(event, damager, target, projectile);
                case "ASTRAL" -> astralMark(event, damager, target);
                case "NECROMANCER" -> necromancerWither(event, damager, target);
                case "GOLEM" -> golemQuake(event, damager, target, projectile);
                default -> {
                }
            }
        }
    }

    private void applyDefensiveClanEffects(EntityDamageByEntityEvent event, Player victim, Player damager) {
        PlayerProfile profile = plugin.data().get(victim);
        if (hasIdentity(profile, "DWARF") && hasArmor(victim)) {
            double reduction = plugin.getConfig().getDouble("clan-effects.dwarf-armor-reduction-percent", 8.0);
            event.setDamage(event.getDamage() * (1.0 - Math.max(0.0, reduction) / 100.0));
        }
        if (hasIdentity(profile, "PHOENIX") && damager != null && ready(victim.getUniqueId(), "phoenix_reflect", plugin.getConfig().getLong("clan-effects.phoenix-reflect-cooldown-ms", 5000))) {
            damager.setFireTicks(Math.max(damager.getFireTicks(), plugin.getConfig().getInt("clan-effects.phoenix-reflect-fire-ticks", 80)));
            double damage = plugin.getConfig().getDouble("clan-effects.phoenix-reflect-damage", 2.0);
            if (damage > 0) damager.damage(damage, victim);
        }
        if (hasIdentity(profile, "SLIME") && damager != null && ready(victim.getUniqueId(), "slime_rebound", plugin.getConfig().getLong("clan-effects.slime-rebound-cooldown-ms", 5500))) {
            double reduction = plugin.getConfig().getDouble("clan-effects.slime-rebound-reduction-percent", 28.0);
            event.setDamage(event.getDamage() * (1.0 - Math.max(0.0, reduction) / 100.0));
            Vector knock = damager.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize().multiply(0.8).setY(0.35);
            damager.setVelocity(knock);
            victim.getWorld().spawnParticle(Particle.ITEM_SLIME, victim.getLocation().add(0, 1.0, 0), 18, 0.5, 0.45, 0.5, 0.02);
        }
        if (hasIdentity(profile, "GOLEM") && hasArmor(victim)) {
            double reduction = plugin.getConfig().getDouble("clan-effects.golem-armor-reduction-percent", 10.0);
            event.setDamage(event.getDamage() * (1.0 - Math.max(0.0, reduction) / 100.0));
        }
        if (hasIdentity(profile, "AETHER")) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, plugin.getConfig().getInt("clan-effects.aether-slowfall-ticks", 60), 0, true, false));
        }
        if (hasIdentity(profile, "VOIDWALKER") && damager != null && ready(victim.getUniqueId(), "void_step", plugin.getConfig().getLong("clan-effects.void-step-cooldown-ms", 9000))) {
            event.setDamage(event.getDamage() * (1.0 - plugin.getConfig().getDouble("clan-effects.void-step-reduction-percent", 40.0) / 100.0));
            victim.getWorld().spawnParticle(Particle.PORTAL, victim.getLocation().add(0, 1.0, 0), 45, 0.5, 0.8, 0.5, 0.08);
            victim.teleport(damager.getLocation().clone().add(damager.getLocation().getDirection().multiply(-1.6)));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, plugin.getConfig().getInt("clan-effects.void-step-speed-ticks", 60), 1, true, false));
        }
    }

    private void raikageStrike(EntityDamageByEntityEvent event, Player damager, LivingEntity target, boolean projectile) {
        if (projectile || !ready(damager.getUniqueId(), "raikage_strike", plugin.getConfig().getLong("clan-effects.raikage-strike-cooldown-ms", 5000))) return;
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1.0, 0), 28, 0.45, 0.55, 0.45, 0.04);
        event.setDamage(event.getDamage() + plugin.getConfig().getDouble("clan-effects.raikage-strike-bonus-damage", 3.0));
        damager.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, plugin.getConfig().getInt("clan-effects.raikage-speed-ticks", 60), 1, true, false));
    }

    private void frostborneChill(Player damager, LivingEntity target) {
        if (!ready(damager.getUniqueId(), "frostborne_chill", plugin.getConfig().getLong("clan-effects.frostborne-chill-cooldown-ms", 6500))) return;
        int ticks = plugin.getConfig().getInt("clan-effects.frostborne-chill-ticks", 55);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 1, true, false));
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1.0, 0), 24, 0.45, 0.55, 0.45, 0.02);
    }

    private void oniCleave(EntityDamageByEntityEvent event, Player damager, LivingEntity target, boolean projectile) {
        if (projectile || !ready(damager.getUniqueId(), "oni_cleave", plugin.getConfig().getLong("clan-effects.oni-cleave-cooldown-ms", 6500))) return;
        double damage = Math.max(0.0, event.getFinalDamage() * plugin.getConfig().getDouble("clan-effects.oni-cleave-damage-percent", 35.0) / 100.0);
        double radius = plugin.getConfig().getDouble("clan-effects.oni-cleave-radius", 3.2);
        for (LivingEntity nearby : target.getLocation().getNearbyLivingEntities(radius)) {
            if (nearby.equals(target) || nearby.equals(damager)) continue;
            nearby.damage(damage, damager);
        }
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1.0, 0), 3, 0.7, 0.2, 0.7, 0.0);
    }

    private void plagueTouch(Player damager, LivingEntity target) {
        if (!ready(damager.getUniqueId(), "plague_touch", plugin.getConfig().getLong("clan-effects.plague-touch-cooldown-ms", 6000))) return;
        int ticks = plugin.getConfig().getInt("clan-effects.plague-poison-ticks", 70);
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, ticks, 0, true, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ticks, 0, true, false));
        target.getWorld().spawnParticle(Particle.SNEEZE, target.getLocation().add(0, 1.0, 0), 18, 0.5, 0.5, 0.5, 0.02);
    }

    private void stormcallerBolt(EntityDamageByEntityEvent event, Player damager, LivingEntity target) {
        if (!ready(damager.getUniqueId(), "stormcaller_bolt", plugin.getConfig().getLong("clan-effects.stormcaller-bolt-cooldown-ms", 7000))) return;
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1.0, 0), 22, 0.5, 0.6, 0.5, 0.04);
        event.setDamage(event.getDamage() + plugin.getConfig().getDouble("clan-effects.stormcaller-bolt-bonus-damage", 2.5));
    }

    private void blazeFuryNova(EntityDamageByEntityEvent event, Player damager, LivingEntity target, boolean projectile) {
        if (projectile || !ready(damager.getUniqueId(), "blazefury_nova", plugin.getConfig().getLong("clan-effects.blazefury-nova-cooldown-ms", 9000))) return;
        double radius = plugin.getConfig().getDouble("clan-effects.blazefury-nova-radius", 4.0);
        double damage = plugin.getConfig().getDouble("clan-effects.blazefury-nova-damage", 3.0);
        for (LivingEntity nearby : damager.getLocation().getNearbyLivingEntities(radius)) {
            if (nearby.equals(damager)) continue;
            nearby.setFireTicks(Math.max(nearby.getFireTicks(), plugin.getConfig().getInt("clan-effects.blazefury-fire-ticks", 80)));
            nearby.damage(damage, damager);
        }
        damager.getWorld().spawnParticle(Particle.FLAME, damager.getLocation().add(0, 1.0, 0), 70, 1.7, 0.5, 1.7, 0.04);
        damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);
    }

    private void wardenbornRoar(EntityDamageByEntityEvent event, Player damager, LivingEntity target, boolean projectile) {
        if (projectile || !ready(damager.getUniqueId(), "wardenborn_roar", plugin.getConfig().getLong("clan-effects.wardenborn-roar-cooldown-ms", 10000))) return;
        event.setDamage(event.getDamage() + plugin.getConfig().getDouble("clan-effects.wardenborn-roar-bonus-damage", 4.5));
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, plugin.getConfig().getInt("clan-effects.wardenborn-darkness-ticks", 60), 0, true, false));
        target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation().add(0, 1.0, 0), 1, 0, 0, 0, 0);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.0f);
    }

    private void astralMark(EntityDamageByEntityEvent event, Player damager, LivingEntity target) {
        if (!ready(damager.getUniqueId(), "astral_mark", plugin.getConfig().getLong("clan-effects.astral-mark-cooldown-ms", 6500))) return;
        event.setDamage(event.getDamage() + plugin.getConfig().getDouble("clan-effects.astral-mark-bonus-damage", 2.0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, plugin.getConfig().getInt("clan-effects.astral-glow-ticks", 80), 0, true, false));
        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1.2, 0), 22, 0.35, 0.45, 0.35, 0.02);
    }

    private void necromancerWither(EntityDamageByEntityEvent event, Player damager, LivingEntity target) {
        if (!ready(damager.getUniqueId(), "necromancer_wither", plugin.getConfig().getLong("clan-effects.necromancer-wither-cooldown-ms", 7500))) return;
        int ticks = plugin.getConfig().getInt("clan-effects.necromancer-wither-ticks", 70);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, ticks, 0, true, false));
        if (target instanceof Player playerTarget) commandMonsters(damager, playerTarget);
        double heal = Math.max(0.0, event.getFinalDamage() * plugin.getConfig().getDouble("clan-effects.necromancer-heal-percent", 8.0) / 100.0);
        healPlayer(damager, heal);
    }

    private void commandMonsters(Player damager, Player target) {
        if (!ready(damager.getUniqueId(), "necromancer_command", plugin.getConfig().getLong("clan-effects.necromancer-command-cooldown-ms", 16000))) return;
        double radius = plugin.getConfig().getDouble("clan-effects.necromancer-command-radius", 30.0);
        int limit = plugin.getConfig().getInt("clan-effects.necromancer-command-limit", 6);
        int controlled = 0;
        for (LivingEntity entity : target.getLocation().getNearbyLivingEntities(radius)) {
            if (controlled >= limit) break;
            if (entity instanceof Monster monster) {
                monster.setTarget(target);
                monster.getWorld().spawnParticle(Particle.SOUL, monster.getLocation().add(0, 1.0, 0), 8, 0.35, 0.35, 0.35, 0.02);
                controlled++;
            }
        }
        if (controlled > 0) plugin.sendNoticeActionBar(target, "&2Necromancer command &8- &f" + controlled + " monster mengejarmu");
    }

    private void golemQuake(EntityDamageByEntityEvent event, Player damager, LivingEntity target, boolean projectile) {
        if (projectile || !ready(damager.getUniqueId(), "golem_quake", plugin.getConfig().getLong("clan-effects.golem-quake-cooldown-ms", 9000))) return;
        double radius = plugin.getConfig().getDouble("clan-effects.golem-quake-radius", 3.0);
        for (LivingEntity entity : target.getLocation().getNearbyLivingEntities(radius)) {
            if (entity.equals(damager)) continue;
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, plugin.getConfig().getInt("clan-effects.golem-quake-slow-ticks", 45), 1, true, false));
        }
        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation(), 32, 1.1, 0.2, 1.1, 0.04, Material.STONE.createBlockData());
        event.setDamage(event.getDamage() + plugin.getConfig().getDouble("clan-effects.golem-quake-bonus-damage", 1.5));
    }

    private void assassinStun(Player damager, LivingEntity target, boolean projectile) {
        if (projectile || !ready(damager.getUniqueId(), "assassin_stun", plugin.getConfig().getLong("clan-effects.assassin-stun-cooldown-ms", 7000))) return;
        int duration = plugin.getConfig().getInt("clan-effects.assassin-stun-ticks", 35);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, plugin.getConfig().getInt("clan-effects.assassin-stun-slowness-amplifier", 5), true, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 0, true, false));
    }

    private void vampireLifesteal(EntityDamageByEntityEvent event, Player damager) {
        double percent = plugin.getConfig().getDouble("clan-effects.vampire-lifesteal-percent", 12.0);
        if (percent <= 0) return;
        double heal = event.getFinalDamage() * percent / 100.0;
        healPlayer(damager, heal);
    }

    private void healPlayer(Player player, double amount) {
        if (amount <= 0 || player.isDead()) return;
        double max = actualMaxHealth(player);
        double health = Math.max(0.0, player.getHealth());
        player.setHealth(Math.min(max, health + plugin.toPhysicalHealthAmount(player, amount)));
        plugin.scheduleHealthDisplayRefresh(player);
    }

    private double actualMaxHealth(Player player) {
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        return attr == null ? 20.0 : Math.max(0.0, attr.getValue());
    }

    private void phoenixFireball(EntityDamageByEntityEvent event, Player damager, LivingEntity target, boolean projectile) {
        if (projectile || !ready(damager.getUniqueId(), "phoenix_fireball", plugin.getConfig().getLong("clan-effects.phoenix-fireball-cooldown-ms", 6000))) return;
        target.setFireTicks(Math.max(target.getFireTicks(), plugin.getConfig().getInt("clan-effects.phoenix-fireball-fire-ticks", 100)));
        SmallFireball fireball = damager.launchProjectile(SmallFireball.class);
        fireball.setShooter(damager);
        fireball.setIsIncendiary(false);
        fireball.setYield(0);
        Vector direction = target.getLocation().toVector().subtract(damager.getEyeLocation().toVector()).normalize();
        fireball.setVelocity(direction.multiply(plugin.getConfig().getDouble("clan-effects.phoenix-fireball-speed", 1.2)));
        event.setDamage(event.getDamage() + plugin.getConfig().getDouble("clan-effects.phoenix-fireball-bonus-damage", 2.0));
    }

    private void kitsuneSpiritTrick(EntityDamageByEntityEvent event, Player damager, LivingEntity target) {
        if (!ready(damager.getUniqueId(), "kitsune_spirit_trick", plugin.getConfig().getLong("clan-effects.kitsune-spirit-cooldown-ms", 6000))) return;
        event.setDamage(event.getDamage() + plugin.getConfig().getDouble("clan-effects.kitsune-spirit-bonus-damage", 2.5));
        int debuffTicks = plugin.getConfig().getInt("clan-effects.kitsune-debuff-ticks", 45);
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, debuffTicks, 0, true, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, debuffTicks, 0, true, false));
        damager.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, plugin.getConfig().getInt("clan-effects.kitsune-speed-ticks", 70), plugin.getConfig().getInt("clan-effects.kitsune-speed-amplifier", 1), true, false));
    }

    private void shadowThunder(EntityDamageByEntityEvent event, Player damager, LivingEntity target, boolean projectile) {
        if (projectile || !plugin.getConfig().getBoolean("clan-effects.shadow-thunder-enabled", true)) return;
        int maxCharges = Math.max(1, plugin.getConfig().getInt("clan-effects.shadow-thunder-max-charges", 10));
        int charges = Math.min(maxCharges, shadowCharges.getOrDefault(damager.getUniqueId(), 0) + 1);
        shadowCharges.put(damager.getUniqueId(), charges);
        if (!ready(damager.getUniqueId(), "shadow_thunder", plugin.getConfig().getLong("clan-effects.shadow-thunder-cooldown-ms", 4500))) return;
        shadowCharges.put(damager.getUniqueId(), 0);
        double damagePerCharge = plugin.getConfig().getDouble("clan-effects.shadow-thunder-damage-per-charge", 1.2);
        for (int i = 0; i < charges; i++) {
            target.getWorld().strikeLightningEffect(target.getLocation());
        }
        event.setDamage(event.getDamage() + (charges * damagePerCharge));
        int debuffTicks = plugin.getConfig().getInt("clan-effects.shadow-debuff-ticks", 60);
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, debuffTicks, 0, true, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, debuffTicks, 0, true, false));
        damager.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, plugin.getConfig().getInt("clan-effects.shadow-buff-ticks", 80), 1, true, false));
        damager.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, plugin.getConfig().getInt("clan-effects.shadow-buff-ticks", 80), 0, true, false));
        plugin.sendNoticeActionBar(damager, "&5Shadow Thunder &8x&f" + charges);
    }

    private String normalizedClan(PlayerProfile profile) {
        return normalizeName(profile.race());
    }

    private String normalizedGuildClan(PlayerProfile profile) {
        return normalizeName(profile.guildClan());
    }

    private String normalizeName(String raw) {
        return raw == null ? "" : raw.replaceAll("[^A-Za-z0-9_]", "").toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * Names in the effect switches come from two separate pools: races (Races.yml) and
     * guild clans (Clans.yml). Matching only the race meant clan-only effects such as
     * ONI or RAIKAGE could never fire, and matching only the clan would break race-only
     * effects such as VAMPIRE lifesteal. Both identities are checked, and each is only
     * used when its own module is enabled.
     */
    private boolean hasIdentity(PlayerProfile profile, String name) {
        if (plugin.racesEnabled() && normalizedClan(profile).equals(name)) return true;
        return plugin.clansEnabled() && normalizedGuildClan(profile).equals(name);
    }

    /**
     * The race and guild clan a player currently has, skipping disabled modules, blanks,
     * and NONE. Returns one entry when both names are the same so an effect that exists
     * in both pools does not fire twice on a single hit.
     */
    private java.util.List<String> identities(PlayerProfile profile) {
        java.util.List<String> names = new java.util.ArrayList<>(2);
        if (plugin.racesEnabled()) addIdentity(names, normalizedClan(profile));
        if (plugin.clansEnabled()) addIdentity(names, normalizedGuildClan(profile));
        return names;
    }

    private void addIdentity(java.util.List<String> names, String name) {
        if (name.isEmpty() || name.equals("NONE") || names.contains(name)) return;
        names.add(name);
    }

    private String oneDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05) return String.valueOf((int) Math.round(value));
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private boolean ready(UUID uuid, String key, long cooldownMs) {
        String id = uuid + ":" + key;
        long now = System.currentTimeMillis();
        long next = cooldowns.getOrDefault(id, 0L);
        if (now < next) return false;
        cooldowns.put(id, now + Math.max(0L, cooldownMs));
        return true;
    }

    private boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE");
    }

    private boolean hasArmor(Player player) {
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) return true;
        }
        return false;
    }

    private Player damager(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof AbstractArrow arrow && arrow.getShooter() instanceof Player player) return player;
        return null;
    }
}
