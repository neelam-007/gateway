package com.l7tech.gateway.api;

import com.l7tech.util.ExceptionUtils;
import com.sun.ws.management.client.exceptions.FaultException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Accessor interface provides access to managed objects.
 *
 * <p>Managed objects that support additional methods use extensions of this
 * interface.</p>
 *
 * <p>Accessor methods will throw unchecked {@code AccessorRuntimeException}s
 * for non-business errors such as network issues.</p>
 */
@SuppressWarnings( { "serial" } )
public interface Accessor<MO extends ManagedObject> {

    /**
     * Get the type managed by this accessor.
     *
     * @return The type class.
     */
    Class<MO> getType();

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
    void put( MO item ) throws AccessorException;

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
    String create( MO item ) throws AccessorException;

    /**
     * Delete a managed object by identifier.
     *
     * @param identifier The identifier of the item to delete.
     * @throws AccessorException If an error occurs
     */
    void delete( String identifier ) throws AccessorException;

    /**
     * Delete a managed object by the given property.
     *
     * <p>All managed objects will support {@code id} access, managed objects with
     * unique names may also support {@code name} access}. Other object specific
     * properties may also be supported.</p>
     *
     * @param property The property to select by.
     * @param value The value to use for selection.
     * @throws AccessorException If an error occurs
     */
    void delete( String property, Object value ) throws AccessorException;

    /**
     * Delete a managed object by the given properties.
     *
     * @param propertyMap The map of properties.
     * @throws AccessorException If an error occurs
     */
    void delete( Map<String,Object> propertyMap ) throws AccessorException;


    /**
     * Enumerate all items of this type.
     *
     * @return An iterator allowing access to all items.
     */
    Iterator<MO> enumerate() throws AccessorException;

    /**
     * General purpose accessor exception.
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
     * Accessor exception for object not found.
     */
    class AccessorNotFoundException extends AccessorException {
        public AccessorNotFoundException( final String message ) {
            super( message );
        }
    }

    /**
     * General purpose accessor runtime exception.
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
     * Accessor runtime exception for SOAP faults.
     */
    class AccessorSOAPFaultException extends AccessorRuntimeException {
        static final Pattern SOAP_FAULT_PATTERN = Pattern.compile( "SOAP Fault: (.*)[\\r\\n]{1,2}     Actor: (.*)[\\r\\n]{1,2}      Code: (.*)[\\r\\n]{1,2}(?:  Subcodes:[ ]{0,1}(.*)[\\r\\n]{1,2}|)(?:    Detail: ((?s:.*)))", Pattern.MULTILINE );
                                                         
        private final String fault;
        private final String role;
        private final String code; //NOTE: wsman does not expose codes and subcodes as QNames so we don't make this part of the public API
        private final List<String> subcodes;
        private final List<String> details;

        public AccessorSOAPFaultException( final FaultException fe ) {
            super( ExceptionUtils.getMessage(fe ), fe );

            Matcher matcher = SOAP_FAULT_PATTERN.matcher( ExceptionUtils.getMessage(fe ) );
            if ( matcher.matches() ) {
                fault = matcher.group( 1 );
                role = matcher.group( 2 );
                code = matcher.group( 3 );
                subcodes = list(matcher.group( 4 ), true);
                details = list(matcher.group( 5 ), false);
            } else {
                fault = ExceptionUtils.getMessage(fe );
                role = "Unknown";
                code = "Unknown";
                subcodes = Collections.emptyList();
                details = Collections.emptyList();
            }
        }

        /**
         * Get the fault reason.
         *
         * @return The reason message.
         */
        public String getFault() {
            return fault;
        }

        /**
         * Get the fault role.
         *
         * @return The role.
         */
        public String getRole() {
            return role;
        }

        /**
         * Get the text values from the fault details.
         *
         * @return The detail text.
         */
        public List<String> getDetails() {
            return details;
        }

        protected String getCode() {
            return code;
        }

        protected List<String> getSubcodes() {
            return subcodes;
        }

        private List<String> list( final String listSeparatedList, final boolean spaceSeparated ) {
            final List<String> list;
            final String splitPattern = spaceSeparated ?
                    "[\r\n ]{1,2}" :
                    "[\r\n]{1,2}";

            if ( listSeparatedList == null ) {
                list = Collections.emptyList();
            } else {
                final List<String> arrayList = new ArrayList<String>();
                final String[] listItems = listSeparatedList.split( splitPattern );
                for ( final String item : listItems ) {
                    if ( !item.trim().isEmpty() ) {
                        arrayList.add( item.trim() );
                    }
                }
                list = Collections.unmodifiableList(arrayList );
            }

            return list;
        }
    }

    /**
     * Accessor runtime exception for network errors.
     */
    class AccessorNetworkException extends AccessorRuntimeException {
        public AccessorNetworkException( final Throwable cause ) {
            super( ExceptionUtils.getMessage( cause ), cause );
        }
    }
}
