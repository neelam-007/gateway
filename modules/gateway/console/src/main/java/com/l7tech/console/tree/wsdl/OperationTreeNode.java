package com.l7tech.console.tree.wsdl;

import javax.wsdl.Operation;

/**
 * Class OperationTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class OperationTreeNode extends WsdlTreeNode {
    private Operation operation;

    OperationTreeNode(Operation o, Options options) {
        super(null, options);
        this.operation = o;
    }

    protected void doLoadChildren() {
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
        return "com/l7tech/console/resources/methodPublic.gif";
    }


    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return operation.getName();
    }

}

