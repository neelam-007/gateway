package com.l7tech.server.util;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 *
 */
public class AuditingThrowsAdviceTest {

    @Test
    public void testIsIncluded() {
        assertTrue( "Default include", AuditingThrowsAdvice.isIncluded( new Exception(), true, Collections.<Class>emptyList(), Collections.<Class>emptyList() ) );
        assertFalse( "Default exclude", AuditingThrowsAdvice.isIncluded( new Exception(), false, Collections.<Class>emptyList(), Collections.<Class>emptyList() ) );

        assertTrue( "Basic include 1", AuditingThrowsAdvice.isIncluded( new Exception(), true, Arrays.<Class>asList( Throwable.class ), Collections.<Class>emptyList() ) );
        assertTrue( "Basic include 2", AuditingThrowsAdvice.isIncluded( new Exception(), false, Arrays.<Class>asList( Throwable.class ), Collections.<Class>emptyList() ) );
        assertFalse( "Basic exclude 2", AuditingThrowsAdvice.isIncluded( new Exception(), true, Collections.<Class>emptyList(), Arrays.<Class>asList( Throwable.class ) ) );
        assertFalse( "Basic exclude 2", AuditingThrowsAdvice.isIncluded( new Exception(), false, Collections.<Class>emptyList(), Arrays.<Class>asList( Throwable.class ) ) );

        assertTrue( "Conflicting include/exclude", AuditingThrowsAdvice.isIncluded( new Exception(), true, Arrays.<Class>asList( Throwable.class ), Arrays.<Class>asList( Throwable.class ) ) );
        assertFalse( "Conflicting include/exclude 2", AuditingThrowsAdvice.isIncluded( new Exception(), false, Arrays.<Class>asList( Throwable.class ), Arrays.<Class>asList( Throwable.class ) ) );

        assertTrue( "Include overrides exclude 1", AuditingThrowsAdvice.isIncluded( new Exception(), true, Arrays.<Class>asList( Exception.class, Throwable.class ), Arrays.<Class>asList( Throwable.class ) ) );
        assertTrue( "Include overrides exclude 2", AuditingThrowsAdvice.isIncluded( new Exception(), false, Arrays.<Class>asList( Exception.class, Object.class ), Arrays.<Class>asList( Throwable.class ) ) );
        assertFalse( "Exclude overrides include 1", AuditingThrowsAdvice.isIncluded( new Exception(), true, Arrays.<Class>asList( Throwable.class ), Arrays.<Class>asList( Exception.class ) ) );
        assertFalse( "Exclude overrides include 1", AuditingThrowsAdvice.isIncluded( new Exception(), false, Arrays.<Class>asList( Throwable.class ), Arrays.<Class>asList( Exception.class ) ) );
    }

}
