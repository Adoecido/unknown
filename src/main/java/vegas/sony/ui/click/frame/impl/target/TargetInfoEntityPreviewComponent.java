package vegas.sony.ui.click.frame.impl.target;

import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.impl.hud.HudModuleFrameBase;
import vegas.sony.utils.render.EntityModelRenderCache;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.wrapper.impl.EntityLivingBase;
import java.awt.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TargetInfoEntityPreviewComponent
extends GuiComponent {
    private static final Color BORDER_COLOR = new Color(255, 255, 255, 22);
    @NotNull
    private Color faceColor = Color.WHITE;
    private EntityLivingBase entity;
    private static final String NULL_FACE_COLOR_MESSAGE = "faceColor is marked non-null but is null";
    @Nullable
    private Color backgroundColor = new Color(100, 100, 100, 70);
    @Nullable
    private HudModuleFrameBase frame;

    public void setBackgroundColor(@Nullable Color color) {
        this.backgroundColor = color;
    }

    @NotNull
    public Color getFaceColor() {
        return this.faceColor;
    }

    public void setFaceColor(@NotNull Color color) {
        if (color == null) {
            throw new NullPointerException(NULL_FACE_COLOR_MESSAGE);
        }
        this.faceColor = color;
    }

    @Nullable
    public Color getBackgroundColor() {
        return this.backgroundColor;
    }

    public EntityLivingBase getEntity() {
        return this.entity;
    }

    public TargetInfoEntityPreviewComponent(double d, double d2) {
        this.o(d);
        this.Y(d2);
    }

    @Override
    public void H() {
        if (this.getEntity() == null || this.getEntity().isNull()) {
            return;
        }
        Color color = this.backgroundColor;
        Color color2 = this.faceColor;
        float f = 1.0f;
        if (this.frame != null) {
            if (color != null) {
                color = this.frame.applyDefaultEditorAlpha(color);
            }
            color2 = this.frame.applyDefaultEditorAlpha(color2);
            f = this.frame.getEditorOpacity();
        }
        if (color != null) {
            GuiRenderPrimitives.e(this.G$src$D$1b2f02a(), this.n(),
                    this.A(), this.L(), color, false, 3.0f, 1.0f);
            Color border = this.frame == null ? BORDER_COLOR
                    : this.frame.applyDefaultEditorAlpha(BORDER_COLOR);
            GuiRenderPrimitives.P(this.G$src$D$1b2f02a(), this.n(),
                    this.A(), this.L(), border, 3.0f, 0.6f, 1.0f);
        }
        EntityModelRenderCache.renderEntity(this.getEntity(), (float)this.G$src$D$1b2f02a(), (float)this.n(), (int)this.A(), (int)this.L(), color2, f);
    }

    public void setEntity(EntityLivingBase entityLivingBase) {
        this.entity = entityLivingBase;
    }

    public void setFrame(@Nullable HudModuleFrameBase frame) {
        this.frame = frame;
    }
}
