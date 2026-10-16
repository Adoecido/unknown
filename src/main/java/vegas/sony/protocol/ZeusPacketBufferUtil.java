package vegas.sony.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ZeusPacketBufferUtil {
    public static int readVarInt(ByteBuf byteBuf) {
        byte by;
        int n = 0;
        int n2 = 0;
        do {
            by = byteBuf.readByte();
            n |= (by & 0x7F) << n2++ * 7;
            if (n2 <= 5) continue;
            throw new RuntimeException("VarInt too big");
        } while ((by & 0x80) == 128);
        return n;
    }

    public static void writeVarInt(ByteBuf byteBuf, int value) {
        while ((value & 0xFFFFFF80) != 0) {
            byteBuf.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
        byteBuf.writeByte(value);
    }

    public static <E extends Enum<E>> E readEnum(ByteBuf byteBuf, Class<E> enumType) {
        return (E)((Enum[])enumType.getEnumConstants())[ZeusPacketBufferUtil.readVarInt(byteBuf)];
    }

    public static void writeString(ByteBuf byteBuf, String value) {
        String safeValue = ZeusPacketBufferUtil.limitUtf8(value, Short.MAX_VALUE);
        byte[] byArray = safeValue.getBytes(StandardCharsets.UTF_8);
        ZeusPacketBufferUtil.writeVarInt(byteBuf, byArray.length);
        byteBuf.writeBytes(byArray);
    }

    /**
     * The protocol stores string byte lengths in a signed short-sized field.  A bad
     * mapped game value must not be allowed to take down the Minecraft client while
     * Netty serializes an outbound packet.
     */
    public static String limitUtf8(String value, int maxBytes) {
        if (value == null || maxBytes <= 0) {
            return "";
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return value;
        }
        int low = 0;
        int high = value.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (middle < value.length() && middle > 0
                    && Character.isHighSurrogate(value.charAt(middle - 1))) {
                --middle;
            }
            if (middle <= low) {
                high = low;
                continue;
            }
            if (value.substring(0, middle).getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return value.substring(0, low);
    }

    public static boolean isStringWithinLimit(String value, int maxCharacters) {
        return value != null
                && maxCharacters >= 0
                && value.length() <= maxCharacters
                && value.getBytes(StandardCharsets.UTF_8).length <= Math.min(Short.MAX_VALUE, maxCharacters * 4);
    }

    public static UUID readUuid(ByteBuf byteBuf) {
        return new UUID(byteBuf.readLong(), byteBuf.readLong());
    }

    public static void writeUuid(ByteBuf byteBuf, UUID uuid) {
        byteBuf.writeLong(uuid.getMostSignificantBits());
        byteBuf.writeLong(uuid.getLeastSignificantBits());
    }

    public static String readString(ByteBuf byteBuf, int maxCharacters) {
        int n2 = ZeusPacketBufferUtil.readVarInt(byteBuf);
        if (n2 > maxCharacters * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + n2 + " > " + maxCharacters * 4 + ")");
        }
        if (n2 < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        }
        byte[] byArray = new byte[n2];
        byteBuf.readBytes(byArray);
        String string = new String(byArray, StandardCharsets.UTF_8);
        if (string.length() > maxCharacters) {
            throw new DecoderException("The received string length is longer than maximum allowed (" + n2 + " > " + maxCharacters + ")");
        }
        return string;
    }

    public static void writeEnum(ByteBuf byteBuf, Enum<?> value) {
        ZeusPacketBufferUtil.writeVarInt(byteBuf, value.ordinal());
    }

    private static RuntimeException preserveRuntimeException(RuntimeException runtimeException) {
        return runtimeException;
    }
}

