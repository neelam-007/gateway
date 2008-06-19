package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.AttemptedCreate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.PublishInternalServiceWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.service.ServiceTemplate;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Set;
import java.util.logging.Level;

/**
 * User: megery
 */
public class PublishInternalServiceAction extends SecureAction{
    ServiceAdmin svcManager;
    Set<ServiceTemplate> templates;

    public PublishInternalServiceAction() {
        super(new AttemptedCreate(EntityType.SERVICE), UI_PUBLISH_SERVICE_WIZARD);
    }
    
    /**
     * @return the action name
     */
    public String getName() {
        return "Publish Internal Service";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Publish Internal Service";
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
        PublishInternalServiceWizard dialog = PublishInternalServiceWizard.getInstance(f);
        dialog.addEntityListener(listener);
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
                    final AbstractTreeNode sn = TreeNodeFactory.asTreeNode(eh);
                    model.insertNodeInto(sn, root, root.getInsertPosition(sn));

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
