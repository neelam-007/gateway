/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.misc.Locator;

import java.util.*;

/**
 * @author alex
 */
public class IdentityProviders {
    public Collection findAll() {
        IdentityProviderConfigManager configManager = (IdentityProviderConfigManager)Locator.getInstance().locate( IdentityProviderConfigManager.class );
        Iterator i = configManager.findAll().iterator();

        IdentityProviderConfig config;
        IdentityProviderType type;
        Class providerInterfaceClass;
        IdentityProvider provider;
        List providers = new ArrayList();
        while ( i.hasNext() ) {
            config = (IdentityProviderConfig)i.next();
            type = config.getType();

            try {
                providerInterfaceClass = Class.forName( type.getClassName() );
                provider = (IdentityProvider)Locator.getInstance().locate(providerInterfaceClass);
                provider.initialize( config );
                providers.add( provider );
            } catch ( ClassNotFoundException cnfe ) {
                throw new RuntimeException( "IdentityProviderType " + type.getOid() + " (" + type.getName() + ") has an incorrect className field: " + type.getClassName(), cnfe );
            }
        }

        return Collections.unmodifiableList( providers );
    }
}
