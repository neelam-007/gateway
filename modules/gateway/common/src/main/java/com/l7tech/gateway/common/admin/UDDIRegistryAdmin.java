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
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIPublishStatus;
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

    @Transactional(readOnly=true)
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=MethodStereotype.GET_PROPERTY_BY_ID)
    boolean subscriptionNotificationsAvailable(long registryOid);

    @Transactional(readOnly=true)
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=MethodStereotype.GET_PROPERTY_BY_ID)
    boolean metricsAvailable(long registryOid) throws FindException;

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
     * Publish the WSDL for a PublishedService for the first time. This will create as many BusinessServices in UDDI
     * as there are wsdl:service's in the PublishedService's WSDL
     *
     * @param publishedServiceOid long oid of the published service
     * @param uddiRegistryOid long oid of the UDDIRegistry
     * @param uddiBusinessKey String key of the owning BusinessEntity from UDDI
     * @param uddiBusinessName String name of the owning BusinessEntity from UDDI
     * @param updateWhenGatewayWsdlChanges boolean if true, then the proxied published service will be udpated as the
     * Gateway's WSDL changes
     * @throws FindException if the published service or uddi registry could not be found
     * @throws PublishProxiedServiceException if the Gateway WSDL cannot be found
     * @throws com.l7tech.objectmodel.UpdateException if the UDDIProxiedService cannot be updated
     * @throws com.l7tech.objectmodel.VersionException if the UDDIProxiedService has an incorrect version
     * @throws com.l7tech.objectmodel.SaveException if the UDDIProxiedService cannot be saved
     * @throws com.l7tech.gateway.common.admin.UDDIRegistryAdmin.UDDIRegistryNotEnabledException if the registry is not enabled
     */
    @Secured(types = {EntityType.UDDI_PROXIED_SERVICE_INFO}, stereotype = MethodStereotype.SAVE_OR_UPDATE)//todo why doesn't save only work?
    void publishGatewayWsdl(long publishedServiceOid, long uddiRegistryOid, String uddiBusinessKey, String uddiBusinessName,
                            boolean updateWhenGatewayWsdlChanges)
            throws FindException, PublishProxiedServiceException, VersionException, UpdateException, SaveException, UDDIRegistryNotEnabledException;

    /**
     * Publish an endpoint to an existing service in UDDI
     *
     * Service must be logically under UDDI control
     * 
     * @param publishedServiceOid long oid of the published service
     * @param removeOthers boolean if true, then all other bindingTemplates will be removed from the BusinessService
     */
    @Secured(types = {EntityType.UDDI_PROXIED_SERVICE_INFO}, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    void publishGatewayEndpoint(long publishedServiceOid, boolean removeOthers) throws FindException, SaveException, UDDIRegistryNotEnabledException;

    /**
     * Updated the UDDI with changes to a Published Service's WSDL
     *
     * This only exists so that the UI can trigger an update to UDDI when the 'Update when gateway wsdl changes'
     * check box is checked
     * 
     * @param uddiProxiedServiceInfoOid long oid of the UDDIProxiedServiceInfo
     *
     * @throws FindException
     * @throws UDDIRegistryNotEnabledException
     * @throws PublishProxiedServiceException
     * @throws UpdateException
     * @throws VersionException
     */
    @Secured(types = {EntityType.UDDI_PROXIED_SERVICE_INFO}, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    void updatePublishedGatewayWsdl(long uddiProxiedServiceInfoOid)
            throws FindException, UDDIRegistryNotEnabledException, PublishProxiedServiceException, UpdateException, VersionException;

    /**
     * Delete all published Business Services which were published to UDDI from the Gateway's WSDL.
     * <p/>
     * This is a best effort only. Regardless of the success from UDDI, we will delete our UDDIProxiedService.
     *
     * Registry must be enabled
     *
     * @param uddiProxiedServiceInfo UDDIProxiedServiceInfo and associated data in UDDI to delete
     * @return String error message if there were any problems deleting from UDDI. null if no errors
     * @throws FindException if there is a problem reading from the database or if the UDDIProxiedService is not found
     * @throws UpdateException any problems updating the database
     * @throws com.l7tech.gateway.common.admin.UDDIRegistryAdmin.UDDIRegistryNotEnabledException if the registry is not enabled
     */
    @Secured(types = {EntityType.UDDI_PROXIED_SERVICE_INFO}, stereotype = MethodStereotype.DELETE_ENTITY)
    void deleteGatewayWsdlFromUDDI(final UDDIProxiedServiceInfo uddiProxiedServiceInfo)
            throws FindException, UDDIRegistryNotEnabledException, UpdateException, DeleteException;

    /**
     * Delete the bindingTemplate which was previously published to an existing BusinessService in UDDI
     *
     * @param uddiProxiedServiceInfo UDDIProxiedServiceInfo which contains the information on the bindingTemplate
     */
    @Secured(types = {EntityType.UDDI_PROXIED_SERVICE_INFO}, stereotype = MethodStereotype.DELETE_ENTITY)
    void deleteGatewayEndpointFromUDDI(final UDDIProxiedServiceInfo uddiProxiedServiceInfo) throws FindException, UDDIRegistryNotEnabledException, UpdateException;

    /**
     * Allows for non final properties which do not rely on UDDI data like the UDDIProxiedServiceInfo's
     * 'updateProxyOnLocalChange' property to be udpated without any any UDDI interaction.
     *
     * @param uddiProxiedServiceInfo UDDIProxiedServiceInfo to update
     * @throws com.l7tech.objectmodel.FindException any problems finding the entity
     * @throws com.l7tech.objectmodel.UpdateException any problems updating the database
     */
    @Secured(types = {EntityType.UDDI_PROXIED_SERVICE_INFO}, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    void updateProxiedServiceOnly(final UDDIProxiedServiceInfo uddiProxiedServiceInfo)
            throws UpdateException, FindException;

    /**
     * Find the UDDIProxiedService for a service, if it exists
     *
     * @param serviceOid the service to get the UDDIProxiedServiec for
     * @return UDDIProxiedService or null if the service does not have one
     * @throws com.l7tech.objectmodel.FindException Any problem finding the proxied service
     */
    @Transactional(readOnly=true)
    @Secured(types={EntityType.UDDI_PROXIED_SERVICE_INFO}, stereotype= MethodStereotype.FIND_ENTITY)
    UDDIProxiedServiceInfo getUDDIProxiedServiceInfo(long serviceOid) throws FindException;

    /**
     * Get the UDDIPublishStatus for the UDDIProxiedServiceInfo. Allows the UI to display
     * the most current state of the UDDI publish / delete interaction to the user.
     *
     * This entity cannot be saved back by the user. There are no admin methods to do this and should not be
     *
     * @param uddiProxiedServiceInfo long oid of the UDDIProxiedServiceInfo to get publish status information for
     * @return UDDIPublishStatus representing the publish status
     * @throws FindException any problems searching the database
     */
    @Transactional(readOnly=true)
    @Secured(types={EntityType.UDDI_PROXIED_SERVICE_INFO}, stereotype= MethodStereotype.FIND_ENTITY)
    UDDIPublishStatus getPublishStatusForProxy(long uddiProxiedServiceInfo) throws FindException;

    /**
     * Find all UDDIProxiedServices which have been published to a UDDIRegistry
     *
     * @param registryOid the UDDIRegistry to search
     * @return Collection<UDDIProxiedService> of all proxied services published to the UDDI Registry
     * @throws FindException if any problems reading from the database or the UDDIRegistry cannot be found 
     */
    @Transactional(readOnly=true)
    @Secured(types={EntityType.UDDI_PROXIED_SERVICE_INFO}, stereotype= MethodStereotype.FIND_ENTITIES)
    Collection<UDDIProxiedServiceInfo> getAllProxiedServicesForRegistry(long registryOid) throws FindException;

    /**
     * Save / Update UDDIServiceControl without changes to related UDDI.
     *
     * @param uddiServiceControl the updated UDDIServiceControl
     * @throws com.l7tech.objectmodel.UpdateException if the UDDIServiceControl cannot be updated
     * @throws com.l7tech.objectmodel.SaveException if the UDDIServiceControl cannot be saved
     * @return the unique object ID that was updated or created.
     * @throws com.l7tech.objectmodel.FindException any problems reading from the database
     */
    @Secured(types = {EntityType.UDDI_SERVICE_CONTROL}, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    long saveUDDIServiceControlOnly(final UDDIServiceControl uddiServiceControl)
            throws UpdateException, SaveException, FindException;

    /**
     * Delete the UDDIServiceControl with id uddiServiceControlOid
     *
     * @param uddiServiceControlOid long id of the UDDIServiceControl to delete
     * @throws FindException   if there is a problem reading from the database or if the UDDIServiceControl is not found
     * @throws DeleteException any problems deleting from the database
     */
    @Secured(types = {EntityType.UDDI_SERVICE_CONTROL}, stereotype = MethodStereotype.DELETE_BY_ID)
    void deleteUDDIServiceControl(final long uddiServiceControlOid) throws FindException, DeleteException;

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
