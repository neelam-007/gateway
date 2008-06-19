package com.l7tech.server.policy.assertion;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.LicenseException;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.composite.ServerAllAssertion;
import com.l7tech.server.policy.assertion.jms.JmsResourceManager;
import com.l7tech.server.transport.jms2.JmsConnectionManagerStub;
import com.l7tech.service.PublishedService;
import junit.framework.TestCase;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;

/**
 * @author: vchan
 */
public class ServerJmsRoutingAssertionTest extends TestCase {

    static {
        // configure the stub connection manager for the test
        JmsConnectionManagerStub.setTestConfig(JmsConnectionManagerStub.TEST_CONFIG_MQS_OUT);
    }

    private ApplicationContext appCtx;
    private WspReader policyReader;
    private ServerPolicyFactory factory;

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

    // test messages
    static final String TEST_MSG_VALID = "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:xmltoday-delayed-quotes\">\n"+
            "   <soapenv:Header/>\n"+
            "   <soapenv:Body>\n"+
            "      <urn:getQuote soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"+
            "         <symbol xsi:type=\"xsd:string\">SUNW</symbol>\n"+
            "      </urn:getQuote>\n"+
            "   </soapenv:Body>\n"+
            "</soapenv:Envelope>";

    protected void setUp() throws Exception {

        // get the spring app context
        if (appCtx == null) {
            appCtx = ApplicationContexts.getTestApplicationContext();
            assertNotNull("Fail - Unable to get applicationContext instance", appCtx);
        }

        // grab the wspReader bean from spring
        if (policyReader == null) {
            policyReader = (WspReader) appCtx.getBean("wspReader", WspReader.class);
            assertNotNull("Fail - Unable to obtain the WspReader bean from the application context.", policyReader);
        }

        // grab the policyFactory
        if (factory == null) {
            factory = (ServerPolicyFactory) appCtx.getBean("policyFactory", ServerPolicyFactory.class);
            assertNotNull("Fail - Unable to obtain \"policyFactory\" from the application context", factory);
        }

        // configure the stub connection manager for this testcase
//        JmsConnectionManagerStub.setTestConfig(JmsConnectionManagerStub.TEST_CONFIG_MQS_OUT);

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     *
     */
    public void testSingleMessage() {

        ServerJmsRoutingAssertion as;
        PolicyEnforcementContext peCtx;
        try {
            as = getAssertion(TEST_POLICY_MQSeries);
            peCtx = createPEContext(TEST_MSG_VALID);

            // gimme time to start the monitor
//            try {
//                Thread.sleep(25000L);
//            } catch(InterruptedException ie) {}


            // run check first time
            AssertionStatus status = as.checkRequest(peCtx);
            assertNotNull(status);
            assertEquals("First check, no errors expected", "No Error", status.getMessage());

            // run same check again
            status = as.checkRequest(peCtx);
            assertNotNull(status);
            assertEquals("Second check, no errors expected", "No Error", status.getMessage());

        } catch (Exception ex) {
            fail("Unexpected failure during test (testOne). " + ex);
        }
    }


    public void testConcurrent2T() {

        ServerJmsRoutingAssertion as;
        PolicyEnforcementContext peCtx;
        ExecutorService exec = null;
        try {
            as = getAssertion(TEST_POLICY_MQSeries);
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
                assertEquals(0, r.failCount);
            }
        } catch (Exception ex) {
            fail("Unexpected failure during test (testOne). " + ex);
        } finally {
            if (exec != null)
                exec.shutdown();

            JmsResourceManager.shutdown();
        }
    }


    public void xxtestConcurrent4T() {

        ServerJmsRoutingAssertion as;
        PolicyEnforcementContext peCtx;
        ExecutorService exec = null;
        try {
            as = getAssertion(TEST_POLICY_MQSeries);
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
                assertEquals(0, r.failCount);
            }
        } catch (Exception ex) {
            fail("Unexpected failure during test (testOne). " + ex);
        } finally {
            if (exec != null)
                exec.shutdown();

            JmsResourceManager.shutdown();
        }
    }


    private PolicyEnforcementContext createPEContext(String testMessage) throws Exception {

        Message req = new Message( testMessage(testMessage) );
        Message res = new Message();
        PolicyEnforcementContext peCtx = new PolicyEnforcementContext(req, res);
        peCtx.setService(new PublishedService());
        return peCtx;
    }

    private ServerJmsRoutingAssertion getAssertion(String policyXml)
            throws IOException, ServerPolicyException
    {
        Assertion as = policyReader.parsePermissively(policyXml);
        assertNotNull(as);
        assertTrue(as instanceof AllAssertion);
        AllAssertion all = (AllAssertion) as;

        for (Object obj : all.getChildren()) {

            if (obj instanceof JmsRoutingAssertion) {
                return new ServerJmsRoutingAssertion((JmsRoutingAssertion) obj, appCtx);
            }
        }
        return null;
    }

    private ServerJmsRoutingAssertion parseAssertion(String policyXml) throws IOException {

        Assertion as = policyReader.parsePermissively(policyXml);
        assertNotNull(as);
        assertTrue(as instanceof AllAssertion);

        try {
            ServerAssertion svrAssert = factory.compilePolicy((AllAssertion) as, true);
            assertNotNull(svrAssert);
            assertTrue(svrAssert instanceof ServerAllAssertion);

            for (Object obj : ((ServerAllAssertion)svrAssert).getChildren()) {

                if (obj instanceof ServerJmsRoutingAssertion) {
                    return (ServerJmsRoutingAssertion) obj;
                }
            }
//            return (ServerAllAssertion) svrAssert;

        } catch (ServerPolicyException spe) {

            fail("Unable to compile test policy: " + spe);
        } catch (LicenseException lie) {

            fail("Unable to compile test policy: " + lie);
        }
        fail("parseAssertion returning null");
        return null;
    }

    private Wsdl testWsdl() throws IOException, SAXException, WSDLException {

        PublishedService output = new PublishedService();
        output.setSoap(true);
        // set wsdl
        Document wsdl = TestDocuments.getTestDocument(TestDocuments.WSDL_STOCK_QUOTE);
        output.setWsdlXml(XmlUtil.nodeToFormattedString(wsdl));
        return output.parsedWsdl();
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

            AssertionStatus status = null;

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