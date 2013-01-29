package com.l7tech.external.assertions.mqnative;

import java.io.Serializable;

/**
 * Rules for propagating MQ message headers.
 */
public class MqNativeMessagePropertyRuleSet implements Serializable {
    private boolean passThroughHeaders;

    public MqNativeMessagePropertyRuleSet() {
    }

    public MqNativeMessagePropertyRuleSet( MqNativeMessagePropertyRuleSet ruleSet ) {
        if ( ruleSet != null ) {
            this.passThroughHeaders = ruleSet.passThroughHeaders;
        }
    }

    public boolean isPassThroughHeaders() {
        return passThroughHeaders;
    }

    public void setPassThroughHeaders( final boolean passThroughHeaders ) {
        this.passThroughHeaders = passThroughHeaders;
    }
}
