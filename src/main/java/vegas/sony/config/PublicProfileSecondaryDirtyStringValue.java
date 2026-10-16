package vegas.sony.config;

import vegas.sony.config.PublicProfileSettings;
import vegas.sony.value.StringValue;

public class PublicProfileSecondaryDirtyStringValue
extends StringValue {
    final PublicProfileSettings settings;
    boolean dirty;

    public PublicProfileSecondaryDirtyStringValue(PublicProfileSettings settings, Object owner, String name, String defaultValue) {
        super(owner, name, defaultValue);
        this.settings = settings;
    }


    @Override
    public void notifyChangeListeners() {
        if (this.dirty) {
            return;
        }
        this.dirty = true;
    }
}
