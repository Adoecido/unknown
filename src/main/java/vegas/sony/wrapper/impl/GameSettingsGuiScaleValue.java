package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class GameSettingsGuiScaleValue
extends Wrapper {
    public static int getAntialiasingLevel() {
        if (GameSettingsGuiScaleValue.sonyVegasInstance.getMappingsMapperCompat().gameSettingsGuiScaleValue == null) {
            return 0;
        }
        return GameSettingsGuiScaleValue.sonyVegasInstance.getMappingsMapperCompat().gameSettingsGuiScaleValue
                .getAntialiasingLevel();
    }


    public GameSettingsGuiScaleValue(Object valueHandle) {
        super(valueHandle);
    }
}

