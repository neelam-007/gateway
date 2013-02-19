package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionTranslator;
import com.l7tech.policy.assertion.PolicyAssertionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ChainedAssertionTranslatorTest {
    private ChainedAssertionTranslator translator;
    private List<AssertionTranslator> translators;
    private Assertion assertion;
    @Mock
    private AssertionTranslator t1;
    @Mock
    private AssertionTranslator t2;

    @Before
    public void setup() {
        translators = new ArrayList<AssertionTranslator>();
        translators.add(t1);
        translators.add(t2);
        translator = new ChainedAssertionTranslator(translators);
        assertion = new StubAssertion();
    }

    @Test
    public void translate() throws Exception {
        when(t1.translate(assertion)).thenReturn(assertion);
        when(t2.translate(assertion)).thenReturn(assertion);

        final Assertion translated = translator.translate(assertion);

        assertSame(assertion, translated);
        final InOrder inOrder = inOrder(t1, t2);
        inOrder.verify(t1).translate(assertion);
        inOrder.verify(t2).translate(assertion);
    }

    /**
     * If one of the chained translators returns an assertion that isn't the sourceAssertion,
     * it should be passed to subsequent translators.
     */
    @Test
    public void translatePassesTranslatedAssertionToSubsequentTranslators() throws Exception {
        final StubAssertion returnedAssertion = new StubAssertion();
        when(t1.translate(assertion)).thenReturn(returnedAssertion);
        when(t2.translate(returnedAssertion)).thenReturn(returnedAssertion);

        final Assertion translated = translator.translate(assertion);

        assertSame(returnedAssertion, translated);
        assertNotSame(assertion, translated);
        final InOrder inOrder = inOrder(t1, t2);
        inOrder.verify(t1).translate(assertion);
        inOrder.verify(t2).translate(returnedAssertion);
    }

    /**
     * If one of the chained translators returns null, null should be passed to subsequent translators.
     */
    @Test
    public void translatePassesNullToSubsequentTranslators() throws Exception {
        when(t1.translate(assertion)).thenReturn(null);
        when(t2.translate(null)).thenReturn(null);

        final Assertion translated = translator.translate(assertion);

        assertNull(translated);
        final InOrder inOrder = inOrder(t1, t2);
        inOrder.verify(t1).translate(assertion);
        inOrder.verify(t2).translate(null);
    }

    @Test(expected = PolicyAssertionException.class)
    public void translatePolicyAssertionException() throws Exception {
        when(t1.translate(assertion)).thenThrow(new PolicyAssertionException(assertion, "Mocking exception"));
        try {
            translator.translate(assertion);
            fail("Expected PolicyAssertionException.");
        } catch (final PolicyAssertionException e) {
            // expected
            verify(t1).translate(assertion);
            verify(t2, never()).translate(any(Assertion.class));
            throw e;
        }
    }

    private class StubAssertion extends Assertion {
    }
}
