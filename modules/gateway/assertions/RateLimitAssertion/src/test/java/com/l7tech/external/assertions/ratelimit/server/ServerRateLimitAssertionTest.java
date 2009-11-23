package com.l7tech.external.assertions.ratelimit.server;

import com.l7tech.util.TestTimeSource;
import com.l7tech.util.TimeSource;
import com.l7tech.common.TestDocuments;
import com.l7tech.external.assertions.ratelimit.RateLimitAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.test.BenchmarkRunner;
import com.l7tech.test.BugNumber;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeoutExecutor;
import com.l7tech.message.Message;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static junit.framework.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test the RateLimitAssertion.
 */
public class ServerRateLimitAssertionTest {
    static {
        System.setProperty("com.l7tech.external.server.ratelimit.logAtInfo", "true");
    }

    private static final Logger log = Logger.getLogger(ServerRateLimitAssertionTest.class.getName());

    /** A mock system clock for testing purposes. */
    private static final TestTimeSource clock = new TestTimeSource() {
        public void sleep(long sleepMillis, int nanos) throws InterruptedException {
            sleepHandler.get().sleep(sleepMillis, nanos);
        }
    };

    private static ApplicationContext applicationContext;
    private static ServerConfigStub serverConfig;

    /**
     * The Sleep strategy to use with the mock system clock, broken out so it can be changed during the test.
     */
    private static final AtomicReference<TimeSource> sleepHandler = new AtomicReference<TimeSource>();

    /**
     * A sleep handler that reports the sleep event so a subclass can decide what blocking or other behavior to perform in response.
     */
    private abstract static class SleepHandler extends TestTimeSource {
        public void sleep(long sleepMillis, int nanos) throws InterruptedException {
            try {
                onSleep(sleepMillis, nanos);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                throw (InterruptedException)new InterruptedException("sleep failed: " + ExceptionUtils.getMessage(e)).initCause(e);
            }
        }

        abstract void onSleep(long sleepMillis, int nanos) throws Exception;
    }

    /**
     * A sleep handler that immediately fails noisily in such a way as to hopefully cause the entire test to abort.
     */
    private static class FailingSleepHandler extends SleepHandler {
        private final String reason;

        FailingSleepHandler(String suffix) {
            this.reason = suffix;
        }

        protected void onSleep(long sleepMillis, int nanos) {
            final String msg = "Unexpected sleep for " + sleepMillis + "ms/" + nanos + "ns: Thread " + Thread.currentThread() + ": " + reason;
            log.log(Level.WARNING, msg);
            fail(msg);
        }
    }

    /**
     * A sleep handler that simulates a sleep by instantly advancing the test clock by the requested time
     * interval, then immediately returning.
     */
    private static class SingleThreadedInstantSleepSimulator extends TestTimeSource {
        public void sleep(long sleepMillis, int nanos) throws InterruptedException {
            clock.advanceByMillis(sleepMillis);
            clock.advanceByNanos(nanos);
        }
    }

    @Before
    public void setUp() throws Exception {
        applicationContext = new ClassPathXmlApplicationContext(new String[]{
                "com/l7tech/external/assertions/ratelimit/server/serverRateLimitAssertionTestApplicationContext.xml"
        });

        // Ensure cleaner doesn't run during the test
        serverConfig = (ServerConfigStub) applicationContext.getBean("serverConfig", ServerConfigStub.class);
        serverConfig.putProperty(RateLimitAssertion.PARAM_CLEANER_PERIOD, String.valueOf(86400L * 1000L));

        // Make test use our fake time source
        ServerRateLimitAssertion.clock = clock;
        ServerRateLimitAssertion.useNanos = true;
        ServerRateLimitAssertion.autoFallbackFromNanos = false;
        sleepHandler.set(new FailingSleepHandler("no sleep handler configured for current test"));
    }

    private PolicyEnforcementContext makeContext() throws Exception {
        Message request = new Message(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT));
        Message response = new Message();
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private ServerAssertion makePolicy(RateLimitAssertion rla) throws Exception {
        return new ServerRateLimitAssertion(rla, applicationContext);
    }

