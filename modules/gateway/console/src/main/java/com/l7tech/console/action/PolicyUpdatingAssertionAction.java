/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.Policy;
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
        if (assertionTreeNode == null) return false;
        try {
            // Case 1: if the node is associated to a published service
            PublishedService svc = assertionTreeNode.getService();
            boolean authorized = canAttemptOperation(new AttemptedUpdate(EntityType.SERVICE, svc));

            // Case 2: if the node is associated to a policy fragment
            if (svc == null && !authorized) {
                Policy policy = assertionTreeNode.getPolicy();
                authorized = canAttemptOperation(new AttemptedUpdate(EntityType.POLICY, policy));
            }
            return authorized;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get current service or policy", e);
        }
    }
}
