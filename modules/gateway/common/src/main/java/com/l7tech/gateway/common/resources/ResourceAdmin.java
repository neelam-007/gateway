package com.l7tech.gateway.common.resources;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.ConstraintViolationException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
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

    @Transactional(readOnly=true)
    @Secured(stereotype= MethodStereotype.FIND_ENTITIES)
    Collection<ResourceEntryHeader> findAllResources() throws FindException;

    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.FIND_ENTITY)
    ResourceEntry findResourceEntryByPrimaryKey(long oid) throws FindException;

    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.FIND_ENTITY)
    ResourceEntry findResourceEntryByUriAndType(String uri, ResourceType type) throws FindException;

    @Secured(stereotype=MethodStereotype.DELETE_ENTITY)
    void deleteResourceEntry(ResourceEntry resourceEntry) throws DeleteException;

    @Secured(stereotype=MethodStereotype.DELETE_BY_ID)
    void deleteResourceEntry(long resourceEntryOid) throws FindException, DeleteException;

    @Secured(stereotype=MethodStereotype.SAVE_OR_UPDATE)
    long saveResourceEntry(ResourceEntry resourceEntry) throws SaveException, UpdateException;

    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<ResourceEntryHeader> findResourceHeadersByType( ResourceType type ) throws FindException;

    @Transactional(readOnly=true)
    @Secured(stereotype=MethodStereotype.FIND_ENTITY)
    ResourceEntryHeader findResourceHeaderByUriAndType( String uri, ResourceType type ) throws FindException;

    @Transactional(readOnly=true)
    @Secured(stereotype= MethodStereotype.FIND_ENTITIES)
    Collection<ResourceEntryHeader> findResourceHeadersByTargetNamespace( String targetNamespace ) throws FindException;

    @Transactional(readOnly=true)
    @Secured(stereotype= MethodStereotype.FIND_ENTITIES)
    Collection<ResourceEntryHeader> findResourceHeadersByPublicIdentifier( String publicIdentifier ) throws FindException;

    @Transactional(readOnly=true)
    int countRegisteredSchemas( Collection<Long> resourceOids ) throws FindException;

    @Transactional(readOnly=true)
    boolean allowSchemaDoctype();

    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.FIND_ENTITIES)
    HttpProxyConfiguration getDefaultHttpProxyConfiguration() throws FindException;
    
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE)
    void setDefaultHttpProxyConfiguration( HttpProxyConfiguration httpProxyConfiguration ) throws SaveException, UpdateException;

    @Secured(types=EntityType.HTTP_CONFIGURATION, stereotype=MethodStereotype.FIND_ENTITIES)
    @Transactional(readOnly=true)
    Collection<HttpConfiguration> findAllHttpConfigurations() throws FindException;

    @Secured(types=EntityType.HTTP_CONFIGURATION, stereotype=MethodStereotype.FIND_ENTITY)
    @Transactional(readOnly=true)
    HttpConfiguration findHttpConfigurationByPrimaryKey( long oid ) throws FindException;

    @Secured(types=EntityType.HTTP_CONFIGURATION, stereotype=MethodStereotype.DELETE_ENTITY)
    void deleteHttpConfiguration( HttpConfiguration httpConfiguration ) throws DeleteException;

    @Secured(types=EntityType.HTTP_CONFIGURATION, stereotype=MethodStereotype.SAVE_OR_UPDATE)
    long saveHttpConfiguration( HttpConfiguration httpConfiguration ) throws SaveException, UpdateException;

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
