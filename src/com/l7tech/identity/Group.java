package com.l7tech.identity;

import com.l7tech.objectmodel.NamedEntity;

import java.util.Collection;

public interface Group extends NamedEntity {
    String getDescription();

    Collection getMembers();
    Collection getMemberHeaders();

    void setDescription( String description );
}
