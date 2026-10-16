package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;
import java.util.function.Consumer;

public class LevelEntityGetter
extends Wrapper {
    public LevelEntityGetter(Object wrappedObject) {
        super(wrappedObject);
    }

    public void forEachEntityInBounds(AxisAlignedBB boundingBox, Consumer consumer) {
        LevelEntityGetter.sonyVegasInstance.getMappingsMapperCompat().levelEntityGetter.forEachEntityInBounds(this.I, boundingBox.getObject(), consumer);
    }
}
