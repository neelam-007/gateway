package com.l7tech.console.tree.wsdl;

import javax.wsdl.Service;

/**
 * Class ServiceTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class ServiceTreeNode extends WsdlTreeNode {
    private Service service;

    ServiceTreeNode(Service s) {
        super(null);
        this.service = s;
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
        return "com/l7tech/console/resources/services16.png";
    }

    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return service.getQName().getLocalPart();
    }

}

