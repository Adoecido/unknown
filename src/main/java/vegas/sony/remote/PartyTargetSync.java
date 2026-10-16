package vegas.sony.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collections;

/** Automatic Target snapshots; binds and ticket-specific Invsee data stay local. */
public final class PartyTargetSync {
    public static final long HEARTBEAT_MILLIS = 2000L;
    public static final long LEASE_MILLIS = 6000L;

    private PartyTargetSync() {}

    static JsonObject payload(JsonObject module, String code, String selectedPlayer) {
        if (!validCode(code) || !validPlayer(selectedPlayer)) return null;
        JsonArray modules = new JsonArray();
        modules.add(module);
        java.util.List<JsonObject> batches = PartySyncPayload.build(modules,
                Collections.singleton(module.get("name").getAsString()), false);
        if (batches.size() != 1) throw new IllegalArgumentException("Target snapshot exceeds party limit");
        JsonObject payload = batches.get(0);
        JsonObject state = payload.getAsJsonObject("state");
        state.getAsJsonArray("modules").get(0).getAsJsonObject().add("enabled", module.get("enabled"));
        JsonObject selection = new JsonObject();
        selection.addProperty("code", code);
        selection.addProperty("name", selectedPlayer);
        state.add("partyTarget", selection);
        return payload;
    }

    private static boolean validCode(String code) {
        return code != null && code.matches("[A-Z0-9]{6}");
    }

    private static boolean validPlayer(String name) {
        return name != null && (name.isEmpty() || name.matches("[A-Za-z0-9_]{1,16}"));
    }

    static boolean validSelection(JsonObject selection) {
        return selection.has("code") && selection.get("code").isJsonPrimitive()
                && selection.has("name") && selection.get("name").isJsonPrimitive()
                && validCode(selection.get("code").getAsString())
                && validPlayer(selection.get("name").getAsString());
    }

    static final class Publisher {
        private String signature = "";
        private long sentAt;

        boolean shouldSend(JsonObject payload, long now) {
            return payload != null && (!payload.toString().equals(this.signature)
                    || now - this.sentAt >= HEARTBEAT_MILLIS);
        }

        void sent(JsonObject payload, long now) {
            this.signature = payload.toString();
            this.sentAt = now;
        }

        void reset() { this.signature = ""; this.sentAt = 0L; }
    }

    public static final class Selection {
        private String code = "";
        private String name = "";
        private long receivedAt;

        public boolean accept(String code, String name, long now) {
            if (!validCode(code) || !validPlayer(name)) return false;
            this.code = code;
            this.name = name;
            this.receivedAt = now;
            return true;
        }

        public boolean follows(String currentCode, boolean leader) {
            if (leader || !this.code.equals(currentCode)) this.clear();
            return !this.code.isEmpty();
        }

        public boolean isFresh(long now) {
            return !this.code.isEmpty() && now >= this.receivedAt
                    && now - this.receivedAt < LEASE_MILLIS;
        }

        public String getName() { return this.name; }
        public void clear() { this.code = ""; this.name = ""; this.receivedAt = 0L; }
    }
}
