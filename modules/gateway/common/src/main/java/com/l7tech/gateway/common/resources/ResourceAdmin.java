package com.l7tech.gateway.common.resources;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * SSM Administration API for Resources and HTTP configuration.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
@Administrative
@Secured
public interface ResourceAdmin {

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
}
