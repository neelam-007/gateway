/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.external.assertions.samlissuer.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlissuer.SamlIssuerAssertion;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
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
import com.l7tech.test.BugNumber;
import com.l7tech.xml.saml.SamlAssertionV2;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.*;
import org.junit.*;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML2.AttributeStatementType;
import x0Assertion.oasisNamesTcSAML2.AttributeType;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

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
        addCredentials(context);
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
        Assert.assertTrue("Issue instant must be before or equal to notBeforeDate", issueInstantCal.before(notBeforeDate) || issueInstantCal.equals(notBeforeDate));

        //not part of bug, just adding sanity check
        final String notOnOrAfter = conditionsNode.getAttributes().getNamedItem("NotOnOrAfter").getNodeValue();
        final Calendar notOnOrAfterDate = DatatypeConverter.parseDate(notOnOrAfter);
        System.out.println(notOnOrAfter + " not on or after");
        Assert.assertTrue("Not on or after must be after not before date.", notBeforeDate.before(notOnOrAfterDate));
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
        addCredentials(context);
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
        Assert.assertTrue("Issue instant must be before or equal to notBeforeDate", issueInstantCal.before(notBeforeDate) || issueInstantCal.equals(notBeforeDate));

        //not part of bug, just adding sanity check
        final String notOnOrAfter = conditionsNode.getAttributes().getNamedItem("NotOnOrAfter").getNodeValue();
        final Calendar notOnOrAfterDate = DatatypeConverter.parseDate(notOnOrAfter);
        System.out.println(notOnOrAfter + " not on or after");
        Assert.assertTrue("Not on or after must be after not before date.", notBeforeDate.before(notOnOrAfterDate));

        //test subject confirmation not before date
        final Node subjectConfirmationDataNode = document.getElementsByTagNameNS(SamlConstants.NS_SAML2, "SubjectConfirmationData").item(0);
        final String subConfNotBefore = subjectConfirmationDataNode.getAttributes().getNamedItem("NotBefore").getNodeValue();
        final Calendar subConfNotBeforeCal = DatatypeConverter.parseDate(subConfNotBefore);
        System.out.println(subConfNotBefore + " subject confirmation data not before");
        Assert.assertTrue("Subject confirmation data's not before should be after the issue instant.", issueInstantCal.before(subConfNotBeforeCal) || issueInstantCal.equals(subConfNotBeforeCal));
    }

    // Basic test coverage for existing support - Attributes are added to an AttributeStatement

    /**
     * Test static attributes configuration. Confirms the following:
     * <ul>
     *     <li>If no attributes, then AttributeStatement is empty.</li>
     *     <li>If static attributes configured, they are added to the AttributeStatement.</li>
     * </ul>
     *
     */
    @Test
    public void testIssuedSamlAssertion_V2() throws Exception {
        {
            //Test no Attributes added. Not supported via UI, but supported on Server side.
            final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement);
            final PolicyEnforcementContext context = getContext();

            //set credentials
            addCredentials(context);
            context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            //Verify AttributeStatement added with no Attributes
            final Node attributeStatement = document.getElementsByTagNameNS(SamlConstants.NS_SAML2, "AttributeStatement").item(0);
            Assert.assertEquals("No child Attribute elements (or any) expected.", 0, attributeStatement.getChildNodes().getLength());
        }

        {
            //Test basic Attribute + AttributeValue addition to AttributeStatement
            final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
            List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

            final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
            nameAttr.setName("nc:PersonGivenName");
            nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
            nameAttr.setValue("James");

            attributes.add(nameAttr);

            samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
            ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement);
            final PolicyEnforcementContext context = getContext();

            //set credentials
            addCredentials(context);
            context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

            final Document document = getIssuedSamlAssertionDoc(context);
            System.out.println(XmlUtil.nodeToFormattedString(document));

            //Verify AttributeStatement added with no Attributes
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

