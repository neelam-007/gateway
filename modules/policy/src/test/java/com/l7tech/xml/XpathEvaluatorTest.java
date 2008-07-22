/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.xml;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.XpathEvaluator;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jaxen.NamespaceContext;
import org.w3c.dom.Document;

import java.util.*;

/**
 * Test <code>XpathEvaluatorTest</code> test the various select/evaluate
 * operations in <code>XpathEvaluator</code>, namespace resolution etc.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XpathEvaluatorTest extends TestCase {
    public XpathEvaluatorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(XpathEvaluatorTest.class);
    }

    private Map<String, String> namespaces = new HashMap<String, String>();
    {
        namespaces.put("SOAP-ENV", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("m", "Some-URI");
        namespaces.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("ware", "http://warehouse.acme.com/ws");
        namespaces.put("sesyn", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("foo", "http://schemas.foo.org/");
        namespaces.put("bar", "http://schemas.bar.org/");
        namespaces.put("baz", "http://schemas.baz.org/");
    }

    public void testOtherFunctions() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        System.out.println( XmlUtil.nodeToFormattedString(doc));
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, namespaces);
        List nodes = xe.select("string(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol)");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            String obj = (String)iterator.next();
            assertTrue("Value must be DIS", obj.equals("DIS"));
        }

        nodes = xe.select("namespace-uri(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice)");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            String obj = (String)iterator.next();
            assertTrue("Value must be Some-URI", obj.equals("Some-URI"));
        }

        nodes = xe.select("string-length(string(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol))");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Double obj = (Double)iterator.next();
            assertTrue("Value must be 3", obj.intValue() == 3);
        }
    }

    public void testContains() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        System.out.println(XmlUtil.nodeToFormattedString(doc));
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, namespaces);
        List nodes = xe.select("contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol,'DI')");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Boolean bool = (Boolean) iterator.next();
            assertTrue("Should return true", bool.booleanValue());
        }

        nodes = xe.select("not(contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol,'BZZT'))");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Boolean bool = (Boolean) iterator.next();
            assertTrue("Should return true", bool.booleanValue());
        }

        nodes = xe.select("not(contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/elnotpresent,'BZZT'))");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Boolean bool = (Boolean) iterator.next();
            assertTrue("Should return true", bool.booleanValue());
        }

        nodes = xe.select("not(contains(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol,'DI'))");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Boolean bool = (Boolean) iterator.next();
            assertTrue("Should return false", !bool.booleanValue());
        }

        nodes = xe.select("not(contains(translate(//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol, 'dis', 'DIS'),'DI'))");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Boolean bool = (Boolean) iterator.next();
            assertTrue("Should return false", !bool.booleanValue());
        }
    }

    public void testBasicXmlMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, (Map)null);
        List nodes = xe.select("//*");
        assertTrue("Size should have been >0", nodes.size() > 0);
    }

    /**
     * Soap messsage test, extract the namespaces and do the XPath query
     *
     * @throws Exception
     */
    public void testSoapMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, namespaces);
        List nodes = xe.select("//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice");
        assertTrue("Size should have been >0", nodes.size() > 0);
        Object ret = xe.evaluate("//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol='DIS'");
        Boolean bool = (Boolean)ret;
        assertTrue("Should have returned true (element exists)", bool.booleanValue());
    }

    /**
     * Test the namespace resolution.
     *
     * @throws Exception
     */
    public void testNamespaceSoapMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, new NamespaceContext() {
            public String translateNamespacePrefixToUri(String prefix) {
                String ns = (String)namespaces.get(prefix);
                if (ns == null) {
                    fail("The prefix does no exist " + prefix);
                }
                return ns;
            }
        });

        xe.select("//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/*");
        xe.evaluate("//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/symbol='DIS'");
    }

    /**
     * Test the namespace resolution, the namespace does not exist in the document
     *
     * @throws Exception
     */
    public void testNamespaceNonExistSoapMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        try {
            XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, new NamespaceContext() {
                public String translateNamespacePrefixToUri(String prefix) {
                    String ns = (String)namespaces.get(prefix);
                    if (ns == null) {
                        throw new NoSuchElementException();
                    }
                    return ns;
                }
            });
            xe.select("//SOAP-ENVX:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/*");
            fail("the NoSuchElementException should have been thrown");
        } catch (NoSuchElementException e) {
            // ok
        }
    }
    
    /**
     * Test the namespace resolution, the namespace does not exist in the document
     *
     * @throws Exception
     */
    public void testMethodElementEnvelopeAcestor() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, new NamespaceContext() {
            public String translateNamespacePrefixToUri(String prefix) {
                String ns = (String)namespaces.get(prefix);
                /*if (ns == null) {
                    fail("The prefix does no exist '" + prefix+"'");
                } */

                return ns;
            }
        });
        List list = xe.select("/SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/ancestor::SOAP-ENV:Envelope");
        assertTrue("Size should have been == 1, returned "+list.size(), list.size() == 1);

        list = xe.select("/SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastBid/ancestor::SOAP-ENV:Envelope");
        assertTrue("Size should have been == 0, returned "+list.size(), list.size() == 0);
    }

    public void testNonSoap() throws Exception {
        Document doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\"/>");
        System.out.println(XmlUtil.nodeToFormattedString(doc));
        HashMap namesapces = new HashMap();
        namesapces.put("s0", "http://grrr.com/nsblah");
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, namesapces);
        List nodes = xe.select("//*[local-name()='blah']");
        assertTrue("Size should have been >0", nodes.size() > 0);
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Object obj = (Object)iterator.next();
            System.out.println(obj);
        }
    }

    public void testXPathStringNumber() throws Exception {
        Document doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\">123</s0:blah>");
        System.out.println(XmlUtil.nodeToFormattedString(doc));
        HashMap namesapces = new HashMap();
        namesapces.put("s0", "http://grrr.com/nsblah");
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, namesapces);
        List nodes = xe.select("/s0:blah=\"123\"");
        assertTrue("Size should have been > 0", nodes.size() > 0);
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Boolean bool = (Boolean)iterator.next();
            assertTrue(bool.booleanValue());
        }
        nodes = xe.select("/s0:blah=123");
        assertTrue("Size should have been > 0", nodes.size() > 0);
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Boolean bool = (Boolean)iterator.next();
            assertTrue(bool.booleanValue());
        }
    }

    public void testLongTextNodes() throws Exception {
        Document doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\">123</s0:blah>");
        HashMap namesapces = new HashMap();
        namesapces.put("s0", "http://grrr.com/nsblah");
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, namesapces);
        List nodes = xe.select("(//*/text())[string-length() > 20]");
        assertTrue(nodes.size() == 0);
        doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\">blahblahblahblahblahblahblahblahblahblahblahblahblah</s0:blah>");
        xe = XpathEvaluator.newEvaluator(doc, namesapces);
        nodes = xe.select("(//*/text())[string-length() > 20]");
        assertTrue(nodes.size() == 1);

    }

    public void testLongAttributesValues() throws Exception {
        Document doc = XmlUtil.stringToDocument("<blah foo=\"blah\">123</blah>");
        HashMap namesapces = new HashMap();
        namesapces.put("s0", "http://grrr.com/nsblah");
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, namesapces);
        List nodes = xe.select("count(//*/@*[string-length() > 20]) > 0");
        System.out.println(nodes.size());

        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Boolean bool = (Boolean)iterator.next();
            assertFalse(bool.booleanValue());
        }
        doc = XmlUtil.stringToDocument("<s0:blah xmlns:s0=\"http://grrr.com/nsblah\" foo=\"blahblahblahblahblahblahblahblahblahblahblahblahblah\">123</s0:blah>");
        xe = XpathEvaluator.newEvaluator(doc, namesapces);
        nodes = xe.select("count(//*/@*[string-length() > 20]) > 0");

        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Boolean bool = (Boolean)iterator.next();
            assertTrue(bool.booleanValue());
        }

        doc = XmlUtil.stringToDocument("<blah blahblahblahblahblahblahblahblahblahblahblahblahblah=\"foo\">123</blah>");
        xe = XpathEvaluator.newEvaluator(doc, namesapces);
        nodes = xe.select("count(//*/@*[string-length() > 20]) > 0");

        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Boolean bool = (Boolean)iterator.next();
            assertFalse(bool.booleanValue());
        }
    }

    public void testWsiBspXpathsThatDontWorkWithTarariDirectXpath() throws InvalidXpathException {
        Map m = new HashMap();
        m.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        m.put("xsd", "http://www.w3.org/2001/XMLSchema");
        m.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        m.put("xenc", "http://www.w3.org/2001/04/xmlenc#");
        m.put("c14n", "http://www.w3.org/2001/10/xml-exc-c14n#");
        m.put("wsse", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
        m.put("wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");

        String R5426 = "0=count(//xenc:EncryptedKey//ds:KeyInfo[count(wsse:SecurityTokenReference)!=1])";
        new XpathExpression(R5426, m).compile();

        String R5427 = "0=count(//xenc:EncryptedData//ds:KeyInfo[count(wsse:SecurityTokenReference)!=1])";
        new XpathExpression(R5427, m).compile();

        String R3065 = "0=count(//ds:Signature/ds:SignedInfo/ds:Reference/ds:Transforms/ds:Transform[@Algorithm=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform\" and count(wsse:TransformationParameters/ds:CanonicalizationMethod)=0])";
        new XpathExpression(R3065, m).compile();
    }

    public void testDeepNested() throws Exception {
        Document doc = XmlUtil.stringToDocument("<blah foo=\"blah\"><foo><bar/></foo></blah>");
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, new HashMap());
        List res = xe.select("count(/*/*/*) > 0");
        Boolean bool = (Boolean)res.iterator().next();
        assertTrue(bool.booleanValue());
        doc = XmlUtil.stringToDocument("<blah foo=\"blah\"><foo/></blah>");
        xe = XpathEvaluator.newEvaluator(doc, new HashMap());
        res = xe.select("count(/*/*/*) > 0");
        bool = (Boolean)res.iterator().next();
        assertFalse(bool.booleanValue());
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}

