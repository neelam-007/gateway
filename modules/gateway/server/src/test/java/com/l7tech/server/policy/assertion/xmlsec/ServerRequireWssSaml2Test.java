package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.mapping.NameFormat;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.Pair;
import com.l7tech.xml.saml.SamlAssertionV2;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ServerRequireWssSaml2Test {

    Message request;
    private SamlAssertionV2 samlAssertionV2;

    @Before
    public void initRequest() throws Exception {
        request = SamlTestUtil.makeSamlRequest(true);
        samlAssertionV2 = new SamlAssertionV2(XmlUtil.stringAsDocument(AUTH_SAML_V2).getDocumentElement(), null);
    }

    @Test
    @BugNumber(5141)
    public void testContextVariableAttr() throws Exception {
        RequireWssSaml2 ass = SamlTestUtil.configureToAllowAttributeBearer(new RequireWssSaml2());
        ServerRequireWssSaml2 sass = new ServerRequireWssSaml2(ass);
        SamlTestUtil.configureServerAssertionInjects(sass);

        PolicyEnforcementContext context = SamlTestUtil.createWssProcessedContext(request);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        SamlTestUtil.checkContextVariableResults(context);
    }


    @Test
    @BugNumber(8287)
    public void testComplexAttributeValue() throws Exception {
        RequireWssSaml2 ass = SamlTestUtil.configureToAllowAttributeBearer(new RequireWssSaml2());
        ass.getAttributeStatement().setAttributes(new SamlAttributeStatement.Attribute[] {
                new SamlAttributeStatement.Attribute("Complex", "urn:c1", "urn:f1", null, true, true),
        });
        ass.setRequireHolderOfKeyWithMessageSignature(false);
        ass.setRequireSenderVouchesWithMessageSignature(false);
        ass.setNoSubjectConfirmation(true);
        ServerRequireWssSaml sass = new ServerRequireWssSaml2(ass);
        SamlTestUtil.configureServerAssertionInjects(sass);


        Message request = new Message(XmlUtil.stringAsDocument(SamlTestUtil.SOAPENV));
        WssDecoratorImpl dec = new WssDecoratorImpl();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSecurityHeaderReusable(true);
        Pair<X509Certificate,PrivateKey> key = TestKeys.getCertAndKey("RSA_512");
        dreq.setSecurityHeaderActor(null);
        dreq.setSenderMessageSigningCertificate(key.left);
        dreq.setSenderMessageSigningPrivateKey(key.right);
        dreq.setSenderSamlToken(new SamlAssertionV2(XmlUtil.stringAsDocument(COMPLEX_SAML).getDocumentElement(), null));
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
            "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"><complex>blah <foo></foo>blah</complex><complex>blah <foo></foo>blah</complex></saml:AttributeValue>");
    }

    /**
     *  Test if the SAML token is expired against the maximum expiry time (or lifetime).
     *
     * @throws Exception
     */
    @Test
    @BugNumber(8866)
    public void testMaximumExpiryTime() throws Exception {
        final Calendar now = Calendar.getInstance(SamlAssertionValidate.UTC_TIME_ZONE);

        // Case 1: SAML Token Not Expired
        String issueInstant = ISO8601Date.format(offsetTime(now, -60000).getTime()); // 1 minute before "now"
        String notBefore = ISO8601Date.format(offsetTime(now, -30000).getTime());    // 30 seconds before "now"
        String notOnOrAfter = ISO8601Date.format(offsetTime(now, 60000).getTime());  // 1 minute after "now"

        AssertionStatus result = verifyExpiration(issueInstant, notBefore, notOnOrAfter, true, 90000); // 90 seconds after IssueInstant => 30 seconds after "now"
        assertEquals("SAML Token is not expired.", AssertionStatus.NONE, result);

        // Case 2: SAML Toke Expired
        result = verifyExpiration(issueInstant, notBefore, notOnOrAfter, true, 50000); // 50 seconds after IssueInstant => 10 seconds before "now"
        assertEquals("SAML Token is expired.", AssertionStatus.FALSIFIED, result);

        // Case 3: Zero "Maximum Expiry Time"
        notOnOrAfter = ISO8601Date.format(offsetTime(now, -50000).getTime()); // 50 seconds before "now"

        result = verifyExpiration(issueInstant, notBefore, notOnOrAfter, true, 0);
        assertEquals("SAML Token is invalid, even thought \"Maximum Expiry Time\" is set zero.", AssertionStatus.FALSIFIED, result);
        
        result = verifyExpiration(issueInstant, notBefore, notOnOrAfter, false, 0);
        assertEquals("Checking SAML Token Expiration is ignored.", AssertionStatus.NONE, result);
    }

    /**
     * Note this test will not always fail when it should, but it will eventually if the evaluation of IssueInstant
     * is modified such that milli second values are not considered.
     */
    @Test
    @BugNumber(11144)
    public void testIssueInstantEvaluationSupportsMilliSeconds() throws Exception {
        final Calendar now = Calendar.getInstance(SamlAssertionValidate.UTC_TIME_ZONE);

        String issueInstant = ISO8601Date.format(now.getTime());
        String notBefore = ISO8601Date.format(offsetTime(now, -30000).getTime());    // simply a valid value for this test
        String notOnOrAfter = ISO8601Date.format(offsetTime(now, 60000).getTime());  // simply a valid value for this test

        // Validate that the assertion is valid when the issue instance is likely to be on the same second as the time
        // when the token is evaluated in checkRequest
        AssertionStatus result = verifyExpiration(issueInstant, notBefore, notOnOrAfter, true, 90000);
        assertEquals("Token should be valid", AssertionStatus.NONE, result);

    }

    // Some test coverage for existing authentication matching behavior

    /**
     * Ensures that an known authentication method value matches.
     */
    @Test
    public void testAuthValue_BuiltInAuthMethods_Success() throws Exception {

        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(2);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml20Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setAuthenticationMethods(new String[]{SamlConstants.AUTHENTICATION_SAML2_PASSWORD});
        requireWssSaml.setAuthenticationStatement(authStmt);

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);

        Message request = ServerRequireWssSamlTest.getDecoratedMessage(samlAssertionV2);
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
        requireWssSaml.setVersion(2);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml20Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setAuthenticationMethods(new String[]{SamlConstants.UNSPECIFIED_AUTHENTICATION});
        requireWssSaml.setAuthenticationStatement(authStmt);

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);

        Message request = ServerRequireWssSamlTest.getDecoratedMessage(samlAssertionV2);
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
        requireWssSaml.setVersion(2);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml20Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setCustomAuthenticationMethods(SamlConstants.AUTHENTICATION_SAML2_PASSWORD);
        requireWssSaml.setAuthenticationStatement(authStmt);

        Message request = ServerRequireWssSamlTest.getDecoratedMessage(samlAssertionV2);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);
        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    /**
     * Ensure assertion fails when there are no built in auth methods chosen and the custom method does not match.
     */
    @Test
    @BugNumber(9657)
    public void testAuthValue_CustomMethods_Failure() throws Exception {
        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(2);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml20Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setAuthenticationMethods(new String[]{SamlConstants.UNSPECIFIED_AUTHENTICATION});
        authStmt.setCustomAuthenticationMethods("nomatch");
        requireWssSaml.setAuthenticationStatement(authStmt);

        Message request = ServerRequireWssSamlTest.getDecoratedMessage(samlAssertionV2);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);
        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.FALSIFIED, result);
    }

    /**
     * Test that multiple values are split and compared correctly.
     * @throws Exception
     */
    @Test
    @BugNumber(9657)
    public void testAuthValue_CustomMethods_Multiple_Values() throws Exception {
        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(2);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml20Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setAuthenticationMethods(new String[]{SamlConstants.UNSPECIFIED_AUTHENTICATION});
        authStmt.setCustomAuthenticationMethods("urn:oasis:names:tc:SAML:2.0:ac:classes:Password1 urn:oasis:names:tc:SAML:2.0:ac:classes:Password");
        requireWssSaml.setAuthenticationStatement(authStmt);

        Message request = ServerRequireWssSamlTest.getDecoratedMessage(samlAssertionV2);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);
        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    /**
     * Test that multiple values are split and compared correctly.
     * @throws Exception
     */
    @Test
    @BugNumber(9657)
    public void testAuthValue_CustomMethods_Context_Variables() throws Exception {
        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(2);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml20Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        authStmt.setAuthenticationMethods(new String[]{SamlConstants.UNSPECIFIED_AUTHENTICATION});
        authStmt.setCustomAuthenticationMethods("${context_var}");
        requireWssSaml.setAuthenticationStatement(authStmt);

        Message request = ServerRequireWssSamlTest.getDecoratedMessage(samlAssertionV2);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        context.setVariable("context_var", SamlConstants.AUTHENTICATION_SAML2_PASSWORD);
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);
        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    /**
     * Note: this bug did not affect SAML 2.0, however this test validates the current behavior.
     * @throws Exception
     */
    @Test
    @BugNumber(10741)
    public void testDefaultValueForNameFormat() throws Exception {
        final RequireWssSaml requireWssSaml = new RequireWssSaml();
        requireWssSaml.setVersion(2);
        requireWssSaml.setCheckAssertionValidity(false);
        requireWssSaml.setNameFormats(new String[] {NameFormat.OTHER.getSaml20Uri()});

        requireWssSaml.setRequireHolderOfKeyWithMessageSignature(false);
        requireWssSaml.setRequireSenderVouchesWithMessageSignature(false);
        requireWssSaml.setNoSubjectConfirmation(true);

        final SamlAttributeStatement samlAttributeStatement = new SamlAttributeStatement();
        requireWssSaml.setAttributeStatement(samlAttributeStatement);

        Message request = ServerRequireWssSamlTest.getDecoratedMessage(new SamlAssertionV2(XmlUtil.stringAsDocument(SAML_V2_NO_NAME_FORMAT).getDocumentElement(), null));
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        System.out.println("Req: " + XmlUtil.nodeToFormattedString(context.getRequest().getXmlKnob().getDocumentReadOnly()));

        ServerRequireWssSaml serverRequireWssSaml = new ServerRequireWssSoapSaml<RequireWssSaml>(requireWssSaml);
        SamlTestUtil.configureServerAssertionInjects(serverRequireWssSaml);
        AssertionStatus result = serverRequireWssSaml.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    private AssertionStatus verifyExpiration(String issueInstant, String notBefore, String notOnOrAfter, boolean checkAssertionValidity, int maxExpiryTime) throws Exception {
        // Create doc
        Document samlAssertionDoc = XmlUtil.stringToDocument(buildSamlDocWithDynamicTime(issueInstant, notBefore, notOnOrAfter));
        System.out.println("Testing SAML Token Expiration: \n" + XmlUtil.nodeToFormattedString(samlAssertionDoc));

        RequireWssSaml2 ass = SamlTestUtil.configureToAllowAttributeBearer(new RequireWssSaml2());
        ass.getAttributeStatement().setAttributes(new SamlAttributeStatement.Attribute[] {
                new SamlAttributeStatement.Attribute("Complex", "urn:c1", "urn:f1", null, true, true),
        });
        ass.setRequireHolderOfKeyWithMessageSignature(false);
        ass.setRequireSenderVouchesWithMessageSignature(false);
        ass.setNoSubjectConfirmation(true);
        ass.setCheckAssertionValidity(checkAssertionValidity);
        ass.setMaxExpiry(maxExpiryTime);
        ServerRequireWssSaml sass = new ServerRequireWssSaml2(ass);
        SamlTestUtil.configureServerAssertionInjects(sass);

        Message request = new Message(XmlUtil.stringAsDocument(SamlTestUtil.SOAPENV));
        WssDecoratorImpl dec = new WssDecoratorImpl();
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSecurityHeaderReusable(true);
        Pair<X509Certificate,PrivateKey> key = TestKeys.getCertAndKey("RSA_512");
        dreq.setSecurityHeaderActor(null);
        dreq.setSenderMessageSigningCertificate(key.left);
        dreq.setSenderMessageSigningPrivateKey(key.right);
        dreq.setSenderSamlToken(new SamlAssertionV2(samlAssertionDoc.getDocumentElement(), null));
        dreq.setPreferredSigningTokenType(DecorationRequirements.PreferredSigningTokenType.X509);
        dreq.setProtectTokens(true);
        dreq.getElementsToSign().add(SoapUtil.getBodyElement(request.getXmlKnob().getDocumentWritable()));
        dec.decorateMessage(request, dreq);

        PolicyEnforcementContext context = SamlTestUtil.createWssProcessedContext(request);

        return sass.checkRequest(context);
    }

    /**
     *  Build a SAML document with customized time values, such as IssueInstant, NotBefore, and NotOnOrAfter.
     *
     * @param issueInstant: the given time value of IssueInstant
     * @param notBefore: the given time value of NotBefore
     * @param notOnOrAfter: the given time value of NotOnOrAfter
     * @return A SAML document with given customized time values.
     */
    private String buildSamlDocWithDynamicTime(String issueInstant, String notBefore, String notOnOrAfter) {
        return "<saml:Assertion ID=\"BinarySecurityToken-0-7e8a1451b089b9c30c92ccd3e702494b\" IssueInstant=\"" + issueInstant + "\" Version=\"2.0\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "    <saml:Issuer>Service Provider</saml:Issuer>\n" +
            "    <saml:Subject>\n" +
            "        <saml:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=John Smith, OU=Java Technology Center, O=IBM, L=Cupertino, ST=California, C=US</saml:NameID>\n" +
            "    </saml:Subject>\n" +
            "    <saml:Conditions NotBefore=\"" + notBefore + "\" NotOnOrAfter=\"" + notOnOrAfter + "\"/>" +
            "    <saml:AttributeStatement>\n" +
            "        <saml:Attribute Name=\"Complex\" NameFormat=\"urn:f1\"><saml:AttributeValue>" + COMPLEX_ATTR_VALUE + COMPLEX_ATTR_VALUE + "</saml:AttributeValue></saml:Attribute>\n" +
            "    </saml:AttributeStatement>\n" +
            "</saml:Assertion>";
    }

    /**
     * Create a new time by applying the offset.  If offset is positive, then the new time will be later than the original time.
     *  If offset is negative, then the new time will be earlier than the orginal time.
     *
     * @param originalCalendar: the original time
     * @param offsetInMilliSeconds: the offset value in milliseconds and could be a positive or negative integer.
     * @return a new time after offset
     */
    private Calendar offsetTime(Calendar originalCalendar, int offsetInMilliSeconds) {
        Calendar newCalendar = (Calendar) originalCalendar.clone();
        newCalendar.add(Calendar.MILLISECOND, offsetInMilliSeconds);
        return newCalendar;
    }

    public static final String COMPLEX_ATTR_VALUE = "<complex xmlns=\"urn:c1\">blah <foo/>blah</complex>";

    public static final String COMPLEX_SAML =
            "<saml:Assertion ID=\"BinarySecurityToken-0-7e8a1451b089b9c30c92ccd3e702494b\" IssueInstant=\"2006-06-27T23:15:30.040Z\" Version=\"2.0\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
                    "    <saml:Issuer>Service Provider</saml:Issuer>\n" +
                    "    <saml:Subject>\n" +
                    "        <saml:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=John Smith, OU=Java Technology Center, O=IBM, L=Cupertino, ST=California, C=US</saml:NameID>\n" +
                    "    </saml:Subject>\n" +
                    "    <saml:AttributeStatement>\n" +
                    "        <saml:Attribute Name=\"Complex\" NameFormat=\"urn:f1\"><saml:AttributeValue>" + COMPLEX_ATTR_VALUE + COMPLEX_ATTR_VALUE + "</saml:AttributeValue></saml:Attribute>\n" +
                    "    </saml:AttributeStatement>\n" +
                    "</saml:Assertion>";

    public static final String AUTH_SAML_V2 =
            "            <saml2:Assertion ID=\"SSB-SamlAssertion-6eee8d1fffa9fe47c6f39b99623a7819\"\n" +
            "                             IssueInstant=\"2011-10-12T02:43:35.902Z\" Version=\"2.0\"\n" +
            "                             xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "                <saml2:Issuer>donal</saml2:Issuer>\n" +
            "                <saml2:Subject>\n" +
            "                    <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">admin</saml2:NameID>\n" +
            "                </saml2:Subject>\n" +
            "                <saml2:Conditions NotBefore=\"2011-10-12T02:41:35.924Z\" NotOnOrAfter=\"2011-10-12T02:48:35.924Z\"/>\n" +
            "                <saml2:AuthnStatement AuthnInstant=\"2011-10-12T02:43:35.901Z\">\n" +
            "                    <saml2:SubjectLocality Address=\"10.7.48.207\"/>\n" +
            "                    <saml2:AuthnContext>\n" +
            "                        <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password\n" +
            "                        </saml2:AuthnContextClassRef>\n" +
            "                    </saml2:AuthnContext>\n" +
            "                </saml2:AuthnStatement>\n" +
            "            </saml2:Assertion>";

    private static final String SAML_V2_NO_NAME_FORMAT = "<saml2:Assertion Version=\"2.0\" ID=\"SamlAssertion-7a98417e26056ecc00cd3cd3af54ce12\"\n" +
            "                 IssueInstant=\"2012-05-11T18:05:55.865Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
            "    <saml2:Issuer>irishman2.l7tech.local</saml2:Issuer>\n" +
            "    <saml2:Subject>\n" +
            "        <saml2:NameID NameQualifier=\"\"/>\n" +
            "    </saml2:Subject>\n" +
            "    <saml2:Conditions NotBefore=\"2012-05-11T18:03:55.865Z\" NotOnOrAfter=\"2012-05-11T18:10:55.866Z\"/>\n" +
            "    <saml2:AttributeStatement>\n" +
            "        <saml2:Attribute Name=\"one\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\">\n" +
            "            <saml2:AttributeValue>one</saml2:AttributeValue>\n" +
            "        </saml2:Attribute>\n" +
            "    </saml2:AttributeStatement>\n" +
            "</saml2:Assertion>";
}
