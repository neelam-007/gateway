/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.fed;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.identity.internal.InternalUserManagerServer;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedUserManager extends InternalUserManagerServer {
    public FederatedUserManager( IdentityProvider provider ) {
        super(provider);
    }

    protected void preDelete( InternalUser user ) throws FindException, DeleteException {
        // No pre-processing required - don't call super
    }

    public Class getImpClass() {
        return FederatedUser.class;
    }

    public Class getInterfaceClass() {
        return User.class;
    }

    public String getTableName() {
        return "fed_user";
    }
}
