package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.Accessor;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.ManagementRuntimeException;

import javax.xml.bind.annotation.XmlSchema;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 */
public class AccessorFactory {

    //- PUBLIC

    public static <MO extends ManagedObject> Accessor<MO> createAccessor( final Class<MO> managedObjectClass,
                                                                          final String url,
                                                                          final ResourceTracker resourceTracker ) {
        if ( hasPolicy(managedObjectClass) ) {
            return new PolicyAccessorImpl<MO>( url, getResourceUri(managedObjectClass), managedObjectClass, resourceTracker );
        } else {
            return new AccessorImpl<MO>( url, getResourceUri(managedObjectClass), managedObjectClass, resourceTracker );
        }
    }

    public static String getResourceName( final Class<?> managedObjectClass ) {
        final AccessorFactory.ManagedResource resource = managedObjectClass.getAnnotation( AccessorFactory.ManagedResource.class );
        if ( resource == null ) {
            throw new ManagementRuntimeException("Missing annotation for resource '"+managedObjectClass.getName()+"'.");
        }
        return resource.name();
    }
    
    public static String getResourceUri( final Class<?> managedObjectClass ) {
        final XmlSchema schema = Accessor.class.getPackage().getAnnotation( XmlSchema.class );
        if ( schema == null ) {
            throw new ManagementRuntimeException("Missing annotation for API package.");
        }
        return schema.namespace() + "/" + getResourceName(managedObjectClass);
    }

    @Retention(value = RUNTIME)
    @Target(TYPE)
    public @interface ManagedResource {
        public abstract String name();
        public abstract boolean hasPolicy() default false;
    }

    //- PRIVATE

    private static boolean hasPolicy( final Class<?> managedObjectClass ) {
        final AccessorFactory.ManagedResource resource = managedObjectClass.getAnnotation( AccessorFactory.ManagedResource.class );
        return resource != null && resource.hasPolicy();
    }

}
