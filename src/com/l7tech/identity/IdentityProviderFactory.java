/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.ldap.LdapIdentityProviderServer;
import com.l7tech.identity.internal.InternalIdentityProviderServer;
import com.l7tech.identity.msad.MsadIdentityProviderServer;

import java.util.*;

/**
 * This factory caches identity providers!
 *
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

    /**
     * call this because a config object is being updated or deleted and you want to inform the cache that
     * correcponding id provider should be removed from cache
     */
    public synchronized static void dropProvider(IdentityProviderConfig config) {
        if (providers == null) return;
        Long key = new Long(config.getOid());
        IdentityProvider existingProvider = (IdentityProvider)providers.get(key);
        if (existingProvider != null) {
            providers.remove(key);
        }
    }

    public synchronized static IdentityProvider makeProvider(IdentityProviderConfig config) {
        try {
            if (providers == null) providers = new HashMap();
            IdentityProvider existingProvider = (IdentityProvider)providers.get(new Long(config.getOid()));
            if (existingProvider == null) {
                if (config.type() == IdentityProviderType.LDAP)
                    existingProvider = new LdapIdentityProviderServer();
                else if (config.type() == IdentityProviderType.INTERNAL)
                    existingProvider = new InternalIdentityProviderServer();
                else if ( config.type() == IdentityProviderType.MSAD)
                    existingProvider = new MsadIdentityProviderServer();
                else throw new RuntimeException("no provider type specified.");
                existingProvider.initialize(config);
                providers.put(new Long(config.getOid()), existingProvider);
            }
            return existingProvider;
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't locate an implementation for IdentityProviderConfig " + config.getName() + " (#" + config.getOid() + "): " + e.toString());
        }
    }

    // note these need to be singletons so that they can be invalidates in case of deletion
    private static HashMap providers = null;

    public static final int MAX_AGE = 60 * 1000;
}
