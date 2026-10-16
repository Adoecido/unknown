package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MTextureManager;
import vegas.sony.wrapper.Wrapper;

public class TextureManager
extends Wrapper {

    public TextureManager(Object object) {
        super(object);
    }

    public void bindTexture(ResourceLocation location) {
        if (ForgeVersion.MC_1_21_0.d()) {
            return;
        }
        MTextureManager.bindTexture(TextureManager.sonyVegasInstance.getMappingsMapperCompat().textureManager, this.I, location.getObject());
    }

    public TextureObject getTexture(ResourceLocation location) {
        return new TextureObject(MTextureManager.getTexture(TextureManager.sonyVegasInstance.getMappingsMapperCompat().textureManager, this.I, location.getObject()));
    }
}

