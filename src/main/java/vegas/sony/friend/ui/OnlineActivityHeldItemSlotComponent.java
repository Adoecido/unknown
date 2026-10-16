package vegas.sony.friend.ui;

import vegas.sony.ui.click.component.ItemStackSlotComponent;
import vegas.sony.utils.MutableColor;
import vegas.sony.utils.render.GuiRenderPrimitives;

public class OnlineActivityHeldItemSlotComponent
extends ItemStackSlotComponent {
    private int disabledOverlayAlpha;

    public void setDisabledOverlayAlpha(int alpha) {
        this.disabledOverlayAlpha = alpha;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        GuiRenderPrimitives.e(this.G$src$D$1b2f02a(), this.n(), this.A(), this.L(), new MutableColor(OnlineActivityHeldItemSlotComponent.J.Z).withAlpha(this.disabledOverlayAlpha), false, 1.0f, 1.0f);
    }

    public int getDisabledOverlayAlpha() {
        return this.disabledOverlayAlpha;
    }
}
