package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.SonyVegas;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GlyphIconComponent;
import vegas.sony.ui.click.frame.FrameHeaderComponent;
import vegas.sony.utils.render.GuiRenderPrimitives;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

public class ClickGuiMainFrameHeader
extends FrameHeaderComponent {
    private static final double HEADER_HEIGHT = 40.0;
    private static final double EDGE_PADDING = 6.0;
    private static final double ACTION_ICON_SPACING = 15.0;
    private final GlyphIconComponent syncIcon;
    private final ClickGuiMainFrameHeaderActionComponent refreshAction = new ClickGuiMainFrameHeaderActionComponent();
    private final List<ClickGuiSectionTabComponent> sectionTabs;
    private final List<GlyphIconComponent> actionIcons;
    private final GlyphIconComponent refreshIcon;
    private final ClickGuiStyle style;

    private void lambda$new$1() {
        SonyVegas.INSTANCE.getSyncThread().requestSave();
        this.syncIcon.setVisible(false);
    }

    private static void lambda$new$0() {
        ClientSettings.INSTANCE.switchFrameStack(ClientSettings.mainStack);
        SonyVegas.INSTANCE.getPublicProfileSettings().guiStyle.setValue(SonyVegas.INSTANCE.getPublicProfileSettings().framesGuiStyle);
        SonyVegas.INSTANCE.getPublicProfileSettings().guiStyle.setValue(SonyVegas.INSTANCE.getPublicProfileSettings().centralGuiStyle);
        SonyVegas.INSTANCE.getNotificationManager().showInfo("Refreshed", "StandaloneGUI refreshed", 1000L);
    }

    public void addSectionTab(ClickGuiSectionTabComponent clickGuiSectionTabComponent) {
        this.sectionTabs.add(clickGuiSectionTabComponent);
        this.addChildren(clickGuiSectionTabComponent);
    }

    @Override
    public void H() {
        double d;
        if (!SonyVegas.INSTANCE.getPublicProfileSettings().autoSave.getEffectiveValue().booleanValue() && SonyVegas.INSTANCE.getSyncThread().hasPendingSave() && System.currentTimeMillis() > SonyVegas.INSTANCE.getSyncThread().getLastSaveTime() + 60000L) {
            this.syncIcon.setVisible(true);
        } else {
            this.syncIcon.setVisible(false);
        }
        double d2 = this.w$src$Lvegas_sony_ui_click_frame_Frame_$y4htd0() != null ? this.w$src$Lvegas_sony_ui_click_frame_Frame_$y4htd0().G$src$D$1b2f02a() : this.G$src$D$1b2f02a();
        double d3 = this.w$src$Lvegas_sony_ui_click_frame_Frame_$y4htd0() != null ? this.w$src$Lvegas_sony_ui_click_frame_Frame_$y4htd0().n() : this.n();
        double d4 = this.w$src$Lvegas_sony_ui_click_frame_Frame_$y4htd0() != null ? this.w$src$Lvegas_sony_ui_click_frame_Frame_$y4htd0().A() : this.A();
        this.K(d2);
        this.S(d3);
        this.o(d4);
        double d5 = this.G$src$D$1b2f02a();
        double d6 = this.n();
        double d7 = this.A();
        double d8 = this.L();
        Color headerColor = this.style == ClickGuiStyle.ICE ? new Color(19, 19, 19, 255) : ClickGuiMainFrameHeader.J.r;
        Color dividerColor = this.style == ClickGuiStyle.ICE ? new Color(48, 48, 48, 255) : ClickGuiMainFrameHeader.J.s;
        if (this.style == ClickGuiStyle.ICE) {
            return;
        }
        GuiRenderPrimitives.p(d5, d6, d7, d8, headerColor, false, 2.0f, 1.0f, 0.0f, ClickGuiMainFrameHeader.J.B, 3);
        GuiRenderPrimitives.C(d5, d6 + d8 - 1.0, d7, 0.5, dividerColor);
        this.refreshAction.K(d5 + EDGE_PADDING);
        this.refreshAction.S(d6 + EDGE_PADDING);
        this.refreshAction.y(6.5f);
        double d9 = d6 + 5.0;
        int n = 0;
        for (int i = this.actionIcons.size() - 1; i >= 0; --i) {
            GlyphIconComponent glyphIconComponent = this.actionIcons.get(i);
            if (!glyphIconComponent.V$src$Z$1xhop3l()) continue;
            d = d5 + d7 - 10.0 - 5.0 - (double)n * ACTION_ICON_SPACING;
            glyphIconComponent.K(d);
            glyphIconComponent.S(d9);
            ++n;
        }
        double d10 = d5 + 6.0;
        d = d6 + d8 - 20.0 - 2.0;
        for (ClickGuiSectionTabComponent clickGuiSectionTabComponent : this.sectionTabs) {
            double d11 = clickGuiSectionTabComponent.getLabelHeight();
            clickGuiSectionTabComponent.o(d11);
            clickGuiSectionTabComponent.Y(20.0);
            clickGuiSectionTabComponent.K(d10);
            clickGuiSectionTabComponent.S(d);
            d10 += d11 + 12.0;
        }
    }

    public ClickGuiMainFrameHeader(ClickGuiMainFrame clickGuiMainFrame) {
        this(clickGuiMainFrame, clickGuiMainFrame.getStyle());
    }

    public ClickGuiMainFrameHeader(ClickGuiMainFrame clickGuiMainFrame, ClickGuiStyle style) {
        super(clickGuiMainFrame);
        this.style = style == null ? ClickGuiStyle.VAPE : style;
        this.refreshIcon = new GlyphIconComponent("weapons", 6.0, 6.0, 10.0, 10.0, ClickGuiMainFrameHeader.J.V, ClickGuiMainFrameHeader.J.f, null);
        this.syncIcon = new GlyphIconComponent("newsync", 6.0, 6.0, 10.0, 10.0, ClickGuiMainFrameHeader.J.V, ClickGuiMainFrameHeader.J.f, null);
        this.sectionTabs = new ArrayList<ClickGuiSectionTabComponent>();
        this.actionIcons = new ArrayList<GlyphIconComponent>();
        this.Y(this.style.getHeaderHeight());
        this.setShowDisabledOverlay(false);
        this.refreshIcon.setCenterHorizontally(true);
        this.refreshIcon.setCenterVertically(true);
        this.refreshIcon.setClickListener(ClickGuiMainFrameHeader::lambda$new$0);
        this.syncIcon.setCenterHorizontally(true);
        this.syncIcon.setCenterVertically(true);
        this.syncIcon.w("Save your profiles to the cloud");
        this.syncIcon.setClickListener(this::lambda$new$1);
        if (this.style == ClickGuiStyle.ICE) {
            this.refreshAction.setVisible(false);
            this.refreshIcon.setVisible(false);
            this.syncIcon.setVisible(false);
        }
        this.actionIcons.add(this.syncIcon);
        this.addChildren(this.refreshAction, this.syncIcon);
    }
}
