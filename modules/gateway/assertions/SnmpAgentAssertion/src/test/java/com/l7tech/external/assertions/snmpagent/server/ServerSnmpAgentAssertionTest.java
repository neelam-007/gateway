package com.l7tech.external.assertions.snmpagent.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.snmpagent.SnmpAgentAssertion;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Unit test for the SnmpAgentAssertion.
 *
 * This comprises a variety of tests:  The assertion is designed to:
 *
 * (DONE) 1. Talk only to a loopback address.  Confirm this functionality.
 * (DONE) 2. Send a request where the service OID is invalid. (Should fail)
 * (DONE) 3a. Test the GET command, part of test 1.
 * (DONE) 3b. Test the GETNEXT command.
 * (DONE) 3c. Test the SET command. (Should fail)
 * (DONE) 3d. Test with a garbage command (Should fail)
 * 4a. Send SNMP service queries, compare returns to known values
 * 4b. Send SNMP service queries, out of range (Should fail)
 *
 * Updated to use JUnit4
 */
public class ServerSnmpAgentAssertionTest {

    private ApplicationContext appContext;
    private PolicyEnforcementContext policyContext;

    @Before
    public void setup() {
        appContext = new ClassPathXmlApplicationContext(new String[] {"/com/l7tech/external/assertions/snmpagent/server/extra-beans.xml"});
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @After
    public void tearDown() {
        appContext = null;
        policyContext = null;
    }


    //
    // LOOPBACK TESTS
    //
    // This will perform two tests: With a valid loopback address, and an IP that is not the loopback address.
    // The valid loopback address will be a) 127.0.0.1, and b) the IP of the machine running the test.
    // The invalid loopback address will be 192.168.245.129.

    @Test
    public void testLoopbackSecurityWithLoopbackAddress() {
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_SERVICE_OID));

        int status = policyContext.getResponse().getHttpResponseKnob().getStatus();
        if (status != HttpServletResponse.SC_OK) {
            // This is the failure we're looking for, as it's not a loopback address.
            fail("The assertion test has failed, returned a non-OK response. ["+status+"]");
        }

        // Because this is supposed to be a success test, we can check for a variable.
        this.compareResponseForServiceCall(MockSnmpValues.GET_SERVICE_OID);
    }

    //
    //
    // This test will occasionally fail on some machines, but pass on others.
    // Until I can investigate and determine the cause, I'll disable this test.
    // (Ideally, this test should _pass_ on addresses that are not 127.0.0.1, but on the tactical build server,
    // this test fails and the machine address _is_ accepted as loopback address...damn my english)
    //
    // RESULT - 24 May 2012: This test is pulled from the suite.  On the test server, the build suite is run on the
    // 127.0.0.1 loopback address, which is what this test sees as the machine address.  Because this is actually
    // a valid loopback address (even though Java is told it's the machine address), the test fails, as it's looking
    // for an address that is non-loopback.
    //
    // This test essentially duplicates the testLoopBackSecurityWithLoopbackAddress() test above on the test server.
    // There is no way to perform a machine (internet) address test on this server, as the VM never sees one.
    //

