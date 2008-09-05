/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.PublishedService;

public abstract class ServiceNodeAction extends NodeAction {
    protected final ServiceNode serviceNode;

    protected ServiceNodeAction(ServiceNode node) {
        super(node);
        this.serviceNode = node;
    }

    protected abstract OperationType getOperation();

    @Override
    public final boolean isAuthorized() {
        if (!Registry.getDefault().isAdminContextPresent()) return false;
        ServiceNode sn = getServiceNode();

        if (sn == null) return false;

        AttemptedOperation ao;
        PublishedService service;
        try {
            service = sn.getEntity();
        } catch (Exception e) {
            logger.warning("Couldn't resolve service");
            return false;
        }
        switch(getOperation()) {
            case CREATE:
                ao = new AttemptedCreate(EntityType.SERVICE);
                break;
            case DELETE:
                ao = new AttemptedDeleteSpecific(EntityType.SERVICE, service);
                break;
            case READ:
                ao = new AttemptedReadSpecific(EntityType.SERVICE, service.getId());
                break;
            case UPDATE:
                ao = new AttemptedUpdate(EntityType.SERVICE, service);
                break;
            default:
                throw new IllegalStateException("Unsupported operation: " + getOperation());
        }
        return canAttemptOperation(ao);
    }

    protected ServiceNode getServiceNode() {
        ServiceNode sn = serviceNode;
        if (sn == null && node != null) sn = getServiceNodeCookie();
        if (sn == null) {
            PolicyTree tree = (PolicyTree) TopComponents.getInstance().getPolicyTree();
            PolicyEditorPanel pep = tree.getPolicyEditorPanel();
            if (pep != null) {
                EntityWithPolicyNode pn = pep.getPolicyNode();
                if (pn instanceof ServiceNode) {
                    sn = (ServiceNode) pn;
                }
            }
        }
        return sn;
    }
}
