package com.l7tech.server.security.kerberos;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Unit test cases for the updated outbound routing with kerberos support (Windows Authenticated only).
 *
 * @author: vchan
 */
@Ignore("Test requires developer setup")
public class KerberosOutboundRoutingTest extends KerberosTest {

    private ApplicationContext appCtx;
    private WspReader policyReader;
    private ServerPolicyFactory factory;
    private WssProcessorImpl trogdor;

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        if (appCtx == null)
            appCtx = ApplicationContexts.getTestApplicationContext();

        // grab the wspReader
        if (policyReader == null) {
            policyReader = (WspReader) appCtx.getBean("wspReader", WspReader.class);
            Assert.assertNotNull("Fail - Unable to obtain \"wspReader\" from the application context", policyReader);
        }

        // grab the policyFactory
        if (factory == null) {
            factory = (ServerPolicyFactory) appCtx.getBean("policyFactory", ServerPolicyFactory.class);
            Assert.assertNotNull("Fail - Unable to obtain \"policyFactory\" from the application context", factory);
        }

        if (trogdor == null) {
            trogdor = new WssProcessorImpl();
        }
    }

    @Test
    public void testConfiguredAccount1() {

        ServerHttpRoutingAssertion as;
        PolicyEnforcementContext peCtx;
        try {

            as = getAssertion(Const.TEST_ASSERTION_CFG1);
            Assert.assertNotNull(as);
            peCtx = getPEContext(Const.TEST_MSG_VALID);

            // run the test
            AssertionStatus status = as.checkRequest(peCtx);
            Assert.assertNotNull(status);
            Assert.assertEquals("No Error", status.getMessage());

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception encountered: " + ex.getMessage());
        }
    }

    @Test
    public void testGatewayKeytab() {

        ServerHttpRoutingAssertion as;
        PolicyEnforcementContext peCtx;
        try {

            as = getAssertion(Const.TEST_ASSERTION_KEYTAB);
            Assert.assertNotNull(as);
            peCtx = getPEContext(Const.TEST_MSG_VALID);

            // run the test
            AssertionStatus status = as.checkRequest(peCtx);
            Assert.assertNotNull(status);
            Assert.assertEquals("No Error", status.getMessage());

        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception encountered: " + ex.getMessage());
        }
    }

    // test is not working -- can't simulate kerberos creds in a unit test
//    public void testDelegatedCreds() {
//
//        ServerHttpRoutingAssertion as;
//        PolicyEnforcementContext peCtx;
//        try {
//
//            as = getAssertion(Const.TEST_ASSERTION_DELEG);
//            assertNotNull(as);
//            peCtx = getPEContext(Const.TEST_MSG_VALID);
//
//            // run the test
//            AssertionStatus status = as.checkRequest(peCtx);
//            assertNotNull(status);
//            assertEquals("No Error", status.getMessage());
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            fail("Unexpected exception encountered: " + ex.getMessage());
//        }
//    }

    private ServerHttpRoutingAssertion getAssertion(String policyXml)
            throws IOException, PolicyAssertionException
    {
        Assertion as = policyReader.parsePermissively(policyXml, WspReader.INCLUDE_DISABLED);
        Assert.assertNotNull(as);
        Assert.assertTrue(as instanceof AllAssertion);
        AllAssertion all = (AllAssertion) as;

        for (Object obj : all.getChildren()) {

            if (obj instanceof HttpRoutingAssertion) {
                return new ServerHttpRoutingAssertion((HttpRoutingAssertion) obj, appCtx);
            }
        }
        return null;
    }

    private PolicyEnforcementContext getPEContext(String testMessage) throws Exception {

        Message req = new Message( testMessage(testMessage) );
        Message res = new Message();


        PolicyEnforcementContext peCtx = new PolicyEnforcementContext(req, res);
        peCtx.setService(new PublishedService());
        return peCtx;
    }


    private Document testMessage(String testMsg) throws IOException, SAXException {
        return XmlUtil.parse( new ByteArrayInputStream(testMsg.getBytes()) );
    }




    /* ================================================================================ */
    private interface Const {

        static final String TEST_ASSERTION_CFG1 = " <?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:HttpNegotiate/>\n" +
                "        <L7p:HttpRoutingAssertion>\n" +
                "            <L7p:KrbConfiguredAccount stringValue=\"vcwinsvr\"/>\n" +
                "            <L7p:KrbConfiguredPassword stringValue=\"p65ssw@rd\"/>\n" +
                "            <L7p:ProtectedServiceUrl stringValue=\"http://vcwinsvr.l7tech.com:8081\"/>\n" +
                "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"Cookie\"/>\n" +
                "                    </L7p:item>\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"SOAPAction\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Rules>\n" +
                "            </L7p:RequestHeaderRules>\n" +
                "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                "            </L7p:RequestParamRules>\n" +
                "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Rules>\n" +
                "            </L7p:ResponseHeaderRules>\n" +
                "        </L7p:HttpRoutingAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        static final String TEST_ASSERTION_KEYTAB = " <?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:HttpNegotiate/>\n" +
                "        <L7p:HttpRoutingAssertion>\n" +
                "            <L7p:KrbUseGatewayKeytab booleanValue=\"true\"/>" +
                "            <L7p:ProtectedServiceUrl stringValue=\"http://vcwinsvr.l7tech.com:8081\"/>\n" +
                "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"Cookie\"/>\n" +
                "                    </L7p:item>\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"SOAPAction\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Rules>\n" +
                "            </L7p:RequestHeaderRules>\n" +
                "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                "            </L7p:RequestParamRules>\n" +
                "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Rules>\n" +
                "            </L7p:ResponseHeaderRules>\n" +
                "        </L7p:HttpRoutingAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        static final String TEST_ASSERTION_DELEG = " <?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:HttpNegotiate/>\n" +
                "        <L7p:HttpRoutingAssertion>\n" +
                "            <L7p:KrbDelegatedAuthentication booleanValue=\"true\"/>" +
                "            <L7p:ProtectedServiceUrl stringValue=\"http://vcwinsvr.l7tech.com:8081\"/>\n" +
                "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"Cookie\"/>\n" +
                "                    </L7p:item>\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"SOAPAction\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Rules>\n" +
                "            </L7p:RequestHeaderRules>\n" +
                "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                "            </L7p:RequestParamRules>\n" +
                "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
                "                    <L7p:item httpPassthroughRule=\"included\">\n" +
                "                        <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Rules>\n" +
                "            </L7p:ResponseHeaderRules>\n" +
                "        </L7p:HttpRoutingAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";


        static final String TEST_MSG_VALID = "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:xmltoday-delayed-quotes\">\n"+
                "   <soapenv:Header/>\n"+
                "   <soapenv:Body>\n"+
                "      <urn:getQuote soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"+
                "         <symbol xsi:type=\"xsd:string\">GE</symbol>\n"+
                "      </urn:getQuote>\n"+
                "   </soapenv:Body>\n"+
                "</soapenv:Envelope>";

    }

}
