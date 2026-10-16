package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MTextureObject;
import vegas.sony.wrapper.Wrapper;

public class TextureObject
extends Wrapper {
    public void setFilter(boolean blur, boolean mipmap) {
        MTextureObject.setFilter(TextureObject.sonyVegasInstance.getMappingsMapperCompat().textureObject, this.I, blur, mipmap);
    }

    public TextureObject(Object object) {
        super(object);
    }

    public int getId() {
        if (ForgeVersion.MC_1_21_6.d()) {
            TextureObjectHandle textureHandle = new TextureObjectHandle(MTextureObject.getTexture(TextureObject.sonyVegasInstance.getMappingsMapperCompat().textureObject, this.I));
            return textureHandle.getId();
        }
        return MTextureObject.getGlTextureId(TextureObject.sonyVegasInstance.getMappingsMapperCompat().textureObject, this.I);
    }
}
