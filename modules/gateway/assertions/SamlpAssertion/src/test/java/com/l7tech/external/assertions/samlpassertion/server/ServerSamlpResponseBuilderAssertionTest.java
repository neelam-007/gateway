package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.SamlpResponseBuilderAssertion;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Element;
import saml.v2.assertion.AssertionType;
import saml.v2.protocol.ResponseType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Basic test coverage. Runs through the main code paths. //todo More verification tests are required.
 * @author darmstrong
 */

public class ServerSamlpResponseBuilderAssertionTest {

    private Marshaller v1Marshaller;
    private Unmarshaller v1Unmarshaller;
    private Unmarshaller v2Unmarshaller;
    private Marshaller v2Marshaller;
    private String lockIdV1 = "lockIdSamlPV1";
    private String lockIdV2 = "lockIdSamlPV2";
    private String unMarLockV1 = lockIdV1+"UnMarshall";
    private String unMarLockV2 = lockIdV2+"UnMarshall";

    @Before
    public void setUp() throws JAXBException {
        v1Unmarshaller = JaxbUtil.getUnmarshallerV1(unMarLockV1);
        v1Marshaller = JaxbUtil.getMarshallerV1(lockIdV1);

        v2Unmarshaller = JaxbUtil.getUnmarshallerV2(unMarLockV2);
        v2Marshaller = JaxbUtil.getMarshallerV2(lockIdV2);
    }

    @After
    public void tearDown(){
        JaxbUtil.releaseJaxbResources(lockIdV1);
        JaxbUtil.releaseJaxbResources(unMarLockV1);

        JaxbUtil.releaseJaxbResources(lockIdV2);
        JaxbUtil.releaseJaxbResources(unMarLockV2);
    }

    /**
     * Basic test for message support. 
     * @throws Exception
     */
    @Test
    public void testSAML2_MessageSupport() throws Exception{

        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlpResponseBuilderAssertion.SamlVersion.SAML2);
        assertion.setStatusMessage("Status Message is ok");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken}");
        assertion.setResponseExtensions("${extension}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_2_0)));
//        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_1_1)));
        context.setVariable("extension", new Message(XmlUtil.parse("<extension>im an extension element</extension>")));
        serverAssertion.checkRequest(context);

        final Message output = (Message) context.getVariable("outputVar");
//        System.out.println("output:" + XmlUtil.nodeToFormattedString(output.getXmlKnob().getDocumentReadOnly()));

        final JAXBElement<ResponseType> typeJAXBElement = v2Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), ResponseType.class);
        final ResponseType responseType = typeJAXBElement.getValue();

        final AssertionType assertionType = (AssertionType) responseType.getAssertionOrEncryptedAssertion().get(0);
        Assert.assertNotNull(assertionType);

        System.out.println("output:" + XmlUtil.nodeToFormattedString(output.getXmlKnob().getDocumentReadOnly()));
    }

    /**
     * Basic test for Element support
     * @throws Exception
     */
    @Test
    public void testSAML2_ElementSupport() throws Exception{

        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlpResponseBuilderAssertion.SamlVersion.SAML2);

        assertion.setResponseAssertions("${samlToken}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final Message msg = new Message(XmlUtil.parse(samlToken_2_0));
        final Element element = msg.getXmlKnob().getDocumentReadOnly().getDocumentElement();
        context.setVariable("samlToken", element);
        serverAssertion.checkRequest(context);

        final Message output = (Message) context.getVariable("outputVar");
        final JAXBElement<ResponseType> typeJAXBElement = v2Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), ResponseType.class);
        final ResponseType responseType = typeJAXBElement.getValue();
        
        final AssertionType assertionType = (AssertionType) responseType.getAssertionOrEncryptedAssertion().get(0);
        Assert.assertNotNull(assertionType);

        System.out.println("output:" + XmlUtil.nodeToFormattedString(output.getXmlKnob().getDocumentReadOnly()));
    }

    /**
     * Basic test for message support.
     * @throws Exception
     */
    @Test
    public void testSAML1_0_MessageSupport() throws Exception{

        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlpResponseBuilderAssertion.SamlVersion.SAML1_1);
        assertion.setStatusMessage("Status Message is ok");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_1_1)));
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable("outputVar");
//        System.out.println("output:" + XmlUtil.nodeToFormattedString(output.getXmlKnob().getDocumentReadOnly()));

        final JAXBElement<saml.v1.protocol.ResponseType> typeJAXBElement = v1Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v1.protocol.ResponseType.class);

