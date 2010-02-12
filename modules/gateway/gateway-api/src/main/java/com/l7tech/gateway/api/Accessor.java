package com.l7tech.gateway.api;

import java.util.Iterator;
import java.util.Map;

/**
 * The Accessor interface provides access to managed objects.
 *
 * <p>Managed objects that support additional methods extend this interface.</p>
 *
 * <p>Accessor methods will throw unchecked {@code AccessorRuntimeException}s
 * for non-business errors such as network issues.</p>
 */
@SuppressWarnings( { "serial" } )
public interface Accessor<MO extends ManagedObject> {

    /**
     * Get a managed object by identifier.
     *
     * <p>This is equivalent to <code>get("id", identifier)<code>.</p>
     *
     * @param identifier The identifier for the object.
     * @return The managed object (never null)
     * @throws AccessorException If an error occurs
     */
    MO get( String identifier ) throws AccessorException;

    /**
     * Get a managed object by the given property.
     *
     * <p>All managed objects will support {@code id} access, managed objects with
     * unique names may also support {@code name} access}. Other object specific
     * properties may also be supported.</p>
     *
     * @param property The property to select by.
     * @param value The value to use for selection.
     * @return The managed object (never null)
     * @throws AccessorException If an error occurs
     */
    MO get( String property, Object value ) throws AccessorException;

    /**
     * Get a managed object by the given properties.
     *
     * @param propertyMap The map of properties.
     * @return The managed object (never null)
     * @throws AccessorException If an error occurs
     */
    MO get( Map<String,Object> propertyMap ) throws AccessorException;

    /**
     * Set values for a managed object.
     *
     * <p>Update the managed object using the given values. Some values may be read-only
     * in which case request values will be ignored.</p>
     *
     * @param item The item with updated values.
     * @throws AccessorException If an error occurs
     */
    void put( ManagedObject item ) throws AccessorException;

    /**
     * Create a new managed object.
     *
     * <p>Create a new managed object using the given values. If not all values are
     * supplied default values may be used.</p>
     *
     * @param item The item to create
     * @return The identifier for the new item
     * @throws AccessorException If an error occurs
     */
    String create( ManagedObject item ) throws AccessorException;

    /**
     * Delete a managed object by identifier.
     *
     * @param identifier The identifier of the item to delete.
     * @throws AccessorException If an error occurs
     */
    void delete( String identifier ) throws AccessorException;

    /**
     * Enumerate all items of this type.
     *
     * @return An iterator allowing access to all items.
     */
    Iterator<MO> enumerate();

    /**
     *
     */
    class AccessorException extends ManagementException {
        public AccessorException( final String message ) {
            super( message );
        }

        public AccessorException( final String message, final Throwable cause ) {
            super( message, cause );
        }

        public AccessorException( final Throwable cause ) {
            super( cause );
        }
    }

    /**
     *
     */
    class AccessorNotFoundException extends AccessorException {
        public AccessorNotFoundException( final String message ) {
            super( message );
        }
    }

    /**
     *
     */
    class AccessorRuntimeException extends ManagementRuntimeException {
        public AccessorRuntimeException( final String message ) {
            super( message );
        }

        public AccessorRuntimeException( final String message, final Throwable cause ) {
            super( message, cause );
        }

        public AccessorRuntimeException( final Throwable cause ) {
            super( cause );
        }
    }

    /**
     *
     */
    class AccessorNetworkException extends AccessorRuntimeException {
        public AccessorNetworkException( final Throwable cause ) {
            super( cause );
        }
    }
}
