package vegas.sony.mapping.runtime;

import vegas.sony.mapping.runtime.MappingOverrideSet;
import vegas.sony.mapping.runtime.MappingOverrideSetV35;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MappingOverrideRegistry {
    private static MappingOverrideSet activeOverrideSet;


    private static MappingOverrideSet createOverrideSet() {
        MappingOverrideSet overrideSet = null;
        switch (ForgeVersion.c()) {
            case 35: {
                overrideSet = new MappingOverrideSetV35(35);
            }
        }
        return overrideSet;
    }

    public static MappingOverrideSet getActiveOverrideSet() {
        if (activeOverrideSet == null) {
            activeOverrideSet = MappingOverrideRegistry.createOverrideSet();
        }
        return activeOverrideSet;
    }
}

