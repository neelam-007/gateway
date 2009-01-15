package com.l7tech.server.migration;

import com.l7tech.objectmodel.migration.*;
import com.l7tech.policy.assertion.Assertion;

import java.lang.reflect.Method;

/**
 * Abstract base implementation of the PropertyResolver that deals with retrieval of other property resolvers
 * through the supplied factory.
 *
 * @author jbufu
 */
public abstract class AbstractPropertyResolver implements PropertyResolver {

    private PropertyResolverFactory factory;

    public AbstractPropertyResolver(PropertyResolverFactory factory) {
        this.factory = factory;
    }

    public PropertyResolver getResolver(Method property) throws PropertyResolverException {

        PropertyResolver resolver = factory.getPropertyResolver(MigrationUtils.getTargetType(property));

        if ( (resolver.getClass().equals(factory.getPropertyResolver(PropertyResolver.Type.DEFAULT).getClass())) &&
             (Assertion.class.isAssignableFrom(property.getDeclaringClass())) )
            return factory.getPropertyResolver(PropertyResolver.Type.ASSERTION);

        return resolver;
    }
}
