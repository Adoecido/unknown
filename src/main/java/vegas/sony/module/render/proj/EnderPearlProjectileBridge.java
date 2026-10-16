package vegas.sony.module.render.proj;

import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityEnderPearl;

public class EnderPearlProjectileBridge
extends EntityEnderPearl {
    public EnderPearlProjectileBridge(Object handle) {
        super(handle);
    }

    public Entity getOwnerEntity() {
        Object ownerHandle = EnderPearlProjectileBridge.sonyVegasInstance.getMappings()
                .enderPearlProjectileBridge.getOwner(this.getObject());
        return new Entity(ownerHandle);
    }
}
