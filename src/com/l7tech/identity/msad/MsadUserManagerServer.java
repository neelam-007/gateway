/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.msad;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;
import com.l7tech.identity.ldap.AbstractLdapConstants;
import com.l7tech.identity.ldap.AbstractLdapUserManagerServer;
import com.l7tech.identity.ldap.LdapConfigSettings;

/**
 * @author alex
 * @version $Revision$
 */
public class MsadUserManagerServer extends AbstractLdapUserManagerServer implements UserManager {
    public MsadUserManagerServer( IdentityProviderConfig config ) {
        super( config );
    }

    protected AbstractLdapConstants getConstants() {
        return _constants;
    }

    protected String doGetGroupMembershipFilter(User user) {
        StringBuffer filter = new StringBuffer();
        filter.append( "(&(objectClass=" );
        filter.append( _constants.groupObjectClass() );
        filter.append( ")(" );
        filter.append( _constants.groupMemberAttribute() );
        filter.append( "=" );
        filter.append( user.getName() );
        filter.append( "))" );
        return filter.toString();
    }

    private MsadConstants _constants = new MsadConstants();
}
