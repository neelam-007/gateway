package com.l7tech.external.assertions.wsaddressing.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Functions;
import com.l7tech.util.MockConfig;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the WsAddressingAssertion.
 */
public class ServerWsAddressingAssertionTest {

    private static final String ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing";
    private static final String ADDRESSING_NAMESPACE_200408 = "http://schemas.xmlsoap.org/ws/2004/08/addressing";

    static {
        // init JCE early to use crypto-j in classpath
        JceProvider.init();
    }

    /**
     * Test find from SignedElements
     */
    @Test
    public void testWsAddressingSigned() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new MockConfig(new Properties()));

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
        assertEquals( "Addressing:FaultTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous1", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "FaultTo" ) ) );
        assertEquals( "Addressing:Action", "http://warehouse.acme.com/ws/listProducts", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "Action" ) ) );
        assertEquals( "Addressing:MessageID", "uuid:6B29FC40-CA47-1067-B31D-00DD010662DA", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "MessageID" ) ) );
        assertEquals( "Addressing:To", "http://fish.l7tech.com:8080/warehouse", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "To" ) ) );
        assertEquals( "Addressing:ReplyTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous2", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "ReplyTo" ) ) );
        assertEquals( "Addressing:From", "http://fish.l7tech.com:8080/warehousesender", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "From" ) ) );
        assertEquals( "Addressing elements", 8, elements.size() );
    }

    /**
     * Test find from ElementCursor
     */
    @Test
    public void testWsAddressing() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new MockConfig(new Properties()));

        // build test signed elements
        Document testDoc = XmlUtil.stringToDocument(MESSAGE);

        // eval
        Map<QName,String> properties = new HashMap<QName,String>();
        List<Element> elements = new ArrayList<Element>();
        swsaa.populateAddressingFromMessage(new DomElementCursor(testDoc), properties, elements);

        // validate
        assertEquals( "Addressing:FaultTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous1", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "FaultTo" ) ) );
        assertEquals( "Addressing:Action", "http://warehouse.acme.com/ws/listProducts", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "Action" ) ) );
        assertEquals( "Addressing:MessageID", "uuid:6B29FC40-CA47-1067-B31D-00DD010662DA", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "MessageID" ) ) );
        assertEquals( "Addressing:To", "http://fish.l7tech.com:8080/warehouse", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "To" ) ) );
        assertEquals( "Addressing:ReplyTo", "http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous2", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "ReplyTo" ) ) );
        assertEquals( "Addressing:From", "http://fish.l7tech.com:8080/warehousesender", properties.get( new QName( ADDRESSING_NAMESPACE_200408, "From" ) ) );
        assertEquals( "Addressing elements", 8, elements.size() );
    }

    /**
     * Test variables set correctly
     */
    @Test
    public void testSetVariables() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        wsaa.setVariablePrefix("PreFix");
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new MockConfig(new Properties()));

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
        assertEquals( "FaultTo", "e", varMap.get( "PreFix.faultto" ) );
        assertEquals( "Action", "a", varMap.get( "PreFix.action" ) );
        assertEquals( "MessageID", "d", varMap.get( "PreFix.messageid" ) );
        assertEquals( "To", "b", varMap.get( "PreFix.to" ) );
        assertEquals( "ReplyTo", "f", varMap.get( "PreFix.replyto" ) );
        assertEquals( "From", "c", varMap.get( "PreFix.from" ) );
        assertEquals( "Namespace", ADDRESSING_NAMESPACE_200408, varMap.get( "PreFix.namespace" ) );
        assertTrue( "Elements", Arrays.equals( new Element[0], (Element[]) varMap.get( "PreFix.elements" ) ) );
    }

    /**
     * Test setting of vars only in selected namespace
     */
    @Test
    public void testSetVariablesForNamespace() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        wsaa.setVariablePrefix("PreFix");
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new MockConfig(new Properties()));

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
        assertEquals( "FaultTo", "k", varMap.get( "PreFix.faultto" ) );
        assertEquals( "Action", "g", varMap.get( "PreFix.action" ) );
        assertEquals( "MessageID", "j", varMap.get( "PreFix.messageid" ) );
        assertEquals( "To", "h", varMap.get( "PreFix.to" ) );
        assertEquals( "ReplyTo", "l", varMap.get( "PreFix.replyto" ) );
        assertEquals( "From", "i", varMap.get( "PreFix.from" ) );
        assertEquals( "Namespace", ADDRESSING_NAMESPACE, varMap.get( "PreFix.namespace" ) );
    }

    /**
     * Should not fail with an IOException for non-soap, status should be NOT_APPLICABLE
     */
    @Test
    @BugNumber(10246)
    public void testNonSoap() throws Exception {
        doTestNonSoap( new WsAddressingAssertion() );
        final WsAddressingAssertion wsaa = new WsAddressingAssertion();
        wsaa.setRequireSignature( true );
        doTestNonSoap( wsaa );
    }

    private void doTestNonSoap( final WsAddressingAssertion wsaa ) throws Exception {
        final ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new MockConfig(new Properties()));
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
        final AssertionStatus status = swsaa.checkRequest( context );
        assertEquals( "Status", AssertionStatus.NOT_APPLICABLE, status );
    }

    @Test
    @BugNumber(8318)
    public void testWsaFrom() throws Exception {
        // init
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new MockConfig(new Properties()));

        // build test signed elements
        Document testDoc = XmlUtil.stringToDocument(BUG8318_MESSAGE);

        // eval
        Map<QName,String> properties = new HashMap<QName,String>();
        List<Element> elements = new ArrayList<Element>();
        swsaa.populateAddressingFromMessage(new DomElementCursor(testDoc), properties, elements);

        // validate
        assertEquals( "Addressing:From", "http://JBoss-Client-From", properties.get( new QName( ADDRESSING_NAMESPACE, "From" ) ) );
        assertEquals( "Addressing:MessageID", "34197a35-581c-480e-8c64-beba09d5e493", properties.get( new QName( ADDRESSING_NAMESPACE, "MessageID" ) ) );
        assertEquals( "Addressing elements", 2, elements.size() );
    }

    @Test
    @BugNumber(10301)
    public void testWsaOther() throws Exception {
        // init
        final String OTHER_NAMESPACE = "http://tempuri.org/my/addressing/namespace";
        WsAddressingAssertion wsaa = new WsAddressingAssertion();
        wsaa.setEnableWsAddressing10( false );
        wsaa.setEnableWsAddressing200408( false );
        wsaa.setEnableOtherNamespace( OTHER_NAMESPACE );
        ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new MockConfig(new Properties()));

        // build test signed elements
        Document testDoc = XmlUtil.stringToDocument(MESSAGE.replace( "http://schemas.xmlsoap.org/ws/2004/08/addressing", OTHER_NAMESPACE ));

        // eval
        Map<QName,String> properties = new HashMap<QName,String>();
        List<Element> elements = new ArrayList<Element>();
        swsaa.populateAddressingFromMessage(new DomElementCursor(testDoc), properties, elements);

        // validate
        assertEquals( "Addressing:FaultTo", "http://tempuri.org/my/addressing/namespace/role/anonymous1", properties.get( new QName( OTHER_NAMESPACE, "FaultTo" ) ) );
        assertEquals( "Addressing:Action", "http://warehouse.acme.com/ws/listProducts", properties.get( new QName( OTHER_NAMESPACE, "Action" ) ) );
        assertEquals( "Addressing:MessageID", "uuid:6B29FC40-CA47-1067-B31D-00DD010662DA", properties.get( new QName( OTHER_NAMESPACE, "MessageID" ) ) );
        assertEquals( "Addressing:To", "http://fish.l7tech.com:8080/warehouse", properties.get( new QName( OTHER_NAMESPACE, "To" ) ) );
        assertEquals( "Addressing:ReplyTo", "http://tempuri.org/my/addressing/namespace/role/anonymous2", properties.get( new QName( OTHER_NAMESPACE, "ReplyTo" ) ) );
        assertEquals( "Addressing:From", "http://fish.l7tech.com:8080/warehousesender", properties.get( new QName( OTHER_NAMESPACE, "From" ) ) );
        assertEquals( "Addressing elements", 8, elements.size() );
    }

    @Test
    @BugNumber(9970)
    public void testDetailAuditing() throws Exception {
        final Document requestNoAddressing = TestDocuments.getTestDocument( TestDocuments.PLACEORDER_CLEARTEXT );
        final Document requestWrongAddressing = XmlUtil.stringToDocument( MESSAGE );
        final Document requestNoAddressingSigned = TestDocuments.getTestDocument( TestDocuments.ETTK_SIGNED_REQUEST );
        final Document requestWrongAddressingSigned = TestDocuments.getTestDocument( TestDocuments.DOTNET_SIGNED_REQUEST2 );
        doTestDetailAuditing( requestNoAddressing, false, AssertionStatus.FALSIFIED, AssertionMessages.WS_ADDRESSING_HEADERS_NONE );
        doTestDetailAuditing( requestWrongAddressing, false, AssertionStatus.FALSIFIED, AssertionMessages.WS_ADDRESSING_FOUND_HEADERS );
        doTestDetailAuditing( requestNoAddressingSigned, true, AssertionStatus.FALSIFIED, AssertionMessages.WS_ADDRESSING_HEADERS_SIGNED_NONE );
        doTestDetailAuditing( requestWrongAddressingSigned, true, AssertionStatus.NONE, AssertionMessages.WS_ADDRESSING_FOUND_SIGNED_HEADERS );
    }

    private void doTestDetailAuditing( final Document request,
                                       final boolean signed,
                                       final AssertionStatus expectedStatus,
                                       final AuditDetailMessage expectedAuditDetail ) throws Exception {
        final TestAudit testAudit = new TestAudit();
        final WsAddressingAssertion wsaa = new WsAddressingAssertion();
        wsaa.setRequireSignature( signed );
        wsaa.setEnableOtherNamespace( "http://schemas.xmlsoap.org/ws/2004/03/addressing" );
        final ServerWsAddressingAssertion swsaa = new ServerWsAddressingAssertion(wsaa, new MockConfig(new Properties()));
        ApplicationContexts.inject( swsaa, Collections.singletonMap( "auditFactory", testAudit.factory() ) );
        final Message requestMessage = new Message(request);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( requestMessage, new Message() );
        if ( wsaa.isRequireSignature() ) {
            final ProcessorResult result = WSSecurityProcessorUtils.getWssResults( requestMessage, "Request", null, testAudit );
            context.getDefaultAuthenticationContext().addCredentials( LoginCredentials.makeLoginCredentials( result.getXmlSecurityTokens()[0], Assertion.class ) );
        }
        final AssertionStatus status = swsaa.checkRequest( context );
        assertEquals( "Status", expectedStatus, status );
        assertTrue("AuditDetail present " + expectedAuditDetail.getId(), testAudit.isAuditPresent( expectedAuditDetail ));

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
            "        <wsa:RelatesTo RelationshipType=\"wsa:Unknown\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous5555</wsa:RelatesTo>\n" +
            "        <wsa:RelatesTo>http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous4</wsa:RelatesTo>\n" +
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
