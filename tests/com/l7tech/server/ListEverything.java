/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.service.ServiceAdmin;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author alex
 * @version $Revision$
 */
public class ListEverything extends SsgAdminSession {
    private ListEverything( String[] args ) throws Exception {
        super( args );
    }

    public static void main(String[] args) throws Exception {
        try {
            ListEverything me = new ListEverything(args);
            me.doIt();
            System.exit(0);
        } catch ( Exception e ) {
            System.exit(1);
        }
    }

    protected Object doSomething() throws Exception {
        ServiceAdmin serviceAdmin = getServiceAdmin();
        EntityHeader[] serviceHeaders = serviceAdmin.findAllPublishedServices();
        for (int i = 0; i < serviceHeaders.length; i++) {
            EntityHeader header = serviceHeaders[i];
            System.out.println( header );
        }

        IdentityProviderConfigManager ipc = getIdentityProviderConfigManager();

        IdentityProvider internalProvider = null;
        IdentityAdmin identityAdmin = getIdentityAdmin();
        EntityHeader[] providerHeaders = identityAdmin.findAllIdentityProviderConfig();
        for (int i = 0; i < providerHeaders.length; i++) {
            EntityHeader header = providerHeaders[i];

            if ( header.getType() == EntityType.ID_PROVIDER_CONFIG ) {
                long oid = header.getOid();
                if ( oid == IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID ) {
                    internalProvider = ipc.getIdentityProvider( oid );
                }
            }
            System.out.println( header );
        }

        if ( internalProvider == null ) throw new IllegalStateException( "No internal provider!" );

        UserManager userManager = internalProvider.getUserManager();
        Collection userHeaders = userManager.findAllHeaders();
        for (Iterator i = userHeaders.iterator(); i.hasNext();) {
            EntityHeader header = (EntityHeader) i.next();
            System.out.println( header );
        }

        GroupManager groupManager = internalProvider.getGroupManager();
        Collection groupHeaders = groupManager.findAllHeaders();
        for (Iterator i = groupHeaders.iterator(); i.hasNext();) {
            EntityHeader header = (EntityHeader) i.next();
            System.out.println( header );
        }

        return null;
    }
}
