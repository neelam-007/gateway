package com.l7tech.identity;

import com.l7tech.objectmodel.NamedEntity;

public interface IdentityProviderConfig extends NamedEntity {
    String getDescription();
    void setDescription( String description );
}
