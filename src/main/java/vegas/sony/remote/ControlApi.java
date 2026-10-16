package vegas.sony.remote;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.module.misc.Panic;

final class ControlApi {
    private static final Gson GSON = new Gson();

    private ControlApi() {
    }

    static boolean isAvailable() {
        return !Panic.isActive() && hasSession();
    }

    static boolean hasSession() {
        try {
            return NativeBridge.rca();
        } catch (Throwable unavailable) {
            return false;
        }
    }

    static JsonObject request(String action, JsonObject arguments) throws Exception {
        if (!isAvailable()) throw new ControlRequestException(401, "session_unavailable");
        String argsJson = arguments == null ? "" : GSON.toJson(arguments);
        String response = NativeBridge.cca(action, argsJson);
        if (response == null || response.isEmpty()) {
            throw new ControlRequestException(502, "native request failed");
        }
        JsonObject result = JsonParser.parseString(response).getAsJsonObject();
        if (result == null) throw new IllegalStateException("Invalid control API response");
        if (!result.has("ok") || !result.get("ok").getAsBoolean()) {
            String error = result.has("error") ? result.get("error").getAsString() : "request_failed";
            throw new ControlRequestException(502, error);
        }
        return result;
    }
}
