package com.l7tech.external.assertions.bufferdata.server;

import com.l7tech.util.Pair;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.TimeSource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Represents an in-memory buffer that preserves insertion order strictly, but may block due to synchronization.
 */
public class OrderedMemoryBuffer implements Buffer {
    static TimeSource timeSource = new TimeSource();

    private final Object lock = new Object();
    private PoolByteArrayOutputStream buffer = new PoolByteArrayOutputStream();
    private long lastExtractTime;

    public OrderedMemoryBuffer() {
        this.lastExtractTime = timeSource.currentTimeMillis();
    }

    @NotNull
    public Pair<BufferStatus,byte[]> appendAndMaybeExtract( @NotNull byte[] dataToAppend, long maxSizeBytes, long maxAgeMillis ) {
        if ( dataToAppend.length > maxSizeBytes ) {
            throw new IllegalArgumentException( "Data chunk size exceeds maximum buffer size" );
        }

        synchronized ( lock ) {
            long now = timeSource.currentTimeMillis();
            long size = buffer.size();
            long age = now - lastExtractTime;
            byte[] extracted = null;

            boolean wasFull = size + dataToAppend.length > maxSizeBytes;
            boolean wasExtracted = false;
            if ( age > maxAgeMillis || wasFull ) {
                extracted = buffer.toByteArray();
                buffer.reset();
                lastExtractTime = timeSource.currentTimeMillis();
                age = 0;
                wasExtracted = true;
            }

            try {
                buffer.write( dataToAppend );
            } catch ( IOException e ) {
                // Can't happen
                throw new RuntimeException( e );
            }

            return new Pair<>( new BufferStatus( buffer.size(), age, wasFull, wasExtracted ), extracted );
        }
    }

    @Override
    public void discard() {
        buffer.close();
    }
}
