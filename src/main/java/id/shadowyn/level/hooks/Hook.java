package id.shadowyn.level.hooks;

import id.shadowyn.level.ShadowynLevelPlugin;

/**
 * Base class for an optional integration with a third-party plugin.
 *
 * <p>Every subclass must keep its third-party imports to itself. Code outside this package
 * refers to a hook only through {@link HookManager}, which hands back {@code null} when the
 * hook is absent. That is what keeps the plugin loading on servers where MythicMobs or
 * MMOItems are not installed: the class is never touched, so its missing types are never
 * resolved.
 */
public abstract class Hook {
    protected final ShadowynLevelPlugin plugin;

    protected Hook(ShadowynLevelPlugin plugin) {
        this.plugin = plugin;
    }

    /** Name shown in the startup log line. */
    public abstract String pluginName();
}
