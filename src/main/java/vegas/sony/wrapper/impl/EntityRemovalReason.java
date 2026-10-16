package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEntityRemovalReason;
import vegas.sony.wrapper.Wrapper;

public class EntityRemovalReason
extends Wrapper {
    public static EntityRemovalReason U() {
        return new EntityRemovalReason(MEntityRemovalReason.U(EntityRemovalReason.sonyVegasInstance.getMappingsMapperCompat().h));
    }

    public static EntityRemovalReason X() {
        return new EntityRemovalReason(MEntityRemovalReason.N(EntityRemovalReason.sonyVegasInstance.getMappingsMapperCompat().h));
    }

    public static EntityRemovalReason c() {
        return new EntityRemovalReason(MEntityRemovalReason.Z(EntityRemovalReason.sonyVegasInstance.getMappingsMapperCompat().h));
    }

    public EntityRemovalReason(Object object) {
        super(object);
    }

    public static EntityRemovalReason K() {
        return new EntityRemovalReason(MEntityRemovalReason.k(EntityRemovalReason.sonyVegasInstance.getMappingsMapperCompat().h));
    }

    public static EntityRemovalReason P() {
        return new EntityRemovalReason(MEntityRemovalReason.i(EntityRemovalReason.sonyVegasInstance.getMappingsMapperCompat().h));
    }
}

