package com.l7tech.identity;

import com.l7tech.objectmodel.NamedEntity;

import java.util.Set;
import java.security.Principal;

public interface Group extends NamedEntity, Principal {
    long getProviderOid();
    String getDescription();

    Set getMembers();
    Set getMemberHeaders();

    void setMembers( Set members );
    void setMemberHeaders( Set memberHeaders );

    void setProviderOid( long oid );
    void setDescription( String description );
}
