package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;

/**
 * This is the tree node corresponding to the RequestWssX509Cert assertion type.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
public class RequestWssX509CertTreeNode extends LeafAssertionTreeNode {
    public RequestWssX509CertTreeNode(RequestWssX509Cert assertion) {
        super(assertion);
    }

    public String getName() {
        return "WSS Sign SOAP request";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }
}
