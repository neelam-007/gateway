package com.l7tech.ssg.objectmodel;


public interface IdentityProvider extends NamedEntity {
    String getDescription();
    IdentityProviderType getType();

    void setDescription( String description );
    void setType( IdentityProviderType type );
}
