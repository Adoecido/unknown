package vegas.sony.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import vegas.sony.account.AccountInfoResponse;
import vegas.sony.ui.click.component.GuiComponent;

public class ApiServices {
    private static volatile ApiServices instance;
    private static GuiComponent[] legacyComponentState;
    private final UserDataApi userDataApi;
    private final PublicProfileApi publicProfileApi;
    private final SettingsApi settingsApi;

    public PublicProfileApi getPublicProfileApi() {
        return this.publicProfileApi;
    }

    public CompletableFuture<ApiResponse<AccountInfoResponse>> getAccountInfo() {
        Supplier<CompletableFuture<ApiResponse<AccountInfoResponse>>> override = accountInfoOverride;
        if (override != null) {
            return override.get();
        }
        String accessToken = ApiAccessTokenProvider.getAccessToken();
        return CompletableFuture.supplyAsync(() -> this.requestAccountInfo(accessToken));
    }

    /** Test seam: offline unit tests replace the live account-info supplier. */
    public static void setAccountInfoOverrideForTesting(
            Supplier<CompletableFuture<ApiResponse<AccountInfoResponse>>> override) {
        accountInfoOverride = override;
    }

    private static volatile Supplier<CompletableFuture<ApiResponse<AccountInfoResponse>>> accountInfoOverride;

    public static GuiComponent[] getLegacyComponentState() {
        return legacyComponentState;
    }

    private ApiResponse requestRegistration(String registrationCode) {
        String accessToken = ApiAccessTokenProvider.getAccessToken();
        try {
            return ApiHttpClient.get("/api/v1/" + accessToken + "/register/" + registrationCode,
                    ApiResponse.class);
        }
        catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    public static ApiServices getInstance() {
        return instance;
    }

    private ApiResponse<AccountInfoResponse> requestAccountInfo(String accessToken) {
        try {
            // Only the relative path exists in Java; the pinned host and the
            // TLS certificate policy live entirely inside the native loader.
            return ApiHttpClient.getApiResponse("/api/v1/" + accessToken + "/authenticated",
                    AccountInfoResponse::fromJson);
        }
        catch (UnsatisfiedLinkError missingLoader) {
            // No injected loader in this JVM: the historical startup signal.
            return ApiResponse.failure("missing_loader_endpoint");
        }
        catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    public SettingsApi getSettingsApi() {
        return this.settingsApi;
    }

    public CompletableFuture<ApiResponse<Boolean>> registerOnlineAccount(String registrationCode) {
        return CompletableFuture.supplyAsync(() -> this.requestRegistration(registrationCode));
    }

    public ApiServices() {
        this.publicProfileApi = new PublicProfileApi();
        this.settingsApi = new SettingsApi();
        this.userDataApi = new UserDataApi();
    }

    static {
        ApiServices.setLegacyComponentState(new GuiComponent[4]);
        instance = new ApiServices();
    }

    public static void setLegacyComponentState(GuiComponent[] state) {
        legacyComponentState = state;
    }

    public UserDataApi getUserDataApi() {
        return this.userDataApi;
    }
}
