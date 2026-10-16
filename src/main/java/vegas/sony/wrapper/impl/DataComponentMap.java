package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MDataComponentMap;
import vegas.sony.wrapper.Wrapper;

import java.util.Set;

public class DataComponentMap
extends Wrapper {
    public Object E(DataComponentType jr_12) {
        return DataComponentMap.sonyVegasInstance.getMappingsMapperCompat().qk.j(this.I, jr_12.getObject());
    }

    public boolean V(DataComponentType jr_12) {
        return DataComponentMap.sonyVegasInstance.getMappingsMapperCompat().qk.V(this.getObject(), jr_12.getObject());
    }

    public static DataComponentMap u() {
        return new DataComponentMap(MDataComponentMap.m(DataComponentMap.sonyVegasInstance.getMappingsMapperCompat().qk));
    }

    public Set Z() {
        return (Set)MDataComponentMap.w(DataComponentMap.sonyVegasInstance.getMappingsMapperCompat().qk, this.I);
    }

    public DataComponentMap(Object object) {
        super(object);
    }
}

