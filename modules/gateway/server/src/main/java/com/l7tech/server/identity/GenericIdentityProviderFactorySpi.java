package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.InvalidIdProviderCfgException;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.BeansException;

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
        if ( start ) {
            configurableProvider.startMaintenance();
        }

        return provider;
    }

    //- PRIVATE

    private final String classname;
    private final String identityProviderBeanName;
    private BeanFactory beanFactory;

}
