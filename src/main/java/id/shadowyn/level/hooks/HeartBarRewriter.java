package id.shadowyn.level.hooks;

import id.shadowyn.level.ShadowynLevelPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

/**
 * The shared math behind the two packet hooks, so ProtocolLib and PacketEvents rewrite
 * to identical values and fixes land in one place.
 *
 * <p>The rule that matters: <b>the bar may only fall when the server's health actually
 * fell.</b> A falling health value is the one and only trigger for the client's hurt
 * flash, so every phantom hit is some packet computing a momentarily lower fraction of
 * {@code health / max} while nothing was hurt. The ways that happens keep multiplying —
 * an armor modifier moves {@code max} a tick before health is rescaled to match, a heal
 * is observed mid-application, the plugin's own RPG-to-physical conversion lands between
 * two packets, float rounding dips a hair below the previous value. Chasing each source
 * individually is endless; refusing to ever send a lower value unless {@code getHealth()}
 * itself dropped kills the whole class. Real damage lowers {@code getHealth()}, so real
 * hits always show, flash included. The held value self-corrects one packet later, once
 * the server's numbers agree with themselves again.
 */
final class HeartBarRewriter {
    private final ShadowynLevelPlugin plugin;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    private static final class State {
        double health;
        double scaled;
        boolean set;
    }

    HeartBarRewriter(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
    }

    /** The health value the client should be told, replacing whatever the packet held. */
    double scaledHealth(Player player) {
        double hearts = plugin.healthDisplayHearts();
        double health = player.getHealth();
        double max = maxHealth(player);
        State state = states.computeIfAbsent(player.getUniqueId(), uuid -> new State());
        double scaled = health / max * hearts;
        // The bar may only fall when health itself fell. A lower fraction with health
        // unchanged or rising is always an artifact — max health mid-swap, a heal
        // observed mid-application, rounding — and sending it is what plays the
        // phantom hurt flash. Hold the previous value; it self-corrects next packet.
        if (state.set && scaled < state.scaled && health >= state.health - 0.01) {
            scaled = state.scaled;
        }
        // The client shows an empty bar at 0, so a living player is floored to the
        // smallest drawable sliver instead.
        scaled = Math.max(0.1, Math.min(hearts, scaled));
        state.health = health;
        state.scaled = scaled;
        state.set = true;
        return scaled;
    }

    void forget(UUID uuid) {
        states.remove(uuid);
    }

    boolean debugEnabled() {
        return plugin.getConfig().getBoolean("settings.health-display-debug", false);
    }

    void debug(String source, Player player, String message) {
        if (!debugEnabled()) return;
        plugin.getLogger().info("[heart-debug/" + source + "] " + player.getName() + ": " + message);
    }

    private double maxHealth(Player player) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = attr == null ? player.getHealth() : attr.getValue();
        return Math.max(1.0, max);
    }
}
