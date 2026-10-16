package vegas.sony.manager.client;

import vegas.sony.api.ApiResponse;
import vegas.sony.api.ApiServices;
import vegas.sony.config.GlobalSettingsPayload;
import vegas.sony.config.SettingsDataType;
import vegas.sony.value.BooleanValue;

public class GlobalSettingsController {
    private boolean firstRun = false;
    private GlobalSettingsPayload settings;
    private boolean loadFailed = false;
    private final BooleanValue cacheData = BooleanValue.create(null, "Cache data //$ui", false, "Caches data locally to improve load time //$");

    public boolean isFirstRun() {
        return this.firstRun;
    }

    public BooleanValue getCacheData() {
        return this.cacheData;
    }

    public void save() {
        this.settings.setCacheEnabled(this.cacheData.getEffectiveValue());
        this.settings.setFirstRun(false);
        try {
            ApiServices.getInstance().getSettingsApi().saveSettings(SettingsDataType.GLOBAL, this.settings);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public void load() {
        try {
            ApiResponse apiResponse = ApiServices.getInstance().getSettingsApi().loadSettings(SettingsDataType.GLOBAL);
            this.loadFailed = false;
            if (apiResponse == null || !apiResponse.isSuccessful()) {
                this.settings = new GlobalSettingsPayload();
                this.settings.initializeDefaults();
            } else {
                this.settings = (GlobalSettingsPayload)apiResponse.getData();
            }
        }
        catch (Exception exception) {
            if (this.settings == null) {
                this.settings = new GlobalSettingsPayload();
            }
            this.settings.initializeDefaults();
            this.loadFailed = true;
        }
        this.firstRun = this.settings.isFirstRun();
        this.cacheData.setValue(this.settings.isCacheEnabled());
    }

    private static Exception propagateException(Exception exception) {
        return exception;
    }
}
