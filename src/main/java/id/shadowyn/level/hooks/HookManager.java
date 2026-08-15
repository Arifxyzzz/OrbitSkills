package id.shadowyn.level.hooks;

import id.shadowyn.level.ShadowynLevelPlugin;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

/**
 * Loads optional third-party integrations, one at a time and never fatally.
 *
 * <p>Four gates stand between a missing plugin and a broken server: the dependency is
 * compile-only so it is never shaded in, the target plugin must actually be enabled, the
 * server owner must leave the integration on in config, and construction runs inside a
 * {@code catch (Throwable)}. The last one matters most — a missing class surfaces as
 * {@link NoClassDefFoundError}, which is an {@link Error} rather than an
 * {@link Exception}, so catching {@code Exception} here would not hold. One failing hook
 * is logged and skipped; the others, and the plugin, carry on.
 */
public final class HookManager {
    private final ShadowynLevelPlugin plugin;
    private final Map<Class<? extends Hook>, Hook> hooks = new LinkedHashMap<>();

    public HookManager(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers every integration whose plugin is present and whose config toggle is on.
     * Called once on enable; safe to call again, as already-loaded hooks are skipped.
     */
    public void registerAll() {
        register(MythicMobsHook.class, "MythicMobs", MythicMobsHook::new);
        register(MmoItemsHook.class, "MMOItems", MmoItemsHook::new);
        // Both packet hooks rewrite the same two packets, so only one may be live.
        // PacketEvents goes first: it hooks the network layer in a way that survives
        // forks whose patched internals ProtocolLib's injector misses.
        register(PacketEventsHook.class, "packetevents", PacketEventsHook::new);
        if (!isRegistered(PacketEventsHook.class)) {
            register(ProtocolLibHook.class, "ProtocolLib", ProtocolLibHook::new);
        }
    }

    /**
     * Builds a hook. Separate from {@link java.util.function.Function} because a hook that
     * resolves another plugin's methods reflectively has to be allowed to fail loudly in its
     * constructor; that failure is what tells the manager to skip it.
     */
    @FunctionalInterface
    private interface HookFactory<T extends Hook> {
        T create(ShadowynLevelPlugin plugin) throws Throwable;
    }

    private <T extends Hook> void register(Class<T> type, String pluginName, HookFactory<T> factory) {
        if (hooks.containsKey(type)) return;
        if (!plugin.getConfig().getBoolean("hooks." + pluginName + ".enabled", true)) return;
        if (!Bukkit.getPluginManager().isPluginEnabled(pluginName)) return;
        try {
            T hook = factory.create(plugin);
            if (hook instanceof Listener listener) {
                Bukkit.getPluginManager().registerEvents(listener, plugin);
            }
            hooks.put(type, hook);
            plugin.getLogger().info("Hooked into " + pluginName + ".");
        } catch (Throwable ex) {
            plugin.getLogger().warning("Could not hook into " + pluginName + ": " + ex);
        }
    }

    /** Returns the hook, or {@code null} when the integration is not loaded. */
    public <T extends Hook> T get(Class<T> type) {
        Hook hook = hooks.get(type);
        return hook == null ? null : type.cast(hook);
    }

    public boolean isRegistered(Class<? extends Hook> type) {
        return hooks.containsKey(type);
    }
}
