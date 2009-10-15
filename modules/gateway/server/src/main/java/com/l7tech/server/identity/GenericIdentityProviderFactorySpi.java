package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.server.Lifecycle;
import com.l7tech.server.LifecycleException;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.BeansException;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Generic identity provider factory suitable for use in IoC container.
 */
public class GenericIdentityProviderFactorySpi implements IdentityProviderFactorySpi, BeanFactoryAware {

    //- PUBLIC

    public GenericIdentityProviderFactorySpi(final String classname,
                                             final String identityProviderBeanName) {
        this.classname = classname;
        this.identityProviderBeanName = identityProviderBeanName;
    }

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public String getClassname() {
        return classname;
    }

    @Override
    public IdentityProvider createIdentityProvider( final IdentityProviderConfig configuration,
                                                    final boolean start ) throws InvalidIdProviderCfgException {
        IdentityProvider provider = (IdentityProvider) beanFactory.getBean(identityProviderBeanName, IdentityProvider.class);

        if ( !(provider instanceof ConfigurableIdentityProvider) ) {
            throw new InvalidIdProviderCfgException("IdentityProvider does not support configuration interface.");
        }

        ConfigurableIdentityProvider configurableProvider = (ConfigurableIdentityProvider) provider;

        configurableProvider.setIdentityProviderConfig( configuration );
        if ( start && provider instanceof Lifecycle ) {
            Lifecycle lifecycleProvider = (Lifecycle) provider;
            try {
                lifecycleProvider.start();
            } catch (LifecycleException e) {
                logger.log( Level.WARNING, "Error starting identity provider '"+getId(provider)+"'.", e );    
            }
        }

        return provider;
    }

    @Override
    public void destroyIdentityProvider( final IdentityProvider identityProvider ) {
        if ( identityProvider instanceof DisposableBean ) {
            DisposableBean disposableBean = (DisposableBean) identityProvider;
            try {
                disposableBean.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to destroy the identity provider '" + getId(identityProvider) + "'.", e);
            }
        }

        if ( identityProvider instanceof Lifecycle ) {
            Lifecycle lifecycleProvider = (Lifecycle) identityProvider;
            try {
                lifecycleProvider.stop();
            } catch (LifecycleException e) {
                logger.log(Level.WARNING, "Error stopping identity provider '" + getId(identityProvider) + "'.", e);
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(GenericIdentityProviderFactorySpi.class.getName());

    private final String classname;
    private final String identityProviderBeanName;
    private BeanFactory beanFactory;

    private String getId( final IdentityProvider identityProvider ) {
        return identityProvider.getConfig().getName() + " (#" + identityProvider.getConfig().getId() + ")";
    }
}
