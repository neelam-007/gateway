package com.l7tech.external.assertions.ratelimit.server;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Represents a particular rate limit enforcement context.
 */
class RateLimitCounter {
    private static final Logger logger = Logger.getLogger(RateLimitCounter.class.getName());

    final AtomicInteger concurrency = new AtomicInteger();  // total not-yet-closed request threads that have passed through an RLA with this counter
    final String name; // counter name, for query/monitoring purposes
    private final ConcurrentLinkedQueue<RateLimitThreadToken> tokenQueue = new ConcurrentLinkedQueue<RateLimitThreadToken>();
    private BigInteger points = BigInteger.valueOf( 0L );
    private BigInteger lastPointsPerSecond = null;
    private BigInteger lastMaxPoints = null;
    private long lastUsed = 0L;
    private long lastSpentMillis = 0L;
    private long lastSpentNanos = Long.MIN_VALUE;
    final AtomicLong blackoutUntil = new AtomicLong( 0L );

    RateLimitCounter(String name) {
        this.name = name;
    }

    class StateSnapshot {
        final BigInteger points;
        final BigInteger pointsCreditedForIdleTime;
        final long lastUsed;
        final long lastSpentMillis;
        final long lastSpentNanos;

        StateSnapshot(BigInteger points, BigInteger pointsCreditedForIdleTime, long lastUsed, long lastSpentMillis, long lastSpentNanos) {
            this.points = points;
            this.pointsCreditedForIdleTime = pointsCreditedForIdleTime;
            this.lastUsed = lastUsed;
            this.lastSpentMillis = lastSpentMillis;
            this.lastSpentNanos = lastSpentNanos;
        }
    }

    synchronized StateSnapshot query() {
        BigInteger newPoints;
        if (lastMaxPoints != null && lastPointsPerSecond != null) {
            newPoints = ServerRateLimitAssertion.useNanos
                    ? findNewPointsNano(ServerRateLimitAssertion.clock.nanoTime(), lastPointsPerSecond, lastMaxPoints)
                    : findNewPointsMilli(ServerRateLimitAssertion.clock.currentTimeMillis(), lastPointsPerSecond, lastMaxPoints);
        } else {
            newPoints = points;
        }


        return new StateSnapshot(points, newPoints, lastUsed, lastSpentMillis, lastSpentNanos);
    }

    // Attempt to spend enough points to send a request.
    // @param now the current time of day
    // @param pointsPerSecond  the number of points given for each 1000ms since the last spend
    // @param maxPoints   maximum number of points this counter should be allowed to accumulate
    // @return 0 if the spend was successful; otherwise, the number of points still needed
    synchronized BigInteger spend(long now, BigInteger pointsPerSecond, BigInteger maxPoints) {
        lastPointsPerSecond = pointsPerSecond;
        lastMaxPoints = maxPoints;
        return ServerRateLimitAssertion.useNanos
                ? spendNano(now, pointsPerSecond, maxPoints)
                : spendMilli(now, pointsPerSecond, maxPoints);
    }

    private synchronized BigInteger spendMilli(long now, BigInteger pointsPerSecond, BigInteger maxPoints) {
        BigInteger newPoints = findNewPointsMilli(now, pointsPerSecond, maxPoints);

        if (newPoints.compareTo(ServerRateLimitAssertion.POINTS_PER_REQUEST) >= 0) {
            // Spend-n-send
            newPoints = newPoints.subtract(ServerRateLimitAssertion.POINTS_PER_REQUEST);
            points = newPoints;
            lastSpentMillis = now;
            lastUsed = now;
            return BigInteger.ZERO;
        }

        // Needs more points
        return ServerRateLimitAssertion.POINTS_PER_REQUEST.subtract(newPoints);
    }

    private synchronized BigInteger findNewPointsMilli(long now, BigInteger pointsPerSecond, BigInteger maxPoints) {
        // First add points for time passed
        long idleMs;
        if (lastSpentMillis > now) {
            // Millisecond clock changed -- ntp adjustment?  shouldn't happen
            idleMs = 0L;
        } else {
            idleMs = now - lastSpentMillis;
        }

        BigInteger newPoints = points.add((pointsPerSecond.multiply(BigInteger.valueOf(idleMs))).divide(ServerRateLimitAssertion.MILLIS_PER_SECOND_BIG));
        if (newPoints.compareTo(maxPoints) > 0)
            newPoints = maxPoints;
        return newPoints;
    }

