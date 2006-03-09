/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EchoRoutingAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public class EchoRoutingAssertionPolicyNode extends LeafAssertionTreeNode {
    public EchoRoutingAssertionPolicyNode(Assertion assertion ) {
        super( assertion );
        if (!(assertion instanceof EchoRoutingAssertion)) throw new IllegalArgumentException("assertion must be an " + EchoRoutingAssertion.class.getName()); 
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    public String getName() {
        return "Echo request to response";
    }

    public boolean canDelete() {
        return true;
    }
}
