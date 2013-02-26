package com.l7tech.policy.assertion.ext;

import java.util.Map;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 2/25/13
 */
public interface VariableServices {

    public String[] getReferencedVarNames(String varName);

    public Object expandVariable(String varName, Map varMap);
}
