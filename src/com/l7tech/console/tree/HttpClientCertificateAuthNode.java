package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;


/**
 * The class represents a node element in the TreeModel.
 * It represents the HTTP certificate based authentication.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HttpClientCertificateAuthNode extends AbstractTreeNode {
    /**
     * construct the <CODE>HttpDigestAuthNode</CODE> instance.
     */
    public HttpClientCertificateAuthNode(){
        super(null);
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
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new HttpClientCert();
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {}

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "HTTP Client Certificate";
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/authentication.gif";
    }
}
