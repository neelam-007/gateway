package com.l7tech.identity.internal;

import com.l7tech.identity.internal.Address;
import com.l7tech.objectmodel.NamedEntity;

import java.util.Collection;

public interface Organization extends NamedEntity {
    Address getAddress();
    Address getBillingAddress();
    Address getMailingAddress();

    Collection getUsers();

    void setAddress( Address address );
    void setBillingAddress( Address billingAddress );
    void setMailingAddress( Address mailingAddress );
}
