/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.xss4j;

import com.ibm.xml.dsig.Canonicalizer;
import com.ibm.xml.dsig.transform.ExclusiveC11r;
import com.ibm.xml.dsig.transform.ExclusiveC11rWC;
import com.ibm.xml.dsig.transform.W3CCanonicalizer;
import com.ibm.xml.dsig.transform.W3CCanonicalizer2WC;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.logging.Logger;

/**
 * Test case demonstrating that both xs4j "exclusive" canonicalizers fail for unknown reasons
 * when canonicalizing the "productid" element in our placeorder_cleartext sample message.
 * Some canonicalizer (ExclusiveC11r and ExclusiveC11rWC) throw an exception when canonicalize
 * certain elements.
 */
public class CanonicalizerBugRepro extends TestCase {
    private static final Logger log = Logger.getLogger(CanonicalizerBugRepro.class.getName());

    public CanonicalizerBugRepro(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(CanonicalizerBugRepro.class);
        return suite;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testExclusiveCanonicalizer() throws Exception {
        useCanon(new ExclusiveC11r());
    }

    public void testExclusiveWithCommentsCanonicalizer() throws Exception {
        useCanon(new ExclusiveC11rWC());
    }

    public void testW3CCanonicalizer() throws Exception {
        useCanon(new W3CCanonicalizer());
    }

    public void testW3CCanonicalizer2WC() throws Exception {
        useCanon(new W3CCanonicalizer2WC());
    }

    private void useCanon(Canonicalizer canon) throws Exception {
        Document message = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element body = SoapUtil.getBodyElement(message);
        Element placeOrder = (Element)message.getElementsByTagNameNS("http://warehouse.acme.com/ws", "placeOrder").item(0);
        Element productid = (Element)message.getElementsByTagName("productid").item(0);

        canon.canonicalize(message, System.out);
        assertTrue(body != null);
        canon.canonicalize(body, System.out);
        assertTrue(placeOrder != null);
        canon.canonicalize(placeOrder, System.out);
        assertTrue(productid != null);
        canon.canonicalize(productid, System.out);

        System.out.println("Node serialized ok.");
    }
}
