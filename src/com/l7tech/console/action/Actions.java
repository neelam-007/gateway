package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.common.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
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
    static boolean deleteEntity(EntityHeaderNode bn, IdentityProviderConfig config) {
        if (bn instanceof UserNode) {
            return deleteUser((UserNode)bn, config);
        } else if (bn instanceof GroupNode) {
            return deleteGroup((GroupNode)bn, config);
        } else if (bn instanceof IdentityProviderNode) {
            return deleteProvider((IdentityProviderNode)bn);
        }
        return false;
    }

    // Deletes the given user
    private static boolean deleteUser(UserNode node, IdentityProviderConfig config) {
        if (!config.isWritable()) return false;
        // Make sure
        if ((JOptionPane.showConfirmDialog(getTopParent(),
          "Are you sure you want to delete " +
          node.getName() + "?",
          "Delete User",
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        // Delete the  node and update the tree
        try {
            EntityHeader eh = node.getEntityHeader();
            IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
            admin.deleteUser(config.getOid(), eh.getStrId());
            return true;
        } catch (DeleteException e) {
            log.log(Level.SEVERE, "Error deleting user", e);
            // Error deleting realm - display error msg
            JOptionPane.showMessageDialog(getTopParent(),
              ExceptionUtils.getMessage(e),
              "User Cannot Be Deleted",
              JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error deleting user", e);
            // Error deleting realm - display error msg
            JOptionPane.showMessageDialog(getTopParent(),
              "Error encountered while deleting " +
              node.getName() +
              ". Please try again later.",
              "Delete User",
              JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    // Deletes the given user
    private static boolean deleteGroup(GroupNode node, IdentityProviderConfig config) {
        if (!config.isWritable()) return false;

        // Make sure
        if ((JOptionPane.showConfirmDialog(getTopParent(),
          "Are you sure you want to delete " +
          node.getName() + "?",
          "Delete Group",
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        // Delete the  node and update the tree
        try {
            EntityHeader eh = node.getEntityHeader();
            Registry.getDefault().getIdentityAdmin().deleteGroup(config.getOid(), eh.getStrId());
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error deleting group", e);
            // Error deleting realm - display error msg
            String msg;
            DeleteException de = getDeleteException(e);
            if (de != null) {
                msg = de.getMessage();
            } else
                msg = "Error encountered while deleting " + node.getName() + ". Please try again later.";
            JOptionPane.showMessageDialog(getTopParent(), msg, "Delete Group", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private static DeleteException getDeleteException(Throwable e) {
        if (e == null) return null;
        if (e instanceof DeleteException) return (DeleteException)e;
        return getDeleteException(e.getCause());
    }


    // Deletes the given user
    private static boolean deleteProvider(IdentityProviderNode nodeIdentity) {
        // Make sure
        if ((JOptionPane.showConfirmDialog(getTopParent(),
          "Are you sure you want to delete " +
          nodeIdentity.getName() + "?",
          "Delete Provider",
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        // Delete the  node and update the tree
        try {
            EntityHeader eh = nodeIdentity.getEntityHeader();
            Registry.getDefault().getIdentityAdmin().deleteIdentityProviderConfig(eh.getOid());
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error deleting provider", e);
            // Error deleting realm - display error msg
            JOptionPane.showMessageDialog(getTopParent(),
              "Error encountered while deleting " +
              nodeIdentity.getName() +
              ". Please try again later.",
              "Delete Provider",
              JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }


    // Deletes the given saervice
    static boolean deleteService(ServiceNode node) {
        // Make sure
        if ((JOptionPane.showConfirmDialog(getTopParent(),
          "Are you sure you want to delete the " + node.getName() + " service?",
          "Delete Service",
          JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        // Delete the  node and update the tree
        try {
            final ServiceAdmin serviceManager = Registry.getDefault().getServiceManager();
            serviceManager.deletePublishedService(Long.toString(node.getPublishedService().getOid()));
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error deleting service", e);
            JOptionPane.showMessageDialog(getTopParent(),
              "Error encountered while deleting " +
              node.getName() +
              ". Please try again later.",
              "Delete Service",
              JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }



    static boolean deleteAssertion(AssertionTreeNode node) {
        // Make sure
        String nodeName = node.getName();
        if (nodeName != null && nodeName.length() > 80) {
            nodeName = nodeName.substring(0,76) + "...";
        }
        if ((JOptionPane.showConfirmDialog(getTopParent(),
                                           "Are you sure you want to delete assertion " + nodeName + "?", "Delete Assertion",
                                           JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        return true;
    }

    private static Frame getTopParent() {
        return TopComponents.getInstance().getTopParent();
    }

    static boolean deleteAssertions(AssertionTreeNode[] nodes) {
        // Make sure
        if (nodes == null || (JOptionPane.showConfirmDialog(getTopParent(),
                                           "Are you sure you want to delete " +nodes.length+ " assertions?", "Delete Assertions",
                                           JOptionPane.YES_NO_OPTION)) != JOptionPane.YES_OPTION) {
            return false;
        }

        return true;
    }

    public static boolean deletePolicyTemplate(PolicyTemplateNode node) {
        // Make sure
        if ((JOptionPane.showConfirmDialog(getTopParent(),
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
        final ActionEvent ke = new ActionEvent(c, 0, "help");

        Runnable r = new Runnable() {
            public void run() {
                TopComponents.getInstance().showHelpTopics();
            }
        };
        SwingUtilities.invokeLater(r);
    }

}
