/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.internal.imp;

import java.util.Collection;

import com.l7tech.identity.internal.Address;
import com.l7tech.identity.internal.Organization;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 */
public class OrganizationImp extends NamedEntityImp implements Organization {
    public Address getAddress() {
        return _address;
    }

    public Address getBillingAddress() {
        return _billingAddress;
    }

    public Address getMailingAddress() {
        return _mailingAddress;
    }

    public Collection getUsers() {
        return _users;
    }

    public void setUsers( Collection users ) {
        _users = users;
    }

    public void setAddress(Address address) {
        _address = address;
    }

    public void setBillingAddress(Address billingAddress) {
        _billingAddress = billingAddress;
    }

    public void setMailingAddress(Address mailingAddress) {
        _mailingAddress = mailingAddress;
    }

    private Address _address;
    private Address _billingAddress;
    private Address _mailingAddress;
    private Collection _users;
}
