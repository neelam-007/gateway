/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy;

import com.l7tech.policy.PolicyFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.assertion.ServerAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerPolicyFactory extends PolicyFactory {
    public ServerAssertion makeServerPolicy( Assertion rootAssertion ) {
        return (ServerAssertion)makeSpecificPolicy( rootAssertion );
    }

    public static ServerPolicyFactory getInstance() {
        if ( _instance == null ) _instance = new ServerPolicyFactory();
        return _instance;
    }

    protected String getPackageName() {
        return "com.l7tech.server.policy.assertion";
    }

    protected String getPrefix() {
        return "Server";
    }

    private static ServerPolicyFactory _instance;
}
