package vegas.sony.ui.click.frame.impl.main;

import func.skidline.RectData;
import vegas.sony.module.Category;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.frame.OutlinedFrameBase;
import vegas.sony.ui.click.frame.impl.main.ClickGuiFriendsPage;
import vegas.sony.ui.click.frame.impl.main.ClickGuiMainFrameHeader;
import vegas.sony.ui.click.frame.impl.main.ClickGuiMainFrameTransitionComponent;
import vegas.sony.ui.click.frame.impl.main.ClickGuiModulesPage;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlayController;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlayLayer;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlayNavigationPanel;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlayPlacement;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlaySpec;
import vegas.sony.ui.click.frame.impl.main.ClickGuiPageBase;
import vegas.sony.ui.click.frame.impl.main.ClickGuiPanelBase;
import vegas.sony.ui.click.frame.impl.main.ClickGuiProfilesPage;
import vegas.sony.ui.click.frame.impl.main.ClickGuiSection;
import vegas.sony.ui.click.frame.impl.main.ClickGuiSectionTabComponent;
import vegas.sony.ui.click.frame.impl.main.ClickGuiSidecarPanelBase;
import vegas.sony.ui.click.layout.ComponentLayout;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.RenderUtils;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.awt.Color;

public class ClickGuiMainFrame
extends OutlinedFrameBase {
    private final ClickGuiStyle style;
    private final List<Runnable> contentOverlayCallbacks;
    private final List<ClickGuiSectionTabComponent> sectionTabs = new ArrayList<ClickGuiSectionTabComponent>();
    private final ClickGuiMainFrameTransitionComponent transitionOverlay;
    private double lastContentInset = -1.0;
    private final PanelComponent contentPanel;
    private final EnumMap<ClickGuiSection, ClickGuiPanelBase> pages;
    private ClickGuiPanelBase activePage;
    private final ClickGuiOverlayController frameOverlayController;
    private final ClickGuiMainFrameHeader header;
    private ClickGuiSection activeSection;
    private final ClickGuiOverlayController contentOverlayController;
    private final List<Runnable> frameOverlayCallbacks;

    public ClickGuiOverlaySpec getActiveOverlaySpec() {
        if (this.frameOverlayController.hasActiveSpec()) {
            return this.frameOverlayController.getActiveSpec();
        }
        if (this.contentOverlayController.hasActiveSpec()) {
            return this.contentOverlayController.getActiveSpec();
        }
        return null;
    }

    @Override
    public void J() {
        boolean bl = this.frameOverlayController.capturesInput();
        boolean bl2 = this.contentOverlayController.capturesInput();
        if (!bl && !bl2) {
            super.J();
            return;
        }
        this.F();
        this.setHovered(true);
        for (GuiComponent guiComponent : this.f()) {
            if (!guiComponent.V$src$Z$1xhop3l() || !guiComponent.t()) continue;
            if (bl) {
                if (guiComponent != this.frameOverlayController.getNavigationPanel() && guiComponent != this.frameOverlayController.getBackdrop()) continue;
                guiComponent.J();
                continue;
            }
            if (bl2) {
                if (guiComponent != this.header && guiComponent != this.contentOverlayController.getNavigationPanel() && guiComponent != this.contentOverlayController.getBackdrop() && guiComponent != this.frameOverlayController.getNavigationPanel() && guiComponent != this.frameOverlayController.getBackdrop()) continue;
                guiComponent.J();
                continue;
            }
            guiComponent.J();
        }
    }

    public void removeFrameOverlayCallback(Runnable runnable) {
        if (runnable != null) {
            this.frameOverlayCallbacks.remove(runnable);
        }
    }

    public ClickGuiOverlayNavigationPanel getActiveOverlayNavigation() {
        return this.getActiveOverlayController().getNavigationPanel();
    }

    public void addContentOverlayCallback(Runnable runnable) {
        if (runnable != null) {
            this.contentOverlayCallbacks.add(runnable);
        }
    }

    public boolean clearActiveSearch() {
        if (this.transitionOverlay.isActive()) {
            return false;
        }
        if (this.activeSection == ClickGuiSection.MODULES && this.activePage instanceof ClickGuiModulesPage) {
            return ((ClickGuiModulesPage)this.activePage).clearActiveSearch();
        }
        return false;
    }

    private void showSection(ClickGuiSection clickGuiSection) {
        ClickGuiPanelBase clickGuiPanelBase = this.pages.computeIfAbsent(clickGuiSection, this::createPage);
        this.closeActiveOverlay();
        if (clickGuiPanelBase == null) {
            return;
        }
        if (this.activePage == clickGuiPanelBase) {
            clickGuiPanelBase.Z$src$V$15w0jcm();
            clickGuiPanelBase.H(true);
            return;
        }
        if (this.activePage != null) {
            this.activePage.K();
            this.contentPanel.removeChild(this.activePage);
        }
        this.activePage = clickGuiPanelBase;
        this.contentPanel.h(clickGuiPanelBase, new Object[0]);
        clickGuiPanelBase.Z$src$V$15w0jcm();
    }

    private void handleSectionTabClick(ClickGuiSection clickGuiSection) {
        this.selectSection(clickGuiSection);
    }

    public ClickGuiMainFrameTransitionComponent getTransitionOverlay() {
        return this.transitionOverlay;
    }

    private double getOverlayWidth(ClickGuiOverlayPlacement clickGuiOverlayPlacement) {
        if (clickGuiOverlayPlacement == ClickGuiOverlayPlacement.OVERLAY) {
            return this.style.getFrameOverlayWidth();
        }
        return this.style.getContentOverlayWidth();
    }

    public ClickGuiStyle getStyle() {
        return this.style;
    }

    private void selectSection(ClickGuiSection clickGuiSection) {
        this.activeSection = clickGuiSection;
        this.updateSelectedTab();
        this.showSection(clickGuiSection);
    }

    @Override
    public double A() {
        return this.style.getFrameWidth();
    }

    public boolean isOverlayTransitioning() {
        return this.frameOverlayController.isOpen() || this.contentOverlayController.isOpen();
    }

    @Override
    public void H() {
        this.updateContentLayout();
        Color frameColor = this.style == ClickGuiStyle.ICE
                ? new Color(19, 19, 19, 255)
                : this.getDisabledOverlayColor();
        boolean clipIceFrame = this.style == ClickGuiStyle.ICE;
        if (clipIceFrame) {
            RenderUtils.m(this.G$src$D$1b2f02a(), this.n(),
                    this.style.getFrameWidth(), this.style.getFrameHeight());
        }
        try {
            GuiRenderPrimitives.e(this.G$src$D$1b2f02a(), this.n(), this.style.getFrameWidth(), this.style.getFrameHeight(), frameColor, false, clipIceFrame ? 4.0f : 2.0f, 1.0f);
            super.H();
        } finally {
            if (clipIceFrame) {
                RenderUtils.T();
            }
        }
    }

    public double getHeaderHeight() {
        return this.style.getHeaderHeight();
    }

    @Override
    public void u() {
        this.h(new RectData(this.G$src$D$1b2f02a(), this.n(), this.style.getFrameWidth(), this.style.getFrameHeight()));
        this.contentOverlayController.updateBounds();
        this.frameOverlayController.updateBounds();
        super.u();
        this.contentOverlayController.updateAnimationState();
        this.frameOverlayController.updateAnimationState();
    }

    @Override
    public String getName() {
        return this.style.getDisplayName();
    }

    public boolean h() {
        return false;
    }

    static void refreshOverlayLayout(ClickGuiMainFrame clickGuiMainFrame) {
        clickGuiMainFrame.updateContentLayout();
    }

    public void closeActiveOverlay() {
        if (this.frameOverlayController.isVisible()) {
            this.frameOverlayController.close();
        } else {
            this.contentOverlayController.close();
        }
        this.updateContentLayout();
    }

    private void updateSelectedTab() {
        for (ClickGuiSectionTabComponent clickGuiSectionTabComponent : this.sectionTabs) {
            clickGuiSectionTabComponent.setSelected(clickGuiSectionTabComponent.getSection() == this.activeSection);
        }
    }

    private void initializeTabs() {
        ClickGuiMainFrameHeader clickGuiMainFrameHeader = this.header;
        for (ClickGuiSection clickGuiSection : ClickGuiSection.values()) {
            ClickGuiSectionTabComponent clickGuiSectionTabComponent = new ClickGuiSectionTabComponent(clickGuiSection);
            clickGuiSectionTabComponent.Y(20.0);
            clickGuiSectionTabComponent.addClickListener(() -> this.handleSectionTabClick(clickGuiSection));
            this.sectionTabs.add(clickGuiSectionTabComponent);
            clickGuiMainFrameHeader.addSectionTab(clickGuiSectionTabComponent);
        }
    }

    public ClickGuiMainFrame() {
        this(ClickGuiStyle.VAPE);
    }

    public ClickGuiMainFrame(ClickGuiStyle style) {
        this.style = style == null ? ClickGuiStyle.VAPE : style;
        this.contentOverlayCallbacks = new ArrayList<Runnable>();
        this.frameOverlayCallbacks = new ArrayList<Runnable>();
        this.pages = new EnumMap(ClickGuiSection.class);
        this.activeSection = ClickGuiSection.MODULES;
        this.o(this.style.getFrameWidth());
        this.Y(this.style.getFrameHeight());
        this.setDisabledOverlayColor(ClickGuiMainFrame.J.i);
        this.setShowDisabledOverlay(false);
        this.g(true);
        this.L(false, true);
        this.Y(false);
        ComponentLayout componentLayout = this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij();
        componentLayout.t(false);
        componentLayout.M(false);
        componentLayout.U(false);
        componentLayout.I(false);
        componentLayout.u(false);
        this.header = new ClickGuiMainFrameHeader(this, this.style);
        this.h(this.header, "wrap");
        this.contentPanel = new PanelComponent(this.style.getFrameWidth(), this.style.getFrameHeight() - this.style.getHeaderHeight());
        this.contentPanel.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("widthwrap");
        this.contentPanel.setShowDisabledOverlay(false);
        this.contentPanel.setShadowEnabled(false);
        if (this.style == ClickGuiStyle.ICE) {
            this.N(false);
            this.contentPanel.N(false);
        }
        this.h(this.contentPanel, new Object[0]);
        this.frameOverlayController = new ClickGuiOverlayController(this, ClickGuiOverlayLayer.FRAME_OVERLAY, this.style.getFrameOverlayWidth(), null);
        this.contentOverlayController = new ClickGuiOverlayController(this, ClickGuiOverlayLayer.CONTENT_OVERLAY, this.style.getContentOverlayWidth(), null);
        this.transitionOverlay = new ClickGuiMainFrameTransitionComponent(this);
        this.addChildren(this.contentOverlayController.getBackdrop(), this.contentOverlayController.getNavigationPanel(), this.frameOverlayController.getBackdrop(), this.frameOverlayController.getNavigationPanel(), this.transitionOverlay);
        if (this.style != ClickGuiStyle.ICE) {
            this.initializeTabs();
        }
        this.selectSection(this.activeSection);
    }

    static List<Runnable> getContentOverlayCallbacks(ClickGuiMainFrame clickGuiMainFrame) {
        return clickGuiMainFrame.contentOverlayCallbacks;
    }

    public Category s$src$Lvegas_sony_module_Category_$154whg1() {
        return Category.NONE;
    }


    private ClickGuiOverlayController getActiveOverlayController() {
        if (this.frameOverlayController.hasActiveSpec()) {
            return this.frameOverlayController;
        }
        return this.contentOverlayController;
    }

    static double getOverlayWidth(ClickGuiMainFrame clickGuiMainFrame, ClickGuiOverlayPlacement clickGuiOverlayPlacement) {
        return clickGuiMainFrame.getOverlayWidth(clickGuiOverlayPlacement);
    }

    public void removeContentOverlayCallback(Runnable runnable) {
        if (runnable != null) {
            this.contentOverlayCallbacks.remove(runnable);
        }
    }

    @Override
    public boolean d$src$Z$1lx9d06() {
        return false;
    }

    public ClickGuiOverlayPlacement getActiveOverlayPlacement() {
        ClickGuiOverlaySpec clickGuiOverlaySpec = this.getActiveOverlaySpec();
        if (clickGuiOverlaySpec != null) {
            return clickGuiOverlaySpec.getPlacement();
        }
        return ClickGuiOverlayPlacement.OVERLAY;
    }

    private void updateContentLayout() {
        double d = this.contentOverlayController.getVisibilityProgress();
        boolean bl = this.contentOverlayController.shiftsContent(d);
        if (bl) {
            double d2;
            double d3;
            double d4 = Math.min(1.0, d * 1.35);
            double d5 = 0.0;
            d5 = (this.contentOverlayController.getWidth() - 6.0) * d4;
            if (this.activePage instanceof ClickGuiModulesPage) {
                d3 = Math.max(0.0, this.G$src$D$1b2f02a() - this.contentPanel.G$src$D$1b2f02a());
                d2 = Math.max(0.0, ((ClickGuiModulesPage)this.activePage).B$src$D$yaoqbo() - 0.0 + d3);
                d5 = Math.min(d5, d2);
            }
            d3 = this.G$src$D$1b2f02a() - d5;
            this.contentPanel.K(d3);
            this.contentPanel.o(this.style.getFrameWidth());
            this.contentPanel.h(null);
            this.contentPanel.H(true);
            d2 = 10.0 * d4;
            if (this.activePage != null && d2 != this.lastContentInset) {
                this.lastContentInset = d2;
                this.activePage.K(this.style.getFrameWidth(), this.style.getFrameHeight(), d2);
            }
            return;
        }
        double d6 = d;
        double d7 = 0.0;
        double d8 = this.G$src$D$1b2f02a() - d7;
        this.contentPanel.K(d8);
        this.contentPanel.o(this.style.getFrameWidth());
        this.contentPanel.h(null);
        this.contentPanel.H(true);
        double d9 = 0.0;
        if (this.activePage != null && d9 != this.lastContentInset) {
            this.lastContentInset = d9;
            this.activePage.K(this.style.getFrameWidth(), this.style.getFrameHeight(), d9);
        }
    }

    static List<Runnable> getFrameOverlayCallbacks(ClickGuiMainFrame clickGuiMainFrame) {
        return clickGuiMainFrame.frameOverlayCallbacks;
    }

    @Override
    public double L() {
        return this.style.getFrameHeight();
    }

    public ClickGuiMainFrameHeader getHeader() {
        return this.header;
    }

    public void addFrameOverlayCallback(Runnable runnable) {
        if (runnable != null) {
            this.frameOverlayCallbacks.add(runnable);
        }
    }

    private ClickGuiOverlayController getOverlayController(ClickGuiOverlayPlacement clickGuiOverlayPlacement) {
        if (clickGuiOverlayPlacement == ClickGuiOverlayPlacement.OVERLAY) {
            return this.frameOverlayController;
        }
        return this.contentOverlayController;
    }

    public void showOverlay(ClickGuiOverlaySpec clickGuiOverlaySpec) {
        Objects.requireNonNull(clickGuiOverlaySpec, "config");
        ClickGuiOverlayController clickGuiOverlayController = this.getOverlayController(clickGuiOverlaySpec.getPlacement());
        clickGuiOverlayController.show(clickGuiOverlaySpec);
        this.updateContentLayout();
    }

    public ClickGuiSidecarPanelBase getActiveSidecar() {
        return this.getActiveOverlayController().getNavigationPanel().getSidecar();
    }

    private ClickGuiPanelBase createPage(ClickGuiSection clickGuiSection) {
        if (this.style == ClickGuiStyle.ICE) {
            return clickGuiSection == ClickGuiSection.MODULES
                    ? new IceClickGuiPage(this.contentPanel.A(), this.contentPanel.L())
                    : null;
        }
        ClickGuiPageBase clickGuiPageBase;
        switch (clickGuiSection) {
            case MODULES: {
                clickGuiPageBase = new ClickGuiModulesPage(this, this.contentPanel.A(), this.contentPanel.L(), this.style.getSidebarWidth());
                break;
            }
            case FRIENDS: {
                clickGuiPageBase = new ClickGuiFriendsPage(this, this.contentPanel.A(), this.contentPanel.L(), this.style.getSidebarWidth());
                break;
            }
            case PROFILES: {
                clickGuiPageBase = new ClickGuiProfilesPage(this, this.contentPanel.A(), this.contentPanel.L(), this.style.getSidebarWidth());
                break;
            }
            default: {
                return null;
            }
        }
        return clickGuiPageBase;
    }
}

