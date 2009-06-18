package com.l7tech.server;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.UserBean;
import com.l7tech.message.*;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.TestIdentityProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.service.ServiceCacheStub;
import com.l7tech.server.tomcat.ResponseKillerValve;
import com.l7tech.server.transport.http.ConnectionId;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.server.policy.assertion.credential.DigestSessions;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.GlobalTarariContext;
import com.l7tech.test.BugNumber;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Assert;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.PasswordAuthentication;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

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
    private static ClusterPropertyCache clusterPropertyCache = null;
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
        {"/encusernametoken", "POLICY_wss_encryptedusernametoken.xml"},
        {"/httpbasic", "POLICY_httpbasic.xml"},
        {"/httproutecookie", "POLICY_httproutecookie.xml"},
        {"/httproutenocookie", "POLICY_httproutenocookie.xml"},
        {"/httproutetaicredchain", "POLICY_httproutetaicredchain.xml"},
        {"/httproutepassthru", "POLICY_httproutepassthru.xml"},
        {"/httproutejms", "POLICY_httproutejms.xml", "WSDL_httproutejms.wsdl"},
        {"/httpwssheaderleave", "POLICY_httpwssheaderleave.xml"},
        {"/httpwssheaderremove", "POLICY_httpwssheaderremove.xml"},
        {"/httpwssheaderpromote", "POLICY_httpwssheaderpromote.xml"},
        {"/attachment", "POLICY_signed_attachment.xml"},
        {"/requestnonxmlok", "POLICY_request_modified_non_xml.xml"},
        {"/httpdigestauth", "POLICY_twohttpdigestauth.xml"},
        {"/x509token", "POLICY_wss_x509credssignedbody.xml"},
        {"/multiplesignatures", "POLICY_multiplesignatures.xml"},
        {"/multiplesignaturesnoid", "POLICY_multiplesignature_noidentity.xml"},
        {"/multiplesignaturestags", "POLICY_multiplesignatures_idtags.xml"},
        {"/wssDecoration1", "POLICY_requestdecoration1.xml"},
        {"/wssDecoration2", "POLICY_requestdecoration2.xml"},
        {"/threatprotections", "POLICY_threatprotections.xml"},
        {"/removeelement", "POLICY_removeelement.xml"},
        {"/addusernametoken", "POLICY_responsesecuritytoken.xml"},
        {"/addtimestamp", "POLICY_responsetimestamp.xml"},
        {"/addsignature", "POLICY_responsesignature.xml"}
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
                 buildUsers();

                 applicationContext = ApplicationContexts.getTestApplicationContext();
                 messageProcessor = (MessageProcessor) applicationContext.getBean("messageProcessor", MessageProcessor.class);
                 auditContext = (AuditContext) applicationContext.getBean("auditContext", AuditContext.class);
                 soapFaultManager = (SoapFaultManager) applicationContext.getBean("soapFaultManager", SoapFaultManager.class);
                 clusterPropertyCache = (ClusterPropertyCache) applicationContext.getBean("clusterPropertyCache", ClusterPropertyCache.class);
                 testingHttpClientFactory = (TestingHttpClientFactory) applicationContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

                 ServiceCacheStub cache = (ServiceCacheStub) applicationContext.getBean("serviceCache", ServiceCacheStub.class);
                 cache.initializeServiceCache();

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

    private static void buildUsers() {
        UserBean ub1 = new UserBean(9898, "Alice");
        ub1.setUniqueIdentifier( "4718592" );
        TestIdentityProvider.addUser(ub1, "Alice", "password".toCharArray(), "CN=Alice, OU=OASIS Interop Test Cert, O=OASIS");

        UserBean ub2 = new UserBean(9898, "Bob");
        ub2.setUniqueIdentifier( "4718593" );
        TestIdentityProvider.addUser(ub2, "Bob", "password".toCharArray(), "CN=Bob, OU=OASIS Interop Test Cert, O=OASIS");
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
            return IOUtils.slurpStream(in);
        }
        finally {
            ResourceUtils.closeQuietly(in);
        }
    }

    /**
     * Test case for having two Http Digest assertions
     *
      * @throws Exception
     */
    @SuppressWarnings({"unchecked"})
    public void testTwoHttpDigestAuth() throws Exception {
        String requestMessage = new String(loadResource("REQUEST_general.xml"));

        //need to register the nonce so that we'll always be using the same nonce = 70ec76c747e23906120eec731341660f
        //create new instance of nonce info so that it can be registered into the digest session
        String nonce = "70ec76c747e23906120eec731341660f";
        Class classNonceInfo = Class.forName("com.l7tech.server.policy.assertion.credential.DigestSessions$NonceInfo");
        Constructor constructor = classNonceInfo.getDeclaredConstructor(String.class, Long.TYPE, Integer.TYPE);
        constructor.setAccessible(true); //suppress Java language accesschecking

        DigestSessions digestSession = DigestSessions.getInstance();
        Field nonceInfoField = DigestSessions.class.getDeclaredField("_nonceInfos");
        nonceInfoField.setAccessible(true);
        Map<String,Object> nonceInfo = (Map<String,Object>) nonceInfoField.get(digestSession);    //grab the field from the digest session

        //register the nonce
        nonceInfo.put(nonce, constructor.newInstance(nonce, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1), 3));

        //create request header for http digest
        String authHeader = "Digest username=\"testuser2\", realm=\"L7SSGDigestRealm\", nonce=\"70ec76c747e23906120eec731341660f\", " +
                "uri=\"/ssg/soap\", response=\"326f367c241545fd0628bc0becf5948e\", qop=auth, nc=00000001, " +
                "cnonce=\"c1f102ea2080f3694288f0841cbfc1b0\", opaque=\"2f9e9d78e4ec2de1258ee75634badb41\"";


        processMessage("/httpdigestauth", requestMessage, "10.0.0.1", AssertionStatus.AUTH_REQUIRED.getNumeric(), null, null);
        processMessage("/httpdigestauth", requestMessage, "10.0.0.1", AssertionStatus.NONE.getNumeric(), null, authHeader); 
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
        processMessage("/ipaddressrange", requestMessage1, "10.0.0.2", AssertionStatus.FALSIFIED.getNumeric(), null, null);
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
     * Test encrypted username token
     */
    public void testEncryptedUsernameToken() throws Exception {
        String requestMessage = new String(loadResource("REQUEST_encryptedusernametoken.xml"));
        processMessage("/encusernametoken", requestMessage, 0);
    }

    /**
     * Test http basic auth with latin-1 charset
     */
    public void testHttpBasic() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));

        String username = "\u00e9\u00e2\u00e4\u00e5";
        PasswordAuthentication pa = new PasswordAuthentication(username, "password".toCharArray());
        String authHeader = "Basic " + HexUtils.encodeBase64( (pa.getUserName() + ":" + new String(pa.getPassword())).getBytes("ISO-8859-1") );
        Result result = processMessage("/httpbasic", requestMessage1, "10.0.0.1", 0, null, authHeader);
        assertTrue("Credential present", !result.context.getDefaultAuthenticationContext().getCredentials().isEmpty());
        assertEquals("Username correct", username, result.context.getDefaultAuthenticationContext().getCredentials().iterator().next().getLogin());
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

        processMessage("/httproutetaicredchain", requestMessage1, "10.0.0.1", 0, new PasswordAuthentication("test", "password".toCharArray()), null);

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
     * Bug #5725: Make sure the correct response is returned in the following scenario:
     * - the request is non-xml (or is modified to be non-xml by the policy)
     * - all policies return success
     * - respnse is SOAP and needs to be decorated
     */
    public void testRequestNonXmlOk() throws Exception
    {
        processMessage("/requestnonxmlok", new String(loadResource("REQUEST_general.xml")), 0);
    }

    /**
     * Test multiple request/response signatures
     */
    public void testMultipleSignatures() throws Exception {
        byte[] responseMessage1 = loadResource("REQUEST_multiplesignatures.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/multiplesignatures", new String(loadResource("REQUEST_multiplesignatures.xml")), 0);
    }

    public void testWssMessageAttributes() throws Exception {
        // build test request and run WSS processor on it
        String message = new String(loadResource("REQUEST_multiplesignatures.xml"));
        final Message request = new Message();
        request.initialize(ContentTypeHeader.XML_DEFAULT, message.getBytes());
        request.getSecurityKnob().setProcessorResult(
            WSSecurityProcessorUtils.getWssResults(request, "multiple signatures test request", new SimpleSecurityTokenResolver(), new LogOnlyAuditor(logger))
        );

        assertEquals("2", getRequestAttribute(request, "request.wss.certificates.count", null));
        assertEquals("CN=OASIS Interop Test CA, O=OASIS", getRequestAttribute(request, "request.wss.certificates.value.1.issuer", null));
        assertEquals("cn=oasis interop test ca,o=oasis", getRequestAttribute(request, "request.wss.certificates.value.1.issuer.canonical", null));
        assertEquals("O=OASIS", getRequestAttribute(request, "request.wss.certificates.value.1.issuer.dn.2", null));
        assertEquals("OASIS Interop Test CA", getRequestAttribute(request, "request.wss.certificates.value.2.issuer.dn.cn", null));
        assertEquals("", getRequestAttribute(request, "request.wss.certificates.value.2.issuerAltNameEmail", null));
        assertEquals("NQM0IBvuplAtETQvk+6gn8C13wE=", getRequestAttribute(request, "request.wss.certificates.value.2.thumbprintSHA1", null));

        assertEquals("2", getRequestAttribute(request, "request.wss.signingcertificates.count", null));
        assertEquals("CN=Bob, OU=OASIS Interop Test Cert, O=OASIS", getRequestAttribute(request, "request.wss.signingcertificates.value.2.subject", null));
        assertEquals("cn=bob,ou=oasis interop test cert,o=oasis", getRequestAttribute(request, "request.wss.signingcertificates.value.2.subject.canonical", null));
        assertEquals("OU=OASIS Interop Test Cert", getRequestAttribute(request, "request.wss.signingcertificates.value.2.subject.dn.2", null));
        assertEquals("OASIS Interop Test Cert", getRequestAttribute(request, "request.wss.signingcertificates.value.1.subject.dn.ou", null));
        assertEquals("", getRequestAttribute(request, "request.wss.signingcertificates.value.1.subjectAltNameEmail", null));
        assertEquals("bg6I8267h0TUcPYvYE0D6k6+UJQ=", getRequestAttribute(request, "request.wss.signingcertificates.value.1.thumbprintSHA1", null));


        // test extraction of legacy wss attribute names (manually added instead of RequireWssX509Cert assertion)
        Object firstCert = getRequestAttribute(request, "request.wss.signingcertificates.value.1", null);
        assertEquals(firstCert, getRequestAttribute(request, "request.wss.signingcertificate", new Pair<String,Object>("request.wss.signingcertificate", firstCert)));

        Object firstCertBase64 = getRequestAttribute(request, "request.wss.signingcertificates.value.1.base64", null);
        assertEquals(firstCertBase64, getRequestAttribute(request, "request.wss.signingcertificate.base64", new Pair<String,Object>("request.wss.signingcertificate.base64", firstCertBase64)));

        Object firstCertPem = getRequestAttribute(request, "request.wss.signingcertificates.value.1.pem", null);
        assertEquals(firstCertPem, getRequestAttribute(request, "request.wss.signingcertificate.pem", new Pair<String,Object>("request.wss.signingcertificate.pem", firstCertPem)));
    }

    private Object getRequestAttribute(final Message request, String attributeName, Pair<String,Object> extraVars) {
        Map<String,Object> vars = new HashMap<String, Object>();
        vars.put("request", request);
        if (extraVars != null) {
            vars.put(extraVars.getKey(), extraVars.getValue());
        }
        Object result = ExpandVariables.processSingleVariableAsDisplayableObject("${" + attributeName + "}", vars, new LogOnlyAuditor(logger));
        System.out.println("\n-----------------------------------\n" + attributeName + ": " + result);
        return result;
    }

    /**
     * Test failure with wrong signing identities in request message
     */
    public void testMultipleSignaturesWrongIdentitiesRequest() throws Exception {
        byte[] responseMessage1 = loadResource("REQUEST_multiplesignatures.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/multiplesignatures", new String(loadResource("REQUEST_multiplesignatures2.xml")), 600);
    }

    /**
     * Test multiple request/response signatures with identity tags
     */
    public void testMultipleSignaturesWithIdTags() throws Exception {
        byte[] responseMessage1 = loadResource("REQUEST_multiplesignatures.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/multiplesignaturestags", new String(loadResource("REQUEST_multiplesignatures.xml")), 0);
    }

    /**
     * Test failure with wrong signing identities in response message
     */
    public void testMultipleSignaturesWrongIdentitiesResponse() throws Exception {
        byte[] responseMessage1 = loadResource("REQUEST_multiplesignatures2.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/multiplesignatures", new String(loadResource("REQUEST_multiplesignatures.xml")), 600);
    }

    /**
     * Test policy failure on missing response signatures
     */
    public void testMultipleSignaturesMissingResponseSignatures() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/multiplesignatures", new String(loadResource("REQUEST_multiplesignatures.xml")), 600);
    }

    /**
     * Test policy failure when there are multiple request signatures and
     * the identity is not specified for a Require Signed Element assertion.
     */
    public void testMultipleSignaturesNoIdentity() throws Exception {
        processMessage("/multiplesignaturesnoid", new String(loadResource("REQUEST_multiplesignatures.xml")), 600);
    }

    /**
     * Test multiple request signatures are rejected by WSS X.509 assertion when not enabled.
     */
    public void testMultipleSignaturesRejected() throws Exception {
        processMessage("/x509token", new String(loadResource("REQUEST_multiplesignatures.xml")), 400);
    }

    /**
     * Test multiple request signatures are rejected by WSS X.509 assertion when not enabled.
     */
    @BugNumber(7285)
    public void testEncryptedKeyWithX509TokenRejected() throws Exception {
        // Test with com.l7tech.server.policy.requireSigningTokenCredential=false for old behaviour
        processMessage("/x509token", new String(loadResource("REQUEST_signed_x509_and_encryptedkey.xml")), 600);
    }

    /**
     * Test success on applying a signature to the request message using the WssDecoration assertion.
     */
    public void testDecorationCommitOnRequest() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/wssDecoration1", new String(loadResource("REQUEST_general.xml")), 0);
    }

    /**
     * Test success on adding a signature that endorses an existing signature from the request
     */
    public void testEndorsingRequest() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/wssDecoration2", new String(loadResource("REQUEST_decoration2.xml")), 0);
    }

    /**
     * Test running threat protections for request, variable and response messages
     */
    public void testThreatProtectionsRequestResponseAndMessageTarget() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/threatprotections", new String(loadResource("REQUEST_general.xml")), 0);
    }

    /**
     * Test removal of an XML element with XPath selection 
     */
    public void testRemoveElement() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        Assert.assertTrue("Request message contains element to remove", XmlUtil.parse(requestMessage).getElementsByTagNameNS("http://warehouse.acme.com/ws", "delay").getLength() > 0);
        processMessage("/removeelement", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                try {
                    Assert.assertEquals("Request message elements removed", 0, context.getRequest().getXmlKnob().getDocumentReadOnly().getElementsByTagNameNS("http://warehouse.acme.com/ws", "delay").getLength());
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });
    }

    /**
     * Test addition of a username token to the response with creds from xpathcredsource
     */
    public void testAddSecurityToken() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        processMessage("/addusernametoken", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                try {
                    final Document document = context.getResponse().getXmlKnob().getDocumentReadOnly();
                    String wsseNS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
                    String dsigNS = "http://www.w3.org/2000/09/xmldsig#";
                    NodeList userNodeList = document.getElementsByTagNameNS(wsseNS, "Username");
                    NodeList passNodeList = document.getElementsByTagNameNS(wsseNS, "Password");
                    Assert.assertEquals("Username found", 1, userNodeList.getLength());
                    Assert.assertEquals("Password found", 1, passNodeList.getLength());
                    Assert.assertEquals("Username", "steve", XmlUtil.getTextValue((Element)userNodeList.item(0)));
                    Assert.assertEquals("Password", "password", XmlUtil.getTextValue((Element)passNodeList.item(0)));
                    NodeList issuerSerialNodeList = document.getElementsByTagNameNS(dsigNS, "X509IssuerSerial");
                    Assert.assertEquals("Issuer/Serial STR found", 1, issuerSerialNodeList.getLength());
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });
    }

    /**
     * Test addition of a signed timestamp to the response
     */
    public void testAddTimestamp() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        processMessage("/addtimestamp", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                try {
                    final Document document = context.getResponse().getXmlKnob().getDocumentReadOnly();
                    String wsuNS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
                    String dsigNS = "http://www.w3.org/2000/09/xmldsig#";
                    NodeList timestampNodeList = document.getElementsByTagNameNS(wsuNS, "Timestamp");
                    Assert.assertEquals("Timestamp found", 1, timestampNodeList.getLength());
                    NodeList issuerSerialNodeList = document.getElementsByTagNameNS(dsigNS, "X509IssuerSerial");
                    Assert.assertEquals("Issuer/Serial STR found", 1, issuerSerialNodeList.getLength());
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });
    }

    /**
     * Test addition of a signature to the response
     */
    public void testAddSignature() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        processMessage("/addsignature", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                try {
                    final Document document = context.getResponse().getXmlKnob().getDocumentReadOnly();
                    String dsigNS = "http://www.w3.org/2000/09/xmldsig#";
                    NodeList signatureNodeList = document.getElementsByTagNameNS(dsigNS, "Signature");
                    Assert.assertEquals("Signature found", 1, signatureNodeList.getLength());
                    NodeList signatureReferenceNodeList = document.getElementsByTagNameNS(dsigNS, "Reference");
                    Assert.assertEquals("Signature references found", 3, signatureReferenceNodeList.getLength()); // 3, body, timestamp and protected token
                    NodeList issuerSerialNodeList = document.getElementsByTagNameNS(dsigNS, "X509IssuerSerial");
                    Assert.assertEquals("Issuer/Serial STR found", 1, issuerSerialNodeList.getLength());
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });
    }

    /**
     *
     */
    private Result processMessage(String uri, String message, int expectedStatus) throws IOException {
        return processMessage(uri, message, "10.0.0.1", expectedStatus, null, null, null);
    }

    /**
     *
     */
    private Result processMessage( String uri, String message, String requestIp, int expectedStatus, PasswordAuthentication contextAuth, String authHeader ) throws IOException {
        return processMessage(uri, message, requestIp, expectedStatus, contextAuth, authHeader, null );
    }

    /**
     *
     */
    private Result processMessage( final String uri,
                                   final String message,
                                   final String requestIp,
                                   final int expectedStatus,
                                   final PasswordAuthentication contextAuth,
                                   final String authHeader,
                                   final Functions.UnaryVoid<PolicyEnforcementContext> validationCallback ) throws IOException {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        hrequest.setMethod("POST");
        if ( message.indexOf("Content-ID: ") < 0 ) {
            hrequest.setContentType("text/xml; charset=utf8");
            hrequest.addHeader("Content-Type", "text/xml; charset=utf8");
        } else {
            String boundary = message.substring(2, message.indexOf('\n'));
            String contentType = "multipart/related; type=\"text/xml\"; boundary=\"" + boundary + "\"";
            System.out.println("Set content type to: "+ contentType);
            hrequest.setContentType(contentType);
            hrequest.addHeader("Content-Type", contentType);
        }
        hrequest.setRemoteAddr(requestIp);
        hrequest.setRequestURI(uri);
        hrequest.setContent(message.getBytes());
        ConnectionId.setConnectionId(new ConnectionId(0,0));
        hrequest.setAttribute("com.l7tech.server.connectionIdentifierObject", ConnectionId.getConnectionId());

        //set authorization header if available
        if (authHeader != null) {
            hrequest.addHeader("Authorization", authHeader);
        }

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
            context.setClusterPropertyCache(clusterPropertyCache);

            //TODO cleanup cookie init?
            context.addCookie(new HttpCookie("cookie", "invalue", 0, null, null));

            // Process message
            request.initialize(stashManager, ctype, hrequest.getInputStream());

            // Add fake auth if requested
            if (contextAuth != null) {
                UserBean user = new UserBean();
                user.setLogin(contextAuth.getUserName());
                user.setCleartextPassword(new String(contextAuth.getPassword()));
                context.getDefaultAuthenticationContext().addAuthenticationResult(
                        new AuthenticationResult(user, new UsernamePasswordSecurityToken(SecurityTokenType.UNKNOWN, contextAuth)));
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
                try {
                    if (validationCallback!=null) validationCallback.call( context );
                } finally {
                    context.close();
                }
            }

            for ( PolicyEnforcementContext.AssertionResult result : context.getAssertionResults( context.getAuditContext() ) ) {
                logger.info( "Assertion '" + result.getAssertion() + "', result: " + result.getStatus() );
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
        request.initialize(TestStashManagerFactory.getInstance().createStashManager(), ctype, new ByteArrayInputStream(message.getBytes()) );
        request.attachJmsKnob(new JmsKnob() {
            @Override
            public boolean isBytesMessage() {
                return true;
            }
            @Override
            public Map<String, Object> getJmsMsgPropMap() {
                //noinspection unchecked
                return Collections.EMPTY_MAP;
            }
            @Override
            public String getSoapAction() {
                return null;
            }
        });

        final PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setReplyExpected(true); // HTTP always expects to receive a reply

        AssertionStatus status = AssertionStatus.UNDEFINED;
        try {
            context.setAuditContext(auditContext);
            context.setSoapFaultManager(soapFaultManager);
            context.setClusterPropertyCache(clusterPropertyCache);

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
