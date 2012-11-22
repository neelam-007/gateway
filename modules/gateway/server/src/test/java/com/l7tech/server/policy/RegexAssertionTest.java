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
import com.l7tech.test.BugNumber;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapMessageGenerator;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Before;
import org.junit.Test;
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
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.Assert.*;

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
        ServerRegex sass = new ServerRegex(regex);
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
        expect(AssertionStatus.FALSIFIED, regex(SUBSTR_FOO), context(PHRASE_POO, PHRASE_ORLY));
    }

    @Test
    public void testNewSimpleMatchNegated() throws Exception {
        expect(AssertionStatus.FALSIFIED, negate(regex(SUBSTR_FOO)), context(PHRASE_FOO, PHRASE_ORLY));
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
    public void testNewSimpleReplaceBadBackref() throws Exception {
        expect(AssertionStatus.SERVER_ERROR, regex(SUBSTR_FOO, SUBSTR_POO + "$1"), context(PHRASE_FOO, PHRASE_ORLY));
    }

    @Test
    public void testNewAutoTargetRequestResponse() throws Exception {
        final PolicyEnforcementContext context = context(PHRASE_FOO, PHRASE_ORLY);

        // Match against request should succeed
        context.setRoutingStatus(RoutingStatus.NONE);
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO), context);

        // Match against response should fail
        context.setRoutingStatus(RoutingStatus.ROUTED);
        expect(AssertionStatus.FALSIFIED, regex(SUBSTR_FOO), context);

        // Match against response should fail
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);
        expect(AssertionStatus.FALSIFIED, regex(SUBSTR_FOO), context);

        // Match against request should succeed again
        context.setRoutingStatus(RoutingStatus.NONE);
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO), context);
    }

    @Test
    public void testNewTargetRequestResponse() throws Exception {
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO, null, REQUEST), context(PHRASE_FOO, PHRASE_ORLY));
        expect(AssertionStatus.FALSIFIED, regex(SUBSTR_FOO, null, RESPONSE), context(PHRASE_FOO, PHRASE_ORLY));
    }

    @Test
    public void testNewTargetMessageVariable() throws Exception {
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO, null, VARIABLE), context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, message(PHRASE_FOO)));
        expect(AssertionStatus.FALSIFIED, regex(SUBSTR_FOO, null, VARIABLE), context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, message(PHRASE_POO)));
    }

    @Test
    public void testNewTargetStringVariable() throws Exception {
        expect(AssertionStatus.NONE, regex(SUBSTR_FOO, null, VARIABLE), context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, PHRASE_FOO));
        expect(AssertionStatus.FALSIFIED, regex(SUBSTR_FOO, null, VARIABLE), context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, PHRASE_POO));
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
    @BugNumber(10329)
    public void testNewResponseTargetNotYetInitialized() throws Exception {
        // bug 10329 - when targeted at an uninitialized response, regex will succeed only if it would otherwise match against the empty string
        // this seems like the best behavior for extrapolating the intended behavior of screening (expected-to-fail) regexes applied to an uninitialized response
        expect(AssertionStatus.FALSIFIED, regex(SUBSTR_FOO, null, RESPONSE), context(PHRASE_ORLY, null));
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
        expect(AssertionStatus.FALSIFIED, regex, context);
    }

    @Test
    public void testNewOverrideCharsetMatchAgainstStringVar() throws Exception {
        PolicyEnforcementContext context = context(PHRASE_ORLY, PHRASE_YARLY, VARIABLE, PHRASE_UNICODE_JOSE);

        Regex regex = regex(PHRASE_UNICODE_JOSE, null, VARIABLE);

        regex.setEncoding("UTF-8");
        expect(AssertionStatus.NONE, regex, context);

        regex.setEncoding("ISO-8859-1");
        expect(AssertionStatus.FALSIFIED, regex, context);
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
        expect(AssertionStatus.FALSIFIED, regex, context("HaLb", ""));
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

    @Test
    public void testPatternContextVariables() throws Exception {
        final Regex regex = regex("${pat}");
        regex.setPatternContainsVariables(true);
        final PolicyEnforcementContext context = context(PHRASE_FOO, PHRASE_ORLY);
        context.setVariable("pat", "mumble");
        expect(AssertionStatus.NONE, regex, context);
    }

    @Test
    public void testPatternLooksLikeContextVariablesButIsnt() throws Exception {
        final Regex regex = regex("${1}");
        regex.setPatternContainsVariables(false); // not actually context variable, just a pattern
        final PolicyEnforcementContext context = context(PHRASE_FOO, PHRASE_ORLY);
        context.setVariable("pat", "mumble");
        expect(AssertionStatus.NONE, regex, context);
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_Array_matchLast() throws Exception {
        doMultivaluedTest(AssertionStatus.NONE, new String[] { "foo", "bar", "blah" });
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_Array_matchAny_Compat() throws Exception {
        // Ensure array behavior is compatible with pre-Fangtooth behavior on lists:
        //   multivalued value should generate a target in the format of
        //   java.util.AbstractCollection.toString(), and try to match against that
        // Pre-Fangtooth, the target would actually be something like "[Ljava.lang.String;@22911fb5" in
        // this situation; however, for Fangtooth, we changed arrays to behave the same way as collections.

        final Regex regex = regex("^" + Pattern.quote("[foo, bar, blah]") + "$");
        regex.setAutoTarget(false);
        regex.setTarget(TargetMessageType.OTHER);
        regex.setOtherTargetMessageVariable("targetmess");

        PolicyEnforcementContext context = context(PHRASE_FOO, PHRASE_ORLY);
        context.setVariable("targetmess", new String[] { "foo", "bar", "blah" });

        expect(AssertionStatus.NONE, regex, context);
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_Array_matchFirst() throws Exception {
        doMultivaluedTest(AssertionStatus.NONE, new String[] { "blah", "bar", "asdf" });
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_Array_matchOnly() throws Exception {
        doMultivaluedTest(AssertionStatus.NONE, new String[] { "blah" });
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_Array_unmatchOnly() throws Exception {
        doMultivaluedTest(AssertionStatus.FALSIFIED, new String[] { "qwer" });
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_Array_matchEmpty() throws Exception {
        doMultivaluedTest(AssertionStatus.FALSIFIED, new String[] { });
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_Array_matchAll() throws Exception {
        doMultivaluedTest(AssertionStatus.NONE, new String[] { "blah", "barblah", "blahasdf" });
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_Array_matchNone() throws Exception {
        doMultivaluedTest(AssertionStatus.FALSIFIED, new String[] { "asdf", "qwer", "zxcv" });
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_List_matchLast() throws Exception {
        doMultivaluedTest(AssertionStatus.NONE, Arrays.asList("foo", "bar", "blah"));
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_List_matchAny_Compat() throws Exception {
        // Ensure compatibility with pre-Fangtooth behavior -- multivalued value should generate a target in the format of
        // java.util.AbstractCollection.toString(), and try to match against that
        final Regex regex = regex("^" + Pattern.quote("[foo, bar, blah]") + "$"); // Require literal match of "[foo, bar, blah]" anchored over entire target
        regex.setAutoTarget(false);
        regex.setTarget(TargetMessageType.OTHER);
        regex.setOtherTargetMessageVariable("targetmess");

        PolicyEnforcementContext context = context(PHRASE_FOO, PHRASE_ORLY);
        context.setVariable("targetmess", Arrays.asList("foo", "bar", "blah"));

        expect(AssertionStatus.NONE, regex, context);
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_List_matchFirst() throws Exception {
        doMultivaluedTest(AssertionStatus.NONE, Arrays.asList("blah", "bar", "qwef"));
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_List_matchOnly() throws Exception {
        doMultivaluedTest(AssertionStatus.NONE, Arrays.asList("blah"));
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_List_unmatchOnly() throws Exception {
        doMultivaluedTest(AssertionStatus.FALSIFIED, Arrays.asList("qwer"));
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_List_matchEmpty() throws Exception {
        doMultivaluedTest(AssertionStatus.FALSIFIED, Collections.emptyList());
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_List_matchAll() throws Exception {
        doMultivaluedTest(AssertionStatus.NONE, Arrays.asList("blah", "barblah", "blahqwef"));
    }

    @Test
    @BugNumber(12148)
    public void testMultivaluedContextVariable_List_matchNone() throws Exception {
        doMultivaluedTest(AssertionStatus.FALSIFIED, Arrays.asList("qwer", "sdgfb", "asdf"));
    }

    private void doMultivaluedTest(AssertionStatus expectedStatus, Object targetValue) throws IOException, PolicyAssertionException {
        final Regex regex = regex("blah");
        regex.setAutoTarget(false);
        regex.setTarget(TargetMessageType.OTHER);
        regex.setOtherTargetMessageVariable("targetmess");

        PolicyEnforcementContext context = context(PHRASE_FOO, PHRASE_ORLY);
        context.setVariable("targetmess", targetValue);

        expect(expectedStatus, regex, context);
    }


    @Test
    public void testFindAll() throws Exception {
        final Regex regex = regex("f([^,]+)");
        regex.setCaptureVar("v");
        regex.setFindAll(true);
        PolicyEnforcementContext context = context("fig,fog,gog,cog,frog,dog,bog,fantastic", PHRASE_ORLY);
        expect(AssertionStatus.NONE, regex, context);

        final Object vvo = context.getVariable("v");
        assertNotNull(vvo);
        assertTrue(vvo instanceof String[]);
        final String[] vv = (String[]) vvo;
        assertEquals(8, vv.length);
        assertTrue(Arrays.equals(vv, new String[]{"fig", "ig", "fog", "og", "frog", "rog", "fantastic", "antastic"}));
    }

    @Test
    public void testFindAll_noInfiniteLoop() throws Exception {
        final Regex regex = regex("");
        regex.setCaptureVar("v");
        regex.setFindAll(true);
        PolicyEnforcementContext context = context("asdfasdf", PHRASE_ORLY);
        expect(AssertionStatus.NONE, regex, context);

        final Object vvo = context.getVariable("v");
        assertNotNull(vvo);
        assertTrue(vvo instanceof String[]);
        final String[] vv = (String[]) vvo;
        assertEquals(9, vv.length);
        assertTrue(Arrays.equals(vv, new String[]{"", "", "", "", "", "", "", "", ""}));
    }

    @Test
    public void testFindAll_omitEntireExpressionCapture() throws Exception {
        final Regex regex = regex("(f[^,]+)");
        regex.setCaptureVar("v");
        regex.setFindAll(true);
        regex.setIncludeEntireExpressionCapture(false);
        PolicyEnforcementContext context = context("fig,fog,gog,cog,frog,dog,bog,fantastic", PHRASE_ORLY);
        expect(AssertionStatus.NONE, regex, context);

        final Object vvo = context.getVariable("v");
        assertNotNull(vvo);
        assertTrue(vvo instanceof String[]);
        final String[] vv = (String[]) vvo;
        assertEquals(4, vv.length);
        assertTrue(Arrays.equals(vv, new String[]{"fig", "fog", "frog", "fantastic"}));
    }

    @Test
    public void testRepeatedReplace() throws Exception {
        final Regex regex = regex("(?:^\\s*|,\\s*|&\\s*)(CN=[^,]+)");
        regex.setCaseInsensitive(true);
        regex.setReplace(true);
        regex.setIncludeEntireExpressionCapture(false);
        regex.setReplaceRepeatCount(1000);
        regex.setReplacement("");
        regex.setCaptureVar("v");

        PolicyEnforcementContext context = context("CN=TSP_Admins,OU=Groepen,DC=kadaster,DC=fto&CN=AP_MIJNKADASTER_BEHEER,OU=Groepen,DC=kadaster,DC=fto", PHRASE_ORLY);
        expect(AssertionStatus.NONE, regex, context);

        final Object vvo = context.getVariable("v");
        assertNotNull(vvo);
        assertTrue(vvo instanceof String[]);
        final String[] vv = (String[]) vvo;

        assertEquals(",OU=Groepen,DC=kadaster,DC=fto,OU=Groepen,DC=kadaster,DC=fto", toString(context.getRequest()));
        assertEquals(2, vv.length);
        assertEquals("CN=TSP_Admins", vv[0]);
        assertEquals("CN=AP_MIJNKADASTER_BEHEER", vv[1]);
    }

    @Test
    public void testRepeatedReplace_withLimit() throws Exception {
        final Regex regex = regex("(?:^\\s*|,\\s*|&\\s*)(CN=[^,]+),?");
        regex.setCaseInsensitive(true);
        regex.setReplace(true);
        regex.setIncludeEntireExpressionCapture(false);
        regex.setReplaceRepeatCount(3);
        regex.setReplacement("CN=");
        regex.setCaptureVar("v");

        PolicyEnforcementContext context = context("CN=TSP_Admins,OU=Groepen,DC=kadaster,DC=fto&CN=AP_MIJNKADASTER_BEHEER,OU=Groepen,DC=kadaster,DC=fto", PHRASE_ORLY);
        expect(AssertionStatus.NONE, regex, context);

        final Object vvo = context.getVariable("v");
        assertNotNull(vvo);
        assertTrue(vvo instanceof String[]);
        final String[] vv = (String[]) vvo;

        assertEquals("CN=DC=kadaster,DC=fto", toString(context.getRequest()));
        assertEquals(5, vv.length);
        assertTrue(Arrays.equals(vv, new String[]{"CN=TSP_Admins", "CN=AP_MIJNKADASTER_BEHEER", "CN=OU=Groepen", "CN=DC=kadaster", "CN=DC=ftoCN=OU=Groepen"}));
    }

    @Test
    @BugNumber(11696)
    public void testOptionalMatch() throws Exception {
        final Regex regex = regex("(foo )(asdf )?(bar )");
        regex.setCaseInsensitive(true);
        regex.setReplace(false);
        regex.setCaptureVar("v");

        PolicyEnforcementContext context = context("foo bar blah bletch", PHRASE_ORLY);
        expect(AssertionStatus.NONE, regex, context);

        final Object vvo = context.getVariable("v");
        assertNotNull(vvo);
        assertTrue(vvo instanceof String[]);
        final String[] vv = (String[]) vvo;

        assertEquals("foo bar ", vv[0]);
        assertEquals("foo ", vv[1]);
        assertNull(vv[2]);
        assertEquals("bar ", vv[3]);
    }

    @Test
    @BugNumber(12515)
    public void contextVariableCaseInsensitive() throws Exception {
        final Regex regex = regex("${test}");
        regex.setCaseInsensitive(true);
        regex.setPatternContainsVariables(true);

        final PolicyEnforcementContext context = context("FooBar", "", "test", "foobar");
        expect(AssertionStatus.NONE, regex, context);
    }
}
