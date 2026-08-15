package id.shadowyn.level;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

/**
 * Bukkit-only fallback for {@link ArmorDisplayListener}.
 *
 * <p>Spigot-based forks have no {@code PlayerArmorChangeEvent}, so without this the only
 * thing restoring the heart bar after an armor swap was the periodic sweep — up to two
 * seconds during which the client draws one heart per real HP point, and re-equipping
 * armor made the bar visibly flip between the raw and the fixed row.
 *
 * <p>Armor can move in three ways a Bukkit server still reports: a click involving the
 * armor slots (including shift-clicks from elsewhere), a right-click equip from the hand,
 * and a dispenser. Breakage is already covered by {@code GrindListener#itemBreak}. Each
 * path schedules the same next-tick resync the Paper listener uses; the per-tick dedupe
 * in the plugin means overlapping triggers cost one packet, not several.
 */
public final class LegacyArmorDisplayListener implements Listener {
    private final ShadowynLevelPlugin plugin;

    public LegacyArmorDisplayListener(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void inventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        boolean armorSlot = event.getSlotType() == InventoryType.SlotType.ARMOR;
        boolean shiftArmor = event.isShiftClick() && isArmor(event.getCurrentItem());
        if (!armorSlot && !shiftArmor) return;
        plugin.scheduleHealthDisplayRefreshAfterEquipment(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void interact(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isArmor(event.getItem())) return;
        plugin.scheduleHealthDisplayRefreshAfterEquipment(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void dispense(BlockDispenseArmorEvent event) {
        if (!(event.getTargetEntity() instanceof Player player)) return;
        plugin.scheduleHealthDisplayRefreshAfterEquipment(player);
    }

    private boolean isArmor(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || item.getType() == Material.TURTLE_HELMET
                || item.getType() == Material.ELYTRA;
    }
}
