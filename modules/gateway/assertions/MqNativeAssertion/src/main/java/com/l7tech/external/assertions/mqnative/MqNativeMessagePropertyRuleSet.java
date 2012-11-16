package com.l7tech.external.assertions.mqnative;

import java.io.Serializable;

/**
 * Rules for propagating MQ message headers.
 */
public class MqNativeMessagePropertyRuleSet implements Serializable {
    private boolean passThroughMqMessageDescriptors;
    private boolean passThroughMqMessageHeaders;
    private boolean passThroughMqMessageProperties;

    public MqNativeMessagePropertyRuleSet() {
    }

    public MqNativeMessagePropertyRuleSet( MqNativeMessagePropertyRuleSet ruleSet ) {
        if ( ruleSet != null ) {
            this.passThroughMqMessageDescriptors = ruleSet.passThroughMqMessageDescriptors;
            this.passThroughMqMessageProperties = ruleSet.passThroughMqMessageProperties;
        }
    }

    public boolean isPassThroughMqMessageDescriptors() {
        return passThroughMqMessageDescriptors;
    }

    public void setPassThroughMqMessageDescriptors(boolean passThroughMqMessageDescriptors) {
        this.passThroughMqMessageDescriptors = passThroughMqMessageDescriptors;
    }

    public boolean isPassThroughMqMessageProperties() {
        return passThroughMqMessageProperties;
    }

    public void setPassThroughMqMessageProperties(boolean passThroughMqMessageProperties) {
        this.passThroughMqMessageProperties = passThroughMqMessageProperties;
    }

    public boolean isPassThroughMqMessageHeaders() {
        return passThroughMqMessageHeaders;
    }

    public void setPassThroughMqMessageHeaders(boolean passThroughMqMessageHeaders) {
        this.passThroughMqMessageHeaders = passThroughMqMessageHeaders;
    }
}
