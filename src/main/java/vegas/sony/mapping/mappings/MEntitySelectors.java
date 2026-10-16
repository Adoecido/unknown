package vegas.sony.mapping.mappings;

import com.google.common.base.Predicate;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.MappingMethodBuilder;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MEntitySelectors
extends Mapping {
    private final MappingMethod collisionPredicateMethod;

    public Object getCollisionPredicate(Object entityHandle) {
        return this.collisionPredicateMethod.invokeObject(null, entityHandle);
    }

    public MEntitySelectors() {
        super(MappedClasses.qW);
        this.collisionPredicateMethod = ((MappingMethodBuilder)((MappingMethodBuilder)((MappingMethodBuilder)this
                .methodBuilder("getTeamCollisionPredicate", Predicate.class, MappedClasses.zc)
                .setTypeForVersion(ForgeVersion.MC_1_16_5.n(), java.util.function.Predicate.class))
                .setNameForVersion(ForgeVersion.MC_1_16_5.n(), "pushableBy"))
                .setStaticMember(true))
                .buildMethod();
    }
}

