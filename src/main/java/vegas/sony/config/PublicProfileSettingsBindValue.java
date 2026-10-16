package vegas.sony.config;

import com.google.gson.JsonObject;
import vegas.sony.config.PublicProfileSettings;
import vegas.sony.input.BindSet;
import vegas.sony.value.BindValue;

public class PublicProfileSettingsBindValue
extends BindValue {
    final PublicProfileSettings settings;

    @Override
    public boolean loadJson(JsonObject object) {
        boolean loaded = super.loadJson(object);
        return loaded;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    public PublicProfileSettingsBindValue(PublicProfileSettings settings, Object owner, String name, BindSet bindSet) {
        super(owner, name, bindSet);
        this.settings = settings;
    }
}
