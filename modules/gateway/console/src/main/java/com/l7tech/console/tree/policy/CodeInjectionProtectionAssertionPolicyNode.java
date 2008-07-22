/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.CodeInjectionProtectionAssertionPropertiesAction;
import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;

import javax.swing.*;

/**
 * A leaf node in a policy tree that represents a Code Injection Protection Assertion.
 */
public class CodeInjectionProtectionAssertionPolicyNode extends LeafAssertionTreeNode {
    public CodeInjectionProtectionAssertionPolicyNode(CodeInjectionProtectionAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        return "Code Injection Protection";
    }

    public Action getPreferredAction() {
        return new CodeInjectionProtectionAssertionPropertiesAction(this);
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/RedYellowShield16.gif";
    }
}
