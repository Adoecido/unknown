package vegas.sony.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import sun.misc.Unsafe;
import vegas.sony.SonyVegas;
import vegas.sony.manager.ModManager;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.gladiador.Target;
import vegas.sony.value.Value;

/** No game, credentials, API requests or actual module lifecycle effects. */
public final class PartyTargetSyncProbe {
    private static final class Recipient extends Target {
        boolean active;
        String expected;
        @Override public boolean isEnabled() { return this.active; }
        @Override public void setEnabled(boolean enabled) {
            if (enabled && !this.expected.equals(this.playerName.getValue())) {
                throw new AssertionError("Target activated before receiving the leader's X settings");
            }
            this.active = enabled;
        }
    }
    private static final class Manager extends ModManager {
        Collection<Mod> modules;
        @Override public Collection<Mod> getTopLevelModules() { return this.modules; }
    }
    public static void main(String[] args) throws Exception {
        Field access = Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        SonyVegas client = (SonyVegas)((Unsafe)access.get(null)).allocateInstance(SonyVegas.class);
        SonyVegas.INSTANCE = client;
        Manager manager = new Manager();
        Field managerField = SonyVegas.class.getDeclaredField("modManager");
        managerField.setAccessible(true);
        managerField.set(client, manager);
        Recipient target = new Recipient();
        for (Value<?, ?> value : target.getAllValues()) value.setPersistenceSuppressed(true);
        target.playerName.setValue("OldY");
        target.expected = "LeaderX";
        target.getBind().getBoundInputs().add(82);
        manager.modules = Arrays.<Mod>asList(new Mod("Other", 0, Category.MISC), target);
        Method serialize = RemoteControlClient.class.getDeclaredMethod("serializeValue", Value.class, String.class);
        serialize.setAccessible(true);
        JsonObject source = new JsonObject();
        source.addProperty("name", "m1");
        source.addProperty("enabled", true);
        JsonArray settings = new JsonArray();
        int index = 0;
        for (Value<?, ?> value : target.getAllValues()) {
            if (value == target.playerName) {
                JsonObject x = (JsonObject)serialize.invoke(RemoteControlClient.INSTANCE, value, "v" + Integer.toString(index, 36));
                x.addProperty("value", "LeaderX");
                settings.add(x);
            }
            ++index;
        }
        JsonObject personal = new JsonObject();
        personal.addProperty("id", "private-bind");
        personal.addProperty("personal", true);
        personal.addProperty("value", "75");
        settings.add(personal);
        source.add("values", settings);
        JsonObject payload = PartyTargetSync.payload(source, "ABC123", "LeaderX");
        check(!payload.toString().contains("private-bind"), "personal binds were shared");
        PartyTargetSync.Publisher publisher = new PartyTargetSync.Publisher();
        check(publisher.shouldSend(payload, 1000), "initial target was not published");
        publisher.sent(payload, 1000);
        check(!publisher.shouldSend(payload, 1100), "unchanged snapshots flooded the API");
        check(publisher.shouldSend(payload, 3000), "heartbeat missing");
        JsonObject party = new JsonObject();
        party.addProperty("code", "ABC123");
        party.addProperty("myRole", "member");
        Field remoteParty = RemoteControlClient.class.getDeclaredField("remotePartyState");
        remoteParty.setAccessible(true);
        remoteParty.set(RemoteControlClient.INSTANCE, party);
        Method apply = RemoteControlClient.class.getDeclaredMethod("applyState", JsonObject.class);
        apply.setAccessible(true);
        apply.invoke(RemoteControlClient.INSTANCE, payload.getAsJsonObject("state"));
        check(target.active && "LeaderX".equals(target.playerName.getValue()), "recipient kept old Y instead of X");
        check(target.getBind().getBoundInputs().equals(Arrays.asList(82)), "recipient's own bind was changed");
        check(target.isAwaitingBrowseSelection(), "missing leader target allowed an unrelated local fallback");
        target.expected = "NextX";
        settings.get(0).getAsJsonObject().addProperty("value", "NextX");
        JsonObject next = PartyTargetSync.payload(source, "ABC123", "NextX");
        check(publisher.shouldSend(next, 1100), "target change was not sent immediately");
        apply.invoke(RemoteControlClient.INSTANCE, next.getAsJsonObject("state"));
        check("NextX".equals(target.playerName.getValue()), "subsequent target change was lost");
        source.addProperty("enabled", false);
        JsonObject off = PartyTargetSync.payload(source, "ABC123", "");
        check(!off.getAsJsonObject("state").getAsJsonArray("modules").get(0).getAsJsonObject().get("enabled").getAsBoolean(),
                "disabled source was accidentally activated");
        apply.invoke(RemoteControlClient.INSTANCE, off.getAsJsonObject("state"));
        check(!target.active, "leader's disable was not applied");
        JsonObject wrongParty = PartyTargetSync.payload(source, "OTHER1", "OldY");
        apply.invoke(RemoteControlClient.INSTANCE, wrongParty.getAsJsonObject("state"));
        check("NextX".equals(target.playerName.getValue()), "another party's queued event was applied");
        PartyTargetSync.Selection selection = new PartyTargetSync.Selection();
        check(selection.accept("ABC123", "LeaderX", 1000) && selection.follows("ABC123", false), "valid follower rejected");
        check(selection.isFresh(6999) && !selection.isFresh(7000), "stale target did not expire at the lease deadline");
        check(selection.follows("ABC123", false), "expiry released the target to unrelated local Y");
        check(!selection.follows("OTHER1", false), "left party kept its target override");
        selection.accept("ABC123", "LeaderX", 1000);
        check(!selection.follows("ABC123", true), "new leader kept following the former leader");
        check(!selection.accept("ABC123", "invalid-name!", 1000), "invalid target was accepted");
        System.out.println("Party Target: wire IDs, X before activation, automatic changes, disable, personal binds and stale safety passed");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
