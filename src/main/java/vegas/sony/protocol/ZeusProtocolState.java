package vegas.sony.protocol;

import vegas.sony.protocol.ZeusAuthenticatedProtocolState;
import vegas.sony.protocol.ZeusPacketDirection;
import vegas.sony.protocol.ZeusUnauthenticatedProtocolState;
import vegas.sony.protocol.packet.HeartbeatPacket;
import vegas.sony.protocol.packet.HeartbeatResponsePacket;
import vegas.sony.protocol.packet.ServerDisconnectPacket;
import vegas.sony.protocol.packet.ZeusSerializablePacket;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public enum ZeusProtocolState {
    UNAUTHENTICATED(ZeusUnauthenticatedProtocolState::register),
    AUTHENTICATED(ZeusAuthenticatedProtocolState::register);

    private final Map<ZeusPacketDirection, List<Class<? extends ZeusSerializablePacket>>> packetClassesByDirection = new HashMap<ZeusPacketDirection, List<Class<? extends ZeusSerializablePacket>>>();

    ZeusProtocolState(Consumer<ZeusProtocolState> registerStatePackets) {
        for (ZeusPacketDirection zeusPacketDirection : ZeusPacketDirection.values()) {
            this.packetClassesByDirection.put(zeusPacketDirection, new ArrayList<Class<? extends ZeusSerializablePacket>>());
        }
        this.registerPacket(ZeusPacketDirection.CLIENT, HeartbeatResponsePacket.class);
        this.registerPacket(ZeusPacketDirection.SERVER, HeartbeatPacket.class);
        this.registerPacket(ZeusPacketDirection.CLIENT, ServerDisconnectPacket.class);
        registerStatePackets.accept(this);
    }

    protected void registerPacket(ZeusPacketDirection zeusPacketDirection, Class<? extends ZeusSerializablePacket> clazz) {
        List<Class<? extends ZeusSerializablePacket>> list = this.packetClassesByDirection.get((Object)zeusPacketDirection);
        list.add(clazz);
    }

    private static InstantiationException a(InstantiationException instantiationException) {
        return instantiationException;
    }

    public int getPacketId(ZeusPacketDirection zeusPacketDirection, ZeusSerializablePacket zeusSerializablePacket) {
        List<Class<? extends ZeusSerializablePacket>> list = this.packetClassesByDirection.get((Object)zeusPacketDirection);
        return list.indexOf(zeusSerializablePacket.getClass());
    }

    public ZeusSerializablePacket createPacket(ZeusPacketDirection zeusPacketDirection, int n) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        List<Class<? extends ZeusSerializablePacket>> list = this.packetClassesByDirection.get((Object)zeusPacketDirection);
        if (n < 0 || n >= list.size()) {
            throw new RuntimeException("Attempted to get packet by id " + n + " in direction " + (Object)((Object)zeusPacketDirection) + " but it's out of bounds.");
        }
        Class<? extends ZeusSerializablePacket> clazz = list.get(n);
        if (clazz == null) {
            {}
            return null;
        }
        return clazz.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
    }
}
