package com.l7tech.identity.internal;

import com.l7tech.objectmodel.NamedEntity;

import java.util.Collection;

public interface Group extends NamedEntity {
    String getDescription();
    
    Collection getMembers();

    void setDescription( String description );
}
