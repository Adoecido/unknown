package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;
import java.util.List;

public class GlStateManagerFogStateBridge
extends Wrapper {
    public List<String> getItemStates() {
        return (List<String>)GlStateManagerFogStateBridge.sonyVegasInstance.getMappingsMapperCompat().q2.getItemStates(this.I);
    }

    public GlStateManagerFogStateBridge(Object wrappedObject) {
        super(wrappedObject);
    }
}
