/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.identity.internal.InternalGroupManager;
import com.l7tech.server.identity.fed.FederatedIdentityProvider;
import com.l7tech.server.identity.fed.FederatedGroupManager;
import com.l7tech.server.identity.fed.FederatedUserManager;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.identity.ldap.LdapUserManager;
import com.l7tech.server.identity.ldap.LdapGroupManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This factory caches identity providers!
 *
 * @author alex
 */
public class IdentityProviderFactory extends ApplicationObjectSupport implements InitializingBean, BeanPostProcessor {
    private AbstractBeanFactory abstractBeanFactory;

    public Collection findAllIdentityProviders(IdentityProviderConfigManager manager) throws FindException {
        List providers = new ArrayList();
        Iterator i = manager.findAllHeaders().iterator();
        EntityHeader header;
        while (i.hasNext()) {
            header = (EntityHeader)i.next();
            IdentityProvider provider = getProvider(header.getOid());
            if (provider != null) providers.add(provider);
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
     * <p/>
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
        if (cachedProvider != null && identityProviderOid != IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID) {
            Integer dbVersion = configManager.getVersion(identityProviderOid);
            if (dbVersion == null) {
                // It's been deleted
                providers.remove(oid);
                return null;
            } else if (dbVersion.longValue() != cachedProvider.getConfig().getVersion()) {
                // It's old, force a reload
                cachedProvider = null;
            }
        }

        if (cachedProvider == null) {
            IdentityProviderConfig config = configManager.findByPrimaryKey(oid.longValue());
            if (config == null) {
                return null;
            }

            try {
                cachedProvider = makeProvider(config);
            } catch (InvalidIdProviderCfgException e) {
                final String msg = "Can't initialize an identity cachedProvider with type " + config.type();
                logger.log(Level.SEVERE, msg, e);
                throw new RuntimeException(msg, e);
            }

            providers.put(oid, cachedProvider);
        }

        return cachedProvider;
    }


    /**
     * Creates a new IdentityProvider of the correct type indicated by the specified
     * {@link IdentityProviderConfig} and initializes it.
     * <p/>
     * Call {@link #getProvider(long)} for runtime use, it has a cache.
     *
     * @param config the configuration to intialize the provider with.
     * @return the newly-initialized IdentityProvider
     * @throws InvalidIdProviderCfgException if the specified configuration cannot be used to construct an
     *                                       IdentityProvider. Call {@link Throwable#getCause()} to find out why!
     */
    public IdentityProvider makeProvider(IdentityProviderConfig config)
      throws InvalidIdProviderCfgException {
        String classname = config.type().getClassname();
        try {
            Class providerClass = Class.forName(classname);
            String name = getBeanName(providerClass);
            IdentityProvider provider = (IdentityProvider)abstractBeanFactory.getBean(name, new Object[]{config});
            return provider;
        } catch (Exception e) {
            throw new InvalidIdProviderCfgException(e);
        }
    }

    /**
     * Creates a new IdentityProvider of the correct type indicated by the specified
     * {@link IdentityProviderConfig} and initializes it. This is a factory method that
     * is registeree in Spring definition
     * <p/>
     * Call {@link #getProvider(long)} for runtime use, it has a cache.
     *
     * @param config the configuration to intialize the provider with.
     * @return the newly-initialized IdentityProvider
     * @throws InvalidIdProviderCfgException if the specified configuration cannot be used to construct an
     *                                       IdentityProvider. Call {@link Throwable#getCause()} to find out why!
     */
    public final IdentityProvider createProviderInstance(IdentityProviderConfig config)
      throws InvalidIdProviderCfgException {
        String classname = config.type().getClassname();
        try {
            Class providerClass = Class.forName(classname);
            Constructor ctor = providerClass.getConstructor(new Class[]{IdentityProviderConfig.class});
            return (IdentityProvider)ctor.newInstance(new Object[]{config});
        } catch (Exception e) {
            throw new InvalidIdProviderCfgException(e);
        }
    }

    /**
     * Creates a new manager instance described  with the class paramaeter for the provider passed.
     * The manager classes may be of {@link GroupManager} of {@link UserManager}. This is a factory
     * method that is registered with the bean definitions to suppport runtime constructor arguments.
     * <p/>
     *
     * @return the newly-initialized manager for the IdentityProvider
     * @throws InvalidIdProviderCfgException if the specified manager could not get onstructed
     */
    public final Object createManagerInstance(Class managerClass, IdentityProvider identityProvider)
      throws InvalidIdProviderCfgException {
        try {
            Constructor ctor = managerClass.getConstructor(new Class[]{IdentityProvider.class});
            return ctor.newInstance(new Object[]{identityProvider});
        } catch (Exception e) {
            throw new InvalidIdProviderCfgException(e);
        }
    }


    /**
     * Find the unique bean name in the spring bean definition repositiry for the class parameter.
     *
     * @param providerClass the class to lookup
     * @return the bean name
     * @throws NoSuchBeanDefinitionException if the bean for the given type could not be found
     * @throws IllegalStateException         if multiple bean definitions are found
     */
    private String getBeanName(Class providerClass) {
        String[] beanNames = getApplicationContext().getBeanDefinitionNames(providerClass);
        if (beanNames.length == 0) {
            throw new NoSuchBeanDefinitionException(providerClass,
              "Could not find the definition of the provider " + providerClass.getName());
        } else if (beanNames.length > 1) {
            throw new IllegalStateException("Ambiguous bean definitions for " + providerClass.getName() +
              "; bean names " + Arrays.asList(beanNames));
        }
        return beanNames[0];
    }


    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     * (and satisfied BeanFactoryAware and ApplicationContextAware).
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    public void afterPropertiesSet() throws Exception {
        ApplicationContext ctx = getApplicationContext();
        if (!(ctx instanceof AbstractApplicationContext)) {
            throw new IllegalStateException("application context is expected to be of '" + AbstractApplicationContext.class + "' received "
              + ctx.getClass());
        }
        AbstractApplicationContext abstractApplicationContext = (AbstractApplicationContext)ctx;
        ConfigurableListableBeanFactory bf = abstractApplicationContext.getBeanFactory();
        if (!(bf instanceof AbstractBeanFactory)) {
            throw new IllegalStateException("Bean Factory is expected to be of " + AbstractBeanFactory.class + "' received "
              + bf.getClass());
        }
        abstractBeanFactory = (AbstractBeanFactory)bf;
    }

    /**
     * Apply this BeanPostProcessor to the given new bean instance <i>before</i> any bean
     * initialization callbacks (like InitializingBean's afterPropertiesSet or a custom
     * init-method). The bean will already be populated with property values.
     * The returned bean instance may be a wrapper around the original.
     *
     * @param bean     the new bean instance
     * @param beanName the beanName of the bean
     * @return the bean instance to use, either the original or a wrapped one
     * @throws org.springframework.beans.BeansException
     *          in case of errors
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
     */
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * Apply this BeanPostProcessor to the given new bean instance <i>after</i> any bean
     * initialization callbacks (like InitializingBean's afterPropertiesSet or a custom
     * init-method).
     * This post processor finishes the construction of the identity povider by creating
     * and attaching corresponding user and grop managers.
     *
     * @param bean     the new bean instance
     * @param beanName the beanName of the bean
     * @return the bean instance to use, either the original or a wrapped one
     * @throws org.springframework.beans.BeansException
     *          in case of errors
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
     */
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (INTERNAL_PROVIDER_BEAN_NAME.equals(beanName)) {
            final InternalIdentityProvider internalIdentityProvider = ((InternalIdentityProvider)bean);
            UserManager um = (UserManager)abstractBeanFactory.getBean("internalUserManager", new Object[]{InternalUserManager.class, bean});
            internalIdentityProvider.setUserManager(um);

            GroupManager gm = (GroupManager)abstractBeanFactory.getBean("internalGroupManager", new Object[]{InternalGroupManager.class, bean});
            internalIdentityProvider.setGroupManager(gm);
        } else if (FEDERATED_PROVIDER_BEAN_NAME.equals(beanName)) {
            final FederatedIdentityProvider federatedIdentityProvider = ((FederatedIdentityProvider)bean);
            UserManager um = (UserManager)abstractBeanFactory.getBean("federatedUserManager", new Object[]{FederatedUserManager.class, bean});
            federatedIdentityProvider.setUserManager((FederatedUserManager)um);
            GroupManager gm = (GroupManager)abstractBeanFactory.getBean("federatedGroupManager", new Object[]{FederatedGroupManager.class, bean});
            federatedIdentityProvider.setGroupManager((FederatedGroupManager)gm);
        } else if (LDAP_PROVIDER_BEAN_NAME.equals(beanName)) {
            final LdapIdentityProvider ldapIdentityProvider = ((LdapIdentityProvider)bean);
            UserManager um = (UserManager)abstractBeanFactory.getBean("ldapUserManager", new Object[]{LdapUserManager.class, bean});
            ldapIdentityProvider.setUserManager((LdapUserManager)um);
            GroupManager gm = (GroupManager)abstractBeanFactory.getBean("ldapGroupManager", new Object[]{LdapGroupManager.class, bean});
            ldapIdentityProvider.setGroupManager((LdapGroupManager)gm);
        }

        return bean;
    }

    // note these need to be singletons so that they can be invalidates in case of deletion
    private static Map providers = new HashMap();

    public static final int MAX_AGE = 60 * 1000;

    private static final Logger logger = Logger.getLogger(IdentityProviderFactory.class.getName());

    private static final String INTERNAL_PROVIDER_BEAN_NAME = "internalIdentityProvider";
    private static final String FEDERATED_PROVIDER_BEAN_NAME = "federatedIdentityProvider";
    private static final String LDAP_PROVIDER_BEAN_NAME = "ldapIdentityProvider";
}
