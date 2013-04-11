package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.transport.jms2.JmsConnectionManagerStub;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.message.Message;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.common.io.XmlUtil;
import junit.framework.Assert;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;

/**
 *
 * TODO: Need to add tests while waiting for responses (w/ tempQueues & reply-to-queue)
 *
 * @author: vchan
 */
@Ignore("These tests require a JMS queue")
public class ServerJmsRoutingAssertionTest {

    static {
        // configure the stub connection manager for the test
        JmsConnectionManagerStub.setTestConfig(JmsConnectionManagerStub.TEST_CONFIG_AMQ_OUT); // apache activeMQ
//        JmsConnectionManagerStub.setTestConfig(JmsConnectionManagerStub.TEST_CONFIG_MQS_OUT); // ibm MQSeries
    }

    private ApplicationContext appCtx;
    private WspReader policyReader;
    private ServerPolicyFactory factory;
    private String testPolicy;

    // test policy data
    static final String TEST_POLICY_AMQ =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:JmsRoutingAssertion>\n" +
            "            <L7p:EndpointName stringValue=\"dynamicQueues/JMS.JUNIT.OUT.Q\"/>\n" +
            "            <L7p:EndpointOid boxedLongValue=\"102\"/>\n" +
            "            <L7p:RequestJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
            "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
            "            </L7p:RequestJmsMessagePropertyRuleSet>\n" +
            "            <L7p:ResponseJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
            "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
            "            </L7p:ResponseJmsMessagePropertyRuleSet>\n" +
            "        </L7p:JmsRoutingAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    static final String TEST_POLICY_MQSeries =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "       <L7p:JmsRoutingAssertion>\n" +
            "            <L7p:EndpointName stringValue=\"cn=VCTEST.Q.OUT\"/>\n" +
            "            <L7p:EndpointOid boxedLongValue=\"104\"/>\n" +
            "            <L7p:RequestJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
            "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
            "            </L7p:RequestJmsMessagePropertyRuleSet>\n" +
            "            <L7p:ResponseJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
            "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
            "            </L7p:ResponseJmsMessagePropertyRuleSet>\n" +
            "       </L7p:JmsRoutingAssertion>" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    static final String TEST_POLICY_FMQ =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "       <L7p:JmsRoutingAssertion>\n" +
            "            <L7p:EndpointName stringValue=\"vchan_out\"/>\n" +
            "            <L7p:EndpointOid boxedLongValue=\"106\"/>\n" +
            "            <L7p:RequestJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
            "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
            "            </L7p:RequestJmsMessagePropertyRuleSet>\n" +
            "            <L7p:ResponseJmsMessagePropertyRuleSet jmsMessagePropertyRuleSet=\"included\">\n" +
            "                <L7p:Rules jmsMessagePropertyRuleArray=\"included\"/>\n" +
            "            </L7p:ResponseJmsMessagePropertyRuleSet>\n" +
            "       </L7p:JmsRoutingAssertion>" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    // test messages
    static final String TEST_MSG_VALID = "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:xmltoday-delayed-quotes\">\n"+
            "   <soapenv:Header/>\n"+
            "   <soapenv:Body>\n"+
            "      <urn:getQuote soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"+
            "         <symbol xsi:type=\"xsd:string\">SUNW</symbol>\n"+
            "      </urn:getQuote>\n"+
            "   </soapenv:Body>\n"+
            "</soapenv:Envelope>";

    @Before
    public void setUp() throws Exception {

        // get the spring app context
        if (appCtx == null) {
            appCtx = ApplicationContexts.getTestApplicationContext();
            Assert.assertNotNull("Fail - Unable to get applicationContext instance", appCtx);
        }

        // grab the wspReader bean from spring
        if (policyReader == null) {
            policyReader = appCtx.getBean("wspReader", WspReader.class);
            Assert.assertNotNull("Fail - Unable to obtain the WspReader bean from the application context.", policyReader);
        }

        // grab the policyFactory
        if (factory == null) {
            factory = appCtx.getBean("policyFactory", ServerPolicyFactory.class);
            Assert.assertNotNull("Fail - Unable to obtain \"policyFactory\" from the application context", factory);
        }

        // configure the stub connection manager for this testcase
//        JmsConnectionManagerStub.setTestConfig(JmsConnectionManagerStub.TEST_CONFIG_MQS_OUT);
        // set the policy for the test
        if (testPolicy == null) {
            testPolicy = TEST_POLICY_AMQ;
//            testPolicy = TEST_POLICY_MQSeries;
//            testPolicy = TEST_POLICY_FMQ;
        }

    }

