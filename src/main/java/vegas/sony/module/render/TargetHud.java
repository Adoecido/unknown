package vegas.sony.module.render;

import java.awt.Color;
import java.util.Locale;
import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventRender2D;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.combat.SilentAura;
import vegas.sony.module.gladiador.Target;
import vegas.sony.rotation.RotationManager;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.unmap.ModeOption;
import vegas.sony.utils.render.EntityModelRenderCache;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RayTraceResult;

/** Target HUD whose complete geometry and typography follow its Scale value. */
public final class TargetHud extends Mod {
    private static final Color CARD = new Color(12, 15, 21, 235);
    private static final Color CARD_SHADOW = new Color(0, 0, 0, 115);
    private static final Color BORDER = new Color(255, 255, 255, 30);
    private static final Color MUTED = new Color(190, 195, 205, 230);

    private final ModeOption j3Ultimate = new ModeOption("J3 Ultimate //$ui");
    private final ModeOption exhibition = new ModeOption("Exhibition //$ui");
    private final ModeOption astralis = new ModeOption("Astralis //$ui");
    private final ModeValue mode = ModeValue.create(this, "Mode //$ui", this.astralis,
            this.j3Ultimate, this.exhibition, this.astralis);
    private final NumberValue range = NumberValue.create(this, "Range //$ui", "#", "",
            2.0, 16.0, 64.0, 1.0);
    private final NumberValue horizontal = NumberValue.create(this, "X //$ui", "#", "%",
            0.0, 50.0, 100.0, 1.0);
    private final NumberValue vertical = NumberValue.create(this, "Y //$ui", "#", "%",
            0.0, 72.0, 100.0, 1.0);
    private final NumberValue scale = NumberValue.create(this, "Scale //$ui", "#.#", "x",
            0.5, 1.0, 2.0, 0.05);

    private Object animatedTarget;
    private float animatedHealth = -1.0f;
    private long lastAnimationNanos;

