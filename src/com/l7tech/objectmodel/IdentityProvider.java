package com.l7tech.objectmodel;


public interface IdentityProvider extends NamedEntity {
    String getDescription();
    IdentityProviderType getType();

    void setDescription( String description );
    void setType( IdentityProviderType type );
}
