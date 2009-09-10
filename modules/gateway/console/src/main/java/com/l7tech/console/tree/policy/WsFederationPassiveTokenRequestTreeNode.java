/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;

import javax.swing.*;

/**
 * Tree node for WsFederationPassiveTokenRequest ....
 */
public class WsFederationPassiveTokenRequestTreeNode extends DefaultAssertionPolicyNode {

    //- PUBLIC

    public WsFederationPassiveTokenRequestTreeNode( WsFederationPassiveTokenRequest assertion ) {
        super( assertion );
    }

    public WsFederationPassiveTokenRequestTreeNode( WsFederationPassiveTokenExchange assertion ) {
        super( assertion );
    }
}
