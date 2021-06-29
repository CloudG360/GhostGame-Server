package net.cg360.spookums.server.util;

import net.cg360.spookums.server.Server;

public class NetworkBuffer {

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


    /** Unsafe way to fetch a byte. Make sure to check first :)*/
    protected byte fetchRawByte() {
        byte b = buffer[pointerIndex];
        pointerIndex++;
        return b;
    }

    /** Unsafe way to write a byte. Make sure to check first :)*/
    protected void writeByte(byte b) {
        buffer[pointerIndex] = b;
        pointerIndex++;
    }



    public boolean putUnsignedByte(int value) {
        if(value > 255) throw new IllegalArgumentException("Provided an 'unsigned byte' with a value greater than 255");
        if(value < 0) throw new IllegalArgumentException("Provided an 'unsigned byte' with a value less than 0");

        if(canReadBytesAhead(1)) {
            byte rawByte = 0x00;
            int degradedValue = value;

            for (byte i = 7; i >= 0; i--) {
                double valCheck = Math.pow(2, i);

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

}
