/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.objectmodel.Entity;
import com.l7tech.policy.assertion.Assertion;

/**
 * Subclasses of IdentityAssertion are used to specify that the entity making
 * a <code>Request</code> (as previously found using a CredentialSourceAssertion)
 * can be authenticated, and is authorized to do so.
 *
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

    /**
     * Sets the OID of the <code>IdentityProvider</code> in which identities specified
     * using this Assertion should be enrolled.
     *
     * @param provider
     */
    public void setIdentityProviderOid( long provider ) {
        _identityProviderOid = provider;
    }

    /**
     * Sets the OID of the <code>IdentityProvider</code> in which identities specified
     * using this Assertion should be enrolled.
     */
    public long getIdentityProviderOid() {
        return _identityProviderOid;
    }

    protected long _identityProviderOid = Entity.DEFAULT_OID;
}
