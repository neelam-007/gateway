package com.l7tech.external.assertions.mqnative;

import java.io.Serializable;

/**
 * Rules for propagating MQ message headers.
 */
public class MqNativeMessagePropertyRuleSet implements Serializable {
    //Message Descriptor
    private boolean passThroughHeaders = true;
    //Message Additional Header
    private boolean passThroughMqMessageHeaders = true;
    //Message Properties
    private boolean passThroughMqMessageProperties = true;

    public MqNativeMessagePropertyRuleSet() {
    }

    public MqNativeMessagePropertyRuleSet( MqNativeMessagePropertyRuleSet ruleSet ) {
        if ( ruleSet != null ) {
            this.passThroughHeaders = ruleSet.passThroughHeaders;
            this.passThroughMqMessageProperties = ruleSet.passThroughMqMessageProperties;
        }
    }

    public boolean isPassThroughHeaders() {
        return passThroughHeaders;
    }

    public void setPassThroughHeaders(boolean passThroughHeaders) {
        this.passThroughHeaders = passThroughHeaders;
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
