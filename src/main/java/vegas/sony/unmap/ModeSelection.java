package vegas.sony.unmap;

import vegas.sony.unmap.INamed;
import vegas.sony.unmap.PropertyContainer;
import vegas.sony.value.ModeValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ModeSelection
extends PropertyContainer
implements INamed {
    private final String name;
    private static int[] legacyState;
    private ModeValue owningMode;
    public static HashMap<ModeValue, List<ModeSelection>> selectionsByMode;

    public ModeSelection(String name) {
        this.name = name;
    }

    public String getSectionSignStrippedName() {
        return vegas.sony.utils.Str.stableVisibleToken("o", this.getName());
    }

    @Override
    public String getName() {
        return this.name;
    }


    public String getSerializedName() {
        return this.toString();
    }

    public ModeValue getMode() {
        return this.owningMode;
    }

    public void attachToMode(ModeValue modeValue) {
        this.owningMode = modeValue;
        if (!selectionsByMode.containsKey(modeValue)) {
            selectionsByMode.put(modeValue, new ArrayList());
        }
        selectionsByMode.get(modeValue).add(this);
    }

    public static void setLegacyState(int[] state) {
        legacyState = state;
    }

    public String toString() {
        return this.getName();
    }

    public static ModeSelection findBySerializedName(ModeValue modeValue, String name) {
        List<ModeSelection> selections = selectionsByMode.get(modeValue);
        for (ModeSelection selection : selections) {
            // Keep profile/native mode parsing independent from the optional
            // getSerializedName helper.  Badlion can resolve an older
            // ModeSelection through its parent loader, where that method is
            // absent even though the mode itself is valid.
            String raw = selection.getName();
            // Also read profiles saved before formatting codes were stripped.
            if (!vegas.sony.utils.Str.stableVisibleToken("o", raw).equalsIgnoreCase(name)
                    && !vegas.sony.utils.Str.visibleEqualsIgnoreCase(raw, name)
                    && !raw.equalsIgnoreCase(name)) continue;
            return selection;
        }
        return null;
    }

    public boolean isSpecialSelection() {
        return false;
    }

    public static int[] getLegacyState() {
        return legacyState;
    }

    static {
        selectionsByMode = new HashMap();
        ModeSelection.setLegacyState(null);
    }
}

