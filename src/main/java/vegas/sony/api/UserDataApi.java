package vegas.sony.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import vegas.sony.api.ApiAccessTokenProvider;
import vegas.sony.api.ApiHttpClient;
import vegas.sony.api.ApiResponse;
import vegas.sony.api.UserDataResponse;
import vegas.sony.sync.RemoteProfileDataMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserDataApi {

    public CompletableFuture<ApiResponse<Boolean>> saveUserData(JsonObject userData) {
        String accessToken = ApiAccessTokenProvider.getAccessToken();
        return CompletableFuture.supplyAsync(() -> this.requestSaveUserData(accessToken, userData));
    }

    private ApiResponse requestSaveUserData(String accessToken, JsonObject userData) {
        try {
            return ApiHttpClient.postApiResponse("/api/v1/" + accessToken + "/profile/private/save/user/", userData, JsonElement::getAsBoolean);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public CompletableFuture<ApiResponse<RemoteProfileDataMap>> saveProfileData(JsonObject profileData) {
        String accessToken = ApiAccessTokenProvider.getAccessToken();
        return CompletableFuture.supplyAsync(() -> this.requestSaveProfileData(accessToken, profileData));
    }

    private ApiResponse requestSaveProfileData(String accessToken, JsonObject profileData) {
        try {
            return ApiHttpClient.postApiResponse("/api/v1/" + accessToken + "/profile/private/save/profile/", profileData, RemoteProfileDataMap::fromJson);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private ApiResponse requestUserData(String accessToken) {
        try {
            return ApiHttpClient.getApiResponse("/api/v1/" + accessToken + "/profile/private/all", UserDataResponse::fromJson);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private ApiResponse requestReservedProfileId(String accessToken) {
        try {
            return ApiHttpClient.postApiResponse("/api/v1/" + accessToken + "/profile/private/reserve/", null, UserDataApi::parseProfileId);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public CompletableFuture<ApiResponse<UserDataResponse>> getUserData() {
        String accessToken = ApiAccessTokenProvider.getAccessToken();
        return CompletableFuture.supplyAsync(() -> this.requestUserData(accessToken));
    }

    private static UUID parseProfileId(JsonElement profileIdElement) {
        return UUID.fromString(profileIdElement.getAsString());
    }

    public CompletableFuture<ApiResponse<UUID>> reserveProfileId() {
        String accessToken = ApiAccessTokenProvider.getAccessToken();
        return CompletableFuture.supplyAsync(() -> this.requestReservedProfileId(accessToken));
    }

    public UserDataApi() {
    }
}
