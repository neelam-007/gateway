package com.l7tech.server.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Injector implementation that uses the ApplicationContext
 */
public class ApplicationContextInjector implements ApplicationContextAware, Injector, InjectingConstructor {

    //- PUBLIC

    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
        autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
    }

    @Override
    public void inject( final Object target ) {
        autowireCapableBeanFactory.autowireBeanProperties( target, AutowireCapableBeanFactory.AUTOWIRE_NO, false );
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public <T> T injectNew( final Class<T> type ) {
        return (T) autowireCapableBeanFactory.autowire( type, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false );
    }

    /**
     * Convenience method for bean dependency injection.
     *
     * @param applicationContext The context to use.
     * @param target The bean to inject.
     */
    public static void inject( final ApplicationContext applicationContext,
                               final Object target ) {
        final ApplicationContextInjector injector = new ApplicationContextInjector();
        injector.setApplicationContext( applicationContext );
        injector.inject( target );
    }

    //- PRIVATE

    private AutowireCapableBeanFactory autowireCapableBeanFactory;
}
