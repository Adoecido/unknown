package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class TextureAtlasSpriteInfo
extends Wrapper {
    public TextureAtlasSpriteInfo(Object handle) {
        super(handle);
    }

    public ResourceLocation getTexturePath() {
        return new ResourceLocation(TextureAtlasSpriteInfo.sonyVegasInstance.getMappingsMapperCompat().textureAtlasSpriteInfo.getTexturePath(this.I));
    }
}
