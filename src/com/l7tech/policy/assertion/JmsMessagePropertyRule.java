/**
 * Copyright (C) 2007 Layer Technologies Inc.
 */
package com.l7tech.policy.assertion;

import java.io.Serializable;

/**
 * A JMS message property rule defines how a JMS message property should be
 * propagated during message routing.
 *
 * <p>A JMS message property has a name and a value. The value is set as a
 * {@link String} and converted to any allowed type by the receiver. A JMS
 * message property rule specifies that either the value should pass through
 * unchanged (if exists) or be created using a custom pattern. The pattern can
 * contain context variable symbols.
 *
 * @since SecureSpan 4.0
 * @author rmak
 */
public class JmsMessagePropertyRule implements Serializable {
    private String _name;
    private boolean _passThru;
    private String _customPattern;

    public JmsMessagePropertyRule() {
    }

    public JmsMessagePropertyRule(final String name, final boolean passThru, final String customPattern) {
        _name = name;
        _passThru = passThru;
        _customPattern = customPattern;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public boolean isPassThru() {
        return _passThru;
    }

    public void setPassThru(boolean passThru) {
        _passThru = passThru;
    }

    /**
     * @return the custom pattern; may contain context variable symbols;
     *         can be <code>null</code> if pass-thru
     */
    public String getCustomPattern() {
        return _customPattern;
    }

    /**
     * Note: Remember to call {@link #setPassThru} with false as appropriate.
     *
     * @param customPattern     the custom pattern; can contain context variable symbols
     */
    public void setCustomPattern(String customPattern) {
        _customPattern = customPattern;
    }
}
