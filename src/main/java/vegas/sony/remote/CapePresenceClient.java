package vegas.sony.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Exchanges the authenticated in-game cape presence for one server. */
public final class CapePresenceClient {
    private CapePresenceClient() { }

    public static Map<String, String> exchange(String serverKey,
                                                String playerName,
                                                String style) throws Exception {
        return exchange(serverKey, playerName, style, true);
    }

    public static Map<String, String> exchange(String serverKey,
                                                String playerName,
                                                String style,
                                                boolean announce) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("server", serverKey);
        request.addProperty("player", playerName);
        // An empty style prevents an older server from refreshing our presence;
        // updated servers also remove it immediately when visible is false.
        request.addProperty("style", announce ? style : "");
        request.addProperty("visible", announce);
        JsonObject response = ControlApi.request("144", request);
        JsonArray players = response.getAsJsonArray("players");
        LinkedHashMap<String, String> capes = new LinkedHashMap<String, String>();
        if (players != null) {
            for (JsonElement element : players) {
                if (element == null || !element.isJsonObject()) continue;
                JsonObject player = element.getAsJsonObject();
                if (!player.has("name") || !player.has("style")) continue;
                String name = cleanName(player.get("name").getAsString());
                String cape = normalizeStyle(player.get("style").getAsString());
                if (name.isEmpty() || cape == null) continue;
                capes.put(name.toLowerCase(Locale.ROOT), cape);
            }
        }
        return Collections.unmodifiableMap(capes);
    }

    private static String normalizeStyle(String style) {
        if ("122".equals(style)) return "122";
        if ("123".equals(style)) return "123";
        if ("718".equals(style)) return "718";
        if ("436".equals(style)) return "436";
        if ("953".equals(style)) return "953";
        return null;
    }

    private static String cleanName(String raw) {
        if (raw == null) return "";
        String name = raw.replaceAll("§.", "").trim();
        return name.matches("[A-Za-z0-9_]{1,16}") ? name : "";
    }
}
