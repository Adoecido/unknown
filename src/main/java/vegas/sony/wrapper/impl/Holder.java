package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MHolder;
import vegas.sony.wrapper.Wrapper;

import java.util.Optional;

public class Holder
extends Wrapper {
    public boolean F(ResourceKey jy_12) {
        return MHolder.q(Holder.sonyVegasInstance.getMappingsMapperCompat().hN, this.I, jy_12.getObject());
    }

    public String Z() {
        return MHolder.F(Holder.sonyVegasInstance.getMappingsMapperCompat().hN, this.I);
    }

    public static Holder A(Object object) {
        return new Holder(MHolder.f(Holder.sonyVegasInstance.getMappingsMapperCompat().hN, object));
    }

    public Holder(Object object) {
        super(object);
    }

    public Object N() {
        return MHolder.E(Holder.sonyVegasInstance.getMappingsMapperCompat().hN, this.I);
    }

    public Optional f() {
        return (Optional)MHolder.m(Holder.sonyVegasInstance.getMappingsMapperCompat().hN, this.I);
    }
}

