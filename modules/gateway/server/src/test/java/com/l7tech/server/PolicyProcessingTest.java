package com.l7tech.server;

import com.l7tech.common.http.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.UserBean;
import com.l7tech.message.*;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.SignatureConfirmation;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.TestIdentityProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.assertion.credential.DigestSessions;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.secureconversation.InboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SessionCreationException;
import com.l7tech.server.service.ServiceCacheStub;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.tomcat.ResponseKillerValve;
import com.l7tech.server.transport.http.ConnectionId;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.tarari.GlobalTarariContext;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPConstants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.PasswordAuthentication;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.common.TestDocuments.getWssInteropAliceCert;
import static com.l7tech.util.Functions.map;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;

/**
 * Functional tests for message processing.
 */
public class PolicyProcessingTest {
    private static final Logger logger = Logger.getLogger(TokenServiceTest.class.getName());
    private static final String POLICY_RES_PATH = "policy/resources/";

    private static PolicyCache policyCache = null;
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
     * - Lax Resolution (true/false optional)
     * - SOAP Service (true/false)
     *
     * NOTE: Currently there's just one service with a WSDL which allows the
     * JMS service resolution to function correctly.
     */
    private static final String[][] TEST_SERVICES = new String[][]{
        {"/sqlattack", "POLICY_sqlattack.xml", null, null, "false"},
        {"/requestsizelimit", "POLICY_requestsizelimit.xml"},
        {"/documentstructure", "POLICY_documentstructure.xml", "WSDL_noops.wsdl", "true"},
        {"/stealthfault", "POLICY_stealthfault.xml"},
        {"/faultlevel", "POLICY_faultlevel.xml"},
        {"/ipaddressrange", "POLICY_iprange.xml"},
        {"/xpathcreds", "POLICY_xpathcreds.xml"},
        {"/usernametoken", "POLICY_usernametoken.xml"},
        {"/encusernametoken", "POLICY_wss_encryptedusernametoken.xml"},
        {"/encusernametokenX509", "POLICY_wss_encryptedusernametoken_x509.xml"},
        {"/encusernametokentags", "POLICY_wss_encryptedusernametokenidtag.xml"},
        {"/httpbasic", "POLICY_httpbasic.xml"},
        {"/httproutecookie", "POLICY_httproutecookie.xml"},
        {"/httproutenocookie", "POLICY_httproutenocookie.xml"},
        {"/httproutetaicredchain", "POLICY_httproutetaicredchain.xml"},
        {"/httproutepassthru", "POLICY_httproutepassthru.xml"},
        {"/httproutepassheaders", "POLICY_httproutepassheaders.xml"},
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
        {"/multiplesignaturestags2", "POLICY_multiplesignatures_idtags2.xml"},
        {"/multiplesignaturesx509SAML", "POLICY_wss_x509_and_SAML.xml"},
        {"/multiplesignaturesvars", "POLICY_wss_multiplesigners_variables.xml"},
        {"/wssDecoration1", "POLICY_requestdecoration1.xml"},
        {"/wssDecoration2", "POLICY_requestdecoration2.xml"},
        {"/wssDecoration3", "POLICY_sign_then_encrypt.xml"},
        {"/threatprotections", "POLICY_threatprotections.xml"},
        {"/removeelement", "POLICY_removeelement.xml"},
        {"/addusernametoken", "POLICY_responsesecuritytoken.xml"},
        {"/addtimestamp", "POLICY_responsetimestamp.xml"},
        {"/addsignature", "POLICY_responsesignature.xml"},
        {"/addsignaturevar", "POLICY_responsesignature_certificate_variable.xml"},
        {"/removeheaders", "POLICY_removeheaders.xml"},
        {"/signatureconfirmation1", "POLICY_signatureconfirmation1.xml"},
        {"/signatureconfirmation2", "POLICY_signatureconfirmation2.xml"},
        {"/signatureconfirmation3", "POLICY_signatureconfirmation3.xml"},
        {"/addusernametoken2", "POLICY_response_wss_usernametoken_digest.xml"},
        {"/addusernametoken3", "POLICY_response_encrypted_usernametoken.xml"},
        {"/secureconversation", "POLICY_secure_conversation.xml"},
        {"/timestampresolution", "POLICY_timestampresolution.xml"},
        {"/wssEncryptResponseIssuerSerial", "POLICY_encrypted_response_issuerserial.xml"},
        {"/hardcoded", "POLICY_hardcodedresponse.xml"},
        {"/signaturenotoken", "POLICY_wss_signaturenotoken.xml"},
    };

    @Before
    public void setUpTest() throws Exception {
        SyspropUtil.setProperty( "com.l7tech.server.serviceResolution.strictSoap", "false" );
    }

