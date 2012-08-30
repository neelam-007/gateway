package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.identity.mapping.NameFormat;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.security.saml.Attribute;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Pair;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.saml.SamlAssertionV1;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ServerRequireWssSamlTest {

    Message request;
    private SamlAssertionV1 samlAssertionV1;

    @Before
    public void initRequest() throws Exception {
        request = SamlTestUtil.makeSamlRequest(false);
        samlAssertionV1 = new SamlAssertionV1(XmlUtil.stringAsDocument(AUTH_SAML_V1).getDocumentElement(), null);
    }

    @Test
    @BugNumber(5141)
    public void testContextVariableAttr() throws Exception {
        RequireWssSaml ass = SamlTestUtil.configureToAllowAttributeBearer(new RequireWssSaml());
        ServerRequireWssSaml sass = new ServerRequireWssSoapSaml<RequireWssSaml>(ass);
        SamlTestUtil.configureServerAssertionInjects(sass);

        PolicyEnforcementContext context = SamlTestUtil.createWssProcessedContext(request);
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        SamlTestUtil.checkContextVariableResults(context);
    }

    @Test
    @BugNumber(8287)
    public void testComplexAttributeValue() throws Exception {
        RequireWssSaml ass = SamlTestUtil.configureToAllowAttributeBearer(new RequireWssSaml());
        ass.getAttributeStatement().setAttributes(new SamlAttributeStatement.Attribute[] {
                new SamlAttributeStatement.Attribute("Complex", "urn:c1", "urn:f1", null, true, true),
        });
        ass.setRequireHolderOfKeyWithMessageSignature(false);
        ass.setRequireSenderVouchesWithMessageSignature(false);
        ass.setNoSubjectConfirmation(true);
        ServerRequireWssSaml sass = new ServerRequireWssSoapSaml<RequireWssSaml>(ass);
        SamlTestUtil.configureServerAssertionInjects(sass);

        Message request = new Message(XmlUtil.stringAsDocument(SamlTestUtil.SOAPENV));
        WssDecoratorImpl dec = new WssDecoratorImpl();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSecurityHeaderReusable(true);
        Pair<X509Certificate,PrivateKey> key = TestKeys.getCertAndKey("RSA_512");
        dreq.setSecurityHeaderActor(null);
        dreq.setSenderMessageSigningCertificate(key.left);
        dreq.setSenderMessageSigningPrivateKey(key.right);
        dreq.setSenderSamlToken(new SamlAssertionV1(XmlUtil.stringAsDocument(COMPLEX_SAML).getDocumentElement(), null));
        dreq.setPreferredSigningTokenType(DecorationRequirements.PreferredSigningTokenType.X509);
        dreq.setProtectTokens(true);
        dreq.getElementsToSign().add(SoapUtil.getBodyElement(request.getXmlKnob().getDocumentWritable()));
        dec.decorateMessage(request, dreq);

        PolicyEnforcementContext context = SamlTestUtil.createWssProcessedContext(request);
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        Object attr1 = context.getVariable("saml.attr.complex");
        assertTrue("Attributes that are present and validated must set context variables", attr1 instanceof String[]);
        assertEquals("Must have found and validated one attribute", 1, ((String[])attr1).length);
        assertEquals("Attributes that are present and validated must set context variables", ((String[])attr1)[0],
                "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\"><complex>blah <foo></foo>blah</complex><complex>blah <foo></foo>blah</complex></saml:AttributeValue>");
    }

    // Some test coverage for existing authentication matching behavior

    /**
     * Ensures that an known authentication method value matches.
     */
    @Test
    public void testAuthValue_BuiltInAuthMethods_Success() throws Exception {

        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(1);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml11Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setAuthenticationMethods(new String[]{SamlConstants.PASSWORD_AUTHENTICATION});
        requireWssSaml.setAuthenticationStatement(authStmt);

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);

        Message request = getDecoratedMessage(samlAssertionV1);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    /**
     * Ensures that an unknown authentication method value fails.
     */
    @Test
    public void testAuthValue_BuiltInAuthMethods_Failure() throws Exception {

        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(1);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml11Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setAuthenticationMethods(new String[]{SamlConstants.UNSPECIFIED_AUTHENTICATION});
        requireWssSaml.setAuthenticationStatement(authStmt);

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);

        Message request = getDecoratedMessage(samlAssertionV1);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.FALSIFIED, result);
    }

    /**
     * Ensure a custom value will enable the assertion to succeed.
     */
    @Test
    @BugNumber(9657)
    public void testAuthValue_CustomMethods() throws Exception {
        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(1);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml11Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setCustomAuthenticationMethods(SamlConstants.PASSWORD_AUTHENTICATION);
        requireWssSaml.setAuthenticationStatement(authStmt);

        Message request = getDecoratedMessage(samlAssertionV1);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);
        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    /**
     * Ensure assertion fails when there are no built in auth methods chosen and the custom method does not match.
     * @throws Exception
     */
    @Test
    @BugNumber(9657)
    public void testAuthValue_CustomMethods_Failure() throws Exception {
        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(1);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml11Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setAuthenticationMethods(new String[]{SamlConstants.UNSPECIFIED_AUTHENTICATION});
        authStmt.setCustomAuthenticationMethods("nomatch");
        requireWssSaml.setAuthenticationStatement(authStmt);

        Message request = getDecoratedMessage(samlAssertionV1);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);
        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.FALSIFIED, result);
    }

    /**
     * Test that multiple values are split and compared correctly.
     */
    @Test
    @BugNumber(9657)
    public void testAuthValue_CustomMethods_Multiple_Values() throws Exception {
        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(1);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml11Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setAuthenticationMethods(new String[]{SamlConstants.UNSPECIFIED_AUTHENTICATION});
        authStmt.setCustomAuthenticationMethods("urn:oasis:names:tc:SAML:1.0:am:password1 urn:oasis:names:tc:SAML:1.0:am:password");
        requireWssSaml.setAuthenticationStatement(authStmt);

        Message request = getDecoratedMessage(samlAssertionV1);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);
        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    /**
     * Test that multiple values are split and compared correctly.
     */
    @Test
    @BugNumber(9657)
    public void testAuthValue_CustomMethods_Context_Variables() throws Exception {
        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(1);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml11Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setAuthenticationMethods(new String[]{SamlConstants.UNSPECIFIED_AUTHENTICATION});
        authStmt.setCustomAuthenticationMethods("${context_var}");
        requireWssSaml.setAuthenticationStatement(authStmt);

        Message request = getDecoratedMessage(samlAssertionV1);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        context.setVariable("context_var", SamlConstants.PASSWORD_AUTHENTICATION);
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);
        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    @Test
    @BugNumber(10741)
    public void testDefaultValueForNameFormat() throws Exception {
        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(1);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml11Uri()}); //unspecified

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        requireWssSaml.setAttributeStatement(samlAttributeStatement);

        final SamlAssertionV1 samlAssertionV11 = new SamlAssertionV1(XmlUtil.stringAsDocument(SAML_V1_NO_NAME_FORMAT).getDocumentElement(), null);
        Message request = getDecoratedMessage(samlAssertionV11);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);
        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    static Message getDecoratedMessage(SamlAssertion samlAssertion) throws SAXException, InvalidDocumentFormatException, IOException, GeneralSecurityException, DecoratorException {
        Message request = new Message(XmlUtil.stringAsDocument(SamlTestUtil.SOAPENV));
        WssDecoratorImpl dec = new WssDecoratorImpl();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSecurityHeaderReusable(true);
        Pair<X509Certificate,PrivateKey> key = TestKeys.getCertAndKey("RSA_512");
        dreq.setSecurityHeaderActor(null);
        dreq.setSenderMessageSigningCertificate(key.left);
        dreq.setSenderMessageSigningPrivateKey(key.right);
        dreq.setSenderSamlToken(samlAssertion);

        dreq.setPreferredSigningTokenType(DecorationRequirements.PreferredSigningTokenType.X509);
        dreq.setProtectTokens(true);
        dreq.getElementsToSign().add(SoapUtil.getBodyElement(request.getXmlKnob().getDocumentWritable()));
        dec.decorateMessage(request, dreq);
        return request;
    }

    public static final String COMPLEX_ATTR_VALUE = "<complex xmlns=\"urn:c1\">blah <foo/>blah</complex>";

    public static final String COMPLEX_SAML =
            "            <saml:Assertion\n" +
            "                AssertionID=\"SamlAssertion-b969eafaf40c222320a1276baf516d56\"\n" +
            "                IssueInstant=\"2010-03-09T06:36:08.140Z\" Issuer=\"Bob\"\n" +
            "                MajorVersion=\"1\" MinorVersion=\"1\" xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\">\n" +
            "                <saml:Conditions NotBefore=\"2010-03-09T06:34:08.000Z\" NotOnOrAfter=\"2010-03-09T06:41:08.493Z\"/>\n" +
            "                <saml:AttributeStatement>\n" +
            "                    <saml:Subject>\n" +
            "                        <saml:NameIdentifier\n" +
            "                            Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\"\n" +
            "                            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"/>\n" +
            "                    </saml:Subject>\n" +
            "                    <saml:Attribute AttributeName=\"Complex\" AttributeNamespace=\"urn:c1\">\n" +
            "                        <saml:AttributeValue>" + COMPLEX_ATTR_VALUE + COMPLEX_ATTR_VALUE + "</saml:AttributeValue>\n" +
            "                    </saml:Attribute>\n" +
            "                </saml:AttributeStatement></saml:Assertion>";

    public static final String AUTH_SAML_V1 ="            " +
            "           <saml:Assertion AssertionID=\"SSB-SamlAssertion-5a235a525cda2866c7dc4d56f3891c87\"\n" +
            "                            IssueInstant=\"2011-10-12T01:06:12.118Z\" Issuer=\"donal\" MajorVersion=\"1\" MinorVersion=\"1\"\n" +
            "                            xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\">\n" +
            "                <saml:Conditions NotBefore=\"2011-10-12T01:04:12.259Z\" NotOnOrAfter=\"2011-10-12T01:11:12.259Z\"/>\n" +
            "                <saml:AuthenticationStatement AuthenticationInstant=\"2011-10-12T01:06:12.111Z\"\n" +
            "                                              AuthenticationMethod=\"urn:oasis:names:tc:SAML:1.0:am:password\">\n" +
            "                    <saml:Subject>\n" +
            "                        <saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">admin\n" +
            "                        </saml:NameIdentifier>\n" +
            "                    </saml:Subject>\n" +
            "                    <saml:SubjectLocality IPAddress=\"10.7.48.207\"/>\n" +
            "                </saml:AuthenticationStatement>\n" +
            "            </saml:Assertion>";

    private static final String SAML_V1_NO_NAME_FORMAT = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\"\n" +
            "                AssertionID=\"SamlAssertion-ed95286e69d85530b85cbcd5ee56166c\" Issuer=\"irishman2.l7tech.local\"\n" +
            "                IssueInstant=\"2012-05-11T17:55:02.061Z\">\n" +
            "    <saml:Conditions NotBefore=\"2012-05-11T17:53:02.061Z\" NotOnOrAfter=\"2012-05-11T18:00:02.061Z\"/>\n" +
            "    <saml:AttributeStatement>\n" +
            "        <saml:Subject>\n" +
            "            <saml:NameIdentifier NameQualifier=\"\"/>\n" +
            "        </saml:Subject>\n" +
            "        <saml:Attribute AttributeName=\"one\" AttributeNamespace=\"\">\n" +
            "            <saml:AttributeValue>one</saml:AttributeValue>\n" +
            "        </saml:Attribute>\n" +
            "    </saml:AttributeStatement>\n" +
            "</saml:Assertion>";
}
