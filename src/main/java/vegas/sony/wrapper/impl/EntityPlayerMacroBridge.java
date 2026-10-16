package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEntityPlayerMacroBridge;
import vegas.sony.module.render.proj.EnderPearlProjectileBridge;

public class EntityPlayerMacroBridge
extends Entity {
    public Entity A$src$Lvegas_sony_wrapper_impl_Entity_$12ijiu4() {
        if (ForgeVersion.MC_1_16_5.v()) {
            return new EntityPlayer(MEntityPlayerMacroBridge.F(EntityPlayerMacroBridge.sonyVegasInstance.getMappings().Da, this.I));
        }
        return new EnderPearlProjectileBridge(this.getObject()).getOwnerEntity();
    }

    public Entity r$src$Lvegas_sony_wrapper_impl_Entity_$18p7x3h() {
        return new Entity(MEntityPlayerMacroBridge.W(EntityPlayerMacroBridge.sonyVegasInstance.getMappings().Da, this.I));
    }


    public EntityPlayerMacroBridge(Object object) {
        super(object);
    }

    public boolean o() {
        return MEntityPlayerMacroBridge.B(EntityPlayerMacroBridge.sonyVegasInstance.getMappings().Da, this.I);
    }
}

