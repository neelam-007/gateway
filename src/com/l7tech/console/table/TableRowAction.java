package com.l7tech.console.table;


import com.l7tech.console.tree.*;
import com.l7tech.objectmodel.EntityHeader;
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
     * Deletes the given EntityTreeNode
     *
     * @param dobj - the node to be deleted
     * @param askQuestion - the flag indicating if the "Are you sure..."
     *                      question should be asked
     * @return true if deleted, false otherwise
     */
    public static boolean delete(BasicTreeNode dobj, boolean askQuestion) {
        boolean rb = false;
        if (dobj instanceof EntityHeaderNode) {
            rb = deleteNode((EntityHeaderNode)dobj, askQuestion);
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
    public static boolean canRefresh(EntityTreeNode node) {
        return false;
    }

    /**
     * @param node   EntityTreeNode  the node to determine if it
     *               has properties
     * @return true if the node has properties, false otherwise
     */
    public static boolean hasProperties(EntityTreeNode node) {
        Object o = node.getUserObject();
        if (o instanceof BasicTreeNode) {
            return hasProperties(o);
        }
        return false;
    }

    /**
     * @param bn   BasicTreeNode  the object to determine if it
     *               has properties
     * @return true if the node has properties, false otherwise
     */
    public static boolean hasProperties(Object bn) {
        return
                bn instanceof EntityHeaderNode ||
                bn instanceof EntityHeader ;
    }

    /**
     *
     * @param node   EntityTreeNode  the node to determine if it
     *               has properties
     * @return true if the node can be expanded, false otherwise
     */
    public static boolean isBrowseable(EntityTreeNode node) {
        Object o = node.getUserObject();
        if (o instanceof BasicTreeNode) {
            return isBrowseable(o);
        }
        return false;
    }

    /**
     * @param bn   BasicTreeNode  the object to determine if it
     *               it supports browsing (that is, has children)
     * @return true if the node supports browsing, false otherwise
     */
    public static boolean isBrowseable(Object bn) {
        return
                (bn instanceof ProvidersFolderNode ||
                bn instanceof AdminFolderNode ||
                bn instanceof GroupFolderNode ||
                bn instanceof UserFolderNode ||
                bn instanceof ProviderNode);
    }

    /**
     * @param bn   BasicTreeNode  the object to determine if it
     *               it supports new children (that is, new operation)
     * @return true if the node accpets children, false otherwise
     */
    public static boolean acceptNewChildren(BasicTreeNode bn) {
        return
                (bn instanceof ProvidersFolderNode) ||
                (bn instanceof AdminFolderNode) ||
                (bn instanceof GroupFolderNode) ||
                (bn instanceof UserFolderNode);
    }

    /**
     * @param node   DirectoryNode  the node to determine if it
     *               can be deleted
     * @return true if the node can be deleted, false otherwise
     */
    public static boolean canDelete(EntityTreeNode node) {
        Object object = node.getUserObject();
        return canDelete(object);
    }

    /**
     * @param obj   the node to determine if it
     *               can be deleted
     * @return true if the node can be deleted, false otherwise
     */
    public static boolean canDelete(Object obj) {
        return obj instanceof EntityHeaderNode ||
               obj instanceof EntityHeader ;
    }


    // Deletes the given entity
    private static boolean deleteNode(EntityHeaderNode entity,
                                  boolean askQuestion) {
        // Make sure
        if (askQuestion && ((JOptionPane.showConfirmDialog(null,
                "Are you sure you wish to delete " +
                entity.getName() + "?",
                "Delete",
                JOptionPane.YES_NO_OPTION)) == JOptionPane.YES_OPTION)) {
            return true;
        }
        return false;
    }
}


