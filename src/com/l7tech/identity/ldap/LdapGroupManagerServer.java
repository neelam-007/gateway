package com.l7tech.identity.ldap;

import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 * List groups in a directory including the member users.
 * This version assumes usage of the posixGroup object class in the directory.
 * This member users are in the memberUid attributes.
 *
 */
public class LdapGroupManagerServer extends AbstractLdapGroupManagerServer implements GroupManager {
    public LdapGroupManagerServer(IdentityProviderConfig config) {
        super(config);
    }

    protected String groupMemberToLogin( String member ) {
        return member;
    }

    protected String doGetGroupMembershipFilter( LdapUser user ) {
        return "(" + _constants.groupMemberAttribute() + "=" + user.getCn() + ")";
    }

    protected AbstractLdapConstants getConstants() {
        return _constants;
    }

    protected User getUserFromGroupMember(String member) throws FindException {
        return getUserManager().findByLogin( member );
    }


    protected LdapConstants _constants = new LdapConstants();
}
