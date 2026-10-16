package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;

public class MStatusEffectSpriteUploader
extends Mapping {
    private MappingField potionSpritesField;
    private MappingMethod getSpriteMethod;

    public static Object getSprite(MStatusEffectSpriteUploader mapping, Object uploaderHandle, Object effectHandle) {
        return mapping.invokeGetSprite(uploaderHandle, effectHandle);
    }

    private Object readPotionSprites(Object minecraftHandle) {
        return this.potionSpritesField.getObject(minecraftHandle);
    }

    public static Object getPotionSprites(MStatusEffectSpriteUploader mapping, Object minecraftHandle) {
        return mapping.readPotionSprites(minecraftHandle);
    }

    public MStatusEffectSpriteUploader() {
        super(MappedClasses.u4);
        Class[] getSpriteParameterTypes = new Class[]{MappedClasses.D3};
        Class getSpriteReturnType = MappedClasses.Db;
        boolean getSpritePublic = true;
        String getSpriteMethodName = "getSprite";
        MStatusEffectSpriteUploader mapping = this;
        this.getSpriteMethod = mapping.Y(getSpriteMethodName, getSpritePublic, getSpriteReturnType, getSpriteParameterTypes);
        Class potionSpritesFieldType = MappedClasses.u4;
        boolean potionSpritesFieldPublic = true;
        String potionSpritesFieldName = "potionSprites";
        Class minecraftClass = MappedClasses.uP;
        MStatusEffectSpriteUploader potionSpritesMapping = this;
        this.potionSpritesField = potionSpritesMapping.registerInstanceFieldForOwner(minecraftClass, potionSpritesFieldName, potionSpritesFieldPublic, potionSpritesFieldType);
    }

    private Object invokeGetSprite(Object uploaderHandle, Object effectHandle) {
        return this.getSpriteMethod.invokeObject(uploaderHandle, effectHandle);
    }
}

