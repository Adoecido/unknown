package vegas.sony.config;

import vegas.sony.module.combat.velocity.VelocityPacketReceiveMode;
import vegas.sony.wrapper.impl.ForgeVersion;

public class BuiltinProfileState
extends BuiltinProfile {
    private static final String PROFILE_NAME = "Classic PVP";

    @Override
    protected void configureModules() {
        this.selectModule(VelocityPacketReceiveMode.class);
    }

    @Override
    public boolean isApplicable() {
        return !ForgeVersion.MC_1_21_0.d();
    }

    public BuiltinProfileState() {
        super(PROFILE_NAME);
    }

}

