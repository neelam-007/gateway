package com.l7tech.console.action;

import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.util.WindowManager;
import com.l7tech.console.MainWindow;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The <code>DeleteEntityAction</code> action deletes the entity
 * such as .
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DeleteEntityAction extends BaseAction {
    static final Logger log = Logger.getLogger(NewUserAction.class.getName());
    EntityHeaderNode node;

    /**
     * create the acciton that deletes
     * @param en the node to delete
     */
    public DeleteEntityAction(EntityHeaderNode en) {
        node = en;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Delete";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Delete";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        boolean deleted = Actions.delete(node);
        if (deleted) {
            node.removeFromParent();
            // change to removeNodeFromParent(MutableTreeNode node)
        }
    }
}
