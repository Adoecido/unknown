package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MTeam;
import vegas.sony.wrapper.Wrapper;

import java.util.Collection;

public class Team
extends Wrapper {
    public boolean isSameTeam(Team team) {
        return MTeam.isSameTeam(Team.sonyVegasInstance.getMappingsMapperCompat().qW, this.I, team.getObject());
    }

    public String getColorPrefix() {
        if (isCheatBreakerLegacy()) {
            return new ScorePlayerTeam(this.I).getPrefix();
        }
        return (String) MTeam.getPrefix(Team.sonyVegasInstance.getMappingsMapperCompat().qW, this.I);
    }

    public String getColorSuffix() {
        if (isCheatBreakerLegacy()) {
            return new ScorePlayerTeam(this.I).getSuffix();
        }
        return (String) MTeam.getSuffix(Team.sonyVegasInstance.getMappingsMapperCompat().qW, this.I);
    }

    public String getRegisteredName() {
        if (isCheatBreakerLegacy()) {
            return new ScorePlayerTeam(this.I).getRegisteredName();
        }
        return (String) MTeam.getRegisteredName(Team.sonyVegasInstance.getMappingsMapperCompat().qW, this.I);
    }

    private static boolean isCheatBreakerLegacy() {
        return ForgeVersion.MC_1_7_10.L() && !Wrapper.sonyVegasInstance.isVanillaMinecraftPresent();
    }

    public Collection<String> getMembershipList() {
        return (Collection<String>) MTeam.getMembershipCollection(Team.sonyVegasInstance.getMappingsMapperCompat().qW, this.I);
    }

    public Object getMembershipCollection() {
        return MTeam.getMembershipCollection(Team.sonyVegasInstance.getMappingsMapperCompat().qW, this.I);
    }

    public Team(Object wrappedObject) {
        super(wrappedObject);
    }
}
