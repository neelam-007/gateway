package com.l7tech.ssg.objectmodel;

import java.util.Collection;

public interface InternalOrganization extends NamedEntity {
    Address getAddress();
    Address getBillingAddress();
    Address getMailingAddress();

    Collection getUsers();

    void setAddress( Address address );
    void setBillingAddress( Address billingAddress );
    void setMailingAddress( Address mailingAddress );
}
