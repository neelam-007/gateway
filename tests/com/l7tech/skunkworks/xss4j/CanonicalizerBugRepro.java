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
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.logging.Logger;

/**
 * Test case demonstrating that both xs4j "exclusive" canonicalizers fail for unknown reasons
 * when canonicalizing the "productid" element in our placeorder_cleartext sample message.
 * It seems that some canonicalizer can only canonalicalize full documents.
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

    private void useCanon(Canonicalizer canon) throws Exception {
        Document message = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        Element productid = (Element)message.getElementsByTagNameNS("", "productid").item(0);
        System.out.println("Using canon to serialize document");
        canon.canonicalize(message, System.out);
        System.out.println("Document serialized ok. Trying to canon a node.");
        canon.canonicalize(productid, System.out);
        System.out.println("Node serialized ok.");
    }
}
