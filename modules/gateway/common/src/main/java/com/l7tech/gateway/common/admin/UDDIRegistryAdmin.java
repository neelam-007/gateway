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
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
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

    @Transactional(readOnly=true)
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=MethodStereotype.FIND_ENTITY)
    UDDIRegistry findByPrimaryKey(long registryOid) throws FindException;

    /**
     * Before the UDDIRegistry is deleted, all data published to the registry should be attempted to be removed.
     *
     * @param oid long oid of the UDDI Registry to delete
     * @throws DeleteException any problems deleting
     * @throws FindException any problems find the UDDI Registry
     * @throws com.l7tech.uddi.UDDIException if there are any problems removing data from UDDI
     * @throws com.l7tech.gateway.common.admin.UDDIRegistryAdmin.UDDIRegistryNotEnabledException if the registry is not enabled
     */
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=DELETE_BY_ID)
    void deleteUDDIRegistry(long oid) throws DeleteException, FindException, UDDIException, UDDIRegistryNotEnabledException;

    /**
     * Test if it is possible to authenticate with the supplied UDDIRegistry
     *
     * @param uddiRegistry UDDIRegistry to test. May not exist as an entity yet.
     * @throws UDDIException if it's not possible to authenticate
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=FIND_ENTITIES)
    void testUDDIRegistryAuthentication(UDDIRegistry uddiRegistry) throws UDDIException;

    /**
     * Publish the Gateway WSDL for a service to UDDI.
     *
     * This operation will be as transactional as possible with a non transactional decoupled UDDI registry.
     * If any save operation fails, the best attempt to roll back changes to the UDDI registry will be made.
     *
     * Registry must be enabled
     *
     * @param uddiProxiedService the UDDIProxiedService to publish.
     * @return oid of the created UDDIProxiedService
     * @throws FindException if the published service or uddi registry could not be found
     * @throws PublishProxiedServiceException if the Gateway WSDL cannot be found
     * @throws com.l7tech.objectmodel.UpdateException if the UDDIProxiedService cannot be updated
     * @throws com.l7tech.objectmodel.VersionException if the UDDIProxiedService has an incorrect version
     * @throws com.l7tech.objectmodel.SaveException if the UDDIProxiedService cannot be saved
     * @throws com.l7tech.gateway.common.admin.UDDIRegistryAdmin.UDDIRegistryNotEnabledException if the registry is not enabled
     */
    @Secured(types={EntityType.UDDI_PROXIED_SERVICE}, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    long publishGatewayWsdl(final UDDIProxiedService uddiProxiedService)
            throws FindException, PublishProxiedServiceException, VersionException, UpdateException, SaveException, UDDIRegistryNotEnabledException;

    /**
     * Delete all published Business Services which were published to UDDI from the Gateway's WSDL.
     * <p/>
     * This is a best effort only. Regardless of the success from UDDI, we will delete our UDDIProxiedService.
     *
     * Registry must be enabled
     *
     * @param uddiProxiedService UDDIProxiedService and associated data in UDDI to delete
     * @return String error message if there were any problems deleting from UDDI. null if no errors
     * @throws FindException if there is a problem reading from the database or if the UDDIProxiedService is not found
     * @throws DeleteException any problems deleting from the database
     * @throws com.l7tech.gateway.common.admin.UDDIRegistryAdmin.UDDIRegistryNotEnabledException if the registry is not enabled
     */
    @Secured(types = {EntityType.UDDI_PROXIED_SERVICE}, stereotype = MethodStereotype.DELETE_ENTITY)
    String deleteGatewayWsdlFromUDDI(final UDDIProxiedService uddiProxiedService)
            throws FindException, DeleteException, UDDIRegistryNotEnabledException;

    /**
     * Allows for non final properties which do not rely on UDDI data like the UDDIProxiedService's
     * 'updateProxyOnLocalChange' property to be udpated without any any UDDI interaction.
     *
     * @param uddiProxiedService UDDIProxiedService to update
     * @throws com.l7tech.objectmodel.FindException any problems finding the entity
     * @throws com.l7tech.objectmodel.UpdateException any problems updating the database
     */
    @Secured(types = {EntityType.UDDI_PROXIED_SERVICE}, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    void updateProxiedServiceOnly(final UDDIProxiedService uddiProxiedService) 
            throws UpdateException, FindException;
    /**
     * Find the UDDIProxiedService for a service, if it exists
     *
     * @param serviceOid the service to get the UDDIProxiedServiec for
     * @return UDDIProxiedService or null if the service does not have one
     * @throws com.l7tech.objectmodel.FindException Any problem finding the proxied service
     */
    @Transactional(readOnly=true)
    @Secured(types={EntityType.UDDI_PROXIED_SERVICE}, stereotype= MethodStereotype.FIND_ENTITY)
    UDDIProxiedService getUDDIProxiedService(long serviceOid) throws FindException;

    /**
     * Find all UDDIProxiedServices which have been published to a UDDIRegistry
     *
     * @param registryOid the UDDIRegistry to search
     * @return Collection<UDDIProxiedService> of all proxied services published to the UDDI Registry
     * @throws FindException if any problems reading from the database or the UDDIRegistry cannot be found 
     */
    @Transactional(readOnly=true)
    @Secured(types={EntityType.UDDI_PROXIED_SERVICE}, stereotype= MethodStereotype.FIND_ENTITIES)
    Collection<UDDIProxiedService> getAllProxiedServicesForRegistry(long registryOid) throws FindException;

    /**
     * Update UDDIServiceControl without changes to related UDDI.
     *
     * @param uddiServiceControl the updated UDDIServiceControl
     */
    @Secured(types = {EntityType.UDDI_SERVICE_CONTROL}, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    void updateUDDIServiceControlOnly(final UDDIServiceControl uddiServiceControl)
            throws UpdateException;
    /**
     * Find the UDDIServiceControl for a service, if it exists
     *
     * @param serviceOid the service to get the UDDIServiceControl for
     * @return UDDIServiceControl or null if the service does not have one
     * @throws com.l7tech.objectmodel.FindException Any problem finding the service control
     */
    @Transactional(readOnly=true)
    @Secured(types={EntityType.UDDI_SERVICE_CONTROL}, stereotype= MethodStereotype.FIND_ENTITY)
    UDDIServiceControl getUDDIServiceControl(long serviceOid) throws FindException;

    /**
     * Find all UDDIServiceControls for a UDDIRegistry
     *
     * @param registryOid the UDDIRegistry to search
     * @return Collection<UDDIServiceControl> of all service controls for the UDDI Registry
     * @throws FindException if any problems occur
     */
    @Transactional(readOnly=true)
    @Secured(types={EntityType.UDDI_SERVICE_CONTROL}, stereotype= MethodStereotype.FIND_ENTITIES)
    Collection<UDDIServiceControl> getAllServiceControlsForRegistry(long registryOid) throws FindException;


    static class PublishProxiedServiceException extends Exception{
        public PublishProxiedServiceException(String message) {
            super(message);
        }
    }

    static class UDDIRegistryNotEnabledException extends Exception{
        public UDDIRegistryNotEnabledException(String message) {
            super(message);
        }
    }
}
