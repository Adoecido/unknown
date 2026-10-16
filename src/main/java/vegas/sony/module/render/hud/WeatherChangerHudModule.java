package vegas.sony.module.render.hud;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.unmap.ModeOption;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.ModeValue;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.WorldClient;

public class WeatherChangerHudModule
extends HudModule {
    private float savedPreviousRainStrength;
    public final ModeValue weatherMode;
    private float savedThunderStrength;
    private final ModeOption rainingOption;
    private float savedPreviousThunderStrength;
    private float savedRainStrength;
    private final ModeOption clearOption = new ModeOption("Clear //$ui");

    @EventHandler
    public void onPreRenderTick(EventPreRenderTick event) {
        WorldClient worldClient = event.getWorld();
        if (worldClient.isNotNull()) {
            if (((ModeSelection)this.weatherMode.getValue()).equals(this.clearOption)) {
                worldClient.T(0.0f);
                worldClient.o(0.0f);
                worldClient.f(0.0f);
                worldClient.g(0.0f);
            } else {
                worldClient.T(1.0f);
                worldClient.o(1.0f);
            }
        }
    }

    @Override
    public void onEnable() {
        if (Minecraft.theWorld().isNotNull()) {
            WorldClient worldClient = Minecraft.theWorld();
            this.savedRainStrength = worldClient.n();
            this.savedPreviousRainStrength = worldClient.N();
            this.savedThunderStrength = worldClient.y();
            this.savedPreviousThunderStrength = worldClient.V();
        }
    }

    public WeatherChangerHudModule() {
        super("Weather //$ui", HudModuleGroup.GAME, "weather //$");
        this.rainingOption = new ModeOption("Raining //$ui");
        this.weatherMode = ModeValue.create((Object)this, "Weather //$ui", this.clearOption, this.clearOption, this.rainingOption);
        this.addValue(this.weatherMode);
        this.setSuffix("Change the weather //$ui");
    }

    @Override
    public void onDisable() {
        if (Minecraft.theWorld().isNotNull()) {
            WorldClient worldClient = Minecraft.theWorld();
            worldClient.T(this.savedRainStrength);
            worldClient.o(this.savedPreviousRainStrength);
            worldClient.f(this.savedThunderStrength);
            worldClient.g(this.savedPreviousThunderStrength);
        }
    }

}

