package com.google.commoncompat.primitives;

public class Longs {

    /**
     * Returns a big-endian representation of {@code value} in an 8-element byte array; equivalent to
     * {@code ByteBuffer.allocate(8).putLong(value).array()}. For example, the input value {@code
     * 0x1213141516171819L} would yield the byte array {@code {0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
     * 0x18, 0x19}}.
     *
     * <p>If you need to convert and concatenate several values (possibly even of different types),
     * use a shared {@link java.nio.ByteBuffer} instance, or use {@link
     * com.google.common.io.ByteStreams#newDataOutput()} to get a growable buffer.
     */
    public static byte[] toByteArray(long value) {
        // Note that this code needs to stay compatible with GWT, which has known
        // bugs when narrowing byte casts of long values occur.
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xffL);
            value >>= 8;
        }
        return result;
    }

}
