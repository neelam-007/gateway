/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jaxen.NamespaceContext;
import org.w3c.dom.Document;

import javax.xml.soap.SOAPMessage;
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

    public void testOtherFunctions() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        System.out.println(XmlUtil.nodeToFormattedString(doc));
        SOAPMessage sm = SoapUtil.asSOAPMessage(doc);
        Map namespaces = XpathEvaluator.getNamespaces(sm);
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
        SOAPMessage sm = SoapUtil.asSOAPMessage(doc);
        Map namespaces = XpathEvaluator.getNamespaces(sm);
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
    }

    public void testBasicXmlMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, (Map)null);
        List nodes = xe.select("//");
        assertTrue("Size should have been >0", nodes.size() > 0);
    }

    /**
     * Soap messsage test, extract the namespaces and do the XPath query
     *
     * @throws Exception
     */
    public void testSoapMessage() throws Exception {
        Document doc = TestDocuments.getTestDocument(TestDocuments.TEST_SOAP_XML);
        SOAPMessage sm = SoapUtil.asSOAPMessage(doc);
        Map namespaces = XpathEvaluator.getNamespaces(sm);

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
        SOAPMessage sm = SoapUtil.asSOAPMessage(doc);
        final Map namespaces = XpathEvaluator.getNamespaces(sm);

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
        SOAPMessage sm = SoapUtil.asSOAPMessage(doc);
        final Map namespaces = XpathEvaluator.getNamespaces(sm);

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
        SOAPMessage sm = SoapUtil.asSOAPMessage(doc);
        final Map namespaces = XpathEvaluator.getNamespaces(sm);

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

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}

