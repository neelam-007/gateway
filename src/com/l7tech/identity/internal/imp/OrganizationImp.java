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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationImp)) return false;

        final OrganizationImp organizationImp = (OrganizationImp) o;

        if (_oid != DEFAULT_OID ? !(_oid == organizationImp._oid) : organizationImp._oid != DEFAULT_OID ) return false;
        if (_address != null ? !_address.equals(organizationImp._address) : organizationImp._address != null) return false;
        if (_billingAddress != null ? !_billingAddress.equals(organizationImp._billingAddress) : organizationImp._billingAddress != null) return false;
        if (_mailingAddress != null ? !_mailingAddress.equals(organizationImp._mailingAddress) : organizationImp._mailingAddress != null) return false;
        if (_users != null ? !_users.equals(organizationImp._users) : organizationImp._users != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (_address != null ? _address.hashCode() : 0);
        result = 29 * result + (_billingAddress != null ? _billingAddress.hashCode() : 0);
        result = 29 * result + (_mailingAddress != null ? _mailingAddress.hashCode() : 0);
        result = 29 * result + (_users != null ? _users.hashCode() : 0);
        result = 29 * result + (int)_oid;
        return result;
    }

    private Address _address;
    private Address _billingAddress;
    private Address _mailingAddress;
    private Collection _users;
}
