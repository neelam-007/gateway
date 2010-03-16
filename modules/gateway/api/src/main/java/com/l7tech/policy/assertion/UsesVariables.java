/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

/**
 * Indicates that this assertion can make use of variables.  {@link #getVariablesUsed()}
 * should return the list of variables that are actually known to be required by this
 * assertion in its current configuration.
 * <p/>
 * If your assertions modifies a variable in-place, it should declare it in both UsesVariables and {@link SetsVariables}.
 * <p/>
 * Warning. This is a hard contract: if your assertion does not implement this interface and
 * tries to access a variable at runtime, it may not be able to access it.
 */
public interface UsesVariables {
    /**
     * Get a list of all context variables this assertion declares that it uses.
     * <p/>
     * If an assertion requires a variable to already exist, but modifies it in-place, it should delcare it
     * in both SetsVariables and {@link UsesVariables}.

     * @return an array of variable names used by the assertion at runtime. these variable names are not
     * boxed in ${name} pattern (must return variable names only)
     */
    String[] getVariablesUsed();
}
