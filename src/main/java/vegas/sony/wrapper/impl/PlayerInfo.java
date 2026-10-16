package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MPlayerInfo;
import vegas.sony.wrapper.Wrapper;

public class PlayerInfo
extends Wrapper {

    public ITextComponent R() {
        return new ITextComponent(MPlayerInfo.l(PlayerInfo.sonyVegasInstance.getMappingsMapperCompat().CG, this.getObject()));
    }

    public ScorePlayerTeam X() {
        return new ScorePlayerTeam(MPlayerInfo.Z(PlayerInfo.sonyVegasInstance.getMappingsMapperCompat().CG, this.getObject()));
    }

    public ResourceLocation i() {
        Object object = MPlayerInfo.J(PlayerInfo.sonyVegasInstance.getMappingsMapperCompat().CG, this.I);
        if (object == null) {
            return null;
        }
        if (ForgeVersion.MC_1_20_6.d()) {
            return new PlayerSkin(object).getTexture();
        }
        return new ResourceLocation(object);
    }

    public GameProfile v() {
        return new GameProfile(MPlayerInfo.W(PlayerInfo.sonyVegasInstance.getMappingsMapperCompat().CG, this.I));
    }

    public PlayerInfo(Object object) {
        super(object);
    }

    public int z() {
        return MPlayerInfo.H(PlayerInfo.sonyVegasInstance.getMappingsMapperCompat().CG, this.I);
    }
}

