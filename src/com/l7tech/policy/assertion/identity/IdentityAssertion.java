/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.objectmodel.Entity;
import com.l7tech.policy.assertion.Assertion;

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
