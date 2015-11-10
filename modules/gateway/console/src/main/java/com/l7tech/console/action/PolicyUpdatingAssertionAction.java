/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.Assertion;

import java.util.Collection;

/**
 * @author alex
 */
public abstract class PolicyUpdatingAssertionAction extends NodeActionWithMetaSupport {
    protected AssertionTreeNode assertionTreeNode;

    public PolicyUpdatingAssertionAction(AssertionTreeNode node, Class assertionClass, final Assertion prototype) {
        super(node, assertionClass, prototype);
        this.assertionTreeNode = node;
    }

    public PolicyUpdatingAssertionAction(AssertionTreeNode node, Collection<Class> classes, final Assertion prototype) {
        super(node, classes, prototype);
        this.assertionTreeNode = node;
    }

    @Override
    public final boolean isAuthorized() {
        if (!Registry.getDefault().isAdminContextPresent()) return false;
        //noinspection SimplifiableIfStatement
        if (assertionTreeNode == null) return false;
        return assertionTreeNode.hasEditPermission();
    }
}
