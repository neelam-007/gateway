package com.l7tech.console.action;

import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;

/**
 * Supporting class for actions.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class Actions {

    /**
     * Deletes the given EntityTreeNode
     *
     * @param bn - the node to be deleted
     * @return true if deleted, false otherwise
     */
    static boolean delete(EntityHeaderNode bn) {
        boolean rb = false;
        if (bn instanceof UserNode) {
            return deleteUser((UserNode) bn);
        } else {
            // Unknown node type .. do nothing
            rb = false;
        }

        JOptionPane.showConfirmDialog(
          Registry.getDefault().getWindowManager().getMainWindow(),
          "Are you sure you wish to delete " + bn.getEntityHeader().getName() + "?",
          "Delete",
          JOptionPane.OK_OPTION);
        return false;

    }

    // Deletes the given Realm
    private static boolean deleteUser(UserNode node) {
        // Make sure
        if ((JOptionPane.showConfirmDialog(null,
          "Are you sure you wish to delete " +
          node.getName() + "?",
          "Delete",
          JOptionPane.YES_NO_OPTION)) == 1) {
            return false;
        }

        // Delete the  node and update the tree
        try {
            EntityHeader eh = node.getEntityHeader();
            User u = new User();
            u.setOid(eh.getOid());
            Registry.getDefault().getInternalUserManager().delete(u);
            return true;
        } catch (Exception e) {
            // Error deleting realm - display error msg
            JOptionPane.showMessageDialog(null,
              "Error encountered while deleting " +
              node.getName() +
              ". Please try again later.",
              "Delete realm",
              JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }
}
