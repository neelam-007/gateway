package com.l7tech.server.ems.util;

import org.apache.wicket.Application;
import org.apache.wicket.IClusterable;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.injection.ComponentInjector;
import org.apache.wicket.injection.ConfigurableInjector;
import org.apache.wicket.injection.IFieldValueFactory;
import org.apache.wicket.injection.web.InjectorHolder;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.ISpringContextLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * "Derived" from SpringComponentInjector
 *
 * See https://issues.apache.org/jira/browse/WICKET-3542
 */
public class SpringInjectComponentInjector extends ComponentInjector {

    /**
     * Metadata key used to store application context holder in application's metadata
     */
    private static final MetaDataKey<ApplicationContextHolder> CONTEXT_KEY = new MetaDataKey<ApplicationContextHolder>() {

        private static final long serialVersionUID = 1L;

    };

    /**
     * Constructor used when spring application context is declared in the spring standard way and
     * can be located through
     * {@link WebApplicationContextUtils#getRequiredWebApplicationContext(ServletContext)}
     *
     * @param webapp wicket web application
     */
    public SpringInjectComponentInjector( final WebApplication webapp ) {
        // locate application context through spring's default location
        // mechanism and pass it on to the proper constructor
        this( webapp, WebApplicationContextUtils.getRequiredWebApplicationContext( webapp
                .getServletContext() ), true );
    }

    /**
     * Constructor
     *
     * @param webapp        wicket web application
     * @param ctx           spring's application context
     * @param wrapInProxies whether or not wicket should wrap dependencies with specialized proxies that can
     *                      be safely serialized. in most cases this should be set to true.
     */
    public SpringInjectComponentInjector( final WebApplication webapp,
                                          final ApplicationContext ctx,
                                          final boolean wrapInProxies ) {
        if ( webapp == null ) {
            throw new IllegalArgumentException( "Argument [[webapp]] cannot be null" );
        }

        if ( ctx == null ) {
            throw new IllegalArgumentException( "Argument [[ctx]] cannot be null" );
        }

        // store context in application's metadata ...
        webapp.setMetaData( CONTEXT_KEY, new ApplicationContextHolder( ctx ) );

        // ... and create and register the annotation aware injector
        InjectorHolder.setInjector( new ConfigurableInjector() {
            private IFieldValueFactory factory = new InjectAnnotationProxyFieldValueFactory( new ContextLocator(), wrapInProxies );

            @Override
            protected IFieldValueFactory getFieldValueFactory() {
                return factory;
            }
        } );
    }

    /**
     * This is a holder for the application context. The reason we need a holder is that metadata
     * only supports storing serializable objects but application context is not. The holder acts as
     * a serializable wrapper for the context. Notice that although holder implements IClusterable
     * it really is not because it has a reference to non serializable context - but this is ok
     * because metadata objects in application are never serialized.
     *
     * @author ivaynberg
     */
    private static class ApplicationContextHolder implements IClusterable {
        private static final long serialVersionUID = 1L;

        private final ApplicationContext context;

        /**
         * Constructor
         */
        private ApplicationContextHolder( final ApplicationContext context ) {
            this.context = context;
        }

        /**
         * @return the context
         */
        public ApplicationContext getContext() {
            return context;
        }
    }

    /**
     * A context locator that locates the context in application's metadata. This locator also keeps
     * a transient cache of the lookup.
     *
     * @author ivaynberg
     */
    private static class ContextLocator implements ISpringContextLocator {
        private transient ApplicationContext context;

        private static final long serialVersionUID = 1L;

        @Override
        public ApplicationContext getSpringContext() {
            if ( context == null ) {
                context = (Application.get().getMetaData( CONTEXT_KEY )).getContext();
            }
            return context;
        }

    }

}
