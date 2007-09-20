package com.l7tech.server;

import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.http.*;
import com.l7tech.common.message.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.xml.SoapFaultLevel;
import com.l7tech.identity.StubDataStore;
import com.l7tech.identity.UserBean;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.AuditContextStubInt;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.tomcat.ResponseKillerValve;
import com.l7tech.server.transport.http.ConnectionId;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.service.PublishedService;
import junit.framework.TestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author rmak
 */
public class PolicyProcessingPerformanceTest extends TestCase {
    private static final Logger logger = Logger.getLogger(TokenServiceTest.class.getName());
    private static final int ASSERTION_STATUS_NONE = AssertionStatus.NONE.getNumeric();
    private static final String POLICY_RES_PATH = "policy/resources/";

    private static ApplicationContext applicationContext = null;
    private static MessageProcessor messageProcessor = null;
    private static AuditContext auditContext = null;
    private static SoapFaultManager soapFaultManager = null;
    private static ClusterPropertyManager clusterPropertyManager = null;
    private static TestingHttpClientFactory testingHttpClientFactory = null;

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
        {"/xsltransformationresponse", "POLICY_xsltransformationresponse.xml"}
    };

    private static String REQUEST_general;
    private static String REQUEST_xpathcreds_success;
    private static String REQUEST_usernametoken_success_1;
    private static String REQUEST_usernametoken_success_2;
    private static String REQUEST_httpwssheaderpromote_success;
    private static String REQUEST_schemaval_request_success;
    private static String REQUEST_schemaval_response_success;

    private static byte[] RESPONSE_general;

    private Level savedLoggerLevel;

    public static void setUpClass() throws Exception {
        // Ordinarily, the application context would take care of configuring the registry,
        // but it has to be done before buildServices() is called, and buildServices() has
        // to be done before the application context is created (at least for this test).
        final AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        tmf.afterPropertiesSet();
        WspConstants.setTypeMappingFinder(tmf);

        buildServices();

        applicationContext = ApplicationContexts.getTestApplicationContext();
        messageProcessor = (MessageProcessor) applicationContext.getBean("messageProcessor", MessageProcessor.class);
        auditContext = (AuditContext) applicationContext.getBean("auditContext", AuditContext.class);
        soapFaultManager = (SoapFaultManager) applicationContext.getBean("soapFaultManager", SoapFaultManager.class);
        clusterPropertyManager = (ClusterPropertyManager) applicationContext.getBean("clusterPropertyManager", ClusterPropertyManager.class);
        testingHttpClientFactory = (TestingHttpClientFactory) applicationContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

        auditContext.flush(); // ensure clear

        REQUEST_general = new String(loadResource("REQUEST_general.xml"));
        REQUEST_xpathcreds_success = new String(loadResource("REQUEST_xpathcreds_success.xml"));
        REQUEST_usernametoken_success_1 = new String(loadResource("REQUEST_usernametoken_success_1.xml"));
        REQUEST_usernametoken_success_2 = new String(loadResource("REQUEST_usernametoken_success_2.xml"));
        REQUEST_httpwssheaderpromote_success = new String(loadResource("REQUEST_httpwssheaderpromote_success.xml"));
        REQUEST_schemaval_request_success = new String(loadResource("REQUEST_schemaval_request.xml"));
        REQUEST_schemaval_response_success = new String(loadResource("REQUEST_schemaval_response_request.xml"));
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
        System.setProperty("com.l7tech.server.serviceResolution.strictSoap", "false");

        // Software-only TransformerFactory to ignore the alluring Tarari impl, even if tarari_raxj.jar is sitting right there
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");

        savedLoggerLevel = Logger.getLogger("com.l7tech.server").getLevel();
        Logger.getLogger("com.l7tech.server").setLevel(Level.OFF);

        setUpClass();
    }

    @Override
    protected void tearDown() throws Exception {
        Logger.getLogger("com.l7tech.server").setLevel(savedLoggerLevel);
    }

    /**
     * Populate the service cache with the test services.
     */
    private static void buildServices() throws Exception {
//        ServiceCache serviceCache = (ServiceCache) applicationContext.getBean("serviceCache", ServiceCache.class);
        long oid = 1;

        Map<Long, PublishedService> services = StubDataStore.defaultStore().getPublishedServices();
        for (String[] serviceInfo : TEST_SERVICES) {
            PublishedService ps = new PublishedService();
            ps.setOid(oid++);
            ps.setName(serviceInfo[0].substring(1));
            ps.setRoutingUri(serviceInfo[0]);
            ps.setPolicyXml(new String(loadResource(serviceInfo[1])));
            ps.setSoap(true);

            if (serviceInfo.length > 2) {
                ps.setWsdlXml(new String(loadResource(serviceInfo[2])));
                ps.setLaxResolution(false);
            } else {
                ps.setLaxResolution(true);
            }

            services.put(ps.getOid(), ps);
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
            return HexUtils.slurpStream(in);
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
        processMessage("/faultlevel", REQUEST_general, ASSERTION_STATUS_NONE /* AssertionStatus.FALSIFIED.getNumeric() */);
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

        Result result = processMessage("/httproutecookie", REQUEST_general, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);

//        assertTrue("Outbound request cookie missing", headerExists(mockClient.getParams().getExtraHeaders(), "Cookie", "cookie=invalue"));
//        assertTrue("Outbound response cookie missing", cookieExists(result.context.getCookies(),"cookie", "outvalue"));
    }

    public void testHttpRoutingCookieNone() throws Exception {
        GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new HttpHeader[]{
                new GenericHttpHeader("Content-Type","text/xml; charset=utf8"),
                new GenericHttpHeader("Set-Cookie","cookie=outvalue"),
        });

        MockGenericHttpClient mockClient2 = buildMockHttpClient(responseHeaders, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient2);

        Result result = processMessage("/httproutenocookie", REQUEST_general, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);

//        assertFalse("Outbound request cookie present", headerExists(mockClient2.getParams().getExtraHeaders(), "Cookie", "cookie=invalue"));
//        assertFalse("Outbound response cookie present", cookieExists(result2.context.getCookies(),"cookie", "outvalue"));
    }

    public void testHttpRoutingSticky() throws Exception {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/httproutepassthru", REQUEST_general, ASSERTION_STATUS_NONE /* AssertionStatus.NONE.getNumeric() */);

//        assertNotNull("Missing connection id", mockClient.getIdentity());
    }

    public void testHttpWssHeaderLeave() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/httpwssheaderleave", REQUEST_usernametoken_success_1, ASSERTION_STATUS_NONE);
