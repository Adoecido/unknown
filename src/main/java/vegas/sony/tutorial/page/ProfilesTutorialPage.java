package vegas.sony.tutorial.page;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.tutorial.MultiComponentHighlightTutorialAction;
import vegas.sony.tutorial.SingleComponentHighlightTutorialAction;
import vegas.sony.tutorial.TutorialPage;
import vegas.sony.tutorial.TutorialTooltipPlacement;
import vegas.sony.tutorial.page.profile.FirstActiveProfileListEntrySelector;
import vegas.sony.tutorial.page.profile.SecondActiveProfileListEntrySelector;
import vegas.sony.tutorial.selector.ProfileCreateNameInputTargetSelector;
import vegas.sony.tutorial.selector.ProfilesNavigationButtonTargetSelector;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.FrameNavigationButtonComponent;
import vegas.sony.ui.click.frame.impl.profile.ProfileCreatePanelComponent;
import vegas.sony.ui.click.frame.impl.profile.ProfileListEntryComponent;
import vegas.sony.ui.click.frame.impl.profile.ProfilesSettingsFrame;

public class ProfilesTutorialPage
extends TutorialPage {
    public ProfilesTutorialPage() {
        super("Profiles");
        this.addAction(new MultiComponentHighlightTutorialAction((GuiComponent)ClientSettings.settingsSearchFrame, new ProfilesNavigationButtonTargetSelector(this, FrameNavigationButtonComponent.class), "Open the profiles frame", "", true).setTooltipPlacement(TutorialTooltipPlacement.RIGHT));
        this.addAction(new SingleComponentHighlightTutorialAction(ClientSettings.getFrame(ProfilesSettingsFrame.class), "Profiles", "Profiles allow you to keep a set of settings contained within a profile.", false).queueMessage("Profiles", "This allows you to easily switch between various settings that are more suited between different situations.").setTooltipPlacement(TutorialTooltipPlacement.RIGHT));
        this.addAction(new MultiComponentHighlightTutorialAction((GuiComponent)ClientSettings.getFrame(ProfilesSettingsFrame.class), new ProfileCreateNameInputTargetSelector(this, ProfileCreatePanelComponent.class), "Creating profiles", "You can add a profile by entering the name for the profile here. When you create a new profile, it starts off with the settings of your currently selected profile.", false).setTooltipPlacement(TutorialTooltipPlacement.BOTTOM));
        this.addAction(new MultiComponentHighlightTutorialAction((GuiComponent)ClientSettings.getFrame(ProfilesSettingsFrame.class), new FirstActiveProfileListEntrySelector(this, ProfileListEntryComponent.class), "Selected profile", "The profile that you currently have selected will be enabled on next launch.", false).setTooltipPlacement(TutorialTooltipPlacement.BOTTOM));
        this.addAction(new MultiComponentHighlightTutorialAction((GuiComponent)ClientSettings.getFrame(ProfilesSettingsFrame.class), new SecondActiveProfileListEntrySelector(this, ProfileListEntryComponent.class), "Enabled modules", "A profile will remember the modules that you currently have enabled, but by default it will not automatically enable them.", false).setTooltipPlacement(TutorialTooltipPlacement.BOTTOM));
        this.addAction(new SingleComponentHighlightTutorialAction(ClientSettings.getFrame(ProfilesSettingsFrame.class).q$src$Lvegas_sony_ui_click_component_IconButtonComponent_$1bvowkh(), "Open the general settings for profiles", "", true).setTooltipPlacement(TutorialTooltipPlacement.RIGHT));
        this.addAction(new SingleComponentHighlightTutorialAction(ClientSettings.getFrame(ProfilesSettingsFrame.class).getAutoEnableModulesToggle(), "Auto-enabling modules", "If you enable this option, your modules for a profile will automatically be re-enabled upon launch, or when switching to that profile", false).queueMessage("Auto-enabling modules", "We recommend keeping this disabled in most circumstances").setTooltipPlacement(TutorialTooltipPlacement.RIGHT));
    }
}
