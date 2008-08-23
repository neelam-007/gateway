/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.panels.ServicePropertiesDialog;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.Collection;
import java.util.logging.Level;

/**
 * Action to edit the published service properties
 */
public class EditServiceProperties extends ServiceNodeAction {
    public EditServiceProperties(ServiceNode node) {
        super(node);
    }

    protected OperationType getOperation() {
        return OperationType.READ;
    }

    public String getName() {
        return "Service Properties";
    }

    public String getDescription() {
        return "View/Edit the properties of the published service";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        final ServiceNode serviceNode = ((ServiceNode)node);
        boolean hasUpdatePermission;
        PublishedService svc;
        try {
            svc = serviceNode.getPublishedService();
            hasUpdatePermission = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.SERVICE, svc));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get service", e);
            throw new RuntimeException(e);
        }

        Functions.UnaryVoid<Boolean> callback = new Functions.UnaryVoid<Boolean>() {
            public void call(Boolean changed) {
                if (changed) {
                    serviceNode.clearCachedEntities();
                    serviceNode.reloadChildren();
                    ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.nodeChanged(node);
                        model.reload(node); // WSDL may have changed
                    }
                    //if this is an original entity, update any aliases it may have, in case it's name, uri or
                    //something else show to the user in the tree changes
                    if(!serviceNode.isAlias()){
                        if (tree !=null) {
                            ServiceHeader sH = (ServiceHeader) serviceNode.getUserObject();
                            tree.updateAllAliases(sH.getOid());
                        }
                    }
                    // update name on top of editor if that service is being edited
                    final WorkSpacePanel cws = TopComponents.getInstance().getCurrentWorkspace();
                    JComponent jc = cws.getComponent();
                    if (jc == null || !(jc instanceof PolicyEditorPanel)) {
                        return;
                    }
                    PolicyEditorPanel pe = (PolicyEditorPanel)jc;
                    try {
                        final EntityWithPolicyNode pn = pe.getPolicyNode();
                        if (pn instanceof ServiceNode) {
                            PublishedService editedSvc = ((ServiceNode) pn).getPublishedService();
                            // if currently edited service was deleted
                            if (serviceNode.getPublishedService().getOid() == editedSvc.getOid()) {
                                // update name on top of editor
                                pe.changeSubjectName(serviceNode.getName());
                                pe.updateHeadings();
                            }
                        }
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "problem modifying policy editor title");
                    }
                }
            }
        };
        editServiceProperties(svc, callback, hasUpdatePermission);
    }

    private void editServiceProperties(final PublishedService svc, final Functions.UnaryVoid<Boolean> resultCallback, boolean hasUpdatePermission) {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final ServicePropertiesDialog dlg = new ServicePropertiesDialog(mw, svc, hasUpdatePermission);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasOKed()) {
                    try {
                        Collection<ServiceDocument> documents = dlg.getServiceDocuments();
                        if (documents == null)
                            Registry.getDefault().getServiceManager().savePublishedService(svc);
                        else
                            Registry.getDefault().getServiceManager().savePublishedServiceWithDocuments(svc, documents);

                    } catch (DuplicateObjectException e) {
                        JOptionPane.showMessageDialog(mw,
                              "Unable to save the service '" + svc.getName() + "'\n" +
                              "because an existing service is already using the URI " + svc.getRoutingUri(),
                              "Service already exists",
                              JOptionPane.ERROR_MESSAGE);
                    } catch (Exception e) {
                        String msg = "Error while changing service properties";
                        logger.log(Level.INFO, msg, e);
                        String errorMessage = e.getMessage();
                        if (errorMessage != null) msg += ":\n" + errorMessage;
                        JOptionPane.showMessageDialog(mw, msg);
                    }
                    resultCallback.call(true);
                    return;
                }
                resultCallback.call(false);
            }
        });
    }
}
