/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.policy;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.MessageProcessorListener;
import com.l7tech.server.MockServletApi;
import com.l7tech.server.SoapMessageProcessingServlet;
import com.l7tech.server.TestMessageProcessor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.ServerRegex;
import com.l7tech.server.service.ServicesHelper;
import com.l7tech.server.transport.http.HttpTransportModuleTester;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapMessageGenerator;
import com.l7tech.xml.soap.SoapUtil;
import static junit.framework.Assert.*;
import org.junit.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.wsdl.Definition;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test the RegEx assertion
 */
public class RegexAssertionTest {
    private static MockServletApi servletApi;
    private static ServerPolicyFactory serverPolicyFactory;
    private SoapMessageProcessingServlet messageProcessingServlet;
    private static ServicesHelper servicesHelper;
    private int tokenCount = 0;
    private static TestMessageProcessor messageProcessor;
    private static AssertionRegistry assertionRegistry;

    private static ServicesHelper getServicesHelper() {
        if (servicesHelper == null) {
            servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/server/resources/testApplicationContext.xml");
            servicesHelper = new ServicesHelper((ServiceAdmin)servletApi.getApplicationContext().getBean("serviceAdmin"));
            messageProcessor = (TestMessageProcessor)servletApi.getApplicationContext().getBean("messageProcessor");
            serverPolicyFactory = (ServerPolicyFactory)servletApi.getApplicationContext().getBean("policyFactory");
            assertionRegistry = (AssertionRegistry) servletApi.getApplicationContext().getBean("assertionRegistry");
        }
        return servicesHelper;
    }

    @Before
    public void setUp() throws Exception {
        tokenCount = 0;
        getServicesHelper().deleteAllServices();
        assertionRegistry.registerAssertion(TestEchoAssertion.class);
        HttpTransportModuleTester.setGlobalConnector(new SsgConnector() {
            @Override
            public boolean offersEndpoint(Endpoint endpoint) {
                return true;
            }
        });
    }

    @Test
    public void testSimpleReplace() throws Exception {
        final String matchToken = "QQQ";
        ServicesHelper.ServiceDescriptor descriptor = servicesHelper.publish("stockQuote", TestDocuments.WSDL, getPolicy("QQQ", "ZZZ"));
        Wsdl wsdl = Wsdl.newInstance(null, new StringReader(descriptor.getWsdlXml()));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);

        SoapMessageGenerator sm = new SoapMessageGenerator(new SoapMessageGenerator.MessageInputGenerator() {
            @Override
            public String generate(String messagePartName, String operationName, Definition definition) {
                ++tokenCount;
                return matchToken;
            }
        }, null);

