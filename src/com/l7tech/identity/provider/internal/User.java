package com.l7tech.identity.provider.internal;

import com.l7tech.identity.provider.internal.Address;
import com.l7tech.objectmodel.StandardEntity;

import java.util.Collection;

public interface User extends StandardEntity {
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

    Collection getGroups();

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
