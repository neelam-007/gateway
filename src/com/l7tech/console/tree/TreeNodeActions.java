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
     * locate the node by name starting at the given node.
     * the method is not geenral, that is it assumes that the
     * userObject() contains <CODE>AbstractTreeNode</CODE>
     * instance.
     *
     * @param name   the name to look for
     * @param node   the intial position where the search starts
     * @return the  <CODE>AbstractTreeNode </CODE> with the given name,
     *              or <B>null</B> if  not found
     */
    public static TreeNode nodeByName(String name, DefaultMutableTreeNode node) {
        Enumeration e = node.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode tn =
              (DefaultMutableTreeNode)e.nextElement();

            if (!(tn instanceof AbstractTreeNode)) {
                continue;
            }
            AbstractTreeNode an = (AbstractTreeNode)tn;

            if (name.equals(an.getName())) {
                return tn;
            }
        }
        return null;
    }

    /**
     * locate the node by name path starting at the given node.
     * the method is not geenral, that is it assumes that the
     * userObject() contains <CODE>AbstractTreeNode</CODE>
     * instance.
     *
     * @param path   the name path to look for
     * @param node   the intial position where the search starts
     * @return the <CODE>TreeNode</CODE> that contains the
     *         userObject with the given name, or <B>null</B> if
     *         not found
     */
    public static TreeNode nodeByNamePath(String[] path, DefaultMutableTreeNode node) {
        DefaultMutableTreeNode n = node;
        for (int i = 0; path !=null && i < path.length; i++) {
            String s = path[i];
            n = (DefaultMutableTreeNode)nodeByName(s, n);
            if (n == null) return null;
        }
        return n;
    }

    /**
     * Deletes the given AbstractTreeNode
     *
     * @param bn - the node to be deleted
     * @return true if deleted, false otherwise
     */
    public static boolean delete(AbstractTreeNode bn) {
        boolean rb = false;
        if (bn instanceof ProviderNode) {
            rb = delete((ProviderNode)bn);
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
          "Are you sure you want to delete " +
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
