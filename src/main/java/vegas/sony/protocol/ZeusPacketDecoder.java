package vegas.sony.protocol;

import vegas.sony.protocol.ZeusPacketBuffer;
import vegas.sony.protocol.ZeusPacketBufferUtil;
import vegas.sony.protocol.ZeusPacketDirection;
import vegas.sony.protocol.ZeusProtocolConstants;
import vegas.sony.protocol.ZeusProtocolState;
import vegas.sony.protocol.packet.ZeusSerializablePacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class ZeusPacketDecoder
extends ByteToMessageDecoder {
    private final ZeusPacketDirection packetDirection;
    private final boolean failOnEmptyPacket;
    private static final String EMPTY_PACKET_MESSAGE = "Read packet with no bytes";

    private static Exception propagateException(Exception exception) {
        return exception;
    }

    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() == 0) {
            if (this.failOnEmptyPacket) {
                throw new RuntimeException(EMPTY_PACKET_MESSAGE);
            }
            return;
        }
        ZeusProtocolState zeusProtocolState = (ZeusProtocolState)((Object)channelHandlerContext.channel().attr(ZeusProtocolConstants.Q).get());
        int n = ZeusPacketBufferUtil.readVarInt(byteBuf);
        ZeusSerializablePacket zeusSerializablePacket = zeusProtocolState.createPacket(this.packetDirection, n);
        try {
            zeusSerializablePacket.S(new ZeusPacketBuffer(byteBuf));
            list.add(zeusSerializablePacket);
        }
        catch (Exception exception) {
            if (this.failOnEmptyPacket) {
                {}
            }
            throw exception;
        }
    }

    public ZeusPacketDecoder(ZeusPacketDirection zeusPacketDirection, boolean bl) {
        this.packetDirection = zeusPacketDirection;
        this.failOnEmptyPacket = bl;
    }
}
