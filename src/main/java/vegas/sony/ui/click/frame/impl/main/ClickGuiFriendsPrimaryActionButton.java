package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.ui.click.animation.ColorAnimation;
import vegas.sony.ui.click.component.gui.InteractiveComponent;
import vegas.sony.ui.click.frame.impl.main.ClickGuiFriendsFriendListComponent;
import vegas.sony.ui.click.frame.impl.main.ClickGuiFriendsRowActions;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.utils.render.GuiRenderPrimitives;

final class ClickGuiFriendsPrimaryActionButton
extends InteractiveComponent {
    private final String label;
    final ClickGuiFriendsFriendListComponent owner;
    private final ColorAnimation backgroundAnimation;

    @Override
    public void H() {
        this.backgroundAnimation.u(this.w$src$Z$e457mb());
        double d = this.G$src$D$1b2f02a();
        double d2 = this.n();
        double d3 = this.A();
        double d4 = this.L();
        GuiRenderPrimitives.B(d, d2, d3, d4, this.backgroundAnimation.getInterpolatedColor(), 2.0f);
        SmoothFontRenderer smoothFontRenderer = this.getAlternateFontRenderer(0.5);
        double d5 = smoothFontRenderer.N(this.label);
        double d6 = smoothFontRenderer.d(this.label);
        double d7 = d + (d3 - d5) / 2.0;
        double d8 = d2 + (d4 - d6) / 2.0;
        smoothFontRenderer.d(this.label, d7, d8, ClickGuiFriendsFriendListComponent.getPrimaryActionTextColor());
    }

    void updateWidth() {
        SmoothFontRenderer smoothFontRenderer = this.getAlternateFontRenderer(0.5);
        double d = smoothFontRenderer.N(this.label) + 12.0;
        this.o(Math.max(d, 14.0));
    }

    ClickGuiFriendsPrimaryActionButton(ClickGuiFriendsFriendListComponent clickGuiFriendsFriendListComponent, String string, ClickGuiFriendsRowActions clickGuiFriendsRowActions) {
        this(clickGuiFriendsFriendListComponent, string);
    }

    @Override
    public double x() {
        return this.A();
    }

    private ClickGuiFriendsPrimaryActionButton(ClickGuiFriendsFriendListComponent clickGuiFriendsFriendListComponent, String string) {
        this.owner = clickGuiFriendsFriendListComponent;
        this.backgroundAnimation = new ColorAnimation(0.15, ClickGuiFriendsFriendListComponent.getPrimaryActionColor(), ClickGuiFriendsFriendListComponent.getPrimaryActionHoverColor());
        this.label = string.toUpperCase();
        this.setShowDisabledOverlay(false);
        this.setVisible(false);
        this.updateWidth();
    }

    @Override
    public double C() {
        return 10.0;
    }
}
