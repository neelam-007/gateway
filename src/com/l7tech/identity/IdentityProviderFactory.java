/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.ldap.LdapIdentityProviderServer;
import com.l7tech.identity.internal.imp.InternalIdentityProviderImp;

import java.util.*;

/**
 * @author alex
 */
public class IdentityProviderFactory {
    public static Collection findAllIdentityProviders(IdentityProviderConfigManager manager) throws FindException {
        List providers = new ArrayList();
        Iterator i = manager.findAll().iterator();
        IdentityProviderConfig config;
        while (i.hasNext()) {
            config = (IdentityProviderConfig)i.next();
            providers.add(makeProvider(config));
        }
        return Collections.unmodifiableList(providers);
    }

    public static IdentityProvider makeProvider(IdentityProviderConfig config) {
        IdentityProvider provider;
        try {
            if (config.type() == IdentityProviderType.LDAP) provider = new LdapIdentityProviderServer();
            else if (config.type() == IdentityProviderType.INTERNAL) provider = new InternalIdentityProviderImp();
            else throw new RuntimeException("no provider type specified.");
            provider.initialize(config);
            return provider;
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't locate an implementation for IdentityProviderConfig " + config.getName() + " (#" + config.getOid() + "): " + e.toString());
        }
    }
}
