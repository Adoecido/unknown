package vegas.sony.remote;

import com.google.gson.JsonObject;

/** Uses the authenticated native control bridge; no publisher secret or host in the client. */
public final class InvseeSyncClient {
    private InvseeSyncClient() {}
    public static JsonObject create() throws Exception { return ControlApi.request("145", null); }
    public static JsonObject poll() throws Exception { return ControlApi.request("146", null); }
    public static void close() throws Exception { ControlApi.request("147", null); }
}
