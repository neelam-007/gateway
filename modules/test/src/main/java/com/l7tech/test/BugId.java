package com.l7tech.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to link a unit test to a Jira issue in a machine readable way.
 * Use it in preference to the old @BugNumber annotation which could only store Bugzilla numbers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface BugId {
    /**
     * @return The Jira issue identifier string for the bug, eg "SSG-233".
     */
    String value();
}
