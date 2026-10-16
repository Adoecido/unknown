package vegas.sony.protocol;

import vegas.sony.protocol.ZeusPacketDirection;
import vegas.sony.protocol.ZeusProtocolState;
import vegas.sony.protocol.packet.AuthenticationPacket;
import vegas.sony.protocol.packet.AuthenticationResponsePacket;
import vegas.sony.protocol.packet.HandshakePacket;
import vegas.sony.protocol.packet.HandshakeResponsePacket;

final class ZeusUnauthenticatedProtocolState {
    private final ZeusProtocolState state;

    private ZeusUnauthenticatedProtocolState(ZeusProtocolState state) {
        this.state = state;
        this.registerPacket(ZeusPacketDirection.SERVER, HandshakePacket.class);
        this.registerPacket(ZeusPacketDirection.CLIENT, HandshakeResponsePacket.class);
        this.registerPacket(ZeusPacketDirection.SERVER, AuthenticationPacket.class);
        this.registerPacket(ZeusPacketDirection.CLIENT, AuthenticationResponsePacket.class);
    }

    static void register(ZeusProtocolState state) {
        new ZeusUnauthenticatedProtocolState(state);
    }

    private void registerPacket(ZeusPacketDirection direction, Class<? extends vegas.sony.protocol.packet.ZeusSerializablePacket> packetClass) {
        this.state.registerPacket(direction, packetClass);
    }
}
