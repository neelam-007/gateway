package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;

import javax.swing.*;


/**
 * The class represents a node element in the TreeModel.
 * It represents the XML encryption of the request node.
 *
 * @author flascell
 * @version 1.0
 */
public class RequestWssIntegrityNode extends AbstractTreeNode {
    private static final String DEFAULT_NAME = "Sign Request";
    private final String name;

    public RequestWssIntegrityNode(String name) {
        super(null);
        this.name = name;
    }

    public RequestWssIntegrityNode() {
        super(null);
        this.name = DEFAULT_NAME;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return super.getActions();
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the assertion this node represnts
     */
    public Assertion asAssertion() {
        return new RequestWssIntegrity();
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
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
     * subclasses override this method
     */
    protected void loadChildren() {}

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return name;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }
}
