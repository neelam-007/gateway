/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.security.rbac.*;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.Entity;

/**
 * @author alex
 */
public abstract class PolicyNodeAction extends NodeAction {
    protected final PolicyEntityNode policyNode;
    protected final EntityType type;

    protected PolicyNodeAction(PolicyEntityNode node) {
        super(node);
        this.policyNode = node;
        // TODO support other types of entities
        type = node instanceof ServiceNode ? EntityType.SERVICE : EntityType.POLICY;
    }

    protected abstract OperationType getOperation();

    @Override
    public final boolean isAuthorized() {
        if (!Registry.getDefault().isAdminContextPresent()) return false;
        PolicyEntityNode pn = getPolicyNode();

        if (pn == null) return false;

        AttemptedOperation ao;
        Entity entity;
        try {
            entity = pn.getEntity();
        } catch (Exception e) {
            logger.warning("Couldn't resolve policy");
            return false;
        }
        switch(getOperation()) {
            case CREATE:
                ao = new AttemptedCreate(type);
                break;
            case DELETE:
                ao = new AttemptedDeleteSpecific(type, entity);
                break;
            case READ:
                ao = new AttemptedReadSpecific(type, entity.getId());
                break;
            case UPDATE:
                ao = new AttemptedUpdate(type, entity);
                break;
            default:
                throw new IllegalStateException("Unsupported operation: " + getOperation());
        }
        return canAttemptOperation(ao);
    }

    protected PolicyEntityNode getPolicyNode() {
        PolicyEntityNode pn = policyNode;
        if (pn == null && node != null) pn = getPolicyNodeCookie();
        if (pn == null) {
            PolicyTree tree = (PolicyTree) TopComponents.getInstance().getPolicyTree();
            PolicyEditorPanel pep = tree.getPolicyEditorPanel();
            if (pep != null) pn = pep.getPolicyNode();
        }
        return pn;
    }
}
