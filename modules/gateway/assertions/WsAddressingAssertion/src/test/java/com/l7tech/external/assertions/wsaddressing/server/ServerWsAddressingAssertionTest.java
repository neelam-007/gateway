package com.l7tech.external.assertions.wsaddressing.server;

import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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

/**
 * Test the WsAddressingAssertion.
 */
public class ServerWsAddressingAssertionTest extends TestCase {

    private static final Logger logger = Logger.getLogger(ServerWsAddressingAssertionTest.class.getName());
    private static final String ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing";
    private static final String ADDRESSING_NAMESPACE_200408 = "http://schemas.xmlsoap.org/ws/2004/08/addressing";

    public ServerWsAddressingAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerWsAddressingAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Test find from SignedElements
     */
    public void testWsAddressingSigned() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger));

        // build test signed elements
        Document testDoc = XmlUtil.stringToDocument(MESSAGE);
        Element header = SoapUtil.getOrMakeHeader(testDoc);
        List<SignedElement> signed = new ArrayList();
        Node node = header.getFirstChild();
        while ( node != null ) {
            if (node.getNodeType() == Node.ELEMENT_NODE)  {
                final Element element = (Element) node;
                signed.add(new SignedElement(){
                    public SigningSecurityToken getSigningSecurityToken() {
                        return null;
                    }

                    public Element asElement() {
                        return element;
                    }

                    public Element getSignatureElement() {
                        return null;
                    }
                });
            }
            node = node.getNextSibling();
        }

        // eval
        Map<QName,String> properties = new HashMap();
        SignedElement[] signedElements = signed.toArray(new SignedElement[0]);
        swsaa.populateAddressingFromSignedElements(signedElements, properties);

        // validate
        assertEquals("Addressing:FaultTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous1", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"FaultTo")));
        assertEquals("Addressing:Action", "http://warehouse.acme.com/ws/listProducts", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"Action")));
        assertEquals("Addressing:MessageID", "uuid:6B29FC40-CA47-1067-B31D-00DD010662DA", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"MessageID")));
        assertEquals("Addressing:To", "http://fish.l7tech.com:8080/warehouse", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"To")));
        assertEquals("Addressing:ReplyTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous2", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"ReplyTo")));
        assertEquals("Addressing:From", "http://fish.l7tech.com:8080/warehousesender", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"From")));
    }

    /**
     * Test find from ElementCursor
     */
    public void testWsAddressing() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger));

        // build test signed elements
        Document testDoc = XmlUtil.stringToDocument(MESSAGE);

        // eval
        Map<QName,String> properties = new HashMap();
        swsaa.populateAddressingFromMessage(new DomElementCursor(testDoc), properties);

        // validate
        assertEquals("Addressing:FaultTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous1", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"FaultTo")));
        assertEquals("Addressing:Action", "http://warehouse.acme.com/ws/listProducts", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"Action")));
        assertEquals("Addressing:MessageID", "uuid:6B29FC40-CA47-1067-B31D-00DD010662DA", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"MessageID")));
        assertEquals("Addressing:To", "http://fish.l7tech.com:8080/warehouse", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"To")));
        assertEquals("Addressing:ReplyTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous2", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"ReplyTo")));
        assertEquals("Addressing:From", "http://fish.l7tech.com:8080/warehousesender", properties.get(new QName(ADDRESSING_NAMESPACE_200408,"From")));
    }

    /**
     * Test variables set correctly
     */
    public void testSetVariables() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        wsaa.setVariablePrefix("PreFix");
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger));

        // build test data
        Map<QName,String> properties = new HashMap();
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"Action"), "a");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"To"), "b");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"From"), "c");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"MessageID"), "d");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"FaultTo"), "e");
        properties.put(new QName(ADDRESSING_NAMESPACE_200408,"ReplyTo"), "f");

        // eval
        final Map<String,String> varMap = new HashMap();
        swsaa.setVariables(properties, ADDRESSING_NAMESPACE_200408, new Functions.BinaryVoid<String,String>(){
            public void call(String name, String value) {
                varMap.put(name,value);   
            }
        });

        // validate
        assertEquals("FaultTo", "e", varMap.get("PreFix.faultto"));
        assertEquals("Action", "a", varMap.get("PreFix.action"));
        assertEquals("MessageID", "d", varMap.get("PreFix.messageid"));
        assertEquals("To", "b", varMap.get("PreFix.to"));
        assertEquals("ReplyTo", "f", varMap.get("PreFix.replyto"));
        assertEquals("From", "c", varMap.get("PreFix.from"));
        assertEquals("Namespace", ADDRESSING_NAMESPACE_200408, varMap.get("PreFix.namespace"));
    }

    /**
     * Test setting of vars only in selected namespace
     */
    public void testSetVariablesForNamespace() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        wsaa.setVariablePrefix("PreFix");
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new LogOnlyAuditor(logger));

        // build test data
        Map<QName,String> properties = new HashMap();
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
        final Map<String,String> varMap = new HashMap();
        swsaa.setVariables(properties, ADDRESSING_NAMESPACE, new Functions.BinaryVoid<String,String>(){
            public void call(String name, String value) {
                varMap.put(name,value);
            }
        });

        // validate
        assertEquals("FaultTo", "k", varMap.get("PreFix.faultto"));
        assertEquals("Action", "g", varMap.get("PreFix.action"));
        assertEquals("MessageID", "j", varMap.get("PreFix.messageid"));
        assertEquals("To", "h", varMap.get("PreFix.to"));
        assertEquals("ReplyTo", "l", varMap.get("PreFix.replyto"));
        assertEquals("From", "i", varMap.get("PreFix.from"));
        assertEquals("Namespace", ADDRESSING_NAMESPACE, varMap.get("PreFix.namespace"));
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
