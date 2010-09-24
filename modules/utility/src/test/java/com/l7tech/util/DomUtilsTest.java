package com.l7tech.util;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DomUtils}.
 */
public class DomUtilsTest {
    private static final String[] VALID_NAMES = new String[]{
            "a",
            "test",
            "foo::sdl4-asdf-",
            "hi",
            "xmlns",
            "xmlns:foo",
            "xmlns:foo:bar:baz:blatch",
            "a_b",
            "a-b",
            "_foo",
            "::",
            ":",
            ":foo",
    };

    private static final String[] INVALID_NAMES = new String[]{
            "3232",
            "3",
            "-foo",
            "ab&out",
            "ab#out",
            "4asdf",
            "test%test",
            "---",
            "JDI#",
            "",
            " test",
            "foo::sdl4-asdf- ",
            "h\ti",
            "xm lns",
            "\na-b",
            "_fo\ro",
            "::\r",
            " : ",
            "\t:foo ",
    };

    private static final String[] VALID_NCNAMES = new String[]{
            "asdf",
            "test",
            "asd3",
            "a-b",
    };

    private static final String[] INVALID_NCNAMES = new String[]{
            ":",
            "foo:bar",
            "asdflkj__%$@",
    };

    private static final String[] VALID_QNAMES = new String[]{
            "asdf",
            "a:foo",
            "foo",
            "xmlns:asdfasdf-asdf",
            "a:b",
    };

    private static final String[] INVALID_QNAMES = new String[]{
            "foo:bar:baz",
            ":",
            "::",
            "",
    };

    private static final String[] VALID_CONTENT = new String[]{
            "asdf",
            "\u0009\u0044\uD7FF",
            new String(new char[]{0x0D}),
            "\uE000\uE932\uFFFD",
    };

    private static final String[] INVALID_CONTENT = new String[]{
            "\uD800",
            "\u0008",
            new String(new char[]{0x00}),
            "\uFFFE",
    };

    @Test(expected = NullPointerException.class)
    public void testNullName() {
        DomUtils.isValidXmlName(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNCName() {
        DomUtils.isValidXmlNcName(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullQName() {
        DomUtils.isValidXmlQname(null);
    }


    @Test
    public void testValidNames() throws Exception {
        for (String s : VALID_NAMES) {
            assertTrue("Should have passed as Name: " + s, DomUtils.isValidXmlName(s));
        }
    }

    @Test
    public void testInvalidNames() throws Exception {
        for (String s : INVALID_NAMES) {
            assertFalse("Should have failed as Name: " + s, DomUtils.isValidXmlName(s));
        }
    }

    @Test
    public void testValidNCNames() throws Exception {
        for (String s : VALID_NCNAMES) {
            assertTrue("Should have passed as NCName: " + s, DomUtils.isValidXmlNcName(s));
        }
    }

    @Test
    public void testInvalidNCNames() throws Exception {
        for (String s : INVALID_NCNAMES) {
            assertFalse("Should have failed as NCName: " + s, DomUtils.isValidXmlNcName(s));
        }

        // All invalid Names are automatically invalid as NCNames as well
        for (String s : INVALID_NAMES) {
            assertFalse("Should have failed as NCName: " + s, DomUtils.isValidXmlNcName(s));
        }
    }

    @Test
    public void testValidQNames() throws Exception {
        for (String s : VALID_QNAMES) {
            assertTrue("Should have passed as QName: " + s, DomUtils.isValidXmlQname(s));
        }

        // All valid NCNames are automatically valid as QNames as well
        for (String s : VALID_NCNAMES) {
            assertTrue("Should have passed as QName: " + s, DomUtils.isValidXmlQname(s));
        }
    }

    @Test
    public void testInvalidQNames() throws Exception {
        for (String s : INVALID_QNAMES) {
            assertFalse("Should have failed as QName: " + s, DomUtils.isValidXmlQname(s));
        }

        // All invalid Names are automatically invalid as QNames as well
        for (String s : INVALID_NAMES) {
            assertFalse("Should have failed as QName: " + s, DomUtils.isValidXmlQname(s));
        }
    }

    @Test
    public void testValidXmlContent() {
        for (String s : VALID_CONTENT) {
            assertTrue("Should have passed as content: " + s, DomUtils.isValidXmlContent(s));
        }
    }

    @Test
    public void testInvalidXmlContent() {
        for (String s : INVALID_CONTENT) {
            assertFalse("Should have failed as content: " + s, DomUtils.isValidXmlContent(s));
        }
    }

    @Test
    public void testGetElementByIdMap_AssertionID() throws Exception {
        Document d = parseXml("<saml:Assertion\n" +
                "    AssertionID=\"SamlAssertion-19c05c396e63b3cc87f49d9cded6e7f8\"\n" +
                "    IssueInstant=\"2009-09-24T16:40:02.713Z\" Issuer=\"Bob\"\n" +
                "    MajorVersion=\"1\" MinorVersion=\"1\" xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\"/>");

        Map<String, Element> byid = DomUtils.getElementByIdMap(d, SoapConstants.DEFAULT_ID_ATTRIBUTE_CONFIG);

        assertEquals(d.getDocumentElement(), byid.get("SamlAssertion-19c05c396e63b3cc87f49d9cded6e7f8"));
    }
    
    @Test
    public void testGetElemenetByIdMap_AssertionIDandWsuId() throws Exception {
        Document d = parseXml("<saml:Assertion AssertionID=\"id-G72c0f008-1D\"\n" +
                " IssueInstant=\"2008-11-11T12:42:36Z\"\n" +
                " Issuer=\"CN=DOD CA-13, OU=PKI, OU=DoD, O=U.S. Government, C=US\"\n" +
                " MajorVersion=\"1\" MinorVersion=\"1\" wsu:Id=\"id-ba5f0078-6b7a-4ece-86bb-60f799532992\" xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\"  xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"/>"
        );

        Map<String, Element> byid = DomUtils.getElementByIdMap(d, SoapConstants.DEFAULT_ID_ATTRIBUTE_CONFIG);

        assertEquals("With the default ID config, for a SAML assertion with both an AssertionID and a wsu:Id (as with NCES), the wsu:Id shall take precedence",
                d.getDocumentElement(), byid.get("id-ba5f0078-6b7a-4ece-86bb-60f799532992"));
        assertNull("With the default ID config, for a SAML assertion with both an AssertionID and a wsu:Id (as with NCES), the wsu:Id shall take precedence",
                byid.get("id-G72c0f008-1D"));

    }

    private static Document parseXml(String elementXml) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(elementXml.getBytes(Charsets.UTF8)));
    }
}
