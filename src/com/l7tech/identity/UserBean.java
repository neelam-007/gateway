/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.policy.assertion.credential.http.HttpDigest;

import java.io.Serializable;

/**
 * @author alex
 * @version $Revision$
 */
public class UserBean implements User, Serializable {
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

    public String getTitle() {
        return _title;
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

    public void setPassword(String password) throws  IllegalStateException {
        if ( password != null && !isAlreadyEncoded(password)) {
            if (_login == null) throw new IllegalStateException("login must be set prior to encoding the password");
            _password = encodePasswd(_login, password);
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

    public void setTitle(String title) {
        _title = title;
    }

    public void setDepartment(String department) {
        _department = department;
    }

    private boolean isAlreadyEncoded(String arg) {
        if (arg == null || arg.length() != 32) return false;
        String hexmembers = "0123456789abcdef";
        for (int i = 0; i < arg.length(); i++) {
            char toto = arg.charAt(i);
            if (hexmembers.indexOf(toto) == -1) {
                return false;
            }
        }
        return true;
    }

    private static String encodePasswd( String a1 ) {
        // MD5 IT
        java.security.MessageDigest md5Helper = null;
        try {
            md5Helper = java.security.MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] digest = md5Helper.digest( a1.getBytes() );
        // ENCODE IT
        if (digest == null) return "";
        char[] hexadecimal = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        if (digest.length != 16) return "";
        char[] buffer = new char[32];

        for (int i = 0; i < 16; i++) {
            int low = (digest[i] & 0x0f);
            int high = ((digest[i] & 0xf0) >> 4);
            buffer[i*2] = hexadecimal[high];
            buffer[i*2 + 1] = hexadecimal[low];
        }
        return new String(buffer);
    }

    public static String encodePasswd(String login, String passwd) {
        String toEncode = login + ":" + HttpDigest.REALM + ":" + passwd;
        return encodePasswd( toEncode );
    }

    public static String encodePasswd( String login, String passwd, String realm ) {
        String toEncode = login + ":" + realm + ":" + passwd;
        return encodePasswd( toEncode );
    }

    public String getName() {
        return _name;
    }

    public void setName( String name ) {
        _name = name;
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

        final UserBean userBean = (UserBean)o;

        if (_providerId != userBean._providerId) return false;
        if (_uniqueId != null ? !_uniqueId.equals(userBean._uniqueId) : userBean._uniqueId != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int)(_providerId ^ (_providerId >>> 32));
        result = 29 * result + (_uniqueId != null ? _uniqueId.hashCode() : 0);
        return result;
    }

    protected long _providerId;
    protected String _uniqueId;
    protected String _name;
    protected String _login;
    protected String _password;
    protected String _firstName;
    protected String _lastName;
    protected String _email;
    protected String _title;
    protected String _department;
    protected int _version;
}
