package com.l7tech.console.action;

import com.l7tech.console.panels.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.GroupNode;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;

/**
 * The <code>GroupPropertiesAction</code> edits the group entity.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class GroupPropertiesAction extends NodeAction {

    public GroupPropertiesAction(GroupNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Group properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/edit group properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
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
                GroupPanel panel = new GroupPanel();
                EditorDialog dialog = new EditorDialog(null, panel);

                panel.edit(((EntityHeaderNode)node).getEntityHeader());
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                dialog.show();
            }
        });
    }
}
