package com.l7tech.server.message.metrics;

import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.event.metrics.AssertionFinished;
import com.l7tech.server.event.metrics.ServiceFinished;
import com.l7tech.server.message.AssertionTraceListener;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.util.Functions;
import org.apache.commons.lang.ClassUtils;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Unit Tests for {@link GatewayMetricsUtils}
 */
@RunWith(MockitoJUnitRunner.class)
public class GatewayMetricsUtilsTest {
    @Mock
    private ServerAssertion serverAssertion;

    @Mock
    private Assertion assertion;

    @Mock
    AssertionTraceListener traceListener;

    @Mock
    private GatewayMetricsListener subscriber;

    private PolicyEnforcementContext context;
    private final GatewayMetricsPublisher publisher = new GatewayMetricsPublisher();

    @Before
    public void setUp() throws FindException {
        Mockito.doReturn(assertion).when(serverAssertion).getAssertion();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
    }

    static class MyListener extends GatewayMetricsListener {
        private final Functions.UnaryVoid<AssertionFinished> callable;
        MyListener(final Functions.UnaryVoid<AssertionFinished> callable) {
            this.callable = callable;
        }

        @Override
        public void assertionFinished(@NotNull final AssertionFinished event) {
            callable.call(event);
        }
    }

    private GatewayMetricsListener createListener(final Functions.UnaryVoid<AssertionFinished> callable) {
        return new MyListener(callable);
    }

    @Test
    public void testSetPublisher() throws Exception {
        GatewayMetricsUtils.setPublisher(context, publisher);
        assertNull("Publisher has no subscribers, should be null", ((GatewayMetricsSupport)context).getGatewayMetricsEventsPublisher());

        publisher.addListener(subscriber);
        GatewayMetricsUtils.setPublisher(context, publisher);
        assertThat("Publisher has subscribers, should be not null", ((GatewayMetricsSupport)context).getGatewayMetricsEventsPublisher(), Matchers.allOf(Matchers.sameInstance(publisher), Matchers.notNullValue()));
    }

    @Test
    public void testSetPublisherForChildPec() throws Exception {
        final GatewayMetricsPublisher publisher = new GatewayMetricsPublisher();
        publisher.addListener(subscriber);

        final PolicyEnforcementContext parent = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        final PolicyEnforcementContext child = PolicyEnforcementContextFactory.createPolicyEnforcementContext(parent);

        GatewayMetricsUtils.setPublisher(parent, publisher);
        assertThat("Publisher has subscribers, should be not null", ((GatewayMetricsSupport)parent).getGatewayMetricsEventsPublisher(), Matchers.allOf(Matchers.sameInstance(publisher), Matchers.notNullValue()));
        GatewayMetricsUtils.setPublisher(parent,child);
        assertThat(((GatewayMetricsSupport)child).getGatewayMetricsEventsPublisher(), Matchers.allOf(Matchers.sameInstance(((GatewayMetricsSupport)parent).getGatewayMetricsEventsPublisher()), Matchers.notNullValue()));
    }

