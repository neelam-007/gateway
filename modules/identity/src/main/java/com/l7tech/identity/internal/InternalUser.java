package com.l7tech.identity.internal;

import com.l7tech.util.HexUtils;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.policy.assertion.credential.http.HttpDigest;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Proxy;

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
    private long expiration = -1;
    protected String hashedPassword;
    private List<PasswordChangeRecord> passwordChangesHistory;
    private long passwordExpiry;
    private boolean changePassword;

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
    }

    public void setHashedPassword(String password) {
        this.hashedPassword = password;
    }

    @Column(name="password", nullable=false, length=32)
    public String getHashedPassword() {
        return hashedPassword;
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
               hashedPassword != null ? hashedPassword.equals(that.hashedPassword) : that.hashedPassword == null;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (hashedPassword != null ? hashedPassword.hashCode() : 0);
        result = 31 * result + (int) (expiration ^ (expiration >>> 32));
        result = 31 * result + (int) (passwordExpiry ^ (passwordExpiry >>> 32));
        result = 31 * result + (passwordChangesHistory != null ? passwordChangesHistory.hashCode() : 0);
        result = 31 * result + Boolean.valueOf(changePassword).hashCode();
        return result;
    }

    @Transient
    public void setCleartextPassword(String newPassword) throws InvalidPasswordException {
        if (newPassword == null) throw new InvalidPasswordException("Empty password is not valid");
        if (newPassword.length() < 6) throw new InvalidPasswordException("Password must be no shorter than 6 characters");
        if (newPassword.length() > 32) throw new InvalidPasswordException("Password must be no longer than 32 characters");

        if (login == null) throw new IllegalStateException("login must be set prior to encoding the password");
        this.hashedPassword = HexUtils.encodePasswd(login, newPassword, HttpDigest.REALM);
    }

    public void setPasswordChanges(long passwordChangeTime, String newPassword) {
        PasswordChangeRecord passChangeRecord = new PasswordChangeRecord();
        passChangeRecord.setInternalUser(this);
        passChangeRecord.setLastChanged(passwordChangeTime);
        passChangeRecord.setPrevHashedPassword(this.getHashedPassword());
        this.passwordChangesHistory.add(passChangeRecord);
        this.hashedPassword = HexUtils.encodePasswd(login, newPassword, HttpDigest.REALM);
        this.changePassword = false;
    }
}
