/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * This InputStream returns the same pseudo-random sequence when read, each time it is instantiated with a given seed.
 */
public class RandomInputStream extends InputStream {
    private final Random random;
    private final long size;

    private long count = 0;

    /**
     * Generate a pseudorandom stream that returns size bytes before EOF, generated from the random number seed seed.
     * @param seed  the seed for the pseudo-random number generator.
     * @param size  the number of bytes to return before signalling EOF.
     */
    public RandomInputStream(long seed, long size) {
        this.random = new Random(seed);
        this.size = size;
    }

    public int read() throws IOException {
        if (count++ >= size) return -1;
        return random.nextInt(256);
    }

    public int read(byte b[]) throws IOException {
        if (b == null) throw new NullPointerException();
        if (b.length == 0) return 0;

        long remaining = size - count;
        if (remaining < 1) return -1;

        if (b.length >= remaining) {
            byte[] rest = new byte[(int)(remaining)];
            random.nextBytes(rest);
            System.arraycopy(rest, 0, b, 0, (int)remaining);
            count += remaining;
            return (int)remaining;
        }

        random.nextBytes(b);
        count += b.length;
        return b.length;
    }

    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
               ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        long remaining = size - count;
        if (remaining < 1) return -1;

        byte[] block = new byte[len];
        random.nextBytes(block);

        if (len >= remaining) {
            System.arraycopy(block, 0, b, off, (int)remaining);
            count += remaining;
            return (int)remaining;
        }

        System.arraycopy(block, 0, b, off, len);
        count += len;
        return len;
    }
}

