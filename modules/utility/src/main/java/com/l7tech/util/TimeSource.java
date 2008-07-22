package com.l7tech.util;

/**
 * Wrapper class for accessing the system clock so that unit tests can appear to manipulate TIME ITSELF.
 */
public class TimeSource {
    /**
     * Get current time of day.
     * <p/>
     * This method just invokes {@link System#currentTimeMillis()}.
     *
     * @return current time of day in millis.
     * @see System#currentTimeMillis()
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Get number of nanoseconds since some point in the past.  Caller may hope that
     * this point is fixed and that the result of calling this method do not depend on which
     * CPU the current thread is running on.
     * <p/>
     * This method just invokes {@link System#nanoTime()}.
     *
     * @return current system timer, in nanoseconds
     * @see System#nanoTime()
     */
    public long nanoTime() {
        return System.nanoTime();
    }

    /**
     * Pause the current thread for the specified number of milli and nanoseconds.
     * <p/>
     * This method just invokes {@link Thread#sleep(long millis, int nanos)}.
     *
     * @param sleepMillis number of milliseconds to sleep.
     * @param nanos  number of nanoseconds to sleep in addition to sleepMillis.
     * @throws InterruptedException if the thread is interrupted before the end of the sleep.
     */
    public void sleep(long sleepMillis, int nanos) throws InterruptedException {
        Thread.sleep(sleepMillis, nanos);
    }
}