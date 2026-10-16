package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MGuiSpriteManager
extends Mapping {
    private final MappingMethod getSpriteMethod;

    public MGuiSpriteManager() {
        super(MappedClasses.D_);
        this.getSpriteMethod = this.Y("getSprite", true, MappedClasses.Db, new Class[]{MappedClasses.zC});
    }

    public Object getSprite(Object spriteManager, Object resourceLocation) {
        return this.getSpriteMethod.invokeObject(spriteManager, resourceLocation);
    }
}

