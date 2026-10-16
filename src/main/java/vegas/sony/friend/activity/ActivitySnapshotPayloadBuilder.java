package vegas.sony.friend.activity;

import vegas.sony.friend.activity.ActivityHealthData;
import vegas.sony.friend.activity.ActivityPositionData;
import vegas.sony.friend.activity.ActivitySnapshotPayload;
import vegas.sony.friend.activity.ActivityTargetData;
import org.jetbrains.annotations.Nullable;

public class ActivitySnapshotPayloadBuilder {
    @Nullable
    private ActivityTargetData target;
    @Nullable
    private ActivityHealthData health;
    @Nullable
    private ActivityPositionData position;

    public ActivitySnapshotPayloadBuilder withPosition(@Nullable ActivityPositionData activityPositionData) {
        this.position = activityPositionData;
        return this;
    }

    public ActivitySnapshotPayloadBuilder withTarget(@Nullable ActivityTargetData activityTargetData) {
        this.target = activityTargetData;
        return this;
    }

    public ActivitySnapshotPayload build() {
        return new ActivitySnapshotPayload(this.position, this.health, this.target);
    }

    public ActivitySnapshotPayloadBuilder withHealth(@Nullable ActivityHealthData activityHealthData) {
        this.health = activityHealthData;
        return this;
    }
}
