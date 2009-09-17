package com.l7tech.util;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 */
@SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
public class MethodUtilTest {

    static class OverridesNothing {
    }

    static class OverridesEqualsButNotHashCode {
        @Override
        public boolean equals(Object obj) {
            return obj == getClass() || super.equals(obj);
        }
    }

    static class OverridesHashCodeButNotEquals {
        @Override
        public int hashCode() {
            return super.hashCode() + 33;
        }
    }

    static class OverridesBoth extends OverridesEqualsButNotHashCode {
        @Override
        public int hashCode() {
            return 4545 + super.hashCode();
        }
    }

    @Test
    public void testOverridesEquals() throws Exception {
        assertTrue(MethodUtil.isEqualsOrHashCodeOverridden(OverridesBoth.class));
        assertTrue(MethodUtil.isEqualsOrHashCodeOverridden(OverridesEqualsButNotHashCode.class));
        assertTrue(MethodUtil.isEqualsOrHashCodeOverridden(OverridesHashCodeButNotEquals.class));
        assertFalse(MethodUtil.isEqualsOrHashCodeOverridden(OverridesNothing.class));
    }
}
