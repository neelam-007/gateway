/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.identity.internal.InternalIdentityProviderServer;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;

import java.util.*;

/**
 * This factory caches identity providers!
 *
 * @author alex
 */
public class IdentityProviderFactory {
    public static Collection findAllIdentityProviders(IdentityProviderConfigManager manager) throws FindException {
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

    /**
     * Returns the {@link IdentityProvider} corresponding to the specified ID.  If possible it will be a cached version,
     * but in all cases it will be up-to-date with respect to the database.
     *
     * @param identityProviderOid the OID of the IdentityProviderConfig record ({@link com.l7tech.server.identity.IdProvConfManagerServer#INTERNALPROVIDER_SPECIAL_OID} for the Internal ID provider)
     * @return the IdentityProvider, or null if it's not in the database (either it was deleted or never existed)
     * @throws FindException
     */
    public synchronized static IdentityProvider getProvider(long identityProviderOid) throws FindException {
        IdProvConfManagerServer configManager = (IdProvConfManagerServer)Locator.getDefault().lookup( IdentityProviderConfigManager.class );
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
            if ( config == null ) throw new FindException("Couldn't find IdentityProviderConfig with oid=" + oid );

            if ( oid.longValue() == IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID ) {
                cachedProvider = new InternalIdentityProviderServer();
            } else {
                if ( config.type() == IdentityProviderType.LDAP ) {
                    cachedProvider = new LdapIdentityProvider();
                } else {
                    throw new RuntimeException( "Can't initialize an identity cachedProvider with type " + config.type() );
                }
            }
            cachedProvider.initialize(config);
            providers.put(oid,cachedProvider);
        }

        return cachedProvider;
    }

    // note these need to be singletons so that they can be invalidates in case of deletion
    private static Map providers = new HashMap();

    public static final int MAX_AGE = 60 * 1000;
}
