/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class PersistentUser extends NamedEntityImp implements User {
    public PersistentUser(UserBean bean) {
        this.bean = bean;
    }

    public PersistentUser() {
        this.bean = new UserBean();
    }

    public String getUniqueIdentifier() {
        return new Long( _oid ).toString();
    }

    public void setOid( long oid ) {
        super.setOid( oid );
        bean.setUniqueIdentifier( Long.toString( oid ) );
    }

    public long getOid() {
        String uniqueId = bean.getUniqueIdentifier();
        if ( uniqueId == null || uniqueId.length() == 0 )
            return -1L;
        else
            return new Long( bean.getUniqueIdentifier() ).longValue();
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
                "\n\tName=" + _name +
                "\n\tFirst name=" + bean.getFirstName() +
                "\n\tLast name=" + bean.getLastName() +
                "\n\tLogin=" + bean.getLogin() +
                "\n\tproviderId=" + bean.getProviderId();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalUser)) return false;
        final InternalUser userImp = (InternalUser) o;
        if (bean.getProviderId() != DEFAULT_OID ? !( bean.getProviderId()== userImp.bean.getProviderId())
                : userImp.bean.getProviderId() != DEFAULT_OID ) return false;
        String login = getLogin();
        String ologin = userImp.getLogin();
        if ( login != null ? !login.equals(ologin) : ologin != null) return false;
        return true;
    }

    public int hashCode() {
        if ( _oid != DEFAULT_OID ) return (int)_oid;
        String login = getLogin();
        if ( login == null ) return System.identityHashCode(this);

        int hash = login.hashCode();
        hash += 29 * (int)bean.getProviderId();
        return hash;
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

    public void setPassword( String password ) {
        bean.setPassword( password );
    }

    public int getVersion() {
        return bean.getVersion();
    }

    public void setVersion(int version) {
        bean.setVersion(version);
    }

    public abstract void copyFrom(User user);

    protected UserBean bean;
}
