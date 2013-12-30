package com.l7tech.gateway.rest;

import com.l7tech.util.CollectionUtils;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.DescriptorVisibility;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.internal.ConstantActiveDescriptor;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Singleton;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * This Features allows you to create custom injection resolvers and use them to resolve dependencies in Jersey
 * Components. This class needs to be abstract because there needs to be a unique implementation of a feature for every
 * different Injection resolver.
 * <p/>
 * {@link A} is the annotation the this injection resolver works with.
 * <p/>
 * {@link I} is the injection resolver
 * <p/>
 * See wiki for more info: <a href="https://wiki.l7tech.com/mediawiki/index.php/REST_Management_API_Framework#Injection">https://wiki.l7tech.com/mediawiki/index.php/REST_Management_API_Framework#Injection</a>
 * See more documentation on how to use injection resolvers here: <a href="https://hk2.java.net/custom-resolver-example.html">https://hk2.java.net/custom-resolver-example.html</a>
 * <p/>
 * {@link A} is the annotation the this injection resolver works with.
 * <p/>
 * {@link I} is the injection resolver
 *
 * @see SpringBeanInjectionResolver This is an example of a custom Injection resolver.
 */
public abstract class CustomInjectionResolverFeature<A extends Annotation, I extends InjectionResolver<A>> extends AbstractBinder implements Feature {

    /**
     * The injection resolver to resolve objects annotated with {@link A} with in Jersey components.
     */
    private I injectionResolver;
    /**
     * The annotation class type that the Injection resolver resolves for.
     */
    private Class<A> annotationType;

    /**
     * Creates a new Custom injection resolver feature for use in Jersey rest api components.
     *
     * @param injectionResolver The injection resolver instance to use.
     * @param annotationType    The annotation class that is used by the injection resolver.
     */
    public CustomInjectionResolverFeature(I injectionResolver, Class<A> annotationType) {
        this.injectionResolver = injectionResolver;
        this.annotationType = annotationType;
    }

    /**
     * This is called To bind the injection resolver.
     */
    @Override
    protected void configure() {
        //This descriptor is used to describe the injection resolver.
        Descriptor descriptor = new ConstantActiveDescriptor<>(injectionResolver, CollectionUtils.<Type>set(new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{annotationType};
            }

            @Override
            public Type getRawType() {
                return InjectionResolver.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        }), Singleton.class, injectionResolver.getClass().getName(), null, DescriptorVisibility.LOCAL, false, false, null, null);
        bind(descriptor);
    }


    /**
     * This is called to the feature.
     */
    @Override
    public boolean configure(final FeatureContext context) {
        return true;
    }
}
