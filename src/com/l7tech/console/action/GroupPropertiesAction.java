package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.EditorDialog;
import com.l7tech.console.panels.GroupPanel;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.GroupNode;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.tree.TreeNode;

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

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  // fla note. make sure this user is internal. otherwise dont show panel
                  if (!isParentIdProviderInternal((EntityHeaderNode)node)) {
                      JOptionPane.showMessageDialog(null, "This group is read-only.", "Read-only", JOptionPane.INFORMATION_MESSAGE);
                      return;
                  }
                  GroupPanel panel = new GroupPanel();
                  JFrame f = Registry.getDefault().getComponentRegistry().getMainWindow();
                  EditorDialog dialog = new EditorDialog(f, panel);

                  panel.edit(((EntityHeaderNode)node).getEntityHeader());
                  dialog.pack();
                  Utilities.centerOnScreen(dialog);
                  dialog.show();
              }
          });
    }

    private boolean isParentIdProviderInternal(EntityHeaderNode usernode) {
        TreeNode parentNode = usernode.getParent();
        while (parentNode != null) {
            if (parentNode instanceof EntityHeaderNode) {
                EntityHeader header = ((EntityHeaderNode)parentNode).getEntityHeader();
                if (header.getType().equals(EntityType.ID_PROVIDER_CONFIG)) {
                    // we found the parent, see if it's internal one
                    if (header.getOid() != IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID) return false;
                    return true;
                }
            }
            parentNode = parentNode.getParent();
        }
        // assume it is unless proven otherwise
        return true;
    }
}
