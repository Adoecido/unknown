package vegas.sony.wrapper.impl;

import com.google.common.base.Predicate;
import vegas.sony.wrapper.Wrapper;

public class EntitySelectors
extends Wrapper {
    public static Predicate getTeamCollisionPredicate(Entity entity) {
        return (Predicate)EntitySelectors.sonyVegasInstance.getMappings().entitySelectors
                .getCollisionPredicate(entity.getObject());
    }

    public EntitySelectors(Object selectorsHandle) {
        super(selectorsHandle);
    }

    public static java.util.function.Predicate pushableBy(Entity entity) {
        return (java.util.function.Predicate)EntitySelectors.sonyVegasInstance.getMappings().entitySelectors
                .getCollisionPredicate(entity.getObject());
    }
}
