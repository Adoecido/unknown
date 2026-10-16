package vegas.sony.manager.client;

import vegas.sony.api.ApiResponse;
import vegas.sony.api.ApiServices;
import vegas.sony.config.OnlineSettingsPayload;
import vegas.sony.config.SettingsDataType;
import vegas.sony.event.impl.EventKeyPress;
import vegas.sony.event.impl.EventMouseButton;
import vegas.sony.manager.client.OnlineSettingsNoopBindAction;
import vegas.sony.manager.client.OnlineSettingsPickPingBindAction;
import vegas.sony.notification.FriendNotificationSettings;
import vegas.sony.protocol.ZeusConnectionManager;
import vegas.sony.unmap.Bendable;
import vegas.sony.unmap.ModeOption;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.wrapper.impl.Minecraft;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class OnlineSettings {
    private final ModeOption toggleModeOption;
    private final BooleanValue shareUsername;
    private final Bendable inventoryDisplayBind;
    private final BooleanValue autoLogin;
    private final BooleanValue selfTargetIndicators;
    private static boolean obfuscationState;
    private final ModeValue indicatorColorMode;
    private final List<Bendable> handledBindings;
    private final Bendable pingBind;
    private boolean loadFailed = false;
    private final ModeOption friendColorOption;
    private final ModeOption partyColorOption;
    private final FriendNotificationSettings friendNotificationSettings = new FriendNotificationSettings();
    private final ModeOption holdModeOption;
    private final BooleanValue shareInventory;
    private final ModeValue inventorySwitchMode;
    private final BooleanValue showSelf;
    private final ModeOption teamColorOption;
    private final BooleanValue targetIndicators;
    private final BooleanValue shareServer;
    private final BooleanValue partyOverheadIndicator;
    private OnlineSettingsPayload payload;

    public OnlineSettings() {
        this.autoLogin = BooleanValue.create(null, "Auto login //$ui", true, "Automatically logs you into SonyVegas online friend services when loading is complete //$");
        this.shareServer = BooleanValue.create(null, "Share server //$ui", true, "Display your current server in friends list\nYour server may be shown if you join a party //$");
        this.shareUsername = BooleanValue.create(null, "Share username //$ui", true, "Display your Minecraft username in friends list\nYour username may be shown if you join a party\nFriend won't be able to sync you as a Minecraft friend with this disabled //$");
        this.shareInventory = BooleanValue.create(null, "Share inventory //$ui", true, "Shares your inventory contents to party members. //$");
        this.pingBind = new OnlineSettingsPickPingBindAction(this);
        this.toggleModeOption = new ModeOption("Toggle //$ui");
        this.holdModeOption = new ModeOption("Hold //$ui");
        this.inventorySwitchMode = ModeValue.create(null, "Bind mode //$ui", this.holdModeOption, this.holdModeOption, this.toggleModeOption);
        this.showSelf = BooleanValue.create(null, "Show Self //$ui", true, "Shows your own overlay in the party overlay //$");
        this.partyColorOption = new ModeOption("Party //$ui");
        this.teamColorOption = new ModeOption("Team //$ui");
        this.friendColorOption = new ModeOption("Friend //$ui");
        this.indicatorColorMode = ModeValue.create(null, "Indicator color //$ui", this.partyColorOption, this.partyColorOption, this.teamColorOption, this.friendColorOption);
        this.partyOverheadIndicator = BooleanValue.create(null, "Party overhead indicator //$ui", true, "Draws a circle above party members //$");
        this.targetIndicators = BooleanValue.create(null, "Target indicators //$ui", true, "Shows who your party members are targeting //$");
        this.selfTargetIndicators = BooleanValue.create(null, "Self target indicators //$ui", true, "Draws indicators on your own target //$");
        this.inventoryDisplayBind = new OnlineSettingsNoopBindAction(this);
        this.handledBindings = Arrays.asList(this.pingBind);
    }

    public ModeOption getPartyColorOption() {
        return this.partyColorOption;
    }

    public void initialize() {
        try {
            ApiResponse apiResponse = ApiServices.getInstance().getSettingsApi().loadSettings(SettingsDataType.ONLINE);
            this.loadFailed = false;
            if (apiResponse == null || !apiResponse.isSuccessful()) {
                this.payload = OnlineSettingsPayload.DEFAULTS;
                ApiServices.getInstance().getSettingsApi().saveSettings(SettingsDataType.ONLINE, this.payload);
            } else {
                this.payload = (OnlineSettingsPayload)apiResponse.getData();
            }
        }
        catch (Exception exception) {
            this.payload.initializeDefaults();
            this.loadFailed = true;
        }
        this.autoLogin.setValue(this.payload.getAutoLogin());
        this.shareUsername.setValue(this.payload.getShowUsername());
        this.shareServer.setValue(this.payload.getShowServer());
        this.shareInventory.setValue(this.payload.getShareInventory());
        this.showSelf.setValue(this.payload.getShowSelf());
        if (this.payload.getInventorySwitchMode() != null) {
            this.inventorySwitchMode.setSelectedIndex(this.payload.getInventorySwitchMode());
        }
        if (this.payload.getPingKeybind() != null) {
            this.pingBind.loadBoundInputs(this.payload.getPingKeybind(), false);
        }
        if (this.payload.getShowInventoryKeybind() != null) {
            this.inventoryDisplayBind.loadBoundInputs(this.payload.getShowInventoryKeybind(), false);
        }
        if (!this.loadFailed) {
            Stream.of(this.shareUsername).forEach(OnlineSettings::attachShareUsernameSyncListener);
        }
    }

    public ModeOption getToggleModeOption() {
        return this.toggleModeOption;
    }

    private static void attachShareUsernameSyncListener(BooleanValue booleanValue) {
        booleanValue.addChangeListener(OnlineSettings::syncShareUsername);
    }

    public static void setObfuscationState(boolean state) {
        obfuscationState = state;
    }

    public static boolean reservedCheck() {
        boolean state = OnlineSettings.getObfuscationState();
        return false;
    }

    public BooleanValue getSelfTargetIndicators() {
        return this.selfTargetIndicators;
    }

    public BooleanValue getAutoLogin() {
        return this.autoLogin;
    }

    public BooleanValue getTargetIndicators() {
        return this.targetIndicators;
    }

    static {
        OnlineSettings.setObfuscationState(true);
    }

    public BooleanValue getShareUsername() {
        return this.shareUsername;
    }

    public Bendable getInventoryDisplayBind() {
        return this.inventoryDisplayBind;
    }

    private static Exception passthroughException(Exception exception) {
        return exception;
    }

    public void handleKeyPress(EventKeyPress eventKeyPress) {
        if (eventKeyPress.getKey() <= 0) {
            return;
        }
        if (!eventKeyPress.isDown()) {
            return;
        }
        if (Minecraft.currentScreen().getObject() != null) {
            return;
        }
        for (Bendable bendable : this.handledBindings) {
            if (!bendable.activateIfMatched(eventKeyPress.getKey())) continue;
            eventKeyPress.setCancelled(true);
        }
    }

    public BooleanValue getPartyOverheadIndicator() {
        return this.partyOverheadIndicator;
    }

    public ModeOption getFriendColorOption() {
        return this.friendColorOption;
    }

    public BooleanValue getShowSelf() {
        return this.showSelf;
    }

    public OnlineSettingsPayload getPayload() {
        if (this.payload == null) {
            OnlineSettingsPayload onlineSettingsPayload = new OnlineSettingsPayload();
            onlineSettingsPayload.initializeDefaults();
            this.payload = onlineSettingsPayload;
        }
        return this.payload;
    }

    public Bendable getPingBind() {
        return this.pingBind;
    }

    private static void syncShareUsername(BooleanValue booleanValue) {
        ZeusConnectionManager.T().u().Y();
    }

    public boolean hasLoadFailed() {
        return this.loadFailed;
    }

    public static boolean getObfuscationState() {
        return obfuscationState;
    }

    public FriendNotificationSettings getFriendNotificationSettings() {
        return this.friendNotificationSettings;
    }

    public ModeValue getInventorySwitchMode() {
        return this.inventorySwitchMode;
    }

    public ModeOption getHoldModeOption() {
        return this.holdModeOption;
    }

    public BooleanValue getShareServer() {
        return this.shareServer;
    }

    public ModeValue getIndicatorColorMode() {
        return this.indicatorColorMode;
    }

    public ModeOption getTeamColorOption() {
        return this.teamColorOption;
    }

    public void handleMouseButton(EventMouseButton eventMouseButton) {
        if (!eventMouseButton.getButtonState()) {
            return;
        }
        int inputCode = -100 + eventMouseButton.getButton();
        for (Bendable bendable : this.handledBindings) {
            if (!bendable.activateIfMatched(inputCode)) continue;
            eventMouseButton.setCancelled(true);
        }
    }

    public BooleanValue getShareInventory() {
        return this.shareInventory;
    }
}
