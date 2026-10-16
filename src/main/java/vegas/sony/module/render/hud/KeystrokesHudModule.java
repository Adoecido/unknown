package vegas.sony.module.render.hud;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventKeyPress;
import vegas.sony.event.impl.EventMouseButton;
import vegas.sony.event.impl.SyntheticAttackRequestEvent;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.ui.click.frame.impl.hud.KeystrokesHudFrame;
import vegas.sony.unmap.ModeOption;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.wrapper.impl.KeyBinding;
import vegas.sony.wrapper.impl.Minecraft;

public class KeystrokesHudModule
extends HudModule {
    public final ModeOption keyboardKeyStyle = new ModeOption("Keyboard //$ui");
    public final ModeOption mouseIconStyle;
    public final ModeValue mouseStyle;
    public final ModeValue keyStyle;
    public final BooleanValue showCpsOnly;
    public final ModeOption mouseButtonStyle;
    public final BooleanValue showSpacebar;
    public final ModeOption arrowKeyStyle = new ModeOption("Arrow //$ui");

    @EventHandler
    public void onKeyPress(EventKeyPress eventKeyPress) {
        KeystrokesHudFrame keystrokesHudFrame = ClientSettings.getFrame(KeystrokesHudFrame.class);
        if (keystrokesHudFrame == null) {
            return;
        }
        keystrokesHudFrame.handleKeyEvent(eventKeyPress);
    }

    public KeystrokesHudModule() {
        super("Keystrokes //$ui", HudModuleGroup.HUD, "keystrokes //$", KeystrokesHudFrame.class);
        this.keyStyle = ModeValue.create((Object)this, "Key Style //$ui", this.keyboardKeyStyle, this.keyboardKeyStyle, this.arrowKeyStyle);
        this.mouseButtonStyle = new ModeOption("Button //$ui");
        this.mouseIconStyle = new ModeOption("Icon //$ui");
        this.mouseStyle = ModeValue.create((Object)this, "Mouse Style //$ui", this.mouseButtonStyle, this.mouseButtonStyle, this.mouseIconStyle);
        this.showSpacebar = BooleanValue.create(this, "Show Spacebar //$ui", true);
        this.showCpsOnly = BooleanValue.create(this, "Show CPS Only //$ui", false);
        this.setSuffix("Shows when your movement keys or mouse buttons are pressed\nAs well as mouse clicks per second //$ui");
        this.addValue(this.keyStyle, this.mouseStyle, this.showSpacebar, this.showCpsOnly);
    }

    public void updateKeyState(KeyBinding keyBinding, boolean pressed) {
        KeystrokesHudFrame keystrokesHudFrame = ClientSettings.getFrame(KeystrokesHudFrame.class);
        if (keystrokesHudFrame == null) {
            return;
        }
        keystrokesHudFrame.updateKeyState(keyBinding, pressed);
    }


    @EventHandler(priority=EventPriority.HIGHEST)
    public void onSyntheticAttack(SyntheticAttackRequestEvent event) {
        if (event.isCanceled() || event.isFollowedByMouseInput() || Minecraft.currentScreen().isNotNull()) {
            return;
        }
        KeystrokesHudFrame keystrokesHudFrame = ClientSettings.getFrame(KeystrokesHudFrame.class);
        if (keystrokesHudFrame == null) {
            return;
        }
        keystrokesHudFrame.registerSyntheticAttack();
    }

    @EventHandler
    public void onMouseButton(EventMouseButton eventMouseButton) {
        KeystrokesHudFrame keystrokesHudFrame = ClientSettings.getFrame(KeystrokesHudFrame.class);
        if (keystrokesHudFrame == null) {
            return;
        }
        keystrokesHudFrame.handleMouseEvent(eventMouseButton);
        if (!eventMouseButton.getButtonState()) {
            return;
        }
        if (Minecraft.currentScreen().isNotNull()) {
            return;
        }
        keystrokesHudFrame.getCpsCounter().recordClick(eventMouseButton.getButton());
    }
}

