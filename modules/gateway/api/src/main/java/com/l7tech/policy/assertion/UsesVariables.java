/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

/**
 * Indicates that this assertion can make use of variables.  {@link #getVariablesUsed()}
 * should return the list of variables that are actually known to be required by this
 * assertion in its current configuration.
 *
 * Warning. This is a hard contract: if your assertion does not implement this interface and
 * tries to access a variable at runtime, it may not be able to access it.
 */
public interface UsesVariables {
    /**
     * @return an array of variable names used by the assertion at runtime. these variable names are not
     * boxed in ${name} pattern (must return variable names only)
     */
    String[] getVariablesUsed();
}
