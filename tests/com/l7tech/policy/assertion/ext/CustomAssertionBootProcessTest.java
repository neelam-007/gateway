/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.ext;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test the custom assertion boot element.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class CustomAssertionBootProcessTest extends TestCase {

    public CustomAssertionBootProcessTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(CustomAssertionBootProcessTest.class);
    }

    public void testBootProcess() throws Exception {
        CustomAssertionsBootProcess ehb = new CustomAssertionsBootProcess();
        ehb.init(new TestComponentConfig());
        ehb.start();
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

}
