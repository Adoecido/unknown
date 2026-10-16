package vegas.sony.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import vegas.sony.api.ApiHttpClient;
import vegas.sony.config.ProfileRemoteMetadata;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class RemoteProfileData {
    @Nullable
    private final ProfileRemoteMetadata metadata;
    private final UUID ownerUuid;
    private final String sonyVegasVersion;
    private final Map<String, Object> data;
    private final UUID profileId;
    private final String name;

    RemoteProfileData(UUID ownerUuid, UUID profileId, String name, String sonyVegasVersion,
            Map<String, Object> data, @Nullable ProfileRemoteMetadata metadata) {
        this.ownerUuid = ownerUuid;
        this.profileId = profileId;
        this.name = name;
        this.sonyVegasVersion = sonyVegasVersion;
        this.data = data;
        this.metadata = metadata;
    }

    public Map<String, Object> getData() {
        return this.data;
    }

    public String getName() {
        return this.name;
    }

    public String getSonyVegasVersion() {
        return this.sonyVegasVersion;
    }

    @Nullable
    public ProfileRemoteMetadata getMetadata() {
        return this.metadata;
    }

    public String toString() {
        return "PrivateProfile{uuid=" + this.ownerUuid + "profileId=" + this.profileId
                + ", name='" + this.name + '\'' + ", data=" + this.data
                + ", metadata=" + this.metadata + '}';
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", this.ownerUuid != null ? this.ownerUuid.toString() : null);
        json.addProperty("profileId", this.profileId != null ? this.profileId.toString() : null);
        json.addProperty("name", this.name);
        json.addProperty("cv", this.sonyVegasVersion);
        json.add("data", this.data != null ? ApiHttpClient.GSON.toJsonTree(this.data) : null);
        json.add("metadata", this.metadata != null ? this.metadata.toJson() : null);
        return json;
    }

    public UUID getProfileId() {
        return this.profileId;
    }


    @Nullable
    @Contract(value="!null -> !null; null -> null")
    public static RemoteProfileData fromJson(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        JsonObject json = element.getAsJsonObject();
        JsonElement ownerUuidElement = json.get("uuid");
        UUID ownerUuid = ownerUuidElement == null || ownerUuidElement.isJsonNull()
                ? null
                : UUID.fromString(ownerUuidElement.getAsString());
        return new RemoteProfileData(
                ownerUuid,
                UUID.fromString(json.get("profileId").getAsString()),
                json.get("name").getAsString(),
                json.get("cv").getAsString(),
                (Map<String, Object>)ApiHttpClient.GSON.fromJson(json.get("data"), Map.class),
                ProfileRemoteMetadata.fromJson(json.get("metadata")));
    }
}

