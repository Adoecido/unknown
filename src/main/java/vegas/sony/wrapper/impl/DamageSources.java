package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class DamageSources
extends Wrapper {
    public DamageSource playerAttack(EntityPlayer player) {
        return new DamageSource(DamageSources.sonyVegasInstance.getMappingsMapperCompat().h0.getPlayerAttackDamageSource(this.getObject(), player.getObject()));
    }

    public DamageSource explosion() {
        return new DamageSource(DamageSources.sonyVegasInstance.getMappingsMapperCompat().h0.getExplosionDamageSource(this.getObject()));
    }

    public DamageSource fall() {
        return new DamageSource(DamageSources.sonyVegasInstance.getMappingsMapperCompat().h0.getFallDamageSource(this.getObject()));
    }

    public DamageSources(Object object) {
        super(object);
    }
}
