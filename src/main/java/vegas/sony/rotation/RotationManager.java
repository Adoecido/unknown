package vegas.sony.rotation;

import vegas.sony.SonyVegas;
import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventMotion;
import vegas.sony.event.impl.EventMouseOverUpdate;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.event.impl.EventPostLocalPlayerTick;
import vegas.sony.event.impl.EventPostRenderTick;
import vegas.sony.event.impl.EventPostRenderWorldPass;
import vegas.sony.event.impl.EventPreEntityRendererMouseUpdate;
import vegas.sony.event.impl.EventPreLocalPlayerTick;
import vegas.sony.event.impl.EventPreMotion;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.event.impl.EventPreRenderWorldPass;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventRender2D;
import vegas.sony.event.impl.EventRenderPlayerPost;
import vegas.sony.event.impl.EventRenderPlayerPre;
import vegas.sony.event.impl.EventRightClickMouse;
import vegas.sony.event.impl.EventSendClickBlockToController;
import vegas.sony.event.impl.EventThreadBoundPostTick;
import vegas.sony.event.impl.EventThreadBoundPreTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.combat.Reach;
import vegas.sony.module.render.Rotate;
import vegas.sony.runtime.CheatBreakerDebugLog;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.movement.MovementInputHelper;
import vegas.sony.movement.PlayerMovementTaskManager;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.utils.MathUtil;
import vegas.sony.utils.MouseOverRayTraceUpdater;
import vegas.sony.utils.MutableColor;
import vegas.sony.utils.NanoTimerUtil;
import vegas.sony.utils.RayTraceUtil;
import vegas.sony.utils.RotationUtil;
import vegas.sony.utils.render.BufferedGuiRenderPrimitives;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.OpenGlBackendHolder;
import vegas.sony.utils.render.RenderUtils;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.C03PacketPlayer;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.GameSettings;
import vegas.sony.wrapper.impl.GlStateManager;
import vegas.sony.wrapper.impl.GuiScreen;
import vegas.sony.wrapper.impl.KeyBinding;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.PlayerInteractEventAction;
import vegas.sony.wrapper.impl.PlayerPositionLookPacketModern;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.S08PacketPlayerPosLook;
import vegas.sony.wrapper.impl.ScaledResolution;
import vegas.sony.wrapper.impl.World;
import java.awt.Color;
import java.util.Set;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public class RotationManager
implements EventListener {
    private boolean playerYawRestorePending;
    private MouseRotationController activeController;
    private float renderBodyYaw;
    private float savedPlayerYaw;
    private static final RayTraceResult EMPTY_RAY_TRACE;
    private float managedPitch;
    private float temporarilySavedPitch;
    public static final RotationManager INSTANCE;
    private float temporarilySavedYaw;
    private float motionYaw;
    private boolean rayTraceRefreshPending;
    private float managedYaw;
    private float previousRenderBodyYaw;
    private int controllerUpdatesThisTick;
    private float previousMotionYaw;
    private MouseButtonActionState forwardMovementOverride;
    private float previousMotionPitch;
    private float mouseSensitivity = 0.0f;
    private boolean movementKeysRemapped;
    private boolean savedLeftKeyState;
    private RayTraceResult normalReachRayTrace;
    private RayTraceResult extendedReachRayTrace;
    private boolean savedRightKeyState;
    private float savedPreviousPitch;
    private boolean savedForwardKeyState;
    private RayTraceResult cachedMouseOverRayTrace;
    private double controllerUpdateAccumulator;
    private boolean controllerHooksPending;
    private float savedPlayerPitch;
    private boolean savedBackKeyState;
    private float savedHeadYaw;
    private float savedPreviousHeadYaw;
    private float savedBodyYaw;
    private float savedPreviousBodyYaw;
    private int renderRotationDepth;
    private boolean managedRotationApplied;
    private boolean motionHookObserved;
    private final NanoTimerUtil controllerUpdateTimer = new NanoTimerUtil();
    private static final float DEFAULT_MOVEMENT_THRESHOLD = 0.4f;
    private boolean renderRotationActive;
    private boolean frameRotationApplied;
    private float frameSavedHeadYaw;
    private float frameSavedPreviousHeadYaw;
    private float frameSavedBodyYaw;
    private float frameSavedPreviousBodyYaw;
    private float motionPitch;
    private float savedRenderYawOffset;
    private long lastMotionControllerLog;

    private boolean shouldRenderModuleRotations() {
        Rotate rotate = SonyVegas.INSTANCE.getModManager().getMod(Rotate.class);
        return rotate != null && rotate.showsPlayerModel()
                || SonyVegas.INSTANCE.getClientSettings().thirdPersonAimView
                        .getEffectiveValue().booleanValue();
    }

    /**
     * True while a module rotation is being drawn onto the local player model.
     * Anything else that rewrites the rendered player angles (Freelook) has to
     * stand down for those frames, or the two fight over the same fields.
     */
    public boolean isRenderingModuleRotation() {
        return this.hasAdaptiveController() && this.shouldRenderModuleRotations();
    }

    private boolean shouldRenderAimIndicator() {
        Rotate rotate = SonyVegas.INSTANCE.getModManager().getMod(Rotate.class);
        return rotate != null && rotate.showsAimIndicator()
                || SonyVegas.INSTANCE.getClientSettings().aimIndicator
                        .getEffectiveValue().booleanValue();
    }

    public float adjustMovementYawFromBindings(float yaw, boolean useCurrentKeyState) {
        GameSettings settings = Minecraft.gameSettings();
        KeyBinding forwardKey = settings.Y();
        KeyBinding leftKey = settings.g$src$Lvegas_sony_wrapper_impl_KeyBinding_$qqn5n3();
        KeyBinding rightKey = settings.x$src$Lvegas_sony_wrapper_impl_KeyBinding_$1cf7isg();
        KeyBinding backKey = settings.s();
        boolean forward = useCurrentKeyState ? forwardKey.isKeyDown() : ClientSettings.isPhysicalKeyDown(forwardKey);
        boolean left = useCurrentKeyState ? leftKey.isKeyDown() : ClientSettings.isPhysicalKeyDown(leftKey);
        boolean right = useCurrentKeyState ? rightKey.isKeyDown() : ClientSettings.isPhysicalKeyDown(rightKey);
        boolean back = useCurrentKeyState ? backKey.isKeyDown() : ClientSettings.isPhysicalKeyDown(backKey);
        return this.adjustMovementYaw(yaw, forward, left, right, back);
    }

    private void updateBodyYaw(float targetYaw) {
        float targetDelta = MathUtil.wrapAngleTo180(targetYaw - this.renderBodyYaw);
        this.renderBodyYaw += targetDelta * 0.3f;
        float headDelta = MathUtil.wrapAngleTo180(this.motionYaw - this.renderBodyYaw);
        if (headDelta < -75.0f) {
            headDelta = -75.0f;
        }
        if (headDelta >= 75.0f) {
            headDelta = 75.0f;
        }
        this.renderBodyYaw = this.motionYaw - headDelta;
        if (headDelta * headDelta > 2500.0f) {
            this.renderBodyYaw += headDelta * 0.2f;
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPostLocalPlayerTick(EventPostLocalPlayerTick event) {
        EntityPlayerSP player = event.getPlayer();
        if (this.playerYawRestorePending) {
            player.H(this.savedPlayerYaw);
            player.z(this.savedPlayerYaw);
            player.F((float)((double)this.savedRenderYawOffset + (double)(this.savedPlayerYaw - this.savedRenderYawOffset) * 0.5));
            this.playerYawRestorePending = false;
        }
        if (this.movementKeysRemapped) {
            this.restoreMovementKeyStates();
            this.movementKeysRemapped = false;
        }
        if (this.renderRotationActive) {
            this.updatePlayerBodyYaw(player);
        }
    }

    public void releaseActiveController() {
        this.releaseController(this.activeController);
    }

    public static float getViewYaw(EntityPlayerSP player) {
        return player.J();
    }

    private void applyRenderRotation(EntityPlayerSP player) {
        if (!this.renderRotationActive || player.isNull()) {
            return;
        }
        // The world pass and the player render both fire this. Only the outermost
        // one may snapshot: otherwise the inner apply saves the already faked
        // angles and the matching restore writes them back permanently.
        if (this.renderRotationDepth++ > 0) {
            return;
        }
        this.savedPlayerPitch = player.V();
        this.savedPreviousPitch = player.D();
        this.savedHeadYaw = player.s();
        this.savedPreviousHeadYaw = player.P$src$F$14ztfk8();
        this.savedBodyYaw = player.W$src$F$153nzpr();
        this.savedPreviousBodyYaw = player.S$src$F$151gtcb();
        float yaw = this.getRenderRotationYaw();
        float pitch = this.getRenderRotationPitch();
        // Head and pitch get prev == current on purpose: the controller already
        // advances every render frame, so letting the renderer interpolate on top
        // of that only smears the model behind the actual silent rotation.
        player.z(yaw);
        player.o(yaw);
        player.X(this.renderBodyYaw);
        player.Y(this.previousRenderBodyYaw);
        player.C(pitch);
        player.l(pitch);
    }

    /** Live silent yaw for this frame, not the value latched at the last tick. */
    private float getRenderRotationYaw() {
        if (this.hasAdaptiveController()) {
            return ((AdaptiveRotationController)this.activeController).getRenderedYaw();
        }
        return this.motionYaw;
    }

    /** Live silent pitch for this frame, not the value latched at the last tick. */
    private float getRenderRotationPitch() {
        if (this.hasAdaptiveController()) {
            return MathUtil.clamp(
                    ((AdaptiveRotationController)this.activeController).getRenderedPitch(),
                    -90.0f, 90.0f);
        }
        return this.motionPitch;
    }

    private void resetControllerUpdateState() {
        this.controllerUpdateAccumulator = 0.0;
        this.controllerUpdatesThisTick = 0;
        this.controllerUpdateTimer.reset();
    }

    public MouseRotationController getActiveController() {
        return this.activeController;
    }

    private void applyManagedRotation(boolean apply) {
        EntityLivingBase player = Minecraft.F();
        if (apply) {
            this.temporarilySavedYaw = player.J();
            this.temporarilySavedPitch = player.V();
            player.H(this.managedYaw);
            player.z(this.managedYaw);
            player.C(this.managedPitch);
            this.managedRotationApplied = true;
        } else {
            player.H(this.temporarilySavedYaw);
            player.z(this.temporarilySavedYaw);
            player.C(this.temporarilySavedPitch);
            this.managedRotationApplied = false;
        }
    }

    public void releaseController(MouseRotationController controller) {
        if (this.activeController != null && this.activeController.equals(controller)) {
            if (this.activeController instanceof AdaptiveRotationController) {
                AdaptiveRotationController adaptiveController = (AdaptiveRotationController)this.activeController;
                adaptiveController.setRelativeMode(true);
            } else {
                this.activeController.setRetainAfterCompletion(false);
                this.activeController.setComplete(true);
            }
        }
    }

    public float getManagedYaw() {
        return this.hasAdaptiveController() ? this.managedYaw : Minecraft.F().J();
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPacketSend(EventPacketSend eventPacketSend) {
        if (this.hasAdaptiveController() && ForgeVersion.MC_1_20_6.v()
                && eventPacketSend.getPacket().isInstance(MappedClasses.qD)) {
            C03PacketPlayer movement = new C03PacketPlayer(eventPacketSend.getPacket());
            movement.setYaw(this.managedYaw);
            movement.setPitch(this.managedPitch);
            movement.setHasRotation(true);
        }
        if (ForgeVersion.MC_1_21_4.d() && this.hasAdaptiveController() && eventPacketSend.getPacket().isInstance(MappedClasses.Dg)) {
            S08PacketPlayerPosLook s08PacketPlayerPosLook = new S08PacketPlayerPosLook(eventPacketSend.getPacket());
            s08PacketPlayerPosLook.setPitch(this.managedPitch);
            s08PacketPlayerPosLook.setYaw(this.managedYaw);
        }
    }

    public boolean hasAdaptiveController() {
        return this.activeController != null && this.activeController instanceof AdaptiveRotationController;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onMouseOverUpdateHighest(EventMouseOverUpdate eventMouseOverUpdate) {
        if (this.hasAdaptiveController()) {
            this.applyManagedMouseOver();
        }
    }

    private void advanceControllerUpdates(double updateCount, EntityPlayerSP player, GuiScreen screen) {
        this.controllerUpdateAccumulator += updateCount;
        int wholeUpdates = (int)Math.round(this.controllerUpdateAccumulator);
        for (int updateIndex = 0; updateIndex < wholeUpdates; ++updateIndex) {
            try {
                this.activeController.update(player, screen);
                this.activeController.applyPendingMovement(screen);
            }
            catch (Exception exception) {
                // empty catch block
            }
            ++this.controllerUpdatesThisTick;
        }
        this.controllerUpdateAccumulator -= (double)wholeUpdates;
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPreEntityRendererMouseUpdate(EventPreEntityRendererMouseUpdate eventPreEntityRendererMouseUpdate) {
        if (this.controllerHooksPending && this.activeController != null) {
            this.activeController.onPreMouseUpdate(eventPreEntityRendererMouseUpdate);
        }
    }

    public void suppressForwardMovement() {
        this.forwardMovementOverride = MouseButtonActionState.RELEASE;
    }

    private void restoreRenderRotation(EntityPlayerSP player) {
        if (this.renderRotationDepth == 0 || --this.renderRotationDepth > 0 || player.isNull()) {
            return;
        }
        player.C(this.savedPlayerPitch);
        player.l(this.savedPreviousPitch);
        player.z(this.savedHeadYaw);
        player.o(this.savedPreviousHeadYaw);
        player.X(this.savedBodyYaw);
        player.Y(this.savedPreviousBodyYaw);
    }

    /**
     * Third person fallback for the player model rotation.
     *
     * <p>The entity render hook is the exact place to fake the model angles, but
     * it is installed by a bytecode transform that legacy runtimes can reject -
     * and then Rotate silently does nothing. Holding the render-only head and
     * body yaw for the whole frame produces the same picture without touching
     * the camera, which reads {@code rotationYaw} and {@code rotationPitch}.
     */
    private void applyFrameRenderRotation(EntityPlayerSP player) {
        if (this.frameRotationApplied || player.isNull()
                || !this.renderRotationActive
                || !this.shouldRenderModuleRotations()
                || Minecraft.gameSettings().x() <= 0) {
            return;
        }
        this.frameSavedHeadYaw = player.s();
        this.frameSavedPreviousHeadYaw = player.P$src$F$14ztfk8();
        this.frameSavedBodyYaw = player.W$src$F$153nzpr();
        this.frameSavedPreviousBodyYaw = player.S$src$F$151gtcb();
        float yaw = this.getRenderRotationYaw();
        player.z(yaw);
        player.o(yaw);
        player.X(this.renderBodyYaw);
        player.Y(this.previousRenderBodyYaw);
        this.frameRotationApplied = true;
    }

    private void restoreFrameRenderRotation(EntityPlayerSP player) {
        if (!this.frameRotationApplied) {
            return;
        }
        this.frameRotationApplied = false;
        if (player.isNull()) {
            return;
        }
        player.z(this.frameSavedHeadYaw);
        player.o(this.frameSavedPreviousHeadYaw);
        player.X(this.frameSavedBodyYaw);
        player.Y(this.frameSavedPreviousBodyYaw);
    }

    /**
     * Drops a render rotation that was applied but never restored - a perspective
     * switch between the two world pass hooks, or a render pass that threw. Without
     * this the faked head angles would stay latched on the player forever.
     */
    private void discardPendingRenderRotation() {
        if (this.renderRotationDepth == 0) {
            return;
        }
        this.renderRotationDepth = 1;
        this.restoreRenderRotation(Minecraft.thePlayer());
        this.renderRotationDepth = 0;
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        EntityPlayerSP player = event.getThePlayer();
        if (!this.shouldRenderAimIndicator()
                || player.isNull() || Minecraft.currentScreen().isNotNull()) {
            return;
        }
        boolean thirdPersonView = Minecraft.gameSettings().x() > 0;
        if (this.hasAdaptiveController() && !thirdPersonView) {
            AdaptiveRotationController controller = (AdaptiveRotationController)this.activeController;
            double centerZ = 0.0;
            float directionLineLength = 15.0f;
            float directionLineWidth = 2.0f;
            OpenGlBackendHolder.backend.pushMatrix();
            RenderUtils.g();
            ScaledResolution resolution = new ScaledResolution();
            float guiScale = 2.0f / (float)Minecraft.G().getScaleFactor();
            OpenGlBackendHolder.backend.translate(
                    resolution.getScaledWidthDouble() / 2.0 / (double)guiScale,
                    resolution.getScaledHeightDouble() / 2.0 / (double)guiScale,
                    centerZ);
            EntityLivingBase renderedPlayer = Minecraft.F();
            float partialTicks = Minecraft.getTimer().renderPartialTicks();
            float interpolatedPlayerYaw = renderedPlayer.J()
                    + (renderedPlayer.J() - renderedPlayer.j()) * partialTicks;
            float interpolatedPlayerPitch = renderedPlayer.V()
                    + (renderedPlayer.V() - renderedPlayer.D()) * partialTicks;
            float interpolatedManagedYaw = this.previousMotionYaw
                    + (this.motionYaw - this.previousMotionYaw) * partialTicks + 90.0f;
            OpenGlBackendHolder.backend.rotate(interpolatedPlayerPitch, -1.0f, 0.0f, 0.0f);
            OpenGlBackendHolder.backend.rotate(90.0f, 0.0f, -1.0f, 0.0f);
            OpenGlBackendHolder.backend.scale(-1.0f, -1.0f, -1.0f);
            OpenGlBackendHolder.backend.translate(0.0, 5.0, 0.5);
            OpenGlBackendHolder.backend.rotate(20.0f, 0.0f, 0.0f, 1.0f);
            this.drawLine(new Color(-1442840576, true), 3.0f, 0.0, -5.0, 0.0, 0.0, 5.0, 0.0);
            float relativeManagedYaw = interpolatedManagedYaw - interpolatedPlayerYaw - 90.0f;
            OpenGlBackendHolder.backend.rotate(relativeManagedYaw, 0.0f, -1.0f, 0.0f);
            this.drawLine(new Color(-1441787888), directionLineWidth + 1.0f,
                    0.0, 0.0, 0.0, directionLineLength, 0.0, 0.0);
            MutableColor lineColor = controller.usesSecondaryColor()
                    ? controller.getSecondaryColor() : controller.getPrimaryColor();
            this.drawLine(new MutableColor(lineColor).withAlpha(255), directionLineWidth,
                    0.0, 0.0, 0.0, directionLineLength, 0.0, 0.0);
            OpenGlBackendHolder.backend.rotate(relativeManagedYaw, 0.0f, 1.0f, 0.0f);
            OpenGlBackendHolder.backend.rotate(interpolatedPlayerYaw, 0.0f, 1.0f, 0.0f);
            boolean blendEnabled = OpenGlBackendHolder.backend.isCapabilityEnabled(3042);
            boolean lightingEnabled = OpenGlBackendHolder.backend.isCapabilityEnabled(2896);
            if (!blendEnabled) {
                OpenGlBackendHolder.backend.enableCapability(3042);
            }
            if (lightingEnabled) {
                OpenGlBackendHolder.backend.disableCapability(2896);
            }
            GL11.glBlendFunc((int)770, (int)771);
            OpenGlBackendHolder.backend.enableCapability(2848);
            OpenGlBackendHolder.backend.disableCapability(3553);
            double markerHalfSize = 5.0;
            Color markerOutlineColor = new Color(-1728053248, true);
            Color markerFillColor = new Color(-1719631744, true);
            if (GuiRenderPrimitives.d()) {
                BufferedGuiRenderPrimitives.drawLine3D(-markerHalfSize, -5.0, -markerHalfSize, markerHalfSize, -5.0, -markerHalfSize, directionLineWidth, markerOutlineColor);
                BufferedGuiRenderPrimitives.drawLine3D(markerHalfSize, -5.0, -markerHalfSize, markerHalfSize, -5.0, markerHalfSize, directionLineWidth, markerOutlineColor);
                BufferedGuiRenderPrimitives.drawLine3D(markerHalfSize, -5.0, markerHalfSize, -markerHalfSize, -5.0, markerHalfSize, directionLineWidth, markerOutlineColor);
                BufferedGuiRenderPrimitives.drawLine3D(-markerHalfSize, -5.0, markerHalfSize, -markerHalfSize, -5.0, -markerHalfSize, directionLineWidth, markerOutlineColor);
                BufferedGuiRenderPrimitives.fillQuad(-markerHalfSize, -5.0, -markerHalfSize, markerHalfSize, -5.0, -markerHalfSize, markerHalfSize, -5.0, markerHalfSize, -markerHalfSize, -5.0, markerHalfSize, markerFillColor);
            } else {
                GL11.glLineWidth((float)1.0f);
                GL11.glBegin((int)2);
                RenderUtils.w(markerOutlineColor);
                GL11.glVertex3d(-markerHalfSize, -5.0, -markerHalfSize);
                GL11.glVertex3d(markerHalfSize, -5.0, -markerHalfSize);
                GL11.glVertex3d(markerHalfSize, -5.0, markerHalfSize);
                GL11.glVertex3d(-markerHalfSize, -5.0, markerHalfSize);
                GL11.glEnd();
                GlStateManager.Y();
                GL11.glBegin((int)7);
                RenderUtils.w(markerFillColor);
                GL11.glVertex3d(-markerHalfSize, -5.0, -markerHalfSize);
                GL11.glVertex3d(markerHalfSize, -5.0, -markerHalfSize);
                GL11.glVertex3d(markerHalfSize, -5.0, markerHalfSize);
                GL11.glVertex3d(-markerHalfSize, -5.0, markerHalfSize);
                GL11.glEnd();
                GlStateManager.L();
            }
            if (lightingEnabled) {
                OpenGlBackendHolder.backend.enableCapability(2896);
            }
            if (!blendEnabled) {
                OpenGlBackendHolder.backend.disableCapability(3042);
            }
            OpenGlBackendHolder.backend.enableCapability(3553);
            OpenGlBackendHolder.backend.disableCapability(2848);
            RenderUtils.f();
            OpenGlBackendHolder.backend.popMatrix();
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onMotionUpdate(EventPreMotion event) {
        if (NativeBridge.isCheatBreaker189Runtime()) {
            long now = System.currentTimeMillis();
            boolean controllerActive = this.hasAdaptiveController();
            if (!this.motionHookObserved || controllerActive && now - this.lastMotionControllerLog >= 2000L) {
                this.lastMotionControllerLog = now;
                CheatBreakerDebugLog.info("ROTATION_EVENT controller="
                        + (this.activeController == null ? "none" : this.activeController.getClass().getName())
                        + " managed=" + this.managedYaw + "/" + this.managedPitch
                        + " event=" + EventMotion.getRotationYaw() + "/" + EventMotion.getRotationPitch());
            }
        }
        if (this.hasAdaptiveController()) {
            EventMotion.setRotationYaw(this.managedYaw);
            EventMotion.setRotationPitch(this.managedPitch);
        }
        this.motionHookObserved = true;
        this.refreshRenderRotation(Minecraft.thePlayer(),
                EventMotion.getRotationYaw(), EventMotion.getRotationPitch());
    }

    /**
     * Latches the rotation that actually leaves the client this tick so the local
     * player model can be drawn with it.
     *
     * <p>The comparison has to run against the untouched camera rotation: movement
     * correction and the tick bound managed rotation both rewrite the player yaw
     * before this point, so comparing against {@code player.J()} made every silent
     * rotation look identical to the real one and the render never engaged.
     */
    private void refreshRenderRotation(EntityPlayerSP player, float yaw, float pitch) {
        if (player.isNull()) {
            this.renderRotationActive = false;
            return;
        }
        boolean rotationChanged = yaw != this.getUnmanagedPlayerYaw(player)
                || pitch != this.getUnmanagedPlayerPitch(player);
        if (rotationChanged && !this.renderRotationActive) {
            // Entering the state: seed the history from the real model so the first
            // rendered frame swings out of the current pose instead of snapping in
            // from whatever the previous rotation left behind.
            this.renderBodyYaw = player.W$src$F$153nzpr();
            this.previousRenderBodyYaw = player.S$src$F$151gtcb();
            this.previousMotionYaw = player.g();
            this.previousMotionPitch = player.a$src$F$1txy325();
        } else {
            this.previousMotionYaw = this.motionYaw;
            this.previousMotionPitch = this.motionPitch;
        }
        this.motionYaw = yaw;
        this.motionPitch = pitch;
        this.renderRotationActive = rotationChanged;
    }

    /** Player yaw with our own temporary rewrites peeled back off. */
    private float getUnmanagedPlayerYaw(EntityPlayerSP player) {
        if (this.playerYawRestorePending) {
            return this.savedPlayerYaw;
        }
        if (this.managedRotationApplied) {
            return this.temporarilySavedYaw;
        }
        return player.J();
    }

    /** Player pitch with our own temporary rewrites peeled back off. */
    private float getUnmanagedPlayerPitch(EntityPlayerSP player) {
        return this.managedRotationApplied ? this.temporarilySavedPitch : player.V();
    }

    public float getPreviousManagedPitch() {
        return this.hasAdaptiveController() ? Minecraft.thePlayer().a$src$F$1txy325() : Minecraft.F().D();
    }

    public boolean isControllerUpdating() {
        return this.activeController != null && !this.activeController.isComplete();
    }

    private double getConfiguredReachDistance() {
        Reach reach = SonyVegas.INSTANCE.getModManager().getMod(Reach.class);
        return reach != null ? reach.getReachDistance() : 3.0;
    }


    private void refreshReachRayTraces() {
        // Reach is optional. It used to be registered unconditionally, so this
        // direct lookup became a silent per-tick NPE after the module was removed.
        double reachDistance = this.getConfiguredReachDistance();
        double hitBoxExpansion = 0.0;
        boolean extendedReach = reachDistance > 3.0;
        this.extendedReachRayTrace = this.rayTraceUsingManagedRotation(reachDistance, (float)hitBoxExpansion, extendedReach);
        this.normalReachRayTrace = this.rayTraceUsingManagedRotation(3.0, 0.0f, false);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onRightClickMouse(EventRightClickMouse eventRightClickMouse) {
        if (this.hasAdaptiveController()) {
            this.applyManagedMouseOver();
        }
    }

    public RotationManager() {
        this.forwardMovementOverride = MouseButtonActionState.NONE;
        this.extendedReachRayTrace = EMPTY_RAY_TRACE;
        this.normalReachRayTrace = EMPTY_RAY_TRACE;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onThreadBoundPreTick(EventThreadBoundPreTick eventThreadBoundPreTick) {
        if (this.hasAdaptiveController()) {
            this.applyManagedRotation(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPreRenderWorldPass(EventPreRenderWorldPass eventPreRenderWorldPass) {
        if (!this.shouldRenderModuleRotations() || Minecraft.gameSettings().x() <= 0) {
            return;
        }
        this.applyRenderRotation(eventPreRenderWorldPass.getPlayer());
    }

    private void updatePlayerBodyYaw(EntityPlayerSP player) {
        this.previousRenderBodyYaw = this.renderBodyYaw;
        double deltaX = player.z() - player.f();
        double deltaZ = player.h() - player.R();
        float horizontalDistanceSquared = (float)(deltaX * deltaX + deltaZ * deltaZ);
        float targetBodyYaw = this.renderBodyYaw;
        if (horizontalDistanceSquared > 0.0025000002f) {
            targetBodyYaw = (float)MathUtil.V(deltaZ, deltaX) * 180.0f / (float)Math.PI - 90.0f;
        }
        if (player.i() > 0) {
            targetBodyYaw = this.motionYaw;
        }
        this.updateBodyYaw(targetBodyYaw);
        while (this.renderBodyYaw - this.previousRenderBodyYaw < -180.0f) {
            this.previousRenderBodyYaw -= 360.0f;
        }
        while (this.renderBodyYaw - this.previousRenderBodyYaw >= 180.0f) {
            this.previousRenderBodyYaw += 360.0f;
        }
    }

    @EventHandler
    public void onPostRenderTick(EventPostRenderTick eventPostRenderTick) {
        this.restoreFrameRenderRotation(eventPostRenderTick.getThePlayer());
        if (this.controllerHooksPending) {
            if (this.activeController != null && eventPostRenderTick.getThePlayer().isNotNull()) {
                try {
                    this.activeController.onPostRenderTick(eventPostRenderTick);
                }
                catch (NullPointerException nullPointerException) {
                    SonyVegas.logThrowable(nullPointerException);
                }
            }
            this.controllerHooksPending = false;
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPostRenderWorldPass(EventPostRenderWorldPass eventPostRenderWorldPass) {
        if (!this.shouldRenderModuleRotations() || Minecraft.gameSettings().x() <= 0) {
            return;
        }
        this.restoreRenderRotation(eventPostRenderWorldPass.getPlayer());
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onSendClickBlockToController(EventSendClickBlockToController eventSendClickBlockToController) {
        if (this.hasAdaptiveController()) {
            this.applyManagedMouseOver();
        }
    }

    public float getMouseSensitivity() {
        return this.mouseSensitivity;
    }

    private void drawLine(Color color, float width, double startX, double startY, double startZ,
                          double endX, double endY, double endZ) {
        boolean blendEnabled = OpenGlBackendHolder.backend.isCapabilityEnabled(3042);
        boolean lightingEnabled = OpenGlBackendHolder.backend.isCapabilityEnabled(2896);
        GL11.glBlendFunc((int)770, (int)771);
        if (!blendEnabled) {
            OpenGlBackendHolder.backend.enableCapability(3042);
        }
        if (lightingEnabled) {
            OpenGlBackendHolder.backend.disableCapability(2896);
        }
        GL11.glBlendFunc((int)770, (int)771);
        OpenGlBackendHolder.backend.enableCapability(2848);
        OpenGlBackendHolder.backend.disableCapability(3553);
        if (GuiRenderPrimitives.d()) {
            BufferedGuiRenderPrimitives.drawLine3D(startX, startY, startZ, endX, endY, endZ, width, color);
        } else {
            GL11.glLineWidth(width);
            GL11.glBegin((int)1);
            RenderUtils.w(color);
            GL11.glVertex3d(startX, startY, startZ);
            GL11.glVertex3d(endX, endY, endZ);
            GL11.glEnd();
        }
        if (lightingEnabled) {
            OpenGlBackendHolder.backend.enableCapability(2896);
        }
        if (!blendEnabled) {
            OpenGlBackendHolder.backend.disableCapability(3042);
        }
        OpenGlBackendHolder.backend.enableCapability(3553);
        OpenGlBackendHolder.backend.disableCapability(2848);
    }

    public float getSensitivityTimeScale() {
        float sensitivityBase = INSTANCE.getMouseSensitivity() * 0.6f + 0.2f;
        float currentScale = sensitivityBase * sensitivityBase * sensitivityBase * 8.0f;
        float referenceBase = 0.5f;
        float referenceScale = referenceBase * referenceBase * referenceBase * 8.0f;
        return referenceScale / currentScale;
    }

    public float adjustMovementYaw(float yaw, boolean forward, boolean left, boolean right, boolean back) {
        float adjustedYaw = yaw;
        if (forward && left) {
            adjustedYaw += 45.0f;
        } else if (back && left) {
            adjustedYaw += 135.0f;
        } else if (left) {
            adjustedYaw += 90.0f;
        } else if (forward && right) {
            adjustedYaw -= 45.0f;
        } else if (back && right) {
            adjustedYaw -= 135.0f;
        } else if (right) {
            adjustedYaw -= 90.0f;
        } else if (back) {
            adjustedYaw += 180.0f;
        }
        return adjustedYaw;
    }

    static {
        EMPTY_RAY_TRACE = new RayTraceResult(null);
        INSTANCE = new RotationManager();
    }

    private void applyManagedMouseOver() {
        if (this.cachedMouseOverRayTrace == null || this.cachedMouseOverRayTrace.isNull()) {
            EntityLivingBase player = Minecraft.F();
            float savedYaw = player.J();
            float savedRenderYaw = player.s();
            float savedPitch = player.V();
            player.H(this.managedYaw);
            player.z(this.managedYaw);
            player.C(this.managedPitch);
            double reachDistance = 3.0;
            if (SonyVegas.INSTANCE.getClientSettings().useReach.getEffectiveValue().booleanValue()) {
                reachDistance = this.getConfiguredReachDistance();
            }
            double hitBoxExpansion = 0.0;
            MouseOverRayTraceUpdater.s((float)reachDistance, (float)hitBoxExpansion);
            this.cachedMouseOverRayTrace = Minecraft.p$src$Lvegas_sony_wrapper_impl_RayTraceResult_$5rw6n0();
            player.H(savedYaw);
            player.z(savedRenderYaw);
            player.C(savedPitch);
        }
        Minecraft.O(this.cachedMouseOverRayTrace);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPacketReceive(EventPacketReceive event) {
        if (!this.hasAdaptiveController() || event.isCanceled()) {
            return;
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return;
        }
        AdaptiveRotationController controller = (AdaptiveRotationController)this.activeController;
        Packet packet = event.getPacket();
        if (packet.isInstance(MappedClasses.zw)) {
            if (controller.isRelativeMode()) {
                controller.setComplete(true);
            } else {
                PlayerPositionLookPacketModern positionLookPacket = new PlayerPositionLookPacketModern(packet);
                float packetYaw = positionLookPacket.getYaw();
                float packetPitch = positionLookPacket.getPitch();
                if (ForgeVersion.MC_1_7_10.Y()) {
                    Set relativeFlags = positionLookPacket.getRelativeFlags();
                    for (Object relativeFlag : relativeFlags) {
                        PlayerInteractEventAction action = new PlayerInteractEventAction(relativeFlag);
                        if (action.T() == PlayerInteractEventAction.e()) {
                            player.C(this.managedPitch);
                            this.managedPitch += packetPitch;
                        }
                        if (action.T() != PlayerInteractEventAction.t()) continue;
                        player.H(this.managedYaw);
                        this.managedYaw += packetYaw;
                    }
                }
            }
        }
    }

    public RayTraceResult getNormalReachRayTrace() {
        return this.normalReachRayTrace;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onThreadBoundPostTick(EventThreadBoundPostTick eventThreadBoundPostTick) {
        if (this.hasAdaptiveController()) {
            this.applyManagedRotation(false);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPreLocalPlayerTick(EventPreLocalPlayerTick event) {
        if (Minecraft.thePlayer().isNull()) {
            this.movementKeysRemapped = false;
            return;
        }
        ModeSelection movementMode = (ModeSelection)SonyVegas.INSTANCE.getClientSettings().movementCorrection.getValue();
        if (movementMode.equals(ClientSettings.NO_MOVEMENT_CORRECTION) || !this.isMovementCorrectionMode(movementMode)) {
            return;
        }
        GameSettings settings = Minecraft.gameSettings();
        boolean keyStateMismatch = this.hasMovementKeyStateMismatch(settings);
        if (!this.hasAdaptiveController()) {
            if (this.movementKeysRemapped) {
                if (!keyStateMismatch) {
                    this.restoreMovementKeyStates();
                }
                this.movementKeysRemapped = false;
            }
            return;
        }

        EntityPlayerSP player = event.getPlayer();
        boolean movementActive = keyStateMismatch
                ? RotationUtil.x() : MovementInputHelper.isPhysicalMovementInputActive();
        boolean shouldRemapMovement = !this.isRotationBlockedByScreen() && movementActive;
        AdaptiveRotationController controller = (AdaptiveRotationController)this.activeController;
        this.savedPlayerYaw = player.J();
        this.savedRenderYawOffset = player.q$src$F$1u6qsjx();
        float referenceYaw = controller.getReferenceYaw();
        float movementYaw = this.adjustMovementYawFromBindings(referenceYaw, keyStateMismatch);
        float appliedPlayerYaw = movementMode.equals(ClientSettings.SLOW_MOVEMENT_CORRECTION)
                ? movementYaw + 180.0f : this.managedYaw;
        player.H(appliedPlayerYaw);
        player.z(appliedPlayerYaw);
        this.playerYawRestorePending = true;

        if (shouldRemapMovement) {
            float relativeMovementYaw = MathUtil.wrapAngleTo180(
                    MathUtil.wrapAngleTo180(appliedPlayerYaw) - movementYaw);
            float relativeMovementRadians = relativeMovementYaw * ((float)Math.PI / 180);
            float forwardProjection = (float)Math.cos(relativeMovementRadians);
            float leftProjection = (float)(-Math.sin(relativeMovementRadians));
            double movementThreshold = PlayerMovementTaskManager.INSTANCE.getActiveTask() != null
                    ? 0.075 : (double)DEFAULT_MOVEMENT_THRESHOLD;
            boolean pressForward = (double)forwardProjection >= movementThreshold;
            boolean pressLeft = (double)leftProjection >= movementThreshold;
            boolean pressRight = (double)leftProjection <= -movementThreshold;
            boolean pressBack = (double)forwardProjection <= -movementThreshold;
            if (this.forwardMovementOverride == MouseButtonActionState.PRESS) {
                pressForward = true;
                pressBack = false;
            } else if (this.forwardMovementOverride == MouseButtonActionState.RELEASE) {
                pressForward = false;
                pressBack = false;
            }
            this.savedForwardKeyState = settings.Y().isKeyDown();
            this.savedLeftKeyState = settings.g$src$Lvegas_sony_wrapper_impl_KeyBinding_$qqn5n3().isKeyDown();
            this.savedRightKeyState = settings.x$src$Lvegas_sony_wrapper_impl_KeyBinding_$1cf7isg().isKeyDown();
            this.savedBackKeyState = settings.s().isKeyDown();
            settings.Y().setPressed(pressForward);
            settings.g$src$Lvegas_sony_wrapper_impl_KeyBinding_$qqn5n3().setPressed(pressLeft);
            settings.x$src$Lvegas_sony_wrapper_impl_KeyBinding_$1cf7isg().setPressed(pressRight);
            settings.s().setPressed(pressBack);
            this.movementKeysRemapped = true;
        } else if (this.movementKeysRemapped) {
            if (!keyStateMismatch) {
                this.restoreMovementKeyStates();
            }
            this.movementKeysRemapped = false;
        }
    }

    private boolean isMovementCorrectionMode(ModeSelection mode) {
        return mode.equals(ClientSettings.PROPER_MOVEMENT_CORRECTION) || mode.equals(ClientSettings.SLOW_MOVEMENT_CORRECTION);
    }

    private boolean hasMovementKeyStateMismatch(GameSettings settings) {
        KeyBinding[] movementKeys = new KeyBinding[]{
                settings.Y(),
                settings.g$src$Lvegas_sony_wrapper_impl_KeyBinding_$qqn5n3(),
                settings.x$src$Lvegas_sony_wrapper_impl_KeyBinding_$1cf7isg(),
                settings.s()
        };
        for (KeyBinding key : movementKeys) {
            if (ClientSettings.isPhysicalKeyDown(key) != key.isKeyDown()) {
                return true;
            }
        }
        return false;
    }

    public RayTraceResult rayTraceUsingManagedRotation(double distance, float hitBoxExpansion, boolean extendedReach) {
        return this.rayTraceUsingManagedRotation(distance, hitBoxExpansion, extendedReach, null);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onRenderPlayerPre(EventRenderPlayerPre eventRenderPlayerPre) {
        if (!this.shouldRenderModuleRotations()) {
            return;
        }
        EntityPlayer player = eventRenderPlayerPre.getEntityPlayer();
        if (!player.isInstance(MappedClasses.z5)) {
            return;
        }
        this.applyRenderRotation(new EntityPlayerSP(player));
    }

    public RayTraceResult rayTraceUsingManagedRotation(boolean includeFluids) {
        RayTraceResult result;
        EntityPlayerSP player = Minecraft.thePlayer();
        World world = player.getWorld();
        if (this.hasAdaptiveController()) {
            EntityLivingBase renderedPlayer = Minecraft.F();
            float savedYaw = renderedPlayer.J();
            float savedRenderYaw = renderedPlayer.s();
            float savedPitch = renderedPlayer.V();
            renderedPlayer.H(this.managedYaw);
            renderedPlayer.z(this.managedYaw);
            renderedPlayer.C(this.managedPitch);
            result = RayTraceUtil.p(world, player, includeFluids);
            renderedPlayer.H(savedYaw);
            renderedPlayer.z(savedRenderYaw);
            renderedPlayer.C(savedPitch);
        } else {
            result = RayTraceUtil.p(world, player, includeFluids);
        }
        return result;
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onTick(EventPreTick event) {
        EntityPlayerSP player = event.getThePlayer();
        GuiScreen screen = event.getCurrentScreen();
        this.mouseSensitivity = Minecraft.gameSettings().y();
        this.rayTraceRefreshPending = true;
        this.cachedMouseOverRayTrace = EMPTY_RAY_TRACE;
        if (player.isNotNull()) {
            this.refreshReachRayTraces();
        }
        if (this.activeController == null || player.isNull()) {
            this.activeController = null;
            this.resetControllerUpdateState();
            return;
        }
        if (this.activeController.isComplete() && !this.activeController.shouldRetainAfterCompletion()) {
            this.activeController = null;
        }
        if (this.activeController == null) {
            this.resetControllerUpdateState();
            return;
        }
        this.controllerUpdateTimer.reset();
        double targetUpdates = Math.round(50.0f * this.getSensitivityTimeScale());
        double remainingUpdates = Math.max(targetUpdates - (double)this.controllerUpdatesThisTick, 0.0);
        this.advanceControllerUpdates(remainingUpdates, player, screen);
        this.controllerUpdatesThisTick = 0;
        if (this.hasAdaptiveController()) {
            AdaptiveRotationController controller = (AdaptiveRotationController)this.activeController;
            this.managedYaw = controller.getRenderedYaw();
            this.managedPitch = MathUtil.clamp(controller.getRenderedPitch(), -90.0f, 90.0f);
            if (!this.motionHookObserved) {
                // Some versions never route us through the motion hook; keep the
                // render state alive straight from the managed rotation instead.
                this.refreshRenderRotation(player, this.managedYaw, this.managedPitch);
            }
            this.applyManagedMouseOver();
        }
        this.refreshReachRayTraces();
    }

    public static float getViewPitch(EntityPlayerSP player) {
        return player.V();
    }

    private boolean isRotationBlockedByScreen() {
        return Minecraft.currentScreen().isNotNull();
    }

    public float getManagedPitch() {
        return this.hasAdaptiveController() ? this.managedPitch : Minecraft.F().V();
    }

    @EventHandler
    public void onPreRenderTick(EventPreRenderTick event) {
        EntityPlayerSP player = event.getThePlayer();
        this.discardPendingRenderRotation();
        this.restoreFrameRenderRotation(player);
        try {
            this.advanceControllerForRenderTick(event, player);
        } finally {
            // After the controller advanced, so the model is drawn with the
            // rotation this very frame sends out.
            this.applyFrameRenderRotation(player);
        }
    }

    private void advanceControllerForRenderTick(EventPreRenderTick event, EntityPlayerSP player) {
        if (this.activeController == null || player.isNull()) {
            this.activeController = null;
            this.resetControllerUpdateState();
            return;
        }
        if (this.activeController.isComplete() && !this.activeController.shouldRetainAfterCompletion()) {
            this.activeController = null;
        }
        if (this.activeController == null) {
            this.resetControllerUpdateState();
            return;
        }
        double timerSpeed = Minecraft.getTimer().getTimerSpeed();
        double elapsedGameMillis = this.controllerUpdateTimer.getElapsedMilliseconds() * timerSpeed;
        this.controllerUpdateTimer.reset();
        double scaledUpdates = elapsedGameMillis * (double)this.getSensitivityTimeScale();
        this.advanceControllerUpdates(scaledUpdates, player, event.getCurrentScreen());
        this.controllerHooksPending = true;
        this.activeController.capturePlayerRotation(event);
    }

    public RayTraceResult getExtendedReachRayTrace() {
        return this.extendedReachRayTrace;
    }

    public void setController(@NotNull MouseRotationController controller) {
        AdaptiveRotationController previousAdaptiveController;
        if (this.activeController == controller) {
            return;
        }
        if (NativeBridge.isCheatBreaker189Runtime()) {
            CheatBreakerDebugLog.info("ROTATION_CONTROLLER from="
                    + (this.activeController == null ? "none" : this.activeController.getClass().getName())
                    + " to=" + controller.getClass().getName());
        }
        if (this.activeController instanceof AdaptiveRotationController
                && controller instanceof AdaptiveRotationController) {
            previousAdaptiveController = (AdaptiveRotationController)this.activeController;
            previousAdaptiveController.setRelativeMode(true);
            previousAdaptiveController.setComplete(true);
            AdaptiveRotationController nextAdaptiveController = (AdaptiveRotationController)controller;
            nextAdaptiveController.setRelativeMode(false);
            nextAdaptiveController.setCurrentYaw(previousAdaptiveController.getRenderedYaw());
            nextAdaptiveController.setCurrentPitch(previousAdaptiveController.getRenderedPitch());
        }
        if (this.activeController == null && controller instanceof AdaptiveRotationController) {
            AdaptiveRotationController adaptiveController = (AdaptiveRotationController)controller;
            this.managedYaw = adaptiveController.getRenderedYaw();
            this.managedPitch = adaptiveController.getRenderedPitch();
        }
        this.activeController = controller;
    }

    @EventHandler(priority=EventPriority.LOW)
    public void onMouseOverUpdate(EventMouseOverUpdate eventMouseOverUpdate) {
        if (!this.rayTraceRefreshPending) {
            return;
        }
        if (Minecraft.thePlayer().isNotNull()) {
            this.refreshReachRayTraces();
            this.rayTraceRefreshPending = false;
        }
    }

    public double getControllerUpdateAccumulator() {
        return this.controllerUpdateAccumulator;
    }

    private void restoreMovementKeyStates() {
        GameSettings settings = Minecraft.gameSettings();
        MovementInputHelper.synchronizeKeyState(settings.Y(), this.savedForwardKeyState);
        MovementInputHelper.synchronizeKeyState(settings.g$src$Lvegas_sony_wrapper_impl_KeyBinding_$qqn5n3(), this.savedLeftKeyState);
        MovementInputHelper.synchronizeKeyState(settings.x$src$Lvegas_sony_wrapper_impl_KeyBinding_$1cf7isg(), this.savedRightKeyState);
        MovementInputHelper.synchronizeKeyState(settings.s(), this.savedBackKeyState);
    }

    public RayTraceResult rayTraceUsingManagedRotation(double distance, float hitBoxExpansion,
                                                       boolean extendedReach,
                                                       @Nullable Predicate<Entity> predicate) {
        RayTraceResult result;
        if (this.hasAdaptiveController()) {
            EntityLivingBase player = Minecraft.F();
            float savedYaw = player.J();
            float savedRenderYaw = player.s();
            float savedPitch = player.V();
            player.H(this.managedYaw);
            player.z(this.managedYaw);
            player.C(this.managedPitch);
            result = RayTraceUtil.U(player, distance, hitBoxExpansion, extendedReach, predicate);
            player.H(savedYaw);
            player.z(savedRenderYaw);
            player.C(savedPitch);
        } else {
            result = RayTraceUtil.q(distance, hitBoxExpansion, extendedReach, predicate);
        }
        return result;
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onRenderPlayerPost(EventRenderPlayerPost eventRenderPlayerPost) {
        if (!this.shouldRenderModuleRotations()) {
            return;
        }
        EntityPlayer player = eventRenderPlayerPost.getEntityPlayer();
        if (!player.isInstance(MappedClasses.z5)) {
            return;
        }
        this.restoreRenderRotation(new EntityPlayerSP(player));
    }

    public void setForwardMovementOverride(boolean pressed) {
        this.forwardMovementOverride = pressed ? MouseButtonActionState.PRESS : MouseButtonActionState.NONE;
    }

    public float getPreviousManagedYaw() {
        return this.hasAdaptiveController() ? Minecraft.thePlayer().g() : Minecraft.F().j();
    }
}
