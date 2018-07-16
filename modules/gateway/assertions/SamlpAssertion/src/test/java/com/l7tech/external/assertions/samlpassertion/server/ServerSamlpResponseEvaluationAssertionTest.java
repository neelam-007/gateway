package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by gurra04 on 3/7/2017.
 */
public class ServerSamlpResponseEvaluationAssertionTest {

    private static ApplicationContext appContext;
    private PolicyEnforcementContext policyContext;

    @BeforeClass
    public static void beforeClass() {
        appContext = ApplicationContexts.getTestApplicationContext();
    }

    @Before
    public void beforeTest() throws Exception {
        policyContext = getContext();
    }

    @Test
    public void testEvaluateSaml11ResponseForNonNilAttributes_Success() throws Exception {

        policyContext.setVariable("samlMsg", new Message(XmlUtil.parse(samlToken_1_1)));
        buildSamlResponse("${samlMsg}", SamlVersion.SAML1_1);

        final SamlpResponseEvaluationAssertion assertion = new SamlpResponseEvaluationAssertion();
        assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());
        assertion.setTarget(TargetMessageType.RESPONSE);
        assertion.setResponseStatusFalsifyAssertion(false);
        assertion.setAttributeStatement(new SamlAttributeStatement(
                new SamlAttributeStatement.Attribute("ssostartpage", "", "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified", null, true, true)
        ));

        final ServerSamlpResponseEvaluationAssertion serverAssertion = new ServerSamlpResponseEvaluationAssertion(assertion);
        final AssertionStatus status = serverAssertion.checkRequest(policyContext);

        Object[] vals = (Object[])policyContext.getVariable("samlpResponse.attribute.ssostartpage");

        assertEquals("Status should be NONE", AssertionStatus.NONE, status);
        assertEquals("http://irishman:8080/web_sso", vals[0]);
    }

    @Test
    public void testEvaluateSaml20ResponseForNonNilAttributes_Success() throws Exception {

        policyContext.setVariable("samlMsg", new Message(XmlUtil.parse(samlToken_2_0)));
        buildSamlResponse("${samlMsg}", SamlVersion.SAML2);

        final SamlpResponseEvaluationAssertion assertion = new SamlpResponseEvaluationAssertion();
        assertion.setVersion(SamlVersion.SAML2.getVersionInt());
        assertion.setTarget(TargetMessageType.RESPONSE);
        assertion.setResponseStatusFalsifyAssertion(false);
        assertion.setAttributeStatement(new SamlAttributeStatement(
                new SamlAttributeStatement.Attribute("ssostartpage", "", "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified", null, true, true)
        ));

        final ServerSamlpResponseEvaluationAssertion serverAssertion = new ServerSamlpResponseEvaluationAssertion(assertion);
        final AssertionStatus status = serverAssertion.checkRequest(policyContext);

        Object[] vals = (Object[])policyContext.getVariable("samlpResponse.attribute.ssostartpage");

        assertEquals("Status should be NONE", AssertionStatus.NONE, status);
        assertEquals("http://irishman:8080/web_sso", vals[0]);
    }

    @Test
    public void testEvaluateSaml11ResponseForNilAttributes_PolicyFalsified() throws Exception {

        policyContext.setVariable("samlMsg", new Message(XmlUtil.parse(samlToken_1_1)));
        buildSamlResponse("${samlMsg}", SamlVersion.SAML1_1);

        final SamlpResponseEvaluationAssertion assertion = new SamlpResponseEvaluationAssertion();
        assertion.setVersion(SamlVersion.SAML1_1.getVersionInt());
        assertion.setTarget(TargetMessageType.RESPONSE);
        assertion.setResponseStatusFalsifyAssertion(false);
        assertion.setAttributeStatement(new SamlAttributeStatement(
                new SamlAttributeStatement.Attribute("ssostartpage", "", "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified", null, true, true),
                new SamlAttributeStatement.Attribute("ssonilpage", "", "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified", null, true, true)
        ));

        final ServerSamlpResponseEvaluationAssertion serverAssertion = new ServerSamlpResponseEvaluationAssertion(assertion);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );
        final AssertionStatus status = serverAssertion.checkRequest(policyContext);

        // SAML1.1 : There's no strict check over NIL values. Hence, expected status is NONE, i.e., SUCCESS.
        assertEquals("Status should be NONE", AssertionStatus.NONE, status);
    }

    @Test
    public void testEvaluateSaml20ResponseForNilAttributes_PolicyFalsified() throws Exception {

        policyContext.setVariable("samlMsg", new Message(XmlUtil.parse(samlToken_2_0)));
        buildSamlResponse("${samlMsg}", SamlVersion.SAML2);

        final SamlpResponseEvaluationAssertion assertion = new SamlpResponseEvaluationAssertion();
        assertion.setVersion(SamlVersion.SAML2.getVersionInt());
        assertion.setTarget(TargetMessageType.RESPONSE);
        assertion.setResponseStatusFalsifyAssertion(false);
        assertion.setAttributeStatement(new SamlAttributeStatement(
                new SamlAttributeStatement.Attribute("ssostartpage", "", "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified", null, true, true),
                new SamlAttributeStatement.Attribute("ssonilpage", "", "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified", null, true, true)
        ));

        final ServerSamlpResponseEvaluationAssertion serverAssertion = new ServerSamlpResponseEvaluationAssertion(assertion);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );
        final AssertionStatus status = serverAssertion.checkRequest(policyContext);

        // SAML2.0: There's strict check over NIL values. Hence, expected status is FALSIFIED.
        assertEquals("Status should be Falsified", AssertionStatus.FALSIFIED, status);
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

    private void buildSamlResponse(String samlMsgVarExpression, SamlVersion version) throws Exception {
        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        assertion.setSignResponse(false);
        assertion.setTarget(TargetMessageType.RESPONSE);

        assertion.setVersion(version.getVersionInt());

        assertion.setStatusMessage("Status Message is ok");
        if (version == SamlVersion.SAML1_1) {
            assertion.setSamlStatusCode(SamlStatus.SAML_SUCCESS.getValue());
        }

        assertion.setResponseAssertions(samlMsgVarExpression);
        assertion.setRecipient("http://recipient.com");

        ServerSamlpResponseBuilderAssertion serverAssertion = new ServerSamlpResponseBuilderAssertion(assertion, appContext);
        serverAssertion.checkRequest(policyContext);
    }

    private final static String samlToken_1_1 = "  <saml:Assertion xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" AssertionID=\"SamlAssertion-94b99f0213cdf988f1931cd55aa91232\" IssueInstant=\"2010-07-12T23:30:29.274Z\" Issuer=\"irishman.l7tech.com\" MajorVersion=\"1\" MinorVersion=\"1\">\n" +
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
            "      <saml:Attribute AttributeName=\"ssonilpage\" AttributeNamespace=\"\">\n" +
            "        <saml:AttributeValue xsi:nil=\"true\"/>\n" +
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

    private final static String samlToken_2_0 = "<saml2:Assertion xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"SamlAssertion-9190940a89f8a5fa9837642a35d01f9a\" IssueInstant=\"2010-08-04T16:54:02.494Z\" Version=\"2.0\">\n" +
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
            "      <saml2:Attribute Name=\"ssonilpage\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\">\n" +
            "        <saml2:AttributeValue xsi:nil=\"true\"/>\n" +
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
