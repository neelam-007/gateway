package com.l7tech.external.assertions.wsaddressing.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Functions;
import com.l7tech.util.MockConfig;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.logging.Logger;

/**
 * Test the WsAddressingAssertion.
 */
public class ServerWsAddressingAssertionTest {

    private static final Logger logger = Logger.getLogger(ServerWsAddressingAssertionTest.class.getName());
    private static final String ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing";
    private static final String ADDRESSING_NAMESPACE_200408 = "http://schemas.xmlsoap.org/ws/2004/08/addressing";

    /**
     * Test find from SignedElements
     */
    @Test
    public void testWsAddressingSigned() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger), new MockConfig(new Properties()));

        // build test signed elements
        Document testDoc = XmlUtil.stringToDocument(MESSAGE);
        Element header = SoapUtil.getOrMakeHeader(testDoc);
        List<SignedElement> signed = new ArrayList<SignedElement>();
        Node node = header.getFirstChild();
        while ( node != null ) {
            if (node.getNodeType() == Node.ELEMENT_NODE)  {
                final Element element = (Element) node;
                signed.add(new SignedElement(){
                    @Override
                    public SigningSecurityToken getSigningSecurityToken() {
                        return null;
                    }

                    @Override
                    public Element asElement() {
                        return element;
                    }

                    @Override
                    public Element getSignatureElement() {
                        return null;
                    }

                    @Override
                    public String getSignatureAlgorithmId() {
                        return null;
                    }

                    @Override
                    public String[] getDigestAlgorithmIds() {
                        return null;
                    }
                });
            }
            node = node.getNextSibling();
        }

        // eval
        Map<QName,String> properties = new HashMap<QName,String>();
        List<Element> elements = new ArrayList<Element>();
        SignedElement[] signedElements = signed.toArray(new SignedElement[signed.size()]);
        swsaa.populateAddressingFromSignedElements(signedElements, properties, elements);

        // validate
        Assert.assertEquals("Addressing:FaultTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous1", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"FaultTo")));
        Assert.assertEquals("Addressing:Action", "http://warehouse.acme.com/ws/listProducts", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"Action")));
        Assert.assertEquals("Addressing:MessageID", "uuid:6B29FC40-CA47-1067-B31D-00DD010662DA", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"MessageID")));
        Assert.assertEquals("Addressing:To", "http://fish.l7tech.com:8080/warehouse", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"To")));
        Assert.assertEquals("Addressing:ReplyTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous2", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"ReplyTo")));
        Assert.assertEquals("Addressing:From", "http://fish.l7tech.com:8080/warehousesender", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"From")));
    }

    /**
     * Test find from ElementCursor
     */
    @Test
    public void testWsAddressing() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger), new MockConfig(new Properties()));

        // build test signed elements
        Document testDoc = XmlUtil.stringToDocument(MESSAGE);

        // eval
        Map<QName,String> properties = new HashMap<QName,String>();
        List<Element> elements = new ArrayList<Element>();
        swsaa.populateAddressingFromMessage(new DomElementCursor(testDoc), properties, elements);

        // validate
        Assert.assertEquals("Addressing:FaultTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous1", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"FaultTo")));
        Assert.assertEquals("Addressing:Action", "http://warehouse.acme.com/ws/listProducts", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"Action")));
        Assert.assertEquals("Addressing:MessageID", "uuid:6B29FC40-CA47-1067-B31D-00DD010662DA", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"MessageID")));
        Assert.assertEquals("Addressing:To", "http://fish.l7tech.com:8080/warehouse", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"To")));
        Assert.assertEquals("Addressing:ReplyTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous2", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"ReplyTo")));
        Assert.assertEquals("Addressing:From", "http://fish.l7tech.com:8080/warehousesender", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"From")));
        Assert.assertEquals("Addressing elements", 6, elements.size());
    }

    /**
     * Test variables set correctly
     */
    @Test
    public void testSetVariables() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        wsaa.setVariablePrefix("PreFix");
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger), new MockConfig(new Properties()));

        // build test data
        Map<QName,String> properties = new HashMap<QName,String>();
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"Action"), "a");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"To"), "b");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"From"), "c");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"MessageID"), "d");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"FaultTo"), "e");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"ReplyTo"), "f");

        // eval
        final Map<String,Object> varMap = new HashMap<String,Object>();
        final List<Element> elements = new ArrayList<Element>();
        swsaa.setVariables(properties, elements, ADDRESSING_NAMESPACE_200408, new Functions.BinaryVoid<String,Object>(){
            @Override
            public void call(String name, Object value) {
                varMap.put(name,value);
            }
        });

        // validate
        Assert.assertEquals("FaultTo", "e", varMap.get("PreFix.faultto"));
        Assert.assertEquals("Action", "a", varMap.get("PreFix.action"));
        Assert.assertEquals("MessageID", "d", varMap.get("PreFix.messageid"));
        Assert.assertEquals("To", "b", varMap.get("PreFix.to"));
        Assert.assertEquals("ReplyTo", "f", varMap.get("PreFix.replyto"));
        Assert.assertEquals("From", "c", varMap.get("PreFix.from"));
        Assert.assertEquals("Namespace", ADDRESSING_NAMESPACE_200408, varMap.get("PreFix.namespace"));
        Assert.assertTrue("Elements", Arrays.equals(new Element[0], (Element[])varMap.get("PreFix.elements")));
    }

    /**
     * Test setting of vars only in selected namespace
     */
    @Test
    public void testSetVariablesForNamespace() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        wsaa.setVariablePrefix("PreFix");
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger), new MockConfig(new Properties()));

        // build test data
        Map<QName,String> properties = new HashMap<QName,String>();
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"Action"), "a");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"To"), "b");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"From"), "c");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"MessageID"), "d");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"FaultTo"), "e");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"ReplyTo"), "f");
        properties.put(new QName(ADDRESSING_NAMESPACE,"Action"), "g");
        properties.put(new QName(ADDRESSING_NAMESPACE,"To"), "h");
        properties.put(new QName(ADDRESSING_NAMESPACE,"From"), "i");
        properties.put(new QName(ADDRESSING_NAMESPACE,"MessageID"), "j");
        properties.put(new QName(ADDRESSING_NAMESPACE,"FaultTo"), "k");
        properties.put(new QName(ADDRESSING_NAMESPACE,"ReplyTo"), "l");

        // eval
        final Map<String,Object> varMap = new HashMap<String,Object>();
        final List<Element> elements = new ArrayList<Element>();
        swsaa.setVariables(properties, elements, ADDRESSING_NAMESPACE, new Functions.BinaryVoid<String,Object>(){
            @Override
            public void call(String name, Object value) {
                varMap.put(name,value);
            }
        });

        // validate
        Assert.assertEquals("FaultTo", "k", varMap.get("PreFix.faultto"));
        Assert.assertEquals("Action", "g", varMap.get("PreFix.action"));
        Assert.assertEquals("MessageID", "j", varMap.get("PreFix.messageid"));
        Assert.assertEquals("To", "h", varMap.get("PreFix.to"));
        Assert.assertEquals("ReplyTo", "l", varMap.get("PreFix.replyto"));
        Assert.assertEquals("From", "i", varMap.get("PreFix.from"));
        Assert.assertEquals("Namespace", ADDRESSING_NAMESPACE, varMap.get("PreFix.namespace"));
    }

    @Test
    @BugNumber(8318)
    public void testWsaFrom() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger), new MockConfig(new Properties()));

        // build test signed elements
        Document testDoc = XmlUtil.stringToDocument(BUG8318_MESSAGE);

        // eval
        Map<QName,String> properties = new HashMap<QName,String>();
        List<Element> elements = new ArrayList<Element>();
        swsaa.populateAddressingFromMessage(new DomElementCursor(testDoc), properties, elements);

        // validate
        Assert.assertEquals("Addressing:From", "http://JBoss-Client-From", properties.get(new QName(ADDRESSING_NAMESPACE,"From")));
        Assert.assertEquals("Addressing:MessageID", "34197a35-581c-480e-8c64-beba09d5e493", properties.get(new QName(ADDRESSING_NAMESPACE,"MessageID")));
        Assert.assertEquals("Addressing elements", 2, elements.size());
    }

    private static final String MESSAGE =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <soapenv:Header xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">\n" +
            "        <wsa:To>http://fish.l7tech.com:8080/warehouse</wsa:To>\n" +
            "        <wsa:From>http://fish.l7tech.com:8080/warehousesender</wsa:From>\n" +
            "        <wsa:MessageID>uuid:6B29FC40-CA47-1067-B31D-00DD010662DA</wsa:MessageID>\n" +
            "        <wsa:MessageID>uuid:6B29FC40-CA47-1067-B31D-00DD010662DB</wsa:MessageID>\n" +  // test of repeated property
            "        <wsa:Action>http://warehouse.acme.com/ws/listProducts</wsa:Action>\n" +
            "        <wsa:ReplyTo>\n" +
            "            <wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous2</wsa:Address>\n" +
            "        </wsa:ReplyTo>\n" +
            "        <wsa:FaultTo>\n" +
            "            <wsa:Address>\n" +
            "                        http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous1\n" +
            "            </wsa:Address>\n" +
            "        </wsa:FaultTo>\n" +
            "    </soapenv:Header>\n" +
            "    <soapenv:Body>\n" +
            "        <tns:listProducts xmlns:tns=\"http://warehouse.acme.com/ws\">\n" +
            "            <tns:delay>0</tns:delay>\n" +
            "        </tns:listProducts>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String BUG8318_MESSAGE =
            "<soapenv:Envelope xmlns:arc=\"http://soa.wgrintra.net/ch/architecture\" xmlns:pin=\"http://soa.wgrintra.net/ch/architecture/PingWsTest_2_0\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "<soapenv:Header>\n" +
                    "<wsa:MessageID xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">34197a35-581c-480e-8c64-beba09d5e493</wsa:MessageID>\n" +
                    "<wsa:From xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">\n" +
                    "<wsa:Address>http://JBoss-Client-From</wsa:Address>\n" +
                    "</wsa:From>\n" +
                    "<wsse:Security soapenv:mustUnderstand=\"1\" xmlns:soapenv=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:soapenv1=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
                    "<wsu:Timestamp wsu:Id=\"Timestamp-7720611\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                    "<wsu:Created>2010-01-05T15:11:14.948Z</wsu:Created>\n" +
                    "<wsu:Expires>2010-01-05T15:12:14.948Z</wsu:Expires>\n" +
                    "</wsu:Timestamp>\n" +
                    "<wsse:UsernameToken>\n" +
                    "<wsse:Username>2467550</wsse:Username>\n" +
                    "\n" +
                    "</wsse:UsernameToken>\n" +
                    "</wsse:Security>\n" +
                    "</soapenv:Header>\n" +
                    "<soapenv:Body>\n" +
                    "<pin:test_ping>\n" +
                    "<pin:uecpirp1>\n" +
                    "<pin:gen_dt_char>11</pin:gen_dt_char>\n" +
                    "<pin:gen_dt_decimal_11_2>12</pin:gen_dt_decimal_11_2>\n" +
                    "</pin:uecpirp1>\n" +
                    "</pin:test_ping>\n" +
                    "</soapenv:Body>\n" +
                    "</soapenv:Envelope>";
}
