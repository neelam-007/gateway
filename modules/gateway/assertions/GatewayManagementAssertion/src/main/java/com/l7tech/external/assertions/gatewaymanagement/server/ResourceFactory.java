package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.AccessibleObject;
import com.l7tech.objectmodel.EntityType;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Factory for resource CRUD.
 */
@SuppressWarnings({"serial"})
public interface ResourceFactory<R> {

    /**
     * Get the entity type for this resource factory.
     *
     * @return The entity type or null if not applicable.
     */
    EntityType getType();

    /**
     * Is this resource factory read only?
     *
     * @return true if read only.
     */
    boolean isReadOnly();

    /**
     * Is resource creation supported?
     *
     * @return true if resource creation is supported.
     */
    boolean isCreateSupported();

    /**
     * Are resource updates supported?
     *
     * @return true if resource updates are supported.
     */
    boolean isUpdateSupported();

    /**
     * Is resource deletion supported?
     *
     * @return true if resource deletion is supported.
     */
    boolean isDeleteSupported();

    /**
     * Get the selector keys for this resource.
     *
     * <p>This is the full set of resource selectors for this resource factory,
     * some selectors may be optional.</p>
     *
     * @return The set of possible selectors for this resource factory.
     */
    Set<String> getSelectors();

    /**
     * Create a new resource and return the selectors.
     *
     * <p>The returned selector set should be sufficient to allow access to the
     * resource.</p>
     *
     * @param resource The resource representation
     * @return The selectors.
     * @throws InvalidResourceException if the resource is null or invalid
     */
    Map<String,String> createResource( Object resource ) throws InvalidResourceException;

    /**
     * Get the resource that matches the given selectors.
     *
     * @param selectorMap The map of selectors
     * @return The resource for the selectors (never null)
     * @throws ResourceNotFoundException if the resource cannot be found.
     */
    R getResource( Map<String, String> selectorMap ) throws ResourceNotFoundException;

    /**
     * Update the resource that matches the given selectors.
     *
     * <p>Any properties of the resource that are read only should not be
     * updated. Such properties should be silently ignored, the returned
     * resource will provide the caller with the current state of the resource.</p>
     *
     * @param selectorMap The map of selectors
     * @param resource The resource representation
     * @return The updated resource
     * @throws ResourceNotFoundException if the resource cannot be found.
     * @throws InvalidResourceException if the updated resource is not valid.
     */
    R putResource( Map<String, String> selectorMap, Object resource ) throws ResourceNotFoundException, InvalidResourceException;

    /**
     * Delete the resource that matches the given selectors.
     *
     * @param selectorMap The map of selectors
     * @return the identifier for the deleted resource
     * @throws ResourceNotFoundException if the resource cannot be found.
     */
    String deleteResource( Map<String, String> selectorMap ) throws ResourceNotFoundException;    

    /**
     * Get selector map for all resources.
     *
     * @return The collection of resource selectors.
     */
    Collection<Map<String, String>> getResources();

    //TODO: need to return a list not a collection
    /**
     * Get selector map for all resource from the given offset, with the given window size.
     *
     * @return The collection of resource selectors.
     */
    Collection<Map<String, String>> getResources(int offset, int windowSize);


    /**
     * Annotation for the resource name.
     *
     * <p>Resource factories should be annotated with their name.</p>
     */
    @Documented
    @Retention(value = RUNTIME)
    @Target(TYPE)
    @interface ResourceType {
        Class<? extends AccessibleObject> type();
    }

    /**
     * Annotation used to mark custom methods on a ResourceFactory.
     *
     * <p>The method signature for a resource method must be one of:</p>
     *
     * <ul>
     *   <li><code>public ANY-RETURN-TYPE ANY-METHOD_NAME( Map<String,String> selectorMap ) ANY-EXCEPTION</li>
     *   <li><code>public ANY-RETURN-TYPE ANY-METHOD_NAME( ANY-TYPE resource ) ANY-EXCEPTION</li>
     *   <li><code>public ANY-RETURN-TYPE ANY-METHOD_NAME( Map<String,String> selectorMap, ANY-TYPE resource ) ANY-EXCEPTION</li>
     * </ul>
     *
     * <p>Depending on whether the method requires selectors, a resource or
     * both, and where <code>ANY-TYPE</code> is the type of the resource
     * passed to the custom method.</p>
     */
    @Documented
    @Retention(value = RUNTIME)
    @Target(METHOD)
    @interface ResourceMethod {
        /**
         * The name for the custom method
         */
        String name();

        /**
         * True if selectors are desired.
         */
        boolean selectors() default false;

        /**
         * True if a resource are desired.
         */
        boolean resource() default false;
    }

    static class ResourceFactoryException extends Exception {
        public ResourceFactoryException( final String message ) {
            super( message );
        }

        public ResourceFactoryException( final String message, final Throwable cause ) {
            super( message, cause );
        }
    }

    /**
     * Exception for a missing resource.
     */
    static class ResourceNotFoundException extends ResourceFactoryException {
        public ResourceNotFoundException( final String message ) {
            super(message);
            EntityContext.setNotFound();
        }

        public ResourceNotFoundException( final String message, final Throwable cause ) {
            super(message, cause);
            EntityContext.setNotFound();
        }
    }

    /**
     * Exception for a missing resource due to invalid selectors.
     */
    static class InvalidResourceSelectors extends ResourceNotFoundException {
        public InvalidResourceSelectors() {
            super("Invalid resource selectors");
        }
    }

    /**
     * Exception for an invalid resource representation.
     */
    static class InvalidResourceException extends ResourceFactoryException {
        public enum ExceptionType { INVALID_VALUES, MISSING_VALUES, UNEXPECTED_TYPE }

        private final ExceptionType type;
        private final String detail;

        public InvalidResourceException( final ExceptionType type, final String detail  ) {
            super( "Resource validation failed due to '" + type.toString() + "' " + detail );
            this.type = type;
            this.detail = detail;
        }

        public ExceptionType getType() {
            return type;
        }

        public String getDetail() {
            return detail;
        }
    }

    /**
     * Runtime exception for unexpected resource access errors
     */
    static class ResourceAccessException extends RuntimeException {
        public ResourceAccessException( final String message, final Throwable cause ) {
            super(message, cause);
        }

        public ResourceAccessException( final String message ) {
            super( message );
        }

        public ResourceAccessException( final Throwable cause ) {
            super(cause);
        }
    }    

    /**
     * Runtime exception for a duplicate resource error
     */
    static class DuplicateResourceAccessException extends RuntimeException {
        public DuplicateResourceAccessException( final String message ) {
            super( message );
        }
    }
}
