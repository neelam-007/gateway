package com.l7tech.console.action;

import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ProviderNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Supporting class for actions.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Actions {
    static Logger log = Logger.getLogger(Actions.class.getName());

    /**
     * Deletes the given EntityTreeNode
     *
     * @param bn - the node to be deleted
     * @return true if deleted, false otherwise
     */
    static boolean deleteEntity(EntityHeaderNode bn) {
        boolean rb = false;
        if (bn instanceof UserNode) {
            return deleteUser((UserNode)bn);
        } else if (bn instanceof ProviderNode) {
            return deleteProvider((ProviderNode)bn);
        } else {
            // Unknown node type .. do nothing
            rb = false;
        }
        JOptionPane.showConfirmDialog(
          getMainWindow(),
          "Are you sure you wish to deleteEntity " + bn.getEntityHeader().getName() + "?",
          "Delete",
          JOptionPane.OK_OPTION);
        return false;
    }

    // Deletes the given user
    private static boolean deleteUser(UserNode node) {
        // Make sure
        if ((JOptionPane.showConfirmDialog(
          getMainWindow(),
          "Are you sure you wish to delete " +
          node.getName() + "?",
          "Delete User",
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
             log.log(Level.SEVERE, "Error deleting user", e);
            // Error deleting realm - display error msg
            JOptionPane.showMessageDialog(
              getMainWindow(),
              "Error encountered while deleting " +
              node.getName() +
              ". Please try again later.",
              "Delete User",
              JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    // Deletes the given user
      private static boolean deleteProvider(ProviderNode node) {
          // Make sure
          if ((JOptionPane.showConfirmDialog(
            getMainWindow(),
            "Are you sure you wish to delete " +
            node.getName() + "?",
            "Delete Provider",
            JOptionPane.YES_NO_OPTION)) == 1) {
              return false;
          }

          // Delete the  node and update the tree
          try {
              EntityHeader eh = node.getEntityHeader();
              IdentityProviderConfig ic = new IdentityProviderConfig();
              ic.setOid(eh.getOid());
              Registry.getDefault().getProviderConfigManager().delete(ic);
              return true;
          } catch (Exception e) {
               log.log(Level.SEVERE, "Error deleting provider", e);
              // Error deleting realm - display error msg
              JOptionPane.showMessageDialog(
                getMainWindow(),
                "Error encountered while deleting " +
                node.getName() +
                ". Please try again later.",
                "Delete Provider",
                JOptionPane.ERROR_MESSAGE);
          }
          return false;
      }



    // Deletes the given saervice
    static boolean deleteService(ServiceNode node) {
        // Make sure
        if ((JOptionPane.showConfirmDialog(
          getMainWindow(),
          "Are you sure you wish to delete service " +
          node.getName() + "?",
          "Delete Service",
          JOptionPane.YES_NO_OPTION)) == 1) {
            return false;
        }

        // Delete the  node and update the tree
        try {
            Registry.getDefault().getServiceManager().delete(node.getPublishedService());
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error deleting service", e);
            JOptionPane.showMessageDialog(
              getMainWindow(),
              "Error encountered while deleting " +
              node.getName() +
              ". Please try again later.",
              "Delete Service",
              JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }


    private static JFrame getMainWindow() {
        return Registry.getDefault().getWindowManager().getMainWindow();
    }

    static boolean deleteAssertion(AssertionTreeNode node) {
            // Make sure
        if ((JOptionPane.showConfirmDialog(
          getMainWindow(),
          "Are you sure you wish to delete assertion " +
          node.getName() + "?",
          "Delete assertion",
          JOptionPane.YES_NO_OPTION)) == 1) {
            return false;
        }

        return true;
    }
}
