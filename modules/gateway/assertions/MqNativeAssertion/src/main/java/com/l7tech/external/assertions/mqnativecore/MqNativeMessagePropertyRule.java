/**
 * Copyright (C) 2007 Layer Technologies Inc.
 */
package com.l7tech.external.assertions.mqnativecore;

import java.io.Serializable;

/**
 * A MQ message property rule defines how a MQ message property should be
 * propagated during message routing.
 *
 * @author wlui
 */
public class MqNativeMessagePropertyRule implements Serializable {
    private String name;
    private String ruleType;
    private String customPattern;

    // Rule Types
    static public final String PASS_THRU = "PASS_THRU";
    static public final String CUSTOMIZE = "CUSTOMIZE";
    static public final String CONTEXT_VAR   = "CONTEXT_VAR";


    public MqNativeMessagePropertyRule() {
    }

    public MqNativeMessagePropertyRule(final String name, String  ruleType, final String customPattern) {
        this.name = name;
        this.ruleType = ruleType;
        this.customPattern = customPattern;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    /**
     * @return the custom pattern; may contain context variable symbols;
     *         can be <code>null</code> if pass-thru
     */
    public String getCustomPattern() {
        return customPattern;
    }

    /**
     * Note: Remember to set rule type
     *
     * @param customPattern     the custom pattern; can contain context variable symbols
     */
    public void setCustomPattern(String customPattern) {
        this.customPattern = customPattern;
    }


}
