package com.l7tech.console.action;

import com.l7tech.console.panels.NewGroupDialog;
import com.l7tech.console.tree.*;
import com.l7tech.console.util.WindowManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.MainWindow;
import com.l7tech.console.event.EntityListenerAdapter;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>NewGroupAction</code> action adds the new group.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class NewGroupAction extends NodeAction {
    static final Logger log = Logger.getLogger(NewGroupAction.class.getName());

    public NewGroupAction(AbstractTreeNode node) {
        super(node);
        this.node = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create new group";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create new group";
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
        SwingUtilities.invokeLater(
          new Runnable() {
            public void run() {
                JFrame f = Registry.getDefault().getWindowManager().getMainWindow();
                NewGroupDialog dialog = new NewGroupDialog(f);
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
                    JTree tree = (JTree)WindowManager.getInstance().getComponent(AssertionsTree.NAME);
                    if (tree != null) {
                        TreeNode[] nodes = node.getPath();
                        TreePath nPath = new TreePath(nodes);
                        if (tree.hasBeenExpanded(nPath)) {
                            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                            model.insertNodeInto(TreeNodeFactory.asTreeNode(eh), node, node.getChildCount());
                        }
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            });
        }
    };
}
