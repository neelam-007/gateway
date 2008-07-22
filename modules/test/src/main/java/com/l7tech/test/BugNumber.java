package com.l7tech.test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * This annotation can be used to link a unit test to a bugzilla number in a machine-readable way.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface BugNumber {
    public static final String DEFAULT_BUGZILLA = "http://layer7tech.com/id/defaultBugzilla";

    /**
	 * The bug number in our bug tracking system that this test case reproduces.
     * @return the bug number in bugzilla, or 0 if not known.
     */
	long value() default 0;

    /**
     * Provides a URI which uniquely identifies an instance of bugzilla in which the {@link #value} is meaningful.
     *
     * @return a URI that uniquely identifies a bugzilla instances in which @{link #value} is meaningful,
     *         or {@link #DEFAULT_BUGZILLA} to assume the local default bugzilla instance.
     */
    String bugzillaInstanceUri() default DEFAULT_BUGZILLA;
}
