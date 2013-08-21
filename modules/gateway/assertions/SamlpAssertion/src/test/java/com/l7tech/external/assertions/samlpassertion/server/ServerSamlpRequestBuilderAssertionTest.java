package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.XmlElementEncryptionConfig;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class ServerSamlpRequestBuilderAssertionTest {

    /**
     * Test that for version 2 we can encrypt the generated NameID. Note Version 1 does not support encrypted elements.
     * @throws Exception
     */
    @Test
    public void testEncryptedID_Version2() throws Exception {
        Document samlpRequest = runEncryptUseCase(new Functions.Binary<Void, XmlElementEncryptionConfig, PolicyEnforcementContext>() {
            @Override
            public Void call(XmlElementEncryptionConfig xmlElementEncryptionConfig, PolicyEnforcementContext policyEnforcementContext) {
                Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_1024");
                try {
                    xmlElementEncryptionConfig.setRecipientCertificateBase64(HexUtils.encodeBase64(k.left.getEncoded(), true));
                } catch (CertificateEncodingException e) {
                    Assert.fail("Unexpected Exception: " + e.getMessage());
                }

                return null;
            }
        });
        String xml = XmlUtil.nodeToFormattedString(samlpRequest);
        assertFalse("Must NOT use OAEP key wrapping", xml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2));
        assertTrue("Must use RSA 1.5 key wrapping", xml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO));
    }

    /*
     * Ensure that encryption using OAEP works as expected, when so configured.
     */
    @Test
    @BugId("SSG-7462")
    public void testEncryptedID_Version2_useOaep() throws Exception {
        Document samlpRequest = runEncryptUseCase(new Functions.Binary<Void, XmlElementEncryptionConfig, PolicyEnforcementContext>() {
            @Override
            public Void call(XmlElementEncryptionConfig xmlElementEncryptionConfig, PolicyEnforcementContext policyEnforcementContext) {
                Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_1024");
                try {
                    xmlElementEncryptionConfig.setRecipientCertificateBase64(HexUtils.encodeBase64(k.left.getEncoded(), true));
                } catch (CertificateEncodingException e) {
                    Assert.fail("Unexpected Exception: " + e.getMessage());
                }
                xmlElementEncryptionConfig.setUseOaep(true);

                return null;
            }
        });
        String xml = XmlUtil.nodeToFormattedString(samlpRequest);
        assertTrue("Must use OAEP key wrapping", xml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2));
        assertFalse("Must NOT use RSA 1.5 key wrapping", xml.contains(SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO));
    }


    /**
     * Validate the EncryptedID can be generated via a cert from a context variable
     */
    @BugNumber(11666)
    @Test
    public void testEncryptIdViaCertInContextVariable() throws Exception {
        runEncryptUseCase(new Functions.Binary<Void, XmlElementEncryptionConfig, PolicyEnforcementContext>() {
            @Override
            public Void call(XmlElementEncryptionConfig xmlElementEncryptionConfig, PolicyEnforcementContext policyEnforcementContext) {
                Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_1024");
                final String certVarName = "cert";
                xmlElementEncryptionConfig.setRecipientCertContextVariableName(certVarName);

                policyEnforcementContext.setVariable(certVarName, k.left);

                return null;
            }
        });
    }

    private Document runEncryptUseCase(Functions.Binary<Void, XmlElementEncryptionConfig, PolicyEnforcementContext> configCallBack) throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        // Absolute minimum configuration to avoid NPE's etc.
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setNameIdentifierValue("test");
        assertion.setAttributeStatement(new SamlAttributeStatement());
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);

        // Require encrypted
        assertion.setEncryptNameIdentifier(true);
        final XmlElementEncryptionConfig config = assertion.getXmlEncryptConfig();

        final PolicyEnforcementContext context = getContext();

        configCallBack.call(config, context);

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message samlpRequest = (Message) context.getVariable(assertion.getOtherTargetMessageVariable());
        final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();

        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        String xPath = "/soapenv:Envelope/soapenv:Body/samlp2:AttributeQuery/saml2:Subject/saml2:EncryptedID";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put(SamlConstants.NS_SAML2_PREFIX, SamlConstants.NS_SAML2);
        prefixToNamespace.put(SamlConstants.NS_SAMLP2_PREFIX, SamlConstants.NS_SAMLP2);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        //first element should be the auth token
        final Element encryptedID = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        Assert.assertEquals("Wrong element found", "EncryptedID", encryptedID.getLocalName());
        return documentElement.getOwnerDocument();
    }

    /**
     * It's possible to incorrectly configure the encryption settings in the UI - by simply not configuring them.
     *
     * Tests that when no context variable is defined that the server assertion cannot be created.
     */
    @Test(expected = ServerPolicyException.class)
    public void testInvalidEncryptionConfiguration() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        // Absolute minimum configuration to avoid NPE's etc.
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setNameIdentifierValue("test");
        assertion.setAttributeStatement(new SamlAttributeStatement());
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);

        // Require encrypted - do not configure encryption settings for test
        assertion.setEncryptNameIdentifier(true);

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final PolicyEnforcementContext context = getContext();
        serverAssertion.checkRequest(context);
    }

    @Test
    @BugNumber(11622)
    public void testCustomNameFormat() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        // Absolute minimum configuration to avoid NPE's etc.
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        final String customformat = "customformat";
        assertion.setCustomNameIdentifierFormat(customformat + "${var}");
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setNameIdentifierValue("test");
        assertion.setAttributeStatement(new SamlAttributeStatement());
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);

        final PolicyEnforcementContext context = getContext();
        context.setVariable("var", "1");

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message samlpRequest = (Message) context.getVariable(assertion.getOtherTargetMessageVariable());
        final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();

        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        String xPath = "/soapenv:Envelope/soapenv:Body/samlp2:AttributeQuery/saml2:Subject/saml2:NameID";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put(SamlConstants.NS_SAML2_PREFIX, SamlConstants.NS_SAML2);
        prefixToNamespace.put(SamlConstants.NS_SAMLP2_PREFIX, SamlConstants.NS_SAMLP2);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        //first element should be the auth token
        final Element nameID = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        Assert.assertEquals("Wrong element found", "NameID", nameID.getLocalName());

        final String format = nameID.getAttribute("Format");
        Assert.assertEquals("Invalid name format found", customformat + "1", format);
    }

    /**
     * Validate that an invalid value causes assertion failure and is audited correctly.
     */
    @Test
    @BugNumber(11622)
    public void testCustomNameFormat_InvalidResolvedValue() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        // Absolute minimum configuration to avoid NPE's etc.
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        final String customformat = "customformat";
        assertion.setCustomNameIdentifierFormat(customformat + "${var}");
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setNameIdentifierValue("test");
        assertion.setAttributeStatement(new SamlAttributeStatement());
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);

        final PolicyEnforcementContext context = getContext();
        context.setVariable("var", "%"); //invalid URI value

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put( "auditFactory", testAudit.factory() )
                .unmodifiableMap()
        );

        try {
            serverAssertion.checkRequest(context);
            Assert.fail("Assertion should have thrown");
        } catch (AssertionStatusException e) {
            Assert.assertEquals("Wrong status found", AssertionStatus.SERVER_ERROR, e.getAssertionStatus());
            // pass
        }

        //validate audits
        for (String s : testAudit) {
            System.out.println(s);
        }

        Assert.assertTrue("Required audit not found", testAudit.isAuditPresentContaining("Invalid URI value found for custom name identifier format. Resolved value: 'customformat%'. Reason: '"));
    }

    /**
     * Tests that when the assertion fails for a configuration error, that it audits at the warning level.
     * <p/>
     * This test validates this for the case when FROM_CREDS is configured but no creds are available.
     */
    @Test
    @BugNumber(11704)
    public void testGenericAuditFor_FromCreds_Exception() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.FROM_CREDS);
        assertion.setAttributeStatement(new SamlAttributeStatement());
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);

        final PolicyEnforcementContext context = getContext();

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Wrong status found", AssertionStatus.FAILED, assertionStatus);

        //validate audits
        for (String s : testAudit) {
            System.out.println(s);
        }

        Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.SAMLP_REQUEST_BUILDER_FAILED_TO_BUILD));
        Assert.assertTrue("Required audit not found", testAudit.isAuditPresentContaining("Missing credentials to populate NameIdentifier"));
    }

    /**
     * Validate that for a SAML 2.0 request that the Issuer element can be customized correctly.
     * All custom Issuer fields support expressions.
     * Format attribute must be a URI.
     *
     * @throws Exception
     */
    @BugNumber(11621)
    @Test
    public void testCustomIssuerValues() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setAttributeStatement(new SamlAttributeStatement());
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);

        // Customize Issuer
        final String customIssuerValue = "Custom Issuer ";
        assertion.setCustomIssuerValue(customIssuerValue + "${var}");
        final String aValidURI = "AValidURI";
        assertion.setCustomIssuerFormat(aValidURI + "${var}");
        final String customIssuerNameQualifier = "Custom Name Qualifier ";
        assertion.setCustomIssuerNameQualifier(customIssuerNameQualifier + "${var}");

        final PolicyEnforcementContext context = getContext();
        context.setVariable("var", "1");

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message samlpRequest = (Message) context.getVariable(assertion.getOtherTargetMessageVariable());
        final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();

        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        String xPath = "/soapenv:Envelope/soapenv:Body/samlp2:AttributeQuery/saml2:Issuer";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put(SamlConstants.NS_SAML2_PREFIX, SamlConstants.NS_SAML2);
        prefixToNamespace.put(SamlConstants.NS_SAMLP2_PREFIX, SamlConstants.NS_SAMLP2);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        //first element should be the auth token
        final Element issuerElement = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        Assert.assertEquals("Wrong element found", "Issuer", issuerElement.getLocalName());

        Assert.assertEquals(customIssuerValue + "1", issuerElement.getTextContent());

        final String format = issuerElement.getAttribute("Format");
        Assert.assertEquals("Invalid name format found", aValidURI + "1", format);

        final String nameQualifier = issuerElement.getAttribute("NameQualifier");
        Assert.assertEquals("Invalid name qualifier found", customIssuerNameQualifier + "1", nameQualifier);
    }

    @Test
    public void testCustomIssuerValues_InvalidFormat() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setAttributeStatement(new SamlAttributeStatement());
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);

        // Customize Issuer
        assertion.setCustomIssuerFormat("Invalid URI %");

        final PolicyEnforcementContext context = getContext();

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put( "auditFactory", testAudit.factory() )
                .unmodifiableMap()
        );

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.FAILED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.SAMLP_REQUEST_BUILDER_FAILED_TO_BUILD));
        Assert.assertTrue(testAudit.isAuditPresentContaining("Invalid Issuer Format attribute value, not a URI"));
    }

    /**
     * Test coverage for default Issuer
     * @throws Exception
     */
    @Test
    public void testDefaultIssuerValues() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setAttributeStatement(new SamlAttributeStatement());
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);

        final PolicyEnforcementContext context = getContext();

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message samlpRequest = (Message) context.getVariable(assertion.getOtherTargetMessageVariable());
        final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();

        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        String xPath = "/soapenv:Envelope/soapenv:Body/samlp2:AttributeQuery/saml2:Issuer";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put(SamlConstants.NS_SAML2_PREFIX, SamlConstants.NS_SAML2);
        prefixToNamespace.put(SamlConstants.NS_SAMLP2_PREFIX, SamlConstants.NS_SAMLP2);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        final Element issuerElement = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        Assert.assertEquals("Wrong element found", "Issuer", issuerElement.getLocalName());

        Assert.assertEquals("CN=Bob, OU=OASIS Interop Test Cert, O=OASIS", issuerElement.getTextContent());

        final String format = issuerElement.getAttribute("Format");
        Assert.assertEquals("Name format should not be found", "", format);

        final String nameQualifier = issuerElement.getAttribute("NameQualifier");
        Assert.assertEquals("Name qualifier should not be found", "", nameQualifier);
    }

    @BugNumber(11808)
    @Test
    public void testIssuerIsConfigurable() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setAttributeStatement(new SamlAttributeStatement());

        //No issuer
        assertion.setAddIssuer(false);
        //turn off signing
        assertion.setSignAssertion(false);

        final PolicyEnforcementContext context = getContext();

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message samlpRequest = (Message) context.getVariable(assertion.getOtherTargetMessageVariable());
        final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();

        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        String xPath = "/soapenv:Envelope/soapenv:Body/samlp2:AttributeQuery/saml2:Issuer";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put(SamlConstants.NS_SAML2_PREFIX, SamlConstants.NS_SAML2);
        prefixToNamespace.put(SamlConstants.NS_SAMLP2_PREFIX, SamlConstants.NS_SAMLP2);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        Assert.assertFalse("No Issuer should have been added", xpathResultSetIterator.hasNext());
    }

    /**
     * Verify Issuer element is not required when singing. Applies to SAML 2.0 only.
     */
    @BugNumber(11809)
    @Test
    public void testIssuerNotRequiredWhenSigning_Saml2() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setAttributeStatement(new SamlAttributeStatement());

        // No issuer
        assertion.setAddIssuer(false);
        // signer is on by default

        final PolicyEnforcementContext context = getContext();

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message samlpRequest = (Message) context.getVariable(assertion.getOtherTargetMessageVariable());
        final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();

        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        String xPath = "/soapenv:Envelope/soapenv:Body/samlp2:AttributeQuery/ds:Signature";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        prefixToNamespace.put(SamlConstants.NS_SAMLP2_PREFIX, SamlConstants.NS_SAMLP2);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        Assert.assertTrue("Signature should have been added", xpathResultSetIterator.hasNext());
    }

    /**
     * Verify that the Issuer element precedes the Signature element so it follows the schema order.
     * Applies to SAML 2.0 only.
     */
    @Test
    public void testIssuerIsFirstWhenSigning_Saml2() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setAttributeStatement(new SamlAttributeStatement());

        // issuer is on by default
        // signer is on by default

        final PolicyEnforcementContext context = getContext();

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message samlpRequest = (Message) context.getVariable(assertion.getOtherTargetMessageVariable());
        final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();

        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        String xPath = "/soapenv:Envelope/soapenv:Body/samlp2:AttributeQuery/*";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        prefixToNamespace.put(SamlConstants.NS_SAMLP2_PREFIX, SamlConstants.NS_SAMLP2);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        final ElementCursor issuerCursor = xpathResultSetIterator.nextElementAsCursor();
        final Element issuerElement = issuerCursor.asDomElement();
        Assert.assertEquals("Issuer element should have been found first", SamlConstants.ELEMENT_ISSUER, issuerElement.getLocalName());

        final ElementCursor sigCursor = xpathResultSetIterator.nextElementAsCursor();
        final Element sigElement = sigCursor.asDomElement();
        Assert.assertEquals("Signature element should have been found second", "Signature", sigElement.getLocalName());
    }

    /**
     * Verify Signature element can be added for SAML 1
     */
    @Test
    public void testSigning_Saml1() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        assertion.setVersion(1);
        assertion.setSoapVersion(1);
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setAttributeStatement(new SamlAttributeStatement());

        // signer is on by default

        final PolicyEnforcementContext context = getContext();

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message samlpRequest = (Message) context.getVariable(assertion.getOtherTargetMessageVariable());
        final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();

        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        String xPath = "/soapenv:Envelope/soapenv:Body/samlp:Request/ds:Signature";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        prefixToNamespace.put(SamlConstants.NS_SAMLP_PREFIX, SamlConstants.NS_SAMLP);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        final ElementCursor sigCursor = xpathResultSetIterator.nextElementAsCursor();
        final Element sigElement = sigCursor.asDomElement();
        Assert.assertEquals("Signature element should have been found second", "Signature", sigElement.getLocalName());
    }

    /**
     * Verify assertion can be configured not to sign the request.
     */
    @Test
    public void testNoSigning_Saml1() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        assertion.setVersion(1);
        assertion.setSoapVersion(1);
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setAttributeStatement(new SamlAttributeStatement());

        assertion.setSignAssertion(false);

        final PolicyEnforcementContext context = getContext();

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message samlpRequest = (Message) context.getVariable(assertion.getOtherTargetMessageVariable());
        final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();

        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        String xPath = "/soapenv:Envelope/soapenv:Body/samlp:Request/ds:Signature";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        prefixToNamespace.put(SamlConstants.NS_SAMLP_PREFIX, SamlConstants.NS_SAMLP);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        Assert.assertFalse("Signature xpath should yield no results",xpathResultSetIterator.hasNext());
    }

    /**
     * Verify assertion can be configured not to sign the request.
     */
    @Test
    public void testNoSigning_Saml2() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        assertion.setVersion(2);
        assertion.setSoapVersion(1);
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setAttributeStatement(new SamlAttributeStatement());

        assertion.setSignAssertion(false);

        final PolicyEnforcementContext context = getContext();

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message samlpRequest = (Message) context.getVariable(assertion.getOtherTargetMessageVariable());
        final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();

        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        String xPath = "/soapenv:Envelope/soapenv:Body/samlp2:AttributeQuery/ds:Signature";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        prefixToNamespace.put(SamlConstants.NS_SAMLP2_PREFIX, SamlConstants.NS_SAMLP2);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        Assert.assertFalse("Signature xpath should yield no results",xpathResultSetIterator.hasNext());
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
