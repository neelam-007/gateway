package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;

/**
 * Class <code>XmlDsigAssertionTreeNode</code> specifies the policy
 * element that represents the XML message signing requirement.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class XmlDsigAssertionTreeNode extends LeafAssertionTreeNode {

    public XmlDsigAssertionTreeNode(Assertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "XML message signed";
    }

    /**
     *Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }
}