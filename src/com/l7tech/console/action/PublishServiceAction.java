package com.l7tech.console.action;

import com.l7tech.console.panels.PublishServiceWizard;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.WindowManager;
import com.l7tech.console.MainWindow;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>PublishServiceAction</code> action invokes the pubish
 * service wizard.                                             l
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class PublishServiceAction extends NodeAction {
    static final Logger log = Logger.getLogger(PublishServiceAction.class.getName());

    public PublishServiceAction(AbstractTreeNode node) {
        super(node);
        this.node = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Publish Service";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Punblish a service specifying access control";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/New16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                PublishServiceWizard dialog = new PublishServiceWizard(null, true);
                dialog.addEntityListener(listener);
                dialog.setResizable(false);
                dialog.show();
            }
        });
    }

    private EntityListener listener = new EntityListenerAdapter() {
        /**
         * Fired when an new entity is added.
         * @param ev event describing the action
         */
        public void entityAdded(final EntityEvent ev) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    EntityHeader eh = (EntityHeader)ev.getEntity();
                    JTree tree = (JTree)WindowManager.getInstance().getComponent(MainWindow.SERVICES_TREE);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.insertNodeInto(TreeNodeFactory.asTreeNode(eh), node, node.getChildCount());
                    } else {
                        log.log(Level.WARNING, "Service tree unreachable.");
                    }
                }
            });
        }
    };
}
