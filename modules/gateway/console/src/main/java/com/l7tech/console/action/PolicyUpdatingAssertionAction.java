/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.Policy;

import java.util.Collection;

/**
 * @author alex
 */
public abstract class PolicyUpdatingAssertionAction extends SecureAction {
    protected AssertionTreeNode assertionTreeNode;

    public PolicyUpdatingAssertionAction(AttemptedOperation attemptedOperation, AssertionTreeNode node) {
        super(attemptedOperation);
        this.assertionTreeNode = node;
    }

    public PolicyUpdatingAssertionAction(AssertionTreeNode node) {
        super(null);
        this.assertionTreeNode = node;
    }

    public PolicyUpdatingAssertionAction(AssertionTreeNode node, Class assertionClass) {
        super(null, assertionClass);
        this.assertionTreeNode = node;
    }

    public PolicyUpdatingAssertionAction(AssertionTreeNode node, Collection<Class> classes) {
        super(null, classes);
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
