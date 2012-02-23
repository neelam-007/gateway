package com.l7tech.external.assertions.samlissuer.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.samlissuer.SamlIssuerAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SamlElementGenericConfig;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.saml.SamlAssertionV1;
import com.l7tech.xml.saml.SamlAssertionV2;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.AssertionType;
import x0Assertion.oasisNamesTcSAML2.AttributeStatementType;
import x0Assertion.oasisNamesTcSAML2.AttributeType;

import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.AttributeValueAddBehavior.ADD_AS_XML;
import static com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.AttributeValueComparison.CANONICALIZE;
import static com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.EmptyBehavior.EMPTY_STRING;
import static com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.EmptyBehavior.EXISTS_NO_VALUE;
import static com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.EmptyBehavior.NULL_VALUE;
import static com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement.Attribute.VariableNotFoundBehavior.REPLACE_EXPRESSION_EMPTY_STRING;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class ServerSamlIssuerAssertionTest {

    /**
     * Ensure NotBefore is either after or has the same value as IssueInstant.
     *
     * Note: NotBefore does not need to be after IssueInstant, it simply needs to not reset the milli seconds value to zero.
     * Not going to test for this as the test could easily fail. Instead the assertion is configured to have a NotBefore
     * time of now, which should always be the same time as or after the IssueInstant time, which is not configurable
     * and is determined based on when the assertion is created.
     *
     * Note: if the bug fix was reverted this test would not always fail, but it would eventually.
     */
    @BugNumber(10263)
    @Test
    public void testNotBeforeMilliSeconds_Saml1() throws Exception {
        SamlIssuerAssertion assertion = new SamlIssuerAssertion();
        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        assertion.setAuthenticationStatement(authStmt);
        assertion.setVersion(1);
        assertion.setSignAssertion(false);//don't need
        assertion.setConditionsNotBeforeSecondsInPast(0);

        ServerSamlIssuerAssertion serverAssertion = new ServerSamlIssuerAssertion(assertion,
                ApplicationContexts.getTestApplicationContext());

        final PolicyEnforcementContext context = getContext();

        //set credentials
        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        final HttpBasicToken basicToken = new HttpBasicToken("testuser", new char[]{'p', 'a', 's', 's'});

        authContext.addCredentials(LoginCredentials.makeLoginCredentials(basicToken, HttpBasic.class));
        context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        serverAssertion.checkRequest(context);
        final String issuedSamlAssertion = (String) context.getVariable("issuedSamlAssertion");

        Message samlAssertion = new Message(XmlUtil.stringAsDocument(issuedSamlAssertion));
        final Document document = samlAssertion.getXmlKnob().getDocumentReadOnly();
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final String samlUri = "urn:oasis:names:tc:SAML:1.0:assertion";
        final Node assertionNode = document.getElementsByTagNameNS(samlUri, "Assertion").item(0);
        final Node issueInstant = assertionNode.getAttributes().getNamedItem("IssueInstant");
        System.out.println(issueInstant.getNodeValue() + " issue instant");

        final Calendar issueInstantCal = DatatypeConverter.parseDate(issueInstant.getNodeValue());

        final Node conditionsNode = document.getElementsByTagNameNS(samlUri, "Conditions").item(0);
        final String notBefore = conditionsNode.getAttributes().getNamedItem("NotBefore").getNodeValue();
        final Calendar notBeforeDate = DatatypeConverter.parseDate(notBefore);
        System.out.println(notBefore + " not before");
        assertTrue("Issue instant must be before or equal to notBeforeDate", issueInstantCal.before(notBeforeDate) || issueInstantCal.equals(notBeforeDate));

        //not part of bug, just adding sanity check
        final String notOnOrAfter = conditionsNode.getAttributes().getNamedItem("NotOnOrAfter").getNodeValue();
        final Calendar notOnOrAfterDate = DatatypeConverter.parseDate(notOnOrAfter);
        System.out.println(notOnOrAfter + " not on or after");
        assertTrue("Not on or after must be after not before date.", notBeforeDate.before(notOnOrAfterDate));
    }

    /**
     * Ensure NotBefore is either after or has the same value as IssueInstant. Validates NotBefore from the Conditions
     * element and the SubjectConfirmation element.
     *
     * See {@link com.l7tech.external.assertions.samlissuer.server.ServerSamlIssuerAssertionTest#testNotBeforeMilliSeconds_Saml1()}
     * javadoc notes.
     */
    @BugNumber(10263)
    @Test
    public void testNotBeforeMilliSeconds_Saml2() throws Exception {
        SamlIssuerAssertion assertion = new SamlIssuerAssertion();
        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        assertion.setAuthenticationStatement(authStmt);
        assertion.setVersion(2);
        assertion.setSignAssertion(false);//don't need
        assertion.setConditionsNotBeforeSecondsInPast(0);
        //also test the subject confirmation not before date
        assertion.setSubjectConfirmationMethodUri(SamlConstants.CONFIRMATION_BEARER);
        assertion.setSubjectConfirmationDataNotBeforeSecondsInPast(0);

        ServerSamlIssuerAssertion serverAssertion = new ServerSamlIssuerAssertion(assertion,
                ApplicationContexts.getTestApplicationContext());

        final PolicyEnforcementContext context = getContext();

        //set credentials
        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        final HttpBasicToken basicToken = new HttpBasicToken("testuser", new char[]{'p', 'a', 's', 's'});

        authContext.addCredentials(LoginCredentials.makeLoginCredentials(basicToken, HttpBasic.class));
        context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        serverAssertion.checkRequest(context);
        final String issuedSamlAssertion = (String) context.getVariable("issuedSamlAssertion");

        Message samlAssertion = new Message(XmlUtil.stringAsDocument(issuedSamlAssertion));
        final Document document = samlAssertion.getXmlKnob().getDocumentReadOnly();
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Node assertionNode = document.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Assertion").item(0);
        final Node issueInstant = assertionNode.getAttributes().getNamedItem("IssueInstant");
        System.out.println(issueInstant.getNodeValue() + " issue instant");

        final Calendar issueInstantCal = DatatypeConverter.parseDate(issueInstant.getNodeValue());

        final Node conditionsNode = document.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Conditions").item(0);
        final String notBefore = conditionsNode.getAttributes().getNamedItem("NotBefore").getNodeValue();
        final Calendar notBeforeDate = DatatypeConverter.parseDate(notBefore);
        System.out.println(notBefore + " not before");
        assertTrue("Issue instant must be before or equal to notBeforeDate", issueInstantCal.before(notBeforeDate) || issueInstantCal.equals(notBeforeDate));

        //not part of bug, just adding sanity check
        final String notOnOrAfter = conditionsNode.getAttributes().getNamedItem("NotOnOrAfter").getNodeValue();
        final Calendar notOnOrAfterDate = DatatypeConverter.parseDate(notOnOrAfter);
        System.out.println(notOnOrAfter + " not on or after");
        assertTrue("Not on or after must be after not before date.", notBeforeDate.before(notOnOrAfterDate));

        //test subject confirmation not before date
        final Node subjectConfirmationDataNode = document.getElementsByTagNameNS(SamlConstants.NS_SAML2, "SubjectConfirmationData").item(0);
        final String subConfNotBefore = subjectConfirmationDataNode.getAttributes().getNamedItem("NotBefore").getNodeValue();
        final Calendar subConfNotBeforeCal = DatatypeConverter.parseDate(subConfNotBefore);
        System.out.println(subConfNotBefore + " subject confirmation data not before");
        assertTrue("Subject confirmation data's not before should be after the issue instant.", issueInstantCal.before(subConfNotBeforeCal) || issueInstantCal.equals(subConfNotBeforeCal));
    }

    // Basic test coverage for existing support - Attributes are added to an AttributeStatement

    /**
     * Higher level test case for bug 10276 - validates the runtime behavior of audience variable value
     */
    @BugNumber(10276)
    @Test
    public void testMultipleAudienceElements_Version2() throws Exception {
        SamlIssuerAssertion assertion = new SamlIssuerAssertion();
        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        assertion.setAuthenticationStatement(authStmt);
        assertion.setVersion(2);
        assertion.setSignAssertion(false);//don't need
        //also test the subject confirmation not before date
        assertion.setSubjectConfirmationMethodUri(SamlConstants.CONFIRMATION_BEARER);

        assertion.setAudienceRestriction("Audience1 ${singleValued} \f audience2 ${multiValued} \n    \t   \r  ${singleVarManyUris} ");

        ServerSamlIssuerAssertion serverAssertion = new ServerSamlIssuerAssertion(assertion,
                ApplicationContexts.getTestApplicationContext());

        final PolicyEnforcementContext context = getContext();
        context.setVariable("singleValued", "singleAudience");
        context.setVariable("multiValued", Arrays.asList("multiAudience1", "multiAudience2 multiAudience3"));
        context.setVariable("singleVarManyUris", "many1 many2  \n   many3");

        //set credentials
        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        final HttpBasicToken basicToken = new HttpBasicToken("testuser", new char[]{'p', 'a', 's', 's'});

        authContext.addCredentials(LoginCredentials.makeLoginCredentials(basicToken, HttpBasic.class));
        context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        serverAssertion.checkRequest(context);
        final String issuedSamlAssertion = (String) context.getVariable("issuedSamlAssertion");

        Message samlAssertion = new Message(XmlUtil.stringAsDocument(issuedSamlAssertion));
        final Document document = samlAssertion.getXmlKnob().getDocumentReadOnly();
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Node audienceRestrictionNode = document.getElementsByTagNameNS(SamlConstants.NS_SAML2, "AudienceRestriction").item(0);
        Assert.assertEquals("Wrong number of Audience elements found", 9, audienceRestrictionNode.getChildNodes().getLength());

        final List<String> expectedValues = Arrays.asList("Audience1", "singleAudience", "audience2", "multiAudience1", "multiAudience2", "multiAudience3", "many1", "many2", "many3");
        for (int i = 0, expectedValuesSize = expectedValues.size(); i < expectedValuesSize; i++) {
            String expectedValue = expectedValues.get(i);
            Assert.assertEquals("Unexpected value found", expectedValue, DomUtils.getTextValue((Element) audienceRestrictionNode.getChildNodes().item(i)));
        }
    }

    /**
     * Higher level test case for bug 10276 - validates the runtime behavior of audience variable value
     */
    @BugNumber(10276)
    @Test
    public void testMultipleAudienceElements_Version1() throws Exception {
        SamlIssuerAssertion assertion = new SamlIssuerAssertion();
        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        assertion.setAuthenticationStatement(authStmt);
        assertion.setVersion(1);
        assertion.setSignAssertion(false);//don't need
        //also test the subject confirmation not before date
        assertion.setSubjectConfirmationMethodUri(SamlConstants.CONFIRMATION_BEARER);

        assertion.setAudienceRestriction("Audience1 ${singleValued} \f audience2 ${multiValued} \n    \t   \r  ${singleVarManyUris} ");

        ServerSamlIssuerAssertion serverAssertion = new ServerSamlIssuerAssertion(assertion,
                ApplicationContexts.getTestApplicationContext());

        final PolicyEnforcementContext context = getContext();
        context.setVariable("singleValued", "singleAudience");
        context.setVariable("multiValued", Arrays.asList("multiAudience1", "multiAudience2 multiAudience3"));
        context.setVariable("singleVarManyUris", "many1 many2  \n   many3");

        //set credentials
        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        final HttpBasicToken basicToken = new HttpBasicToken("testuser", new char[]{'p', 'a', 's', 's'});

        authContext.addCredentials(LoginCredentials.makeLoginCredentials(basicToken, HttpBasic.class));
        context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        serverAssertion.checkRequest(context);
        final String issuedSamlAssertion = (String) context.getVariable("issuedSamlAssertion");

        Message samlAssertion = new Message(XmlUtil.stringAsDocument(issuedSamlAssertion));
        final Document document = samlAssertion.getXmlKnob().getDocumentReadOnly();
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Node audienceRestrictionNode = document.getElementsByTagNameNS(SamlConstants.NS_SAML, "AudienceRestrictionCondition").item(0);
        Assert.assertEquals("Wrong number of Audience elements found", 9, audienceRestrictionNode.getChildNodes().getLength());

        final List<String> expectedValues = Arrays.asList("Audience1", "singleAudience", "audience2", "multiAudience1", "multiAudience2", "multiAudience3", "many1", "many2", "many3");
        for (int i = 0, expectedValuesSize = expectedValues.size(); i < expectedValuesSize; i++) {
            String expectedValue = expectedValues.get(i);
            Assert.assertEquals("Unexpected value found", expectedValue, DomUtils.getTextValue((Element) audienceRestrictionNode.getChildNodes().item(i)));
        }
    }

    /**
     * Ensure that invalid configuration of an AttributeValue is not allowed by the server assertion's constructor.
     * @throws Exception
     */
    @Test(expected = ServerPolicyException.class)
    public void testAttributeStatement_RepatIfMulti_Validation() throws Exception {
        // If true, and more than just one variable is referenced, then is multi is ignored and the values are concatenated.
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String multiVarName = "names";
        nameAttr.setValue("${" + multiVarName + "} just some text"); // invalid configuration
        nameAttr.setRepeatIfMulti(true);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        getServerAssertion(samlAttributeStatement, 2);
    }

    /**
     * Tests that an AttributeStatement with no Attributes can be created correctly.
     */
    @Test
    public void testAttributeStatement_AttributeValue_ConvertToString_NoRepeat_NoValue() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final PolicyEnforcementContext context = getContext();

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            //Verify AttributeStatement added with no Attributes
            final Node attributeStatement = document.getElementsByTagNameNS(SamlConstants.NS_SAML2, "AttributeStatement").item(0);
            Assert.assertEquals("No child Attribute elements (or any) expected.", 0, attributeStatement.getChildNodes().getLength());
        }
        {
            // V1
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);
            final PolicyEnforcementContext context = getContext();

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            //Verify AttributeStatement added with no Attributes
            final NodeList foundAttributes = document.getElementsByTagNameNS(SamlConstants.NS_SAML, "Attribute");
            Assert.assertEquals("No child Attribute elements (or any) expected.", 0, foundAttributes.getLength());
        }
    }

    /**
     * Tests attributes added correctly when convert to string and no repeat is configured. (This is the most normal case).
     */
    @Test
    public void testAttributeStatement_AttributeValue_ConvertToString_NoRepeat() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setValue("James");

        samlAttributeStatement.setAttributes(new SamlAttributeStatement.Attribute[]{nameAttr});

        {
            nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final PolicyEnforcementContext context = getContext();

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML2.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Only 1 attribute expected", 1, attributeTypesList.size());

            final AttributeType attributeType = attributeTypesList.get(0);
            Assert.assertEquals("Wrong name found", "nc:PersonGivenName", attributeType.getName());
            Assert.assertEquals("Wrong name format found", SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC, attributeType.getNameFormat());

            final XmlObject[] attributeValueArray = attributeType.getAttributeValueArray();
            Assert.assertEquals("Only 1 attribute value expected", 1, attributeValueArray.length);

            final XmlCursor xmlCursor = attributeValueArray[0].newCursor();
            Assert.assertEquals("Wrong attribute value found", "James", xmlCursor.getTextValue());
            xmlCursor.dispose();
        }
        {
            // V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);
            final PolicyEnforcementContext context = getContext();

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType)xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Only 1 attribute expected", 1, attributeTypesList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeType attributeType = attributeTypesList.get(0);
            Assert.assertEquals("Wrong name found", "nc:PersonGivenName", attributeType.getAttributeName());
            Assert.assertEquals("Wrong name format found", namespace, attributeType.getAttributeNamespace());

            final XmlObject[] attributeValueArray = attributeType.getAttributeValueArray();
            Assert.assertEquals("Only 1 attribute value expected", 1, attributeValueArray.length);

            final XmlCursor xmlCursor = attributeValueArray[0].newCursor();
            Assert.assertEquals("Wrong attribute value found", "James", xmlCursor.getTextValue());
            xmlCursor.dispose();
        }
    }

    /**
     * Validate that when repeat if multivalued is configured with a single multi valued variable reference that multiple
     * Attribute elements are created.
     */
    @Test
    public void testAttributeStatement_AttributeValue_ConvertToString_Repeat() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        final String multiVarName = "names";
        nameAttr.setValue("${" + multiVarName + "}");
        nameAttr.setRepeatIfMulti(true);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final PolicyEnforcementContext context = getContext();
        context.setVariable(multiVarName, Arrays.asList("James", "Kirk"));

        {
            //V2
            nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 2, attributeTypesList.size());

            validateAttributeV2(attributeTypesList.get(0), "nc:PersonGivenName", "James", 1);
            validateAttributeV2(attributeTypesList.get(1), "nc:PersonGivenName", "Kirk", 1);
        }

        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 2, attributeTypesList.size());

            validateAttributeV1(attributeTypesList.get(0), "nc:PersonGivenName", namespace, "James", 1);
            validateAttributeV1(attributeTypesList.get(1), "nc:PersonGivenName", namespace, "Kirk", 1);
        }
    }

    /**
     * Validate that when repeat if multivalued is not configured that all values are concatenated.
     */
    @Test
    public void testAttributeStatement_AttributeValue_ConvertToString_NoRepeat_Expression() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String multiVarName = "names";
        nameAttr.setValue("${" + multiVarName + "} just some text");
        nameAttr.setRepeatIfMulti(false);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        final PolicyEnforcementContext context = getContext();
        context.setVariable(multiVarName, Arrays.asList("James", "Kirk"));

        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            validateAttributeV2(attributeTypesList.get(0), "nc:PersonGivenName", "James, Kirk just some text", 1);
        }
        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            validateAttributeV1(attributeTypesList.get(0), "nc:PersonGivenName", namespace, "James, Kirk just some text", 1);
        }
    }

    /**
     * Tests empty AttributeValue when referenced variable does not exist.
     */
    @Test
    public void testAttributeStatement_AttributeValue_ConvertToString_Variable_Does_Not_Exist() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String multiVarName = "names";
        nameAttr.setValue("${" + multiVarName + "}");
        nameAttr.setRepeatIfMulti(false);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        final PolicyEnforcementContext context = getContext();

        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            validateAttributeV2(attributeTypesList.get(0), "nc:PersonGivenName", "", 1);
        }
        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            validateAttributeV1(attributeTypesList.get(0), "nc:PersonGivenName", namespace, "", 1);
        }
    }

    /**
     * Validate a Message variable is added correct to an AttributeValue.
     */
    @Test
    public void testAttributeStatement_AttributeValue_AddAsXML_No_Repeat_Message() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String multiVarName = "messageRef";
        nameAttr.setValue("${" + multiVarName + "}");
        nameAttr.setRepeatIfMulti(false);
        nameAttr.setAddBehavior(ADD_AS_XML);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        final PolicyEnforcementContext context = getContext();
        String customXml =
        "<custom:PersonName xmlns:custom=\"http://custom.com\" >" +
        "James" +
        "</custom:PersonName>";

        Message mVar = new Message(XmlUtil.parse(customXml));
        context.setVariable(multiVarName, mVar);

        context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());
            final AttributeType attributeType = attributeTypesList.get(0);
            final XmlObject attributeValue = attributeType.getAttributeValueArray(0);
            final Node valueNode = attributeValue.getDomNode();
            System.out.println(XmlUtil.nodeToFormattedString(valueNode));

            String expected = "<saml2:AttributeValue xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
                    "    <custom:PersonName xmlns:custom=\"http://custom.com\">James</custom:PersonName>\n" +
                    "</saml2:AttributeValue>";

            failIfXmlNotEqual(expected, (Element) valueNode, "Incorrect AttributeValue");
        }
        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeType attributeType = attributeTypesList.get(0);
            final XmlObject attributeValue = attributeType.getAttributeValueArray(0);
            final Node valueNode = attributeValue.getDomNode();
            System.out.println(XmlUtil.nodeToFormattedString(valueNode));

            String expected = "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\">\n" +
                    "    <custom:PersonName xmlns:custom=\"http://custom.com\">James</custom:PersonName>\n" +
                    "</saml:AttributeValue>";

            failIfXmlNotEqual(expected, (Element) valueNode, "Incorrect AttributeValue");
        }
    }

    /**
     * Validate mixed content added correctly to an AttributeValue. Also tests Element and String support.
     */
    @Test
    public void testAttributeStatement_AttributeValue_AddAsXML_No_Repeat_MixedContent() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String varNameMsg = "messageRef";
        final String varNameElm = "elementRef";
        nameAttr.setValue("${" + varNameMsg + "} this is some text inbetween elements ${" + varNameElm + "}");

        nameAttr.setRepeatIfMulti(false);
        nameAttr.setAddBehavior(ADD_AS_XML);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        final PolicyEnforcementContext context = getContext();

        String customXml =
        "<custom:PersonName xmlns:custom=\"http://custom.com\" >" +
        "James" +
        "</custom:PersonName>";

        Message mVar = new Message(XmlUtil.parse(customXml));
        context.setVariable(varNameMsg, mVar);

        mVar = new Message(XmlUtil.parse(customXml));
        context.setVariable(varNameElm, mVar.getXmlKnob().getDocumentReadOnly().getDocumentElement());

        context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());

            final AttributeType attributeType = attributeTypesList.get(0);
            final XmlObject attributeValue = attributeType.getAttributeValueArray(0);
            final Node valueNode = attributeValue.getDomNode();
            System.out.println(XmlUtil.nodeToFormattedString(valueNode));

            final Document emptyDoc = XmlUtil.createEmptyDocument("AttributeValue", "saml2", SamlConstants.NS_SAML2);
            final Element personName = XmlUtil.createAndAppendElementNS(emptyDoc.getDocumentElement(), "PersonName", "http://custom.com", "custom");
            final Text textNode = emptyDoc.createTextNode("James");
            personName.appendChild(textNode);
            emptyDoc.getDocumentElement().appendChild(personName);

            final Text textNode2 = emptyDoc.createTextNode(" this is some text inbetween elements ");
            emptyDoc.getDocumentElement().appendChild(textNode2);

            final Element personName1 = XmlUtil.createAndAppendElementNS(emptyDoc.getDocumentElement(), "PersonName", "http://custom.com", "custom");
            final Text textNode1 = emptyDoc.createTextNode("James");
            personName1.appendChild(textNode1);
            emptyDoc.getDocumentElement().appendChild(personName1);

            failIfNotEqual(emptyDoc.getDocumentElement(), (Element) valueNode, "Incorrect mixed context created.", false);
        }
        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());

            final x0Assertion.oasisNamesTcSAML1.AttributeType attributeType = attributeTypesList.get(0);
            final XmlObject attributeValue = attributeType.getAttributeValueArray(0);
            final Node valueNode = attributeValue.getDomNode();
            System.out.println(XmlUtil.nodeToFormattedString(valueNode));

            final Document emptyDoc = XmlUtil.createEmptyDocument("AttributeValue", "saml", SamlConstants.NS_SAML);
            final Element personName = XmlUtil.createAndAppendElementNS(emptyDoc.getDocumentElement(), "PersonName", "http://custom.com", "custom");
            final Text textNode = emptyDoc.createTextNode("James");
            personName.appendChild(textNode);
            emptyDoc.getDocumentElement().appendChild(personName);

            final Text textNode2 = emptyDoc.createTextNode(" this is some text inbetween elements ");
            emptyDoc.getDocumentElement().appendChild(textNode2);

            final Element personName1 = XmlUtil.createAndAppendElementNS(emptyDoc.getDocumentElement(), "PersonName", "http://custom.com", "custom");
            final Text textNode1 = emptyDoc.createTextNode("James");
            personName1.appendChild(textNode1);
            emptyDoc.getDocumentElement().appendChild(personName1);

            failIfNotEqual(emptyDoc.getDocumentElement(), (Element) valueNode, "Incorrect mixed context created.", false);
        }
    }

    /**
     * Validate repeat if multivalued for Add as XML.
     */
    @Test
    public void testAttributeStatement_AttributeValue_AddAsXML_Repeat() throws Exception {
        //Test Multi with 3 elements - A Message, Element and a String
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String multiVarName = "multiVar";
        nameAttr.setValue("${" + multiVarName + "}");
        nameAttr.setRepeatIfMulti(true);
        nameAttr.setAddBehavior(ADD_AS_XML);
        attributes.add(nameAttr);

        String customXml =
        "<custom:PersonName xmlns:custom=\"http://custom.com\" >" +
        "James" +
        "</custom:PersonName>";

        Message mVar = new Message(XmlUtil.parse(customXml));
        mVar = new Message(XmlUtil.parse(customXml));

        final PolicyEnforcementContext context = getContext();

        // Multi var contains a Message, a String and an Element.
        final String text = "This is some text";
        List<Object> multiVarValue = Arrays.asList(mVar, text, mVar.getXmlKnob().getDocumentReadOnly().getDocumentElement());
        context.setVariable(multiVarName, multiVarValue);

        {
            // V2
            samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 3, attributeTypesList.size());
            System.out.println(XmlUtil.nodeToFormattedString(attrStmtType.getDomNode()));

            // Element type
            AttributeType attributeType = attributeTypesList.get(0);
            XmlObject attributeValue = attributeType.getAttributeValueArray(0);
            Node valueNode = attributeValue.getDomNode();
            failIfXmlNotEqual(customXml, (Element) valueNode.getChildNodes().item(0), "Invalid first element");

            // Text type
            final AttributeType attributeTypeText = attributeTypesList.get(1);
            final XmlObject attributeTextValue = attributeTypeText.getAttributeValueArray(0);
            final Node valueTextNode = attributeTextValue.getDomNode();

            final String textValue = XmlUtil.getTextValue((Element) valueTextNode, false);
            Assert.assertEquals("Invalid text node value", text, textValue);

            // Element type
            attributeType = attributeTypesList.get(2);
            attributeValue = attributeType.getAttributeValueArray(0);
            valueNode = attributeValue.getDomNode();
            failIfXmlNotEqual(customXml, (Element) valueNode.getChildNodes().item(0), "Invalid first element");
        }
        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 3, attributeTypesList.size());
            System.out.println(XmlUtil.nodeToFormattedString(attrStmtType.getDomNode()));

            // Element type
            x0Assertion.oasisNamesTcSAML1.AttributeType attributeType = attributeTypesList.get(0);
            XmlObject attributeValue = attributeType.getAttributeValueArray(0);
            Node valueNode = attributeValue.getDomNode();
            failIfXmlNotEqual(customXml, (Element) valueNode.getChildNodes().item(0), "Invalid first element");

            // Text type
            final x0Assertion.oasisNamesTcSAML1.AttributeType attributeTypeText = attributeTypesList.get(1);
            final XmlObject attributeTextValue = attributeTypeText.getAttributeValueArray(0);
            final Node valueTextNode = attributeTextValue.getDomNode();

            final String textValue = XmlUtil.getTextValue((Element) valueTextNode, false);
            Assert.assertEquals("Invalid text node value", text, textValue);

            // Element type
            attributeType = attributeTypesList.get(2);
            attributeValue = attributeType.getAttributeValueArray(0);
            valueNode = attributeValue.getDomNode();
            failIfXmlNotEqual(customXml, (Element) valueNode.getChildNodes().item(0), "Invalid first element");
        }
    }

    public void failIfNotEqual(Element expectedElement, Element actualElement, String failMsg, boolean stripWhiteSpace) throws SAXException, IOException {
        ByteArrayOutputStream byteOutExpected = new ByteArrayOutputStream();

        if (stripWhiteSpace) {
            DomUtils.stripWhitespace(expectedElement);
        }
        XmlUtil.canonicalize(expectedElement, byteOutExpected);

        if (stripWhiteSpace) {
            DomUtils.stripWhitespace(actualElement);
        }

        ByteArrayOutputStream byteOutActual = new ByteArrayOutputStream();
        XmlUtil.canonicalize(actualElement, byteOutActual);

        junit.framework.Assert.assertEquals(failMsg, new String(byteOutExpected.toByteArray()), new String(byteOutActual.toByteArray()));
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

    private void validateAttributeV2(final AttributeType attributeType,
                                     final String expectedName,
                                     final String expectedValue,
                                     final int expectedNum) {
        Assert.assertEquals("Wrong name found", expectedName, attributeType.getName());
        Assert.assertEquals("Wrong name format found", SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC, attributeType.getNameFormat());

        final XmlObject[] attributeValueArray = attributeType.getAttributeValueArray();
        Assert.assertEquals("Wrong number of AttributeValue elements found", expectedNum, attributeValueArray.length);

        final XmlCursor xmlCursor = attributeValueArray[0].newCursor();
        Assert.assertEquals("Wrong attribute value found", expectedValue, xmlCursor.getTextValue());
        xmlCursor.dispose();
    }

    private void validateAttributeV1(final x0Assertion.oasisNamesTcSAML1.AttributeType attributeType,
                                     final String expectedName,
                                     final String expectedNamespace,
                                     final String expectedValue,
                                     final int expectedNum) {
        Assert.assertEquals("Wrong name found", expectedName, attributeType.getAttributeName());
        Assert.assertEquals("Wrong name format found", expectedNamespace, attributeType.getAttributeNamespace());

        final XmlObject[] attributeValueArray = attributeType.getAttributeValueArray();
        Assert.assertEquals("Wrong number of AttributeValue elements found", expectedNum, attributeValueArray.length);

        final XmlCursor xmlCursor = attributeValueArray[0].newCursor();
        Assert.assertEquals("Wrong attribute value found", expectedValue, xmlCursor.getTextValue());
        xmlCursor.dispose();
    }

    /**
     * Test that the attributes added to an AttributeStatement are correctly filtered for a set of requested
     * attributes supplied as Element references.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_Elements() throws Exception {
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("James");

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        final String filterExpression = "${attributeQuery.attributes}";
        final List<Element> reqAttributes = getAttributesFromRequest(attributeRequest_V2, SamlConstants.NS_SAML2, "Attribute");

        TestAudit audit = runTest(attributes, filterExpression, Arrays.asList(new Pair<String, Object>("attributeQuery.attributes", reqAttributes)), SamlConstants.NS_SAML2, 3, 2).right;
        for (String s : audit) {
            System.out.println(s);
        }
        assertTrue(audit.isAuditPresent(AssertionMessages.SAML_ISSUER_ATTR_STMT_FILTERED_ATTRIBUTES));
    }

    /**
     * Validate support for an filter expression that yields no usable variable values. If no usable values are supported
     * then no filtering will be done, in this case supplying an attribute filter has no affect. When this happens it is
     * audited at the WARNING level.
     * <p/>
     * This test validates support for an empty multi valued variable, empty string and non empty string.
     * <p/>
     * Audits are verified to ensure enough debug information is provided for these situations.
     */
    @Test
    public void testEmptyFilter_SupportedAndAuditing() throws Exception {
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("James");

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        final String filterExpression = "${attributeQuery.attributes}";
        TestAudit testAudit = runTest(attributes, filterExpression, Collections.<Pair<String, Object>>emptyList(), SamlConstants.NS_SAML2, 4, 2).right;
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE));

        // in this case the filter is simply empty - variable exsits but no values were found
        testAudit = runTest(attributes, filterExpression, Arrays.asList(new Pair<String, Object>("attributeQuery.attributes", Collections.emptyList())), SamlConstants.NS_SAML2, 4, 2).right;
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_ISSUER_ATTR_STMT_FILTER_EXPRESSION_NO_VALUES));
        assertTrue(testAudit.isAuditPresentContaining("Filter expression '${attributeQuery.attributes}' yielded no values."));

        // an emtpy string is found at runtime, this is logged with a warning.
        testAudit = runTest(attributes, filterExpression, Arrays.asList(new Pair<String, Object>("attributeQuery.attributes", "")), SamlConstants.NS_SAML2, 4, 2).right;
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_ISSUER_ATTR_STMT_PROCESSING_WARNING));
        assertTrue(testAudit.isAuditPresentContaining("Unsupported variable value found of type String when extracting filter Attributes: ''"));
        assertTrue(testAudit.isAuditPresentContaining("Filter expression '${attributeQuery.attributes}' yielded no values."));

        testAudit = runTest(attributes, filterExpression, Arrays.asList(new Pair<String, Object>("attributeQuery.attributes", "something odd resolved at runtime")), SamlConstants.NS_SAML2, 4, 2).right;
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_ISSUER_ATTR_STMT_PROCESSING_WARNING));
        assertTrue(testAudit.isAuditPresentContaining("Unsupported variable value found of type String when extracting filter Attributes: 'something odd resolved at runtime'"));
        assertTrue(testAudit.isAuditPresentContaining("Filter expression '${attributeQuery.attributes}' yielded no values."));
    }

    /**
     * Test that the attributes added to an AttributeStatement are correctly filtered for a set of requested
     * attributes supplied as Element and Message references.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_Elements_And_Messages() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("James");

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        //add another a as message variable
        String attributeMiddleName =
        "  <saml:Attribute Name=\"nc:PersonMiddleName\"\n" +
        "       xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
        "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
        "  </saml:Attribute>";

        Message mVar = new Message(XmlUtil.parse(attributeMiddleName));

        final String filterExpression = "${attributeQuery.attributes} ${MiddleName}";
        final List<Element> reqAttrs = getAttributesFromRequest(attributeRequest_V2, SamlConstants.NS_SAML2, "Attribute");

        final Document document = runTest(attributes, filterExpression,
                Arrays.asList(new Pair<String, Object>("attributeQuery.attributes", reqAttrs.get(0)), //just first element
                        new Pair<String, Object>("MiddleName", mVar)),
                SamlConstants.NS_SAML2, 2, 2).left;

        System.out.println(XmlUtil.nodeToFormattedString(document.getDocumentElement()));

        // not_in_request should not be present as it was not in the request
        SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
        final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
        x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

        final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());

        final AttributeStatementType attrStmtType = attrStmtList.get(0);
        List<x0Assertion.oasisNamesTcSAML2.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
        for (AttributeType attributeType : attributeTypesList) {
            Assert.assertFalse("Attribute with this name should not be found", "not_in_request".equals(attributeType.getName()));
        }
    }

    /**
     * Test that the attributes added to an AttributeStatement are correctly filtered for a set of requested
     * attributes supplied as Element references.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V1_Elements() throws Exception {
        // Statically configure attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNamespace("http://uri.com");
        nameAttr.setValue("James");

        final SamlAttributeStatement.Attribute middleNameAttr = new SamlAttributeStatement.Attribute();
        middleNameAttr.setName("nc:PersonMiddleName");
        middleNameAttr.setNamespace("http://uri.com");
        middleNameAttr.setValue("Tiberius");

        final SamlAttributeStatement.Attribute lastNameAttr = new SamlAttributeStatement.Attribute();
        lastNameAttr.setName("nc:PersonSurName");
        lastNameAttr.setNamespace("http://uri.com");
        lastNameAttr.setValue("Kirk");

        // This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNamespace("http://uri.com");
        attributeNotInRequest.setValue("not_in_request_value");

        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();
        attributes.add(nameAttr);
        attributes.add(middleNameAttr);
        attributes.add(lastNameAttr);
        attributes.add(attributeNotInRequest);

        final String filterExpression = "${attributeQuery.attributes}";
        final List<Element> reqAttrs = getAttributesFromRequest(attributeRequest_V1, SamlConstants.NS_SAML, "AttributeDesignator");

        runTest(attributes, filterExpression, Arrays.asList(new Pair<String, Object>("attributeQuery.attributes", reqAttrs)), SamlConstants.NS_SAML, 3, 1);
    }

    /**
     * Test that the attributes added to an AttributeStatement are correctly filtered for a set of requested
     * attributes.
     * This test case ensures that incoming AttributeValue are respected and that only matching values are returned.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_Match() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("Name does not match request so this attribute should be filtered out");

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        final PolicyEnforcementContext context = getContext();
        context.setVariable(varName, getElementsFromXml(attributeRequestWithAttributeValue, SamlConstants.NS_SAML2, "Attribute"));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 2, attribute.size());

        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_ISSUER_ATTR_STMT_VALUE_EXCLUDED_ATTRIBUTE_DETAILS));
        assertTrue(testAudit.isAuditPresentContaining("Resolved value for Attribute 'nc:PersonGivenName' was filtered as its value 'Name does not match request so this attribute should be filtered out' was not included in the corresponding filter Attribute's AttributeValue."));
    }

    /**
     * Validate that a variable with a null value in AttributeValue when configured to be added as XML does not NPE
     */
    @BugNumber(11646)
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_Add_As_XML_NPE() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("${nullvar}");
        nameAttr.setAddBehavior(SamlAttributeStatement.Attribute.AttributeValueAddBehavior.ADD_AS_XML);

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final PolicyEnforcementContext context = getContext();
        context.setVariable(varName, getElementsFromXml(attributeRequestWithAttributeValue, SamlConstants.NS_SAML2, "Attribute"));
        context.setVariable("nullvar", null);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 2, attribute.size());
    }

    /**
     * Validate that a context variable with a null value referenced from an attribute value does not cause an NPE
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_NPE() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("${nullvar}");

        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final PolicyEnforcementContext context = getContext();
        context.setVariable("nullvar", null);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 1, attribute.size());
    }

    /**
     * Validate that a variable used in the Filter Attribute text field which resolves to null does not NPE
     *
     * Also validates that if an AttributeValue contains a Message with invalid XML no NPE occurs.
     *
     * Auditing is validated for both scenarios.
     */
    @BugNumber(11613)
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeFilterExpression_NPE() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("${notxml}");
        nameAttr.setAddBehavior(SamlAttributeStatement.Attribute.AttributeValueAddBehavior.ADD_AS_XML);

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        // Bug repro - this variable will be null at runtime
        samlAttributeStatement.setFilterExpression("${nullvar}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        final PolicyEnforcementContext context = getContext();
        final Message notXmlMsg = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream("not xml".getBytes()));
        context.setVariable("nullvar", null);
        context.setVariable("notxml", notXmlMsg);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        // We expect all attributes as none were filtered
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement, none should have been filtered", 4, attribute.size());

        for (String s : testAudit) {
            System.out.println(s);
        }

        Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_ISSUER_ATTR_STMT_PROCESSING_WARNING));
        Assert.assertTrue(testAudit.isAuditPresentContaining("Invalid XML message referenced within Attribute configuration"));
    }

    @BugNumber(11955)
    @Test
    public void testAttriubuteStatement_Filter_InvalidXML_NoNPE() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("name");
        nameAttr.setAddBehavior(SamlAttributeStatement.Attribute.AttributeValueAddBehavior.ADD_AS_XML);

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        samlAttributeStatement.setFilterExpression("${notXml}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        final PolicyEnforcementContext context = getContext();
        final Message notXmlMsg = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream("not xml".getBytes()));
        context.setVariable("notXml", notXmlMsg);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        // We expect all attributes as none were filtered
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement, none should have been filtered", 4, attribute.size());

        for (String s : testAudit) {
            System.out.println(s);
        }

        Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.MESSAGE_VARIABLE_BAD_XML));

        Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_ISSUER_ATTR_STMT_FILTER_EXPRESSION_NO_VALUES));
        Assert.assertTrue(testAudit.isAuditPresentContaining("Filter expression '${notXml}' yielded no values."));
    }

    @BugNumber(11955)
    @Test
    public void testAttriubuteStatement_Filter_EmptyMessage_NoNPE() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("name");
        nameAttr.setAddBehavior(SamlAttributeStatement.Attribute.AttributeValueAddBehavior.ADD_AS_XML);

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        samlAttributeStatement.setFilterExpression("${emptyMsg}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        final PolicyEnforcementContext context = getContext();
        final Message notXmlMsg = new Message(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream("".getBytes()));
        context.setVariable("emptyMsg", notXmlMsg);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        // We expect all attributes as none were filtered
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement, none should have been filtered", 4, attribute.size());

        for (String s : testAudit) {
            System.out.println(s);
        }

        Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.MESSAGE_VARIABLE_BAD_XML));

        Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_ISSUER_ATTR_STMT_FILTER_EXPRESSION_NO_VALUES));
        Assert.assertTrue(testAudit.isAuditPresentContaining("Filter expression '${emptyMsg}' yielded no values."));
    }

    /**
     * Tests that configured Attributes are correctly filtered based on request attributes when the runtime value
     * of an Attribute may be multiple values.
     * In this case none of the configured values match the filter.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_NoMatch_Repeat() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String multiValuedVar = "multiValuedVar";
        nameAttr.setValue("${" + multiValuedVar + "}");
        nameAttr.setRepeatIfMulti(true);

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final PolicyEnforcementContext context = getContext();
        context.setVariable(varName, getElementsFromXml(attributeRequestWithAttributeValue, SamlConstants.NS_SAML2, "Attribute"));
        context.setVariable(multiValuedVar, Arrays.asList("Name1", "Name2"));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 2, attribute.size());
        failIfNotEqual(attrStatement, XmlUtil.parse(expectedWhenNameFiltered).getDocumentElement(), "Unexpected AttributeStatement created.", true);
    }

    /**
     * Same test as above, just check that incoming empty strings are just treated as tokens same as non empty strings are.
     * @throws Exception
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_EmptyIncoming_AttributeValue() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String multiValuedVar = "multiValuedVar";
        nameAttr.setValue("${" + multiValuedVar + "}");
        nameAttr.setRepeatIfMulti(true);

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final PolicyEnforcementContext context = getContext();
        context.setVariable(varName, getElementsFromXml(attributeRequestWithEmptyAttributeValue, SamlConstants.NS_SAML2, "Attribute"));
        context.setVariable(multiValuedVar, Arrays.asList("Name1", "Name2"));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 2, attribute.size());
        failIfNotEqual(attrStatement, XmlUtil.parse(expectedWhenNameFiltered).getDocumentElement(), "Unexpected AttributeStatement created.", true);
    }

    /**
     * Tests that configured Attributes are correctly filtered based on request attributes when the runtime value
     * of an Attribute may be multiple values.
     * This test ensures that only matching values are returned - when there can be many both incoming and configured
     * such that only the intersection of values is returned.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_Choice_Repeat() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String multiValuedVar = "multiValuedVar";
        nameAttr.setValue("${" + multiValuedVar + "}");
        nameAttr.setRepeatIfMulti(true);

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final PolicyEnforcementContext context = getContext();
        context.setVariable(varName, getElementsFromXml(attributeRequestWithAttributeValue, SamlConstants.NS_SAML2, "Attribute"));
        context.setVariable(multiValuedVar, Arrays.asList("Jim", "James", "John"));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        //Note: THIS WILL FAIL WHEN WE START PRODUCING A SINGLE ATTRIBUTE WITH MULTIPLE VALUES INSTEAD OF MULTIPLE ATTRIBUTES. See bug 11200
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 4, attribute.size());

        failIfNotEqual(attrStatement, XmlUtil.parse(expectedChosenAttributeStatementMulti).getDocumentElement(), "Unexpected AttributeStatement created.", true);
    }

    /**
     * Tests that XML is supported for Attribute Values and when XML that incoming AttributeValues are correctly
     * matched against configured (runtime) XML values.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_Choice_Repeat_XML() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String multiValuedVar = "multiValuedVar";
        nameAttr.setValue("${" + multiValuedVar + "}");
        nameAttr.setRepeatIfMulti(true);
        nameAttr.setAddBehavior(ADD_AS_XML);
        nameAttr.setValueComparison(CANONICALIZE);

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final PolicyEnforcementContext context = getContext();
        // add filter expression variables.

        context.setVariable(varName, getElementsFromXml(attributeRequestWithAttributeValueXml, SamlConstants.NS_SAML2, "Attribute"));
        context.setVariable(multiValuedVar, Arrays.asList(
                new Message(XmlUtil.parse("<xml>Jim</xml>")),
                new Message(XmlUtil.parse("<xml>James</xml>")),
                new Message(XmlUtil.parse("<xml>John</xml>"))));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        //Note: THIS WILL FAIL WHEN WE START PRODUCING A SINGLE ATTRIBUTE WITH MULTIPLE VALUES INSTEAD OF MULTIPLE ATTRIBUTES. See bug 11200
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 4, attribute.size());

        failIfNotEqual(attrStatement, XmlUtil.parse(expectedChosenAttributeStatementMultiXml).getDocumentElement(), "Unexpected AttributeStatement created.", true);
    }

    /**
     * Test that incoming AttributeValue elements are used to filter the set of return values - e.g. the Gateway
     * can choose from an incoming list.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_Choice() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("Jim"); //Jim is one of two values in the request - so it should be included in the response.

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final PolicyEnforcementContext context = getContext();
        // add filter expression variables.

        context.setVariable(varName, getElementsFromXml(attributeRequestWithAttributeValue, SamlConstants.NS_SAML2, "Attribute"));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 3, attribute.size());

        failIfNotEqual(attrStatement, XmlUtil.parse(expectedChosenAttributeStatement).getDocumentElement(), "Unexpected AttributeStatement created.", true);
    }

    /**
     * Tests that when CANONICALIZE is chosen for AttributeValue comparison that it continues to work for simple
     * string values.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_String_Canonalize() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("Jim");
        nameAttr.setValueComparison(CANONICALIZE);

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();
        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final PolicyEnforcementContext context = getContext();
        context.setVariable(varName, getElementsFromXml(attributeRequestWithAttributeValue, SamlConstants.NS_SAML2, "Attribute"));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 3, attribute.size());

        failIfNotEqual(attrStatement, XmlUtil.parse(expectedChosenAttributeStatement).getDocumentElement(), "Unexpected AttributeStatement created.", true);
    }

    /**
     * Tests attribute value comparison for canonicalized XML AttributeValue values.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_XML_Canonalize() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String xmlVarName = "xmlVar";
        nameAttr.setValue("${" + xmlVarName + "}");
        nameAttr.setValueComparison(CANONICALIZE);
        nameAttr.setAddBehavior(ADD_AS_XML);

        Message xmlMsgForVar = new Message(XmlUtil.parse("<xml>Jim</xml>"));

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final PolicyEnforcementContext context = getContext();
        context.setVariable(varName, getElementsFromXml(attributeRequestWithAttributeValuesXml, SamlConstants.NS_SAML2, "Attribute"));
        context.setVariable(xmlVarName, xmlMsgForVar);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 3, attribute.size());

        failIfNotEqual(attrStatement, XmlUtil.parse(expectedAttributeStatementXmlAttributeValue).getDocumentElement(), "Unexpected AttributeStatement created.", true);
    }

    /**
     * Tests attribute value comparison for canonicalized AttributeValue values with mixed content.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_XML_MixedContent_Canonalize() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String xmlVarName = "xmlVar";
        nameAttr.setValue("${" + xmlVarName + "} just some text ");//trailing spaces
        nameAttr.setValueComparison(CANONICALIZE);
        nameAttr.setAddBehavior(ADD_AS_XML);

        Message xmlMsgForVar = new Message(XmlUtil.parse("<xml>Jim</xml>"));

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();

        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final PolicyEnforcementContext context = getContext();
        context.setVariable(varName, getElementsFromXml(attributeRequestWithAttributeValuesXmlMixedContent, SamlConstants.NS_SAML2, "Attribute"));
        context.setVariable(xmlVarName, xmlMsgForVar);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 3, attribute.size());

        failIfNotEqual(attrStatement, XmlUtil.parse(expectedAttributeStatementXmlAttributeValueMixedContent).getDocumentElement(), "Unexpected AttributeStatement created.", true);
    }

    /**
     * Tests the 'If variable not found' config option when configured to replace a variable with an empty string
     */
    @Test
    public void testAttributeStatement_AttributeValue_VariableNotFound_ReplaceVarWithEmpty() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String varName = "empty";
        nameAttr.setValue("${" + varName + "} just some text");
        nameAttr.setRepeatIfMulti(false);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        final PolicyEnforcementContext context = getContext();
        context.setVariable(varName, "");

        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            validateAttributeV2(attributeTypesList.get(0), "nc:PersonGivenName", " just some text", 1);
        }
        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            validateAttributeV1(attributeTypesList.get(0), "nc:PersonGivenName", namespace, " just some text", 1);
        }
    }

    /**
     * Tests the 'If variable not found' config option when configured to replace an expression with an empty string
     */
    @Test
    public void testAttributeStatement_AttributeValue_VariableNotFound_ReplaceExpressionWithEmpty() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        final String varName = "empty";
        nameAttr.setValue("${" + varName + "} just some text");
        nameAttr.setRepeatIfMulti(false);
        nameAttr.setVariableNotFoundBehavior(REPLACE_EXPRESSION_EMPTY_STRING);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        final PolicyEnforcementContext context = getContext();
