package com.l7tech.common.io;

import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

import EDU.oswego.cs.dl.util.concurrent.ReentrantLock;

import com.l7tech.common.util.CausedIOException;

/**
 * An InputStream that will timeout if no data is read.
 *
 * <p>There is also a minimum thoughput to prevent "slow-write" attacks.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class TimeoutInputStream extends InputStream {

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
        currentBlockers.add(this.bin);
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
        if(!bin.timedOut){
            in.close();
            currentBlockers.remove(this.bin);
        }
    }

    /**
     *
     */
    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    /**
     * Reset the mark in the usual manner.
     *
     * @throws IOException if an underlying error occurs or if there is a timeout
     */
    public void reset() throws IOException {
        checkTimeout();
        in.reset();
    }

    /**
     *
     */
    public boolean markSupported() {
        return in.markSupported();
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
    private static final Set currentBlockers = Collections.synchronizedSet(new HashSet());

    // Create and start the timer daemon
    static {
        Thread timer = new Thread(new Interrupter());
        timer.setDaemon(true);
        timer.setName("InputTimeoutThread");
        timer.start();
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
     *
     */
    private ReentrantLock lock;

    /**
     *
     */
    private void acquireLock() throws IOException {
        try {
            lock.acquire();
        }
        catch(InterruptedException ie) {
            throw new CausedIOException(ie);
        }
    }

    /**
     *
     */
    private void releaseLock() {
        lock.release();
    }

    /**
     *
     */
    private boolean isOuterLock() {
        return lock.holds()==1;
    }

    /**
     * Check for timeout, throw if necessary
     */
    private void checkTimeout() throws IOException {
        if(bin.timedOut) throw new IOException("Stream timeout");
    }

    /**
     * Called on entry to a blocking section (activate timeout)
     */
    private void enterBlocking() throws IOException {
        acquireLock();
        checkTimeout();
        if(isOuterLock()) {
            bin.enter();
        }
    }

    /**
     * Called on exit from a blocking section (deactivate timeout)
     */
    private void exitBlocking() throws IOException {
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
     *
     */
    private int trackBytes(int bytes) {
        if(bytes>0) {
            bin.bytesRead += bytes;
        }
        else if(bytes<0) {
            bin.readComplete = true;
        }
        return bytes;
    }

    /**
     *
     */
    private long trackBytes(long bytes) {
        if(bytes>0) {
            bin.bytesRead += bytes;
        }
        else if(bytes<0) {
            bin.readComplete = true;
        }
        return bytes;
    }

    /**
     * Data for the blocker
     */
    private static class BlockerInfo {
        private Thread threadToInterrupt;
        private InputStream inputStreamToClose;
        private boolean cleared;
        private boolean readComplete;
        private long bytesRead;
        private long streamStartTime;
        private long operationStartTime;
        private boolean timedOut;

        private long readTimeout;
        private long minSlowTime;
        private int minDataRate;

        private BlockerInfo(InputStream toClose, long timeout, long slowTime, int dataRate) {
            threadToInterrupt = Thread.currentThread();
            inputStreamToClose = toClose;
            streamStartTime = System.currentTimeMillis();
            bytesRead = 0;
            readComplete = false;
            cleared = true;
            timedOut = false;
            readTimeout = timeout;
            minSlowTime = slowTime;
            minDataRate = dataRate;
        }

        private void enter() {
            if(cleared==false) throw new IllegalStateException("Reentrance or multi-threading problem!");
            cleared = false;
            operationStartTime = System.currentTimeMillis();
        }

        private void exit() {
            cleared = true;
        }
    }

    /**
     * Runnable for checking on currently blocked IO.
     */
    private static class Interrupter implements Runnable {

        private void interrupt(BlockerInfo bi, boolean justFlag) {
            if(!justFlag) {
                InputStream localIS = bi.inputStreamToClose;
                bi.inputStreamToClose = null;
                if(localIS!=null) {
                    // interrupt the thread and then close its input stream
                    try{ localIS.close(); }catch(IOException ioe){}
                }
            }
            bi.timedOut = true; // mark timeOut after closing
            currentBlockers.remove(bi);
        }

        public void run() {
            logger.info("InputStream timeout thread starting.");
            try {
                for(;;) {
                    Thread.sleep(500);

                    // copy set so we can iterate without a lock
                    Set copy = null;
                    synchronized(currentBlockers) {
                        copy = new HashSet(currentBlockers);
                    }

                    long timeNow = System.currentTimeMillis();
                    for(Iterator iterator = copy.iterator(); iterator.hasNext(); ) {
                        BlockerInfo bi = (BlockerInfo) iterator.next();

                        // check for completed read
                        if(bi.readComplete) {
                            currentBlockers.remove(bi);
                            continue;
                        }

                        // check for blocked IO timeouts
                        if(!bi.cleared && (timeNow-bi.operationStartTime)>bi.readTimeout) {
                            logger.fine("IO timeout, interrupting.");
                            interrupt(bi,false);
                            continue;
                        }

                        // check for slow read timeouts
                        if(bi.minDataRate > 0) {
                            long activeTime = timeNow - bi.streamStartTime;
                            if(activeTime > bi.minSlowTime) {
                                // we have enough data to make a rate check valid
                                int bytesPerSecond = (int)(bi.bytesRead / activeTime);
                                if(bytesPerSecond < bi.minDataRate) {
                                    if(!bi.cleared) logger.fine("IO too slow, interrupting.");
                                    interrupt(bi, bi.cleared);
                                }
                            }
                        }
                    }
                }
            }
            catch(InterruptedException ie) {
                // shutdown
            }
            logger.info("InputStream timeout thread stopping.");
        }
    }
}
