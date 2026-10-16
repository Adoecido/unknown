package vegas.sony.api;

import vegas.sony.api.ApiAccessTokenProvider;
import vegas.sony.api.ApiHttpClient;
import vegas.sony.api.ApiResponse;
import vegas.sony.config.RefreshableSettingsPayload;
import vegas.sony.config.SettingsDataType;
import vegas.sony.config.SettingsPayload;

public class SettingsApi {

    public <T> T saveSettings(SettingsDataType settingsDataType, SettingsPayload settingsPayload) throws Exception {
        String accessToken = ApiAccessTokenProvider.getAccessToken();
        if (settingsPayload instanceof RefreshableSettingsPayload) {
            ((RefreshableSettingsPayload)settingsPayload).refreshFromCurrentSettings();
        }
        return (T)ApiHttpClient.post("/api/v1/" + accessToken + "/settings/save/" + settingsDataType.getScope().getRouteName(), settingsDataType.getPayloadClass(), settingsPayload);
    }

    public SettingsApi() {
    }

    private static Exception preserveException(Exception error) {
        return error;
    }

    public <T> ApiResponse<T> loadSettings(SettingsDataType settingsDataType) throws Exception {
        String accessToken = ApiAccessTokenProvider.getAccessToken();
        ApiResponse response = ApiHttpClient.get("/api/v1/" + accessToken + "/settings/load/" + settingsDataType.getScope().getRouteName(), ApiResponse.class);
        if (response == null) {
            return null;
        }
        if (!response.isSuccessful()) {
            return ApiResponse.failure(response.getError());
        }
        return ApiResponse.success((T)ApiHttpClient.GSON.fromJson(ApiHttpClient.GSON.toJson(response.getData()), settingsDataType.getPayloadClass()));
    }
}
