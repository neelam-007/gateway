/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.tree.AbstractLeafPaletteNode;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;

/**
 * A leaf node in the assertions palette tree that represents the Code Injection Protection Assertion.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class CodeInjectionProtectionAssertionPaletteNode extends AbstractLeafPaletteNode {
    public CodeInjectionProtectionAssertionPaletteNode() {
        super("Code Injection Protection", "com/l7tech/console/resources/RedYellowShield16.gif");
    }

    /**
     * Return a new instance of the underlying assertion.
     */
    public Assertion asAssertion() {
        return new CodeInjectionProtectionAssertion();
    }
}
