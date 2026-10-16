package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MChannel;
import vegas.sony.wrapper.Wrapper;

public class Channel
extends Wrapper {
    public boolean usesBlockLight() {
        return MChannel.usesBlockLight(Channel.sonyVegasInstance.getMappingsMapperCompat().bakedModel, this.I);
    }

    public Channel(Object handle) {
        super(handle);
    }
}
