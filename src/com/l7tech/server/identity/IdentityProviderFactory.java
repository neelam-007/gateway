/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This factory caches identity providers!
 *
 * @author alex
 */
public class IdentityProviderFactory extends ApplicationObjectSupport {

    public Collection findAllIdentityProviders(IdentityProviderConfigManager manager) throws FindException {
        List providers = new ArrayList();
        Iterator i = manager.findAllHeaders().iterator();
        EntityHeader header;
        while (i.hasNext()) {
            header = (EntityHeader)i.next();
            IdentityProvider provider = getProvider(header.getOid());
            if ( provider != null ) providers.add(provider);
        }
        return Collections.unmodifiableList(providers);
    }

    /**
     * call this because a config object is being updated or deleted and you want to inform the cache that
     * corresponding id provider should be removed from cache
     */
    public synchronized void dropProvider(IdentityProviderConfig config) {
        if (providers == null) return;
        Long key = new Long(config.getOid());
        IdentityProvider existingProvider = (IdentityProvider)providers.get(key);
        if (existingProvider != null) {
            providers.remove(key);
        }
    }

    /**
     * Returns the {@link IdentityProvider} corresponding to the specified ID.
     *
     * If possible it will be a cached version, but in all cases it will be up-to-date
     * with respect to the database, because the version is always checked.
     *
     * @param identityProviderOid the OID of the IdentityProviderConfig record ({@link com.l7tech.server.identity.IdProvConfManagerServer#INTERNALPROVIDER_SPECIAL_OID} for the Internal ID provider)
     * @return the IdentityProvider, or null if it's not in the database (either it was deleted or never existed)
     * @throws FindException
     */
    public synchronized IdentityProvider getProvider(long identityProviderOid) throws FindException {
        IdentityProviderConfigManager configManager = (IdentityProviderConfigManager)getApplicationContext().getBean("identityProviderConfigManager");
        Long oid = new Long(identityProviderOid);

        IdentityProvider cachedProvider = (IdentityProvider)providers.get(oid);
        if ( cachedProvider != null && identityProviderOid != IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID ) {
            Integer dbVersion = configManager.getVersion(identityProviderOid);
            if ( dbVersion == null ) {
                // It's been deleted
                providers.remove(oid);
                return null;
            } else if ( dbVersion.longValue() != cachedProvider.getConfig().getVersion() ) {
                // It's old, force a reload
                cachedProvider = null;
            }
        }

        if ( cachedProvider == null ) {
            IdentityProviderConfig config = configManager.findByPrimaryKey(oid.longValue());
            if ( config == null ) {
                return null;
            }

            try {
                cachedProvider = makeProvider(config);
            } catch ( InvalidIdProviderCfgException e ) {
                final String msg = "Can't initialize an identity cachedProvider with type " + config.type();
                logger.log(Level.SEVERE, msg, e);
                throw new RuntimeException(msg, e);
            }

            providers.put(oid,cachedProvider);
        }

        return cachedProvider;
    }

    /**
     * Creates a new IdentityProvider of the correct type indicated by the specified
     * {@link IdentityProviderConfig} and initializes it.
     *
     * Uses reflection, so don't call often!  Call {@link #getProvider(long)} for runtime use, it has a cache.
     *
     * @param config the configuration to intialize the provider with.
     * @return the newly-initialized IdentityProvider
     * @throws InvalidIdProviderCfgException if the specified configuration cannot be used to construct an
     * IdentityProvider. Call {@link Throwable#getCause()} to find out why!
     */
    public IdentityProvider makeProvider( IdentityProviderConfig config)
      throws InvalidIdProviderCfgException {
        IdentityProvider cachedProvider;
        String classname = config.type().getClassname();
        try {
            Class providerClass = Class.forName(classname);
            Constructor ctor = providerClass.getConstructor(new Class[] { IdentityProviderConfig.class, ApplicationContext.class });
            cachedProvider = (IdentityProvider)ctor.newInstance(new Object[] { config, getApplicationContext() });
        } catch ( Exception e ) {
            throw new InvalidIdProviderCfgException(e);
        }
        return cachedProvider;
    }

    // note these need to be singletons so that they can be invalidates in case of deletion
    private static Map providers = new HashMap();

    public static final int MAX_AGE = 60 * 1000;
    private static final Logger logger = Logger.getLogger(IdentityProviderFactory.class.getName());
}
