/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.util.SoapMessageGenerator;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.message.Message;
import com.l7tech.common.LicenseException;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.*;
import com.l7tech.server.transport.http.HttpTransportModuleTester;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.service.ServicesHelper;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.wsdl.Definition;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Test the RegEx assertion
 */
public class RegexAssertionTest extends TestCase {
    private static MockServletApi servletApi;
    private static ServerPolicyFactory serverPolicyFactory;
    private SoapMessageProcessingServlet messageProcessingServlet;
    private static ServicesHelper servicesHelper;
    private int tokenCount = 0;
    private static TestMessageProcessor messageProcessor;

    /**
     * test <code>EchoAssertionTest</code> constructor
     */
    public RegexAssertionTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * ServerPolicyFactoryTest <code>TestCase</code>
     */
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite(RegexAssertionTest.class);
        return suite;
    }

    private static ServicesHelper getServicesHelper() {
        if (servicesHelper == null) {
            servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/common/testApplicationContext.xml");
            servicesHelper = new ServicesHelper((ServiceAdmin)servletApi.getApplicationContext().getBean("serviceAdmin"));
            messageProcessor = (TestMessageProcessor)servletApi.getApplicationContext().getBean("messageProcessor");
            serverPolicyFactory = (ServerPolicyFactory)servletApi.getApplicationContext().getBean("policyFactory");
        }
        return servicesHelper;
    }

    public void setUp() throws Exception {
        tokenCount = 0;
        getServicesHelper().deleteAllServices();
        HttpTransportModuleTester.setGlobalConnector(new SsgConnector() {
            public boolean offersEndpoint(Endpoint endpoint) {
                return true;
            }
        });
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

    public void testSimpleReplace() throws Exception {
        final String matchToken = "QQQ";
        ServicesHelper.ServiceDescriptor descriptor = servicesHelper.publish("stockQuote", TestDocuments.WSDL, getPolicy("QQQ", "ZZZ"));
        Wsdl wsdl = Wsdl.newInstance(null, new StringReader(descriptor.getWsdlXml()));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);

        SoapMessageGenerator sm = new SoapMessageGenerator(new SoapMessageGenerator.MessageInputGenerator() {
            public String generate(String messagePartName, String operationName, Definition definition) {
                ++tokenCount;
                return matchToken;
            }
        });

        int verifiedTokens = 0;
        Pattern verifier = Pattern.compile("ZZZ");
        SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];
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

    public void testBackReferenceReplace() throws Exception {
        final String matchToken = "QQQ";
        ServicesHelper.ServiceDescriptor descriptor = servicesHelper.publish("stockQuote", TestDocuments.WSDL, getPolicy("QQQ", "$0ZZZ"));
        Wsdl wsdl = Wsdl.newInstance(null, new StringReader(descriptor.getWsdlXml()));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);

        SoapMessageGenerator sm = new SoapMessageGenerator(new SoapMessageGenerator.MessageInputGenerator() {
            public String generate(String messagePartName, String operationName, Definition definition) {
                ++tokenCount;
                return matchToken;
            }
        });

        int verifiedTokens = 0;
        Pattern verifier = Pattern.compile("QQQZZZ");
        SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];
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


    public void testTwoRegexExpressionsInSequence() throws Exception {
        final String matchToken = "QQQ";
        ServicesHelper.ServiceDescriptor descriptor = servicesHelper.publish("stockQuote", TestDocuments.WSDL, getPolicy("QQQ", "ZZZ", "ZZZ", "OOO"));
        Wsdl wsdl = Wsdl.newInstance(null, new StringReader(descriptor.getWsdlXml()));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);

        SoapMessageGenerator sm = new SoapMessageGenerator(new SoapMessageGenerator.MessageInputGenerator() {
            public String generate(String messagePartName, String operationName, Definition definition) {
                ++tokenCount;
                return matchToken;
            }
        });

        int verifiedTokens = 0;
        Pattern verifier = Pattern.compile("OOO");
        SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];
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


    public void testPrependXmlDeclNoWhitespace() throws Exception {
        String message = "<foo><bar/></foo>";

        Regex regex = new Regex();
        regex.setProceedIfPatternMatches(true);
        regex.setRegex("^(.*)$");
        regex.setReplace(true);
        regex.setReplacement("<?xml version=\"1.0\" encoding=\"UTF-8\"?>$1");

        testReplacement(message, regex);
    }


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
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        ServerAssertion sass =  serverPolicyFactory.compilePolicy(regex, false);
        AssertionStatus result = sass.checkRequest(context);
        Document doc = request.getXmlKnob().getDocumentReadOnly();
        assertEquals(AssertionStatus.NONE, result);
    }


    public void testSimpleReplaceWithVariables() throws Exception {
        final String matchToken = "QQQ";
        ServicesHelper.ServiceDescriptor descriptor = servicesHelper.publish("stockQuote", TestDocuments.WSDL, getPolicy("QQQ", "${bingo}"));
        Wsdl wsdl = Wsdl.newInstance(null, new StringReader(descriptor.getWsdlXml()));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);

        SoapMessageGenerator sm = new SoapMessageGenerator(new SoapMessageGenerator.MessageInputGenerator() {
            public String generate(String messagePartName, String operationName, Definition definition) {
                ++tokenCount;
                return matchToken;
            }
        });
        MessageProcessorListener procesorListener = new MessageProcessorListener() {
            public void beforeProcessMessage(PolicyEnforcementContext context) {
                context.setVariable("bingo", "ZZZ");
            }

            public void afterProcessMessage(PolicyEnforcementContext context) {
            }
        };
        messageProcessor.addProcessorListener(procesorListener);

        int verifiedTokens = 0;
        Pattern verifier = Pattern.compile("ZZZ");
        SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];
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
}
