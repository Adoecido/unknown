package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEnumCreatureAttribute;
import vegas.sony.wrapper.Wrapper;

public class DamageSource
extends Wrapper {
    public static DamageSource C(EntityPlayer entityPlayer) {
        if (ForgeVersion.MC_1_20_6.d()) {
            return new DamageSource(MEnumCreatureAttribute.X(DamageSource.sonyVegasInstance.getMappings().hW, Minecraft.theWorld().q(), entityPlayer.getObject()));
        }
        return new DamageSource(MEnumCreatureAttribute.X(DamageSource.sonyVegasInstance.getMappings().hW, null, entityPlayer.getObject()));
    }


    public DamageSource(Object object) {
        super(object);
    }

    public static DamageSource m$src$Lvegas_sony_wrapper_impl_DamageSource_$z0ibym() {
        return new DamageSource(MEnumCreatureAttribute.E(DamageSource.sonyVegasInstance.getMappings().hW));
    }
}