//        String request = new String(mockClient.getRequestBody());
//        assertTrue("Security header missing", request.indexOf("<wsse:Username>user</wsse:Username>") > 0);
    }

    public void testHttpWssHeaderRemove() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/httpwssheaderremove", REQUEST_usernametoken_success_1, ASSERTION_STATUS_NONE);
//        String request = new String(mockClient.getRequestBody());
//        assertTrue("Security header not removed", request.indexOf("<wsse:Username>user</wsse:Username>") < 0);
    }

    public void testHttpWssHeaderPromote() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/httpwssheaderpromote", REQUEST_httpwssheaderpromote_success, ASSERTION_STATUS_NONE);
//        String request = new String(mockClient.getRequestBody());
//        assertTrue("Promoted security header missing", request.indexOf("<wsse:Username>user</wsse:Username>") > 0 && request.indexOf("asdf") < 0);
    }

    public void testSchemaValidationRequest() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/schemavalrequest", REQUEST_schemaval_request_success, ASSERTION_STATUS_NONE);
//        String request = new String(mockClient.getRequestBody());
//        assertTrue("Promoted security header missing", request.indexOf("<wsse:Username>user</wsse:Username>") > 0 && request.indexOf("asdf") < 0);
    }

    public void testSchemaValidationResponse() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/schemavalresponse", REQUEST_schemaval_response_success, ASSERTION_STATUS_NONE);
