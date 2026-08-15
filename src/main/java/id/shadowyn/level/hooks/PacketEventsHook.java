package id.shadowyn.level.hooks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlaySendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateHealth;
import id.shadowyn.level.ShadowynLevelPlugin;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * PacketEvents twin of {@link ProtocolLibHook}: locks the client's heart bar at the
 * network layer by rewriting the two packets it is built from.
 *
 * <p>{@code UPDATE_HEALTH} carries the filled amount and is rewritten to the scaled
 * value computed from server-side truth, never from the packet's own number, so the
 * rewrite is idempotent however mangled the fork's packet was. {@code UPDATE_ATTRIBUTES}
 * carries {@code max_health}, which is what decides how many hearts the client draws;
 * the receiving player's own entry is replaced with the configured heart count and its
 * modifiers dropped. Other entities and other attributes pass through untouched.
 *
 * <p>This exists alongside the ProtocolLib hook because some Spigot forks patch their
 * network internals in ways ProtocolLib's injector misses, while PacketEvents hooks in
 * differently and survives. Only one of the two is registered — see
 * {@link HookManager#registerAll()} — so the packets are never rewritten twice.
 */
public final class PacketEventsHook extends Hook implements org.bukkit.event.Listener {
    private final HeartBarRewriter rewriter;

    @org.bukkit.event.EventHandler
    public void quit(org.bukkit.event.player.PlayerQuitEvent event) {
        rewriter.forget(event.getPlayer().getUniqueId());
    }

    public PacketEventsHook(ShadowynLevelPlugin plugin) {
        super(plugin);
        this.rewriter = new HeartBarRewriter(plugin);
        PacketEvents.getAPI().getEventManager().registerListener(
                new SimplePacketListenerAbstract(PacketListenerPriority.HIGHEST) {
                    @Override
                    public void onPacketPlaySend(PacketPlaySendEvent event) {
                        if (event.getPacketType() == PacketType.Play.Server.UPDATE_HEALTH) {
                            rewriteHealth(event);
                        } else if (event.getPacketType() == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
                            rewriteAttributes(event);
                        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                            rewriteMetadata(event);
                        }
                    }
                });
    }

    private void rewriteHealth(PacketSendEvent event) {
        if (!plugin.healthDisplayScalingEnabled()) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        WrapperPlayServerUpdateHealth wrapper = new WrapperPlayServerUpdateHealth(event);
        // Death packets pass through untouched: a death screen that never sees 0
        // keeps the player stuck alive client-side.
        if (wrapper.getHealth() <= 0.0f) {
            rewriter.forget(player.getUniqueId());
            return;
        }
        float before = wrapper.getHealth();
        float scaled = (float) rewriter.scaledHealth(player);
        wrapper.setHealth(scaled);
        event.markForReEncode(true);
        rewriter.debug("pe", player, "UPDATE_HEALTH " + before + " -> " + scaled);
    }

    /**
     * Entity metadata also carries a raw health field (index 9 on modern versions).
     * Vanilla clients ignore it for their own HUD, but some forks broadcast it to the
     * player themselves on damage and the client applies it — the one-frame flash of
     * the real heart count on every hit. Rewrite it to the same scaled value the
     * UPDATE_HEALTH packets carry so every path agrees.
     */
    private void rewriteMetadata(PacketSendEvent event) {
        if (!plugin.healthDisplayScalingEnabled()) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata wrapper =
                new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata(event);
        // Only the receiver's own metadata can repaint their heart bar.
        if (wrapper.getEntityId() != player.getEntityId()) return;
        boolean changed = false;
        for (com.github.retrooper.packetevents.protocol.entity.data.EntityData<?> data : wrapper.getEntityMetadata()) {
            if (data.getIndex() != 9) continue;
            Object value = data.getValue();
            if (!(value instanceof Float raw)) continue;
            // Dead is dead: the death screen needs the real zero.
            if (raw <= 0.0f) continue;
            float scaled = (float) rewriter.scaledHealth(player);
            @SuppressWarnings("unchecked")
            com.github.retrooper.packetevents.protocol.entity.data.EntityData<Float> healthData =
                    (com.github.retrooper.packetevents.protocol.entity.data.EntityData<Float>) data;
            healthData.setValue(scaled);
            changed = true;
            rewriter.debug("pe", player, "ENTITY_METADATA health " + raw + " -> " + scaled);
        }
        if (changed) event.markForReEncode(true);
    }

    private void rewriteAttributes(PacketSendEvent event) {
        if (!plugin.healthDisplayScalingEnabled()) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        WrapperPlayServerUpdateAttributes wrapper = new WrapperPlayServerUpdateAttributes(event);
        // Only the receiver's own attributes drive their heart bar.
        if (wrapper.getEntityId() != player.getEntityId()) return;
        boolean changed = false;
        for (WrapperPlayServerUpdateAttributes.Property property : wrapper.getProperties()) {
            // "generic.max_health" up to 1.21.1, "max_health" from 1.21.2 — and never
            // "max_absorption".
            String key = String.valueOf(property.getAttribute().getName());
            if (!key.endsWith("max_health")) continue;
            rewriter.debug("pe", player, "UPDATE_ATTRIBUTES max_health " + property.getValue()
                    + " (+" + property.getModifiers().size() + " modifiers) -> " + plugin.healthDisplayHearts());
            property.setValue(plugin.healthDisplayHearts());
            property.setModifiers(new java.util.ArrayList<>(List.of()));
            changed = true;
        }
        if (changed) event.markForReEncode(true);
    }

    @Override
    public String pluginName() {
        return "packetevents";
    }
}
