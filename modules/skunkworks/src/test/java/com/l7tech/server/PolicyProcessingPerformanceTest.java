package com.l7tech.server;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.UserBean;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.secureconversation.InboundSecureConversationContextManager;
import com.l7tech.server.service.ServiceCacheStub;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.tomcat.ResponseKillerValve;
import com.l7tech.server.transport.http.ConnectionId;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.util.*;
import com.l7tech.xml.SoapFaultLevel;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author rmak
 */
public class PolicyProcessingPerformanceTest extends TestCase {
    private static final Logger logger = Logger.getLogger(PolicyProcessingPerformanceTest.class.getName());
    private static final int ASSERTION_STATUS_NONE = AssertionStatus.NONE.getNumeric();
    private static final String POLICY_RES_PATH = "policy/resources/";

    private static MessageProcessor messageProcessor = null;
    private static SoapFaultManager soapFaultManager = null;
    private static TestingHttpClientFactory testingHttpClientFactory = null;
    private static InboundSecureConversationContextManager inboundSecureConversationContextManager = null;

    static {
        SyspropUtil.setProperty( "com.l7tech.security.prov.rsa.libpath.nonfips", "USECLASSPATH" );
        JceProvider.init();
    }

    /**
     * Test services, data is:
     *
     * - URL (may be null)
     * - Policy resource path (policy/tests/XXX)
     * - WSDL resource path (Optional)
     *
     * NOTE: Currently there's just one service with a WSDL which allows the
     * JMS service resolution to function correctly.
     */
    private static final String[][] TEST_SERVICES = new String[][]{
        {"/sqlattack", "POLICY_sqlattack.xml"},
        {"/requestsizelimit", "POLICY_requestsizelimit.xml"},
        {"/documentstructure", "POLICY_documentstructure.xml"},
        {"/faultlevel", "POLICY_faultlevel.xml"},
        {"/ipaddressrange", "POLICY_iprange.xml"},
        {"/xpathcreds", "POLICY_xpathcreds.xml"},
        {"/usernametoken", "POLICY_usernametoken.xml"},
        {"/httproutecookie", "POLICY_httproutecookie.xml"},
        {"/httproutenocookie", "POLICY_httproutenocookie.xml"},
        {"/httproutetaicredchain", "POLICY_httproutetaicredchain.xml"},
        {"/httproutepassthru", "POLICY_httproutepassthru.xml"},
        {"/httproutejms", "POLICY_httproutejms.xml", "WSDL_httproutejms.wsdl"},
        {"/httpwssheaderleave", "POLICY_httpwssheaderleave.xml"},
        {"/httpwssheaderremove", "POLICY_httpwssheaderremove.xml"},
        {"/httpwssheaderpromote", "POLICY_httpwssheaderpromote.xml"},
        {"/schemavalrequest", "POLICY_schemavalrequest.xml"},
        {"/schemavalresponse", "POLICY_schemavalresponse.xml"},
        {"/emptypolicy", "POLICY_emptypolicy.xml"},
        {"/evaluaterequestxpath", "POLICY_evaluaterequestxpath.xml"},
        {"/evaluateresponsexpath", "POLICY_evaluateresponsexpath.xml"},
        {"/policylogic", "POLICY_policylogic.xml"},
        {"/regularexpression", "POLICY_regularexpression.xml"},
        {"/wsdloperation", "POLICY_wsdloperation.xml", "WSDL_warehouse.wsdl"},
        {"/xsltransformationrequest", "POLICY_xsltransformationrequest.xml"},
        {"/xsltransformationresponse", "POLICY_xsltransformationresponse.xml"},
        {"/wsssign", "POLICY_wss_signbody.xml"},
        {"/wssenc", "POLICY_wss_encbody.xml"},
    };

    private static String REQUEST_general;
    private static String REQUEST_xpathcreds_success;
    private static String REQUEST_usernametoken_success_1;
    private static String REQUEST_usernametoken_success_2;
    private static String REQUEST_httpwssheaderpromote_success;
    private static String REQUEST_schemaval_request_success;
    private static String REQUEST_schemaval_response_success;
    private static String REQUEST_signed;    
    private static String REQUEST_encrypted;

