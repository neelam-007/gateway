/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.msad;

import com.l7tech.identity.*;
import com.l7tech.identity.ldap.AbstractLdapConstants;
import com.l7tech.identity.ldap.AbstractLdapGroupManagerServer;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.objectmodel.FindException;


/**
 * @author alex
 * @version $Revision$
 */
public class MsadGroupManagerServer extends AbstractLdapGroupManagerServer implements GroupManager {
    public MsadGroupManagerServer( IdentityProviderConfig config ) {
        super( config );
    }

    protected String doGetGroupMembershipFilter( LdapUser user) {
        StringBuffer filter = new StringBuffer();
        filter.append( "(&(|" );
        String[] objclasses = _constants.groupObjectClass();
        for (int i = 0; i < objclasses.length; i++) {
            filter.append( "(objectClass=" + objclasses[i] + ")");
        }
        filter.append( ")" );
        filter.append( "(|" );
        String[] memberAttrs = _constants.groupMemberAttribute();
        for (int i = 0; i < memberAttrs.length; i++) {
            filter.append("(" + memberAttrs[i] + "=" + user.getDn() + ")");
        }
        filter.append( ")" );
        filter.append( ")" );
        return filter.toString();
    }

    protected AbstractLdapConstants getConstants() {
        return _constants;
    }

    protected User getUserFromGroupMember( String member ) throws FindException {
        return getUserManager().findByPrimaryKey( member );
    }

    private MsadConstants _constants = new MsadConstants();
}
