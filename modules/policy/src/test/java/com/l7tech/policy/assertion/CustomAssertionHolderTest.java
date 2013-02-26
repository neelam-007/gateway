package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.ext.CustomAssertion;
import org.junit.Before;
import org.junit.Test;

import static com.l7tech.policy.assertion.AssertionMetadata.DESCRIPTION;
import static com.l7tech.policy.assertion.AssertionMetadata.SHORT_NAME;
import static com.l7tech.policy.assertion.CustomAssertionHolder.CUSTOM_ASSERTION;
import static org.junit.Assert.*;

public class CustomAssertionHolderTest {
    private static final String DESC = "My Custom Assertion Description";
    private static final String NAME = "My Custom Assertion";
    private CustomAssertionHolder holder;

    @Before
    public void setup() {
        holder = new CustomAssertionHolder();
        holder.setCustomAssertion(new StubCustomAssertion());
        holder.setDescriptionText(DESC);
    }

    @Test
    public void meta() {
        final AssertionMetadata meta = holder.meta();
        assertEquals(NAME, meta.get(SHORT_NAME));
        assertEquals(DESC, meta.get(DESCRIPTION));
    }

    @Test
    public void metaNullCustomAssertion() {
        holder.setCustomAssertion(null);
        final AssertionMetadata meta = holder.meta();
        assertEquals(CUSTOM_ASSERTION, meta.get(SHORT_NAME));
    }

    @Test
    public void metaNullDescription() {
        holder.setDescriptionText(null);
        final AssertionMetadata meta = holder.meta();
        assertEquals(CUSTOM_ASSERTION, meta.get(DESCRIPTION));
    }

    private class StubCustomAssertion implements CustomAssertion {
        @Override
        public String getName() {
            return NAME;
        }
    }
}
