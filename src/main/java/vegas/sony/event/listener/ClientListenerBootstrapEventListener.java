package vegas.sony.event.listener;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventLivingUpdate;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.SPacketEntityStatus;
import vegas.sony.wrapper.impl.WorldClient;

public class ClientListenerBootstrapEventListener
implements EventListener {
    private static GuiComponent[] obfuscationState;


    @EventHandler(priority=EventPriority.LOW)
    public void onPacketReceive(EventPacketReceive eventPacketReceive) {
        if (!eventPacketReceive.getPacket().isInstance(MappedClasses.lU)) {
            return;
        }
        SPacketEntityStatus entityStatusPacket = new SPacketEntityStatus(eventPacketReceive.getPacket().getObject());
        WorldClient world = Minecraft.theWorld();
        if (world.isNull()) {
            return;
        }
        Entity entity = world.V(entityStatusPacket.getEntityId());
        if (entity.isNull()) {
            return;
        }
        new EventLivingUpdate(entity).fire();
    }

    static {
        if (ClientListenerBootstrapEventListener.getObfuscationState() != null) {
            ClientListenerBootstrapEventListener.setObfuscationState(new GuiComponent[1]);
        }
    }

    public static GuiComponent[] getObfuscationState() {
        return obfuscationState;
    }

    public static void setObfuscationState(GuiComponent[] state) {
        obfuscationState = state;
    }
}

