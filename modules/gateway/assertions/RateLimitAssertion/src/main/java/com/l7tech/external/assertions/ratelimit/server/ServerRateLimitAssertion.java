package com.l7tech.external.assertions.ratelimit.server;

import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.util.Background;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeSource;
import com.l7tech.external.assertions.ratelimit.RateLimitAssertion;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the RateLimitAssertion.
 * @see com.l7tech.external.assertions.ratelimit.RateLimitAssertion
 */
public class ServerRateLimitAssertion extends AbstractServerAssertion<RateLimitAssertion> {
    private static final Logger logger = Logger.getLogger(ServerRateLimitAssertion.class.getName());
    private static final int MAX_REQUESTS_PER_SECOND = 80220368; // Limit to 80million requests per second limit to avoid nanosecond overflow
    private static final long NANOS_PER_MILLI = 1000000L;  // Number of nanoseconds in one millisecond
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long NANOS_PER_SECOND = MILLIS_PER_SECOND * NANOS_PER_MILLI;
    private static final long CLUSTER_POLL_INTERVAL = 43 * MILLIS_PER_SECOND; // Check every 43 seconds to see if cluster size has changed
    private static final int DEFAULT_MAX_QUEUED_THREADS = 20;
    private static final int DEFAULT_CLEANER_PERIOD = 13613;
    private static final int DEFAULT_MAX_NAP_TIME = 4703;
    private static final int DEFAULT_MAX_TOTAL_SLEEP_TIME = 18371;
    private static final long MAX_IDLE_TIME = 3 * MILLIS_PER_SECOND; // Point pool maxes out after 3 seconds idle
    private static final long POINTS_PER_REQUEST = 0x8000L; // cost in points to send a single request
    private static final Level SUBINFO_LEVEL =
                Boolean.getBoolean("com.l7tech.external.server.ratelimit.logAtInfo") ? Level.INFO : Level.FINE;

    static final AtomicInteger maxSleepThreads = new AtomicInteger(DEFAULT_MAX_QUEUED_THREADS);

    private static final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<String, Counter>();
    private static final AtomicLong lastClusterCheck = new AtomicLong();
    private static final AtomicInteger clusterSize = new AtomicInteger();
    private static final Lock clusterCheckLock = new ReentrantLock();
    private static final AtomicInteger curSleepThreads = new AtomicInteger();
    private static final AtomicLong cleanerPeriod = new AtomicLong(DEFAULT_CLEANER_PERIOD);
    private static final AtomicLong maxNapTime = new AtomicLong(DEFAULT_MAX_NAP_TIME);
    private static final AtomicLong maxTotalSleepTime = new AtomicLong(DEFAULT_MAX_TOTAL_SLEEP_TIME);
    static boolean useNanos = true;
    static boolean autoFallbackFromNanos = !Boolean.getBoolean("com.l7tech.external.server.ratelimit.forceNanos");
    static TimeSource clock = new TimeSource();

    static {
        Background.scheduleRepeated(new TimerTask() {
            private long lastCheck = 0;

            public void run() {
                long now = System.currentTimeMillis();
                if (now - lastCheck > cleanerPeriod.get()) {
                    cleanOldCounters(now);
                    lastCheck = System.currentTimeMillis();
                }
            }
        }, 3659, 3659);
    }

    private final RateLimitAssertion rla;
    private final ClusterInfoManager clusterInfoManager;
    private final ServerConfig serverConfig;
    private final Auditor auditor;
    private final String[] variablesUsed;
    private final String counterNameRaw;

    public ServerRateLimitAssertion(RateLimitAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.rla = assertion;
        this.auditor = new Auditor(this, context, logger);
        this.variablesUsed = rla.getVariablesUsed();
        this.counterNameRaw = rla.getCounterName();
        this.clusterInfoManager = (ClusterInfoManager)context.getBean("clusterInfoManager", ClusterInfoManager.class);
        if (clusterInfoManager == null) throw new PolicyAssertionException(rla, "Missing clusterInfoManager bean");

        this.serverConfig = (ServerConfig)context.getBean("serverConfig", ServerConfig.class);
        if (serverConfig == null) throw new PolicyAssertionException(rla, "Missing serverConfig bean");
    }

    private static class ThreadToken {
        private boolean notified = false;

        private synchronized boolean waitIfPossible() throws CausedIOException {
            // Check for pending notification
            if (notified)
                return true;
            notified = false;

            try {
                int sleepers = curSleepThreads.incrementAndGet();
                if (sleepers > (long)maxSleepThreads.get())
                    return false;

                if (logger.isLoggable(SUBINFO_LEVEL))
                    logger.log(SUBINFO_LEVEL, "Thread " + Thread.currentThread().getName() + ": WAIT to be notified by previous in line");
                wait(maxNapTime.get());
                return true;
            } catch (InterruptedException e) {
                throw new CausedIOException("Thread interrupted", e);
            } finally {
                curSleepThreads.decrementAndGet();
            }
        }

