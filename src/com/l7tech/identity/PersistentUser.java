/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PersistentUser extends NamedEntityImp implements User {
    public PersistentUser(UserBean bean) {
        this.bean = bean;
        String uid = bean.getId();
        if (uid == null) {
            this._oid = DEFAULT_OID;
        } else {
            this._oid = Long.valueOf(uid);
        }
    }

    public PersistentUser() {
        this.bean = new UserBean();
    }

    public String getId() {
        return Long.toString(_oid);
    }

    public void setOid( long oid ) {
        super.setOid( oid );
        bean.setUniqueIdentifier( Long.toString( oid ) );
    }

    public long getOid() {
        String uniqueId = bean.getId();
        if ( uniqueId == null || uniqueId.length() == 0 )
            return -1L;
        else
            return Long.parseLong(bean.getId());
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId) {
        bean.setProviderId(providerId);
    }

    public UserBean getUserBean() {
        return bean;
    }

    public String getName() {
        return bean.getName();
    }

    public void setName( String name ) {
        super.setName(name);
        bean.setName( name );
    }

    public String getSubjectDn() {
        return bean.getSubjectDn();
    }

    public void setSubjectDn(String subjectDn) {
        bean.setSubjectDn(subjectDn);
    }

    /**
     * set the login before setting the password.
     * if the password is not encoded, this will encode it.
     */
    public void setLogin(String login) {
        bean.setLogin( login );
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return bean.getProviderId();
    }

    public String getLogin() {
        return bean.getLogin();
    }

    public String getPassword() {
        return bean.getPassword();
    }

    public String getFirstName() {
        return bean.getFirstName();
    }

    public String getLastName() {
        return bean.getLastName();
    }

    public String getEmail() {
        return bean.getEmail();
    }

    public String getDepartment() {
        return bean.getDepartment();
    }

    public String toString() {
        return "com.l7tech.identity.User." +
                "\n\tName=" + bean.getName() +
                "\n\tFirst name=" + bean.getFirstName() +
                "\n\tLast name=" + bean.getLastName() +
                "\n\tLogin=" + bean.getLogin() +
                "\n\tproviderId=" + bean.getProviderId();
    }

    public void setDepartment( String department ) {
        bean.setDepartment( department );
    }

    public void setEmail( String email ) {
        bean.setEmail( email );
    }

    public void setFirstName( String firstName ) {
        bean.setFirstName( firstName );
    }

    public void setLastName( String lastName ) {
        bean.setLastName( lastName );
    }

    public int getVersion() {
        return bean.getVersion();
    }

    public void setVersion(int version) {
        bean.setVersion(version);
    }

    public boolean isEquivalentId(Object thatId) {
        return getId().equals(thatId.toString());
    }

    public abstract void copyFrom(User user);

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PersistentUser that = (PersistentUser) o;

        if (bean != null ? !bean.equals(that.bean) : that.bean != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (bean != null ? bean.hashCode() : 0);
        return result;
    }

    protected UserBean bean;
}