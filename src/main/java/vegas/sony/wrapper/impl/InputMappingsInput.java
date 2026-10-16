package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MInputMappingsInput;
import vegas.sony.wrapper.Wrapper;

public class InputMappingsInput
extends Wrapper {
    public InputMappingsInput(Object wrappedObject) {
        super(wrappedObject);
    }

    public int getKeyCode() {
        return MInputMappingsInput.getKeyCode(InputMappingsInput.sonyVegasInstance.getMappingsMapperCompat().hm, this.I);
    }
}