        int verifiedTokens = 0;
        Pattern verifier = Pattern.compile("ZZZ");
        SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
        for (SoapMessageGenerator.Message request : requests) {
            SOAPMessage msg = request.getSOAPMessage();

            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            msg.writeTo(bo);
            servletApi.reset();
            MockHttpServletRequest mhreq = servletApi.getServletRequest();
            mhreq.addHeader(SoapUtil.SOAPACTION, request.getSOAPAction());
            mhreq.setContent(bo.toByteArray());
            MockHttpServletResponse mhres = servletApi.getServletResponse();
            messageProcessingServlet = new SoapMessageProcessingServlet();
            messageProcessingServlet.init(servletApi.getServletConfig());
            messageProcessingServlet.doPost(mhreq, mhres);
            String result = new String(mhres.getContentAsByteArray(), mhres.getCharacterEncoding());
            Matcher matcher = verifier.matcher(result);
            while (matcher.find()) {
                ++verifiedTokens;
            }
        }
        assertEquals(verifiedTokens, tokenCount);
    }

    @Test
    public void testBackReferenceReplace() throws Exception {
        final String matchToken = "QQQ";
        ServicesHelper.ServiceDescriptor descriptor = servicesHelper.publish("stockQuote", TestDocuments.WSDL, getPolicy("QQQ", "$0ZZZ"));
        Wsdl wsdl = Wsdl.newInstance(null, new StringReader(descriptor.getWsdlXml()));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);

        SoapMessageGenerator sm = new SoapMessageGenerator(new SoapMessageGenerator.MessageInputGenerator() {
            @Override
            public String generate(String messagePartName, String operationName, Definition definition) {
                ++tokenCount;
                return matchToken;
            }
        }, null);

        int verifiedTokens = 0;
        Pattern verifier = Pattern.compile("QQQZZZ");
        SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
        for (SoapMessageGenerator.Message request : requests) {
            SOAPMessage msg = request.getSOAPMessage();

            servletApi.reset();
            MockHttpServletRequest mhreq = servletApi.getServletRequest();
            mhreq.addHeader(SoapUtil.SOAPACTION, request.getSOAPAction());
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            msg.writeTo(bo);
            mhreq.setContent(bo.toByteArray());
            MockHttpServletResponse mhres = servletApi.getServletResponse();
            messageProcessingServlet = new SoapMessageProcessingServlet();
            messageProcessingServlet.init(servletApi.getServletConfig());
            messageProcessingServlet.doPost(mhreq, mhres);
            String result = new String(mhres.getContentAsByteArray(), mhres.getCharacterEncoding());
            Matcher matcher = verifier.matcher(result);
            while (matcher.find()) {
                ++verifiedTokens;
            }
        }
        assertEquals(verifiedTokens, tokenCount);
    }


    @Test
    public void testTwoRegexExpressionsInSequence() throws Exception {
        final String matchToken = "QQQ";
        ServicesHelper.ServiceDescriptor descriptor = servicesHelper.publish("stockQuote", TestDocuments.WSDL, getPolicy("QQQ", "ZZZ", "ZZZ", "OOO"));
        Wsdl wsdl = Wsdl.newInstance(null, new StringReader(descriptor.getWsdlXml()));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);

        SoapMessageGenerator sm = new SoapMessageGenerator(new SoapMessageGenerator.MessageInputGenerator() {
            @Override
            public String generate(String messagePartName, String operationName, Definition definition) {
                ++tokenCount;
                return matchToken;
            }
        }, null);

        int verifiedTokens = 0;
        Pattern verifier = Pattern.compile("OOO");
        SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
        for (SoapMessageGenerator.Message request : requests) {
            SOAPMessage msg = request.getSOAPMessage();

            servletApi.reset();
            MockHttpServletRequest mhreq = servletApi.getServletRequest();
            mhreq.addHeader(SoapUtil.SOAPACTION, request.getSOAPAction());
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            msg.writeTo(bo);
            mhreq.setContent(bo.toByteArray());
            MockHttpServletResponse mhres = servletApi.getServletResponse();
            messageProcessingServlet = new SoapMessageProcessingServlet();
            messageProcessingServlet.init(servletApi.getServletConfig());
            messageProcessingServlet.doPost(mhreq, mhres);
            String result = new String(mhres.getContentAsByteArray(), mhres.getCharacterEncoding());
            Matcher matcher = verifier.matcher(result);
            while (matcher.find()) {
                ++verifiedTokens;
            }
        }
        assertEquals(verifiedTokens, tokenCount);
    }


    @Test
    public void testPrependXmlDeclNoWhitespace() throws Exception {
        String message = "<foo><bar/></foo>";

        Regex regex = new Regex();
        regex.setProceedIfPatternMatches(true);
        regex.setRegex("^(.*)$");
        regex.setReplace(true);
        regex.setReplacement("<?xml version=\"1.0\" encoding=\"UTF-8\"?>$1");

        testReplacement(message, regex);
    }


    @Test
    public void testPrependXmlDeclWithWhitespace() throws Exception {
        String message = "\n<foo><bar/></foo>";

        Regex regex = new Regex();
        regex.setProceedIfPatternMatches(true);
        regex.setRegex("^(.*)$");
        regex.setReplace(true);
        regex.setReplacement(" <?xml version=\"1.0\" encoding=\"UTF-8\"?>$1");

        try {
            testReplacement(message, regex);
            fail("SAXParseException was not thrown for whitesapce in front of XML declaration");
        } catch (SAXParseException e) {
            // Ok
        }
    }


    private void testReplacement(String message, Regex regex) throws SAXException, LicenseException, IOException, PolicyAssertionException {
        Message request = new Message(XmlUtil.stringToDocument(message));
        Message response = new Message();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        ServerAssertion sass =  serverPolicyFactory.compilePolicy(regex, false);
        AssertionStatus result = sass.checkRequest(context);
        request.getXmlKnob().getDocumentReadOnly();
        assertEquals(AssertionStatus.NONE, result);
    }


    @Test
    public void testSimpleReplaceWithVariables() throws Exception {
        final String matchToken = "QQQ";
        ServicesHelper.ServiceDescriptor descriptor = servicesHelper.publish("stockQuote", TestDocuments.WSDL, getPolicy("QQQ", "${bingo}"));
        Wsdl wsdl = Wsdl.newInstance(null, new StringReader(descriptor.getWsdlXml()));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);

        SoapMessageGenerator sm = new SoapMessageGenerator(new SoapMessageGenerator.MessageInputGenerator() {
            @Override
            public String generate(String messagePartName, String operationName, Definition definition) {
                ++tokenCount;
                return matchToken;
            }
        }, null);
        MessageProcessorListener procesorListener = new MessageProcessorListener() {
            @Override
            public void beforeProcessMessage(PolicyEnforcementContext context) {
                context.setVariable("bingo", "ZZZ");
            }

            @Override
            public void afterProcessMessage(PolicyEnforcementContext context) {
            }
        };
        messageProcessor.addProcessorListener(procesorListener);

        int verifiedTokens = 0;
        Pattern verifier = Pattern.compile("ZZZ");
        SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
        for (SoapMessageGenerator.Message request : requests) {
            SOAPMessage msg = request.getSOAPMessage();

            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            msg.writeTo(bo);
            servletApi.reset();
            MockHttpServletRequest mhreq = servletApi.getServletRequest();
            mhreq.addHeader(SoapUtil.SOAPACTION, request.getSOAPAction());
            mhreq.setContent(bo.toByteArray());
            MockHttpServletResponse mhres = servletApi.getServletResponse();
            messageProcessingServlet = new SoapMessageProcessingServlet();
            messageProcessingServlet.init(servletApi.getServletConfig());
            messageProcessingServlet.doPost(mhreq, mhres);
            String result = new String(mhres.getContentAsByteArray(), mhres.getCharacterEncoding());
            Matcher matcher = verifier.matcher(result);
            while (matcher.find()) {
                ++verifiedTokens;
            }
        }
        messageProcessor.removeProcessorListener(procesorListener);
        assertEquals(verifiedTokens, tokenCount);
    }

    private static Assertion getPolicy(String matchPattern, String replace) {
        Regex regex = new Regex();
        regex.setRegex(matchPattern);
        regex.setReplacement(replace);
        regex.setReplace(true);

        return new AllAssertion(Arrays.asList(regex, new TestEchoAssertion()));
    }


    private static Assertion getPolicy(String matchPattern, String replace, String matchPattern2, String replace2) {
        Regex regex = new Regex();
        regex.setRegex(matchPattern);
        regex.setReplacement(replace);
        regex.setReplace(true);

        Regex regex2 = new Regex();
        regex2.setReplace(true);
        regex2.setRegex(matchPattern2);
        regex2.setReplacement(replace2);

        return new AllAssertion(Arrays.asList(regex, regex2, new TestEchoAssertion()));
    }

    private PolicyEnforcementContext context() throws IOException {
        return context(PHRASE_ORLY, PHRASE_YARLY);
    }

    private PolicyEnforcementContext context(String request, String response) throws IOException {
        return context(request, response, null, null);
    }

    private PolicyEnforcementContext context(String request, String response, String varname, Object value) throws IOException {
        Message req = message(request);
        Message resp = response == null ? new Message() : message(response);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(req, resp);
        if (varname != null) context.setVariable(varname, value);
        return context;
    }

    private Message message(String request) throws IOException {
        Message req = new Message();
        req.initialize(ContentTypeHeader.TEXT_DEFAULT, request.getBytes("utf-8"));
        return req;
    }

    private void expect(AssertionStatus expected, Regex regex, PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        ServerRegex sass = new ServerRegex(regex, null);
        AssertionStatus result;
        try {
            result = sass.checkRequest(context);
        } catch (AssertionStatusException ase) {
            result = ase.getAssertionStatus();
        }
        assertEquals(expected, result);
    }

    private String toString(PartInfo part) throws IOException {
        final byte[] bytes = part.getBytesIfAlreadyAvailable();
        assertNotNull("All tests use ByteArrayStashManager, so bytes should always be available", bytes);
        return new String(bytes, part.getContentType().getEncoding());
    }

    private String toString(Message message) throws IOException {
        return toString(message.getMimeKnob().getFirstPart());
    }

    private Regex negate(Regex regex) {
        regex.setProceedIfPatternMatches(!regex.isProceedIfPatternMatches());
        return regex;
    }

    private Regex regex(String regex) {
        return regex(regex, null);
    }

    private Regex regex(String regex, String replacement) {
        return regex(regex, replacement, null);
    }

    private Regex regex(String regex, String replacement, String target) {
        Regex ass = new Regex();
        ass.setRegex(regex);
        ass.setReplace(replacement != null);
        ass.setReplacement(replacement);
        if (target != null) {
            ass.setAutoTarget(false);
            if (REQUEST.equals(target)) {
                ass.setTarget(TargetMessageType.REQUEST);
            } else if (RESPONSE.equals(target)) {
                ass.setTarget(TargetMessageType.RESPONSE);
            } else {
                ass.setTarget(TargetMessageType.OTHER);
                ass.setOtherTargetMessageVariable(target);
            }
        }
        return ass;
    }

    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";
    private static final String VARIABLE = "my.funky.variable";

    private static final String PHRASE_FOO = "My mumbletyfoo is murbled!";
    private static final String PHRASE_POO = "My mumbletypoo is murbled!";
    private static final String PHRASE_UNICODE_JOSE = "jos\u00e9hern\u00e1ndez";
    private static final String PHRASE_UNICDE_GEN = "ingenier\u00eda";
    private static final String PHRASE_UNICODE_INTERNATIONAL = "International \u0436\u2665\u0152.exe";
    private static final String SUBSTR_FOO = "mumbletyfoo";
    private static final String SUBSTR_POO = "mumbletypoo";
    private static final String PHRASE_ORLY = "orly?";
    private static final String PHRASE_YARLY = "yarly";



    @Test
    public void testNewSimpleMatch() throws Exception {
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO), context(PHRASE_FOO, PHRASE_ORLY));
        expect(AssertionStatus.FAILED, regex(SUBSTR_FOO), context(PHRASE_POO, PHRASE_ORLY));
    }

    @Test
    public void testNewSimpleMatchNegated() throws Exception {
        expect(AssertionStatus.FAILED, negate(regex(SUBSTR_FOO)), context(PHRASE_FOO, PHRASE_ORLY));
        expect(AssertionStatus.NONE, negate(regex(SUBSTR_FOO)), context(PHRASE_POO, PHRASE_ORLY));
    }

    @Test
    public void testNewSimpleReplace() throws Exception {
        PolicyEnforcementContext context = context(PHRASE_FOO, PHRASE_ORLY);
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO, SUBSTR_POO), context);
        assertEquals(PHRASE_POO, toString(context.getRequest()));

        context = context(PHRASE_POO, PHRASE_ORLY);
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO, PHRASE_YARLY), context);
        assertEquals(PHRASE_POO, toString(context.getRequest()));

        context = context(PHRASE_POO, PHRASE_ORLY);
        expect(AssertionStatus.NONE, regex(SUBSTR_POO, PHRASE_YARLY), context);
        assertEquals("My yarly is murbled!", toString(context.getRequest()));
    }

    @Test
    public void testNewAutoTargetRequestResponse() throws Exception {
        final PolicyEnforcementContext context = context(PHRASE_FOO, PHRASE_ORLY);

        // Match against request should succeed
        context.setRoutingStatus(RoutingStatus.NONE);
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO), context);

        // Match against response should fail
        context.setRoutingStatus(RoutingStatus.ROUTED);
        expect(AssertionStatus.FAILED, regex(SUBSTR_FOO), context);

        // Match against response should fail
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);
        expect(AssertionStatus.FAILED, regex(SUBSTR_FOO), context);

        // Match against request should succeed again
        context.setRoutingStatus(RoutingStatus.NONE);
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO), context);
    }

    @Test
    public void testNewTargetRequestResponse() throws Exception {
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO, null, REQUEST), context(PHRASE_FOO, PHRASE_ORLY));
        expect(AssertionStatus.FAILED, regex(SUBSTR_FOO, null, RESPONSE), context(PHRASE_FOO, PHRASE_ORLY));
    }

    @Test
    public void testNewTargetMessageVariable() throws Exception {
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO, null, VARIABLE), context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, message(PHRASE_FOO)));
        expect(AssertionStatus.FAILED, regex(SUBSTR_FOO, null, VARIABLE), context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, message(PHRASE_POO)));
    }

    @Test
    public void testNewTargetStringVariable() throws Exception {
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO, null, VARIABLE), context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, PHRASE_FOO));
        expect(AssertionStatus.FAILED, regex(SUBSTR_FOO, null, VARIABLE), context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, PHRASE_POO));
    }
    
    @Test
    public void testReplaceStringVariable() throws Exception {
        PolicyEnforcementContext context = context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, PHRASE_FOO);
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO, SUBSTR_POO, VARIABLE), context);
        assertEquals(PHRASE_POO, context.getVariable(VARIABLE));

        context = context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, PHRASE_POO);
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO, PHRASE_YARLY, VARIABLE), context);
        assertEquals(PHRASE_POO, context.getVariable(VARIABLE));

        context = context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, PHRASE_POO);
        expect(AssertionStatus.NONE, regex(SUBSTR_POO, PHRASE_YARLY, VARIABLE), context);
        assertEquals("My yarly is murbled!", context.getVariable(VARIABLE));
    }

    @Test
    public void testNewInvalidPattern() throws Exception {
        expect(AssertionStatus.SERVER_ERROR, regex("["), context());
    }

    @Test
    public void testNewInvalidTargetVariable() throws Exception {
        expect(AssertionStatus.SERVER_ERROR, regex(SUBSTR_FOO, null, "nonexistentvariable"), context());
    }

    @Test
    public void testNewResponseTargetNotYetInitialized() throws Exception {
        try {
            expect(AssertionStatus.SERVER_ERROR, regex(SUBSTR_FOO, null, RESPONSE), context(PHRASE_ORLY, null));
            fail("Expected exception not thrown (response not yet attached to an InputStream)");
        } catch (IllegalStateException ise) {
            assertTrue(ise.getMessage().contains("This Message has not yet been attached to an InputStream"));
        }
    }

    @Test
    public void testNewOverrideCharsetMatchAgainstMessage() throws Exception {
        PolicyEnforcementContext context = context();

        // Request is encoded in ISO-8859-1, but lies and claims UTF-8
        context.getRequest().initialize(ContentTypeHeader.parseValue("text/plain; charset=UTF-8"),
                PHRASE_UNICODE_JOSE.getBytes("ISO-8859-1"));

        Regex regex = regex(PHRASE_UNICODE_JOSE);

        regex.setEncoding("ISO-8859-1");
        expect(AssertionStatus.NONE, regex, context);

        regex.setEncoding("UTF-8");
        expect(AssertionStatus.FAILED, regex, context);
    }

    @Test
    public void testNewOverrideCharsetMatchAgainstStringVar() throws Exception {
        PolicyEnforcementContext context = context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, PHRASE_UNICODE_JOSE);

        Regex regex = regex(PHRASE_UNICODE_JOSE, null, VARIABLE);

        regex.setEncoding("UTF-8");
        expect(AssertionStatus.NONE, regex, context);

        regex.setEncoding("ISO-8859-1");
        expect(AssertionStatus.FAILED, regex, context);
    }

    @Test
    public void testNewOverrideCharsetMatchReplaceStringVar() throws Exception {
        PolicyEnforcementContext context = context(PHRASE_FOO, PHRASE_ORLY, VARIABLE, PHRASE_YARLY);

        Regex regex = regex(PHRASE_YARLY, PHRASE_UNICODE_JOSE, VARIABLE);

        regex.setEncoding("UTF-8");
        expect(AssertionStatus.NONE, regex, context);
        assertEquals(PHRASE_UNICODE_JOSE, context.getVariable(VARIABLE));

        regex.setEncoding("ISO-8859-1");
        expect(AssertionStatus.NONE, regex, context);

        // TODO  This output is probably not correct; this needs more thought about what should actually happen here
        assertEquals(new String(PHRASE_UNICODE_JOSE.getBytes("UTF-8"), "ISO-8859-1"), context.getVariable(VARIABLE));
    }

    @Test
    public void testNewCaptureGroups() throws Exception {
        Regex regex = regex("m(.*?o)o");
        regex.setCaptureVar("capture");
        PolicyEnforcementContext context = context(PHRASE_FOO, PHRASE_ORLY);
        expect(AssertionStatus.NONE, regex, context);

        //noinspection unchecked
        String[] captured = (String[]) context.getVariable("capture");
        assertEquals("mumbletyfoo", captured[0]);
        assertEquals("umbletyfo", captured[1]);
    }

    @Test
    public void testNewCaseInsensitiveMatch() throws Exception {
        final Regex regex = regex("BlAh");
        regex.setCaseInsensitive(true);
        expect(AssertionStatus.NONE, regex, context("bLaH", ""));
        expect(AssertionStatus.FAILED, regex, context("HaLb", ""));
    }

    @Test
    public void testNewNoSuchPart() throws Exception {
        final Regex regex = regex(SUBSTR_FOO);
        regex.setMimePart(3); // nonexistent part
        expect(AssertionStatus.FAILED, regex, context(PHRASE_FOO, PHRASE_ORLY));
    }

    @Test
    public void testNewNoReplacement() throws Exception {
        final Regex regex = regex(SUBSTR_FOO);
        regex.setReplace(true);
        regex.setReplacement(null);
        try {
            expect(AssertionStatus.FAILED, regex, context(PHRASE_FOO, PHRASE_ORLY));
            fail("Expected assertion was not thrown");
        } catch (PolicyAssertionException e) {
            // Ok
        }
    }
}