//        String request = new String(mockClient.getRequestBody());
//        assertTrue("Promoted security header missing", request.indexOf("<wsse:Username>user</wsse:Username>") > 0 && request.indexOf("asdf") < 0);
    }

    public void testEmptyPolicy() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/emptypolicy", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    public void testEvaluateRequestXpath() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/evaluaterequestxpath", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    public void testEvaluateResponseXpath() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/evaluateresponsexpath", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    public void testPolicyLogic() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/policylogic", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    public void testRegularExpression() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/regularexpression", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    public void testWsdlOperation() throws Exception  {
        // setUpClass();
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/wsdloperation", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    public void testXslTransformationRequest() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/xsltransformationrequest", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    public void testXslTransformationResponse() throws Exception  {
        MockGenericHttpClient mockClient = buildMockHttpClient(null, RESPONSE_general);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/xsltransformationresponse", REQUEST_general, ASSERTION_STATUS_NONE);
    }

    /**
     *
     */
    private PolicyProcessingPerformanceTest.Result processMessage(String uri, String message, int expectedStatus) throws IOException {
        return processMessage(uri, message, "10.0.0.1", expectedStatus, false);
    }

    /**
     * @param expectedStatus    policy processing status code expected; ignored if set to {@link #ASSERTION_STATUS_NONE}
     */
    private PolicyProcessingPerformanceTest.Result processMessage(String uri, String message, String requestIp, int expectedStatus, boolean addAuth) throws IOException {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        hrequest.setMethod("POST");
        hrequest.setContentType("text/xml; charset=utf8");
        hrequest.setRemoteAddr(requestIp);
        hrequest.setRequestURI(uri);
        hrequest.setContent(message.getBytes());
        hrequest.addHeader("SOAPAction", "http://warehouse.acme.com/ws/listProducts");
        ConnectionId.setConnectionId(new ConnectionId(0,0));
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

        final PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setReplyExpected(true); // HTTP always expects to receive a reply

        final StashManager stashManager = TestStashManagerFactory.getInstance().createStashManager();

        AssertionStatus status = AssertionStatus.UNDEFINED;
        try {
            context.setAuditContext(auditContext);
            context.setSoapFaultManager(soapFaultManager);
            context.setClusterPropertyManager(clusterPropertyManager);

            //TODO cleanup cookie init?
            context.addCookie(new HttpCookie("cookie", "invalue", 0, null, null));

            // Process message
            request.initialize(stashManager, ctype, hrequest.getInputStream());

            // Add fake auth if requested
            if (addAuth) {
                UserBean user = new UserBean();
                user.setLogin("test");
                user.setCleartextPassword("password");
                context.addAuthenticationResult(new AuthenticationResult(user));
            }

            status = messageProcessor.processMessage(context);

            // if the policy is not successful AND the stealth flag is on, drop connection
            if (status != AssertionStatus.NONE) {
                SoapFaultLevel faultLevelInfo = context.getFaultlevel();
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
                    HexUtils.copyStream(response.getMimeKnob().getEntireMessageBodyAsInputStream(), responseos);
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
            if (e instanceof CausedIOException && e.getMessage() == ResponseKillerValve.ATTRIBUTE_FLAG_NAME) {
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
            try {
                auditContext.flush();
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected exception when flushing audit data.", e);
            }
            finally {
                context.close();
            }

            if (expectedStatus != ASSERTION_STATUS_NONE)
                assertEquals("Policy status", expectedStatus, status.getNumeric());
        }

        return new PolicyProcessingPerformanceTest.Result(context, hresponse, ((AuditContextStubInt) auditContext).getLastRecord());
    }

    /**
     *
     */
    private PolicyProcessingPerformanceTest.Result processJmsMessage(String message, int expectedStatus) throws IOException {
        // Initialize processing context
        final Message response = new Message();
        final Message request = new Message();

        ContentTypeHeader ctype = ContentTypeHeader.XML_DEFAULT;
        try {
            request.initialize(TestStashManagerFactory.getInstance().createStashManager(), ctype, new ByteArrayInputStream(message.getBytes()) );
            request.attachJmsKnob(new JmsKnob() {
                public boolean isBytesMessage() {
                    return true;
                }
                public Map<String, Object> getJmsMsgPropMap() {
                    //noinspection unchecked
                    return Collections.EMPTY_MAP;
                }
                public String getSoapAction() {
                    return null;
                }
            });
        } catch(NoSuchPartException nspe) {
            throw new CausedIOException("Mime init error", nspe);
        }

        final PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setReplyExpected(true); // HTTP always expects to receive a reply

        AssertionStatus status = AssertionStatus.UNDEFINED;
        try {
            context.setAuditContext(auditContext);
            context.setSoapFaultManager(soapFaultManager);
            context.setClusterPropertyManager(clusterPropertyManager);

            status = messageProcessor.processMessage(context);

            // if the policy is not successful AND the stealth flag is on, drop connection
            if (status != AssertionStatus.NONE) {
                SoapFaultLevel faultLevelInfo = context.getFaultlevel();

                if (logger.isLoggable(Level.FINEST))
                    logger.finest("checking for potential connection drop because status is " + status.getMessage());
                if (faultLevelInfo.getLevel() == SoapFaultLevel.DROP_CONNECTION) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("No policy found and global setting is to go stealth in this case. " +
                                    "Instructing valve to drop connection completly." + faultLevelInfo.toString());
                    throw new CausedIOException(ResponseKillerValve.ATTRIBUTE_FLAG_NAME);
                }
            }

            if (status == AssertionStatus.NONE) {
                if (response.getKnob(MimeKnob.class) == null) {
                    // Routing successful, but no actual response received, probably due to a one-way JMS send.
                    logger.fine("servlet transport returning a placeholder empty response to a successful one-way message");
                } else {
                    // Transmit the response and return
                    logger.fine("servlet transport returned status ?" +
                                ". content-type " + response.getMimeKnob().getOuterContentType().getFullValue());
                }
            } else {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("500 (none 200?) result.");
            }
        } catch (Throwable e) {
            if (e instanceof CausedIOException && e.getMessage() == ResponseKillerValve.ATTRIBUTE_FLAG_NAME) {
                // not an error
            } else {
                e.printStackTrace();

                // if the policy throws AND the stealth flag is set, drop connection
                if (context.isStealthResponseMode()) {
                    logger.log(Level.INFO, "Policy threw error and stealth mode is set. " +
                                           "Instructing valve to drop connection completely.",
                                           e);
                }
            }
        } finally {
            try {
                auditContext.flush();
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Unexpected exception when flushing audit data.", e);
            }
            finally {
                context.close();
            }

            assertEquals("Policy status", expectedStatus, status.getNumeric());
        }

        return new PolicyProcessingPerformanceTest.Result(context, null, ((AuditContextStubInt) auditContext).getLastRecord());
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

        MockGenericHttpClient mockClient = new MockGenericHttpClient(200,
                                                                     headers,
                                                                     ContentTypeHeader.XML_DEFAULT,
                                                                     new Long(message.length),
                                                                     message);

        return mockClient;
    }

    /**
     *
     */
    private static boolean headerExists(List headers, String headername, String headervaluecontains) {
        boolean exists = false;

        if (headers != null) {
            for (Iterator headerIter = headers.iterator(); headerIter.hasNext(); ) {
                Object headerObj = headerIter.next();
                if (headerObj instanceof HttpHeader) {
                    HttpHeader header = (HttpHeader) headerObj;
                    if (headername.equals(header.getName()) &&
                        (headervaluecontains == null || (header.getFullValue()!=null && header.getFullValue().indexOf(headervaluecontains)>=0))) {
                        exists = true;
                        break;
                    }
                }
            }
        }

        return exists;
    }

    /**
     *
     */
    private static boolean cookieExists(Set<HttpCookie> cookies, String name, String value) {
        boolean exists = false;

        if (cookies != null) {
            for (HttpCookie cookie : cookies) {
                if (name.equals(cookie.getCookieName()) &&
                    (value == null || value.equals(cookie.getCookieValue()))) {
                    exists = true;
                    break;
                }
            }
        }

        return exists;
    }

    /**
     *
     */
    private static final class Result {
        private PolicyEnforcementContext context;
        private MockHttpServletResponse response;
        private AuditRecord audit;

        Result(PolicyEnforcementContext context, MockHttpServletResponse response, AuditRecord audit) {
            this.context = context;
            this.response = response;
            this.audit = audit;
        }
    }
}
