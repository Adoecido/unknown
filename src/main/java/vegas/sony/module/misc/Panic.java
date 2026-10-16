package vegas.sony.module.misc;

import vegas.sony.SonyVegas;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import java.util.LinkedHashMap;
import java.util.Map;

/** Temporarily disables the exact module set that was active when engaged. */
public final class Panic extends Mod {
    private static volatile boolean active;
    private boolean running;
    private final Map<Mod, Boolean> previousStates = new LinkedHashMap<Mod, Boolean>();

    public Panic() {
        super("Panic //$ui", 0, Category.MISC, "Disables all modules //$ui");
    }

    public static boolean isActive() {
        return active;
    }

    @Override
    public void onEnable() {
        if (this.running) return;
        this.running = true;
        active = true;
        try {
            this.previousStates.clear();
            for (Mod module : SonyVegas.INSTANCE.getModManager().getTopLevelModules()) {
                if (module == this || module.getCategory() == Category.NONE) continue;
                this.previousStates.put(module, module.isEnabled());
            }
            SonyVegas.INSTANCE.getModManager().runWithStateNotificationsSuppressed(() -> {
                for (Map.Entry<Mod, Boolean> state : this.previousStates.entrySet()) {
                    if (!state.getValue().booleanValue()) continue;
                    try {
                        state.getKey().setEnabled(false, true);
                    } catch (Throwable error) {
                        SonyVegas.logThrowable(error);
                    }
                }
            });
        } finally {
            this.running = false;
        }
    }

    @Override
    public void onDisable() {
        if (this.running) return;
        if (this.previousStates.isEmpty()) {
            active = false;
            return;
        }
        this.running = true;
        try {
            SonyVegas.INSTANCE.getModManager().runWithStateNotificationsSuppressed(() -> {
                for (Map.Entry<Mod, Boolean> state : this.previousStates.entrySet()) {
                    Mod module = state.getKey();
                    boolean enabled = state.getValue().booleanValue();
                    if (module.isEnabled() == enabled) continue;
                    try {
                        module.setEnabled(enabled, true);
                    } catch (Throwable error) {
                        SonyVegas.logThrowable(error);
                    }
                }
            });
        } finally {
            this.previousStates.clear();
            active = false;
            this.running = false;
        }
    }
}
