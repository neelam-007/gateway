package com.l7tech.console;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.util.SyspropUtil;

/**
 * @author alex
 */
public class ListEverything extends SsgAdminSession {


    private ListEverything( String[] args ) throws Exception {
        super( args );
    }

    public static void main(final String[] args) throws Exception {
        try {
            SyspropUtil.setProperty( "com.l7tech.console.suppressVersionCheck", "true" );
            ListEverything me = new ListEverything(args);
            me.doSomething();
            System.exit(0);
        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected Object doSomething() throws Exception {
        ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
        EntityHeader[] serviceHeaders = serviceAdmin.findAllPublishedServices();
        for (EntityHeader header : serviceHeaders) {
            System.out.println(header);
        }

        IdentityProviderConfig internalProviderConfig = null;
        IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
        EntityHeader[] providerHeaders = identityAdmin.findAllIdentityProviderConfig();
        for (EntityHeader header : providerHeaders) {
            if (header.getType() == EntityType.ID_PROVIDER_CONFIG) {
                Goid oid = header.getGoid();
                if (oid.equals(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID)) {
                    internalProviderConfig = identityAdmin.findIdentityProviderConfigByID(oid);
                }
            }
            System.out.println(header);
        }

        if ( internalProviderConfig == null ) throw new IllegalStateException( "No internal provider!" );

        EntityHeaderSet<IdentityHeader> userHeaders = identityAdmin.findAllUsers(internalProviderConfig.getGoid());
        for(EntityHeader userHeader : userHeaders) {
            System.out.println(userHeader);
        }

        EntityHeaderSet<IdentityHeader> groupHeaders = identityAdmin.findAllGroups(internalProviderConfig.getGoid());
        for(EntityHeader groupHeader : groupHeaders) {
            System.out.println(groupHeader);
        }
        return null;
    }
}
