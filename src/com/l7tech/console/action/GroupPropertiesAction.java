package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.EditorDialog;
import com.l7tech.console.panels.GroupPanel;
import com.l7tech.console.tree.AssertionsTree;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.GroupNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.NoSuchElementException;

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
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (idProvider == null) {
                    idProvider = getIdentityProvider((EntityHeaderNode)node);
                }
                GroupPanel panel = new GroupPanel();
                JFrame f = Registry.getDefault().getComponentRegistry().getMainWindow();
                final EntityHeader header = ((EntityHeaderNode)node).getEntityHeader();
                EditorDialog dialog = new EditorDialog(f, panel);
                try {
                    panel.edit(header, idProvider);
                    dialog.pack();
                    Utilities.centerOnScreen(dialog);
                    dialog.show();
                } catch (NoSuchElementException e) {
                    removeGroupTreeNode(header);
                }
            }
        });
    }

    public void setIdProvider(IdentityProvider idProvider) {
        this.idProvider = idProvider;
    }

    private void removeGroupTreeNode(EntityHeader header) {
        JTree tree = (JTree)TopComponents.getInstance().getComponent(AssertionsTree.NAME);
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        model.removeNodeFromParent(node);
    }

    private IdentityProvider idProvider;
}
