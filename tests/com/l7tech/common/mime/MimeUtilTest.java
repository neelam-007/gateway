/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

/**
 * @author mike
 */
public class MimeUtilTest extends TestCase {
    private static Logger log = Logger.getLogger(MimeUtilTest.class.getName());

    public MimeUtilTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MimeUtilTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSomething() throws Exception {
        // TODO write some damned tests!
    }

}
