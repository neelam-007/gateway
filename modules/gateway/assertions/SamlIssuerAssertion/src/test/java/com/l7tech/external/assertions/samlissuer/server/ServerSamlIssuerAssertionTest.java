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
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;
import org.junit.*;
import org.w3c.dom.Node;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.Calendar;

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
