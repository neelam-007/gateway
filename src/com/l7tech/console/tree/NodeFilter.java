package com.l7tech.console.tree;

import javax.swing.tree.TreeNode;

/**
 * The <CODE>NodeFilter</CODE> interface filters the node.
 * 
 */
public interface NodeFilter {
  /**
   * @param node  the <code>TreeNode</code> to examine
   * @return  true if filter accepts the node, false otherwise
   */
  boolean accept(TreeNode node);
}
