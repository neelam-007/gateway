package com.l7tech.identity.internal;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.UserBean;

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
}
