package vegas.sony.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class ProtocolOutboundSafetyProbe {
    private ProtocolOutboundSafetyProbe() {
    }

    public static void main(String[] args) {
        StringBuilder oversized = new StringBuilder();
        for (int i = 0; i < 99795; ++i) {
            oversized.append('x');
        }
        String clipped = ZeusPacketBufferUtil.limitUtf8(oversized.toString(), Short.MAX_VALUE);
        if (clipped.length() != Short.MAX_VALUE) {
            throw new AssertionError("oversized ASCII value was not limited to protocol capacity");
        }
        ByteBuf buffer = Unpooled.buffer();
        ZeusPacketBufferUtil.writeString(buffer, oversized.toString());
        String restored = ZeusPacketBufferUtil.readString(buffer, Short.MAX_VALUE);
        if (!clipped.equals(restored)) {
            throw new AssertionError("outbound string serialization did not preserve the safe prefix");
        }
        String unicode = "ab😀😀😀😀";
        String unicodePrefix = ZeusPacketBufferUtil.limitUtf8(unicode, 7);
        if (!"ab😀".equals(unicodePrefix)) {
            throw new AssertionError("UTF-8 limiting split a character or selected the wrong prefix");
        }
        if (ZeusPacketBufferUtil.isStringWithinLimit(oversized.toString(), 128)
                || !ZeusPacketBufferUtil.isStringWithinLimit("127.0.0.1:25565", 128)
                || ZeusPacketBufferUtil.isStringWithinLimit(null, 128)) {
            throw new AssertionError("outbound field length validation is incorrect");
        }
        System.out.println("Outbound protocol string safety probe passed.");
    }
}
