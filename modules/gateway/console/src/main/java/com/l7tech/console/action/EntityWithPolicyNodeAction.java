/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.Entity;

/**
 * Abstract class providing common logic for any policy, service or alias
 * @param <HT> An AbstractTreeNode
 */
public abstract class EntityWithPolicyNodeAction<HT extends EntityWithPolicyNode> extends NodeAction {
    protected final HT entityNode;

    protected EntityWithPolicyNodeAction(HT node) {
        super(node);
        this.entityNode = node;
    }

    protected abstract OperationType getOperation();

    @Override
    public final boolean isAuthorized() {
        if (!Registry.getDefault().isAdminContextPresent()) return false;
        EntityWithPolicyNode sn = getEntityWithPolicyNode();

        if (sn == null) return false;

        AttemptedOperation ao;
        Entity entity;
        try {
            entity = sn.getEntity();
        } catch (Exception e) {
            logger.warning("Couldn't resolve service");
            return false;
        }

        EntityType type = EntityType.findTypeByEntity( entity.getClass() );
        
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

    protected EntityWithPolicyNode getEntityWithPolicyNode() {
        EntityWithPolicyNode sn = entityNode;
        if (sn == null && node != null) sn = getEntityWithPolicyNodeCookie();
        if (sn == null) {
            PolicyTree tree = (PolicyTree) TopComponents.getInstance().getPolicyTree();
            PolicyEditorPanel pep = tree.getPolicyEditorPanel();
            if (pep != null)  sn = pep.getPolicyNode();
        }
        return sn;
    }
}
