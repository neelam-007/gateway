package com.l7tech.xml.soap;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.HttpRequestKnobAdapter;
import com.l7tech.message.Message;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import com.l7tech.wsdl.Wsdl;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Operation;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * For testing stuff in SoapUtil class
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Mar 23, 2006<br/>
 */
public class SoapUtilTest  {

    @Test
    public void testGetOperationRPC() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "PlaceOrder_cleartext.xml");
        Document wsdldoc = TestDocuments.getTestDocument(TestDocuments.DIR + "AxisWarehouse.wsdl");
        Message msg = makeMessage(soapdoc, "");
        Wsdl wsdl = Wsdl.newInstance(null, wsdldoc);
        Operation op = SoapUtil.getBindingAndOperation(wsdl, msg).right;
        assertFalse(op == null);
        assertTrue(op.getName().equals("placeOrder"));
        // make sure this returns null when it needs to (totally unrelated request)
        soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "GetLastTradePriceSoapRequest.xml");
        msg = makeMessage(soapdoc, "");
        final Pair<Binding, Operation> nullPair = SoapUtil.getBindingAndOperation(wsdl, msg);
        assertTrue(nullPair == null);
    }

    /**
     * Same test as above, but with soap version supplied
     * @throws Exception
     */
    @Test
    public void testSoapVersion_1_1() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "PlaceOrder_cleartext.xml");
        Document wsdldoc = TestDocuments.getTestDocument(TestDocuments.DIR + "AxisWarehouse.wsdl");
        Message msg = makeMessage(soapdoc, "");
        Wsdl wsdl = Wsdl.newInstance(null, wsdldoc);
        Operation op = SoapUtil.getBindingAndOperation(wsdl, msg, SoapVersion.SOAP_1_1).right;
        assertFalse(op == null);
        assertTrue(op.getName().equals("placeOrder"));
        // make sure this returns null when it needs to (totally unrelated request)
        soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "GetLastTradePriceSoapRequest.xml");
        msg = makeMessage(soapdoc, "");
        final Pair<Binding, Operation> nullPair = SoapUtil.getBindingAndOperation(wsdl, msg, null);
        assertTrue(nullPair == null);
    }

    /**
     * Same test as above, but with a soap version supplied which finds no results
     * @throws Exception
     */
    @Test
    public void testSoapVersion_1_2_NotFound() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "PlaceOrder_cleartext.xml");
        Document wsdldoc = TestDocuments.getTestDocument(TestDocuments.DIR + "AxisWarehouse.wsdl");
        Message msg = makeMessage(soapdoc, "");
        Wsdl wsdl = Wsdl.newInstance(null, wsdldoc);
        final Pair<Binding, Operation> pair = SoapUtil.getBindingAndOperation(wsdl, msg, SoapVersion.SOAP_1_2);
        assertTrue(pair == null);
    }

    @Test
    public void testSoapVersion_1_2() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "warehouseRequestOutOfOrderHeader.xml");
        Document wsdldoc = TestDocuments.getTestDocument(TestDocuments.DIR + "Warehouse_1_2_Only.wsdl");
        Message msg = makeMessage(soapdoc, "");
        Wsdl wsdl = Wsdl.newInstance(null, wsdldoc);
        Operation op = SoapUtil.getBindingAndOperation(wsdl, msg, SoapVersion.SOAP_1_2).right;
        assertFalse(op == null);
        assertTrue(op.getName().equals("placeOrder"));
        // make sure this returns null when it needs to (totally unrelated request)
        soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "GetLastTradePriceSoapRequest.xml");
        msg = makeMessage(soapdoc, "");
        final Pair<Binding, Operation> nullPair = SoapUtil.getBindingAndOperation(wsdl, msg, null);
        assertTrue(nullPair == null);
    }

    @Test
    public void testSoapVersion_1_1_NotFound() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "warehouseRequestOutOfOrderHeader.xml");
        Document wsdldoc = TestDocuments.getTestDocument(TestDocuments.DIR + "Warehouse_1_2_Only.wsdl");
        Message msg = makeMessage(soapdoc, "");
        Wsdl wsdl = Wsdl.newInstance(null, wsdldoc);
        final Pair<Binding, Operation> pair = SoapUtil.getBindingAndOperation(wsdl, msg, SoapVersion.SOAP_1_1);
        assertNull(pair);
    }

    @Test
    public void testGetOperationDOC() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "dotNetSignedSoapRequest.xml");
        Document wsdldoc = TestDocuments.getTestDocument( "com/l7tech/policy/resources/warehouse.wsdl" );
        Message msg = makeMessage(soapdoc, "http://warehouse.acme.com/ws/listProducts");
        Wsdl wsdl = Wsdl.newInstance(null, wsdldoc);
        Operation op = SoapUtil.getBindingAndOperation(wsdl, msg).right;
        assertFalse(op == null);
        assertTrue(op.getName().equals("listProducts"));
        // in this case, the operation should be identifiable even if the soapaction is incorrect
        msg = makeMessage(soapdoc, ":foo:bar");
        final Pair<Binding, Operation> notNullPair = SoapUtil.getBindingAndOperation(wsdl, msg);
        assertFalse(notNullPair == null);
        assertTrue(op.getName().equals("listProducts"));
    }

    @BugNumber(2304)
    @Test
    public void testBugzilla2304() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "facadeAddService.xml");
        Document wsdldoc = TestDocuments.getTestDocument(TestDocuments.DIR + "bugzilla2304.wsdl");
        Message msg = makeMessage(soapdoc, "http://systinet.com/j2ee/ejb/ServiceFacade#addService?KExjYS9iYy9nb3YvYWcvY3Nvd3Mvc2VydmljZXMvU2VydmljZTspTGNhL2JjL2dvdi9hZy9jc293cy9zZXJ2aWNlcy9TZXJ2aWNlOw==");
        Wsdl wsdl = Wsdl.newInstance(null, wsdldoc);
        Operation op = SoapUtil.getBindingAndOperation(wsdl, msg).right;
        assertFalse(op == null);
        assertTrue(op.getName().equals("addService"));
        // same request with different soapaction should yield different operation
        msg = makeMessage(soapdoc, "http://systinet.com/j2ee/ejb/ServiceFacade#updateService?KExjYS9iYy9nb3YvYWcvY3Nvd3Mvc2VydmljZXMvU2VydmljZTspVg==");
        op = SoapUtil.getBindingAndOperation(wsdl, msg).right;
        assertFalse(op == null);
        assertTrue(op.getName().equals("updateService"));
        // without the soapaction, this should yield an ambiguity
        msg = makeMessage(soapdoc, "");
        final Pair<Binding, Operation> nullPair = SoapUtil.getBindingAndOperation(wsdl, msg);
        assertTrue(nullPair == null);
    }

    @SuppressWarnings({ "deprecation" })
    @BugNumber(3250)
    @Test
    public void testBug3250_QA_Wsdl() throws Exception {
        Wsdl w = Wsdl.newInstance(TestDocuments.DIR, TestDocuments.getTestDocument(TestDocuments.DIR + "bug3250.wsdl"));
        for (Object o : w.getBindingOperations()) {
            BindingOperation bop = (BindingOperation) o;
            System.out.println("Got target namespace for " + bop.getName() + ": " + SoapUtil.findTargetNamespace(w.getDefinition(), bop));
        }
    }

    @SuppressWarnings({ "deprecation" })
    @BugNumber(3250)
    @Test
    public void testBug3250_BofA_Wsdl() throws Exception {
        Wsdl w = Wsdl.newInstance(TestDocuments.DIR, TestDocuments.getTestDocument(TestDocuments.DIR + "AuthenticateServiceV001.wsdl"));
        for (Object o : w.getBindingOperations()) {
            BindingOperation bop = (BindingOperation) o;
            System.out.println("Got target namespace for " + bop.getName() + ": " + SoapUtil.findTargetNamespace(w.getDefinition(), bop));
        }
    }

    @BugNumber(3888)
    @Test
    public void testBug3888_IsSoapRejectsProcessingInstructions() throws Exception {
        Document doc = XmlUtil.stringToDocument(SOAP_MESSAGE_WITH_PROCESSING_INSTRUCTION_BEFORE_CONTENT);
        assertFalse(SoapUtil.isSoapMessage(doc));

        // TODO find a fast way to fix it so it fails on PIs hidden within the body as well
        doc = XmlUtil.stringToDocument(SOAP_MESSAGE_WITH_PROCESSING_INSTRUCTION_IN_BODY);
        assertTrue(SoapUtil.isSoapMessage(doc));
    }

    @Test
    public void testIsSoapBody() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "PlaceOrder_cleartext.xml");
        DomUtils.visitNodes(soapdoc.getDocumentElement(), new Functions.UnaryVoid<Node>(){
            @Override
            public void call(final Node node) {
                if ( node.getNodeType() == Node.ELEMENT_NODE ) {
                    Element element = (Element) node;
                    boolean isBody = false;
                    if ( "http://schemas.xmlsoap.org/soap/envelope/".equals(element.getNamespaceURI()) && element.getLocalName().equals("Body") &&
                         element.getParentNode() != null && element.getParentNode().getParentNode() == node.getOwnerDocument() &&
                         "http://schemas.xmlsoap.org/soap/envelope/".equals(element.getParentNode().getNamespaceURI()) && element.getParentNode().getLocalName().equals("Envelope") ) {
                        isBody = true;
                    }
                    try {
                        assertEquals( isBody + " " + element, isBody, SoapUtil.isBody(element) );
                    } catch (InvalidDocumentFormatException e) {
                        throw ExceptionUtils.wrap(e);
                    }
                }
            }
        });
    }

    @Test
    public void testIsSoapHeader() throws Exception {
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "PlaceOrder_cleartext.xml");
        DomUtils.visitNodes(soapdoc.getDocumentElement(), new Functions.UnaryVoid<Node>(){
            @Override
            public void call(final Node node) {
                if ( node.getNodeType() == Node.ELEMENT_NODE ) {
                    Element element = (Element) node;
                    boolean isHeader = false;
                    if ( "http://schemas.xmlsoap.org/soap/envelope/".equals(element.getNamespaceURI()) && element.getLocalName().equals("Header") &&
                         element.getParentNode() != null && element.getParentNode().getParentNode() == node.getOwnerDocument() &&
                         "http://schemas.xmlsoap.org/soap/envelope/".equals(element.getParentNode().getNamespaceURI()) && element.getParentNode().getLocalName().equals("Envelope") ) {
                        isHeader = true;
                    }
                    try {
                        assertEquals( isHeader + " " + element, isHeader, SoapUtil.isHeader(element) );
                    } catch (InvalidDocumentFormatException e) {
                        throw ExceptionUtils.wrap(e);
                    }
                }
            }
        });
    }

    @Test
    public void testUuidFormat() throws Exception {
        String id = SoapUtil.generateUniqueUri("prefix:", true);
        assertTrue(id.startsWith("prefix:"));
        String[] bits = id.split(":")[1].split("-");
        assertEquals(8, bits[0].length());
        assertEquals(4, bits[1].length());
        assertEquals(4, bits[2].length());
        assertEquals(4, bits[3].length());
        assertEquals(12, bits[4].length());
    }

    @Test
    public void testWsAddressing() throws Exception {
        try {
            SoapUtil.getWsaAddressingElements( XmlUtil.parse( "<a/>" ));
            fail("Expected exception for non SOAP message.");
        } catch ( InvalidDocumentFormatException e ) {
            // expected
        }

        final List<Element> empty = SoapUtil.getWsaAddressingElements(  XmlUtil.parse( "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body/></soap:Envelope>" ) );
        assertNotNull( "Empty list not null", empty );
        assertTrue("Empty list", empty.isEmpty());
        
        final Document soapDoc = TestDocuments.getTestDocument(TestDocuments.DIR + "dotNetSignedSoapRequest.xml");
        final List<Element> elements = SoapUtil.getWsaAddressingElements( soapDoc );
        assertEquals("Element count", 4, elements.size());

        final List<Element> notFound = SoapUtil.getWsaAddressingElements( soapDoc, new String[]{SoapUtil.WSA_NAMESPACE_10} );
        assertNotNull( "Empty list not null", notFound );
        assertTrue("Empty list", notFound.isEmpty());

        final List<Element> found = SoapUtil.getWsaAddressingElements( soapDoc, new String[]{SoapUtil.WSA_NAMESPACE} );
        assertEquals("Element count", 4, found.size());
    }

    @Test
    public void testGetSoapActionFromBindingOperation() throws Exception{
        Document soapdoc = TestDocuments.getTestDocument(TestDocuments.DIR + "warehouseRequestOutOfOrderHeader.xml");
        Document wsdldoc = TestDocuments.getTestDocument(TestDocuments.DIR + "Warehouse_1_2_Only.wsdl");
        Message msg = makeMessage(soapdoc, "");
        Wsdl wsdl = Wsdl.newInstance(null, wsdldoc);
        final Pair<Binding, Operation> pair = SoapUtil.getBindingAndOperation(wsdl, msg, SoapVersion.SOAP_1_2);
        assertFalse(pair == null);

        final Binding binding = pair.left;
        final BindingOperation bindingOperation = binding.getBindingOperation("listProducts", null, null);
        final String soapAction = SoapUtil.extractSoapAction(bindingOperation, SoapVersion.SOAP_1_2);
        assertEquals("http://warehouse.acme.com/ws/listProducts", soapAction);
    }

    public static final String SOAP_MESSAGE_WITH_PROCESSING_INSTRUCTION_BEFORE_CONTENT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<?xml-stylesheet type=\"text/xsl\"\n" +
            "href=\"http://hugh.l7tech.com/xsl/harmless.xsl\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "<soap:Body>\n" +
            "  <placeOrder xmlns=\"http://warehouse.acme.com/ws\">\n" +
            "    <productid>111111114</productid>\n" +
            "      <amount>1</amount>\n" +
            "      <price>1230</price>\n" +
            "      <accountid>997</accountid>\n" +
            "  </placeOrder>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    public static final String SOAP_MESSAGE_WITH_PROCESSING_INSTRUCTION_IN_BODY =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "<soap:Body>\n" +
            "  <placeOrder xmlns=\"http://warehouse.acme.com/ws\">\n" +
            "    <productid>111111114</productid>\n" +
            "<?xml-stylesheet type=\"text/xsl\"\n" +
            "href=\"http://hugh.l7tech.com/xsl/harmless.xsl\"?>\n" +
            "      <amount>1</amount>\n" +
            "      <price>1230</price>\n" +
            "      <accountid>997</accountid>\n" +
            "  </placeOrder>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";


    private Message makeMessage(final Document doc, final String saction) {
        // produce fake message with arguments
        Message output = new Message(doc);
        output.attachHttpRequestKnob(new HttpRequestKnobAdapter(saction));
        return output;
    }
    
    @Test
    public void testDefaultIdAttrConfig() throws Exception {
        StringBuilder out = new StringBuilder();
        Set<FullQName> qns = SoapConstants.DEFAULT_ID_ATTRIBUTE_QNAMES;
        for (FullQName qn : qns) {
            out.append(qn.toString()).append("\n");
        }
        System.out.println(out.toString());
    }
}
