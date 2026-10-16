package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class TextureObjectHandle
extends Wrapper {
    public int resolveFramebufferId(int depthTextureId) {
        return TextureObjectHandle.sonyVegasInstance.getMappingsMapperCompat().gpuTexture.resolveFramebufferId(this.I, depthTextureId);
    }

    public int getId() {
        return TextureObjectHandle.sonyVegasInstance.getMappingsMapperCompat().gpuTexture.getTextureId(this.I);
    }

    public TextureObjectHandle(Object object) {
        super(object);
    }
}
