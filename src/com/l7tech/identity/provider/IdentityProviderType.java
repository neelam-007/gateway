package com.l7tech.identity.provider;

import com.l7tech.objectmodel.NamedEntity;


public interface IdentityProviderType extends NamedEntity {
    String getDescription();
    String getClassName();

    void setDescription( String description );
    void setClassName( String className );
}
