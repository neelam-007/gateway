/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.io.InputStream;
import java.io.IOException;

/**
 * Utility for hex encoding.
 * User: mike
 * Date: Jul 15, 2003
 * Time: 2:31:32 PM
 */
public class HexUtils {
    private HexUtils() {}

    /**
     * Encodes the 128 bit (16 bytes) MD5 into a 32 character String.
     *
     * @param binaryData Array containing the digest
     * @return A String containing the encoded MD5, or empty string if encoding failed
     */
    public static String encodeMd5Digest(byte[] binaryData) {
        if (binaryData == null) return "";

        char[] hexadecimal ={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        if (binaryData.length != 16) return "";

        char[] buffer = new char[32];

        for (int i = 0; i < 16; i++) {
            int low = (binaryData[i] & 0x0f);
            int high = ((binaryData[i] & 0xf0) >> 4);
            buffer[i*2] = hexadecimal[high];
            buffer[i*2 + 1] = hexadecimal[low];
        }
        return new String(buffer);
    }

    /**
     * Slurp a stream into a byte array and return it.
     * @param stream
     * @param maxSize maximum size to read
     * @return a byte array.
     */
    public static byte[] slurpStream(InputStream stream, int maxSize) throws IOException {
        byte[] bb = new byte[maxSize];
        int remaining = maxSize;
        int offset = 0;
        for (;;) {
            int n = stream.read(bb, offset, remaining);
            offset += n;
            remaining -= n;
            if (n < 1 || remaining < 1) {
                byte[] ret = new byte[maxSize - remaining + 1];
                System.arraycopy(bb, 0, ret, 0, offset + 1);
                return ret;
            }
        }
        /* NOTREACHED */
    }
}
