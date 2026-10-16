package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.config.ClientSettings;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.utils.ThreadBoundExecutor;
import vegas.sony.utils.render.shader.ShaderProgram;
import vegas.sony.wrapper.impl.Minecraft;

public class EventTickBase
extends Event {
    private static final EventListeners EVENT_LISTENERS;
    private static GuiComponent[] obfuscationState;
    public static final ThreadBoundExecutor POST_TICK_EXECUTOR;
    public static final ThreadBoundExecutor PRE_TICK_EXECUTOR;

    public static void setTickObfuscationState(GuiComponent[] state) {
        obfuscationState = state;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    static {
        PRE_TICK_EXECUTOR = new ThreadBoundExecutor();
        POST_TICK_EXECUTOR = new ThreadBoundExecutor();
        EventTickBase.setTickObfuscationState(null);
        EVENT_LISTENERS = new EventListeners();
    }

    private static Exception identityException(Exception exception) {
        return exception;
    }

    @Override
    public boolean fire() {
        try {
            if (SonyVegas.INSTANCE.isTickActionPending()) {
                SonyVegas.INSTANCE.setPendingTickAction(false);
            }
            if (Minecraft.theWorld().isNotNull()) {
                ClientSettings.pendingSanityReset = true;
            }
            ShaderProgram.setCurrentProgramId(-1);
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
        return super.fire();
    }

    public static GuiComponent[] getTickObfuscationState() {
        return obfuscationState;
    }
}
