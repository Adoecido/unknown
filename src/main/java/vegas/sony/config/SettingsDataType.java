package vegas.sony.config;

import vegas.sony.config.GlobalSettingsPayload;
import vegas.sony.config.OnlineSettingsPayload;
import vegas.sony.config.SettingsPayload;
import vegas.sony.config.SettingsScope;

public enum SettingsDataType {
    GLOBAL(SettingsScope.GLOBAL, GlobalSettingsPayload.class),
    ONLINE(SettingsScope.ONLINE, OnlineSettingsPayload.class);

    private final SettingsScope scope;
    private final Class<? extends SettingsPayload> payloadClass;
    private static final /* synthetic */ SettingsDataType[] ENUM_VALUES;

    static {
        String[] serializedNames = new String[]{"ONLINE", "GLOBAL"};


        ENUM_VALUES = new SettingsDataType[]{GLOBAL, ONLINE};
    }

    public Class<? extends SettingsPayload> getPayloadClass() {
        return this.payloadClass;
    }

    private SettingsDataType(SettingsScope scope, Class<? extends SettingsPayload> payloadClass) {
        this.scope = scope;
        this.payloadClass = payloadClass;
    }

    public SettingsScope getScope() {
        return this.scope;
    }

}
