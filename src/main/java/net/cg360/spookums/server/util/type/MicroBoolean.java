package net.cg360.spookums.server.util.type;

import net.cg360.spookums.server.util.clean.Check;

/**
 * A utility class which allows for booleans to be compressed
 * in a way which uses a byte to store them in a more compact
 * manner.
 */
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


    /**
     * Sets a value within a boolean
     * @param index the bit in the storage byte to modify (0 to 7 inclusive)
     * @param value the value of the boolean
     */
    public MicroBoolean setValue(int index, boolean value) {
        Check.inclusiveBounds(index, 0, 7, "index");

        // Sets the bits differently based on if they're true or false.
        // If true, use an OR bitwise operator to set the position to 1.
        // If false, use an AND bitwise operator + the compliment of the mask to set
        // the position to 0.
        if(value) this.values |= ((0b00000001 << index) & 0xFF);
        else      this.values &= ((~(0b00000001 << index)) & 0xFF);

        return this;
    }

    /**
     * @param index the bit in the storage byte to fetch (0 to 7 inclusive)
     * @return the value at the index
     */
    public boolean getValue(int index) {
        Check.inclusiveBounds(index, 0, 7, "index");
        byte mask = (byte) ((0b00000001 << index) & 0xFF);

        return (values & mask) != 0x00; // Return if bit under mask is NOT 0, meaning the bool is false.
    }

    public byte getStorageByte() {
        return values;
    }


    public boolean isEmpty() {
        return this.values == 0x00;
    }

    // Below are a few static methods that make creation look a bit nicer :D

    public static MicroBoolean empty() { return new MicroBoolean((byte) 0x00); }
    public static MicroBoolean from(byte source) { return new MicroBoolean(source); }
    public static MicroBoolean of(boolean... bools) { return new MicroBoolean(bools); }
}
