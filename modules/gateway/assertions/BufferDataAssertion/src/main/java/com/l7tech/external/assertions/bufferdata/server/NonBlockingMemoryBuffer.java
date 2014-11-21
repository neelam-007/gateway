package com.l7tech.external.assertions.bufferdata.server;

import com.l7tech.util.Pair;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.TimeSource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents an in-memory buffer that is order preserving but does not block due to synchronization except while
 * the buffer is being extracted.
 * <p/>
 * References are retained to the stored byte arrays, in a queue.
 * <p/>
 * Users must not retain their own references to the byte arrays once they are donated to the buffer.
 */
public class NonBlockingMemoryBuffer implements Buffer {
    static TimeSource timeSource = new TimeSource();

    // Append can be done with only read lock, but extract requires write lock.
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConcurrentLinkedQueue<byte[]> queue = new ConcurrentLinkedQueue<>();
    private final AtomicLong bufferSize = new AtomicLong();
    private final AtomicLong lastUsedTime = new AtomicLong();
    private final String name;
    private long lastExtractTime;

    public NonBlockingMemoryBuffer( String name ) {
        this.name = name;
        long now = timeSource.currentTimeMillis();
        this.lastUsedTime.set( now );
        this.lastExtractTime = now;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public Pair<BufferStatus,byte[]> appendAndMaybeExtract( @NotNull byte[] dataToAppend, long maxSizeBytes, long maxAgeMillis ) {
        if ( dataToAppend.length > maxSizeBytes ) {
            throw new IllegalArgumentException( "Data chunk size exceeds maximum buffer size" );
        }

        long now = timeSource.currentTimeMillis();
        lastUsedTime.set( now );
        int recSize = dataToAppend.length;
        long age;
        boolean wasFull;
        boolean wasTooOld;

        try {
            lock.readLock().lock();

            age = now - lastExtractTime;
            wasFull = bufferSize.get() + dataToAppend.length > maxSizeBytes;
            wasTooOld = age > maxAgeMillis;

            if ( !wasFull && !wasTooOld ) {
                // We can deal with this here and now
                final long newBufSize;
                if ( recSize > 0 ) {
                    queue.offer( dataToAppend );
                    newBufSize = bufferSize.addAndGet( recSize );
                } else {
                    newBufSize = bufferSize.get();
                }
                return new Pair<>( new BufferStatus( newBufSize, age, false, false ), null );
            }

        } finally {
            lock.readLock().unlock();
        }

        // Extract required.  Get write lock, then check again.
        // We will still need to append after the extract
        byte[] extracted = null;
        boolean wasExtracted = false;
        PoolByteArrayOutputStream os = null;

        try {
            lock.writeLock().lock();

            age = now - lastExtractTime;
            wasFull = bufferSize.get() + dataToAppend.length > maxSizeBytes;
            wasTooOld = age > maxAgeMillis;

            if ( wasFull || wasTooOld ) {
                os = new PoolByteArrayOutputStream();

                byte[] b;
                while ( null != ( b = queue.poll() ) ) {
                    try {
                        os.write( b );
                    } catch ( IOException e ) {
                        throw new RuntimeException( e ); // can't happen
                    }
                }

                bufferSize.set( 0 );
                lastExtractTime = timeSource.currentTimeMillis();
                extracted = os.toByteArray();
                age = 0;
                wasExtracted = true;
            }

            final long newBufSize;
            if ( recSize > 0 ) {
                queue.offer( dataToAppend );
                newBufSize = bufferSize.addAndGet( recSize );
            } else {
                newBufSize = bufferSize.get();
            }

            return new Pair<>( new BufferStatus( newBufSize, age, wasFull, wasExtracted ), extracted );

        } finally {
            lock.writeLock().unlock();
            ResourceUtils.closeQuietly( os );
        }
    }

    @Override
    public void discard() {
        queue.clear();
        bufferSize.set( 0 );
    }

    @Override
    public boolean discardIfLastUsedBefore( long minLastUsedTime ) {
        boolean ret = false;

        try {
            lock.writeLock().lock();

            if ( lastUsedTime.get() < minLastUsedTime ) {
                discard();
                ret = true;
            }

        } finally {
            lock.writeLock().unlock();
        }

        return ret;
    }
}
