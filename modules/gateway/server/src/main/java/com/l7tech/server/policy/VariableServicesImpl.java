package com.l7tech.server.policy;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.assertion.ext.VariableServices;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.util.Map;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 2/25/13
 */
public class VariableServicesImpl implements VariableServices{

    private final Audit audit;

    public VariableServicesImpl(Audit audit) {
        this.audit = audit;
    }

    @Override
    public String[] getReferencedVarNames(String varName) {
        return Syntax.getReferencedNames(varName);
    }

    @Override
    public Object expandVariable(String varName, Map varMap) {
        if(varName == null || varMap == null)
            return  null;

        return ExpandVariables.process(varName, varMap, audit);
    }
}