    /**
     *
     */
    @Ignore("This test requires a JMS queue")
    @Test
    public void testSingleMessage() {

        ServerJmsRoutingAssertion as;
        PolicyEnforcementContext peCtx;
        try {
            as = getAssertion(testPolicy);
            peCtx = createPEContext(TEST_MSG_VALID);

            // gimme time to start the monitor
//            try {
//                Thread.sleep(25000L);
//            } catch(InterruptedException ie) {}


            // run check first time
            AssertionStatus status = as.checkRequest(peCtx);
            Assert.assertNotNull(status);
            Assert.assertEquals("First check, no errors expected", "No Error", status.getMessage());

            // run same check again
            status = as.checkRequest(peCtx);
            Assert.assertNotNull(status);
            Assert.assertEquals("Second check, no errors expected", "No Error", status.getMessage());

        } catch (Exception ex) {
            Assert.fail("Unexpected failure during test (testOne). " + ex);
        }
    }


    @Ignore("This test requires a JMS queue")
    @Test
    public void testConcurrent2T() {

        ServerJmsRoutingAssertion as;
        PolicyEnforcementContext peCtx;
        ExecutorService exec = null;
        try {
            as = getAssertion(testPolicy);
            peCtx = createPEContext(TEST_MSG_VALID);

            // 2 threads
            ConcurrentTestRunner[] runners = {
                    new ConcurrentTestRunner(as, peCtx),
                    new ConcurrentTestRunner(as, peCtx)
            };

            exec = runTestRunners(runners);

            try {
                Thread.sleep(RUNTIME + 5000L);
            } catch (InterruptedException iex) {} // ignore

            for (ConcurrentTestRunner r : runners) {
                Assert.assertEquals(0, r.failCount);
            }
        } catch (Exception ex) {
            Assert.fail("Unexpected failure during test (testOne). " + ex);
        } finally {
            if (exec != null)
                exec.shutdown();
        }
    }


    public void testConcurrent4T() {

        ServerJmsRoutingAssertion as;
        PolicyEnforcementContext peCtx;
        ExecutorService exec = null;
        try {
            as = getAssertion(testPolicy);
            peCtx = createPEContext(TEST_MSG_VALID);

            // 4 threads
            ConcurrentTestRunner[] runners = {
                    new ConcurrentTestRunner(as, peCtx),
                    new ConcurrentTestRunner(as, peCtx),
                    new ConcurrentTestRunner(as, peCtx),
                    new ConcurrentTestRunner(as, peCtx)
            };

            exec = runTestRunners(runners);

            try {
                Thread.sleep(RUNTIME + 5000L);
            } catch (InterruptedException iex) {} // ignore

            for (ConcurrentTestRunner r : runners) {
                Assert.assertEquals(0, r.failCount);
            }
        } catch (Exception ex) {
            Assert.fail("Unexpected failure during test (testOne). " + ex);
        } finally {
            if (exec != null)
                exec.shutdown();
        }
    }


    private PolicyEnforcementContext createPEContext(String testMessage) throws Exception {

        Message req = new Message( testMessage(testMessage) );
        Message res = new Message();
        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, res);
        peCtx.setService(new PublishedService());
        return peCtx;
    }

    private ServerJmsRoutingAssertion getAssertion(String policyXml)
            throws IOException, ServerPolicyException
    {
        Assertion as = policyReader.parsePermissively(policyXml, WspReader.INCLUDE_DISABLED);
        Assert.assertNotNull(as);
        Assert.assertTrue(as instanceof AllAssertion);
        AllAssertion all = (AllAssertion) as;

        for (Object obj : all.getChildren()) {

            if (obj instanceof JmsRoutingAssertion) {
                return new ServerJmsRoutingAssertion((JmsRoutingAssertion) obj, appCtx);
            }
        }
        return null;
    }

    private Document testMessage(String testMsg) throws IOException, SAXException {
        return XmlUtil.parse( new ByteArrayInputStream(testMsg.getBytes()) );
    }

    private ExecutorService runTestRunners(ConcurrentTestRunner[] runners) {

        ExecutorService exec = java.util.concurrent.Executors.newFixedThreadPool(runners.length);
        for (ConcurrentTestRunner r : runners) {
            exec.execute(r);
        }
        return exec;
    }

    private static final long RUNTIME = 60000L;
    class ConcurrentTestRunner implements Runnable {

        ServerJmsRoutingAssertion sa;
        PolicyEnforcementContext peCtx;
        int failCount;
        Random rand = new Random(System.currentTimeMillis());

        ConcurrentTestRunner(ServerJmsRoutingAssertion sa, PolicyEnforcementContext peCtx) {
            this.sa = sa;
            this.peCtx = peCtx;
        }

        public void run() {

            AssertionStatus status;

            long startTime = System.currentTimeMillis();
            long now = startTime;

            while((now-startTime) < RUNTIME) {

                try {
                    status = sa.checkRequest(peCtx);
                    if (status == null || !"No Error".equals(status.getMessage())) {
                        failCount++;
                    }

                    Thread.sleep(nextWaitInterval());

                } catch (Exception ex) {
                    // ignore
                }

                now = System.currentTimeMillis();
            }
        }

        long nextWaitInterval() {
            return Math.abs((rand.nextLong() % 10L) + 1) * 100L; // between 1/10 - 1 second
        }
    }

}