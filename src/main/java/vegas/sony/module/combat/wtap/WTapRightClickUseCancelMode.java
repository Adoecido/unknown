package vegas.sony.module.combat.wtap;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventClickMouse;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.module.combat.HitSelect;
import vegas.sony.rotation.RotationManager;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.GuiScreen;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RayTraceResult;

public class WTapRightClickUseCancelMode
extends SubModule<HitSelect> {
    private int cooldownTicks = -1;
    private boolean cancelActive = false;
    private static final long COOLDOWN_TICKS;

    static {
        COOLDOWN_TICKS = -5882419382201614328L;
    }

    public WTapRightClickUseCancelMode(Mod mod, String string) {
        super(mod, string);
    }

    @EventHandler
    public void onClickMouse(EventClickMouse eventClickMouse) {
        if (this.isCancelActive()) {
            eventClickMouse.setCancelled(true);
        }
    }


    @EventHandler
    public void onTick(EventPreTick eventPreTick) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            this.cancelActive = false;
            return;
        }
        if (this.cooldownTicks > 0) {
            --this.cooldownTicks;
            this.cancelActive = false;
            return;
        }
        RayTraceResult rayTrace = RotationManager.INSTANCE.getExtendedReachRayTrace();
        EntityLivingBase target = rayTrace.isNotNull() && rayTrace.getEntity().isInstance(MappedClasses.zm)
                ? new EntityLivingBase(rayTrace.getEntity()) : new EntityLivingBase(null);
        GuiScreen screen = Minecraft.currentScreen();
        if (target.isNull() && screen.isNull()) {
            this.cancelActive = false;
            return;
        }
        if (target.isNotNull() && player.b$src$Z$fqlxe4() && target.V$src$I$fk0dv5() > 12) {
            if (!((HitSelect)this.getParent()).shouldTrigger()) {
                this.cooldownTicks = (int)COOLDOWN_TICKS;
            }
            this.cancelActive = true;
            return;
        }
        this.cancelActive = false;
    }

    public boolean isCancelActive() {
        return this.cancelActive;
    }
}