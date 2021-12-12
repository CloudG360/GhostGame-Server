package net.cg360.spookums.server.util;

import net.cg360.spookums.server.util.math.Vector2;

import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

/**
 * A utility class based in structure on Java's ByteBuffer. It
 * was implemented identically on both this project and the C#
 * client to ensure data sent across the network follows a
 * consistent format.
 *
 * This fixed a lot of the issues I was having when transferring data so nice
 */
public class NetworkBuffer {

    public static final int MAX_UNSIGNED_SHORT_VALUE = (int) (Math.pow(2, 16) - 1);
    public static final short MAX_UNSIGNED_BYTE_VALUE = (short) (Math.pow(2, 8) - 1);

    public static final double VECTOR2_ACCURACY = 100000D;

    public static final int SHORT_BYTE_COUNT = 2;
    public static final int INT_BYTE_COUNT = 4;
    public static final int LONG_BYTE_COUNT = 8;
    public static final int VECTOR2_BYTE_COUNT = 8;

    protected byte[] buffer;
    protected int pointerIndex;

    protected NetworkBuffer(byte[] bytes) {
        this.buffer = bytes;
        this.pointerIndex = 0;
    }

    // The 2 methods below are just nice ways of instantiating a NetworkBuffer, similar to a ByteBuffer

    public static NetworkBuffer wrap(byte... bytes) { return new NetworkBuffer(bytes); }
    public static NetworkBuffer allocate(int size) { return new NetworkBuffer(new byte[size]); }

    /** Counts the amount of bytes between the pointer (inclusive) and the end of the buffer. */
    public int countBytesRemaining() {
        return buffer.length - pointerIndex;
    }

    /** Checks if the buffer has a provided number of bytes left before the end. */
    public boolean canReadBytesAhead(int bytesAhead) {
        return countBytesRemaining() >= bytesAhead;
    }


    /** Sets pointer index to 0. */
    public void reset() {
        pointerIndex = 0;
    }

    /** Moves the pointer forward by 1 position. */
    public void skip() {
        skip(1);
    }

    /** Moves the pointer forward a set number of positions. */
    public void skip(int delta) {
        pointerIndex = Math.min(buffer.length - 1, pointerIndex + delta);
    }

    /** Rewinds the pointer by 1 position. */
    public void rewind() {
        rewind(1);
    }

    /** Rewinds the pointer a set number of positions. */
    public void rewind(int delta) {
        pointerIndex = Math.max(0, pointerIndex - delta);
    }

    public int capacity() {
        return this.buffer.length;
    }

    protected void incrementPointer() {
        // This was encased in a method literally to debug it.
        // It works now but I'm keeping it here :D
        pointerIndex++;
    }


    /** Unsafe way to fetch a byte. Make sure to check first :) */
    protected byte fetchRawByte() {
        byte b = buffer[pointerIndex];
        incrementPointer();
        return b;
    }

    /** Unsafe way to fetch a series of bytes. Make sure to check first :) */
    protected byte[] fetchRawBytes(int byteCount) {
        byte[] bytes = new byte[byteCount];

        for(int i = 0; i < byteCount; i++) {
            bytes[i] = buffer[pointerIndex];
            incrementPointer();
        }
        return bytes;
    }

    /** Unsafe way to write a byte. Make sure to check first :) */
    protected void writeByte(byte b) {
        buffer[pointerIndex] = b;
        incrementPointer();
    }

    /** Unsafe way to write a series of bytes. Make sure to check first :) */
    protected void writeBytes(byte[] bytes) {
        for(byte b : bytes) writeByte(b);
    }

    // Could be signed, unsigned, or even a string character.
    /** Fetches a byte from the buffer without converting it. */
    public byte get() {
        if(canReadBytesAhead(1)) {
            return fetchRawByte();
        }
        throw new BufferUnderflowException();
    }

    /** Fetches a quantity of bytes from the buffer without converting them. */
    public void get(byte[] target) {
        if(canReadBytesAhead(target.length)) {
            for(int i = 0; i < target.length; i++) target[i] = fetchRawByte();
            return;
        }
        throw new BufferUnderflowException();
    }

