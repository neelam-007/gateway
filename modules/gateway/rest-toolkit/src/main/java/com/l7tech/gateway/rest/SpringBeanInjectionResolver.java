package com.l7tech.gateway.rest;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.springframework.context.ApplicationContext;

import javax.inject.Singleton;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This Injection resolver will perform the injection for Factory classes into the Jersey resources.
 */
@Singleton
public class SpringBeanInjectionResolver implements InjectionResolver<SpringBean> {
    private static final Logger logger = Logger.getLogger(SpringBeanInjectionResolver.class.getName());
    private static final boolean throwExceptionsOnUnresolvable = true;

    //The application context to find the beans in.
    private ApplicationContext applicationContext;

    /**
     * Creates a SpringBeanInjector with that will find beans in the given application context
     *
     * @param applicationContext The application context that this Injector resolver will look in to find the objects to
     *                           inject.
     */
    public SpringBeanInjectionResolver(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * This will find the bean that is referenced by the {@link Injectee}
     *
     * @param injectee The injectee that is needing to be injected
     * @param root     This is usually null
     * @return Returns the bean to set the injectee to
     */
    @Override
    public Object resolve(final Injectee injectee, ServiceHandle<?> root) {

        //Validate that the applicationContext has been set.
        if (applicationContext == null) {
            return reportResolverError("Application context has not been set. Cannot inject Spring Beans.");
        }

        //Get the type of bean required.
        final Class<?> beanType = (Class<?>) injectee.getRequiredType();
        //If the bean required is the application context then return it.
        if(ApplicationContext.class.equals(beanType)){
            return applicationContext;
        }
        //Find the bean by its type in the application context.
        Object bean = applicationContext.getBean(beanType);
        if (bean == null) {
            return reportResolverError("Cannot resolve bean: ''{0}'' of type: {1} in class {2}. Found 0 matching beans.", injectee.getParent(), beanType.getName(), injectee.getInjecteeClass().getName());
        }
        return bean;
    }

    /**
     * Logs a warning when there was an error resolving a bean. This will also with throw an exception or return null
     * depending on {@link #throwExceptionsOnUnresolvable}
     *
     * @param message The error message.
     */
    private Object reportResolverError(String message, Object... messageArgs) {
        logger.log(Level.WARNING, message, messageArgs);
        if (throwExceptionsOnUnresolvable) {
            throw new IllegalArgumentException(String.format(message, messageArgs));
        } else {
            return null;
        }
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return false;
    }

    /**
     * Returning true will make it so that method with a parameter with the {@link @SpringBean} annotation will
     * automatically be called when the Object is being initialized
     *
     * @return false
     */
    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }
}
