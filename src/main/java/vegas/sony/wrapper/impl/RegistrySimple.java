package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRegistrySimple;
import vegas.sony.wrapper.Wrapper;
import java.util.Set;

public class RegistrySimple
extends Wrapper {
    public Object S(Object object) {
        return MRegistrySimple.s(RegistrySimple.sonyVegasInstance.getMappings().hD, this.I, object);
    }

    public Set D() {
        return RegistrySimple.sonyVegasInstance.getMappings().hD.g(this.I);
    }

    public RegistrySimple(Object object) {
        super(object);
    }
}

