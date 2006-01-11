package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;

import javax.naming.directory.Attributes;
import java.io.Serializable;

/**
 * User from an LDAP directory.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 */
public class LdapUser implements User, LdapIdentity, Serializable {
    public static final String[] HASH_PREFIXES = {
        "{md5}", "{md4}", "{smd5}",
        "{sha}", "{sha1}", "{ssha}",
        "{crypt}",
    };

    private String dn;
    private UserBean userBean;
    private Attributes attributes;

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
            for (int i = 0; i < HASH_PREFIXES.length; i++) {
                String prefix = HASH_PREFIXES[i];
                if ( lcpass.startsWith( prefix ) ) {
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

    public String getUniqueIdentifier() {
        return dn;
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public long getProviderId() {
        return userBean.getProviderId();
    }

    /**
     * this is not persisted, it is set at run time by the provider who creates the object
     */
    public void setProviderId( long providerId) {
        userBean.setProviderId(providerId);
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public String toString() {
        return "com.l7tech.identity.ldap.LdapUser." +
                "\n\tName=" + getName() +
                "\n\tFirst name=" + getFirstName() +
                "\n\tLast name=" + getLastName() +
                "\n\tLogin=" + getLogin() +
                "\n\tproviderId=" + userBean.getProviderId();
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
        userBean.setUniqueIdentifier(dn);
    }

    public String getCn() {
        return userBean.getName();
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setCn(String cn) {
        userBean.setName( cn );
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LdapUser)) return false;
        final LdapUser userImp = (LdapUser) o;
        final long providerId = userBean.getProviderId();
        if ( providerId != IdentityProviderConfig.DEFAULT_OID ? !( providerId == userImp.getProviderId() ) : userImp.getProviderId() != IdentityProviderConfig.DEFAULT_OID ) return false;
        String login = userBean.getLogin();
        String ologin = userImp.getLogin();
        if ( login != null ? !login.equals(ologin) : ologin != null) return false;
        return true;
    }

    public int hashCode() {
        if ( dn == null ) return System.identityHashCode(this);

        int hash = dn.hashCode();
        hash += 29 * (int)getProviderId();
        return hash;
    }

    public String getName() {
        return userBean.getName();
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
