package com.l7tech.identity.ldap;

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
public class LdapUser extends LdapIdentityBase implements User, Serializable {
    public static final String[] HASH_PREFIXES = {
        "{md5}", "{md4}", "{smd5}",
        "{sha}", "{sha1}", "{ssha}",
        "{crypt}",
    };

    private UserBean userBean;

    public LdapUser( UserBean bean ) {
        userBean = bean;
    }

    public LdapUser() {
        userBean = new UserBean();
    }

    public String getLogin() {
        return userBean.getLogin();
    }

    public String getPassword() {
        return userBean.getPassword();
    }

    public String getFirstName() {
        return userBean.getFirstName();
    }

    public String getLastName() {
        return userBean.getLastName();
    }

    public String getEmail() {
        return userBean.getEmail();
    }

    public String getDepartment() {
        return userBean.getDepartment();
    }

    public String getSubjectDn() {
        return dn;
    }

    public UserBean getUserBean() {
        return userBean;
    }

    public void setLogin(String login) {
        userBean.setLogin( login );
    }

    /**
     * set the login before setting the password.
     * if the password is not encoded, this will encode it.
     */
    public void setPassword(String password) throws  IllegalStateException {
        if ( password != null && !isAlreadyEncoded(password)) {
            // Check for LDAP hashed passwords
            String lcpass = password.toLowerCase();
            for (String prefix : HASH_PREFIXES) {
                if (lcpass.startsWith(prefix)) {
                    userBean.setPassword(null);
                    return;
                }
            }
        }
        String login = userBean.getLogin();
        if ( login == null) throw new IllegalStateException("login must be set prior to encoding the password");
        userBean.setPassword( password );
    }

    public void setFirstName(String firstName) {
        userBean.setFirstName( firstName );
    }

    public void setLastName(String lastName) {
        userBean.setLastName( lastName );
    }

    public void setEmail(String email) {
        userBean.setEmail( email );
    }

    public void setDepartment(String department) {
        userBean.setDepartment( department );
    }

    public long getProviderId() {
        return userBean.getProviderId();
    }

    @Override
    public void setProviderId( long providerId) {
        super.setProviderId(providerId);
        userBean.setProviderId(providerId);
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        userBean.setName(name);
    }

    @Override
    public synchronized void setDn(String dn) {
        super.setDn(dn);
        userBean.setSubjectDn(dn);
    }

    @Override
    public void setCn(String cn) {
        super.setCn(cn);
        userBean.setName(cn);
    }

    public String toString() {
        return "com.l7tech.identity.ldap.LdapUser." +
                "\n\tName=" + getName() +
                "\n\tFirst name=" + getFirstName() +
                "\n\tLast name=" + getLastName() +
                "\n\tLogin=" + getLogin() +
                "\n\tproviderId=" + userBean.getProviderId();
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
        setPassword(imp.getPassword());
        setAttributes(imp.getAttributes());
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LdapUser ldapUser = (LdapUser) o;

        if (userBean != null ? !userBean.equals(ldapUser.userBean) : ldapUser.userBean != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (userBean != null ? userBean.hashCode() : 0);
        return result;
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
}
