package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;

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

    private String login;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private String department;

    public LdapUser() {
        this(IdentityProviderConfig.DEFAULT_OID, null, null);
    }

    public LdapUser(long providerOid, String dn, String cn) {
        super(providerOid, dn, cn);
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public String getSubjectDn() {
        return dn;
    }

    public void setLogin(String login) {
        this.login = login;
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
                    this.password = null;
                    return;
                }
            }
        }
        String login = getLogin();
        if ( login == null) throw new IllegalStateException("login must be set prior to encoding the password");
        this.password = password;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LdapUser ldapUser = (LdapUser) o;

        if (department != null ? !department.equals(ldapUser.department) : ldapUser.department != null) return false;
        if (email != null ? !email.equals(ldapUser.email) : ldapUser.email != null) return false;
        if (firstName != null ? !firstName.equals(ldapUser.firstName) : ldapUser.firstName != null) return false;
        if (lastName != null ? !lastName.equals(ldapUser.lastName) : ldapUser.lastName != null) return false;
        if (login != null ? !login.equals(ldapUser.login) : ldapUser.login != null) return false;
        if (password != null ? !password.equals(ldapUser.password) : ldapUser.password != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (login != null ? login.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (department != null ? department.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "com.l7tech.identity.ldap.LdapUser." +
                "\n\tName=" + getName() +
                "\n\tFirst name=" + getFirstName() +
                "\n\tLast name=" + getLastName() +
                "\n\tLogin=" + getLogin() +
                "\n\tproviderId=" + getProviderId();
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
