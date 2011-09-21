package com.l7tech.external.assertions.ratelimit.server;

import com.l7tech.util.CausedIOException;

import java.util.logging.Logger;

/**
 * Represents a particular thread's position in the queue to enter a rate limit counter.
 */
class RateLimitThreadToken {
    private static final Logger logger = Logger.getLogger(RateLimitThreadToken.class.getName());

    boolean notified = false;

    synchronized boolean waitIfPossible() throws CausedIOException {
        // Check for pending notification
        if (notified)
            return true;
        notified = false;

        try {
            int sleepers = ServerRateLimitAssertion.curSleepThreads.incrementAndGet();
            if ( (long) sleepers > (long) ServerRateLimitAssertion.maxSleepThreads.get())
                return false;

            if (logger.isLoggable(ServerRateLimitAssertion.SUBINFO_LEVEL))
                logger.log(ServerRateLimitAssertion.SUBINFO_LEVEL, "Thread " + Thread.currentThread().getName() + ": WAIT to be notified by previous in line");
            wait(ServerRateLimitAssertion.maxNapTime.get());
            return true;
        } catch (InterruptedException e) {
            throw new CausedIOException("Thread interrupted", e);
        } finally {
            ServerRateLimitAssertion.curSleepThreads.decrementAndGet();
        }
    }

    public synchronized void doNotify() {
        notified = true;
        super.notify();
    }
}
