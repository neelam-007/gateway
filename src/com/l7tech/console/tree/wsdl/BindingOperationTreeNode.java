package com.l7tech.console.tree.wsdl;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;

/**
 * Class BindingOperationTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class BindingOperationTreeNode extends WsdlTreeNode {
    private BindingOperation operation;

    public BindingOperationTreeNode(BindingOperation bo) {
        super(null);
        this.operation = bo;
    }

    protected void loadChildren() {
        final Message m = operation.getOperation().getInput().getMessage();
        children = null;
        insert(new MessageTreeNode(m), 0);
    }

    /**
     * Returns true if the receiver is a leaf
     */
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Forward16.gif";
    }

    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return operation.getName();
    }

}

