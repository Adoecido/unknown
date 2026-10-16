package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MTextureAtlasSpriteInfo;
import vegas.sony.wrapper.Wrapper;

public class TextureAtlas
extends Wrapper {
    public TextureAtlasSprite getSprite(ResourceLocation location) {
        return new TextureAtlasSprite(MTextureAtlasSpriteInfo.getSprite(TextureAtlas.sonyVegasInstance.getMappings().textureAtlas, this.I, location.getObject()));
    }

    public TextureAtlas(Object object) {
        super(object);
    }

    public static ResourceLocation getBlocksAtlasLocation() {
        return new ResourceLocation(MTextureAtlasSpriteInfo.getBlocksAtlasLocation(TextureAtlas.sonyVegasInstance.getMappings().textureAtlas));
    }

    public ResourceLocation getTextureLocation() {
        return new ResourceLocation(MTextureAtlasSpriteInfo.getTextureLocation(TextureAtlas.sonyVegasInstance.getMappings().textureAtlas, this.I));
    }
}
