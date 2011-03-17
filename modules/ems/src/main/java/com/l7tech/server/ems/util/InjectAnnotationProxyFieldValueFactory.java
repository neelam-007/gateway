package com.l7tech.server.ems.util;

import org.apache.wicket.injection.IFieldValueFactory;
import org.apache.wicket.proxy.LazyInitProxyFactory;
import org.apache.wicket.spring.ISpringContextLocator;
import org.apache.wicket.spring.SpringBeanLocator;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Derived" from AnnotProxyFieldValueFactory
 *
 * See https://issues.apache.org/jira/browse/WICKET-3542
 */
public class InjectAnnotationProxyFieldValueFactory implements IFieldValueFactory {
    private final ISpringContextLocator contextLocator;

    private final ConcurrentHashMap<SpringBeanLocator, Object> cache = new ConcurrentHashMap<SpringBeanLocator, Object>();

    private final boolean wrapInProxies;

    /**
     * @param contextLocator spring context locator
     */
    public InjectAnnotationProxyFieldValueFactory( final ISpringContextLocator contextLocator ) {
        this( contextLocator, true );
    }

    /**
     * @param contextLocator spring context locator
     * @param wrapInProxies  whether or not wicket should wrap dependencies with specialized proxies that can
     *                       be safely serialized. in most cases this should be set to true.
     */
    public InjectAnnotationProxyFieldValueFactory( final ISpringContextLocator contextLocator,
                                                   final boolean wrapInProxies ) {
        if ( contextLocator == null ) {
            throw new IllegalArgumentException( "[contextLocator] argument cannot be null" );
        }
        this.contextLocator = contextLocator;
        this.wrapInProxies = wrapInProxies;
    }

    /**
     * @see org.apache.wicket.injection.IFieldValueFactory#getFieldValue(java.lang.reflect.Field,
     *      java.lang.Object)
     */
    @Override
    public Object getFieldValue( final Field field,
                                 final Object fieldOwner ) {
        if ( field.isAnnotationPresent( Inject.class ) ) {
            final Named annotationNamed = field.getAnnotation( Named.class );
            final SpringBeanLocator locator = new SpringBeanLocator( annotationNamed == null ? "" : annotationNamed.value(), field.getType(), contextLocator );

            // only check the cache if the bean is a singleton
            if ( cache.containsKey( locator ) ) {
                return cache.get( locator );
            }


            final Object target;
            if ( wrapInProxies ) {
                target = LazyInitProxyFactory.createProxy( field.getType(), locator );
            } else {
                target = locator.locateProxyTarget();
            }

            // only put the proxy into the cache if the bean is a singleton
            if ( locator.isSingletonBean() ) {
                cache.put( locator, target );
            }
            return target;
        } else {
            return null;
        }
    }

    /**
     * @see org.apache.wicket.injection.IFieldValueFactory#supportsField(java.lang.reflect.Field)
     */
    @Override
    public boolean supportsField( final Field field ) {
        return field.isAnnotationPresent( Inject.class );
    }
}


