package id.shadowyn.level.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedAttribute;
import id.shadowyn.level.ShadowynLevelPlugin;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * Rewrites the two outgoing packets the client builds the heart bar from, at the
 * network layer.
 *
 * <p>Everything else the plugin does about the heart bar is reactive: a fork sends the
 * client raw values, the client draws the wrong bar (and plays the hurt flash for the
 * apparent drop), and a tick later a resync repairs it. The repair cannot un-play the
 * flash. This hook removes the reactive step entirely — the packet carrying the wrong
 * number is corrected before it leaves the server, so the client never has anything
 * to flash about.
 *
 * <p>Two packets matter, and both have to agree:
 *
 * <ul>
 *   <li>{@code UPDATE_HEALTH} carries the filled amount. It is rewritten to the scaled
 *       value computed from the server-side truth ({@code getHealth} over the max-health
 *       attribute), never from what is already in the packet — so the rewrite is
 *       idempotent whether the fork sent it raw, half-scaled, or twice. Food and
 *       saturation ride in the same packet and are left untouched.</li>
 *   <li>{@code UPDATE_ATTRIBUTES} carries {@code max_health}, and this is the one that
 *       decides <em>how many hearts the client draws</em>. Bukkit's health scaling is
 *       supposed to disguise it as the scale value; the forks this hook exists for send
 *       the raw attribute instead, which is exactly the "bar shows the real hearts until
 *       something re-sends it" symptom. The player's own max-health entry is replaced
 *       with the configured heart count, modifiers stripped. Other entities' attribute
 *       packets, and every other attribute, pass through unchanged.</li>
 * </ul>
 */
public final class ProtocolLibHook extends Hook implements org.bukkit.event.Listener {

    private final HeartBarRewriter rewriter;

    @org.bukkit.event.EventHandler
    public void quit(org.bukkit.event.player.PlayerQuitEvent event) {
        rewriter.forget(event.getPlayer().getUniqueId());
    }

    public ProtocolLibHook(ShadowynLevelPlugin plugin) {
        super(plugin);
        this.rewriter = new HeartBarRewriter(plugin);
        HeartBarRewriter rewriter = this.rewriter;
        // The adapter has its own `plugin` field of the generic Plugin type, which
        // shadows ours inside the anonymous classes.
        ShadowynLevelPlugin orbit = plugin;
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.UPDATE_HEALTH) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.isPlayerTemporary()) return;
                        if (!orbit.healthDisplayScalingEnabled()) return;
                        Player player = event.getPlayer();
                        if (player == null) return;
                        float packetHealth = event.getPacket().getFloat().read(0);
                        // Death packets must pass through untouched: a death screen
                        // that never sees 0 keeps the player stuck alive client-side.
                        if (packetHealth <= 0.0f) {
                            rewriter.forget(player.getUniqueId());
                            return;
                        }
                        float scaled = (float) rewriter.scaledHealth(player);
                        event.getPacket().getFloat().write(0, scaled);
                        rewriter.debug("plib", player, "UPDATE_HEALTH " + packetHealth + " -> " + scaled);
                    }
                });
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.UPDATE_ATTRIBUTES) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.isPlayerTemporary()) return;
                        if (!orbit.healthDisplayScalingEnabled()) return;
                        Player player = event.getPlayer();
                        if (player == null) return;
                        // Only the receiver's own attributes drive their heart bar.
                        if (event.getPacket().getIntegers().read(0) != player.getEntityId()) return;
                        List<WrappedAttribute> attributes = event.getPacket().getAttributeCollectionModifier().read(0);
                        if (attributes == null || attributes.isEmpty()) return;
                        int index = -1;
                        for (int i = 0; i < attributes.size(); i++) {
                            String key = String.valueOf(attributes.get(i).getAttributeKey());
                            // "generic.max_health" on 1.21.1, "max_health" on 1.21.2+,
                            // and never "max_absorption".
                            if (key.endsWith("max_health")) {
                                index = i;
                                break;
                            }
                        }
                        if (index < 0) return;
                        rewriter.debug("plib", player, "UPDATE_ATTRIBUTES max_health "
                                + attributes.get(index).getBaseValue()
                                + " (+" + attributes.get(index).getModifiers().size() + " modifiers) -> "
                                + orbit.healthDisplayHearts());
                        // The same packet instance can be queued for several receivers
                        // (entity trackers), so mutate a clone, not the shared one.
                        PacketContainer packet = event.getPacket().deepClone();
                        List<WrappedAttribute> rewritten = new ArrayList<>(packet.getAttributeCollectionModifier().read(0));
                        rewritten.set(index, WrappedAttribute.newBuilder()
                                .packet(packet)
                                .attributeKey(String.valueOf(rewritten.get(index).getAttributeKey()))
                                .baseValue(orbit.healthDisplayHearts())
                                .modifiers(List.of())
                                .build());
                        packet.getAttributeCollectionModifier().write(0, rewritten);
                        event.setPacket(packet);
                    }
                });
        // Entity metadata also carries a raw health field (index 9 on modern versions).
        // Vanilla clients ignore it for their own HUD, but some forks broadcast it to
        // the player themselves on damage and the client applies it — the one-frame
        // flash of the real heart count on every hit. Rewrite it to the same scaled
        // value the UPDATE_HEALTH packets carry so every path agrees.
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_METADATA) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.isPlayerTemporary()) return;
                        if (!orbit.healthDisplayScalingEnabled()) return;
                        Player player = event.getPlayer();
                        if (player == null) return;
                        // Only the receiver's own metadata can repaint their heart bar.
                        if (event.getPacket().getIntegers().read(0) != player.getEntityId()) return;
                        List<com.comphenix.protocol.wrappers.WrappedDataValue> values =
                                event.getPacket().getDataValueCollectionModifier().read(0);
                        if (values == null || values.isEmpty()) return;
                        int index = -1;
                        for (int i = 0; i < values.size(); i++) {
                            var value = values.get(i);
                            if (value != null && value.getIndex() == 9 && value.getValue() instanceof Float) {
                                index = i;
                                break;
                            }
                        }
                        if (index < 0) return;
                        float raw = (Float) values.get(index).getValue();
                        // Dead is dead: the death screen needs the real zero.
                        if (raw <= 0.0f) return;
                        float scaled = (float) rewriter.scaledHealth(player);
                        // The same packet instance can be queued for several receivers
                        // (entity trackers), so mutate a clone, not the shared one.
                        PacketContainer packet = event.getPacket().deepClone();
                        List<com.comphenix.protocol.wrappers.WrappedDataValue> rewritten =
                                new ArrayList<>(packet.getDataValueCollectionModifier().read(0));
                        rewritten.get(index).setValue(scaled);
                        packet.getDataValueCollectionModifier().write(0, rewritten);
                        event.setPacket(packet);
                        rewriter.debug("plib", player, "ENTITY_METADATA health " + raw + " -> " + scaled);
                    }
                });
    }

    @Override
    public String pluginName() {
        return "ProtocolLib";
    }
}
