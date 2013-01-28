package com.l7tech.test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * This annotation can be used to link a unit test to a bugzilla number in a machine-readable way.
 * <p/>
 * This should no longer be used for new tests.  Use {@link BugId} instead.
 * It is fine to include both annotations on test methods that refer to a bug that predates the Jira changeover.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface BugNumber {
    /**
	 * The bug number in our bug tracking system that this test case reproduces.
     * @return the bug number in bugzilla, or 0 if not known.
     */
	long value() default 0;
}
