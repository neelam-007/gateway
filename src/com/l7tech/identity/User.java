package com.l7tech.identity;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.Set;
import java.util.Collections;
import java.security.Principal;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 * User either from the internal identity provider or a ldap directory.
 * In the case of ldap, the name property contains the dn.
 * Password property is stored as HEX(MD5(login:password)). If you pass a clear text passwd in
 * setPassword, this encoding will be done ofr you (provided that login was set before). 
 */
public class User extends NamedEntityImp implements Principal {

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
        return _groups;
    }

    public Set getGroupHeaders() {
        return _groupHeaders;
    }

    public void setGroups( Set groups ) {
        _groups = groups;
    }

    public void setGroupHeaders( Set groupHeaders ) {
        _groupHeaders = groupHeaders;
    }

    public void setLogin(String login) {
        _login = login;
    }

    /**
     * set the login before setting the password.
     * if the password is not encoded, this will encode it.
     */
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

    public String getCert() {
        return _cert;
    }

    public void setCert( String cert ) {
        _cert = cert;
    }

    public int getCertResetCounter() {
        return _certResetCounter;
    }

    public void setCertResetCounter(int certResetCounter) {
        _certResetCounter = certResetCounter;
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return providerId;
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    public String toString() {
        return "com.l7tech.identity.User." +
                "\n\tName=" + _name +
                "\n\tFirst name=" + _firstName +
                "\n\tLast name=" + _lastName +
                "\n\tLogin=" + _login +
                "\n\tPassword=" + _password +
                "\n\tproviderId=" + providerId;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        final User userImp = (User) o;
        if ( providerId != DEFAULT_OID ? !( providerId== userImp.providerId ) : userImp.providerId != DEFAULT_OID ) return false;
        if (_login != null ? !_login.equals(userImp._login) : userImp._login != null) return false;
        return true;
    }

    public int hashCode() {
        if ( _oid > 0 ) return (int)_oid;
        if ( _login == null ) return System.identityHashCode(this);

        int hash = _login.hashCode();
        hash += 29 * (int)providerId;
        return hash;
    }

    public static String encodePasswd(String login, String passwd) {
        String toEncode = login + ":" + passwd;

        // MD5 IT
        java.security.MessageDigest md5Helper = null;
        try {
            md5Helper = java.security.MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] digest = md5Helper.digest(toEncode.getBytes());
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

    /**
     * allows to set all properties from another object
     */
    public void copyFrom(User objToCopy) {
        setOid(objToCopy.getOid());
        setName(objToCopy.getName());
        setProviderId(objToCopy.getProviderId());
        setLogin(objToCopy.getLogin());
        setDepartment(objToCopy.getDepartment());
        setEmail(objToCopy.getEmail());
        setFirstName(objToCopy.getFirstName());
        setGroups(objToCopy.getGroups());
        setLastName(objToCopy.getLastName());
        setTitle(objToCopy.getTitle());
        setCert( objToCopy.getCert() );
        setCertResetCounter( objToCopy.getCertResetCounter() );
        _password = objToCopy.getPassword();
    }

    // ************************************************
    // PRIVATES
    // ************************************************

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

    private String _login;
    private String _password;
    private String _firstName;
    private String _lastName;
    private String _email;
    private String _title;
    private String _department;
    private int _certResetCounter;
    private String _cert;

    private Set _groups = Collections.EMPTY_SET;
    private Set _groupHeaders = Collections.EMPTY_SET;
    private long providerId = DEFAULT_OID;
}