    /** @return a boolean from the current pointer position in the network buffer. */
    public boolean getBoolean() {
        if(canReadBytesAhead(1)) {
            byte value = get();
            return value == ((byte) 0x01);
        }
        throw new BufferUnderflowException();
    }

    /** @return an unsigned byte from the current pointer position in the network buffer. */
    public short getUnsignedByte() {
        if(canReadBytesAhead(1)) {
            short total = 0;

            total |= (fetchRawByte() & 0x00FF);
            return (short) (total & 0xFFFF);
        }
        throw new BufferUnderflowException();
    }

    /** @return an unsigned short from the current pointer position in the network buffer. */
    public int getUnsignedShort() {
        if(canReadBytesAhead(2)) {
            int total = 0;

            total += (((int) fetchRawByte()) << 8) & 0xFF00;
            total += ((int) fetchRawByte()) & 0x00FF;

            return total & 0xFFFF;
        }
        throw new BufferUnderflowException();
    }

    /** @return an unsigned int from the current pointer position in the network buffer. */
    public long getUnsignedInt() {
        if(canReadBytesAhead(4)) {
            long total = 0;

            total += (((long) fetchRawByte()) << 24) & 0xFF000000;
            total += (((long) fetchRawByte()) << 16) & 0x00FF0000;
            total += (((long) fetchRawByte()) << 8 ) & 0x0000FF00;
            total +=  ((long) fetchRawByte())        & 0x000000FF;

            return total & 0xFFFFFFFF;
        }
        throw new BufferUnderflowException();
    }

    /** @return an int from the current pointer position in the network buffer. */
    public int getInt() {
        if(canReadBytesAhead(4)) {
            int number = 0x00000000;

            number |= fetchRawByte() << 24;
            number |= fetchRawByte() << 16;
            number |= fetchRawByte() << 8;
            number |= fetchRawByte();

            return number;
        }
        throw new BufferUnderflowException();
    }

    /** @return a UTF8 formatted string (<256 bytes length) from the current pointer position in the network buffer. */
    public String getSmallUTF8String() {
        if(canReadBytesAhead(1)) {
            short length = getUnsignedByte();

            if(canReadBytesAhead(length)) {
                byte[] strBytes = fetchRawBytes(length);
                return new String(strBytes, StandardCharsets.UTF_8);
            }
        }

        throw new BufferUnderflowException();
    }

    public String getUTF8String() {
        if(canReadBytesAhead(2)) {
            int length = getUnsignedShort();

            if(canReadBytesAhead(length)) {
                byte[] strBytes = fetchRawBytes(length);
                return new String(strBytes, StandardCharsets.UTF_8);
            }
        }

        throw new BufferUnderflowException();
    }

    /** @return a Vector2 from the current pointer position in the network buffer. **/
    public Vector2 getVector2() {
        double xIn = this.getInt();
        double zIn = this.getInt();
        return new Vector2(xIn / VECTOR2_ACCURACY, zIn / VECTOR2_ACCURACY);
    }




    /** Sets bytes to the buffer without converting it.*/
    public boolean put(byte... b) {
        if(canReadBytesAhead(b.length)) {
            for (byte value : b) writeByte(value);
            return true;
        }
        return false;
    }

    public boolean putBoolean(boolean bool) {
        if(canReadBytesAhead(1)) {
            writeByte((byte) (bool ? 0x01 : 0x00));
            return true;
        }
        return false;
    }

    public boolean putUnsignedByte(int value) {
        if(value > 255) throw new IllegalArgumentException("Provided an 'unsigned byte' with a value greater than 255");
        if(value < 0) throw new IllegalArgumentException("Provided an 'unsigned byte' with a value less than 0");

        if(canReadBytesAhead(1)) {
            byte rawByte = 0x00;
            int degradedValue = value;

            for (byte i = 7; i >= 0; i--) {
                int valCheck = (int) Math.pow(2, i);

                if ((degradedValue - valCheck) >= 0) {
                    degradedValue -= valCheck;
                    rawByte |= 1 << i;
                }
            }

            writeByte(rawByte);
            return true;
        }

        return false;
    }

