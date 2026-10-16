package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class TextComponentTranslation
extends Wrapper {
    public Object getModelIdentity() {
        return TextComponentTranslation.sonyVegasInstance.getMappings().trackingItemStackRenderState.getModelIdentity(this.I);
    }

    public TextComponentTranslation(Object handle) {
        super(handle);
    }
}
