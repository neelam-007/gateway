/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.PublishNonSoapServiceWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.logging.Level;

/**
 * SSM action to publish a non-soap xml service.
 */
public class PublishNonSoapServiceAction extends SecureAction {
    public PublishNonSoapServiceAction() {
        super(new AttemptedCreate(EntityType.SERVICE), UI_PUBLISH_XML_WIZARD);
    }

    public String getName() {
        return "Publish XML Application";
    }

    public String getDescription() {
        return "Publish a non-soap XML application";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/xmlObject16.gif";
    }

    protected void performAction() {
        final Frame mw = TopComponents.getInstance().getTopParent();
        PublishNonSoapServiceWizard wiz = PublishNonSoapServiceWizard.getInstance(mw);
        wiz.pack();
        Utilities.centerOnScreen(wiz);
        wiz.addEntityListener(listener);
        wiz.setModal(true);
        DialogDisplayer.display(wiz);
    }

    private EntityListener listener = new EntityListenerAdapter() {
        /**
         * Fired when an new entity is added.
         *
         * @param ev event describing the action
         */
        public void entityAdded(final EntityEvent ev) {
            EntityHeader eh = (EntityHeader)ev.getEntity();
            final JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            if (tree != null) {
                AbstractTreeNode root = TopComponents.getInstance().getServicesFolderNode();
                TreeNode[] nodes = root.getPath();
                TreePath nPath = new TreePath(nodes);
                //Remove any filter before insert
                TopComponents.getInstance().clearFilter();

                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                final AbstractTreeNode sn = TreeNodeFactory.asTreeNode(eh, null);
                model.insertNodeInto(sn, root, root.getInsertPosition(sn, RootNode.getComparator()));
                RootNode rootNode = (RootNode) model.getRoot();
                rootNode.addEntity(eh.getOid(), sn);
                tree.setSelectionPath(new TreePath(sn.getPath()));
                
                tree.setSelectionPath(new TreePath(sn.getPath()));
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        new EditPolicyAction((ServiceNode)sn).invoke();

                        //reset filter to ALL
                        ((ServicesAndPoliciesTree) tree).filterTreeToDefault();
                    }
                });
            } else {
                log.log(Level.WARNING, "Service tree unreachable.");
            }
        }
    };
}
