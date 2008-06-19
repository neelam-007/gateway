package com.l7tech.server;

import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.http.*;
import com.l7tech.common.message.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.xml.SoapFaultLevel;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.identity.StubDataStore;
import com.l7tech.identity.UserBean;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.tomcat.ResponseKillerValve;
import com.l7tech.server.transport.http.ConnectionId;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.service.PublishedService;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Functional tests for message processing.
 *
 * @author Steve Jones
 */
public class PolicyProcessingTest extends TestCase {
    private static final Logger logger = Logger.getLogger(TokenServiceTest.class.getName());
    private static final String POLICY_RES_PATH = "policy/resources/";

    private static ApplicationContext applicationContext = null;
    private static MessageProcessor messageProcessor = null;
    private static AuditContext auditContext = null;
    private static SoapFaultManager soapFaultManager = null;
    private static ClusterPropertyManager clusterPropertyManager = null;
    private static TestingHttpClientFactory testingHttpClientFactory = null;

    static {
        System.setProperty(JceProvider.ENGINE_PROPERTY, JceProvider.BC_ENGINE);
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
        {"/stealthfault", "POLICY_stealthfault.xml"},
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
        {"/attachment", "POLICY_signed_attachment.xml"},
    };

    /**
     *
     */
    public PolicyProcessingTest(String name) {
        super(name);
        System.setProperty("com.l7tech.server.serviceResolution.strictSoap", "false");
    }

    @Override
    protected void setUp() throws Exception {
        // Software-only TransformerFactory to ignore the alluring Tarari impl, even if tarari_raxj.jar is sitting right there
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
    }

    /**
     *
     */
    public static Test suite() {
         final TestSuite suite = new TestSuite(PolicyProcessingTest.class);
         return new TestSetup(suite) {

             @Override
             protected void setUp() throws Exception {
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

                 ServiceCache cache = (ServiceCache) applicationContext.getBean("serviceCache", ServiceCache.class);
                 cache.initiateIntegrityCheckProcess(); Thread.sleep(4500); // sleep to wait for integrity check

                 auditContext.flush(); // ensure clear
             }
         };
    }

    /**
     *
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Populate the service cache with the test services.
     */
    private static void buildServices() throws Exception {
        long oid = 1;

        Map<Long, PublishedService> services = StubDataStore.defaultStore().getPublishedServices();
        for (String[] serviceInfo : TEST_SERVICES) {
            PublishedService ps = new PublishedService();
            ps.setOid(oid++);
            ps.setName(serviceInfo[0].substring(1));
            ps.setRoutingUri(serviceInfo[0]);
            ps.getPolicy().setXml(new String(loadResource(serviceInfo[1])));
            ps.getPolicy().setOid(ps.getOid());
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
            in = PolicyProcessingTest.class.getResourceAsStream(resourcePath);
            if (in == null)
                throw new IOException("Could not find resource '"+resourcePath+"'.");
            return HexUtils.slurpStream(in);
        }
        finally {
            ResourceUtils.closeQuietly(in);
        }
    }

    /**
     * Test rejection of message with SQL injection.
     */
    public void testSQLAttack() throws Exception  {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        String requestMessage2 = new String(loadResource("REQUEST_sqlattack_fail.xml"));

        processMessage("/sqlattack", requestMessage1,  0);
        processMessage("/sqlattack", requestMessage2,  AssertionStatus.BAD_REQUEST.getNumeric());
    }

    /**
     * Test large message rejection.
     */
    public void testRequestSizeLimit() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        String requestMessage2 = new String(loadResource("REQUEST_requestsizelimit_fail.xml"));

