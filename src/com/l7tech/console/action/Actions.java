package com.l7tech.console.action;

import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.Group;
import com.l7tech.identity.CannotDeleteAdminAccountException;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;

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
        } else if (bn instanceof GroupNode) {
            return deleteGroup((GroupNode)bn);
        } else if (bn instanceof ProviderNode) {
            return deleteProvider((ProviderNode)bn);
        } else {
            // Unknown node type .. do nothing
            rb = false;
        }
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
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        // Delete the  node and update the tree
        try {
            EntityHeader eh = node.getEntityHeader();
            User u = new User();
            u.setOid(eh.getOid());
            Registry.getDefault().getInternalUserManager().delete(u);
            return true;
        } catch (CannotDeleteAdminAccountException e) {
            log.log(Level.SEVERE, "Error deleting user", e);
            // Error deleting realm - display error msg
            JOptionPane.showMessageDialog(
              getMainWindow(),
              "User " +
              node.getName() +
              " is an administrator, and cannot be deleted.",
              "Delete User",
              JOptionPane.ERROR_MESSAGE);
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
    private static boolean deleteGroup(GroupNode node) {
        // Make sure
        if ((JOptionPane.showConfirmDialog(
          getMainWindow(),
          "Are you sure you wish to delete " +
          node.getName() + "?",
          "Delete Group",
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        // Delete the  node and update the tree
        try {
            EntityHeader eh = node.getEntityHeader();
            Group g = new Group();
            g.setOid(eh.getOid());
            Registry.getDefault().getInternalGroupManager().delete(g);
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error deleting group", e);
            // Error deleting realm - display error msg
            JOptionPane.showMessageDialog(
              getMainWindow(),
              "Error encountered while deleting " +
              node.getName() +
              ". Please try again later.",
              "Delete Group",
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
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
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
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
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
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        return true;
    }

    /**
     * perform the action passed
     *
     * @param a the action to invoke
     */
    public static void invokeAction(Action a) {
        if (a instanceof BaseAction) {
            ((BaseAction)a).performAction();
        }
    }

    public static boolean deletePolicyTemplate(PolicyTemplateNode node) {
        // Make sure
        if ((JOptionPane.showConfirmDialog(
          getMainWindow(),
          "Are you sure you wish to delete template " +
          node.getName() + "?",
          "Delete Policy Template",
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
          return false;
        }
         // Delete the  node and update the tree
        try {
            File file = node.getFile();
            if (file.exists()) {
                if (!file.delete()) {
                    throw new IOException("Error deleting file "+file.getName());
                }
            }
            return true;
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(Level.WARNING, e, "Error deleting policy template");
        }
        return false;
    }
}
