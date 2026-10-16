package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MSoundAwareEntityFX;

public class SoundAwareEntityFX
extends EntityFX {
    public EnumParticleTypes Z$src$Lvegas_sony_wrapper_impl_EnumParticleTypes_$1aa3947() {
        return new EnumParticleTypes(MSoundAwareEntityFX.b(SoundAwareEntityFX.sonyVegasInstance.getMappings().R4, this.I));
    }

    public void O(Entity entity) {
        MSoundAwareEntityFX.u(SoundAwareEntityFX.sonyVegasInstance.getMappings().R4, this.I, entity.getObject());
    }

    public Entity M$src$Lvegas_sony_wrapper_impl_Entity_$1791qxt() {
        return new Entity(MSoundAwareEntityFX.q(SoundAwareEntityFX.sonyVegasInstance.getMappings().R4, this.I));
    }

    public SoundAwareEntityFX(Object object) {
        super(object);
    }
}