//    @Test
//    public void testLoopbackSecurityWithMachineAddress() {
//        InetAddress localhost = null;
//        try {
//            localhost = InetAddress.getLocalHost();
//        } catch (UnknownHostException e) {
//            fail("Unable to get the localhost ip address for test.");
//        }
//
//        performLoopbackSecurityTest(buildGenericServiceCall("https://" + localhost.getHostAddress(), null, null, MockSnmpValues.GET_SERVICE_OID, null));
//        // We are expecting an invalid address error to be returned, and it's located in the context.
//        int status = policyContext.getResponse().getHttpResponseKnob().getStatus();
//        if (status != HttpServletResponse.SC_FORBIDDEN) {
//            // This is the failure we're looking for, as it's not a loopback address.
//            // NOTE: this is specifically for the tactical build server - I want to see what address it reports.
//            // -> On my machine, it reports the address
//            fail("The assertion test has failed, it accepted an address that is non-loopback. ["+status+", test-addr: "+localhost.getHostAddress()+"]");
//        }
//        System.out.println("testLoopbackSecurityWithMachineAddress: Passed using test-addr: " + localhost.getHostAddress());
//    }

    @Test
    public void testLoopbackSecurityWithOtherAddress() {
        performLoopbackSecurityTest(buildGenericServiceCall("https://192.168.245.129", null, null, MockSnmpValues.GET_SERVICE_OID, null));
        int status = policyContext.getResponse().getHttpResponseKnob().getStatus();
        if (status != HttpServletResponse.SC_FORBIDDEN) {
            // This is the failure we're looking for, as it's not a loopback address.
            fail("The assertion test has failed, it accepted an address that is non-loopback. ["+status+"]");
        }
    }

    @Test
    // We know at this point it responds to a valid service id.  Let's try some invalid ones.
    // Pass a bogus OID as the starting digits.
    public void testInvalidServiceNumber() {
        performLoopbackSecurityTest(buildGenericServiceCall(null, null, ".0.1.2.6.32.91.15023", MockSnmpValues.GET_SERVICE_OID, null));
        int status = policyContext.getResponse().getHttpResponseKnob().getStatus();
        if (status != HttpServletResponse.SC_BAD_REQUEST) {
            // This is the failure we're looking for, as it's not a loopback address.
            fail("The assertion test has failed, it accepted an address that is invalid. ["+status+"]");
        }
    }

    @Test
    // This test makes the last number of the OID invalid, not the first part.
    public void testInvalidServiceNumberFinalOID() {
        performLoopbackSecurityTest(buildGenericServiceCall(null, null, null, MockSnmpValues.GET_SERVICE_OID, "0000000000000000000000000000c123"));
        int status = policyContext.getResponse().getHttpResponseKnob().getStatus();
        if (status != HttpServletResponse.SC_BAD_REQUEST) {
            // This is the failure we're looking for, as it's not a loopback address.
            fail("The assertion test has failed, it accepted an address that is invalid. ["+status+"]");
        }
    }

    @Test
    // This attempts to send the GETNEXT command on a service
    public void testGetNextService() {
        performLoopbackSecurityTest(buildGetNextServiceCall(MockSnmpValues.GET_SERVICE_OID));
        int status = policyContext.getResponse().getHttpResponseKnob().getStatus();
        if (status != HttpServletResponse.SC_OK) {
            // This is the failure we're looking for, as it's not a loopback address.
            fail("The assertion test has failed, it refused a GETNEXT request.");
        }
    }

    @Test
    // This attempt to send a SET command on a service
    public void testSetService() {
        performLoopbackSecurityTest(buildGenericServiceCall(null, "SET", null, MockSnmpValues.GET_SERVICE_OID, MockSnmpValues.TEST_SERVICE_GOID_INT));
        int status = policyContext.getResponse().getHttpResponseKnob().getStatus();
        if (status != HttpServletResponse.SC_BAD_REQUEST) {
            // This is the failure we're looking for, as it's not a loopback address.
            fail("The assertion test has failed, it accepted a SET command.");
        }
    }

    @Test
    // This attempt to send a non-SNMP command to a service.  Similar to the SET test above, but SET is a valid SNMP command.
    public void testShaveMonkeyService() {
        performLoopbackSecurityTest(buildGenericServiceCall(null, "SHAVE.MONKEYS", null, MockSnmpValues.GET_SERVICE_OID, MockSnmpValues.TEST_SERVICE_GOID_INT));
        int status = policyContext.getResponse().getHttpResponseKnob().getStatus();
        if (status != HttpServletResponse.SC_BAD_REQUEST) {
            // This is the failure we're looking for, as it's not a loopback address.
            fail("The assertion test has failed, it accepted a SHAVE.MONKEYS command.");
        }
    }

    @Test
    public void testInvalidFieldService1() {
        performLoopbackSecurityTest(buildGenericServiceCall(null, null, null, MockSnmpValues.snmpServiceMap.size()+1, MockSnmpValues.TEST_SERVICE_GOID_INT));
        int status = policyContext.getResponse().getHttpResponseKnob().getStatus();
        if (status != HttpServletResponse.SC_BAD_REQUEST) {
            // This is the failure we're looking for, as it's not a loopback address.
            fail("The assertion test has failed, it accepted a GET command for an invalid service entry.");
        }
    }

    @Test
    public void testInvalidFieldService2() {
        performLoopbackSecurityTest(buildGenericServiceCall(null, null, null, -1, MockSnmpValues.TEST_SERVICE_GOID_INT));
        int status = policyContext.getResponse().getHttpResponseKnob().getStatus();
        if (status != HttpServletResponse.SC_BAD_REQUEST) {
            // This is the failure we're looking for, as it's not a loopback address.
            fail("The assertion test has failed, it accepted a GET command for an invalid service entry.");
        }
    }

    @Test
    // Tests service entry 1 against a known value
    public void testServiceEntry1() {
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_SERVICE_OID));
        compareResponseForServiceCall(MockSnmpValues.GET_SERVICE_OID);
    }

    @Test
    public void testServiceEntry2() {
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_SERVICE_NAME));
        compareResponseForServiceCall(MockSnmpValues.GET_SERVICE_NAME);
    }
    /***
     *  The following unit test need to be re-written
     *
    @Test
    public void testServiceEntry3() {
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_REQUESTS_RECEIVED));
        compareResponseForServiceCall(MockSnmpValues.GET_REQUESTS_RECEIVED);
    }

    @Test
    public void testServiceEntry4() {
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_REQUESTS_AUTHORIZED));
        compareResponseForServiceCall(MockSnmpValues.GET_REQUESTS_AUTHORIZED);
    }

    @Test
    public void testServiceEntry5() {
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_REQUESTS_COMPLETED));
        compareResponseForServiceCall(MockSnmpValues.GET_REQUESTS_COMPLETED);
    }
    */

    @Test
    public void testServiceEntry6() {
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_FAILED_ROUTES_DAY));
        compareResponseForServiceCall(MockSnmpValues.GET_FAILED_ROUTES_DAY);
    }

    @Test
    public void testServiceEntry7() {
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_FAILED_ROUTES_HOUR));
        compareResponseForServiceCall(MockSnmpValues.GET_FAILED_ROUTES_HOUR);
    }

    @Test
    public void testServiceEntry8() {
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_FAILED_ROUTES_FINE));
        compareResponseForServiceCall(MockSnmpValues.GET_FAILED_ROUTES_FINE);
    }

    @Test
    public void testServiceEntry9() {
        // This one we need to calculate the amount, based on the values we have used.
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_AVERAGE_BACKEND_TIME_DAY));
        compareResponseForServiceCall(MockSnmpValues.GET_AVERAGE_BACKEND_TIME_DAY);
    }

    @Test
    public void testServiceEntry10() {
        // This one we need to calculate the amount, based on the values we have used.
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_AVERAGE_BACKEND_TIME_HOUR));
        compareResponseForServiceCall(MockSnmpValues.GET_AVERAGE_BACKEND_TIME_HOUR);
    }

    @Test
    public void testServiceEntry11() {
        // This one we need to calculate the amount, based on the values we have used.
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_AVERAGE_BACKEND_TIME_FINE));
        compareResponseForServiceCall(MockSnmpValues.GET_AVERAGE_BACKEND_TIME_FINE);
    }

    @Test
    public void testServiceEntry12() {
        // This one we need to calculate the amount, based on the values we have used.
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_AVERAGE_FRONTEND_TIME_DAY));
        compareResponseForServiceCall(MockSnmpValues.GET_AVERAGE_FRONTEND_TIME_DAY);
    }

    @Test
    public void testServiceEntry13() {
        // This one we need to calculate the amount, based on the values we have used.
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_AVERAGE_FRONTEND_TIME_HOUR));
        compareResponseForServiceCall(MockSnmpValues.GET_AVERAGE_FRONTEND_TIME_HOUR);
    }

    @Test
    public void testServiceEntry14() {
        // This one we need to calculate the amount, based on the values we have used.
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_AVERAGE_FRONTEND_TIME_FINE));
        compareResponseForServiceCall(MockSnmpValues.GET_AVERAGE_FRONTEND_TIME_FINE);
    }

    @Test
    public void testServiceEntry15() {
        // This one we need to calculate the amount, based on the values we have used.
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_POLICY_VIOLATIONS_DAY));
        compareResponseForServiceCall(MockSnmpValues.GET_POLICY_VIOLATIONS_DAY);
    }

    @Test
    public void testServiceEntry16() {
        // This one we need to calculate the amount, based on the values we have used.
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_POLICY_VIOLATIONS_HOUR));
        compareResponseForServiceCall(MockSnmpValues.GET_POLICY_VIOLATIONS_HOUR);
    }

    @Test
    public void testServiceEntry17() {
        // This one we need to calculate the amount, based on the values we have used.
        performLoopbackSecurityTest(buildGetServiceCall(MockSnmpValues.GET_POLICY_VIOLATIONS_FINE));
        compareResponseForServiceCall(MockSnmpValues.GET_POLICY_VIOLATIONS_FINE);
    }

    /**
     *
     * Support Methods
     *
     */
    private void performLoopbackSecurityTest(String url) {
        // Create a URL with the smnp management request.
        policyContext.getRequest().attachHttpRequestKnob(new _MockHttpRequestKnob(url));
        try {
            policyContext.getRequest().initialize(ContentTypeHeader.OCTET_STREAM_DEFAULT, "blah".getBytes());
        } catch (IOException e) {
            fail("Error creating the request message");
        }

        try {
            policyContext.getResponse().initialize(ContentTypeHeader.OCTET_STREAM_DEFAULT, "<!-- test -->".getBytes());
            // We add an empty response knob, as the assertion doesn't return a http response.
            policyContext.getResponse().attachHttpResponseKnob(new _MockHttpResponseKnob());
        } catch (IOException e) {
            fail("Unable to create response message");
        }

        SnmpAgentAssertion assertion = new SnmpAgentAssertion();
        ServerSnmpAgentAssertion serverAssertion = null;
        try {
            serverAssertion = new ServerSnmpAgentAssertion(assertion, appContext);
        } catch (Exception e) {
            fail("Unable to initialize the server assertion.");
        }

        // There are no values to set in the assertion. We go straight to the test.
        // We're ready to try the server assertion.
        try {
            // Remember, the request for this assertion is in the HTTP request header.
            serverAssertion.checkRequest(policyContext);
        } catch (Exception e) {
            e.printStackTrace();
            fail("The server assertion failed during the test.");
        }

    }

    private String buildGenericServiceCall(@Nullable String serviceUrl, @Nullable String snmpCommand, @Nullable String serviceAddress, @Nullable Integer fieldID, @Nullable String serviceGOID) {
        // Builds a string in the pattern of:
        // "https://localhost:8080/snmp/management/GET/.1.3.6.1.4.1.17304.7.1.1.49152"
        StringBuilder urlCall = new StringBuilder();
        urlCall.append(serviceUrl == null ? MockSnmpValues.TEST_SERVICE_URL : serviceUrl);
        urlCall.append("/");
        urlCall.append(snmpCommand == null ? MockSnmpValues.GET_COMMAND : snmpCommand);
        urlCall.append("/");
        urlCall.append(serviceAddress == null ? MockSnmpValues.TEST_SERVICE_ADDRESS : serviceAddress);
        urlCall.append(".");
        urlCall.append(fieldID == null ? MockSnmpValues.GET_SERVICE_OID : fieldID);
        urlCall.append(".");
        urlCall.append(serviceGOID == null ? MockSnmpValues.TEST_SERVICE_GOID_INT : serviceGOID);

        return new String(urlCall);
    }

    private String buildGetServiceCall(int fieldID) {
        return this.buildGenericServiceCall(null, null, null, fieldID, null);
    }

    private String buildGetNextServiceCall(int fieldID) {
        return this.buildGenericServiceCall(null, MockSnmpValues.GET_NEXT_COMMAND, null, fieldID, null);
    }

    private void compareResponseForServiceCall(int fieldId) {
        this.compareResponseForServiceCall(fieldId, null);
    }

    private void compareResponseForServiceCall(int fieldId, String expectedValue) {

        // This takes the variable response from the assertion, compares the serviceAddress, the syntaxType and the
        // expected value verses what we are expecting from the field id (the info we requested)
        // Because this is supposed to be a success test, we can check for a variable.
        String assertionResponse = null;
        try {
            Object theResponse = policyContext.getVariable("snmp.agent.response");
            if (theResponse instanceof String) {
                assertionResponse = (String)theResponse;
            } else {
                throw new Exception();  // We can't parse the response.  This is a fail.
            }
        } catch (Exception e) {
            fail("Could not retrieve or parse the server response in ${snmp.agent.response}");
        }

        // The response is in the form of: serviceAddress\nsyntaxType\nexpectedValue
        String[] responseLines = assertionResponse.split("\n");

        // Perform an explicit compare of each line, trimmed.
        String fullServiceAddress = MockSnmpValues.TEST_SERVICE_ADDRESS + "." + fieldId + "." + MockSnmpValues.TEST_SERVICE_GOID_INT;
        String syntaxType = MockSnmpValues.snmpServiceMap.get(fieldId).syntaxType;
        if (expectedValue == null) {
            // Clobber the passed in value with the value from the assertion.  This is expected.
            expectedValue = MockSnmpValues.snmpServiceMap.get(fieldId).expectedValue;
        }
        assertTrue("The serviceAddress doesn't match what was expected.", responseLines[0].trim().equals(fullServiceAddress));
        assertTrue("The syntaxType doesn't match what was expected.", responseLines[1].trim().equalsIgnoreCase(syntaxType));
        assertTrue("The expectedValue doesn't match what was expected", responseLines[2].trim().equalsIgnoreCase(expectedValue));

    }

    private class _MockHttpRequestKnob implements HttpRequestKnob {

        private final String url;

        public _MockHttpRequestKnob(String url) {
            this.url = url;
        }

        @Override
        public HttpCookie[] getCookies() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getCookies()");
            return new HttpCookie[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public HttpMethod getMethod() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getMethod()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getMethodAsString() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getMethodAsString()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getRequestUri() {
            // This wants the last part of the URL, no protocol or machine name.
            URL theUrl = null;
            try {
                theUrl = new URL(url);
            } catch (Exception e) {
                fail("Test failed.  Unable to create a URL class to proceed with the test.");
            }
            return theUrl.getPath();
        }

        @Override
        public String getRequestUrl() {
            return url;
        }

        @Override
        public URL getRequestURL() {
            System.out.println("*** CALL *** MockHttpRequestKnob: (URL)getRequestURL()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public long getDateHeader(String name) throws ParseException {
            System.out.println("*** CALL *** MockHttpRequestKnob: getDateHeader()");
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public int getIntHeader(String name) {
            System.out.println("*** CALL *** MockHttpRequestKnob: getIntHeader()");
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getHeaderFirstValue(String name) {
            System.out.println("*** CALL *** MockHttpRequestKnob: getHeaderFirstValue()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getHeaderSingleValue(String name) throws IOException {
            System.out.println("*** CALL *** MockHttpRequestKnob: getHeaderSingleValue()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public X509Certificate[] getClientCertificate() throws IOException {
            System.out.println("*** CALL *** MockHttpRequestKnob: getClientCertificate()");
            return new X509Certificate[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public boolean isSecure() {
            System.out.println("*** CALL *** MockHttpRequestKnob: isSecure()");
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getParameter(String name) throws IOException {
            System.out.println("*** CALL *** MockHttpRequestKnob: getParameter()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Map getParameterMap() throws IOException {
            System.out.println("*** CALL *** MockHttpRequestKnob: getParameterMap()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String[] getParameterValues(String s) throws IOException {
            System.out.println("*** CALL *** MockHttpRequestKnob: getParameterValues()");
            return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Enumeration getParameterNames() throws IOException {
            System.out.println("*** CALL *** MockHttpRequestKnob: getParameterNames()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Object getConnectionIdentifier() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getConnectionIdentifier()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getQueryString() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getQueryString()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String[] getHeaderValues(String name) {
            System.out.println("*** CALL *** MockHttpRequestKnob: getHeaderValues()");
            return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String[] getHeaderNames() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getHeaderNames()");
            return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getSoapAction() throws IOException {
            System.out.println("*** CALL *** MockHttpRequestKnob: getSoapAction()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getRemoteAddress() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getRemoteAddress()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getRemoteHost() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getRemoteHost()");
            return null;
        }

        @Override
        public int getRemotePort() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getRemotePort()");
            return 0;
        }

        @Override
        public String getLocalAddress() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getLocalAddress()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getLocalHost() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getLocalHost()");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public int getLocalPort() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getLocalPort()");
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public int getLocalListenerPort() {
            System.out.println("*** CALL *** MockHttpRequestKnob: getListenerPort()");
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }



    private class _MockHttpResponseKnob implements HttpResponseKnob {

        int statusCode = 0;

        @Override
        public void addChallenge(String value) {
            System.out.println("*** CALL *** MockHttpResponseKnob: addChallenge()");
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void setStatus(int code) {
            this.statusCode = code;
            //System.out.println("*** CALL *** MockHttpResponseKnob: setStatus()");
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public int getStatus() {
            //System.out.println("*** CALL *** MockHttpResponseKnob: getStatus()");
            //return 0;  //To change body of implemented methods use File | Settings | File Templates.
            return this.statusCode;
        }
    }


}