    /**
     *
     */
    @BeforeClass
    public static void setUpSuite() throws Exception {
        // Software-only TransformerFactory to ignore the alluring Tarari impl, even if tarari_raxj.jar is sitting right there
        SyspropUtil.setProperty( "javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl" );

        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
        policyCache = applicationContext.getBean("policyCache", PolicyCache.class);
        messageProcessor = applicationContext.getBean("messageProcessor", MessageProcessor.class);
        soapFaultManager = applicationContext.getBean("soapFaultManager", SoapFaultManager.class);
        testingHttpClientFactory = applicationContext.getBean("httpRoutingHttpClientFactory2", TestingHttpClientFactory.class);
        inboundSecureConversationContextManager = applicationContext.getBean("inboundSecureConversationContextManager", InboundSecureConversationContextManager.class);

        ServiceCacheStub cache = applicationContext.getBean("serviceCache", ServiceCacheStub.class);
        cache.initializeServiceCache();

        buildServices( applicationContext.getBean("serviceManager", ServiceManager.class) );
        buildUsers();

        createSecureConversationSession(); // session used in testing
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.security.prov.rsa.libpath.nonfips",
            "com.l7tech.server.serviceResolution.strictSoap",
            "javax.xml.transform.TransformerFactory"
        );
    }

    private long getServiceOid( String resolutionUri ) {
        long oid = 0;

        for ( int i=0; i<TEST_SERVICES.length; i++ ) {
            if ( TEST_SERVICES[i][0].equals( resolutionUri ) ) {
                oid = i+1;
                break;
            }
        }

        Assert.assertTrue( "Service not found", oid > 0 );

        return oid;
    }

    /**
     * Populate the service cache with the test services.
     */
    private static void buildServices( final ServiceManager serviceManager ) throws Exception {
        long oid = 1;

        for (String[] serviceInfo : TEST_SERVICES) {
            PublishedService ps = new PublishedService();
            ps.setOid(oid++);
            ps.setName(serviceInfo[0].substring(1));
            ps.setRoutingUri(serviceInfo[0]);
            ps.getPolicy().setXml(new String(loadResource(serviceInfo[1])));
            ps.getPolicy().setOid(ps.getOid());

            if (serviceInfo.length > 4 && serviceInfo[4] != null) {
                ps.setSoap(Boolean.parseBoolean( serviceInfo[4] ));
            } else {
                ps.setSoap(true);
            }

            if (serviceInfo.length > 2 && serviceInfo[2] != null) {
                ps.setWsdlXml(new String(loadResource(serviceInfo[2])));
                if (serviceInfo.length > 3 && serviceInfo[3] != null) {
                    ps.setLaxResolution(Boolean.parseBoolean( serviceInfo[3] ));
                } else {
                    ps.setLaxResolution(false);
                }
            } else {
                ps.setLaxResolution(true);
            }

            serviceManager.update( ps );
        }
    }

    private static void buildUsers() {
        UserBean ub1 = new UserBean(9898, "Alice");
        ub1.setUniqueIdentifier( "4718592" );
        TestIdentityProvider.addUser(ub1, "Alice", "password".toCharArray(), "CN=Alice, OU=OASIS Interop Test Cert, O=OASIS");

        UserBean ub2 = new UserBean(9898, "Bob");
        ub2.setUniqueIdentifier( "4718593" );
        TestIdentityProvider.addUser(ub2, "Bob", "password".toCharArray(), "CN=Bob, OU=OASIS Interop Test Cert, O=OASIS");

        GroupBean gb1 = new GroupBean(9898, "BobGroup");
        gb1.setUniqueIdentifier( "4718594" );
        TestIdentityProvider.addGroup(gb1, ub2);
    }

    private static void createSecureConversationSession() throws SessionCreationException {
        // Create a well known test session for secure conversation testing
        UserBean ub1 = new UserBean(9898, "Alice");
        ub1.setUniqueIdentifier( "4718592" );
        if (inboundSecureConversationContextManager.getSession("http://www.layer7tech.com/uuid/00000001") == null) {
            inboundSecureConversationContextManager.createContextForUser(
                    "http://www.layer7tech.com/uuid/00000001",
                    "http://www.layer7tech.com/uuid/00000001",
                    System.currentTimeMillis() + TimeUnit.DAYS.getMultiplier(),
                    ub1,
                    new byte[16]);
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
    @Test
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
    @Test
	public void testSQLAttack() throws Exception  {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        String requestMessage2 = new String(loadResource("REQUEST_sqlattack_fail.xml"));

        processMessage("/sqlattack", requestMessage1,  0);
        processMessage("/sqlattack", requestMessage2,  AssertionStatus.BAD_REQUEST.getNumeric());
    }

    /**
     * Test large message rejection.
     */
    @Test
	public void testRequestSizeLimit() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        String requestMessage2 = new String(loadResource("REQUEST_requestsizelimit_fail.xml"));

        processMessage("/requestsizelimit", requestMessage1,  0);
        processMessage("/requestsizelimit", requestMessage2, AssertionStatus.FALSIFIED.getNumeric());
    }

    /**
     * Test rejection of messages with dubious structure
     */
    @Test
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
    @Test
	public void testStealthFault() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));

        Result result = processMessage("/stealthfault", requestMessage1, AssertionStatus.FALSIFIED.getNumeric());
        assertEquals( "Should be stealth response mode.", true, result.context.isStealthResponseMode() );
    }

    /**
     * Test fault level
     */
    @Test
	public void testFaultLevel() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));

        Result result = processMessage("/faultlevel", requestMessage1, AssertionStatus.FALSIFIED.getNumeric());
        assertEquals("Should be stealth response mode.", true, result.context.isStealthResponseMode());
    }

    /**
     * Test rejection of requests from bad IP address
     */
    @Test
	public void testIPAddressRange() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));

        processMessage("/ipaddressrange", requestMessage1, 0);
        processMessage( "/ipaddressrange", requestMessage1, "10.0.0.2", AssertionStatus.FALSIFIED.getNumeric(), null, null );
    }

    /**
     * Test xpath credentials are found
     */
    @Test
	public void testXPathCredentials() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_xpathcreds_success.xml"));
        String requestMessage2 = new String(loadResource("REQUEST_general.xml"));

        processMessage("/xpathcreds", requestMessage1, 0);
        processMessage("/xpathcreds", requestMessage2, AssertionStatus.AUTH_REQUIRED.getNumeric());
    }

    /**
     * Test username token messages (incl without password)
     */
    @Test
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
    @Test
	public void testEncryptedUsernameToken() throws Exception {
        String requestMessage = new String(loadResource("REQUEST_encryptedusernametoken.xml"));
        processMessage("/encusernametoken", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call( final PolicyEnforcementContext policyEnforcementContext ) {
                try {
                    // Check the response contains a DerivedKeyToken (so is signed by the encrypted key)
                    Assert.assertEquals("DerivedKeyToken elements", 1,
                            policyEnforcementContext.getResponse().getXmlKnob().getDocumentReadOnly().getElementsByTagNameNS(
                                    "http://schemas.xmlsoap.org/ws/2004/04/sc", "DerivedKeyToken" ).getLength() );
                } catch ( Exception e ) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });
    }

    /**
     * Test encrypted username token with response signed by X.509 token
     */
    @BugNumber(7382)
    @Test
	public void testEncryptedUsernameTokenX509SignedResonse() throws Exception {
        String requestMessage = new String(loadResource("REQUEST_encryptedusernametoken.xml"));
        processMessage("/encusernametokenX509", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call( final PolicyEnforcementContext policyEnforcementContext ) {
                try {
                    // Check the response contains an X509Data element (so is not signed by the encrypted key)
                    Assert.assertEquals("X509Data elements", 1,
                            policyEnforcementContext.getResponse().getXmlKnob().getDocumentReadOnly().getElementsByTagNameNS(
                                    "http://www.w3.org/2000/09/xmldsig#", "X509Data" ).getLength() );
                } catch ( Exception e ) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });
    }

    /**
     * Test encrypted username token with idtags
     */
    @Test
	public void testEncryptedUsernameTokenIdentityTag() throws Exception {
        String requestMessage = new String(loadResource("REQUEST_encryptedusernametoken.xml"));
        processMessage("/encusernametokentags", requestMessage, 0);
    }

    /**
     * Test http basic auth with latin-1 charset
     */
    @Test
	public void testHttpBasic() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));

        String username = "\u00e9\u00e2\u00e4\u00e5";
        PasswordAuthentication pa = new PasswordAuthentication(username, "password".toCharArray());
        String authHeader = "Basic " + HexUtils.encodeBase64( (pa.getUserName() + ":" + new String(pa.getPassword())).getBytes("ISO-8859-1") );
        Result result = processMessage("/httpbasic", requestMessage1, "10.0.0.1", 0, null, authHeader);
        assertTrue("Credential present", !result.context.getDefaultAuthenticationContext().getCredentials().isEmpty());
        assertEquals( "Username correct", username, result.context.getDefaultAuthenticationContext().getCredentials().iterator().next().getLogin() );
    }

    /**
     * Test cookie pass-thru (or not)
     */
    @Test
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

        assertFalse( "Outbound request cookie present", headerExists( mockClient2.getParams().getExtraHeaders(), "Cookie", "cookie=invalue" ) );
        assertFalse( "Outbound response cookie present", newCookieExists( result2.context.getCookies(), "cookie", "outvalue" ) );
    }

    /**
     * Test outbound headers
     */
    @Test
	public void testHttpRoutingHeaders() throws Exception {
        final String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        final byte[] responseMessage1 = loadResource("RESPONSE_general.xml");

        final Pair<String,String> extraHeader = new Pair<String,String>( "X-TEST_HEADER", "value" );
        MockGenericHttpClient mockClient = buildCallbackMockHttpClient( null, new Functions.Binary<byte[], byte[], GenericHttpRequestParams>() {
            @Override
            public byte[] call( final byte[] bytes, final GenericHttpRequestParams genericHttpRequestParams ) {
                boolean foundHeader = false;
                for ( final HttpHeader header : genericHttpRequestParams.getExtraHeaders() ) {
                    if ( extraHeader.left.equalsIgnoreCase( header.getName() ) ) {
                        foundHeader = true;
                    }
                }
                assertTrue( "httpHeader routed", foundHeader );
                return responseMessage1;
            }
        } );
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage( "/httproutecookie", requestMessage1, "10.0.0.1", 0, null, null, extraHeader, null, new Functions.UnaryVoid<PolicyEnforcementContext>() {
            @Override
            public void call( final PolicyEnforcementContext policyEnforcementContext ) {
                Audit logOnlyAuditor = new LoggingAudit( logger );
                String variable = ExpandVariables.process( "${request.http.header.x-test_header}", policyEnforcementContext.getVariableMap( new String[]{ "request.http.header.x-test_header" }, logOnlyAuditor ), logOnlyAuditor );
                assertEquals( "Http header variable value", "value", variable );
            }
        } );
    }

    /**
     * Test outbound headers
     */
    @Test
	public void testHttpRoutingBlockHeaders() throws Exception {
        final String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        final byte[] responseMessage1 = loadResource("RESPONSE_general.xml");

        final Pair<String,String> extraHeader = new Pair<String,String>( "X-TEST_HEADER_BAD", "value" );
        MockGenericHttpClient mockClient = buildCallbackMockHttpClient(null, new Functions.Binary<byte[],byte[],GenericHttpRequestParams>(){
            @Override
            public byte[] call( final byte[] bytes, final GenericHttpRequestParams genericHttpRequestParams ) {
                boolean foundHeader = false;
                for ( final HttpHeader header : genericHttpRequestParams.getExtraHeaders() ) {
                    if ( extraHeader.left.equalsIgnoreCase( header.getName() ) ) {
                        foundHeader = true;
                    }
                }
                assertFalse( "httpHeader routed", foundHeader );
                return responseMessage1;
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/httproutecookie", requestMessage1, "10.0.0.1", 0, null, null, extraHeader, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call( final PolicyEnforcementContext policyEnforcementContext ) {
                Audit logOnlyAuditor = new LoggingAudit(logger);
                String variable =  ExpandVariables.process( "${request.http.header.x-test_header_bad}", policyEnforcementContext.getVariableMap( new String[]{"request.http.header.x-test_header_bad"}, logOnlyAuditor) ,logOnlyAuditor);
                assertEquals( "Http header variable value", "value", variable );
            }
        });
    }

    /**
     * Test outbound headers
     */
    @Test
	public void testHttpRoutingAllHeaders() throws Exception {
        final String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        final byte[] responseMessage1 = loadResource("RESPONSE_general.xml");

        final Pair<String,String> extraHeader = new Pair<String,String>( "X-TEST_HEADER", "value" );
        MockGenericHttpClient mockClient = buildCallbackMockHttpClient(null, new Functions.Binary<byte[],byte[],GenericHttpRequestParams>(){
            @Override
            public byte[] call( final byte[] bytes, final GenericHttpRequestParams genericHttpRequestParams ) {
                boolean foundHeader = false;
                for ( final HttpHeader header : genericHttpRequestParams.getExtraHeaders() ) {
                    if ( extraHeader.left.equalsIgnoreCase( header.getName() ) ) {
                        foundHeader = true;
                    }
                }
                assertTrue( "httpHeader routed", foundHeader );
                return responseMessage1;
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/httproutepassheaders", requestMessage1, "10.0.0.1", 0, null, null, extraHeader, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call( final PolicyEnforcementContext policyEnforcementContext ) {
                Audit logOnlyAuditor = new LoggingAudit(logger);
                String variable =  ExpandVariables.process( "${request.http.header.x-test_header}", policyEnforcementContext.getVariableMap( new String[]{"request.http.header.x-test_header"}, logOnlyAuditor) ,logOnlyAuditor);
                assertEquals( "Http header variable value", "value", variable );
            }
        });
    }

    /**
     * Test that TAI info is routed
     */
    @Test
	public void testHttpRoutingTAI() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage( "/httproutetaicredchain", requestMessage1, "10.0.0.1", 0, new PasswordAuthentication( "test", "password".toCharArray() ), null );

        assertTrue("Outbound request TAI header missing", headerExists(mockClient.getParams().getExtraHeaders(), "IV_USER", "test"));
        assertTrue( "Outbound request TAI cookie missing", headerExists( mockClient.getParams().getExtraHeaders(), "Cookie", "IV_USER=test" ) );
    }

    /**
     * Test connection id propagation
     */
    @Test
	public void testHttpRoutingSticky() throws Exception {
        //This just tests that the context info gets to the CommonsHttpClient
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        byte[] responseMessage1 = loadResource( "RESPONSE_general.xml" );

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient( mockClient );

        processMessage( "/httproutepassthru", requestMessage1, 0 );

        assertNotNull( "Missing connection id", mockClient.getIdentity() );
    }

    /**
     * Test HTTP routing for JMS message
     */
    @Test
	public void testHttpRoutingJmsIn() throws Exception {
        //This just tests that the context info gets to the CommonsHttpClient
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient( mockClient );

        processJmsMessage( requestMessage1, 0, 0 );
    }

    /**
     * Test WSS header handling
     */
    @Test
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
        assertTrue( "Promoted security header missing", request3.indexOf( "<wsse:Username>user</wsse:Username>" ) > 0 && request3.indexOf( "asdf" ) < 0 );
    }

    /**
     * Test WSS Signed Attachment processing
     */
    @Test
	public void testWssSignedAttachment() throws Exception {
        String requestMessage1 = new String(loadResource("REQUEST_signed_attachment.txt"));

        processMessage("/attachment", requestMessage1, 0);
    }

    /**
     * Test WSS Signed Attachment processing failure
     */
    @Test
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
    @Test
	public void testRequestNonXmlOk() throws Exception
    {
        processMessage("/requestnonxmlok", new String(loadResource("REQUEST_general.xml")), 0);
    }

    /**
     * Test multiple request/response signatures
     */
    @Test
	public void testMultipleSignatures() throws Exception {
        byte[] responseMessage1 = loadResource("REQUEST_multiplesignatures.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/multiplesignatures", new String(loadResource("REQUEST_multiplesignatures.xml")), 0);
    }

    @Test
	public void testWssMessageAttributes() throws Exception {
        // build test request and run WSS processor on it
        String message = new String(loadResource("REQUEST_multiplesignatures.xml"));
        final Message request = new Message();
        request.initialize(ContentTypeHeader.XML_DEFAULT, message.getBytes());
        request.getSecurityKnob().setProcessorResult(
            WSSecurityProcessorUtils.getWssResults(request, "multiple signatures test request", new SimpleSecurityTokenResolver(), new LoggingAudit(logger))
        );

        assertEquals("2", getRequestAttribute(request, "request.wss.certificates.count", null));
        assertEquals("CN=OASIS Interop Test CA, O=OASIS", getRequestAttribute(request, "request.wss.certificates.value.1.issuer", null));
        assertEquals("cn=oasis interop test ca,o=oasis", getRequestAttribute(request, "request.wss.certificates.value.1.issuer.canonical", null));
        assertEquals("CN=OASIS Interop Test CA", getRequestAttribute(request, "request.wss.certificates.value.1.issuer.dn.2", null));
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
        Object result = ExpandVariables.processSingleVariableAsDisplayableObject("${" + attributeName + "}", vars, new LoggingAudit(logger));
        System.out.println("\n-----------------------------------\n" + attributeName + ": " + result);
        return result;
    }

    /**
     * Test failure with wrong signing identities in request message
     */
    @Test
	public void testMultipleSignaturesWrongIdentitiesRequest() throws Exception {
        byte[] responseMessage1 = loadResource("REQUEST_multiplesignatures.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/multiplesignatures", new String(loadResource("REQUEST_multiplesignatures2.xml")), 600);
    }

    /**
     * Test multiple request/response signatures with identity tags (WssSignature selection by signature)
     */
    @Test
	public void testMultipleSignaturesWithIdTags() throws Exception {
        byte[] responseMessage1 = loadResource("REQUEST_multiplesignatures.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/multiplesignaturestags", new String(loadResource("REQUEST_multiplesignatures.xml")), 0);
    }

    /**
     * Test multiple request/response signatures with identity tags (WssSignature selection by signature reference)
     */
    @Test
	public void testMultipleSignaturesWithIdTags2() throws Exception {
        byte[] responseMessage1 = loadResource("REQUEST_multiplesignatures.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/multiplesignaturestags2", new String(loadResource("REQUEST_multiplesignatures.xml")), 0);
    }

    /**
     * Test failure with wrong signing identities in response message
     */
    @Test
	public void testMultipleSignaturesWrongIdentitiesResponse() throws Exception {
        byte[] responseMessage1 = loadResource("REQUEST_multiplesignatures2.xml");

        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        processMessage("/multiplesignatures", new String(loadResource("REQUEST_multiplesignatures.xml")), 600);
    }

    /**
     * Test policy failure on missing response signatures
     */
    @Test
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
    @Test
	public void testMultipleSignaturesNoIdentity() throws Exception {
        processMessage("/multiplesignaturesnoid", new String(loadResource("REQUEST_multiplesignatures.xml")), 600);
    }

    /**
     * Test multiple request signatures are rejected by WSS X.509 assertion when not enabled.
     */
    @Test
	public void testMultipleSignaturesRejected() throws Exception {
        processMessage("/x509token", new String(loadResource("REQUEST_multiplesignatures.xml")), 400);
    }

    /**
     * Test multiple request signatures with X.509 and SAML credential with both tokens signing the same elements.
     * This test also has a group identity target.
     */
    @Test
	public void testMultipleSignaturesX509AndSAML() throws Exception {
        processMessage("/multiplesignaturesx509SAML", new String(loadResource("REQUEST_wss_x509_and_SAML.xml")), 0);
    }

    /**
     * Test multiple request signatures with message variables and new context variables in template response.
     */
    @Test
	public void testMultipleSignaturesVariables() throws Exception {
        processMessage("/multiplesignaturesvars", new String(loadResource("REQUEST_multiplesignatures3.xml")), "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                try {
                    final Document document = context.getResponse().getXmlKnob().getDocumentReadOnly();
                    final String tns = "http://warehouse.acme.com/ws";

                    NodeList certcountNodeList = document.getElementsByTagNameNS(tns, "certcount");
                    Assert.assertEquals("certcount found", 1, certcountNodeList.getLength());
                    Assert.assertEquals("certcount", "2", XmlUtil.getTextValue((Element)certcountNodeList.item(0)));

                    NodeList signingcertcountNodeList = document.getElementsByTagNameNS(tns, "signingcertcount");
                    Assert.assertEquals("signingcertcount found", 1, signingcertcountNodeList.getLength());
                    Assert.assertEquals("signingcertcount", "2", XmlUtil.getTextValue((Element)signingcertcountNodeList.item(0)));

                    NodeList signingcert1NodeList = document.getElementsByTagNameNS(tns, "signingcert1");
                    Assert.assertEquals("signingcert1 found", 1, signingcert1NodeList.getLength());
                    Assert.assertEquals("signingcert1", "CN=Alice, OU=OASIS Interop Test Cert, O=OASIS", XmlUtil.getTextValue((Element)signingcert1NodeList.item(0)));

                    NodeList signingcert2NodeList = document.getElementsByTagNameNS(tns, "signingcert2");
                    Assert.assertEquals("signingcert2 found", 1, signingcert2NodeList.getLength());
                    Assert.assertEquals("signingcert2", "CN=Bob, OU=OASIS Interop Test Cert, O=OASIS", XmlUtil.getTextValue((Element)signingcert2NodeList.item(0)));

                    NodeList signingcert1cnNodeList = document.getElementsByTagNameNS(tns, "signingcert1cn");
                    Assert.assertEquals("signingcert1cn found", 2, signingcert1cnNodeList.getLength());
                    Assert.assertEquals("signingcert1cn", "Alice", XmlUtil.getTextValue((Element)signingcert1cnNodeList.item(0)));
                    Assert.assertEquals("signingcert1cn", "Alice", XmlUtil.getTextValue((Element)signingcert1cnNodeList.item(1)));

                    NodeList signingcert1rdn1NodeList = document.getElementsByTagNameNS(tns, "signingcert1rdn1");
                    Assert.assertEquals("signingcert1rdn1 found", 1, signingcert1rdn1NodeList.getLength());
                    Assert.assertEquals("signingcert1rdn1", "O=OASIS", XmlUtil.getTextValue((Element)signingcert1rdn1NodeList.item(0)));

                    NodeList signingcert1b64NodeList = document.getElementsByTagNameNS(tns, "signingcert1b64");
                    Assert.assertEquals("signingcert1b64 found", 1, signingcert1b64NodeList.getLength());
                    Assert.assertTrue("signingcert1b64", XmlUtil.getTextValue((Element)signingcert1b64NodeList.item(0)).startsWith( "MII" ));
                    Assert.assertFalse("signingcert1b64", XmlUtil.getTextValue((Element)signingcert1b64NodeList.item(0)).contains( " " ));
                    Assert.assertFalse("signingcert1b64", XmlUtil.getTextValue((Element)signingcert1b64NodeList.item(0)).contains( "\n" ));

                    NodeList creduserNodeList = document.getElementsByTagNameNS(tns, "creduser");
                    Assert.assertEquals("creduser found", 1, creduserNodeList.getLength());
                    Assert.assertEquals("creduser", "CN=Bob, OU=OASIS Interop Test Cert, O=OASIS", XmlUtil.getTextValue((Element)creduserNodeList.item(0)));

                    NodeList authusersNodeList = document.getElementsByTagNameNS(tns, "authusers");
                    Assert.assertEquals("authusers found", 1, authusersNodeList.getLength());
                    Assert.assertEquals("authusers", "Alice, Bob", XmlUtil.getTextValue((Element)authusersNodeList.item(0)));

                    NodeList authdnsNodeList = document.getElementsByTagNameNS(tns, "authdns");
                    Assert.assertEquals("authdns found", 1, authdnsNodeList.getLength());
                    Assert.assertEquals("authdns", "CN=Alice, OU=OASIS Interop Test Cert, O=OASIS, CN=Bob, OU=OASIS Interop Test Cert, O=OASIS", XmlUtil.getTextValue((Element)authdnsNodeList.item(0)));
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });
    }

    /**
     * Test multiple request signatures are rejected by WSS X.509 assertion when not enabled.
     */
    @BugNumber(7285)
    @Test
	public void testEncryptedKeyWithX509TokenRejected() throws Exception {
        // Test with com.l7tech.server.policy.requireSigningTokenCredential=false for old behaviour
        processMessage("/x509token", new String(loadResource("REQUEST_signed_x509_and_encryptedkey.xml")), 600);
    }

    /**
     * Test X.509 signature with invalid signature algorithm is rejected
     */
    @BugNumber(7528)
    @Test
	public void testHmacSha1X509TokenRejected() throws Exception {
        processMessage("/x509token", new String(loadResource("REQUEST_signed_hmac_sha1_certificate.xml")), 500);
    }

    /**
     * Test success on applying a signature to the request message using the WssDecoration assertion.
     */
    @Test
	public void testDecorationCommitOnRequest() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/wssDecoration1", new String(loadResource("REQUEST_general.xml")), 0);
    }

    /**
     * Test success on adding a signature that endorses an existing signature from the request
     */
    @Test
	public void testEndorsingRequest() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/wssDecoration2", new String(loadResource("REQUEST_decoration2.xml")), 0);
    }

    /**
     * Test that using signing, apply, encrypt, apply creates a (BSP) valid security header.
     */
    @Test
	public void testDecorationSignThenEncrypt() throws Exception {
        processMessage("/wssDecoration3", new String(loadResource("REQUEST_general.xml")), 0);
    }

    /**
     * Test running threat protections for request, variable and response messages
     */
    @Test
	public void testThreatProtectionsRequestResponseAndMessageTarget() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        processMessage("/threatprotections", new String(loadResource("REQUEST_general.xml")), 0);
    }

    /**
     * Test removal of an XML element with XPath selection 
     */
    @Test
	public void testRemoveElement() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        Assert.assertTrue( "Request message contains element to remove", XmlUtil.parse( requestMessage ).getElementsByTagNameNS( "http://warehouse.acme.com/ws", "delay" ).getLength() > 0 );
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
    @Test
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
                    String wsseNS = SoapUtil.SECURITY_NAMESPACE;
                    String dsigNS = SoapUtil.DIGSIG_URI;
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
    @Test
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
                    String wsuNS = SoapUtil.WSU_NAMESPACE;
                    String dsigNS = SoapUtil.DIGSIG_URI;
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
    @Test
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
                    String dsigNS = SoapUtil.DIGSIG_URI;
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
     * Test addition of a signature to the response using a certificate from a variable
     */
    @BugNumber(11671)
    @Test
    public void testAddSignatureCertificateVariable() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        processMessage("/addsignaturevar", requestMessage, "10.0.0.1", 0, null, null, null, singletonMap("certificate", getWssInteropAliceCert()), new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                try {
                    final Document document = context.getResponse().getXmlKnob().getDocumentReadOnly();
                    final String dsigNS = SoapUtil.DIGSIG_URI;
                    final NodeList signatureNodeList = document.getElementsByTagNameNS(dsigNS, "Signature");
                    Assert.assertEquals("Signature found", 1, signatureNodeList.getLength());
                    final NodeList x509SerialNumberNodeList = document.getElementsByTagNameNS(dsigNS, "X509SerialNumber");
                    Assert.assertEquals("X509SerialNumber found", 1, x509SerialNumberNodeList.getLength());
                    Assert.assertEquals( "Serial number value", "127901500862700997089151460209364726264", XmlUtil.getTextValue( (Element)x509SerialNumberNodeList.item( 0 ) ) );
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });
    }

    /**
     * Test removal of the security header from request and response messages.
     */
    @Test
	public void testRemoveSecurityHeaders() throws Exception {
        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        final String requestMessage = new String(loadResource("REQUEST_signed.xml"));
        Assert.assertTrue("Request message contains security header to remove", XmlUtil.parse(requestMessage).getElementsByTagNameNS(SoapUtil.SECURITY_NAMESPACE, "Security").getLength() > 0);
        processMessage("/removeheaders", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                try {
                    final Document requestDocument = context.getRequest().getXmlKnob().getDocumentReadOnly();
                    NodeList securityHeaderNodeList1 = requestDocument.getElementsByTagNameNS(SoapUtil.SECURITY_NAMESPACE, "Security");
                    Assert.assertEquals("Request security headers found", 0, securityHeaderNodeList1.getLength());

                    // Ensure empty header is removed
                    NodeList soapHeaderNodeList1 = requestDocument.getElementsByTagNameNS(SOAPConstants.URI_NS_SOAP_ENVELOPE, "Header");
                    Assert.assertEquals("Request soap headers found", 0, soapHeaderNodeList1.getLength());

                    final Document responseDocument = context.getResponse().getXmlKnob().getDocumentReadOnly();
                    NodeList securityHeaderNodeList2 = responseDocument.getElementsByTagNameNS(SoapUtil.SECURITY_NAMESPACE, "Security");
                    Assert.assertEquals("Response security headers found", 0, securityHeaderNodeList2.getLength());

                    NodeList soapHeaderNodeList2 = responseDocument.getElementsByTagNameNS(SOAPConstants.URI_NS_SOAP_ENVELOPE, "Header");
                    Assert.assertEquals("Response soap headers found", 0, soapHeaderNodeList2.getLength());
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });
    }

    /**
     * This simulates one SSG routing to another SSG.
     *
     * The first policy signs the message and routes then validates the
     * signature confirmation in the response message.
     *
     * The second policy validates the signature and sends a signed
     * response with signature confirmation.
     */
    @Test
	public void testSignatureConfirmation() throws Exception {
        MockGenericHttpClient mockClient = buildCallbackMockHttpClient(null, new Functions.Binary<byte[], byte[],GenericHttpRequestParams>(){
            @Override
            public byte[] call( final byte[] requestBytes, final GenericHttpRequestParams parameters ) {
                final byte[][] responseHolder = new byte[1][];
                try {
                    final Document outboundRequest = XmlUtil.parse( new String(requestBytes) );
                    // Ensure there's a signature in there
                    NodeList signatureValueNodeList = outboundRequest.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "SignatureValue");
                    Assert.assertEquals("Request signature value found", 1, signatureValueNodeList.getLength());
                    final String signatureValue = XmlUtil.getTextValue( (Element)signatureValueNodeList.item(0) );

                    //System.out.println( XmlUtil.nodeToFormattedString( outboundRequest ) );
                    processMessage("/signatureconfirmation2", new String(requestBytes), "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
                        @Override
                        public void call(final PolicyEnforcementContext context) {
                            try {
                                responseHolder[0] = IOUtils.slurpStream( context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream() );
                            } catch (Exception e) {
                                throw ExceptionUtils.wrap(e);
                            }
                        }
                    });
                    final Document inboundResponse = XmlUtil.parse( new String(responseHolder[0]) );
                    //System.out.println( XmlUtil.nodeToFormattedString( inboundResponse ) );

                    // Ensure there's a signature confirmation in there
                    NodeList signatureConfirmationNodeList = inboundResponse.getElementsByTagNameNS(SoapConstants.SECURITY11_NAMESPACE, "SignatureConfirmation");
                    Assert.assertEquals("Response signature confirmation found", 1, signatureConfirmationNodeList.getLength());
                    Assert.assertEquals("Signature confirmation matches signature value", signatureValue, ((Element)signatureConfirmationNodeList.item(0)).getAttribute("Value"));
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
                return responseHolder[0];
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        processMessage("/signatureconfirmation1", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call( final PolicyEnforcementContext context ) {
                final ProcessorResult result = context.getResponse().getSecurityKnob().getProcessorResult();
                Assert.assertNotNull( "Response message should have WSS processing results", result );
                Assert.assertNotNull( "Response message should have signature confirmation", result.getSignatureConfirmation() );
                System.out.println( result.getSignatureConfirmation().getErrors() );
                Assert.assertTrue( "Signature confirmation should not have errors", result.getSignatureConfirmation().getStatus() != SignatureConfirmation.Status.INVALID );
            }
        });

        // reset the mock client, as some unit tests don't set this before running 
        testingHttpClientFactory.setMockHttpClient(buildMockHttpClient(null, loadResource("RESPONSE_general.xml")));
    }

    /**
     * This simulates one SSG routing to another SSG.
     *
     * This is similar to the basic signature confirmation test but has
     * signature confirmation for the inbound and outbound messages.
     */
    @Test
	public void testSignatureConfirmationInOut() throws Exception {
        MockGenericHttpClient mockClient = buildCallbackMockHttpClient(null, new Functions.Binary<byte[], byte[], GenericHttpRequestParams>(){
            @Override
            public byte[] call( final byte[] requestBytes, final GenericHttpRequestParams parameters ) {
                final byte[][] responseHolder = new byte[1][];
                try {
                    final Document outboundRequest = XmlUtil.parse( new String(requestBytes) );
                    //System.out.println( XmlUtil.nodeToFormattedString( outboundRequest ) );

                    // Ensure there's a signature in there
                    NodeList signatureValueNodeList = outboundRequest.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "SignatureValue");
                    Assert.assertEquals("Request signature value found", 1, signatureValueNodeList.getLength());
                    final String signatureValue = XmlUtil.getTextValue( (Element)signatureValueNodeList.item(0) );

                    processMessage("/signatureconfirmation2", new String(requestBytes), "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
                        @Override
                        public void call(final PolicyEnforcementContext context) {
                            try {
                                responseHolder[0] = IOUtils.slurpStream( context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream() );
                            } catch (Exception e) {
                                throw ExceptionUtils.wrap(e);
                            }
                        }
                    });
                    final Document inboundResponse = XmlUtil.parse( new String(responseHolder[0]) );
                    //System.out.println( XmlUtil.nodeToFormattedString( inboundResponse ) );

                    // Ensure there's a signature confirmation in there
                    NodeList signatureConfirmationNodeList = inboundResponse.getElementsByTagNameNS(SoapConstants.SECURITY11_NAMESPACE, "SignatureConfirmation");
                    Assert.assertEquals("Response signature confirmation found", 1, signatureConfirmationNodeList.getLength());
                    Assert.assertEquals("Signature confirmation matches signature value", signatureValue, ((Element)signatureConfirmationNodeList.item(0)).getAttribute("Value"));
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
                return responseHolder[0];
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);
        final String requestMessage = new String(loadResource("REQUEST_signed.xml"));

        final Document inboundRequest = XmlUtil.parse( requestMessage );
        // Ensure there's a signature in there
        NodeList signatureValueNodeList = inboundRequest.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "SignatureValue");
        Assert.assertEquals("Inbound request signature value found", 1, signatureValueNodeList.getLength());
        final String inboundSignatureValue = XmlUtil.getTextValue( (Element)signatureValueNodeList.item(0) );

        processMessage("/signatureconfirmation3", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call( final PolicyEnforcementContext context ) {
                // Test that the inbound response was validated
                SecurityKnob responseSK = context.getResponse().getSecurityKnob();
                final ProcessorResult result = responseSK.getProcessorResult();

                Assert.assertNotNull( "Response message should have WSS processing results", result );
                Assert.assertTrue( "Validation of signature confirmations was not performed for the message.", responseSK.isSignatureConfirmationValidated() );
                System.out.println( result.getSignatureConfirmation().getErrors() );
                Assert.assertTrue( "Signature confirmation validation failed: " + result.getSignatureConfirmation().getErrors(), 
                                   result.getSignatureConfirmation().getStatus() != SignatureConfirmation.Status.INVALID );

                // Test that the outbound response is confirmed
                try {
                    Document outboundResponse = context.getResponse().getXmlKnob().getDocumentReadOnly();
                    System.out.println( XmlUtil.nodeToFormattedString( outboundResponse ) );
                    NodeList signatureConfirmationNodeList = outboundResponse.getElementsByTagNameNS(SoapConstants.SECURITY11_NAMESPACE, "SignatureConfirmation");
                    Assert.assertEquals("Response signature confirmation found", 1, signatureConfirmationNodeList.getLength());
                    Assert.assertEquals("Signature confirmation matches signature value", inboundSignatureValue, ((Element)signatureConfirmationNodeList.item(0)).getAttribute("Value"));
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });

        // reset the mock client, as some unit tests don't set this before running
        testingHttpClientFactory.setMockHttpClient(buildMockHttpClient(null, loadResource("RESPONSE_general.xml")));
    }

    @BugNumber(10970)
    @Test
    public void testSignatureNoToken() throws Exception {
        String requestMessage = new String(loadResource("REQUEST_signed.xml"));
        Result result = processMessage("/signaturenotoken", requestMessage, "10.0.0.1", 600, null, null );

        final AuditRecord ar = AuditContextStub.getGlobalLastRecord();
        assertTrue("audit details shall be present", !ar.getDetails().isEmpty());
        final List<Integer> detailIds = map( ar.getDetails(),
                Functions.<Integer, AuditDetail>propertyTransform( AuditDetail.class, "messageId" ) );
        assertTrue("Saw expected audit", detailIds.contains( MessageProcessingMessages.WSS_NO_SIGNING_TOKEN.getId() ));
    }

    @BugNumber(7253)
    @Test
	public void testHardcodedResolution() throws Exception {
        // Test a SOAP message resolves to an XML service
        String requestMessage1 = new String(loadResource("REQUEST_general.xml"));
        processJmsMessage(requestMessage1, 0, getServiceOid("/sqlattack")); // 1 is /sqlattack

        // Test a SOAP message does not resolve to a SOAP service if lax resolution is
        // disabled and the request does not match an operation
        String requestMessage2 = new String(loadResource("RESPONSE_general.xml")); // the response is not a valid request message
        processJmsMessage(requestMessage2, 404, getServiceOid("/httproutejms"));

        // Test a SOAP message resolves to a SOAP service if lax resolution is
        // enabled and the request does not match an operation
        processJmsMessage(requestMessage2, 0, getServiceOid("/documentstructure"));
    }

    @Test
	public void testAddWssUsernameToken() throws Exception {        
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        processMessage("/addusernametoken2", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                try {
                    final Document document = context.getResponse().getXmlKnob().getDocumentReadOnly();
                    String wsseNS = SoapUtil.SECURITY_NAMESPACE;
                    String wsuNS = SoapUtil.WSU_NAMESPACE;
                    NodeList userNodeList = document.getElementsByTagNameNS(wsseNS, "Username");
                    NodeList passNodeList = document.getElementsByTagNameNS(wsseNS, "Password");
                    NodeList nonceNodeList = document.getElementsByTagNameNS(wsseNS, "Nonce");
                    NodeList createdNodeList = document.getElementsByTagNameNS(wsuNS, "Created");
                    Assert.assertEquals("Username found", 1, userNodeList.getLength());
                    Assert.assertEquals("Password found", 1, passNodeList.getLength());
                    Assert.assertEquals("Nonce found", 1, nonceNodeList.getLength());
                    Assert.assertEquals("Created found", 2, createdNodeList.getLength()); // 1 in security header, 1 in token
                    Assert.assertEquals("Username", "username", XmlUtil.getTextValue((Element)userNodeList.item(0)));
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });
    }

    @Test
	public void testAddEncryptedWssUsernameToken() throws Exception {
        final String requestMessage = new String(loadResource("REQUEST_signed.xml"));
        processMessage("/addusernametoken3", requestMessage, 0);
    }

    @Test
    public void testSecureConversation() throws Exception {
        final String requestMessage = new String(loadResource("REQUEST_secure_conversation.xml"));
        processMessage("/secureconversation", requestMessage, 0);
    }

    /**
     * Test that processing of a signature with HMACOutputLength of 1 bit fails. 
     */
    @BugNumber(7526)
    @Test
    public void testSecureConversationHMACOutputLength() throws Exception {
        final String requestMessage = new String(loadResource("REQUEST_secure_conversation_hmacoutputlength.xml"));
        processMessage("/secureconversation", requestMessage, 500);
    }

    @Test
	public void testWssTimestampResolution() throws Exception {
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        processMessage("/timestampresolution", requestMessage, 0);
    }

    @BugNumber(7299)
    @Test
    public void testWssEncryptResponseIssuerSerial() throws Exception {
        final String requestMessage = new String(loadResource("REQUEST_signed.xml"));
        processMessage("/wssEncryptResponseIssuerSerial", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                try {
                    final Document document = context.getResponse().getXmlKnob().getDocumentReadOnly();
                    Assert.assertEquals( "IssuerSerial count", 1, document.getElementsByTagNameNS( "http://www.w3.org/2000/09/xmldsig#", "X509IssuerSerial").getLength());
                } catch (Exception e) {
                    throw ExceptionUtils.wrap(e);
                }
            }
        });
    }

    /**
     * Verify that global policies are run and do not affect the service policy results (context.getAssertionResults()) 
     */
    @Test
    public void testGlobalPoliciesRun() throws Exception {
        String polStart = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:AuditDetailAssertion><L7p:Detail stringValue=\"";
        String polEnd   = "\"/></L7p:AuditDetailAssertion></wsp:All></wsp:Policy>";
        Collection<String> pids = new ArrayList<String>();
        pids.add( policyCache.registerGlobalPolicy( "global-pre", PolicyType.GLOBAL_FRAGMENT, PolicyType.TAG_GLOBAL_MESSAGE_RECEIVED, polStart + "global-1" + polEnd ) );
        pids.add( policyCache.registerGlobalPolicy( "global-pre-security", PolicyType.GLOBAL_FRAGMENT, PolicyType.TAG_GLOBAL_PRE_SECURITY, polStart + "global-2" + polEnd ) );
        pids.add( policyCache.registerGlobalPolicy( "global-pre-service", PolicyType.GLOBAL_FRAGMENT, PolicyType.TAG_GLOBAL_PRE_SERVICE, polStart + "global-3" + polEnd ) );
        pids.add( policyCache.registerGlobalPolicy( "global-post-service", PolicyType.GLOBAL_FRAGMENT, PolicyType.TAG_GLOBAL_POST_SERVICE, polStart + "global-4" + polEnd ) );
        pids.add( policyCache.registerGlobalPolicy( "global-post-security", PolicyType.GLOBAL_FRAGMENT, PolicyType.TAG_GLOBAL_POST_SECURITY, polStart + "global-5" + polEnd ) );
        pids.add( policyCache.registerGlobalPolicy( "global-post", PolicyType.GLOBAL_FRAGMENT, PolicyType.TAG_GLOBAL_MESSAGE_COMPLETED, polStart + "global-6" + polEnd ) );

        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        Result result = processMessage("/addtimestamp", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                assertEquals("Number of assertion results", 3, context.getAssertionResults().size());
            }
        });

        // Check auditing from global policies
        final AuditRecord ar = AuditContextStub.getGlobalLastRecord();
        final Collection<AuditDetail> details = ar.getDetails();
        int globalIndex = 1;
        for( final AuditDetail detail : details ) {
            if ( detail.getMessageId()==-4 && ArrayUtils.contains( detail.getParams(), "global-"+globalIndex)) {
                globalIndex++;
            } else if ( detail.getMessageId()==-4 && ArrayUtils.contains( detail.getParams(), "global-1") ) {
                fail("Global policy evaluated multiple times");
            }
        }
        assertEquals("Found global policy audit details", Integer.valueOf(globalIndex), Integer.valueOf(7));

        for ( final String pid : pids ) {
            policyCache.unregisterGlobalPolicy( pid );
        }
    }

    /**
     * Verify that global policies can update audit/fault settings but not context variables.
     */
    @Test
    public void testGlobalPoliciesContextModification() throws Exception {
        final String pid = policyCache.registerGlobalPolicy( "global-pre-service", PolicyType.GLOBAL_FRAGMENT, PolicyType.TAG_GLOBAL_PRE_SERVICE, new String(loadResource("POLICY_global.xml")) );

        byte[] responseMessage1 = loadResource("RESPONSE_general.xml");
        MockGenericHttpClient mockClient = buildMockHttpClient(null, responseMessage1);
        testingHttpClientFactory.setMockHttpClient(mockClient);
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        processMessage("/addtimestamp", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                assertEquals("Number of assertion results", 3, context.getAssertionResults().size());
                assertEquals("Audit level", Level.WARNING, context.getAuditLevel());
                assertEquals("Audit request", true, context.isAuditSaveRequest() );
                assertEquals("Audit response", true, context.isAuditSaveResponse() );
                assertNotNull("Fault level null", context.getFaultlevel());
                assertEquals("Fault level value", 4, context.getFaultlevel().getLevel()); // 4 full trace
                assertEquals("Fault level sign", true, context.getFaultlevel().isSignSoapFault());
                try {
                    context.getVariable( "test" );
                    fail("Variable should not exist 'test'");
                } catch ( NoSuchVariableException e ) {
                    // OK
                }
            }
        });

        policyCache.unregisterGlobalPolicy( pid );
    }

    @Test
    public void testHardcodedResponse() throws Exception {
        final String requestMessage = new String(loadResource("REQUEST_general.xml"));
        processMessage("/hardcoded", requestMessage, "10.0.0.1", 0, null, null, new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call(final PolicyEnforcementContext context) {
                try {
                    final Document document = context.getResponse().getXmlKnob().getDocumentReadOnly();
                    Assert.assertTrue( "content-type", ContentTypeHeader.XML_DEFAULT.matches( context.getResponse().getMimeKnob().getOuterContentType() ) );
                    Assert.assertEquals( "content-type charset", "utf-8", context.getResponse().getMimeKnob().getOuterContentType().getParam("charset"));
                    Assert.assertEquals( "content", "<xml>body</xml>", XmlUtil.nodeToString(document) );
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
        return processMessage( uri, message, requestIp, expectedStatus, contextAuth, authHeader, null, null, validationCallback );
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
                                   final Pair<String,String> extraHeader,
                                   final Map<String,?> variables,
                                   final Functions.UnaryVoid<PolicyEnforcementContext> validationCallback ) throws IOException {

        MockServletContext servletContext = new MockServletContext();
        final MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        final MockHttpServletResponse hresponse = new MockHttpServletResponse();

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

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);
        if ( variables != null ) {
            for ( final Map.Entry<String,?> entry : variables.entrySet() ) {
                context.setVariable( entry.getKey(), entry.getValue() );
            }
        }

        final StashManager stashManager = TestStashManagerFactory.getInstance().createStashManager();

        AssertionStatus status = AssertionStatus.UNDEFINED;
        try {
            //TODO cleanup cookie init?
            context.addCookie(new HttpCookie("cookie", "invalue", 0, null, null));

            // Process message
            request.initialize(stashManager, ctype, hrequest.getInputStream());

            // Add fake auth if requested
            if (contextAuth != null) {
                UserBean user = new UserBean();
                user.setLogin(contextAuth.getUserName());
                context.getDefaultAuthenticationContext().addAuthenticationResult(
                        new AuthenticationResult(user, new UsernamePasswordSecurityToken(SecurityTokenType.UNKNOWN, contextAuth)));
            }

            // Add extra header if requested
            if ( extraHeader != null ) {
                HttpOutboundRequestFacet
                        .getOrCreateHttpOutboundRequestKnob( request )
                        .addHeader( extraHeader.left, extraHeader.right );
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
                if (validationCallback!=null) validationCallback.call( context );
            } finally {
                context.close();
            }

            for ( PolicyEnforcementContext.AssertionResult result : context.getAssertionResults() ) {
                logger.info( "Assertion '" + result.getAssertion() + "', result: " + result.getStatus() );
            }

            assertEquals("Policy status", expectedStatus, status.getNumeric());
        }

        return new Result(context);
    }

    /**
     *
     */
    private Result processJmsMessage( final String message,
                                      final int expectedStatus,
                                      final long serviceOid ) throws IOException {
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
                return Collections.emptyMap();
            }
            @Override
            public String getSoapAction() {
                return null;
            }
            @Override
            public long getServiceOid() {
                return serviceOid;
            }

            @Override
            public String[] getHeaderValues(String name) {
                return new String[0];
            }

            @Override
            public String[] getHeaderNames() {
                return new String[0];
            }
        });

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

        AssertionStatus status = AssertionStatus.UNDEFINED;
        try {
            status = messageProcessor.processMessage(context);

            // if the policy is not successful AND the stealth flag is on, drop connection
            if (status != AssertionStatus.NONE) {
                SoapFaultLevel faultLevelInfo = context.getFaultlevel();
                if ( faultLevelInfo==null ) faultLevelInfo = soapFaultManager.getDefaultBehaviorSettings();
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
            context.close();

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
    private static MockGenericHttpClient buildCallbackMockHttpClient( GenericHttpHeaders headers,
                                                                      final Functions.Binary<byte[],byte[],GenericHttpRequestParams> router ) {
        if (headers == null) {
            HttpHeader[] responseHeaders = new HttpHeader[]{
                    new GenericHttpHeader("Content-Type","text/xml; charset=utf8"),
            };
            headers = new GenericHttpHeaders(responseHeaders);
        }

        final MockGenericHttpClient[] clients = new MockGenericHttpClient[1];
        clients[0] = new MockGenericHttpClient(200,
                headers,
                ContentTypeHeader.XML_DEFAULT,
                null,
                null){
            @Override
            protected byte[] getResponseBody() {
                return router.call( clients[0].getRequestBody(), clients[0].getParams() );
            }
        };

        return clients[0];
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
