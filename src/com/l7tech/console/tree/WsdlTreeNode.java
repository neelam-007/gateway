package com.l7tech.console.tree;

import com.l7tech.service.Wsdl;
import org.apache.log4j.Category;

import javax.swing.tree.TreeNode;
import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.Message;
import java.util.*;


/**
 * the WSDL Tree Node represents the WSDL backed model.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @see BasicTreeNode
 */
public abstract class WsdlTreeNode implements TreeNode {
    private TreeNode parent;
    protected List children = new ArrayList(0);
    private boolean hasLoadedChildren = false;

    protected WsdlTreeNode(TreeNode parent) {
        this.parent = parent;
    }
    /**
     * creates a <CODE>TreeNode</CODE> with the given Wsdl
     * as a user object.
     *
     * @param wsdl the tree node this node points to
     */
    public static TreeNode newInstance(Wsdl wsdl) {
        if (wsdl == null) {
            throw new IllegalArgumentException();
        }
        return new DefinitionsTreeNode(wsdl.getDefinition());
    }

    /**
     * Returns the child <code>TreeNode</code> at index
     * <code>childIndex</code>.
     */
    public TreeNode getChildAt(int childIndex) {
        return (TreeNode)children.get(childIndex);
    }

    /**
     * Returns the number of children <code>TreeNode</code>s the receiver
     * contains.
     */
    public int getChildCount() {
        if (!hasLoadedChildren) {
            loadChildren();
            hasLoadedChildren = true;
        }
        return children.size();

    }

    protected abstract void loadChildren();


    /**
     * Returns the parent <code>TreeNode</code> of the receiver.
     */
    public TreeNode getParent() {
        return parent;
    }

    /**
     * Returns the index of <code>node</code> in the receivers children.
     * If the receiver does not contain <code>node</code>, -1 will be
     * returned.
     */
    public int getIndex(TreeNode node) {
        for (int i = children.size()-1; i >=0;i--) {
            if (children.get(i).equals(node)) {
                return i;
            }
        }
        return -1;
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
    public abstract boolean isLeaf();

    /**
     * Returns the children of the receiver as an <code>Enumeration</code>.
     */
    public Enumeration children() {
        return Collections.enumeration(children);
    }
}

class DefinitionsTreeNode extends WsdlTreeNode {
    private Definition definition;

    DefinitionsTreeNode(Definition def) {
        super(null);
        this.definition = def;
    }

    protected void loadChildren() {
        Map messages = definition.getMessages();
        for (Iterator i = messages.values().iterator(); i.hasNext();) {
            children.add(new MessageTreeNode(this, (Message)i.next()));
        }
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() { return false; }

    /**
     * @return  a string representation of the object.
     */
    public String toString() {
        return definition.getQName().getNamespaceURI();
    }

}

class MessageTreeNode extends WsdlTreeNode {
    private Message message;

    MessageTreeNode(TreeNode parent, Message m) {
        super(parent);
        this.message = m;
    }

    protected void loadChildren() {
    }

    /** Returns true if the receiver is a leaf */
    public boolean isLeaf() { return true; }

    /**
     * @return  a string representation of the object.
     */
    public String toString() {
        return message.getQName().getLocalPart();
    }

}