package com.l7tech.console.action;

import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Supporting class for actions.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Actions {
    static Logger log = Logger.getLogger(Actions.class.getName());

    /**
     * Deletes the given EntityHeaderNode
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
        // Check that the user is not read-only
        if (!isParentIdProviderInternal(node)) {
            JOptionPane.showMessageDialog(null, "This user is read-only.",
                                                "Read-only",
                                                JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

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
            Registry.getDefault().getInternalUserManager().delete(eh.getStrId());
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
        // Check that the group is not read-only
        if (!isParentIdProviderInternal(node)) {
            JOptionPane.showMessageDialog(null, "This user is read-only.",
                                                "Read-only",
                                                JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

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
            Registry.getDefault().getInternalGroupManager().delete(eh.getStrId());
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
            Registry.getDefault().
              getServiceManager().
              deletePublishedService(node.getPublishedService().getOid());
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
        return Registry.getDefault().getComponentRegistry().getMainWindow();
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
                    throw new IOException("Error deleting file " + file.getName());
                }
            }
            return true;
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(Level.WARNING, e, "Error deleting policy template");
        }
        return false;
    }

    /**
     * invoke help programatically the F1 - help
     * @param c the event source component
     */
    public static void invokeHelp(Component c) {
        final KeyEvent ke =
          new KeyEvent(
            c,
            KeyEvent.KEY_PRESSED,
            0,
            0,
            KeyEvent.VK_F1, KeyEvent.CHAR_UNDEFINED);

        Runnable r = new Runnable() {
            public void run() {
                KeyboardFocusManager.
                  getCurrentKeyboardFocusManager().dispatchKeyEvent(ke);
            }
        };
        SwingUtilities.invokeLater(r);
    }

    private static boolean isParentIdProviderInternal(EntityHeaderNode usernode) {
        TreeNode parentNode = usernode.getParent();
        while (parentNode != null) {
            if (parentNode instanceof EntityHeaderNode) {
                EntityHeader header = ((EntityHeaderNode)parentNode).getEntityHeader();
                if (header.getType().equals(EntityType.ID_PROVIDER_CONFIG)) {
                    // we found the parent, see if it's internal one
                    if (header.getOid() != IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID) return false;
                    return true;
                }
            }
            parentNode = parentNode.getParent();
        }
        // assume it is unless proven otherwise
        return true;
    }
}
