package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRegistry;
import vegas.sony.wrapper.Wrapper;

import java.util.Optional;
import java.util.stream.Stream;

public class Registry
extends Wrapper {
    public Object t(int n) {
        return MRegistry.p(Registry.sonyVegasInstance.getMappingsMapperCompat().CV, this.I, n);
    }

    public int K(Object object) {
        return MRegistry.e(Registry.sonyVegasInstance.getMappingsMapperCompat().CV, this.I, object);
    }

    public ResourceLocation W(Object object) {
        return new ResourceLocation(MRegistry.F(Registry.sonyVegasInstance.getMappingsMapperCompat().CV, this.I, object));
    }

    public Optional<Holder> t(ResourceLocation resourceLocation) {
        Object object = this.B(resourceLocation);
        if (object == null) {
            return Optional.empty();
        }
        return Optional.of(this.J(object));
    }


    public Object B(ResourceLocation resourceLocation) {
        return MRegistry.d(Registry.sonyVegasInstance.getMappingsMapperCompat().CV, this.I, resourceLocation.getObject());
    }

    public Stream o() {
        return MRegistry.h(Registry.sonyVegasInstance.getMappingsMapperCompat().CV, this.getObject());
    }

    public Holder J(Object object) {
        return new Holder(MRegistry.S(Registry.sonyVegasInstance.getMappingsMapperCompat().CV, this.I, object));
    }

    public Registry(Object object) {
        super(object);
    }
}

