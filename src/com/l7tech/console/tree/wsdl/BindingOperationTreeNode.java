package com.l7tech.console.tree.wsdl;

import javax.wsdl.BindingOperation;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Output;

/**
 * Class BindingOperationTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class BindingOperationTreeNode extends WsdlTreeNode {
    protected BindingOperation operation;

    public BindingOperationTreeNode(BindingOperation bo, Options options) {
        super(null, options);
        this.operation = bo;
        this.setAllowsChildren(wsdlOptions.isShowInputMessages() || wsdlOptions.isShowOutputMessages());
    }

    protected void loadChildren() {
        children = null;
        int index = 0;
        if (wsdlOptions.isShowInputMessages()) {
            final Input input = operation.getOperation().getInput();
            if (input != null) {
                final Message m = input.getMessage();
                insert(new MessageTreeNode(m, wsdlOptions), index++);
            }
        }

        if (wsdlOptions.isShowOutputMessages()) {
            final Output output = operation.getOperation().getOutput();
            if (output != null) {
                final Message m = output.getMessage();
                insert(new MessageTreeNode(m, wsdlOptions), index++);
            }
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
        return !wsdlOptions.isShowInputMessages() && !wsdlOptions.isShowOutputMessages();
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return wsdlOptions.isShowInputMessages() || wsdlOptions.isShowOutputMessages();
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

