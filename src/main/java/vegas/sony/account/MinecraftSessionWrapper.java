package vegas.sony.account;

import vegas.sony.mapping.mappings.MSession;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.SessionType;
import java.util.Optional;
import java.util.UUID;

public class MinecraftSessionWrapper
extends Wrapper {

    public static MinecraftSessionWrapper createModern(String username, UUID profileId, String accessToken,
                                                       Optional<String> xuid, Optional<String> clientId,
                                                       SessionType accountType) {
        return new MinecraftSessionWrapper(MSession.B(MinecraftSessionWrapper.sonyVegasInstance.getMappings().hw, username,
                profileId, accessToken, xuid, clientId, accountType.getObject()));
    }

    public MinecraftSessionWrapper(Object session) {
        super(session);
    }

    public String getUsername() {
        return MinecraftSessionWrapper.sonyVegasInstance.getMappings().hw.s(this.I);
    }

    public UUID getProfileId() {
        if (ForgeVersion.MC_1_20_6.d()) {
            return (UUID)MinecraftSessionWrapper.sonyVegasInstance.getMappings().hw.V(this.I);
        }
        String rawProfileId = (String)MinecraftSessionWrapper.sonyVegasInstance.getMappings().hw.V(this.I);
        if (rawProfileId == null || rawProfileId.isEmpty()) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        return UUID.fromString(rawProfileId.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }

    public static MinecraftSessionWrapper createLegacy(String username, String profileId, String accessToken,
                                                       String sessionType) {
        return new MinecraftSessionWrapper(MSession.t(MinecraftSessionWrapper.sonyVegasInstance.getMappings().hw, username,
                profileId, accessToken, sessionType));
    }
}
