package com.l7tech.identity.internal;

import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.InvalidPasswordException;

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
public class InternalUser extends PersistentUser {
    public InternalUser( UserBean bean ) {
        super(bean);
    }

    public InternalUser() {
        super();
        bean.setProviderId(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
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
        try {
            setPassword(imp.getPassword());
        } catch (InvalidPasswordException e) {
            throw new RuntimeException(e); // cannot happen
        }
        setSubjectDn( imp.getSubjectDn() );
    }

    public void setPassword(String password) throws InvalidPasswordException {
        setPassword(password, false);
    }

    /**
     * Set the password for this user
     * 
     * @param password the password (clear or encoded)
     * @param hintIsClear true if you want to communicate that the password is in clear text
     * @throws InvalidPasswordException
     */
    public void setPassword(String password, boolean hintIsClear) throws InvalidPasswordException {
        if (password == null) throw new InvalidPasswordException("Empty password is not valid");
        if (hintIsClear || !HexUtils.containsOnlyHex(password)) {
            if (password.length() < 6) throw new InvalidPasswordException("Password must be at least 6 " +
                                                                          "characters long");
            if (password.length() > 32) throw new InvalidPasswordException("Password must be no longer " +
                                                                           "than 32 characters long");
        }
        bean.setPassword(password, hintIsClear);
    }

    /**
     * Account expiration.
     * @return -1 if not set (never expires) or the time in millis after which this account should be considered expired.
     */
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

    private long expiration = -1;
}
