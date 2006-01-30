/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

/**
 * Indicates that this assertion can make use of variables.  {@link #getVariablesUsed()}
 * should return the list of variables that are actually known to be required by this
 * assertion in its current configuration.
 */
public interface UsesVariables {
    String[] getVariablesUsed();
}
