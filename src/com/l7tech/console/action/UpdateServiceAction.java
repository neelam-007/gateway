/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.Registry;
import com.l7tech.service.PublishedService;

/**
 * @author alex
 */
public abstract class UpdateServiceAction extends NodeAction {
    public UpdateServiceAction(ServiceNode node) {
        super(node);
    }

    @Override
    public boolean isAuthorized() {
        if (Registry.getDefault().isAdminContextPresent()) return false;
        if (node == null) return false;
        try {
            PublishedService service = ((ServiceNode)node).getPublishedService();
            return canAttemptOperation(new AttemptedUpdate(EntityType.SERVICE, service));
        } catch (Exception e) {
            logger.warning("Couldn't resolve service");
            return false;
        }
    }
}
