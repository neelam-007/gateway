package com.l7tech.external.assertions.wsaddressing.server;

import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import javax.xml.namespace.QName;

import com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.Functions;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.common.io.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.junit.Test;
import org.junit.Assert;

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
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger));

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
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger));

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
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger));

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
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger));

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
}
