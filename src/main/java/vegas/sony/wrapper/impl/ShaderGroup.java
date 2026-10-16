package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

import java.util.List;

public class ShaderGroup
extends Wrapper {
    public ShaderGroup(Object object) {
        super(object);
    }

    public void resize(int n, int n2) {
        ShaderGroup.sonyVegasInstance.getMappingsMapperCompat().Qv.G(this.I, n, n2);
    }

    public static ShaderGroup create(TextureManager textureManager, ShaderGroupState shaderGroupState, Framebuffer framebuffer, ResourceLocation resourceLocation) {
        return new ShaderGroup(ShaderGroup.sonyVegasInstance.getMappingsMapperCompat().Qv.k(textureManager.getObject(), shaderGroupState.getObject(), framebuffer.getObject(), resourceLocation.getObject()));
    }

    public List getFramebuffers() {
        return ShaderGroup.sonyVegasInstance.getMappingsMapperCompat().Qv.h(this.I);
    }
}

