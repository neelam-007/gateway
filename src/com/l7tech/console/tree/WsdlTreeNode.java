package com.l7tech.console.tree;

import com.l7tech.service.Wsdl;
import org.apache.log4j.Category;

import javax.swing.tree.TreeNode;
import java.util.Enumeration;


/**
 * the WSDL Tree Node represents the WSDL backed model.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @see BasicTreeNode
 */
public class WsdlTreeNode implements TreeNode {
    private static final Category log = Category.getInstance(WsdlTreeNode.class.getName());
    private Wsdl wsdl;
    private WsdlTreeNode[] nodes = new WsdlTreeNode[0];

    /**
     * creates a <CODE>TreeNode</CODE> with the given Wsdl
     * as a user object.
     *
     * @param wsdl the tree node this node points to
     */
    public WsdlTreeNode(Wsdl wsdl) {
        this.wsdl = wsdl;
        if (wsdl == null) {
            throw new NullPointerException("tree node");
        }
    }

    /**
     * Returns the child <code>TreeNode</code> at index
     * <code>childIndex</code>.
     */
    public TreeNode getChildAt(int childIndex) {
        return null;
    }

    /**
     * Returns the number of children <code>TreeNode</code>s the receiver
     * contains.
     */
    public int getChildCount() {
        return 0;
    }

    /**
     * Returns the parent <code>TreeNode</code> of the receiver.
     */
    public TreeNode getParent() {
        return null;
    }

    /**
     * Returns the index of <code>node</code> in the receivers children.
     * If the receiver does not contain <code>node</code>, -1 will be
     * returned.
     */
    public int getIndex(TreeNode node) {
        return 0;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * Returns true if the receiver is a leaf.
     */
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns the children of the receiver as an <code>Enumeration</code>.
     */
    public Enumeration children() {
        return null;
    }
}

