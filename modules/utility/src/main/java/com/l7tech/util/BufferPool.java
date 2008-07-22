/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import java.util.*;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * Class that provides access to a pool of not-zeroed-on-use byte arrays to use as read buffers.
 * Each thread has its own pool of small buffers, but buffers larger than 64k are shared process wide
 * (and involve some synchronization).
 * <p/>
 * <b>Note</b>: Since buffers are not prezeroed they might contain sensitive data from previous unrelated computations.
 * Buffers obtained from this class, even partially overwritten ones, should never be saved, logged, or serialized over RMI.
 *
 * @noinspection unchecked
 */
public class BufferPool {
    public static final int MIN_BUFFER_SIZE = 1024;

    // Don't accumulate an infinite number of buffers in the pool
    private static int[] MAX_BUFFERS_PER_LIST = { 20, 20, 10, 4, 20, 15, 10, 5 };
    private static int MAX_HUGE_BUFFERS = 4;

    private int[] sizes = { 1024, 4096, 16384, 65536, 128 * 1024, 256 * 1024, 512 * 1024, 1024 * 1024 };

    private final LinkedList p1k = new LinkedList();
    private final LinkedList p4k = new LinkedList();
    private final LinkedList p16k = new LinkedList();
    private final LinkedList p64k = new LinkedList();

    /** Index above which the buffer pools are no longer thread-local. */
    private static final int LAST_LOCAL_INDEX = 3;

    private static final LinkedList p128k = new LinkedList();
    private static final LinkedList p256k = new LinkedList();
    private static final LinkedList p512k = new LinkedList();
    private static final LinkedList p1024k = new LinkedList();
    private static final LinkedList pHuge = new LinkedList();

    private final LinkedList[] buffers = new LinkedList[] {
            p1k,  // list of buffers that are 1k
            p4k,  // list of buffers that are 4k
            p16k, // etcetera
            p64k,
            p128k, // no longer thread-local from here down
            p256k,
            p512k,
            p1024k,
    };

    private BufferPool() {        
    }

    private static ThreadLocal localPool = new ThreadLocal() {
        protected Object initialValue() {
            return new BufferPool();
        }
    };

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
        if (minSize > 1024 * 1024) return getHugeBuffer(minSize);
        BufferPool that = (BufferPool)localPool.get();
        return that.doGetBuffer(minSize);
    }

    private byte[] doGetBuffer(int minSize) {
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            if (size >= minSize) {
                LinkedList buffers = this.buffers[i];
                if (i <= LAST_LOCAL_INDEX) {
                    // Thread-local -- no synch needed
                    return getBufferFromList(buffers, size, minSize);
                }
                // Shared -- synch needed
                synchronized (buffers) {
                    return getBufferFromList(buffers, size, minSize);
                }
            }
        }
        // Normally not possible to reach this.  If we do, must have asked for huge buffer.
        assert minSize > 1024 * 1024;
        return getHugeBuffer(minSize);
    }

    private byte[] getBufferFromList(LinkedList buffers, int size, int minSize) {
        while (!buffers.isEmpty()) {
            final SoftReference bufRef = (SoftReference)buffers.removeFirst();
            final byte[] buf = (byte[])bufRef.get();
            if (buf != null) {
                assert buf.length >= minSize;
                return buf;
            }
        }
        return new byte[size];
    }

    /**
     * Return a no-longer-needed buffer to the pool.  The buffer need not have been allocated using this class
     * originally, as long as it is larger than {@link #MIN_BUFFER_SIZE},
     * although allocating all buffers through this class will help ensure that the buffer sizes stay reasonable.
     * <p/>
     * Buffers must be returned only on threads that will be calling getBuffer() some time in the future.
     * Specifically, buffers <b>must not</b> be returned on the Finalizer thread since small buffers will
     * just accumulate there forever.
     *
     * @param buffer the buffer to return to the pool. No action is taken if this is null or smaller than {@link #MIN_BUFFER_SIZE}.
     */
    public static void returnBuffer(byte[] buffer) {
        if (buffer == null || buffer.length < MIN_BUFFER_SIZE) return;
        assert !"Finalizer".equals(Thread.currentThread().getName());
        if (buffer.length > 1024 * 1024) {
            returnHugeBuffer(buffer);
            return;
        }
        BufferPool that = (BufferPool)localPool.get();
        that.doReturnBuffer(buffer);
    }

    private void doReturnBuffer(byte[] buffer) {
        final int size = buffer.length;
        if (size > 1024 * 1024) {
            returnHugeBuffer(buffer);
            return;
        }
        for (int i = sizes.length - 1; i >= 0; --i) {
            if (size >= sizes[i]) {
                LinkedList buffers = this.buffers[i];
                if (i <= LAST_LOCAL_INDEX) {
                    // Thread local -- no synch needed
                    if (buffers.size() < MAX_BUFFERS_PER_LIST[i])
                        buffers.add(new SoftReference(buffer));
                    return;
                }
                // Shared -- synch needed
                synchronized (buffers) {
                    if (buffers.size() < MAX_BUFFERS_PER_LIST[i])
                        buffers.add(new SoftReference(buffer));
                    return;
                }
            }
        }
        /* NOTREACHED */
    }

    /**
     * Get a buffer that is bigger than 1mb.
     *
     * @param size the size of the huge buffer to get.  Must be greater than 1024 * 1024.
     * @return a buffer of at least the specified size.  Never null.
     */
    private static byte[] getHugeBuffer(int size) {
        if (size < 1024 * 1024) throw new IllegalArgumentException("size must be greater than 1mb");
        synchronized (pHuge) {
            for (ListIterator i = pHuge.listIterator(); i.hasNext();) {
                WeakReference ref = (WeakReference)i.next();
                byte[] buff = (byte[])ref.get();
                if (buff == null) {
                    i.remove();
                } else if (buff.length >= size) {
                    i.remove();
                    return buff;
                }
            }
        }
        return new byte[size];
    }

    /**
     * Return a buffer that is bigger than 1mb.
     *
     * @param buffer the buffer to return.  Must not be null.  Must be larger than 1mb.
     */
    private static void returnHugeBuffer(byte[] buffer) {
        final int size = buffer.length;
        if (size < 1024 * 1024) throw new IllegalArgumentException("size must be greater than 1mb");
        synchronized (pHuge) {
            while (pHuge.size() >= MAX_HUGE_BUFFERS)
                pHuge.removeFirst();
            pHuge.add(new WeakReference(buffer));
        }
    }
}
