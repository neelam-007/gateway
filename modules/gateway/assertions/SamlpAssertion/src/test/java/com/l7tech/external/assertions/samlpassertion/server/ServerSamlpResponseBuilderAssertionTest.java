package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.SamlStatus;
import com.l7tech.external.assertions.samlpassertion.SamlVersion;
import com.l7tech.external.assertions.samlpassertion.SamlpResponseBuilderAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
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
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import saml.support.ds.SignatureType;
import saml.v2.assertion.AssertionType;
import saml.v2.assertion.EncryptedElementType;
import saml.v2.assertion.NameIDType;
import saml.v2.protocol.ResponseType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * @author darmstrong
 */
@SuppressWarnings({"JavaDoc"})
public class ServerSamlpResponseBuilderAssertionTest {

    private Unmarshaller v1Unmarshaller;
    private Unmarshaller v2Unmarshaller;

    @Before
    public void setUp() throws JAXBException {
        v1Unmarshaller = JaxbUtil.getUnmarshallerV1();

        v2Unmarshaller = JaxbUtil.getUnmarshallerV2();
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

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());
        assertion.setStatusMessage("Status Message is ok");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken} ${attributeToken}");
        assertion.setResponseExtensions("${extension}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_2_0)));
        context.setVariable("attributeToken", new Message(XmlUtil.parse(v2_AttributeAssertion)));
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
        failIfXmlNotEqual(v2_AttributeAssertion, attributeTokenElement, "Invalid attribute assertion element found");
    }

    /**
     * Validates every profile rule which the Server assertion checks when the validate system property is on
     * @throws Exception
     */
    @BugNumber(9056)
    @Test
    public void testSaml_2_0_validateAllProfileRules() throws Exception{
        v2EvaluateAllProfileRules(true, true);
    }

    /**
     * Tests that validation is correctly controlled by the validation system property.
     * @throws Exception
     */
    @BugNumber(9056)
    @Test
    public void testSaml_2_0_NoValidationWhenSystemPropertyIsOff() throws Exception{
        try {
            SyspropUtil.setProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile", "false" );
            v2EvaluateAllProfileRules(false, true);
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile" );
        }
    }

    /**
     * Validates every profile rule which the Server assertion checks when the validate system property is on
     * @throws Exception
     */
    @BugNumber(9056)
    @Test
    public void testSaml_1_1_validateAllProfileRules() throws Exception{
        v1EvaluateAllProfileRules(true, true);
    }

    /**
     * Tests that validation is correctly controlled by the validation system property.
     * @throws Exception
     */
    @BugNumber(9056)
    @Test
    public void testSaml_1_1_NoValidationWhenSystemPropertyIsOff() throws Exception{
        try {
            SyspropUtil.setProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile", "false" );
            v1EvaluateAllProfileRules(false, true);
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile" );
        }
    }

    /**
     * Tests configuration not to validate Web SSO rules.
     */
    @BugNumber(10960)
    @Test
    public void testSaml_2_0_NoValidationWhenAssertionNotConfiguredToValidate() throws Exception{
        v2EvaluateAllProfileRules(false, false);
    }

    /**
     * Tests configuration not to validate Web SSO rules.
     */
    @BugNumber(10960)
    @Test
    public void testSaml_1_0_NoValidationWhenAssertionNotConfiguredToValidate() throws Exception{
        v1EvaluateAllProfileRules(false, false);
    }

    private void v1EvaluateAllProfileRules(final boolean expectThrow, boolean validateWebSsoRules) throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        final PolicyEnforcementContext context = getContext();
        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());
        assertion.setSamlStatusCode(SamlStatus.SAML_SUCCESS.getValue());
        assertion.setValidateWebSsoRules(validateWebSsoRules);

        assertion.setResponseAssertions("${samlToken}");
        context.setVariable("samlToken", new Message(XmlUtil.parse(v1_1AttributeAssertion)));
        boolean correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("A recipient must be supplied.", correctExceptionThrown);

        assertion.setRecipient("   - ");
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("A valid recipient must be supplied.", correctExceptionThrown);

        assertion.setRecipient("http://recipient.com");
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("An SSO assertion must be supplied.", correctExceptionThrown);

        //No problems when two assertions are presented. Both satisfy bearer criteria
        assertion.setResponseAssertions("${samlToken} ${attributeToken}");
        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_1_1)));
        context.setVariable("attributeToken", new Message(XmlUtil.parse(v1_1AttributeAssertion)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, false);
        Assert.assertTrue("No exception should be thrown.", correctExceptionThrown);

        //One bearer assertion is missing the NotBefore and NotOnOrAfter Conditions - should be ok, as only 1 is needed

        assertion.setResponseAssertions("${samlToken}");
        context.setVariable("samlToken", new Message(XmlUtil.parse(v1_missingNotBeforeCondition)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("NotBefore condition attribute is missing.", correctExceptionThrown);

        //non bearer subject based statement
        assertion.setResponseAssertions("${samlToken} ${attributeToken}");
        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_1_1)));
        context.setVariable("attributeToken", new Message(XmlUtil.parse(v1_attributeOnlySenderVouches)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("Each subject based assertion must use a confirmation method of bearer.", correctExceptionThrown);

    }

    private void v2EvaluateAllProfileRules(final boolean expectThrow, boolean validateWebSsoRules) throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        final PolicyEnforcementContext context = getContext();
        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");
        assertion.setVersion(SamlVersion.SAML2.getVersionInt());
        assertion.setValidateWebSsoRules(validateWebSsoRules);

        //If Success an Assertion must be supplied
        boolean correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("Status is success so a SAML token is required.", correctExceptionThrown);

        //If not success, then no assertions are allowed
        assertion.setSamlStatusCode(SamlStatus.SAML2_AUTHN_FAILED.getValue());
        assertion.setResponseAssertions("${samlToken}");
        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_2_0)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("Status is not success so no SAML token can be configured.", correctExceptionThrown);

        //Issuer required if response is signed
        assertion.setSamlStatusCode(SamlStatus.SAML2_SUCCESS.getValue());
        assertion.setSignResponse(true);

        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("Issuer is required when the response is signed.", correctExceptionThrown);

        //Subjects must be the same across all assertions
        assertion.setResponseAssertions("${samlToken} ${attributeDifferentSubject}");
        assertion.setSignResponse(false);
        context.setVariable("attributeDifferentSubject", new Message(XmlUtil.parse(v2_AttributeDifferentSubject)));

        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("All assertions must have the same subject.", correctExceptionThrown);

        //Response must contain a bearer assertion
        assertion.setResponseAssertions("${samlToken}");
        context.setVariable("samlToken", new Message(XmlUtil.parse(v2_NoBearerAssertion)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("A bearer assertion is required.", correctExceptionThrown);

        //Response must contain a valid bearer assertion
        //No Recipient attribute
        context.setVariable("samlToken", new Message(XmlUtil.parse(v2_NoRecipientAssertion)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("A bearer assertion with a Recipient attribute is required.", correctExceptionThrown);

        //No NoOnOrAfter attribute
        context.setVariable("samlToken", new Message(XmlUtil.parse(v2_NoNotOnorAfterAssertion)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("A bearer assertion with a NotOnOrAfter attribute is required.", correctExceptionThrown);

        //Contains a NotBefore attribute
        context.setVariable("samlToken", new Message(XmlUtil.parse(v2_WithNotBeforeAssertion)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("A bearer assertion with no NotBefore attribute is required.", correctExceptionThrown);

        //No authentication statement
        context.setVariable("samlToken", new Message(XmlUtil.parse(v2_NoAuthenticationStatement)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("A bearer assertion with no NotBefore attribute is required.", correctExceptionThrown);

        //No audience restriction on an individual bearer assertion
        assertion.setResponseAssertions("${samlToken} ${attributeNoAudience}");
        context.setVariable("attributeNoAudience", new Message(XmlUtil.parse(v2_NoAudienceRestriction)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("A bearer assertion with no NotBefore attribute is required.", correctExceptionThrown);

        //All issuers must be the same
        assertion.setResponseAssertions("${samlToken} ${tokenDiffIssuer}");
        context.setVariable("tokenDiffIssuer", new Message(XmlUtil.parse(v2_DifferentIssuer)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("All Issuers must be the same.", correctExceptionThrown);

        //Issuer has the wrong format
        assertion.setResponseAssertions("${samlToken}");
        context.setVariable("samlToken", new Message(XmlUtil.parse(v2_IssuerWrongFormat)));
        correctExceptionThrown = evaluateServerAssertion(appContext, context, assertion, expectThrow);
        Assert.assertTrue("Issuer has an incorrect format.", correctExceptionThrown);

    }

    private boolean evaluateServerAssertion(ApplicationContext appContext,
                                            PolicyEnforcementContext context,
                                            SamlpResponseBuilderAssertion assertion,
                                            boolean wantException)
            throws PolicyAssertionException, IOException {
        ServerSamlpResponseBuilderAssertion  serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);
        boolean correctExceptionThrown = (wantException)? false: true;
        try {
            serverAssertion.checkRequest(context);
        } catch (AssertionStatusException e) {
            correctExceptionThrown = (wantException)? true: false;
        }
        return correctExceptionThrown;
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
        assertion.includeIssuer(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

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
     * Tests support for Element variables and index multi valued variables.
     * Tests that Issuer is added and the response is signed. Validates that the signature references the ID attribute
     * on the Response element.
     *
     * @throws Exception
     */
    @Test
    @BugNumber(9077)
    public void testSAML2_0_IndexVariableSupport() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.includeIssuer(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

        assertion.setResponseAssertions("${samlToken[1]} ${attributeToken}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final Message msg = new Message(XmlUtil.parse(samlToken_2_0));
        final Element element = msg.getXmlKnob().getDocumentReadOnly().getDocumentElement();
        List list = new ArrayList();
        list.add(null);
        list.add(element);
        context.setVariable("samlToken", list);
        context.setVariable("attributeToken", new Message(XmlUtil.parse(v2_AttributeAssertion)));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable("outputVar");
        final ElementCursor cursor = new DomElementCursor(output.getXmlKnob().getDocumentReadOnly());
        final HashMap<String, String> map = getNamespaces();

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression("/samlp2:Response/saml2:Assertion", map).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        failIfXmlNotEqual(samlToken_2_0, xpathResultSetIterator.nextElementAsCursor().asDomElement(), "Incorrect Assertion found");
        failIfXmlNotEqual(v2_AttributeAssertion, xpathResultSetIterator.nextElementAsCursor().asDomElement(), "Incorrect Assertion found");
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

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

        assertion.includeIssuer(true);
        assertion.setSamlStatusCode(SamlStatus.SAML2_SUCCESS.getValue());
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

        final Document documentReadOnly = output.getXmlKnob().getDocumentReadOnly();
        System.out.println(XmlUtil.nodeToFormattedString(documentReadOnly));
        final ElementCursor cursor = new DomElementCursor(documentReadOnly);
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
     * When an encrypted assertion is supplied, then all profile rules which relate to the contents of the included
     * SAML assertions cannot be validated. This tests that the Issuer rule is validated when encrypted assertions are
     * included.
     *
     * @throws Exception
     */
    @BugNumber(9096)
    @Test(expected = AssertionStatusException.class)
    public void testSAML2_0_IssuerRuleEnforcedWhenEncryptedAssertionIsPresent() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

        assertion.includeIssuer(false);
        assertion.setSamlStatusCode(SamlStatus.SAML2_SUCCESS.getValue());
        final String encryptedToken = "encryptedAssertion";
        assertion.setEncryptedAssertions("${"+encryptedToken+"}");
        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();

        final Message encryptedMessage = new Message(XmlUtil.parse(v2_EncryptedAssertion));
        context.setVariable(encryptedToken, encryptedMessage);
        serverAssertion.checkRequest(context);
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

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

        assertion.includeIssuer(true);
        assertion.setSamlStatusCode(SamlStatus.SAML2_SUCCESS.getValue());
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

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());
        assertion.setSamlStatusCode(SamlStatus.SAML2_AUTHN_FAILED.getValue());

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

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

        assertion.includeIssuer(false);
        assertion.setSamlStatusCode(SamlStatus.SAML2_AUTHN_FAILED.getValue());
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

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

        assertion.includeIssuer(true);
        assertion.setSamlStatusCode(SamlStatus.SAML2_AUTHN_FAILED.getValue());

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

        final Document documentReadOnly = output.getXmlKnob().getDocumentReadOnly();
        System.out.println(XmlUtil.nodeToFormattedString(documentReadOnly));
        final Node responseElement = documentReadOnly.getFirstChild();

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
     * Test Custom Issuer assertion config value is added correctly to the samlp:Response.
     */
    @Test
    @BugNumber(11079)
    public void testSaml2_0_CustomIssuer() throws Exception {
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "${outputVar}";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

        assertion.includeIssuer(true);
        final String issuerVar = "issuerVar";
        assertion.setCustomIssuerValue("${" + issuerVar + "}");

        assertion.setSamlStatusCode(SamlStatus.SAML2_AUTHN_FAILED.getValue());

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        //yyyy-MM-ddTHH:mm:ssZ
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final String customIssuerValue = "Custom Issuer";
        context.setVariable(issuerVar, customIssuerValue);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);

        final Document documentReadOnly = output.getXmlKnob().getDocumentReadOnly();
        System.out.println(XmlUtil.nodeToFormattedString(documentReadOnly));
        final JAXBElement<saml.v2.protocol.ResponseType> typeJAXBElement = v2Unmarshaller.unmarshal(documentReadOnly, saml.v2.protocol.ResponseType.class);
        final saml.v2.protocol.ResponseType responseType = typeJAXBElement.getValue();

        final NameIDType nameIDType = responseType.getIssuer();
        Assert.assertEquals("Invalid custom Issuer value found", customIssuerValue, nameIDType.getValue());
    }

    /**
     * Validate the Format attribute is supported. Also validates the Qualifier.
     */
    @Test
    @BugNumber(11079)
    public void testSaml2_0_IssuerFormatAttribute() throws Exception {
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "${outputVar}";
        assertion.setOtherTargetMessageVariable(outputVar);
        assertion.setVersion(SamlVersion.SAML2.getVersionInt());
        assertion.setValidateWebSsoRules(false);

        assertion.includeIssuer(true);
        final String issuerVar = "issuerVar";
        assertion.setCustomIssuerValue("${" + issuerVar + "}");
        final String customIssuerFormat = "http://custom.uri";
        assertion.setCustomIssuerFormat(customIssuerFormat);
        final String qualifier = "qualifier";
        assertion.setCustomIssuerNameQualifier("${" + qualifier + "}");

        assertion.setSamlStatusCode(SamlStatus.SAML2_SUCCESS.getValue());

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        //yyyy-MM-ddTHH:mm:ssZ
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        final String customIssuerValue = "Custom Issuer";
        final String qualifierValue = "Qualifier Value";
        context.setVariable(issuerVar, customIssuerValue);
        context.setVariable(qualifier, qualifierValue);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);

        final Document documentReadOnly = output.getXmlKnob().getDocumentReadOnly();
        System.out.println(XmlUtil.nodeToFormattedString(documentReadOnly));
        final JAXBElement<saml.v2.protocol.ResponseType> typeJAXBElement = v2Unmarshaller.unmarshal(documentReadOnly, saml.v2.protocol.ResponseType.class);
        final saml.v2.protocol.ResponseType responseType = typeJAXBElement.getValue();

        final NameIDType nameIDType = responseType.getIssuer();
        Assert.assertEquals("Invalid custom Issuer value found", customIssuerValue, nameIDType.getValue());
        Assert.assertEquals("Invalid custom Issuer Format value found", customIssuerFormat, nameIDType.getFormat());
        Assert.assertEquals("Invalid custom Issuer Qualifier value found", qualifierValue, nameIDType.getNameQualifier());
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

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

        assertion.includeIssuer(false);

        final String token = "samlToken";
        assertion.setResponseAssertions("${" + token + "}");

        assertion.setSamlStatusCode(SamlStatus.SAML2_SUCCESS.getValue());

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        //yyyy-MM-ddTHH:mm:ssZ
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        final String extensions = "extensions";
        assertion.setResponseExtensions("${" + extensions + "}");

        ServerSamlpResponseBuilderAssertion serverAssertion;
        try {
            SyspropUtil.setProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile", "false" );
            serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile" );
        }

        final PolicyEnforcementContext context = getContext();
        final String extensionXml = "<extension>im an extension element</extension>";
        context.setVariable(extensions, new Message(XmlUtil.parse(extensionXml)));
        context.setVariable(token, new Message(XmlUtil.parse(samlToken_2_0)));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable(outputVar);
        final Document documentReadOnly = output.getXmlKnob().getDocumentReadOnly();
        System.out.println(XmlUtil.nodeToFormattedString(documentReadOnly));
        final Node responseElement = documentReadOnly.getFirstChild();
        
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

        assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());

        assertion.includeIssuer(true);
        assertion.setSamlStatusCode(SamlStatus.SAML_REQUEST_DENIED.getValue());

        final String responseId = "Response_" + HexUtils.generateRandomHexId(16);
        assertion.setResponseId(responseId);
        final String issueInstant = "2010-08-11T17:13:02Z";
        //yyyy-MM-ddTHH:mm:ssZ
        assertion.setIssueInstant(issueInstant);

        final String requestId = "RequestId-dahkcbfkifieemhlmpmhiocldceihfeoeajkdook";
        assertion.setInResponseTo(requestId);

        ServerSamlpResponseBuilderAssertion serverAssertion;
        try {
            SyspropUtil.setProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile", "false" );
            serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile" );
        }

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

        assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());
        assertion.setRecipient("http//recipient.com");

        final String token = "samlToken";
        assertion.setResponseAssertions("${" + token + "}");

        assertion.setSamlStatusCode(SamlStatus.SAML_SUCCESS.getValue());

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

        assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());
        assertion.setRecipient("http://recipient.com");

        assertion.setSamlStatusCode(SamlStatus.SAML_REQUEST_DENIED.getValue());
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

        ServerSamlpResponseBuilderAssertion serverAssertion;
        try {
            SyspropUtil.setProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile", "false" );
            serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.external.assertions.samlpassertion.validateSSOProfile" );
        }

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

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

        assertion.includeIssuer(false);
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

        assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());

        assertion.includeIssuer(false);
        assertion.setSamlStatusCode(SamlStatus.SAML_SUCCESS.getValue());
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

        assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());
        assertion.setSamlStatusCode(SamlStatus.SAML_SUCCESS.getValue());
        assertion.setStatusMessage("Status Message is ok");
        assertion.setRecipient("http://recipient.com");

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
     * Validates that Element type variables are supported and that index multi valued variables are also supported
     *
     * @throws Exception
     */
    @BugNumber(9077)
    @Test
    public void testSAML1_1_ElementAndIndexVariableSupport() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(true);
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("outputVar");

        assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());
        assertion.setSamlStatusCode(SamlStatus.SAML_SUCCESS.getValue());
        assertion.setStatusMessage("Status Message is ok");
        assertion.setRecipient("http://recipient.com");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken[0]} ${attributeToken}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final Message msg = new Message(XmlUtil.parse(samlToken_1_1));
        final Element element = msg.getXmlKnob().getDocumentReadOnly().getDocumentElement();
        List list = new ArrayList();
        list.add(element);
        context.setVariable("samlToken", list);
        context.setVariable("attributeToken", new Message(XmlUtil.parse(v1_1AttributeAssertion)));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message output = (Message) context.getVariable("outputVar");
        final JAXBElement<saml.v1.protocol.ResponseType> typeJAXBElement = v1Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v1.protocol.ResponseType.class);

        final saml.v1.protocol.ResponseType responseType = typeJAXBElement.getValue();

        final saml.v1.assertion.AssertionType assertionType = responseType.getAssertion().get(0);
        Assert.assertNotNull(assertionType);

        final ElementCursor cursor = new DomElementCursor(output.getXmlKnob().getDocumentReadOnly());
        final HashMap<String, String> map = getNamespaces();

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression("/samlp:Response/saml:Assertion", map).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        failIfXmlNotEqual(samlToken_1_1, xpathResultSetIterator.nextElementAsCursor().asDomElement(), "Incorrect Assertion found");
        failIfXmlNotEqual(v1_1AttributeAssertion, xpathResultSetIterator.nextElementAsCursor().asDomElement(), "Incorrect Assertion found");

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

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());
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

        assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());
        assertion.setStatusMessage("Status Message is ok");

        final PolicyEnforcementContext context = getContext();

        assertion.setResponseAssertions("${samlToken}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        context.setVariable("samlToken", new Message(XmlUtil.parse(samlToken_2_0)));
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be SERVER_ERROR", AssertionStatus.SERVER_ERROR, status);
    }

    @Test(expected = AssertionStatusException.class)
    public void testAssertionFieldMustBeConfigured() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

        assertion.includeIssuer(false);
        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();
        serverAssertion.checkRequest(context);
    }

    @Test
    public void testSaml2_0_AssertionNotFoundAtRuntime() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);

        assertion.setVersion(SamlVersion.SAML2.getVersionInt());

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

        assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());

        final String token = "samlToken";
        assertion.setResponseAssertions("${" + token + "}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext();

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be server error", AssertionStatus.SERVER_ERROR, status);
    }

    @BugNumber(11076)
    @Test
    public void testStatusCodeSupportsVariables() throws Exception{
        {
            // SAML 1.1
            final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
            SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
            assertion.setSignResponse(false);
            assertion.setTarget(TargetMessageType.OTHER);
            final String outputVar = "outputVar";
            assertion.setOtherTargetMessageVariable(outputVar);
            assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());
            assertion.setValidateWebSsoRules(false);

            assertion.setSamlStatusCode("${var}");

            ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

            final PolicyEnforcementContext context = getContext();
            context.setVariable("var", SamlStatus.SAML_SUCCESS.getValue());

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

            final Message output = (Message) context.getVariable("outputVar");
            final JAXBElement<saml.v1.protocol.ResponseType> typeJAXBElement = v1Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), saml.v1.protocol.ResponseType.class);

            final saml.v1.protocol.ResponseType responseType = typeJAXBElement.getValue();
            final QName value = responseType.getStatus().getStatusCode().getValue();
            Assert.assertEquals("Wrong value added for Status Code", SamlStatus.SAML_SUCCESS.getValue(), value.getLocalPart());
        }

        {
            // SAML 2.0
            final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
            SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
            assertion.setSignResponse(false);
            assertion.setTarget(TargetMessageType.OTHER);
            final String outputVar = "outputVar";
            assertion.setOtherTargetMessageVariable(outputVar);
            assertion.setVersion(SamlVersion.SAML2.getVersionInt());
            assertion.setValidateWebSsoRules(false);

            assertion.setSamlStatusCode("${var}");

            ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);

            final PolicyEnforcementContext context = getContext();
            context.setVariable("var", SamlStatus.SAML2_SUCCESS.getValue());

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

            final Message output = (Message) context.getVariable("outputVar");
            final JAXBElement<ResponseType> typeJAXBElement = v2Unmarshaller.unmarshal(output.getXmlKnob().getDocumentReadOnly(), ResponseType.class);

            final ResponseType responseType = typeJAXBElement.getValue();
            final String value = responseType.getStatus().getStatusCode().getValue();
            Assert.assertEquals("Wrong value added for Status Code", SamlStatus.SAML2_SUCCESS.getValue(), value);
        }
    }

    @Test
    public void testStatusCodeSupportsVariables_UnknownValue() throws Exception{
        {
            // SAML 1.1
            final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
            SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
            assertion.setSignResponse(false);
            assertion.setTarget(TargetMessageType.OTHER);
            final String outputVar = "outputVar";
            assertion.setOtherTargetMessageVariable(outputVar);
            assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());
            assertion.setSamlStatusCode(SamlStatus.SAML_SUCCESS.getValue());
            assertion.setValidateWebSsoRules(false);

            assertion.setSamlStatusCode("${var}%");

            ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);
            final TestAudit testAudit = new TestAudit();
            ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                    .put("auditFactory", testAudit.factory())
                    .unmodifiableMap()
            );

            final PolicyEnforcementContext context = getContext();
            context.setVariable("var", SamlStatus.SAML_SUCCESS.getValue());

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be SERVER ERROR", AssertionStatus.SERVER_ERROR, status);

            for (String s : testAudit) {
                System.out.println(s);
            }

            Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC));
            Assert.assertTrue(testAudit.isAuditPresentContaining("Unknown SAML 1.1 status code value:"));
        }

        {
            // SAML 2.0
            final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
            SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
            assertion.setSignResponse(false);
            assertion.setTarget(TargetMessageType.OTHER);
            final String outputVar = "outputVar";
            assertion.setOtherTargetMessageVariable(outputVar);
            assertion.setVersion(SamlVersion.SAML2.getVersionInt());
            assertion.setValidateWebSsoRules(false);

            assertion.setSamlStatusCode("${var}%");

            ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);
            final TestAudit testAudit = new TestAudit();
            ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                    .put("auditFactory", testAudit.factory())
                    .unmodifiableMap()
            );

            final PolicyEnforcementContext context = getContext();
            context.setVariable("var", SamlStatus.SAML2_SUCCESS.getValue());

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be SERVER ERROR", AssertionStatus.SERVER_ERROR, status);

            for (String s : testAudit) {
                System.out.println(s);
            }

            Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC));
            Assert.assertTrue(testAudit.isAuditPresentContaining("Unknown SAML 2.0 status code value:"));
        }
    }

    @BugNumber(11771)
    @Test
    public void testStatusCodeSupportsVariables_UnknownCode_Empty() throws Exception{
        {
            // SAML 1.1
            final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
            SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
            assertion.setSignResponse(false);
            assertion.setTarget(TargetMessageType.OTHER);
            final String outputVar = "outputVar";
            assertion.setOtherTargetMessageVariable(outputVar);
            assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());
            assertion.setSamlStatusCode(SamlStatus.SAML_SUCCESS.getValue());
            assertion.setValidateWebSsoRules(false);

            assertion.setSamlStatusCode("");

            ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);
            final TestAudit testAudit = new TestAudit();
            ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                    .put("auditFactory", testAudit.factory())
                    .unmodifiableMap()
            );

            final PolicyEnforcementContext context = getContext();

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be SERVER ERROR", AssertionStatus.SERVER_ERROR, status);

            for (String s : testAudit) {
                System.out.println(s);
            }

            Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC));
            Assert.assertTrue(testAudit.isAuditPresentContaining("Unknown SAML 1.1 status code value:"));
        }

        {
            // SAML 2.0
            final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
            SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
            assertion.setSignResponse(false);
            assertion.setTarget(TargetMessageType.OTHER);
            final String outputVar = "outputVar";
            assertion.setOtherTargetMessageVariable(outputVar);
            assertion.setVersion(SamlVersion.SAML2.getVersionInt());
            assertion.setValidateWebSsoRules(false);

            assertion.setSamlStatusCode("");

            ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);
            final TestAudit testAudit = new TestAudit();
            ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                    .put("auditFactory", testAudit.factory())
                    .unmodifiableMap()
            );

            final PolicyEnforcementContext context = getContext();

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be SERVER ERROR", AssertionStatus.SERVER_ERROR, status);

            for (String s : testAudit) {
                System.out.println(s);
            }

            Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.SAMLP_RESPONSE_BUILDER_GENERIC));
            Assert.assertTrue(testAudit.isAuditPresentContaining("Unknown SAML 2.0 status code value:"));
        }
    }

    /**
     * If a variable was used to supply the status code, it's value must be used when determining if an assertion is
     * required when validating Web SSO rules.
     *
     */
    @BugNumber(11907)
    @Test
    public void testStatusCodeSupportsVariables_ValidatesWebSsoCorrectly() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.OTHER);
        final String outputVar = "outputVar";
        assertion.setOtherTargetMessageVariable(outputVar);
        assertion.setVersion(SamlVersion.SAML2.getVersionInt());
        // this means an assertion is required with success
        assertion.setValidateWebSsoRules(true);

        assertion.setSamlStatusCode("${statusVar}");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        final PolicyEnforcementContext context = getContext();
        context.setVariable("statusVar", SamlStatus.SAML2_SUCCESS.getValue());

        try {
            serverAssertion.checkRequest(context);
            Assert.fail("Assertion should have thrown");
        } catch (AssertionStatusException e) {
            // pass
        }

        for (String s : testAudit) {
            System.out.println(s);
        }

        Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.SAMLP_PROCREQ_PROFILE_VIOLATION));
        Assert.assertTrue(testAudit.isAuditPresentContaining("Assertion(s) and / or EncryptedAssertion(s) are not configured. One ore more assertions are required when Response represents Success"));
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

    private final static String samlToken_2_0 = "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"SamlAssertion-9190940a89f8a5fa9837642a35d01f9a\" IssueInstant=\"2010-08-04T16:54:02.494Z\" Version=\"2.0\">\n" +
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

    private final static String v2_AttributeAssertion = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-657be8ad16685a0cc13c2c3966d00358\" IssueInstant=\"2010-08-13T18:41:15.906Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer>irishman.l7tech.com</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Recipient=\"https://cs2.salesforce.com\" NotOnOrAfter=\"2010-08-13T18:46:15.908Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2010-08-13T18:36:15.000Z\" NotOnOrAfter=\"2010-08-13T18:46:15.907Z\"><saml2:AudienceRestriction><saml2:Audience>https://saml.salesforce.com</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AttributeStatement><saml2:Attribute Name=\"ssostartpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue>http://irishman:8080/salesforce_saml2</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>";

    private final static String v1_1AttributeAssertion = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\" AssertionID=\"SamlAssertion-2f95ff7d175a00aeb11fd3c884ef40d0\" Issuer=\"irishman.l7tech.com\" IssueInstant=\"2010-08-13T20:03:07.445Z\"><saml:Conditions NotBefore=\"2010-08-13T19:58:07.000Z\" NotOnOrAfter=\"2010-08-13T20:08:07.446Z\"><saml:AudienceRestrictionCondition><saml:Audience>https://saml.salesforce.com</saml:Audience></saml:AudienceRestrictionCondition></saml:Conditions><saml:AttributeStatement><saml:Subject><saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml:NameIdentifier><saml:SubjectConfirmation><saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:bearer</saml:ConfirmationMethod></saml:SubjectConfirmation></saml:Subject><saml:Attribute AttributeName=\"test\" AttributeNamespace=\"\"><saml:AttributeValue>testvalue</saml:AttributeValue></saml:Attribute></saml:AttributeStatement></saml:Assertion>";

    private final static String v2_EncryptedAssertion = "<xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\"/><ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\"><xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/><ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:X509Data><ds:X509IssuerSerial><ds:X509IssuerName>OU=www.verisign.com/CPS Incorp.by Ref. LIABILITY LTD.(c)97 VeriSign, OU=VeriSign International Server CA - Class 3, OU=\"VeriSign, Inc.\", O=VeriSign Trust Network</ds:X509IssuerName><ds:X509SerialNumber>160479753879882120110470297434291570115</ds:X509SerialNumber></ds:X509IssuerSerial></ds:X509Data></ds:KeyInfo><xenc:CipherData><xenc:CipherValue>HvTpDcuk42MmouMpo1ZUJDSE0UrD8kqLdN58qwGWAnoPquEGYSXumJ2kKblA72EEHEEZJihb/qCfpaU1ZALJixI/PJn2COsK092/s1wnsMyay7SmrJ6MD+eCDCOYWWgiYIyhnSL9KHuF7ULXOw8xX1XAXUJFeAJpB/LtpekgNq8=</xenc:CipherValue></xenc:CipherData></xenc:EncryptedKey></ds:KeyInfo><xenc:CipherData><xenc:CipherValue>RIfrijQJDgu8GtRV3yTo7mNYZlheoMzTbXtWRhqvpPuJQurTzDy9dkBkhvgtDrPwXtoPpmw0gmHrYRfNmli9q8h7M+CDZEEVdyRvynbZh7Z2zuoL8op8adzgt9JoaW79xjVRZj0zkZ/S0h/G/wPLEOk05k3l+3Umik/3XHqCEpmabm5FKSx9qq/deYzElppFRokxumtPMuNHvwZhdQvKeDuArqA2LDq/U5kijRh7KkotibKwxBR0zbNNk3kLKadCfmHdk3x0kn8vbD4Kpvm4pVvySPtBoVty73UFgpSyirkHRzOkTihCNBJpAKaUcRyJ9d2BbHGLdcf8caRCNdXWntynBm4D5mHFteVWIolUwdCewhh9ZE1wcL5fcNFSSzi30sNQknP8f57Lt8lM0ikqTdsRZc2Hh2MVIsCkZrDuT2m3TtCoUOlTjtnEP3Of0HJbYAWOGfsmbSjlXMTEXTrX4SxPRUfvISMY5RQQKbg4G8AfbbGGK031d1REzLEqHG6zdKvc2Zb0zhstqHeO7ZIpl+rKR+/7/Pe1YtwYIsYEvsMbBJ+NqZZbC1CytjBLkp2n80ZP09230XGpDHK0XCXmKAQpBbay7BT7ULHTUGasovpKKH4ytIA0tvLJ2yfh5Z359zId3VxZZlxxRcJmJBPuApYkvbq5sVchJVHMCfQTKeRn5FYrQu6GaQp+edA1ni63I/lsPNAR7BBqBtmealk/LpROUWKMGbpRagJXUWkTi2i6nfs0hVOQP7lLHkV1SJZeBRMSixJatuNTQ1diqQQJgUfr4XFGvnnFRxqnSBuCphsAYqi5rTqfKLTVteWtdRL+smlD+l+lMcDkHzMtzHO3QyIiNknrPzKsAAbJjUf82vHOMorY5TbkddZ/2BPyBfr4y/IzTWYxHAuwHPsi6u23x4qWJOidBxDfRzfSm5K6op2i00D4rEPpALEHKWJj7aoizv4Tmd4YeUO49K0q6A6RzqP9H77hYvG+nWvc9Kodg/JNB91dEmyJMso+07WaGJYnynzwtqpeDxf+rtNO6SeDoqxtVACoNMj0SaOgdYPl4lDmEqBrXh+HE78SRZ2oSt3ATttkyQ9EoLsHMKcjprhqpUIyI/ipdwx2HH0sNLFjrwfFzhfhANMI/PqBhbZ2C4/bWANhflguB62O+RxbeFW8JQcOdot9lXRAE3KrGuullS6SytYcFwnvt/ysUktQ+ZUEmO2ZBJvJLQaNdT41R6DPjKu0rdKtNRhC6IwK88s0K0/mUYc9I6ixOX0FrgxBSBZ75U1NABXGeJPtzV5XHUNMgTdajqvJINiNNe6cqc3TiPxkuSBY0DBe3c4ZMkT4OCg72PuZ62dnoZ9GU3nu9SIEcYv5JFRdy3YuJWP3wIteFBLil8HBibbxCmf8uHiUZxXad9AvOYLj0G5TFZ0hRUlN4Vb6dOF8c325j9+0VtxNXRbHepDVQBQYdyXh6FuA4ajGOUBVqJjZdhCPgVHACnWT7z6xRewa/g3FPfJxmOX+Vl9OopzJGY77z1GzWK/vyvUtKfCWM0ea8Z/U9byOJA6pzg7UUgX5lMYAhkg2Inr8OegNSAHhe9EFrWolf3THVhNPAaOYAF3wNX3eOaHzpvGyYdhTVZXYwfybJfsBrI82bhZkx5TZdXMa8eFK3cuhglZIh/xXVaxfDto5/l6UWPiqhfhum/ewdCYEv9TK2tB2i4pCndy87KTG/sDVn79MV6StujSX80WGToz3cOOlmPx3ItuoMZiRV8Gdu7P5zj2C9sEuNYulQMpghlvrFvH4Ms5V7qMECloUKWKpTr2XcV6w2hyY5eLjo/w9Y4G3roSGbJF4iSXzuGdN6/ocFijz7C30FV/5tATiLgahcPOtuUYpE39hmtknI/1hZGYzxUiPQkOe/9EZoU1j+Z9pswAxC/loQgm89T/UOmvyiE2CJBJXAw==</xenc:CipherValue></xenc:CipherData></xenc:EncryptedData>";

    private final static String v2_AttributeDifferentSubject = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-1e62b2db80e83e01d20269fc7626d80d\" IssueInstant=\"2010-08-19T21:32:47.565Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer>irishman.l7tech.com</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">DifferentSubject</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Recipient=\"https://cs2.salesforce.com\" NotOnOrAfter=\"2010-08-19T21:37:47.574Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2010-08-19T21:27:47.000Z\" NotOnOrAfter=\"2010-08-19T21:37:47.573Z\"><saml2:AudienceRestriction><saml2:Audience>https://saml.salesforce.com</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AttributeStatement><saml2:Attribute Name=\"ssostartpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue>http://irishman:8080/salesforce_saml2</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>";

    private final static String v2_NoBearerAssertion = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-28a9a13e5f591d095dabb382aa904ed0\" IssueInstant=\"2010-08-19T21:45:37.464Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer>irishman.l7tech.com</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:sender-vouches\"><saml2:NameID>CN=irishman.l7tech.com</saml2:NameID><saml2:SubjectConfirmationData Recipient=\"https://cs2.salesforce.com\" NotOnOrAfter=\"2010-08-19T21:50:37.466Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2010-08-19T21:40:37.000Z\" NotOnOrAfter=\"2010-08-19T21:50:37.465Z\"><saml2:AudienceRestriction><saml2:Audience>https://saml.salesforce.com</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AttributeStatement><saml2:Attribute Name=\"ssostartpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue>http://irishman:8080/salesforce_saml2</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement><saml2:AuthnStatement AuthnInstant=\"2010-08-19T21:45:37.463Z\"><saml2:SubjectLocality Address=\"10.7.48.153\"/><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement></saml2:Assertion>";

    private final static String v2_NoRecipientAssertion = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-67d7c95b5f9190914d7bbc9e37a18c72\" IssueInstant=\"2010-08-19T22:05:09.103Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer>irishman.l7tech.com</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData NotOnOrAfter=\"2010-08-19T22:10:09.473Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2010-08-19T22:00:09.000Z\" NotOnOrAfter=\"2010-08-19T22:10:09.459Z\"><saml2:AudienceRestriction><saml2:Audience>https://saml.salesforce.com</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AttributeStatement><saml2:Attribute Name=\"ssostartpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue>http://irishman:8080/salesforce_saml2</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement><saml2:AuthnStatement AuthnInstant=\"2010-08-19T22:05:09.103Z\"><saml2:SubjectLocality Address=\"10.7.48.153\"/><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement></saml2:Assertion>";

    private final static String v2_NoNotOnorAfterAssertion = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-14350d15192ed351934eb883b203b490\" IssueInstant=\"2010-08-19T22:07:18.925Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer>irishman.l7tech.com</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Recipient=\"https://cs2.salesforce.com\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2010-08-19T22:02:18.000Z\" NotOnOrAfter=\"2010-08-19T22:12:18.927Z\"><saml2:AudienceRestriction><saml2:Audience>https://saml.salesforce.com</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AttributeStatement><saml2:Attribute Name=\"ssostartpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue>http://irishman:8080/salesforce_saml2</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement><saml2:AuthnStatement AuthnInstant=\"2010-08-19T22:07:18.925Z\"><saml2:SubjectLocality Address=\"10.7.48.153\"/><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement></saml2:Assertion>";

    private final static String v2_WithNotBeforeAssertion = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-e67d73b85c20e36d99fea3ca1a1167d8\" IssueInstant=\"2010-08-19T22:10:22.911Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer>irishman.l7tech.com</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Recipient=\"https://cs2.salesforce.com\" NotBefore=\"2010-08-19T22:08:22.000Z\" NotOnOrAfter=\"2010-08-19T22:15:22.919Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2010-08-19T22:05:22.000Z\" NotOnOrAfter=\"2010-08-19T22:15:22.916Z\"><saml2:AudienceRestriction><saml2:Audience>https://saml.salesforce.com</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AttributeStatement><saml2:Attribute Name=\"ssostartpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue>http://irishman:8080/salesforce_saml2</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement><saml2:AuthnStatement AuthnInstant=\"2010-08-19T22:10:22.910Z\"><saml2:SubjectLocality Address=\"10.7.48.153\"/><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement></saml2:Assertion>";

    private final static String v2_NoAuthenticationStatement = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-f49ce894463964a4b2f062badc809d7c\" IssueInstant=\"2010-08-19T22:27:23.204Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer>irishman.l7tech.com</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Recipient=\"https://cs2.salesforce.com\" NotBefore=\"2010-08-19T22:25:23.000Z\" NotOnOrAfter=\"2010-08-19T22:32:23.462Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2010-08-19T22:22:23.000Z\" NotOnOrAfter=\"2010-08-19T22:32:23.450Z\"><saml2:AudienceRestriction><saml2:Audience>https://saml.salesforce.com</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AttributeStatement><saml2:Attribute Name=\"ssostartpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue>http://irishman:8080/salesforce_saml2</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>";

    private final static String v2_NoAudienceRestriction = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-9b4a23f512a4883a1fd3e2c6fbd7e693\" IssueInstant=\"2010-08-19T22:31:08.272Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer>irishman.l7tech.com</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Recipient=\"https://cs2.salesforce.com\" NotBefore=\"2010-08-19T22:29:08.000Z\" NotOnOrAfter=\"2010-08-19T22:36:08.277Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2010-08-19T22:26:08.000Z\" NotOnOrAfter=\"2010-08-19T22:36:08.276Z\"><saml2:AudienceRestriction><saml2:Audience/></saml2:AudienceRestriction></saml2:Conditions><saml2:AttributeStatement><saml2:Attribute Name=\"ssostartpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue>http://irishman:8080/salesforce_saml2</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement></saml2:Assertion>";

    private final static String v2_DifferentIssuer = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-cf0c5912e98790bcbe392759d8fc89e5\" IssueInstant=\"2010-08-19T22:39:44.876Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:Issuer>donaltest.l7tech.com</saml2:Issuer><saml2:Subject><saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml2:NameID><saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml2:SubjectConfirmationData Recipient=\"https://cs2.salesforce.com\" NotOnOrAfter=\"2010-08-19T22:44:44.879Z\"/></saml2:SubjectConfirmation></saml2:Subject><saml2:Conditions NotBefore=\"2010-08-19T22:34:44.000Z\" NotOnOrAfter=\"2010-08-19T22:44:44.878Z\"><saml2:AudienceRestriction><saml2:Audience>https://saml.salesforce.com</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions><saml2:AttributeStatement><saml2:Attribute Name=\"ssostartpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\"><saml2:AttributeValue>http://irishman:8080/salesforce_saml2</saml2:AttributeValue></saml2:Attribute></saml2:AttributeStatement><saml2:AuthnStatement AuthnInstant=\"2010-08-19T22:39:44.876Z\"><saml2:SubjectLocality Address=\"10.7.48.153\"/><saml2:AuthnContext><saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement></saml2:Assertion>";

    private final static String v2_IssuerWrongFormat = "  <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"SamlAssertion-9190940a89f8a5fa9837642a35d01f9a\" IssueInstant=\"2010-08-04T16:54:02.494Z\" Version=\"2.0\">\n" +
            "    <saml2:Issuer Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\">irishman.l7tech.com</saml2:Issuer>\n" +
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

    private final static String v1_attributeOnlySenderVouches = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\" AssertionID=\"SamlAssertion-02e04fe2cc5399162184dd5e164f9754\" Issuer=\"irishman.l7tech.com\" IssueInstant=\"2010-08-20T17:03:57.202Z\"><saml:Conditions NotBefore=\"2010-08-20T16:58:57.000Z\" NotOnOrAfter=\"2010-08-20T17:08:57.205Z\"><saml:AudienceRestrictionCondition><saml:Audience>https://saml.salesforce.com</saml:Audience></saml:AudienceRestrictionCondition></saml:Conditions><saml:AttributeStatement><saml:Subject><saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml:NameIdentifier><saml:SubjectConfirmation><saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:sender-vouches</saml:ConfirmationMethod></saml:SubjectConfirmation></saml:Subject><saml:Attribute AttributeName=\"testattribute\" AttributeNamespace=\"\"><saml:AttributeValue>testvalue</saml:AttributeValue></saml:Attribute></saml:AttributeStatement></saml:Assertion>";

    private final static String v1_attributeOnlyNoConditions = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\" AssertionID=\"SamlAssertion-aff1bc9c20d57e1cfdd1b797b194b735\" Issuer=\"irishman.l7tech.com\" IssueInstant=\"2010-08-20T17:11:49.230Z\"><saml:Conditions NotBefore=\"2010-08-20T17:09:49.000Z\" NotOnOrAfter=\"2010-08-20T17:16:49.231Z\"><saml:AudienceRestrictionCondition><saml:Audience>https://saml.salesforce.com</saml:Audience></saml:AudienceRestrictionCondition></saml:Conditions><saml:AttributeStatement><saml:Subject><saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\" NameQualifier=\"\">jspies@layer7tech.com.sso</saml:NameIdentifier><saml:SubjectConfirmation><saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:bearer</saml:ConfirmationMethod></saml:SubjectConfirmation></saml:Subject><saml:Attribute AttributeName=\"testattribute\" AttributeNamespace=\"\"><saml:AttributeValue>testvalue</saml:AttributeValue></saml:Attribute></saml:AttributeStatement></saml:Assertion>";

    private final static String v1_missingNotBeforeCondition = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" AssertionID=\"SamlAssertion-94b99f0213cdf988f1931cd55aa91232\" IssueInstant=\"2010-07-12T23:30:29.274Z\" Issuer=\"irishman.l7tech.com\" MajorVersion=\"1\" MinorVersion=\"1\">\n" +
            "    <saml:Conditions NotOnOrAfter=\"2010-07-12T23:32:29.276Z\">\n" +
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

}
