/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditWsFederationPassiveTokenRequestAction;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;

import javax.swing.*;

/**
 * Tree node for WsFederationPassiveTokenRequest ....
 */
public class WsFederationPassiveTokenRequestTreeNode extends LeafAssertionTreeNode {

    //- PUBLIC

    public WsFederationPassiveTokenRequestTreeNode( WsFederationPassiveTokenRequest assertion ) {
        super( assertion );
    }

    public WsFederationPassiveTokenRequestTreeNode( WsFederationPassiveTokenExchange assertion ) {
        super( assertion );
    }

    public Action getPreferredAction() {
        return new EditWsFederationPassiveTokenRequestAction(this);
    }

    public String getName() {
        String name = null;
        Assertion assertion = asAssertion();
        if (assertion instanceof WsFederationPassiveTokenRequest) {
            name = "Obtain Credentials using WS-Federation Request to " + ((WsFederationPassiveTokenRequest)assertion).getIpStsUrl();
        }
        else {
            WsFederationPassiveTokenExchange _assertion = (WsFederationPassiveTokenExchange) assertion;
            if((_assertion.getIpStsUrl()!=null && _assertion.getIpStsUrl().length()>0) || !_assertion.isAuthenticate()) {
                name = "Exchange credentials using WS-Federation request to " + _assertion.getIpStsUrl();
            }
            else {
                name = "Authenticate with WS-Federation protected service at " + _assertion.getReplyUrl();
            }
        }
        return name;
    }

    //- PROTECTED

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }
}