        public synchronized void doNotify() {
            notified = true;
            super.notify();
        }
    }

    private static class Counter {
        private final AtomicInteger concurrency = new AtomicInteger();  // total not-yet-closed request threads that have passed through an RLA with this counter
        private final ConcurrentLinkedQueue<ThreadToken> tokenQueue = new ConcurrentLinkedQueue<ThreadToken>();
        private long points = 0;
        private long lastUsed = 0;
        private long lastSpentMillis = 0;
        private long lastSpentNanos = Long.MIN_VALUE;

        // Attempt to spend enough points to send a request.
        // @param now the current time of day
        // @param pointsPerSecond  the number of points given for each 1000ms since the last spend
        // @param maxPoints   maximum number of points this counter should be allowed to accumulate
        // @return 0 if the spend was successful; otherwise, the number of points still needed
        private synchronized long spend(long now, long pointsPerSecond, long maxPoints) {
            return useNanos
                    ? spendNano(now, pointsPerSecond, maxPoints)
                    : spendMilli(now, pointsPerSecond, maxPoints);
        }

        private synchronized long spendMilli(long now, long pointsPerSecond, long maxPoints) {
            // First add points for time passed
            lastUsed = now;
            long idleMs;
            if (lastSpentMillis > now) {
                // Millisecond clock changed -- ntp adjustment?  shouldn't happen
                idleMs = 0;
            } else {
                idleMs = now - lastSpentMillis;
                if (idleMs > MAX_IDLE_TIME) idleMs = MAX_IDLE_TIME;
            }

            long newPoints = points + (idleMs * pointsPerSecond) / MILLIS_PER_SECOND;
            if (newPoints > maxPoints)
                newPoints = maxPoints;

            if (newPoints >= POINTS_PER_REQUEST) {
                // Spend-n-send
                newPoints -= POINTS_PER_REQUEST;
                points = newPoints;
                lastSpentMillis = now;
                return 0;
            }

            // Needs more points
            points = newPoints;
            return POINTS_PER_REQUEST - newPoints;
        }

        private synchronized long spendNano(long now, long pointsPerSecond, long maxPoints) {
            // First add points for time passed
            final long maxIdleNanos = MAX_IDLE_TIME * NANOS_PER_MILLI;
            lastUsed = now;
            final long nanoNow = clock.nanoTime();
            long idleNanos;
            if (lastSpentNanos == Long.MIN_VALUE) {
                idleNanos = maxIdleNanos;
            } else if (lastSpentNanos > nanoNow) {
                // Nano jump backwards in time detected (Sun Java bug 6458294)
                idleNanos = 0;
                if (autoFallbackFromNanos && Math.abs(nanoNow - lastSpentNanos) > 10L * NANOS_PER_MILLI) {
                    synchronized (ServerRateLimitAssertion.class) {
                        if (useNanos) {
                            logger.severe("Nanosecond timer is too unreliable on this system; will use millisecond timer instead from now on");
                            useNanos = false;
                        }
                    }
                }

            } else {
                idleNanos = nanoNow - lastSpentNanos;
                if (idleNanos > maxIdleNanos) idleNanos = maxIdleNanos;
            }

            long newPoints = points + (idleNanos * pointsPerSecond) / NANOS_PER_SECOND;
            if (newPoints > maxPoints)
                newPoints = maxPoints;

            if (newPoints >= POINTS_PER_REQUEST) {
                // Spend-n-send
                newPoints -= POINTS_PER_REQUEST;
                points = newPoints;
                lastSpentNanos = nanoNow;
                return 0;
            }

            // Needs more points
            points = newPoints;
            return POINTS_PER_REQUEST - newPoints;
        }

        private synchronized boolean isStale(long now) {
            return concurrency.get() < 1 && tokenQueue.isEmpty() && (now - lastUsed) > (MAX_IDLE_TIME * 10);
        }

        private void removeToken(ThreadToken token) {
            tokenQueue.remove(token);
            ThreadToken wake = tokenQueue.peek();
            if (wake != null) wake.doNotify();
        }

