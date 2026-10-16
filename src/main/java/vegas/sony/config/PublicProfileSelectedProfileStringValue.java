package vegas.sony.config;

import vegas.sony.SonyVegas;
import vegas.sony.config.Profile;
import vegas.sony.config.PublicProfileSettings;
import vegas.sony.utils.StringUtils;
import vegas.sony.value.StringValue;
import java.util.UUID;

public class PublicProfileSelectedProfileStringValue
extends StringValue {
    final PublicProfileSettings settings;
    private static final String MISSING_ONLINE_UUID_SUFFIX = " has no online uuid";


    public String getActiveProfileId() {
        Profile activeProfile = SonyVegas.INSTANCE.getProfilesManager().getActiveProfile();
        UUID onlineId = activeProfile.getOnlineId();
        if (onlineId == null) {
            SonyVegas.debugLog(activeProfile.getName() + MISSING_ONLINE_UUID_SUFFIX);
            return "";
        }
        return onlineId.toString();
    }

    public PublicProfileSelectedProfileStringValue(PublicProfileSettings settings, Object owner, String name, String defaultValue) {
        super(owner, name, defaultValue);
        this.settings = settings;
    }

    public void setSelectedProfile(String profileIdentifier) {
        super.setValue(profileIdentifier);
        boolean isUuid = StringUtils.n(profileIdentifier);
        if (isUuid) {
            Profile profile = SonyVegas.INSTANCE.getProfilesManager().getProfileByOnlineId(UUID.fromString(profileIdentifier));
            if (profile != null) {
                PublicProfileSettings.setSelectedProfile(this.settings, profile);
            }
        } else {
            Profile profile = SonyVegas.INSTANCE.getProfilesManager().getProfileByName(profileIdentifier);
            if (profile != null) {
                PublicProfileSettings.setSelectedProfile(this.settings, profile);
            }
        }
    }
}
