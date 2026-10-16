package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.MappingMethodBuilder;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;
import java.util.Collection;

public class MTeam
extends Mapping {
    private static final String IS_SAME_TEAM_METHOD_NAME = "isSameTeam";
    private final MappingMethod isSameTeamMethod;
    private final MappingField prefixField;
    private final MappingField suffixField;
    private final MappingField registeredNameField;
    private final MappingField colorField;
    private final MappingMethod getMembershipCollectionMethod;

    public static boolean isSameTeam(MTeam mapping, Object team, Object otherTeam) {
        return mapping.invokeIsSameTeam(team, otherTeam);
    }

    private boolean invokeIsSameTeam(Object team, Object otherTeam) {
        return this.isSameTeamMethod.invokeBoolean(team, otherTeam);
    }

    private Object getPrefix(Object team) {
        return this.prefixField.getObject(team);
    }

    private Object getSuffix(Object team) {
        return this.suffixField.getObject(team);
    }

    private Object getRegisteredName(Object team) {
        return this.registeredNameField.getObject(team);
    }

    private Object getMembershipCollection(Object team) {
        return this.getMembershipCollectionMethod.invokeObject(team);
    }

    public static Object getPrefix(MTeam mapping, Object team) {
        return mapping.getPrefix(team);
    }

    public static Object getSuffix(MTeam mapping, Object team) {
        return mapping.getSuffix(team);
    }

    public static Object getRegisteredName(MTeam mapping, Object team) {
        return mapping.getRegisteredName(team);
    }

    public static Object getMembershipCollection(MTeam mapping, Object team) {
        return mapping.getMembershipCollection(team);
    }

    public MTeam() {
        super(MappedClasses.Yh);
        this.isSameTeamMethod = this.Y(IS_SAME_TEAM_METHOD_NAME, true, Boolean.TYPE, new Class[]{MappedClasses.Yh});
        
        // CB 1.8.9 (Allatori) fields
        if (ForgeVersion.MC_1_7_10.L() && !Wrapper.sonyVegasInstance.isVanillaMinecraftPresent()) {
            // CheatBreaker keeps these values on ScorePlayerTeam. Team's wrapper
            // delegates there, so do not attempt to resolve fields that do not exist.
            this.prefixField = null;
            this.suffixField = null;
            this.registeredNameField = null;
            this.colorField = null;
        } else {
            // Fallback for other versions - try common field names
            Class<String> clazz = String.class;
            boolean bl = true;
            this.prefixField = this.J("namePrefix", bl, clazz);
            this.suffixField = this.J("nameSuffix", bl, clazz);
            this.registeredNameField = this.J("registeredName", bl, clazz);
            this.colorField = this.J("color", bl, clazz);
        }
        
        // getMembershipCollection - descriptor ()Ljava/util/Collection;
        Class<Collection> collClazz = Collection.class;
        boolean bl = true;
        String membershipMethodName = "getMembershipCollection";
        this.getMembershipCollectionMethod = this.Y(membershipMethodName, bl, collClazz, new Class[0]);
    }
}

