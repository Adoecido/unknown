package vegas.sony.protocol.packet;

import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.packet.ZeusSerializablePacket;

public class LocationCheckResponsePacket
implements ZeusSerializablePacket {
    private boolean locationValid;

    public boolean isLocationValid() {
        return this.locationValid;
    }

    public LocationCheckResponsePacket(boolean locationValid) {
        this.locationValid = locationValid;
    }

    public LocationCheckResponsePacket() {
    }

    @Override
    public void S(ZeusPacketBuffer zeusPacketBuffer) {
        this.locationValid = zeusPacketBuffer.readBoolean();
    }

    @Override
    public void o(ZeusPacketBuffer zeusPacketBuffer) {
        zeusPacketBuffer.writeBoolean(this.locationValid);
    }
}
