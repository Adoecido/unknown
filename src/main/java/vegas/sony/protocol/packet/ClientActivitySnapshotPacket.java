package vegas.sony.protocol.packet;

import vegas.sony.friend.activity.ActivitySnapshotPayload;
import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ZeusSerializablePacket;

public class ClientActivitySnapshotPacket
implements ZeusSerializablePacket {
    private ActivitySnapshotPayload activitySnapshot;

    public ClientActivitySnapshotPacket() {
    }

    public ActivitySnapshotPayload getActivitySnapshot() {
        return this.activitySnapshot;
    }

    @Override
    public void S(ZeusPacketBuffer zeusPacketBuffer) {
        this.activitySnapshot = new ActivitySnapshotPayload(zeusPacketBuffer);
    }

    public ClientActivitySnapshotPacket(ActivitySnapshotPayload activitySnapshot) {
        this.activitySnapshot = activitySnapshot;
    }

    @Override
    public void o(ZeusPacketBuffer zeusPacketBuffer) {
        this.activitySnapshot.writeTo(zeusPacketBuffer);
    }
}
