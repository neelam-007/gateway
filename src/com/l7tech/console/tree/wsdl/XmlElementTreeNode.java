package com.l7tech.console.tree.wsdl;



/**
 * Class <code>XmlElementTreeNode</code> is an generic representation
 * of an XML tree node.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XmlElementTreeNode extends WsdlTreeNode {
    private final String name;

    public XmlElementTreeNode(String name, Options options) {
        super(null, options);
        if (name == null) {
            throw new IllegalArgumentException();
        }
        this.name = name;
    }

    /**
     * @return the node name
     */
    public String getName() {
        return name;
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
        return "com/l7tech/console/resources/xmlelement.gif";
    }

    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return name;
    }
}

