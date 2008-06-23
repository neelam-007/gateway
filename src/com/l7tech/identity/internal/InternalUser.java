package com.l7tech.identity.internal;

import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.InvalidPasswordException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * User from the internal identity provider.
 * Password property is stored as HEX(MD5(login:L7SSGDigestRealm:password)). If you pass a clear text passwd in
 * setPassword, this encoding will be done ofr you (provided that login was set before).
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
@Table(name="internal_user")
public class InternalUser extends PersistentUser {
    private long expiration = -1;
    protected String hashedPassword;

    public InternalUser() {
        this(null);
    }

    public InternalUser(String login) {
        super(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, login);
    }

    /**
     * allows to set all properties from another object
     */
    public void copyFrom( User objToCopy ) {
        InternalUser imp = (InternalUser)objToCopy;
        setOid(imp.getOid());
        setName(imp.getName());
        setProviderId(imp.getProviderId());
        setLogin(imp.getLogin());
        setDepartment(imp.getDepartment());
        setEmail(imp.getEmail());
        setFirstName(imp.getFirstName());
        setLastName(imp.getLastName());
        setExpiration(imp.getExpiration());
        setHashedPassword(imp.getHashedPassword());
        setSubjectDn( imp.getSubjectDn() );
    }

    public void setHashedPassword(String password) {
        this.hashedPassword = password;
    }

    @Column(name="password", nullable=false, length=32)
    public String getHashedPassword() {
        return hashedPassword;
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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        InternalUser that = (InternalUser) o;

        if (expiration != that.expiration) return false;
        if (hashedPassword != null ? !hashedPassword.equals(that.hashedPassword) : that.hashedPassword != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (hashedPassword != null ? hashedPassword.hashCode() : 0);
        result = 31 * result + (int) (expiration ^ (expiration >>> 32));
        return result;
    }

    public void setCleartextPassword(String newPassword) throws InvalidPasswordException {
        if (newPassword == null) throw new InvalidPasswordException("Empty password is not valid");
        if (newPassword.length() < 6) throw new InvalidPasswordException("Password must be no shorter than 6 characters");
        if (newPassword.length() > 32) throw new InvalidPasswordException("Password must be no longer than 32 characters");

        if (login == null) throw new IllegalStateException("login must be set prior to encoding the password");
        this.hashedPassword = HexUtils.encodePasswd(login, newPassword);
    }
}
