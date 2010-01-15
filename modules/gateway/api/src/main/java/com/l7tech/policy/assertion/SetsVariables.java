/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.VariableMetadata;

/**
 * Indicates that this assertion is capable of setting one or more variables.
 */
public interface SetsVariables {
    /**
     * Get a description of the variables this assertion sets.  The general expectation is that these
     * variables will exist and be assigned values after the server assertion's checkRequest method
     * has returned.
     *
     * @return an array of VariableMetadata instances.  May be empty, but should never be null.
     * @throws com.l7tech.policy.variable.VariableNameSyntaxException (unchecked) if one of the variable names
     *         currently configured on this object does not use the correct syntax.
     */
    VariableMetadata[] getVariablesSet();
}
