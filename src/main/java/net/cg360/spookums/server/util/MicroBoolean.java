package net.cg360.spookums.server.util;

import net.cg360.spookums.server.util.clean.Check;

public final class MicroBoolean {

    private byte values;

    public MicroBoolean(boolean... bools){
        Check.inclusiveBounds(bools.length, 0, 7, "bools.length");
        this.values = 0x00;

        for(int i = 0; (i < 8) && (i < bools.length); i++) {
            if(bools[i]) {
                this.values |= ((0b00000001 << i) & 0xFF);
            }
        }
    }

    private MicroBoolean(byte source) {
        this.values = source;
    }



    public void setValue(int index, boolean value) {
        Check.inclusiveBounds(index, 0, 7, "index");

        // Sets the bits differently based on if they're true or false.
        // If true, use an OR bitwise operator to set the position to 1.
        // If false, use an AND bitwise operator + the compliment of the mask to set
        // the position to 0.
        if(value) this.values |= ((0b00000001 << index) & 0xFF);
        else      this.values &= ((~(0b00000001 << index)) & 0xFF);
    }

    public boolean getValue(int index) {
        Check.inclusiveBounds(index, 0, 7, "index");
        byte mask = (byte) ((0b00000001 << index) & 0xFF);

        return (values & mask) != 0x00; // Return if bit under mask is NOT 0, meaning the bool is false.
    }

    public byte getStorageByte() {
        return values;
    }

    public static MicroBoolean empty() { return new MicroBoolean((byte) 0x00); }
    public static MicroBoolean from(byte source) { return new MicroBoolean(source); }
    public static MicroBoolean of(boolean... bools) { return new MicroBoolean(bools); }
}
