package vegas.sony.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import vegas.sony.runtime.NativeBridge;

/**
 * Account API transport. Callers pass only validated relative paths
 * ("/api/v1/..."); the native loader transport joins them to the pinned
 * control host and performs the request itself. No URL, host name or
 * scheme is ever materialized in the Java heap, and the transport applies
 * certificate pinning on every request.
 */
public class ApiHttpClient {
    private static boolean opaqueState;
    private static final DateFormat API_DATE_FORMAT;
    public static final Gson GSON;

    @Nullable
    @Contract(value = "null -> null")
    public static Date parseApiDate(@Nullable String dateText) throws ParseException {
        if (dateText == null) {
            return null;
        }
        return API_DATE_FORMAT.parse(dateText);
    }

    static {
        ApiHttpClient.setOpaqueState(true);
        GSON = new GsonBuilder().serializeNulls().create();
        API_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static String perform(String httpMethod, String path, @Nullable Object body) throws Exception {
        int[] statusOut;
        String response;
        if (path == null || !path.startsWith("/api/v1/")) {
            throw new IOException("invalid_api_path");
        }
        String jsonBody = body != null ? GSON.toJson(body) : null;
        statusOut = new int[1];
        response = NativeBridge.acb(httpMethod, path, jsonBody, statusOut);
        if (response == null) {
            if (statusOut[0] != 0) {
                // Non-2xx: preserve the historical status-exception contract.
                throw new ApiHttpStatusException(path, httpMethod, statusOut[0]);
            }
            throw new IOException("native_http_0");
        }
        return response;
    }

    private static <R> R request(String path, String httpMethod, Class<R> responseType,
            @Nullable Object body) throws Exception {
        return (R) GSON.fromJson(perform(httpMethod, path, body), responseType);
    }

    public static boolean opaquePredicate() {
        boolean state = ApiHttpClient.getOpaqueState();
        return false;
    }

    public static <R> ApiResponse<R> getApiResponse(String path, Function<JsonElement, R> dataParser)
            throws Exception {
        return ApiResponse.fromJson(ApiHttpClient.get(path, JsonObject.class), dataParser);
    }

    public static void setOpaqueState(boolean state) {
        opaqueState = state;
    }

    public static <R> ApiResponse<R> postApiResponse(String path, Object body,
            Function<JsonElement, R> dataParser) throws Exception {
        return ApiResponse.fromJson(ApiHttpClient.request(path, "POST", JsonObject.class, body), dataParser);
    }

    public static <R> R post(String path, Class<R> responseType, Object body) throws Exception {
        return ApiHttpClient.request(path, "POST", responseType, body);
    }

    public static <R> ApiResponse<R> deleteApiResponse(String path, Object body,
            Function<JsonElement, R> dataParser) throws Exception {
        return ApiResponse.fromJson(ApiHttpClient.request(path, "DELETE", JsonObject.class, body), dataParser);
    }

    public static <R> R delete(String path, Class<R> responseType) throws Exception {
        return ApiHttpClient.request(path, "DELETE", responseType, null);
    }

    public static <R> R delete(String path, Class<R> responseType, Object body) throws Exception {
        return ApiHttpClient.request(path, "DELETE", responseType, body);
    }

    public static boolean getOpaqueState() {
        return opaqueState;
    }

    public static <R> R get(String path, Class<R> responseType) throws Exception {
        return ApiHttpClient.request(path, "GET", responseType, null);
    }
}
