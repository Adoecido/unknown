package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class InputMappingsInputFactory
extends Wrapper {
    public static InputMappingsInputFactory create(int keyCode, int scanCode, int modifiers) {
        return new InputMappingsInputFactory(InputMappingsInputFactory.sonyVegasInstance.getMappingsMapperCompat().Du.createKeyEvent(keyCode, scanCode, modifiers));
    }

    public InputMappingsInputFactory(Object handle) {
        super(handle);
    }
}
