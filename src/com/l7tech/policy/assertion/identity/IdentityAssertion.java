/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.identity.*;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class IdentityAssertion extends Assertion {
    protected IdentityAssertion() {
        super();
    }

    protected IdentityAssertion( long oid ) {
        _identityProviderOid = oid;
    }

    public void setIdentityProviderOid( long provider ) {
        _identityProviderOid = provider;
    }

    public long getIdentityProviderOid() {
        return _identityProviderOid;
    }

    protected long _identityProviderOid = Entity.DEFAULT_OID;
}
