package vegas.sony.sync;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import vegas.sony.SonyVegas;
import vegas.sony.account.AccountInfo;
import vegas.sony.api.ApiHttpClient;
import vegas.sony.api.ApiHttpStatusException;
import vegas.sony.api.ApiResponse;
import vegas.sony.api.ApiServices;
import vegas.sony.api.UserDataResponse;
import vegas.sony.config.Profile;
import vegas.sony.config.SettingsDataType;
import vegas.sony.manager.client.OnlineConnectionManager;
import vegas.sony.notification.SettingsSyncStatusNotification;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.runtime.CheatBreakerDebugLog;
import vegas.sony.utils.Base64Util;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncThread {
    private SyncStoreRequestWorker storeRequestWorker;
    private final SyncDebounceWorker debounceWorker;
    private final AtomicBoolean pendingSave = new AtomicBoolean();
    private final SonyVegas sonyvegas;
    private long lastSaveTime;
    private boolean onlineSettingsApplied;

    public SyncThread(SonyVegas sonyvegas) {
        this.sonyvegas = sonyvegas;
        this.debounceWorker = new SyncDebounceWorker();
    }

    public void saveSettings() {
        CheatBreakerDebugLog.info("CONFIG_SAVE_BEGIN pending=" + this.pendingSave.get());
        try {
            SettingsSyncStatusNotification notification = new SettingsSyncStatusNotification();
            if (!this.sonyvegas.getPublicProfileSettings().autoSave.getEffectiveValue()) {
                this.sonyvegas.getNotificationManager().enqueue(notification, true);
            }

            this.syncOnlineSettings();
            this.prepareActiveProfileForSave();

            JsonObject settingsPayload = this.buildSettingsPayload(true);
            JsonObject profilesPayload = this.sonyvegas.getProfilesManager().toJson(true);
            for (Profile profile : this.sonyvegas.getProfilesManager().getProfiles()) {
                profile.setSaveQueued(true);
            }

            ApiResponse<Boolean> settingsResponse = ApiServices.getInstance().getUserDataApi().saveUserData(settingsPayload)
                    .exceptionally(error -> handleSettingsSaveFailure(notification, error))
                    .join();
            this.updateSettingsSaveStatus(notification, settingsResponse);

            ApiResponse<RemoteProfileDataMap> profilesResponse = ApiServices.getInstance().getUserDataApi().saveProfileData(profilesPayload)
                    .exceptionally(error -> handleProfilesSaveFailure(notification, error))
                    .join();
            this.updateProfilesSaveStatus(notification, profilesResponse);
            this.applySavedProfileIds(profilesResponse);

            notification.complete();
            CheatBreakerDebugLog.info("CONFIG_SAVE_RESULT settings="
                    + (settingsResponse == null ? "null" : settingsResponse.isSuccessful())
                    + " profiles=" + (profilesResponse == null ? "null" : profilesResponse.isSuccessful()));
            if (notification.hasSaveError() && this.sonyvegas.getPublicProfileSettings().autoSave.getEffectiveValue()) {
                this.sonyvegas.getNotificationManager().show(notification);
            }
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
            CheatBreakerDebugLog.throwable("CONFIG_SAVE", exception);
        }
        finally {
            this.pendingSave.set(false);
            CheatBreakerDebugLog.info("CONFIG_SAVE_END");
        }
    }

    private void prepareActiveProfileForSave() {
        try {
            Profile activeProfile = this.sonyvegas.getProfilesManager().getActiveProfile();
            if (activeProfile != null) {
                activeProfile.captureCurrentState();
            }
        }
        catch (Throwable throwable) {
            SonyVegas.logThrowable(throwable);
        }
    }

    private void updateSettingsSaveStatus(SettingsSyncStatusNotification notification, ApiResponse<Boolean> response) {
        if (response == null) {
            if (notification.getSettingsSaveStatus() == 1) {
                notification.setSettingsSaveStatus(5);
            }
            return;
        }
        if (response.isSuccessful()) {
            notification.setSettingsSaveStatus(1);
        } else if (isRateLimited(response)) {
            notification.setSettingsSaveStatus(3);
        } else {
            notification.setSettingsSaveStatus(4);
        }
    }

    private void updateProfilesSaveStatus(SettingsSyncStatusNotification notification, ApiResponse<RemoteProfileDataMap> response) {
        if (response == null) {
            if (notification.getProfilesSaveStatus() == 1) {
                notification.setProfilesSaveStatus(5);
            }
            return;
        }
        if (response.isSuccessful()) {
            notification.setProfilesSaveStatus(1);
            for (Profile profile : this.sonyvegas.getProfilesManager().getProfiles()) {
                profile.setDirty(false);
            }
        } else if (isRateLimited(response)) {
            notification.setProfilesSaveStatus(3);
        } else {
            notification.setProfilesSaveStatus(4);
            notification.setApiErrorDetail(response.getError());
        }
    }

    private void applySavedProfileIds(ApiResponse<RemoteProfileDataMap> response) {
        if (response == null || !response.isSuccessful()) {
            return;
        }
        RemoteProfileDataMap responseData = response.getData();
        assert responseData != null;
        if (responseData == null) {
            throw new IllegalStateException("Successful profile save response did not include profile data");
        }
        for (Profile profile : this.sonyvegas.getProfilesManager().getProfiles()) {
            RemoteProfileData savedProfile = responseData.getProfiles().values().stream()
                    .filter(candidate -> matchesProfileName(profile, candidate))
                    .findFirst()
                    .orElse(null);
            if (savedProfile != null && !savedProfile.getProfileId().equals(profile.getOnlineId())) {
                profile.setOnlineId(savedProfile.getProfileId());
            }
        }
    }

    private static boolean matchesProfileName(Profile profile, RemoteProfileData remoteProfile) {
        String profileName = profile.getName();
        return remoteProfile.getName().equals(profileName)
                || remoteProfile.getName().equals("b64:" + Base64Util.encodeUtf8Base64(profileName));
    }

    private static boolean isRateLimited(ApiResponse<?> response) {
        String error = response.getError();
        return error != null && (error.contains("Slow down") || error.contains("Rate limited"));
    }

    private static ApiResponse<Boolean> handleSettingsSaveFailure(SettingsSyncStatusNotification notification, Throwable error) {
        Throwable cause = rootCause(error);
        if (cause instanceof ApiHttpStatusException) {
            notification.setSettingsSaveStatus(((ApiHttpStatusException)cause).getStatusCode());
        } else {
            notification.setSettingsSaveStatus(2);
        }
        return null;
    }

    private static ApiResponse<RemoteProfileDataMap> handleProfilesSaveFailure(SettingsSyncStatusNotification notification, Throwable error) {
        Throwable cause = rootCause(error);
        if (cause instanceof ApiHttpStatusException) {
            notification.setProfilesSaveStatus(((ApiHttpStatusException)cause).getStatusCode());
        } else {
            notification.setProfilesSaveStatus(2);
            if (cause instanceof IOException) {
                notification.setIoExceptionClassName(cause.getClass().getName());
                notification.setIoExceptionMessage(cause.getMessage());
            }
        }
        return null;
    }

    private static Throwable rootCause(Throwable throwable) {
        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return throwable;
    }

    public void requestSave() {
        this.lastSaveTime = System.currentTimeMillis();
        this.pendingSave.set(false);
        if (this.storeRequestWorker == null) {
            this.storeRequestWorker = new SyncStoreRequestWorker();
            new Thread(this.storeRequestWorker, "sw").start();
        }
        this.storeRequestWorker.requestSave();
    }

    public boolean hasPendingSave() {
        return this.pendingSave.get();
    }

    public long getLastSaveTime() {
        return this.lastSaveTime;
    }

    public void loadConfig() {
        try {
            AccountInfo accountInfo = this.sonyvegas.getAccountInfo();
            if (accountInfo == null) {
                CheatBreakerDebugLog.info("CONFIG_LOAD_SKIPPED accountInfo unavailable");
                return;
            }
            CheatBreakerDebugLog.info("CONFIG_LOAD_BEGIN profilesEnabled="
                    + accountInfo.hasProfilesEnabled());
            if (accountInfo.hasProfilesEnabled()) {
                this.loadRemoteConfig();
            } else {
                this.loadStandaloneConfig();
            }
        }
        catch (Throwable failure) {
            SonyVegas.logThrowable(failure);
            CheatBreakerDebugLog.throwable("CONFIG_LOAD", failure);
        }
    }

    private void loadRemoteConfig() {
        ApiResponse<UserDataResponse> response = ApiServices.getInstance().getUserDataApi().getUserData()
                .exceptionally(error -> null)
                .join();
        if (response == null || !response.isSuccessful()) {
            CheatBreakerDebugLog.info("CONFIG_LOAD_REMOTE_RESULT response="
                    + (response == null ? "null" : "unsuccessful"));
            return;
        }
        UserDataResponse userData = response.getData();
        assert userData != null;
        if (userData == null) {
            return;
        }

        HashMap<UUID, JsonObject> profiles = new HashMap<UUID, JsonObject>();
        for (RemoteProfileData remoteProfile : userData.getProfiles().values()) {
            profiles.put(remoteProfile.getProfileId(), remoteProfile.toJson());
        }
        this.sonyvegas.getPublicProfileManager().replaceProfiles(userData.getPublicProfiles().values());

        JsonObject config = new JsonObject();
        if (userData.getFriends() != null) {
            config.add("friends", userData.getFriends());
        }
        config.add("profiles", ApiHttpClient.GSON.toJsonTree(profiles));
        if (userData.getOtherData() != null) {
            config.add("otherData", userData.getOtherData());
        }
        this.sonyvegas.loadConfigData(config, true);
        CheatBreakerDebugLog.info("CONFIG_LOAD_REMOTE_APPLIED profiles=" + profiles.size());
    }

    private void loadStandaloneConfig() {
        String encodedSettings = NativeBridge.gp("all");
        String decodedSettings = encodedSettings == null
                ? ""
                : new String(Base64Util.decodeBase64(encodedSettings), StandardCharsets.UTF_8).trim();
        JsonReader reader = new JsonReader(new StringReader(decodedSettings));
        reader.setLenient(true);
        JsonObject config = new Gson().fromJson(reader, JsonObject.class);
        if (config == null) {
            CheatBreakerDebugLog.info("CONFIG_LOAD_STANDALONE_EMPTY");
            return;
        }
        this.sonyvegas.loadConfigData(config, false);
        CheatBreakerDebugLog.info("CONFIG_LOAD_STANDALONE_APPLIED");
        for (Profile profile : this.sonyvegas.getProfilesManager().getProfiles()) {
            profile.setDirty(true);
        }
    }

    public void clearPendingSave() {
        this.pendingSave.set(false);
    }

    public void markDirty() {
        this.pendingSave.set(true);
        this.debounceWorker.markChanged();
    }

    public void start() {
        new Thread(this.debounceWorker, "sd").start();
    }

    private void syncOnlineSettings() {
        if (!OnlineConnectionManager.INSTANCE.getSettings().hasLoadFailed()) {
            try {
                ApiServices.getInstance().getSettingsApi().saveSettings(SettingsDataType.ONLINE, OnlineConnectionManager.INSTANCE.getSettings().getPayload());
            }
            catch (Exception ignored) {
            }
        }
        try {
            boolean[] onlineSettingsState = NativeBridge.gls();
            boolean shouldApplySettings = onlineSettingsState[0];
            boolean secondaryState = onlineSettingsState[1];
            if (!this.onlineSettingsApplied && shouldApplySettings) {
                OnlineConnectionManager.INSTANCE.getGlobalSettingsController().getCacheData().setPersistenceSuppressed(true);
                OnlineConnectionManager.INSTANCE.getGlobalSettingsController().getCacheData().setValue(secondaryState);
                OnlineConnectionManager.INSTANCE.getGlobalSettingsController().getCacheData().setPersistenceSuppressed(false);
                this.onlineSettingsApplied = true;
            }
            OnlineConnectionManager.INSTANCE.getGlobalSettingsController().save();
        }
        catch (Throwable ignored) {
        }
    }

    public JsonObject buildSettingsPayload(boolean splitProfiles) {
        JsonObject payload = new JsonObject();
        payload.add("friends", this.sonyvegas.getFriendManager().toJson());
        payload.add(splitProfiles ? "otherData" : "otherdata", this.sonyvegas.getSettingsManager().toJson());
        if (!splitProfiles) {
            payload.add("profiles", this.sonyvegas.getProfilesManager().toJson(false));
        }
        return payload;
    }
}
