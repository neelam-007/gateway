package com.l7tech.console.tree.wsdl;

import javax.wsdl.Operation;
import javax.wsdl.PortType;
import java.util.Iterator;

/**
 * Class PortTypeTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PortTypeTreeNode extends WsdlTreeNode {
    private PortType portType;

    PortTypeTreeNode(PortType p) {
        super(null);
        this.portType = p;
    }

    protected void loadChildren() {
        int index = 0;
        children = null;
        for (Iterator i = portType.getOperations().iterator(); i.hasNext();) {
            insert(new OperationTreeNode((Operation)i.next()), index++);
        }
    }

    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/interface.gif";
    }


    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return portType.getQName().getLocalPart();
    }
}

