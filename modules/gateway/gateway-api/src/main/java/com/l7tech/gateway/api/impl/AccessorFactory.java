package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.Accessor;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.ManagementRuntimeException;

import javax.xml.bind.annotation.XmlSchema;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 */
public class AccessorFactory {

    //- PUBLIC

    @SuppressWarnings({ "unchecked" })
    public static <MO extends ManagedObject> Accessor<MO> createAccessor( final Class<MO> managedObjectClass,
                                                                          final String url,
                                                                          final ResourceTracker resourceTracker ) {
        if ( hasAccessor(managedObjectClass) ) {
            final Class<? extends Accessor> accessorClass = getAccessor( managedObjectClass );
            try {
                final Constructor constructor = accessorClass.getDeclaredConstructor( String.class, String.class, Class.class, ResourceTracker.class );
                return (Accessor<MO>) constructor.newInstance( url, getResourceUri(managedObjectClass), managedObjectClass, resourceTracker );
            } catch ( InstantiationException e ) {
                throw new ManagementRuntimeException("Error creating accessor for '"+managedObjectClass.getName()+"'", e);
            } catch ( IllegalAccessException e ) {
                throw new ManagementRuntimeException("Error creating accessor for '"+managedObjectClass.getName()+"'", e);
            } catch ( InvocationTargetException e ) {
                throw new ManagementRuntimeException("Error creating accessor for '"+managedObjectClass.getName()+"'", e);
            } catch ( NoSuchMethodException e ) {
                throw new ManagementRuntimeException("Error creating accessor for '"+managedObjectClass.getName()+"'", e);
            }
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
        public abstract Class<? extends Accessor> accessorType() default Accessor.class;
    }

    //- PRIVATE

    private static boolean hasAccessor( final Class<?> managedObjectClass ) {
        return !Accessor.class.equals( getAccessor( managedObjectClass ) );
    }

    private static Class<? extends Accessor> getAccessor( final Class<?> managedObjectClass ) {
        final AccessorFactory.ManagedResource resource = managedObjectClass.getAnnotation( AccessorFactory.ManagedResource.class );
        return resource != null ? resource.accessorType() : Accessor.class;
    }
}
