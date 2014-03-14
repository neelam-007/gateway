/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class that provides access to a pool of not-zeroed-on-use byte arrays to use as read buffers.
 * <p/>
 * <b>Note</b>: Since buffers are not prezeroed they might contain sensitive data from previous unrelated computations.
 * Buffers obtained from this class, even partially overwritten ones, should never be saved, logged, or serialized over RMI.
 *
 * @noinspection unchecked
 */
public class BufferPool {
    public static final String PROP_ENABLE_BUFFER_POOL = "com.l7tech.util.BufferPool.enabled";
    public static final String PROP_ENABLE_HUGE_POOL = "com.l7tech.util.BufferPool.hugePool.enabled";

    static final boolean ENABLE_BUFFER_POOL = SyspropUtil.getBoolean( PROP_ENABLE_BUFFER_POOL, true );
    static final boolean ENABLE_HUGE_POOL = SyspropUtil.getBoolean( PROP_ENABLE_HUGE_POOL, true );

    static final int MIN_BUFFER_SIZE = 1024;
    static final int HUGE_THRESHOLD = 1024 * 1024;

    private static final int[] sizes = { 1024, 4096, 16384, 65536, 128 * 1024, 256 * 1024, 512 * 1024, 1024 * 1024};

    static class Pool {
        final int size;
        final Queue<Reference<byte[]>> queue = new ConcurrentLinkedQueue<Reference<byte[]>>();

        Pool(int size) {
            this.size = size;
        }

        byte[] getBuffer() {
            for (int i = 0; i < 32; ++i) {
                Reference<byte[]> ref = queue.poll();
                if (ref == null)
                    break;
                byte[] bytes = ref.get();
                if (bytes != null)
                    return bytes;
                // That ref was reclaimed by the GC; try the next one
            }
            return null;
        }

        byte[] getOrCreateBuffer() {
            byte[] ret = getBuffer();
            return ret != null ? ret : new byte[size];
        }

        boolean returnBuffer(byte[] offering) {
            return offering.length == size &&
                    queue.offer(new SoftReference<byte[]>(offering));
        }

        void clear() {
            queue.clear();
        }
    }

    static class HugePool {
        final Queue<Reference<byte[]>> queue = new ConcurrentLinkedQueue<Reference<byte[]>>();

        byte[] getBuffer(int wantSize) {
            assert wantSize >= HUGE_THRESHOLD;
            // Avoid returning a huge buffer that is more than double the requested size, to avoid tying up
            // enormous amounts of memory.
            int maxSize = wantSize > Integer.MAX_VALUE/2 ? wantSize : wantSize * 2;
            for (int i = 0; i < 32; ++i) {
                Reference<byte[]> ref = queue.poll();
                if (ref == null)
                    break;
                byte[] bytes = ref.get();
                if (bytes != null) {
                    final int len = bytes.length;
                    if (len >= wantSize && len <= maxSize)
                        return bytes;

                    // Discard the unneeded-at-this-time too-big huge buffer, so it doesn't hang around in the pool (SSG-8091)
                }
                // That ref was reclaimed by the GC; try the next one
            }
            return null;
        }

        byte[] getOrCreateBuffer(int wantSize) {
            byte[] ret = getBuffer(wantSize);
            return ret != null ? ret : new byte[wantSize];
        }

        boolean returnBuffer(byte[] offering) {
            return offering.length >= HUGE_THRESHOLD &&
                    queue.offer(new SoftReference<byte[]>(offering));
        }

        void clear() {
            queue.clear();
        }
    }

    private static final HugePool hugePool = new HugePool();
    private static final Pool[] pools;
    static {
        pools = new Pool[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            pools[i] = new Pool(size);
        }
    }

    private BufferPool() {
    }

