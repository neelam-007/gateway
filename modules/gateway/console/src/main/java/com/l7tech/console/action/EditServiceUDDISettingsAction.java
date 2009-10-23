/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.ServiceUDDISettingsDialog;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import java.util.logging.Level;
import java.awt.*;

public class EditServiceUDDISettingsAction extends NodeAction{

    public EditServiceUDDISettingsAction(AbstractTreeNode node) {
        super(node);
    }

    @Override
    public String getName() {
        return "Service UDDI Settings";
    }

    @Override
    public String getDescription() {
        return "View/Edit the UDDI settings of the published service";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    @Override
    protected void performAction() {
        final ServiceNode serviceNode = ((ServiceNode)node);
        boolean hasUpdatePermission;
        PublishedService svc;
        try {
            svc = serviceNode.getEntity();
            hasUpdatePermission = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.UDDI_REGISTRY, svc));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get service", e);
            throw new RuntimeException(e);
        }

        //ServiceUDDISettingsDialog
        final Frame mw = TopComponents.getInstance().getTopParent();
        final ServiceUDDISettingsDialog dlg = new ServiceUDDISettingsDialog(mw, svc, hasUpdatePermission);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);

    }
}
