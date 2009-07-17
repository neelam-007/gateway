package com.l7tech.external.assertions.xacmlpdp.server;

import com.sun.xacml.finder.AttributeFinderModule;
import com.sun.xacml.attr.*;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.EvaluationCtx;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.policy.variable.NoSuchVariableException;

import java.util.HashSet;
import java.util.Set;
import java.net.URI;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 17-Mar-2009
 * Time: 9:00:10 PM
 */
class ContextVariableAttributeFinderModule extends AttributeFinderModule {
    public static final String PREFIX = "urn:layer7tech.com:contextVariable:";
    private PolicyEnforcementContext policyContext;

    public ContextVariableAttributeFinderModule(PolicyEnforcementContext policyContext) {
        this.policyContext = policyContext;
    }

    @Override
    public boolean isDesignatorSupported() {
        return true;
    }

    @Override
    public Set getSupportedDesignatorTypes() {
        HashSet<Integer> set = new HashSet<Integer>();
        set.add(AttributeDesignator.ENVIRONMENT_TARGET);
        return set;
    }

    @Override
    public EvaluationResult findAttribute(URI attributeType, URI attributeId,
                                          URI issuer, URI subjectCategory,
                                          EvaluationCtx context,
                                          int designatorType) {
        // we only know about environment attributes
        if (designatorType != AttributeDesignator.ENVIRONMENT_TARGET)
            return AttributeFinderModuleUtils.createEmptyEvaluationResult(attributeType);

        // figure out which attribute we're looking for
        String attrName = attributeId.toString();
        if(!attrName.startsWith(PREFIX)) {
            return AttributeFinderModuleUtils.createEmptyEvaluationResult(attributeType);
        } else {
            attrName = attrName.substring(PREFIX.length());
        }

        try {
            Object value = policyContext.getVariable(attrName);
            if(value != null) {
                if(value instanceof String) {
                    return new EvaluationResult(AttributeFinderModuleUtils.createSingleStringBag((String)value));
                } else if(value instanceof String[]) {
                    return new EvaluationResult(AttributeFinderModuleUtils.createMultipleStringBag((String[])value));
                } else if(value instanceof Integer) {
                    return new EvaluationResult(AttributeFinderModuleUtils.createSingleIntegerBag((Integer)value));
                } else if(value instanceof Boolean) {
                    return new EvaluationResult(AttributeFinderModuleUtils.createSingleBooleanBag((Boolean)value));
                } else {
                    return AttributeFinderModuleUtils.createEmptyEvaluationResult(attributeType);
                }
            } else {
                return AttributeFinderModuleUtils.createEmptyEvaluationResult(attributeType);
            }
        } catch(NoSuchVariableException nsve) {
            return AttributeFinderModuleUtils.createEmptyEvaluationResult(attributeType);
        }
    }
}
