/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesEntities;

/**
 * Subclasses of IdentityAssertion are used to specify that the entity making
 * a <code>Request</code> (as previously found using a CredentialSourceAssertion)
 * can be authenticated, and is authorized to do so.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class IdentityAssertion extends Assertion implements UsesEntities {
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

    public EntityHeader[] getEntitiesUsed() {
        return new EntityHeader[] { new EntityHeader(Long.toString(_identityProviderOid), EntityType.ID_PROVIDER_CONFIG, null, null) };
    }

    protected long _identityProviderOid = PersistentEntity.DEFAULT_OID;
}
