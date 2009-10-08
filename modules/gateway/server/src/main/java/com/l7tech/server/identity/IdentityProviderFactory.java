/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * This factory caches identity providers!
 *
 * @author alex
 */
public class IdentityProviderFactory implements ApplicationContextAware, ApplicationListener, PropertyChangeListener {
    private ApplicationContext springContext;
    private final IdentityProviderConfigManager identityProviderConfigManager;

    public IdentityProviderFactory(final IdentityProviderConfigManager identityProviderConfigManager) {
        this.identityProviderConfigManager = identityProviderConfigManager;     
    }

    public Collection<IdentityProvider> findAllIdentityProviders() throws FindException {
        List<IdentityProvider> providers = new ArrayList<IdentityProvider>();
        IdentityProviderConfigManager configManager = getIdentityProviderConfigurationManager();
        Iterator i = configManager.findAllHeaders().iterator();
        EntityHeader header;
        while (i.hasNext()) {
            header = (EntityHeader)i.next();
            IdentityProvider provider = getProvider(header.getOid());
            if (provider != null) providers.add(provider);
        }
        return Collections.unmodifiableList(providers);
    }

    /**
     * Returns the {@link IdentityProvider} corresponding to the specified ID.
     * <p/>
     * If possible it will be a cached version, but in all cases it will be up-to-date
     * with respect to the database, because the version is always checked.
     *
     * @param idpOid the OID of the IdentityProviderConfig record
     * @return the IdentityProvider, or null if it's not in the database (either it was deleted or never existed)
     * @throws FindException if there is problem loading information from the database
     */
    public IdentityProvider getProvider(long idpOid) throws FindException {

        IdentityProvider cachedProvider = providers.get(idpOid);
        if (cachedProvider != null)
            return cachedProvider;

        synchronized (this) {
            cachedProvider = providers.get(idpOid);
            if (cachedProvider == null) {
                IdentityProviderConfigManager configManager = getIdentityProviderConfigurationManager();
                IdentityProviderConfig config = configManager.findByPrimaryKey(idpOid);
                if (config == null) {
                    return null;
                }

                try {
                    cachedProvider = makeProvider(config, true);
                } catch (InvalidIdProviderCfgException e) {
                    final String msg = "Can't initialize an identity cachedProvider with type " + config.type();
                    logger.log(Level.SEVERE, msg, e);
                    throw new RuntimeException(msg, e);
                }

                providers.put(idpOid, cachedProvider);
            }
        }

        return cachedProvider;
    }


    /**
     * Allows the administrator to test the validity of a new IPC before saving
     * it.
     *
     * If the IPC is not valid an InvalidIdProviderCfgException is thrown
     *
     * @param identityProviderConfig the new config object (not yet saved)
     */
    public void test(final IdentityProviderConfig identityProviderConfig) throws InvalidIdProviderCfgException {
        IdentityProvider provider = makeProvider(identityProviderConfig, false);
        provider.test(false);
    }

    /**
     * Creates a new IdentityProvider of the correct type indicated by the specified
     * {@link IdentityProviderConfig} and initializes it.
     * <p/>
     * Call {@link #getProvider(long)} for runtime use, it has a cache.
     *
     * @param config the configuration to intialize the provider with.
     * @param start true to start provider maintenance tasks
     * @return the newly-initialized IdentityProvider
     * @throws InvalidIdProviderCfgException if the specified configuration cannot be used to construct an
     *                                       IdentityProvider. Call {@link Throwable#getCause()} to find out why!
     */
    @SuppressWarnings({ "unchecked" })
    private IdentityProvider makeProvider( final IdentityProviderConfig config, final boolean start ) throws InvalidIdProviderCfgException {
        String classname = config.type().getClassname();

        // locate factory for type (class)
        IdentityProviderFactorySpi factorySpi = null;
        Map<String,IdentityProviderFactorySpi> factories = springContext.getBeansOfType(IdentityProviderFactorySpi.class);
        for ( Map.Entry<String,IdentityProviderFactorySpi> entry : factories.entrySet() ) {
            String name = entry.getKey();
            IdentityProviderFactorySpi factory = entry.getValue();

            if ( classname.equals(factory.getClassname()) ) {
                logger.log(Level.FINE, "Using identity provider factory ''{0}'' for type ''{1}''.", new String[]{name, classname});
                factorySpi = factory;
                break;
            }
        }

        if ( factorySpi == null ) {
            throw new InvalidIdProviderCfgException("Could not find factory for identity provider type '"+classname+"'.");                                                              
        }

        try {
            return factorySpi.createIdentityProvider( config, start );
        } catch (InvalidIdProviderCfgException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidIdProviderCfgException(e);
        }
    }

    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
        this.springContext = applicationContext;
    }

    /**
     * Avoid too many calls to getBean 
     */
    private IdentityProviderConfigManager getIdentityProviderConfigurationManager() {
        return identityProviderConfigManager;
    }

    @Override
    public void onApplicationEvent(final ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent iev = (EntityInvalidationEvent)event;
            if (IdentityProviderConfig.class.isAssignableFrom(iev.getEntityClass())) {
                // Throw them out of the cache so they get reloaded next time they are needed
                long[] oids = iev.getEntityIds();
                for (long oid : oids) {
                    destroyProvider(oid);
                    providers.remove(oid);
                }
            }
        }
    }

    private void destroyProvider(long oid) {
        IdentityProvider x = providers.get(oid);
        if (x instanceof DisposableBean) {
            DisposableBean disposableBean = (DisposableBean) x;
            try {
                disposableBean.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to destroy the identity provider " + oid, e);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Set<Long> keys = providers.keySet();
        for (Long key : keys) {
            IdentityProvider provider = providers.get(key);
            if (provider != null && provider instanceof PropertyChangeListener) {
                PropertyChangeListener listener = (PropertyChangeListener) provider;
                listener.propertyChange(evt);
            }
        }
    }

    // note these need to be singletons so that they can be invalidates in case of deletion
    private static Map<Long, IdentityProvider> providers = new ConcurrentHashMap<Long, IdentityProvider>();

    public static final int MAX_AGE = 60 * 1000;

    private static final Logger logger = Logger.getLogger(IdentityProviderFactory.class.getName());
}
