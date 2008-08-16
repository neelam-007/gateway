/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.PublishServiceWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>PublishServiceAction</code> action invokes the pubish
 * service wizard.                                             l
 */
public class PublishServiceAction extends SecureAction {
    static final Logger log = Logger.getLogger(PublishServiceAction.class.getName());

    public PublishServiceAction() {
        super(new AttemptedCreate(EntityType.SERVICE), UI_PUBLISH_SERVICE_WIZARD);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Publish SOAP Web Service";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Publish a SOAP Web service";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/services16.png";
    }

    /**
     */
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        //PublishServiceWizard dialog = new PublishServiceWizard(f, false);
        PublishServiceWizard dialog = PublishServiceWizard.getInstance(f);
        dialog.addEntityListener(listener);
        //dialog.setResizable(false);
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        DialogDisplayer.display(dialog);
    }

    private EntityListener listener = new EntityListenerAdapter() {
        /**
         * Fired when an new entity is added.
         *
         * @param ev event describing the action
         */
        public void entityAdded(final EntityEvent ev) {
            EntityHeader eh = (EntityHeader)ev.getEntity();
            JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            if (tree != null) {
                AbstractTreeNode root = TopComponents.getInstance().getServicesFolderNode();
                TreeNode[] nodes = root.getPath();
                TreePath nPath = new TreePath(nodes);
                if (tree.hasBeenExpanded(nPath)) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    final AbstractTreeNode sn = TreeNodeFactory.asTreeNode(eh, null);
                    model.insertNodeInto(sn, root, root.getInsertPosition(sn, RootNode.getComparator()));

                    tree.setSelectionPath(new TreePath(sn.getPath()));
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            new EditPolicyAction((ServiceNode)sn).invoke();
                        }
                    });
                }
            } else {
                log.log(Level.WARNING, "Service tree unreachable.");
            }
        }
    };

}
