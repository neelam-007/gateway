package com.l7tech.security.saml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.test.BugNumber;
import com.l7tech.util.DomUtils;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SamlAssertionGeneratorTest {

    @Test
    @BugNumber(10276)
    public void testMultipleAudienceElements_Version2() throws Exception {
        SamlAssertionGenerator generator = new SamlAssertionGenerator(new SignerInfo(
                TestDocuments.getDotNetServerPrivateKey(),
                new X509Certificate[] { TestDocuments.getDotNetServerCertificate() }
        ));

        SubjectStatement stmt = getStatement();
        SamlAssertionGenerator.Options opts = getOptsForAudience();
        opts.setVersion(2);

        final Document samlAssertion = generator.createAssertion(stmt, opts);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final Element documentElement = samlAssertion.getDocumentElement();
        String xPath = "/saml2:Assertion/saml2:Conditions/saml2:AudienceRestriction/saml2:Audience";
        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put(SamlConstants.NS_SAML2_PREFIX, SamlConstants.NS_SAML2);
        validateAudience(documentElement, xPath, prefixToNamespace);
    }

    @Test
    @BugNumber(10276)
    public void testMultipleAudienceElements_Version1() throws Exception {
        SamlAssertionGenerator generator = new SamlAssertionGenerator(new SignerInfo(
                TestDocuments.getDotNetServerPrivateKey(),
                new X509Certificate[] { TestDocuments.getDotNetServerCertificate() }
        ));

        SubjectStatement stmt = getStatement();
        SamlAssertionGenerator.Options opts = getOptsForAudience();
        opts.setVersion(1);

        final Document samlAssertion = generator.createAssertion(stmt, opts);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final Element documentElement = samlAssertion.getDocumentElement();
        String xPath = "/saml:Assertion/saml:Conditions/saml:AudienceRestrictionCondition/saml:Audience";
        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put(SamlConstants.NS_SAML_PREFIX, SamlConstants.NS_SAML);
        validateAudience(documentElement, xPath, prefixToNamespace);
    }

    /**
     * Simple test to validate existing behavior of automatic Issuer value in SAML assertions.
     */
    @Test
    public void testIssuer_Version1() throws Exception {
        SamlAssertionGenerator generator = new SamlAssertionGenerator(new SignerInfo(
                TestDocuments.getDotNetServerPrivateKey(),
                new X509Certificate[] { TestDocuments.getDotNetServerCertificate() }
        ));

        SubjectStatement stmt = getStatement();
        SamlAssertionGenerator.Options opts = getOptsForAudience();
        opts.setVersion(1);

        final Document samlAssertion = generator.createAssertion(stmt, opts);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final Attr issuerAttr = samlAssertion.getDocumentElement().getAttributeNode("Issuer");
        System.out.println(issuerAttr.getValue());
        Assert.assertEquals("Did not find correct default issuer value", "Bob", issuerAttr.getValue());
    }

    /**
     * Simple test to validate existing behavior of automatic Issuer value in SAML assertions.
     */
    @Test
    public void testIssuer_Version2() throws Exception {
        SamlAssertionGenerator generator = new SamlAssertionGenerator(new SignerInfo(
                TestDocuments.getDotNetServerPrivateKey(),
                new X509Certificate[] { TestDocuments.getDotNetServerCertificate() }
        ));

        SubjectStatement stmt = getStatement();
        SamlAssertionGenerator.Options opts = getOptsForAudience();
        opts.setVersion(2);

        final Document samlAssertion = generator.createAssertion(stmt, opts);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final Element documentElement = samlAssertion.getDocumentElement();
        validateSaml2Issuer(documentElement, "Bob", null, null);
    }

    /**
     * Test custom issuer value for SAML 1
     */
    @BugNumber(10035)
    @Test
    public void testCustomIssuer_Version1() throws Exception {
        SamlAssertionGenerator generator = new SamlAssertionGenerator(new SignerInfo(
                TestDocuments.getDotNetServerPrivateKey(),
                new X509Certificate[] { TestDocuments.getDotNetServerCertificate() }
        ));

        SubjectStatement stmt = getStatement();
        SamlAssertionGenerator.Options opts = getOptsForAudience();
        opts.setVersion(1);
        final String customIssuer = "Custom Issuer Value";
        opts.setCustomIssuer(customIssuer);

        final Document samlAssertion = generator.createAssertion(stmt, opts);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final Attr issuerAttr = samlAssertion.getDocumentElement().getAttributeNode("Issuer");
        System.out.println(issuerAttr.getValue());
        Assert.assertEquals("Did not find correct custom issuer value", customIssuer, issuerAttr.getValue());
    }

    /**
     * Tests custom issuer for SAML 2.
     */
    @BugNumber(10035)
    @Test
    public void testCustomIssuer_Version2() throws Exception {
        SamlAssertionGenerator generator = new SamlAssertionGenerator(new SignerInfo(
                TestDocuments.getDotNetServerPrivateKey(),
                new X509Certificate[] { TestDocuments.getDotNetServerCertificate() }
        ));

        SubjectStatement stmt = getStatement();
        SamlAssertionGenerator.Options opts = getOptsForAudience();
        opts.setVersion(2);
        final String customIssuer = "Custom Issuer Value";
        opts.setCustomIssuer(customIssuer);

        final Document samlAssertion = generator.createAssertion(stmt, opts);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final Element documentElement = samlAssertion.getDocumentElement();
        validateSaml2Issuer(documentElement, customIssuer, null, null);
    }

    /**
     * Tests custom issuer with customized attributes for SAML 2.
     */
    @BugNumber(10035)
    @Test
    public void testCustomIssuer_CustomAttributes_Version2() throws Exception {
        SamlAssertionGenerator generator = new SamlAssertionGenerator(new SignerInfo(
                TestDocuments.getDotNetServerPrivateKey(),
                new X509Certificate[] { TestDocuments.getDotNetServerCertificate() }
        ));

        SubjectStatement stmt = getStatement();
        SamlAssertionGenerator.Options opts = getOptsForAudience();
        opts.setVersion(2);
        final String customIssuer = "Custom Issuer Value";
        opts.setCustomIssuer(customIssuer);
        opts.setCustomIssuerNameFormatUri(SamlConstants.NAMEIDENTIFIER_ENTITY);
        final String customIssuerNameQualifier = "Custom Name Qualifier";
        opts.setCustomIssuerNameQualifier(customIssuerNameQualifier);

        final Document samlAssertion = generator.createAssertion(stmt, opts);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final Element documentElement = samlAssertion.getDocumentElement();
        validateSaml2Issuer(documentElement, customIssuer, SamlConstants.NAMEIDENTIFIER_ENTITY, customIssuerNameQualifier);
    }

    /**
     * Tests default issuer with customized attributes for SAML 2.
     */
    @BugNumber(10035)
    @Test
    public void testDefaultIssuer_CustomAttributes_Version2() throws Exception {
        SamlAssertionGenerator generator = new SamlAssertionGenerator(new SignerInfo(
                TestDocuments.getDotNetServerPrivateKey(),
                new X509Certificate[] { TestDocuments.getDotNetServerCertificate() }
        ));

        SubjectStatement stmt = getStatement();
        SamlAssertionGenerator.Options opts = getOptsForAudience();
        opts.setVersion(2);
        opts.setCustomIssuerNameFormatUri(SamlConstants.NAMEIDENTIFIER_ENTITY);
        final String customIssuerNameQualifier = "Custom Name Qualifier";
        opts.setCustomIssuerNameQualifier(customIssuerNameQualifier);

        final Document samlAssertion = generator.createAssertion(stmt, opts);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final Element documentElement = samlAssertion.getDocumentElement();
        validateSaml2Issuer(documentElement, null, SamlConstants.NAMEIDENTIFIER_ENTITY, customIssuerNameQualifier);
    }

    private void validateSaml2Issuer(Element documentElement,
                                     String expectedIssuer,
                                     String expectedNameFormat,
                                     String expectedNameQualifier) throws Exception{
        String xPath = "/saml2:Assertion/saml2:Issuer";
        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put(SamlConstants.NS_SAML2_PREFIX, SamlConstants.NS_SAML2);
        final ElementCursor cursor = new DomElementCursor(documentElement);
        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();
        final Element issuerElement = xpathResultSetIterator.nextElementAsCursor().asDomElement();

        if (expectedIssuer != null) {
            final String foundIssuer = DomUtils.getTextValue(issuerElement);
            Assert.assertEquals("Did not find correct default issuer value", expectedIssuer, foundIssuer);
        }

        if (expectedNameFormat != null) {
            final String foundFormat = issuerElement.getAttribute("Format");
            Assert.assertEquals("Did not find correct NameFormat attribute value", expectedNameFormat, foundFormat);
        }

        if (expectedNameQualifier != null) {
            final String foundQualifier = issuerElement.getAttribute("NameQualifier");
            Assert.assertEquals("Did not find correct NameQualifier attribute value", expectedNameQualifier, foundQualifier);
        }
    }

    private void validateAudience(Element documentElement, String xPath, Map<String, String> prefixToNamespace) throws XPathExpressionException, InvalidXpathException {
        final ElementCursor cursor = new DomElementCursor(documentElement);
        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        //we expect 3 audience elements
        for (int i = 0; i < 3; i++) {
            ElementCursor currentElement = xpathResultSetIterator.nextElementAsCursor();
            final Element node = currentElement.asDomElement();
            Assert.assertEquals("No Audience found", "Audience", node.getLocalName());
            Assert.assertEquals("Wrong Audience element value found", "Audience" + (i + 1), DomUtils.getTextValue(node));
        }
    }

    private SamlAssertionGenerator.Options getOptsForAudience() {
        SamlAssertionGenerator.Options opts = new SamlAssertionGenerator.Options();
        opts.setSignAssertion(false);
        opts.setAudienceRestriction(Arrays.asList("Audience1", "Audience2", "Audience3"));
        return opts;
    }

    /**
     * Get a statement - specifics not important for the test cases.
     */
    private SubjectStatement getStatement() {
        return SubjectStatement.createAttributeStatement(
                    LoginCredentials.makeLoginCredentials(
                            new HttpBasicToken("foo", "bar".toCharArray()), HttpBasic.class),
                    SubjectStatement.BEARER, "foo", "urn:example.com:attributes", "bar",
                    KeyInfoInclusionType.NONE, NameIdentifierInclusionType.NONE, null, null, null);
    }
}
