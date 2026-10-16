package vegas.sony.config;

import vegas.sony.wrapper.impl.ForgeVersion;

public class Minecraft121BuiltinProfile
extends BuiltinProfile {
    private static final String PROFILE_NAME = "Modern PVP";

    @Override
    protected void configureModules() {
    }

    public Minecraft121BuiltinProfile() {
        super(PROFILE_NAME);
    }

    @Override
    public boolean isApplicable() {
        return ForgeVersion.MC_1_21_0.d();
    }
}