//        final Object o = v1Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly());
//        System.out.println(o);
        final saml.v1.protocol.ResponseType responseType = typeJAXBElement.getValue();

        final saml.v1.assertion.AssertionType assertionType = responseType.getAssertion().get(0);
        Assert.assertNotNull(assertionType);

        System.out.println("output:" + XmlUtil.nodeToFormattedString(output.getXmlKnob().getDocumentReadOnly()));
    }

    /**
     * Basic test for element support.
     * @throws Exception
     */
    @Test
    public void testSAML1_0_ElementSupport() throws Exception{

        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlpResponseBuilderAssertion.SamlVersion.SAML1_1);
        assertion.setStatusMessage("Status Message is ok");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final Message msg = new Message(XmlUtil.parse(samlToken_1_1));
        final Element element = msg.getXmlKnob().getDocumentReadOnly().getDocumentElement();
        context.setVariable("samlToken", element);

        serverAssertion.checkRequest(context);

        final Message output = (Message) context.getVariable("outputVar");
//        System.out.println("output:" + XmlUtil.nodeToFormattedString(output.getXmlKnob().getDocumentReadOnly()));

        final JAXBElement<saml.v1.protocol.ResponseType> typeJAXBElement = v1Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v1.protocol.ResponseType.class);

//        final Object o = v1Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly());
//        System.out.println(o);
        final saml.v1.protocol.ResponseType responseType = typeJAXBElement.getValue();

        final saml.v1.assertion.AssertionType assertionType = responseType.getAssertion().get(0);
        Assert.assertNotNull(assertionType);

        System.out.println("output:" + XmlUtil.nodeToFormattedString(output.getXmlKnob().getDocumentReadOnly()));        
    }

    /**
     * If incorrect version of a SAML token is supplied, assertion should fail.
     * @throws Exception
     */
    @Test
    public void testSAML2_InvalidSamlAssertion() throws Exception{

        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlpResponseBuilderAssertion.SamlVersion.SAML2);
        assertion.setStatusMessage("Status Message is ok");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken}");
        assertion.setResponseExtensions("${extension}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_1_1)));
        context.setVariable("extension", new Message(XmlUtil.parse("<extension>im an extension element</extension>")));
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be SERVER_ERROR", AssertionStatus.SERVER_ERROR, status);
    }

    /**
     * If incorrect version of a SAML token is supplied, assertion should fail.
     * @throws Exception
     */
    @Test
    public void testSAML1_0_InvalidSamlAssertion() throws Exception{

        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlpResponseBuilderAssertion.SamlVersion.SAML1_1);
        assertion.setStatusMessage("Status Message is ok");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_2_0)));
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be SERVER_ERROR", AssertionStatus.SERVER_ERROR, status);
    }

    private PolicyEnforcementContext getContext() throws IOException {

        Message request = new Message();
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        PolicyEnforcementContext policyEnforcementContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        return policyEnforcementContext;

    }

    private final static String samlToken_1_1 = "  <saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" AssertionID=\"SamlAssertion-94b99f0213cdf988f1931cd55aa91232\" IssueInstant=\"2010-07-12T23:30:29.274Z\" Issuer=\"irishman.l7tech.com\" MajorVersion=\"1\" MinorVersion=\"1\">\n" +
            "    <saml:Conditions NotBefore=\"2010-07-12T23:28:29.000Z\" NotOnOrAfter=\"2010-07-12T23:32:29.276Z\">\n" +
            "      <saml:AudienceRestrictionCondition>\n" +
            "        <saml:Audience>https://saml.salesforce.com</saml:Audience>\n" +
            "      </saml:AudienceRestrictionCondition>\n" +
            "    </saml:Conditions>\n" +
            "    <saml:AttributeStatement>\n" +
            "      <saml:Subject>\n" +
            "        <saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"\">dummy</saml:NameIdentifier>\n" +
            "        <saml:SubjectConfirmation>\n" +
            "          <saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:bearer</saml:ConfirmationMethod>\n" +
            "        </saml:SubjectConfirmation>\n" +
            "      </saml:Subject>\n" +
            "      <saml:Attribute AttributeName=\"ssostartpage\" AttributeNamespace=\"\">\n" +
            "        <saml:AttributeValue>http://irishman:8080/web_sso</saml:AttributeValue>\n" +
            "      </saml:Attribute>\n" +
            "    </saml:AttributeStatement>\n" +
            "    <saml:AuthenticationStatement AuthenticationInstant=\"2010-07-12T23:30:29.274Z\" AuthenticationMethod=\"urn:oasis:names:tc:SAML:1.0:am:password\">\n" +
            "      <saml:Subject>\n" +
            "        <saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"\">dummy</saml:NameIdentifier>\n" +
            "        <saml:SubjectConfirmation>\n" +
            "          <saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:bearer</saml:ConfirmationMethod>\n" +
            "        </saml:SubjectConfirmation>\n" +
            "      </saml:Subject>\n" +
            "      <saml:SubjectLocality IPAddress=\"10.7.48.153\"/>\n" +
            "    </saml:AuthenticationStatement>\n" +
            "  </saml:Assertion>";

    private final static String samlToken_2_0 = "  <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"SamlAssertion-9190940a89f8a5fa9837642a35d01f9a\" IssueInstant=\"2010-08-04T16:54:02.494Z\" Version=\"2.0\">\n" +
            "    <saml2:Issuer>irishman.l7tech.com</saml2:Issuer>\n" +
            "    <saml2:Subject>\n" +
            "      <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml2:NameID>\n" +
            "      <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
            "        <saml2:SubjectConfirmationData NotOnOrAfter=\"2010-07-30T22:53:06.375Z\" Recipient=\"https://cs2.salesforce.com\"/>\n" +
            "      </saml2:SubjectConfirmation>\n" +
            "    </saml2:Subject>\n" +
            "    <saml2:Conditions NotBefore=\"2010-08-04T16:50:02.000Z\" NotOnOrAfter=\"2010-08-04T16:58:02.754Z\">\n" +
            "      <saml2:AudienceRestriction>\n" +
            "        <saml2:Audience>https://saml.salesforce.com</saml2:Audience>\n" +
            "      </saml2:AudienceRestriction>\n" +
            "    </saml2:Conditions>\n" +
            "    <saml2:AttributeStatement>\n" +
            "      <saml2:Attribute Name=\"ssostartpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\">\n" +
            "        <saml2:AttributeValue>http://irishman:8080/web_sso</saml2:AttributeValue>\n" +
            "      </saml2:Attribute>\n" +
            "    </saml2:AttributeStatement>\n" +
            "    <saml2:AuthnStatement AuthnInstant=\"2010-08-04T16:54:02.494Z\">\n" +
            "      <saml2:SubjectLocality Address=\"10.7.48.153\"/>\n" +
            "      <saml2:AuthnContext>\n" +
            "        <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml2:AuthnContextClassRef>\n" +
            "        <saml2:AuthnContextDecl xmlns:saccpwd=\"urn:oasis:names:tc:SAML:2.0:ac:classes:Password\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"saccpwd:AuthnContextDeclarationBaseType\">\n" +
            "          <saccpwd:AuthnMethod>\n" +
            "            <saccpwd:Authenticator>\n" +
            "              <saccpwd:RestrictedPassword>\n" +
            "                <saccpwd:Length min=\"3\"/>\n" +
            "              </saccpwd:RestrictedPassword>\n" +
            "            </saccpwd:Authenticator>\n" +
            "          </saccpwd:AuthnMethod>\n" +
            "        </saml2:AuthnContextDecl>\n" +
            "      </saml2:AuthnContext>\n" +
            "    </saml2:AuthnStatement>\n" +
            "  </saml2:Assertion>";
}
