package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MDataComponents;
import vegas.sony.wrapper.Wrapper;

public class DataComponents
extends Wrapper {
    public DataComponents(Object object) {
        super(object);
    }

    public static DataComponentType E() {
        return new DataComponentType(MDataComponents.E(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType V() {
        return new DataComponentType(MDataComponents.m(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType l() {
        return new DataComponentType(MDataComponents.T(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType X() {
        return new DataComponentType(MDataComponents.e(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType p() {
        return new DataComponentType(MDataComponents.L(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType k() {
        return new DataComponentType(MDataComponents.F(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType d() {
        return new DataComponentType(MDataComponents.a(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType F() {
        return new DataComponentType(MDataComponents.y(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType T() {
        return new DataComponentType(MDataComponents.M(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType o() {
        return new DataComponentType(MDataComponents.h(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType R() {
        return new DataComponentType(MDataComponents.O(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType t() {
        return new DataComponentType(MDataComponents.I(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType O() {
        return new DataComponentType(MDataComponents.z(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }

    public static DataComponentType I() {
        return new DataComponentType(MDataComponents.b(DataComponents.sonyVegasInstance.getMappingsMapperCompat().q5));
    }
}

