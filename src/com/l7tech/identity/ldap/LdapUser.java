package com.l7tech.identity.ldap;

import com.l7tech.identity.internal.imp.UserImp;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 17, 2003
 *
 * A ldap specific implementation of the user type which contains
 * properties relating to ldap such as dn.
 */
public class LdapUser extends UserImp {
    public String getDN() {
        return dn;
    }

    public void setDN(String dn) {
        this.dn = dn;
    }

    public String getName() {
        return _name;
    }

    public void setName( String name ) {
        _name = name;
    }

    public String toString() {
        return "com.l7tech.identity.ldap.LdapUser."
                + "\n\tFirst name=" + getFirstName()
                + "\n\tLast name=" + getLastName()
                + "\n\tLogin=" + getLogin()
                + "\n\temail=" + getEmail()
                + "\n\tName=" + getName()
                + "\n\tPassword=" + getPassword()
                + "\n\tDN=" + getDN();
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private String dn = null;
    private String _name = null;
}
