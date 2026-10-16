package vegas.sony.mapping;

import vegas.sony.event.impl.EventNameFormat;
import vegas.sony.mapping.InsertedEventCallback;
import vegas.sony.module.render.entity.RenderEntityContextCache;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.ITextComponent;

public class PlayerNameFormatCallback
implements InsertedEventCallback {
    public static boolean s = false;
    private final Object U;
    private ITextComponent I;

    private ITextComponent grabDisplayName(EntityPlayer entityPlayer) {
        s = true;
        ITextComponent iTextComponent = RenderEntityContextCache.getDisplayName(entityPlayer);
        s = false;
        return iTextComponent;
    }

    @Override
    public boolean fire() {
        if (s) {
            s = false;
            return false;
        }
        EntityPlayer entityPlayer = new EntityPlayer(this.U);
        ITextComponent iTextComponent = RenderEntityContextCache.getCustomName(entityPlayer);
        if (iTextComponent != null) {
            this.I = iTextComponent;
        } else {
            iTextComponent = this.grabDisplayName(entityPlayer);
            this.I = new ITextComponent(iTextComponent);
            EventNameFormat eventNameFormat = new EventNameFormat(entityPlayer, this.I);
            eventNameFormat.fire();
            this.I = eventNameFormat.getDisplayName();
            RenderEntityContextCache.setCustomName(entityPlayer, this.I);
        }
        return true;
    }

    public PlayerNameFormatCallback(Object object) {
        this.U = object;
    }

    public ITextComponent getDisplayName() {
        return this.I;
    }


    public Object getRawDisplayName() {
        return this.I.getObject();
    }
}

