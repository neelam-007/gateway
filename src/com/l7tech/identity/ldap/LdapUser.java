package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;

import java.io.Serializable;

/**
 * User from an LDAP directory.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 */
public class LdapUser implements User, Serializable {
    public static final String[] HASH_PREFIXES = {
        "{md5}", "{md4}", "{smd5}",
        "{sha}", "{sha1}", "{ssha}",
        "{crypt}",
    };

    public LdapUser( UserBean bean ) {
        _userBean = bean;
    }

    public LdapUser() {
        _userBean = new UserBean();
    }

    public String getLogin() {
        return _userBean.getLogin();
    }

    public String getPassword() {
        return _userBean.getPassword();
    }

    public String getFirstName() {
        return _userBean.getFirstName();
    }

    public String getLastName() {
        return _userBean.getLastName();
    }

    public String getEmail() {
        return _userBean.getEmail();
    }

    public String getTitle() {
        return _userBean.getTitle();
    }

    public String getDepartment() {
        return _userBean.getDepartment();
    }

    public String getSubjectDn() {
        return _dn;
    }

    public UserBean getUserBean() {
        return _userBean;
    }

    public void setLogin(String login) {
        _userBean.setLogin( login );
    }

    /**
     * set the login before setting the password.
     * if the password is not encoded, this will encode it.
     */
    public void setPassword(String password) throws  IllegalStateException {
        if ( password != null && !isAlreadyEncoded(password)) {
            // Check for LDAP hashed passwords
            String lcpass = password.toLowerCase();
            for (int i = 0; i < HASH_PREFIXES.length; i++) {
                String prefix = HASH_PREFIXES[i];
                if ( lcpass.startsWith( prefix ) ) {
                    _userBean.setPassword(null);
                    return;
                }
            }
        }
        String login = _userBean.getLogin();
        if ( login == null) throw new IllegalStateException("login must be set prior to encoding the password");
        _userBean.setPassword( password );
    }

    public void setFirstName(String firstName) {
        _userBean.setFirstName( firstName );
    }

    public void setLastName(String lastName) {
        _userBean.setLastName( lastName );
    }

    public void setEmail(String email) {
        _userBean.setEmail( email );
    }

    public void setTitle(String title) {
        _userBean.setTitle( title );
    }

    public void setDepartment(String department) {
        _userBean.setDepartment( department );
    }

    public String getUniqueIdentifier() {
        return _dn;
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return _userBean.getProviderId();
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId) {
        _userBean.setProviderId(providerId);
    }

    public String toString() {
        return "com.l7tech.identity.ldap.LdapUser." +
                "\n\tName=" + getName() +
                "\n\tFirst name=" + getFirstName() +
                "\n\tLast name=" + getLastName() +
                "\n\tLogin=" + getLogin() +
                "\n\tproviderId=" + _userBean.getProviderId();
    }

    public String getDn() {
        return _dn;
    }

    public void setDn(String dn) {
        _dn = dn;
        _userBean.setUniqueIdentifier(dn);
    }

    public String getCn() {
        return _userBean.getName();
    }

    public void setCn(String cn) {
        _userBean.setName( cn );
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LdapUser)) return false;
        final LdapUser userImp = (LdapUser) o;
        final long providerId = _userBean.getProviderId();
        if ( providerId != IdentityProviderConfig.DEFAULT_OID ? !( providerId == userImp.getProviderId() ) : userImp.getProviderId() != IdentityProviderConfig.DEFAULT_OID ) return false;
        String login = _userBean.getLogin();
        String ologin = userImp.getLogin();
        if ( login != null ? !login.equals(ologin) : ologin != null) return false;
        return true;
    }

    public int hashCode() {
        if ( _dn == null ) return System.identityHashCode(this);

        int hash = _dn.hashCode();
        hash += 29 * (int)getProviderId();
        return hash;
    }

    public String getName() {
        return _userBean.getName();
    }

    /**
     * allows to set all properties from another object
     */
    public void copyFrom( User objToCopy) {
        LdapUser imp = (LdapUser)objToCopy;
        setDn(imp.getDn());
        setCn(imp.getCn());
        setProviderId(imp.getProviderId());
        setLogin(imp.getLogin());
        setDepartment(imp.getDepartment());
        setEmail(imp.getEmail());
        setFirstName(imp.getFirstName());
        setLastName(imp.getLastName());
        setTitle(imp.getTitle());
        setPassword(imp.getPassword());
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

    private String _dn;

    private UserBean _userBean;
}
