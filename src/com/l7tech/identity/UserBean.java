/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.identity;

import com.l7tech.common.util.HexUtils;
import com.l7tech.objectmodel.Entity;

import java.io.Serializable;

/**
 * @author alex
 */
public class UserBean implements User, Serializable {
    public UserBean() {
    }

    public UserBean(String login) {
        this._name = login;
        this._login = login;
    }

    public String getUniqueIdentifier() {
        return _uniqueId;
    }

    public void setUniqueIdentifier( String uid ) {
        _uniqueId = uid;
    }

    public void setLogin(String login) {
        _login = login;
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

    public String getDepartment() {
        return _department;
    }

    public UserBean getUserBean() {
        return this;
    }

    public long getProviderId() {
        return _providerId;
    }

    public void setProviderId( long providerId ) {
        _providerId = providerId;
    }

    public void setPassword(String password) throws IllegalStateException {
        setPassword(password, false);
    }

    /**
     * Set the password for this user
     *
     * @param password the password (clear or encoded)
     * @param hintIsClear true if you want to communicate that the password is in clear text
     */
    public void setPassword(String password, boolean hintIsClear) throws IllegalStateException {
        if (password != null && (hintIsClear || !HexUtils.containsOnlyHex(password))) {
            if (_login == null) throw new IllegalStateException("login must be set prior to encoding the password");
            _password = HexUtils.encodePasswd(_login, password);
        }
        else _password = password;
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

    public void setDepartment(String department) {
        _department = department;
    }

    public String getName() {
        return _name;
    }

    public void setName( String name ) {
        _name = name;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn( String subjectDn ) {
        this.subjectDn = subjectDn;
    }

    public int getVersion() {
        return _version;
    }

    public void setVersion( int version ) {
        _version = version;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserBean)) return false;

        final UserBean userBean = (UserBean) o;

        if (_providerId != userBean._providerId) return false;
        return !(_login != null ? !_login.equals(userBean._login) : userBean._login != null);

    }

    public int hashCode() {
        int result;
        result = (int) (_providerId ^ (_providerId >>> 32));
        result = 29 * result + (_login != null ? _login.hashCode() : 0);
        return result;
    }

    private static final long serialVersionUID = -2689153614711342567L;

    protected long _providerId = Entity.DEFAULT_OID;
    protected String _uniqueId;
    protected String _name;
    protected String _login;
    protected String _password;
    protected String _firstName;
    protected String _lastName;
    protected String _email;
    protected String _department;
    protected String subjectDn;
    protected int _version;
}
