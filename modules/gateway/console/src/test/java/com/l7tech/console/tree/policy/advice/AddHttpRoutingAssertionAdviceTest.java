package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AddHttpRoutingAssertionAdviceTest {
    private AddHttpRoutingAssertionAdvice advice;
    @Mock
    private PolicyChange change;

    @Before
    public void setup() {
        advice = new AddHttpRoutingAssertionAdvice();
    }

    @Test
    public void proceed() {
        final HttpRoutingAssertion route = new HttpRoutingAssertion();
        final PolicyEvent event = new PolicyEvent("source", null, null, new Assertion[]{route});
        when(change.getEvent()).thenReturn(event);

        advice.proceed(change);
        assertTrue(route.getRequestHeaderRules().isForwardAll());
        assertTrue(route.getResponseHeaderRules().isForwardAll());
        verify(change).proceed();
    }

    @Test
    public void proceedNullChange() {
        advice.proceed(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void proceedTooManyAssertions() {
        final HttpRoutingAssertion child1 = new HttpRoutingAssertion();
        final HttpRoutingAssertion child2 = new HttpRoutingAssertion();
        final PolicyEvent event = new PolicyEvent("source", null, null, new Assertion[]{child1, child2});
        when(change.getEvent()).thenReturn(event);
        try {
            advice.proceed(change);
            fail("Expected IllegalArgumentException due to too many assertions");
        } catch (final IllegalArgumentException e) {
            assertFalse(child1.getRequestHeaderRules().isForwardAll());
            assertFalse(child1.getResponseHeaderRules().isForwardAll());
            assertFalse(child2.getRequestHeaderRules().isForwardAll());
            assertFalse(child2.getResponseHeaderRules().isForwardAll());
            verify(change, never()).proceed();
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void proceedTooFewAssertions() {
        final PolicyEvent event = new PolicyEvent("source", null, null, new Assertion[]{});
        when(change.getEvent()).thenReturn(event);
        try {
            advice.proceed(change);
            fail("Expected IllegalArgumentException due to too few assertions");
        } catch (final IllegalArgumentException e) {
            verify(change, never()).proceed();
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void proceedNotHttpRouteAssertion() {
        final PolicyEvent event = new PolicyEvent("source", null, null, new Assertion[]{new AllAssertion()});
        when(change.getEvent()).thenReturn(event);
        try {
            advice.proceed(change);
            fail("Expected IllegalArgumentException due to invalid assertion type");
        } catch (final IllegalArgumentException e) {
            verify(change, never()).proceed();
            throw e;
        }
    }
}
