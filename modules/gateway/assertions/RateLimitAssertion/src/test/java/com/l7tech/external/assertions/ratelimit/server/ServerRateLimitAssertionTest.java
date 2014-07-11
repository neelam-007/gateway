package com.l7tech.external.assertions.ratelimit.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.external.assertions.ratelimit.RateLimitAssertion;
import com.l7tech.external.assertions.ratelimit.RateLimitQueryAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ClusterInfoManagerStub;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.test.BenchmarkRunner;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.Assert.*;

/**
 * Test the RateLimitAssertion.
 */
public class ServerRateLimitAssertionTest {
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
        serverConfig = applicationContext.getBean("serverConfig", ServerConfigStub.class);
        serverConfig.putProperty(RateLimitAssertion.PARAM_CLEANER_PERIOD, String.valueOf(86400L * 1000L));

        // Make test use our fake time source
        ServerRateLimitAssertion.clock = clock;
        ServerRateLimitAssertion.useNanos = true;
        ServerRateLimitAssertion.autoFallbackFromNanos = false;
        sleepHandler.set(new FailingSleepHandler("no sleep handler configured for current test"));

        // Turn off auditing for limit exceeded, to avoid flooding the TeamCity test server log and eating time
        ServerRateLimitAssertion.auditLimitExceeded = false;
    }

    private PolicyEnforcementContext makeContext() throws Exception {
        Message request = new Message(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT));
        Message response = new Message();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        context.setVariable("one", "1");
        context.setVariable("five", "5");
        context.setVariable("ten", "10");
        context.setVariable("tenthousand", "10000");
        context.setVariable("negone", "-1");
        context.setVariable("zero", "0");
        context.setVariable("junk", "asdfasdf");
        return context;
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
        rla.setMaxConcurrency("${ten}");
        rla.setMaxRequestsPerSecond("${tenThousand}");
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

    /**
     * Ensure if logOnly is true, assertion should only log that the rate was exceeded and not fail.
     */
    @Test
    @BugNumber(10495)
    public void testLogOnly() throws Exception{
        doLogOnlyTest(false);
    }

    /**
     * If logOnly and shapeRequests are both true, logOnly takes precedence.
     */
    @Test
    @BugNumber(10495)
    public void testLogOnlySupersedesShapeRequest() throws Exception{
        doLogOnlyTest(true);
    }

    private void doLogOnlyTest(final boolean shapeRequests) throws Exception {
        ServerRateLimitAssertion.auditLimitExceeded = true;
        final RateLimitAssertion rla = new RateLimitAssertion();
        rla.setMaxRequestsPerSecond("1");
        rla.setLogOnly(true);
        rla.setShapeRequests(shapeRequests);
        final ServerAssertion serverAssertion = makePolicy(rla);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject( serverAssertion, Collections.singletonMap( "auditFactory", testAudit.factory() ) );

        clock.sync();
        for (int i = 0; i < 5; ++i) {
            assertEquals( AssertionStatus.NONE, serverAssertion.checkRequest( makeContext() ) );
            assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(makeContext()));
            assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(makeContext()));
            assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(makeContext()));
            assertTrue( testAudit.isAuditPresent( AssertionMessages.RATELIMIT_RATE_EXCEEDED ) );
            clock.advanceByNanos(ServerRateLimitAssertion.NANOS_PER_SECOND / 5);
            testAudit.reset();
        }
    }

    @Test
    @BugNumber(9997)
    public void testRateLimitQuerySimple() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setMaxRequestsPerSecond("25");
        rla.setShapeRequests(false);
        rla.setHardLimit(true);
        rla.setCounterName("testRateLimitQuerySimple-5252421246");
        rla.setMaxConcurrency("0");
        ServerAssertion ass = makePolicy(rla);

        RateLimitQueryAssertion rlqa = new RateLimitQueryAssertion();
        rlqa.setCounterName(rla.getCounterName());
        ServerAssertion qass = new ServerRateLimitQueryAssertion(rlqa);

        PolicyEnforcementContext context = makeContext();
        assertEquals(AssertionStatus.NONE, qass.checkRequest(context));

        clock.sync();
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));

        context = makeContext();
        assertEquals(AssertionStatus.NONE, qass.checkRequest(context));
        assertEquals("0", context.getVariable("counter.requestsremaining").toString());

        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));

        context = makeContext();
        assertEquals(AssertionStatus.NONE, qass.checkRequest(context));
        assertEquals("0", context.getVariable("counter.requestsremaining").toString());

        clock.advanceByNanos(ServerRateLimitAssertion.NANOS_PER_SECOND / 100);

        context = makeContext();
        assertEquals(AssertionStatus.NONE, qass.checkRequest(context));
        assertEquals("0", context.getVariable("counter.requestsremaining").toString());

        clock.advanceByNanos(ServerRateLimitAssertion.NANOS_PER_SECOND / 100);

        clock.advanceByNanos(ServerRateLimitAssertion.NANOS_PER_SECOND / 100);

        clock.advanceByNanos(ServerRateLimitAssertion.NANOS_PER_SECOND / 100);

        context = makeContext();
        assertEquals(AssertionStatus.NONE, qass.checkRequest(context));
        assertEquals("1", context.getVariable("counter.requestsremaining").toString());

        // Should be maxed out now -- test by waiting another second
        clock.advanceByNanos(ServerRateLimitAssertion.NANOS_PER_SECOND);

        context = makeContext();
        assertEquals(AssertionStatus.NONE, qass.checkRequest(context));
        assertEquals("1", context.getVariable("counter.requestsremaining").toString());
    }

    @Test
    @BugNumber(8090)
    public void testSimpleFailLimitNoSleepNoBurst() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setMaxRequestsPerSecond("25");
        rla.setShapeRequests(false);
        rla.setHardLimit(true);
        rla.setCounterName("MyTestCounter-23423");
        rla.setMaxConcurrency("0");
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
        rla.setMaxRequestsPerSecond("25");
        rla.setShapeRequests(false);
        rla.setHardLimit(true);
        rla.setCounterName("MyTestCounter-211-43636536");
        rla.setMaxConcurrency("0");
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
                rla.setMaxRequestsPerSecond(String.valueOf(maxReqPerSec));
                rla.setShapeRequests(false);
                rla.setHardLimit(true);
                rla.setCounterName("MyTestCounter-44-2923847");
                rla.setMaxConcurrency("0");
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
        rla.setMaxRequestsPerSecond("3");
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
    public void testBadContextVariable_rate() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("testBadContextVariable_rate");
        rla.setMaxRequestsPerSecond("${negone}");
        ServerAssertion ass = makePolicy(rla);

        clock.sync();
        assertEquals(AssertionStatus.SERVER_ERROR, checkRequest(ass, makeContext()));
    }

    @Test
    @BugNumber(10688)
    public void testBadContextVariable_rate_zero() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("testBadContextVariable_rate");
        rla.setMaxRequestsPerSecond("${zero}");
        ServerAssertion ass = makePolicy(rla);

        clock.sync();
        assertEquals(AssertionStatus.SERVER_ERROR, checkRequest(ass, makeContext()));
    }

    @Test
    public void testBadContextVariable_rate_notlong() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("testBadContextVariable_rate_notlong");
        rla.setMaxRequestsPerSecond("${junk}");
        ServerAssertion ass = makePolicy(rla);

        clock.sync();
        assertEquals(AssertionStatus.SERVER_ERROR, checkRequest(ass, makeContext()));
    }

    @Test
    public void testBadContextVariable_window() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("testBadContextVariable_window");
        rla.setMaxRequestsPerSecond("1");
        rla.setWindowSizeInSeconds("${negone}");
        ServerAssertion ass = makePolicy(rla);

        clock.sync();
        assertEquals(AssertionStatus.SERVER_ERROR, checkRequest(ass, makeContext()));
    }

    @Test
    public void testBadContextVariable_concurrency() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("testBadContextVariable_concurrency");
        rla.setMaxRequestsPerSecond("1");
        rla.setMaxConcurrency("${negone}");
        ServerAssertion ass = makePolicy(rla);

        clock.sync();
        assertEquals(AssertionStatus.SERVER_ERROR, checkRequest(ass, makeContext()));
    }

    @Test
    public void testBadContextVariable_blackout() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("testBadContextVariable_blackout");
        rla.setMaxRequestsPerSecond("1");
        rla.setBlackoutPeriodInSeconds("${negone}");
        ServerAssertion ass = makePolicy(rla);

        clock.sync();
        assertEquals(AssertionStatus.NONE, checkRequest(ass, makeContext()));
        assertEquals(AssertionStatus.SERVER_ERROR, checkRequest(ass, makeContext()));
    }


    @Test
    public void testSimpleRateLimitWithBlackout() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("SimpleRateLimitWithBlackout");
        rla.setMaxRequestsPerSecond("3");
        rla.setBlackoutPeriodInSeconds("${five}");
        ServerAssertion ass = makePolicy(rla);

        clock.sync();
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        clock.advanceByMillis(1001);
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        clock.advanceByMillis(4000);
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
    }

    @Test
    public void testSimpleRateLimitWithLargerWindow() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setWindowSizeInSeconds("10");
        rla.setCounterName("SimpleRateLimitWithLargerWindow");
        rla.setMaxRequestsPerSecond("3");
        ServerAssertion ass = makePolicy(rla);

        clock.sync();
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext())); // Initial request to create the counter

        clock.advanceByMillis(10001L); // Charge up full 10 seconds worth of idle points
        for (int i = 0; i < 30; ++i)
            assertEquals("round " + i, AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));

        clock.advanceByMillis(10001L);
        for (int i = 0; i < 30; ++i)
            assertEquals("round " + i, AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));

        clock.advanceByMillis(5001L); // Charge up half the max idle points
        for (int i = 0; i < 15; ++i)
            assertEquals("round " + i, AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));

        clock.advanceByMillis(50000L); // Idle for longer than the max, to ensure it cuts off at the max
        for (int i = 0; i < 30; ++i)
            assertEquals("round " + i, AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));

    }

    @Test
    public void testSimpleRateLimitWithSleep() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setCounterName("testSimpleRateLimitWithSleep");
        int rps = 10;
        rla.setMaxRequestsPerSecond(String.valueOf(rps));
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
    public void testHighRateLimitNoSleepMultiThread() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setCounterName("testHighRateLimitNoSleepMultiThread");
        int rps = 90000;
        rla.setMaxRequestsPerSecond(String.valueOf(rps));
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
        rla.setMaxConcurrency("0");
        rla.setShapeRequests(false);
        rla.setMaxRequestsPerSecond("90000");

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
        rla.setMaxRequestsPerSecond(String.valueOf(maxReqPerSecond));
        rla.setMaxConcurrency("0");
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

    /**
     * Test that pre bug fix 5041 policies can be successfull and correctly parsed
     * @throws Exception
     */
    @Test
    @BugNumber(5041)
    public void testRateLimitOldSupport() throws Exception{
        //this xml uses an intValue property for MaxConcurrency and MaxRequestsPerSecond, which has been changed
        //to a String. An int setter is provided for the backwards compatabilityss
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "   <L7p:RateLimit>\n" +
                "      <L7p:CounterName stringValue=\"PRESET(bfff418adf8bb0a5)${request.clientid}\"/>\n" +
                "      <L7p:MaxConcurrency intValue=\"23\"/>\n" +
                "      <L7p:MaxRequestsPerSecond intValue=\"10076\"/>\n" +
                "   </L7p:RateLimit>\n" +
                "</wsp:Policy>";


        AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        WspReader wspReader = new WspReader(tmf);
        tmf.registerAssertion(RateLimitAssertion.class);

        final RateLimitAssertion rla = (RateLimitAssertion) wspReader.parseStrictly(xml, WspReader.INCLUDE_DISABLED);
        assertTrue("Invalid maxConcurrency value expected '23' got '" + rla.getMaxConcurrency()+"'", rla.getMaxConcurrency().equals("23"));
        assertTrue("Invalid maxRequestsPerSecond expected '10076' got '" + rla.getMaxRequestsPerSecond()+"'", rla.getMaxRequestsPerSecond().equals("10076"));
    }

    @Test
    @BugNumber(9924)
    public void testSplitLimitsAcrossCluster() throws Exception {
        testSplit("200", true, "10", true, 2, 3);
        testSplit("200", true, "10", true, 3, 3);
        testSplit("200", true, "10", true, 2, 2);
        testSplit("200", false, "10", true, 2, 3);
        testSplit("200", true, "10", false, 2, 3);
        testSplit("10", false, "5", false, 2, 3);
        testSplit("1", true, "1", true, 10, 20);
        testSplit("1", true, "1", true, 1, 1);
        testSplit("150", true, "20", true, 1, 1);
    }

    private void testSplit(String maxRate, boolean splitRate, String maxConc, boolean splitConc, int upnodes, int totalnodes) throws Exception {
        RateLimitAssertion ass = new RateLimitAssertion();
        ass.setCounterName(String.format("testSplitLimitsAcrossCluster-%s-%b-%s-%b-%d-%d", maxRate, splitRate, maxConc, splitConc, upnodes, totalnodes));
        ass.setMaxConcurrency(maxConc);
        ass.setSplitConcurrencyLimitAcrossNodes(splitConc);
        ass.setMaxRequestsPerSecond(maxRate);
        ass.setSplitRateLimitAcrossNodes(splitRate);

        long expectedConc = splitConc ? (Long.valueOf(maxConc) / upnodes) : Long.valueOf(maxConc);
        long expectedRate = splitRate ? (Long.valueOf(maxRate) / upnodes) : Long.valueOf(maxRate);

        setClusterSize(upnodes, totalnodes);
        ServerRateLimitAssertion sass = (ServerRateLimitAssertion) makePolicy(ass);
        assertEquals("Max concurrency should be " + expectedConc + " for "  + ass.getCounterName(),
                expectedConc, sass.findMaxConcurrency(makeContext()));
        assertEquals("Max rate should be " + expectedRate + " for" + ass.getCounterName(),
                expectedRate, sass.findPointsPerSecond(makeContext()).divide(ServerRateLimitAssertion.POINTS_PER_REQUEST).longValue());
    }

    private void setClusterSize(int upnodes, int totalnodes) {
        ClusterInfoManagerStub clusterInfoManager = applicationContext.getBean("clusterInfoManager", ClusterInfoManagerStub.class);
        ArrayList<ClusterNodeInfo> nodes = new ArrayList<ClusterNodeInfo>();
        nodes.add(clusterInfoManager.getSelfNodeInf());
        upnodes--;  // always count ourself as an up node
        for (int i = 1; i < totalnodes; ++i) {
            final ClusterNodeInfo info = new ClusterNodeInfo();
            info.setName("Fake node #" + i);
            info.setNodeIdentifier("fakenodeid" + i);
            info.setLastUpdateTimeStamp(upnodes-- > 0 ? clock.currentTimeMillis() : 0);
            nodes.add(info);
        }
        clusterInfoManager.setClusterStatus(nodes);
        ServerRateLimitAssertion.lastClusterCheck.set(0);
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

    private AssertionStatus checkRequest(ServerAssertion ass, PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            return ass.checkRequest(context);
        } catch (AssertionStatusException e) {
            assertTrue("AssertionStatusException shall not be used to report successful execution", !AssertionStatus.NONE.equals(e.getAssertionStatus()));
            return e.getAssertionStatus();
        }
    }
}
