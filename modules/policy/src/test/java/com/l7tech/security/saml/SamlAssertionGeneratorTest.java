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
        SamlAssertionGenerator.Options opts = getOpts();
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
        SamlAssertionGenerator.Options opts = getOpts();
        opts.setVersion(1);

        final Document samlAssertion = generator.createAssertion(stmt, opts);
        System.out.println(XmlUtil.nodeToFormattedString(samlAssertion));

        final Element documentElement = samlAssertion.getDocumentElement();
        String xPath = "/saml:Assertion/saml:Conditions/saml:AudienceRestrictionCondition/saml:Audience";
        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put(SamlConstants.NS_SAML_PREFIX, SamlConstants.NS_SAML);
        validateAudience(documentElement, xPath, prefixToNamespace);
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

    private SamlAssertionGenerator.Options getOpts() {
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
