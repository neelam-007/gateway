/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.action;

import com.l7tech.console.tree.policy.BridgeRoutingAssertionTreeNode;

/**
 * Action for showing {@link com.l7tech.policy.assertion.BridgeRoutingAssertion} properties dialog.
 */
public class BridgeRoutingAssertionPropertiesAction extends HttpRoutingAssertionPropertiesAction {
    public BridgeRoutingAssertionPropertiesAction(BridgeRoutingAssertionTreeNode node) {
        super(node);
    }
}
