package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.config.ClientSettings;
import vegas.sony.config.Profile;
import vegas.sony.event.EventBus;
import vegas.sony.event.EventListeners;
import vegas.sony.event.impl.EventKeyInputBase;
import vegas.sony.input.KeyboardInput;
import vegas.sony.manager.client.OnlineConnectionManager;
import vegas.sony.module.Mod;
import vegas.sony.wrapper.impl.KeyBinding;
import vegas.sony.wrapper.impl.Minecraft;

public class EventKeyPress
extends EventKeyInputBase {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();

    public EventKeyPress(int key, boolean down) {
        super(key, down);
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    @Override
    public boolean fire() {
        // Module binds are game controls.  Handle them before the click-GUI
        // shortcut branch, which intentionally consumes keyboard input while
        // the client UI is transitioning.  Mod.u() still refuses input while
        // any Minecraft screen is open, so typing in chat or a menu cannot
        // toggle a module.
        if (this.getKey() > 0 && Minecraft.currentScreen().getObject() == null) {
            if (this.isDown()) {
                for (Profile profile : SonyVegas.INSTANCE.getProfilesManager().getProfiles()) {
                    profile.activateIfMatched(this.getKey());
                }
            }
            for (Mod mod : SonyVegas.INSTANCE.getModManager().collectMods()) {
                mod.u(this);
            }
        }
        if (!vegas.sony.module.none.ClientSettings.INSTANCE.isInputEnabled() && KeyboardInput.isKeyDown(9) && KeyboardInput.isKeyDown(114)) {
            boolean timingEnabled = EventBus.timingEnabled = !EventBus.timingEnabled;
        }
        if (!vegas.sony.module.none.ClientSettings.INSTANCE.isInputEnabled() && this.getKey() != 27) {
            boolean handled = vegas.sony.module.none.ClientSettings.INSTANCE.handleSearchShortcut(this);
            return handled;
        }
        OnlineConnectionManager.INSTANCE.getSettings().handleKeyPress(this);
        return super.fire();
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    public boolean isKeybinding(KeyBinding keyBinding) {
        return this.getKey() == ClientSettings.getPlatformKeyCode(keyBinding);
    }

}

