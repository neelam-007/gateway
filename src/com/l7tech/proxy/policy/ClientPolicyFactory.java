/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy;

import com.l7tech.policy.PolicyFactory;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.proxy.policy.assertion.ClientAssertion;

import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientPolicyFactory extends PolicyFactory {
    public ClientAssertion makeClientPolicy( Assertion rootAssertion ) {
        return (ClientAssertion)makeSpecificPolicy( rootAssertion );
    }

    public static ClientPolicyFactory getInstance() {
        if ( _instance == null ) _instance = new ClientPolicyFactory();
        return _instance;
    }

    protected String getPackageName() {
        return "com.l7tech.proxy.policy.assertion";
    }

    protected String getPrefix() {
        return "Client";
    }

    private static ClientPolicyFactory _instance;
}