    @Test
    public void testConcurrencyLimit() throws Exception {
        clock.sync();

        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("testConcurrencyLimit");
        rla.setMaxConcurrency(10);
        rla.setMaxRequestsPerSecond(10000);
        ServerAssertion ass = makePolicy(rla);

        PolicyEnforcementContext pecs[] = new PolicyEnforcementContext[20];

        // First 10 should enter no problem
        for (int i = 0; i < 10; ++i) {
            PolicyEnforcementContext pec = pecs[i] = makeContext();
            AssertionStatus result = ass.checkRequest(pec);
            assertEquals(result, AssertionStatus.NONE);
        }

        // next 10 should be rejected
        for (int i = 10; i < 20; ++i) {
            PolicyEnforcementContext pec = pecs[i] = makeContext();
            AssertionStatus result = ass.checkRequest(pec);
            assertEquals(result, AssertionStatus.SERVICE_UNAVAILABLE);
        }

        // Close first 10
        for (int i = 0; i < 10; ++i)
            pecs[i].close();

        // A new one should still be rejected
        {
            PolicyEnforcementContext pec = makeContext();
            AssertionStatus result = ass.checkRequest(pec);
            assertEquals(result, AssertionStatus.SERVICE_UNAVAILABLE);
            pec.close();
        }

        // Close second 10
        for (int i = 10; i < 20; ++i)
            pecs[i].close();

        // A new one should now be accepted
        {
            PolicyEnforcementContext pec = makeContext();
            AssertionStatus result = ass.checkRequest(pec);
            assertEquals(result, AssertionStatus.NONE);
            pec.close();
        }
    }

    static final AtomicInteger nextId = new AtomicInteger(1);

    static final ConcurrentHashMap<Treq, Object> running = new ConcurrentHashMap<Treq, Object>();

    @Test
    @BugNumber(8090)
    public void testSimpleFailLimitNoSleepNoBurst() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setMaxRequestsPerSecond(25);
        rla.setShapeRequests(false);
        rla.setHardLimit(true);
        rla.setCounterName("MyTestCounter-23423");
        rla.setMaxConcurrency(0);
        ServerAssertion ass = makePolicy(rla);

