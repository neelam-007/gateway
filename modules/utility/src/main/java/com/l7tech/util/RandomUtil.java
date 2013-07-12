package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Provides pooled access to instances of the default SecureRandom.
 */
public class RandomUtil {
    private static final Queue<SecureRandom> cache = new ConcurrentLinkedQueue<SecureRandom>();

    private static @NotNull Functions.Nullary<SecureRandom> factory = new Functions.Nullary<SecureRandom>() {
        @Override
        public SecureRandom call() {
            return new SecureRandom();
        }
    };

    /**
     * Generate some random bytes using a default SecureRandom instance.
     *
     * @param bytes a byte array to fill with random bytes.  Required.
     * @see SecureRandom#nextBytes(byte[])
     */
    public static void nextBytes(@NotNull byte[] bytes) {
        SecureRandom sr = getSecureRandom();
        sr.nextBytes(bytes);
        returnSecureRandom(sr);
    }

    /**
     * Generate a random integer using a default SecureRandom instance.
     *
     * @return the next random int value, evenly distributed between among all 2^32 possible values
     * @see SecureRandom#nextInt()
     */
    public static int nextInt() {
        SecureRandom sr = getSecureRandom();
        try {
            return sr.nextInt();
        } finally {
            returnSecureRandom(sr);
        }
    }

    /**
     * Generate a random integer using a default SecureRandom instance.
     *
     * @param n non-inclusive upper bound on integer to be generated.  Must be positive.
     * @return the next random int value, evenly distributed between (0,n]
     * @see SecureRandom#nextInt(int)
     */
    public static int nextInt(int n) {
        SecureRandom sr = getSecureRandom();
        try {
            return sr.nextInt(n);
        } finally {
            returnSecureRandom(sr);
        }
    }

    /**
     * Generate a random long using a default SecureRandom instance.
     *
     * @return the next random long value, evenly distributed between among all 2^64 possible values
     * @see SecureRandom#nextLong()
     */
    public static long nextLong() {
        SecureRandom sr = getSecureRandom();
        try {
            return sr.nextLong();
        } finally {
            returnSecureRandom(sr);
        }
    }

    /**
     * Get a default SecureRandom instance, creating a new one if necessary.
     *
     * @return a SecureRandom instance.  Never null.
     */
    static @NotNull SecureRandom getSecureRandom() {
        SecureRandom sr = cache.poll();
        if (sr == null) sr = factory.call();
        if (sr == null) sr = new SecureRandom();
        return sr;
    }

    /**
     * Return a SecureRandom instance to the cache.
     *
     * @param sr a default SecureRandom instance that the caller no longer needs and will not be reusing.  Required.
     */
    static void returnSecureRandom(@NotNull SecureRandom sr) {
        cache.offer(sr);
    }

    /**
     * Discard all cached SecureRandom instances.
     */
    public static void clearCache() {
        cache.clear();
    }

    /**
     * Change the way the new default SecureRandom instances are created.  This also clears all cached
     * SecureRandom instances.
     *
     * @param secureRandomFactory a new factory to create a new SecureRandom instance.  Required.
     */
    public static void setFactory(@NotNull Functions.Nullary<SecureRandom> secureRandomFactory) {
        factory = secureRandomFactory;
        clearCache();
    }
}
