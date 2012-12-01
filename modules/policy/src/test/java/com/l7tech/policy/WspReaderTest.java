package com.l7tech.policy;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.ForEachLoopAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.test.BugNumber;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.wsdl.BindingInfo;
import com.l7tech.wsdl.BindingOperationInfo;
import com.l7tech.xml.xpath.XpathExpression;
import org.junit.AfterClass;
import org.junit.Test;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.policy.wsp.WspReader.INCLUDE_DISABLED;
import static com.l7tech.policy.wsp.WspReader.OMIT_DISABLED;
import static org.junit.Assert.*;

/**
 * Test policy deserializer.
 * User: mike
 * Date: Jun 10, 2003
 * Time: 3:33:36 PM
 */
public class WspReaderTest {
    private static Logger log = Logger.getLogger(WspReaderTest.class.getName());
    private static final ClassLoader cl = WspReaderTest.class.getClassLoader();
    private static String RESOURCE_PATH = "com/l7tech/policy/resources";
    private static String SIMPLE_POLICY = RESOURCE_PATH + "/simple_policy.xml";

    private final WspReader wspReader;
    {
        final AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        wspReader = new WspReader(tmf);
    }

    static {
        SyspropUtil.setProperty( "com.l7tech.policy.wsp.checkAccessors", "true" );
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.policy.wsp.checkAccessors"
        );
    }

    @Test
    public void testParseWsp() throws Exception {
        InputStream wspStream = cl.getResourceAsStream(SIMPLE_POLICY);
        Assertion policy = wspReader.parsePermissively( XmlUtil.parse(wspStream).getDocumentElement(), INCLUDE_DISABLED);
        log.info("Got back policy: " + policy);
        assertTrue(policy != null);
        assertTrue(policy instanceof ExactlyOneAssertion);
        ExactlyOneAssertion eoa = (ExactlyOneAssertion)policy;
        assertTrue(eoa.getChildren().size() == 5);
        assertTrue(eoa.getChildren().get(0) instanceof AllAssertion);

        // Do a round trip policyA -> xmlA -> policyB -> xmlB and verify that both XMLs match
        String xmlA = WspWriter.getPolicyXml(policy);
        log.info("Parsing policy: " + xmlA);
        Assertion policyB = wspReader.parseStrictly(xmlA, INCLUDE_DISABLED);
        String xmlB = WspWriter.getPolicyXml(policyB);
        assertEquals(xmlA, xmlB);
    }

    @Test
    public void testParseNonXml() {
        try {
            wspReader.parseStrictly("asdfhaodh/asdfu2h$9ha98h", INCLUDE_DISABLED);
            fail("Expected IOException not thrown");
        } catch (IOException e) {
            // Ok
        }
    }

    @Test
    public void testParseStrangeXml() {
        try {
            wspReader.parseStrictly("<foo><bar blee=\"1\"/></foo>", INCLUDE_DISABLED);
            fail("Expected IOException not thrown");
        } catch (IOException e) {
            // Ok
        }
    }

    @Test
    public void testParseSwAPolicy() throws Exception {
        Assertion policy = WspWriterTest.createSoapWithAttachmentsPolicy();
        String serialized = WspWriter.getPolicyXml(policy);
        Assertion parsedPolicy = wspReader.parseStrictly(serialized, INCLUDE_DISABLED);
        assertTrue(parsedPolicy instanceof AllAssertion);
        AllAssertion all = (AllAssertion)parsedPolicy;
        Assertion kid = all.getChildren().get(0);
        assertTrue(kid instanceof RequestSwAAssertion);
        RequestSwAAssertion swa = (RequestSwAAssertion)kid;

        assertTrue(swa.getBindings().size() == 1);
        String bindingInfoName = swa.getBindings().keySet().iterator().next();
        BindingInfo bindingInfo = swa.getBindings().get(bindingInfoName);
        assertNotNull(bindingInfo);

        assertNotNull(bindingInfo.getBindingName());
        assertTrue(bindingInfo.getBindingName().length() > 0);
        assertEquals(bindingInfo.getBindingName(), "serviceBinding1");

        Map bops = bindingInfo.getBindingOperations();
        assertFalse(bops.isEmpty());
        BindingOperationInfo[] bois = (BindingOperationInfo[])bops.values().toArray(new BindingOperationInfo[0]);
        assertTrue(bois.length == 1);

        String reserialized = WspWriter.getPolicyXml(parsedPolicy);
        assertEquals(reserialized.length(), serialized.length());

    }

    @Test
    public void testCollectionMappings() throws Exception {
        // Use SqlAttackAssertion since it uses Set
        SqlAttackAssertion ass = new SqlAttackAssertion();
        ass.setProtection(SqlAttackAssertion.PROT_MSSQL);
        ass.setProtection(SqlAttackAssertion.PROT_ORASQL);
        ass.setProtection(SqlAttackAssertion.PROT_META);

        String xml = WspWriter.getPolicyXml(ass);
        log.info("Serialized SqlProtectionAssertion: \n" + xml);

        SqlAttackAssertion out = (SqlAttackAssertion)wspReader.parseStrictly(xml, INCLUDE_DISABLED);
        assertNotNull(out);
        assertTrue(out.getProtections().contains(SqlAttackAssertion.PROT_ORASQL));
        assertFalse(out.getProtections().contains(SqlAttackAssertion.PROT_METATEXT));
    }

    private static final Object[][] VERSIONS = {
        new Object[] { "simple_policy_21.xml", "2.1" },
        new Object[] { "simple_policy_30.xml", "3.0" },
        new Object[] { "simple_policy_31.xml", "3.1" },
    };

    private void trySeamlessPolicyUpgrade(String policyFile) throws Exception {
        InputStream policyStream = null;
        try {
            log.info("Trying to parse policy document; " + policyFile);
            policyStream = cl.getResourceAsStream(RESOURCE_PATH + "/" + policyFile);
            Document policy = XmlUtil.parse(policyStream);
            Assertion root = wspReader.parsePermissively(policy.getDocumentElement(), INCLUDE_DISABLED);
            assertTrue(root != null);
            assertTrue(root instanceof ExactlyOneAssertion);
        } finally {
            if (policyStream != null) policyStream.close();
        }
    }

    @Test
    public void testSeamlessPolicyUpgrades() throws Exception {
        for (Object[] version : VERSIONS) {
            String policyFile = (String) version[0];
            trySeamlessPolicyUpgrade(policyFile);
        }
    }

    @Test
    public void testSeamlessUpgradeFrom21() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_21.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = wspReader.parsePermissively(doc.getDocumentElement(), INCLUDE_DISABLED);
        log.info("Policy tree constructed after reading 2.1 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    @Test
    public void testSeamlessUpgradeFrom30() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_30.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = wspReader.parsePermissively(doc.getDocumentElement(), INCLUDE_DISABLED);
        log.info("Policy tree constructed after reading 3.0 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    @Test
    public void testSeamlessUpgradeFrom31() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_31.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = wspReader.parsePermissively(doc.getDocumentElement(), INCLUDE_DISABLED);
        log.info("Policy tree constructed after reading 3.1 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    @Test
    public void testSeamlessUpgradeFrom32() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_32.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = wspReader.parsePermissively(doc.getDocumentElement(), INCLUDE_DISABLED);
        log.info("Policy tree constructed after reading 3.2 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    @Test
    public void testSeamlessUpgradeFrom50() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "renamed_wss_assertions_50.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = wspReader.parsePermissively(doc.getDocumentElement(), INCLUDE_DISABLED);
        log.info("Policy tree constructed after reading 5.0 policy XML:\n" + ass);
        assertTrue("Policy not null", ass != null);
        assertTrue("Root is AllAssertion", ass instanceof AllAssertion);

        final List children = ((AllAssertion)ass).getChildren();
        assertEquals( "Expected number of assertions", 25, children.size());

        final XmlSecurityRecipientContext localRecipientContext = new XmlSecurityRecipientContext("", null);
        final XmlSecurityRecipientContext recipientContext = new XmlSecurityRecipientContext(
                "Alice",
                "MIIDDDCCAfSgAwIBAgIQM6YEf7FVYx/tZyEXgVComTANBgkqhkiG9w0BAQUFADAwMQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMB4XDTA1MDMxOTAwMDAwMFoXDTE4MDMxOTIzNTk1OVowQjEOMAwGA1UECgwFT0FTSVMxIDAeBgNVBAsMF09BU0lTIEludGVyb3AgVGVzdCBDZXJ0MQ4wDAYDVQQDDAVBbGljZTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAoqi99By1VYo0aHrkKCNT4DkIgPL/SgahbeKdGhrbu3K2XG7arfD9tqIBIKMfrX4Gp90NJa85AV1yiNsEyvq+mUnMpNcKnLXLOjkTmMCqDYbbkehJlXPnaWLzve+mW0pJdPxtf3rbD4PS/cBQIvtpjmrDAU8VsZKT8DN5Kyz+EZsCAwEAAaOBkzCBkDAJBgNVHRMEAjAAMDMGA1UdHwQsMCowKKImhiRodHRwOi8vaW50ZXJvcC5iYnRlc3QubmV0L2NybC9jYS5jcmwwDgYDVR0PAQH/BAQDAgSwMB0GA1UdDgQWBBQK4l0TUHZ1QV3V2QtlLNDm+PoxiDAfBgNVHSMEGDAWgBTAnSj8wes1oR3WqqqgHBpNwkkPDzANBgkqhkiG9w0BAQUFAAOCAQEABTqpOpvW+6yrLXyUlP2xJbEkohXHI5OWwKWleOb9hlkhWntUalfcFOJAgUyH30TTpHldzx1+vK2LPzhoUFKYHE1IyQvokBN2JjFO64BQukCKnZhldLRPxGhfkTdxQgdf5rCK/wh3xVsZCNTfuMNmlAM6lOAg8QduDah3WFZpEA0s2nwQaCNQTNMjJC8tav1CBr6+E5FAmwPXP7pJxn9Fw9OXRyqbRA4v2y7YpbGkG2GI9UvOHw6SGvf4FRSthMMO35YbpikGsLix3vAsXWWi4rwfVOYzQK0OFPNi9RMCUdSH06m9uLWckiCxjos0FQODZE9l4ATGy9s9hNVwryOJTw==");

        int assIndex = 0;
        {
            assertTrue("Assertion is request encryption", children.get(assIndex) instanceof RequireWssEncryptedElement);
            RequireWssEncryptedElement assertion = (RequireWssEncryptedElement) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals("Xpath value", "/soapenv:Envelope/soapenv:Body[position()=1]", assertion.getXpathExpression().getExpression());
            assertEquals("Encryption type", "http://www.w3.org/2001/04/xmlenc#aes128-cbc", assertion.getXEncAlgorithm());
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }
        
        assIndex++;
        {
            assertTrue("Assertion is request encryption", children.get(assIndex) instanceof RequireWssEncryptedElement);
            RequireWssEncryptedElement assertion = (RequireWssEncryptedElement) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals("Xpath value", "/soapenv:Envelope/soapenv:Body[position()=1]", assertion.getXpathExpression().getExpression());
            assertEquals("Encryption type", "http://www.w3.org/2001/04/xmlenc#aes128-cbc", assertion.getXEncAlgorithm());
            assertEquals("Recipient context", recipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is request signature", children.get(assIndex) instanceof RequireWssSignedElement);
            RequireWssSignedElement assertion = (RequireWssSignedElement) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals("Xpath value", "/soapenv:Envelope/soapenv:Body", assertion.getXpathExpression().getExpression());
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is request signature", children.get(assIndex) instanceof RequireWssSignedElement);
            RequireWssSignedElement assertion = (RequireWssSignedElement) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals("Xpath value", "/soapenv:Envelope/soapenv:Body", assertion.getXpathExpression().getExpression());
            assertEquals("Recipient context", recipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is request replay protection", children.get(assIndex) instanceof WssReplayProtection);
            WssReplayProtection assertion = (WssReplayProtection) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is request replay protection", children.get(assIndex) instanceof WssReplayProtection);
            WssReplayProtection assertion = (WssReplayProtection) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals("Recipient context", recipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is request SAMLv1.1", children.get(assIndex) instanceof RequireWssSaml);
            RequireWssSaml assertion = (RequireWssSaml) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals( "Audience restriction", "", assertion.getAudienceRestriction() );
            assertNotNull( "Authentication Statement present", assertion.getAuthenticationStatement() );
            assertArrayEquals( "Authentication statement auth methods", new String[]{"urn:ietf:rfc:3075"}, assertion.getAuthenticationStatement().getAuthenticationMethods() );
            assertArrayEquals( "Name formats", new String[]{"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName"}, assertion.getNameFormats() );
            assertEquals( "Name qualifier", "", assertion.getNameQualifier() );
            assertTrue("isRequireHolderOfKeyWithMessageSignature", assertion.isRequireHolderOfKeyWithMessageSignature());
            assertFalse("isRequireSenderVouchesWithMessageSignature", assertion.isRequireSenderVouchesWithMessageSignature());
            assertArrayEquals( "SubjectConfirmations", new String[]{"urn:oasis:names:tc:SAML:1.0:cm:holder-of-key"}, assertion.getSubjectConfirmations() );
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is request SAMLv1.1", children.get(assIndex) instanceof RequireWssSaml);
            RequireWssSaml assertion = (RequireWssSaml) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals( "Audience restriction", "", assertion.getAudienceRestriction() );
            assertNotNull( "Authentication Statement present", assertion.getAuthenticationStatement() );
            assertArrayEquals( "Authentication statement auth methods", new String[]{"urn:ietf:rfc:3075"}, assertion.getAuthenticationStatement().getAuthenticationMethods() );
            assertArrayEquals( "Name formats", new String[]{"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName"}, assertion.getNameFormats() );
            assertEquals( "Name qualifier", "", assertion.getNameQualifier() );
            assertTrue("isRequireHolderOfKeyWithMessageSignature", assertion.isRequireHolderOfKeyWithMessageSignature());
            assertFalse("isRequireSenderVouchesWithMessageSignature", assertion.isRequireSenderVouchesWithMessageSignature());
            assertArrayEquals( "SubjectConfirmations", new String[]{"urn:oasis:names:tc:SAML:1.0:cm:holder-of-key"}, assertion.getSubjectConfirmations() );
            assertEquals("Recipient context", recipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is request SAMLv2", children.get(assIndex) instanceof RequireWssSaml2);
            RequireWssSaml2 assertion = (RequireWssSaml2) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals( "Audience restriction", "", assertion.getAudienceRestriction() );
            assertNotNull( "Authentication Statement present", assertion.getAuthenticationStatement() );
            assertArrayEquals( "Authentication statement auth methods", new String[]{"urn:ietf:rfc:3075"}, assertion.getAuthenticationStatement().getAuthenticationMethods() );
            assertArrayEquals( "Name formats", new String[]{"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName"}, assertion.getNameFormats() );
            assertEquals( "Name qualifier", "", assertion.getNameQualifier() );
            assertTrue("isRequireHolderOfKeyWithMessageSignature", assertion.isRequireHolderOfKeyWithMessageSignature());
            assertFalse("isRequireSenderVouchesWithMessageSignature", assertion.isRequireSenderVouchesWithMessageSignature());
            assertArrayEquals( "SubjectConfirmations", new String[]{"urn:oasis:names:tc:SAML:1.0:cm:holder-of-key"}, assertion.getSubjectConfirmations() );
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is request SAMLv2", children.get(assIndex) instanceof RequireWssSaml2);
            RequireWssSaml2 assertion = (RequireWssSaml2) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals( "Audience restriction", "", assertion.getAudienceRestriction() );
            assertNotNull( "Authentication Statement present", assertion.getAuthenticationStatement() );
            assertArrayEquals( "Authentication statement auth methods", new String[]{"urn:ietf:rfc:3075"}, assertion.getAuthenticationStatement().getAuthenticationMethods() );
            assertArrayEquals( "Name formats", new String[]{"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName"}, assertion.getNameFormats() );
            assertEquals( "Name qualifier", "", assertion.getNameQualifier() );
            assertTrue("isRequireHolderOfKeyWithMessageSignature", assertion.isRequireHolderOfKeyWithMessageSignature());
            assertFalse("isRequireSenderVouchesWithMessageSignature", assertion.isRequireSenderVouchesWithMessageSignature());
            assertArrayEquals( "SubjectConfirmations", new String[]{"urn:oasis:names:tc:SAML:1.0:cm:holder-of-key"}, assertion.getSubjectConfirmations() );
            assertEquals("Recipient context", recipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion ", children.get(assIndex) instanceof RequireWssTimestamp);
            RequireWssTimestamp assertion = (RequireWssTimestamp) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals( "Max Expiry", 3600000, assertion.getMaxExpiryMilliseconds() );
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion ", children.get(assIndex) instanceof RequireWssTimestamp);
            RequireWssTimestamp assertion = (RequireWssTimestamp) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals( "Max Expiry", 3600000, assertion.getMaxExpiryMilliseconds() );
            assertEquals("Recipient context", recipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion ", children.get(assIndex) instanceof RequireWssX509Cert);
            RequireWssX509Cert assertion = (RequireWssX509Cert) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion ", children.get(assIndex) instanceof RequireWssX509Cert);
            RequireWssX509Cert assertion = (RequireWssX509Cert) children.get(assIndex);
            assertTrue("Assertion targets request", Assertion.isRequest(assertion));
            assertEquals("Recipient context", recipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is response encryption", children.get(assIndex) instanceof WssEncryptElement);
            WssEncryptElement assertion = (WssEncryptElement) children.get(assIndex);
            assertTrue("Assertion targets response", Assertion.isResponse(assertion));
            assertEquals("Xpath value", "/soapenv:Envelope/soapenv:Body[position()=1]", assertion.getXpathExpression().getExpression());
            assertEquals("Encryption type", "http://www.w3.org/2001/04/xmlenc#aes128-cbc", assertion.getXEncAlgorithm());
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is response encryption", children.get(assIndex) instanceof WssEncryptElement);
            WssEncryptElement assertion = (WssEncryptElement) children.get(assIndex);
            assertTrue("Assertion targets response", Assertion.isResponse(assertion));
            assertEquals("Xpath value", "/soapenv:Envelope/soapenv:Body[position()=1]", assertion.getXpathExpression().getExpression());
            assertEquals("Encryption type", "http://www.w3.org/2001/04/xmlenc#aes128-cbc", assertion.getXEncAlgorithm());
            assertEquals("Recipient context", recipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is response signature", children.get(assIndex) instanceof WssSignElement);
            WssSignElement assertion = (WssSignElement) children.get(assIndex);
            assertTrue("Assertion targets response", Assertion.isResponse(assertion));
            assertFalse("Protect tokens", assertion.isProtectTokens());
            assertEquals("Key reference type", "BinarySecurityToken", assertion.getKeyReference());
            assertTrue("Key is default", assertion.isUsesDefaultKeyStore());
            assertEquals("Xpath value", "/soapenv:Envelope/soapenv:Body[position()=1]", assertion.getXpathExpression().getExpression());
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is response signature", children.get(assIndex) instanceof WssSignElement);
            WssSignElement assertion = (WssSignElement) children.get(assIndex);
            assertTrue("Assertion targets response", Assertion.isResponse(assertion));
            assertFalse("Protect tokens", assertion.isProtectTokens());
            assertEquals("Key reference type", "BinarySecurityToken", assertion.getKeyReference());
            assertFalse("Key is default", assertion.isUsesDefaultKeyStore());
            assertEquals("Key alias", "alice", assertion.getKeyAlias());
            assertEquals("Xpath value", "/soapenv:Envelope/soapenv:Body[position()=1]", assertion.getXpathExpression().getExpression());
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is response signature", children.get(assIndex) instanceof WssSignElement);
            WssSignElement assertion = (WssSignElement) children.get(assIndex);
            assertTrue("Assertion targets response", Assertion.isResponse(assertion));
            assertFalse("Protect tokens", assertion.isProtectTokens());
            assertEquals("Key reference type", "BinarySecurityToken", assertion.getKeyReference());
            assertTrue("Key is default", assertion.isUsesDefaultKeyStore());
            assertEquals("Xpath value", "/soapenv:Envelope/soapenv:Body[position()=1]", assertion.getXpathExpression().getExpression());
            assertEquals("Recipient context", recipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is response security token", children.get(assIndex) instanceof AddWssSecurityToken);
            AddWssSecurityToken assertion = (AddWssSecurityToken) children.get(assIndex);
            assertTrue("Assertion targets response", Assertion.isResponse(assertion));
            assertEquals("Token type", SecurityTokenType.WSS_USERNAME, assertion.getTokenType());
            assertEquals("Key reference type", "SubjectKeyIdentifier", assertion.getKeyReference());
            assertTrue("Key is default", assertion.isUsesDefaultKeyStore());
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is response security token", children.get(assIndex) instanceof AddWssSecurityToken);
            AddWssSecurityToken assertion = (AddWssSecurityToken) children.get(assIndex);
            assertTrue("Assertion targets response", Assertion.isResponse(assertion));
            assertEquals("Token type", SecurityTokenType.WSS_USERNAME, assertion.getTokenType());
            assertEquals("Key reference type", "SubjectKeyIdentifier", assertion.getKeyReference());
            assertFalse("Key is default", assertion.isUsesDefaultKeyStore());
            assertEquals("Key alias", "alice", assertion.getKeyAlias());
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }
        
        assIndex++;
        {
            assertTrue("Assertion is response security token", children.get(assIndex) instanceof AddWssSecurityToken);
            AddWssSecurityToken assertion = (AddWssSecurityToken) children.get(assIndex);
            assertTrue("Assertion targets response", Assertion.isResponse(assertion));
            assertEquals("Token type", SecurityTokenType.WSS_USERNAME, assertion.getTokenType());
            assertEquals("Key reference type", "SubjectKeyIdentifier", assertion.getKeyReference());
            assertTrue("Key is default", assertion.isUsesDefaultKeyStore());
            assertEquals("Recipient context", recipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is response timestamp", children.get(assIndex) instanceof AddWssTimestamp);
            AddWssTimestamp assertion = (AddWssTimestamp) children.get(assIndex);
            assertTrue("Assertion targets response", Assertion.isResponse(assertion));
            assertEquals("Key reference type", "SubjectKeyIdentifier", assertion.getKeyReference());
            assertTrue("Key is default", assertion.isUsesDefaultKeyStore());
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is response timestamp", children.get(assIndex) instanceof AddWssTimestamp);
            AddWssTimestamp assertion = (AddWssTimestamp) children.get(assIndex);
            assertTrue("Assertion targets response", Assertion.isResponse(assertion));
            assertEquals("Key reference type", "SubjectKeyIdentifier", assertion.getKeyReference());
            assertFalse("Key is default", assertion.isUsesDefaultKeyStore());
            assertEquals("Key alias", "alice", assertion.getKeyAlias());
            assertEquals("Recipient context", localRecipientContext, assertion.getRecipientContext() );
        }

        assIndex++;
        {
            assertTrue("Assertion is response timestamp", children.get(assIndex) instanceof AddWssTimestamp);
            AddWssTimestamp assertion = (AddWssTimestamp) children.get(assIndex);
            assertTrue("Assertion targets response", Assertion.isResponse(assertion));
            assertEquals("Key reference type", "SubjectKeyIdentifier", assertion.getKeyReference());
            assertTrue("Key is default", assertion.isUsesDefaultKeyStore());
            assertEquals("Recipient context", recipientContext, assertion.getRecipientContext() );
        }
    }


    /* TODO figure out where to put this
    public void testEqualityRename() throws Exception {
        String policy = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "                <L7p:EqualityAssertion>\n" +
                "                    <L7p:Expression1 stringValue=\"foo\"/>\n" +
                "                    <L7p:Expression2 stringValue=\"bar\"/>\n" +
                "                </L7p:EqualityAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        Document doc = XmlUtil.parse(new StringReader(policy), false);
        AllAssertion ass = (AllAssertion)wspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 3.4 policy XML:\n" + ass);
        assertTrue(ass != null);
        ComparisonAssertion comp = (ComparisonAssertion)ass.getChildren().get(0);
        assertEquals(comp.getExpression1(), "foo");
        assertEquals(comp.getExpression2(), "bar");
        assertEquals(comp.getOperator(), ComparisonOperator.EQ);
        assertFalse(comp.isNegate());
    }
    */

    @Test
    public void testUnknownElementGetsPreserved() throws Exception {


        String policyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/200" +
                "2/12/policy\">\n" +
                "    <All>\n" +
                "        <HttpRoutingAssertion>\n" +
                "            <ProtectedServiceUrl stringValue=\"http://hugh/ACMEWarehouseWS/Service1.asmx\"/>\n" +
                "        </HttpRoutingAssertion>\n" +
                "        <EmailAlert>\n" +
                "            <Message stringValue=\"Woot!  Hola!\n" +
                "\n" +
                "Blah blah blah!\n" +
                "\"/>\n" +
                "            <TargetEmailAddress stringValue=\"mlyons@layer7-tech.com\"/>\n" +
                "            <SourceEmailAddress stringValue=\"mlyons-4@layer7-tech.com\"/>\n" +
                "            <Subject stringValue=\"ALERT ALERT from EmailAlertAssertion asdfhasdhf\"/>\n" +
                "        </EmailAlert>\n" +
                "    </All>\n" +
                "</wsp:Policy>\n";

        Assertion p = wspReader.parsePermissively(policyXml, INCLUDE_DISABLED);
        String parsed1 = p.toString();
        log.info("Parsed data including unknown element: " + parsed1);

        String out = WspWriter.getPolicyXml(p);
        Assertion p2 = wspReader.parsePermissively(out, INCLUDE_DISABLED);
        String parsed2 = p2.toString();
        log.info("After reparsing: " + parsed2);

        assertEquals(parsed1, parsed2);

    }

    @Test
    public void testPreserveRequestXpathExpressionBug1894() throws Exception {
        final String xp = "//asdfasdf/foo/bar/zort";
        final String ns1 = "urn:fasdfasdfasdaqqthf";
        final String ns2 = "urn:kjhakjshdfaksqgergqergqegrd";
        Map nsmap = new HashMap();
        nsmap.put("ns1", ns1);
        nsmap.put("ns2", ns2);
        RequestXpathAssertion ass = new RequestXpathAssertion(new XpathExpression(xp, nsmap));
        String policyXml = WspWriter.getPolicyXml(ass);
        log.info("Serialized policy XML: " + policyXml);
        RequestXpathAssertion got = (RequestXpathAssertion)wspReader.parsePermissively(policyXml, INCLUDE_DISABLED);
        final String gotXpath = got.getXpathExpression().getExpression();
        log.info("Parsed xpath: " + gotXpath);
        final Map gotNsmap = got.getXpathExpression().getNamespaces();
        log.info("Parsed nsmap: " + gotNsmap);
        assertEquals(xp, gotXpath);
        assertEquals(ns1, gotNsmap.get("ns1"));
        assertEquals(ns2, gotNsmap.get("ns2"));
    }

    @Test
    public void testReproBug2215() throws Exception {
        final String policyxml = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <wsse:Integrity wsp:Usage=\"wsp:Required\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
                "            <L7p:xmlSecurityRecipientContext xmlSecurityRecipientContext=\"included\">\n" +
                "                <L7p:Base64edX509Certificate stringValue=\"MIIEdTCCA96gAwIBAgIQFs1YCiUfRT3jH24nbk09TzANBgkqhkiG9w0BAQUFADCBujEfMB0GA1UEChMWVmVyaVNpZ24gVHJ1c3QgTmV0d29yazEXMBUGA1UECxMOVmVyaVNpZ24sIEluYy4xMzAxBgNVBAsTKlZlcmlTaWduIEludGVybmF0aW9uYWwgU2VydmVyIENBIC0gQ2xhc3MgMzFJMEcGA1UECxNAd3d3LnZlcmlzaWduLmNvbS9DUFMgSW5jb3JwLmJ5IFJlZi4gTElBQklMSVRZIExURC4oYyk5NyBWZXJpU2lnbjAeFw0wNDAzMDMwMDAwMDBaFw0wNjAzMDMyMzU5NTlaMIG5MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTESMBAGA1UEBxQJUGFsbyBBbHRvMRUwEwYDVQQKFAxQYXlwYWwsIEluYy4xHDAaBgNVBAsUE0luZm9ybWF0aW9uIFN5c3RlbXMxMzAxBgNVBAsUKlRlcm1zIG9mIHVzZSBhdCB3d3cudmVyaXNpZ24uY29tL3JwYSAoYykwMDEXMBUGA1UEAxQOd3d3LnBheXBhbC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKbFw8ZbqbLIHOd+alJfxK5Ig4opcjPdE4eL9NVh4Tm2bfXou0uy7XSuo4dYqCCXfod5bxU351pLepuXOLhqOoy6ENk68ZlK9IHNkobtjTTDLln0uUfTv718unovVLEIWjYdlVNlcshbd1ttdd/SPlXm2N4f3icrXLWdEesJsAApAgMBAAGjggF5MIIBdTAJBgNVHRMEAjAAMAsGA1UdDwQEAwIFoDBGBgNVHR8EPzA9MDugOaA3hjVodHRwOi8vY3JsLnZlcmlzaWduLmNvbS9DbGFzczNJbnRlcm5hdGlvbmFsU2VydmVyLmNybDBEBgNVHSAEPTA7MDkGC2CGSAGG+EUBBxcDMCowKAYIKwYBBQUHAgEWHGh0dHBzOi8vd3d3LnZlcmlzaWduLmNvbS9ycGEwKAYDVR0lBCEwHwYJYIZIAYb4QgQBBggrBgEFBQcDAQYIKwYBBQUHAwIwNAYIKwYBBQUHAQEEKDAmMCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC52ZXJpc2lnbi5jb20wbQYIKwYBBQUHAQwEYTBfoV2gWzBZMFcwVRYJaW1hZ2UvZ2lmMCEwHzAHBgUrDgMCGgQUj+XTGoasjY5rw8+AatRIGCx7GS4wJRYjaHR0cDovL2xvZ28udmVyaXNpZ24uY29tL3ZzbG9nby5naWYwDQYJKoZIhvcNAQEFBQADgYEAR46ofsDJMdV7FIGZ98O3dkEFTkD9FrRE6XWVX2LebPvBebONSpIeGEjV/hJ919eGnFjujTmN1Pn98/G+xUBQeFsSN/3mdgujE5Yg4h4Zc8UXTOzlKUL/0H/xnU5gkZvtX0qJjylqrugb3+yaVXFAC7DU/2oZ/wSFFHCBmwm7QF8=\"/>\n" +
                "                <L7p:Actor stringValue=\"fdsfd\"/>\n" +
                "            </L7p:xmlSecurityRecipientContext>\n" +
                "            <wsse:MessageParts\n" +
                "                Dialect=\"http://www.w3.org/TR/1999/REC-xpath-19991116\"\n" +
                "                xmlns:e=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                "                xmlns:s0=\"http://warehouse.acme.com/ws\"\n" +
                "                xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                "                xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">/soapenv:Envelope/soapenv:Body</wsse:MessageParts>\n" +
                "        </wsse:Integrity>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        Assertion p = wspReader.parsePermissively(policyxml, INCLUDE_DISABLED);
        AllAssertion root = (AllAssertion)p;
        RequireWssSignedElement rwi = (RequireWssSignedElement)root.children().next();
        assertTrue(rwi.getRecipientContext().getActor().equals("fdsfd"));
    }

    @Test
    public void testReproBug2215ForSAML() throws Exception {
        final String policyxml = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <wsse:SecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "            <wsse:TokenType>urn:oasis:names:tc:SAML:1.0:assertion#Assertion</wsse:TokenType>\n" +
            "            <L7p:SamlParams>\n" +
            "                <L7p:AudienceRestriction stringValue=\"\"/>\n" +
            "                <L7p:SubjectConfirmations stringArrayValue=\"included\">\n" +
            "                    <L7p:item stringValue=\"urn:oasis:names:tc:SAML:1.0:cm:holder-of-key\"/>\n" +
            "                </L7p:SubjectConfirmations>\n" +
            "                <L7p:RecipientContext xmlSecurityRecipientContext=\"included\">\n" +
            "                    <L7p:Base64edX509Certificate stringValue=\"MIIEdTCCA96gAwIBAgIQFs1YCiUfRT3jH24nbk09TzANBgkqhkiG9w0BAQUFADCBujEfMB0GA1UEChMWVmVyaVNpZ24gVHJ1c3QgTmV0d29yazEXMBUGA1UECxMOVmVyaVNpZ24sIEluYy4xMzAxBgNVBAsTKlZlcmlTaWduIEludGVybmF0aW9uYWwgU2VydmVyIENBIC0gQ2xhc3MgMzFJMEcGA1UECxNAd3d3LnZlcmlzaWduLmNvbS9DUFMgSW5jb3JwLmJ5IFJlZi4gTElBQklMSVRZIExURC4oYyk5NyBWZXJpU2lnbjAeFw0wNDAzMDMwMDAwMDBaFw0wNjAzMDMyMzU5NTlaMIG5MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTESMBAGA1UEBxQJUGFsbyBBbHRvMRUwEwYDVQQKFAxQYXlwYWwsIEluYy4xHDAaBgNVBAsUE0luZm9ybWF0aW9uIFN5c3RlbXMxMzAxBgNVBAsUKlRlcm1zIG9mIHVzZSBhdCB3d3cudmVyaXNpZ24uY29tL3JwYSAoYykwMDEXMBUGA1UEAxQOd3d3LnBheXBhbC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKbFw8ZbqbLIHOd+alJfxK5Ig4opcjPdE4eL9NVh4Tm2bfXou0uy7XSuo4dYqCCXfod5bxU351pLepuXOLhqOoy6ENk68ZlK9IHNkobtjTTDLln0uUfTv718unovVLEIWjYdlVNlcshbd1ttdd/SPlXm2N4f3icrXLWdEesJsAApAgMBAAGjggF5MIIBdTAJBgNVHRMEAjAAMAsGA1UdDwQEAwIFoDBGBgNVHR8EPzA9MDugOaA3hjVodHRwOi8vY3JsLnZlcmlzaWduLmNvbS9DbGFzczNJbnRlcm5hdGlvbmFsU2VydmVyLmNybDBEBgNVHSAEPTA7MDkGC2CGSAGG+EUBBxcDMCowKAYIKwYBBQUHAgEWHGh0dHBzOi8vd3d3LnZlcmlzaWduLmNvbS9ycGEwKAYDVR0lBCEwHwYJYIZIAYb4QgQBBggrBgEFBQcDAQYIKwYBBQUHAwIwNAYIKwYBBQUHAQEEKDAmMCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC52ZXJpc2lnbi5jb20wbQYIKwYBBQUHAQwEYTBfoV2gWzBZMFcwVRYJaW1hZ2UvZ2lmMCEwHzAHBgUrDgMCGgQUj+XTGoasjY5rw8+AatRIGCx7GS4wJRYjaHR0cDovL2xvZ28udmVyaXNpZ24uY29tL3ZzbG9nby5naWYwDQYJKoZIhvcNAQEFBQADgYEAR46ofsDJMdV7FIGZ98O3dkEFTkD9FrRE6XWVX2LebPvBebONSpIeGEjV/hJ919eGnFjujTmN1Pn98/G+xUBQeFsSN/3mdgujE5Yg4h4Zc8UXTOzlKUL/0H/xnU5gkZvtX0qJjylqrugb3+yaVXFAC7DU/2oZ/wSFFHCBmwm7QF8=\"/>\n" +
            "                    <L7p:Actor stringValue=\"ppal\"/>\n" +
            "                </L7p:RecipientContext>\n" +
            "                <L7p:NameFormats stringArrayValue=\"included\">\n" +
            "                    <L7p:item stringValue=\"urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName\"/>\n" +
            "                </L7p:NameFormats>\n" +
            "                <L7p:RequireHolderOfKeyWithMessageSignature booleanValue=\"true\"/>\n" +
            "                <L7p:AuthenticationStatement samlAuthenticationInfo=\"included\">\n" +
            "                    <L7p:AuthenticationMethods stringArrayValue=\"included\">\n" +
            "                        <L7p:item stringValue=\"urn:oasis:names:tc:SAML:1.0:am:HardwareToken\"/>\n" +
            "                    </L7p:AuthenticationMethods>\n" +
            "                </L7p:AuthenticationStatement>\n" +
            "                <L7p:NameQualifier stringValue=\"\"/>\n" +
            "            </L7p:SamlParams>\n" +
            "        </wsse:SecurityToken>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

        Assertion p = wspReader.parsePermissively(policyxml, INCLUDE_DISABLED);
        AllAssertion root = (AllAssertion)p;

        RequireWssSaml rwi = (RequireWssSaml)root.children().next();
        assertTrue(rwi.getRecipientContext().getActor().equals("ppal"));
    }

    @Test
    public void testReproBug2214TabsInEmail() throws Exception {
        final String body = "foo\r\nbar baz blah\tbleet blot";

        EmailAlertAssertion ema = new EmailAlertAssertion();
        ema.setSubject("Hi there");
        ema.setTargetEmailAddress("blah@blah.example.com");
        ema.messageString(body);

        String emXml = WspWriter.getPolicyXml(ema);
        EmailAlertAssertion got = (EmailAlertAssertion)wspReader.parseStrictly(emXml, INCLUDE_DISABLED);

        assertEquals(got.messageString(), body);
    }

    @Test
    public void testSslAssertionOptionChange() throws Exception {
        SslAssertion sa = new SslAssertion(SslAssertion.OPTIONAL);
        String got = WspWriter.getPolicyXml(sa);
        assertNotNull(got);
        assertTrue(got.contains("SslAssertion"));
        assertTrue(got.contains("Optional"));

        SslAssertion sa2 = (SslAssertion)wspReader.parseStrictly(got, INCLUDE_DISABLED);
        assertEquals(sa2.getOption(), SslAssertion.OPTIONAL);
    }

    @Test
    public void testXsltFrom35Static() throws Exception {
        String xsl35 = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">" +
                "<L7p:XslTransformation>\n" +
                "            <L7p:Direction intValue=\"1\"/>\n" +
                "            <L7p:XslSrc stringValue=\"&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                "&lt;xsl:transform version=&quot;1.0&quot; xmlns:xsl=&quot;http://www.w3.org/1999/XSL/Transform&quot;\n" +
                "                             xmlns:soapenv=&quot;http://schemas.xmlsoap.org/soap/envelope/&quot;&gt;\n" +
                "        &lt;xsl:template match=&quot;/&quot;&gt;\n" +
                "                &lt;xsl:copy&gt;\n" +
                "                        &lt;xsl:apply-templates/&gt;\n" +
                "                &lt;/xsl:copy&gt;\n" +
                "        &lt;/xsl:template&gt;\n" +
                "        &lt;xsl:template match=&quot;soapenv:Body&quot;&gt;\n" +
                "                &lt;xsl:copy&gt;\n" +
                "                        &lt;xsl:apply-templates select=&quot;node()|@*&quot; /&gt;\n" +
                "                &lt;/xsl:copy&gt;\n" +
                "        &lt;xsl:comment&gt;SSG WAS HERE&lt;/xsl:comment&gt;\n" +
                "        &lt;/xsl:template&gt;\n" +
                "        &lt;xsl:template match=&quot;node()|@*&quot;&gt;\n" +
                "                &lt;xsl:copy&gt;\n" +
                "                        &lt;xsl:apply-templates select=&quot;node()|@*&quot; /&gt;\n" +
                "                &lt;/xsl:copy&gt;\n" +
                "        &lt;/xsl:template&gt;\n" +
                "&lt;/xsl:transform&gt;\"/>\n" +
                "            <L7p:TransformName stringValue=\"\"/>\n" +
                "            <L7p:FetchUrlRegexes stringArrayValue=\"included\"/>\n" +
                "        </L7p:XslTransformation>" +
                "</wsp:Policy>";
        XslTransformation xslt = (XslTransformation)wspReader.parseStrictly(xsl35, INCLUDE_DISABLED);
        assertNotNull(xslt);
        System.out.println(WspWriter.getPolicyXml(xslt));
    }

    @Test
    public void testXsltFrom35Fetchingly() throws Exception {
        String xsl35 = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <L7p:XslTransformation>\n" +
                "            <L7p:Direction intValue=\"1\"/>\n" +
                "            <L7p:FetchAllowWithoutStylesheet booleanValue=\"false\"/>\n" +
                "            <L7p:FetchXsltFromMessageUrls booleanValue=\"true\"/>\n" +
                "            <L7p:FetchUrlRegexes stringArrayValue=\"included\">\n" +
                "                <L7p:item stringValue=\".*\"/>\n" +
                "            </L7p:FetchUrlRegexes>\n" +
                "        </L7p:XslTransformation>\n" +
                "</wsp:Policy>";
        XslTransformation xslt = (XslTransformation)wspReader.parseStrictly(xsl35, INCLUDE_DISABLED);
        assertNotNull(xslt);
        System.out.println(WspWriter.getPolicyXml(xslt));
    }

    @Test
    public void testPolicyFromBug2160() throws Exception {
        String policy = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Policy xmlns=\"http://www.layer7tech.com/ws/policy\">\n" +
                "    <All>\n" +
                "        <XmlRequestSecurity>\n" +
                "            <Elements elementSecurityArrayValue=\"included\">\n" +
                "                <item elementSecurityValue=\"included\">\n" +
                "                    <Cipher stringValue=\"AES\"/>\n" +
                "                    <KeyLength intValue=\"128\"/>\n" +
                "                    <PreconditionXpath xpathExpressionValueNull=\"null\"/>\n" +
                "                    <ElementXpath xpathExpressionValue=\"included\">\n" +
                "                        <Expression stringValue=\"/soapenv:Envelope\"/>\n" +
                "                        <Namespaces mapValue=\"included\">\n" +
                "                            <entry>\n" +
                "                                <key stringValue=\"xsi\"/>\n" +
                "                                <value stringValue=\"http://www.w3.org/2001/XMLSchema-instance\"/>\n" +
                "                            </entry>\n" +
                "                            <entry>\n" +
                "                                <key stringValue=\"xsd\"/>\n" +
                "                                <value stringValue=\"http://www.w3.org/2001/XMLSchema\"/>\n" +
                "                            </entry>\n" +
                "                            <entry>\n" +
                "                                <key stringValue=\"tns1\"/>\n" +
                "                                <value stringValue=\"http://echo.l7tech.com\"/>\n" +
                "                            </entry>\n" +
                "                            <entry>\n" +
                "                                <key stringValue=\"soapenv\"/>\n" +
                "                                <value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
                "                            </entry>\n" +
                "                        </Namespaces>\n" +
                "                    </ElementXpath>\n" +
                "                    <Encryption booleanValue=\"false\"/>\n" +
                "                </item>\n" +
                "            </Elements>\n" +
                "        </XmlRequestSecurity>\n" +
                "        <SpecificUser>\n" +
                "            <UserLogin stringValue=\"test\"/>\n" +
                "            <IdentityProviderOid longValue=\"-2\"/>\n" +
                "        </SpecificUser>\n" +
                "        <HttpRoutingAssertion>\n" +
                "            <AttachSamlSenderVouches booleanValue=\"false\"/>\n" +
                "            <UserAgent stringValueNull=\"null\"/>\n" +
                "            <ProtectedServiceUrl stringValue=\"http://sat.test.example.com:8080/axis/services/Echo\"/>\n" +
                "            <Login stringValueNull=\"null\"/>\n" +
                "            <GroupMembershipStatement booleanValue=\"false\"/>\n" +
                "            <Password stringValueNull=\"null\"/>\n" +
                "            <Realm stringValueNull=\"null\"/>\n" +
                "            <MaxConnections intValue=\"100\"/>\n" +
                "            <SamlAssertionExpiry intValue=\"5\"/>\n" +
                "        </HttpRoutingAssertion>\n" +
                "        <XmlResponseSecurity>\n" +
                "            <Elements elementSecurityArrayValue=\"included\">\n" +
                "                <item elementSecurityValue=\"included\">\n" +
                "                    <Cipher stringValue=\"AES\"/>\n" +
                "                    <KeyLength intValue=\"128\"/>\n" +
                "                    <PreconditionXpath xpathExpressionValueNull=\"null\"/>\n" +
                "                    <ElementXpath xpathExpressionValue=\"included\">\n" +
                "                        <Expression stringValue=\"/soapenv:Envelope\"/>\n" +
                "                        <Namespaces mapValue=\"included\">\n" +
                "                            <entry>\n" +
                "                                <key stringValue=\"xsi\"/>\n" +
                "                                <value stringValue=\"http://www.w3.org/2001/XMLSchema-instance\"/>\n" +
                "                            </entry>\n" +
                "                            <entry>\n" +
                "                                <key stringValue=\"xsd\"/>\n" +
                "                                <value stringValue=\"http://www.w3.org/2001/XMLSchema\"/>\n" +
                "                            </entry>\n" +
                "                            <entry>\n" +
                "                                <key stringValue=\"tns1\"/>\n" +
                "                                <value stringValue=\"http://echo.l7tech.com\"/>\n" +
                "                            </entry>\n" +
                "                            <entry>\n" +
                "                                <key stringValue=\"soapenv\"/>\n" +
                "                                <value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
                "                            </entry>\n" +
                "                        </Namespaces>\n" +
                "                    </ElementXpath>\n" +
                "                    <Encryption booleanValue=\"false\"/>\n" +
                "                </item>\n" +
                "            </Elements>\n" +
                "        </XmlResponseSecurity>\n" +
                "    </All>\n" +
                "</Policy>";

        Assertion got = wspReader.parseStrictly(policy, INCLUDE_DISABLED);
        assertNotNull(got);
        log.info("Parsed policy from Bug #2160: " + WspWriter.getPolicyXml(got));
    }

    @Test
    public void testResourceInfo() throws Exception {
        tryIt(WspWriterTest.makeStaticInfo());
        tryIt(WspWriterTest.makeMessageInfo());
        tryIt(WspWriterTest.makeSingleInfo());
    }

    private void tryIt(AssertionResourceInfo rinfo) throws IOException {
        XslTransformation xslt = new XslTransformation();
        xslt.setResourceInfo(rinfo);
        String policy = WspWriter.getPolicyXml(xslt);
        XslTransformation newXslt = (XslTransformation) wspReader.parseStrictly(policy, INCLUDE_DISABLED);
        assertTrue(newXslt.getResourceInfo().getType().equals(xslt.getResourceInfo().getType()));
    }

    @Test
    public void testBug3456() throws Exception {
        try {
            WspReader.getDefault().parseStrictly(BUG_3456_POLICY, INCLUDE_DISABLED);
            fail("Expected exception not thrown for invalid attribute Unknown HtmlFormDataType name: 'string (any)'");
        } catch (InvalidPolicyStreamException e) {
            log.log(Level.INFO, "Caught expected exception: " + ExceptionUtils.getMessage(e), e);
            // Ok
        }

        Assertion got = WspReader.getDefault().parsePermissively(BUG_3456_POLICY, INCLUDE_DISABLED);
        log.info("Got: " + got);
    }

    @Test
    public void testFilterSomeDisabledAssertions() throws Exception {
        Assertion got = WspReader.getDefault().parseStrictly(MIX_OF_ENABLED_AND_DISABLED, OMIT_DISABLED);
        Iterator<Assertion> it = got.preorderIterator();
        while (it.hasNext()) {
            Assertion assertion = it.next();
            assertTrue("Disabled assertion should not have been included in output", assertion.isEnabled());
        }
    }

    @Test
    public void testAllAssertionsDisabled() throws Exception {
        Assertion got = WspReader.getDefault().parseStrictly(ALL_DISABLED, OMIT_DISABLED);
        log.info("Got: " + got);
        assertNull("A policy with all assertions disabled should filter down to null", got);
    }

    @Test
    public void testOneEnabledOneDisabled() throws Exception {
        Assertion got = WspReader.getDefault().parseStrictly(ONE_DISABLED_ONE_ENABLED, OMIT_DISABLED);
        got = Assertion.simplify(got, true, false);
        assertTrue("Should simplify down to TrueAssertion, the only one that was enabled", got instanceof TrueAssertion);
    }
    
    @Test
    public void testBug3637SchemaParse() throws Exception {
        AllAssertion all = (AllAssertion)WspReader.getDefault().parsePermissively(BUG_3637_SCHEMA_PARSING_PROBLEM, INCLUDE_DISABLED);
        SchemaValidation sv = (SchemaValidation)all.getChildren().iterator().next();
        AssertionResourceInfo ri = sv.getResourceInfo();
        assertEquals(AssertionResourceType.STATIC, ri.getType());
        StaticResourceInfo sri = (StaticResourceInfo)ri;
        assertNotNull(sri.getDocument());
    }

    @BugNumber(8933)
    @Test
    public void testReadPolicyWithInvalidXMLCharacters() throws Exception {
        final AllAssertion all = (AllAssertion)WspReader.getDefault().parsePermissively(INVALID_XML_ESCAPED, INCLUDE_DISABLED);
        final CommentAssertion commentAssertion = (CommentAssertion)all.getChildren().iterator().next();
        assertNotNull( "Comment", commentAssertion.getAssertionComment() );
        assertEquals( "Comment text", "This is not valid for XML -> \u0006", commentAssertion.getAssertionComment().getAssertionComment( Assertion.Comment.RIGHT_COMMENT ) );
    }

    @Test
    public void testForEachRoundTrip() throws Exception {
        final ForEachLoopAssertion ass = new ForEachLoopAssertion(Arrays.asList(new FalseAssertion()));
        ass.setIterationLimit(3);
        ass.setLoopVariableName("things");
        ass.setVariablePrefix("i");
        Assertion got = WspReader.getDefault().parsePermissively(WspWriter.getPolicyXml(ass), WspReader.Visibility.omitDisabled);
        assertTrue(got instanceof ForEachLoopAssertion);
        assertEquals("things", ((ForEachLoopAssertion)got).getLoopVariableName());
    }

    @Test
    public void testOmitDisabledPreservesAssertionOrdinals() throws Exception {
        Assertion disabledAll;
        AllAssertion root = new AllAssertion(Arrays.asList(
            new OneOrMoreAssertion(Arrays.asList(
                new TrueAssertion(),
                disabledAll = new AllAssertion(Arrays.asList(
                    new TrueAssertion(),
                    new FalseAssertion()
                ))
            )),
            new TrueAssertion()
        ));
        disabledAll.setEnabled(false);

        // Get policy XML
        String policyXml = WspWriter.getPolicyXml(root);

        AllAssertion includeDisabled = (AllAssertion) WspReader.getDefault().parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
        AllAssertion omitDisabled = (AllAssertion) WspReader.getDefault().parseStrictly(policyXml, WspReader.OMIT_DISABLED);

        Assertion laterOnFromIncludeDisabled = includeDisabled.getChildren().get(1);
        Assertion laterOnFromOmitDisabled = omitDisabled.getChildren().get(1);

        assertEquals("Assertion ordinals should remain the same when disabled assertions are omitted at parsing time", laterOnFromIncludeDisabled.getOrdinal(), laterOnFromOmitDisabled.getOrdinal());
    }


    private static final String BUG_3456_POLICY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
    "    <wsp:All wsp:Usage=\"Required\">\n" +
    "        <L7p:HtmlFormDataAssertion>\n" +
    "            <L7p:AllowPost booleanValue=\"true\"/>\n" +
    "            <L7p:AllowGet booleanValue=\"true\"/>\n" +
    "            <L7p:FieldSpecs htmlFormFieldSpecArray=\"included\">\n" +
    "                <L7p:item htmlFormFieldSpec=\"included\">\n" +
    "                    <L7p:MaxOccurs intValue=\"1\"/>\n" +
    "\n" +
    "                    <L7p:DataType fieldDataType=\"string (any)\"/>\n" +
    "                    <L7p:Name stringValue=\"param1\"/>\n" +
    "                </L7p:item>\n" +
    "                <L7p:item htmlFormFieldSpec=\"included\">\n" +
    "                    <L7p:MaxOccurs intValue=\"4\"/>\n" +
    "                    <L7p:DataType fieldDataType=\"string (any)\"/>\n" +
    "                    <L7p:MinOccurs intValue=\"2\"/>\n" +
    "                    <L7p:Name stringValue=\"param2\"/>\n" +
    "                </L7p:item>\n" +
    "\n" +
    "            </L7p:FieldSpecs>\n" +
    "        </L7p:HtmlFormDataAssertion>\n" +
    "        <L7p:HttpRoutingAssertion>\n" +
    "            <L7p:ProtectedServiceUrl stringValue=\"http://hugh:8081/RoutingExtensionsTest\"/>\n" +
    "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
    "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
    "                    <L7p:item httpPassthroughRule=\"included\">\n" +
    "                        <L7p:Name stringValue=\"Cookie\"/>\n" +
    "                    </L7p:item>\n" +
    "\n" +
    "                    <L7p:item httpPassthroughRule=\"included\">\n" +
    "                        <L7p:Name stringValue=\"SOAPAction\"/>\n" +
    "                    </L7p:item>\n" +
    "                </L7p:Rules>\n" +
    "            </L7p:RequestHeaderRules>\n" +
    "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
    "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
    "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
    "            </L7p:RequestParamRules>\n" +
    "\n" +
    "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
    "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
    "                    <L7p:item httpPassthroughRule=\"included\">\n" +
    "                        <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
    "                    </L7p:item>\n" +
    "                </L7p:Rules>\n" +
    "            </L7p:ResponseHeaderRules>\n" +
    "        </L7p:HttpRoutingAssertion>\n" +
    "    </wsp:All>\n" +
    "\n" +
    "</wsp:Policy>";

    //

    public static final String BUG_3637_SCHEMA_PARSING_PROBLEM =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SchemaValidation>\n" +
            "            <L7p:Schema stringValue=\"&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt; &lt;s:schema elementFormDefault=&quot;qualified&quot;     targetNamespace=&quot;http://www.xignite.com/services/1&quot;     xmlns:http=&quot;http://schemas.xmlsoap.org/wsdl/http/&quot;     xmlns:mime=&quot;http://schemas.xmlsoap.org/wsdl/mime/&quot;     xmlns:s=&quot;http://www.w3.org/2001/XMLSchema&quot;     xmlns:soap=&quot;http://schemas.xmlsoap.org/wsdl/soap/&quot;     xmlns:soapenc=&quot;http://schemas.xmlsoap.org/soap/encoding/&quot;     xmlns:tm=&quot;http://microsoft.com/wsdl/mime/textMatching/&quot;     xmlns:tns=&quot;http://www.xignite.com/services/1&quot; xmlns:wsdl=&quot;http://schemas.xmlsoap.org/wsdl/&quot;&gt;     &lt;s:element name=&quot;GetCriteria&quot;&gt;         &lt;s:complexType&gt;             &lt;s:sequence&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;1&quot;                     name=&quot;CriteriaType&quot; type=&quot;tns:CriteriaTypes&quot;/&gt;             &lt;/s:sequence&gt;         &lt;/s:complexType&gt;     &lt;/s:element&gt;     &lt;s:simpleType name=&quot;CriteriaTypes&quot;&gt;         &lt;s:restriction base=&quot;s:string&quot;&gt;             &lt;s:enumeration value=&quot;Region&quot;/&gt;             &lt;s:enumeration value=&quot;Division&quot;/&gt;             &lt;s:enumeration value=&quot;SubDivision&quot;/&gt;             &lt;s:enumeration value=&quot;City&quot;/&gt;             &lt;s:enumeration value=&quot;Source&quot;/&gt;             &lt;s:enumeration value=&quot;Language&quot;/&gt;         &lt;/s:restriction&gt;     &lt;/s:simpleType&gt;     &lt;s:complexType name=&quot;Common&quot;&gt;         &lt;s:sequence&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;1&quot; name=&quot;Outcome&quot; type=&quot;tns:OutcomeTypes&quot;/&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Message&quot; type=&quot;s:string&quot;/&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Identity&quot; type=&quot;s:string&quot;/&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;1&quot; name=&quot;Delay&quot; type=&quot;s:double&quot;/&gt;         &lt;/s:sequence&gt;     &lt;/s:complexType&gt;     &lt;s:simpleType name=&quot;OutcomeTypes&quot;&gt;         &lt;s:restriction base=&quot;s:string&quot;&gt;             &lt;s:enumeration value=&quot;Success&quot;/&gt;             &lt;s:enumeration value=&quot;SystemError&quot;/&gt;             &lt;s:enumeration value=&quot;RequestError&quot;/&gt;             &lt;s:enumeration value=&quot;RegistrationError&quot;/&gt;         &lt;/s:restriction&gt;     &lt;/s:simpleType&gt;     &lt;s:complexType name=&quot;ArrayOfCriterion&quot;&gt;         &lt;s:sequence&gt;             &lt;s:element maxOccurs=&quot;unbounded&quot; minOccurs=&quot;0&quot;                 name=&quot;Criterion&quot; nillable=&quot;true&quot; type=&quot;tns:Criterion&quot;/&gt;         &lt;/s:sequence&gt;     &lt;/s:complexType&gt;     &lt;s:complexType name=&quot;Criterion&quot;&gt;         &lt;s:sequence&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Name&quot; type=&quot;s:string&quot;/&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;1&quot; name=&quot;Count&quot; type=&quot;s:int&quot;/&gt;         &lt;/s:sequence&gt;     &lt;/s:complexType&gt;     &lt;s:element name=&quot;Header&quot; type=&quot;tns:Header&quot;/&gt;     &lt;s:complexType name=&quot;Header&quot;&gt;         &lt;s:sequence&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Username&quot; type=&quot;s:string&quot;/&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Password&quot; type=&quot;s:string&quot;/&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Tracer&quot; type=&quot;s:string&quot;/&gt;         &lt;/s:sequence&gt;     &lt;/s:complexType&gt;     &lt;s:element name=&quot;GetStatistics&quot;&gt;         &lt;s:complexType&gt;             &lt;s:sequence&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;1&quot;                     name=&quot;CriteriaType&quot; type=&quot;tns:CriteriaTypes&quot;/&gt;             &lt;/s:sequence&gt;         &lt;/s:complexType&gt;     &lt;/s:element&gt;     &lt;s:complexType name=&quot;ArrayOfStatistic&quot;&gt;         &lt;s:sequence&gt;             &lt;s:element maxOccurs=&quot;unbounded&quot; minOccurs=&quot;0&quot;                 name=&quot;Statistic&quot; nillable=&quot;true&quot; type=&quot;tns:Statistic&quot;/&gt;         &lt;/s:sequence&gt;     &lt;/s:complexType&gt;     &lt;s:complexType name=&quot;Statistic&quot;&gt;         &lt;s:sequence&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Period&quot; type=&quot;s:string&quot;/&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Name&quot; type=&quot;s:string&quot;/&gt;             &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;1&quot; name=&quot;Count&quot; type=&quot;s:int&quot;/&gt;         &lt;/s:sequence&gt;     &lt;/s:complexType&gt;     &lt;s:element name=&quot;GetArticle&quot;&gt;         &lt;s:complexType&gt;             &lt;s:sequence&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;ArticleId&quot; type=&quot;s:string&quot;/&gt;             &lt;/s:sequence&gt;         &lt;/s:complexType&gt;     &lt;/s:element&gt;     &lt;s:element name=&quot;Search&quot;&gt;         &lt;s:complexType&gt;             &lt;s:sequence&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                     name=&quot;PublishedAfterDate&quot; type=&quot;s:string&quot;/&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                     name=&quot;PublishedBeforeDate&quot; type=&quot;s:string&quot;/&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Region&quot; type=&quot;s:string&quot;/&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Division&quot; type=&quot;s:string&quot;/&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                     name=&quot;SubDivision&quot; type=&quot;s:string&quot;/&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Title&quot; type=&quot;s:string&quot;/&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;SubTitle&quot; type=&quot;s:string&quot;/&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;City&quot; type=&quot;s:string&quot;/&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Source&quot; type=&quot;s:string&quot;/&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Language&quot; type=&quot;s:string&quot;/&gt;             &lt;/s:sequence&gt;         &lt;/s:complexType&gt;     &lt;/s:element&gt;     &lt;s:complexType name=&quot;ArrayOfAbstract&quot;&gt;         &lt;s:sequence&gt;             &lt;s:element maxOccurs=&quot;unbounded&quot; minOccurs=&quot;0&quot;                 name=&quot;Abstract&quot; nillable=&quot;true&quot; type=&quot;tns:Abstract&quot;/&gt;         &lt;/s:sequence&gt;     &lt;/s:complexType&gt;     &lt;s:complexType name=&quot;Abstract&quot;&gt;         &lt;s:complexContent mixed=&quot;false&quot;&gt;             &lt;s:extension base=&quot;tns:Common&quot;&gt;                 &lt;s:sequence&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                         name=&quot;ArticleId&quot; type=&quot;s:string&quot;/&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                         name=&quot;InsertDate&quot; type=&quot;s:string&quot;/&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                         name=&quot;PublishDate&quot; type=&quot;s:string&quot;/&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Region&quot; type=&quot;s:string&quot;/&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                         name=&quot;Division&quot; type=&quot;s:string&quot;/&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                         name=&quot;SubDivision&quot; type=&quot;s:string&quot;/&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Title&quot; type=&quot;s:string&quot;/&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                         name=&quot;SubTitle&quot; type=&quot;s:string&quot;/&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Source&quot; type=&quot;s:string&quot;/&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;City&quot; type=&quot;s:string&quot;/&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                         name=&quot;Language&quot; type=&quot;s:string&quot;/&gt;                 &lt;/s:sequence&gt;             &lt;/s:extension&gt;         &lt;/s:complexContent&gt;     &lt;/s:complexType&gt;     &lt;s:element name=&quot;GetTopStories&quot;&gt;         &lt;s:complexType&gt;             &lt;s:sequence&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;1&quot;                     name=&quot;CriteriaType&quot; type=&quot;tns:CriteriaTypes&quot;/&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                     name=&quot;CategoryName&quot; type=&quot;s:string&quot;/&gt;                 &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;1&quot; name=&quot;StoryCount&quot; type=&quot;s:int&quot;/&gt;             &lt;/s:sequence&gt;         &lt;/s:complexType&gt;     &lt;/s:element&gt;     &lt;s:complexType name=&quot;ArrayOfCategory&quot;&gt;         &lt;s:sequence&gt;             &lt;s:element maxOccurs=&quot;unbounded&quot; minOccurs=&quot;0&quot;                 name=&quot;Category&quot; nillable=&quot;true&quot; type=&quot;tns:Category&quot;/&gt;         &lt;/s:sequence&gt;     &lt;/s:complexType&gt;     &lt;s:complexType name=&quot;Category&quot;&gt;         &lt;s:complexContent mixed=&quot;false&quot;&gt;             &lt;s:extension base=&quot;tns:Common&quot;&gt;                 &lt;s:sequence&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot; name=&quot;Name&quot; type=&quot;s:string&quot;/&gt;                     &lt;s:element maxOccurs=&quot;1&quot; minOccurs=&quot;0&quot;                         name=&quot;Abstracts&quot; type=&quot;tns:ArrayOfAbstract&quot;/&gt;                 &lt;/s:sequence&gt;             &lt;/s:extension&gt;         &lt;/s:complexContent&gt;     &lt;/s:complexType&gt; &lt;/s:schema&gt; \"/>\n" +
            "        </L7p:SchemaValidation>\n" +
            "        <L7p:HttpRoutingAssertion>\n" +
            "            <L7p:ProtectedServiceUrl stringValue=\"http://paul/performance/GetTopStories_small_RESPONSE.xml\"/>\n" +
            "            <L7p:Login stringValue=\"\"/>\n" +
            "            <L7p:Password stringValue=\"\"/>\n" +
            "            <L7p:Realm stringValue=\"\"/>\n" +
            "        </L7p:HttpRoutingAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";


    public static final String MIX_OF_ENABLED_AND_DISABLED =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:ExactlyOne wsp:Usage=\"Required\">\n" +
            "        <wsp:All wsp:Usage=\"Required\">\n" +
            "            <L7p:TrueAssertion><L7p:Enabled booleanValue=\"false\"/></L7p:TrueAssertion>\n" +
            "            <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
            "                <L7p:HttpBasic><L7p:Enabled booleanValue=\"false\"/></L7p:HttpBasic>\n" +
            "                <L7p:HttpDigest/>\n" +
            "                <L7p:HttpNegotiate/>\n" +
            "                <L7p:TrueAssertion/>\n" +
            "                <L7p:OversizedText/>\n" +
            "                <L7p:RemoveElement/>\n" +
            "            </wsp:OneOrMore>\n" +
            "        </wsp:All>\n" +
            "        <L7p:TrueAssertion/>\n" +
            "        <L7p:FalseAssertion/>\n" +
            "    </wsp:ExactlyOne>\n" +
            "</wsp:Policy>";

    public static final String ONE_DISABLED_ONE_ENABLED =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:ExactlyOne wsp:Usage=\"Required\">\n" +
            "        <wsp:All wsp:Usage=\"Required\">\n" +
            "            <L7p:TrueAssertion/>\n" +
            "            <wsp:OneOrMore wsp:Usage=\"Required\" L7p:Enabled=\"false\">\n" +
            "                <L7p:HttpBasic/>\n" +
            "                <L7p:HttpDigest/>\n" +
            "                <L7p:HttpNegotiate/>\n" +
            "                <L7p:TrueAssertion/>\n" +
            "                <L7p:OversizedText/>\n" +
            "                <L7p:RemoveElement/>\n" +
            "            </wsp:OneOrMore>\n" +
            "        </wsp:All>\n" +
            "    </wsp:ExactlyOne>\n" +
            "</wsp:Policy>";

    public static final String ALL_DISABLED =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:ExactlyOne wsp:Usage=\"Required\">\n" +
            "        <wsp:All wsp:Usage=\"Required\">\n" +
            "            <L7p:TrueAssertion><L7p:Enabled booleanValue=\"false\"/></L7p:TrueAssertion>\n" +
            "            <wsp:OneOrMore wsp:Usage=\"Required\" L7p:Enabled=\"false\">\n" +
            "                <L7p:HttpBasic/>\n" +
            "                <L7p:HttpDigest/>\n" +                    
            "                <L7p:HttpNegotiate/>\n" +
            "                <L7p:TrueAssertion/>\n" +
            "                <L7p:OversizedText/>\n" +
            "                <L7p:RemoveElement/>\n" +
            "            </wsp:OneOrMore>\n" +
            "        </wsp:All>\n" +
            "    </wsp:ExactlyOne>\n" +
            "</wsp:Policy>";

    public static final String INVALID_XML_ESCAPED =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:AssertionComment assertionComment=\"included\">\n" +
            "                <L7p:Properties mapValue=\"included\">\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
            "                        <L7p:value stringValueBase64=\"VGhpcyBpcyBub3QgdmFsaWQgZm9yIFhNTCAtPiAG\"/>\n" +
            "                    </L7p:entry>\n" +
            "                </L7p:Properties>\n" +
            "            </L7p:AssertionComment>\n" +
            "            <L7p:Comment stringValue=\"Blah\"/>\n" +
            "        </L7p:CommentAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";
}
