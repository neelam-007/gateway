package com.l7tech.external.assertions.mqnativecore;

/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */

import java.io.Serializable;

/**
 * Set of rules for propagating MQ message properties.
 *
 * @author wlui
 */
public class MqNativeMessagePropertyRuleSet implements Serializable, Cloneable {
    private String ruleType;
    private MqNativeMessagePropertyRule[] _rules;

    public MqNativeMessagePropertyRuleSet() {
        ruleType = MqNativeMessagePropertyRule.PASS_THRU;
        _rules = new MqNativeMessagePropertyRule[0];
    }

    /**
     * @throws IllegalArgumentException if <code>rules</code> is <code>null</code>.
     */
    public MqNativeMessagePropertyRuleSet(final String ruleType, final MqNativeMessagePropertyRule[] rules) {
        if (rules == null) throw new IllegalArgumentException("Rules array must not be null.");
        this.ruleType = ruleType;
        _rules = rules;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    /**
     * @return individual rules to be used when ruleType!=PASS_THRU
     */
    public MqNativeMessagePropertyRule[] getRules() {
        return _rules;
    }

    /**
     * @throws IllegalArgumentException if <code>rules</code> is <code>null</code>.
     */
    public void setRules(final MqNativeMessagePropertyRule[] rules) {
        if (rules == null) throw new IllegalArgumentException("Rules array must not be null.");
        _rules = rules;
    }

    public String[] getVariablesUsed() {
        if(MqNativeMessagePropertyRule.CONTEXT_VAR.equals(ruleType)){
            return new String[]{_rules[0].getName()};
        }
        return new String[0];
    }
}