    public TargetHud() {
        super("TargetHUD //$ui", 0xB3F0FF, Category.RENDER,
                "Displays the current Aura, Target, or crosshair player //$ui");
        this.addValue(this.mode, this.range, this.horizontal, this.vertical, this.scale);
        this.range.setDescription("Maximum distance for a player to appear in TargetHUD //$ui");
        this.horizontal.setDescription("Horizontal HUD anchor //$ui");
        this.vertical.setDescription("Vertical HUD anchor //$ui");
        this.scale.setDescription("Scales the complete HUD, including text and model //$ui");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRender2D(EventRender2D event) {
        if (Minecraft.currentScreen().isNotNull()) return;
        EntityLivingBase target = this.resolveTarget();
        if (target == null || target.isNull()) return;
        float health;
        float maxHealth;
        try {
            health = Math.max(0.0f, target.w$src$F$15l9epb());
            maxHealth = Math.max(1.0f, target.I$src$F$14vyvep());
        } catch (Throwable ignored) {
            return;
        }
        maxHealth = Math.max(maxHealth, health);
        this.updateHealthAnimation(target.getObject(), health);
        double screenWidth = event.getDisplayWidth() / 2.0;
        double screenHeight = event.getDisplayHeight() / 2.0;
        double anchorX = this.getLayoutX(screenWidth);
        double anchorY = this.getLayoutY(screenHeight);
        double hudScale = this.scale.getValue();
        if (this.mode.getValue() == this.j3Ultimate) {
            this.drawJ3(target, health, maxHealth, anchorX, anchorY, hudScale);
        } else if (this.mode.getValue() == this.exhibition) {
            this.drawExhibition(target, health, maxHealth, anchorX, anchorY, hudScale);
        } else {
            this.drawAstralis(target, health, maxHealth, anchorX, anchorY, hudScale);
        }
    }

    /** Position entry point used by the in-GUI visual-layout brush. */
    public void setLayoutPosition(double x, double y, double screenWidth, double screenHeight) {
        if (screenWidth > 0.0) horizontal.setValue(clamp(x / screenWidth * 100.0, 0.0, 100.0));
        if (screenHeight > 0.0) vertical.setValue(clamp(y / screenHeight * 100.0, 0.0, 100.0));
    }

    public double getLayoutX(double screenWidth) {
        return screenWidth * horizontal.getValue() / 100.0;
    }

    public double getLayoutY(double screenHeight) {
        return screenHeight * vertical.getValue() / 100.0;
    }

    private void drawAstralis(EntityLivingBase target, float health, float maxHealth,
                              double anchorX, double anchorY, double hudScale) {
        SmoothFontRenderer title = visualFont(15, true, hudScale);
        SmoothFontRenderer detail = visualFont(11, false, hudScale);
        if (title == null || detail == null) return;
        String name = fit(title, cleanName(target.getName()), 108.0 * hudScale);
        double width = Math.max(174.0 * hudScale, 65.0 * hudScale + title.N(name) + 20.0 * hudScale);
        double height = 60.0 * hudScale;
        double x = anchorX - width / 2.0;
        double y = anchorY - height / 2.0;
        Color accent = accent();
        GuiRenderPrimitives.g(x, y, width, height, (float) (7.0 * hudScale),
                (float) (7.0 * hudScale), CARD_SHADOW);
        GuiRenderPrimitives.e(x, y, width, height, CARD, false, (float) (7.0 * hudScale), 1.0f);
        GuiRenderPrimitives.P(x, y, width, height, BORDER, (float) (7.0 * hudScale), 0.75f, 1.0f);
        GuiRenderPrimitives.j(x + 4.0 * hudScale, y + 5.0 * hudScale,
                2.0 * hudScale, height - 10.0 * hudScale, accent);
        double headSize = 42.0 * hudScale;
        double headX = x + 11.0 * hudScale;
        double headY = y + (height - headSize) / 2.0;
        GuiRenderPrimitives.e(headX, headY, headSize, headSize, new Color(5, 7, 10, 235),
                false, (float) (5.0 * hudScale), 1.0f);
        EntityModelRenderCache.renderEntity(target, (float) headX, (float) headY,
                Math.max(16, (int) Math.round(headSize)), Math.max(16, (int) Math.round(headSize)), Color.WHITE, 4.0f);
        double textX = headX + headSize + 9.0 * hudScale;
        title.d(name, textX, y + 9.0 * hudScale, Color.WHITE);
        detail.d(String.format(Locale.ROOT, "%.1f HP", health), textX, y + 26.0 * hudScale, secondary());
        double barX = textX;
        double barY = y + height - 15.0 * hudScale;
        double barWidth = Math.max(20.0 * hudScale, x + width - barX - 10.0 * hudScale);
        double barHeight = Math.max(3.0 * hudScale, 7.0 * hudScale);
        GuiRenderPrimitives.e(barX, barY, barWidth, barHeight, new Color(255, 255, 255, 32),
                false, (float) (3.0 * hudScale), 1.0f);
        double ratio = clamp(animatedHealth / maxHealth, 0.0, 1.0);
        if (ratio > 0.0) GuiRenderPrimitives.e(barX, barY, barWidth * ratio, barHeight, accent,
                false, (float) (3.0 * hudScale), 1.0f);
    }

    /** FDP J3 Ultimate's deliberately card-free two-line layout. */
    private void drawJ3(EntityLivingBase target, float health, float maxHealth,
                        double anchorX, double anchorY, double hudScale) {
        SmoothFontRenderer title = visualFont(13, true, hudScale);
        SmoothFontRenderer detail = visualFont(11, false, hudScale);
        if (title == null || detail == null) return;
        String name = "[" + cleanName(target.getName()) + "]";
        String hp = String.format(Locale.ROOT, "%.0f \u2665 / %.0f \u2665", health, maxHealth);
        double width = Math.max(36.0 * hudScale, Math.max(title.N(name), detail.N(hp)));
        double height = title.d(name) + detail.d(hp) + 5.0 * hudScale;
        double x = anchorX - width / 2.0;
        double y = anchorY - height / 2.0;
        title.d(name, x + (width - title.N(name)) / 2.0, y, accent());
        detail.d(hp, x + (width - detail.N(hp)) / 2.0, y + title.d(name) + 4.0 * hudScale, secondary());
    }

    /** FDP Exhibition-inspired compact card with its segmented health bar. */
    private void drawExhibition(EntityLivingBase target, float health, float maxHealth,
                                double anchorX, double anchorY, double hudScale) {
        SmoothFontRenderer title = visualFont(12, true, hudScale);
        SmoothFontRenderer detail = visualFont(9, false, hudScale);
        if (title == null || detail == null) return;
        String name = fit(title, cleanName(target.getName()), 90.0 * hudScale);
        double width = Math.max(126.0 * hudScale, 47.0 * hudScale + title.N(name) + 8.0 * hudScale);
        double height = 45.0 * hudScale;
        double x = anchorX - width / 2.0;
        double y = anchorY - height / 2.0;
        Color accent = accent();
        GuiRenderPrimitives.g(x, y, width, height, (float) (3.0 * hudScale),
                (float) (3.0 * hudScale), CARD_SHADOW);
        GuiRenderPrimitives.e(x, y, width, height, CARD, false, (float) (3.0 * hudScale), 1.0f);
        GuiRenderPrimitives.P(x, y, width, height, BORDER, (float) (3.0 * hudScale), 0.75f, 1.0f);
        double headSize = 39.0 * hudScale;
        double headX = x + 3.0 * hudScale;
        double headY = y + 3.0 * hudScale;
        EntityModelRenderCache.renderEntity(target, (float) headX, (float) headY,
                Math.max(16, (int) Math.round(headSize)), Math.max(16, (int) Math.round(headSize)), Color.WHITE, 3.0f);
        double textX = x + 46.0 * hudScale;
        title.d(name, textX, y + 5.0 * hudScale, Color.WHITE);
        double barX = textX;
        double barY = y + 15.0 * hudScale;
        double barWidth = Math.min(70.0 * hudScale, x + width - barX - 7.0 * hudScale);
        double barHeight = Math.max(2.5 * hudScale, 4.0 * hudScale);
        GuiRenderPrimitives.e(barX, barY, barWidth, barHeight, new Color(255, 255, 255, 35), false, 0.0f, 1.0f);
        GuiRenderPrimitives.e(barX, barY, barWidth * clamp(health / maxHealth, 0.0, 1.0), barHeight,
                accent, false, 0.0f, 1.0f);
        for (int segment = 1; segment < 10; segment++) {
            GuiRenderPrimitives.j(barX + barWidth * segment / 10.0, barY, Math.max(0.6, hudScale), barHeight,
                    new Color(0, 0, 0, 135));
        }
        double distance = 0.0;
        try { distance = Minecraft.thePlayer().i(target.z(), target.N(), target.h()); } catch (Throwable ignored) { }
        detail.d(String.format(Locale.ROOT, "%.1f HP  |  %.1fm", health, distance), textX,
                y + 23.0 * hudScale, secondary());
    }

    private SmoothFontRenderer visualFont(int baseSize, boolean semibold, double hudScale) {
        ArrayListModule arrayList = SonyVegas.INSTANCE.getModManager().getMod(ArrayListModule.class);
        int size = Math.max(8, (int) Math.round(baseSize * hudScale));
        return arrayList != null ? arrayList.getVisualFont(size, semibold)
                : SonyVegas.INSTANCE.getFontManager().getHudFont("inter", size, semibold);
    }

    private Color accent() {
        ArrayListModule arrayList = SonyVegas.INSTANCE.getModManager().getMod(ArrayListModule.class);
        Color color = arrayList == null ? new Color(179, 240, 255) : arrayList.getHudAccentColor();
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
    }

    private Color secondary() {
        ArrayListModule arrayList = SonyVegas.INSTANCE.getModManager().getMod(ArrayListModule.class);
        Color color = arrayList == null ? MUTED : arrayList.getHudSecondaryColor();
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 235);
    }

