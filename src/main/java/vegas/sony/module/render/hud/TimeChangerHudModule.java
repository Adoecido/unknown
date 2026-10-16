package vegas.sony.module.render.hud;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventWorldTime;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.WorldClient;

public class TimeChangerHudModule
extends HudModule {
    private long overriddenWorldTime;
    private final NumberValue timeValue = NumberValue.create((Object)this, "Time //$ui", "#", "hours", 0.0, 12.0, 24.0, 1.0);

    private void applyWorldTime(WorldClient worldClient, long time) {
        if (ForgeVersion.MC_1_16_5.d()) {
            worldClient.getWorldInfo().setGameTime(time);
            worldClient.getWorldInfo().setDayTime(time);
        } else {
            worldClient.i(time);
        }
    }


    @EventHandler
    public void onPreRenderTick(EventPreRenderTick event) {
        if (ForgeVersion.MC_1_8_9.L()) {
            return;
        }
        if (event.getWorld().isNull()) {
            return;
        }
        double hours = (Double)this.timeValue.getValue();
        if ((hours -= 6.0) < 0.0) {
            hours = 24.0 + hours;
        }
        this.applyWorldTime(event.getWorld(), Math.round(hours * 1000.0));
    }

    public TimeChangerHudModule() {
        super("Time Changer //$ui", HudModuleGroup.GAME, "time_changer //$");
        this.setSuffix("Sets the in-game world time //$ui");
        this.addValue(this.timeValue);
    }

    @EventHandler
    public void onWorldTime(EventWorldTime event) {
        event.setWorldTime(this.overriddenWorldTime);
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        if (ForgeVersion.MC_1_8_9.A()) {
            return;
        }
        double hours = (Double)this.timeValue.getValue();
        if ((hours -= 6.0) < 0.0) {
            hours = 24.0 + hours;
        }
        this.overriddenWorldTime = Math.round(hours * 1000.0);
    }
}

