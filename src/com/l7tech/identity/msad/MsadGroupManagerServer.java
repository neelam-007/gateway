/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.msad;

import com.l7tech.identity.*;
import com.l7tech.identity.ldap.AbstractLdapConstants;
import com.l7tech.identity.ldap.AbstractLdapGroupManagerServer;
import com.l7tech.objectmodel.FindException;

import java.util.StringTokenizer;

/**
 * @author alex
 * @version $Revision$
 */
public class MsadGroupManagerServer extends AbstractLdapGroupManagerServer implements GroupManager {
    public MsadGroupManagerServer( IdentityProviderConfig config ) {
        super( config );
    }

    protected String groupMemberToLogin( String member ) {
        StringTokenizer stok = new StringTokenizer( member, "," );
        String token;
        String attr = _constants.groupMemberAttribute();
        String name, value;
        while ( stok.hasMoreTokens() ) {
            token = stok.nextToken();
            int epos = token.indexOf("=");
            if ( epos >= 0 ) {
                name = token.substring(0,epos);
                value = token.substring(epos+1);

                if ( name.equalsIgnoreCase( attr ) ) return value;
            }
        }

        return "";
    }

    protected AbstractLdapConstants getConstants() {
        return _constants;
    }

    protected User getUserFromGroupMember( String member ) throws FindException {
        return getUserManager().findByPrimaryKey( member );
    }

    private MsadConstants _constants = new MsadConstants();
}
