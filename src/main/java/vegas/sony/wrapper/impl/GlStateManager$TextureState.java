package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class GlStateManager$TextureState
extends Wrapper {
    public GlStateManager$TextureState(Object textureStateHandle) {
        super(textureStateHandle);
    }

    public int getTextureName() {
        return GlStateManager$TextureState.sonyVegasInstance.getMappingsMapperCompat().glTextureState.getTextureName(this.I);
    }

    public void setTextureName(int textureName) {
        GlStateManager$TextureState.sonyVegasInstance.getMappingsMapperCompat().glTextureState
                .setTextureName(this.I, textureName);
    }
}
