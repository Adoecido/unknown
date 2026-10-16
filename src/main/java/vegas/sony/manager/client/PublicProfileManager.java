package vegas.sony.manager.client;

import vegas.sony.SonyVegas;
import vegas.sony.api.ApiResponse;
import vegas.sony.api.ApiServices;
import vegas.sony.config.Profile;
import vegas.sony.config.PublicProfile;
import vegas.sony.config.PublicProfileShareInfo;
import vegas.sony.config.PublicProfileSummary;
import vegas.sony.event.impl.PublicProfileCreatedEvent;
import vegas.sony.event.impl.PublicProfileDeletedEvent;
import vegas.sony.event.impl.PublicProfileTagsUpdatedEvent;
import vegas.sony.notification.NotificationType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.UnmodifiableView;

public class PublicProfileManager {
    private final Map<String, String> tagsByLowercase = new LinkedHashMap<String, String>();
    public static final int reservedConstant;
    private final Map<Long, PublicProfile> profilesById = new LinkedHashMap<Long, PublicProfile>();
    static final boolean assertionsDisabled;


    private static ApiResponse handlePopularTagsFailure(Throwable throwable) {
        SonyVegas.logThrowable(throwable);
        return null;
    }

    private void handlePopularTagsResponse(ApiResponse apiResponse, Throwable throwable) {
        if (throwable != null) {
            SonyVegas.logThrowable(throwable);
            return;
        }
        if (!apiResponse.isSuccessful()) {
            return;
        }
        if (!assertionsDisabled && apiResponse.getData() == null) {
            throw new AssertionError();
        }
        this.replaceTags((Collection)apiResponse.getData());
    }

    public List<Profile> getDerivedProfiles() {
        ArrayList<Profile> derivedProfiles = new ArrayList<Profile>();
        for (PublicProfile publicProfile : this.profilesById.values()) {
            Profile profile;
            PublicProfileShareInfo publicProfileShareInfo = publicProfile.getShareInfo();
            if (publicProfileShareInfo == null || publicProfileShareInfo.getDerivedFrom() == null || (profile = SonyVegas.INSTANCE.getProfilesManager().getProfileByOnlineId(publicProfileShareInfo.getDerivedFrom())) == null) continue;
            derivedProfiles.add(profile);
        }
        return derivedProfiles;
    }

    public void addProfileTags(PublicProfile publicProfile) {
        this.addTags(publicProfile.getTags());
    }

    public void replaceTags(Collection<String> tags) {
        this.tagsByLowercase.clear();
        this.addTags(tags);
    }

    public void addProfile(PublicProfile publicProfile) {
        this.profilesById.put(publicProfile.getProfileId(), publicProfile);
        if (!publicProfile.getTags().isEmpty()) {
            this.addTags(publicProfile.getTags());
        }
        new PublicProfileCreatedEvent(publicProfile).fire();
    }

    public PublicProfileManager() {
        ApiServices.getInstance().getPublicProfileApi().getMostPopularTags().whenCompleteAsync(this::handlePopularTagsResponse).exceptionally(PublicProfileManager::handlePopularTagsFailure);
    }

    public Collection<String> getTags() {
        return this.tagsByLowercase.values();
    }

    public static void showInfo(String message) {
        SonyVegas.INSTANCE.getNotificationManager().show("Public Profiles", message, NotificationType.INFO, 5000L, true);
    }

    public void addTags(Collection<String> tags) {
        for (String tag : tags) {
            if (this.tagsByLowercase.containsKey(tag.toLowerCase())) continue;
            this.tagsByLowercase.put(tag.toLowerCase(), tag);
        }
        new PublicProfileTagsUpdatedEvent(tags).fire();
    }

    static {
        long reservedSeed = 2496869938925404163L;
        reservedConstant = (int)reservedSeed;
        assertionsDisabled = !PublicProfileManager.class.desiredAssertionStatus();
    }

    public void addSummaryTags(PublicProfileSummary summary) {
        this.addTags(summary.getTags());
    }

    public void replaceProfile(PublicProfile previousProfile, PublicProfile replacement) {
        PublicProfile existingProfile = this.profilesById.put(replacement.getProfileId(), replacement);
        if (existingProfile == null) {
            this.addProfile(replacement);
            return;
        }
        if (!replacement.getTags().isEmpty()) {
            this.addTags(replacement.getTags());
        }
        new PublicProfileDeletedEvent(previousProfile).fire();
        new PublicProfileCreatedEvent(replacement).fire();
    }

    public void removeProfile(PublicProfile publicProfile) {
        this.profilesById.remove(publicProfile.getProfileId());
        new PublicProfileDeletedEvent(publicProfile).fire();
    }

    public static void showWarning(String message) {
        SonyVegas.INSTANCE.getNotificationManager().show("Public Profiles", message, NotificationType.WARNING, 5000L, true);
    }

    public void replaceProfiles(Collection<PublicProfile> profiles) {
        this.profilesById.clear();
        for (PublicProfile publicProfile : profiles) {
            this.profilesById.put(publicProfile.getProfileId(), publicProfile);
        }
    }

    public @UnmodifiableView Map<Long, PublicProfile> getProfilesById() {
        return this.profilesById;
    }
}
