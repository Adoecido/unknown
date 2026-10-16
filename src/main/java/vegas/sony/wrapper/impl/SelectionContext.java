package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class SelectionContext
extends Wrapper {
    public SelectionContext(Object handle) {
        super(handle);
    }

    public static SelectionContext forEntity(Entity entity) {
        return new SelectionContext(SelectionContext.sonyVegasInstance.getMappingsMapperCompat().selectionContext.forEntity(entity.getObject()));
    }
}
