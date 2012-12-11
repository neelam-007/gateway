package com.l7tech.identity.internal;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.util.Charsets;
import com.l7tech.util.PoolByteArrayOutputStream;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO change to use NamedEntityWithPropertiesImp
/**
 * User from the internal identity provider.
 * Password property is stored as HEX(MD5(login:L7SSGDigestRealm:password)). If you pass a clear text passwd in
 * setPassword, this encoding will be done for you (provided that login was set before).
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 *
 *
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="internal_user")
public class InternalUser extends PersistentUser {
    public static final String PROPERTIES_KEY_SSH_USER_PUBLIC_KEY = "SshUserPublicKey";

    private static final long serialVersionUID = 5056935403670495223L;
    private static final Charset PROPERTIES_ENCODING = Charsets.UTF8;

    private long expiration = -1;
    protected String hashedPassword;
    /**
     * DigestAuthenticator is the only applicable client of this property.
     * Should not be used anywhere for authentication other than HTTPDigest, which is deprecated.
     * Cannot be used to authenticate administrative users.
     */
    private String httpDigest;

    private List<PasswordChangeRecord> passwordChangesHistory;
    private long passwordExpiry;
    private boolean changePassword;
    private boolean enabled = true;
    private transient String xmlProperties;
    private Map<String, String> properties;

    public InternalUser() {
        this(null);
    }

    public InternalUser(String login) {
        super(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, login);
    }

    /**
     * allows to set all properties from another object
     */
    @Override
    public void copyFrom( User objToCopy ) {
        InternalUser imp = (InternalUser)objToCopy;
        setOid(imp.getOid());
        setName(imp.getName());
        setProviderId(imp.getProviderId());
        setLogin(imp.getLogin());
        setDepartment(imp.getDepartment());
        setDescription(imp.getDescription());
        setEmail(imp.getEmail());
        setFirstName(imp.getFirstName());
        setLastName(imp.getLastName());
        setExpiration(imp.getExpiration());
        setHashedPassword(imp.getHashedPassword());
        setSubjectDn( imp.getSubjectDn() );
        setPasswordChangesHistory(imp.getPasswordChangesHistory());
        setPasswordExpiry(imp.getPasswordExpiry());
        setChangePassword(imp.isChangePassword());
        setEnabled(imp.isEnabled());
        setHttpDigest(imp.getHttpDigest());
        setXmlProperties(imp.getXmlProperties());
    }

    public void setHashedPassword(String password) {
        this.hashedPassword = password;
    }

    @Column(name="password", nullable=false, length=256)
    public String getHashedPassword() {
        return hashedPassword;
    }

    /**
     * @return Digest compatible with HTTP Digest. May be null if digest is not supported by the Gateway.
     */
    @Column(name="digest", nullable=true, length=32)
    public String getHttpDigest() {
        return httpDigest;
    }

    /**
     * Do not use this method unless you will maintain the invariant that the clear text password this digest
     * represents is the same as the clear text password maintained by hashedPassword.
     * 
     * @param httpDigest
     */
    public void setHttpDigest(String httpDigest) {
        this.httpDigest = httpDigest;
    }

    @Override
    @Column(name="description", length=255)
    public String getDescription() {
        return super.getDescription();
    }

    /**
     * Account expiration.
     * @return -1 if not set (never expires) or the time in millis after which this account should be considered expired.
     */
    @Column(nullable=false)
    public long getExpiration() {
        return expiration;
    }

    /**
     * Set the account expiration.
     * @param expiration -1 if never expires or the time in millis after which this account should be considered expired.
     */
    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="internalUser")
    @OrderBy("lastChanged ASC")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    public List<PasswordChangeRecord> getPasswordChangesHistory() {
        return passwordChangesHistory;
    }

    @Column(name="password_expiry")
    public long getPasswordExpiry() {
        return passwordExpiry;
    }

    public void setPasswordExpiry(long passwordExpiry) {
        this.passwordExpiry = passwordExpiry;
    }

    @Column(name="enabled")
    public boolean isEnabled(){
        return enabled;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

    @Column(name="change_password")
    public boolean isChangePassword() {
        return changePassword;
    }

    public void setChangePassword(boolean changePassword) {
        this.changePassword = changePassword;
    }

    public void setPasswordChangesHistory(List<PasswordChangeRecord> passwordChangesHistory) {
        if ( this.passwordChangesHistory != null) {
            for (PasswordChangeRecord changeRecord : passwordChangesHistory) {
                if (!this.passwordChangesHistory.contains(changeRecord)) {
                    this.passwordChangesHistory.add(changeRecord);
                }
            }
        } else {
            this.passwordChangesHistory = passwordChangesHistory;
        }
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        InternalUser that = (InternalUser) o;

        return expiration == that.expiration &&
               passwordExpiry == that.passwordExpiry &&
               changePassword == that.changePassword &&
               hashedPassword != null ? hashedPassword.equals(that.hashedPassword) : that.hashedPassword == null &&
               enabled == that.enabled;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (hashedPassword != null ? hashedPassword.hashCode() : 0);
        result = 31 * result + (int) (expiration ^ (expiration >>> 32));
        result = 31 * result + (int) (passwordExpiry ^ (passwordExpiry >>> 32));
        result = 31 * result + (passwordChangesHistory != null ? passwordChangesHistory.hashCode() : 0);
        result = 31 * result + Boolean.valueOf(changePassword).hashCode();
        result = 31 * result + Boolean.valueOf(enabled).hashCode();
        return result;
    }

    /**
     * Record a password change for a user. Does not modify users current hashed password.
     *
     * @param oldPasswordHash HASHED version of the users previous password.
     */
    public void addPasswordChange(String oldPasswordHash) {
        if(hashedPassword == null) throw new IllegalStateException("Cannot set password history before user has a hashed password.");
        if(hashedPassword.equals(oldPasswordHash)) throw new IllegalArgumentException("Cannot add a password change record for users current password.");

        final PasswordChangeRecord passChangeRecord = new PasswordChangeRecord(
                this, System.currentTimeMillis(), oldPasswordHash);
        this.passwordChangesHistory.add(passChangeRecord);
        this.changePassword = false;
    }

    @Column(name="properties",length=Integer.MAX_VALUE)
    @Lob
    public String getXmlProperties() {
        if ( xmlProperties == null ) {
            Map<String, String> properties = this.properties;
            if ( properties == null ) return null;
            PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
            try {
                XMLEncoder xe = new XMLEncoder(new NonCloseableOutputStream(baos));
                xe.writeObject(properties);
                xe.close();
                xmlProperties = baos.toString(PROPERTIES_ENCODING);
            } finally {
                baos.close();
            }
        }
        return xmlProperties;
    }

    public void setXmlProperties( final String xml ) {
        if (xml != null && xml.equals(xmlProperties)) return;
        this.xmlProperties = xml;
        if ( xml != null && xml.length() > 0 ) {
            XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
            //noinspection unchecked
            this.properties = (Map<String, String>)xd.readObject();
        }
    }

    public String getProperty( final String propertyName ) {
        String propertyValue = null;

        Map<String,String> properties = this.properties;
        if (properties != null) {
            propertyValue = properties.get(propertyName);
        }

        return propertyValue;
    }

    public void setProperty( final String propertyName, final String propertyValue ) {
        Map<String,String> properties = this.properties;
        if (properties == null) {
            properties = new HashMap<String, String>();
            this.properties = properties;
        }

        properties.put(propertyName, propertyValue);

        // invalidate cached properties
        xmlProperties = null;
    }
}