    /**
     * Get a buffer that is at least the specified size.  If possible, this buffer will come from a pool
     * of already-used buffers, and may have leftover random garbage still inside it.  To get the
     * benefit of this mechanism, be sure to return unused buffers when you no longer need them.
     * <p/>
     * <b>Note</b>: Since buffers are not prezeroed they might contain sensitive data from previous unrelated computations.
     * Buffers obtained from this class, even partially overwritten ones, should never be saved, logged, or serialized over RMI.
     *
     * @param minSize  the minimum size of buffer to obtain.
     * @return a buffer that is at least the requested size, but that may not be new.  Never null.
     * @throws OutOfMemoryError if a new buffer can't be created
     */
    public static byte[] getBuffer(int minSize) {
        if (minSize > HUGE_THRESHOLD)
            return getHugeBuffer(minSize);
        for (Pool pool : pools) {
            if (pool.size >= minSize)
                return pool.getOrCreateBuffer();
        }
        // Normally not possible to reach this.  If we do, must have asked for huge buffer.
        assert minSize > HUGE_THRESHOLD;
        return getHugeBuffer(minSize);
    }

    /**
     * Return a no-longer-needed buffer to the pool.  The buffer need not have been allocated using this class
     * originally, as long as it is larger than {@link #getMinBufferSize()},
     * although allocating all buffers through this class will help ensure that the buffer sizes stay reasonable.
     *
     * @param buffer the buffer to return to the pool. No action is taken if this is null or smaller than {@link #getMinBufferSize()}.
     * @return true if the returned buffer was accepted by a buffer pool.  False if the buffer was discarded (too small or too big, or too many buffers of that size class already pooled)
     */
    public static boolean returnBuffer(byte[] buffer) {
        if ( !ENABLE_BUFFER_POOL )
            return false;
        if (buffer == null || buffer.length < MIN_BUFFER_SIZE)
            return false;
        final int size = buffer.length;
        if (size > HUGE_THRESHOLD) {
            return returnHugeBuffer(buffer);
        }
        for (int i = pools.length - 1; i >= 0; i--) {
            Pool pool = pools[i];
            if (pool.size <= size) {
                return pool.returnBuffer(buffer);
            }
        }
        // Normally not possible to reach this.  If we do, must have returned a tiny buffer.
        assert size < MIN_BUFFER_SIZE;
        return false;
    }

    /**
     * Get a buffer that is bigger than 1mb.
     *
     * @param size the size of the huge buffer to get.  Must be greater than 1024 * 1024.
     * @return a buffer of at least the specified size.  Never null.
     */
    private static byte[] getHugeBuffer(int size) {
        if (size < HUGE_THRESHOLD)
            throw new IllegalArgumentException("size must be greater than 1mb");
        return hugePool.getOrCreateBuffer(size);
    }

    /**
     * @return the size of the smallest buffer that will ever be accepted by {@link #returnBuffer}.
     */
    public static int getMinBufferSize() {
        return MIN_BUFFER_SIZE;
    }

    /**
     * Return a buffer that is bigger than 1mb.
     *
     * @param buffer the buffer to return.  Must not be null.  Must be larger than 1mb.
     * @return true if the buffer was accepted by the hugePool.  false if the buffer was discarded (too small or too big, or too many huge buffers already)
     */
    private static boolean returnHugeBuffer(byte[] buffer) {
        if ( !ENABLE_HUGE_POOL )
            return false;
        final int size = buffer.length;
        if (size < HUGE_THRESHOLD)
            throw new IllegalArgumentException("size must be greater than 1mb");
        return hugePool.returnBuffer(buffer);
    }

    static int getNumSizeClasses() {
        return sizes.length + 1;
    }

    static int getSizeClass(int wantSize) {
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            if (size >= wantSize)
                return i;
        }
        return sizes.length;
    }

    static int getSizeOfSizeClass(int sizeClass) {
        return sizeClass < sizes.length ? sizes[sizeClass] : Integer.MAX_VALUE;
    }

    // Used by test code to reset pools between tests
    static void clearAllPools() {
        hugePool.clear();
        for (Pool pool : pools)
            pool.clear();
    }
}
