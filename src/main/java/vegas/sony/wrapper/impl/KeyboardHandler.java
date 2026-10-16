package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MKeyboardHandler;
import vegas.sony.wrapper.Wrapper;

public class KeyboardHandler
extends Wrapper {
    public KeyboardHandler(Object object) {
        super(object);
    }

    public String F() {
        return MKeyboardHandler.X(KeyboardHandler.sonyVegasInstance.getMappingsMapperCompat().CR, this.I);
    }

    public void y(String string) {
        MKeyboardHandler.f(KeyboardHandler.sonyVegasInstance.getMappingsMapperCompat().CR, this.I, string);
    }
}

