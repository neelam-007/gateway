package com.l7tech.identity.ldap;

import com.l7tech.common.io.CertUtils;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Goid;

import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User from an LDAP directory.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 */
public class LdapUser extends LdapIdentityBase implements User, Serializable {
    private static final Logger logger = Logger.getLogger(LdapUser.class.getName());

    private static final String[] HASH_PREFIXES = {
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
    private byte[] ldapCertBytes;

    private transient X509Certificate cachedCert;

    public LdapUser() {
        this(IdentityProviderConfig.DEFAULT_GOID, null, null);
    }

    public LdapUser(Goid providerGoid, String dn, String cn) {
        super(providerGoid, dn, cn);
    }

    @Override
    public String getLogin() {
        return login;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getDepartment() {
        return department;
    }

    @Override
    public String getSubjectDn() {
        return dn;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * set the login before setting the password.
     * if the password is not encoded, this will encode it.
     * - //todo - fix comment or method - password will not be encoded by this method.
     */
    public void setPassword(String pass) throws IllegalStateException {
        if (pass != null) {
            // Check for LDAP hashed passwords
            String lcpass = pass.toLowerCase();
            for (String prefix : HASH_PREFIXES) {
                if (lcpass.startsWith(prefix)) {
                    logger.log(Level.FINE, "LDAP password is hashed with {0}; ignoring", prefix);
                    this.password = null;
                    return;
                }
            }
        }
        this.password = pass;
    }

    /**
     * @return password, likely to be null.
     */
    public String getPassword() {
        return password;
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

    @Override
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

    @Override
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

    @Override
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
        setLdapCertBytes(imp.getLdapCertBytes());
    }

    public byte[] getLdapCertBytes() {
        return ldapCertBytes;
    }

    public void setLdapCertBytes(byte[] certbytes) {
        ldapCertBytes = certbytes;
    }

    public synchronized X509Certificate getCertificate() throws CertificateException {
        if (cachedCert == null) {
            if (ldapCertBytes == null) return null;
            cachedCert = CertUtils.decodeCert(ldapCertBytes);
        }
        return cachedCert;
    }

    public synchronized void setCertificate(X509Certificate cachedCert) throws CertificateEncodingException {
        this.cachedCert = cachedCert;
        this.ldapCertBytes = cachedCert.getEncoded();
    }

}
