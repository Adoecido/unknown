package vegas.sony.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Wire format contains settings only; UI metadata and personal binds stay local. */
final class PartySyncPayload {
    static final int MAX_BYTES = 16 * 1024;

    static List<JsonObject> build(JsonArray modules, Set<String> selected, boolean activate) {
        List<JsonObject> result = new ArrayList<JsonObject>();
        JsonArray batch = new JsonArray();
        for (JsonElement element : modules) {
            JsonObject source = element.getAsJsonObject();
            String name = source.get("name").getAsString();
            if (!selected.contains(name)) continue;
            JsonObject module = new JsonObject();
            module.addProperty("name", name);
            if (activate) module.addProperty("enabled", true);
            JsonArray values = new JsonArray();
            for (JsonElement raw : source.getAsJsonArray("values")) {
                JsonObject value = raw.getAsJsonObject();
                if (value.has("personal") && value.get("personal").getAsBoolean()) continue;
                JsonObject compact = new JsonObject();
                compact.add("id", value.get("id"));
                compact.add("value", value.get("value"));
                values.add(compact);
            }
            module.add("values", values);
            List<JsonObject> fragments = new ArrayList<JsonObject>();
            JsonArray single = new JsonArray();
            single.add(module);
            if (size(payload(single)) <= MAX_BYTES) {
                fragments.add(module);
            } else {
                // Keep each value intact; activate only after this module's final fragment.
                JsonArray part = new JsonArray();
                for (JsonElement value : values) {
                    JsonArray candidate = part.deepCopy();
                    candidate.add(value);
                    JsonObject fragment = fragment(name, candidate, activate);
                    single = new JsonArray();
                    single.add(fragment);
                    if (size(payload(single)) > MAX_BYTES) {
                        if (part.size() == 0) throw new IllegalArgumentException("Single setting exceeds party limit");
                        fragments.add(fragment(name, part, false));
                        part = new JsonArray();
                    }
                    part.add(value);
                    single = new JsonArray();
                    single.add(fragment(name, part, activate));
                    if (size(payload(single)) > MAX_BYTES) throw new IllegalArgumentException("Single setting exceeds party limit");
                }
                fragments.add(fragment(name, part, activate));
            }
            for (JsonObject fragment : fragments) {
                JsonArray candidate = batch.deepCopy();
                candidate.add(fragment);
                if (size(payload(candidate)) > MAX_BYTES && batch.size() > 0) {
                    result.add(payload(batch));
                    batch = new JsonArray();
                }
                batch.add(fragment);
            }
        }
        if (batch.size() > 0) result.add(payload(batch));
        return result;
    }

    private static JsonObject fragment(String name, JsonArray values, boolean activate) {
        JsonObject module = new JsonObject();
        module.addProperty("name", name);
        module.add("values", values);
        if (activate) module.addProperty("enabled", true);
        return module;
    }

    private static JsonObject payload(JsonArray modules) {
        JsonObject state = new JsonObject();
        state.add("modules", modules);
        JsonObject payload = new JsonObject();
        payload.add("state", state);
        return payload;
    }

    private static int size(JsonObject payload) {
        return payload.toString().getBytes(StandardCharsets.UTF_8).length;
    }
}
