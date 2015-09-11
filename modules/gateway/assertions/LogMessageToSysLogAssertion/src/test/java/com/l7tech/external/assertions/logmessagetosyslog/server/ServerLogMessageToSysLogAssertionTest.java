package com.l7tech.external.assertions.logmessagetosyslog.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.log.syslog.SyslogSeverity;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.BuildInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Test the LogMessageToSysLogAssertion.
 * <p/>
 * Test Cases:
 * 1) Happy Path no cluster property
 * 2) Missing Message
 * 3) Missing Severity
 * 4) Missing Syslog
 * 5) Syslog not around anymore
 * 6) Syslog not visible anymore
 */
public class ServerLogMessageToSysLogAssertionTest {

    private static final Logger log = Logger.getLogger(ServerLogMessageToSysLogAssertionTest.class.getName());

    private static MockApplicationContext applicationContext;

    private PolicyEnforcementContext peCtx;
    private LogMessageToSysLogAssertion assertion;
    private ServerLogMessageToSysLogAssertion serverAssertion;


    @BeforeClass
    public static void setUpBeforeClass() {

        // Get the spring app context
        if (applicationContext == null) {
            applicationContext = new MockApplicationContext();
            assertNotNull("Fail - Unable to get applicationContext instance", applicationContext);
        }
    }

