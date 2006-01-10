package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;

/**
 * @author $Author$
 * @version $Revision$
 */
public class KerberosTreeNode extends LeafAssertionTreeNode {

   public KerberosTreeNode(RequestWssKerberos assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Require Kerberos Authentication";
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
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
        return "com/l7tech/console/resources/authentication.gif";
    }
}
