package vegas.sony.module.utility.inventory.cleaner.ui;

import vegas.sony.module.utility.inventory.cleaner.ui.InventoryItemPreviewIconComponent;
import vegas.sony.ui.click.animation.ColorAnimation;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.ImageRenderer;
import vegas.sony.wrapper.impl.Item;
import vegas.sony.wrapper.impl.ItemStack;
import java.awt.Color;
import org.jetbrains.annotations.Nullable;

public class InventoryItemPreviewComponent
extends GuiComponent {
    private final ColorAnimation closeIconAnimation;
    private final boolean removable;
    private static final String CLOSE_ICON = "newclose";
    private final InventoryItemPreviewIconComponent iconComponent;
    private final ColorAnimation dividerAnimation;

    @Override
    public double C() {
        return 14.0;
    }

    public InventoryItemPreviewComponent(@Nullable ItemStack itemStack, boolean removable) {
        this.dividerAnimation = new ColorAnimation(0.15, new Color(0, 0, 0, 0), InventoryItemPreviewComponent.J.d);
        this.closeIconAnimation = new ColorAnimation(0.15, new Color(0, 0, 0, 0), Color.WHITE);
        this.iconComponent = new InventoryItemPreviewIconComponent(this, itemStack);
        this.removable = removable;
        this.setPropagateMouseEvents(true);
        this.addChildren(this.iconComponent);
    }

    @Override
    public double x() {
        return 14.0;
    }

    public InventoryItemPreviewComponent(@Nullable Item item, boolean removable) {
        this.dividerAnimation = new ColorAnimation(0.15, new Color(0, 0, 0, 0), InventoryItemPreviewComponent.J.d);
        this.closeIconAnimation = new ColorAnimation(0.15, new Color(0, 0, 0, 0), Color.WHITE);
        this.iconComponent = new InventoryItemPreviewIconComponent(this, item);
        this.removable = removable;
        this.setPropagateMouseEvents(true);
        this.addChildren(this.iconComponent);
    }


    @Override
    public void c() {
        this.iconComponent.K(this.G$src$D$1b2f02a() + 1.0);
        this.iconComponent.S(this.n() + 1.0);
        super.c();
        if (this.removable) {
            GuiRenderPrimitives.V(this.G$src$D$1b2f02a() + this.A() - 5.0, this.n(), 6.0, 1.0, this.dividerAnimation.getInterpolatedColor());
            ImageRenderer.drawImage(this.closeIconAnimation.getInterpolatedColor(), (float)(this.G$src$D$1b2f02a() + this.A() - 5.5), (float)(this.n() - 1.0), CLOSE_ICON, 7.0f, 7.0f, false);
        }
    }

    @Override
    public void F() {
        if (!this.w$src$Z$e457mb()) {
            this.dividerAnimation.J();
            this.closeIconAnimation.J();
        }
    }

    @Override
    public void onEnable() {
        this.dividerAnimation.J();
        this.closeIconAnimation.J();
    }
}

