package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ModelManager
extends Wrapper {
    public TextureAtlas getAtlas(ResourceLocation location) {
        return new TextureAtlas(ModelManager.sonyVegasInstance.getMappingsMapperCompat().CZ.getAtlas(this.I, location.getObject()));
    }

    public ModelManager(Object object) {
        super(object);
    }
}
