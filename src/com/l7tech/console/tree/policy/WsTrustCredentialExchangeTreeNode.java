/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditWsTrustCredentialExchangeAction;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class WsTrustCredentialExchangeTreeNode extends LeafAssertionTreeNode {
    private EditWsTrustCredentialExchangeAction editAction = new EditWsTrustCredentialExchangeAction(this);

    public WsTrustCredentialExchangeTreeNode( WsTrustCredentialExchange assertion ) {
        super( assertion );
        _assertion = assertion;
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    public Action getPreferredAction() {
        return editAction;
    }

    public Action[] getActions() {
        List actions = new ArrayList();
        actions.add(editAction);
        actions.addAll(Arrays.asList(super.getActions()));
        return (Action[])actions.toArray(new Action[0]);
    }

    public boolean canDelete() {
        return true;
    }

    private WsTrustCredentialExchange _assertion;

    public String getName() {
        return "Exchange credentials using WS-Trust request to " + _assertion.getTokenServiceUrl();
    }
}
