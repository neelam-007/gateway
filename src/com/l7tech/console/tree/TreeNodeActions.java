package com.l7tech.console.tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;


/**
 * TreeNodeActions - bag of utility methods for TreeNodes.
 */
public class TreeNodeActions {

    // Hide the constructor
    private TreeNodeActions() {
    }

    /**
     * locate the name
     * the method is not geenral, that is it assumes that the
     * userObject() contains <CODE>AbstractTreeNode</CODE>
     * instance.
     *
     * @param name   the name to look for
     * @param node   the intial position where the search starts
     * @return the <CODE>TreeNode</CODE> that contains the
     *         userObject with the given name, or <B>null</B> if
     *         not found
     */
    public static TreeNode nodeByName(String name, DefaultMutableTreeNode node) {
        Enumeration enum = node.breadthFirstEnumeration();
        while (enum.hasMoreElements()) {
            DefaultMutableTreeNode tn =
              (DefaultMutableTreeNode) enum.nextElement();

            if (!(tn instanceof AbstractTreeNode)) {
                continue;
            }
            AbstractTreeNode an = (AbstractTreeNode) tn;

            if (name.equals(an.getName())) {
                return tn;
            }
        }
        return null;
    }

    /**
     * Deletes the given EntityTreeNode
     *
     * @param node - the node to be deleted
     * @return true if deleted, false otherwise
     */
    public static boolean deleteNode(EntityTreeNode node) {
        boolean rb = false;
        // Dispatch deletion based on the actual node type
        Object object = node.getUserObject();
        if (object instanceof AbstractTreeNode) {
            rb = delete((AbstractTreeNode) object);
        }
        return rb;
    }

    /**
     * Deletes the given EntityTreeNode
     *
     * @param bn - the node to be deleted
     * @return true if deleted, false otherwise
     */
    public static boolean delete(AbstractTreeNode bn) {
        boolean rb = false;
        if (bn instanceof ProviderNode) {
            rb = delete((ProviderNode) bn);
        } else {
            // Unknown node type .. do nothing
            rb = false;
        }
        return rb;
    }

    // Deletes the given Realm
    private static boolean delete(ProviderNode node) {
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
