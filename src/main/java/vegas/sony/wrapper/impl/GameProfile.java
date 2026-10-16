package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MGameProfile;
import vegas.sony.wrapper.Wrapper;
import java.util.UUID;

public class GameProfile
extends Wrapper {
    public GameProfile(Object object) {
        super(object);
    }

    public static GameProfile create(UUID uUID, String string) {
        return new GameProfile(MGameProfile.N(GameProfile.sonyVegasInstance.getMappingsMapperCompat().d, uUID, string));
    }

    public String getName() {
        return MGameProfile.P(GameProfile.sonyVegasInstance.getMappingsMapperCompat().d, this.I);
    }

    public void setName(String string) {
        MGameProfile.z(GameProfile.sonyVegasInstance.getMappingsMapperCompat().d).setObject(this.I, string);
    }

    public UUID getUUID() {
        return MGameProfile.h(GameProfile.sonyVegasInstance.getMappingsMapperCompat().d, this.I);
    }
}

