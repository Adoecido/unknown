package vegas.sony.tutorial.page;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.none.TextGuiSettingsFrame;
import vegas.sony.tutorial.MultiComponentHighlightTutorialAction;
import vegas.sony.tutorial.SingleComponentHighlightTutorialAction;
import vegas.sony.tutorial.TutorialPage;
import vegas.sony.tutorial.TutorialTooltipPlacement;
import vegas.sony.tutorial.selector.SettingsFrameHeaderPrimaryButtonTargetSelector;
import vegas.sony.tutorial.selector.SettingsFrameHeaderSecondaryButtonTargetSelector;
import vegas.sony.tutorial.selector.TextGuiQuickActionsSettingsButtonTargetSelector;
import vegas.sony.tutorial.selector.TextGuiSettingsNonHeaderComponentTargetSelector;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.ClickGuiQuickActionsComponent;
import vegas.sony.ui.click.frame.SettingsFrameHeaderComponent;
import vegas.sony.ui.click.frame.impl.quickactions.QuickActionsFrame;

public class TextGuiTutorialPage
extends TutorialPage {
    public TextGuiTutorialPage() {
        super("Overlays");
        this.addAction(new MultiComponentHighlightTutorialAction((GuiComponent)ClientSettings.settingsSearchFrame, new TextGuiQuickActionsSettingsButtonTargetSelector(this, ClickGuiQuickActionsComponent.class), "Overlays Menu", "Open the overlays menu by clicking this button", true).setTooltipPlacement(TutorialTooltipPlacement.RIGHT));
        this.addAction(new SingleComponentHighlightTutorialAction(ClientSettings.getFrame(QuickActionsFrame.class), "Overlays menu", "Here you can enable overlays. Overlays are frames that will render on-top of your screen while in game.", false).setTooltipPlacement(TutorialTooltipPlacement.RIGHT));
        this.addAction(new SingleComponentHighlightTutorialAction(ClientSettings.getFrame(QuickActionsFrame.class).m$src$Lvegas_sony_ui_click_frame_impl_quickactions_QuickA$1kmfigl(), "Text GUI", "Enable the Text GUI by clicking this button", true).setTooltipPlacement(TutorialTooltipPlacement.RIGHT));
        this.addAction(new SingleComponentHighlightTutorialAction(ClientSettings.getFrame(TextGuiSettingsFrame.class), "Text GUI", "This is the Text GUI overlay. It will show you a list of the modules that you have enabled", false).setTooltipPlacement(TutorialTooltipPlacement.RIGHT));
        this.addAction(new SingleComponentHighlightTutorialAction(ClientSettings.getFrame(TextGuiSettingsFrame.class).K$src$Lvegas_sony_ui_click_frame_impl_TextGuiOverlayCompo$1shgn4i(), "Text GUI", "The enabled modules will appear below here", false).setTooltipPlacement(TutorialTooltipPlacement.RIGHT).setVerticalOffset(50.0));
        this.addAction(new MultiComponentHighlightTutorialAction((GuiComponent)ClientSettings.getFrame(TextGuiSettingsFrame.class), new SettingsFrameHeaderPrimaryButtonTargetSelector(this, SettingsFrameHeaderComponent.class), "Open the Text GUI settings", "", true).setTooltipPlacement(TutorialTooltipPlacement.RIGHT));
        this.addAction(new MultiComponentHighlightTutorialAction((GuiComponent)ClientSettings.getFrame(TextGuiSettingsFrame.class), new TextGuiSettingsNonHeaderComponentTargetSelector(this, GuiComponent.class), "Text GUI settings", "Here you can adjust settings for the Text GUI", false).setTooltipPlacement(TutorialTooltipPlacement.RIGHT));
        this.addAction(new MultiComponentHighlightTutorialAction((GuiComponent)ClientSettings.getFrame(TextGuiSettingsFrame.class), new SettingsFrameHeaderSecondaryButtonTargetSelector(this, SettingsFrameHeaderComponent.class), "Close the Text GUI settings", "", true).setTooltipPlacement(TutorialTooltipPlacement.RIGHT));
    }
}
