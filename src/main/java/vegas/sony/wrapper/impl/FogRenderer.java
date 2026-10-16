package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class FogRenderer
extends Wrapper {
    public FogRenderer(Object wrappedObject) {
        super(wrappedObject);
    }

    public Object getBuffer(FogType fogType) {
        return FogRenderer.sonyVegasInstance.getMappingsMapperCompat().fogRenderer.getBuffer(this.I, fogType.getObject());
    }
}
