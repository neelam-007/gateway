package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;


/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 * This manager class lists users in a ldap directory given a LdapIdentityProviderConfig object
 * This manager does not support save, update or delete.
 *
 * This version assumes users are registered in inetOrgPerson objects. Login is "uid"
 * attribute, password is "userPassword" attribute, name is "cn", first name is "givenName",
 * last name is "sn".
 */
public class LdapUserManagerServer extends AbstractLdapUserManagerServer implements UserManager {
    public LdapUserManagerServer(IdentityProviderConfig config) {
        super(config);
    }

    public AbstractLdapConstants getConstants() {
        return _constants;
    }

    protected String doGetGroupMembershipFilter(User user) {
        return "(" + _constants.groupMemberAttribute() + "=" + user.getLogin() + ")";
    }

    private LdapConstants _constants = new LdapConstants();
}
