package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MTextureAtlasSpriteInfoBridge
extends Mapping {
    private final MappingMethod texturePathMethod;

    public MTextureAtlasSpriteInfoBridge() {
        super(MappedClasses.zI);
        this.texturePathMethod = this.Y("texturePath", true, MappedClasses.zC, new Class[]{});
    }

    public Object getTexturePath(Object spriteInfo) {
        return this.texturePathMethod.invokeObject(spriteInfo, new Object[0]);
    }
}

