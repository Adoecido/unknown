package vegas.sony.render;

import vegas.sony.SonyVegas;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.wrapper.impl.EntityRenderer;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.GameSettings;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.ResourceLocation;
import vegas.sony.wrapper.impl.ShaderGroup;

public class ShaderGroupRenderStateManager {
    private boolean active;
    private ShaderGroup previousShaderGroup;
    private static final ShaderGroupRenderStateManager INSTANCE;
    public ShaderGroup activeShaderGroup;
    public boolean shadersPreviouslyEnabled;
    private int activeWidth = -1;
    private int activeHeight = -1;
    private static final int CONTROL_FLOW_SEED;

    public void enable() {
        if (this.active) {
            this.resizeIfNeeded();
            return;
        }
        if (ForgeVersion.MC_1_16_5.d() && ForgeVersion.MC_1_16_5_ACTUAL.B() && SonyVegas.INSTANCE.isForgeAbsent() && !SonyVegas.INSTANCE.isVanillaMinecraftPresent()) {
            return;
        }
        GameSettings gameSettings = Minecraft.gameSettings();
        if (gameSettings.d() > 0 || !gameSettings.Y$src$Z$1rxemad()) {
            return;
        }
        boolean shadersEnabled = gameSettings.M();
        if (shadersEnabled && NativeBridge.isForgeAbsent() && !SonyVegas.INSTANCE.isVanillaMinecraftPresent()) {
            return;
        }
        EntityRenderer entityRenderer = Minecraft.m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf();
        if (entityRenderer.isNull()) {
            return;
        }
        this.previousShaderGroup = entityRenderer.L();
        this.shadersPreviouslyEnabled = shadersEnabled;
        int displayWidth = Minecraft.J();
        int displayHeight = Minecraft.h();
        if (this.shadersPreviouslyEnabled) {
            gameSettings.m(false);
            Minecraft.getFrameBuffer().createBindFramebuffer(displayWidth, displayHeight);
            entityRenderer.updateShaderGroupSize(displayWidth, displayHeight);
            Minecraft.O().loadRenderers();
        }
        this.createShaderGroup(displayWidth, displayHeight);
        this.activeWidth = displayWidth;
        this.activeHeight = displayHeight;
        this.active = true;
    }

    private void resizeIfNeeded() {
        int displayWidth = Minecraft.J();
        int displayHeight = Minecraft.h();
        if (displayWidth <= 0 || displayHeight <= 0
                || (displayWidth == this.activeWidth && displayHeight == this.activeHeight)) {
            return;
        }
        if (this.activeShaderGroup == null || this.activeShaderGroup.isNull()) {
            this.createShaderGroup(displayWidth, displayHeight);
        } else {
            this.activeShaderGroup.resize(displayWidth, displayHeight);
        }
        this.activeWidth = displayWidth;
        this.activeHeight = displayHeight;
    }


    public boolean isActive() {
        return this.active;
    }

    public void disable() {
        this.disableInternal();
    }

    private void disableInternal() {
        if (!this.active) {
            return;
        }
        GameSettings gameSettings = Minecraft.gameSettings();
        if (gameSettings.d() > 0 || !gameSettings.Y$src$Z$1rxemad()) {
            return;
        }
        EntityRenderer entityRenderer = Minecraft.m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf();
        if (entityRenderer.isNull()) {
            return;
        }
        if (this.shadersPreviouslyEnabled) {
            int displayWidth = Minecraft.J();
            int displayHeight = Minecraft.h();
            Minecraft.getFrameBuffer().createBindFramebuffer(displayWidth, displayHeight);
            entityRenderer.updateShaderGroupSize(displayWidth, displayHeight);
            Minecraft.O().loadRenderers();
            gameSettings.m(true);
        }
        entityRenderer.setUseShader(false);
        entityRenderer.J(new ShaderGroup(null));
        this.activeShaderGroup = null;
        this.previousShaderGroup = null;
        this.activeWidth = -1;
        this.activeHeight = -1;
        this.active = false;
    }

    public static ShaderGroupRenderStateManager getInstance() {
        return INSTANCE;
    }

    static {
        long seed = 6018231015514832914L;
        CONTROL_FLOW_SEED = (int)seed;
        INSTANCE = new ShaderGroupRenderStateManager();
    }

    private void createShaderGroup(int displayWidth, int displayHeight) {
        this.activeShaderGroup = ShaderGroup.create(Minecraft.getTextureManager(), Minecraft.m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf().K(), Minecraft.getFrameBuffer(), new ResourceLocation(EntityRenderer.getShaderResourceLocations()[18]));
        Minecraft.m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf().J(this.activeShaderGroup);
        this.activeShaderGroup.resize(displayWidth, displayHeight);
        Minecraft.m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf().setUseShader(true);
    }
}

