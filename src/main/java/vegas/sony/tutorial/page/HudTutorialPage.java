package vegas.sony.tutorial.page;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.tutorial.MultiComponentHighlightTutorialAction;
import vegas.sony.tutorial.SingleComponentHighlightTutorialAction;
import vegas.sony.tutorial.TutorialPage;
import vegas.sony.tutorial.TutorialTooltipPlacement;
import vegas.sony.tutorial.selector.HudTutorialKeystrokesHudModuleRowSelector;
import vegas.sony.tutorial.selector.HudTutorialNamedHudModuleRowSelector;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.impl.hud.HudEditorReturnToMainLayerFrame;
import vegas.sony.ui.click.frame.impl.hud.HudModuleListEntry;
import vegas.sony.ui.click.frame.impl.hud.HudModuleListPanel;
import vegas.sony.ui.click.frame.impl.hud.HudModuleSelectorFrame;

public class HudTutorialPage
extends TutorialPage {
    public HudTutorialPage() {
        super("Legit mod menu");
        this.addAction(new SingleComponentHighlightTutorialAction(ClientSettings.moduleSearchFrame.n$src$Lvegas_sony_ui_click_frame_impl_ModuleSearchFrameHe$xia8v2().G$src$Lvegas_sony_ui_click_component_IconButtonComponent_$1pnwa51(), "Click this button to open the legit mod menu", "", true).setTooltipPlacement(TutorialTooltipPlacement.BOTTOM));
        this.addAction(new SingleComponentHighlightTutorialAction(ClientSettings.getFrame(HudModuleSelectorFrame.class), "This is the legit mod menu", "It includes various features generally included inside legit clients, or as mods.", false).queueMessage("This is the legit mod menu", "None of these features are detectable and do not necessarily provide any unfair advantage.").setTooltipPlacement(TutorialTooltipPlacement.TOP));
        this.addAction(new MultiComponentHighlightTutorialAction((GuiComponent)ClientSettings.getFrame(HudModuleListPanel.class), new HudTutorialNamedHudModuleRowSelector(this, HudModuleListEntry.class), "Legit mods", "Some modules such as freelook or NoClickDelay are typically disallowed by servers and thus have been removed from legit clients.", false).queueMessage("Legit mods", "However these banned mods are included and can safely be used without the server knowing that you're using them."));
        this.addAction(new MultiComponentHighlightTutorialAction((GuiComponent)ClientSettings.getFrame(HudModuleListPanel.class), new HudTutorialKeystrokesHudModuleRowSelector(this, HudModuleListEntry.class), "Enable the keystrokes mod", "", true));
        this.addAction(new SingleComponentHighlightTutorialAction(ClientSettings.getFrame(HudEditorReturnToMainLayerFrame.class).getHeader().getReturnButton(), "Click this button to return to the cheat menu", "", true).setTooltipPlacement(TutorialTooltipPlacement.BOTTOM));
    }
}
