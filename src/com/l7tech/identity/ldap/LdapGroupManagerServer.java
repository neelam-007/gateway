package com.l7tech.identity.ldap;

import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;

/**
 * List groups in a directory including the member users.
 * This version assumes usage of the posixGroup object class in the directory.
 * This member users are in the memberUid attributes.
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 13, 2003
 */
public class LdapGroupManagerServer extends AbstractLdapGroupManagerServer implements GroupManager {
    public LdapGroupManagerServer(IdentityProviderConfig config) {
        super(config);
    }

    protected String groupMemberToLogin( String member ) {
        return member;
    }

    protected String doGetGroupMembershipFilter( LdapUser user ) {
        String output = "(|";
        String[] memberAttrs = _constants.groupMemberAttribute();
        for (int i = 0; i < memberAttrs.length; i++) {
            // could refer either to cn or dn
            output +=       "(" + memberAttrs[i] + "=" + user.getCn() + ")";
            output +=       "(" + memberAttrs[i] + "=" + user.getDn() + ")";
        }
        output +=       ")";
        return output;
    }

    protected AbstractLdapConstants getConstants() {
        return _constants;
    }

    protected User getUserFromGroupMember(String member) throws FindException {
        // member could refer to either the cn or the dn value
        if (member.indexOf('=') >= 0) {
            return getUserManager().findByPrimaryKey(member);
        }
        else return getUserManager().findByLogin(member);
    }


    protected LdapConstants _constants = new LdapConstants();
}
