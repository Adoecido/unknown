package vegas.sony.event.impl;

import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.MatrixStack;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RenderPlayer;

public class EventRenderPlayerPre
extends Event {
    private double y;
    private final float partialTicks;
    private double x;
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private final EntityPlayer player;
    private final MatrixStack matrixStack;
    private final RenderPlayer renderer;
    private double z;

    public EventRenderPlayerPre(Object rendererHandle, Object playerHandle, double x, double y, double z, float partialTicks) {
        this.renderer = new RenderPlayer(rendererHandle);
        this.player = new EntityPlayer(playerHandle);
        this.partialTicks = partialTicks;
        this.matrixStack = null;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean fire() {
        return super.fire();
    }

    public double getZ() {
        return this.z;
    }

    public EventRenderPlayerPre(Object rendererHandle, Object playerHandle, float partialTicks, Object matrixStackHandle) {
        this.renderer = new RenderPlayer(rendererHandle);
        this.player = new EntityPlayer(playerHandle);
        this.partialTicks = partialTicks;
        this.matrixStack = new MatrixStack(matrixStackHandle);
        this.x = this.player.z();
        this.y = this.player.N();
        this.z = this.player.h();
    }

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public EntityPlayer getEntityPlayer() {
        return this.player;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }

    public EventRenderPlayerPre(Object playerHandle, Object matrixStackHandle, Object rendererHandle) {
        this.renderer = new RenderPlayer(rendererHandle);
        this.player = new EntityPlayer(playerHandle);
        this.matrixStack = new MatrixStack(matrixStackHandle);
        this.partialTicks = Minecraft.getTimer().renderPartialTicks();
        this.x = this.player.z();
        this.y = this.player.N();
        this.z = this.player.h();
    }

    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public MatrixStack getMatrixStack() {
        return this.matrixStack;
    }

    public RenderPlayer getRenderer() {
        return this.renderer;
    }
}
