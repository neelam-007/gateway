package com.l7tech.console.tree.wsdl;

import javax.wsdl.BindingOperation;
import javax.wsdl.Message;
import javax.wsdl.Input;

/**
 * Class BindingOperationTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class BindingOperationTreeNode extends WsdlTreeNode {
    private BindingOperation operation;

    public BindingOperationTreeNode(BindingOperation bo, Options options) {
        super(null, options);
        this.operation = bo;
    }

    protected void loadChildren() {
        final Input input = operation.getOperation().getInput();
        if (input !=null) {
            final Message m = input.getMessage();
            children = null;
            insert(new MessageTreeNode(m, wsdlOptions), 0);
        }
    }
    /**
     * get the binding operation this node represents
     *
     * @return the corresponding binding operation
     */
    public BindingOperation getOperation() {
        return operation;
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

