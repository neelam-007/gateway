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
import com.l7tech.common.xml.TestDocuments;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.logging.Logger;

/**
 * Test case demonstrating that both xs4j "exclusive" canonicalizers fail for unknown reasons
 * when canonicalizing the "productid" element in our placeorder_cleartext sample message.
 */
public class CanonicalizerBugRepro extends TestCase {
    private static final Logger log = Logger.getLogger(CanonicalizerBugRepro.class.getName());

    public CanonicalizerBugRepro(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(CanonicalizerBugRepro.class);
        TestSetup wrapper = new TestSetup(suite) {
            // this setup is run before all tests
            protected void setUp() throws Exception {
            }

            protected void tearDown() throws Exception {
            }
        };
        return wrapper;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testExclusiveCanonicalizer() throws Exception {
        Document message = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element productid = (Element)message.getElementsByTagNameNS("", "productid").item(0);
        assertNotNull(productid);

        Canonicalizer exclusive = new ExclusiveC11r();

        exclusive.canonicalize(productid, System.err);

    }

    public void testExclusiveWithCommentsCanonicalizer() throws Exception {
        Document message = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element productid = (Element)message.getElementsByTagNameNS("", "productid").item(0);
        assertNotNull(productid);

        Canonicalizer exclusive = new ExclusiveC11rWC();

        exclusive.canonicalize(productid, System.err);

    }

    public void testW3CCanonicalizer() throws Exception {
        Document message = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element productid = (Element)message.getElementsByTagNameNS("", "productid").item(0);
        assertNotNull(productid);

        Canonicalizer exclusive = new W3CCanonicalizer();

        exclusive.canonicalize(productid, System.err);
    }
}
