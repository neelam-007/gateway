package com.l7tech.gateway.common.resources;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collection;

/**
 * SSM Administration API for Resources and HTTP configuration.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
@Administrative
@Secured(types=EntityType.RESOURCE_ENTRY)
public interface ResourceAdmin {

    /**
     * Find all resource entry headers.
     *
     * @return The collection of headers (may be empty, never null)
     * @throws FindException If an error occurs
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    Collection<ResourceEntryHeader> findAllResources() throws FindException;

    /**
     * Find a resource entry by primary key.
     *
     * @param goid The primary key
     * @return The resource or null if not found
     * @throws FindException If an error occurs
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.FIND_ENTITY)
    ResourceEntry findResourceEntryByPrimaryKey(Goid goid) throws FindException;

    /**
     * Find a resource entry by URI and type.
     *
     * @param uri The URI to use (required)
     * @param type The type to match (may be null)
     * @return The resource or null if not found (or incorrect type)
     * @throws FindException If an error occurs
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.FIND_ENTITY)
    ResourceEntry findResourceEntryByUriAndType(String uri, ResourceType type) throws FindException;

    /**
     * Delete the given resource entry.
     *
     * @param resourceEntry The resource entry to delete.
     * @throws DeleteException If an error occurs
     */
    @Secured(stereotype=MethodStereotype.DELETE_ENTITY)
    void deleteResourceEntry(ResourceEntry resourceEntry) throws DeleteException;

    /**
     * Delete the resource entry with the given identifier if it exists.
     *
     * @param resourceEntryGoid The primary key of the resource entry to delete
     * @throws FindException If an error occurs
     * @throws DeleteException If an error occurs
     */
    @Secured(stereotype=MethodStereotype.DELETE_BY_ID)
    void deleteResourceEntry(Goid resourceEntryGoid) throws FindException, DeleteException;

    /**
     * Save the given resource entry.
     *
     * @param resourceEntry The resource entry to save or update.
     * @return The primary key of the resource entry
     * @throws SaveException If an error occurs
     * @throws UpdateException If an error occurs
     */
    @Secured(stereotype=MethodStereotype.SAVE_OR_UPDATE)
    Goid saveResourceEntry(ResourceEntry resourceEntry) throws SaveException, UpdateException;

    /**
     * Save a bag of resource entries.
     *
     * <p>This allows multiple resources to be saved atomically.</p>
     *
     * @param resourceEntryBag The resource entry bag.
     * @throws SaveException If an error occurs
     * @throws UpdateException If an error occurs
     */
    @Secured(stereotype=MethodStereotype.SAVE_OR_UPDATE)
    void saveResourceEntryBag( final ResourceEntryBag resourceEntryBag ) throws SaveException, UpdateException;

    /**
     * Find resource entry headers by resource type.
     *
     * @param type The resource type to find (may be null)
     * @return The collection of resource entry headers (may be empty, never null)
     * @throws FindException If an error occurs
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    Collection<ResourceEntryHeader> findResourceHeadersByType( ResourceType type ) throws FindException;

    /**
     * Find a resource entry header by URI and type.
     *
     * @param uri The URI of the resource (required)
     * @param type The type of the resource (may be null)
     * @return The resource entry header or null if not found (or incorrect type)
     * @throws FindException If an error occurs.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    ResourceEntryHeader findResourceHeaderByUriAndType( String uri, ResourceType type ) throws FindException;

    /**
     * Find resource entry headers by key and type.
     *
     * @param key The target namespace (may be null)
     * @param type The type of the resource (may be null)
     * @return The collection of resource entry headers (may be empty, never null)
     * @throws FindException If an error occurs.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype= MethodStereotype.FIND_HEADERS)
    Collection<ResourceEntryHeader> findResourceHeadersByKeyAndType( String key, ResourceType type ) throws FindException;

    /**
     * Find resource entry headers by target namespace.
     *
     * <p>This will find resource of type XML Schema matching the given target
     * namespace.</p>
     *
     * @param targetNamespace The target namespace (may be null)
     * @return The collection of resource entry headers (may be empty, never null)
     * @throws FindException If an error occurs.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype= MethodStereotype.FIND_HEADERS)
    Collection<ResourceEntryHeader> findResourceHeadersByTargetNamespace( String targetNamespace ) throws FindException;

    /**
     * Find resource entry headers by public identifier.
     *
     * <p>This will find resource of type DTD matching the given public
     * identifier.</p>
     *
     * @param publicIdentifier The target namespace (required)
     * @return The collection of resource entry headers (may be empty, never null)
     * @throws FindException If an error occurs.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype= MethodStereotype.FIND_HEADERS)
    Collection<ResourceEntryHeader> findResourceHeadersByPublicIdentifier( String publicIdentifier ) throws FindException;

    /**
     * Find the collection of default resources.
     *
     * <p>Default resources are not persistent and not used at runtime.</p>
     *
     * @return The collection of default resources (may be empty, never null)
     * @throws FindException
     */
    @Transactional(readOnly=true)
    Collection<ResourceEntryHeader> findDefaultResources() throws FindException;

    /**
     * Find a default resource ty URI.
     *
     * @param uri The URI of the default resource.
     * @return The resource entry or null.
     * @throws FindException If an error occurs.
     */
    @Transactional(readOnly=true)
    ResourceEntry findDefaultResourceByUri( final String uri ) throws FindException;

    /**
     * Count the given resources that are registered.
     *
     * <p>This will count the number of given resources that are either
     * registered, or are dependencies of registered resources.</p>
     *
     * @param resourceGoids The set of resources to check.
     * @return The count of registered resources.
     * @throws FindException If an error occurs.
     */
    @Transactional(readOnly=true)
    int countRegisteredSchemas( Collection<Goid> resourceGoids ) throws FindException;

    /**
     * Are schema doctypes currently permitted.
     *
     * <p>This should only be used to advise on a resources current runtime
     * compatibility.</p>
     *
     * @return True if permitted.
     */
    @Transactional(readOnly=true)
    boolean allowSchemaDoctype();

    /**
     * Get the default HTTP proxy configuration.
     *
     * @return The proxy configuration (may be invalid, never null)
     * @throws FindException If an error occurs
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.FIND_ENTITIES)
    HttpProxyConfiguration getDefaultHttpProxyConfiguration() throws FindException;

    /**
     * Set the default HTTP proxy configuration.
     *
     * @param httpProxyConfiguration The configuration to use (may be null)
     * @throws SaveException If an error occurs
     * @throws UpdateException If an error occurs
     */
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE)
    void setDefaultHttpProxyConfiguration( HttpProxyConfiguration httpProxyConfiguration ) throws SaveException, UpdateException;

    /**
     * Find all HTTP configurations.
     *
     * @return The collection of HTTP configurations (may be empty, never null)
     * @throws FindException If an error occurs
     */
    @Secured(types=EntityType.HTTP_CONFIGURATION, stereotype=MethodStereotype.FIND_ENTITIES)
    @Transactional(readOnly=true)
    Collection<HttpConfiguration> findAllHttpConfigurations() throws FindException;

    /**
     * Find an HTTP configuration by primary key.
     *
     * @param goid The primary key of the HTTP configuration (required)
     * @return The HTTP configuration or null
     * @throws FindException If an error occurs
     */
    @Secured(types=EntityType.HTTP_CONFIGURATION, stereotype=MethodStereotype.FIND_ENTITY)
    @Transactional(readOnly=true)
    HttpConfiguration findHttpConfigurationByPrimaryKey( Goid goid ) throws FindException;

    /**
     * Delete the given HTTP configuration.
     *
     * @param httpConfiguration The HTTP configuration to delete.
     * @throws DeleteException If an error occurs
     */
    @Secured(types=EntityType.HTTP_CONFIGURATION, stereotype=MethodStereotype.DELETE_ENTITY)
    void deleteHttpConfiguration( HttpConfiguration httpConfiguration ) throws DeleteException;

    /**
     * Save the given HTTP configuration.
     *
     * @param httpConfiguration The HTTP configuration to save.
     * @return The primary key of the HTTP configuration
     * @throws SaveException If an error occurs
     * @throws UpdateException If an error occurs
     */
    @Secured(types=EntityType.HTTP_CONFIGURATION, stereotype=MethodStereotype.SAVE_OR_UPDATE)
    Goid saveHttpConfiguration( HttpConfiguration httpConfiguration ) throws SaveException, UpdateException;

    /**
     * Get a resource from a URL.
     * <p/>
     * URL may be http or https with or without client authentication.
     *
     * @param url the url that the gateway will use to resolve the resource. this may contain
     *            userinfo type credentials
     * @return the contents resolved by this url
     * @throws java.io.IOException            thrown on I/O error accessing the url
     * @throws java.net.MalformedURLException thrown on malformed url
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    String resolveResource(String url) throws IOException;

}
