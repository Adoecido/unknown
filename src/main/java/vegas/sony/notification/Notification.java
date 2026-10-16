package vegas.sony.notification;

import func.skidline.RectData;
import vegas.sony.SonyVegas;
import vegas.sony.module.render.ArrayListModule;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.OpenGlBackendHolder;
import vegas.sony.utils.render.RenderUtils;
import java.awt.Color;
import org.lwjgl.opengl.GL11;

public class Notification
extends AbstractNotification {
    private boolean started;
    private long expiresAt = Long.MAX_VALUE;
    private static final Color DEFAULT_ACCENT_COLOR = new Color(255, 72, 78);
    private static final Color BACKGROUND_COLOR = new Color(8, 8, 8, 230);
    private static final Color BACKGROUND_HOVER_COLOR = new Color(13, 13, 13, 238);
    private static final double HORIZONTAL_PADDING = 6.0;
    private static final double CARD_HEIGHT = 34.0;
    private static final double SINGLE_LINE_CARD_HEIGHT = 24.0;
    private static final double PROGRESS_INSET = 5.0;
    private static final double PROGRESS_HEIGHT = 2.5;
    private static final double PROGRESS_GAP = 2.0;
    private long durationMillis;

    private double getRenderX() {
        return SonyVegas.INSTANCE.getNotificationManager()
                .getNotificationRenderX(this.getCurrentX(), this.getWidth());
    }

    public long getExpiresAt() {
        return this.expiresAt;
    }


    @Override
    public boolean shouldRemove() {
        return this.isExpired() && this.getCurrentX() >= 4.0;
    }

    @Override
    public void handleClick(double mouseX, double mouseY) {
        if (!this.getBounds().J(mouseX, mouseY)) {
            return;
        }
        this.dismiss();
    }

    public Notification(NotificationType type, String title, NotificationContent content,
            double x, double y, long durationMillis) {
        super(type, title, content, x, y);
        this.durationMillis = Math.max(1L, durationMillis);
    }

    private double getRenderY() {
        return SonyVegas.INSTANCE.getNotificationManager()
                .getNotificationRenderY(this.getCurrentY(), this.getHeight());
    }

    public double getRemainingProgress() {
        return Math.max(Math.min((double)(this.expiresAt - System.currentTimeMillis())
                / (double)this.durationMillis, 1.0), 0.0);
    }

    public void setDuration(long durationMillis) {
        this.durationMillis = Math.max(1L, durationMillis);
        this.expiresAt = System.currentTimeMillis() + this.durationMillis;
    }

    @Override
    public double getWidth() {
        SmoothFontRenderer titleFont = SonyVegas.INSTANCE.getFontManager().W(0.82, true);
        double titleWidth = this.hasTitle() ? titleFont.N(this.getTitle()) : 0.0;
        double contentWidth = this.hasContent() ? this.getContent().getWidth() : 0.0;
        double textWidth = Math.max(titleWidth, contentWidth);
        return Math.max(82.0, textWidth + HORIZONTAL_PADDING * 2.0);
    }

    @Override
    public double getHeight() {
        return this.getCardHeight() + PROGRESS_GAP + PROGRESS_HEIGHT;
    }

    public void dismiss() {
        this.setTargetX(5.0);
    }

    @Override
    public void render() {
        if (!this.started) {
            this.started = true;
            this.setDuration(this.durationMillis);
        }
        float inverseScale = (float)(1.0 / SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor());
        boolean blendingEnabled = GL11.glIsEnabled((int)3042);
        float scale = 1.0f / inverseScale;
        if (GuiRenderPrimitives.d()) {
            OpenGlBackendHolder.backend.scale(scale, scale, scale);
        }
        double renderX = this.getRenderX();
        double renderY = this.getRenderY();
        double width = this.getWidth();
        double cardHeight = this.getCardHeight();
        Color accentColor = this.getAccentColor();
        boolean hovered = this.getBounds().Z(RenderUtils.h());
        GuiRenderPrimitives.e(renderX, renderY, width, cardHeight,
                hovered ? BACKGROUND_HOVER_COLOR : BACKGROUND_COLOR,
                false, 5.0f, 1.0f);
        SmoothFontRenderer titleFont = SonyVegas.INSTANCE.getFontManager().W(0.82, true);
        if (this.hasTitle() && this.hasContent()) {
            titleFont.d(this.getTitle(), renderX + HORIZONTAL_PADDING,
                    renderY + 4.0, accentColor);
            this.getContent().render(renderX + HORIZONTAL_PADDING,
                    renderY + 12.0);
        } else if (this.hasTitle()) {
            double titleY = renderY + (cardHeight - titleFont.d(this.getTitle())) / 2.0;
            titleFont.d(this.getTitle(), renderX + HORIZONTAL_PADDING,
                    titleY, accentColor);
        } else if (this.hasContent()) {
            double contentY = renderY + (cardHeight - this.getContent().getHeight()) / 2.0;
            this.getContent().render(renderX + HORIZONTAL_PADDING, contentY);
        }
        double remainingProgress = this.getRemainingProgress();
        if (remainingProgress > 0.0) {
            double availableProgressWidth = width - PROGRESS_INSET * 2.0;
            double progressWidth = availableProgressWidth * remainingProgress;
            double progressX = SonyVegas.INSTANCE.getNotificationManager().isDockedRight()
                    ? renderX + width - PROGRESS_INSET - progressWidth
                    : renderX + PROGRESS_INSET;
            GuiRenderPrimitives.e(progressX,
                    renderY + cardHeight + PROGRESS_GAP,
                    progressWidth, PROGRESS_HEIGHT,
                    accentColor, false, (float)(PROGRESS_HEIGHT / 2.0), 1.0f);
        }
        if (remainingProgress <= 0.0) {
            this.dismiss();
        }
        if (GuiRenderPrimitives.d()) {
            OpenGlBackendHolder.backend.scale(inverseScale, inverseScale, inverseScale);
        }
        OpenGlBackendHolder.backend.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (blendingEnabled) {
            GL11.glEnable((int)3042);
        }
    }

    private RectData getBounds() {
        return new RectData(this.getRenderX(), this.getRenderY(), this.getWidth(), this.getHeight());
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= this.expiresAt;
    }

    private boolean hasTitle() {
        return this.getTitle() != null && !this.getTitle().isEmpty();
    }

    private boolean hasContent() {
        return this.getContent() != null && this.getContent().getHeight() > 0.0;
    }

    private double getCardHeight() {
        if (this.hasTitle() && this.hasContent()) return CARD_HEIGHT;
        if (this.hasContent()) {
            return Math.max(SINGLE_LINE_CARD_HEIGHT,
                    this.getContent().getHeight() + 7.0);
        }
        return SINGLE_LINE_CARD_HEIGHT;
    }

    private Color getAccentColor() {
        try {
            ArrayListModule arrayList = SonyVegas.INSTANCE.getModManager()
                    .getMod(ArrayListModule.class);
            if (arrayList != null) {
                Color configured = arrayList.getHudAccentColor();
                return new Color(configured.getRed(), configured.getGreen(),
                        configured.getBlue(), 255);
            }
        } catch (Throwable ignored) {
        }
        return DEFAULT_ACCENT_COLOR;
    }
}

