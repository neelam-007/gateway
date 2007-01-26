package com.l7tech.server.policy.assertion;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.Background;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RateLimitAssertion;
import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.ServerConfig;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

/**
 * Server side implementation of the RateLimitAssertion.
 * @see com.l7tech.policy.assertion.RateLimitAssertion
 */
public class ServerRateLimitAssertion extends AbstractServerAssertion<RateLimitAssertion> implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerRateLimitAssertion.class.getName());
    private static final long CLUSTER_POLL_INTERVAL = 1000 * 43; // Check every 43 seconds to see if cluster size has changed
    private static final int DEFAULT_MAX_SLEEP_THREADS = 20;
    private static final int DEFAULT_CLEANER_PERIOD = 13613;
    private static final int DEFAULT_MAX_NAP_TIME = 4703;
    private static final int DEFAULT_MAX_TOTAL_SLEEP_TIME = 18371;
    private static final Level SUBINFO_LEVEL = Level.INFO;

    static final AtomicInteger maxSleepThreads = new AtomicInteger(DEFAULT_MAX_SLEEP_THREADS);

    private static final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<String, Counter>();
    private static final AtomicLong lastClusterCheck = new AtomicLong();
    private static final AtomicInteger clusterSize = new AtomicInteger();
    private static final Lock clusterCheckLock = new ReentrantLock();
    private static final AtomicInteger curSleepThreads = new AtomicInteger();
    private static final AtomicLong cleanerPeriod = new AtomicLong(DEFAULT_CLEANER_PERIOD);
    private static final AtomicLong maxNapTime = new AtomicLong(DEFAULT_MAX_NAP_TIME);
    private static final AtomicLong maxTotalSleepTime = new AtomicLong(DEFAULT_MAX_TOTAL_SLEEP_TIME);
    private static final ThreadLocal threadTokens = new ThreadLocal() {
        protected Object initialValue() {
            return new Object();
        }
    };

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

    private static class Counter {
        private static final long MAX_IDLE_TIME = 10 * 1000L; // Point pool maxes out after 10 seconds idle
        private static final long POINTS_PER_REQUEST = 65536L; // cost in points to send a single request

        private final AtomicInteger concurrency = new AtomicInteger();  // total not-yet-closed request threads that have passed through an RLA with this counter
        private final LinkedList<Object> tokenQueue = new LinkedList<Object>();
        private long points = 0;
        private long lastUsed = 0;
        private long lastSpent = 0;

        // Attempt to spend enough points to send a request.
        // @param now the current time of day
        // @param pointsPerSecond  the number of points given for each 1000ms since the last spend
        // @param maxPoints   maximum number of points this counter should be allowed to accumulate
        // @return 0 if the spend was successful; otherwise, the number of points still needed
        private synchronized long spend(long now, long pointsPerSecond, long maxPoints) {
            // First add points for time passed
            lastUsed = now;
            long idleMs = now - lastSpent;
            if (idleMs > MAX_IDLE_TIME) idleMs = MAX_IDLE_TIME;

            long newPoints = points + (idleMs * pointsPerSecond) / 1000L;
            if (newPoints > maxPoints)
                newPoints = maxPoints;

            if (newPoints >= POINTS_PER_REQUEST) {
                // Spend-n-send
                newPoints -= POINTS_PER_REQUEST;
                points = newPoints;
                lastSpent = now;
                return 0;
            }

            // Needs more points
            points = newPoints;
            return POINTS_PER_REQUEST - newPoints;
        }

        private synchronized boolean isStale(long now) {
            return concurrency.get() < 1 && getNumWaiters() < 1 && (now - lastUsed) > MAX_IDLE_TIME;
        }

        private synchronized int getNumWaiters() {
            return tokenQueue.size();
        }

        private void removeToken(Object token) {
            Object wakeToken = null;
            synchronized (this) {
                tokenQueue.remove(token);
                if (tokenQueue.size() > 0)
                    wakeToken = tokenQueue.getFirst();
            }
            if (wakeToken != null) {
                synchronized (wakeToken) {
                    wakeToken.notify();
                }
            }
        }

        // Unconditionally add the specified token to the end of the queue, and then
        // wait until it is in first place (or the maximum wait time or concurrency limit has been hit).
        //
        // @return 0 if the given token is now in first place
        //         1 if the sleep concurrency limit was hit
        //         2 if the maximum total sleep time was hit
        private int pushTokenAndWaitUntilFirst(long startTime, Object token) throws IOException {
            synchronized (this) {
                tokenQueue.addLast(token);
                if (tokenQueue.size() == 1)
                    return 0;
            }

            for (;;) {
                synchronized (token) {
                    if (!waitIfPossible(token, curSleepThreads, maxSleepThreads.get()))
                        return 1;

                    synchronized (this) {
                        if (tokenQueue.getFirst().equals(token))
                            return 0;
                    }
                }

                if (isOverslept(startTime))
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
                auditor.logAndAudit(AssertionMessages.RATELIMIT_CONCURRENCY_EXCEEDED, new String[] { counterName });
                return AssertionStatus.SERVICE_UNAVAILABLE;
            }
        }

        boolean canSleep = rla.isShapeRequests();
        long pps = findPointsPerSecond();

        final long maxPoints = rla.isHardLimit() ? (Counter.POINTS_PER_REQUEST + (Counter.POINTS_PER_REQUEST / 2)) : pps;                               

        return !canSleep
               ? checkNoSleep(pps, counter, counterName, maxPoints)
               : checkWithSleep(pps, counter, counterName, maxPoints);
    }


    private AssertionStatus checkNoSleep(long pps, Counter counter, String counterName, long maxPoints) throws IOException {
        if (counter.spend(System.currentTimeMillis(), pps, maxPoints) == 0) {
            // Successful spend.
            return AssertionStatus.NONE;
        }

        auditor.logAndAudit(AssertionMessages.RATELIMIT_RATE_EXCEEDED, new String[] { counterName });
        return AssertionStatus.SERVICE_UNAVAILABLE;
    }


    private AssertionStatus checkWithSleep(long pps, Counter counter, String counterName, long maxPoints) throws IOException {
        final Object token = threadTokens.get();
        final long maxnap = maxNapTime.get();
        long startTime = System.currentTimeMillis();

        try {
            int result = counter.pushTokenAndWaitUntilFirst(startTime, token);
            if (result == 1) {
                auditor.logAndAudit(AssertionMessages.RATELIMIT_NODE_CONCURRENCY, new String[] { counterName });
                return AssertionStatus.SERVICE_UNAVAILABLE;
            } else if (result != 0) {
                auditor.logAndAudit(AssertionMessages.RATELIMIT_SLEPT_TOO_LONG, new String[] { counterName });
                return AssertionStatus.SERVICE_UNAVAILABLE;
            }

            // We are now in first place.  Run until we either succeed or hit a time or concurrency limit.
            for (;;) {
                long now = System.currentTimeMillis();

                long shortfall = counter.spend(now, pps, maxPoints);
                if (shortfall == 0)
                    return AssertionStatus.NONE;

                if (isOverslept(startTime)) {
                    auditor.logAndAudit(AssertionMessages.RATELIMIT_SLEPT_TOO_LONG, new String[] { counterName });
                    return AssertionStatus.SERVICE_UNAVAILABLE;
                }

                long sleepTime =  shortfall * 1000L / pps + 2;
                if (sleepTime < 1) sleepTime = 1;
                if (sleepTime > maxnap) sleepTime = maxnap; // don't sleep for too long

                if (!sleepIfPossible(curSleepThreads, maxSleepThreads.get(), sleepTime)) {
                    auditor.logAndAudit(AssertionMessages.RATELIMIT_NODE_CONCURRENCY, new String[] { counterName });
                    return AssertionStatus.SERVICE_UNAVAILABLE;
                }
            }            

        } finally {
            // Leave lineup and wake next in line if we were first
            counter.removeToken(token);
        }
    }


    private static boolean isOverslept(long startTime) {
        return System.currentTimeMillis() - startTime > maxTotalSleepTime.get();
    }

    private static boolean sleepIfPossible(AtomicInteger sleepCounter, int maxSleepers, long sleepMillis) throws CausedIOException {
        if (sleepMillis < 1)
            throw new IllegalArgumentException("sleepMillis must be positive");
        try {
            int sleepers = sleepCounter.incrementAndGet();
            if (sleepers > maxSleepers)
                return false;

            if (logger.isLoggable(SUBINFO_LEVEL)) logger.log(SUBINFO_LEVEL, "Rate limit: Thead " + Thread.currentThread().getName() + ": sleeping " + sleepMillis + " ms");
            Thread.sleep(sleepMillis);
            return true;
        } catch (InterruptedException e) {
            throw new CausedIOException("Thread interrupted", e);
        } finally {
            sleepCounter.decrementAndGet();
        }
    }

    private static boolean waitIfPossible(Object token, AtomicInteger sleepCounter, long maxSleepers) throws CausedIOException {
        try {
            int sleepers = sleepCounter.incrementAndGet();
            if (sleepers > maxSleepers)
                return false;

            if (logger.isLoggable(SUBINFO_LEVEL)) logger.log(SUBINFO_LEVEL, "Thread " + Thread.currentThread().getName() + ": WAIT to be notified by previous in line");
            token.wait(maxNapTime.get());
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
        return rla.getMaxRequestsPerSecond() * Counter.POINTS_PER_REQUEST / getClusterSize();
    }

    // @return the cluster size. always positive
    private int getClusterSize() {
        long now = System.currentTimeMillis();
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
                        lastClusterCheck.set(System.currentTimeMillis());
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
                    if (System.currentTimeMillis() - lastClusterCheck.get() > CLUSTER_POLL_INTERVAL) {
                        logger.log(SUBINFO_LEVEL, "Checking current cluster size");
                        clusterSize.set(loadClusterSizeFromDb());
                        lastClusterCheck.set(System.currentTimeMillis());
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
            maxSleepThreads.set(serverConfig.getIntProperty(ServerConfig.PARAM_RATELIMIT_MAX_CONCURRENCY, DEFAULT_MAX_SLEEP_THREADS));
            cleanerPeriod.set(serverConfig.getIntProperty(ServerConfig.PARAM_RATELIMIT_CLEANER_PERIOD, DEFAULT_CLEANER_PERIOD));
            maxNapTime.set(serverConfig.getIntProperty(ServerConfig.PARAM_RATELIMIT_MAX_NAP_TIME, DEFAULT_MAX_NAP_TIME));
            maxTotalSleepTime.set(serverConfig.getIntProperty(ServerConfig.PARAM_RATELIMIT_MAX_TOTAL_SLEEP_TIME, DEFAULT_MAX_TOTAL_SLEEP_TIME));
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
        return ExpandVariables.process(counterNameRaw, context.getVariableMap(variablesUsed, auditor));
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
                    if (logger.isLoggable(SUBINFO_LEVEL)) logger.log(SUBINFO_LEVEL, "Removing stale rate limit counter " + entry.getKey());
                    it.remove();
                }
            }
        }
    }
}