    private static byte[] RESPONSE_general;

    private Level savedLoggerLevel;

    /**
     * Run all tests via a test suite.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Set up a test suite for all tests in PolicyProcessingPerformanceTest.
     * @return a test suite
     */
    public static Test suite() {
         final TestSuite suite = new TestSuite(PolicyProcessingPerformanceTest.class);
         return new TestSetup(suite) {
             @Override
             protected void setUp() throws Exception {
                 PolicyProcessingPerformanceTest.setUpClass();
             }

             @Override
             protected void tearDown() throws Exception {
                 SyspropUtil.clearProperties(
                     "com.l7tech.security.prov.rsa.libpath.nonfips",
                     "com.l7tech.server.serviceResolution.strictSoap",
                     "javax.xml.transform.TransformerFactory"
                 );
             }
         };
    }

    public static void setUpClass() throws Exception {
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();

        messageProcessor = applicationContext.getBean("messageProcessor", MessageProcessor.class);
        soapFaultManager = applicationContext.getBean("soapFaultManager", SoapFaultManager.class);
        testingHttpClientFactory = applicationContext.getBean("httpRoutingHttpClientFactory2", TestingHttpClientFactory.class);
        inboundSecureConversationContextManager = applicationContext.getBean("inboundSecureConversationContextManager", InboundSecureConversationContextManager.class);

        // well known test session
        if (inboundSecureConversationContextManager.getSession("http://www.layer7tech.com/uuid/00000000") == null) {
            inboundSecureConversationContextManager.createContextForUser(
                    "http://www.layer7tech.com/uuid/00000000",
                    "http://www.layer7tech.com/uuid/00000000",
                    System.currentTimeMillis() + (long) TimeUnit.DAYS.getMultiplier(),
                    new UserBean(),
                    new byte[16]);
        }

        buildServices( applicationContext.getBean("serviceManager", ServiceManager.class) );

        ServiceCacheStub cache = applicationContext.getBean("serviceCache", ServiceCacheStub.class);
        cache.initializeServiceCache();

        REQUEST_general = new String(loadResource("REQUEST_general.xml"));
        REQUEST_xpathcreds_success = new String(loadResource("REQUEST_xpathcreds_success.xml"));
        REQUEST_usernametoken_success_1 = new String(loadResource("REQUEST_usernametoken_success_1.xml"));
        REQUEST_usernametoken_success_2 = new String(loadResource("REQUEST_usernametoken_success_2.xml"));
        REQUEST_httpwssheaderpromote_success = new String(loadResource("REQUEST_httpwssheaderpromote_success.xml"));
        REQUEST_schemaval_request_success = new String(loadResource("REQUEST_schemaval_request.xml"));
        REQUEST_schemaval_response_success = new String(loadResource("REQUEST_schemaval_response_request.xml"));
        REQUEST_signed = new String(loadResource("REQUEST_signed.xml"));
        REQUEST_encrypted = new String(loadResource("REQUEST_encrypted.xml"));
        RESPONSE_general = loadResource("RESPONSE_general.xml");
    }

    public static void tearDownClass() throws Exception {
        // destroy services?
    }

    public PolicyProcessingPerformanceTest() {
    }

    public PolicyProcessingPerformanceTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        SyspropUtil.setProperty( "com.l7tech.server.serviceResolution.strictSoap", "false" );

        // Software-only TransformerFactory to ignore the alluring Tarari impl, even if tarari_raxj.jar is sitting right there
        SyspropUtil.setProperty( "javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl" );

