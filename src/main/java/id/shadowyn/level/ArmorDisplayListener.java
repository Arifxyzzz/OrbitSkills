package id.shadowyn.level;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Re-applies the fixed heart bar when armor changes.
 *
 * <p>Armor pieces can carry max-health attribute modifiers, and equipping or removing
 * one makes the server resend the player's health. Paper keeps the heart scale across
 * that resend; some forks do not, and the client briefly draws one heart per real HP
 * point instead of the configured row.
 *
 * <p>This lives in its own class because {@link PlayerArmorChangeEvent} is Paper-only.
 * If the server lacks it, registration fails for this listener alone and the rest of
 * the plugin's handlers stay active — see
 * {@link ShadowynLevelPlugin#registerArmorDisplayListener()}.
 */
public final class ArmorDisplayListener implements Listener {
    private final ShadowynLevelPlugin plugin;

    public ArmorDisplayListener(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void armorChange(PlayerArmorChangeEvent event) {
        plugin.scheduleHealthDisplayRefreshAfterEquipment(event.getPlayer());
    }
}
