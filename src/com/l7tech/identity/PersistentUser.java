/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

/**
 * @author alex
 * @version $Revision$
 */
@MappedSuperclass
public abstract class PersistentUser extends NamedEntityImp implements User {
    protected String login;
    protected long providerOid;
    protected String subjectDn;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String department;

    /*Put in to allow for JAXB processing*/
    public PersistentUser(){

    }
    
    protected PersistentUser(long providerOid, String login) {
        this.providerOid = providerOid;
        this.login = login;
        this._name = login;
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId) {
        this.providerOid = providerId;
    }

    @Transient
    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return providerOid;
    }

    public String getLogin() {
        return login;
    }

    @Column(name="first_name", length=32)
    public String getFirstName() {
        return firstName;
    }

    @Column(name="last_name", length=32)
    public String getLastName() {
        return lastName;
    }

    @Column(length=128)
    public String getEmail() {
        return email;
    }

    @Transient
    public String getDepartment() {
        return department;
    }

    public String toString() {
        return "com.l7tech.identity.User." +
                "\n\tName=" + _name +
                "\n\tFirst name=" + firstName +
                "\n\tLast name=" + lastName +
                "\n\tLogin=" + login +
                "\n\tproviderId=" + providerOid;
    }

    public void setDepartment( String department ) {
        this.department = department;
    }

    public void setEmail( String email ) {
        this.email = email;
    }

    public void setFirstName( String firstName ) {
        this.firstName = firstName;
    }

    public void setLastName( String lastName ) {
        this.lastName = lastName;
    }

    public boolean isEquivalentId(Object thatId) {
        return getId().equals(thatId.toString());
    }

    public abstract void copyFrom(User user);

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PersistentUser that = (PersistentUser) o;

        if (providerOid != that.providerOid) return false;
        if (department != null ? !department.equals(that.department) : that.department != null) return false;
        if (email != null ? !email.equals(that.email) : that.email != null) return false;
        if (firstName != null ? !firstName.equals(that.firstName) : that.firstName != null) return false;
        if (lastName != null ? !lastName.equals(that.lastName) : that.lastName != null) return false;
        if (login != null ? !login.equals(that.login) : that.login != null) return false;
        if (subjectDn != null ? !subjectDn.equals(that.subjectDn) : that.subjectDn != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (login != null ? login.hashCode() : 0);
        result = 31 * result + (int) (providerOid ^ (providerOid >>> 32));
        result = 31 * result + (subjectDn != null ? subjectDn.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (department != null ? department.hashCode() : 0);
        return result;
    }
}