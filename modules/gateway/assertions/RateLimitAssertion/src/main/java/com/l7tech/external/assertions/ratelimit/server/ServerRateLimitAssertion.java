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
    static final long NANOS_PER_MILLI = 1000000L;  // Number of nanoseconds in one millisecond
    private static final BigInteger NANOS_PER_MILLI_BIG = BigInteger.valueOf(NANOS_PER_MILLI);
    private static final long MILLIS_PER_SECOND = 1000L;
    static final BigInteger MILLIS_PER_SECOND_BIG = BigInteger.valueOf(MILLIS_PER_SECOND);
    static final long NANOS_PER_SECOND = MILLIS_PER_SECOND * NANOS_PER_MILLI;
    static final BigInteger NANOS_PER_SECOND_BIG = BigInteger.valueOf(NANOS_PER_SECOND);
    private static final long DEFAULT_CLUSTER_POLL_INTERVAL = 43L * MILLIS_PER_SECOND; // Check every 43 seconds to see if cluster size has changed
    private static final long DEFAULT_CLUSTER_STATUS_INTERVAL = 8L * MILLIS_PER_SECOND; // Nodes are considered "up" if they are the current node or if they have updated their status row within the last 8 seconds
    private static final int DEFAULT_MAX_QUEUED_THREADS = 20;
    private static final int DEFAULT_CLEANER_PERIOD = 13613;
    private static final int DEFAULT_MAX_NAP_TIME = 4703;
    private static final int DEFAULT_MAX_TOTAL_SLEEP_TIME = 18371;
    static final BigInteger POINTS_PER_REQUEST = BigInteger.valueOf(0x8000L); // cost in points to send a single request
    static final BigInteger DEFAULT_MAX_POINTS = (POINTS_PER_REQUEST.add(POINTS_PER_REQUEST.divide(BigInteger.valueOf(2L))));
    static final Level SUBINFO_LEVEL =
            ConfigFactory.getBooleanProperty( "com.l7tech.external.server.ratelimit.logAtInfo", false ) ? Level.INFO : Level.FINE;

    static final AtomicInteger maxSleepThreads = new AtomicInteger(DEFAULT_MAX_QUEUED_THREADS);

    private static final ConcurrentHashMap<String, RateLimitCounter> counters = new ConcurrentHashMap<String, RateLimitCounter>();
    static final AtomicLong lastClusterCheck = new AtomicLong();
    private static final AtomicReference<BigInteger> clusterSize = new AtomicReference<BigInteger>();
    private static final Lock clusterCheckLock = new ReentrantLock();
    static final AtomicInteger curSleepThreads = new AtomicInteger();
    static final AtomicLong cleanerPeriod = new AtomicLong( (long) DEFAULT_CLEANER_PERIOD );
    static final AtomicLong maxNapTime = new AtomicLong( (long) DEFAULT_MAX_NAP_TIME );
    private static final AtomicLong maxTotalSleepTime = new AtomicLong( (long) DEFAULT_MAX_TOTAL_SLEEP_TIME );
    private static final AtomicLong clusterPollInterval = new AtomicLong( DEFAULT_CLUSTER_POLL_INTERVAL );
    private static final AtomicLong clusterStatusInteval = new AtomicLong( DEFAULT_CLUSTER_STATUS_INTERVAL );
    static boolean auditLimitExceeded = true;
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

        this.windowSizeInSecondsFinder = makeBigIntFinder(assertion.getWindowSizeInSeconds(), "Burst spread limit", getAudit(), 1L );
        this.maxConcurrencyFinder = makeBigIntFinder(assertion.getMaxConcurrency(), "Maximum concurrent requests", getAudit(), 0L );
        this.maxRequestsPerSecondFinder = makeBigIntFinder(assertion.getMaxRequestsPerSecond(), "Maximum requests per second", getAudit(), 1L );
        final String blackout = assertion.getBlackoutPeriodInSeconds();
        this.blackoutSecondsFinder = blackout == null ? null : makeBigIntFinder(blackout, "Blackout period in seconds", getAudit(), 1L );
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final String counterName = getConterName(context);
        final RateLimitCounter counter = findCounter(counterName);

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
                ? DEFAULT_MAX_POINTS
                : pps.multiply(windowSizeInSecondsFinder.call(context));

        final boolean logOnly = assertion.isLogOnly();
        final boolean canSleep = assertion.isShapeRequests();
        AssertionStatus ret;
        if(logOnly || !canSleep){
            ret = checkNoSleep(pps, counter, counterName, maxPoints);
        }else{
            ret = checkWithSleep(pps, counter, counterName, maxPoints);
        }

        if (ret != AssertionStatus.NONE && assertion.getBlackoutPeriodInSeconds() != null) {
            long blackoutMillis = blackoutSecondsFinder.call(context).longValue() * 1000L;
            counter.blackoutFor(clock.currentTimeMillis(), blackoutMillis);
        }

        return ret;
    }


    private AssertionStatus checkNoSleep(BigInteger pps, RateLimitCounter counter, String counterName, BigInteger maxPoints) throws IOException {
        if (BigInteger.ZERO.equals(counter.spend(clock.currentTimeMillis(), pps, maxPoints))) {
            // Successful spend.
            return AssertionStatus.NONE;
        }

        if ( auditLimitExceeded )
            logAndAudit( AssertionMessages.RATELIMIT_RATE_EXCEEDED, counterName );
        if(assertion.isLogOnly()){
            return AssertionStatus.NONE;
        }

        return AssertionStatus.SERVICE_UNAVAILABLE;
    }


    private AssertionStatus checkWithSleep(BigInteger pps, RateLimitCounter counter, String counterName, BigInteger maxPoints) throws IOException {
        final RateLimitThreadToken token = new RateLimitThreadToken();
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


    static boolean isOverslept(long startTime, long now) {
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

    int findMaxConcurrency(PolicyEnforcementContext context) throws NoSuchVariableException, NumberFormatException {
        final BigInteger conc = maxConcurrencyFinder.call(context);
        return assertion.isSplitConcurrencyLimitAcrossNodes()
                ? conc.divide(getClusterSize()).intValue()
                : conc.intValue();
    }

    BigInteger findPointsPerSecond(PolicyEnforcementContext context) throws NoSuchVariableException, NumberFormatException {
        BigInteger rps = maxRequestsPerSecondFinder.call(context);
        if (rps.compareTo(BigInteger.ONE) < 0) throw new IllegalStateException("Max requests per second cannot be less than 1");
        final BigInteger pps = POINTS_PER_REQUEST.multiply(rps);
        return assertion.isSplitRateLimitAcrossNodes() ? pps.divide(getClusterSize()) : pps;
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
    private static RateLimitCounter findCounter(String counterName) {
        RateLimitCounter counter = findExistingCounter(counterName);
        if (counter != null)
            return counter;
        RateLimitCounter prev = counters.putIfAbsent(counterName, counter = new RateLimitCounter(counterName));
        return prev != null ? prev : counter;
    }

    /**
     * Find an existing counter, without creating it if it doesn't already exist.
     *
     * @param counterName the counter name to look up.
     * @return the counter, or null if no such counter currently exists.
     * Note that counters that have not been used recently will be deleted, as part of normal operation.
     */
    public static RateLimitCounter findExistingCounter(String counterName) {
        return counters.get(counterName);
    }



    private String getConterName(PolicyEnforcementContext context) {
        return ExpandVariables.process(counterNameRaw, context.getVariableMap(variablesUsed, getAudit()), getAudit());
    }

    // Caller should ensure that only one thread at a time ever calls this.
    private static void cleanOldCounters(long now) {
        Set<Map.Entry<String,RateLimitCounter>> entries = counters.entrySet();
        final Iterator<Map.Entry<String,RateLimitCounter>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, RateLimitCounter> entry = it.next();
            RateLimitCounter counter = entry.getValue();
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
                        audit.logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Variable value for rate limit field '" + fieldName + "' is not a valid integer" );
                        throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
                    }

                    if (longVal < min) {
                        audit.logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Variable value for rate limit field '" + fieldName + "' is below minimum of " + min );
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
