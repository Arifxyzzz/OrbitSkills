package id.shadowyn.level.hooks;

import id.shadowyn.level.ShadowynLevelPlugin;
import java.lang.reflect.Method;
import org.bukkit.inventory.ItemStack;

/**
 * Reads MMOItems item identity off an {@link ItemStack}.
 *
 * <p>Unlike MythicMobs, this one cannot be done with Bukkit alone. MMOItems writes
 * {@code MMOITEMS_ITEM_ID} and {@code MMOITEMS_ITEM_TYPE} at the root of the item's custom
 * data, while Bukkit's persistent data container lives in a {@code PublicBukkitValues}
 * sub-compound below it. The tags are neighbours, not the same place, so the only supported
 * way in is MMOItems' own static readers.
 *
 * <p>They are called reflectively so the plugin never carries an MMOItems dependency and
 * never fails to link without one. Both methods are resolved once here in the constructor —
 * if either is missing, construction fails and {@link HookManager} skips the whole hook
 * rather than letting every later call throw.
 *
 * <p>What this deliberately does not do is read MMOItems' stats. OrbitSkills has its own
 * stat and levelling model; borrowing another plugin's numbers would mean two systems
 * fighting over the same damage. All that is taken here is identity — which item this is,
 * and what kind of thing it is — and OrbitSkills decides for itself what that is worth.
 */
public final class MmoItemsHook extends Hook {
    private final Method getId;
    private final Method getTypeName;

    public MmoItemsHook(ShadowynLevelPlugin plugin) throws ReflectiveOperationException {
        super(plugin);
        Class<?> api = Class.forName("net.Indyuce.mmoitems.MMOItems");
        this.getId = api.getMethod("getID", ItemStack.class);
        this.getTypeName = api.getMethod("getTypeName", ItemStack.class);
    }

    @Override
    public String pluginName() {
        return "MMOItems";
    }

    public boolean isCustomItem(ItemStack item) {
        return itemId(item) != null;
    }

    /** Returns the MMOItems id of the item, or {@code null} for a plain vanilla one. */
    public String itemId(ItemStack item) {
        return invoke(getId, item);
    }

    /**
     * Returns the MMOItems type name — {@code SWORD}, {@code BOW}, {@code STAFF} and so on —
     * or {@code null} when the item has none.
     */
    public String itemType(ItemStack item) {
        return invoke(getTypeName, item);
    }

    private String invoke(Method method, ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        try {
            Object result = method.invoke(null, item);
            if (!(result instanceof String text) || text.isBlank()) return null;
            return text;
        } catch (Throwable ignored) {
            // A single odd item is not worth breaking the hit that asked about it.
            return null;
        }
    }
}
