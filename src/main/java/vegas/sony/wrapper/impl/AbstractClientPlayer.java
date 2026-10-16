package vegas.sony.wrapper.impl;

import vegas.sony.module.render.freecam.FreecamPlayerBridge;
import vegas.sony.ui.click.component.GuiComponent;

public class AbstractClientPlayer
extends EntityPlayer {
    private static GuiComponent[] o;

    public static void R(GuiComponent[] guiComponentArray) {
        o = guiComponentArray;
    }

    public FreecamPlayerBridge getFreecamPlayerBridge() {
        return new FreecamPlayerBridge(AbstractClientPlayer.sonyVegasInstance.getMappings().Dk.getClientAvatarState(this.I));
    }

    static {
        if (AbstractClientPlayer.I() != null) {
            AbstractClientPlayer.R(new GuiComponent[5]);
        }
    }


    public AbstractClientPlayer(Object object) {
        super(object);
    }

    public static GuiComponent[] I() {
        return o;
    }

    public ResourceLocation O() {
        if (ForgeVersion.MC_1_20_6.d()) {
            return new PlayerSkin(AbstractClientPlayer.sonyVegasInstance.getMappings().Dk.getLocationSkin(this.I)).getTexture();
        }
        return new ResourceLocation(AbstractClientPlayer.sonyVegasInstance.getMappings().Dk.getLocationSkin(this.I));
    }
}

