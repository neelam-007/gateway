package com.l7tech.policy;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ComparisonOperator;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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

    public WspReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WspReaderTest.class);
    }

    public void testParseWsp() throws Exception {
        InputStream wspStream = cl.getResourceAsStream(SIMPLE_POLICY);
        Assertion policy = WspReader.parsePermissively(XmlUtil.parse(wspStream).getDocumentElement());
        log.info("Got back policy: " + policy);
        assertTrue(policy != null);
        assertTrue(policy instanceof ExactlyOneAssertion);
        ExactlyOneAssertion eoa = (ExactlyOneAssertion)policy;
        assertTrue(eoa.getChildren().size() == 5);
        assertTrue(eoa.getChildren().get(0) instanceof AllAssertion);

        // Do a round trip policyA -> xmlA -> policyB -> xmlB and verify that both XMLs match
        String xmlA = WspWriter.getPolicyXml(policy);
        log.info("Parsing policy: " + xmlA);
        Assertion policyB = WspReader.parseStrictly(xmlA);
        String xmlB = WspWriter.getPolicyXml(policyB);
        assertEquals(xmlA, xmlB);
    }

    private interface throwingRunnable {
        void run() throws Throwable;
    }

    private void mustThrow(Class mustThrowThis, throwingRunnable tr) {
        boolean caught = false;
        try {
            System.err.println(">>>>>> The following operation should throw the exception " + mustThrowThis);
            tr.run();
        } catch (Throwable t) {
            caught = true;
            if (!mustThrowThis.isAssignableFrom(t.getClass()))
                t.printStackTrace();
            assertTrue(mustThrowThis.isAssignableFrom(t.getClass()));
        }
        assertTrue(caught);
        System.err.println(">>>>>> The correct exception was thrown.");
    }

    public void testParseNonXml() {
        mustThrow(IOException.class, new throwingRunnable() {
            public void run() throws IOException {
                WspReader.parseStrictly("asdfhaodh/asdfu2h$9ha98h");
            }
        });
    }

    public void testParseStrangeXml() {
        mustThrow(IOException.class,  new throwingRunnable() {
            public void run() throws IOException {
                WspReader.parseStrictly("<foo><bar blee=\"1\"/></foo>");
            }
        });
    }

    public void testParseSwAPolicy() throws Exception {
        Assertion policy = WspWriterTest.createSoapWithAttachmentsPolicy();
        String serialized = WspWriter.getPolicyXml(policy);
        Assertion parsedPolicy = WspReader.parseStrictly(serialized);
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

        SqlAttackAssertion out = (SqlAttackAssertion)WspReader.parseStrictly(xml);
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
            Assertion root = WspReader.parsePermissively(policy.getDocumentElement());
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
        Assertion ass = WspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 2.1 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    public void testSeamlessUpgradeFrom30() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_30.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = WspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 3.0 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    public void testSeamlessUpgradeFrom31() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_31.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = WspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 3.1 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

    public void testSeamlessUpgradeFrom32() throws Exception {
        InputStream is = cl.getResourceAsStream(RESOURCE_PATH + "/" + "simple_policy_32.xml");
        Document doc = XmlUtil.parse(is);
        Assertion ass = WspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 3.2 policy XML:\n" + ass);
        assertTrue(ass != null);
        assertTrue(ass instanceof ExactlyOneAssertion);
    }

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
        AllAssertion ass = (AllAssertion)WspReader.parsePermissively(doc.getDocumentElement());
        log.info("Policy tree constructed after reading 3.4 policy XML:\n" + ass);
        assertTrue(ass != null);
        ComparisonAssertion comp = (ComparisonAssertion)ass.getChildren().get(0);
        assertEquals(comp.getExpression1(), "foo");
        assertEquals(comp.getExpression2(), "bar");
        assertEquals(comp.getOperator(), ComparisonOperator.EQ);
        assertFalse(comp.isNegate());
    }

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

        Assertion p = WspReader.parsePermissively(policyXml);
        String parsed1 = p.toString();
        log.info("Parsed data including unknown element: " + parsed1);

        String out = WspWriter.getPolicyXml(p);
        Assertion p2 = WspReader.parsePermissively(out);
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
        RequestXpathAssertion got = (RequestXpathAssertion)WspReader.parsePermissively(policyXml);
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

        Assertion p = WspReader.parsePermissively(policyxml);
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

        Assertion p = WspReader.parsePermissively(policyxml);
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
        EmailAlertAssertion got = (EmailAlertAssertion)WspReader.parseStrictly(emXml);

        assertEquals(got.messageString(), body);
    }

    public void testSslAssertionOptionChange() throws Exception {
        SslAssertion sa = new SslAssertion(SslAssertion.OPTIONAL);
        String got = WspWriter.getPolicyXml(sa);
        assertNotNull(got);
        assertTrue(got.contains("SslAssertion"));
        assertTrue(got.contains("Optional"));

        SslAssertion sa2 = (SslAssertion)WspReader.parseStrictly(got);
        assertEquals(sa2.getOption(), SslAssertion.OPTIONAL);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}