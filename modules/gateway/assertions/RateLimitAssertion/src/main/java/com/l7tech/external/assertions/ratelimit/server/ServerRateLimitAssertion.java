package com.l7tech.external.assertions.ratelimit.server;

import com.l7tech.external.assertions.ratelimit.RateLimitAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
    private static final long NANOS_PER_MILLI = 1000000L;  // Number of nanoseconds in one millisecond
    private static final BigInteger NANOS_PER_MILLI_BIG = BigInteger.valueOf(NANOS_PER_MILLI);
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final BigInteger MILLIS_PER_SECOND_BIG = BigInteger.valueOf(MILLIS_PER_SECOND);
    static final long NANOS_PER_SECOND = MILLIS_PER_SECOND * NANOS_PER_MILLI;
    private static final BigInteger NANOS_PER_SECOND_BIG = BigInteger.valueOf(NANOS_PER_SECOND);
    private static final long CLUSTER_POLL_INTERVAL = 43 * MILLIS_PER_SECOND; // Check every 43 seconds to see if cluster size has changed
    private static final int DEFAULT_MAX_QUEUED_THREADS = 20;
    private static final int DEFAULT_CLEANER_PERIOD = 13613;
    private static final int DEFAULT_MAX_NAP_TIME = 4703;
    private static final int DEFAULT_MAX_TOTAL_SLEEP_TIME = 18371;
    private static final BigInteger POINTS_PER_REQUEST = BigInteger.valueOf(0x8000L); // cost in points to send a single request
    private static final Level SUBINFO_LEVEL =
                Boolean.getBoolean("com.l7tech.external.server.ratelimit.logAtInfo") ? Level.INFO : Level.FINE;

    static final AtomicInteger maxSleepThreads = new AtomicInteger(DEFAULT_MAX_QUEUED_THREADS);

    private static final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<String, Counter>();
    private static final AtomicLong lastClusterCheck = new AtomicLong();
    private static final AtomicReference<BigInteger> clusterSize = new AtomicReference<BigInteger>();
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

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now - lastCheck > cleanerPeriod.get()) {
                    cleanOldCounters(now);
                    lastCheck = System.currentTimeMillis();
                }
            }
        }, 3659, 3659);
    }

    interface BigIntFinder extends Functions.Unary<BigInteger, PolicyEnforcementContext> {}

    private final RateLimitAssertion rla;
    private final ClusterInfoManager clusterInfoManager;
    private final ServerConfig serverConfig;
    private final Auditor auditor;
    private final String[] variablesUsed;
    private final String counterNameRaw;
    private final BigIntFinder windowSizeInSecondsFinder;
    private final BigIntFinder maxConcurrencyFinder;
    private final BigIntFinder maxRequestsPerSecondFinder;
    private final BigIntFinder blackoutSecondsFinder;

    public ServerRateLimitAssertion(RateLimitAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.rla = assertion;
        this.auditor = new Auditor(this, context, logger);
        this.variablesUsed = rla.getVariablesUsed();
        this.counterNameRaw = rla.getCounterName();
        this.clusterInfoManager = context.getBean("clusterInfoManager", ClusterInfoManager.class);
        if (clusterInfoManager == null) throw new PolicyAssertionException(rla, "Missing clusterInfoManager bean");

        this.serverConfig = context.getBean("serverConfig", ServerConfig.class);
        if (serverConfig == null) throw new PolicyAssertionException(rla, "Missing serverConfig bean");

        this.windowSizeInSecondsFinder = makeBigIntFinder(assertion.getWindowSizeInSeconds(), auditor);
        this.maxConcurrencyFinder = makeBigIntFinder(assertion.getMaxConcurrency(), auditor);
        this.maxRequestsPerSecondFinder = makeBigIntFinder(assertion.getMaxRequestsPerSecond(), auditor);
        final String blackout = assertion.getBlackoutPeriodInSeconds();
        this.blackoutSecondsFinder = blackout == null ? null : makeBigIntFinder(blackout, auditor);
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
        private BigInteger points = BigInteger.valueOf(0);
        private long lastUsed = 0;
        private long lastSpentMillis = 0;
        private long lastSpentNanos = Long.MIN_VALUE;
        private final AtomicLong blackoutUntil = new AtomicLong(0);

        // Attempt to spend enough points to send a request.
        // @param now the current time of day
        // @param pointsPerSecond  the number of points given for each 1000ms since the last spend
        // @param maxPoints   maximum number of points this counter should be allowed to accumulate
        // @return 0 if the spend was successful; otherwise, the number of points still needed
        private synchronized BigInteger spend(long now, BigInteger pointsPerSecond, BigInteger maxPoints) {
            return useNanos
                    ? spendNano(now, pointsPerSecond, maxPoints)
                    : spendMilli(now, pointsPerSecond, maxPoints);
        }

        private synchronized BigInteger spendMilli(long now, BigInteger pointsPerSecond, BigInteger maxPoints) {
            // First add points for time passed
            long idleMs;
            if (lastSpentMillis > now) {
                // Millisecond clock changed -- ntp adjustment?  shouldn't happen
                idleMs = 0;
            } else {
                idleMs = now - lastSpentMillis;
            }

            BigInteger newPoints = points.add((pointsPerSecond.multiply(BigInteger.valueOf(idleMs))).divide(MILLIS_PER_SECOND_BIG));
            if (newPoints.compareTo(maxPoints) > 0)
                newPoints = maxPoints;

            if (newPoints.compareTo(POINTS_PER_REQUEST) >= 0) {
                // Spend-n-send
                newPoints = newPoints.subtract(POINTS_PER_REQUEST);
                points = newPoints;
                lastSpentMillis = now;
                lastUsed = now;
                return BigInteger.ZERO;
            }

            // Needs more points
            return POINTS_PER_REQUEST.subtract(newPoints);
        }

        private synchronized BigInteger spendNano(long now, BigInteger pointsPerSecond, BigInteger maxPoints) {
            // First add points for time passed
            final long nanoNow = clock.nanoTime();
            BigInteger newPoints;
            if (lastSpentNanos == Long.MIN_VALUE) {
                newPoints = maxPoints;
            } else if (lastSpentNanos > nanoNow) {
                // Nano jump backwards in time detected (Sun Java bug 6458294)
                if (autoFallbackFromNanos && Math.abs(nanoNow - lastSpentNanos) > 10L * NANOS_PER_MILLI) {
                    synchronized (ServerRateLimitAssertion.class) {
                        if (useNanos) {
                            logger.severe("Nanosecond timer is too unreliable on this system; will use millisecond timer instead from now on");
                            useNanos = false;
                        }
                    }
                }
                newPoints = points;
            } else {
                long idleNanos = nanoNow - lastSpentNanos;
                newPoints = points.add((pointsPerSecond.multiply(BigInteger.valueOf(idleNanos))).divide(NANOS_PER_SECOND_BIG));
            }

            if (newPoints.compareTo(maxPoints) > 0)
                newPoints = maxPoints;

            if (newPoints.compareTo(POINTS_PER_REQUEST) >= 0) {
                // Spend-n-send
                newPoints = newPoints.subtract(POINTS_PER_REQUEST);
                points = newPoints;
                lastUsed = now;
                lastSpentNanos = nanoNow;
                return BigInteger.ZERO;
            }

            // Needs more points
            return POINTS_PER_REQUEST.subtract(newPoints);
        }

        private synchronized boolean isStale(long now) {
            return concurrency.get() < 1 && tokenQueue.isEmpty() && (now - lastUsed) > cleanerPeriod.get();
        }

        private void removeToken(ThreadToken token) {
            tokenQueue.remove(token);
            ThreadToken wake = tokenQueue.peek();
            if (wake != null) wake.doNotify();
        }

        public void blackoutFor(long now, long millis) {
            long when = now + millis;
            blackoutUntil.set(when);
        }

        public boolean checkBlackedOut(long now) {
            long until = blackoutUntil.get();

            if (until < 1)
                return false; // No blackout in effect

            if (now < until)
                return true; // Currently blacked out

            // Blackout just expired
            blackoutUntil.compareAndSet(until, 0);
            return false;
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

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final String counterName = getConterName(context);
        final Counter counter = findCounter(counterName);

        if (counter.checkBlackedOut(clock.currentTimeMillis())) {
            auditor.logAndAudit(AssertionMessages.RATELIMIT_BLACKED_OUT);
            return AssertionStatus.SERVICE_UNAVAILABLE;
        }

        final int maxConcurrency;
        try {
            maxConcurrency = findMaxConcurrency(context);
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            return AssertionStatus.FAILED;
        } catch (NumberFormatException e) {
            auditor.logAndAudit(AssertionMessages.VARIABLE_INVALID_VALUE, rla.getMaxConcurrency(), "Integer");
            return AssertionStatus.FAILED;
        }

        if (maxConcurrency > 0) {
            // Enforce maximum concurrency for this counter
            final int newConcurrency = counter.concurrency.incrementAndGet();
            context.runOnClose(new Runnable() {
                @Override
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
        final BigInteger pps;
        try {
            pps = findPointsPerSecond(context);
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            return AssertionStatus.FAILED;
        } catch (NumberFormatException e) {
            auditor.logAndAudit(AssertionMessages.VARIABLE_INVALID_VALUE, rla.getMaxRequestsPerSecond(), "Integer");
            return AssertionStatus.FAILED;
        }

        final BigInteger maxPoints = rla.isHardLimit()
                ? (POINTS_PER_REQUEST.add(POINTS_PER_REQUEST.divide(BigInteger.valueOf(2))))
                : pps.multiply(windowSizeInSecondsFinder.call(context));

        AssertionStatus ret = !canSleep
               ? checkNoSleep(pps, counter, counterName, maxPoints)
               : checkWithSleep(pps, counter, counterName, maxPoints);

        if (ret != AssertionStatus.NONE && assertion.getBlackoutPeriodInSeconds() != null) {
            long blackoutMillis = blackoutSecondsFinder.call(context).longValue() * 1000L;
            counter.blackoutFor(clock.currentTimeMillis(), blackoutMillis);
        }

        return ret;
    }


    private AssertionStatus checkNoSleep(BigInteger pps, Counter counter, String counterName, BigInteger maxPoints) throws IOException {
        if (BigInteger.ZERO.equals(counter.spend(clock.currentTimeMillis(), pps, maxPoints))) {
            // Successful spend.
            return AssertionStatus.NONE;
        }

        auditor.logAndAudit(AssertionMessages.RATELIMIT_RATE_EXCEEDED, counterName);
        return AssertionStatus.SERVICE_UNAVAILABLE;
    }


    private AssertionStatus checkWithSleep(BigInteger pps, Counter counter, String counterName, BigInteger maxPoints) throws IOException {
        final ThreadToken token = new ThreadToken();
        final BigInteger maxnap = BigInteger.valueOf(maxNapTime.get());
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

                BigInteger shortfall = counter.spend(now, pps, maxPoints);
                if (shortfall.equals(BigInteger.ZERO))
                    return AssertionStatus.NONE;

                if (isOverslept(startTime, now)) {
                    auditor.logAndAudit(AssertionMessages.RATELIMIT_SLEPT_TOO_LONG, counterName);
                    return AssertionStatus.SERVICE_UNAVAILABLE;
                }

                BigInteger sleepNanos = shortfall.multiply(NANOS_PER_SECOND_BIG).divide(pps.add(BigInteger.ONE));
                if (sleepNanos.compareTo(BigInteger.ONE) < 0) sleepNanos = BigInteger.ONE;
                BigInteger[] dr = sleepNanos.divideAndRemainder(NANOS_PER_MILLI_BIG);
                BigInteger sleepTime = dr[0];
                int sleepNanosInt = dr[1].intValue();

                if (sleepTime.compareTo(maxnap) > 0) sleepTime = maxnap; // don't sleep for too long

                if (!sleepIfPossible(curSleepThreads, maxSleepThreads.get(), sleepTime.longValue(), sleepNanosInt)) {
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
                logger.log(SUBINFO_LEVEL, "Rate limit: Thread " + Thread.currentThread().getName() + ": sleeping " + sleepMillis + "ms " + nanos + "ns");
            clock.sleep(sleepMillis, nanos);
            return true;
        } catch (InterruptedException e) {
            throw new CausedIOException("Thread interrupted", e);
        } finally {
            sleepCounter.decrementAndGet();
        }
    }

    private int findMaxConcurrency(PolicyEnforcementContext context) throws NoSuchVariableException, NumberFormatException {
        return maxConcurrencyFinder.call(context).divide(getClusterSize()).intValue();
    }

    private BigInteger findPointsPerSecond(PolicyEnforcementContext context) throws NoSuchVariableException, NumberFormatException {
        BigInteger rps = maxRequestsPerSecondFinder.call(context);
        if (rps.compareTo(BigInteger.ONE) < 0) throw new IllegalStateException("Max requests per second cannot be less than 1");
        return POINTS_PER_REQUEST.multiply(rps).divide(getClusterSize());
    }

    // @return the cluster size. always positive
    private BigInteger getClusterSize() {
        long now = clock.currentTimeMillis();
        final long lastCheck = lastClusterCheck.get();
        final BigInteger oldSize = clusterSize.get();

        if (oldSize == null) {
            // Never been initialized.  Always pause to get a value, but only one thread will actually
            // do the work.
            clusterCheckLock.lock();
            try {
                if (clusterSize.get() == null) {
                    logger.log(SUBINFO_LEVEL, "Initializing cluster size");
                    clusterSize.set(BigInteger.valueOf(loadClusterSizeFromDb()));
                    lastClusterCheck.set(clock.currentTimeMillis());
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
                        clusterSize.set(BigInteger.valueOf(loadClusterSizeFromDb()));
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

    private static BigIntFinder makeBigIntFinder(final String variableExpression, final Audit auditor) {
        final String[] varsUsed = Syntax.getReferencedNames(variableExpression);
        if (varsUsed.length > 0) {
            // Context variable
            return new BigIntFinder() {
                @Override
                public BigInteger call(PolicyEnforcementContext context) {
                    return new BigInteger(ExpandVariables.process(variableExpression, context.getVariableMap(varsUsed, auditor), auditor));
                }
            };
        } else {
            // Constant value
            final BigInteger i = new BigInteger(variableExpression);
            return new BigIntFinder() {
                @Override
                public BigInteger call(PolicyEnforcementContext context) {
                    return i;
                }
            };
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
