/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.AssertionError;
import com.l7tech.message.Request;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.Response;
import com.l7tech.message.SoapResponse;

/**
 * Test the logic of composite assertions.
 * Relies on no concrete assertions other than TrueAssertion and FalseAssertion.
 * User: mike
 * Date: Jun 13, 2003
 * Time: 9:54:23 AM
 */
public class CompositeAssertionTest extends TestCase {
    private static Logger log = Logger.getLogger(CompositeAssertionTest.class.getName());

    public CompositeAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(CompositeAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSimpleLogic() throws Exception {
        Request req = new SoapRequest(null);
        Response resp = new SoapResponse(null);

        {
            final ArrayList kidsTrueFalseTrue = new ArrayList(Arrays.asList(new Assertion[] {
                new TrueAssertion(),
                new FalseAssertion(),
                new TrueAssertion()
            }));
            assertTrue(new OneOrMoreAssertion(kidsTrueFalseTrue).checkRequest(req, resp) == AssertionError.NONE);
            assertFalse(new ExactlyOneAssertion(kidsTrueFalseTrue).checkRequest(req, resp) == AssertionError.NONE);
            assertFalse(new AllAssertion(kidsTrueFalseTrue).checkRequest(req, resp) == AssertionError.NONE);
        }

        {
            final ArrayList kidsTrueTrueTrue = new ArrayList(Arrays.asList(new Assertion[] {
                new TrueAssertion(),
                new TrueAssertion(),
                new TrueAssertion()
            }));
            assertTrue(new OneOrMoreAssertion(kidsTrueTrueTrue).checkRequest(req, resp) == AssertionError.NONE);
            assertFalse(new ExactlyOneAssertion(kidsTrueTrueTrue).checkRequest(req, resp) == AssertionError.NONE);
            assertTrue(new AllAssertion(kidsTrueTrueTrue).checkRequest(req, resp) == AssertionError.NONE);
        }

        {
            final ArrayList kidsFalseTrueFalse = new ArrayList(Arrays.asList(new Assertion[] {
                new FalseAssertion(),
                new TrueAssertion(),
                new FalseAssertion()
            }));
            assertTrue(new OneOrMoreAssertion(kidsFalseTrueFalse).checkRequest(req, resp) == AssertionError.NONE);
            assertTrue(new ExactlyOneAssertion(kidsFalseTrueFalse).checkRequest(req, resp) == AssertionError.NONE);
            assertFalse(new AllAssertion(kidsFalseTrueFalse).checkRequest(req, resp) == AssertionError.NONE);
        }

        {
            final ArrayList kidsFalseFalse = new ArrayList(Arrays.asList(new Assertion[] {
                new FalseAssertion(),
                new FalseAssertion()
            }));
            assertFalse(new OneOrMoreAssertion(kidsFalseFalse).checkRequest(req, resp) == AssertionError.NONE);
            assertFalse(new ExactlyOneAssertion(kidsFalseFalse).checkRequest(req, resp) == AssertionError.NONE);
            assertFalse(new AllAssertion(kidsFalseFalse).checkRequest(req, resp) == AssertionError.NONE);
        }
    }
}
