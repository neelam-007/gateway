package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;

/**
 * Node for the RequestWssX509Cert assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
public class RequestWssX509Node extends AbstractTreeNode {
    public RequestWssX509Node() {
        super(null);
    }
    protected void loadChildren() {
    }

    public String getName() {
        return "WSS Signature";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }

    public Assertion asAssertion() {
        return new RequestWssX509Cert();
    }
}
