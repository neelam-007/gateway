package com.l7tech.console.tree.policy;

import com.l7tech.console.action.RequestSwAAssertionPropertiesAction;
import com.l7tech.policy.assertion.RequestSwAAssertion;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertionPolicyTreeNode extends LeafAssertionTreeNode {
    static final Logger log = Logger.getLogger(RequestSwAAssertionPolicyTreeNode.class.getName());

    public RequestSwAAssertionPolicyTreeNode(RequestSwAAssertion assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
    }

    public String getName() {
        return "SOAP Request with Attachment";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new RequestSwAAssertionPropertiesAction(this);
    }
}