    public boolean putUnsignedShort(int value) {
        if(value >= Math.pow(2, 16)) throw new IllegalArgumentException("Provided an 'unsigned short' with a value greater than 2^16");
        if(value < 0) throw new IllegalArgumentException("Provided an 'unsigned short' with a value less than 0");

        if(canReadBytesAhead(2)) {
            short rawShort = 0x0000;
            int degradedValue = value;

            for (byte i = 15; i >= 0; i--) {
                double valCheck = Math.pow(2, i);

                if ((degradedValue - valCheck) >= 0) {
                    degradedValue -= valCheck;
                    rawShort |= 1 << i;
                }
            }

            byte bUpper = (byte) ((rawShort & 0xFF00) >> 8);
            byte bLower = (byte) (rawShort & 0x00FF);

            writeByte(bUpper);
            writeByte(bLower);
            return true;
        }

        return false;
    }

    public boolean putUnsignedInt(long value) {
        if(value >= Math.pow(2, 32)) throw new IllegalArgumentException("Provided an 'unsigned int' with a value greater than 2^32");
        if(value < 0) throw new IllegalArgumentException("Provided an 'unsigned int' with a value less than 0");

        if(canReadBytesAhead(4)) {
            int rawShort = 0x00000000;
            long degradedValue = value;

            for (byte i = 31; i >= 0; i--) {
                double valCheck = Math.pow(2, i);

                if ((degradedValue - valCheck) >= 0) {
                    degradedValue -= valCheck;
                    rawShort |= 1 << i;
                }
            }

            byte bUpper =    (byte) ((rawShort & 0xFF000000) >> 24);
            byte bUpperMid = (byte) ((rawShort & 0x00FF0000) >> 16);
            byte bLowerMid = (byte) ((rawShort & 0x0000FF00) >> 8 );
            byte bLower =    (byte)  (rawShort & 0x000000FF);

            writeByte(bUpper);
            writeByte(bUpperMid);
            writeByte(bLowerMid);
            writeByte(bLower);
            return true;
        }

        return false;
    }


    public int putInt(int value) {
        if(canReadBytesAhead(4)) {
            writeByte( (byte) ((value & 0xFF000000) >> 24) );
            writeByte( (byte) ((value & 0x00FF0000) >> 16) );
            writeByte( (byte) ((value & 0x0000FF00) >> 8 ) );
            writeByte( (byte)  (value & 0x000000FF)        );
            return INT_BYTE_COUNT;
        }
        return 0;
    }

    /** @return the amount of bytes written. */
    public int putUTF8String(String string) {
        if(string == null || string.length() == 0) return 0;

        byte[] strBytes = string.getBytes(StandardCharsets.UTF_8);
        if(strBytes.length >= MAX_UNSIGNED_SHORT_VALUE) throw new IllegalArgumentException("String exceeds the limit of "+MAX_UNSIGNED_SHORT_VALUE+" bytes.");

        // Check if both the length of bytes + the length short can be included.
        if(canReadBytesAhead(2 + strBytes.length)) {
            if(putUnsignedShort(strBytes.length)) {
                writeBytes(strBytes);
                return SHORT_BYTE_COUNT + strBytes.length;
            }
        }

        return 0;
    }

    /** @return the amount of bytes written. */
    public int putSmallUTF8String(String string) {
        if(string == null || string.length() == 0) return 0;

        byte[] strBytes = string.getBytes(StandardCharsets.UTF_8);
        if(strBytes.length >= MAX_UNSIGNED_BYTE_VALUE) throw new IllegalArgumentException("String exceeds the limit of "+MAX_UNSIGNED_BYTE_VALUE+" bytes.");

        // Check if both the length of bytes + the length short can be included.
        if(canReadBytesAhead(1 + strBytes.length)) {
            if(putUnsignedByte(strBytes.length)) {
                writeBytes(strBytes);
                return 1 + strBytes.length;
            }
        }
        return 0;
    }

    public int putVector2(Vector2 vector) {
        double lX = vector.getX() * VECTOR2_ACCURACY;
        double lZ = vector.getZ() * VECTOR2_ACCURACY;

        this.putInt((int) Math.floor(lX));
        this.putInt((int) Math.floor(lZ));

        return VECTOR2_BYTE_COUNT;
    }


}
