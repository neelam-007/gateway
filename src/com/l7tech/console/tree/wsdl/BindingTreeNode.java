package com.l7tech.console.tree.wsdl;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import java.util.Iterator;

/**
 * Class BindingTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class BindingTreeNode extends WsdlTreeNode {
    private Binding binding;

    BindingTreeNode(Binding b) {
        super(null);
        this.binding = b;
    }

    protected void loadChildren() {
        int index = 0;
        children = null;
        for (Iterator i = binding.getBindingOperations().iterator(); i.hasNext();) {
            insert(new BindingOperationTreeNode((BindingOperation)i.next()), index++);
        }
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
        return binding.getQName().getLocalPart();
    }
}

