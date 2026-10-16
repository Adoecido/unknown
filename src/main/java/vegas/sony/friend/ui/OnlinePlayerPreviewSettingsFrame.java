package vegas.sony.friend.ui;

import com.google.gson.JsonObject;
import vegas.sony.friend.ui.OnlinePlayerPreviewComponent;
import vegas.sony.friend.ui.OnlinePlayerPreviewDividerComponent;
import vegas.sony.friend.ui.OnlinePlayerPreviewSettings;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.value.BooleanToggleComponent;
import vegas.sony.ui.click.component.value.NumberSliderComponent;
import vegas.sony.ui.click.frame.impl.hud.HudSettingsFrameBase;
import vegas.sony.ui.click.frame.impl.quickactions.QuickActionsFrame;

public class OnlinePlayerPreviewSettingsFrame
extends HudSettingsFrameBase {
    private boolean scaleDragging;
    private static GuiComponent[] obfuscationComponents;
    private final NumberSliderComponent scaleSlider;
    private final OnlinePlayerPreviewSettings settings = new OnlinePlayerPreviewSettings();
    private final NumberSliderComponent framesPerSecondSlider;
    private final NumberSliderComponent fieldOfViewSlider;
    private final BooleanToggleComponent levelViewToggle;

    @Override
    public void t(JsonObject jsonObject) {
        super.t(jsonObject);
        ClientSettings.getFrame(QuickActionsFrame.class).N$src$Lvegas_sony_ui_click_frame_impl_quickactions_QuickA$1smecqc().setValue(this.V$src$Z$1xhop3l());
    }

    @Override
    public void Y() {
        if (this.scaleSlider.isDragging()) {
            if (!this.scaleDragging) {
                this.scaleDragging = true;
            }
        } else if (this.scaleDragging) {
            this.scaleDragging = false;
            this.H(true);
        }
    }

    public OnlinePlayerPreviewSettings getSettings() {
        return this.settings;
    }

    @Override
    public String getName() {
        return "Rearview";
    }

    static {
        OnlinePlayerPreviewSettingsFrame.setObfuscationComponents(null);
    }

    public OnlinePlayerPreviewSettingsFrame() {
        super("newrearview", "Rearview");
        this.scaleSlider = new NumberSliderComponent(this.settings.scale);
        this.framesPerSecondSlider = new NumberSliderComponent(this.settings.framesPerSecond);
        this.fieldOfViewSlider = new NumberSliderComponent(this.settings.fieldOfView);
        this.levelViewToggle = new BooleanToggleComponent(this.settings.levelView);
        this.scaleSlider.setDisabledOverlayColor(OnlinePlayerPreviewSettingsFrame.J.r);
        this.framesPerSecondSlider.setDisabledOverlayColor(OnlinePlayerPreviewSettingsFrame.J.r);
        this.fieldOfViewSlider.setDisabledOverlayColor(OnlinePlayerPreviewSettingsFrame.J.r);
        this.levelViewToggle.setDisabledOverlayColor(OnlinePlayerPreviewSettingsFrame.J.r);
        this.addSettings(this.scaleSlider, this.framesPerSecondSlider, this.fieldOfViewSlider, this.levelViewToggle,
                new OnlinePlayerPreviewDividerComponent(this, OnlinePlayerPreviewSettingsFrame.J.l));
        this.h(new OnlinePlayerPreviewComponent(this), new Object[0]);
    }


    public static GuiComponent[] getObfuscationComponents() {
        return obfuscationComponents;
    }

    public static void setObfuscationComponents(GuiComponent[] components) {
        obfuscationComponents = components;
    }
}

