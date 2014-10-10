package com.l7tech.external.assertions.bufferdata.server;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages in-memory buffers.
 * TODO a background thread to discard buffers that haven't been used in a long time (like a day)
 */
public class MemoryBufferManager {

    static ConcurrentMap<String, Buffer> bufferMap = new ConcurrentHashMap<>();

    @NotNull
    public static Buffer getOrCreateBuffer( @NotNull String key ) {
        Buffer b = bufferMap.get( key );
        if ( b == null ) {
            b = new NonBlockingMemoryBuffer();
            Buffer existing = bufferMap.putIfAbsent( key, b );
            if ( existing != null)
                b = existing;
        }
        return b;
    }

    public static void discardAllBuffers() {
        for ( Buffer buffer : bufferMap.values() ) {
            buffer.discard();
        }
    }

    public static void onModuleUnloaded() {
        discardAllBuffers();
    }
}
