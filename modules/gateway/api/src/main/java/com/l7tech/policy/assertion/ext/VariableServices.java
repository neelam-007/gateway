package com.l7tech.policy.assertion.ext;

import java.util.Map;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 2/25/13
 */
public interface VariableServices {

    /**
     * Use {@link com.l7tech.policy.variable.ContextVariablesUtils#getReferencedNames(String)} instead.
     */
    @Deprecated
    public String[] getReferencedVarNames(String varName);

    /**
     * Use {@link com.l7tech.policy.assertion.ext.message.CustomPolicyContext#expandVariable(String, java.util.Map)} instead.
     */
    @Deprecated
    public Object expandVariable(String varName, Map varMap);
}
