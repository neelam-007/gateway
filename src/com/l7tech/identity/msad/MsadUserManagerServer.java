/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.msad;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.ldap.AbstractLdapConstants;
import com.l7tech.identity.ldap.AbstractLdapUserManagerServer;

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

    private MsadConstants _constants = new MsadConstants();
}
