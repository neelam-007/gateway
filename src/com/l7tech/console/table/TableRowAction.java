package com.l7tech.console.table;


import com.l7tech.console.tree.AdminFolderNode;
import com.l7tech.console.tree.BasicTreeNode;
import com.l7tech.console.tree.DirectoryTreeNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.ProviderNode;
import com.l7tech.console.tree.ProvidersFolderNode;
import com.l7tech.console.tree.UserFolderNode;
import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;


/**
 * TableRowAction - set of actions on DirectoryTreeNodes;
 * Currently only delete actions are implemented.
 */
public class TableRowAction {
    private static final Category log =
            Category.getInstance(TableRowAction.class.getName());


    // Hide the constructor
    private TableRowAction() {
    }

    /**
     * locate the name
     * the method is not geenral, that is it assumes that the
     * userObject() contains <CODE>BasicTreeNode</CODE>
     * instance.
     *
     * @param name   the name to look for
     * @param node   the intiial position where the search starts
     * @return the <CODE>TreeNode</CODE> that contains the
     *         userObject with the given name, or <B>null</B> if
     *         not found
     */
    public static TreeNode nodeByName(String name, DefaultMutableTreeNode node) {
        Enumeration enum = node.breadthFirstEnumeration();
        while (enum.hasMoreElements()) {
            DefaultMutableTreeNode tn =
                    (DefaultMutableTreeNode) enum.nextElement();
            Object o = tn.getUserObject();
            if (!(o instanceof BasicTreeNode)) {
                continue;
            }
            BasicTreeNode dobj = (BasicTreeNode) o;

            if (name.equals(dobj.getFqName())) {
                return tn;
            }
        }
        return null;
    }

    /**
     * Deletes the given DirectoryTreeNode
     *
     * @param node - the node to be deleted
     * @return true if deleted, false otherwise
     */
    public static boolean deleteNode(DirectoryTreeNode node, boolean askQuestion) {
        boolean rb = false;
        // Dispatch deletion based on the actual node type
        Object object = node.getUserObject();
        if (object instanceof BasicTreeNode) {
            rb = delete((BasicTreeNode) object, askQuestion);
        }
        return rb;
    }

    /**
     * Deletes the given DirectoryTreeNode
     *
     * @param dobj - the node to be deleted
     * @param askQuestion - the flag indicating if the "Are you sure..."
     *                      question should be asked
     * @return true if deleted, false otherwise
     */
    public static boolean delete(BasicTreeNode dobj, boolean askQuestion) {
        boolean rb = false;
        if (dobj instanceof ProviderNode) {
            rb = delete((ProviderNode) dobj, askQuestion);
        } else if (dobj instanceof EntityHeaderNode) {
            rb = delete((EntityHeaderNode) dobj, askQuestion);
        } else {
            // Unknown node type .. do nothing
            rb = false;
        }
        return rb;
    }

    /**
     * @param node   DirectoryNode  the node to determine if it
     *               can refresh
     * @return true if the node can be refreshed, false otherwise
     */
    public static boolean canRefresh(DirectoryTreeNode node) {
        return false;
    }

    /**
     * @param node   DirectoryTreeNode  the node to determine if it
     *               has properties
     * @return true if the node has properties, false otherwise
     */
    public static boolean hasProperties(DirectoryTreeNode node) {
        Object o = node.getUserObject();
        if (o instanceof BasicTreeNode) {
            return hasProperties((BasicTreeNode) o);
        }
        return false;
    }

    /**
     * @param dobj   BasicTreeNode  the object to determine if it
     *               has properties
     * @return true if the node has properties, false otherwise
     */
    public static boolean hasProperties(BasicTreeNode dobj) {
        return dobj instanceof EntityHeaderNode;
    }

    /**
     *
     * @param node   DirectoryTreeNode  the node to determine if it
     *               has properties
     * @return true if the node can be expanded, false otherwise
     */
    public static boolean isBrowseable(DirectoryTreeNode node) {
        Object o = node.getUserObject();
        if (o instanceof BasicTreeNode) {
            return isBrowseable((BasicTreeNode) o);
        }
        return false;
    }

    /**
     * @param dobj   BasicTreeNode  the object to determine if it
     *               it supports browsing (that is, has children)
     * @return true if the node supports browsing, false otherwise
     */
    public static boolean isBrowseable(BasicTreeNode dobj) {
        return
                (dobj instanceof ProvidersFolderNode ||
                dobj instanceof AdminFolderNode ||
                dobj instanceof UserFolderNode ||
                dobj instanceof ProviderNode);
    }

    /**
     * @param dobj   BasicTreeNode  the object to determine if it
     *               it supports new children (that is, new operation)
     * @return true if the node accpets children, false otherwise
     */
    public static boolean acceptNewChildren(BasicTreeNode dobj) {
        return
                (dobj instanceof ProvidersFolderNode) ||
                (dobj instanceof AdminFolderNode) ||
                (dobj instanceof UserFolderNode);
    }

    /**
     * @param node   DirectoryNode  the node to determine if it
     *               can be deleted
     * @return true if the node can be deleted, false otherwise
     */
    public static boolean canDelete(DirectoryTreeNode node) {
        Object object = node.getUserObject();

        if (object instanceof BasicTreeNode) {
            return canDelete((BasicTreeNode) object);
        }
        return false;
    }

    /**
     * @param obj   the node to determine if it
     *               can be deleted
     * @return true if the node can be deleted, false otherwise
     */
    public static boolean canDelete(BasicTreeNode obj) {
        return obj instanceof EntityHeaderNode;
    }


    // Deletes the given Realm
    private static boolean delete(ProviderNode realmTreeNode,
                                  boolean askQuestion) {
        // Make sure
        if (askQuestion && ((JOptionPane.showConfirmDialog(null,
                "Are you sure you wish to delete " +
                realmTreeNode.getFqName() + "?",
                "Delete realm",
                JOptionPane.YES_NO_OPTION)) == 1)) {
            return false;
        }
        return false;
    }
}


