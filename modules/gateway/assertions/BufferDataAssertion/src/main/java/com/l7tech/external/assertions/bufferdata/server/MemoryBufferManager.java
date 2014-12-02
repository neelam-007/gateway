package com.l7tech.external.assertions.bufferdata.server;

import com.l7tech.external.assertions.bufferdata.BufferDataAssertion;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.util.Background;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.SyspropUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages in-memory buffers.
 */
public class MemoryBufferManager {
    private static final Logger logger = Logger.getLogger( MemoryBufferManager.class.getName() );

    // Scan about every 7 minutes for old idle buffers
    private static long CLEANUP_DELAY = SyspropUtil.getLong( "com.l7tech.assertions.bufferdata.cleanupDelay", 426527L );

    private static final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private static ConcurrentMap<String, Buffer> bufferMap = new ConcurrentHashMap<>();
    private static AtomicBoolean cleanupTaskStarted = new AtomicBoolean( false );
    private static final TimerTask cleanupTask = new TimerTask() {
        @Override
        public void run() {
            runBufferCleanup();
        }
    };

    @NotNull
    public static Buffer getOrCreateBuffer( @NotNull String key ) {
        try {
            rwlock.readLock().lock();

            Buffer b = bufferMap.get( key );
            if ( b == null ) {
                b = new NonBlockingMemoryBuffer( key );
                maybeStartCleanupTask();
                Buffer existing = bufferMap.putIfAbsent( key, b );
                if ( existing != null) {
                    b = existing;
                } else {
                    logger.fine( "Created new empty buffer named " + key );
                }
            }

            return b;

        } finally {
            rwlock.readLock().unlock();
        }
    }

    private static void maybeStartCleanupTask() {
        if ( cleanupTaskStarted.compareAndSet( false, true ) ) {
            Background.scheduleRepeated( cleanupTask, CLEANUP_DELAY, CLEANUP_DELAY );
        }
    }

    private static void stopCleanupTask() {
        Background.cancel( cleanupTask );
        cleanupTaskStarted.set( false );
    }

    public static void discardAllBuffers() {
        for ( Buffer buffer : bufferMap.values() ) {
            buffer.discard();
        }
    }

    public static void runBufferCleanup() {
        try {
            long maxIdleBufferAgeSec = ConfigFactory.getLongProperty( BufferDataAssertion.PARAM_MAX_IDLE_BUFFER_AGE, BufferDataAssertion.DEFAULT_MAX_IDLE_BUFFER_AGE );
            long maxIdleBufferAgeMillis = maxIdleBufferAgeSec * 1000L;
            long now = System.currentTimeMillis();
            long minLastUsedTime = now - maxIdleBufferAgeMillis;

            rwlock.writeLock().lock();

            Collection<Buffer> buffers = bufferMap.values();
            Iterator<Buffer> it = buffers.iterator();
            while ( it.hasNext() ) {
                Buffer buffer = it.next();
                if ( buffer.discardIfLastUsedBefore( minLastUsedTime ) ) {
                    logger.log( Level.INFO, "Discarding idle buffer named " + buffer.getName() );
                    it.remove();
                }
            }

        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public static void onModuleUnloaded() {
        stopCleanupTask();
        discardAllBuffers();
    }
}
