package com.l7tech.console.tree.policy;

import com.l7tech.console.action.SslPropertiesAction;
import com.l7tech.policy.assertion.SslAssertion;

import javax.swing.*;

/**
 * Class <code>SslAssertionTreeNode</code> specifies the SSL
 * assertion requirement.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SslAssertionTreeNode extends LeafAssertionTreeNode {
    public SslAssertionTreeNode(SslAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        String ret = "Require SSL or TLS Transport";
        SslAssertion sa = (SslAssertion)getUserObject();
        if (SslAssertion.FORBIDDEN.equals(sa.getOption()))
            ret = "Forbid SSL or TLS transport";
        else if (SslAssertion.OPTIONAL.equals(sa.getOption()))
            ret = "Optional SSL or TLS transport";
        if (sa.isRequireClientAuthentication()) {
            ret += " with Client Certificate Authentication";
        }

        return ret;
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new SslPropertiesAction(this);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/ssl.gif";
    }
}