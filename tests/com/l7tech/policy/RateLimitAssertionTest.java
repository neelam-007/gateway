package com.l7tech.policy;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.util.TimeoutExecutor;
import com.l7tech.common.message.Message;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RateLimitAssertion;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.InvocationTargetException;

/**
 * Test the RateLimitAssertion.
 */
public class RateLimitAssertionTest extends TestCase {
    private static final Logger log = Logger.getLogger(RateLimitAssertionTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerPolicyFactory serverPolicyFactory;
    private static ServerConfigStub serverConfig;

    public RateLimitAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(RateLimitAssertionTest.class);
        TestSetup wrapper = new TestSetup(suite) {

            protected void setUp() throws Exception {
                applicationContext = ApplicationContexts.getTestApplicationContext();
                serverPolicyFactory = (ServerPolicyFactory) applicationContext.getBean("policyFactory", ServerPolicyFactory.class);
                serverConfig = (ServerConfigStub) applicationContext.getBean("serverConfig", ServerConfigStub.class);
            }

            protected void tearDown() throws Exception {
                ;
            }
        };
        return wrapper;
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
    static final ConcurrentHashMap<ReqThread, Object> running = new ConcurrentHashMap<ReqThread, Object>();
    class ReqThread extends Thread {
        private final ServerAssertion ass;
        private AssertionStatus result = null;
        private PolicyEnforcementContext context = null;

        private ReqThread(ServerAssertion ass) {
            this.ass = ass;
            setName("ReqThread" + nextId.getAndIncrement());
            setDaemon(true);
        }

        public void run() {
            closeContext();
            try {
                running.put(this, Boolean.TRUE);
                context = makeContext();
                setResult(ass.checkRequest(context));
            } catch (Exception e) {
                log.log(Level.WARNING, "Exception in test request thread", e);
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

        public synchronized AssertionStatus getResult() {
            return result;
        }

        public synchronized void setResult(AssertionStatus result) {
            this.result = result;
        }

        protected void finalize() throws Throwable {
            super.finalize();
            closeContext();
        }
    }

    // Kill all request threads and wait for them to finish
    private static void killAll() throws InterruptedException {
        Set<ReqThread> threads = new HashSet<ReqThread>(running.keySet());
        for (ReqThread thread : threads)
            thread.interrupt();
    }

    public void testSimpleRateLimit() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setMaxRequestsPerSecond(2);
        ServerAssertion ass = makePolicy(rla);

        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        Thread.sleep(1000);
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
        assertEquals(AssertionStatus.SERVICE_UNAVAILABLE, ass.checkRequest(makeContext()));
    }

    public void testSimpleRateLimitWithSleep() throws Exception {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setMaxRequestsPerSecond(2);
        rla.setShapeRequests(true);
        final ServerAssertion ass = makePolicy(rla);

        final boolean[] finished = { false };
        try {
            TimeoutExecutor.runWithTimeout(new Timer(), 4000, new Callable<Object>() {
                public Object call() throws Exception {
                    assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
                    assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
                    assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
                    assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
                    assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
                    assertEquals(AssertionStatus.NONE, ass.checkRequest(makeContext()));
                    finished[0] = true;
                    return null;
                }
            });
            // Ok
        } catch (InvocationTargetException e) {
            fail("Exception (timeout?) while trying to send 6 requests through a shaping rate limit of 2 req/sec");
        }
        assertTrue("All 6 requests went through in under 4 seconds", finished[0]);
    }

    public void testSleepLimit() throws Exception {
        serverConfig.putProperty(ServerConfig.PARAM_RATELIMIT_MAX_CONCURRENCY, "1");

        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setMaxRequestsPerSecond(1);
        rla.setMaxConcurrency(0);
        ServerAssertion ass = makePolicy(rla);

        ReqThread t1 = new ReqThread(ass);
        ReqThread t2 = new ReqThread(ass);
        ReqThread t3 = new ReqThread(ass);

        // Send a request.  Should run to completion.
        t1.start();

        // Send second request immediately.  Should sleep for at least half a second.
        t2.start();

        // Try to send third request.  Should fail immediately.
        t3.start();

        Thread.sleep(10);
        assertEquals(AssertionStatus.NONE, t1.getResult()); // first one should have succeeded
        assertNull(t2.getResult()); // second one should still be sleeping
        assertEquals(t3.getResult(), AssertionStatus.SERVICE_UNAVAILABLE);  // third one should have failed immediately

    }

}
