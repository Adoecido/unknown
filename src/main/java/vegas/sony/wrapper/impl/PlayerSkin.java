package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class PlayerSkin
extends Wrapper {

    public TextureAtlasSpriteInfo getBodySpriteInfo() {
        return new TextureAtlasSpriteInfo(PlayerSkin.sonyVegasInstance.getMappingsMapperCompat().playerSkin.getBody(this.I));
    }

    public PlayerSkin(Object playerSkinHandle) {
        super(playerSkinHandle);
    }

    public ResourceLocation getTexture() {
        if (ForgeVersion.MC_1_21_10.d()) {
            return this.getBodySpriteInfo().getTexturePath();
        }
        return new ResourceLocation(PlayerSkin.sonyVegasInstance.getMappingsMapperCompat().playerSkin.getTexture(this.I));
    }
}