        // Unconditionally add the specified token to the end of the queue, and then
        // wait until it is in first place (or the maximum wait time or concurrency limit has been hit).
        //
        // @return 0 if the given token is now in first place
        //         1 if the sleep concurrency limit was hit
        //         2 if the maximum total sleep time was hit
        private int pushTokenAndWaitUntilFirst(long startTime, ThreadToken token) throws IOException {
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

                if (isOverslept(startTime, clock.currentTimeMillis()))
                    return 2;
            }
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final String counterName = getConterName(context);
        final Counter counter = findCounter(counterName);

        int maxConcurrency = findMaxConcurrency();
        if (maxConcurrency > 0) {
            // Enforce maximum concurrency for this counter
            final int newConcurrency = counter.concurrency.incrementAndGet();
            context.runOnClose(new Runnable() {
                public void run() {
                    counter.concurrency.decrementAndGet();
                }
            });

            if (newConcurrency > maxConcurrency) {
                auditor.logAndAudit(AssertionMessages.RATELIMIT_CONCURRENCY_EXCEEDED, counterName);
                return AssertionStatus.SERVICE_UNAVAILABLE;
            }
        }

        boolean canSleep = rla.isShapeRequests();
        long pps = findPointsPerSecond();

        final long maxPoints = rla.isHardLimit() ? (POINTS_PER_REQUEST + (POINTS_PER_REQUEST / 2)) : pps;

        return !canSleep
               ? checkNoSleep(pps, counter, counterName, maxPoints)
               : checkWithSleep(pps, counter, counterName, maxPoints);
    }


    private AssertionStatus checkNoSleep(long pps, Counter counter, String counterName, long maxPoints) throws IOException {
        if (counter.spend(clock.currentTimeMillis(), pps, maxPoints) == 0) {
            // Successful spend.
            return AssertionStatus.NONE;
        }

        auditor.logAndAudit(AssertionMessages.RATELIMIT_RATE_EXCEEDED, counterName);
        return AssertionStatus.SERVICE_UNAVAILABLE;
    }


    private AssertionStatus checkWithSleep(long pps, Counter counter, String counterName, long maxPoints) throws IOException {
        final ThreadToken token = new ThreadToken();
        final long maxnap = maxNapTime.get();
        long startTime = clock.currentTimeMillis();

        try {
            int result = counter.pushTokenAndWaitUntilFirst(startTime, token);
            if (result == 1) {
                auditor.logAndAudit(AssertionMessages.RATELIMIT_NODE_CONCURRENCY, counterName);
                return AssertionStatus.SERVICE_UNAVAILABLE;
            } else if (result != 0) {
                auditor.logAndAudit(AssertionMessages.RATELIMIT_SLEPT_TOO_LONG, counterName);
                return AssertionStatus.SERVICE_UNAVAILABLE;
            }

            // We are now in first place.  Run until we either succeed or hit a time or concurrency limit.
            for (;;) {
                long now = clock.currentTimeMillis();

                long shortfall = counter.spend(now, pps, maxPoints);
                if (shortfall == 0)
                    return AssertionStatus.NONE;

                if (isOverslept(startTime, now)) {
                    auditor.logAndAudit(AssertionMessages.RATELIMIT_SLEPT_TOO_LONG, counterName);
                    return AssertionStatus.SERVICE_UNAVAILABLE;
                }

                long sleepNanos = shortfall * NANOS_PER_SECOND / pps + 1;
                if (sleepNanos < 1) sleepNanos = 1;
                long sleepTime = sleepNanos / NANOS_PER_MILLI;
                sleepNanos %= NANOS_PER_MILLI;

                if (sleepTime > maxnap) sleepTime = maxnap; // don't sleep for too long

                if (!sleepIfPossible(curSleepThreads, maxSleepThreads.get(), sleepTime, (int)sleepNanos)) {
                    auditor.logAndAudit(AssertionMessages.RATELIMIT_NODE_CONCURRENCY, counterName);
                    return AssertionStatus.SERVICE_UNAVAILABLE;
                }
            }

        } finally {
            // Leave lineup and wake next in line if we were first
            counter.removeToken(token);
        }
    }


    private static boolean isOverslept(long startTime, long now) {
        return now - startTime > maxTotalSleepTime.get();
    }

    private static boolean sleepIfPossible(AtomicInteger sleepCounter, int maxSleepers, long sleepMillis, int nanos) throws CausedIOException {
        try {
            int sleepers = sleepCounter.incrementAndGet();
            if (sleepers > maxSleepers)
                return false;

            if (logger.isLoggable(SUBINFO_LEVEL))
                logger.log(SUBINFO_LEVEL, "Rate limit: Thead " + Thread.currentThread().getName() + ": sleeping " + sleepMillis + "ms " + nanos + "ns");
            clock.sleep(sleepMillis, nanos);
            return true;
        } catch (InterruptedException e) {
            throw new CausedIOException("Thread interrupted", e);
        } finally {
            sleepCounter.decrementAndGet();
        }
    }

