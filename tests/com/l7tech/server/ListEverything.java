/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.service.ServiceAdmin;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;

/**
 * @author alex
 * @version $Revision$
 */
public class ListEverything extends SsgAdminSession {


    private ListEverything( String[] args ) throws Exception {
        super( args );
    }

    public static void main(final String[] args) throws Exception {
        Subject.doAsPrivileged(new Subject(), new PrivilegedAction() {
            public Object run() {
                try {
                    ListEverything me = new ListEverything(args);
                    me.doSomething();
                    System.exit(0);
                } catch ( Exception e ) {
                    e.printStackTrace();
                    System.exit(1);
                }
                return null;
            }
        }, null);
    }

    protected Object doSomething() throws Exception {
        ServiceAdmin serviceAdmin = getAdminContext().getServiceAdmin();
        EntityHeader[] serviceHeaders = serviceAdmin.findAllPublishedServices();
        for (int i = 0; i < serviceHeaders.length; i++) {
            EntityHeader header = serviceHeaders[i];
            System.out.println( header );
        }

        IdentityProviderConfig internalProviderConfig = null;
        IdentityAdmin identityAdmin = getAdminContext().getIdentityAdmin();
        EntityHeader[] providerHeaders = identityAdmin.findAllIdentityProviderConfig();
        for (int i = 0; i < providerHeaders.length; i++) {
            EntityHeader header = providerHeaders[i];

            if ( header.getType() == EntityType.ID_PROVIDER_CONFIG ) {
                long oid = header.getOid();
                if ( oid == IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID ) {
                    internalProviderConfig = identityAdmin.findIdentityProviderConfigByID(oid);
                }
            }
            System.out.println( header );
        }

        if ( internalProviderConfig == null ) throw new IllegalStateException( "No internal provider!" );

        EntityHeader[] userHeaders = identityAdmin.findAllUsers(internalProviderConfig.getOid());
        for (int i = 0; i < userHeaders.length; i++) {
            EntityHeader userHeader = userHeaders[i];
            System.out.println(userHeader);
        }

        EntityHeader[] groupHeaders = identityAdmin.findAllGroups(internalProviderConfig.getOid());
        for (int i = 0; i < groupHeaders.length; i++) {
            EntityHeader groupHeader = groupHeaders[i];
            System.out.println(groupHeader);
        }
        return null;
    }
}
