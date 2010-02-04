package com.l7tech.external.assertions.samlissuer;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlConstants;

import static com.l7tech.policy.wsp.WspReader.INCLUDE_DISABLED;
import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 */
public class SamlIssuerAssertionTest {

    private final WspReader wspReader;
    {
        final AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        tmf.registerAssertion(SamlIssuerAssertion.class);
        WspConstants.setTypeMappingFinder(tmf);
        wspReader = new WspReader(tmf);
    }

    @BeforeClass
    public static void init() throws Exception {
        AssertionRegistry.installEnhancedMetadataDefaults();
    }

    @Test
    public void testAuthStatement() throws Exception {
        SamlIssuerAssertion sia = new SamlIssuerAssertion();
        final SamlAuthenticationStatement authst = new SamlAuthenticationStatement();
        authst.setAuthenticationMethods(new String[] { SamlConstants.X509_PKI_AUTHENTICATION });
        sia.setAuthenticationStatement(authst);
        assertTrue(WspWriter.getPolicyXml(sia).contains(SamlConstants.X509_PKI_AUTHENTICATION));
    }

    @Test
    public void testSamlIssuerAssertion() throws Exception {
        SamlIssuerAssertion sia = new SamlIssuerAssertion();
        sia.setNameIdentifierType(NameIdentifierInclusionType.NONE);
        String xml = WspWriter.getPolicyXml(sia);
        
        SamlIssuerAssertion sia2 = (SamlIssuerAssertion) wspReader.parseStrictly(xml, INCLUDE_DISABLED);
        assertEquals(sia2.getNameIdentifierType(), NameIdentifierInclusionType.NONE);
    }

    @Test
    public void testSamlIssuerPolicyBeforeModular() throws Exception {
        Assertion ass = wspReader.parseStrictly(POLICY_BEFORE_MODULAR, INCLUDE_DISABLED);
        assertTrue(ass != null);
        assertTrue(ass instanceof AllAssertion);
        AllAssertion all = (AllAssertion) ass;
        assertTrue(all.getChildren().size() == 1);
        assertTrue(all.getChildren().get(0) instanceof SamlIssuerAssertion);
    }

    private static final String POLICY_BEFORE_MODULAR = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
        "    <wsp:All wsp:Usage=\"Required\">\n" +
        "        <L7p:SamlIssuer>\n" +
        "            <L7p:AttributeStatement samlAttributeInfo=\"included\">\n" +
        "                <L7p:Attributes samlAttributeElementInfoArray=\"included\">\n" +
        "                    <L7p:item samlAttributeElementInfo=\"included\">\n" +
        "                        <L7p:Name stringValue=\"aa1\"/>\n" +
        "                        <L7p:Namespace stringValue=\"an1\"/>\n" +
        "                        <L7p:Value stringValue=\"av1\"/>\n" +
        "                    </L7p:item>\n" +
        "                </L7p:Attributes>\n" +
        "            </L7p:AttributeStatement>\n" +
        "            <L7p:AudienceRestriction stringValue=\"\"/>\n" +
        "            <L7p:AuthenticationStatement samlAuthenticationInfo=\"included\">\n" +
        "                <L7p:AuthenticationMethods stringArrayValue=\"included\"/>\n" +
        "            </L7p:AuthenticationStatement>\n" +
        "            <L7p:AuthorizationStatement samlAuthorizationInfo=\"included\">\n" +
        "                <L7p:Action stringValue=\"a1\"/>\n" +
        "                <L7p:ActionNamespace stringValue=\"n1\"/>\n" +
        "                <L7p:Resource stringValue=\"r1\"/>\n" +
        "            </L7p:AuthorizationStatement>\n" +
        "            <L7p:ConditionsNotBeforeSecondsInPast intValue=\"120\"/>\n" +
        "            <L7p:ConditionsNotOnOrAfterExpirySeconds intValue=\"300\"/>\n" +
        "            <L7p:DecorationTypes decorationTypes=\"\"/>\n" +
        "            <L7p:NameQualifier stringValue=\"\"/>\n" +
        "            <L7p:SubjectConfirmationMethodUri stringValue=\"urn:oasis:names:tc:SAML:1.0:cm:holder-of-key\"/>\n" +
        "            <L7p:Version boxedIntegerValue=\"1\"/>\n" +
        "        </L7p:SamlIssuer>\n" +
        "    </wsp:All>\n" +
        "</wsp:Policy>";
}
