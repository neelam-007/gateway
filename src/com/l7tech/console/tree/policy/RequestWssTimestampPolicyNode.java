/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.RequestWssTimestampPropertiesAction;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;

import javax.swing.*;

/**
 * Specifies the policy element that represents the soap response timestamp requirement.
 */
public class RequestWssTimestampPolicyNode extends LeafAssertionTreeNode {
    public RequestWssTimestampPolicyNode(RequestWssTimestamp assertion) {
        super(assertion);
    }

    public String getName() {
        return "Require Timestamp in Request";
    }

    public Action getPreferredAction() {
        return new RequestWssTimestampPropertiesAction(this);
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }
}
