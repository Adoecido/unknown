package vegas.sony.remote;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PartySyncProbe {
    public static void main(String[] args) {
        JsonArray modules = new JsonArray();
        Set<String> selected = new HashSet<String>();
        for (int i = 0; i < 100; i++) {
            JsonObject module = new JsonObject();
            module.addProperty("name", "Module" + i);
            module.addProperty("enabled", false);
            module.addProperty("category", "Do not transmit");
            JsonArray values = new JsonArray();
            for (int j = 0; j < 20; j++) {
                JsonObject value = new JsonObject();
                value.addProperty("id", "setting" + j);
                value.addProperty("value", "Teste de configuração çãé " + j);
                value.addProperty("name", "Do not transmit");
                values.add(value);
            }
            JsonObject bind = new JsonObject();
            bind.addProperty("personal", true);
            bind.addProperty("id", "private-bind");
            bind.addProperty("value", "82");
            values.add(bind);
            module.add("values", values);
            modules.add(module);
            if (i % 2 == 0) selected.add("Module" + i);
        }
        for (boolean activate : new boolean[]{false, true}) {
            List<JsonObject> batches = PartySyncPayload.build(modules, selected, activate);
            check(batches.size() > 1, "large selections must be batched");
            Set<String> received = new HashSet<String>();
            for (JsonObject payload : batches) {
                check(payload.toString().getBytes(StandardCharsets.UTF_8).length <= 16384, "wire byte limit");
                check(!payload.toString().contains("Do not transmit"), "UI metadata leaked");
                check(!payload.toString().contains("private-bind"), "personal bind leaked");
                for (JsonElement element : payload.getAsJsonObject("state").getAsJsonArray("modules")) {
                    JsonObject module = element.getAsJsonObject();
                    check(module.has("enabled") == activate, "config-only must omit enabled state");
                    if (activate) check(module.get("enabled").getAsBoolean(), "activate must enable even disabled source modules");
                    received.add(module.get("name").getAsString());
                    check(module.getAsJsonArray("values").size() == 20, "settings lost");
                }
            }
            check(received.equals(selected), "selection changed");
        }
        check(PartySyncPayload.build(modules, Collections.<String>emptySet(), false).isEmpty(), "empty selection");
        JsonObject large = modules.get(0).getAsJsonObject().deepCopy();
        JsonArray largeValues = new JsonArray();
        for (int i = 0; i < 1000; i++) {
            JsonObject value = new JsonObject();
            value.addProperty("id", "setting" + i);
            value.addProperty("value", "abcdefghijklmnopqrstuvwxyz0123456789");
            largeValues.add(value);
        }
        large.add("values", largeValues);
        JsonArray largeModules = new JsonArray();
        largeModules.add(large);
        List<JsonObject> fragments = PartySyncPayload.build(largeModules, Collections.singleton("Module0"), true);
        check(fragments.size() > 1, "large module must be split");
        int settings = 0;
        for (int i = 0; i < fragments.size(); i++) {
            JsonObject payload = fragments.get(i);
            check(payload.toString().getBytes(StandardCharsets.UTF_8).length <= 16384, "fragment limit");
            for (JsonElement element : payload.getAsJsonObject("state").getAsJsonArray("modules")) {
                JsonObject module = element.getAsJsonObject();
                settings += module.getAsJsonArray("values").size();
                check(module.has("enabled") == (i == fragments.size() - 1), "activate after last fragment");
            }
        }
        check(settings == 1000, "fragmented settings lost");
        check(RemoteControlClient.validBindKey(82) && RemoteControlClient.validBindKey(-98)
                && RemoteControlClient.validBindKey(0) && !RemoteControlClient.validBindKey(999), "bind validation");
        for (int key : new int[]{13, 37, 40, 96, 124, 186, 222, -100, -99, -97, -96}) {
            check(RemoteControlClient.validBindKey(key), "keyboard/mouse capture rejected: " + key);
        }
        check(!RemoteControlClient.validBindKey(-101) && !RemoteControlClient.validBindKey(256), "invalid key accepted");
        check(!new vegas.sony.event.impl.EventMouseOverUpdate(0).fire(), "callback before mappings must be harmless");
        System.out.println("Party selection, byte limits, modes, personal binds and early callback checks passed.");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
