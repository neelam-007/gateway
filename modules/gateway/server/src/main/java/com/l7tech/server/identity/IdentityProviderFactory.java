package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.server.util.PostStartupApplicationListener;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This factory caches identity providers!
 *
 * @author alex
 */
public class IdentityProviderFactory implements ApplicationContextAware, PostStartupApplicationListener {

    // - PUBLIC

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
            IdentityProvider provider = getProvider(header.getGoid());
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
    public IdentityProvider getProvider(Goid idpOid) throws FindException {

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
     * @param testUser  example username if required by this provider type, or null
     * @param testPassword example password if required by this provider type, or null
     * @throws InvalidIdProviderCfgException if the test fails
     */
    public void test(final IdentityProviderConfig identityProviderConfig, String testUser, char[] testPassword) throws InvalidIdProviderCfgException {
        IdentityProvider provider = makeProvider(identityProviderConfig, false);
        provider.test(false, testUser, testPassword);
    }

    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
        this.springContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(final ApplicationEvent event) {
        if (event instanceof GoidEntityInvalidationEvent) {
            GoidEntityInvalidationEvent iev = (GoidEntityInvalidationEvent)event;
            if (IdentityProviderConfig.class.isAssignableFrom(iev.getEntityClass())) {
                // Throw them out of the cache so they get reloaded next time they are needed
                Goid[] oids = iev.getEntityIds();
                for (Goid oid : oids) {
                    destroyProvider(oid);
                    providers.remove(oid);
                }
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(IdentityProviderFactory.class.getName());

    // note these need to be singletons so that they can be invalidates in case of deletion
    private static Map<Goid, IdentityProvider> providers = new ConcurrentHashMap<Goid, IdentityProvider>();

    private final IdentityProviderConfigManager identityProviderConfigManager;
    private ApplicationContext springContext;
    private Map<String,IdentityProviderFactorySpi> factories;

    /**
     * Avoid too many calls to getBean
     */
    private IdentityProviderConfigManager getIdentityProviderConfigurationManager() {
        return identityProviderConfigManager;
    }

    @SuppressWarnings({ "unchecked" })
    private Map<String,IdentityProviderFactorySpi> getIdentityProviderFactories() {
        Map<String,IdentityProviderFactorySpi> factories = this.factories;

        if ( factories == null ) {
            factories = this.factories = springContext.getBeansOfType(IdentityProviderFactorySpi.class);
        }         

        return factories;
    }

    private IdentityProviderFactorySpi getIdentityProviderFactory( final IdentityProviderConfig config ) throws InvalidIdProviderCfgException {
        final String classname = config.type().getClassname();

        IdentityProviderFactorySpi factorySpi = null;
        Map<String,IdentityProviderFactorySpi> factories = getIdentityProviderFactories();
        for ( Map.Entry<String,IdentityProviderFactorySpi> entry : factories.entrySet() ) {
            String name = entry.getKey();
            IdentityProviderFactorySpi factory = entry.getValue();

            if ( classname.equals(factory.getClassname()) ) {
                logger.log( Level.FINE, "Using identity provider factory ''{0}'' for type ''{1}''.", new String[]{name, classname});
                factorySpi = factory;
                break;
            }
        }

        if ( factorySpi == null ) {
            throw new InvalidIdProviderCfgException("Could not find factory for identity provider type '"+classname+"'.");
        }

        return factorySpi;
    }

    /**
     * Creates a new IdentityProvider of the correct type indicated by the specified
     * {@link IdentityProviderConfig} and initializes it.
     * <p/>
     * Call {@link #getProvider(com.l7tech.objectmodel.Goid)} for runtime use, it has a cache.
     *
     * @param config the configuration to intialize the provider with.
     * @param start true to start provider maintenance tasks
     * @return the newly-initialized IdentityProvider
     * @throws InvalidIdProviderCfgException if the specified configuration cannot be used to construct an
     *                                       IdentityProvider. Call {@link Throwable#getCause()} to find out why!
     */
    @SuppressWarnings({ "unchecked" })
    private IdentityProvider makeProvider( final IdentityProviderConfig config, final boolean start ) throws InvalidIdProviderCfgException {
        IdentityProviderFactorySpi factorySpi = getIdentityProviderFactory( config );

        try {
            return factorySpi.createIdentityProvider( config, start );
        } catch (InvalidIdProviderCfgException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidIdProviderCfgException(e);
        }
    }

    private void destroyProvider( final Goid oid ) {
        IdentityProvider identityProvider = providers.get(oid);
        if ( identityProvider != null ) {
            try {
                getIdentityProviderFactory(identityProvider.getConfig()).destroyIdentityProvider(identityProvider);
            } catch ( InvalidIdProviderCfgException e ) {
                logger.warning( "Could not find factory to destroy identity provider '"+identityProvider.getConfig().type().getClassname()+"'." );
            }
        }
    }
}
