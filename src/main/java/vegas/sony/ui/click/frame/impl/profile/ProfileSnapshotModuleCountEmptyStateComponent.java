package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.font.SmoothFontRenderer;

public class ProfileSnapshotModuleCountEmptyStateComponent
extends GuiComponent {
    private final int moduleCount;
    private static final String LABEL = "AFFECTED MODULES";

    @Override
    public void F() {
    }

    @Override
    public double C() {
        return 8.0;
    }

    @Override
    public void g(GuiMouseEvent guiMouseEvent) {
    }

    public ProfileSnapshotModuleCountEmptyStateComponent(int moduleCount) {
        this.moduleCount = moduleCount;
    }

    @Override
    public void u() {
    }

    @Override
    public double x() {
        return 110.0;
    }

    @Override
    public void I() {
    }

    @Override
    public void H() {
        SmoothFontRenderer smoothFontRenderer = this.getAlternateFontRenderer(0.8);
        SmoothFontRenderer smoothFontRenderer2 = this.getAlternateFontRenderer(0.8);
        smoothFontRenderer.d(this.moduleCount + " ", this.G$src$D$1b2f02a() + 10.0, this.n() + 1.0, ProfileSnapshotModuleCountEmptyStateComponent.J.A);
        smoothFontRenderer2.d(LABEL, this.G$src$D$1b2f02a() + 10.0 + smoothFontRenderer.N(this.moduleCount + " "), this.n() + 1.0, ProfileSnapshotModuleCountEmptyStateComponent.J.h);
    }
}