//            final Node domNode = attributeValueArray[0].getDomNode();
//            System.out.println("Printing Dom Node");
//            System.out.println(XmlUtil.nodeToFormattedString(domNode));

        }
    }

    /**
     * Test that the attributes added to an AttributeStatement are correctly filtered for a set of requested
     * attributes supplied as Element references.
     */
    @Test
    public void testAttriubuteStatement_Filtered_V2_Elements() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("James");

        final SamlAttributeStatement.Attribute middleNameAttr = new SamlAttributeStatement.Attribute();
        middleNameAttr.setName("nc:PersonMiddleName");
        middleNameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        middleNameAttr.setValue("Tiberius");

        final SamlAttributeStatement.Attribute lastNameAttr = new SamlAttributeStatement.Attribute();
        lastNameAttr.setName("nc:PersonSurName");
        lastNameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        lastNameAttr.setValue("Kirk");

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.add(middleNameAttr);
        attributes.add(lastNameAttr);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement);

        final PolicyEnforcementContext context = getContext();
        // add filter expression variables.

        final Document attributeQuery = XmlUtil.parse(attributeRequest);
        System.out.println(XmlUtil.nodeToFormattedString(attributeQuery));

        //Verify AttributeStatement added with no Attributes
        final NodeList attrList = attributeQuery.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Attribute");

        List<Element> elementList = new ArrayList<Element>();
        for (int i = 0; i < attrList.getLength(); i++) {
            final Node attribute = attrList.item(i);
            elementList.add((Element) attribute);
        }

        context.setVariable(varName, elementList);

        //set credentials
        addCredentials(context);
        context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 3, attribute.size());

        //todo validate contents
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

        final SamlAttributeStatement.Attribute middleNameAttr = new SamlAttributeStatement.Attribute();
        middleNameAttr.setName("nc:PersonMiddleName");
        middleNameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        middleNameAttr.setValue("Tiberius");

        final SamlAttributeStatement.Attribute lastNameAttr = new SamlAttributeStatement.Attribute();
        lastNameAttr.setName("nc:PersonSurName");
        lastNameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        lastNameAttr.setValue("Kirk");

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.add(middleNameAttr);
        attributes.add(lastNameAttr);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        final String middleNameVar = "MiddleName";
        samlAttributeStatement.setFilterExpression("${" + varName + "} ${" + middleNameVar + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement);

        final PolicyEnforcementContext context = getContext();
        // add filter expression variables.

        final Document attributeQuery = XmlUtil.parse(attributeRequest);
        System.out.println(XmlUtil.nodeToFormattedString(attributeQuery));

        //Verify AttributeStatement added with no Attributes
        final NodeList attrList = attributeQuery.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Attribute");

        List<Element> elementList = new ArrayList<Element>();
        elementList.add((Element) attrList.item(0)); // just add the first.

        context.setVariable(varName, elementList);

        //add another a as message variable
        String attributeMiddleName =
        "  <saml:Attribute Name=\"nc:PersonMiddleName\"\n" +
        "       xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
        "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
        "  </saml:Attribute>";

        Message mVar = new Message(XmlUtil.parse(attributeMiddleName));
        context.setVariable(middleNameVar, mVar);

        //set credentials
        addCredentials(context);
        context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 2, attribute.size());

        //todo validate contents
    }

    /**
     * Test that the attributes added to an AttributeStatement are correctly filtered for a set of requested
     * attributes.
     * This test case ensures that incoming AttributeValue are respected and that only matching values are returned.
     * TODO - Remove @ignore when AttributeValue comparision is implemented.
     */
    @Test
    @Ignore
    public void testAttriubuteStatement_Filtered_V2_AttributeValue_Match() throws Exception {
        //Test basic Attribute + AttributeValue addition to AttributeStatement
        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        List<SamlAttributeStatement.Attribute> attributes = new ArrayList<SamlAttributeStatement.Attribute>();

        // Statically configure 3 attributes
        final SamlAttributeStatement.Attribute nameAttr = new SamlAttributeStatement.Attribute();
        nameAttr.setName("nc:PersonGivenName");
        nameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameAttr.setValue("Name does not match request so this attribute should be filtered out");

        final SamlAttributeStatement.Attribute middleNameAttr = new SamlAttributeStatement.Attribute();
        middleNameAttr.setName("nc:PersonMiddleName");
        middleNameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        middleNameAttr.setValue("Tiberius");

        final SamlAttributeStatement.Attribute lastNameAttr = new SamlAttributeStatement.Attribute();
        lastNameAttr.setName("nc:PersonSurName");
        lastNameAttr.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        lastNameAttr.setValue("Kirk");

        //This is the attribute we expect to not be included in the created saml token
        final SamlAttributeStatement.Attribute attributeNotInRequest = new SamlAttributeStatement.Attribute();
        attributeNotInRequest.setName("not_in_request");
        attributeNotInRequest.setNameFormat(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        attributeNotInRequest.setValue("not_in_request_value");

        attributes.add(nameAttr);
        attributes.add(middleNameAttr);
        attributes.add(lastNameAttr);
        attributes.add(attributeNotInRequest);

        samlAttributeStatement.setAttributes(attributes.toArray(new SamlAttributeStatement.Attribute[attributes.size()]));
        final String varName = "attributeQuery.attributes";
        samlAttributeStatement.setFilterExpression("${" + varName + "}");
        ServerSamlIssuerAssertion serverAssertion = getServerAssertion(samlAttributeStatement);

        final PolicyEnforcementContext context = getContext();
        // add filter expression variables.

        final Document attributeQuery = XmlUtil.parse(attributeRequestWithAttributeValue);
        System.out.println(XmlUtil.nodeToFormattedString(attributeQuery));

        //Verify AttributeStatement added with no Attributes
        final NodeList attrList = attributeQuery.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Attribute");

        List<Element> elementList = new ArrayList<Element>();
        for (int i = 0; i < attrList.getLength(); i++) {
            final Node attribute = attrList.item(i);
            elementList.add((Element) attribute);
        }

        context.setVariable(varName, elementList);

        //set credentials
        addCredentials(context);
        context.getRequest().initialize(XmlUtil.parse("<xml></xml>"));

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be none.", AssertionStatus.NONE, status);

        final Document document = getIssuedSamlAssertionDoc(context);
        System.out.println(XmlUtil.nodeToFormattedString(document));

        final Element assertionElm = document.getDocumentElement();
        final Element attrStatement = XmlUtil.findFirstChildElementByName(assertionElm, SamlConstants.NS_SAML2, "AttributeStatement");
        final List<Element> attribute = XmlUtil.findChildElementsByName(attrStatement, SamlConstants.NS_SAML2, "Attribute");
        Assert.assertEquals("Incorrect number of Attributes found in AttributeStatement", 2, attribute.size());
    }

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

    private static final String attributeRequest = "" +
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

    private ServerSamlIssuerAssertion getServerAssertion(SamlAttributeStatement attributeStatement) throws ServerPolicyException {
        SamlIssuerAssertion assertion = new SamlIssuerAssertion();
        assertion.setAttributeStatement(attributeStatement);
        assertion.setVersion(2);
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
}
