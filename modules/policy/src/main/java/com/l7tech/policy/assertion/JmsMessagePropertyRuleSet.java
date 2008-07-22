/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import java.io.Serializable;

/**
 * Set of rules for propagating JMS message properties.
 *
 * @since SecureSpan 4.0
 * @author rmak
 */
public class JmsMessagePropertyRuleSet implements Serializable {
    private boolean _passThruAll;
    private JmsMessagePropertyRule[] _rules;

    public JmsMessagePropertyRuleSet() {
        _passThruAll = true;
        _rules = new JmsMessagePropertyRule[0];
    }

    /**
     * @throws IllegalArgumentException if <code>rules</code> is <code>null</code>.
     */
    public JmsMessagePropertyRuleSet(final boolean passThruAll, final JmsMessagePropertyRule[] rules) {
        if (rules == null) throw new IllegalArgumentException("Rules array must not be null.");
        _passThruAll = passThruAll;
        _rules = rules;
    }

    public boolean isPassThruAll() {
        return _passThruAll;
    }

    public void setPassThruAll(boolean passThruAll) {
        _passThruAll = passThruAll;
    }

    /**
     * @return individual rules to be used when {@link #isPassThruAll} returns false
     */
    public JmsMessagePropertyRule[] getRules() {
        return _rules;
    }

    /**
     * @throws IllegalArgumentException if <code>rules</code> is <code>null</code>.
     */
    public void setRules(final JmsMessagePropertyRule[] rules) {
        if (rules == null) throw new IllegalArgumentException("Rules array must not be null.");
        _rules = rules;
    }
}
