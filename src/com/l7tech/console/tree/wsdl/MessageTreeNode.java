package com.l7tech.console.tree.wsdl;

import javax.wsdl.Message;

/**
 * Class MessageTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class MessageTreeNode extends WsdlTreeNode {
    private Message message;

    MessageTreeNode(Message m) {
        super(null);
        this.message = m;
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
        return "com/l7tech/console/resources/SendMail16.gif";
    }


    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return message.getQName().getLocalPart();
    }
}