    @Before
    public void setup() {

        // Get the policy enforcement context
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        assertion = new LogMessageToSysLogAssertion();
        assertion.setSyslogGoid(applicationContext.getSinkConfiguration().getGoid());
        assertion.setSysLogSeverity(SyslogSeverity.INFORMATIONAL.toString());
        assertion.setMessageText("Pow");
        try {
            serverAssertion = new ServerLogMessageToSysLogAssertion(assertion, applicationContext);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage());
            fail("No exception excepted");
        }
    }

    @After
    public void tearDown() {
        applicationContext.getSinkConfiguration().setEnabled(false);
    }

    @org.junit.Test
    public void testTC1_HappyPath() throws Exception {

        ServerLogMessageToSysLogAssertion serverAssertion = new ServerLogMessageToSysLogAssertion(assertion, applicationContext);

        AssertionStatus status = serverAssertion.checkRequest(peCtx);
        assertNotNull(status);
        assertEquals("First Check", AssertionStatus.NONE.getMessage(), status.getMessage());

        assertEquals("Pow", applicationContext.getSyslogManager().getSyslogMock().getLogMessage());
        assertEquals("INFORMATIONAL", SyslogSeverity.INFORMATIONAL.toString());
        assertEquals(false, assertion.isCEFEnabled());
    }

    @org.junit.Test
    public void testMessageWithVariables() throws Exception {

        assertion.setMessageText("${requestId}");

        ServerLogMessageToSysLogAssertion serverAssertion = new ServerLogMessageToSysLogAssertion(assertion, applicationContext);
        serverAssertion.checkRequest(peCtx);

        assertTrue(applicationContext.getSyslogManager().getSyslogMock().getLogMessage(), applicationContext.getSyslogManager().getSyslogMock().getLogMessage().matches("[a-z0-9]{16}[-][a-z0-9]"));
    }

    @org.junit.Test
    public void testClusterProperty() throws Exception {

        // CEF enabled, process == ""
        assertion.setCEFEnabled(true);
        ServerLogMessageToSysLogAssertion serverAssertion = new ServerLogMessageToSysLogAssertion(assertion, applicationContext);
        serverAssertion.checkRequest(peCtx);

        assertEquals("", applicationContext.getSyslogManager().getSyslogMock().getProcess());

        // CEF disabled, process = SSG[<requestId>]
        assertion.setCEFEnabled(false);
        serverAssertion = new ServerLogMessageToSysLogAssertion(assertion, applicationContext);
        serverAssertion.checkRequest(peCtx);

        assertTrue(applicationContext.getSyslogManager().getSyslogMock().getProcess(), applicationContext.getSyslogManager().getSyslogMock().getProcess().matches("SSG\\[[a-z0-9]{16}[-][a-z0-9]\\]"));
    }

    @org.junit.Test
    public void testTC2_MissingMessage() throws Exception {

        assertion.setMessageText("");

        ServerLogMessageToSysLogAssertion serverAssertion = new ServerLogMessageToSysLogAssertion(assertion, applicationContext);

        AssertionStatus status = serverAssertion.checkRequest(peCtx);
        assertNotNull(status);
        assertEquals("First Check", AssertionStatus.FAILED.getMessage(), status.getMessage());
    }

    @org.junit.Test
    public void testTC3_MissingSeverity() throws Exception {

        assertion.setSysLogSeverity("");
        try {
            new ServerLogMessageToSysLogAssertion(assertion, applicationContext);
            fail("No assertion without severity!");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @org.junit.Test
    public void testTC4_MissingSysLog() throws Exception {

        assertion.setSyslogGoid(null);
        try {
            new ServerLogMessageToSysLogAssertion(assertion, applicationContext);
            fail("No assertion without syslog server!");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @org.junit.Test
    public void testTC6_SysLogEnabled() throws Exception {

        applicationContext.getSinkConfiguration().setEnabled(true);

        serverAssertion = new ServerLogMessageToSysLogAssertion(assertion, applicationContext);
        AssertionStatus status = serverAssertion.checkRequest(peCtx);
        assertEquals(AssertionStatus.FAILED.getMessage(), status.getMessage());
    }

    @Test
    public void testCEFEnabledNoKeyValues() {
        assertion.setCEFEnabled(true);
        assertion.setCefSeverity(1);
        assertion.setCefSignatureId("sigID");
        assertion.setCefSignatureName("sigName");
        try {
            AssertionStatus status = serverAssertion.checkRequest(peCtx);
            assertNotNull(status);
            assertEquals(AssertionStatus.NONE.getMessage(), status.getMessage());

            // check the output that gets written to the syslog server
            assertEquals("CEF:0|Layer7 Technologies Inc.|SecureSpan Gateway|" + BuildInfo.getFormalProductVersion() + "|sigID|sigName|1|", applicationContext.getSyslogManager().getSyslogMock().getLogMessage());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCEFEnabledWithKeyValues() {
        assertion.setCEFEnabled(true);
        assertion.setCefSeverity(1);
        assertion.setCefSignatureId("sigID");
        assertion.setCefSignatureName("sigName");
        Map<String, String> keyValues = new HashMap<String, String>();
        keyValues.put("a", "ab");
        keyValues.put("b", "a b");
        keyValues.put("c", "a=b");
        keyValues.put("d", "a== b");
        assertion.setCefExtensionKeyValuePairs(keyValues);

        try {
            serverAssertion.checkRequest(peCtx);

            // check the output that gets written to the syslog server
            assertEquals("CEF:0|Layer7 Technologies Inc.|SecureSpan Gateway|" + BuildInfo.getFormalProductVersion() + "|sigID|sigName|1|a=ab b=a b c=a\\=b d=a\\=\\= b ", applicationContext.getSyslogManager().getSyslogMock().getLogMessage());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCEFEnabledWithKeyValuesMultiline() {
        assertion.setCEFEnabled(true);
        assertion.setCefSeverity(1);
        assertion.setCefSignatureId("sigID");
        assertion.setCefSignatureName("sigName");
        Map<String, String> keyValues = new HashMap<String, String>();
        keyValues.put("msg", "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ws=\"http://warehouse.acme.com/ws\">\n" +
                "   <soapenv:Header/>\r\n" +
                "   <soapenv:Body>\n" +
                "      <ws:placeOrder soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "         <productid xsi:type=\"xsd:long\">10</productid>\n" +
                "         <amount xsi:type=\"xsd:long\">12</amount>\r\n\r\n" +
                "         <price xsi:type=\"xsd:float\">1.5E2</price>\n" +
                "         <accountid xsi:type=\"xsd:long\">moin</accountid>\n" +
                "      </ws:placeOrder>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>");
        assertion.setCefExtensionKeyValuePairs(keyValues);

        try {
            serverAssertion.checkRequest(peCtx);

            // check the output that gets written to the syslog server
            assertEquals("CEF:0|Layer7 Technologies Inc.|SecureSpan Gateway|" + BuildInfo.getFormalProductVersion() + "|sigID|sigName|1|msg=<soapenv:Envelope xmlns:xsi\\=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd\\=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv\\=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ws\\=\"http://warehouse.acme.com/ws\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <ws:placeOrder soapenv:encodingStyle\\=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                    "         <productid xsi:type\\=\"xsd:long\">10</productid>\n" +
                    "         <amount xsi:type\\=\"xsd:long\">12</amount>\n" +
                    "         <price xsi:type\\=\"xsd:float\">1.5E2</price>\n" +
                    "         <accountid xsi:type\\=\"xsd:long\">moin</accountid>\n" +
                    "      </ws:placeOrder>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope> ", applicationContext.getSyslogManager().getSyslogMock().getLogMessage());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCEFHeaderWithPipes() {
        assertion.setCEFEnabled(true);
        assertion.setCefSeverity(1);
        assertion.setCefSignatureId("sigID | has || must escape");
        assertion.setCefSignatureName("sig | Name");
        try {
            serverAssertion.checkRequest(peCtx);

            // check the output that gets written to the syslog server
            assertEquals("CEF:0|Layer7 Technologies Inc.|SecureSpan Gateway|" + BuildInfo.getFormalProductVersion() + "|sigID \\| has \\|\\| must escape|sig \\| Name|1|", applicationContext.getSyslogManager().getSyslogMock().getLogMessage());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCEFExtensionWithVariables() {
        assertion.setCEFEnabled(true);
        assertion.setCefSeverity(1);
        assertion.setCefSignatureId("sigID");
        assertion.setCefSignatureName("sig");
        Map<String, String> keyValues = new HashMap<String, String>();
        keyValues.put("externalId", "${requestId}");
        assertion.setCefExtensionKeyValuePairs(keyValues);
        try {
            serverAssertion.checkRequest(peCtx);

            // check the output that gets written to the syslog server
            // CEF:0|Layer7 Technologies Inc.|SecureSpan Gateway|8.3.00|sigID|sig|1|externalId=0000014d9219fd43-c
            String loggedMessage = applicationContext.getSyslogManager().getSyslogMock().getLogMessage();

            String expectedHeader = "CEF:0|Layer7 Technologies Inc.|SecureSpan Gateway|" + BuildInfo.getFormalProductVersion() + "|sigID|sig|";
            assertTrue("'" + loggedMessage + "'", loggedMessage.startsWith(expectedHeader));
            assertTrue("'" + loggedMessage + "'", loggedMessage.substring(expectedHeader.length()).matches("1\\|externalId=[a-z0-9]{16}[-][a-z0-9] "));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }
}
