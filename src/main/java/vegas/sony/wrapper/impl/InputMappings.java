package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MInputMappings;
import vegas.sony.wrapper.Wrapper;

public class InputMappings
extends Wrapper {
    public static InputMappings S(int n, int n2) {
        return new InputMappings(MInputMappings.k(InputMappings.sonyVegasInstance.getMappingsMapperCompat().CW, n, n2));
    }

    public InputMappings(Object object) {
        super(object);
    }
}

