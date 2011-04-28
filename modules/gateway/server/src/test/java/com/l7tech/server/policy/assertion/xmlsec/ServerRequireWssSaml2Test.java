package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml2;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.server.message.PolicyEnforcementContext;
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

    @Before
    public void initRequest() throws Exception {
        request = SamlTestUtil.makeSamlRequest(true);
    }

    @Test
    @BugNumber(5141)
    public void testContextVariableAttr() throws Exception {
        RequireWssSaml2 ass = SamlTestUtil.configureToAllowAttributeBearer(new RequireWssSaml2());
        ServerRequireWssSaml2 sass = new ServerRequireWssSaml2(ass, SamlTestUtil.beanFactory, null);

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
        ServerRequireWssSaml sass = new ServerRequireWssSaml2(ass, SamlTestUtil.beanFactory, null);


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
        now.clear(Calendar.MILLISECOND); //clear millis xsd:dateTime does not have it

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
        ServerRequireWssSaml sass = new ServerRequireWssSaml2(ass, SamlTestUtil.beanFactory, null);

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
}