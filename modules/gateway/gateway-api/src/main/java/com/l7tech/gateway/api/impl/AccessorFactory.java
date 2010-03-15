package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.AccessibleObject;
import com.l7tech.gateway.api.Accessor;
import com.l7tech.gateway.api.ManagementRuntimeException;
import com.sun.ws.management.client.ResourceFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Factory for resource Accessors, for use from API client. 
 */
public class AccessorFactory {

    //- PUBLIC

    @SuppressWarnings({ "unchecked" })
    public static <MO extends AccessibleObject> Accessor<MO> createAccessor( final Class<MO> managedObjectClass,
                                                                             final String url,
                                                                             final ResourceFactory resourceFactory,
                                                                             final ResourceTracker resourceTracker ) {
        Accessor<MO> accessor;

        if ( hasAccessor(managedObjectClass) ) {
            final Class<? extends Accessor> accessorClass = getAccessor( managedObjectClass );
            try {
                final Constructor constructor = accessorClass.getDeclaredConstructor( String.class, String.class, Class.class, ResourceFactory.class, ResourceTracker.class );
                accessor = (Accessor<MO>) constructor.newInstance( url, AccessorSupport.getResourceUri(managedObjectClass), managedObjectClass, resourceFactory, resourceTracker );
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
            accessor = new AccessorImpl<MO>( url, AccessorSupport.getResourceUri(managedObjectClass), managedObjectClass, resourceFactory, resourceTracker );
        }

        return accessor;
    }

    //- PRIVATE

    private static boolean hasAccessor( final Class<?> accessibleObjectClass ) {
        return !Accessor.class.equals( getAccessor( accessibleObjectClass ) );
    }

    @SuppressWarnings({ "unchecked" })
    private static Class<? extends Accessor> getAccessor( final Class<?> accessibleObjectClass ) {
        final AccessorSupport.AccessibleResource resource = accessibleObjectClass.getAnnotation( AccessorSupport.AccessibleResource.class );
        try {
            return resource != null ?
                    (Class<? extends Accessor>) Class.forName( resource.accessorClassname() ) :
                    Accessor.class;
        } catch ( ClassNotFoundException e ) {
            throw new ManagementRuntimeException("Error accessing accessor class '"+resource.accessorClassname()+"'.", e);
        }
    }
}