        processMessage("/requestsizelimit", requestMessage1,  0);
        processMessage("/requestsizelimit", requestMessage2, AssertionStatus.FALSIFIED.getNumeric());
    }

    /**
     * Test rejection of messages with dubious structure
     */
    public void testDocumentStructure() throws Exception {
        GlobalTarariContext tgc = TarariLoader.getGlobalContext();
        if (tgc != null) {
            // Make sure document statistics collection is initialized
            tgc.compileAllXpaths();
        }

        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        String requestMessage2 = new String(loadResource("REQUEST_documentstructure_fail.xml"));

        processMessage("/documentstructure", requestMessage1, 0);
        processMessage("/documentstructure", requestMessage2, AssertionStatus.BAD_REQUEST.getNumeric());
    }

    /**
     * Test stealth fault (upgraded to fault level assertion)
     */
    public void testStealthFault() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));

        Result result = processMessage("/stealthfault", requestMessage1, AssertionStatus.FALSIFIED.getNumeric());
        assertEquals("Should be stealth response mode.", true, result.context.isStealthResponseMode());
    }

    /**
     * Test fault level
     */
    public void testFaultLevel() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));

        Result result = processMessage("/faultlevel", requestMessage1, AssertionStatus.FALSIFIED.getNumeric());
        assertEquals("Should be stealth response mode.", true, result.context.isStealthResponseMode());
    }

    /**
     * Test rejection of requests from bad IP address
     */
    public void testIPAddressRange() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));

        processMessage("/ipaddressrange", requestMessage1, 0);
        processMessage("/ipaddressrange", requestMessage1, "10.0.0.2", AssertionStatus.FALSIFIED.getNumeric(), false);
    }

    /**
     * Test xpath credentials are found
     */
    public void testXPathCredentials() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_xpathcreds_success.xml"));
        String requestMessage2 = new String(loadResource("REQUEST_general.xml"));

        processMessage("/xpathcreds", requestMessage1, 0);
        processMessage("/xpathcreds", requestMessage2, AssertionStatus.AUTH_REQUIRED.getNumeric());
    }

    /**
     * Test username token messages (incl without password)
     */
    public void testUsernameToken() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_usernametoken_success_1.xml"));
        String requestMessage2 = new String(loadResource("REQUEST_usernametoken_success_2.xml"));
        String requestMessage3 = new String(loadResource("REQUEST_general.xml"));

        processMessage("/usernametoken", requestMessage1, 0);
        processMessage("/usernametoken", requestMessage2, 0);
        processMessage("/usernametoken", requestMessage3, AssertionStatus.AUTH_REQUIRED.getNumeric());
    }

    /**
     * Test cookie pass-thru (or not)
     */
    public void testHttpRoutingCookie() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");

        GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new HttpHeader[]{
                new GenericHttpHeader("Content-Type","text/xml; charset=utf8"),
                new GenericHttpHeader("Set-Cookie","cookie=outvalue"),
        });

        MockGenericHttpClient mockClient = buildMockHttpClient(responseHeaders, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        Result result = processMessage("/httproutecookie", requestMessage1, 0);

        assertTrue("Outbound request cookie missing", headerExists(mockClient.getParams().getExtraHeaders(), "Cookie", "cookie=invalue"));
        assertTrue("Outbound response cookie missing", newCookieExists(result.context.getCookies(),"cookie", "outvalue"));

        MockGenericHttpClient mockClient2 = buildMockHttpClient(responseHeaders, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient2);

        Result result2 = processMessage("/httproutenocookie", requestMessage1, 0);

        assertFalse("Outbound request cookie present", headerExists(mockClient2.getParams().getExtraHeaders(), "Cookie", "cookie=invalue"));
        assertFalse("Outbound response cookie present", newCookieExists(result2.context.getCookies(),"cookie", "outvalue"));
    }

    /**
     * Test that TAI info is routed
     */
    public void testHttpRoutingTAI() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/httproutetaicredchain", requestMessage1, "10.0.0.1", 0, true);

        assertTrue("Outbound request TAI header missing", headerExists(mockClient.getParams().getExtraHeaders(), "IV_USER", "test"));
        assertTrue("Outbound request TAI cookie missing", headerExists(mockClient.getParams().getExtraHeaders(), "Cookie", "IV_USER=test"));
    }

    /**
     * Test connection id propagation
     */
    public void testHttpRoutingSticky() throws Exception {
        //This just tests that the context info gets to the CommonsHttpClient
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/httproutepassthru", requestMessage1, 0);

        assertNotNull("Missing connection id", mockClient.getIdentity());
    }

    /**
     * Test HTTP routing for JMS message
     */
    public void testHttpRoutingJmsIn() throws Exception {
        //This just tests that the context info gets to the CommonsHttpClient
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processJmsMessage(requestMessage1, 0);
    }

    /**
     * Test WSS header handling
     */
    public void testHttpRoutingWssHeader() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_usernametoken_success_1.xml"));
        String requestMessage2 = new String(loadResource("REQUEST_httpwssheaderpromote_success.xml"));

        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/httpwssheaderleave", requestMessage1, 0);
        String request1 = new String(mockClient.getRequestBody());
        assertTrue("Security header missing", request1.indexOf("<wsse:Username>user</wsse:Username>")>0);

        processMessage("/httpwssheaderremove", requestMessage1, 0);
        String request2 = new String(mockClient.getRequestBody());
        assertTrue("Security header not removed", request2.indexOf("<wsse:Username>user</wsse:Username>")<0);

        processMessage("/httpwssheaderpromote", requestMessage2, 0);
        String request3 = new String(mockClient.getRequestBody());
        assertTrue("Promoted security header missing", request3.indexOf("<wsse:Username>user</wsse:Username>")>0 && request3.indexOf("asdf")<0);
    }

    /**
     * Test WSS Signed Attachment processing
     */
    public void testWssSignedAttachment() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_signed_attachment.txt"));

        processMessage("/attachment", requestMessage1, 0);
    }

    /**
     * Test WSS Signed Attachment processing failure
     */
    public void testWssSignedAttachmentFailure() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_unsigned_attachment.txt"));

        processMessage("/attachment", requestMessage1, 600);
    }

    /**
     *
     */
    private Result processMessage(String uri, String message, int expectedStatus) throws IOException {
        return processMessage(uri, message, "10.0.0.1", expectedStatus, false);
    }

    /**
     *
     */
    private Result processMessage(String uri, String message, String requestIp, int expectedStatus, boolean addAuth) throws IOException {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        hrequest.setMethod("POST");
        if ( message.indexOf("Content-ID: ") < 0 ) {
            hrequest.setContentType("text/xml; charset=utf8");
        } else {
            String boundary = message.substring(2, message.indexOf('\n'));
            String contentType = "multipart/related; type=\"text/xml\"; boundary=\"" + boundary + "\"";
            System.out.println("Set content type to: "+ contentType);
            hrequest.setContentType(contentType);
        }
        hrequest.setRemoteAddr(requestIp);
        hrequest.setRequestURI(uri);
        hrequest.setContent(message.getBytes());
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
            //noinspection StringEquality
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

            assertEquals("Policy status", expectedStatus, status.getNumeric());
        }

        return new Result(context);
    }

    /**
     *
     */
    private Result processJmsMessage(String message, int expectedStatus) throws IOException {
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
                logger.finest("checking for potential connection drop because status is " + status.getMessage());
                if (faultLevelInfo.getLevel() == SoapFaultLevel.DROP_CONNECTION) {
                    logger.info("No policy found and global setting is to go stealth in this case. " +
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
                logger.info("500 (none 200?) result.");
            }
        } catch (Throwable e) {
            //noinspection StringEquality
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

        return new Result(context);
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

    /**
     *
     */
    private static boolean headerExists(List headers, String headername, String headervaluecontains) {
        boolean exists = false;

        if (headers != null) {
            for( Object headerObj : headers ) {
                if( headerObj instanceof HttpHeader ) {
                    HttpHeader header = (HttpHeader) headerObj;
                    if( headername.equals( header.getName() ) &&
                            ( headervaluecontains == null || ( header.getFullValue() != null && header.getFullValue().indexOf( headervaluecontains ) >= 0 ) ) ) {
                        exists = true;
                        break;
                    }
                }
            }
        }

        return exists;
    }

    /**
     * Check that there is a new cookie with the given name / value
     */
    private static boolean newCookieExists(Set<HttpCookie> cookies, String name, String value) {
        boolean exists = false;

        if (cookies != null) {
            for (HttpCookie cookie : cookies) {
                if (cookie.isNew() &&
                    name.equals(cookie.getCookieName()) &&
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

        Result(PolicyEnforcementContext context) {
            this.context = context;
        }
    }
}