    private EntityLivingBase resolveTarget() {
        EntityLivingBase candidate = auraTarget();
        if (isEligible(candidate)) return candidate;
        candidate = targetModuleTarget();
        if (isEligible(candidate)) return candidate;
        try {
            RayTraceResult rayTrace = RotationManager.INSTANCE.getExtendedReachRayTrace();
            if (rayTrace != null && rayTrace.isNotNull() && rayTrace.getEntity().isNotNull()) {
                candidate = new EntityLivingBase(rayTrace.getEntity().getObject());
                if (isEligible(candidate)) return candidate;
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private EntityLivingBase auraTarget() {
        SilentAura aura = SonyVegas.INSTANCE.getModManager().getMod(SilentAura.class);
        return aura != null && aura.isEnabled() ? aura.getTarget() : null;
    }

    private EntityLivingBase targetModuleTarget() {
        Target targetModule = SonyVegas.INSTANCE.getModManager().getMod(Target.class);
        return targetModule != null && targetModule.isEnabled() ? targetModule.getCurrentTarget() : null;
    }

    private boolean isEligible(EntityLivingBase candidate) {
        if (candidate == null || candidate.isNull() || Minecraft.thePlayer().isNull()) return false;
        try {
            if (!candidate.isInstance(MappedClasses.Yl) || candidate.getObject() == Minecraft.thePlayer().getObject()
                    || candidate.M$src$Z$ff28xj() || candidate.w$src$F$15l9epb() <= 0.0f) return false;
            EntityPlayerSP player = Minecraft.thePlayer();
            return player.i(candidate.z(), candidate.N(), candidate.h()) <= range.getValue();
        } catch (Throwable ignored) { return false; }
    }

    private void updateHealthAnimation(Object targetHandle, float health) {
        long now = System.nanoTime();
        if (animatedTarget != targetHandle || animatedHealth < 0.0f) {
            animatedTarget = targetHandle;
            animatedHealth = health;
            lastAnimationNanos = now;
            return;
        }
        double elapsed = Math.max(0.0, Math.min(0.1, (now - lastAnimationNanos) / 1_000_000_000.0));
        lastAnimationNanos = now;
        animatedHealth += (health - animatedHealth) * (float) (1.0 - Math.exp(-10.0 * elapsed));
    }

    private static String cleanName(String name) {
        return name == null ? "Player" : name.replaceAll("\u00a7.", "").trim();
    }

    private static String fit(SmoothFontRenderer font, String text, double maximumWidth) {
        if (font.N(text) <= maximumWidth) return text;
        String suffix = "...";
        while (!text.isEmpty() && font.N(text + suffix) > maximumWidth) text = text.substring(0, text.length() - 1);
        return text + suffix;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
