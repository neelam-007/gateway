/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

import com.l7tech.common.xml.TestDocuments;
import org.w3c.dom.Document;

/**
 * @author mike
 */
public class WssDecoratorTest extends TestCase {
    private static Logger log = Logger.getLogger(WssDecoratorTest.class.getName());

    public WssDecoratorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WssDecoratorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSimpleDecoration() throws Exception {
        WssDecorator wssDecorator = new WssDecoratorImpl();

        Document soapMsg = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);

        

    }
}
