package vegas.sony.mapping.runtime;

import vegas.sony.mapping.mappings.MMinecraft;
import vegas.sony.mapping.mappings.MVertexFormat;
import vegas.sony.mapping.runtime.MappingOverrideSet;

public class MappingOverrideSetV35
extends MappingOverrideSet {
    private static boolean featureFlag;

    @Override
    protected void registerOverrides() {
        this.registerOverride(MMinecraft.class, MVertexFormat.class);
    }

    public static void setFeatureFlag(boolean enabled) {
        featureFlag = enabled;
    }

    public MappingOverrideSetV35(int mappingVersion) {
        super(mappingVersion);
    }


    public static boolean isFeatureFlagEnabled() {
        return featureFlag;
    }

    public static boolean isFeatureFlagDisabled() {
        boolean enabled = MappingOverrideSetV35.isFeatureFlagEnabled();
        return !enabled;
    }

    static {
        if (MappingOverrideSetV35.isFeatureFlagDisabled()) {
            MappingOverrideSetV35.setFeatureFlag(true);
        }
    }
}

