/*
 * Created on 7-May-2003
 */
package com.l7tech.objectmodel.imp;

import java.util.Collection;

import com.l7tech.identity.provider.internal.Address;
import com.l7tech.identity.provider.internal.InternalOrganization;

/**
 * @author alex
 */
public class InternalOrganizationImp extends NamedEntityImp implements InternalOrganization {
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
