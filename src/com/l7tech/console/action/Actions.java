package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.CannotDeleteAdminAccountException;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ObjectNotFoundException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    static boolean deleteEntity(EntityHeaderNode bn, IdentityProvider provider) throws ObjectNotFoundException {
        if (bn instanceof UserNode) {
            return deleteUser((UserNode)bn, provider);
        } else if (bn instanceof GroupNode) {
            return deleteGroup((GroupNode)bn, provider);
        } else if (bn instanceof ProviderNode) {
            return deleteProvider((ProviderNode)bn);
        }
        return false;
    }

    // Deletes the given user
    private static boolean deleteUser(UserNode node, IdentityProvider provider) throws ObjectNotFoundException {
        if (provider.isReadOnly()) return false;
        // Make sure
        if ((JOptionPane.showConfirmDialog(getMainWindow(),
          "Are you sure you want to delete " +
          node.getName() + "?",
          "Delete User",
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        // Delete the  node and update the tree
        try {
            EntityHeader eh = node.getEntityHeader();
            provider.getUserManager().delete(eh.getStrId());
            return true;
        } catch (CannotDeleteAdminAccountException e) {
            log.log(Level.SEVERE, "Error deleting user", e);
            // Error deleting realm - display error msg
            JOptionPane.showMessageDialog(getMainWindow(),
              "User " +
              node.getName() +
              " is an administrator, and cannot be deleted.",
              "Delete User",
              JOptionPane.ERROR_MESSAGE);
        } catch (DeleteException e) {
            log.log(Level.SEVERE, "Error deleting user", e);
            // Error deleting realm - display error msg
            JOptionPane.showMessageDialog(getMainWindow(),
              "Error encountered while deleting " +
              node.getName() +
              ". Please try again later.",
              "Delete User",
              JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }


    // Deletes the given user
    private static boolean deleteGroup(GroupNode node, IdentityProvider provider) throws ObjectNotFoundException {
        if (provider.isReadOnly()) return false;

        // Make sure
        if ((JOptionPane.showConfirmDialog(getMainWindow(),
          "Are you sure you want to delete " +
          node.getName() + "?",
          "Delete Group",
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        // Delete the  node and update the tree
        try {
            EntityHeader eh = node.getEntityHeader();
            provider.getGroupManager().delete(eh.getStrId());
            return true;
        } catch (DeleteException e) {
            log.log(Level.SEVERE, "Error deleting group", e);
            // Error deleting realm - display error msg
            JOptionPane.showMessageDialog(getMainWindow(),
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
        if ((JOptionPane.showConfirmDialog(getMainWindow(),
          "Are you sure you want to delete " +
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
        } catch (DeleteException e) {
            log.log(Level.SEVERE, "Error deleting provider", e);
            // Error deleting realm - display error msg
            JOptionPane.showMessageDialog(getMainWindow(),
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
        if ((JOptionPane.showConfirmDialog(getMainWindow(),
          "Are you sure you want to delete the " + node.getName() +  " Web service?",
          "Delete Web Service",
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
            JOptionPane.showMessageDialog(getMainWindow(),
              "Error encountered while deleting " +
              node.getName() +
              ". Please try again later.",
              "Delete Service",
              JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }


    private static JFrame getMainWindow() {
        return TopComponents.getInstance().getMainWindow();
    }

    static boolean deleteAssertion(AssertionTreeNode node) {
        // Make sure
        if ((JOptionPane.showConfirmDialog(getMainWindow(),
          "Are you sure you want to delete assertion " +
          node.getName() + "?",
          "Delete Assertion",
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        return true;
    }

    public static boolean deletePolicyTemplate(PolicyTemplateNode node) {
        // Make sure
        if ((JOptionPane.showConfirmDialog(getMainWindow(),
          "Are you sure you want to delete template " +
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
     *
     * @param c the event source component
     */
    public static void invokeHelp(Component c) {
        final KeyEvent ke =
          new KeyEvent(c,
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

    /**
     * Update the input map of the JDialog's <code>JLayeredPane</code> so
     * the ESC keystroke  invoke dispose on the dialog.
     *  
     * @param d the dialog
     */
    public static void setEscKeyStrokeDisposes(final JDialog d) {
        JLayeredPane layeredPane = d.getLayeredPane();
        final KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, "close-it");
        layeredPane.getActionMap().put("close-it",
          new AbstractAction() {
              public void actionPerformed(ActionEvent evt) {
                  d.dispose();
              }
          });
    }

      /**
     * Update the input map of the JFrame's <code>JLayeredPane</code> so
     * the ESC keystroke nvoke dispose on the frame.
     *
     * @param f the frame
     */
    public static void setEscKeyStrokeDisposes(final JFrame f) {
        JLayeredPane layeredPane = f.getLayeredPane();
        final KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, "close-it");
        layeredPane.getActionMap().put("close-it",
          new AbstractAction() {
              public void actionPerformed(ActionEvent evt) {
                  f.dispose();
              }
          });
    }
}
