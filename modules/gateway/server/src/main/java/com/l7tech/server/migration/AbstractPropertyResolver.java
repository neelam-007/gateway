package com.l7tech.server.migration;

import com.l7tech.objectmodel.migration.*;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.ExternalEntityHeader;
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
    private Type type;

    public AbstractPropertyResolver(PropertyResolverFactory factory, Type type) {
        this.factory = factory;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public PropertyResolver getResolver(Method property) throws PropertyResolverException {

        PropertyResolver resolver = factory.getPropertyResolver(MigrationUtils.getTargetType(property));

        if ( (resolver.getClass().equals(factory.getPropertyResolver(PropertyResolver.Type.DEFAULT).getClass())) &&
             (Assertion.class.isAssignableFrom(property.getDeclaringClass())) )
            return factory.getPropertyResolver(PropertyResolver.Type.ASSERTION);

        return resolver;
    }

    public Entity valueMapping(ExternalEntityHeader header) throws PropertyResolverException {
        throw new PropertyResolverException("Value mapping not supported for header: " + header);
    }

    protected static Object getPropertyValue(Object object, Method property) throws PropertyResolverException {
        final Object propertyValue;
        try {
            propertyValue = property.invoke(object);
        } catch (Exception e) {
            throw new PropertyResolverException("Error getting property value for entity: " + object, e);
        }
        return propertyValue;
    }
}
