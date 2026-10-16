package vegas.sony.remote;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import vegas.sony.manager.ModManager;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.ModeValue;

public final class MenuRegressionProbe {
    static final class InternalClicker extends Mod {
        InternalClicker() { super("c\u00a7rlicker", 0, Category.NONE); }
    }
    static final class PublicClicker extends Mod {
        PublicClicker() { super("Cl\u00a7ricker", 0, Category.COMBAT); }
    }

    public static void main(String[] args) throws Exception {
        ModeSelection mode = new ModeSelection("T\u00a7ra\u00a7rmpinhas Group");
        if (!vegas.sony.utils.Str.visibleEqualsIgnoreCase(
                mode.toString(), "Tampinhas Group"))
            throw new AssertionError("Segmented mode text changed visibly");
        String opaqueModeId = mode.getSectionSignStrippedName();
        if (!opaqueModeId.startsWith("o") || opaqueModeId.contains("Tampinhas"))
            throw new AssertionError("Mode identifier is readable");
        ModeValue owner = ModeValue.create(null, "Mode", mode, mode);
        if (ModeSelection.findBySerializedName(owner, "Tampinhas Group") != mode)
            throw new AssertionError("New mode serialization cannot be loaded");
        if (ModeSelection.findBySerializedName(owner, "Trarmpinhas Group") != mode)
            throw new AssertionError("Legacy mode serialization cannot be loaded");

        ModManager manager = new ModManager();
        Mod internal = new InternalClicker();
        Mod visible = new PublicClicker();
        if (Category.COMBAT.getRemoteId() != 122
                || Category.RENDER.getRemoteId() != 125) {
            throw new AssertionError("Category wire IDs changed");
        }
        if (visible.getValue("Hide from ArrayList //$ui") == null) {
            throw new AssertionError("Public module has no ArrayList visibility option");
        }
        if (internal.getValue("Hide from ArrayList //$ui") != null) {
            throw new AssertionError("Internal module exposed an ArrayList visibility option");
        }
        Map<Class<? extends Mod>, Mod> entries = new LinkedHashMap<>();
        entries.put(internal.getClass(), internal);
        entries.put(visible.getClass(), visible);
        Field field = ModManager.class.getDeclaredField("activeModulesByType");
        field.setAccessible(true);
        field.set(manager, entries);
        if (manager.getMod("Clicker") != visible || manager.getMod("c\u00a7rlicker") != visible)
            throw new AssertionError("Internal Aura clicker shadows the public clicker");
        entries.remove(visible.getClass());
        if (manager.getMod("Clicker") != internal)
            throw new AssertionError("Internal-only lookup stopped working");
        System.out.println("Menu regressions passed: formatted modes, IDs, ArrayList visibility, clicker lookup");
    }
}
