package vegas.sony.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import vegas.sony.SonyVegas;
import vegas.sony.config.PublicProfileSettings;
import vegas.sony.friend.Friend;
import vegas.sony.manager.ModManager;
import vegas.sony.manager.client.IndependentSettingsManager;
import vegas.sony.manager.client.FriendManager;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.gladiador.Target;
import vegas.sony.module.render.ClickGui;
import vegas.sony.sync.SyncThread;
import vegas.sony.value.Value;
import vegas.sony.wrapper.impl.ForgeVersion;
import sun.misc.Unsafe;

/** Verifies that every 1.8.9 module setting reaches the native menu snapshot. */
public final class BadlionModuleSnapshotProbe {
    private BadlionModuleSnapshotProbe() {
    }

    public static void main(String[] args) throws Exception {
        ForgeVersion.gameVersion = ForgeVersion.MC_1_8_9.getProtocolVersion();
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        SonyVegas client = (SonyVegas)((Unsafe)unsafeField.get(null)).allocateInstance(SonyVegas.class);
        SonyVegas.INSTANCE = client;
        ModManager manager = new ModManager();
        Field managerField = SonyVegas.class.getDeclaredField("modManager");
        managerField.setAccessible(true);
        managerField.set(client, manager);
        Field settingsManagerField = SonyVegas.class.getDeclaredField("independentSettingsManager");
        settingsManagerField.setAccessible(true);
        settingsManagerField.set(client, new IndependentSettingsManager());
        Field syncField = SonyVegas.class.getDeclaredField("syncThread");
        syncField.setAccessible(true);
        syncField.set(client, new SyncThread(client));
        Field publicSettingsField = SonyVegas.class.getDeclaredField("publicProfileSettings");
        publicSettingsField.setAccessible(true);
        publicSettingsField.set(client, new PublicProfileSettings());
        Field valueManagerField = SonyVegas.class.getDeclaredField("valueManager");
        valueManagerField.setAccessible(true);
        valueManagerField.set(client, new vegas.sony.manager.ValueManager());
        Field clientSettingsField = SonyVegas.class.getDeclaredField("clientSettings");
        clientSettingsField.setAccessible(true);
        clientSettingsField.set(client, new vegas.sony.config.ClientSettings());
        FriendManager friendManager = new FriendManager();
        Field friendManagerField = SonyVegas.class.getDeclaredField("friendManager");
        friendManagerField.setAccessible(true);
        friendManagerField.set(client, friendManager);
        friendManager.getFriends().add(new Friend("SavedFriend", "SavedFriend"));
        client.getClientSettings().addFriendBind.getValue().getBoundInputs().add(-98);
        manager.init();

        Method buildState = RemoteControlClient.class.getDeclaredMethod("buildState");
        buildState.setAccessible(true);
        JsonObject state = (JsonObject)buildState.invoke(RemoteControlClient.INSTANCE);
        JsonArray modules = state.getAsJsonArray("modules");
        require(manager.getMod("Friends") == null
                        && findModuleByPlainName(modules, "Friends") == null,
                "Friends still exists as a duplicate public module");
        JsonObject friends = state.getAsJsonObject("friends");
        require(friends != null && friends.getAsJsonArray("names").size() == 1
                        && "SavedFriend".equals(friends.getAsJsonArray("names").get(0).getAsString())
                        && friends.get("enabled").getAsBoolean()
                        && friends.get("middle").getAsBoolean()
                        && friends.getAsJsonArray("suggestions").toString().contains("SavedFriend"),
                "Settings lost the saved friend list or middle-click option");
        require(friendManager.isFriend("savedfriend") && friendManager.toJson().size() == 1,
                "Removing the module broke friend exclusion or persistence");

        int expectedModules = 0;
        int expectedValues = 0;
        int actualValues = 0;
        Set<String> types = new HashSet<String>();
        for (Mod module : manager.getTopLevelModules()) {
            if (module.getCategory() == Category.NONE || module instanceof ClickGui) continue;
            ++expectedModules;
            JsonObject serialized = findModule(modules, module);
            require(serialized != null, "Missing module from Badlion snapshot: " + module.getName());
            int expectedForModule = 0;
            for (Value<?, ?> value : module.getAllValues()) {
                if (value.isSerializable()) ++expectedForModule;
            }
            JsonArray values = serialized.getAsJsonArray("values");
            require(values != null && values.size() == expectedForModule,
                    "Incomplete settings for " + module.getName() + ": "
                            + (values == null ? 0 : values.size()) + '/' + expectedForModule);
            expectedValues += expectedForModule;
            actualValues += values.size();
            for (JsonElement element : values) {
                JsonObject value = element.getAsJsonObject();
                require(value.has("id") && value.has("name") && value.has("type") && value.has("value"),
                        "Malformed setting in " + module.getName() + ": " + value);
                require(value.get("id").getAsString().matches("v[0-9a-z]+"),
                        "Readable setting identifier leaked: " + value.get("id"));
                types.add(value.get("type").getAsString());
            }
        }
        require(modules.size() == expectedModules,
                "Wrong Badlion module count: " + modules.size() + '/' + expectedModules);
        require(actualValues == expectedValues, "Wrong Badlion setting total");
        require(types.contains("mode") && types.contains("flags") && types.contains("list")
                        && types.contains("range") && types.contains("color"),
                "Complex controls are absent from the Badlion snapshot: " + types);

        JsonObject aura = findModuleByPlainName(modules, "Aura");
        require(aura != null, "Aura is missing from the Badlion snapshot");
        JsonObject hitMode = findValue(aura, "Hit Mode");
        require(hitMode != null && hitMode.getAsJsonArray("options").size() == 2,
                "Aura Hit Mode must offer Legit and Blatant");
        JsonObject targets = findValue(aura, "Target Settings Aura");
        require(targets != null && targets.getAsJsonArray("options").size() == 7,
                "Aura target filters are incomplete");

        require(findModuleByPlainName(modules, "Aura2") == null,
                "Aura2 must not appear as a separate module");

        Target target = manager.getMod(Target.class);
        require(target != null, "Target module is missing");
        JsonObject targetSnapshot = findModuleByPlainName(modules, "Target");
        require(targetSnapshot != null && findValue(targetSnapshot, "Safe Hits") != null,
                "Safe Hits is missing from the Target menu snapshot");
        target.automatic.setValue(false);
        target.showBrowsedPlayers.setValue(false);
        require(target.showAllPlayers.areConditionsMet(),
                "Show All Players disappeared in Manual mode");
        target.automatic.setValue(true);
        require(target.showAllPlayers.areConditionsMet(),
                "Show All Players disappeared in Automatic mode");

        JsonObject cape = findModuleByPlainName(modules, "Cape");
        JsonObject capeStyle = cape == null ? null : findValue(cape, "Style");
        require(capeStyle != null && capeStyle.getAsJsonArray("options").size() == 5,
                "New cape styles are missing from the menu snapshot");
        require(findValue(cape, "Visibility") != null,
                "Cape visibility mode is missing from the menu snapshot");

        System.out.println("PASS: Badlion snapshot modules=" + modules.size()
                + " values=" + actualValues + " types=" + types.size()
                + ", Aura Hit Mode complete");
    }

    private static JsonObject findModule(JsonArray modules, Mod expected) {
        for (JsonElement element : modules) {
            JsonObject module = element.getAsJsonObject();
            require(module.get("name").getAsString().matches("m[0-9a-z]+"),
                    "Readable module identifier leaked: " + module.get("name"));
            if (module.has("label")
                    && expected.matchesName(module.get("label").getAsString())) return module;
        }
        return null;
    }

    private static JsonObject findModuleByPlainName(JsonArray modules, String name) {
        for (JsonElement element : modules) {
            JsonObject module = element.getAsJsonObject();
            if (module.has("label")
                    && plain(module.get("label").getAsString()).equalsIgnoreCase(name)) return module;
        }
        return null;
    }

    private static JsonObject findValue(JsonObject module, String id) {
        for (JsonElement element : module.getAsJsonArray("values")) {
            JsonObject value = element.getAsJsonObject();
            if (plain(value.get("name").getAsString()).equalsIgnoreCase(id)) return value;
        }
        return null;
    }

    private static String plain(String value) {
        return value == null ? "" : value.replaceAll("\\u00a7.", "")
                .replace("//$ui", "").replace("//$", "").trim().replaceAll("\\s+", " ");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
