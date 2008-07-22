package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;

/**
 * @author $Author$
 */
public class KerberosTreeNode extends LeafAssertionTreeNode {
   public KerberosTreeNode(RequestWssKerberos assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Require WSS Kerberos Token";
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