        savedLoggerLevel = Logger.getLogger("com.l7tech.server").getLevel();
        Logger.getLogger("com.l7tech.server").setLevel(Level.OFF);
    }

    @Override
    protected void tearDown() throws Exception {
        Logger.getLogger("com.l7tech.server").setLevel(savedLoggerLevel);
    }

    /**
     * Populate the service cache with the test services.
     */
    /**
     * Populate the service cache with the test services.
     */
    private static void buildServices( final ServiceManager serviceManager ) throws Exception {
        long oid = 1L;

        for (String[] serviceInfo : TEST_SERVICES) {
            PublishedService ps = new PublishedService();
            ps.setGoid(new Goid(0,oid++));
            ps.setName(serviceInfo[0].substring(1));
            ps.setRoutingUri(serviceInfo[0]);
            ps.getPolicy().setXml(new String(loadResource(serviceInfo[1])));
            ps.getPolicy().setGoid(ps.getGoid());
            ps.setSoap(true);

            if (serviceInfo.length > 2) {
                ps.setWsdlXml(new String(loadResource(serviceInfo[2])));
                ps.setLaxResolution(false);
            } else {
                ps.setLaxResolution(true);
            }

            serviceManager.update( ps );
        }
    }


    /**
     * Load a resource from the resource directory.
     */
    private static byte[] loadResource(String resourceName) throws IOException {
        InputStream in = null;
        try {
            String resourcePath = POLICY_RES_PATH + resourceName;
            in = PolicyProcessingPerformanceTest.class.getResourceAsStream(resourcePath);
            if (in == null)
                throw new IOException("Could not find resource '"+resourcePath+"'.");
            return IOUtils.slurpStream(in);
        }
        finally {
            ResourceUtils.closeQuietly(in);
        }
    }

    /**
     * Test a request message that passes SQL Attack Protection Assertion.
     */
    public void testSQLAttack() throws Exception  {
        processMessage("/sqlattack", REQUEST_general, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);
    }

    /**
     * Test a request message that passes Request Size Limit Assertion.
     */
    public void testRequestSizeLimit() throws Exception  {
        processMessage("/requestsizelimit", REQUEST_general, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);
    }

    /**
     * Test a request message that passes Document Structure Threats Assertion.
     */
    public void testDocumentStructure() throws Exception  {
        processMessage("/documentstructure", REQUEST_general, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);
    }

    /**
     * Test a request message that triggers Fault Level Assertion.
     */
    public void testFaultLevel() throws Exception  {
        processMessage("/faultlevel", REQUEST_general, AssertionStatus.FALSIFIED.getNumeric());
    }

    /**
     * Test a request message that passes XPath Credentials Assertion.
     */
    public void testXPathCreds() throws Exception  {
        processMessage("/xpathcreds", REQUEST_xpathcreds_success, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);
    }

    /**
     * Test a request message that passes Username Token Assertion.
     */
    public void testUsernameToken_1() throws Exception  {
        processMessage("/usernametoken", REQUEST_usernametoken_success_1, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);
    }

    /**
     * Test a request message that passes Username Token Assertion.
     */
    public void testUsernameToken_2() throws Exception  {
        processMessage("/usernametoken", REQUEST_usernametoken_success_2, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);
    }

    /**
     * Test cookie pass through.
     */
    public void testHttpRoutingCookie() throws Exception {
        GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new HttpHeader[]{
                new GenericHttpHeader("Content-Type","text/xml; charset=utf8"),
                new GenericHttpHeader("Set-Cookie","cookie=outvalue"),
        });

        MockGenericHttpClient mockClient = buildMockHttpClient(responseHeaders, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/httproutecookie", REQUEST_general, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);
    }

    public void testHttpRoutingCookieNone() throws Exception {
        GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new HttpHeader[]{
                new GenericHttpHeader("Content-Type","text/xml; charset=utf8"),
                new GenericHttpHeader("Set-Cookie","cookie=outvalue"),
        });

        MockGenericHttpClient mockClient2 = buildMockHttpClient(responseHeaders, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient2);

        processMessage("/httproutenocookie", REQUEST_general, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);
    }

    public void testHttpRoutingSticky() throws Exception {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/httproutepassthru", REQUEST_general, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);
    }

    public void testHttpWssHeaderLeave() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/httpwssheaderleave", REQUEST_usernametoken_success_1, ASSERTION_STATUS_NONE);
    }

    public void testHttpWssHeaderRemove() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/httpwssheaderremove", REQUEST_usernametoken_success_1, ASSERTION_STATUS_NONE);
    }

    public void testHttpWssHeaderPromote() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/httpwssheaderpromote", REQUEST_httpwssheaderpromote_success, ASSERTION_STATUS_NONE);
    }

    public void testSchemaValidationRequest() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/schemavalrequest", REQUEST_schemaval_request_success, ASSERTION_STATUS_NONE);
    }

    public void testSchemaValidationResponse() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/schemavalresponse", REQUEST_schemaval_response_success, ASSERTION_STATUS_NONE);
    }

    /**
     * Test an empty policy.
     * @throws Exception
     */
    public void testEmptyPolicy() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/emptypolicy", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    /**
     * Test a policy with evaluate request Xpath.
     * @throws Exception
     */
    public void testEvaluateRequestXpath() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/evaluaterequestxpath", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    /**
     * Test a policy with evaluate response Xpath.
     * @throws Exception
     */
    public void testEvaluateResponseXpath() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/evaluateresponsexpath", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    /**
     * Test a policy with policy logic.
     * @throws Exception
     */
    public void testPolicyLogic() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/policylogic", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    /**
     * Test a policy with regular expression.
     * @throws Exception
     */
    public void testRegularExpression() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/regularexpression", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    /**
     * Test a policy with WSDL Operation.
     * @throws Exception
     */
    public void testWsdlOperation() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/wsdloperation", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    /**
     * Test a policy with XSL transformation request.
     * @throws Exception
     */
    public void testXslTransformationRequest() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/xsltransformationrequest", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    /**
     * Test a policy with XSL transformation response.
     * @throws Exception
     */
    public void testXslTransformationResponse() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/xsltransformationresponse", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    public void testWssSignedRequest() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/wsssign", REQUEST_signed, AssertionStatus.NONE.getNumeric());
    }

    public void testWssEncryptedRequest() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/wssenc", REQUEST_encrypted, AssertionStatus.NONE.getNumeric());
    }

    /**
     *
     */
    private void processMessage(String uri, String message, int expectedStatus) throws IOException {
        processMessage(uri, message, "10.0.0.1", expectedStatus, false);
    }

    /**
     * @param expectedStatus    policy processing status code expected; ignored if set to {@link #ASSERTION_STATUS_NONE}
     */
    private void processMessage(String uri, String message, String requestIp, int expectedStatus, boolean addAuth) throws IOException {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        hrequest.setMethod("POST");
        hrequest.setContentType("text/xml; charset=utf8");
        hrequest.setRemoteAddr(requestIp);
        hrequest.setRequestURI(uri);
        hrequest.setContent(message.getBytes());
        hrequest.addHeader("SOAPAction", "http://warehouse.acme.com/ws/listProducts");
        ConnectionId.setConnectionId(new ConnectionId( 0L, 0L ));
        hrequest.setAttribute("com.l7tech.server.connectionIdentifierObject", ConnectionId.getConnectionId());

        // Initialize processing context
        final Message response = new Message();
        final Message request = new Message();

        final String rawct = hrequest.getContentType();
        ContentTypeHeader ctype = rawct != null && rawct.length() > 0
          ? ContentTypeHeader.parseValue(rawct)
          : ContentTypeHeader.XML_DEFAULT;

        final HttpRequestKnob reqKnob = new HttpServletRequestKnob(hrequest);
        request.attachHttpRequestKnob(reqKnob);

        final HttpServletResponseKnob respKnob = new HttpServletResponseKnob(hresponse);
        response.attachHttpResponseKnob(respKnob);

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

        final StashManager stashManager = TestStashManagerFactory.getInstance().createStashManager();

        AssertionStatus status = AssertionStatus.UNDEFINED;
        try {
            //TODO cleanup cookie init?
            context.addCookie(new HttpCookie("cookie", "invalue", 0, null, null));

            // Process message
            request.initialize(stashManager, ctype, hrequest.getInputStream());

            // Add fake auth if requested
            if (addAuth) {
                UserBean user = new UserBean();
                user.setLogin("test");
                context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(user, new OpaqueSecurityToken()));
            }

            status = messageProcessor.processMessage(context);

            // if the policy is not successful AND the stealth flag is on, drop connection
            if (status != AssertionStatus.NONE) {
                SoapFaultLevel faultLevelInfo = context.getFaultlevel();
                if ( faultLevelInfo==null ) faultLevelInfo = soapFaultManager.getDefaultBehaviorSettings();
                logger.finest("checking for potential connection drop because status is " + status.getMessage());
                if (faultLevelInfo.getLevel() == SoapFaultLevel.DROP_CONNECTION) {
                    logger.info("No policy found and global setting is to go stealth in this case. " +
                                "Instructing valve to drop connection completly." + faultLevelInfo.toString());
                    hrequest.setAttribute(ResponseKillerValve.ATTRIBUTE_FLAG_NAME,
                                          ResponseKillerValve.ATTRIBUTE_FLAG_NAME);
                    throw new CausedIOException(ResponseKillerValve.ATTRIBUTE_FLAG_NAME);
                }
            }

            // Send response headers
            respKnob.beginResponse();

            int routeStat = respKnob.getStatus();
            if (routeStat < 1) {
                if (status == AssertionStatus.NONE) {
                    routeStat = HttpServletResponse.SC_OK;
                } else {
                    // Request wasn't routed
                    routeStat = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
                }
            }

            if (status == AssertionStatus.NONE) {
                if (response.getKnob(MimeKnob.class) == null) {
                    // Routing successful, but no actual response received, probably due to a one-way JMS send.
                    hresponse.setStatus(200);
                    hresponse.setContentType(null);
                    hresponse.setContentLength(0);
                    hresponse.getOutputStream().close();
                    logger.fine("servlet transport returning a placeholder empty response to a successful one-way message");
                } else {
                    // Transmit the response and return
                    hresponse.setStatus(routeStat);
                    hresponse.setContentType(response.getMimeKnob().getOuterContentType().getFullValue());
                    OutputStream responseos = hresponse.getOutputStream();
                    IOUtils.copyStream(response.getMimeKnob().getEntireMessageBodyAsInputStream(), responseos);
                    responseos.close();
                    logger.fine("servlet transport returned status " + routeStat +
                                ". content-type " + response.getMimeKnob().getOuterContentType().getFullValue());
                }
            } else if (respKnob.hasChallenge()) {
                logger.info("Challenge result.");
            } else {
                logger.info("500 (none 200?) result.");
            }
        } catch (Throwable e) {
            if (e instanceof CausedIOException && ResponseKillerValve.ATTRIBUTE_FLAG_NAME.equals( e.getMessage() ) ) {
                // not an error
            } else {
                e.printStackTrace();

                // if the policy throws AND the stealth flag is set, drop connection
                if (context.isStealthResponseMode()) {
                    logger.log(Level.INFO, "Policy threw error and stealth mode is set. " +
                                           "Instructing valve to drop connection completely.",
                                           e);
                    hrequest.setAttribute(ResponseKillerValve.ATTRIBUTE_FLAG_NAME,
                                          ResponseKillerValve.ATTRIBUTE_FLAG_NAME);
                }
            }
        } finally {
            context.close();
            assertEquals("Policy status", expectedStatus, status.getNumeric());
        }
    }

    /**
     *
     */
    private static MockGenericHttpClient buildMockHttpClient(GenericHttpHeaders headers, byte[] message) {
        if (headers == null) {
            HttpHeader[] responseHeaders = new HttpHeader[]{
                    new GenericHttpHeader("Content-Type","text/xml; charset=utf8"),
            };
            headers = new GenericHttpHeaders(responseHeaders);
        }

        return new MockGenericHttpClient(200,
            headers,
            ContentTypeHeader.XML_DEFAULT,
            (long)message.length,
            message);
    }
}
