package com.l7tech.console.tree.wsdl;

import javax.wsdl.Part;

/**
 * Class MessagePartTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class MessagePartTreeNode extends WsdlTreeNode {
    private Part messagePart;

    MessagePartTreeNode(Part p, Options options) {
        super(null, options);
        messagePart = p;
    }

    /**
     * get the message part this node represents
     *
     * @return the corresponding message part
     */
    public Part getMessagePart() {
        return messagePart;
    }

    protected void loadChildren() {
    }

    /**
     * Returns true if the receiver is a leaf
     */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/paramIn.gif";
    }

    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return messagePart.getName();
    }
}

