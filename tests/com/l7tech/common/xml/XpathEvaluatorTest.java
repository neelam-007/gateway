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
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPMessage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * Test <code>XpathEvaluatorTest</code> test the various select/evaluate
 * operations in <code>XpathEvaluator</code>, namespace resolution etc.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XpathEvaluatorTest extends TestCase {
    public static final String TEST_SOAP_XML = "com/l7tech/service/resources/GetLastTradePriceSoapRequest.xml";

    public XpathEvaluatorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(XpathEvaluatorTest.class);
    }

    public void testBasicXmlMessage() throws Exception {
        Document doc = getTestDocument(TEST_SOAP_XML);
        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, (Map)null);
        List nodes = xe.select("//");
        assertTrue("Size should have been >0", nodes.size() > 0);
    }

    /**
     * Soap messsage test, extract the namespaces and
     *
     * @throws Exception
     */
    public void testSoapMessage() throws Exception {
        Document doc = getTestDocument(TEST_SOAP_XML);
        SOAPMessage sm = SoapUtil.asSOAPMessage(doc);
        Map namespaces = XpathEvaluator.getNamespaces(sm);

        XpathEvaluator xe = XpathEvaluator.newEvaluator(doc, namespaces);
        List nodes = xe.select("//SOAP-ENV:Envelope/SOAP-ENV:Body/m:GetLastTradePrice/*");
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
        Document doc = getTestDocument(TEST_SOAP_XML);
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
        Document doc = getTestDocument(TEST_SOAP_XML);
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


    private Document getTestDocument(String resourcetoread)
      throws IOException, SAXException {
        InputStream i = getInputStream(resourcetoread);
        return XmlUtil.parse(i);
    }

    private InputStream getInputStream(String resourcetoread) throws FileNotFoundException {
        if (resourcetoread == null) {
            resourcetoread = TEST_SOAP_XML;
        }
        ClassLoader cl = getClass().getClassLoader();
        InputStream i = cl.getResourceAsStream(resourcetoread);
        if (i == null) {
            throw new FileNotFoundException(resourcetoread);
        }
        return i;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}

