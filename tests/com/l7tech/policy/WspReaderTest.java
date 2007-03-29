package com.l7tech.policy;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.wsp.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Test policy deserializer.
 * User: mike
 * Date: Jun 10, 2003
 * Time: 3:33:36 PM
 */
public class WspReaderTest extends TestCase {
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
        System.setProperty("com.l7tech.policy.wsp.checkAccessors", "true");
    }
    
    public WspReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WspReaderTest.class);
    }

    public void testParseWsp() throws Exception {
        InputStream wspStream = cl.getResourceAsStream(SIMPLE_POLICY);
        Assertion policy = wspReader.parsePermissively(XmlUtil.parse(wspStream).getDocumentElement());
        log.info("Got back policy: " + policy);
        assertTrue(policy != null);
        assertTrue(policy instanceof ExactlyOneAssertion);
        ExactlyOneAssertion eoa = (ExactlyOneAssertion)policy;
        assertTrue(eoa.getChildren().size() == 5);
        assertTrue(eoa.getChildren().get(0) instanceof AllAssertion);

        // Do a round trip policyA -> xmlA -> policyB -> xmlB and verify that both XMLs match
        String xmlA = WspWriter.getPolicyXml(policy);
        log.info("Parsing policy: " + xmlA);
        Assertion policyB = wspReader.parseStrictly(xmlA);
        String xmlB = WspWriter.getPolicyXml(policyB);
        assertEquals(xmlA, xmlB);
    }

    public void testParseNonXml() {
        try {
            wspReader.parseStrictly("asdfhaodh/asdfu2h$9ha98h");
            fail("Expected IOException not thrown");
        } catch (IOException e) {
            // Ok
        }
    }

    public void testParseStrangeXml() {
        try {
            wspReader.parseStrictly("<foo><bar blee=\"1\"/></foo>");
            fail("Expected IOException not thrown");
        } catch (IOException e) {
            // Ok
        }
    }

    public void testParseSwAPolicy() throws Exception {
        Assertion policy = WspWriterTest.createSoapWithAttachmentsPolicy();
        String serialized = WspWriter.getPolicyXml(policy);
        Assertion parsedPolicy = wspReader.parseStrictly(serialized);
        assertTrue(parsedPolicy instanceof AllAssertion);
        AllAssertion all = (AllAssertion)parsedPolicy;
        Assertion kid = (Assertion)all.getChildren().get(0);
        assertTrue(kid instanceof RequestSwAAssertion);
        RequestSwAAssertion swa = (RequestSwAAssertion)kid;

        assertTrue(swa.getBindings().size() == 1);
        String bindingInfoName = (String) swa.getBindings().keySet().iterator().next();
        BindingInfo bindingInfo = (BindingInfo) swa.getBindings().get(bindingInfoName);
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

    public void testCollectionMappings() throws Exception {
        // Use SqlAttackAssertion since it uses Set
        SqlAttackAssertion ass = new SqlAttackAssertion();
        ass.setProtection(SqlAttackAssertion.PROT_MSSQL);
        ass.setProtection(SqlAttackAssertion.PROT_ORASQL);
        ass.setProtection(SqlAttackAssertion.PROT_META);

        String xml = WspWriter.getPolicyXml(ass);
        log.info("Serialized SqlProtectionAssertion: \n" + xml);

        SqlAttackAssertion out = (SqlAttackAssertion)wspReader.parseStrictly(xml);
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
            Assertion root = wspReader.parsePermissively(policy.getDocumentElement());
            assertTrue(root != null);
            assertTrue(root instanceof ExactlyOneAssertion);
        } finally {
            if (policyStream != null) policyStream.close();
        }
    }

    public void testSeamlessPolicyUpgrades() throws Exception {
        for (int i = 0; i < VERSIONS.length; i++) {
            Object[] version = VERSIONS[i];
            String policyFile = (String)version[0];
            trySeamlessPolicyUpgrade(policyFile);
        }
    }

    public void testSeamlessUpgradeFrom21() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_21.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = wspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 2.1 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    public void testSeamlessUpgradeFrom30() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_30.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = wspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 3.0 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    public void testSeamlessUpgradeFrom31() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_31.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = wspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 3.1 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    public void testSeamlessUpgradeFrom32() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_32.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = wspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 3.2 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
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

        Assertion p = wspReader.parsePermissively(policyXml);
        String parsed1 = p.toString();
        log.info("Parsed data including unknown element: " + parsed1);

        String out = WspWriter.getPolicyXml(p);
        Assertion p2 = wspReader.parsePermissively(out);
        String parsed2 = p2.toString();
        log.info("After reparsing: " + parsed2);

        assertEquals(parsed1, parsed2);

    }

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
        RequestXpathAssertion got = (RequestXpathAssertion)wspReader.parsePermissively(policyXml);
        final String gotXpath = got.getXpathExpression().getExpression();
        log.info("Parsed xpath: " + gotXpath);
        final Map gotNsmap = got.getXpathExpression().getNamespaces();
        log.info("Parsed nsmap: " + gotNsmap);
        assertEquals(xp, gotXpath);
        assertEquals(ns1, gotNsmap.get("ns1"));
        assertEquals(ns2, gotNsmap.get("ns2"));
    }

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

        Assertion p = wspReader.parsePermissively(policyxml);
        AllAssertion root = (AllAssertion)p;
        RequestWssIntegrity rwi = (RequestWssIntegrity)root.children().next();
        assertTrue(rwi.getRecipientContext().getActor().equals("fdsfd"));
    }

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

        Assertion p = wspReader.parsePermissively(policyxml);
        AllAssertion root = (AllAssertion)p;

        RequestWssSaml rwi = (RequestWssSaml)root.children().next();
        assertTrue(rwi.getRecipientContext().getActor().equals("ppal"));
    }

    // TODO reenable this test as soon as we are ready to fix it
    public void testReproBug2214TabsInEmail() throws Exception {
        final String body = "foo\r\nbar baz blah\tbleet blot";

        EmailAlertAssertion ema = new EmailAlertAssertion();
        ema.setSubject("Hi there");
        ema.setTargetEmailAddress("blah@blah.example.com");
        ema.messageString(body);

        String emXml = WspWriter.getPolicyXml(ema);
        EmailAlertAssertion got = (EmailAlertAssertion)wspReader.parseStrictly(emXml);

        assertEquals(got.messageString(), body);
    }

    public void testSslAssertionOptionChange() throws Exception {
        SslAssertion sa = new SslAssertion(SslAssertion.OPTIONAL);
        String got = WspWriter.getPolicyXml(sa);
        assertNotNull(got);
        assertTrue(got.contains("SslAssertion"));
        assertTrue(got.contains("Optional"));

        SslAssertion sa2 = (SslAssertion)wspReader.parseStrictly(got);
        assertEquals(sa2.getOption(), SslAssertion.OPTIONAL);
    }

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
        XslTransformation xslt = (XslTransformation)wspReader.parseStrictly(xsl35);
        assertNotNull(xslt);
        System.out.println(WspWriter.getPolicyXml(xslt));
    }

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
        XslTransformation xslt = (XslTransformation)wspReader.parseStrictly(xsl35);
        assertNotNull(xslt);
        System.out.println(WspWriter.getPolicyXml(xslt));
    }

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

        Assertion got = wspReader.parseStrictly(policy);
        assertNotNull(got);
        log.info("Parsed policy from Bug #2160: " + WspWriter.getPolicyXml(got));
    }

    public void testResourceInfo() throws Exception {
        tryIt(WspWriterTest.makeStaticInfo());
        tryIt(WspWriterTest.makeMessageInfo());
        tryIt(WspWriterTest.makeSingleInfo());
    }

    private void tryIt(AssertionResourceInfo rinfo) throws IOException {
        XslTransformation xslt = new XslTransformation();
        xslt.setResourceInfo(rinfo);
        String policy = WspWriter.getPolicyXml(xslt);
        XslTransformation newXslt = (XslTransformation) wspReader.parseStrictly(policy);
        assertTrue(newXslt.getResourceInfo().getType().equals(xslt.getResourceInfo().getType()));
    }

    public void testBug3456() throws Exception {
        try {
            WspReader.getDefault().parseStrictly(BUG_3456_POLICY);
            fail("Expected exception not thrown for invalid attribute Unknown HtmlFormDataType name: 'string (any)'");
        } catch (InvalidPolicyStreamException e) {
            log.log(Level.INFO, "Caught expected exception: " + ExceptionUtils.getMessage(e), e);
            // Ok
        }

        Assertion got = WspReader.getDefault().parsePermissively(BUG_3456_POLICY);
        log.info("Got: " + got);
    }
    
    public void testBug3637SchemaParse() throws Exception {
        AllAssertion all = (AllAssertion)WspReader.getDefault().parsePermissively(BUG_3637_SCHEMA_PARSING_PROBLEM);
        SchemaValidation sv = (SchemaValidation)all.getChildren().iterator().next();
        AssertionResourceInfo ri = sv.getResourceInfo();
        assertEquals(AssertionResourceType.STATIC, ri.getType());
        StaticResourceInfo sri = (StaticResourceInfo)ri;
        assertNotNull(sri.getDocument());
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

    public static void main(String[] args) {
        System.out.println("Heya");
        junit.textui.TestRunner.run(suite());
    }

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
}