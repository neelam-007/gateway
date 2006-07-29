package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.panels.PublishServiceWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>PublishServiceAction</code> action invokes the pubish
 * service wizard.                                             l
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PublishServiceAction extends SecureAction {
    static final Logger log = Logger.getLogger(PublishServiceAction.class.getName());

    public PublishServiceAction() {
        super(null);
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
        JFrame f = TopComponents.getInstance().getMainWindow();
        //PublishServiceWizard dialog = new PublishServiceWizard(f, false);
        PublishServiceWizard dialog = PublishServiceWizard.getInstance(f);
        dialog.addEntityListener(listener);
        //dialog.setResizable(false);
        dialog.pack();
        dialog.setSize(750, 500);
        Utilities.centerOnScreen(dialog);
        dialog.setVisible(true);
    }

    private EntityListener listener = new EntityListenerAdapter() {
        /**
         * Fired when an new entity is added.
         *
         * @param ev event describing the action
         */
        public void entityAdded(final EntityEvent ev) {
            EntityHeader eh = (EntityHeader)ev.getEntity();
            JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
            if (tree != null) {
                AbstractTreeNode root = (AbstractTreeNode)tree.getModel().getRoot();
                TreeNode[] nodes = root.getPath();
                TreePath nPath = new TreePath(nodes);
                if (tree.hasBeenExpanded(nPath)) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    final AbstractTreeNode sn = TreeNodeFactory.asTreeNode(eh);
                    model.insertNodeInto(sn, root, root.getInsertPosition(sn));

                    tree.setSelectionPath(new TreePath(sn.getPath()));
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            new EditServicePolicyAction((ServiceNode)sn).invoke();
                        }
                    });
                }
            } else {
                log.log(Level.WARNING, "Service tree unreachable.");
            }
        }
    };

}