    @Ignore
    @Test
    public void testReadOnlyPecThreadOwner() throws Throwable {
        final AtomicReference<Throwable> potentialErrorRef = new AtomicReference<>();
        final GatewayMetricsListener listener = createListener(
                new Functions.UnaryVoid<AssertionFinished>() {
                    @Override
                    public void call(final AssertionFinished assertionFinished) {
                        try {
                            Assert.assertNotNull(assertionFinished);

                            final PolicyEnforcementContext context = assertionFinished.getContext();
                            Assert.assertNotNull(context);

                            Assert.assertEquals(context.getClass(), GatewayMetricsUtils.getPecClass());

                            try {
                                context.getAllVariables();
                                Assert.fail("Accessing getAllVariables from different thread is not allowed");
                            } catch (final IllegalStateException ignore) {
                                // this is expected
                            }

                            try {
                                context.setTraceListener(traceListener);
                                Assert.fail("Method setTraceListener is not supported");
                            } catch (final UnsupportedOperationException ignore) {
                                // this is expected
                            }

                            for (final Method method : PolicyEnforcementContext.class.getDeclaredMethods()) {
                                final Type[] types = method.getGenericParameterTypes();
                                try {
                                    if (types.length > 0) {
                                        final Object[] args = new Object[types.length];
                                        for (int i = 0; i < types.length; ++i) {
                                            if (types[i] instanceof Class<?>) {
                                                final Class<?> clazz = (Class<?>)types[i];
                                                if (clazz.isPrimitive()) {
                                                    final Class<?> wrapperClass = ClassUtils.primitiveToWrapper(clazz);
                                                    if (Boolean.class.equals(wrapperClass)) {
                                                        args[i] = true;
                                                    } else if (Character.class.equals(wrapperClass)) {
                                                        args[i] = 'a';
                                                    } else if (Byte.class.equals(wrapperClass)) {
                                                        args[i] = (byte)0;
                                                    } else if (Short.class.equals(wrapperClass)) {
                                                        args[i] = (short)0;
                                                    } else if (Integer.class.equals(wrapperClass)) {
                                                        args[i] = 0;
                                                    } else if (Long.class.equals(wrapperClass)) {
                                                        args[i] = (long)0;
                                                    } else if (Float.class.equals(wrapperClass)) {
                                                        args[i] = (float)0;
                                                    } else if (Double.class.equals(wrapperClass)) {
                                                        args[i] = (double)0;
                                                    }
                                                }
                                            }
                                        }
                                        method.invoke(context, args);
                                    } else {
                                        method.invoke(context);
                                    }
                                } catch (InvocationTargetException ex) {
                                    final Throwable cause = ex.getCause();
                                    Assert.assertNotNull(cause);
                                    Assert.assertThat(cause, Matchers.anyOf(Matchers.instanceOf(UnsupportedOperationException.class), Matchers.instanceOf(IllegalStateException.class)));
                                    if (cause instanceof IllegalStateException) {
                                        Assert.assertThat(cause.getMessage(), Matchers.equalTo("PEC is not owned by this thread"));
                                    }
                                    if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                                        Assert.assertThat(cause, Matchers.instanceOf(IllegalStateException.class));
                                    }
                                }
                            }
                        } catch (Throwable ex) {
                            potentialErrorRef.set(ex);
                        }
                    }
                }
        );
        publisher.addListener(listener);

        final AtomicReference<PolicyEnforcementContext> contextRef = new AtomicReference<>();
        final AtomicReference<AssertionFinished> assertionFinishedEventRef = new AtomicReference<>();

        // create a new thread per property
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // create the context in a new thread
                    final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null, null);
                    GatewayMetricsUtils.setPublisher(context, publisher);
                    // wire it up
                    final AssertionFinished assertionFinished = GatewayMetricsUtils.createAssertionFinishedEvent(context, serverAssertion.getAssertion(), new LatencyMetrics(0, 0));
                    Assert.assertNotNull(assertionFinished.getContext());

                    // this thread should be able to access the content
                    assertionFinished.getContext().getAllVariables();

                    contextRef.set(context);
                    assertionFinishedEventRef.set(assertionFinished);

                } catch (Throwable ex) {
                    potentialErrorRef.set(ex);
                }
            }
        });
        thread.start();
        thread.join(10000); // should be more than reasonable time to finish!!!!

        // make sure the thread finishes without errors
        Assert.assertNull(potentialErrorRef.get());

        // get the assertionFinished event
        final AssertionFinished assertionFinished = assertionFinishedEventRef.get();
        Assert.assertNotNull(assertionFinished);

        publisher.publishEvent(assertionFinished);

        // make sure the thread finishes without errors
        final Throwable ex = potentialErrorRef.get();
        if (ex != null) {
            throw ex;
        }
    }

    @Test
    public void testCreateAssertionFinishedEvent() throws Exception {
        final LatencyMetrics assertionMetrics = new LatencyMetrics(0, 0);
        final Assertion assertion = serverAssertion.getAssertion();
        final AssertionFinished assertionFinished = GatewayMetricsUtils.createAssertionFinishedEvent(context, assertion, assertionMetrics);
        assertThat("Assertion in assertionFinished should be the same instance and cannot be null", assertionFinished.getAssertion(),  Matchers.allOf(Matchers.sameInstance(assertion), Matchers.notNullValue()));
        assertThat("AssertionMetrics in assertionFinished should be the same instance and cannot be null", assertionFinished.getAssertionMetrics(), Matchers.allOf(Matchers.sameInstance(assertionMetrics), Matchers.notNullValue()));
    }

    @Test
    public void testCreateServiceFinishedEvent() throws Exception {
        final LatencyMetrics serviceMetrics = new LatencyMetrics(0, 0);
        final ServiceFinished serviceFinished = GatewayMetricsUtils.createServiceFinishedEvent(context, serviceMetrics);
        assertThat("AssertionMetrics in assertionFinished should be the same instance and cannot be null", serviceFinished.getServiceMetrics(), Matchers.allOf(Matchers.sameInstance(serviceMetrics), Matchers.notNullValue()));
    }
}