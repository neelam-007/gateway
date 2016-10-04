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
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.event.metrics.AssertionFinished;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.metrics.GatewayMetricsListener;
import com.l7tech.server.message.metrics.GatewayMetricsPublisher;
import com.l7tech.server.message.metrics.GatewayMetricsUtils;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.composite.ServerAllAssertion;
import com.l7tech.server.policy.assertion.composite.ServerExactlyOneAssertion;
import com.l7tech.server.policy.assertion.composite.ServerOneOrMoreAssertion;
import com.l7tech.test.BugId;
import com.l7tech.util.Functions;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

/**
 * Test the logic of composite assertions.
 * Relies on no concrete assertions other than TrueAssertion, FalseAssertion, and RaiseErrorAssertion.
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

    @Test
    @BugId( "SSG-13883" )
    public void testAssertionLatencyOnError() throws Exception {

        doTestAssertionLatencyOnError(new RaiseErrorAssertion());

        doTestAssertionLatencyOnError(
                new TestLatencyOnErrorAssertion(
                        new Functions.UnaryThrows<AssertionStatus, PolicyEnforcementContext, RuntimeException>() {
                            @Override
                            public AssertionStatus call(final PolicyEnforcementContext context) throws RuntimeException {
                                throw new RuntimeException("RuntimeException from my TestLatencyOnErrorAssertion");
                            }
                        },
                        "Test Latency onError Assertion with RuntimeException"
                )
        );

        doTestAssertionLatencyOnError(
                new TestLatencyOnErrorAssertion(
                        new Functions.UnaryThrows<AssertionStatus, PolicyEnforcementContext, Error>() {
                            @Override
                            public AssertionStatus call(final PolicyEnforcementContext context) throws Error {
                                throw new Error("Error from my TestLatencyOnErrorAssertion");
                            }
                        },
                        "Test Latency onError Assertion with Error"
                )
        );
    }

    private void doTestAssertionLatencyOnError(final Assertion assertion) throws Exception {
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        final GatewayMetricsPublisher publisher = new GatewayMetricsPublisher();
        final GatewayMetricsListener subscriber = Mockito.mock(GatewayMetricsListener.class);
        publisher.addListener(subscriber);
        GatewayMetricsUtils.setPublisher(context, publisher);

        ServerPolicyFactory.doWithEnforcement(false, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Mockito.verify(subscriber, Mockito.never()).assertionFinished(Mockito.any(AssertionFinished.class));
                try {
                    new ServerAllAssertion(new AllAssertion(Collections.singletonList(assertion)), applicationContext).checkRequest(context);
                } catch (final Throwable ex) {
                    if (assertion instanceof RaiseErrorAssertion) {
                        Assert.assertThat(ex, Matchers.instanceOf(RaisedByPolicyException.class));
                    } else if (assertion instanceof TestLatencyOnErrorAssertion) {
                        final Throwable expectedException = ((TestLatencyOnErrorAssertion)assertion).getExceptionThrown();
                        Assert.assertNotNull(expectedException);
                        Assert.assertThat(ex, Matchers.sameInstance(expectedException));
                    } else {
                        Assert.fail("Unexpected assertion type: " + assertion.getClass().getName());
                    }
                    Mockito.verify(subscriber, Mockito.times(1)).assertionFinished(Mockito.any(AssertionFinished.class));
                }
                return null;
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    public static class TestLatencyOnErrorAssertion extends Assertion {

        private static final String META_INITIALIZED = TestLatencyOnErrorAssertion.class.getName() + ".metadataInitialized";

        private final Functions.UnaryThrows<AssertionStatus, PolicyEnforcementContext, ? extends Throwable> checkRequestCallback;
        private final String shortName, longName;

        private Throwable exceptionThrown;

        // default constructor is only used by policy processor framework
        @SuppressWarnings("unused")
        public TestLatencyOnErrorAssertion() {
            this.checkRequestCallback = null;
            this.shortName = null;
            this.longName = null;
        }

        TestLatencyOnErrorAssertion(
                final Functions.UnaryThrows<AssertionStatus, PolicyEnforcementContext, ? extends Throwable> checkRequestCallback,
                final String name
        ) {
            this(checkRequestCallback, name, name);
        }

        TestLatencyOnErrorAssertion(
                final Functions.UnaryThrows<AssertionStatus, PolicyEnforcementContext, ? extends Throwable> checkRequestCallback,
                final String shortName,
                final String longName
        ) {
            Assert.assertNotNull(checkRequestCallback);
            this.checkRequestCallback = checkRequestCallback;
            Assert.assertThat(shortName, Matchers.not(Matchers.isEmptyOrNullString()));
            this.shortName = shortName;
            Assert.assertThat(longName, Matchers.not(Matchers.isEmptyOrNullString()));
            this.longName = longName;
        }

        @Override
        public AssertionMetadata meta() {
            final DefaultAssertionMetadata meta = super.defaultMeta();
            if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
                return meta;

            // Set description for GUI
            meta.put(AssertionMetadata.SHORT_NAME, shortName);
            meta.put(AssertionMetadata.LONG_NAME, longName);

            // request default feature set name for our class name, since we are a known optional module
            // that is, we want our required feature set to be "assertion:SimpleGatewayMetricExtractor" rather than "set:modularAssertions"
            meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

            // want a placeholder server assertion that always fails
            meta.put(AssertionMetadata.SERVER_ASSERTION_CLASSNAME, ServerTestLatencyOnErrorAssertion.class.getName());

            meta.put(META_INITIALIZED, Boolean.TRUE);
            return meta;
        }

        @NotNull
        Functions.UnaryThrows<AssertionStatus, PolicyEnforcementContext, ? extends Throwable> getCheckRequestCallback() {
            Assert.assertNotNull(checkRequestCallback);
            return checkRequestCallback;
        }

        void setExceptionThrown(final Throwable exceptionThrown) {
            this.exceptionThrown = exceptionThrown;
        }

        public Throwable getExceptionThrown() {
            return exceptionThrown;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class ServerTestLatencyOnErrorAssertion extends AbstractServerAssertion<TestLatencyOnErrorAssertion> {

        public ServerTestLatencyOnErrorAssertion(@NotNull final TestLatencyOnErrorAssertion assertion) {
            super(assertion);
        }

        @Override
        public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
            final TestLatencyOnErrorAssertion assertion = getAssertion();
            Assert.assertNotNull(assertion);
            try {
                return assertion.getCheckRequestCallback().call(context);
            } catch (final Throwable e) {
                assertion.setExceptionThrown(e);
                if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                } else if (e instanceof Error) {
                    throw (Error)e;
                }
                Assert.fail("Unexpected exception thrown: " + e.getClass().getName());
                return null;
            }
        }
    }
}
