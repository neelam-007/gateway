package com.l7tech.identity.provider;

import com.l7tech.objectmodel.NamedEntity;


public interface IdentityProviderConfig extends NamedEntity {
    String getDescription();
    IdentityProviderType getType();

    void setDescription( String description );
    void setType( IdentityProviderType type );
}