    private synchronized BigInteger spendNano(long now, BigInteger pointsPerSecond, BigInteger maxPoints) {
        // First add points for time passed
        final long nanoNow = ServerRateLimitAssertion.clock.nanoTime();
        BigInteger newPoints = findNewPointsNano(nanoNow, pointsPerSecond, maxPoints);

        if (newPoints.compareTo(ServerRateLimitAssertion.POINTS_PER_REQUEST) >= 0) {
            // Spend-n-send
            newPoints = newPoints.subtract(ServerRateLimitAssertion.POINTS_PER_REQUEST);
            points = newPoints;
            lastUsed = now;
            lastSpentNanos = nanoNow;
            return BigInteger.ZERO;
        }

        // Needs more points
        return ServerRateLimitAssertion.POINTS_PER_REQUEST.subtract(newPoints);
    }

    private synchronized BigInteger findNewPointsNano(long nanoNow, BigInteger pointsPerSecond, BigInteger maxPoints) {
        BigInteger newPoints;
        if (lastSpentNanos == Long.MIN_VALUE) {
            newPoints = maxPoints;
        } else if (lastSpentNanos > nanoNow) {
            // Nano jump backwards in time detected (Sun Java bug 6458294)
            if (ServerRateLimitAssertion.autoFallbackFromNanos && Math.abs(nanoNow - lastSpentNanos) > 10L * ServerRateLimitAssertion.NANOS_PER_MILLI) {
                synchronized (ServerRateLimitAssertion.class) {
                    if (ServerRateLimitAssertion.useNanos) {
                        logger.severe("Nanosecond timer is too unreliable on this system; will use millisecond timer instead from now on");
                        ServerRateLimitAssertion.useNanos = false;
                    }
                }
            }
            newPoints = points;
        } else {
            long idleNanos = nanoNow - lastSpentNanos;
            newPoints = points.add((pointsPerSecond.multiply(BigInteger.valueOf(idleNanos))).divide(ServerRateLimitAssertion.NANOS_PER_SECOND_BIG));
        }

        if (newPoints.compareTo(maxPoints) > 0)
            newPoints = maxPoints;
        return newPoints;
    }

    synchronized boolean isStale(long now) {
        return concurrency.get() < 1 && tokenQueue.isEmpty() && (now - lastUsed) > ServerRateLimitAssertion.cleanerPeriod.get();
    }

    void removeToken(RateLimitThreadToken token) {
        tokenQueue.remove(token);
        RateLimitThreadToken wake = tokenQueue.peek();
        if (wake != null) wake.doNotify();
    }

    public void blackoutFor(long now, long millis) {
        long when = now + millis;
        blackoutUntil.set(when);
    }

    public boolean checkBlackedOut(long now) {
        long until = blackoutUntil.get();

        if (until < 1L )
            return false; // No blackout in effect

        if (now < until)
            return true; // Currently blacked out

        // Blackout just expired
        blackoutUntil.compareAndSet(until, 0L );
        return false;
    }

    // Unconditionally add the specified token to the end of the queue, and then
    // wait until it is in first place (or the maximum wait time or concurrency limit has been hit).
    //
    // @return 0 if the given token is now in first place
    //         1 if the sleep concurrency limit was hit
    //         2 if the maximum total sleep time was hit
    int pushTokenAndWaitUntilFirst(long startTime, RateLimitThreadToken token) throws IOException {
        synchronized (token) {
            token.notified = false;
            tokenQueue.offer(token);
            if (token.equals(tokenQueue.peek()))
                return 0;
        }

        for (;;) {
            synchronized (token) {
                if (!token.waitIfPossible())
                    return 1;
                if (token.equals(tokenQueue.peek()))
                    return 0;
                token.notified = false;
            }

            if (ServerRateLimitAssertion.isOverslept(startTime, ServerRateLimitAssertion.clock.currentTimeMillis()))
                return 2;
        }
    }

    public String getName() {
        return name;
    }
}
