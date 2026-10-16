package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MGameSettingsGuiScale;
import vegas.sony.wrapper.Wrapper;

public class GameSettingsGuiScale
extends Wrapper {
    public GameSettingsGuiScale(Object handle) {
        super(handle);
    }

    public Vec3 getBase() {
        return new Vec3(MGameSettingsGuiScale.getBase(GameSettingsGuiScale.sonyVegasInstance.getMappingsMapperCompat().vecDeltaCodec, this.I));
    }
}
