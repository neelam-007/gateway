/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.dom.DOMSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Test XpathEvaluator class.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XpathEvaluatorTest extends TestCase {
    private static Logger log = Logger.getLogger(XpathEvaluatorTest.class.getName());
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

