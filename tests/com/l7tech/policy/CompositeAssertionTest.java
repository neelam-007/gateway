/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.server.policy.assertion.composite.ServerAllAssertion;
import com.l7tech.server.policy.assertion.composite.ServerExactlyOneAssertion;
import com.l7tech.server.policy.assertion.composite.ServerOneOrMoreAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

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

    public void testSimpleLogic() throws IOException, PolicyAssertionException {
        Request req = new SoapRequest(null);
        Response resp = new SoapResponse(null);

        {
            final List kidsTrueFalseTrue = Arrays.asList(new Assertion[] {
                new TrueAssertion(),
                new FalseAssertion(),
                new TrueAssertion()
            });
            assertTrue(new ServerOneOrMoreAssertion( new OneOrMoreAssertion( kidsTrueFalseTrue ) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertFalse(new ServerExactlyOneAssertion( new ExactlyOneAssertion( kidsTrueFalseTrue ) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertFalse(new ServerAllAssertion( new AllAssertion( kidsTrueFalseTrue ) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertFalse(new ServerAllAssertion( new AllAssertion( kidsTrueFalseTrue ) ).checkRequest(req, resp) == AssertionStatus.NONE);
        }

        {
            final List kidsTrueTrueTrue = Arrays.asList(new Assertion[] {
                new TrueAssertion(),
                new TrueAssertion(),
                new TrueAssertion()
            });
            assertTrue(new ServerOneOrMoreAssertion( new OneOrMoreAssertion( kidsTrueTrueTrue ) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertFalse(new ServerExactlyOneAssertion( new ExactlyOneAssertion( kidsTrueTrueTrue ) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertTrue(new ServerAllAssertion( new AllAssertion( kidsTrueTrueTrue ) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertTrue(new ServerAllAssertion( new AllAssertion( kidsTrueTrueTrue ) ).checkRequest(req, resp) == AssertionStatus.NONE);
        }

        {
            final List kidsFalseTrueFalse = Arrays.asList(new Assertion[] {
                new FalseAssertion(),
                new TrueAssertion(),
                new FalseAssertion()
            });
            assertTrue(new ServerOneOrMoreAssertion( new OneOrMoreAssertion( kidsFalseTrueFalse ) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertTrue(new ServerExactlyOneAssertion( new ExactlyOneAssertion( kidsFalseTrueFalse) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertFalse(new ServerAllAssertion( new AllAssertion( kidsFalseTrueFalse) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertFalse(new ServerAllAssertion( new AllAssertion( kidsFalseTrueFalse) ).checkRequest(req, resp) == AssertionStatus.NONE);
        }

        {
            final List kidsFalseFalse = Arrays.asList(new Assertion[] {
                new FalseAssertion(),
                new FalseAssertion()
            });
            assertFalse(new ServerOneOrMoreAssertion( new OneOrMoreAssertion( kidsFalseFalse ) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertFalse(new ServerExactlyOneAssertion( new ExactlyOneAssertion( kidsFalseFalse ) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertFalse(new ServerAllAssertion( new AllAssertion( kidsFalseFalse ) ).checkRequest(req, resp) == AssertionStatus.NONE);
            assertFalse(new ServerAllAssertion( new AllAssertion( kidsFalseFalse ) ).checkRequest(req, resp) == AssertionStatus.NONE);
        }
    }

    public void testCompositeLogic() throws IOException, PolicyAssertionException {
        Request req = new SoapRequest(null);
        Response resp = new SoapResponse(null);

        final List kidsTrueFalseTrue = Arrays.asList(new Assertion[] {
            new TrueAssertion(),
            new FalseAssertion(),
            new TrueAssertion()
        });
        final OneOrMoreAssertion true1 = new OneOrMoreAssertion(kidsTrueFalseTrue);
        final ExactlyOneAssertion false1 = new ExactlyOneAssertion(kidsTrueFalseTrue);
        final AllAssertion false2 = new AllAssertion(kidsTrueFalseTrue);

        final List kidsFalseFalse = Arrays.asList(new Assertion[] {
            false1,
            false2
        });
        final OneOrMoreAssertion false3 = new OneOrMoreAssertion(kidsFalseFalse);

        final List kidsTrueTrueTrue = Arrays.asList(new Assertion[] {
            true1,
            new AllAssertion(Arrays.asList(new Assertion[] {
                new TrueAssertion(),
                new TrueAssertion()
            })),
            new TrueAssertion()
        });
        final OneOrMoreAssertion true2 = new OneOrMoreAssertion(kidsTrueTrueTrue);
        final AllAssertion true3 = new AllAssertion(kidsTrueTrueTrue);

        final List kidsTrueFalse = Arrays.asList(new Assertion[] {
            true2,
            false3
        });
        final AllAssertion false4 = new AllAssertion(kidsTrueFalse);
        final ExactlyOneAssertion true4 = new ExactlyOneAssertion(kidsTrueFalse);

        assertTrue( new ServerOneOrMoreAssertion( true1 ).checkRequest(req, resp) == AssertionStatus.NONE);
        assertTrue( new ServerOneOrMoreAssertion( true2 ).checkRequest(req, resp) == AssertionStatus.NONE);
        assertTrue( new ServerAllAssertion( true3 ).checkRequest(req, resp) == AssertionStatus.NONE);
        assertTrue( new ServerExactlyOneAssertion( true4 ).checkRequest(req, resp) == AssertionStatus.NONE);
        assertFalse( new ServerExactlyOneAssertion( false1 ).checkRequest(req, resp) == AssertionStatus.NONE);
        assertFalse( new ServerAllAssertion( false2 ).checkRequest(req, resp) == AssertionStatus.NONE);
        assertFalse( new ServerOneOrMoreAssertion( false3 ).checkRequest(req, resp) == AssertionStatus.NONE);
        assertFalse( new ServerAllAssertion( false4 ).checkRequest(req, resp) == AssertionStatus.NONE);
    }
}
