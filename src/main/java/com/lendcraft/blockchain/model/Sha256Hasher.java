package com.lendcraft.blockchain.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Sha256Hasher {
    private static final int[] K_CONSTANTS = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };
    private static final int[] H_INITIAL = {
            0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
    };
    private static final int BLOCK_SIZE_BYTES = 64;
    private static final int PADDING_LIMIT_BYTES = 56;
    private static final int WORDS_IN_BLOCK = 16;

    public static String computeHash(String input) {
        byte[] data = input.getBytes(StandardCharsets.UTF_8);
        int[] h = H_INITIAL.clone();
        processBytes(data, h);
        return convertToHexString(h);
    }

    private static void processBytes(byte[] data, int[] h) {
        long fileLengthInBits = (long) data.length * 8;
        byte[] blockBuffer = new byte[BLOCK_SIZE_BYTES];
        int bytesProcessed = 0;

        while (bytesProcessed < data.length) {
            int remaining = data.length - bytesProcessed;
            if (remaining >= BLOCK_SIZE_BYTES) {
                System.arraycopy(data, bytesProcessed, blockBuffer, 0, BLOCK_SIZE_BYTES);
                processBlock(blockBuffer, h);
                bytesProcessed += BLOCK_SIZE_BYTES;
            } else {
                System.arraycopy(data, bytesProcessed, blockBuffer, 0, remaining);
                Arrays.fill(blockBuffer, remaining, BLOCK_SIZE_BYTES, (byte) 0);
                blockBuffer[remaining] = (byte) 0x80;

                if (remaining < PADDING_LIMIT_BYTES) {
                    addLengthToBlock(blockBuffer, fileLengthInBits);
                    processBlock(blockBuffer, h);
                } else {
                    processBlock(blockBuffer, h);
                    byte[] finalBuffer = new byte[BLOCK_SIZE_BYTES];
                    addLengthToBlock(finalBuffer, fileLengthInBits);
                    processBlock(finalBuffer, h);
                }
                break;
            }
        }
        if (bytesProcessed == data.length && data.length % BLOCK_SIZE_BYTES == 0) {
            Arrays.fill(blockBuffer, (byte) 0);
            blockBuffer[0] = (byte) 0x80;
            addLengthToBlock(blockBuffer, fileLengthInBits);
            processBlock(blockBuffer, h);
        }
    }

    private static void addLengthToBlock(byte[] block, long lengthInBits) {
        for (int i = 0; i < 8; i++) {
            block[PADDING_LIMIT_BYTES + i] = (byte) (lengthInBits >>> (56 - i * 8));
        }
    }

    private static void processBlock(byte[] block, int[] h) {
        int[] wBuffer = new int[BLOCK_SIZE_BYTES];
        for (int i = 0; i < WORDS_IN_BLOCK; i++) {
            int offset = i * 4;
            wBuffer[i] = ((block[offset] & 0xFF) << 24) |
                    ((block[offset + 1] & 0xFF) << 16) |
                    ((block[offset + 2] & 0xFF) << 8) |
                    (block[offset + 3] & 0xFF);
        }
        for (int j = WORDS_IN_BLOCK; j < BLOCK_SIZE_BYTES; j++) {
            int sigma0 = rotateRight(wBuffer[j - 15], 7) ^ rotateRight(wBuffer[j - 15], 18) ^ (wBuffer[j - 15] >>> 3);
            int sigma1 = rotateRight(wBuffer[j - 2], 17) ^ rotateRight(wBuffer[j - 2], 19) ^ (wBuffer[j - 2] >>> 10);
            wBuffer[j] = wBuffer[j - 16] + sigma0 + wBuffer[j - 7] + sigma1;
        }
        int a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], varH = h[7];
        for (int z = 0; z < BLOCK_SIZE_BYTES; z++) {
            int sigma1 = rotateRight(e, 6) ^ rotateRight(e, 11) ^ rotateRight(e, 25);
            int ch = (e & f) ^ (~e & g);
            int temp1 = varH + sigma1 + ch + K_CONSTANTS[z] + wBuffer[z];
            int sigma0 = rotateRight(a, 2) ^ rotateRight(a, 13) ^ rotateRight(a, 22);
            int maj = (a & b) ^ (a & c) ^ (b & c);
            int temp2 = sigma0 + maj;
            varH = g;
            g = f;
            f = e;
            e = d + temp1;
            d = c;
            c = b;
            b = a;
            a = temp1 + temp2;
        }
        h[0] += a;
        h[1] += b;
        h[2] += c;
        h[3] += d;
        h[4] += e;
        h[5] += f;
        h[6] += g;
        h[7] += varH;
    }
    private static int rotateRight(int value, int bits) {
        return (value >>> bits) | (value << (32 - bits));
    }
    private static String convertToHexString(int[] hashValues) {
        StringBuilder sb = new StringBuilder(64);
        for (int val : hashValues) {
            sb.append(String.format("%08x", val));
        }
        return sb.toString();
    }
}
