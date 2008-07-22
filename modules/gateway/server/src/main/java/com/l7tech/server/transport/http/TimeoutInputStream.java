package com.l7tech.server.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.servlet.ServletInputStream;

import com.l7tech.util.ShutdownExceptionHandler;
import com.l7tech.util.ResourceUtils;

/**
 * An InputStream that will timeout if no data is read.
 *
 * <p>There is also a minimum thoughput to prevent "slow-write" attacks.</p>
 *
 * <p>Note that although this class extends ServletInputStream, you can only
 * use the {@link #readLine(byte[],int,int) readLine} method if the wrapped
 * stream is a ServletInputStream (else you will get an exception)</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class TimeoutInputStream extends ServletInputStream {

    //- PUBLIC

    /**
     * Create a timeout input stream.
     *
     * <p>Create a timeout InputStream that allows 20 seconds per blocking read
     * and has a minimum transfer rate of 1kBps (first checked after 20 seconds).</p>
     *
     * @param inputStream the stream to wrap
     * @throws IOException if the given stream is null
     */
    public TimeoutInputStream(InputStream inputStream) throws IOException {
        this(inputStream, DEFAULT_TIMEOUT, DEFAULT_MIN_SLOW_DATA_TIME, DEFAULT_SLOW_BYTE_SEC_LIMIT);
    }

    /**
     * Create a timeout input stream with the given timeout properties
     *
     * @param inputStream the stream to wrap
     * @param readTimeout the timeout for a blocked read in milliseconds
     * @param timeBeforeRateCheck the time to wait before checking minimum transfer rate
     * @param minRate the minimum acceptable transfer rate in kB (Kilobytes), 0 for no limit
     * @throws IOException if the given stream is null
     */
    public TimeoutInputStream(InputStream inputStream, long readTimeout, long timeBeforeRateCheck, int minRate) throws IOException {
        if(inputStream==null) throw new IOException("null inputStream");

        // init
        this.in = inputStream;
        this.bin = new BlockerInfo(this,readTimeout,timeBeforeRateCheck,minRate);
        this.lock = new ReentrantLock();

        // track
        newBlockerQueue.add(this.bin);
    }

    /**
     * Read a byte.
     *
     * @return the byte, in the usual manner.
     * @throws IOException if an underlying error occurs or if there is a timeout
     */
    public int read() throws IOException {
        enterBlocking();
        try {
            return trackBytes(in.read());
        }
        finally {
            exitBlocking();
        }
    }

    /**
     * Read into a byte array.
     *
     * @param b the byte array
     * @return the number of bytes, etc
     * @throws IOException if an underlying error occurs or if there is a timeout
     */
    public int read(byte[] b) throws IOException {
        enterBlocking();
        try {
            return trackBytes(in.read(b));
        }
        finally {
            exitBlocking();
        }
    }

    /**
     * Read into a (portion of a) byte array.
     *
     * @param b the byte array
     * @param off the offset
     * @param len the length
     * @return the number of bytes, etc
     * @throws IOException if an underlying error occurs or if there is a timeout
     */
    public int read(byte[] b, int off, int len) throws IOException {
        enterBlocking();
        try {
            return trackBytes(in.read(b, off, len));
        }
        finally {
            exitBlocking();
        }
    }

    /**
     * Read a line of input into a (portion of a) byte array.
     *
     * @param b the byte array
     * @param off the offset
     * @param len  the length
     * @return the number of bytes, etc
     * @throws IOException if an underlying error occurs or if there is a timeout
     * @throws UnsupportedOperationException if the wrapped stream is not a ServletInputStream
     */
    public int readLine(byte[] b, int off, int len) throws IOException {
        enterBlocking();
        try {
            return trackBytes(((ServletInputStream)in).readLine(b, off, len));
        }
        catch(ClassCastException cce) {
            throw new UnsupportedOperationException("The underlying stream does not support readLine!");
        }
        finally {
            exitBlocking();
        }
    }

    /**
     * Skip bytes from the input stream.
     *
     * @param n the number of bytes to skip
     * @return the number of bytes skipped
     * @throws IOException if an underlying error occurs or if there is a timeout
     */
    public long skip(long n) throws IOException {
        enterBlocking();
        try {
            return trackBytes(in.skip(n));
        }
        finally {
            exitBlocking();
        }
    }

    /**
     * Check how many non-blocking, bytes can be read/skipped.
     *
     * @return the number of bytes
     * @throws IOException if an underlying error occurs or if there is a timeout
     */
    public int available() throws IOException {
        checkTimeout();
        return in.available();
    }

    /**
     * Close this input stream.
     *
     * @throws IOException if an underlying error occurs
     */
    public void close() throws IOException {
        if(!bin.isTimedOut()){
            in.close();
            doneBlocking();
        }
    }

    /**
     *
     */
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    /**
     * Reset the mark in the usual manner.
     *
     * @throws IOException if an underlying error occurs or if there is a timeout
     */
    public synchronized void reset() throws IOException {
        checkTimeout();
        in.reset();
    }

    /**
     *
     */
    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     * Called to indicate that this stream is no longer in use (but the underlying
     * stream may or may not be closed / read completely)
     *
     * You must not read from the stream after calling this method.
     */
    public final void done() {
        bin.markReadComplete();
    }

    //- PROTECTED

    /**
     * Called on entry to a blocking section (activate timeout)
     */
    protected final void enterBlocking() throws IOException {
        acquireLock();
        checkTimeout();
        if(isOuterLock()) {
            bin.enter();
        }
    }

    /**
     * Called on exit from a blocking section (deactivate timeout)
     */
    protected final void exitBlocking() throws IOException {
        try {
            if(isOuterLock()) {
                bin.exit();
            }
            checkTimeout();
        }
        finally {
            releaseLock();
        }
    }

    /**
     * Called on exit from a blocking section (deactivate timeout)
     */
    protected final void doneBlocking() {
        bin.done();
    }

    /**
     * Record a number of bytes read. If -1 is passed then the stream is
     * flagged as fully read.
     */
    protected final int trackBytes(int bytes) {
        bin.trackBytes(bytes);
        return bytes;
    }

    /**
     * Record a number of bytes read. If -1 is passed then the stream is
     * flagged as fully read.
     */
    protected final long trackBytes(long bytes) {
        bin.trackBytes(bytes);
        return bytes;
    }

    //- PRIVATE

    /**
     * Log for the class
     */
    private static final Logger logger = Logger.getLogger(TimeoutInputStream.class.getName());

    /**
     * Socket read timeout
     */
    private static final long DEFAULT_TIMEOUT = 1000L * 20L;

    /**
     * 20 secs of data before considering a slow input
     */
    private static final long DEFAULT_MIN_SLOW_DATA_TIME = 1000L * 20L;

    /**
     * 1KB per second
     */
    private static final int  DEFAULT_SLOW_BYTE_SEC_LIMIT = 1024;

    /**
     * Set of currently in use TimeoutInputStream.BlockerInfos
     */
    private static final Queue<BlockerInfo> newBlockerQueue = new ConcurrentLinkedQueue();

    // Create and start the timer daemon
    private static Object timerLock = new Object();
    private static Exception diedWithException = null;
    private static Thread timer = null;
    static {
        synchronized(timerLock) {
            timer = new Thread(new Interrupter());
            timer.setDaemon(true);
            timer.setName("InputTimeoutThread");
            timer.setUncaughtExceptionHandler(ShutdownExceptionHandler.getInstance());
            timer.start();
        }

        Thread timerWatcher = new Thread(new InterrupterWatcher());
        timerWatcher.setDaemon(true);
        timerWatcher.setName("InputTimeoutThreadWatchdog");
        timerWatcher.setUncaughtExceptionHandler(ShutdownExceptionHandler.getInstance());
        timerWatcher.start();
    }

    /**
     * The wrapped input stream
     */
    private InputStream in;

    /**
     * The timeout info
     */
    private BlockerInfo bin;

    /**
     * reentrant to allow for read calling another read method
     */
    private ReentrantLock lock;

    /**
     *
     */
    private void acquireLock() {
        lock.lock();
    }

    /**
     *
     */
    private void releaseLock() {
        lock.unlock();
    }

    /**
     *
     */
    private boolean isOuterLock() {
        return lock.getHoldCount()==1;
    }

    /**
     * Check for timeout, throw if necessary
     */
    private void checkTimeout() throws IOException {
        if(bin.isTimedOut()) throw new IOException("Stream timeout");
    }

    /**
     * Data for the blocker
     */
    private static class BlockerInfo {
        // lock for data sync
        private Object lock = new Object();

        // thread shared data
        private boolean cleared;
        private boolean readComplete;
        private boolean timedOut;

        private long bytesRead;
        private long operationStartTime;

        // immutable data
        private final long streamStartTime;
        private final InputStream inputStreamToClose;
        private final long readTimeout;
        private final long minSlowTime;
        private final int minDataRate;

        BlockerInfo(InputStream toClose, long timeout, long slowTime, int dataRate) {
            inputStreamToClose = toClose;
            streamStartTime = System.currentTimeMillis();
            readTimeout = timeout;
            minSlowTime = slowTime;
            minDataRate = dataRate;

            synchronized(lock) {
                cleared = true;
                readComplete = false;
                timedOut = false;
                bytesRead = 0;
                operationStartTime = 0;
            }
        }

        void trackBytes(int bytes) {
            if(bytes>0) {
                synchronized (lock) {
                    bytesRead += bytes;
                }
            }
            else if(bytes<0) {
                synchronized (lock) {
                    readComplete = true;
                }
            }
        }

        void trackBytes(long bytes) {
            if(bytes>0) {
                synchronized (lock) {
                    bytesRead += bytes;
                }
            }
            else if(bytes<0) {
                synchronized (lock) {
                    readComplete = true;
                }
            }
        }

        void enter() {
            long time = System.currentTimeMillis();
            synchronized (lock) {
                if(cleared==false) throw new IllegalStateException("Reentrance or multi-threading problem!");
                cleared = false;
                operationStartTime = time;
            }
        }

        void exit() {
            synchronized (lock) {
                cleared = true;
            }
        }

        void done() {
            synchronized (lock) {
                readComplete = true;
            }
        }

        void markReadComplete() {
            synchronized (lock) {
                readComplete = true;
            }
        }

        boolean isReadComplete() {
            synchronized (lock) {
                return readComplete;
            }
        }

        void markTimedOut() {
            synchronized (lock) {
                readComplete = true;
            }
        }

        boolean isTimedOut() {
            synchronized (lock) {
                return timedOut;
            }
        }

        boolean readTimeout(long timeNow) {
            boolean timeout = false;
            synchronized(lock) {
                timeout = !cleared && (timeNow-operationStartTime)>readTimeout;
            }
            return timeout;
        }

        void slowRead(long timeNow, boolean[] flags) {
            flags[0] = false;
            flags[1] = false;

            long bytesRead;
            boolean cleared;
            synchronized(lock) {
                bytesRead = this.bytesRead;
                cleared = this.cleared;
            }

            if(minDataRate > 0) {
                long activeTime = timeNow - streamStartTime;
                if(activeTime > minSlowTime) {
                    // we have enough data to make a rate check valid
                    int bytesPerSecond = (int)(bytesRead / activeTime);
                    if(bytesPerSecond < minDataRate) {
                        flags[0] = true;
                        flags[1] = cleared;
                    }
                }
            }

        }
    }

    /**
     * Runnable that restarts the other runnable if it dies ...
     */
    private static class InterrupterWatcher implements Runnable {
        public void run() {
            try {
                while (true) {
                    Thread.sleep(50);

                    Thread timerThread = null;
                    Exception exception = null;
                    synchronized(timerLock) {
                        timerThread = timer;
                        exception = diedWithException;
                    }

                    if (timerThread != null && !timerThread.isAlive()) {
                        if (exception != null) {
                            logger.log(Level.SEVERE, "Timeout input stream processing thread died unexpectedly.", exception);
                        }

                        logger.log(Level.INFO, "Restarting input stream processing thread ...");
                        synchronized(timerLock) {
                            diedWithException = null;
                            timer = new Thread(new Interrupter());
                            timer.setDaemon(true);
                            timer.setName("InputTimeoutThread");
                            timer.setUncaughtExceptionHandler(ShutdownExceptionHandler.getInstance());
                            timer.start();
                        }
                        logger.log(Level.INFO, "Restarted input stream processing thread.");
                    }
                }
            }
            catch(InterruptedException ie) {
                // shutdown
            }
        }
    }

    /**
     * Runnable for checking on currently blocked IO.
     */
    private static class Interrupter implements Runnable {
        private Set currentBlockers = new HashSet(100);

        private void interrupt(BlockerInfo bi, boolean justFlag) {
            if(!justFlag) {
                // interrupt the thread and then close its input stream
                ResourceUtils.closeQuietly(bi.inputStreamToClose);
            }
            bi.markTimedOut(); // mark timeOut after closing
        }

        public void run() {
            logger.info("InputStream timeout thread starting.");
            try {
                boolean processedBlockers = false;
                for(;;) {
                    if (!processedBlockers)
                        Thread.sleep(100);
                    else
                        Thread.sleep(10);
                    processedBlockers = false;

                    // process new arrivals
                    BlockerInfo newBlocker = null;
                    while ((newBlocker = newBlockerQueue.poll()) != null ) {
                        if (!newBlocker.isReadComplete()) {
                            currentBlockers.add(newBlocker);
                            processedBlockers = true;
                        }
                    }

                    //
                    long timeNow = System.currentTimeMillis();
                    for(Iterator iterator = currentBlockers.iterator(); iterator.hasNext(); ) {
                        BlockerInfo blockerInfo = (BlockerInfo) iterator.next();

                        // check for completed read
                        if(blockerInfo.isReadComplete()) {
                            iterator.remove();
                            continue;
                        }

                        // check for blocked IO timeouts
                        if(blockerInfo.readTimeout(timeNow)) {
                            logger.fine("IO timeout, interrupting.");
                            interrupt(blockerInfo,false);
                            iterator.remove();
                            continue;
                        }

                        // flag to check again soon
                        processedBlockers = true;

                        // check for slow read timeouts
                        boolean[] slowReadFlags = new boolean[2];
                        blockerInfo.slowRead(timeNow, slowReadFlags);
                        if(slowReadFlags[0]) {
                            if(!slowReadFlags[1]) logger.fine("IO too slow, interrupting.");
                            interrupt(blockerInfo, slowReadFlags[1]);
                            iterator.remove();
                        }
                    }
                }
            }
            catch(InterruptedException ie) {
                // shutdown
                synchronized(timerLock) {
                    timer = null;   
                }
            }
            catch(Exception exception) {
                synchronized(timerLock) {
                    diedWithException = exception;
                }
            }
            logger.info("InputStream timeout thread stopping.");
        }
    }
}
