package vegas.sony.module.blatant.antibot;

import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.ModeValue;

public class AntiBotModeValue
extends ModeValue {
    public static AntiBotModeValue create(Object owner, String name, String description,
                                          ModeSelection defaultMode, ModeSelection ... modes) {
        AntiBotModeValue value = new AntiBotModeValue(owner, name, name, defaultMode, modes);
        value.setDescription(description);
        for (ModeSelection mode : modes) {
            mode.attachToMode(value);
        }
        return value;
    }

    public AntiBotModeValue(Object owner, String name, String description,
                            ModeSelection defaultMode, ModeSelection[] modes) {
        super(owner, name, description, defaultMode, modes);
    }
}
