package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MStatusEffectSpriteUploader;
import vegas.sony.wrapper.Wrapper;

public class StatusEffectSpriteUploader
extends Wrapper {
    public StatusEffectSpriteUploader(Object handle) {
        super(handle);
    }

    public TextureAtlasSprite getSprite(StatusEffect statusEffect) {
        return new TextureAtlasSprite(MStatusEffectSpriteUploader.getSprite(StatusEffectSpriteUploader.sonyVegasInstance.getMappingsMapperCompat().statusEffectSpriteUploader, this.I, statusEffect.getObject()));
    }

    public TextureAtlasSprite getSprite(Holder holder) {
        return new TextureAtlasSprite(MStatusEffectSpriteUploader.getSprite(StatusEffectSpriteUploader.sonyVegasInstance.getMappingsMapperCompat().statusEffectSpriteUploader, this.I, holder.getObject()));
    }

    public static StatusEffectSpriteUploader getPotionSprites() {
        Object uploaderHandle = MStatusEffectSpriteUploader.getPotionSprites(StatusEffectSpriteUploader.sonyVegasInstance.getMappingsMapperCompat().statusEffectSpriteUploader, Minecraft.i());
        return new StatusEffectSpriteUploader(uploaderHandle);
    }
}
