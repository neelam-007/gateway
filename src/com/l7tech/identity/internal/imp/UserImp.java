/*
 * Created on 7-May-2003
 */
package com.l7tech.identity.internal.imp;

import java.util.Set;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.imp.EntityImp;
import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * @author alex
 */
public class UserImp extends NamedEntityImp /*implements User*/ {
    /*public String getName() {
        return _login;
    }

    public void setName( String name ) {
        _login = name;
    }*/

    public long getProviderOid() {
        return _providerOid;
    }

    public String getLogin() {
        return _login;
    }

    public String getPassword() {
        return _password;
    }

    public String getFirstName() {
        return _firstName;
    }

    public String getLastName() {
        return _lastName;
    }

    public String getEmail() {
        return _email;
    }

    public String getTitle() {
        return _title;
    }

    public String getDepartment() {
        return _department;
    }

    public Set getGroups() {
        if (_groups == null) return new java.util.HashSet();
        return _groups;
    }

    public Set getGroupHeaders() {
        if (_groupHeaders == null) return new java.util.HashSet();
        return _groupHeaders;
    }

    public void setGroups( Set groups ) {
        _groups = groups;
    }

    public void setGroupHeaders( Set groupHeaders ) {
        _groupHeaders = groupHeaders;
    }

    public void setProviderOid( long oid ) {
        _providerOid = oid;
    }

    public void setLogin(String login) {
        _login = login;
    }

    public void setPassword(String password) {
        _password = password;
    }

    public void setFirstName(String firstName) {
        _firstName = firstName;
    }

    public void setLastName(String lastName) {
        _lastName = lastName;
    }

    public void setEmail(String email) {
        _email = email;
    }

    public void setTitle(String title) {
        _title = title;
    }

    public void setDepartment(String department) {
        _department = department;
    }

    public String toString() {
        return "com.l7tech.identity.internal.imp.UserImp." +
                "\n\tName=" + _name +
                "\n\tFirst name=" + _firstName +
                "\n\tLast name=" + _lastName +
                "\n\tLogin=" + _login +
                "\n\tPassword=" + _password;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserImp)) return false;
        final UserImp userImp = (UserImp) o;

        if (_oid != DEFAULT_OID ? !(_oid == userImp._oid) : userImp._oid != DEFAULT_OID ) return false;
        if (_providerOid != userImp._providerOid) return false;
        if (_login != null ? !_login.equals(userImp._login) : userImp._login != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int) (_providerOid ^ (_providerOid >>> 32));
        result = 29 * result + (_login != null ? _login.hashCode() : 0);
        result = 29 * result + (int)_oid;
        return result;
    }

    private long _providerOid;
    private String _login;
    private String _password;
    private String _firstName;
    private String _lastName;
    private String _email;
    private String _title;
    private String _department;
    private Set _groups;
    private Set _groupHeaders;
}
