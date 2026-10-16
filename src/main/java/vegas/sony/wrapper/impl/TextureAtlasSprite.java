package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MTextureAtlasSprite;
import vegas.sony.wrapper.Wrapper;

public class TextureAtlasSprite
extends Wrapper {
    public ResourceLocation getAtlasLocation() {
        return new ResourceLocation(MTextureAtlasSprite.getAtlasLocation(TextureAtlasSprite.sonyVegasInstance.getMappingsMapperCompat().textureAtlasSprite, this.I));
    }

    public void setU0(float u0) {
        MTextureAtlasSprite.setU0(TextureAtlasSprite.sonyVegasInstance.getMappingsMapperCompat().textureAtlasSprite, this.I, u0);
    }

    public void setU1(float u1) {
        MTextureAtlasSprite.setU1(TextureAtlasSprite.sonyVegasInstance.getMappingsMapperCompat().textureAtlasSprite, this.I, u1);
    }

    public TextureAtlasSprite(Object object) {
        super(object);
    }

    public void setSecondaryPosition(int position) {
        MTextureAtlasSprite.setSecondaryPosition(TextureAtlasSprite.sonyVegasInstance.getMappingsMapperCompat().textureAtlasSprite, this.I, position);
    }

    public void setV0(float v0) {
        MTextureAtlasSprite.setV0(TextureAtlasSprite.sonyVegasInstance.getMappingsMapperCompat().textureAtlasSprite, this.I, v0);
    }

    public void setV1(float v1) {
        MTextureAtlasSprite.setV1(TextureAtlasSprite.sonyVegasInstance.getMappingsMapperCompat().textureAtlasSprite, this.I, v1);
    }

    public float[] getTextureCoordinates() {
        return MTextureAtlasSprite.getTextureCoordinates(TextureAtlasSprite.sonyVegasInstance.getMappingsMapperCompat().textureAtlasSprite, this.I);
    }

    public Object getContentsOrAtlasTexture() {
        return MTextureAtlasSprite.getContentsOrAtlasTexture(TextureAtlasSprite.sonyVegasInstance.getMappingsMapperCompat().textureAtlasSprite, this.I);
    }

    public void setPrimaryPosition(int position) {
        MTextureAtlasSprite.setPrimaryPosition(TextureAtlasSprite.sonyVegasInstance.getMappingsMapperCompat().textureAtlasSprite, this.I, position);
    }
}
