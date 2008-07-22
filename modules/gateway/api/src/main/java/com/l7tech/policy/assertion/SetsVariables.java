/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.VariableMetadata;

/**
 * Indicates that this assertion is capable of setting one or more variables.
 */
public interface SetsVariables {
    VariableMetadata[] getVariablesSet();
}
