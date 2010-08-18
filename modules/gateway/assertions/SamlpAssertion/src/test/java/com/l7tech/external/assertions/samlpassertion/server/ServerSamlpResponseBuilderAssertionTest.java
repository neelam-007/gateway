package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.SamlStatus;
import com.l7tech.external.assertions.samlpassertion.SamlVersion;
import com.l7tech.external.assertions.samlpassertion.SamlpResponseBuilderAssertion;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import saml.support.ds.SignatureType;
import saml.v2.assertion.AssertionType;
import saml.v2.assertion.EncryptedElementType;
import saml.v2.assertion.NameIDType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
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
     * Basic test for message support. Runs through main code path, where most values are auto generated.
     * Response is not signed. No issuer is added. Tests that more than one assertion can be added (authentication and attribute)
     * @throws Exception
     */
    @Test
    public void testSAML2_0_MessageSupport() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlVersion.SAML2);
        assertion.setStatusMessage("Status Message is ok");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken} ${attributeToken}");
        assertion.setResponseExtensions("${extension}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_2_0)));
        context.setVariable("attributeToken", new Message(XmlUtil.parse(v2_0AttributeAssertion)));
        context.setVariable("extension", new Message(XmlUtil.parse("<extension>im an extension element</extension>")));
        serverAssertion.checkRequest(context);

        final Message output = (Message) context.getVariable("outputVar");

        final JAXBElement<saml.v2.protocol.ResponseType> typeJAXBElement = v2Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v2.protocol.ResponseType.class);
        final saml.v2.protocol.ResponseType responseType = typeJAXBElement.getValue();

        //test required fields were added
        final String value = responseType.getStatus().getStatusCode().getValue();
        Assert.assertEquals("Invalid status found", SamlStatus.SAML2_SUCCESS.getValue(), value);

        final XMLGregorianCalendar gregCal = responseType.getIssueInstant();
        Assert.assertNotNull("No issue instant found", gregCal);

        //test no issuer was added
        final NameIDType nameIDType = responseType.getIssuer();
        Assert.assertNull("No issuer should have been added", nameIDType);

        //test no signature
        Assert.assertNull("Response should not be singed", responseType.getSignature());
        
        //test multiple assertions ok
        final List<Object> allAssertions = responseType.getAssertionOrEncryptedAssertion();
        Assert.assertEquals("Incorrect number of assertions found", 2, allAssertions.size());

        final AssertionType assertionType = (AssertionType) allAssertions.get(0);
        Assert.assertNotNull(assertionType);

        final ElementCursor cursor = new DomElementCursor(output.getXmlKnob().getDocumentReadOnly());
        final HashMap<String, String> map = getNamespaces();

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression("/samlp2:Response/saml2:Assertion", map).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        //first element should be the auth token
        final Element authTokenElement = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        failIfXmlNotEqual(samlToken_2_0, authTokenElement, "Invalid assertion element found");

        final Element attributeTokenElement = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        failIfXmlNotEqual(v2_0AttributeAssertion, attributeTokenElement, "Invalid attribute assertion element found");
    }

    /**
     * Basic test for Element support. Tests that Issuer is added and the response is signed. Validates that the
     * signature references the ID attribute on the Response element.
     * 
     * @throws Exception
     */
    @Test
    @BugNumber(9047)
    public void testSAML2_0_ElementSupport() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setAddIssuer(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlVersion.SAML2);

        assertion.setResponseAssertions("${samlToken}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final Message msg = new Message(XmlUtil.parse(samlToken_2_0));
        final Element element = msg.getXmlKnob().getDocumentReadOnly().getDocumentElement();
        context.setVariable("samlToken", element);
        serverAssertion.checkRequest(context);

        final Message output = (Message) context.getVariable("outputVar");
        final JAXBElement<saml.v2.protocol.ResponseType> typeJAXBElement = v2Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v2.protocol.ResponseType.class);
        final saml.v2.protocol.ResponseType responseType = typeJAXBElement.getValue();
        
        final AssertionType assertionType = (AssertionType) responseType.getAssertionOrEncryptedAssertion().get(0);
        Assert.assertNotNull(assertionType);

        //Validate auto fields
        //Attributes
        final String responseId = responseType.getID();
        Assert.assertTrue("responseId must not be null or empty", responseId != null && !responseId.trim().isEmpty());
        Assert.assertTrue("responseId is not a valid xsd:NCName", DomUtils.isValidXmlNcName(responseId));

        final XMLGregorianCalendar grepCal = responseType.getIssueInstant();
        Assert.assertNotNull("IssueInstant was not set", grepCal);

        Assert.assertEquals("Incorrect InResponseTo found", null, responseType.getInResponseTo());
        Assert.assertEquals("Incorrect Destination found", null, responseType.getDestination());
        Assert.assertEquals("Incorrect Consent found", null, responseType.getConsent());

        //signed
        final SignatureType sig = responseType.getSignature();
        Assert.assertNotNull("Response should be signed", sig);

        //issuer was added.
        final NameIDType nameIDType = responseType.getIssuer();
        Assert.assertNotNull("Issuer was not added", nameIDType);
        final String value = nameIDType.getValue();
        Assert.assertTrue("Value should be non null and not empty", value != null && !value.trim().isEmpty());

        //Validate the signature reference matches the ID attribute of the Response
        Assert.assertEquals("Signature does not reference the Response ID attribute", "#" + responseId, sig.getSignedInfo().getReference().get(0).getURI());
    }

    /**
     * Sets various values instead of relying on default behaviour. Validates the values. Also tests that more than
     * one extension variable can be supplied.
     * @throws Exception
     */
    @Test
    public void testSAML2_0_AllValuesAreSupplied() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML2);

        assertion.setAddIssuer(false);
        assertion.setSamlStatus(SamlStatus.SAML2_SUCCESS);
        final String statusMessage = "Status Message";
        assertion.setStatusMessage(statusMessage);
        final String statusDetail = "statusDetail";
        assertion.setStatusDetail("${" + statusDetail + "}");

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        //yyyy-MM-ddTHH:mm:ssZ
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        final String dest = "http://destination.com";
        assertion.setDestination(dest);

        final String consent = "http://consenturi.com";
        assertion.setConsent(consent);

        final String token = "samlToken";
        assertion.setResponseAssertions("${" + token + "}");
        final String encryptedToken = "encryptedAssertion";
        assertion.setEncryptedAssertions("${"+encryptedToken+"}");

        final String extensions = "extensions";
        assertion.setResponseExtensions("${" + extensions + "} ${" + extensions + "}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final String statusDetailIn = "<xml>Status Detail</xml>";
        context.setVariable(statusDetail, new Message(XmlUtil.parse(statusDetailIn)));

        final Message msg = new Message(XmlUtil.parse(samlToken_2_0));
        final Element element = msg.getXmlKnob().getDocumentReadOnly().getDocumentElement();
        context.setVariable(token, element);

        final Message encryptedMessage = new Message(XmlUtil.parse(v2_EncryptedAssertion));
        context.setVariable(encryptedToken, encryptedMessage);

        final String extensionXml = "<extension>im an extension element</extension>";
        context.setVariable(extensions, new Message(XmlUtil.parse(extensionXml)));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);
        final JAXBElement<saml.v2.protocol.ResponseType> typeJAXBElement = v2Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v2.protocol.ResponseType.class);
        final saml.v2.protocol.ResponseType responseType = typeJAXBElement.getValue();

        //Validate
        Assert.assertEquals("Incorrect version found", "2.0", responseType.getVersion());

        //Status
        final saml.v2.protocol.StatusType statusType = responseType.getStatus();
        final saml.v2.protocol.StatusCodeType statusCode = statusType.getStatusCode();
        Assert.assertEquals("Incorrect status code found", SamlStatus.SAML2_SUCCESS.getValue(), statusCode.getValue());
        Assert.assertEquals("Incorrect status message found", statusMessage, statusType.getStatusMessage());
        final Element detailElement = (Element) statusType.getStatusDetail().getAny().iterator().next();
        failIfXmlNotEqual(statusDetailIn, detailElement, "Incorrect status detail found");

        //Attributes
        Assert.assertEquals("Incorrect ID found", responseId, responseType.getID());

        final XMLGregorianCalendar grepCal = responseType.getIssueInstant();
        DateFormat dateFormat  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setCalendar(grepCal.toGregorianCalendar());
        final Date date = grepCal.toGregorianCalendar().getTime();
        Assert.assertEquals("Incorrect IssueInstant found", issueInstant, dateFormat.format(date));

        Assert.assertEquals("Incorrect InResponseTo found", requestId, responseType.getInResponseTo());
        Assert.assertEquals("Incorrect Destination found", dest, responseType.getDestination());
        Assert.assertEquals("Incorrect Consent found", consent, responseType.getConsent());        

        //Elements
        //Assertion
        final AssertionType assertionType = (AssertionType) responseType.getAssertionOrEncryptedAssertion().get(0);
        Assert.assertNotNull("Assertion was not found", assertionType);

        final ElementCursor cursor = new DomElementCursor(output.getXmlKnob().getDocumentReadOnly());
        final HashMap<String, String> map = getNamespaces();

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression("/samlp2:Response/saml2:Assertion", map).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();
        final Element assertionElement = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        failIfXmlNotEqual(samlToken_2_0, assertionElement, "Invalid assertion element found");

        //Extensions
        xpathResult = cursor.getXpathResult(new XpathExpression("/samlp2:Response/samlp2:Extensions/extension", map).compile());
        xpathResultSetIterator = xpathResult.getNodeSet().getIterator();
        Element extensionOut = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        failIfXmlNotEqual(extensionXml, extensionOut, "Invalid extensions element found");

        extensionOut = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        failIfXmlNotEqual(extensionXml, extensionOut, "Invalid extensions element found");

        //EncryptedAssertion - bug 9058
        xpathResult = cursor.getXpathResult(new XpathExpression("/samlp2:Response/saml2:EncryptedAssertion/xenc:EncryptedData", map).compile());
        xpathResultSetIterator = xpathResult.getNodeSet().getIterator();
        Element encryptedOut = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        failIfXmlNotEqual(v2_EncryptedAssertion, encryptedOut, "Invalid EncryptedAssertion element found");
    }

    /**
     * Tests that if the status is success, then the requirement for an assertion is satisified by providing an
     * EncryptedAssertion.
     * 
     * @throws Exception
     */
    @BugNumber(9058)
    @Test
    public void testSAML2_0_EncrypedAssertionOnlySuccess() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML2);

        assertion.setAddIssuer(false);
        assertion.setSamlStatus(SamlStatus.SAML2_SUCCESS);
        final String encryptedToken = "encryptedAssertion";
        assertion.setEncryptedAssertions("${"+encryptedToken+"}");
        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();

        final Message encryptedMessage = new Message(XmlUtil.parse(v2_EncryptedAssertion));
        context.setVariable(encryptedToken, encryptedMessage);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);
        final JAXBElement<saml.v2.protocol.ResponseType> typeJAXBElement = v2Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v2.protocol.ResponseType.class);
        final saml.v2.protocol.ResponseType responseType = typeJAXBElement.getValue();

        final EncryptedElementType assertionType = (EncryptedElementType) responseType.getAssertionOrEncryptedAssertion().get(0);
        Assert.assertNotNull("EncryptedAssertion was not found", assertionType);

        final ElementCursor cursor = new DomElementCursor(output.getXmlKnob().getDocumentReadOnly());
        final HashMap<String, String> map = getNamespaces();

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression("/samlp2:Response/saml2:EncryptedAssertion/xenc:EncryptedData", map).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();
        Element encryptedOut = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        failIfXmlNotEqual(v2_EncryptedAssertion, encryptedOut, "Invalid EncryptedAssertion element found");
    }

    /**
     * Tests that a field which expects Message / Element or XML as a String variables handles the following cases:
     * 1) Invalid XML
     * 2) Null value
     * 3) Unexpected type found
     *
     * Validates that each condition above causes the assertion to fail with SERVER_ERROR. Look at the output to see
     * the various messages logged, if running manually.
     * 
     * @throws Exception
     */
    @BugNumber(9048)
    @Test
    public void test_InvalidElementVariables() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML2);
        assertion.setSamlStatus(SamlStatus.SAML2_AUTHN_FAILED);

        String extensions = "extensions";
        assertion.setResponseExtensions("${" + extensions + "}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        //invalid xml

        final String extensionXml = "<extension>im an extension element</extension";
        context.setVariable(extensions, extensionXml);

        AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be SERVER_ERROR", AssertionStatus.SERVER_ERROR, status);

        //unsupported variable type
        context.setVariable(extensions, new Integer(1));
        status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be SERVER_ERROR", AssertionStatus.SERVER_ERROR, status);

        //null value built in variable
        assertion.setResponseExtensions("${gateway.invalid}");
        serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);
        
        status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be SERVER_ERROR", AssertionStatus.SERVER_ERROR, status);
    }

    @Test(expected = PolicyAssertionException.class)
    public void testSAML2_0_NoAssertionsIfNotSuccess() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML2);

        assertion.setAddIssuer(false);
        assertion.setSamlStatus(SamlStatus.SAML2_AUTHN_FAILED);

        final String token = "samlToken";
        assertion.setResponseAssertions("${" + token + "}");
        new ServerSamlpResponseBuilderAssertion(assertion, appContext);
    }

    /**
     * Validate that a non success message can be successfully created with the response builder.
     * @throws Exception
     */
    @Test
    public void testSAML2_0_NonSuccessResponse() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML2);

        assertion.setAddIssuer(false);
        assertion.setSamlStatus(SamlStatus.SAML2_AUTHN_FAILED);
        final String statusMessage = "Status Message";
        assertion.setStatusMessage(statusMessage);
        final String statusDetail = "statusDetail";
        assertion.setStatusDetail("${" + statusDetail + "}");

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        //yyyy-MM-ddTHH:mm:ssZ
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        final String dest = "http://destination.com";
        assertion.setDestination(dest);

        final String consent = "http://consenturi.com";
        assertion.setConsent(consent);

        final String extensions = "extensions";
        assertion.setResponseExtensions("${" + extensions + "}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final String statusDetailIn = "<xml>Status Detail</xml>";
        context.setVariable(statusDetail, new Message(XmlUtil.parse(statusDetailIn)));
        final String extensionXml = "<extension>im an extension element</extension>";
        context.setVariable(extensions, new Message(XmlUtil.parse(extensionXml)));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);

        final JAXBElement<saml.v2.protocol.ResponseType> typeJAXBElement = v2Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v2.protocol.ResponseType.class);
        final saml.v2.protocol.ResponseType responseType = typeJAXBElement.getValue();

        //Validate
        Assert.assertEquals("Incorrect version found", "2.0", responseType.getVersion());

        //Status
        final saml.v2.protocol.StatusType statusType = responseType.getStatus();
        final saml.v2.protocol.StatusCodeType statusCode = statusType.getStatusCode();
        Assert.assertEquals("Incorrect status code found", SamlStatus.SAML2_AUTHN_FAILED.getValue(), statusCode.getValue());
        Assert.assertEquals("Incorrect status message found", statusMessage, statusType.getStatusMessage());
        final Element detailElement = (Element) statusType.getStatusDetail().getAny().iterator().next();
        failIfXmlNotEqual(statusDetailIn, detailElement, "Incorrect status detail found");

        //Attributes
        Assert.assertEquals("Incorrect ID found", responseId, responseType.getID());

        final XMLGregorianCalendar grepCal = responseType.getIssueInstant();
        DateFormat dateFormat  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setCalendar(grepCal.toGregorianCalendar());
        final Date date = grepCal.toGregorianCalendar().getTime();
        Assert.assertEquals("Incorrect IssueInstant found", issueInstant, dateFormat.format(date));

        Assert.assertEquals("Incorrect InResponseTo found", requestId, responseType.getInResponseTo());
        Assert.assertEquals("Incorrect Destination found", dest, responseType.getDestination());
        Assert.assertEquals("Incorrect Consent found", consent, responseType.getConsent());

        //Elements

        final ElementCursor cursor = new DomElementCursor(output.getXmlKnob().getDocumentReadOnly());
        final HashMap<String, String> map = getNamespaces();

        //Extensions
        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression("/samlp2:Response/samlp2:Extensions/extension", map).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();
        final Element extensionOut = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        failIfXmlNotEqual(extensionXml, extensionOut, "Invalid extensions element found");
    }

    /**
     * Tests that the Issuer is the first element, Signature second and Extensions third.
     * @throws Exception
     */
    @Test
    @BugNumber(9035)
    public void testSaml2_0_IssuerIsFirst() throws Exception {
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML2);

        assertion.setAddIssuer(true);
        assertion.setSamlStatus(SamlStatus.SAML2_AUTHN_FAILED);

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        //yyyy-MM-ddTHH:mm:ssZ
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);

        final Node responseElement = output.getXmlKnob().getDocumentReadOnly().getFirstChild();

        Node childNode = responseElement.getFirstChild();
        Assert.assertEquals("Incorrect element found", SamlConstants.NS_SAML2, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Issuer", childNode.getLocalName());

        childNode = childNode.getNextSibling();
        Assert.assertEquals("Incorrect element found", DsigUtil.DIGSIG_URI, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Signature", childNode.getLocalName());

        childNode = childNode.getNextSibling();
        Assert.assertEquals("Incorrect element found", SamlConstants.NS_SAMLP2, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Status", childNode.getLocalName());
    }

    /**
     * Tests that the Signature is the first element.
     * @throws Exception
     */
    @Test
    @BugNumber(9035)
    public void testSaml2_0_SignatureIsFirstWhenNoIssuer() throws Exception {
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML2);

        assertion.setAddIssuer(false);

        final String token = "samlToken";
        assertion.setResponseAssertions("${" + token + "}");

        assertion.setSamlStatus(SamlStatus.SAML2_SUCCESS);

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        //yyyy-MM-ddTHH:mm:ssZ
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        final String extensions = "extensions";
        assertion.setResponseExtensions("${" + extensions + "}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final String extensionXml = "<extension>im an extension element</extension>";
        context.setVariable(extensions, new Message(XmlUtil.parse(extensionXml)));
        context.setVariable(token, new Message(XmlUtil.parse(samlToken_2_0)));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);
        final Node responseElement = output.getXmlKnob().getDocumentReadOnly().getFirstChild();
        
        Node childNode = responseElement.getFirstChild();
        Assert.assertEquals("Incorrect element found", DsigUtil.DIGSIG_URI, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Signature", childNode.getLocalName());

        childNode = childNode.getNextSibling();
        Assert.assertEquals("Incorrect element found", SamlConstants.NS_SAMLP2, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Extensions", childNode.getLocalName());

        childNode = childNode.getNextSibling();
        Assert.assertEquals("Incorrect element found", SamlConstants.NS_SAMLP2, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Status", childNode.getLocalName());

        childNode = childNode.getNextSibling();
        Assert.assertEquals("Incorrect element found", SamlConstants.NS_SAML2, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Assertion", childNode.getLocalName());
    }

    /**
     * Tests structure of created samlp:Response
     * @throws Exception
     */
    @Test
    public void testSaml1_1_VerifyStructureSignatureNoSuccess() throws Exception {
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML1_1);

        assertion.setAddIssuer(true);
        assertion.setSamlStatus(SamlStatus.SAML_REQUEST_DENIED);

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        //yyyy-MM-ddTHH:mm:ssZ
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);

        final Node responseElement = output.getXmlKnob().getDocumentReadOnly().getFirstChild();

        Node childNode = responseElement.getFirstChild();
        Assert.assertEquals("Incorrect element found", DsigUtil.DIGSIG_URI, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Signature", childNode.getLocalName());

        childNode = childNode.getNextSibling();
        Assert.assertEquals("Incorrect element found", SamlConstants.NS_SAMLP, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Status", childNode.getLocalName());
    }
    
    /**
     * Tests structure of created samlp:Response
     * @throws Exception
     */
    @Test
    public void testSaml1_1_VerifyStructureSuccessNoSignature() throws Exception {
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML1_1);

        assertion.setAddIssuer(false);

        final String token = "samlToken";
        assertion.setResponseAssertions("${" + token + "}");

        assertion.setSamlStatus(SamlStatus.SAML_SUCCESS);

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        //yyyy-MM-ddTHH:mm:ssZ
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        context.setVariable(token, new Message(XmlUtil.parse(samlToken_1_1)));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);
        final Node responseElement = output.getXmlKnob().getDocumentReadOnly().getFirstChild();

        Node childNode = responseElement.getFirstChild();
        Assert.assertEquals("Incorrect element found", DsigUtil.DIGSIG_URI, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Signature", childNode.getLocalName());

        childNode = childNode.getNextSibling();
        Assert.assertEquals("Incorrect element found", SamlConstants.NS_SAMLP, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Status", childNode.getLocalName());

        childNode = childNode.getNextSibling();
        Assert.assertEquals("Incorrect element found", SamlConstants.NS_SAML, childNode.getNamespaceURI());
        Assert.assertEquals("Incorrect element found", "Assertion", childNode.getLocalName());
    }

    /**
     * Validate that a non success message can be successfully created with the response builder.
     * @throws Exception
     */
    @Test
    public void testSAML1_1_NonSuccessResponse() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML1_1);

        assertion.setAddIssuer(false);
        assertion.setSamlStatus(SamlStatus.SAML_REQUEST_DENIED);
        final String statusMessage = "Status Message";
        assertion.setStatusMessage(statusMessage);
        final String statusDetail = "statusDetail";
        assertion.setStatusDetail("${" + statusDetail + "}");

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        //yyyy-MM-ddTHH:mm:ssZ
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final String statusDetailIn = "<xml>Status Detail</xml>";
        context.setVariable(statusDetail, new Message(XmlUtil.parse(statusDetailIn)));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);

        final JAXBElement<saml.v1.protocol.ResponseType> typeJAXBElement = v1Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v1.protocol.ResponseType.class);
        final saml.v1.protocol.ResponseType responseType = typeJAXBElement.getValue();

        //Validate
        Assert.assertEquals("Incorrect major version found", new BigInteger("1"), responseType.getMajorVersion());
        Assert.assertEquals("Incorrect minor version found", new BigInteger("1"), responseType.getMinorVersion());

        //Status
        final saml.v1.protocol.StatusType statusType = responseType.getStatus();
        final saml.v1.protocol.StatusCodeType statusCode = statusType.getStatusCode();
        Assert.assertEquals("Incorrect status code found", new QName(SamlConstants.NS_SAMLP, SamlStatus.SAML_REQUEST_DENIED.getValue()), statusCode.getValue());
        Assert.assertEquals("Incorrect status message found", statusMessage, statusType.getStatusMessage());
        final Element detailElement = (Element) statusType.getStatusDetail().getAny().iterator().next();
        failIfXmlNotEqual(statusDetailIn, detailElement, "Incorrect status detail found");

        //Attributes
        Assert.assertEquals("Incorrect ID found", responseId, responseType.getResponseID());

        final XMLGregorianCalendar grepCal = responseType.getIssueInstant();
        DateFormat dateFormat  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setCalendar(grepCal.toGregorianCalendar());
        final Date date = grepCal.toGregorianCalendar().getTime();
        Assert.assertEquals("Incorrect IssueInstant found", issueInstant, dateFormat.format(date));

        Assert.assertEquals("Incorrect InResponseTo found", requestId, responseType.getInResponseTo());
    }

    /**
     * Validates that any response id generated is valid. No Colon, cannot start with a digit.
     * @throws Exception
     */
    @Test
    public void testSAML2_0_InvalidResponseIdSupplied() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML2);

        assertion.setAddIssuer(false);
        final String responseId = "0" + HexUtils.generateRandomHexId(16) + ":";
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        assertion.setIssueInstant(issueInstant);

        final String token = "samlToken";
        assertion.setResponseAssertions("${" + token + "}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final Message msg = new Message(XmlUtil.parse(samlToken_2_0));
        final Element element = msg.getXmlKnob().getDocumentReadOnly().getDocumentElement();
        context.setVariable(token, element);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be server error", AssertionStatus.SERVER_ERROR, status);
    }

    /**
     * Sets various values instead of relying on default behaviour. Validates the values
     * @throws Exception
     */
    @Test
    public void testSAML1_1_AllValuesAreSupplied() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML1_1);

        assertion.setAddIssuer(false);
        assertion.setSamlStatus(SamlStatus.SAML_SUCCESS);
        final String statusMessage = "Status Message";
        assertion.setStatusMessage(statusMessage);
        final String statusDetail = "statusDetail";
        assertion.setStatusDetail("${" + statusDetail + "}");

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        final String recipient = "http://recipient.com";
        assertion.setRecipient(recipient);

        final String token = "samlToken";
        assertion.setResponseAssertions("${" + token + "}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final String statusDetailIn = "<xml>Status Detail</xml>";
        context.setVariable(statusDetail, new Message(XmlUtil.parse(statusDetailIn)));
        final Message msg = new Message(XmlUtil.parse(samlToken_1_1));
        final Element element = msg.getXmlKnob().getDocumentReadOnly().getDocumentElement();
        context.setVariable(token, element);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be success", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);

        final JAXBElement<saml.v1.protocol.ResponseType> typeJAXBElement = v1Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v1.protocol.ResponseType.class);
        final saml.v1.protocol.ResponseType responseType = typeJAXBElement.getValue();

        //Validate
        Assert.assertEquals("Incorrect major version found", new BigInteger("1"), responseType.getMajorVersion());
        Assert.assertEquals("Incorrect minor version found", new BigInteger("1"), responseType.getMinorVersion());

        //Status
        final saml.v1.protocol.StatusType statusType = responseType.getStatus();
        final saml.v1.protocol.StatusCodeType statusCode = statusType.getStatusCode();
        final QName value = statusCode.getValue();
        Assert.assertEquals("Incorrect status code local part found", SamlStatus.SAML_SUCCESS.getValue(), value.getLocalPart());
        Assert.assertEquals("Incorrect status code found", SamlConstants.NS_SAMLP, value.getNamespaceURI());

        Assert.assertEquals("Incorrect status message found", statusMessage, statusType.getStatusMessage());
        final Element detailElement = (Element) statusType.getStatusDetail().getAny().iterator().next();
        failIfXmlNotEqual(statusDetailIn, detailElement, "Incorrect status detail found");

        //Attributes
        Assert.assertEquals("Incorrect ID found", responseId, responseType.getResponseID());

        final XMLGregorianCalendar grepCal = responseType.getIssueInstant();
        DateFormat dateFormat  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setCalendar(grepCal.toGregorianCalendar());
        final Date date = grepCal.toGregorianCalendar().getTime();
        Assert.assertEquals("Incorrect IssueInstant found", issueInstant, dateFormat.format(date));

        Assert.assertEquals("Incorrect InResponseTo found", requestId, responseType.getInResponseTo());
        Assert.assertEquals("Incorrect Recipient found", recipient, responseType.getRecipient());

        //Elements
        //Assertion
        final saml.v1.assertion.AssertionType assertionType = responseType.getAssertion().get(0);
        Assert.assertNotNull("Assertion was not found", assertionType);

        final ElementCursor cursor = new DomElementCursor(output.getXmlKnob().getDocumentReadOnly());
        final HashMap<String, String> map = getNamespaces();

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression("/samlp:Response/saml:Assertion", map).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();
        final Element assertionElement = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        failIfXmlNotEqual(samlToken_1_1, assertionElement, "Invalid assertion element found");
    }

    /**
     * Basic test for message support.
     * @throws Exception
     */
    @Test
    public void testSAML1_1_MessageSupport() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlVersion.SAML1_1);
        assertion.setStatusMessage("Status Message is ok");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken} ${samlAttributeToken}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_1_1)));
        context.setVariable("samlAttributeToken", new Message(XmlUtil.parse(v1_1AttributeAssertion)));
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable("outputVar");

        final JAXBElement<saml.v1.protocol.ResponseType> typeJAXBElement = v1Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v1.protocol.ResponseType.class);

        final saml.v1.protocol.ResponseType responseType = typeJAXBElement.getValue();

        //test multiple assertions ok
        final List<saml.v1.assertion.AssertionType> allAssertions = responseType.getAssertion();
        Assert.assertEquals("Incorrect number of assertions found", 2, allAssertions.size());

        final saml.v1.assertion.AssertionType assertionType = allAssertions.get(0);
        Assert.assertNotNull(assertionType);

        final ElementCursor cursor = new DomElementCursor(output.getXmlKnob().getDocumentReadOnly());
        final HashMap<String, String> map = getNamespaces();

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression("/samlp:Response/saml:Assertion", map).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        //first element should be the auth token
        final Element authTokenElement = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        failIfXmlNotEqual(samlToken_1_1, authTokenElement, "Invalid assertion element found");

        final Element attributeTokenElement = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        failIfXmlNotEqual(v1_1AttributeAssertion, attributeTokenElement, "Invalid attribute assertion element found");
    }

    /**
     * Basic test for element support. Validates that the response is signed. Validates that the signature references
     * the ResponseId attribute of the Response element.
     *
     * @throws Exception
     */
    @Test
    public void testSAML1_1_ElementSupport() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlVersion.SAML1_1);
        assertion.setStatusMessage("Status Message is ok");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final Message msg = new Message(XmlUtil.parse(samlToken_1_1));
        final Element element = msg.getXmlKnob().getDocumentReadOnly().getDocumentElement();
        context.setVariable("samlToken", element);

        serverAssertion.checkRequest(context);

        final Message output = (Message) context.getVariable("outputVar");
        final JAXBElement<saml.v1.protocol.ResponseType> typeJAXBElement = v1Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v1.protocol.ResponseType.class);

        final saml.v1.protocol.ResponseType responseType = typeJAXBElement.getValue();

        final saml.v1.assertion.AssertionType assertionType = responseType.getAssertion().get(0);
        Assert.assertNotNull(assertionType);

        final SignatureType sig = responseType.getSignature();
        Assert.assertNotNull("No signature found", sig);

        //Validate the signature reference matches the ID attribute of the Response
        final String responseId = responseType.getResponseID();
        Assert.assertEquals("Signature does not reference the Response resposneId attribute", "#" + responseId, sig.getSignedInfo().getReference().get(0).getURI());

    }

    /**
     * If incorrect version of a SAML token is supplied, assertion should fail.
     * @throws Exception
     */
    @Test
    public void testSAML2_0_InvalidSamlAssertion() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlVersion.SAML2);
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
    public void testSAML1_1_InvalidSamlAssertion() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setSamlVersion(SamlVersion.SAML1_1);
        assertion.setStatusMessage("Status Message is ok");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_2_0)));
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be SERVER_ERROR", AssertionStatus.SERVER_ERROR, status);
    }

    @Test(expected = PolicyAssertionException.class)
    public void testAssertionFieldMustBeConfigured() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML2);

        assertion.setAddIssuer(false);
        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be server error", AssertionStatus.SERVER_ERROR, status);
    }

    @Test
    public void testSaml2_0_AssertionNotFoundAtRuntime() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML2);

        final String token = "samlToken";
        assertion.setResponseAssertions("${" + token + "}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be server error", AssertionStatus.SERVER_ERROR, status);
    }

    @Test
    public void testSaml1_1_AssertionNotFoundAtRuntime() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setSamlVersion(SamlVersion.SAML1_1);

        final String token = "samlToken";
        assertion.setResponseAssertions("${" + token + "}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be server error", AssertionStatus.SERVER_ERROR, status);
    }

    private HashMap<String, String> getNamespaces() {
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("samlp2", SamlConstants.NS_SAMLP2);
        map.put("samlp", SamlConstants.NS_SAMLP);
        map.put("saml2", SamlConstants.NS_SAML2);
        map.put("saml", SamlConstants.NS_SAML);
        map.put("xenc", "http://www.w3.org/2001/04/xmlenc#");
        map.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        map.put("ac", "urn:oasis:names:tc:SAML:2.0:ac");
        return map;
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

    public void failIfXmlNotEqual(String expectedXml, Element actualElement, String failMsg) throws SAXException, IOException {
        ByteArrayOutputStream byteOutExpected = new ByteArrayOutputStream();
        final Element expectedElement = XmlUtil.parse(expectedXml).getDocumentElement();
        DomUtils.stripWhitespace(expectedElement);
        XmlUtil.canonicalize(expectedElement, byteOutExpected);

        DomUtils.stripWhitespace(actualElement);
        ByteArrayOutputStream byteOutActual = new ByteArrayOutputStream();
        XmlUtil.canonicalize(actualElement, byteOutActual);

        Assert.assertEquals(failMsg, new String(byteOutExpected.toByteArray()), new String(byteOutActual.toByteArray()));
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

    private final static String v2_0AttributeAssertion = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-657be8ad16685a0cc13c2c3966d00358\" IssueInstant=\"2010-08-13T18:41:15.906Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer>irishman.l7tech.com</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Recipient=\"https://cs2.salesforce.com\" NotOnOrAfter=\"2010-08-13T18:46:15.908Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2010-08-13T18:36:15.000Z\" NotOnOrAfter=\"2010-08-13T18:46:15.907Z\"><saml2:AudienceRestriction><saml2:Audience>https://saml.salesforce.com</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AttributeStatement><saml2:Attribute Name=\"ssostartpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue>http://irishman:8080/salesforce_saml2</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>";

    private final static String v1_1AttributeAssertion = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\" AssertionID=\"SamlAssertion-2f95ff7d175a00aeb11fd3c884ef40d0\" Issuer=\"irishman.l7tech.com\" IssueInstant=\"2010-08-13T20:03:07.445Z\"><saml:Conditions NotBefore=\"2010-08-13T19:58:07.000Z\" NotOnOrAfter=\"2010-08-13T20:08:07.446Z\"><saml:AudienceRestrictionCondition><saml:Audience>https://saml.salesforce.com</saml:Audience></saml:AudienceRestrictionCondition></saml:Conditions><saml:AttributeStatement><saml:Subject><saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml:NameIdentifier><saml:SubjectConfirmation><saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:bearer</saml:ConfirmationMethod></saml:SubjectConfirmation></saml:Subject><saml:Attribute AttributeName=\"test\" AttributeNamespace=\"\"><saml:AttributeValue>testvalue</saml:AttributeValue></saml:Attribute></saml:AttributeStatement></saml:Assertion>";

    private final static String v2_EncryptedAssertion = "<xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\"/><ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/><ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:X509Data><ds:X509IssuerSerial><ds:X509IssuerName>OU=www.verisign.com/CPS Incorp.by Ref. LIABILITY LTD.(c)97 VeriSign, OU=VeriSign International Server CA - Class 3, OU=\"VeriSign, Inc.\", O=VeriSign Trust Network</ds:X509IssuerName><ds:X509SerialNumber>160479753879882120110470297434291570115</ds:X509SerialNumber></ds:X509IssuerSerial></ds:X509Data></ds:KeyInfo><xenc:CipherData><xenc:CipherValue>HvTpDcuk42MmouMpo1ZUJDSE0UrD8kqLdN58qwGWAnoPquEGYSXumJ2kKblA72EEHEEZJihb/qCfpaU1ZALJixI/PJn2COsK092/s1wnsMyay7SmrJ6MD+eCDCOYWWgiYIyhnSL9KHuF7ULXOw8xX1XAXUJFeAJpB/LtpekgNq8=</xenc:CipherValue></xenc:CipherData></xenc:EncryptedKey></ds:KeyInfo><xenc:CipherData><xenc:CipherValue>RIfrijQJDgu8GtRV3yTo7mNYZlheoMzTbXtWRhqvpPuJQurTzDy9dkBkhvgtDrPwXtoPpmw0gmHrYRfNmli9q8h7M+CDZEEVdyRvynbZh7Z2zuoL8op8adzgt9JoaW79xjVRZj0zkZ/S0h/G/wPLEOk05k3l+3Umik/3XHqCEpmabm5FKSx9qq/deYzElppFRokxumtPMuNHvwZhdQvKeDuArqA2LDq/U5kijRh7KkotibKwxBR0zbNNk3kLKadCfmHdk3x0kn8vbD4Kpvm4pVvySPtBoVty73UFgpSyirkHRzOkTihCNBJpAKaUcRyJ9d2BbHGLdcf8caRCNdXWntynBm4D5mHFteVWIolUwdCewhh9ZE1wcL5fcNFSSzi30sNQknP8f57Lt8lM0ikqTdsRZc2Hh2MVIsCkZrDuT2m3TtCoUOlTjtnEP3Of0HJbYAWOGfsmbSjlXMTEXTrX4SxPRUfvISMY5RQQKbg4G8AfbbGGK031d1REzLEqHG6zdKvc2Zb0zhstqHeO7ZIpl+rKR+/7/Pe1YtwYIsYEvsMbBJ+NqZZbC1CytjBLkp2n80ZP09230XGpDHK0XCXmKAQpBbay7BT7ULHTUGasovpKKH4ytIA0tvLJ2yfh5Z359zId3VxZZlxxRcJmJBPuApYkvbq5sVchJVHMCfQTKeRn5FYrQu6GaQp+edA1ni63I/lsPNAR7BBqBtmealk/LpROUWKMGbpRagJXUWkTi2i6nfs0hVOQP7lLHkV1SJZeBRMSixJatuNTQ1diqQQJgUfr4XFGvnnFRxqnSBuCphsAYqi5rTqfKLTVteWtdRL+smlD+l+lMcDkHzMtzHO3QyIiNknrPzKsAAbJjUf82vHOMorY5TbkddZ/2BPyBfr4y/IzTWYxHAuwHPsi6u23x4qWJOidBxDfRzfSm5K6op2i00D4rEPpALEHKWJj7aoizv4Tmd4YeUO49K0q6A6RzqP9H77hYvG+nWvc9Kodg/JNB91dEmyJMso+07WaGJYnynzwtqpeDxf+rtNO6SeDoqxtVACoNMj0SaOgdYPl4lDmEqBrXh+HE78SRZ2oSt3ATttkyQ9EoLsHMKcjprhqpUIyI/ipdwx2HH0sNLFjrwfFzhfhANMI/PqBhbZ2C4/bWANhflguB62O+RxbeFW8JQcOdot9lXRAE3KrGuullS6SytYcFwnvt/ysUktQ+ZUEmO2ZBJvJLQaNdT41R6DPjKu0rdKtNRhC6IwK88s0K0/mUYc9I6ixOX0FrgxBSBZ75U1NABXGeJPtzV5XHUNMgTdajqvJINiNNe6cqc3TiPxkuSBY0DBe3c4ZMkT4OCg72PuZ62dnoZ9GU3nu9SIEcYv5JFRdy3YuJWP3wIteFBLil8HBibbxCmf8uHiUZxXad9AvOYLj0G5TFZ0hRUlN4Vb6dOF8c325j9+0VtxNXRbHepDVQBQYdyXh6FuA4ajGOUBVqJjZdhCPgVHACnWT7z6xRewa/g3FPfJxmOX+Vl9OopzJGY77z1GzWK/vyvUtKfCWM0ea8Z/U9byOJA6pzg7UUgX5lMYAhkg2Inr8OegNSAHhe9EFrWolf3THVhNPAaOYAF3wNX3eOaHzpvGyYdhTVZXYwfybJfsBrI82bhZkx5TZdXMa8eFK3cuhglZIh/xXVaxfDto5/l6UWPiqhfhum/ewdCYEv9TK2tB2i4pCndy87KTG/sDVn79MV6StujSX80WGToz3cOOlmPx3ItuoMZiRV8Gdu7P5zj2C9sEuNYulQMpghlvrFvH4Ms5V7qMECloUKWKpTr2XcV6w2hyY5eLjo/w9Y4G3roSGbJF4iSXzuGdN6/ocFijz7C30FV/5tATiLgahcPOtuUYpE39hmtknI/1hZGYzxUiPQkOe/9EZoU1j+Z9pswAxC/loQgm89T/UOmvyiE2CJBJXAw==</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData>";
}
