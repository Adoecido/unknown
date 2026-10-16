package vegas.sony.config;

import vegas.sony.config.Profile;
import vegas.sony.config.PublicProfilePrimaryDirtyStringValue;
import vegas.sony.config.PublicProfileSecondaryDirtyStringValue;
import vegas.sony.config.PublicProfileSelectedProfileStringValue;
import vegas.sony.config.PublicProfileSettingsBindSet;
import vegas.sony.config.PublicProfileSettingsBindValue;
import vegas.sony.ui.font.FontSelector;
import vegas.sony.unmap.ModeOption;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.BindValue;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.StringValue;

public class PublicProfileSettings {
    public NumberValue volume;
    public BooleanValue profileSwitchNotifications;
    private Profile selectedProfile;
    public ModeValue language;
    public BooleanValue friendNotifications;
    public BindValue guiBind;
    public BooleanValue muted;
    public BooleanValue autoSave;
    public BooleanValue notifications;
    public StringValue hudLayout;
    public final ModeOption centralGuiStyle;
    public BooleanValue autoLoadModuleStates;
    public final ModeOption framesGuiStyle;
    public StringValue alternateAccounts;
    private static int runtimeState;
    public BooleanValue saveToCloud = BooleanValue.create(this, "Save to Cloud //$ui", false, "Logs into an account in offline mode. //$");
    public ModeValue guiStyle;
    public StringValue alteningKey;
    public StringValue selectedProfileId;
    public BooleanValue framePositionsPerProfile;
    public BooleanValue toggleAlerts;

    private void clearAlteningKeyWhenCloudEnabled(BooleanValue saveToCloudValue) {
        if (saveToCloudValue.getEffectiveValue().booleanValue()) {
            this.alteningKey.setValue("");
        }
    }

    public static int getDefaultRuntimeState() {
        int currentState = PublicProfileSettings.getRuntimeState();
        return 0;
    }

    public static void setRuntimeState(int runtimeState) {
        PublicProfileSettings.runtimeState = runtimeState;
    }

    public PublicProfileSettings() {
        this.saveToCloud.addChangeListener(this::clearAlteningKeyWhenCloudEnabled);
        this.alteningKey = new PublicProfilePrimaryDirtyStringValue(this, this, "alteningKey", "");
        this.autoLoadModuleStates = BooleanValue.create(this, "Auto-load module states //$ui", true, "Automatically enable saved module states upon loading, and when selecting profiles //$");
        this.alternateAccounts = new PublicProfileSecondaryDirtyStringValue(this, this, "alts", "");
        this.selectedProfileId = (StringValue)((StringValue)new PublicProfileSelectedProfileStringValue(this, this, "selectedprofile_uuid", "").setBase64Encoded(true)).addAlias("selectedprofile");
        this.autoSave = BooleanValue.create(this, "Auto save //$ui", false, "Automatically sync your settings //$");
        this.framePositionsPerProfile = BooleanValue.create(this, "Frame positions per profile //$ui", false, "Saves the positions of your GUI frames per profile //$");
        this.language = (ModeValue)ModeValue.create((Object)this, "Language //$ui", FontSelector.j, FontSelector.j, FontSelector.S, FontSelector.c, FontSelector.a, FontSelector.P).setResettable(false);
        this.volume = NumberValue.create(this, "Volume //$ui", "#", "%", 0.0, 50.0, 100.0);
        this.muted = BooleanValue.create(this, "Muted //$ui", false, "Mutes all sounds //$");
        this.guiBind = new PublicProfileSettingsBindValue(this, this, "GUI Bind", new PublicProfileSettingsBindSet(this, 161));
        this.framesGuiStyle = new ModeOption("Frames //$ui", 0.8);
        this.centralGuiStyle = new ModeOption("Central //$ui", 0.8);
        this.guiStyle = ModeValue.create((Object)this, "GUI style //$ui", "Switch between the frames gui and the central gui //$", (ModeSelection)this.framesGuiStyle, this.centralGuiStyle, this.framesGuiStyle);
        this.notifications = BooleanValue.create(this, "Notifications //$ui", true, "Shows notifications //$");
        this.hudLayout = StringValue.create(this, "HUD layout", "{}").setBase64Encoded(true);
        this.toggleAlerts = BooleanValue.create(this, "Toggle alert //$ui", false, "Notifies you if a module is enabled/disabled. //$");
        this.profileSwitchNotifications = BooleanValue.create(this, "Profile switch //$ui", false, "Notifies you when you switch profiles //$");
        this.friendNotifications = BooleanValue.create(this, "Friend notifications //$ui", true, "Shows notifications related to friends //$");
    }

    public Profile getSelectedProfile() {
        return this.selectedProfile;
    }

    static {
        PublicProfileSettings.setRuntimeState(36);
    }

    public static Profile setSelectedProfile(PublicProfileSettings settings, Profile profile) {
        settings.selectedProfile = profile;
        return settings.selectedProfile;
    }

    public static int getRuntimeState() {
        return runtimeState;
    }

}

