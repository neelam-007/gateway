/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditWsTrustCredentialExchangeAction;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;

import javax.swing.*;

public class WsTrustCredentialExchangeTreeNode extends LeafAssertionTreeNode<WsTrustCredentialExchange> {
    private final EditWsTrustCredentialExchangeAction editAction = new EditWsTrustCredentialExchangeAction(this);

    public WsTrustCredentialExchangeTreeNode(WsTrustCredentialExchange assertion) {
        super(assertion);
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    public Action getPreferredAction() {
        return editAction;
    }

    public String getName() {
        return "Exchange credentials using WS-Trust request to " + assertion.getTokenServiceUrl();
    }
}
