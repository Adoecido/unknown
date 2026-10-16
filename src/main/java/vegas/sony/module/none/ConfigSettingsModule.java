package vegas.sony.module.none;

import com.google.gson.JsonObject;
import vegas.sony.module.Mod;
import vegas.sony.value.Value;

public class ConfigSettingsModule
extends Mod {
    public void loadMatchingValues(JsonObject jsonObject) {
        for (Value<?, ?> value : this.getAllValues()) {
            if (!value.matchesJsonId(jsonObject)) continue;
            value.loadJson(jsonObject);
        }
    }

    public ConfigSettingsModule(String name) {
        super(name);
    }

}

