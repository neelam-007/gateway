/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy;

//import com.l7tech.server.ApplicationContexts;
import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.composite.ServerAllAssertion;
import com.l7tech.server.policy.assertion.composite.ServerExactlyOneAssertion;
import com.l7tech.server.policy.assertion.composite.ServerOneOrMoreAssertion;
import com.l7tech.test.BugId;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

/**
 * Test the logic of composite assertions.
 * Relies on no concrete assertions other than TrueAssertion and FalseAssertion.
 * User: mike
 * Date: Jun 13, 2003
 * Time: 9:54:23 AM
 */
public class CompositeAssertionTest {
    ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();

    @Test
    public void testSimpleLogic() throws Exception {
        ServerPolicyFactory.doWithEnforcement(false, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(new ByteArrayStashManager(),
                ContentTypeHeader.XML_DEFAULT, new EmptyInputStream()), new Message(), false);
                {
                    final List kidsTrueFalseTrue = Arrays.asList(
                            new TrueAssertion(),
                            new FalseAssertion(),
                            new TrueAssertion());

                    assertTrue(new ServerOneOrMoreAssertion(new OneOrMoreAssertion(kidsTrueFalseTrue), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertFalse(new ServerExactlyOneAssertion(new ExactlyOneAssertion(kidsTrueFalseTrue), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertFalse(new ServerAllAssertion(new AllAssertion(kidsTrueFalseTrue), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertFalse(new ServerAllAssertion(new AllAssertion(kidsTrueFalseTrue), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                }

                {
                    final List kidsTrueTrueTrue = Arrays.asList(
                            new TrueAssertion(),
                            new TrueAssertion(),
                            new TrueAssertion());
                    assertTrue(new ServerOneOrMoreAssertion(new OneOrMoreAssertion(kidsTrueTrueTrue), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertFalse(new ServerExactlyOneAssertion(new ExactlyOneAssertion(kidsTrueTrueTrue), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertTrue(new ServerAllAssertion(new AllAssertion(kidsTrueTrueTrue), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertTrue(new ServerAllAssertion(new AllAssertion(kidsTrueTrueTrue), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                }

                {
                    final List kidsFalseTrueFalse = Arrays.asList(
                            new FalseAssertion(),
                            new TrueAssertion(),
                            new FalseAssertion());
                    assertTrue(new ServerOneOrMoreAssertion(new OneOrMoreAssertion(kidsFalseTrueFalse), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertTrue(new ServerExactlyOneAssertion(new ExactlyOneAssertion(kidsFalseTrueFalse), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertFalse(new ServerAllAssertion(new AllAssertion(kidsFalseTrueFalse), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertFalse(new ServerAllAssertion(new AllAssertion(kidsFalseTrueFalse), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                }

                {
                    final List kidsFalseFalse = Arrays.asList(
                            new FalseAssertion(),
                            new FalseAssertion());
                    assertFalse(new ServerOneOrMoreAssertion(new OneOrMoreAssertion(kidsFalseFalse), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertFalse(new ServerExactlyOneAssertion(new ExactlyOneAssertion(kidsFalseFalse), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertFalse(new ServerAllAssertion(new AllAssertion(kidsFalseFalse), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                    assertFalse(new ServerAllAssertion(new AllAssertion(kidsFalseFalse), applicationContext).checkRequest(context) == AssertionStatus.NONE);
                }
                return null;
            }
        });
    }

    @Test
    public void testCompositeLogic() throws Exception {
        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(new ByteArrayStashManager(),
                        ContentTypeHeader.XML_DEFAULT, new EmptyInputStream()),
                        new Message(),
                        false);

        final List kidsTrueFalseTrue = Arrays.asList(
                new TrueAssertion(),
                new FalseAssertion(),
                new TrueAssertion());
        final OneOrMoreAssertion true1 = new OneOrMoreAssertion(kidsTrueFalseTrue);
        final ExactlyOneAssertion false1 = new ExactlyOneAssertion(kidsTrueFalseTrue);
        final AllAssertion false2 = new AllAssertion(kidsTrueFalseTrue);

        final List kidsFalseFalse = Arrays.asList(false1,
                false2);
        final OneOrMoreAssertion false3 = new OneOrMoreAssertion(kidsFalseFalse);

        final List kidsTrueTrueTrue = Arrays.asList(true1,
                new AllAssertion(Arrays.asList(
                        new TrueAssertion(),
                        new TrueAssertion())),
                new TrueAssertion());
        final OneOrMoreAssertion true2 = new OneOrMoreAssertion(kidsTrueTrueTrue);
        final AllAssertion true3 = new AllAssertion(kidsTrueTrueTrue);

        final List kidsTrueFalse = Arrays.asList(
                true2,
                false3);
        final AllAssertion false4 = new AllAssertion(kidsTrueFalse);
        final ExactlyOneAssertion true4 = new ExactlyOneAssertion(kidsTrueFalse);

        ServerPolicyFactory.doWithEnforcement(false, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                assertTrue(new ServerOneOrMoreAssertion(true1, applicationContext).checkRequest(context) == AssertionStatus.NONE);
                assertTrue(new ServerOneOrMoreAssertion(true2, applicationContext).checkRequest(context) == AssertionStatus.NONE);
                assertTrue(new ServerAllAssertion(true3, applicationContext).checkRequest(context) == AssertionStatus.NONE);
                assertTrue(new ServerExactlyOneAssertion(true4, applicationContext).checkRequest(context) == AssertionStatus.NONE);
                assertFalse(new ServerExactlyOneAssertion(false1, applicationContext).checkRequest(context) == AssertionStatus.NONE);
                assertFalse(new ServerAllAssertion(false2, applicationContext).checkRequest(context) == AssertionStatus.NONE);
                assertFalse(new ServerOneOrMoreAssertion(false3, applicationContext).checkRequest(context) == AssertionStatus.NONE);
                assertFalse(new ServerAllAssertion(false4, applicationContext).checkRequest(context) == AssertionStatus.NONE);
                return null;
            }
        });
    }

    @Test
    public void testCantChangeChildrenIfLocked() throws Exception {
        CompositeAssertion ca = new AllAssertion(Arrays.asList(new TrueAssertion(), new FalseAssertion()));
        ca.getChildren().add(new TrueAssertion());
        ca.addChild(new FalseAssertion());
        assertEquals(4, ca.getChildren().size());

        ca.lock();

        try {
            ca.getChildren().add(new TrueAssertion());
            fail("Expected exception not thrown -- locked composite should forbid modifying children");
        } catch (RuntimeException e) {
            // Ok
        }

        try {
            ca.addChild(new FalseAssertion());
            fail("Expected exception not thrown -- locked composite should forbid adding children");
        } catch (RuntimeException e) {
            // Ok
        }

        assertEquals(4, ca.getChildren().size());
    }

    @Test
    @BugId( "SSG-9757" )
    public void testEmptyAllSucceeds() throws Exception {
        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(new ByteArrayStashManager(),
                                ContentTypeHeader.XML_DEFAULT, new EmptyInputStream()),
                        new Message(),
                        false);

        AllAssertion all = new AllAssertion( Collections.<Assertion>emptyList() );
        ServerAllAssertion sAll = new ServerAllAssertion( all, applicationContext );
        assertEquals( AssertionStatus.NONE, sAll.checkRequest( context ) );
    }

    @Test
    @BugId( "SSG-9757" )
    public void testEmptyOrFails() throws Exception {
        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(new ByteArrayStashManager(),
                                ContentTypeHeader.XML_DEFAULT, new EmptyInputStream()),
                        new Message(),
                        false);

        OneOrMoreAssertion oom = new OneOrMoreAssertion( Collections.<Assertion>emptyList() );
        ServerOneOrMoreAssertion sOom = new ServerOneOrMoreAssertion( oom, applicationContext );
        assertEquals( AssertionStatus.FALSIFIED, sOom.checkRequest( context ) );
    }
}
