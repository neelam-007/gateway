/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.BridgeRoutingAssertionPropertiesAction;
import com.l7tech.policy.assertion.RoutingAssertion;

import javax.swing.*;

/**
 * Represents a policy tree node for a BridgeRoutingAssertion.
 */
public class BridgeRoutingAssertionTreeNode extends HttpRoutingAssertionTreeNode {
    public BridgeRoutingAssertionTreeNode(RoutingAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        return super.getName() + " using SecureSpan Bridge";
    }

    public Action getPreferredAction() {
        return new BridgeRoutingAssertionPropertiesAction(this);
    }
}
