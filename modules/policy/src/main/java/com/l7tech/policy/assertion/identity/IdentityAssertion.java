/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.PersistentEntity;
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
    protected long _identityProviderOid = PersistentEntity.DEFAULT_OID;

    protected IdentityAssertion() { }

    protected IdentityAssertion(long providerOid) {
        this._identityProviderOid = providerOid;
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

    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(oldEntityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG) && oldEntityHeader.getOid() == _identityProviderOid &&
                newEntityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG))
        {
            _identityProviderOid = newEntityHeader.getOid();
        }
    }

    /**
     * The identity to use when logging a failed authentication.  For a specific user, this will be the user name
     * or login; for a group, this will be the group name; and for other identity assertions it will be some other
     * useful indication of which identity assertion just failed to authenticate.
     *
     * @return  a String representing this identity assertion, typically a user or group name, or null if no such
     *          information is available.  (Implementors are discouraged from returning null if there is anything
     *          at all useful they could return instead.)
     */
    public abstract String loggingIdentity();
}
