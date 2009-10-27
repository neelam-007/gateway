/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 *
 * Admin interface for UDDI Registries
 *
 * @author darmstrong
 */
package com.l7tech.gateway.common.admin;

import org.springframework.transaction.annotation.Transactional;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.FIND_ENTITIES;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.DELETE_BY_ID;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.objectmodel.*;
import com.l7tech.uddi.UDDIException;

import java.util.Collection;

@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types= EntityType.UDDI_REGISTRY)
@Administrative
public interface UDDIRegistryAdmin {

    @Secured(types=EntityType.UDDI_REGISTRY, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    long saveUDDIRegistry(UDDIRegistry uddiRegistry) throws SaveException, UpdateException;

    /**
     * Download all UDDI Registry records on this cluster.
     *
     * @return a List of UDDIRegistry instances.  Never null.
     * @throws FindException if there is a problem reading from the database
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=FIND_ENTITIES)
    Collection<UDDIRegistry> findAllUDDIRegistries() throws FindException;

    /**
     * Before the UDDIRegistry is deleted, all data published to the registry should be attempted to be removed.
     *
     * @param oid long oid of the UDDI Registry to delete
     * @throws DeleteException any problems deleting
     * @throws FindException any problems find the UDDI Registry
     */
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=DELETE_BY_ID)
    void deleteUDDIRegistry(long oid) throws DeleteException, FindException, UDDIException;

    /**
     * Test if it is possible to authenticate with the supplied UDDIRegistry
     *
     * @param uddiRegistry UDDIRegistry to test. May not exist as an entity yet.
     * @throws FindException if the UDDIRegistry cannot be found
     * @throws UDDIException if it's not possible to authenticate
     */
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=FIND_ENTITIES)
    void testUDDIRegistryAuthentication(UDDIRegistry uddiRegistry) throws FindException, UDDIException;

    /**
     * Publish the Gateway WSDL for a service to UDDI.
     *
     * This operation will be as transactional as possible with a non transactional decoupled UDDI registry.
     * If any save operation fails, the best attempt to roll back changes to the UDDI registry will be made.
     *
     * @param uddiProxiedService the UDDIProxiedService to publish.
     * @return oid of the created UDDIProxiedService
     * @throws FindException if the published service or uddi registry could not be found
     * @throws PublishProxiedServiceException if the Gateway WSDL cannot be found
     * @throws com.l7tech.objectmodel.UpdateException if the UDDIProxiedService cannot be updated
     * @throws com.l7tech.objectmodel.VersionException if the UDDIProxiedService has an incorrect version
     * @throws com.l7tech.objectmodel.SaveException if the UDDIProxiedService cannot be saved
     */
    @Secured(types={EntityType.UDDI_PROXIED_SERVICE}, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    long publishGatewayWsdl(final UDDIProxiedService uddiProxiedService) throws FindException, PublishProxiedServiceException, VersionException, UpdateException, SaveException;

    @Secured(types={EntityType.UDDI_PROXIED_SERVICE}, stereotype= MethodStereotype.DELETE_ENTITY)
    void deleteGatewayWsdlFromUDDI( final UDDIProxiedService uddiProxiedService) throws FindException, UDDIException, DeleteException;

    /**
     * Find the UDDIProxiedService for a service, if it exists
     *
     * @param serviceOid the service to get the UDDIProxiedServiec for
     * @return UDDIProxiedService or null if the service does not have one
     * @throws com.l7tech.objectmodel.FindException Any problem finding the proxied service
     */
    @Secured(types={EntityType.UDDI_PROXIED_SERVICE}, stereotype= MethodStereotype.FIND_ENTITIES)
    UDDIProxiedService getUDDIProxiedService(long serviceOid) throws FindException;

    /**
     * Find all UDDIProxiedServices which have been published to a UDDIRegistry
     * @param registryOid the UDDIRegistry to search
     * @return Collection<UDDIProxiedService> of all proxied services published to the UDDI Registry
     * @throws FindException if any problems searching
     */
    @Secured(types={EntityType.UDDI_PROXIED_SERVICE}, stereotype= MethodStereotype.FIND_ENTITIES)
    Collection<UDDIProxiedService> getAllProxiedServicesForRegistry(long registryOid) throws FindException;

    static class PublishProxiedServiceException extends Exception{
        public PublishProxiedServiceException(String message) {
            super(message);
        }
    }
}
