package com.l7tech.identity;

import com.l7tech.identity.internal.Address;
import com.l7tech.identity.internal.Organization;
import com.l7tech.objectmodel.NamedEntity;

import java.util.Set;
import java.security.Principal;

public interface User extends NamedEntity, Principal {
    long getProviderOid();
    String getLogin();
    String getPassword();
    String getFirstName();
    String getLastName();
    String getEmail();
    String getTitle();
    Organization getOrganization();
    String getDepartment();
    Address getAddress();
    Address getMailingAddress();
    Address getBillingAddress();

    Set getGroups();
    Set getGroupHeaders();

    void setGroups( Set groups );
    void setGroupHeaders( Set groupHeaders );

    void setProviderOid( long oid );
    void setLogin( String login );
    void setPassword( String password );
    void setFirstName( String firstName );
    void setLastName( String lastName );
    void setEmail( String email );
    void setTitle( String title );
    void setOrganization( Organization organization );
    void setDepartment( String department );
    void setAddress( Address address );
    void setMailingAddress( Address mailingAddress );
    void setBillingAddress( Address billingAddress );
}
