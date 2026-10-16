package vegas.sony.manager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sun.misc.Unsafe;
import vegas.sony.module.Mod;

/** No game, native code, filesystem cleanup, or real module side effects. */
public final class PanicProbe {
    private static class FakeMod extends Mod {
        boolean active;
        boolean broken;
        int disables;
        Runnable duringDisable;
        FakeMod() { super("unused"); }
        @Override public boolean isEnabled() { return active; }
        @Override public boolean isVisible() { return false; }
        @Override public void setEnabled(boolean enabled, boolean bypass) {
            if (!bypass) throw new AssertionError("Panic transitions must bypass visibility");
            if (!enabled) ++disables;
            if (broken) throw new IllegalStateException("Simulated transition failure");
            active = enabled;
            if (!enabled && duringDisable != null) duringDisable.run();
        }
    }
    private static class Manager extends ModManager {
        List<Mod> modules;
        @Override public Collection<Mod> collectMods() { return modules; }
    }
    public static void main(String[] args) throws Exception {
        Field access = Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        Unsafe unsafe = (Unsafe) access.get(null);
        Manager manager = (Manager) unsafe.allocateInstance(Manager.class);
        manager.modules = new ArrayList<Mod>();
        FakeMod first = (FakeMod) unsafe.allocateInstance(FakeMod.class);
        FakeMod broken = (FakeMod) unsafe.allocateInstance(FakeMod.class);
        FakeMod last = (FakeMod) unsafe.allocateInstance(FakeMod.class);
        first.active = broken.active = last.active = true;
        broken.broken = true;
        manager.modules.add(first);
        manager.modules.add(broken);
        manager.modules.add(last);
        Field suppression = ModManager.class.getDeclaredField("suppressStateNotifications");
        suppression.setAccessible(true);
        suppression.setBoolean(manager, true);
        require(manager.disableAllModules() == 1, "Failure count incorrect");
        require(!first.active && !last.active && last.disables == 1, "One failure prevented later modules from disabling");
        require(suppression.getBoolean(manager), "Lost prior notification suppression");
        broken.broken = false;
        suppression.setBoolean(manager, false);
        require(manager.disableAllModules() == 0 && !suppression.getBoolean(manager), "Suppression not restored");
        require(first.disables == 1, "Already disabled module toggled again");
        first.active = true;
        last.active = true;
        broken.active = false;
        Map<Mod, Boolean> snapshot = new LinkedHashMap<Mod, Boolean>();
        for (Mod module : manager.modules) snapshot.put(module, module.isEnabled());
        manager.runWithStateNotificationsSuppressed(() -> {
            for (Map.Entry<Mod, Boolean> state : snapshot.entrySet()) {
                if (state.getValue()) state.getKey().setEnabled(false, true);
            }
        });
        require(!first.active && !last.active, "Panic transition did not disable active modules");
        broken.active = true; // A module enabled while Panic is active.
        manager.runWithStateNotificationsSuppressed(() -> {
            for (Map.Entry<Mod, Boolean> state : snapshot.entrySet()) {
                if (state.getKey().isEnabled() != state.getValue()) {
                    state.getKey().setEnabled(state.getValue(), true);
                }
            }
        });
        require(first.active && last.active && !broken.active,
                "Panic did not restore the exact captured state");
        require(!suppression.getBoolean(manager), "Snapshot restore leaked notification suppression");
        System.out.println("PASS: disable-all failure isolation and exact Panic snapshot restoration");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
