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
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
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
    private static final long DEFAULT_CLUSTER_POLL_INTERVAL = 43L * MILLIS_PER_SECOND; // Check every 43 seconds to see if cluster size has changed
    private static final long DEFAULT_CLUSTER_STATUS_INTERVAL = 8L * MILLIS_PER_SECOND; // Nodes are considered "up" if they are the current node or if they have updated their status row within the last 8 seconds
    private static final int DEFAULT_MAX_QUEUED_THREADS = 20;
    private static final int DEFAULT_CLEANER_PERIOD = 13613;
    private static final int DEFAULT_MAX_NAP_TIME = 4703;
    private static final int DEFAULT_MAX_TOTAL_SLEEP_TIME = 18371;
    private static final BigInteger POINTS_PER_REQUEST = BigInteger.valueOf(0x8000L); // cost in points to send a single request
    private static final Level SUBINFO_LEVEL =
            ConfigFactory.getBooleanProperty( "com.l7tech.external.server.ratelimit.logAtInfo", false ) ? Level.INFO : Level.FINE;

    static final AtomicInteger maxSleepThreads = new AtomicInteger(DEFAULT_MAX_QUEUED_THREADS);

    private static final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<String, Counter>();
    private static final AtomicLong lastClusterCheck = new AtomicLong();
    private static final AtomicReference<BigInteger> clusterSize = new AtomicReference<BigInteger>();
    private static final Lock clusterCheckLock = new ReentrantLock();
    private static final AtomicInteger curSleepThreads = new AtomicInteger();
    private static final AtomicLong cleanerPeriod = new AtomicLong( (long) DEFAULT_CLEANER_PERIOD );
    private static final AtomicLong maxNapTime = new AtomicLong( (long) DEFAULT_MAX_NAP_TIME );
    private static final AtomicLong maxTotalSleepTime = new AtomicLong( (long) DEFAULT_MAX_TOTAL_SLEEP_TIME );
    private static final AtomicLong clusterPollInterval = new AtomicLong( DEFAULT_CLUSTER_POLL_INTERVAL );
    private static final AtomicLong clusterStatusInteval = new AtomicLong( DEFAULT_CLUSTER_STATUS_INTERVAL );
    static boolean useNanos = true;
    static boolean autoFallbackFromNanos = !ConfigFactory.getBooleanProperty( "com.l7tech.external.server.ratelimit.forceNanos", false );
    static TimeSource clock = new TimeSource();

    static {
        Background.scheduleRepeated(new TimerTask() {
            private long lastCheck = 0L;

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now - lastCheck > cleanerPeriod.get()) {
                    cleanOldCounters(now);
                    lastCheck = System.currentTimeMillis();
                }
            }
        }, 3659L, 3659L );
    }

    interface BigIntFinder extends Functions.Unary<BigInteger, PolicyEnforcementContext> {}

    private final ClusterInfoManager clusterInfoManager;
    private final Config config;

    private final String[] variablesUsed;
    private final String counterNameRaw;
    private final BigIntFinder windowSizeInSecondsFinder;
    private final BigIntFinder maxConcurrencyFinder;
    private final BigIntFinder maxRequestsPerSecondFinder;
    private final BigIntFinder blackoutSecondsFinder;

    public ServerRateLimitAssertion(RateLimitAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.counterNameRaw = assertion.getCounterName();
        this.clusterInfoManager = context.getBean("clusterInfoManager", ClusterInfoManager.class);
        if (clusterInfoManager == null) throw new PolicyAssertionException(assertion, "Missing clusterInfoManager bean");

        this.config = context.getBean("serverConfig", Config.class);
        if ( config == null) throw new PolicyAssertionException(assertion, "Missing serverConfig bean");

        this.windowSizeInSecondsFinder = makeBigIntFinder(assertion.getWindowSizeInSeconds(), "windowSizeInSeconds", getAudit(), 1L );
        this.maxConcurrencyFinder = makeBigIntFinder(assertion.getMaxConcurrency(), "maxConcurrency", getAudit(), 0L );
        this.maxRequestsPerSecondFinder = makeBigIntFinder(assertion.getMaxRequestsPerSecond(), "maxRequestsPerSecond", getAudit(), 0L );
        final String blackout = assertion.getBlackoutPeriodInSeconds();
        this.blackoutSecondsFinder = blackout == null ? null : makeBigIntFinder(blackout, "blackoutPeriodInSeconds", getAudit(), 1L );
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
                if ( (long) sleepers > (long)maxSleepThreads.get())
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
        private BigInteger points = BigInteger.valueOf( 0L );
        private long lastUsed = 0L;
        private long lastSpentMillis = 0L;
        private long lastSpentNanos = Long.MIN_VALUE;
        private final AtomicLong blackoutUntil = new AtomicLong( 0L );

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
                idleMs = 0L;
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
            logAndAudit( AssertionMessages.RATELIMIT_BLACKED_OUT );
            return AssertionStatus.SERVICE_UNAVAILABLE;
        }

        final int maxConcurrency;
        try {
            maxConcurrency = findMaxConcurrency(context);
        } catch (NoSuchVariableException e) {
            logAndAudit( AssertionMessages.NO_SUCH_VARIABLE, e.getVariable() );
            return AssertionStatus.FAILED;
        } catch (NumberFormatException e) {
            logAndAudit( AssertionMessages.VARIABLE_INVALID_VALUE, assertion.getMaxConcurrency(), "Integer" );
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
                logAndAudit( AssertionMessages.RATELIMIT_CONCURRENCY_EXCEEDED, counterName );
                return AssertionStatus.SERVICE_UNAVAILABLE;
            }
        }

        boolean canSleep = assertion.isShapeRequests();
        final BigInteger pps;
        try {
            pps = findPointsPerSecond(context);
        } catch (NoSuchVariableException e) {
            logAndAudit( AssertionMessages.NO_SUCH_VARIABLE, e.getVariable() );
            return AssertionStatus.FAILED;
        } catch (NumberFormatException e) {
            logAndAudit( AssertionMessages.VARIABLE_INVALID_VALUE, assertion.getMaxRequestsPerSecond(), "Integer" );
            return AssertionStatus.FAILED;
        }

        final BigInteger maxPoints = assertion.isHardLimit()
                ? (POINTS_PER_REQUEST.add(POINTS_PER_REQUEST.divide(BigInteger.valueOf( 2L ))))
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

        logAndAudit( AssertionMessages.RATELIMIT_RATE_EXCEEDED, counterName );
        return AssertionStatus.SERVICE_UNAVAILABLE;
    }


    private AssertionStatus checkWithSleep(BigInteger pps, Counter counter, String counterName, BigInteger maxPoints) throws IOException {
        final ThreadToken token = new ThreadToken();
        final BigInteger maxnap = BigInteger.valueOf(maxNapTime.get());
        long startTime = clock.currentTimeMillis();

        try {
            int result = counter.pushTokenAndWaitUntilFirst(startTime, token);
            if (result == 1) {
                logAndAudit( AssertionMessages.RATELIMIT_NODE_CONCURRENCY, counterName );
                return AssertionStatus.SERVICE_UNAVAILABLE;
            } else if (result != 0) {
                logAndAudit( AssertionMessages.RATELIMIT_SLEPT_TOO_LONG, counterName );
                return AssertionStatus.SERVICE_UNAVAILABLE;
            }

            // We are now in first place.  Run until we either succeed or hit a time or concurrency limit.
            for (;;) {
                long now = clock.currentTimeMillis();

                BigInteger shortfall = counter.spend(now, pps, maxPoints);
                if (shortfall.equals(BigInteger.ZERO))
                    return AssertionStatus.NONE;

                if (isOverslept(startTime, now)) {
                    logAndAudit( AssertionMessages.RATELIMIT_SLEPT_TOO_LONG, counterName );
                    return AssertionStatus.SERVICE_UNAVAILABLE;
                }

                BigInteger sleepNanos = shortfall.multiply(NANOS_PER_SECOND_BIG).divide(pps.add(BigInteger.ONE));
                if (sleepNanos.compareTo(BigInteger.ONE) < 0) sleepNanos = BigInteger.ONE;
                BigInteger[] dr = sleepNanos.divideAndRemainder(NANOS_PER_MILLI_BIG);
                BigInteger sleepTime = dr[0];
                int sleepNanosInt = dr[1].intValue();

                if (sleepTime.compareTo(maxnap) > 0) sleepTime = maxnap; // don't sleep for too long

                if (!sleepIfPossible(curSleepThreads, maxSleepThreads.get(), sleepTime.longValue(), sleepNanosInt)) {
                    logAndAudit( AssertionMessages.RATELIMIT_NODE_CONCURRENCY, counterName );
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
                    clusterSize.set(BigInteger.valueOf( (long) loadClusterSizeFromDb() ));
                    lastClusterCheck.set(clock.currentTimeMillis());
                }
            } finally {
                clusterCheckLock.unlock();
            }
        } else {
            final long pollInterval = clusterPollInterval.get();
            if (now - lastCheck >= pollInterval) {
                // We have an existing value, but it's looking a bit stale.  Have one thread try to fetch an update
                // while the rest continue working with the existing value.
                if (clusterCheckLock.tryLock()) {
                    try {
                        // See if we still need to do it
                        if (clock.currentTimeMillis() - lastClusterCheck.get() > pollInterval) {
                            logger.log(SUBINFO_LEVEL, "Checking current cluster size");
                            final int newSize = loadClusterSizeFromDb();
                            clusterSize.set(BigInteger.valueOf( (long) newSize));
                            lastClusterCheck.set(clock.currentTimeMillis());
                            if (newSize != oldSize.longValue()) {
                                logger.info("Rate limit cluster size changed from " + oldSize + " to " + newSize + " active nodes");
                            }
                        }
                    } finally {
                        clusterCheckLock.unlock();
                    }
                }
            }
        }

        return clusterSize.get();
    }

    // Unconditionally load the cluster size from the database.
    private int loadClusterSizeFromDb() {
        try {
            clusterPollInterval.set(config.getLongProperty( RateLimitAssertion.PARAM_CLUSTER_POLL_INTERVAL, DEFAULT_CLUSTER_POLL_INTERVAL ));
            clusterStatusInteval.set(config.getLongProperty( RateLimitAssertion.PARAM_CLUSTER_STATUS_INTERVAL, DEFAULT_CLUSTER_STATUS_INTERVAL ) );
            maxSleepThreads.set(config.getIntProperty(RateLimitAssertion.PARAM_MAX_QUEUED_THREADS, DEFAULT_MAX_QUEUED_THREADS));
            cleanerPeriod.set( (long) config.getIntProperty( RateLimitAssertion.PARAM_CLEANER_PERIOD, DEFAULT_CLEANER_PERIOD ) );
            maxNapTime.set( (long) config.getIntProperty( RateLimitAssertion.PARAM_MAX_NAP_TIME, DEFAULT_MAX_NAP_TIME ) );
            maxTotalSleepTime.set( (long) config.getIntProperty( RateLimitAssertion.PARAM_MAX_TOTAL_SLEEP_TIME, DEFAULT_MAX_TOTAL_SLEEP_TIME ) );
            ClusterNodeInfo selfNodeInf = clusterInfoManager.getSelfNodeInf();
            String selfNodeId = selfNodeInf == null ? "" : selfNodeInf.getNodeIdentifier();
            if (selfNodeId == null) selfNodeId = "";

            int upnodes = 1;
            long now = System.currentTimeMillis();
            Collection<ClusterNodeInfo> nodes = clusterInfoManager.retrieveClusterStatus();
            for (ClusterNodeInfo node : nodes) {
                if (selfNodeId.equals(node.getNodeIdentifier()))
                    continue;

                if (now - node.getLastUpdateTimeStamp() <= clusterStatusInteval.get())
                    upnodes++;
            }

            if (logger.isLoggable(SUBINFO_LEVEL)) logger.log(SUBINFO_LEVEL, "Using cluster size: " + upnodes);
            return upnodes;
        } catch (FindException e) {
            logAndAudit( AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO,
                    new String[]{ "Unable to check cluster status: " + ExceptionUtils.getMessage( e ) },
                    e );
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
        return ExpandVariables.process(counterNameRaw, context.getVariableMap(variablesUsed, getAudit()), getAudit());
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

    private static BigIntFinder makeBigIntFinder(final String variableExpression, final String fieldName, final Audit audit, final long min) {
        final String[] varsUsed = Syntax.getReferencedNames(variableExpression);
        if (varsUsed.length > 0) {
            // Context variable
            return new BigIntFinder() {
                @Override
                public BigInteger call(PolicyEnforcementContext context) {
                    final String str = ExpandVariables.process(variableExpression, context.getVariableMap(varsUsed, audit), audit);

                    final long longVal;
                    try {
                        longVal = Long.valueOf(str);
                    } catch (NumberFormatException e) {
                        audit.logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Variable value for rate limit field " + fieldName + " is not a valid integer" );
                        throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
                    }

                    if (longVal < min) {
                        audit.logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Variable value for rate limit field " + fieldName + " is below minimum of " + min );
                        throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
                    }

                    return BigInteger.valueOf(longVal);
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
}
