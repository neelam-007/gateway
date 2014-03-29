package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import static com.l7tech.policy.assertion.CodeInjectionProtectionType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test coverage of Code Injection Protection Assertion
 *
 * @author darmstrong
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class ServerCodeInjectionProtectionAssertionTest {
    // URL components
    private static final String BENIGN_URL_PATH = "/path/to/resource";
    private static final String BENIGN_URL_QUERY_STRING = "var1=val1&var2=val2";

    // message resources
    private static final String VALID_SOAP_REQUEST = "ValidListProductsRequestSOAPMessage.xml";
    private static final String VALID_SOAP_RESPONSE = "ValidEchoResponseSOAPMessage.xml";

    private StashManager stashManager;
    private TestAudit testAudit;
    private SecurityManager originalSecurityManager;

    @Before
    public void setUp() {
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        StashManagerFactory factory = (StashManagerFactory) appContext.getBean("stashManagerFactory");
        stashManager = factory.createStashManager();

        testAudit = new TestAudit();
        originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new GatewayPermissiveLoggingSecurityManager());
    }

    @After
    public void tearDown() throws Exception {
        System.setSecurityManager(originalSecurityManager);
    }

    // Body scan tests

    /**
     * Tests the normal case - no invalid characters
     * @throws Exception
     */
    @BugNumber(8971)
    @Test
    public void testJsonCodeInjection() throws Exception {
        final String varName = "jsonInput";

        CodeInjectionProtectionAssertion assertion = createAssertion(TargetMessageType.OTHER, false, false, true,
                PHP_EVAL_INJECTION, HTML_JAVASCRIPT, SHELL_INJECTION);
        assertion.setOtherTargetMessageVariable(varName);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = getContext(null, ContentTypeHeader.APPLICATION_JSON);
        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(noInvalidCharacters.getBytes())));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals("Status should be NONE", AssertionStatus.NONE, status);
        assertTrue(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON));
        checkAuditPresence(false, false, false, false, false);
    }

    /**
     * Valid benign response message, test against all protection types, should pass without issue.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_ValidSOAPResponse_AssertionStatusNone() throws Exception {
        CodeInjectionProtectionAssertion assertion =
                createAssertion(TargetMessageType.RESPONSE, false, false, true,
                        PHP_EVAL_INJECTION, HTML_JAVASCRIPT, SHELL_INJECTION,
                        SHELL_INJECTION, LDAP_DN_INJECTION, LDAP_SEARCH_INJECTION);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        Message response = new Message(XmlUtil.parse(getClass().getResourceAsStream(VALID_SOAP_RESPONSE)));

        PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), response);

        context.setRoutingStatus(RoutingStatus.ROUTED);

        final AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals("Status should be NONE", AssertionStatus.NONE, status);
        assertTrue(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_XML.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_XML));
        checkAuditPresence(false, false, false, false, false);
    }

    /**
     * Tests invalid characters in a key. Invalid character is ;
     * @throws Exception
     */
    @BugNumber(8971)
    @Test
    public void testJsonCodeInjection_InvalidKey() throws Exception {
        final String varName = "jsonInput";

        CodeInjectionProtectionAssertion assertion = createAssertion(TargetMessageType.OTHER, false, false, true,
                PHP_EVAL_INJECTION, HTML_JAVASCRIPT, SHELL_INJECTION);
        assertion.setOtherTargetMessageVariable(varName);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = getContext(null, ContentTypeHeader.APPLICATION_JSON);
        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(invalidCharsInKeyValue.getBytes())));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals("Status should be FALSIFIED", AssertionStatus.FALSIFIED, status);
        assertTrue(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON));
        checkAuditPresence(false, false, true, false, false);
    }

    /**
     * Tests invalid characters in a value. Invalid character is '
     * @throws Exception
     */
    @BugNumber(8971)
    @Test
    public void testJsonCodeInjection_InvalidValue() throws Exception {
        final String varName = "jsonInput";

        CodeInjectionProtectionAssertion assertion = createAssertion(TargetMessageType.OTHER, false, false, true,
                PHP_EVAL_INJECTION, HTML_JAVASCRIPT, SHELL_INJECTION);
        assertion.setOtherTargetMessageVariable(varName);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = getContext(null, ContentTypeHeader.APPLICATION_JSON);
        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(invalidCharactersInValue.getBytes())));

        AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals("Status should be FALSIFIED", AssertionStatus.FALSIFIED, status);

        context.setVariable(varName, new Message(stashManager, ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(invalidCharactersInValueInArray.getBytes())));

        status = serverAssertion.checkRequest(context);
        assertEquals("Status should be FALSIFIED", AssertionStatus.FALSIFIED, status);
        assertTrue(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON));
        checkAuditPresence(false, false, true, false, false);
    }

    /**
     * Tests that the correct status is returned when the assertion is configured to use the request.
     * @throws Exception
     */
    @BugNumber(8971)
    @Test
    public void testJsonCodeInjection_InvalidValueInRequest() throws Exception {
        CodeInjectionProtectionAssertion assertion = createAssertion(TargetMessageType.REQUEST, false, false, true,
                PHP_EVAL_INJECTION, HTML_JAVASCRIPT, SHELL_INJECTION);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = getContext(invalidCharactersInValue, ContentTypeHeader.APPLICATION_JSON);

        AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals("Status should be BAD_REQUEST", AssertionStatus.BAD_REQUEST, status);
        assertTrue(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON));
        checkAuditPresence(false, false, true, false, false);
    }

    @BugNumber(10008)
    @Test
    public void testLdapCodeInjection_Backslash() throws Exception {
        CodeInjectionProtectionAssertion assertion =
                createAssertion(TargetMessageType.REQUEST, false, false, true, LDAP_DN_INJECTION);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = getContext("\\", ContentTypeHeader.TEXT_DEFAULT);

        AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals("Incorrect status returned", AssertionStatus.FALSIFIED, status);
        assertTrue(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT));
        checkAuditPresence(false, false, true, false, false);
    }

    @BugNumber(10008)
    @Test
    public void testLdapSearchInjection_Backslash() throws Exception {
        CodeInjectionProtectionAssertion assertion =
                createAssertion(TargetMessageType.REQUEST, false, false, true, LDAP_SEARCH_INJECTION);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = getContext("\\", ContentTypeHeader.TEXT_DEFAULT);

        AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals("Incorrect status returned", AssertionStatus.FALSIFIED, status);
        assertTrue(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT));
        checkAuditPresence(false, false, true, false, false);
    }

    @BugNumber(10011)
    @Test
    public void testXpathInjection_SemiColon() throws Exception {
        CodeInjectionProtectionAssertion assertion =
                createAssertion(TargetMessageType.REQUEST, false, false, true, XPATH_INJECTION);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = getContext(";", ContentTypeHeader.TEXT_DEFAULT);

        AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals("Incorrect status returned", AssertionStatus.FALSIFIED, status);
        assertTrue(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT));
        checkAuditPresence(false, false, true, false, false);
    }

    /**
     * Test all supported code injections, apart from HTML, for it's range of illegal characters against the request body for a text
     * document. Expected outcome for each illegal character is assertion falsified.
     */
    @Test
    public void testCodeProtections_ApartFromHTML() throws Exception {
        final CodeInjectionProtectionType[] protectionTypes = CodeInjectionProtectionType.values();
        for (CodeInjectionProtectionType protectionType : protectionTypes) {

            if (protectionType == HTML_JAVASCRIPT) continue; //not as simple as going through its chars

            System.out.println(protectionType.getDisplayName());

            CodeInjectionProtectionAssertion assertion =
                    createAssertion(TargetMessageType.REQUEST, false, false, true, protectionType);

            ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

            final Pattern pattern = protectionType.getPattern();
            System.out.println(protectionType.getDescription());
            final String str = pattern.toString();
            final String patternString = str.substring(1, str.length() - 1);//ignore the opening and closing square brackets
            for (int i = 0; i < patternString.length(); i++) {

                final String illegalString = new String(new char[] {patternString.charAt(i)});
                final PolicyEnforcementContext context = getContext(illegalString, ContentTypeHeader.TEXT_DEFAULT);

                AssertionStatus status = serverAssertion.checkRequest(context);
                assertEquals("Incorrect status returned", AssertionStatus.FALSIFIED, status);
                assertTrue(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT.getMessage(),
                        testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT));
                checkAuditPresence(false, false, true, false, false);

                context.close();//close to ensure the stash manager is closed, otherwise it will continue to initialize the request with the same content!
            }
        }
    }

    // Routing check tests

    /**
     * Check is performed on REQUEST, post-routing - the server should return a FAILED status.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_REQUESTMessagePostRouting_AssertionStatusFailed() throws Exception {
        CodeInjectionProtectionAssertion assertion =
                createAssertion(TargetMessageType.REQUEST, false, false, true, SHELL_INJECTION);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = getContext("", ContentTypeHeader.TEXT_DEFAULT);
        context.setRoutingStatus(RoutingStatus.ROUTED);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);

        assertTrue(testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_ALREADY_ROUTED));

        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_FORMDATA.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_FORMDATA));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_URLENCODED.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_URLENCODED));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_XML.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_XML));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_TEXT.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_TEXT));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_XML.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_XML));

        checkAuditPresence(false, false, false, false, false);
    }

    /**
     * Check is performed on RESPONSE, not routed - the server should return a status of NONE.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_RESPONSEMessageNotRouted_AssertionStatusNone() throws Exception {
        CodeInjectionProtectionAssertion assertion =
                createAssertion(TargetMessageType.RESPONSE, false, false, true, SHELL_INJECTION);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = getContext("", ContentTypeHeader.TEXT_DEFAULT);
        context.setRoutingStatus(RoutingStatus.NONE);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        assertTrue(testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROTECTION_SKIP_RESPONSE_NOT_ROUTED));

        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_TEXT));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_JSON));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_FORMDATA.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_FORMDATA));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_URLENCODED.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_URLENCODED));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_XML.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_XML));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_TEXT.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_TEXT));
        assertFalse(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_XML.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_ATTACHMENT_XML));

        checkAuditPresence(false, false, false, false, false);
    }

    // Request URL Path tests

    /**
     * Target is a Context Variable but only includePath is specified - CODEINJECTIONPROTECTION_NOT_HTTP should
     * be logged and the path should not be scanned, but the assertion should succeed.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_IncludeUrlPathForVariableTarget_NotHttpLogged_AssertionStatusNone() throws Exception {
        final String varName = "testInput";

        CodeInjectionProtectionAssertion assertion =
                createAssertion(TargetMessageType.OTHER, true, false, false, SHELL_INJECTION);
        assertion.setOtherTargetMessageVariable(varName);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = getContext("", ContentTypeHeader.TEXT_DEFAULT);
        context.setVariable(varName, new Message());

        final AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROTECTION_NOT_HTTP));

        checkAuditPresence(false, false, false, false, false);
    }

    /**
     * Request URL Path contains double dash characters - should be caught by ORA_SQL protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveDoubleDashInUrlPathCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl("/path/to/re'source", BENIGN_URL_QUERY_STRING, true, false, PHP_EVAL_INJECTION);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(true, false, false, true, false);
    }

    // Scan all target message components test

    /**
     * Valid benign request message, test all components against all protection types, should pass without issue.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_IncludeAllComponentsForRequestTarget_AssertionStatusNone() throws Exception {
        CodeInjectionProtectionAssertion assertion = createAssertion(TargetMessageType.REQUEST, true, true, true,
                        CodeInjectionProtectionType.SHELL_INJECTION);
        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        Message request = createRequest(HttpMethod.POST, BENIGN_URL_PATH, BENIGN_URL_QUERY_STRING,
                new String(IOUtils.slurpStream(getClass().getResourceAsStream(VALID_SOAP_REQUEST)), Charsets.UTF8),
                ContentTypeHeader.XML_DEFAULT);

        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        final AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_XML.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_BODY_XML));
        checkAuditPresence(true, true, false, false, false);
    }

    // Request URL Query String tests

    /**
     * Target is a Context Variable but only includeQueryString is specified - SQLATTACK_NOT_HTTP should be logged and
     * the URL Query String should not be scanned, but the assertion should succeed.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_IncludeUrlQueryStringForVariableTarget_NotHttpLogged_AssertionStatusNone() throws Exception {
        final String varName = "testInput";

        CodeInjectionProtectionAssertion assertion =
                createAssertion(TargetMessageType.OTHER, true, false, false, SHELL_INJECTION);
        assertion.setOtherTargetMessageVariable(varName);

        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        final PolicyEnforcementContext context = getContext("", ContentTypeHeader.TEXT_DEFAULT);
        context.setVariable(varName, new Message());

        final AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROTECTION_NOT_HTTP));

        checkAuditPresence(false, false, false, false, false);
    }

    /**
     * Request URL Query String contains single quote character - should be caught by PHP_EVAL_INJECTION protection.
     * @throws Exception
     */
    @Test
    public void testCheckRequest_InvasiveSingleQuoteInUrlQueryStringCaught_AssertionStatusBadRequest() throws Exception {
        final AssertionStatus status =
                runTestOnRequestUrl(BENIGN_URL_PATH, "input=da'ta", false, true, PHP_EVAL_INJECTION);

        assertEquals(AssertionStatus.BAD_REQUEST, status);

        checkAuditPresence(false, true, false, false, true);
    }

    // Helper methods

    private AssertionStatus runTestOnRequestUrl(String urlPath, String urlQueryString,
                                                boolean includePath, boolean includeQueryString,
                                                CodeInjectionProtectionType... protections)
            throws IOException, PolicyAssertionException, SAXException {
        CodeInjectionProtectionAssertion assertion =
                createAssertion(TargetMessageType.REQUEST, includePath, includeQueryString, false, protections);
        ServerCodeInjectionProtectionAssertion serverAssertion = createServer(assertion);

        Message request = createRequest(HttpMethod.GET, urlPath, urlQueryString, "", ContentTypeHeader.TEXT_DEFAULT);

        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        return serverAssertion.checkRequest(context);
    }

    private CodeInjectionProtectionAssertion createAssertion(TargetMessageType targetMessageType, boolean includeUrlPath,
                                                             boolean includeUrlQueryString, boolean includeBody,
                                                             CodeInjectionProtectionType... protections) {
        CodeInjectionProtectionAssertion assertion = new CodeInjectionProtectionAssertion();

        assertion.setTarget(targetMessageType);
        assertion.setIncludeUrlPath(includeUrlPath);
        assertion.setIncludeUrlQueryString(includeUrlQueryString);
        assertion.setIncludeBody(includeBody);
        assertion.setProtections(protections);

        return assertion;
    }

    private ServerCodeInjectionProtectionAssertion createServer(CodeInjectionProtectionAssertion assertion)
            throws PolicyAssertionException {
        ServerCodeInjectionProtectionAssertion serverAssertion = new ServerCodeInjectionProtectionAssertion(assertion);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        return serverAssertion;
    }

    private PolicyEnforcementContext getContext(String requestBody, ContentTypeHeader contentTypeHeader) throws IOException {
        Message request = new Message();
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setMethod(HttpMethod.POST.name());
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        if (requestBody != null) {
            request.initialize(stashManager, contentTypeHeader,
                    new ByteArrayInputStream(requestBody.getBytes(Charsets.UTF8)));
        }

        PolicyEnforcementContext policyEnforcementContext =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        return policyEnforcementContext;
    }

    private Message createRequest(HttpMethod httpMethod, @Nullable String requestPath, @Nullable String queryString,
                                  @Nullable String body, ContentTypeHeader contentTypeHeader) throws IOException {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hRequest = new MockHttpServletRequest(servletContext);

        hRequest.setMethod(httpMethod.name());
        hRequest.addHeader("Content-Type", contentTypeHeader.getFullValue());

        if (null != requestPath) {
            hRequest.setRequestURI(requestPath);
        }

        if (null != queryString) {
            hRequest.setQueryString(queryString);
        }

        Message request = new Message();

        if (null != body) {
            request.initialize(stashManager, contentTypeHeader, new ByteArrayInputStream(body.getBytes(Charsets.UTF8)));
        }

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));

        return request;
    }

    /**
     * Checks presence or absence of audits to confirm the correct operations were/not carried out and any expected
     * results were recorded properly.
     */
    private void checkAuditPresence(boolean scanningUrlPath, boolean scanningUrlQueryString,
                                    boolean detectedBody, boolean detectedUrlPath, boolean detectedUrlQueryString) {
        assertEquals(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_URL_PATH.getMessage(), scanningUrlPath,
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_URL_PATH));
        assertEquals(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_URL_QUERY_STRING.getMessage(),
                scanningUrlQueryString,
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROJECTION_SCANNING_URL_QUERY_STRING));
        assertEquals(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED.getMessage(), detectedBody,
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED));
        assertEquals(AssertionMessages.SQLATTACK_DETECTED_PATH.getMessage(), detectedUrlPath,
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PATH));
        assertEquals(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PARAM.getMessage(), detectedUrlQueryString,
                testAudit.isAuditPresent(AssertionMessages.CODEINJECTIONPROTECTION_DETECTED_PARAM));
    }

    private static String noInvalidCharacters = "{\n" +
            "   \"type\":\"object\",\n" +
            "   \"properties\": {\n" +
            "\t\t\"reportType\": {\"type\":\"string\"},\n" +
            "\t\t\"entityType\": {\"type\":\"string\"},\n" +
            "\t\t\"isIgnoringPagination\": {\"type\":\"boolean\"},\n" +
            "\t\t\"entities\": {\n" +
            "\t\t\t\"type\":\"array\",\n" +
            "\t\t\t\"items\": {\n" +
            "\t\t\t\t\"type\":\"object\",\n" +
            "\t\t\t\t\"properties\":{\"clusterId\":{\"type\":\"string\"}},\n" +
            "\t\t\t\t\"additionalProperties\":false\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"summaryChart\": {\"type\":\"boolean\"},\n" +
            "\t\t\"summaryReport\": {\"type\":\"boolean\"},\n" +
            "\t\t\"reportName\": {\"type\":\"string\"}\n" +
            "\t},\n" +
            "\t\"additionalProperties\":false\n" +
            "}";

    private static String invalidCharsInKeyValue = "{\n" +
            "   \"type\":\"object\",\n" +
            "   \"properties\": {\n" +
            "\t\t\"reportType\": {\"type\":\"string\"},\n" +
            "\t\t\"entit;yType\": {\"type\":\"string\"},\n" +
            "\t\t\"isIgnoringPagination\": {\"type\":\"boolean\"},\n" +
            "\t\t\"entities\": {\n" +
            "\t\t\t\"type\":\"array\",\n" +
            "\t\t\t\"items\": {\n" +
            "\t\t\t\t\"type\":\"object\",\n" +
            "\t\t\t\t\"properties\":{\"clusterId\":{\"type\":\"string\"}},\n" +
            "\t\t\t\t\"additionalProperties\":false\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"summaryChart\": {\"type\":\"boolean\"},\n" +
            "\t\t\"summaryReport\": {\"type\":\"boolean\"},\n" +
            "\t\t\"reportName\": {\"type\":\"string\"}\n" +
            "\t},\n" +
            "\t\"additionalProperties\":false\n" +
            "}";

    private static String invalidCharactersInValue = "{\n" +
            "   \"type\":\"object\",\n" +
            "   \"properties\": {\n" +
            "\t\t\"reportType\": {\"type\":\"string\"},\n" +
            "\t\t\"entityType\": {\"type\":\"string\"},\n" +
            "\t\t\"isIgnoringPagination\": {\"type\":\"boolean\"},\n" +
            "\t\t\"entities\": {\n" +
            "\t\t\t\"type\":\"array\",\n" +
            "\t\t\t\"items\": {\n" +
            "\t\t\t\t\"type\":\"object\",\n" +
            "\t\t\t\t\"properties\":{\"clusterId\":{\"type\":\"string\"}},\n" +
            "\t\t\t\t\"additionalProperties\":false\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"summaryChart\": {\"type\":\"boolean\"},\n" +
            "\t\t\"summaryReport\": {\"type\":\"b'oolean\"},\n" +
            "\t\t\"reportName\": {\"type\":\"string\"}\n" +
            "\t},\n" +
            "\t\"additionalProperties\":false\n" +
            "}";

    private static String invalidCharactersInValueInArray = "{\n" +
            "   \"type\":\"object\",\n" +
            "   \"properties\": {\n" +
            "\t\t\"reportType\": {\"type\":\"string\"},\n" +
            "\t\t\"entityType\": {\"type\":\"string\"},\n" +
            "\t\t\"isIgnoringPagination\": {\"type\":\"boolean\"},\n" +
            "\t\t\"entities\": {\n" +
            "\t\t\t\"type\":\"array\",\n" +
            "\t\t\t\"items\": {\n" +
            "\t\t\t\t\"type\":\"object\",\n" +
            "\t\t\t\t\"properties\":{\"clusterId\":{\"type\":\"st'ring\"}},\n" +
            "\t\t\t\t\"additionalProperties\":false\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"summaryChart\": {\"type\":\"boolean\"},\n" +
            "\t\t\"summaryReport\": {\"type\":\"b'oolean\"},\n" +
            "\t\t\"reportName\": {\"type\":\"string\"}\n" +
            "\t},\n" +
            "\t\"additionalProperties\":false\n" +
            "}";

}
