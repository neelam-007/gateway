package com.l7tech.server.policy.assertion;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.CausedIOException;
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
import java.util.Collection;

/**
 * Server side implementation of the RateLimitAssertion.
 * @see com.l7tech.policy.assertion.RateLimitAssertion
 */
public class ServerRateLimitAssertion extends AbstractServerAssertion<RateLimitAssertion> implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerRateLimitAssertion.class.getName());
    private static final long CLUSTER_POLL_INTERVAL = 1000 * 43; // Check every 43 seconds to see if cluster size has changed
    private static final int DEFAULT_MAX_SLEEP_THREADS = 20;

    private static final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<String, Counter>();
    private static final AtomicLong lastClusterCheck = new AtomicLong();
    private static final AtomicInteger clusterSize = new AtomicInteger();
    private static final Lock clusterCheckLock = new ReentrantLock();
    private static final AtomicInteger maxSleepThreads = new AtomicInteger(DEFAULT_MAX_SLEEP_THREADS);
    private static final AtomicInteger curSleepThreads = new AtomicInteger();


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
        private final AtomicLong charge = new AtomicLong();
        private final AtomicInteger concurrency = new AtomicInteger();
        private final AtomicLong lastUsed = new AtomicLong();
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
        long noderps = findMaxRate();
        long now = System.currentTimeMillis();
        long charge;

        do {
            synchronized (counter) {
                charge = counter.charge.get();
                charge -= (now - counter.lastUsed.get());
                if (charge < 0) charge = 0;
                counter.charge.set(charge);
                counter.lastUsed.set(now);
            }

            if (charge <= noderps) {
                // Still room for more
                break;
            }

            if (!canSleep) {
                auditor.logAndAudit(AssertionMessages.RATELIMIT_RATE_EXCEEDED, new String[] { counterName });
                return AssertionStatus.SERVICE_UNAVAILABLE;
            }

            // TODO add max sleep time
            // TODO add FIFO semantics
            // TODO replace rate limiting algorithm with one that works!
            try {
                int curSleeping = curSleepThreads.incrementAndGet();
                if (curSleeping > maxSleepThreads.get()) {
                    auditor.logAndAudit(AssertionMessages.RATELIMIT_NODE_CONCURRENCY, new String[] { counterName });
                    return AssertionStatus.SERVICE_UNAVAILABLE;
                }
                Thread.sleep(charge - noderps);
            } catch (InterruptedException e) {
                throw new CausedIOException("Thread interrupted", e);
            } finally {
                curSleepThreads.decrementAndGet();
            }
        } while (charge > noderps);

        counter.charge.addAndGet(1000);
        return AssertionStatus.NONE;
    }

    private int findMaxConcurrency() {
        return rla.getMaxConcurrency() / getClusterSize();
    }

    private long findMaxRate() {
        return rla.getMaxRequestsPerSecond() * 1000L / getClusterSize();
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
                if (clusterSize.get() < 1) {
                    logger.fine("Initializing cluster size");
                    clusterSize.set(loadClusterSizeFromDb());
                }
            } finally {
                clusterCheckLock.unlock();
            }
        } else if (now - lastCheck >= CLUSTER_POLL_INTERVAL) {
            // We have an existing value, but it's looking a bit stale.  Have one thread try to fetch an update
            // while the rest continue working with the existing value.
            if (clusterCheckLock.tryLock()) {
                try {
                    logger.fine("Checking current cluster size");
                    clusterSize.set(loadClusterSizeFromDb());
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
            Collection<ClusterNodeInfo> got = clusterInfoManager.retrieveClusterStatus();
            return got == null || got.size() < 1 ? 1 : got.size();
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
}
