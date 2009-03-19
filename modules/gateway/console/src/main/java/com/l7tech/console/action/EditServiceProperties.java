/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.panels.ServicePropertiesDialog;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.logging.Level;
import java.util.*;

/**
 * Action to edit the published service properties
 */
public class EditServiceProperties extends EntityWithPolicyNodeAction<ServiceNode> {
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
            svc = serviceNode.getEntity();
            hasUpdatePermission = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.SERVICE, svc));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get service", e);
            throw new RuntimeException(e);
        }

        final Frame mw = TopComponents.getInstance().getTopParent();
        final ServicePropertiesDialog dlg = new ServicePropertiesDialog(mw, svc, hasUpdatePermission);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasOKed()) {
                    serviceNode.clearCachedEntities();
                    serviceNode.reloadChildren();

                    final ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.nodeChanged(node);
                        model.reload(node); // WSDL may have changed

                        //if this is an original entity, update any aliases it may have, in case it's name, uri or
                        //something else show to the user in the tree changes
                        if(!(serviceNode instanceof ServiceNodeAlias)){
                            ServiceHeader sH = (ServiceHeader) serviceNode.getUserObject();
                            tree.updateAllAliases(sH.getOid());

                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    refreshTree(serviceNode, tree);
                                }
                            });
                        }
                    }

                    // update name on top of editor if that service is being edited
                    final WorkSpacePanel cws = TopComponents.getInstance().getCurrentWorkspace();
                    JComponent jc = cws.getComponent();
                    if (jc == null || !(jc instanceof PolicyEditorPanel)) return;

                    PolicyEditorPanel pe = (PolicyEditorPanel)jc;
                    try {
                        final EntityWithPolicyNode pn = pe.getPolicyNode();

                        if (pn instanceof ServiceNode) {
                            PublishedService editedSvc = ((ServiceNode) pn).getEntity();
                            // if currently edited service was deleted
                            if (serviceNode.getEntityOid() == editedSvc.getOid()) {
                                // update name on top of editor
                                pe.changeSubjectName(serviceNode.getName());
                                pe.updateHeadings();
                            }
                        }
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "problem modifying policy editor title");
                    }
                }
                serviceNode.clearCachedEntities();
            }
        });
    }

    private void sortChildren(AbstractTreeNode node){
        if(!(node instanceof FolderNode)){
            return;
        }

        java.util.List<AbstractTreeNode> childNodes = new ArrayList<AbstractTreeNode>();
        for(int i = 0; i < node.getChildCount(); i++){
            AbstractTreeNode childNode = (AbstractTreeNode)node.getChildAt(i);
            childNodes.add(childNode);
            if(childNode instanceof FolderNode){
                sortChildren(childNode);
            }
        }

        //Detach all children
        node.removeAllChildren();
        for(AbstractTreeNode atn: childNodes){
            node.insert(atn, node.getInsertPosition(atn, RootNode.getComparator()));
        }
    }

    private void refreshTree(EntityWithPolicyNode serviceNode, final JTree tree) {
        try {
            serviceNode.updateUserObject();

            TreePath rootPath = tree.getPathForRow(0);
            final TreePath selectedNodeTreePath = tree.getSelectionPath();
            final Enumeration pathEnum = tree.getExpandedDescendants(rootPath);

            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            RootNode rootNode = (RootNode) model.getRoot();
            sortChildren(rootNode);
            model.nodeStructureChanged(rootNode);

            while (pathEnum.hasMoreElements()) {
                Object pathObj = pathEnum.nextElement();
                TreePath tp = (TreePath) pathObj;
                tree.expandPath(tp);
            }
            tree.setSelectionPath(selectedNodeTreePath);

        } catch (FindException fe) {
            logger.info("Cannot the new service to update service node.");
        }
    }
}