    private int findMaxConcurrency() {
        return rla.getMaxConcurrency() / getClusterSize();
    }

    private long findPointsPerSecond() {
        int rps = rla.getMaxRequestsPerSecond();
        if (rps > MAX_REQUESTS_PER_SECOND)
            rps = MAX_REQUESTS_PER_SECOND; // Guard agaist overflow
        return rps * POINTS_PER_REQUEST / getClusterSize();
    }

    // @return the cluster size. always positive
    private int getClusterSize() {
        long now = clock.currentTimeMillis();
        final long lastCheck = lastClusterCheck.get();
        final int oldSize = clusterSize.get();

        if (oldSize < 1) {
            // Never been initialized.  Always pause to get a value, but only one thread will actually
            // do the work.
            clusterCheckLock.lock();
            try {
                synchronized (ServerRateLimitAssertion.class) {
                    if (clusterSize.get() < 1) {
                        logger.log(SUBINFO_LEVEL, "Initializing cluster size");
                        clusterSize.set(loadClusterSizeFromDb());
                        lastClusterCheck.set(clock.currentTimeMillis());
                    }
                }
            } finally {
                clusterCheckLock.unlock();
            }
        } else if (now - lastCheck >= CLUSTER_POLL_INTERVAL) {
            // We have an existing value, but it's looking a bit stale.  Have one thread try to fetch an update
            // while the rest continue working with the existing value.
            if (clusterCheckLock.tryLock()) {
                try {
                    // See if we still need to do it
                    if (clock.currentTimeMillis() - lastClusterCheck.get() > CLUSTER_POLL_INTERVAL) {
                        logger.log(SUBINFO_LEVEL, "Checking current cluster size");
                        clusterSize.set(loadClusterSizeFromDb());
                        lastClusterCheck.set(clock.currentTimeMillis());
                    }
                } finally {
                    clusterCheckLock.unlock();
                }
            }
        }

        return clusterSize.get();
    }

    // Unconditionally load the cluster size from the database.
    private int loadClusterSizeFromDb() {
        try {
            maxSleepThreads.set(serverConfig.getIntProperty(RateLimitAssertion.PARAM_MAX_QUEUED_THREADS, DEFAULT_MAX_QUEUED_THREADS));
            cleanerPeriod.set(serverConfig.getIntProperty(RateLimitAssertion.PARAM_CLEANER_PERIOD, DEFAULT_CLEANER_PERIOD));
            maxNapTime.set(serverConfig.getIntProperty(RateLimitAssertion.PARAM_MAX_NAP_TIME, DEFAULT_MAX_NAP_TIME));
            maxTotalSleepTime.set(serverConfig.getIntProperty(RateLimitAssertion.PARAM_MAX_TOTAL_SLEEP_TIME, DEFAULT_MAX_TOTAL_SLEEP_TIME));
            Collection<ClusterNodeInfo> got = clusterInfoManager.retrieveClusterStatus();
            final int ret = got == null || got.size() < 1 ? 1 : got.size();
            if (logger.isLoggable(SUBINFO_LEVEL)) logger.log(SUBINFO_LEVEL, "Using cluster size: " + ret);
            return ret;
        } catch (FindException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO,
                                new String[] { "Unable to check cluster status: " + ExceptionUtils.getMessage(e) },
                                e);
            return 1;
        }
    }

    // @return the existing counter with this name, creating a new one if necessary.
    private static Counter findCounter(String counterName) {
        Counter counter = counters.get(counterName);
        if (counter != null)
            return counter;
        Counter prev = counters.putIfAbsent(counterName, counter = new Counter());
        return prev != null ? prev : counter;
    }

    private String getConterName(PolicyEnforcementContext context) {
        return ExpandVariables.process(counterNameRaw, context.getVariableMap(variablesUsed, auditor), auditor);
    }

    // Caller should ensure that only one thread at a time ever calls this.
    private static void cleanOldCounters(long now) {
        Set<Map.Entry<String,Counter>> entries = counters.entrySet();
        final Iterator<Map.Entry<String,Counter>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Counter> entry = it.next();
            Counter counter = entry.getValue();
            if (counter == null) {
                it.remove();
            } else {
                if (counter.isStale(now)) {
                    if (logger.isLoggable(Level.INFO)) logger.log(Level.INFO, "Removing stale rate limiter " + entry.getKey());
                    it.remove();
                }
            }
        }
    }

    /**
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.info("ServerRateLimitAssertion is preparing itself to be unloaded");
    }
}
