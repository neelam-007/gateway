package com.l7tech.server.policy.assertion;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.TimeoutExecutor;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RateLimitAssertion;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test the RateLimitAssertion.
 */
public class ServerRateLimitAssertionTest extends TestCase {
    static {
        System.setProperty("com.l7tech.server.ratelimit.logAtInfo", "true");
    }

    private static final Logger log = Logger.getLogger(ServerRateLimitAssertionTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerPolicyFactory serverPolicyFactory;
    private static ServerConfigStub serverConfig;

    public ServerRateLimitAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(ServerRateLimitAssertionTest.class);
        return new TestSetup(suite) {

            protected void setUp() throws Exception {
                applicationContext = ApplicationContexts.getTestApplicationContext();
                serverPolicyFactory = (ServerPolicyFactory) applicationContext.getBean("policyFactory", ServerPolicyFactory.class);
                serverConfig = (ServerConfigStub) applicationContext.getBean("serverConfig", ServerConfigStub.class);
                serverConfig.putProperty(ServerConfig.PARAM_RATELIMIT_CLEANER_PERIOD, String.valueOf(99999));
            }
        };
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private PolicyEnforcementContext makeContext() throws Exception {
        Message request = new Message(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT));
        Message response = new Message();
        return new PolicyEnforcementContext(request, response);
    }

    private ServerAssertion makePolicy(RateLimitAssertion rla) throws Exception {
        return serverPolicyFactory.compilePolicy(rla, false);
    }

    public void testConcurrencyLimit() throws Exception {
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

    public void testSimpleRateLimit() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("SimpleRateLimit");
        rla.setMaxRequestsPerSecond(3);
        ServerAssertion ass = makePolicy(rla);

        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        Thread.sleep(1001);
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
    }

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

        final boolean[] finished = { false };
        try {
            TimeoutExecutor.runWithTimeout(new Timer(), timeoutMillis, new Callable<Object>() {
                public Object call() throws Exception {
                    for (int i = 0; i < 30; ++i)
                        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
                    finished[0] = true;
                    return null;
                }
            });
            // Ok
        } catch (InvocationTargetException e) {
            fail("Exception (timeout?) while trying to " + desc);
        }
        assertTrue("Success: " + desc, finished[0]);
    }

    class Treq implements Callable<AssertionStatus> {

        private final ServerAssertion ass;
        private final String name;
        private PolicyEnforcementContext context = null;

        private Treq(ServerAssertion ass, String name) {
            this.ass = ass;
            this.name = name;
        }

        public AssertionStatus call() throws Exception {
            Thread.currentThread().setName("treq " + name);
            closeContext();
            try {
                running.put(this, Boolean.TRUE);
                context = makeContext();
                AssertionStatus result = ass.checkRequest(context);
                log.info("treq " + name + " got result " + result);
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

    public List<Callable<AssertionStatus>> makeRequests(int num, ServerAssertion sass, String namePrefix) {
        List<Callable<AssertionStatus>> ret = new ArrayList<Callable<AssertionStatus>>(num);
        for (int i = 0; i < num; ++i)
            ret.add(new Treq(sass, namePrefix + i));
        return ret;
    }

    public void testSleepLimit111() throws Exception {
        testSleepLimit(1, 1, 1);
    }

    public void testSleepLimit222() throws Exception {
        testSleepLimit(2, 2, 2);
    }

    public void testSleepLimit333() throws Exception {
        testSleepLimit(3, 3, 3);
    }

    public void testSleepLimit490() throws Exception {
        testSleepLimit(4, 9, 0);
    }


    public void testSleepLimit(int maxReqPerSecond, int maxNodeConcurrency, int postFailures) throws Exception {
        int nThreads = maxReqPerSecond + maxNodeConcurrency + postFailures;
        ExecutorService es = Executors.newFixedThreadPool(nThreads);

        // Warm up the thread pool in advance
        final long[] a = {0};
        Future[] warmup = new Future[nThreads * 3];
        for (int i = 0; i < warmup.length; ++i) {
            warmup[i] = es.submit(new Runnable() {
                public void run() {
                    a[0] += a[0];
                }
            });
        }
        for (Future future : warmup)
            future.get();

        serverConfig.putProperty(ServerConfig.PARAM_RATELIMIT_MAX_CONCURRENCY, String.valueOf(maxNodeConcurrency));
        ServerRateLimitAssertion.maxSleepThreads.set(maxNodeConcurrency);

        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setHardLimit(false);
        rla.setCounterName("testSleepLimit_" + maxReqPerSecond + "_" + maxNodeConcurrency + "_" + postFailures);
        rla.setMaxRequestsPerSecond(maxReqPerSecond);
        rla.setMaxConcurrency(0);
        rla.setShapeRequests(true);
        ServerAssertion ass = makePolicy(rla);

        // Under rate limit -- will succeed immediately
        List<Callable<AssertionStatus>> tu = makeRequests(maxReqPerSecond, ass, "Underlimit");

        // Over rate limit but within sleep limit -- will be delayed but will eventually succeed
        List<Callable<AssertionStatus>> ts = makeRequests(maxNodeConcurrency, ass, "Sleeper");

        // Over rate limit and over sleep limit -- will fail immediately
        List<Callable<AssertionStatus>> tf = makeRequests(postFailures, ass, "Failer");

        List<Future<AssertionStatus>> fu = submitAll(es, tu);
        Thread.sleep(111);
        assertTrue(isAllDone(fu));
        assertTrue(isAllEquals(fu, AssertionStatus.NONE));

        List<Future<AssertionStatus>> fs = submitAll(es, ts);
        Thread.sleep(20);
        assertTrue(isAllNotDone(fs));

        long before = System.currentTimeMillis();
        List<Future<AssertionStatus>> ff = submitAll(es, tf);

        // next overlimit reqs should fail essentially immediately
        assertTrue(isAllEquals(ff, AssertionStatus.SERVICE_UNAVAILABLE));
        long after = System.currentTimeMillis();
        assertTrue(after - before < 220);

        // ..but the ones that got delayed eventually succeeded
        assertTrue(isAllEquals(fs, AssertionStatus.NONE));
    }

    private List<Future<AssertionStatus>> submitAll(ExecutorService es, List<Callable<AssertionStatus>> tasks) {
        List<Future<AssertionStatus>> ret = new ArrayList<Future<AssertionStatus>>();
        for (Callable<AssertionStatus> task : tasks) {
            ret.add(es.submit(task));
        }
        return ret;
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

    private boolean isAllEquals(List<Future<AssertionStatus>> futures, AssertionStatus want) throws ExecutionException, InterruptedException {
        for (Future<AssertionStatus> future : futures)
            if (!want.equals(future.get()))
                return false;
        return true;
    }
}
