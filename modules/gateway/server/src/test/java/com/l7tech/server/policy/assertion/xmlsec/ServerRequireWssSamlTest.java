package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Pair;
import com.l7tech.xml.saml.SamlAssertionV1;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ServerRequireWssSamlTest {

    Message request;

    @Before
    public void initRequest() throws Exception {
        request = SamlTestUtil.makeSamlRequest(false);
    }

    @Test
    @BugNumber(5141)
    public void testContextVariableAttr() throws Exception {
        RequireWssSaml ass = SamlTestUtil.configureToAllowAttributeBearer(new RequireWssSaml());
        ServerRequireWssSaml sass = new ServerRequireWssSaml<RequireWssSaml>(ass, SamlTestUtil.beanFactory, null);

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
        ServerRequireWssSaml sass = new ServerRequireWssSaml<RequireWssSaml>(ass, SamlTestUtil.beanFactory, null);

        Message request = new Message(XmlUtil.stringAsDocument(SamlTestUtil.SOAPENV),0);
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
}
