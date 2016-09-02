package com.l7tech.server.message;

import com.l7tech.message.Message;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.server.message.metrics.GatewayMetricsListener;
import com.l7tech.server.message.metrics.GatewayMetricsPublisher;
import com.l7tech.server.message.metrics.GatewayMetricsSupport;
import com.l7tech.server.message.metrics.GatewayMetricsUtils;
import com.l7tech.server.policy.assertion.RoutingResultListener;
import com.l7tech.test.BugId;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@BugId("SSG-9124")
public class ChildPolicyEnforcementContextTest {
    private static final String NAME = "varName";
    private static final String VALUE = "testValue";

    private ChildPolicyEnforcementContext child;
    private PolicyEnforcementContext parent;
    private Message request;
    private Message response;

    @Before
    public void setup() {
        request = new Message();
        response = new Message();
        parent = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        child = (ChildPolicyEnforcementContext) PolicyEnforcementContextFactory.createPolicyEnforcementContext(parent);
    }

    @Test
    public void putParentVariableNotPrefixed() throws Exception {
        parent.setVariable(NAME, VALUE);
        child.putParentVariable(NAME, false);
        assertEquals(VALUE, child.getVariable(NAME));
    }

    @Test
    public void putParentVariablesPrefixed() throws Exception {
        parent.setVariable(NAME + ".test", VALUE);
        child.putParentVariable(NAME, true);
        assertEquals(VALUE, child.getVariable(NAME + ".test"));
    }

    @Test
    public void getOrCreateTargetMessageRequest() throws Exception {
        assertEquals(request, child.getOrCreateTargetMessage(new MessageTargetableSupport(TargetMessageType.REQUEST), false));
    }

    @Test
    public void getOrCreateTargetMessageResponse() throws Exception {
        assertEquals(response, child.getOrCreateTargetMessage(new MessageTargetableSupport(TargetMessageType.RESPONSE), false));
    }

    @Test(expected= NoSuchVariableException.class)
    public void getOrCreateTargetMessageOtherDoesNotExist() throws Exception {
        child.getOrCreateTargetMessage(new MessageTargetableSupport(TargetMessageType.OTHER), false);
    }

    /**
     * If msg already exists on the parent context, should retrieve this msg.
     */
    @Test
    public void getOrCreateTargetMessageOtherParentVariable() throws Exception {
        final Message parentMsg = new Message();
        parent.setVariable(NAME, parentMsg);
        child.putParentVariable(NAME, true);
        final Message childMsg = child.getOrCreateTargetMessage(new MessageTargetableSupport(NAME), false);
        assertEquals(parentMsg, childMsg);
    }

    @Test
    public void getOrCreateTargetMessageOtherNotParentVariable() throws Exception {
        final Message parentMsg = new Message();
        parent.setVariable(NAME, parentMsg);
        // do not set parent variable on child context
        final Message childMsg = child.getOrCreateTargetMessage(new MessageTargetableSupport(NAME), false);
        assertNotSame(parentMsg, childMsg);
    }

    @Test
    public void contextWithRoutingAssertionSettingShouldRelayToParent() throws MalformedURLException {
        Set<String> forwarded = new HashSet<>();
        PolicyEnforcementContext parent = buildParentExpectingForwards(forwarded);

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(parent, true);
        invokeAllRoutingMetricMethods(context);

        Set<String> expected = new HashSet<>();
        for (Method method : PolicyEnforcementContext.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(RoutingMetricsRelated.class)) {
                expected.add(method.getName());
            }
        }
        assertTrue("Some expected methods are not being forwarded: " + CollectionUtils.subtract(expected, forwarded),
                forwarded.containsAll(expected));
        assertTrue("Some unexpected methods are being forwarded " + CollectionUtils.subtract(forwarded, expected),
                expected.containsAll(forwarded));
    }

    @Test
    public void contextWithoutRoutingAssertionSettingShouldNotRelayToParent() throws MalformedURLException {
        Set<String> forwarded = new HashSet<>();
        PolicyEnforcementContext parent = buildParentExpectingForwards(forwarded);

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(parent);
        invokeAllRoutingMetricMethods(context);

        assertTrue("No methods should be getting forwarded", forwarded.isEmpty());
    }

    /**
     * Test {@link GatewayMetricsUtils#setPublisher(PolicyEnforcementContext, PolicyEnforcementContext)} method.
     * {@link ChildPolicyEnforcementContext} is only accessible within its package; therefore, the test is written here
     * @throws Exception
     */
    @Test
    public void testSetPublisherForChildPec() throws Exception {
        GatewayMetricsPublisher publisher = new GatewayMetricsPublisher();
        GatewayMetricsListener subscriber = new GatewayMetricsListener() {};
        publisher.addListener(subscriber);
        GatewayMetricsUtils.setPublisher(parent, publisher);

        GatewayMetricsUtils.setPublisher(parent,child);
        assertEquals(((GatewayMetricsSupport)parent).getGatewayMetricsEventsPublisher(), child.getGatewayMetricsEventsPublisher());
    }

    private PolicyEnforcementContext buildParentExpectingForwards(final Set<String> forwarded) {
        Answer answer = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                forwarded.add(invocationOnMock.getMethod().getName());
                return null;
            }
        };

        PolicyEnforcementContext parent = mock(PolicyEnforcementContext.class);
        when(parent.getRequestId()).thenReturn(RequestIdGenerator.next());

        when(parent.getEndTime()).thenAnswer(answer);
        when(parent.getRoutedServiceUrl()).then(answer);
        when(parent.getRoutingEndTime()).then(answer);
        when(parent.getRoutingStartTime()).then(answer);
        when(parent.getRoutingStatus()).thenAnswer(answer);
        when(parent.getRoutingTotalTime()).then(answer);
        when(parent.getStartTime()).thenAnswer(answer);
        when(parent.isPostRouting()).thenAnswer(answer);
        when(parent.isReplyExpected()).thenAnswer(answer);
        when(parent.isRequestWasCompressed()).then(answer);
        when(parent.isResponseWss11()).thenAnswer(answer);
        doAnswer(answer).when(parent).routingFinished();
        doAnswer(answer).when(parent).routingStarted();
        doAnswer(answer).when(parent).setEndTime();
        doAnswer(answer).when(parent).setRequestWasCompressed(anyBoolean());
        doAnswer(answer).when(parent).setResponseWss11(anyBoolean());
        doAnswer(answer).when(parent).setRoutedServiceUrl(any(URL.class));
        doAnswer(answer).when(parent).setRoutingStatus(any(RoutingStatus.class));

        return parent;
    }

    private void invokeAllRoutingMetricMethods(PolicyEnforcementContext context) throws MalformedURLException {
        context.getEndTime();
        context.getRoutedServiceUrl();
        context.getRoutingEndTime();
        context.getRoutingStartTime();
        context.getRoutingStatus();
        context.getRoutingTotalTime();
        context.getStartTime();
        context.isPostRouting();
        context.isReplyExpected();
        context.isRequestWasCompressed();
        context.isResponseWss11();
        context.routingFinished();
        context.routingStarted();
        context.setEndTime();
        context.setRequestWasCompressed(true);
        context.setResponseWss11(true);
        context.setRoutedServiceUrl(new URL("http", "ca.com", 80, "file"));
        context.setRoutingStatus(RoutingStatus.ATTEMPTED);
    }
}
