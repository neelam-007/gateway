package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserManager;

/**
 * This manager class lists users in a ldap directory given a LdapIdentityProviderConfig object.
 * This manager does not support save, update or delete.
 *
 * This version assumes users are registered in inetOrgPerson objects. Login is "uid"
 * attribute, password is "userPassword" attribute, name is "cn", first name is "givenName",
 * last name is "sn".
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 13, 2003
 */
public class LdapUserManagerServer extends AbstractLdapUserManagerServer implements UserManager {
    public LdapUserManagerServer(IdentityProviderConfig config) {
        super(config);
    }

    public AbstractLdapConstants getConstants() {
        return _constants;
    }

    private LdapConstants _constants = new LdapConstants();
}
