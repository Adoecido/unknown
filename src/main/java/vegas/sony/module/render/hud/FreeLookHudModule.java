package vegas.sony.module.render.hud;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPostEntityRendererMouseUpdate;
import vegas.sony.event.impl.EventPostRenderTick;
import vegas.sony.event.impl.EventPostRenderWorldPass;
import vegas.sony.event.impl.EventPostTick;
import vegas.sony.event.impl.EventPreEntityRendererMouseUpdate;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.event.impl.EventPreRenderWorldPass;
import vegas.sony.event.impl.EventRenderPlayerPost;
import vegas.sony.event.impl.EventRenderPlayerPre;
import vegas.sony.event.impl.EventTickBase;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.rotation.RotationManager;
import vegas.sony.unmap.ModeOption;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.utils.MathUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;

public class FreeLookHudModule
extends HudModule {
    private static float savedPitch;
    private final ModeOption holdMode = new ModeOption("Hold //$ui");
    private final ModeOption forwardMode;
    private static float renderPitch;
    private static float originalYaw;
    private final ModeOption thirdPersonMode;
    private final NumberValue sensitivity;
    private static boolean active;
    private double mouseDY;
    private EntityLivingBase renderPlayer;
    private final ModeOption toggleMode = new ModeOption("Toggle //$ui");
    private static float savedYaw;
    private double mouseLastY;
    private final BooleanValue customSensitivity;
    private final ModeOption firstPersonMode;
    private static float renderYaw;
    private boolean enabled = false;
    public final ModeValue startingPositionMode;
    private static float originalPitch;
    private static float pitchOffset;
    private static float yawOffset;
    private double mouseLastX;
    public final ModeValue activationMode = ModeValue.create((Object)this, "Activate Freelook //$ui", this.holdMode, this.holdMode, this.toggleMode);
    public final ModeValue perspectiveMode;
    private final ModeOption backwardMode;
    private int savedPerspective = -1;
    private double mouseDX;


    public static float getRenderYaw() {
        return renderYaw;
    }

    @EventHandler
    public void onPostRenderTick(EventPostRenderTick event) {
        if (!this.enabled) {
            return;
        }
        this.restorePlayerView();
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPreRenderWorldPass(EventPreRenderWorldPass event) {
        this.applyWorldRotation();
    }

    private double getSensitivity() {
        if (!this.customSensitivity.getEffectiveValue().booleanValue()) {
            return (double)Minecraft.gameSettings().y() * 0.6 * 0.2 * 8.0;
        }
        return (Double)this.sensitivity.getValue();
    }

    private void applyRenderRotation() {
        if (!this.enabled) {
            return;
        }
        if (RotationManager.INSTANCE.isRenderingModuleRotation()) {
            return;
        }
        this.applyCameraRotation();
    }

    @Override
    public void onBindActivated() {
        if (!this.isEnabled() || Minecraft.currentScreen().isNotNull()) {
            return;
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            this.mouseLastX = Minecraft.s().R();
            this.mouseLastY = Minecraft.s().b();
        }
        if (this.activationMode.getValue() == this.toggleMode) {
            EventTickBase.POST_TICK_EXECUTOR.execute(this::toggleFreelook);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onRenderPlayerPost(EventRenderPlayerPost event) {
        this.applyRenderRotation();
    }

    @EventHandler
    public void onPreMouseUpdate(EventPreEntityRendererMouseUpdate event) {
        if (!active) {
            return;
        }
        if (!this.enabled) {
            return;
        }
        if (Minecraft.currentScreen().isNotNull()) {
            return;
        }
        this.applyCameraRotation();
    }

    @EventHandler
    public void onPostTick(EventPostTick event) {
        if (this.activationMode.getValue() != this.holdMode) {
            return;
        }
        if (Minecraft.currentScreen().isNotNull()) {
            return;
        }
        if (Minecraft.thePlayer().isNull()) {
            return;
        }
        this.enabled = this.getBind().areBoundInputsDown();
        if (!this.enabled) {
            active = false;
            if (this.savedPerspective == -1) {
                return;
            }
            Minecraft.gameSettings().I(this.savedPerspective);
            this.savedPerspective = -1;
            this.restorePlayerView();
        }
    }

    @EventHandler
    public void onPostMouseUpdate(EventPostEntityRendererMouseUpdate event) {
        if (!this.enabled) {
            return;
        }
        this.restorePlayerView();
    }

    public static boolean isActive() {
        return active;
    }

    private void toggleFreelook() {
        this.enabled = !this.enabled;
        if (!this.enabled) {
            active = false;
            if (this.savedPerspective == -1) {
                return;
            }
            Minecraft.gameSettings().I(this.savedPerspective);
            this.savedPerspective = -1;
            this.restorePlayerView();
        }
    }

    private void restorePlayerView() {
        EntityPlayerSP entityPlayerSP = Minecraft.thePlayer();
        if (entityPlayerSP.isNull() || Minecraft.theWorld().isNull()) {
            return;
        }
        entityPlayerSP.H(savedPitch);
        entityPlayerSP.D(savedPitch);
        entityPlayerSP.z(savedPitch);
        entityPlayerSP.o(savedPitch);
        entityPlayerSP.C(savedYaw);
        entityPlayerSP.l(savedYaw);
    }

    public void capturePlayerRotation(float pitch, float yaw) {
        savedPitch = pitch;
        savedYaw = yaw;
    }

    public static float getRenderPitch() {
        return renderPitch;
    }

    private void applyWorldRotation() {
        if (!this.enabled) {
            return;
        }
        if (RotationManager.INSTANCE.isRenderingModuleRotation()) {
            return;
        }
        this.restorePlayerView();
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onRenderPlayerPre(EventRenderPlayerPre event) {
        this.applyWorldRotation();
    }

    private void applyCameraRotation() {
        if (SharedModuleControlClaims.renderPass.isRenderBlocked()) {
            return;
        }
        this.renderPlayer.H(renderPitch);
        this.renderPlayer.D(renderPitch);
        this.renderPlayer.C(renderYaw);
        this.renderPlayer.l(renderYaw);
    }

    public static float getSavedPitch() {
        return savedPitch;
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPostRenderWorldPass(EventPostRenderWorldPass event) {
        this.applyRenderRotation();
    }

    public FreeLookHudModule() {
        super("Freelook //$ui", HudModuleGroup.GAME, "freelook2 //$");
        this.customSensitivity = BooleanValue.create(this, "Use Custom Sensitivity //$ui", false, "Enable to set a separate sensitivity from Minecraft using a slider //$ui");
        this.sensitivity = NumberValue.create(this, "Sensitivity //$ui", "#.#", "", 0.001, 0.5, 1.0);
        this.thirdPersonMode = new ModeOption("3rd Person //$ui");
        this.firstPersonMode = new ModeOption("1st Person //$ui");
        this.perspectiveMode = ModeValue.create((Object)this, "Perspective //$ui", this.thirdPersonMode, this.thirdPersonMode, this.firstPersonMode);
        this.forwardMode = new ModeOption("Forward //$ui");
        this.backwardMode = new ModeOption("Backward //$ui");
        this.startingPositionMode = ModeValue.create((Object)this, "Starting Position //$ui", this.forwardMode, this.forwardMode, this.backwardMode);
        this.setSuffix("Freely rotates your perspective //$ui");
        this.setShowKeybindSetting(true);
        this.addValue(this.activationMode, this.startingPositionMode, this.customSensitivity, this.sensitivity);
        this.customSensitivity.addDependentValues(this.sensitivity);
    }

    public static float getSavedYaw() {
        return savedYaw;
    }

    private void updateMouseDelta() {
        if (ForgeVersion.MC_1_16_5.d()) {
            this.mouseDX = this.mouseLastX - Minecraft.s().R();
            this.mouseDY = this.mouseLastY - Minecraft.s().b();
            this.mouseLastX = Minecraft.s().R();
            this.mouseLastY = Minecraft.s().b();
            return;
        }
        this.mouseDX = -Minecraft.s().d();
        this.mouseDY = Minecraft.s().z();
    }

    @EventHandler
    public void onPreRenderTick(EventPreRenderTick event) {
        int perspective;
        this.renderPlayer = event.getThePlayer();
        if (!this.enabled) {
            return;
        }
        if (!active) {
            originalYaw = this.renderPlayer.V();
            originalPitch = this.renderPlayer.J();
            this.savedPerspective = Minecraft.gameSettings().x();
            yawOffset = ((ModeSelection)this.startingPositionMode.getValue()).equals(this.forwardMode) ? 0.0f : 180.0f;
            pitchOffset = 0.0f;
            this.mouseDX = 0.0;
            this.mouseDY = 0.0;
            renderPitch = originalPitch;
            renderYaw = originalYaw;
            savedYaw = originalYaw;
            savedPitch = originalPitch;
            active = true;
            return;
        }
        perspective = ((ModeSelection)this.perspectiveMode.getValue()).equals(this.firstPersonMode) ? 0 : 1;
        if (Minecraft.gameSettings().x() != perspective) {
            Minecraft.gameSettings().I(perspective);
        }
        if (Minecraft.currentScreen().isNotNull()) {
            yawOffset = 0.0f;
            pitchOffset = 0.0f;
            this.mouseDX = this.mouseDY = (double)0.0f;
        }
        this.updateMouseDelta();
        double yawDelta = this.mouseDX * this.getSensitivity() * 0.15;
        double pitchDelta = this.mouseDY * this.getSensitivity() * 0.15;
        renderPitch = (float)(yawDelta - (double)yawOffset + (double)originalPitch);
        renderYaw = (float)(pitchDelta - (double)pitchOffset + (double)originalYaw);
        renderYaw = MathUtil.clamp(renderYaw, -90.0f, 90.0f);
        yawOffset = (float)(yawDelta + (double)yawOffset);
        pitchOffset = (float)(pitchDelta + (double)pitchOffset);
        pitchOffset = MathUtil.clamp(pitchOffset, -(90.0f - originalYaw), 90.0f + originalYaw);
    }
}
