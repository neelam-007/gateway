package com.l7tech.console.tree;

import javax.swing.tree.TreeNode;

/**
 * The <CODE>NodeFilter</CODE> interface filters the node.
 *
 */
public class SubstringNodeFilter implements NodeFilter {
    /**
     * constructor	with a string to match (substring)
     *
     * @param match  the substring to match
     */
    public SubstringNodeFilter(String match) {
        this.match = match;
    }

    /**
     * @param node  the <code>TreeNode</code> to examine
     * @return  true if filter accepts the node, false otherwise
     */
    public boolean accept(TreeNode node) {
        //!! removed when moved to Entry
        return false;
    }

    private String match;
}
