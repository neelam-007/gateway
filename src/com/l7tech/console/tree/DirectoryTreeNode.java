package com.l7tech.console.tree;

import org.apache.log4j.Category;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;




/**
 * the Tree Node encapsulating a <code>BasicTreeNode</code>.
 * 
 * Note that this class will be deprecated in the future.
 * It will be replaced with the BasicTreeNode that implements
 * fully the TreeNode interface.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @see com.l7tech.console.tree.BasicTreeNode
 */
public class DirectoryTreeNode extends DefaultMutableTreeNode {
  private static final Category log = Category.getInstance(DirectoryTreeNode.class.getName());

  /**
   * creates a <CODE>TreeNode</CODE> with the given BasicTreeNode
   * as a user object.
   * 
   * @param treeNode the tree node this node points to
   */
  public DirectoryTreeNode(BasicTreeNode treeNode) {
    super(treeNode);
    if (treeNode == null) {
      throw new NullPointerException("tree node");
    }
  }

  /**
   * Returns this node's <CODE>BasicTreeNode</CODE> that
   * is stored as user object.
   * 
   * @return the BasicTreeNode stored at this node 
   *         by the user
   */
  public BasicTreeNode getBasicTreeNode() {
    return (BasicTreeNode)getUserObject();
  }
  /**
   * a DirectoryTreeNode is a leaf if it cannot contain nodes
   */
  public boolean isLeaf() {
    return getBasicTreeNode().isLeaf();
  }

  /**
   * Determines whether this node can have children.
   * Only contexts can have children.
   * 
   * @return true if it allows children, false otherwise
   */
  public boolean getAllowsChildren() {
    return getBasicTreeNode().getAllowsChildren();
  }

  /**
   * Removes all of this node's children, setting their parents to null.
   * If this node has no children, this method does nothing.
   */
  public void removeAllChildren() {
    super.removeAllChildren();
    hasLoaded = false;
  }
  /**
   * Has this node loaded his children?
   * If the node is <CODE>Leaf</CODE> it always returns
   * true.
   *
   * @return true if child nodes were loaded, false otherwise
   */
  public boolean hasLoadedChildren() {
    if (isLeaf()) {
      return true;
    }
    return hasLoaded;
  }

  /**
   * load children under this node
   *
   * @param force  forces reload if true
   */
  public void loadChildren(boolean force) {
    if (force) {
      removeAllChildren();
      loadChildren();
    } else {
      if (!hasLoaded) {
        loadChildren();
      }
    }
  }

  /**
   * load the children under this node
   */
  protected void loadChildren() {
    // if it is a leaf, just say we have loaded them
    if (!getAllowsChildren()) {
      hasLoaded = true;
      return;
    }
    try {
      Enumeration enum = getBasicTreeNode().children();

      List nodes = new ArrayList();

      while (enum.hasMoreElements()) {
        nodes.add(enum.nextElement());
      }
      Iterator iterator = nodes.iterator();
      int index = 0;
      while (iterator.hasNext()) {
        insert(new DirectoryTreeNode((BasicTreeNode)iterator.next()), index++);
      }
      hasLoaded = true;
    } catch (Exception e) {
      log.error("loadChildren()", e);
    }
  }

  /**
   * Sorts the children under this node with the specified
   * Comparator.
   * 
   * @param c      the sort Comparator
   */
  public void sortChildren(Comparator c) {
    Collections.sort(children, c);
  }

  /**
   * @return the TreeNode in this node's child array at the specified
   * index using the filter if specified
   */
  TreeNode getChildAt(int index, NodeFilter filter) {
    if (filter == null) {
      return super.getChildAt(index);
    }

    if (children == null) {
      throw new ArrayIndexOutOfBoundsException("node has no children");
    }

    int visibleIndex = -1;
    int realIndex = -1;
    Enumeration enum = children.elements();
    while (enum.hasMoreElements()) {
      if (filter.accept((javax.swing.tree.TreeNode)enum.nextElement())) {
        visibleIndex++;
      }
      realIndex++;
      if (visibleIndex == index) {
        return(javax.swing.tree.TreeNode)children.elementAt(realIndex);
      }
    }
    throw new ArrayIndexOutOfBoundsException("index unmatched");
  }

  /**
   * @return returns the number of children of parent. Returns 0 if the
   *         node is a leaf or if it has no children. parent must be a
   *         node previously obtained from this data source.
   */
  int getChildCount(NodeFilter filter) {
    int realCount = getChildCount();
    if (filter == null) {
      return realCount;
    }
    if (children == null) {
      return 0;
    }

    int count = 0;
    Enumeration enum = children.elements();
    while (enum.hasMoreElements()) {
      if (filter.accept((TreeNode)enum.nextElement())) {
        count++;
      }
    }
    return count;
  }

  /**
   * return the number of children for this folder node. The first
   * time this method is called we load up all of the folders
   * under the store's defaultFolder
   * 
   * @return 
   */
  public int getChildCount() {
    if (!hasLoaded) {
      loadChildren();
    }
    return super.getChildCount();
  }

  /**
   * default comparator, sort directory object labels
   * as two strings lexicographically.
   * Note, the <i>folders</I> have to be considered specially,
   * that is, the folder is <I>always</I> lower value then
   * any other object.
   *
   * @see String.compareTo()
   */
  public static final Comparator
  DEFAULT_COMPARATOR = new Comparator() {
    /**
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     * @throws ClassCastException if the arguments' types prevent them from
     *         being compared by this Comparator.
     */
    public int compare(Object o1, Object o2) {
      // Need to work with Entrys
      BasicTreeNode n1 = null;
      BasicTreeNode n2 = null;
      n1 = ((DirectoryTreeNode)o1).getBasicTreeNode();
      n2 = ((DirectoryTreeNode)o2).getBasicTreeNode();
      // need to add logic for folders
      return n1.getLabel().compareToIgnoreCase(n2.getLabel());
    }
  };

  /**
   * is the object a folder?
   *
   * @param object the object to check
   * @return true if object is any of the folders, false otherwise
   */
  private static boolean isFolder(Object object) {
    Class clazz = object.getClass();
    return
    clazz.equals(ProvidersFolderTreeNode.class) ||
    clazz.equals(AdminFolderTreeNode.class) ||
    clazz.equals(UserFolderTreeNode.class);
  }

  boolean hasLoaded = false;
}