        clock.sync();
        for (int i = 0; i < 2500; ++i) {
            assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
            assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
            assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
            assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
            assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
            clock.advanceByNanos(ServerRateLimitAssertion.NANOS_PER_SECOND / 25);
        }
    }

    @Test
    @BugNumber(8090)
    public void testSimpleFailLimitNoSleepNoBurst_moreIterations() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setMaxRequestsPerSecond(25);
        rla.setShapeRequests(false);
        rla.setHardLimit(true);
        rla.setCounterName("MyTestCounter-211-43636536");
        rla.setMaxConcurrency(0);
        ServerAssertion ass = makePolicy(rla);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger attemptCount = new AtomicInteger(0);
        long nanosPerTick = ServerRateLimitAssertion.NANOS_PER_SECOND / 733;

        clock.sync();
        for (int i = 0; i < 733; ++i) {
            attemptCount.incrementAndGet();
            if (AssertionStatus.NONE.equals(ass.checkRequest(makeContext())))
                successCount.incrementAndGet();
            clock.advanceByNanos(nanosPerTick);
        }

        assertEquals(26L, successCount.get());
    }

    @Ignore("Enable this test to perform an expensive verification that rate limits with many failures work correctly with concurrency")
    @Test
    @BugNumber(8090)
    public void testMultiThreadSimpleFailNoSleepNoBurst() throws Exception {
        clock.sync();
        final long origNanoTime = clock.nanoTime();
        final AtomicBoolean needTicker = new AtomicBoolean(false);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger attemptCount = new AtomicInteger(0);
        final int maxReqPerSec = 100;

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    doRun();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public void doRun() throws Exception {
                RateLimitAssertion rla = new RateLimitAssertion();
                rla.setMaxRequestsPerSecond(maxReqPerSec);
                rla.setShapeRequests(false);
                rla.setHardLimit(true);
                rla.setCounterName("MyTestCounter-44-2923847");
                rla.setMaxConcurrency(0);
                ServerAssertion ass = makePolicy(rla);

                // Only one ticker thread
                final boolean isTicker = needTicker.compareAndSet(false, true);
                long nanosPerTick = ServerRateLimitAssertion.NANOS_PER_SECOND / 700;

                for (int i = 0; i < 1000; ++i) {
                    attemptCount.incrementAndGet();
                    if (AssertionStatus.NONE.equals(ass.checkRequest(makeContext())))
                        successCount.incrementAndGet();
                    attemptCount.incrementAndGet();
                    if (AssertionStatus.NONE.equals(ass.checkRequest(makeContext())))
                        successCount.incrementAndGet();
                    if (isTicker)
                        clock.advanceByNanos(nanosPerTick);
                    attemptCount.incrementAndGet();
                    if (AssertionStatus.NONE.equals(ass.checkRequest(makeContext())))
                        successCount.incrementAndGet();
                    attemptCount.incrementAndGet();
                    if (AssertionStatus.NONE.equals(ass.checkRequest(makeContext())))
                        successCount.incrementAndGet();
                    attemptCount.incrementAndGet();
                    if (AssertionStatus.NONE.equals(ass.checkRequest(makeContext())))
                        successCount.incrementAndGet();
                    if (isTicker)
                        clock.advanceByNanos(nanosPerTick);
                }
            }
        };


        BenchmarkRunner br = new BenchmarkRunner(r, 25, 5, "ratebench");
        br.run();

        System.out.println("Total requests attempted: " + attemptCount.get());
        int successes = successCount.get();
        System.out.println("Total requests succeeded: " + successes);
        double seconds = ((double)(clock.nanoTime() - origNanoTime)) / ServerRateLimitAssertion.NANOS_PER_SECOND;
        System.out.println("Total time spent (virtual): " + seconds + " sec");
        double effectiveReqPerSec = successes / seconds;
        System.out.println("Effective requests per second: " + effectiveReqPerSec);
        System.out.println("Expected requests per second limit: " + maxReqPerSec);
        assertTrue("Actual effective successful requests per second should be within 20% of rate limit",
                Math.abs(maxReqPerSec - effectiveReqPerSec) < maxReqPerSec * 0.2); // Permitted to be off by up to 20% only

    }

    @Test
    public void testSimpleRateLimit() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("SimpleRateLimit");
        rla.setMaxRequestsPerSecond(3);
        ServerAssertion ass = makePolicy(rla);

        clock.sync();
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        clock.advanceByMillis(1001);
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
    }

    @Test
    public void testSimpleRateLimitWithSleep() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setCounterName("testSimpleRateLimitWithSleep");
        int rps = 10;
        rla.setMaxRequestsPerSecond(rps);
        rla.setShapeRequests(true);
        final ServerAssertion ass = makePolicy(rla);
        final int nreq = rps * 3;
        int timeoutSec = nreq / rps + 1;
        long timeoutMillis = timeoutSec * 1000L;
        String desc = "send " + nreq + " requests through shaping rate limit of " + rps + " req/sec in under " + timeoutSec + " sec";

        sleepHandler.set(new SingleThreadedInstantSleepSimulator());

        final boolean[] finished = { false };
        try {
            TimeoutExecutor.runWithTimeout(new Timer(), timeoutMillis, new Callable<Object>() {
                public Object call() throws Exception {
                    for (int i = 0; i < nreq; ++i)
                        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
                    finished[0] = true;
                    return null;
                }
            });
            // Ok
        } catch (InvocationTargetException e) {
            final String msg = "Exception (timeout?) while trying to " + desc;
            log.log(Level.WARNING, msg, e);
            fail(msg);
        }
        assertTrue("Success: " + desc, finished[0]);
    }

    @Test
    public void testHighRateLimitNoSleepSingleThreadNanos() throws Exception {
        ServerRateLimitAssertion.useNanos = true;

        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setCounterName("testHighRateLimitNoSleepSingleThread");
        int rps = 80220369; // 1 higher than highest safe value of 80220368
        rla.setMaxRequestsPerSecond(rps);
        rla.setShapeRequests(false);
        rla.setHardLimit(false);
        final ServerAssertion ass = makePolicy(rla);
        final int nreq = 5000;
        String desc = "send " + nreq + " requests through non-shaping rate limit of " + rps + " req/sec";

        sleepHandler.set(new FailingSleepHandler("supposed to be shapeRequests=false"));
        clock.sync();
        for (int i = 0; i < nreq; ++i) {
            final PolicyEnforcementContext context = makeContext();
            try {
                assertEquals("Limit should have succeeded (Iteration " + i + ")", AssertionStatus.NONE, ass.checkRequest(context));
                if (i % 11 == 0)
                    clock.advanceByNanos(3416L);
                if (i % 3000 == 0)
                    log.info("Request " + i + " of " + nreq);
            } finally {
                context.close();
            }
        }

        log.info("Success: " + desc);
    }

    @Test
    public void testHighRateLimitNoSleepSingleThreadMillis() throws Exception {
        ServerRateLimitAssertion.useNanos = false;

        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setCounterName("testHighRateLimitNoSleepSingleThread");
        int rps = 80220369; // 1 higher than highest safe value of 80220368
        rla.setMaxRequestsPerSecond(rps);
        rla.setShapeRequests(false);
        rla.setHardLimit(false);
        final ServerAssertion ass = makePolicy(rla);
        final int nreq = 5000;
        String desc = "send " + nreq + " requests through non-shaping rate limit of " + rps + " req/sec";

        sleepHandler.set(new FailingSleepHandler("supposed to be shapeRequests=false"));
        clock.sync();
        for (int i = 0; i < nreq; ++i) {
            final PolicyEnforcementContext context = makeContext();
            try {
                assertEquals(AssertionStatus.NONE, ass.checkRequest(context));
                if (i % 11 == 0)
                    clock.advanceByNanos(3416L);
                if (i % 3000 == 0)
                    log.info("Request " + i + " of " + nreq);
            } finally {
                context.close();
            }
        }

        log.info("Success: " + desc);
    }

    @Test
    public void testHighRateLimitNoSleepMultiThread() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setCounterName("testHighRateLimitNoSleepMultiThread");
        int rps = 90000;
        rla.setMaxRequestsPerSecond(rps);
        rla.setShapeRequests(false);
        rla.setHardLimit(false);
        final ServerAssertion ass = makePolicy(rla);
        final int nreq = rps / 120;
        String desc = "send " + nreq + " requests through non-shaping rate limit of " + rps + " req/sec";

        sleepHandler.set(new FailingSleepHandler("supposed to be shapeRequests=false"));

        clock.sync();
        BenchmarkRunner bench = new BenchmarkRunner(new Runnable() {
            public void doRun() throws Exception {
                for (int i = 0; i < nreq; ++i) {
                    final PolicyEnforcementContext context = makeContext();
                    try {
                        assertEquals(AssertionStatus.NONE, ass.checkRequest(context));
                        if (i % 77 == 0)
                            clock.advanceByNanos(243000L);
                        if (i % 3000 == 0)
                            log.info("Request " + i + " of " + nreq);
                    } finally {
                        context.close();
                    }
                }
            }

            public void run() {
                try {
                    doRun();
                } catch (Exception e) {
                    final String msg = "Exception in test: " + ExceptionUtils.getMessage(e);
                    log.log(Level.WARNING, msg, e);
                    fail(msg);
                }
            }
        }, 10, "testHighRateLimitNoSleep");
        bench.setThreadCount(10);
        bench.run();

        clock.advanceByMillis(6000L);
        BenchmarkRunner bench2 = new BenchmarkRunner(new Runnable() {
            public void doRun() throws Exception {
                for (int i = 0; i < nreq; ++i) {
                    final PolicyEnforcementContext context = makeContext();
                    try {
                        assertEquals(AssertionStatus.NONE, ass.checkRequest(context));
                        if (i % 77 == 0)
                            clock.advanceByNanos(243000L);
                        if (i % 3000 == 0)
                            log.info("Request " + i + " of " + nreq);
                    } finally {
                        context.close();
                    }
                }
            }

            public void run() {
                try {
                    doRun();
                } catch (Exception e) {
                    final String msg = "Exception in test: " + ExceptionUtils.getMessage(e);
                    log.log(Level.WARNING, msg, e);
                    fail(msg);
                }
            }
        }, 10, "testHighRateLimitNoSleep");
        bench2.setThreadCount(10);
        bench2.run();

        log.info("Success: " + desc);
    }

    @Test
    public void test90kLimitSequentialNanos() throws Exception {
        ServerRateLimitAssertion.useNanos = true;

        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setCounterName("test90kLimitSequentialNanos");
        rla.setHardLimit(true);
        rla.setMaxConcurrency(0);
        rla.setShapeRequests(false);
        rla.setMaxRequestsPerSecond(90000);

        ServerAssertion ass = makePolicy(rla);
        PolicyEnforcementContext context;
        sleepHandler.set(new FailingSleepHandler("supposed to be shapeRequests=false"));
        clock.sync();

        context = makeContext();
        assertEquals(AssertionStatus.NONE, ass.checkRequest(context));
        context.close();

        clock.advanceByMillis(5000L);

        context = makeContext();
        assertEquals(AssertionStatus.NONE, ass.checkRequest(context));
        context.close();
    }

    class Treq implements Callable<AssertionStatus> {

        private final ServerAssertion ass;
        private final String name;
        private final CyclicBarrier beforeBarrier;
        private final CyclicBarrier afterBarrier;
        private PolicyEnforcementContext context = null;

        private Treq(ServerAssertion ass, String name, CyclicBarrier beforeBarrier, CyclicBarrier afterBarrier) {
            if (name == null || ass == null) throw new NullPointerException();
            this.ass = ass;
            this.name = name;
            this.beforeBarrier = beforeBarrier;
            this.afterBarrier = afterBarrier;
        }

        public AssertionStatus call() throws Exception {
            Thread.currentThread().setName("treq " + name);
            closeContext();
            try {
                running.put(this, Boolean.TRUE);
                context = makeContext();
                if (beforeBarrier != null) beforeBarrier.await(30, SECONDS);
                AssertionStatus result = ass.checkRequest(context);
                log.info("treq " + name + " got result " + result);
                if (afterBarrier != null) afterBarrier.await(30, SECONDS);
                return result;
            } catch (Exception e) {
                log.log(Level.WARNING, "Treq exception", e);
                throw e;
            } finally {
                closeContext();
                running.remove(this);
            }
        }

        private void closeContext() {
            if (context != null) {
                context.close();
                context = null;
            }
        }

        protected void finalize() throws Throwable {
            super.finalize();
            closeContext();
        }
    }

    public List<Callable<AssertionStatus>> makeRequests(int num, ServerAssertion sass, String namePrefix, CyclicBarrier beforeBarrier, CyclicBarrier afterBarrier) {
        List<Callable<AssertionStatus>> ret = new ArrayList<Callable<AssertionStatus>>(num);
        for (int i = 0; i < num; ++i)
            ret.add(new Treq(sass, namePrefix + i, beforeBarrier, afterBarrier));
        return ret;
    }

    @Test
    public void testSleepLimit111() throws Exception {
        testSleepLimit(1, 1, 1);
    }

    @Test
    public void testSleepLimit222() throws Exception {
        testSleepLimit(2, 2, 2);
    }

    @Test
    public void testSleepLimit333() throws Exception {
        testSleepLimit(3, 3, 3);
    }

    @Test
    public void testSleepLimit444() throws Exception {
        testSleepLimit(4, 4, 4);
    }

    @Test
    public void testSleepLimit490() throws Exception {
        testSleepLimit(4, 9, 0);
    }

    @Test
    public void testSleepLimit491() throws Exception {
        testSleepLimit(4, 9, 1);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public void testSleepLimit(int maxReqPerSecond, int maxNodeConcurrency, int postFailures) throws Exception {
        int nThreads = maxReqPerSecond + maxNodeConcurrency + postFailures;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        serverConfig.putProperty(RateLimitAssertion.PARAM_MAX_QUEUED_THREADS, String.valueOf(maxNodeConcurrency));
        ServerRateLimitAssertion.maxSleepThreads.set(maxNodeConcurrency);

        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("testSleepLimit_" + maxReqPerSecond + "_" + maxNodeConcurrency + "_" + postFailures);
        rla.setMaxRequestsPerSecond(maxReqPerSecond);
        rla.setMaxConcurrency(0);
        rla.setShapeRequests(true);
        ServerAssertion ass = makePolicy(rla);

        // Prime server assertion with lots of idle time
        clock.sync();
        sleepHandler.set(new FailingSleepHandler("during primer"));
        new Treq(ass, "primer", null, null).call();
        clock.advanceByMillis(30000L);


        final int numInitial = maxReqPerSecond;
        final int numSleepers = maxNodeConcurrency;
        final int numFailures = postFailures;

        // Under rate limit -- will succeed immediately
        List<Callable<AssertionStatus>> initialRequests = makeRequests(numInitial, ass, "Underlimit", null, null);

        // Over rate limit but within sleep limit -- will be delayed but will eventually succeed
        CyclicBarrier beforeSleepBarrier = new CyclicBarrier(numSleepers);
        List<Callable<AssertionStatus>> delayedRequests = makeRequests(numSleepers, ass, "Sleeper", beforeSleepBarrier, null);

        // Over rate limit and over sleep limit -- will fail immediately
        List<Callable<AssertionStatus>> failRequests = makeRequests(numFailures, ass, "Failer", null, null);

        //
        // Initial requests should all succeed immediately
        //
        sleepHandler.set(new FailingSleepHandler("during initial requests"));
        List<Future<AssertionStatus>> initialResults = submitAll(executor, initialRequests);
        joinAll(initialResults);
        assertTrue(isAllDone(initialResults));
        assertTrue(isAllEquals(initialResults, AssertionStatus.NONE));

        //
        // Next batch of requests should all be put to sleep immediately
        //
        final Semaphore beginSleep = new Semaphore(-1);
        final Semaphore endSleep = new Semaphore(-1000);
        sleepHandler.set(new SleepHandler() {
            protected void onSleep(long sleepMillis, int nanos) throws Exception {
                beginSleep.release(2);
                endSleep.acquire();
            }
        });
        List<Future<AssertionStatus>> delayedResults = submitAll(executor, delayedRequests);
        beginSleep.acquire(); // Wait for first sleeper to start sleeping
        Thread.sleep(10L * (numSleepers + 1)); // Give all sleepers time to get blocked up
        assertTrue(isAllNotDone(delayedResults));

        //
        // Final batch of requests should all fail immediately, since the sleepers are blocked and
        // are sitting at the limit for rate limiter's sleeping thread quota
        //
        List<Future<AssertionStatus>> failResults = submitAll(executor, failRequests);
        joinAll(failResults);
        assertTrue(isAllEquals(failResults, AssertionStatus.SERVICE_UNAVAILABLE));

        // ..but the ones that got delayed eventually succeeded
        for (int i = 0; i < (numSleepers + 1) * 2; i++) {
            clock.advanceByMillis(500);
            endSleep.release(1000); // wakey wakey
            Thread.sleep(5L);
        }

        assertTrue(isAllEquals(delayedResults, AssertionStatus.NONE));
    }

    private List<Future<AssertionStatus>> submitAll(ExecutorService es, List<Callable<AssertionStatus>> tasks) {
        List<Future<AssertionStatus>> ret = new ArrayList<Future<AssertionStatus>>();
        for (Callable<AssertionStatus> task : tasks) {
            ret.add(es.submit(task));
        }
        return ret;
    }

    private void joinAll(List<? extends Future> futures) throws ExecutionException, InterruptedException, TimeoutException {
        for (Future future : futures)
            future.get(30, SECONDS);
    }

    private boolean isAllDone(List<Future<AssertionStatus>> futures) {
        for (Future<AssertionStatus> future : futures)
            if (!future.isDone())
                return false;
        return true;
    }

    private boolean isAllNotDone(List<Future<AssertionStatus>> futures) {
        for (Future<AssertionStatus> future : futures)
            if (future.isDone())
                return false;
        return true;
    }

    private boolean isAllEquals(List<Future<AssertionStatus>> futures, AssertionStatus want) throws ExecutionException, InterruptedException, TimeoutException {
        for (Future<AssertionStatus> future : futures)
            if (!want.equals(future.get(30, SECONDS)))
                return false;
        return true;
    }
}