package com.l7tech.external.assertions.saml2attributequery.server;

import static org.junit.Assert.*;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.external.assertions.saml2attributequery.SamlToLdapMap;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 2-Feb-2009
 * Time: 11:42:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class Saml2AttributeQueryTest {
    private static final String TEST_QUERY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"+
        "  <soapenv:Header/>\n"+
        "  <soapenv:Body>\n"+
        "    <samlp:AttributeQuery xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"aaf23196-1773-2113-474a-fe114412ab72\" IssueInstant=\"2004-12-05T09:22:04Z\" Version=\"2.0\">\n"+
        "      <saml:Subject>\n"+
        "        <saml:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:unspecified\">jabba</saml:NameID>\n"+
        "      </saml:Subject>\n"+
        "      <saml:Attribute FriendlyName=\"givenName\" Name=\"urn:oid:givenName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n"+
        "        <saml:AttributeValue>test</saml:AttributeValue>\n"+
        "        <saml:AttributeValue>Jabba</saml:AttributeValue>\n"+
        "      </saml:Attribute>\n"+
        "      <saml:Attribute FriendlyName=\"cn\" Name=\"urn:oid:cn\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"/>\n"+
        "    </samlp:AttributeQuery>\n"+
        "  </soapenv:Body>\n"+
        "</soapenv:Envelope>";

    private static final String ENCODED_ATTR_MAP = "rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAA" +
            "AADdwQAAAAKc3IARmNvbS5sN3RlY2guZXh0ZXJuYWwuYXNzZXJ0aW9ucy5zYW1sMmF0dHJpYnV0ZXF1ZXJ5LlNhbWxUb0xkYXBNYXAkR" +
            "W50cnlkj4/+K1qF0wIAA0wACGxkYXBOYW1ldAASTGphdmEvbGFuZy9TdHJpbmc7TAAKbmFtZUZvcm1hdHEAfgADTAAIc2FtbE5hbWVxA" +
            "H4AA3hwdAACY250ADF1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybWF0OmJhc2ljdAAKdXJuOm9pZDpjbnNxA" +
            "H4AAnQACWdpdmVuTmFtZXEAfgAGdAARdXJuOm9pZDpnaXZlbk5hbWVzcQB+AAJ0AAJzbnEAfgAGdAAPdXJuOm9pZDpzdXJuYW1leA==";

    @Test
    public void testSaml2AttributeQueryConstructorTest() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();

        Document doc = parser.parse(new ByteArrayInputStream(TEST_QUERY_XML.getBytes("UTF-8")));
        NodeList elements = doc.getElementsByTagNameNS(SamlConstants.NS_SAMLP2, "AttributeQuery");
        Element queryElement = (Element)elements.item(0);

        Saml2AttributeQuery query = new Saml2AttributeQuery(queryElement, new SamlToLdapMap(ENCODED_ATTR_MAP));

        assertEquals("aaf23196-1773-2113-474a-fe114412ab72", query.getID());
        assertEquals("jabba", query.getSubject());
        assertEquals("urn:oasis:names:tc:SAML:2.0:nameid-format:unspecified", query.getSubjectNameFormat());
        assertNull(query.getSubjectNameQualifier());

        assertEquals(2, query.getAttributeFilters().size());

        assertEquals("urn:oid:givenName", query.getAttributeFilters().get(0).getSaml2AttributeName());
        assertEquals("givenName", query.getAttributeFilters().get(0).getLdapAttributeName());
        assertEquals("urn:oasis:names:tc:SAML:2.0:attrname-format:basic", query.getAttributeFilters().get(0).getNameFormat());
        assertEquals(2, query.getAttributeFilters().get(0).getAllowedValues().size());
        assertTrue(query.getAttributeFilters().get(0).getAllowedValues().contains("test"));
        assertTrue(query.getAttributeFilters().get(0).getAllowedValues().contains("Jabba"));

        assertEquals("urn:oid:cn", query.getAttributeFilters().get(1).getSaml2AttributeName());
        assertEquals("cn", query.getAttributeFilters().get(1).getLdapAttributeName());
        assertEquals("urn:oasis:names:tc:SAML:2.0:attrname-format:basic", query.getAttributeFilters().get(1).getNameFormat());
        assertEquals(0, query.getAttributeFilters().get(1).getAllowedValues().size());
    }
}