//        context.setVariable(varName, ""); Don't set the variable
        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            validateAttributeV2(attributeTypesList.get(0), "nc:PersonGivenName", "", 1);
        }
        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            validateAttributeV1(attributeTypesList.get(0), "nc:PersonGivenName", namespace, "", 1);
        }
    }

    /**
     * Tests the 'If value resolves to empty string' behavior - Add empty attribute value
     */
    @Test
    public void testAttributeStatement_AttributeValue_BehaviorWhenEmpty_AddEmptyAttributeValue() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("");  // empty value
        nameAttr.setRepeatIfMulti(false);
        nameAttr.setEmptyBehavior(EMPTY_STRING);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        final PolicyEnforcementContext context = getContext();
        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            validateAttributeV2(attributeTypesList.get(0), "nc:PersonGivenName", "", 1);
        }
        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            validateAttributeV1(attributeTypesList.get(0), "nc:PersonGivenName", namespace, "", 1);
        }
    }

    /**
     * Tests the 'If value resolves to empty string' behavior - 'Do not add AttributeValue'.
     */
    @Test
    public void testAttributeStatement_AttributeValue_BehaviorWhenEmpty_DoNotAddAttributeValue() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("");  // empty value
        nameAttr.setRepeatIfMulti(false);
        nameAttr.setEmptyBehavior(EXISTS_NO_VALUE);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        final PolicyEnforcementContext context = getContext();
        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            final XmlObject[] attributeValueArray = attributeTypesList.get(0).getAttributeValueArray();
            Assert.assertEquals("No AttributeValue elements expected", 0, attributeValueArray.length);
        }
        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            final XmlObject[] attributeValueArray = attributeTypesList.get(0).getAttributeValueArray();
            Assert.assertEquals("No AttributeValue elements expected", 0, attributeValueArray.length);
        }
    }

    /**
     * Tests the 'If value resolves to empty string' behavior - 'Add null value AttributeValue'.
     */
    @Test
    public void testAttributeStatement_AttributeValue_BehaviorWhenEmpty_AddNullValueAttributeValue() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("");  // empty value
        nameAttr.setRepeatIfMulti(false);
        nameAttr.setEmptyBehavior(NULL_VALUE);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        final PolicyEnforcementContext context = getContext();
        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            final XmlObject[] attributeValueArray = attributeTypesList.get(0).getAttributeValueArray();
            Assert.assertEquals("Wrong number of AttributeValue elements found", 1, attributeValueArray.length);

            final XmlCursor xmlCursor = attributeValueArray[0].newCursor();
            Assert.assertFalse("AttributeValue element should have no value", xmlCursor.toFirstChild());
            final String nilText = xmlCursor.getAttributeText(new QName("http://www.w3.org/2001/XMLSchema-instance", "nil"));
            Assert.assertEquals("Wrong xsi:nil attribute value found", "true", nilText);
            xmlCursor.dispose();

        }
        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            final XmlObject[] attributeValueArray = attributeTypesList.get(0).getAttributeValueArray();
            Assert.assertEquals("Wrong number of AttributeValue elements found", 1, attributeValueArray.length);

            final XmlCursor xmlCursor = attributeValueArray[0].newCursor();
            Assert.assertFalse("AttributeValue element should have no value", xmlCursor.toFirstChild());
            final String nilText = xmlCursor.getAttributeText(new QName("http://www.w3.org/2001/XMLSchema-instance", "nil"));
            Assert.assertEquals("Wrong xsi:nil attribute value found", "true", nilText);
            xmlCursor.dispose();
        }
    }

    /**
     * Tests the 'If value resolves to empty string' behavior when XML values (Message / Element) are allowed and don't exist.
     */
    @Test
    public void testAttributeStatement_AttributeValue_BehaviorWhenEmpty_AddNullValueAttributeValue_ConfiguredForMessages() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("");  // empty value
        nameAttr.setRepeatIfMulti(false);
        nameAttr.setAddBehavior(ADD_AS_XML);
        nameAttr.setEmptyBehavior(NULL_VALUE);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        final PolicyEnforcementContext context = getContext();
        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV2 samlAssertionV2 = new SamlAssertionV2(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV2.getXmlBeansAssertionType();
            x0Assertion.oasisNamesTcSAML2.AssertionType assertionType = (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlBeansAssertionType;

            final List<AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            final XmlObject[] attributeValueArray = attributeTypesList.get(0).getAttributeValueArray();
            Assert.assertEquals("Wrong number of AttributeValue elements found", 1, attributeValueArray.length);

            final XmlCursor xmlCursor = attributeValueArray[0].newCursor();
            Assert.assertFalse("AttributeValue element should have no value", xmlCursor.toFirstChild());
            final String nilText = xmlCursor.getAttributeText(new QName("http://www.w3.org/2001/XMLSchema-instance", "nil"));
            Assert.assertEquals("Wrong xsi:nil attribute value found", "true", nilText);
            xmlCursor.dispose();

        }
        {
            //V1
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            SamlAssertionV1 samlAssertionV1 = new SamlAssertionV1(document.getDocumentElement(), null);
            final XmlObject xmlBeansAssertionType = samlAssertionV1.getXmlBeansAssertionType();
            AssertionType assertionType = (AssertionType) xmlBeansAssertionType;

            final List<x0Assertion.oasisNamesTcSAML1.AttributeStatementType> attrStmtList = Arrays.asList(assertionType.getAttributeStatementArray());
            Assert.assertEquals("Only 1 attribute statement expected", 1, attrStmtList.size());

            final x0Assertion.oasisNamesTcSAML1.AttributeStatementType attrStmtType = attrStmtList.get(0);
            List<x0Assertion.oasisNamesTcSAML1.AttributeType> attributeTypesList = Arrays.asList(attrStmtType.getAttributeArray());
            Assert.assertEquals("Wrong number of Attribute elements found.", 1, attributeTypesList.size());

            final XmlObject[] attributeValueArray = attributeTypesList.get(0).getAttributeValueArray();
            Assert.assertEquals("Wrong number of AttributeValue elements found", 1, attributeValueArray.length);

            final XmlCursor xmlCursor = attributeValueArray[0].newCursor();
            Assert.assertFalse("AttributeValue element should have no value", xmlCursor.toFirstChild());
            final String nilText = xmlCursor.getAttributeText(new QName("http://www.w3.org/2001/XMLSchema-instance", "nil"));
            Assert.assertEquals("Wrong xsi:nil attribute value found", "true", nilText);
            xmlCursor.dispose();
        }
    }

    /**
     * Tests the 'If value resolves to empty string' behavior when XML values (Message / Element) are allowed and don't exist.
     */
    @Test
    public void testAttributeStatement_AttributeValue_Fail_If_Any_Attributes_Missing() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("");  // empty value
        nameAttr.setMissingWhenEmpty(true);
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        samlAttributeStatement.setFailIfAnyAttributeIsMissing(true);

        {
            // V2
            final PolicyEnforcementContext context = getContext();
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            try {
                serverAssertion.checkRequest(context);
                Assert.fail("AssertionStatusException should have been thrown.");
            } catch (AssertionStatusException e) {
            }
            Assert.assertNotNull(context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_MISSING_ATTRIBUTE_NAMES));
        }
        {
            //V1
            final PolicyEnforcementContext context = getContext();
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            try {
                serverAssertion.checkRequest(context);
                Assert.fail("AssertionStatusException should have been thrown.");
            } catch (AssertionStatusException e) {
            }
            Assert.assertNotNull(context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_MISSING_ATTRIBUTE_NAMES));
        }
    }

    /**
     * Tests fail case when a filter Attribute is unknown to the static Attribute configuration.
     */
    @Test
    public void testAttributeStatement_AttributeValue_Fail_When_Unknown_Attribute_InFilter() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("James");
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String filterExpression = "${attributeQuery.attributes}";
        samlAttributeStatement.setFilterExpression(filterExpression);
        samlAttributeStatement.setFailIfUnknownAttributeInFilter(true);

        {
            // V2
            final PolicyEnforcementContext context = getContext();
            context.setVariable("attributeQuery.attributes", getAttributesFromRequest(attributeRequest_V2, SamlConstants.NS_SAML2, "Attribute"));

            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

            try {
                serverAssertion.checkRequest(context);
                Assert.fail("Assertion should have failed as unknown attribute was requested.");
            } catch (AssertionStatusException e) {
            }
            Assert.assertNotNull(context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_UNKNOWN_ATTRIBUTE_NAMES));
        }
        {
            //V1
            final PolicyEnforcementContext context = getContext();
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            context.setVariable("attributeQuery.attributes", getAttributesFromRequest(attributeRequest_V1, SamlConstants.NS_SAML, "AttributeDesignator"));

            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            try {
                serverAssertion.checkRequest(context);
                Assert.fail("Assertion should have failed as unknown attribute was requested.");
            } catch (AssertionStatusException e) {
            }
            Assert.assertNotNull(context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_UNKNOWN_ATTRIBUTE_NAMES));
       }
    }

    /**
     * Tests that assertion fails when an attribute value is filtered due to incoming filter, when configured to do so.
     */
    @BugNumber(11651)
    @Test
    public void testAttributeStatement_AttributeValue_Fail_When_Attribute_Filtered() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("Donal");
        attributes.add(nameAttr);

        final List<SamlAttributeStatement.Attribute> someAttributes = getSomeAttributes();
        for (SamlAttributeStatement.Attribute someAttribute : someAttributes) {
            attributes.add(someAttribute);
        }

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String filterExpression = "${attributeQuery.attributes}";
        samlAttributeStatement.setFilterExpression(filterExpression);
        samlAttributeStatement.setFailIfAttributeValueExcludesAttribute(true);

        // Only applies to V2, V1 does not support incoming AttributeValue elements.
        final PolicyEnforcementContext context = getContext();
        context.setVariable("attributeQuery.attributes", getAttributesFromRequest(attributeRequestWithAttributeValue, SamlConstants.NS_SAML2, "Attribute"));

        SamlIssuerAssertion assertion = new SamlIssuerAssertion();
        assertion.setAttributeStatement(samlAttributeStatement);
        assertion.setVersion(2);
        assertion.setSignAssertion(false);//don't need
        ServerSamlIssuerAssertion serverAssertion = new ServerSamlIssuerAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        try {
            serverAssertion.checkRequest(context);
            Assert.fail("Assertion should have failed as Attribute was filtered out.");
        } catch (AssertionStatusException e) {
        }
        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresentContaining("Attribute filter AttributeValue excluded some Attributes: [Name=nc:PersonGivenName NameFormat=urn:oasis:names:tc:SAML:2.0:attrname-format:basic]"));
        final Object filterVariable = context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_EXCLUDED_ATTRIBUTES);
        Assert.assertNotNull(filterVariable);
        Assert.assertEquals(filterVariable.toString(), "[Name=nc:PersonGivenName NameFormat=urn:oasis:names:tc:SAML:2.0:attrname-format:basic]");
    }

    @Test
    public void testAttributeStatement_Filter_NoAttributesAdded() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:NotInFilter");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("NotInFilter Value");
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String filterExpression = "${attributeQuery.attributes}";
        samlAttributeStatement.setFilterExpression(filterExpression);
        samlAttributeStatement.setFailIfNoAttributesAdded(true);

        {
            // V2
            final PolicyEnforcementContext context = getContext();
            context.setVariable("attributeQuery.attributes", getAttributesFromRequest(attributeRequest_V2, SamlConstants.NS_SAML2, "Attribute"));

            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

            try {
                serverAssertion.checkRequest(context);
                Assert.fail("Assertion should have failed as unknown attribute was requested.");
            } catch (AssertionStatusException e) {
            }
            final Object variable = context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_NO_ATTRIBUTES_ADDED);
            Assert.assertNotNull(variable);
            final boolean b = Boolean.parseBoolean(variable.toString());
            System.out.println("Variable value: " + b);
        }
        {
            //V1
            final PolicyEnforcementContext context = getContext();
            final String namespace = "http://namespace.com";
            nameAttr.setNamespace(namespace);
            context.setVariable("attributeQuery.attributes", getAttributesFromRequest(attributeRequest_V1, SamlConstants.NS_SAML, "AttributeDesignator"));

            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);

            try {
                serverAssertion.checkRequest(context);
                Assert.fail("Assertion should have failed as unknown attribute was requested.");
            } catch (AssertionStatusException e) {
            }
            final Object variable = context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_NO_ATTRIBUTES_ADDED);
            Assert.assertNotNull(variable);
            final boolean b = Boolean.parseBoolean(variable.toString());
            System.out.println("Variable value: " + b);
       }
    }

    /**
     * If attributes were filtered out due to incoming attribute values, then the 'Fail if no attributes' added feature
     * should fail if this causes no attributes to be added.
     *
     * Applies to SAML 2 only.
     *
     * @throws Exception
     */
    @BugNumber(11739)
    @Test
    public void testAttributeStatement_Filter_NoAttributesAdded_BecauseOfAttributeValue() throws Exception {
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("Not in filter");
        attributes.add(nameAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String filterExpression = "${attributeQuery.attributes}";
        samlAttributeStatement.setFilterExpression(filterExpression);
        samlAttributeStatement.setFailIfNoAttributesAdded(true);

        final PolicyEnforcementContext context = getContext();
        context.setVariable("attributeQuery.attributes", getAttributesFromRequest(attributeRequestWithAttributeValue, SamlConstants.NS_SAML2, "Attribute"));

        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);

        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put( "auditFactory", testAudit.factory() )
                .unmodifiableMap()
        );

        try {
            serverAssertion.checkRequest(context);
            Assert.fail("Assertion should have failed as unknown attribute was requested.");
        } catch (AssertionStatusException e) {
        }

        // expected related set variable have correct values
        final Object variable = context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_NO_ATTRIBUTES_ADDED);
        Assert.assertNotNull(variable);
        final boolean b = Boolean.parseBoolean(variable.toString());
        System.out.println("Variable value: " + b);

        final String expectedValue = "[Name=nc:PersonGivenName NameFormat=urn:oasis:names:tc:SAML:2.0:attrname-format:basic]";
        final Object excluded = context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_EXCLUDED_ATTRIBUTES);
        System.out.println(excluded);
        Assert.assertEquals("Incorrect attribute excluded", expectedValue, excluded);

        for (String s : testAudit) {
            System.out.println(s);
        }

        // validate audits
        testAudit.isAuditPresent(AssertionMessages.SAML_ISSUER_ATTR_STMT_FILTER_REMOVED_ALL_ATTRIBUTES);
        testAudit.isAuditPresent(AssertionMessages.SAML_ISSUER_ATTR_STMT_VALUE_EXCLUDED_ATTRIBUTES);
        testAudit.isAuditPresentContaining("Attribute filter AttributeValue excluded some Attributes: [Name=nc:PersonGivenName NameFormat=urn:oasis:names:tc:SAML:2.0:attrname-format:basic]");
    }

    /**
     * Tests that all context variables are set, when no fail modes are configured.
     *
     * In the pre 6.2 case the conditions for when these variables are not provided cannot happen, so they should
     * all contain either empty string values or false. Accessing these variables should not cause WARNING logging
     * due to the variable not existing. (Not tested automatically)
     */
    @Test
    public void testAttributeStatement_AllSetContextVariables_Normal_AllEmpty() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setNamespace("http://namespace.com");
        nameAttr.setValue("James");

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();
        attributes.add(nameAttr);
        attributes.addAll(middleAndLastName);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));

        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final PolicyEnforcementContext context = getContext();

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_UNKNOWN_ATTRIBUTE_NAMES, "", context);
            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_FILTERED_ATTRIBUTES, "", context);
            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_EXCLUDED_ATTRIBUTES, "", context);
            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_MISSING_ATTRIBUTE_NAMES, "", context);
        }
        {
            // V1
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);
            final PolicyEnforcementContext context = getContext();

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_UNKNOWN_ATTRIBUTE_NAMES, "", context);
            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_FILTERED_ATTRIBUTES, "", context);
            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_MISSING_ATTRIBUTE_NAMES, "", context);

            // Ensure excluded was not set.
            try {
                context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_EXCLUDED_ATTRIBUTES);
                Assert.fail("Variable " + samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_EXCLUDED_ATTRIBUTES + " should not exist.");
            } catch (NoSuchVariableException e) {
            }
        }
    }

    /**
     * Tests that all context variables are set, when no fail modes are configured.
     */
    @Test
    public void testAttributeStatement_AllSetContextVariables_Normal_AllSet() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setNamespace("http://uri.com");
        nameAttr.setValue("UnexpectedName"); // Excluded

        final SamlAttributeStatement.Attribute missingAttr = new SamlAttributeStatement.Attribute();
        missingAttr.setName("Missing");
        missingAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        missingAttr.setNamespace("http://uri.com");
        missingAttr.setValue(""); // Missing
        missingAttr.setMissingWhenEmpty(true);

        final SamlAttributeStatement.Attribute notInFilterAttr = new SamlAttributeStatement.Attribute();
        notInFilterAttr.setName("NotInFilter");
        notInFilterAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        notInFilterAttr.setNamespace("http://uri.com");
        notInFilterAttr.setValue("notInFilterAttr");

        final List<SamlAttributeStatement.Attribute> middleAndLastName = getSomeAttributes();
        attributes.add(nameAttr);
        attributes.add(middleAndLastName.get(0)); // don't add surname - 'Unknown'
        attributes.add(notInFilterAttr); // 'Filtered'
        attributes.add(missingAttr);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");

        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final PolicyEnforcementContext context = getContext();
            final List<Element> elementsFromXml = getElementsFromXml(attributeRequestWithAttributeValue, SamlConstants.NS_SAML2, "Attribute");
            // add an attribute which will be 'filtered'.
            elementsFromXml.add(XmlUtil.parse("<saml:Attribute xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" Name=\"Missing\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"/>").getDocumentElement());
            context.setVariable(varName, elementsFromXml);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_UNKNOWN_ATTRIBUTE_NAMES, "[Name=nc:PersonSurName NameFormat=urn:oasis:names:tc:SAML:2.0:attrname-format:basic]", context);
            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_FILTERED_ATTRIBUTES, "[Name=NotInFilter NameFormat=urn:oasis:names:tc:SAML:2.0:attrname-format:basic]", context);
            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_EXCLUDED_ATTRIBUTES, "[Name=nc:PersonGivenName NameFormat=urn:oasis:names:tc:SAML:2.0:attrname-format:basic]", context);
            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_MISSING_ATTRIBUTE_NAMES, "[Name=Missing NameFormat=urn:oasis:names:tc:SAML:2.0:attrname-format:basic]", context);
        }
        {
            // V1
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);
            final PolicyEnforcementContext context = getContext();

            final List<Element> elementsFromXml = getElementsFromXml(attributeRequest_V1, SamlConstants.NS_SAML, "AttributeDesignator");
            elementsFromXml.add(XmlUtil.parse("<saml:AttributeDesignator xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" AttributeName=\"Missing\" AttributeNamespace=\"http://uri.com\"/>").getDocumentElement());
            context.setVariable(varName, elementsFromXml);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_UNKNOWN_ATTRIBUTE_NAMES, "[AttributeName=nc:PersonSurName AttributeNamespace=http://uri.com]", context);
            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_FILTERED_ATTRIBUTES, "[AttributeName=NotInFilter AttributeNamespace=http://uri.com]", context);
            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_MISSING_ATTRIBUTE_NAMES, "[AttributeName=Missing AttributeNamespace=http://uri.com]", context);

            // Ensure excluded was not set.
            try {
                context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_EXCLUDED_ATTRIBUTES);
                Assert.fail("Variable " + samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_EXCLUDED_ATTRIBUTES+ " should not exist.");
            } catch (NoSuchVariableException e) {
            }
        }
    }

    /**
     * Test that no attributes can be added and when so that the correct context variable is set.
     * @throws Exception
     */
    @Test
    public void testAttributeStatement_NoAttributes() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        final SamlAttributeStatement.Attribute notInFilterAttr = new SamlAttributeStatement.Attribute();
        notInFilterAttr.setName("NotInFilter");
        notInFilterAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        notInFilterAttr.setNamespace("http://uri.com");
        notInFilterAttr.setValue("notInFilterAttr");

        attributes.add(notInFilterAttr); // 'Filtered'

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        samlAttributeStatement.setFailIfNoAttributesAdded(false);

        {
            // V2
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 2);
            final PolicyEnforcementContext context = getContext();
            final List<Element> elementsFromXml = getElementsFromXml(attributeRequestWithAttributeValue, SamlConstants.NS_SAML2, "Attribute");
            context.setVariable(varName, elementsFromXml);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_NO_ATTRIBUTES_ADDED, "true", context);
        }
        {
            // V1
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, 1);
            final PolicyEnforcementContext context = getContext();

            final List<Element> elementsFromXml = getElementsFromXml(attributeRequest_V1, SamlConstants.NS_SAML, "AttributeDesignator");
            elementsFromXml.add(XmlUtil.parse("<saml:AttributeDesignator xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" AttributeName=\"Missing\" AttributeNamespace=\"http://uri.com\"/>").getDocumentElement());
            context.setVariable(varName, elementsFromXml);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            validateVariableNotNull(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_NO_ATTRIBUTES_ADDED, "true", context);

            // Ensure excluded was not set.
            try {
                context.getVariable(samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_EXCLUDED_ATTRIBUTES);
                Assert.fail("Variable " + samlAttributeStatement.getVariablePrefix() + "." + SamlAttributeStatement.SUFFIX_EXCLUDED_ATTRIBUTES+ " should not exist.");
            } catch (NoSuchVariableException e) {
            }
        }

    }

    /**
     * When the assertion cannot be added to the response, due to the response not containing well formed XML, validate
     * the correct audit occurs.
     */
    @BugNumber(11631)
    @Test
    public void test_CannotParseResponse_CorrectAudit() throws Exception {
        SamlIssuerAssertion assertion = new SamlIssuerAssertion();
        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        assertion.setAuthenticationStatement(authStmt);
        assertion.setVersion(2);
        assertion.setSignAssertion(false);//don't need
        //required for test case
        assertion.setDecorationTypes(EnumSet.of(SamlElementGenericConfig.DecorationType.RESPONSE));

        ServerSamlIssuerAssertion serverAssertion = new ServerSamlIssuerAssertion(assertion,
                ApplicationContexts.getTestApplicationContext());


        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put( "auditFactory", testAudit.factory() )
                .unmodifiableMap()
        );

        final PolicyEnforcementContext context = getContext();

        //set credentials
        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        final HttpBasicToken basicToken = new HttpBasicToken("testuser", new char[]{'p', 'a', 's', 's'});

        authContext.addCredentials(LoginCredentials.makeLoginCredentials(basicToken, HttpBasic.class));
        context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));
        context.getResponse().initialize(ContentTypeHeader.XML_DEFAULT, "not xml".getBytes());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SAML_ISSUER_CANNOT_PARSE_XML));
    }

    private void validateVariableNotNull(String variableName, String expectedValue, final PolicyEnforcementContext context) throws NoSuchVariableException {
        final Object variable = context.getVariable(variableName);
        Assert.assertNotNull(variable);
        if (expectedValue != null) {
            Assert.assertEquals("Unexpected variable value", expectedValue, variable.toString());
        }
    }

    private static final String expectedWhenNameFiltered = "    <saml2:AttributeStatement xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "        <saml2:Attribute Name=\"nc:PersonMiddleName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Tiberius</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonSurName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Kirk</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "    </saml2:AttributeStatement>";

    private static final String expectedAttributeStatementXmlAttributeValueMixedContent = "    <saml2:AttributeStatement xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "        <saml2:Attribute Name=\"nc:PersonGivenName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>\n" +
            "                <xml>Jim</xml> just some text \n" +
            "            </saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonMiddleName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Tiberius</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonSurName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Kirk</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "    </saml2:AttributeStatement>";

    private static final String expectedAttributeStatementXmlAttributeValue = "    <saml2:AttributeStatement xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "        <saml2:Attribute Name=\"nc:PersonGivenName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>\n" +
            "                <xml>Jim</xml>\n" +
            "            </saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonMiddleName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Tiberius</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonSurName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Kirk</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "    </saml2:AttributeStatement>";

    private static final String expectedChosenAttributeStatementMultiXml = "    <saml2:AttributeStatement xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "        <saml2:Attribute Name=\"nc:PersonGivenName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue><xml>Jim</xml></saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonGivenName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue><xml>James</xml></saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonMiddleName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Tiberius</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonSurName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Kirk</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "    </saml2:AttributeStatement>";

    private static final String expectedChosenAttributeStatementMulti = "    <saml2:AttributeStatement xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "        <saml2:Attribute Name=\"nc:PersonGivenName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Jim</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonGivenName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>James</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonMiddleName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Tiberius</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonSurName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Kirk</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "    </saml2:AttributeStatement>";

    private static final String expectedChosenAttributeStatement = "    <saml2:AttributeStatement xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "        <saml2:Attribute Name=\"nc:PersonGivenName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Jim</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonMiddleName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Tiberius</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "        <saml2:Attribute Name=\"nc:PersonSurName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "            <saml2:AttributeValue>Kirk</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "    </saml2:AttributeStatement>";

    private static final String attributeRequestWithAttributeValue =
            "<samlp:AttributeQuery\n" +
            "  xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "  xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "  ID=\"aaf23196-1773-2113-474a-fe114412ab72\"\n" +
            "  Destination=\"urn:idmanagement.gov:icam:bae:v2:1:7000:0000\"\n" +
            "  Version=\"2.0\"\n" +
            "  IssueInstant=\"2006-07-17T22:26:40Z\">\n" +
            "  <saml:Issuer>urn:idmanagement.gov:icam:bae:v2:1:2100:1700</saml:Issuer>\n" +
            "  <saml:Subject>\n" +
            "    <saml:NameID\n" +
            "      Format=\"urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n\">\n" +
            "          70001234000002110000000000000000\n" +
            "    </saml:NameID>\n" +
            "  </saml:Subject>\n" +
            "  <saml:Attribute Name=\"nc:PersonGivenName\"\n" +
            "      NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "     <saml:AttributeValue>Jim</saml:AttributeValue>\n" +
            "     <saml:AttributeValue>James</saml:AttributeValue>\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonMiddleName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonSurName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "</samlp:AttributeQuery>";

    private static final String attributeRequestWithEmptyAttributeValue =
            "<samlp:AttributeQuery\n" +
            "  xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "  xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "  ID=\"aaf23196-1773-2113-474a-fe114412ab72\"\n" +
            "  Destination=\"urn:idmanagement.gov:icam:bae:v2:1:7000:0000\"\n" +
            "  Version=\"2.0\"\n" +
            "  IssueInstant=\"2006-07-17T22:26:40Z\">\n" +
            "  <saml:Issuer>urn:idmanagement.gov:icam:bae:v2:1:2100:1700</saml:Issuer>\n" +
            "  <saml:Subject>\n" +
            "    <saml:NameID\n" +
            "      Format=\"urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n\">\n" +
            "          70001234000002110000000000000000\n" +
            "    </saml:NameID>\n" +
            "  </saml:Subject>\n" +
            "  <saml:Attribute Name=\"nc:PersonGivenName\"\n" +
            "      NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "     <saml:AttributeValue> </saml:AttributeValue>\n" +
            "     <saml:AttributeValue></saml:AttributeValue>\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonMiddleName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonSurName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "</samlp:AttributeQuery>";

    private static final String attributeRequestWithAttributeValueXml =
            "<samlp:AttributeQuery\n" +
            "  xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "  xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "  ID=\"aaf23196-1773-2113-474a-fe114412ab72\"\n" +
            "  Destination=\"urn:idmanagement.gov:icam:bae:v2:1:7000:0000\"\n" +
            "  Version=\"2.0\"\n" +
            "  IssueInstant=\"2006-07-17T22:26:40Z\">\n" +
            "  <saml:Issuer>urn:idmanagement.gov:icam:bae:v2:1:2100:1700</saml:Issuer>\n" +
            "  <saml:Subject>\n" +
            "    <saml:NameID\n" +
            "      Format=\"urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n\">\n" +
            "          70001234000002110000000000000000\n" +
            "    </saml:NameID>\n" +
            "  </saml:Subject>\n" +
            "  <saml:Attribute Name=\"nc:PersonGivenName\"\n" +
            "      NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "     <saml:AttributeValue><xml>Jim</xml></saml:AttributeValue>\n" +
            "     <saml:AttributeValue><xml>James</xml></saml:AttributeValue>\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonMiddleName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonSurName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "</samlp:AttributeQuery>";

    private static final String attributeRequestWithAttributeValuesXmlMixedContent =
            "<samlp:AttributeQuery\n" +
            "  xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "  xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "  ID=\"aaf23196-1773-2113-474a-fe114412ab72\"\n" +
            "  Destination=\"urn:idmanagement.gov:icam:bae:v2:1:7000:0000\"\n" +
            "  Version=\"2.0\"\n" +
            "  IssueInstant=\"2006-07-17T22:26:40Z\">\n" +
            "  <saml:Issuer>urn:idmanagement.gov:icam:bae:v2:1:2100:1700</saml:Issuer>\n" +
            "  <saml:Subject>\n" +
            "    <saml:NameID\n" +
            "      Format=\"urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n\">\n" +
            "          70001234000002110000000000000000\n" +
            "    </saml:NameID>\n" +
            "  </saml:Subject>\n" +
            "  <saml:Attribute Name=\"nc:PersonGivenName\"\n" +
            "      NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "     <saml:AttributeValue><xml>Jim</xml> just some text </saml:AttributeValue>\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonMiddleName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonSurName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "</samlp:AttributeQuery>";

    private static final String attributeRequestWithAttributeValuesXml =
            "<samlp:AttributeQuery\n" +
            "  xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "  xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "  ID=\"aaf23196-1773-2113-474a-fe114412ab72\"\n" +
            "  Destination=\"urn:idmanagement.gov:icam:bae:v2:1:7000:0000\"\n" +
            "  Version=\"2.0\"\n" +
            "  IssueInstant=\"2006-07-17T22:26:40Z\">\n" +
            "  <saml:Issuer>urn:idmanagement.gov:icam:bae:v2:1:2100:1700</saml:Issuer>\n" +
            "  <saml:Subject>\n" +
            "    <saml:NameID\n" +
            "      Format=\"urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n\">\n" +
            "          70001234000002110000000000000000\n" +
            "    </saml:NameID>\n" +
            "  </saml:Subject>\n" +
            "  <saml:Attribute Name=\"nc:PersonGivenName\"\n" +
            "      NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "     <saml:AttributeValue><xml>Jim</xml></saml:AttributeValue>\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonMiddleName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonSurName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "</samlp:AttributeQuery>";

    private static final String attributeRequest_V2 = "" +
            "<samlp:AttributeQuery\n" +
            "  xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "  xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "  ID=\"aaf23196-1773-2113-474a-fe114412ab72\"\n" +
            "  Destination=\"urn:idmanagement.gov:icam:bae:v2:1:7000:0000\"\n" +
            "  Version=\"2.0\"\n" +
            "  IssueInstant=\"2006-07-17T22:26:40Z\">\n" +
            "  <saml:Issuer>urn:idmanagement.gov:icam:bae:v2:1:2100:1700</saml:Issuer>\n" +
            "  <saml:Subject>\n" +
            "    <saml:NameID\n" +
            "      Format=\"urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n\">\n" +
            "          70001234000002110000000000000000\n" +
            "    </saml:NameID>\n" +
            "  </saml:Subject>\n" +
            "  <saml:Attribute Name=\"nc:PersonGivenName\"\n" +
            "      NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonMiddleName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonSurName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "</samlp:AttributeQuery>";

    private static final String attributeRequest_V1 = "" +
            "<samlp:AttributeQuery xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" xmlns:samlp=\"urn:oasis:names:tc:SAML:1.0:protocol\">\n" +
            "    <saml:Subject>\n" +
            "        <saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">Donal</saml:NameIdentifier>\n" +
            "    </saml:Subject>\n" +
            "    <saml:AttributeDesignator AttributeName=\"nc:PersonGivenName\" AttributeNamespace=\"http://uri.com\"/>\n" +
            "    <saml:AttributeDesignator AttributeName=\"nc:PersonMiddleName\" AttributeNamespace=\"http://uri.com\"/>\n" +
            "    <saml:AttributeDesignator AttributeName=\"nc:PersonSurName\" AttributeNamespace=\"http://uri.com\"/>\n" +
            "</samlp:AttributeQuery>";

    private List<SamlAttributeStatement.Attribute> getSomeAttributes() {
        final SamlAttributeStatement.Attribute middleNameAttr = new SamlAttributeStatement.Attribute();
        middleNameAttr.setName("nc:PersonMiddleName");
        middleNameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        middleNameAttr.setNamespace("http://uri.com");
        middleNameAttr.setValue("Tiberius");

        final SamlAttributeStatement.Attribute lastNameAttr = new SamlAttributeStatement.Attribute();
        lastNameAttr.setName("nc:PersonSurName");
        lastNameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        lastNameAttr.setNamespace("http://uri.com");
        lastNameAttr.setValue("Kirk");

        return Arrays.asList(middleNameAttr, lastNameAttr);
    }

    private ServerSamlIssuerAssertion getServerAssertion(SamlAttributeStatement attributeStatement, int version) throws ServerPolicyException {
        SamlIssuerAssertion assertion = new SamlIssuerAssertion();
        assertion.setAttributeStatement(attributeStatement);
        assertion.setVersion(version);
        assertion.setSignAssertion(false);//don't need

        return new ServerSamlIssuerAssertion(assertion, ApplicationContexts.getTestApplicationContext());
    }

    private Document getIssuedSamlAssertionDoc(PolicyEnforcementContext context) throws NoSuchVariableException, SAXException, IOException {
        final String issuedSamlAssertion = (String) context.getVariable("issuedSamlAssertion");
        Message samlAssertion = new Message(XmlUtil.stringAsDocument(issuedSamlAssertion));
        final Document document = samlAssertion.getXmlKnob().getDocumentReadOnly();
        return document;
    }

    private void addCredentials(PolicyEnforcementContext context) {
        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        final HttpBasicToken basicToken = new HttpBasicToken("testuser", new char[]{'p', 'a', 's', 's'});

        authContext.addCredentials(LoginCredentials.makeLoginCredentials(basicToken, HttpBasic.class));
    }

    private PolicyEnforcementContext getContext() throws Exception {

        Message request = new Message();
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        PolicyEnforcementContext policyEnforcementContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        policyEnforcementContext.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        //set credentials
        addCredentials(policyEnforcementContext);

        return policyEnforcementContext;
    }

    private List<Element> getAttributesFromRequest(String requestXml,
                                                   String samlNamespace,
                                                   String attributeElementName) throws Exception {
        final Document attributeQuery = XmlUtil.parse(requestXml);
        System.out.println(XmlUtil.nodeToFormattedString(attributeQuery));

        final NodeList attrList = attributeQuery.getElementsByTagNameNS(samlNamespace, attributeElementName);

        List<Element> elementList = new ArrayList<Element>();
        for (int i = 0; i < attrList.getLength(); i++) {
            final Node attribute = attrList.item(i);
            elementList.add((Element) attribute);
        }
        return elementList;
    }

    /**
     * Runs the server assertion configured with the Attributes from attributes, filterExpression and context vars
     * in varNameValuePair.
     * Asserts status is NONE and that the number of Attributes added to the created AttributeStatement equals the
     * expected number expectedReturnedAttributes
     *
     * No other Attribute validation is done.
     *
     * @param attributes Attributes to add to assertion.
     * @param filterExpression assertion filter expression
     * @param varNameValuePair all context variables
     * @param samlNamespace namespace for version
     * @param expectedReturnedAttributes number of Attributes expected in created AttributeStatement
     * @param version SAML version
     * @return Document for issued SAML Assertion
     * @throws Exception any exceptions
     */
    private Pair<Document, TestAudit> runTest(List<SamlAttributeStatement.Attribute> attributes,
                         String filterExpression,
                         List<Pair<String, Object>> varNameValuePair,
                         String samlNamespace,
                         int expectedReturnedAttributes,
                         int version) throws Exception {

        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        samlAttributeStatement.setFilterExpression(filterExpression);
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement, version);

        final PolicyEnforcementContext context = getContext();
        for (Pair<String, Object> stringObjectPair : varNameValuePair) {
            context.setVariable(stringObjectPair.left, stringObjectPair.right);
        }

        //set credentials
        addCredentials(context);
        context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, samlNamespace, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, samlNamespace, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", expectedReturnedAttributes, attribute.size());

        return new Pair<Document, TestAudit>(document, testAudit);
    }

    private List<Element> getElementsFromXml(String attributeRequest, String samlNS, String nameAttrName) throws Exception {
        final Document attributeQuery = XmlUtil.parse(attributeRequest);
        System.out.println(XmlUtil.nodeToFormattedString(attributeQuery));
        final NodeList attrList = attributeQuery.getElementsByTagNameNS(samlNS, nameAttrName);
        List<Element> elementList = new ArrayList<Element>();
        for (int i = 0; i < attrList.getLength(); i++) {
            final Node attribute = attrList.item(i);
            elementList.add((Element) attribute);
        }

        return elementList;
    }
}
