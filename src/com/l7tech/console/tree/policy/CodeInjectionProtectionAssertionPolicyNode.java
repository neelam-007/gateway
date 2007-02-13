/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;
import com.l7tech.console.action.CodeInjectionProtectionAssertionPropertiesAction;

import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A leaf node in a policy tree that represents a Code Injection Protection Assertion.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class CodeInjectionProtectionAssertionPolicyNode extends LeafAssertionTreeNode {
    public CodeInjectionProtectionAssertionPolicyNode(CodeInjectionProtectionAssertion assertion) {
        super(assertion);
    }

    public boolean canDelete() {
        return true;
    }

    public String getName() {
        return "Code Injection Protection";
    }

    public Action[] getActions() {
        final List<Action> list = new ArrayList<Action>();
        list.add(new CodeInjectionProtectionAssertionPropertiesAction(this));
        list.addAll(Arrays.asList(super.getActions()));
        return list.toArray(new Action[]{});
    }

    public Action getPreferredAction() {
        return new CodeInjectionProtectionAssertionPropertiesAction(this);
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/RedYellowShield16.gif";
    }

    protected void loadChildren() {
    }
}
