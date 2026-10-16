package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.utils.MathUtil;
import vegas.sony.wrapper.impl.BlockPos;
import vegas.sony.wrapper.impl.BlockState;
import vegas.sony.wrapper.impl.ForgeVersion;

public class EventBlockModelRender
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private final Object rendererHandle;
    private final boolean checkSides;
    private final Object bufferHandle;
    private final Object blockAccessHandle;
    private final Object modelHandle;
    private boolean result;
    private final Object blockPosHandle;
    private final Object blockStateHandle;

    public EventBlockModelRender(Object rendererHandle, Object blockAccessHandle, Object modelHandle, Object blockStateHandle, Object blockPosHandle, Object bufferHandle, boolean checkSides) {
        this.rendererHandle = rendererHandle;
        this.blockAccessHandle = blockAccessHandle;
        this.modelHandle = modelHandle;
        this.blockStateHandle = blockStateHandle;
        this.blockPosHandle = blockPosHandle;
        this.bufferHandle = bufferHandle;
        this.checkSides = checkSides;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    private static Exception identityException(Exception exception) {
        return exception;
    }

    public boolean getResult() {
        return this.result;
    }

    @Override
    public boolean fire() {
        return false;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }
}
