package com.l7tech.proxy;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.DomElementCursor;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.xpath.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import javax.xml.xpath.XPathExpressionException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Test the WSDL proxying feature built into the XML VPN Client (aka SecureSpan Bridge aka SecureSpan Agent aka Client Proxy).
 * @noinspection SingleCharacterStringConcatenation
 */
public class BridgeWsdlProxyTest {
    private static final Logger log = Logger.getLogger(BridgeWsdlProxyTest.class.getName());

    private BridgeTestHarness bt;

    @Before
    public void setUp() throws Exception {
        ResourceUtils.closeQuietly(bt);
        bt = new BridgeTestHarness();
        bt.reset();
    }

    @After
    public void tearDown() throws Exception {
        ResourceUtils.closeQuietly(bt);
        bt = null;
    }

    @Test
    public void testWsilRewriting() throws Exception {
        bt.ssgFaker.setWsdlProxyResponseBody(SIMPLE_WSIL);
        bt.ssgFake.setUseSslByDefault(false);

        String bridgeUrl = bt.proxyUrl + bt.ssg0ProxyEndpoint + "/wsil";
        SimpleHttpClient httpClient = new SimpleHttpClient(new UrlConnectionHttpClient());

        SimpleHttpClient.SimpleHttpResponse response = httpClient.get(new GenericHttpRequestParams(new URL(bridgeUrl)));
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().isXml());

        log.info("Received result: " + new String(response.getBytes()));

        // Make sure all the URLs got rewritten to point at the VPN client
        ElementCursor wsil = new DomElementCursor(XmlUtil.stringToDocument(new String(response.getBytes())));
        CompiledXpath matchDescriptions = new XpathExpression("//*[local-name()='description']").compile();
        String expectedUrlPrefix = bt.proxyUrl + bt.ssg0ProxyEndpoint + "/wsdl?";

        List<ElementCursor> descriptions = matchNodes(wsil, matchDescriptions);
        assertTrue(!descriptions.isEmpty());
        for (ElementCursor description : descriptions) {
            String loc = description.getAttributeValue("location");
            assertTrue(loc.startsWith(expectedUrlPrefix));
        }
    }

    @Test
    public void testWsdlRewriting() throws Exception {
        bt.ssgFaker.setWsdlProxyResponseBody(SIMPLE_WSDL);
        bt.ssgFake.setUseSslByDefault(false);

        String bridgeUrl = bt.proxyUrl + bt.ssg0ProxyEndpoint + "/wsdl?serviceoid=22609924";
        SimpleHttpClient httpClient = new SimpleHttpClient(new UrlConnectionHttpClient());

        SimpleHttpClient.SimpleHttpResponse response = httpClient.get(new GenericHttpRequestParams(new URL(bridgeUrl)));
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().isXml());

        log.info("Received result: " + new String(response.getBytes()));

        // Make sure all the URLs got rewritten to point at the VPN client
        ElementCursor wsil = new DomElementCursor(XmlUtil.stringToDocument(new String(response.getBytes())));
        CompiledXpath matchDescriptions = new XpathExpression("//*[local-name()='service']/*[local-name()='port']/*[local-name()='address']").compile();
        String expectedUrlPrefix = bt.proxyUrl + bt.ssg0ProxyEndpoint + "/service/";

        List<ElementCursor> descriptions = matchNodes(wsil, matchDescriptions);
        assertTrue(!descriptions.isEmpty());
        for (ElementCursor description : descriptions) {
            String loc = description.getAttributeValue("location");
            assertTrue(loc.startsWith(expectedUrlPrefix));
        }
    }
    
    public static List<ElementCursor> matchNodes(ElementCursor cursor, CompiledXpath xpath) throws XPathExpressionException {
        cursor.moveToRoot();
        XpathResult result = cursor.getXpathResult(xpath, true);
        if (!result.matches()) return Collections.emptyList();
        XpathResultNodeSet ns = result.getNodeSet();
        if (ns.isEmpty()) return Collections.emptyList();

        List<ElementCursor> ret = new ArrayList<ElementCursor>();
        XpathResultIterator it = ns.getIterator();
        while (it.hasNext()) {
            ElementCursor ec = it.nextElementAsCursor();
            assertNotNull(ec);
            ret.add(ec);
        }
        return ret;
    }

    private static final String SIMPLE_WSIL =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<?xml-stylesheet type=\"text/xsl\" href=\"/ssg/wsil2xhtml.xml\"?>\n" +
            "<inspection xmlns=\"http://schemas.xmlsoap.org/ws/2001/10/inspection/\">\n" +
            "\t<service>\n" +
            "\t\t<abstract>Warehouse</abstract>\n" +
            "\t\t<description referencedNamespace=\"http://schemas.xmlsoap.org/wsdl/\" location=\"http://localhost:8080/ssg/wsdl?serviceoid=21299203\"/>\n" +
            "\t</service>\n" +
            "\t<service>\n" +
            "\t\t<abstract>DeadOrAlive</abstract>\n" +
            "\n" +
            "\t\t<description referencedNamespace=\"http://schemas.xmlsoap.org/wsdl/\" location=\"http://localhost:8080/ssg/wsdl?serviceoid=22609922\"/>\n" +
            "\t</service>\n" +
            "\t<service>\n" +
            "\t\t<abstract>GeoServe</abstract>\n" +
            "\t\t<description referencedNamespace=\"http://schemas.xmlsoap.org/wsdl/\" location=\"http://localhost:8080/ssg/wsdl?serviceoid=22609923\"/>\n" +
            "\t</service>\n" +
            "\t<service>\n" +
            "\t\t<abstract>AuthenticateService</abstract>\n" +
            "\n" +
            "\t\t<description referencedNamespace=\"http://schemas.xmlsoap.org/wsdl/\" location=\"http://localhost:8080/ssg/wsdl?serviceoid=22609924\"/>\n" +
            "\t</service>\n" +
            "</inspection>";

    private static final String SIMPLE_WSDL =
            "<definitions xmlns=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\" xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\" xmlns:s=\"http://www.w3.org/2001/XMLSchema\" xmlns:s0=\"http://webservices.geomonster.com/\" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:sourcens0=\"http://webservices.geomonster.com/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\" xmlns:tns=\"http://tempuri.org/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" name=\"NewService\" targetNamespace=\"http://tempuri.org/\">\n" +
            "  <types>\n" +
            "  </types>\n" +
            "  <portType name=\"NewPortType\">\n" +
            "  </portType>\n" +
            "  <binding name=\"NewPortTypeBinding\" type=\"tns:NewPortType\">\n" +
            "    <soap:binding style=\"rpc\" transport=\"http://schemas.xmlsoap.org/soap/http\"></soap:binding>\n" +
            "  </binding>\n" +
            "  <service name=\"Service\">\n" +
            "\n" +
            "    <port binding=\"tns:NewPortTypeBinding\" name=\"ServicePort\">\n" +
            "      <soap:address location=\"http://localhost:8080/service/22609923\"></soap:address>\n" +
            "    </port>\n" +
            "  </service>\n" +
            "</definitions>";

}
