package com.l7tech.common.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A TimeSource that can be controlled for test purposes.
 */
public class TestTimeSource extends TimeSource {
    public static final long NANOS_PER_MILLI = 1000000L;
    
    private final AtomicLong currentTimeMillis = new AtomicLong();
    private final AtomicLong nanoTime = new AtomicLong();
    private final AtomicLong leftoverNanos = new AtomicLong(0);

    /**
     * Create a TestTimeSource initialized to the current time.
     */
    public TestTimeSource() {
        sync();
    }

    /**
     * Create a TestTimeSource initialized to the specified time.
     *
     * @param currentTimeMillis  the initial time of day millis.
     * @param nanoTime  the initial system timer nanos.  This is an idependent counter, not
     *                  just an offset from currentTimeMillis.
     */
    public TestTimeSource(long currentTimeMillis, long nanoTime) {
        setCurrentTimeMillis(currentTimeMillis);
        setNanoTime(nanoTime);
    }

    public long currentTimeMillis() {
        return getCurrentTimeMillis();
    }

    public long nanoTime() {
        return getNanoTime();
    }

    /**
     * Synchronize test clock with current real system time.
     */
    public void sync() {
        setCurrentTimeMillis(super.currentTimeMillis());
        setNanoTime(super.nanoTime());
    }


    public long getCurrentTimeMillis() {
        return currentTimeMillis.get();
    }

    public void setCurrentTimeMillis(long currentTimeMillis) {
        this.leftoverNanos.set(0);
        this.currentTimeMillis.set(currentTimeMillis);
    }

    public long getNanoTime() {
        return nanoTime.get();
    }

    public void setNanoTime(long nanoTime) {
        this.nanoTime.set(nanoTime);
    }

    /**
     * Simulate time advancing by the specified number of nanoseconds.  This advances both the millisecond
     * and nanosecond timers.
     * <p/>
     * If the advance isn't evenly divisible by one million, the remainder will be stored and applied to the
     * next advance, in order to keep the millisecond timer advancing correctly.
     *
     * @param nanos the number of nanoseconds to add the counters
     */
    public void advanceByNanos(long nanos) {
        nanoTime.addAndGet(nanos);
        nanos += leftoverNanos.getAndSet(0);
        long millis = nanos / NANOS_PER_MILLI;
        leftoverNanos.addAndGet(nanos % NANOS_PER_MILLI);
        currentTimeMillis.addAndGet(millis);
    }

    /**
     * Simulate time advancing by the specified number of milliseconds.  This advances both the millisecond
     * and nanosecond timers.
     *
     * @param millis the number of milliseconds to add to the counters
     */
    public void advanceByMillis(long millis) {
        nanoTime.addAndGet(millis * NANOS_PER_MILLI);
        currentTimeMillis.addAndGet(millis);
    }

    public void sleep(long sleepMillis, int nanos) throws InterruptedException {
        advanceByMillis(sleepMillis);
        advanceByNanos(nanos);
    }
}
