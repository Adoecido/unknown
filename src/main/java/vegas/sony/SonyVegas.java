package vegas.sony;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import vegas.sony.account.AccountInfo;
import vegas.sony.account.AccountInfoResponse;
import vegas.sony.account.LicenseManager;
import vegas.sony.api.ApiAccessTokenProvider;
import vegas.sony.api.ApiResponse;
import vegas.sony.api.ApiServices;
import vegas.sony.api.LicenseVerifier;
import vegas.sony.asm.helper.DescUtils;
import vegas.sony.combat.AttackStrengthTracker;
import vegas.sony.config.ConfigJsonUtils;
import vegas.sony.config.ModuleProfileMetadataCodec;
import vegas.sony.config.Profile;
import vegas.sony.config.PublicProfileSettings;
import vegas.sony.event.ClientListenerBootstrap;
import vegas.sony.event.EventBus;
import vegas.sony.event.EventNameFormatRewriteService;
import vegas.sony.event.IEvent;
import vegas.sony.event.impl.EventRenderWorldPassExecutorDrain;
import vegas.sony.event.listener.ClientSettingsEventForwarder;
import vegas.sony.event.listener.EventTimingOverlayListener;
import vegas.sony.event.listener.SonyVegasClientEventListener;
import vegas.sony.event.listener.SonyVegasLifecycleEventListener;
import vegas.sony.event.listener.SonyVegasShutdownEventListener;
import vegas.sony.event.listener.WorldChangeEventDispatcher;
import vegas.sony.friend.FriendAliasEventListener;
import vegas.sony.friend.OnlineFriendActivityListener;
import vegas.sony.friend.ping.PingManager;
import vegas.sony.friend.ui.OnlineActivityPanelOptions;
import vegas.sony.input.InputEventDispatcher;
import vegas.sony.lifecycle.ClientDirectoryCleanupCallback;
import vegas.sony.manager.MacroManager;
import vegas.sony.manager.ModManager;
import vegas.sony.manager.SearchManager;
import vegas.sony.manager.ValueManager;
import vegas.sony.manager.client.EnemyManager;
import vegas.sony.manager.client.FriendManager;
import vegas.sony.manager.client.IndependentSettingsManager;
import vegas.sony.manager.client.OnlineConnectionManager;
import vegas.sony.manager.client.OnlineFriendManager;
import vegas.sony.manager.client.OnlineManager;
import vegas.sony.manager.client.ProfilesManager;
import vegas.sony.manager.client.PublicProfileManager;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapper;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.MappingProfileSnapshotRegistry;
import vegas.sony.mapping.PrimaryMappingTaskSet;
import vegas.sony.mapping.mappings.MMinecraft;
import vegas.sony.mapping.runtime.RuntimeNameMappingRegistry;
import vegas.sony.combat.AttackPacketTimingTracker;
import vegas.sony.module.macro.MacroEventListener;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.render.nametags.NameTagsRenderStateTracker;
import vegas.sony.module.render.entity.RenderEntityContextCacheListener;
import vegas.sony.module.utility.inventory.ItemStackSemanticResolver;
import vegas.sony.module.utility.inventory.cleaner.InventoryFilterPresetRegistry;
import vegas.sony.module.utility.inventory.cleaner.ui.InventoryCleanerProfileValueRefreshListener;
import vegas.sony.movement.PlayerMovementTaskManager;
import vegas.sony.notification.NotificationManager;
import vegas.sony.notification.NotificationSoundPlayer;
import vegas.sony.rotation.RotationManager;
import vegas.sony.remote.LocalControlServer;
import vegas.sony.remote.RemoteControlClient;
import vegas.sony.runtime.CheatBreakerDebugLog;
import vegas.sony.runtime.HostClientDetection;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.runtime.PayloadResources;
import vegas.sony.status.NativePresenceUpdater;
import vegas.sony.sync.SyncThread;
import vegas.sony.tutorial.TutorialManager;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.value.FloatingValueDropdownLayer;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.font.FontManager;
import vegas.sony.ui.font.FontOption;
import vegas.sony.ui.font.FontSelector;
import vegas.sony.unmap.BendableInputDispatcher;
import vegas.sony.unmap.GLUtils;
import vegas.sony.unmap.ItemHelper;
import vegas.sony.utils.AttackCooldownUtil;
import vegas.sony.utils.Base64Util;
import vegas.sony.utils.Str;
import vegas.sony.utils.TimerUtil;
import vegas.sony.utils.network.PacketDispatchGuard;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.ImageRenderer;
import vegas.sony.utils.render.OpenGlDeviceInfo;
import vegas.sony.utils.render.RenderBatchManager;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.PotionRegistry;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class SonyVegas {
    private static int opaqueState; // obfuscation opaque-predicate counter (get=getOpaqueState(), set=setOpaqueState(int)); do not treat as meaningful state
    public static SonyVegas INSTANCE;
    public static boolean mappingsLoaded;
    public boolean enabled;
    public static boolean renderReady;
    public static final boolean DEBUG = false; // always false; likely dead compile-time debug toggle
    public static final boolean DEV = false; // always false; likely dead compile-time toggle
    public static final String VERSION;
    static TimerUtil loadTimer;
    private boolean forgeAbsent;
    private boolean mappingsRemapped;
    private boolean vanillaMinecraftPresentCache;
    private boolean renderInitialized;
    private boolean vanillaMinecraftChecked;
    private boolean pendingTickAction; // one-shot: set via setPendingTickAction(true), consumed+cleared next client tick in EventTickBase.fire()
    private boolean unclassifiedFlag463; // purpose unconfirmed: setUnclassifiedFlag463(boolean)/isUnclassifiedFlag463Set have no discoverable callers
    private static volatile boolean pendingClientFrameInit;
    private static volatile boolean pendingBadlionRenderFinalization;
    private static volatile boolean pendingBadlionMappingRetry;
    private FriendManager friendManager;
    private FontManager fontManager;
    private ItemHelper itemHelper;
    private ProfilesManager profilesManager;
    private ClientListenerBootstrap clientListenerBootstrap;
    private PublicProfileSettings publicProfileSettings;
    private InventoryFilterPresetRegistry inventoryFilterPresetRegistry;
    private NotificationSoundPlayer notificationSoundPlayer;
    private GLUtils glUtils;
    private SearchManager searchManager;
    private EnemyManager enemyManager;
    private PublicProfileManager publicProfileManager;
    private AccountInfo accountInfo;
    private volatile boolean accountInfoReady;
    private volatile String accountInfoFailureReason = "not_initialized";
    private final Object accountInfoLock = new Object();
    private MacroManager macroManager;
    private String cachedAllData;
    private OnlineFriendManager onlineFriendManager;
    private ModManager modManager;
    private LicenseManager licenseManager;
    private IndependentSettingsManager independentSettingsManager;
    private Boolean isLabyModCache;
    private Boolean isCheatBreakerCache;
    private PrimaryMappingTaskSet primaryMappingTaskSet;
    private OnlineManager onlineManager;
    private ValueManager valueManager;
    private FontSelector fontSelector;
    private Mapper mapper;
    private NotificationManager notificationManager;
    private ItemStackSemanticResolver itemStackSemanticResolver;
    private vegas.sony.config.ClientSettings clientSettings;
    private NativePresenceUpdater nativePresenceUpdater;
    private Boolean isFabricPresentCache;
    private ModuleProfileMetadataCodec moduleProfileMetadataCodec;
    private Object directoryCleanupCallback;
    private SyncThread syncThread;
    //used for time bomb
    private static Date buildDate;
    private static int[] supportedVersionIds;

    public boolean isOnlineConnected() {
        return OnlineConnectionManager.INSTANCE.getGlobalSettingsController().isFirstRun();
    }

    public boolean isMappingsRemapped() {
        return this.mappingsRemapped;
    }

    public void loadConfigData(JsonObject configRoot, boolean useNewOtherDataKey) {
        if (configRoot == null) {
            CheatBreakerDebugLog.info("CONFIG_ROOT_NULL");
            return;
        }
        CheatBreakerDebugLog.info("CONFIG_APPLY keys=" + configRoot.keySet()
                + " profiles=" + configRoot.has("profiles")
                + " otherKey=" + (useNewOtherDataKey ? "otherData" : "otherdata"));
        JsonObject profilesData;
        JsonObject profilesElement;
        JsonArray friendsArray = ConfigJsonUtils.getJsonArray(configRoot, "friends");
        if (friendsArray != null) {
            this.friendManager.loadFriends(friendsArray);
        }
        if ((profilesElement = ConfigJsonUtils.getJsonObject(configRoot, "profiles")) != null) {
            profilesData = configRoot.get("profiles").getAsJsonObject();
            this.profilesManager.loadJson(profilesData);
        } else {
            SonyVegas.debugLog("profilesData is NULL!");
        }
        JsonArray otherData = ConfigJsonUtils.getJsonArray(configRoot, useNewOtherDataKey ? "otherData" : "otherdata");
        if (otherData != null && otherData.size() > 0) {
            this.independentSettingsManager.loadIndependentSettings(otherData);
        } else {
            SonyVegas.debugLog("otherData is NULL!");
        }
        INSTANCE.getProfilesManager().updatePublicProfileLinks();
    }

    public MacroManager getMacrosManager() {
        return this.macroManager;
    }

    public void loadMappings() {
        this.traceStep(18);
        int opaqueSeed = SonyVegas.getOpaqueState();
        MappedClasses.p();
        mappingsLoaded = true;
        int opaqueBranch = opaqueSeed;
        if (opaqueBranch != 0) {
            this.traceStep(19);
            if (this.forgeAbsent && ForgeVersion.MC_26_1.v()) {
                NativeBridge.fs();
                MappedClasses.p();
                NativeBridge.rsc();
            }
            MappingProfileSnapshotRegistry.X();
            RuntimeNameMappingRegistry.initializeRegistry();
            this.traceStep(20);
            this.mapper = new Mapper();
            this.mapper.loadMappings();
            this.traceStep(21);
            MappingProfileSnapshotRegistry.y();
            if (this.isForgeRemapActive()) {
                NativeBridge.fs();
                Mapper.RF.clear();
                this.mappingsRemapped = true;
                this.mapper = new Mapper();
                this.mapper.loadMappings();
            } else {
                this.mappingsRemapped = false;
            }
            MappingProfileSnapshotRegistry.h();
            this.traceStep(22);
            NativeBridge.su(Minecraft.Q$src$Lvegas_sony_account_MinecraftSessionWrapper_$1ftnn3u().getUsername());
            if (GuiComponent.getLegacyComponentState() == null) {
                SonyVegas.setOpaqueState(++opaqueBranch);
            }
            return;
        }
        this.traceStep(19);
        this.mapper = new Mapper();
        this.mapper.loadMappings();
        this.traceStep(21);
        MappingProfileSnapshotRegistry.y();
        this.mappingsRemapped = false;
        MappingProfileSnapshotRegistry.h();
        this.traceStep(22);
        NativeBridge.su(Minecraft.Q$src$Lvegas_sony_account_MinecraftSessionWrapper_$1ftnn3u().getUsername());
        if (GuiComponent.getLegacyComponentState() == null) {
            SonyVegas.setOpaqueState(++opaqueBranch);
        }
    }

    public static String formatThrowable(Throwable error) {
        return "";
    }

    public FriendManager getFriendManager() {
        return this.friendManager;
    }

    public InventoryFilterPresetRegistry getInventoryFilterPresetRegistry() {
        return this.inventoryFilterPresetRegistry;
    }

    private void logInitError(String phase, Throwable error) {
        // Runtime diagnostics are intentionally disabled in production.
    }

    public static void setOpaqueState(int value) {
        opaqueState = value;
    }

    public void setPendingTickAction(boolean pending) {
        this.pendingTickAction = pending;
    }

    public Mapper getMappings() {
        return this.mapper;
    }

    public static void notifyNativeStackTrace() {
        DescUtils.traceStack();
        NativeBridge.mb(101);
    }

    private static boolean lambda$registerListeners$0(IEvent event) {
        return AttackStrengthTracker.INSTANCE.isHealthPredictionEnabled();
    }

    public SearchManager getSearch() {
        return this.searchManager;
    }

    public EnemyManager getEnemyManager() {
        return this.enemyManager;
    }

    public boolean isForgeRemapInactive() {
        return !this.isForgeRemapActive();
    }

    public NotificationSoundPlayer getNotificationSoundPlayer() {
        return this.notificationSoundPlayer;
    }

    public FontSelector getFontSelector() {
        return this.fontSelector;
    }

    public void setUnclassifiedFlag463(boolean value) {
        this.unclassifiedFlag463 = value;
    }

    public boolean isFabricMinecraftPresent() {
        if (this.isFabricPresentCache == null) {
            String fabricMinecraftClass = "net/minecraft/class_310";
            this.isFabricPresentCache = NativeBridge.gc(fabricMinecraftClass) != null;
        }
        return this.isFabricPresentCache;
    }

    public ItemHelper getItemHelper() {
        return this.itemHelper;
    }

    public AccountInfo getAccountInfo() {
        return this.accountInfo;
    }

    public boolean isForgeRemapActive() {
        if (this.isLabyModPresent() || this.isCheatBreakerPresent()) {
            return false;
        }
        return ForgeVersion.MC_26_1.v();
    }

    public static byte[] readResource(String resourceName) {
        String resourcePath = "resources/" + resourceName;
        // In memory-injection mode classes and resource entries are owned by
        // different loaders. Read the embedded JAR through the registered
        // native bridge first, matching the original payload resource path.
        try {
            byte[] embedded = NativeBridge.gfb(resourcePath);
            if (embedded != null) {
                return embedded;
            }
        }
        catch (Throwable failure) {
            if (NativeBridge.isCheatBreaker189Runtime()) {
                CheatBreakerDebugLog.throwable(
                        "EMBEDDED_RESOURCE " + resourcePath, failure);
            }
        }
        byte[] fallback = PayloadResources.readBytes(
                SonyVegas.class, "/" + resourcePath);
        if (fallback == null && NativeBridge.isCheatBreaker189Runtime()) {
            CheatBreakerDebugLog.info("MISSING_RESOURCE " + resourcePath);
        }
        return fallback;
    }

    public boolean initAccountInfo() {
        if (this.accountInfoReady) {
            return this.accountInfo != null;
        }
        synchronized (this.accountInfoLock) {
            if (this.accountInfoReady) {
                return this.accountInfo != null;
            }
            boolean granted = this.loadAccountInfoFromProvider();
            this.accountInfoReady = true;
            if (!granted) {
                this.accountInfo = null;
            }
            return granted;
        }
    }

    /** Local-dev offline switch. Explicit opt-in only, never the default. */
    static boolean isDevOffline() {
        String devOfflineFlag = null;
        try {
            devOfflineFlag = System.getProperty("sv.devOffline //$");
        } catch (Throwable ignored) {
        }
        if (devOfflineFlag == null) {
            try {
                devOfflineFlag = System.getenv("SV_DEV_OFFLINE");
            } catch (Throwable ignored) {
            }
        }
        return "1".equals(devOfflineFlag) || "true".equalsIgnoreCase(devOfflineFlag);
    }

    /** Runs the (single) account-info round trip. Failure never authorizes. */
    private boolean loadAccountInfoFromProvider() {
        try {
            // Local-dev offline path: explicit opt-in only, never the default.
            // Lets Lunar/Forge local runs exercise every module without VPS:
            // -Dsv.devOffline=1 or SV_DEV_OFFLINE=1 synthesizes a dev account
            // and skips both the loader token and the signed-license halt.
            // Production (flag absent) keeps the exact previous behavior.
            if (SonyVegas.isDevOffline()) {
                // Dev-only tripwire: record any thread death (e.g. game thread
                // killed by a bad retransform) with a full stack in the local
                // debug log. Production installs never set this handler.
                try {
                    Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                        try {
                            CheatBreakerDebugLog.throwable("UNCAUGHT thread=" + thread.getName(), error);
                        } catch (Throwable ignored) {
                        }
                    });
                } catch (Throwable ignored) {
                }
                JsonObject devData = new JsonObject();
                devData.addProperty("userId", 7L);
                devData.addProperty("username", "dev-local");
                devData.addProperty("accountCreation", "2024-03-17T00:00:00.000Z");
                devData.addProperty("licensed", true);
                devData.addProperty("registered", true);
                devData.addProperty("profiles", true);
                devData.addProperty("banned", false);
                AccountInfo devAccount = AccountInfo.fromResponse(AccountInfoResponse.fromJson(devData));
                if (devAccount == null) {
                    this.accountInfoFailureReason = "invalid_account_data";
                    return false;
                }
                this.accountInfo = devAccount;
                this.accountInfoFailureReason = "";
                CheatBreakerDebugLog.info("ACCOUNT_INFO_READY devOffline local //$");
                return true;
            }
            String accountToken = ApiAccessTokenProvider.getAccessToken();
            if (accountToken == null || accountToken.isEmpty() || "0".equals(accountToken)) {
                this.accountInfoFailureReason = "missing_loader_token";
                CheatBreakerDebugLog.info("ACCOUNT_INFO_UNAVAILABLE reason=missing_loader_token");
                return false;
            }
            ApiResponse<AccountInfoResponse> response = ApiServices.getInstance().getAccountInfo().join();
            if (response == null || !response.isSuccessful() || response.getData() == null) {
                this.accountInfoFailureReason = response == null ? "no_response"
                        : !response.isSuccessful() ? "api_rejected" : "empty_account_data";
                CheatBreakerDebugLog.info("ACCOUNT_INFO_UNAVAILABLE reason="
                        + (response == null ? "no_response"
                        : !response.isSuccessful() ? "request_failed detail=" + response.getError() : "empty_data"));
                String error = response == null ? "empty response" : response.getError();
                SonyVegas.logError("Failed to initialize account details1: " + error);
                return false;
            }
            String licenseFailure = LicenseVerifier.verify(
                    response.getData().getLicense(), response.getData().getUserId());
            if (licenseFailure != null) {
                this.accountInfoFailureReason = "license_" + licenseFailure;
                // Hard stop: a server response sem licença assinada (ou com
                // assinatura inválida/expirada) means the product is being
                // served by an outdated or hostile endpoint. Owner requirement:
                // the game process must crash instead of running unlicensed.
                CheatBreakerDebugLog.info("ACCOUNT_INFO_UNAVAILABLE reason=license_" + licenseFailure);
                SonyVegas.logError("license verification failed: " + licenseFailure);
                Runtime.getRuntime().halt(13);
                return false; // unreachable; keeps the compiler happy
            }
            AccountInfo initializedAccount = AccountInfo.fromResponse(response.getData());
            if (initializedAccount == null) {
                this.accountInfoFailureReason = "invalid_account_data";
                CheatBreakerDebugLog.info("ACCOUNT_INFO_UNAVAILABLE reason=invalid_data");
                SonyVegas.logError("Failed to initialize account details1: empty account data");
                return false;
            }
            this.accountInfo = initializedAccount;
            this.accountInfoFailureReason = "";
            CheatBreakerDebugLog.info("ACCOUNT_INFO_READY profilesEnabled="
                    + initializedAccount.hasProfilesEnabled());
        }
        catch (CancellationException | CompletionException error) {
            Throwable rootCause = error;
            while (rootCause.getCause() != null) rootCause = rootCause.getCause();
            this.accountInfoFailureReason = "account_request_" + rootCause.getClass().getSimpleName()
                    + (rootCause instanceof vegas.sony.api.ApiHttpStatusException
                    ? "_http_" + ((vegas.sony.api.ApiHttpStatusException)rootCause).getStatusCode() : "")
                    + (rootCause instanceof java.io.IOException && rootCause.getMessage() != null
                    && rootCause.getMessage().matches("(?:winhttp|native_http)_[0-9]+")
                    ? "_" + rootCause.getMessage() : "");
            CheatBreakerDebugLog.info("ACCOUNT_INFO_UNAVAILABLE exception="
                    + error.getClass().getSimpleName()
                    + " cause=" + rootCause.getClass().getSimpleName()
                    + (rootCause instanceof vegas.sony.api.ApiHttpStatusException
                    ? " status=" + ((vegas.sony.api.ApiHttpStatusException)rootCause).getStatusCode() : "")
                    + " reason=" + this.accountInfoFailureReason);
            SonyVegas.logError("Failed to initialize account details2: " + SonyVegas.formatThrowable(error));
            return false;
        }
        catch (Throwable tokenProviderFailure) {
            this.accountInfoFailureReason = "account_provider_" + tokenProviderFailure.getClass().getSimpleName();
            // Without the native token source (e.g. non-native bootstrap) no
            // retry is useful; a thrown LinkageError previously escaped the
            // failure handling and left startup half-done.
            CheatBreakerDebugLog.info("ACCOUNT_INFO_UNAVAILABLE reason=token_provider_error type="
                    + tokenProviderFailure.getClass().getSimpleName());
            SonyVegas.logError("Failed to initialize account details3: "
                    + SonyVegas.formatThrowable(tokenProviderFailure));
            return false;
        }
        return true;
    }

    /** True only after initAccountInfo obtained a valid account payload. */
    public boolean isAuthorized() {
        return this.accountInfoReady && this.accountInfo != null;
    }

    /** Startup diagnostics contain reason codes, never tokens or request URLs. */
    public String getAccountInfoFailureReason() {
        return this.accountInfoFailureReason;
    }

    public boolean isFeatureDisabled() {
        return false;
    }

    public ModManager getModManager() {
        return this.modManager;
    }

    public vegas.sony.config.ClientSettings getClientSettings() {
        return this.clientSettings;
    }

    public void initializeManagers() {
        CheatBreakerDebugLog.info("INIT_MANAGERS begin version=" + ForgeVersion.c());
        this.glUtils = new GLUtils();
        this.glUtils.initializeVertexBuffer(24, 1);
        this.itemHelper = new ItemHelper();
        this.publicProfileManager = new PublicProfileManager();
        SonyVegas.opaquePredicate();
        this.fontSelector = new FontSelector();
        this.valueManager = new ValueManager();
        this.independentSettingsManager = new IndependentSettingsManager();
        this.friendManager = new FriendManager();
        this.onlineFriendManager = new OnlineFriendManager();
        this.enemyManager = new EnemyManager();
        this.macroManager = new MacroManager();
        this.searchManager = new SearchManager();
        this.clientSettings = new vegas.sony.config.ClientSettings();
        this.publicProfileSettings = new PublicProfileSettings();
        this.modManager = new ModManager();
        CheatBreakerDebugLog.info("INIT_MANAGERS modManager.init begin");
        this.modManager.init();
        CheatBreakerDebugLog.info("INIT_MANAGERS modManager.init end");
        this.moduleProfileMetadataCodec = new ModuleProfileMetadataCodec();
        this.traceStep(23);
        NativeBridge.sce("INIT phase=primary-mappings begin");
        CheatBreakerDebugLog.info("INIT_MANAGERS primaryMappings begin");
        this.initPrimaryMappingTasks();
        CheatBreakerDebugLog.info("INIT_MANAGERS primaryMappings end");
        NativeBridge.sce("INIT phase=primary-mappings end");
        this.traceStep(24);
        NativeBridge.sce("INIT phase=item-stack begin");
        CheatBreakerDebugLog.info("INIT_MANAGERS itemStack begin");
        this.itemStackSemanticResolver = new ItemStackSemanticResolver();
        this.itemStackSemanticResolver.loadMappings();
        CheatBreakerDebugLog.info("INIT_MANAGERS itemStack end");
        NativeBridge.sce("INIT phase=item-stack end");
        try {
            this.itemStackSemanticResolver.reportMissingMappings();
        }
        catch (Throwable ignored) {
            // empty catch block
        }
        PotionRegistry.d();
        NativeBridge.sce("INIT phase=registries begin");
        this.inventoryFilterPresetRegistry = new InventoryFilterPresetRegistry();
        this.profilesManager = new ProfilesManager();
        CheatBreakerDebugLog.info("INIT_MANAGERS modules begin");
        this.modManager.initializeAllModules();
        CheatBreakerDebugLog.info("INIT_MANAGERS modules end");
        NativeBridge.sce("INIT phase=registries end");
        NativeBridge.sce("INIT phase=notification begin");
        this.notificationManager = new NotificationManager();
        CheatBreakerDebugLog.info("INIT_MANAGERS notifications ready");
        NativeBridge.sce("INIT phase=notification end");
        NativeBridge.sce("INIT phase=sync-config begin");
        this.syncThread.loadConfig();
        CheatBreakerDebugLog.info("INIT_MANAGERS config end");
        NativeBridge.sce("INIT phase=sync-config end");
        this.traceStep(25);
        NativeBridge.sce("INIT phase=module-finish begin");
        this.modManager.finishModuleInitialization();
        NativeBridge.sce("INIT phase=module-finish end");
        try {
            if (this.publicProfileSettings.getSelectedProfile() != null) {
                NativeBridge.sce("INIT phase=profile-selected");
                this.getProfilesManager().setActiveProfile(this.publicProfileSettings.getSelectedProfile());
            } else {
                NativeBridge.sce("INIT phase=profile-load");
                this.getProfilesManager().getActiveProfile();
            }
            if (this.profilesManager.getActiveProfile() != null) {
                if (SonyVegas.INSTANCE.getPublicProfileSettings().autoLoadModuleStates.getEffectiveValue().booleanValue()) {
                    this.profilesManager.getActiveProfile().applyEnabledModuleStates();
                }
            }
        }
        catch (Throwable profileError) {
            /*
             * Badlion can retain a profile written by a different client build.
             * A malformed optional profile must not abort the native bootstrap
             * before listeners and the HUD are registered.  Keep the defaults
             * and record the exception for the native diagnostic log.
             */
            NativeBridge.sce("PROFILE_LOAD_FAILED " + profileError.getClass().getName()
                    + ": " + String.valueOf(profileError.getMessage()));
            if (!NativeBridge.isBadlion189Runtime()) {
                throw profileError;
            }
        }
        NativeBridge.sce("INIT phase=profiles-applied");
        vegas.sony.module.Mod auraModule = this.modManager.getMod(vegas.sony.module.combat.SilentAura.class);
        vegas.sony.module.Mod aimModule = this.modManager.getMod(vegas.sony.module.combat.AimAssist.class);
        vegas.sony.module.Mod kbModule = this.modManager.getMod(vegas.sony.module.combat.HitFlick.class);
        CheatBreakerDebugLog.info("EVENT_MODULE_STATES aura=" + (auraModule != null && auraModule.isEnabled())
                + " aim=" + (aimModule != null && aimModule.isEnabled())
                + " kb=" + (kbModule != null && kbModule.isEnabled())
                + " profiles=" + this.profilesManager.getProfiles().size());
        this.modManager.enableDefaultHudModules();
        NativeBridge.sce("INIT phase=defaults-enabled");
        this.traceStep(26);
        this.onlineManager = new OnlineManager();
        this.traceStep(27);
        this.notificationSoundPlayer = new NotificationSoundPlayer();
        InputEventDispatcher.getInstance().registerHandlers();
        this.initClientListeners();
        LocalControlServer.INSTANCE.start();
        RemoteControlClient.INSTANCE.start();
        this.syncThread.clearPendingSave();
        if (!NativeBridge.isBadlion189Runtime()) {
            this.showLoadCompleteNotification();
            try {
                this.getFontSelector().N((FontOption)SonyVegas.INSTANCE.getPublicProfileSettings().language.getValue());
            } catch (Throwable fontFailure) {
                vegas.sony.runtime.CheatBreakerDebugLog.throwable("boot font select", fontFailure);
            }
        }
        this.syncThread.start();
        this.traceStep(28);
        NativeBridge.dc();
        this.registerEventListeners();
        try {
            OnlineConnectionManager.INSTANCE.initialize();
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
        if (NativeBridge.isBadlion189Runtime()) {
            /*
             * TextNotificationContent and FontSelector create legacy font
             * renderers. Badlion bootstraps this method on the injector thread,
             * where LWJGL has no current context. Finish those two UI actions
             * from the final Display.update callback after every listener and
             * worker required by modules has already been started.
             */
            pendingBadlionRenderFinalization = true;
            NativeBridge.sce("INIT phase=render-finalization-pending");
        }
    }

    public ItemStackSemanticResolver getItemStackResolver() {
        return this.itemStackSemanticResolver;
    }

    public boolean isVanillaMinecraftPresent() {
        if (this.vanillaMinecraftChecked) {
            return this.vanillaMinecraftPresentCache;
        }
        this.vanillaMinecraftChecked = true;
        if (!this.forgeAbsent) {
            return false;
        }
        if (ForgeVersion.MC_26_1.d()) {
            return false;
        }
        String vanillaMinecraftClass = "net/minecraft/client/Minecraft";
        this.vanillaMinecraftPresentCache = NativeBridge.gc(vanillaMinecraftClass) != null;
        return this.vanillaMinecraftPresentCache;
    }

    public static int opaquePredicate() {
        int state = SonyVegas.getOpaqueState();
        if (state == 0) {
            return 20;
        }
        return 0;
    }

    public boolean isUnclassifiedFlag463Set() {
        return this.unclassifiedFlag463;
    }

    public ModuleProfileMetadataCodec getModuleProfileMetadataCodec() {
        return this.moduleProfileMetadataCodec;
    }

    public boolean isCheatBreakerPresent() {
        if (this.isCheatBreakerCache != null) {
            return this.isCheatBreakerCache;
        }
        if (!ForgeVersion.MC_1_8_9.L()) {
            this.isCheatBreakerCache = false;
            return false;
        }
        this.isCheatBreakerCache = HostClientDetection.isCheatBreakerPresent();
        return this.isCheatBreakerCache;
    }

    public boolean isLabyModPresent() {
        if (this.isLabyModCache != null) {
            return this.isLabyModCache;
        }
        if (!ForgeVersion.MC_1_8_9.L()) {
            this.isLabyModCache = false;
            return false;
        }
        try {
            Class.forName("net.laby.launcher.classloading.LabyClassLoader", false, SonyVegas.class.getClassLoader());
            this.isLabyModCache = true;
        }
        catch (Throwable ignored) {
            this.isLabyModCache = false;
        }
        return this.isLabyModCache;
    }

    public int getAccountTier() {
        if (this.accountInfo == null) {
            return 0;
        }
        return (int)this.accountInfo.getUserId();
    }

    public NotificationManager getNotificationManager() {
        return this.notificationManager;
    }

    public TutorialManager getTutorialManager() {
        return null;
    }

    public PrimaryMappingTaskSet getPrimaryMappingTaskSet() {
        return this.primaryMappingTaskSet;
    }

    public ValueManager getValueManager() {
        return this.valueManager;
    }

    public NativePresenceUpdater getNativePresenceUpdater() {
        return this.nativePresenceUpdater;
    }

    public boolean isTickActionPending() {
        return this.pendingTickAction;
    }

    public void saveAndStop() {
        Profile activeProfile;
        this.syncThread.markDirty();
        if (this.profilesManager != null && (activeProfile = this.profilesManager.getActiveProfileOrNull()) != null) {
            activeProfile.setDirty(true);
        }
    }

    public void exportFramesConfig(String filePath) {
        try {
            String output = this.modManager.buildTranslationTemplate();
            for (Frame frame : ClientSettings.getAllFrames()) {
                if (frame instanceof FloatingValueDropdownLayer || !frame.J$src$Z$1eqdghz()) continue;
                String frameLine = "frame." + frame.getName().toLowerCase().replace(" ", "_") + "=" + frame.getName();
                output = output + frameLine + "\n";
            }
            FileOutputStream fileOut = new FileOutputStream(filePath);
            fileOut.write(output.getBytes());
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
            {}
        }
    }

    public FontManager getFontManager() {
        return this.fontManager;
    }

    public OnlineManager getOnlineManager() {
        return this.onlineManager;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public static boolean isSupportedVersion() {
        for (int versionId : supportedVersionIds) {
            if (versionId != ForgeVersion.c()) continue;
            return true;
        }
        return false;
    }

    private static boolean lambda$registerListeners$1(IEvent event) {
        return EventBus.timingEnabled;
    }

    public void initClientListeners() {
        this.primaryMappingTaskSet.X();
        this.primaryMappingTaskSet.d();
        if (NativeBridge.isBadlion189Runtime()) {
            this.primaryMappingTaskSet.retransformApplied();
        }
        pendingBadlionMappingRetry = NativeBridge.isBadlion189Runtime();
        this.clientListenerBootstrap = new ClientListenerBootstrap();
        this.clientListenerBootstrap.registerListeners();
    }

    private void registerEventListeners() {
        EventBus.getInstance().registerListener(PacketDispatchGuard.b, new Predicate[0]);
        EventBus.getInstance().registerListener(new BendableInputDispatcher(), new Predicate[0]);
        EventBus.getInstance().registerListener(AttackPacketTimingTracker.INSTANCE, new Predicate[0]);
        EventBus.getInstance().registerListener(PingManager.INSTANCE, new Predicate[0]);
        EventBus.getInstance().registerListener(RotationManager.INSTANCE, new Predicate[0]);
        EventBus.getInstance().registerListener(PlayerMovementTaskManager.INSTANCE, new Predicate[0]);
        EventBus.getInstance().registerListener(OnlineFriendActivityListener.INSTANCE, new Predicate[0]);
        EventBus.getInstance().registerListener(OnlineActivityPanelOptions.INSTANCE, new Predicate[0]);
        EventBus.getInstance().registerListener(AttackStrengthTracker.INSTANCE, SonyVegas::lambda$registerListeners$0);
        EventBus.getInstance().registerListener(new ClientSettingsEventForwarder(), new Predicate[0]);
        EventBus.getInstance().registerListener(new WorldChangeEventDispatcher(), new Predicate[0]);
        EventBus.getInstance().registerListener(new RenderEntityContextCacheListener(), new Predicate[0]);
        EventBus.getInstance().registerListener(new MacroEventListener(), new Predicate[0]);
        EventBus.getInstance().registerListener(new InventoryCleanerProfileValueRefreshListener(), new Predicate[0]);
        EventBus.getInstance().registerListener(EventTimingOverlayListener.INSTANCE, SonyVegas::lambda$registerListeners$1);
        EventBus.getInstance().registerListener(new SonyVegasShutdownEventListener(), new Predicate[0]);
        EventBus.getInstance().registerListener(NameTagsRenderStateTracker.INSTANCE, new Predicate[0]);
        EventBus.getInstance().registerListener(this.notificationManager, new Predicate[0]);
        EventBus.getInstance().registerListener(new AttackCooldownUtil(), new Predicate[0]);
        EventBus.getInstance().registerListener(this.modManager, new Predicate[0]);
        if (ForgeVersion.MC_1_8_9.L()) {
            EventBus.getInstance().registerListener(new SonyVegasLifecycleEventListener(), new Predicate[0]);
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            EventBus.getInstance().registerListener(new EventNameFormatRewriteService(), new Predicate[0]);
        } else {
            EventBus.getInstance().registerListener(new FriendAliasEventListener(), new Predicate[0]);
        }
        if (ForgeVersion.MC_1_21_10.d()) {
            EventBus.getInstance().registerListener(new SonyVegasClientEventListener(), new Predicate[0]);
        }
    }

    private void showLoadCompleteNotification() {
        // Boot-time fonts may not be ready yet; a notification must never
        // abort the remaining startup (sync, listeners, online manager).
        try {
            this.notificationManager.showInfo("Opening Config",
                    "Opening a new secure Crimson session in your browser", 5000L);
        } catch (Throwable notificationFailure) {
            vegas.sony.runtime.CheatBreakerDebugLog.throwable("load complete notification", notificationFailure);
        }
    }

    public boolean isForgeAbsent() {
        return this.forgeAbsent;
    }

    public ProfilesManager getProfilesManager() {
        return this.profilesManager;
    }

    public void initializeRender() {
        if (this.renderInitialized) {
            return;
        }
        try {
            OpenGlDeviceInfo.collectDeviceInfo();
            vegas.sony.runtime.CheatBreakerDebugLog.info("INIT_MANAGERS initRender deviceinfo //$");
            if (GuiRenderPrimitives.d()) {
                try {
                    RenderBatchManager.getInstance();
                }
                catch (Throwable error) {
                    this.logInitError("RenderEngine initialization", error);
                    return;
                }
            }
            vegas.sony.runtime.CheatBreakerDebugLog.info("INIT_MANAGERS initRender batch //$");
            try {
                this.fontManager = new FontManager();
                this.fontManager.e();
            }
            catch (Throwable error) {
                this.logInitError("FontManager initialization", error);
                return;
            }
            vegas.sony.runtime.CheatBreakerDebugLog.info("INIT_MANAGERS initRender fonts //$");
            try {
                ImageRenderer.preloadResources();
            }
            catch (Throwable error) {
                this.logInitError("DrawTexture pre-cache", error);
                return;
            }
            vegas.sony.runtime.CheatBreakerDebugLog.info("INIT_MANAGERS initRender preload //$");
            try {
                GuiRenderPrimitives.B();
            }
            catch (Throwable error) {
                this.logInitError("Render initialization", error);
                return;
            }
            this.renderInitialized = true;
        }
        catch (Throwable error) {
            SonyVegas.logThrowable(error);
        }
    }

    public void traceStep(int step) {
        NativeBridge.trs(step);
    }

    public static void logThrowable(Throwable error) {
        // Release build: no diagnostic output.
    }

    public LicenseManager getLicenseManager() {
        return this.licenseManager;
    }

    public GLUtils getGlUtils() {
        return this.glUtils;
    }

    public static void logTimed(String message) {
        // Release build: no diagnostic output.
    }

    static {
        SonyVegas.setOpaqueState(66);
        VERSION = "4.21";
        supportedVersionIds = new int[]{13, 15, 23, 35, 36, 61};
        buildDate = new Date(1710640988922L);
        renderReady = false;
        mappingsLoaded = false;
        loadTimer = new TimerUtil();
    }

    public static void requestClientFrameInit() {
        pendingClientFrameInit = true;
    }

    public static void runPendingClientFrameInit() {
        if (pendingClientFrameInit && !ClientSettings.framesInitialized) {
            pendingClientFrameInit = false;
            try {
                ClientSettings.initializeFrames();
            }
            catch (Throwable error) {
                pendingClientFrameInit = true;
                NativeBridge.sce("FRAME_INIT_FAILED " + error.getClass().getName()
                        + ": " + String.valueOf(error.getMessage()));
            }
        }
        if (!ClientSettings.framesInitialized || !pendingBadlionRenderFinalization) {
            return;
        }
        pendingBadlionRenderFinalization = false;
        try {
            INSTANCE.getFontSelector().N((FontOption)INSTANCE.getPublicProfileSettings().language.getValue());
            NativeBridge.sce("FONT_INIT render thread OK");
        }
        catch (Throwable fontError) {
            NativeBridge.sce("FONT_INIT render thread failed -> "
                    + fontError.getClass().getName() + ": " + String.valueOf(fontError.getMessage()));
        }
        try {
            INSTANCE.showLoadCompleteNotification();
            NativeBridge.sce("LOAD_NOTIFICATION render thread OK");
        }
        catch (Throwable notificationError) {
            NativeBridge.sce("LOAD_NOTIFICATION render thread failed -> "
                    + notificationError.getClass().getName() + ": "
                    + String.valueOf(notificationError.getMessage()));
        }
    }

    /** Retries only mapping tasks that did not commit during bootstrap. */
    public static void retryPendingBadlionMappings() {
        if (!pendingBadlionMappingRetry) {
            return;
        }
        pendingBadlionMappingRetry = false;
        SonyVegas instance = INSTANCE;
        if (instance == null || instance.primaryMappingTaskSet == null) {
            return;
        }
        try {
            instance.primaryMappingTaskSet.d();
        }
        catch (Throwable error) {
            NativeBridge.sce("BADLION_MAPPING_RETRY_FAILED "
                    + error.getClass().getName() + ": "
                    + String.valueOf(error.getMessage()));
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private void initPrimaryMappingTasks() {
        int opaqueSeed = SonyVegas.opaquePredicate();
        if (NativeBridge.isLunar189Runtime()) {
            vegas.sony.mapping.JavassistMappingTask.preseedLunarGameClasses();
            // Frames initialize on the first Render2D fire (see the Lunar
            // branch in EventRender2D); no need to wait for the legacy drain.
            SonyVegas.requestClientFrameInit();
        }
        if (NativeBridge.isBadlion189Runtime()) {
            /*
             * Keep the complete 1.8.9 mapping set. The final-frame hook only
             * replaces Badlion's incompatible vanilla HUD injection; combat,
             * input, tick, movement and packet hooks are still required for
             * the modules themselves to work.
             */
            this.primaryMappingTaskSet = new PrimaryMappingTaskSet();
            requestClientFrameInit();
            return;
        }
        this.primaryMappingTaskSet = new PrimaryMappingTaskSet();
        if (NativeBridge.isBadlion189Runtime()
                || NativeBridge.isCheatBreaker189Runtime()) {
            /*
             * Badlion keeps the vanilla 1.8.9 namespace, but its client
             * loader does not expose the mapped addScheduledTask member at
             * the point the bootstrap thread runs. Calling Minecraft.v here
             * therefore resolves a null MappingMethod and aborts the whole
             * NativeBridge.start sequence before the GUI can be registered.
             * The Badlion Render2D hook is installed later by
             * initClientListeners(); defer frame setup until that hook fires.
             */
            if (NativeBridge.isCheatBreaker189Runtime()) {
                CheatBreakerDebugLog.info("initPrimaryMappingTasks CB: schedule initializeFrames");
                Object minecraft = Minecraft.i();
                CheatBreakerDebugLog.info("Minecraft.i()="
                        + (minecraft == null ? "null" : minecraft.getClass().getName()));
                MappingMethod scheduled = MMinecraft.m(this.getMappings().U);
                CheatBreakerDebugLog.info("addScheduledTask mapped="
                        + (scheduled != null && !scheduled.hasResolutionFailed())
                        + " runtime="
                        + (scheduled == null ? "null" : scheduled.getRuntimeName())
                        + " owner="
                        + (scheduled == null ? "null" : scheduled.getOwnerClass()));
            }
            FutureTask<Void> initializationTask = new FutureTask<Void>(() -> {
                ClientSettings.initializeFrames();
                return null;
            });
            boolean scheduled = false;
            try {
                Minecraft.v(initializationTask);
                scheduled = true;
            }
            catch (Throwable scheduleFailure) {
                if (NativeBridge.isBadlion189Runtime()
                        || NativeBridge.isCheatBreaker189Runtime()) {
                    CheatBreakerDebugLog.throwable("Minecraft.addScheduledTask", scheduleFailure);
                } else {
                    throw new CompletionException(scheduleFailure);
                }
            }
            if (scheduled) {
                try {
                    initializationTask.get(10L, TimeUnit.SECONDS);
                    if (NativeBridge.isCheatBreaker189Runtime()) {
                        CheatBreakerDebugLog.info("initializeFrames OK via MC thread");
                    }
                    return;
                }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    initializationTask.cancel(false);
                    throw new CompletionException(interrupted);
                }
                catch (ExecutionException execution) {
                    Throwable cause = execution.getCause();
                    if (NativeBridge.isCheatBreaker189Runtime()) {
                        CheatBreakerDebugLog.throwable(
                                "ClientSettings.initializeFrames",
                                cause == null ? execution : cause);
                    }
                    throw new CompletionException(
                            cause == null ? execution : cause);
                }
                catch (TimeoutException timeout) {
                    initializationTask.cancel(false);
                    if (!NativeBridge.isBadlion189Runtime()
                            && !NativeBridge.isCheatBreaker189Runtime()) {
                        throw new CompletionException(timeout);
                    }
                    CheatBreakerDebugLog.info(
                            "initializeFrames timed out; deferring to Render2D");
                }
            }
            if (!ClientSettings.framesInitialized) {
                SonyVegas.requestClientFrameInit();
                CheatBreakerDebugLog.info(
                        "pending initializeFrames on next Render2D; continuing boot");
                return;
            }
            return;
        }
        this.primaryMappingTaskSet.L();
        this.primaryMappingTaskSet.d();
        int opaqueBranch = opaqueSeed;
        boolean devOfflineBoot = SonyVegas.isDevOffline();
        boolean lunarBoot = NativeBridge.isLunar189Runtime();
        EventRenderWorldPassExecutorDrain.EXECUTOR.execute(ClientSettings::initializeFrames);
        long framesWaitStartNanos = System.nanoTime();
        long lastFramesWaitLogSec = 0L;
        try {
            while (!ClientSettings.framesInitialized) {
                try {
                    Thread.sleep(10L);
                    if (opaqueBranch != 0) return;
                    // Lunar: frames initialize on the first Render2D fire
                    // (Lunar189Render2DMappingTask + the Render2D gate), so
                    // the legacy drain wait is skipped outright. Every other
                    // runtime keeps its exact previous behavior.
                    if (lunarBoot) {
                        CheatBreakerDebugLog.info("INIT_MANAGERS Lunar skips frames wait; frames via Render2D //$");
                        break;
                    }
                    // Dev-only watchdog: the drain executor only runs while a
                    // world renders. If frames never initialize (title screen
                    // or a hook that never fires on this client), log progress
                    // and continue boot after 120s instead of stalling the
                    // worker forever. Production keeps the original wait.
                    long waitedSec = (System.nanoTime() - framesWaitStartNanos) / 1000000000L;
                    if (waitedSec - lastFramesWaitLogSec >= 10L) {
                        lastFramesWaitLogSec = waitedSec;
                        CheatBreakerDebugLog.info("INIT_MANAGERS waiting frames " + waitedSec + "s //$");
                    }
                    if (devOfflineBoot && waitedSec >= 120L) {
                        CheatBreakerDebugLog.info("INIT_MANAGERS frames wait timeout, continuing boot (devOffline) //$");
                        break;
                    }
                }
                catch (InterruptedException interrupted) {
                    SonyVegas.logThrowable(interrupted);
                    if (opaqueBranch == 0) continue;
                    break;
                }
            }
        }
        catch (CompletionException completionException) {
            try {
                throw SonyVegas.rethrow(completionException);
            }
            catch (CompletionException retriedCompletionException) {
                throw SonyVegas.rethrow(retriedCompletionException);
            }
        }
        this.primaryMappingTaskSet.C();
    }

    public OnlineFriendManager getOnlineFriendManager() {
        return this.onlineFriendManager;
    }

    public IndependentSettingsManager getSettingsManager() {
        return this.independentSettingsManager;
    }

    public static void logError(String message) {
        {}
    }

    public static void debugLog(String message) { }

    private static <T extends Throwable> T rethrow(T error) {
        return error;
    }

    public SyncThread getSyncThread() {
        return this.syncThread;
    }

    public PublicProfileManager getPublicProfileManager() {
        return this.publicProfileManager;
    }

    public PublicProfileSettings getPublicProfileSettings() {
        return this.publicProfileSettings;
    }

    public static int getOpaqueState() {
        return opaqueState;
    }

    public SonyVegas() {
        int opaqueBranch = SonyVegas.opaquePredicate();
        if (opaqueBranch != 0) {
            this.nativePresenceUpdater = new NativePresenceUpdater();
            this.syncThread = new SyncThread(this);
            this.vanillaMinecraftChecked = false;
            this.vanillaMinecraftPresentCache = false;
            this.isFabricPresentCache = null;
            this.renderInitialized = false;
            this.isLabyModCache = null;
            this.isCheatBreakerCache = null;
            HostClientDetection.reset();
            this.traceStep(17);
            INSTANCE = this;
            this.forgeAbsent = NativeBridge.isForgeAbsent();
            this.directoryCleanupCallback = new ClientDirectoryCleanupCallback();
            GuiComponent.setLegacyComponentState(new GuiComponent[4]);
            return;
        }
        this.nativePresenceUpdater = new NativePresenceUpdater();
        this.syncThread = new SyncThread(this);
        this.vanillaMinecraftChecked = false;
        this.vanillaMinecraftPresentCache = false;
        this.isFabricPresentCache = null;
        this.renderInitialized = false;
        this.isLabyModCache = null;
        this.isCheatBreakerCache = null;
        HostClientDetection.reset();
        this.traceStep(17);
        INSTANCE = this;
        this.forgeAbsent = NativeBridge.isForgeAbsent();
        this.directoryCleanupCallback = new ClientDirectoryCleanupCallback();
    }

    public synchronized String getDecodedAllData(boolean refresh) {
        block6: {
            block5: {
                try {
                    if (!refresh || this.cachedAllData != null) break block5;
                }
                catch (CompletionException completionException) {
                    throw SonyVegas.rethrow(completionException);
                }
                String encoded = NativeBridge.gp("all");
                byte[] decoded = Base64Util.decodeBase64(encoded);
                this.cachedAllData = new String(decoded);
                break block6;
            }
            try {
                if (this.cachedAllData == null) {
                    return "";
                }
            }
            catch (CompletionException completionException) {
                throw SonyVegas.rethrow(completionException);
            }
        }
        return this.cachedAllData;
    }

    public Mapper getMappingsCompat() {
        return this.getMappings();
    }

    public Mapper getMappingsMapperCompat() {
        return this.getMappings();
    }
}
