package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.EditorDialog;
import com.l7tech.console.panels.GroupPanel;
import com.l7tech.console.tree.AssertionsTree;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.GroupNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.Group;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.identity.MemberOfGroup;

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
    GroupNode gn;
    public GroupPropertiesAction(GroupNode node) {
        super(node, MemberOfGroup.class);
        this.gn = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Group Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit Group Properties";
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
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (config == null) {
                    config = getIdentityProviderConfig((EntityHeaderNode)node);
                }
                final EntityHeader header = ((EntityHeaderNode)node).getEntityHeader();
                GroupPanel panel = GroupPanel.newInstance(config, header);
                JFrame f = TopComponents.getInstance().getMainWindow();
                EditorDialog dialog = new EditorDialog(f, panel, true);
                try {
                    panel.edit(header, config);
                    dialog.pack();
                    Utilities.centerOnScreen(dialog);
                    dialog.setVisible(true);
                    // bugzilla #1989
                    // catch changes here and update node
                    if (panel.wasOKed()) {
                        Group g = panel.getGroup();
                        EntityHeader nodeobject = (EntityHeader)gn.getUserObject();
                        nodeobject.setDescription(g.getDescription());
                        gn.firePropertyChange(nodeobject, null, nodeobject, nodeobject);
                        // invalidate parent dialog
                        firePropertyChange(null, null, null);
                    }
                } catch (NoSuchElementException e) {
                    // Bugzilla #801 - removing the group from the tree should not be performed 
                    // removeGroupTreeNode(header);
                }
            }
        });
    }

    public void setIdProviderConfig(IdentityProviderConfig config) {
        this.config = config;
    }


    private void removeGroupTreeNode(EntityHeader header) {
        JTree tree = (JTree)TopComponents.getInstance().getComponent(AssertionsTree.NAME);
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        model.removeNodeFromParent(node);
    }

    private IdentityProviderConfig config;
}
