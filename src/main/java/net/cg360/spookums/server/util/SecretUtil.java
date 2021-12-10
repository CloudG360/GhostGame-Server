package net.cg360.spookums.server.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class SecretUtil {

    // Source: https://qvault.io/cryptography/how-sha-2-works-step-by-step-sha-256/
    public static final int[] HASH_CONSTANTS = {
            0x6a09e667,
            0xbb67ae85,
            0x3c6ef372,
            0xa54ff53a,
            0x510e527f,
            0x9b05688c,
            0x1f83d9ab,
            0x5be0cd19
    };

    public static final int[] ROUND_CONSTANTS = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    public static SaltyHash createSHA256Hash(String plain) {
        return createSHA256Hash(plain, null);
    }

    // SHA256 breaks the plaintext into 512-bit blocks
    // This is not a perfect copy of SHA256 :)
    // It's the same algorithm but some values differ due to bit order.
    public static SaltyHash createSHA256Hash(String plain, byte[] saltIn) {
        int[] hashNums = copyArray(HASH_CONSTANTS);
        int[] roundNums = copyArray(ROUND_CONSTANTS);

        byte[] data = plain.getBytes(StandardCharsets.UTF_8);
        byte[] salt = (saltIn == null) ? generateSalt(16) : saltIn;

        if(salt.length > 0) {
            byte[] newData = new byte[data.length + salt.length];

            for (int i = 0; i < data.length; i++) newData[i] = data[i];
            for (int i = 0; i < salt.length; i++) newData[data.length + i] = salt[i];
            data = newData;
        }

        byte[][] buffer = get512ChunkBuffer(data);

        // Iterate the following process through each chunk.
        // Iterate the following process through each chunk.
        for(int chunkI = 0; chunkI < buffer.length; chunkI++) {
            byte[] chunk = buffer[chunkI];
            int[] word32arr = new int[16 + 48]; // 16 for chunk data, 48 for empty

            // Reassemble the bytes into 32bit words
            for(int i = 0; i < chunk.length; i++) {
                int wordI = i % 4;
                int subShiftIndex = 4 - (i + 1);
                word32arr[wordI] |= (chunk[i] << subShiftIndex);
            }

            // The array should be initialized with 0's but I want to make
            // sure. This fills the last 48 words with 0's
            // Each of these empty words are then modified.
            for(int i = 16; i < 48; i++) {
                word32arr[i] = 0;

                // And here is the modification!
                int s0 =  Integer.rotateRight(word32arr[i - 15], 7)
                        ^ Integer.rotateRight(word32arr[i - 15], 18)
                        ^ (word32arr[i - 15] >> 3);

                int s1 =  Integer.rotateRight(word32arr[i - 2], 17)
                        ^ Integer.rotateRight(word32arr[i - 2], 19)
                        ^ (word32arr[i - 2] >> 10);

                word32arr[i] = addUnsigned(word32arr[i - 16], s0, word32arr[i - 7], s1);
            }

            ////// "compression"
            int[] work = copyArray(hashNums);

            for(int i = 0; i < 64; i++) {
                int s1 = Integer.rotateRight(work[4], 6) ^ Integer.rotateRight(work[4], 11) ^ Integer.rotateRight(work[4], 25);
                int ch = (work[4] & work[5]) ^ ((~work[4]) & work[6]);
                int tmp1 = addUnsigned(work[7], s1, ch, roundNums[i], word32arr[i]);

                int s0 = Integer.rotateRight(work[0], 2) ^ Integer.rotateRight(work[0], 13) ^ Integer.rotateRight(work[0], 22);
                int maj = (work[0] & work[1]) ^ (work[0] & work[2]) ^ (work[1] & work[2]);
                int tmp2 = addUnsigned(s0, maj);

                work[7] = work[6];
                work[6] = work[5];
                work[5] = work[4];
                work[4] = addUnsigned(work[3], tmp1);
                work[3] = work[2];
                work[2] = work[1];
                work[1] = work[0];
                work[0] = addUnsigned(tmp1, tmp2);
            }

            // Replacing the hashNums!
            for(int i = 0; i < hashNums.length; i++) {
                hashNums[i] = addUnsigned(hashNums[i], work[i]);
            }
        }


        StringBuilder hash = new StringBuilder();
        for(int i = 0; i < hashNums.length; i++) {
            String hexRep = Integer.toHexString(hashNums[i]);
            hash.append(hexRep);
        }

        return new SaltyHash(hash.toString(), salt);
    }

    protected static byte[][] get512ChunkBuffer(byte[] data) {
        // length + a byte that stars with 1 for spacing + 64-bit word for length
        int lenDat = data.length + 1 + 8;
        int fillByteCount = 64 - (lenDat % 64);

        byte[] buffer = new byte[lenDat + fillByteCount];
        int offset = data.length;

        ////// Add original length | Little endian. Copy data + append with 10000000
        for(int d = 0; d < data.length; d++) buffer[d] = data[d];
        buffer[offset] = (byte) (0b10000000 & 0xFF);
        offset++;

        ////// Fill in with 0's to complete the padding.
        for(int f = 0; f < fillByteCount; f++) buffer[offset + f] = 0x00;
        offset += fillByteCount;

        ////// Add original length | Little endian.
        // Java counts array indexes with int, so only 32 bits are useful.
        // Thus here, I'm adding 4 empty bytes to fit the 64-bit size.
        for(int i = 0; i < 4; i++) buffer[offset + i] = 0x00;
        offset += 4;

        // Now I'm adding the actual length!
        int originalLength = data.length;
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(originalLength);
        bb.clear();
        for(int i = 0; i < 4; i++) {
            buffer[offset + i] = bb.get();
        }
        offset += 4;

        // Extract the padded data into blocks.
        byte[][] chunks = new byte[Math.floorDiv(buffer.length, 64)][64];
        for(int i = 0; i < buffer.length; i++) {
            int blockI = Math.floorDiv(i, 64);
            chunks[blockI][i] = buffer[i];
        }

        return chunks;
    }

    public static byte[] generateSalt(int len) {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[len];
        random.nextBytes(randomBytes);
        return randomBytes;
    }



    public static int[] copyArray(int[] sourceArray) {
        int[] target = new int[sourceArray.length];
        for(int i = 0; i < sourceArray.length; i++) target[i] = sourceArray[i];

        return target;
    }

    public static int addUnsigned(int... numbers) {
        long value = 0;
        for (int i = 0; i < numbers.length; i++) {
            value += Integer.toUnsignedLong(numbers[i]);
        }

        return (int) value;
    }



    public static final class SaltyHash {

        private String hash;
        private byte[] salt;

        public SaltyHash(String hash, byte[] salt) {
            this.hash = hash;
            this.salt = salt;
        }

        public String getHash() {
            return hash;
        }

        public byte[] getSalt() {
            return salt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SaltyHash saltyHash = (SaltyHash) o;
            return Objects.equals(hash, saltyHash.hash) && Arrays.equals(salt, saltyHash.salt);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(hash);
            result = 31 * result + Arrays.hashCode(salt);
            return result;
        }
    }
}
