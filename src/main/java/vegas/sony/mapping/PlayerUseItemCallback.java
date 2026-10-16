package vegas.sony.mapping;

import vegas.sony.event.impl.EventPlayerUseItem;
import vegas.sony.mapping.InsertedEventCallback;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.EnumHand;
import vegas.sony.wrapper.impl.ItemStack;

public class PlayerUseItemCallback
implements InsertedEventCallback {
    private final EntityPlayer h;
    private final EnumHand r;

    @Override
    public boolean fire() {
        ItemStack itemStack = this.h.i(this.r);
        EventPlayerUseItem eventPlayerUseItem = new EventPlayerUseItem(itemStack.getObject());
        return eventPlayerUseItem.fire();
    }

    public PlayerUseItemCallback(Object object, Object object2) {
        this.h = new EntityPlayer(object);
        this.r = new EnumHand(object2);
    }
}

