package net.cg360.spookums.server.util;

import net.cg360.spookums.server.Server;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

public class NetworkBuffer {

    public static int MAX_UNSIGNED_SHORT_VALUE = (int) (Math.pow(2, 16) - 1);
    public static short MAX_UNSIGNED_BYTE_VALUE = (short) (Math.pow(2, 8) - 1);

    protected byte[] buffer;
    protected int pointerIndex;

    protected NetworkBuffer(byte[] bytes) {
        this.buffer = bytes;
        this.pointerIndex = 0;
    }

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
        pointerIndex++; // this was encased literally to debug it.
    }


    /** Unsafe way to fetch a byte. Make sure to check first :)*/
    protected byte fetchRawByte() {
        byte b = buffer[pointerIndex];
        incrementPointer();
        return b;
    }

    protected byte[] fetchRawBytes(int byteCount) {
        byte[] bytes = new byte[byteCount];

        for(int i = 0; i < byteCount; i++) {
            bytes[i] = buffer[pointerIndex];
            incrementPointer();
        }
        return bytes;
    }

    /** Unsafe way to write a byte. Make sure to check first :)*/
    protected void writeByte(byte b) {
        buffer[pointerIndex] = b;
        incrementPointer();

    }

    /** Unsafe way to write a series of bytes. Make sure to check first :)*/
    protected void writeBytes(byte[] bytes) {
        for(byte b : bytes) writeByte(b);
    }

    // Could be signed, unsigned, or even a string character.
    /** Fetches a byte from the buffer without converting it.*/
    public byte get() {
        if(canReadBytesAhead(1)) {
            return fetchRawByte();
        }
        throw new BufferUnderflowException();
    }

    /** Fetches a quantity of bytes from the buffer without converting them.*/
    public void get(byte[] target) {
        if(canReadBytesAhead(target.length)) {
            for(int i = 0; i < target.length; i++) target[i] = fetchRawByte();
            return;
        }
        throw new BufferUnderflowException();
    }

    public int getUnsignedShort() {
        if(canReadBytesAhead(2)) {
            int total = 0;

            total += (((int) fetchRawByte()) << 8) & 0xFF00;
            total += ((int) fetchRawByte()) & 0x00FF;

            return total & 0xFFFF;
        }
        throw new BufferUnderflowException();
    }

    public String getUnboundUTF8String(int byteCount) {
        if(canReadBytesAhead(byteCount)) {
           byte[] strBytes = fetchRawBytes(byteCount);
           return new String(strBytes, StandardCharsets.UTF_8);
        }
        throw new BufferUnderflowException();
    }




    /** Sets bytes to the buffer without converting it.*/
    public boolean put(byte... b) {
        if(canReadBytesAhead(b.length)) {
            for (byte value : b) writeByte(value);
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

    /** @return the amount of bytes written. */
    public int putUTF8String(String string) {
        if(string.length() == 0) return 0;

        byte[] strBytes = string.getBytes(StandardCharsets.UTF_8);
        if(strBytes.length <= MAX_UNSIGNED_SHORT_VALUE) throw new IllegalArgumentException("String exceeds the limit of "+MAX_UNSIGNED_SHORT_VALUE+" bytes.");

        // Check if both the length of bytes + the length short can be included.
        if(canReadBytesAhead(2 + strBytes.length)) {
            if(putUnsignedShort(strBytes.length)) {
                writeBytes(strBytes);
                return 2 + strBytes.length;
            }
        }

        return 0;
    }

    /** @return the amount of bytes written. */
    public int putSmallUTF8String(String string) {
        if(string.length() == 0) return 0;

        byte[] strBytes = string.getBytes(StandardCharsets.UTF_8);
        if(strBytes.length <= MAX_UNSIGNED_BYTE_VALUE) throw new IllegalArgumentException("String exceeds the limit of "+MAX_UNSIGNED_BYTE_VALUE+" bytes.");

        // Check if both the length of bytes + the length short can be included.
        if(canReadBytesAhead(1 + strBytes.length)) {
            if(putUnsignedByte(strBytes.length)) {
                writeBytes(strBytes);
                return 1 + strBytes.length;
            }
        }
        return 0;
    }

    /** A string is added without length marking bytes at the start. */
    public int putUnboundUTF8String(String string) {
        if(string.length() == 0) return 0;
        byte[] strBytes = string.getBytes(StandardCharsets.UTF_8);

        // Check if both the length of bytes + the length short can be included.
        if(canReadBytesAhead(strBytes.length)) {
            writeBytes(strBytes);
            return strBytes.length;
        }
        return 0;
    }

}
