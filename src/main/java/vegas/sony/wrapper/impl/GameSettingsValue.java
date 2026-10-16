package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MGameSettingsValue;
import vegas.sony.wrapper.Wrapper;

public class GameSettingsValue
extends Wrapper {
    public GameSettingsValue(Object object) {
        super(object);
    }

    public void a(Object object) {
        MGameSettingsValue.S(GameSettingsValue.sonyVegasInstance.getMappingsMapperCompat().DM, this.I, object);
    }

    public void f(Object object) {
        GameSettingsValue.sonyVegasInstance.getMappingsMapperCompat().DM.b(this.I, object);
    }

    public Object i() {
        return MGameSettingsValue.U(GameSettingsValue.sonyVegasInstance.getMappingsMapperCompat().DM, this.I);
    }
}

