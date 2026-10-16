package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class TextureManagerHandle
extends Wrapper {
    public TextureObjectHandle getColorTexture() {
        return new TextureObjectHandle(TextureManagerHandle.sonyVegasInstance.getMappingsMapperCompat().renderTarget.getColorTexture(this.I));
    }

    public int getColorTextureId() {
        TextureObjectHandle colorTexture = this.getColorTexture();
        if (colorTexture.isNull()) {
            return -1;
        }
        return colorTexture.getId();
    }

    public TextureObjectHandle getDepthTexture() {
        return new TextureObjectHandle(TextureManagerHandle.sonyVegasInstance.getMappingsMapperCompat().renderTarget.getDepthTexture(this.I));
    }


    public TextureManagerHandle(Object object) {
        super(object);
    }

    public int getDepthTextureId() {
        TextureObjectHandle depthTexture = this.getDepthTexture();
        if (depthTexture.isNull()) {
            return -1;
        }
        return depthTexture.getId();
    }
}

