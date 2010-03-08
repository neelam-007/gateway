package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.AccessibleObject;
import com.l7tech.gateway.api.Accessor;
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
    public static <MO extends AccessibleObject> Accessor<MO> createAccessor( final Class<MO> managedObjectClass,
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

    public static String getResourceName( final Class<? extends AccessibleObject> managedObjectClass ) {
        final AccessibleResource resource = managedObjectClass.getAnnotation( AccessibleResource.class );
        if ( resource == null ) {
            throw new ManagementRuntimeException("Missing annotation for resource '"+managedObjectClass.getName()+"'.");
        }
        return resource.name();
    }
    
    public static String getResourceUri( final Class<? extends AccessibleObject> managedObjectClass ) {
        final XmlSchema schema = Accessor.class.getPackage().getAnnotation( XmlSchema.class );
        if ( schema == null ) {
            throw new ManagementRuntimeException("Missing annotation for API package.");
        }
        return schema.namespace() + "/" + getResourceName(managedObjectClass);
    }

    @Retention(value = RUNTIME)
    @Target(TYPE)
    public @interface AccessibleResource {
        public abstract String name();
        // avoids using class reference since in some deployments the client
        // classes may not be available
        public abstract String accessorClassname() default "com.l7tech.gateway.api.Accessor";
    }

    //- PRIVATE

    private static boolean hasAccessor( final Class<?> accessibleObjectClass ) {
        return !Accessor.class.equals( getAccessor( accessibleObjectClass ) );
    }

    @SuppressWarnings({ "unchecked" })
    private static Class<? extends Accessor> getAccessor( final Class<?> accessibleObjectClass ) {
        final AccessibleResource resource = accessibleObjectClass.getAnnotation( AccessibleResource.class );
        try {
            return resource != null ?
                    (Class<? extends Accessor>) Class.forName( resource.accessorClassname() ) :
                    Accessor.class;
        } catch ( ClassNotFoundException e ) {
            throw new ManagementRuntimeException("Error accessing accessor class '"+resource.accessorClassname()+"'.", e);
        }
    }
}
