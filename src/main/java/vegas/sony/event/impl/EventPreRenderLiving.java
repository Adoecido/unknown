package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.MatrixStack;

public class EventPreRenderLiving
extends Event {
    private final Object matrixStackHandle;
    private MatrixStack matrixStack;
    private Entity entity;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private final double x;
    private final double y;
    private final double z;
    private final Object entityHandle;

    public Entity getEntity() {
        if (this.entity == null) {
            this.entity = new Entity(this.entityHandle);
        }
        return this.entity;
    }

    public double getZ() {
        return this.z;
    }


    @Override
    public boolean fire() {
        return super.fire();
    }

    public double getY() {
        return this.y;
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    public MatrixStack getMatrixStack() {
        if (this.matrixStack == null) {
            this.matrixStack = new MatrixStack(this.matrixStackHandle);
        }
        return this.matrixStack;
    }

    public double getX() {
        return this.x;
    }

    public EventPreRenderLiving(Object entityHandle, double x, double y, double z, Object matrixStackHandle) {
        this.entityHandle = entityHandle;
        this.matrixStackHandle = matrixStackHandle;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

