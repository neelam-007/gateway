/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Locator;

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
        IdentityProviderType type = config.getType();
        String className = type.getClassName();
        IdentityProvider provider;

        try {
            provider = (IdentityProvider)Class.forName(className).newInstance();
            provider.initialize(config);
            return provider;
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't locate an implementation of " + className + " for IdentityProviderConfig " + config.getName() + " (#" + config.getOid() + "): " + e.toString());
        }
    }
}
