package com.l7tech.console.tree;

import org.apache.log4j.Category;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;


/**
 * TreeNodeAction - set of actions on DirectoryTreeNodes;
 * Currently only delete actions are implemented.
 */
public class TreeNodeAction {
  private static final Category log =
    Category.getInstance(TreeNodeAction.class.getName());


  // Hide the constructor
  private TreeNodeAction() {
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
        (DefaultMutableTreeNode)enum.nextElement();
      Object o = tn.getUserObject();
      if (!(o instanceof BasicTreeNode)) {
        continue;
      }
      BasicTreeNode dobj = (BasicTreeNode)o;

      if (name.equals(dobj.getName())) {
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
  public static boolean deleteNode(DirectoryTreeNode node) {
    boolean rb = false;
    // Dispatch deletion based on the actual node type
    Object object = node.getUserObject();
    if (object instanceof BasicTreeNode) {
      rb = delete((BasicTreeNode)object);
    }
    return rb;
  }

  /**
   * Deletes the given DirectoryTreeNode
   *
   * @param bn - the node to be deleted
   * @return true if deleted, false otherwise
   */
  public static boolean delete(BasicTreeNode bn) {
    boolean rb = false;
    if (bn instanceof ProviderNode) {
      rb = delete((ProviderNode) bn);
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
      return hasProperties((BasicTreeNode)o);
    }
    return false;
  }

  /**
   * @param bn   BasicTreeNode  the object to determine if it
   *               has properties
   * @return true if the node has properties, false otherwise
   */
  public static boolean hasProperties(BasicTreeNode bn) {
    return
      bn instanceof ProviderNode ||
      bn instanceof GroupNode ||
      bn instanceof UserNode;
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
      return isBrowseable((BasicTreeNode)o);
    }
    return false;
  }

  /**
   * @param bn   BasicTreeNode  the object to determine if it
   *               has properties
   * @return true if the node has properties, false otherwise
   */
  public static boolean isBrowseable(BasicTreeNode bn) {
    return
      (bn instanceof ProvidersFolderNode ||
       bn instanceof ProviderNode);
  }

  /**
   * @param node   DirectoryNode  the node to determine if it
   *               can be deleted
   * @return true if the node can be deleted, false otherwise
   */
  public static boolean canDelete(DirectoryTreeNode node) {
    Object object = node.getUserObject();

    if (object instanceof BasicTreeNode) {
      return canDelete((BasicTreeNode)object);
    }
    return false;
  }

  /**
   * @param bn   the node to determine if it
   *               can be deleted
   * @return true if the node can be deleted, false otherwise
   */
  public static boolean canDelete(BasicTreeNode bn) {
    return bn instanceof EntityHeaderNode;
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

    // Delete the realm node and update the tree
    try {
      // Try deleting realm
//      RealmUpdater realmUp =
//        (RealmUpdater)ServiceLocator.getInstance().getService(SOAPConstants.REALM_UPDATER, RealmUpdater.class);
//      realmUp.deleteRealm(realmTreeNode.getObjectId().longValue());

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
