/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 *
 * Admin interface for UDDI Registries
 *
 * @author darmstrong
 */
package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIPublishStatus;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.*;
import com.l7tech.uddi.UDDIException;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.DELETE_BY_ID;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.FIND_ENTITIES;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.TEST_CONFIGURATION;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types= EntityType.UDDI_REGISTRY)
@Administrative
public interface UDDIRegistryAdmin {

    enum EndpointScheme {HTTP, HTTPS}
    
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype= MethodStereotype.SAVE_OR_UPDATE)
    long saveUDDIRegistry(UDDIRegistry uddiRegistry) throws SaveException, UpdateException, FindException;

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
     * Find a UDDIRegistry with by it's primary key
     *
     * @param registryOid long id of registry to find
     * @return UDDIRegistry. Null if not found e.g. does not exist.
     * @throws FindException Never thrown for the entity not existing. Any db exception is wrapped in a FindException.
     */
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
     * Test if it is possible to connect to the UDDI Registry with the settings contained in the supplied UDDIRegistry
     *
     * Username / password are not required in the UDDIRegistry, we will work with the information supplied.
     *
     * Test should validate the existence of a well known UDDI v3 key such as: uddi:uddi.org:specification:v3_policy
     *
     * Note: this test will only succeed on a UDDI v3 Registry
     *
     * @param uddiRegistry UDDIRegistry to test. May not exist as an entity yet. Required.
     * @throws UDDIException if it's not possible to authenticate
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.UDDI_REGISTRY, stereotype=TEST_CONFIGURATION)
    void testUDDIRegistryAuthentication(UDDIRegistry uddiRegistry) throws UDDIException;

    /**
     * Publish the WSDL for a PublishedService for the first time. This will create as many BusinessServices in UDDI
     * as there are wsdl:service's in the PublishedService's WSDL
     *
     * @param publishedService long oid of the published service
     * @param uddiRegistryOid long oid of the UDDIRegistry
     * @param uddiBusinessKey String key of the owning BusinessEntity from UDDI
     * @param uddiBusinessName String name of the owning BusinessEntity from UDDI
     * @param updateWhenGatewayWsdlChanges boolean if true, then the proxied published service will be udpated as the
     * Gateway's WSDL changes
     * @param securityZone the SecurityZone to assign to the UDDIProxiedServiceInfo created on publish
     * @throws FindException if the published service or uddi registry could not be found
     * @throws PublishProxiedServiceException if the Gateway WSDL cannot be found
     * @throws com.l7tech.objectmodel.UpdateException if the UDDIProxiedService cannot be updated
     * @throws com.l7tech.objectmodel.VersionException if the UDDIProxiedService has an incorrect version
     * @throws com.l7tech.objectmodel.SaveException if the UDDIProxiedService cannot be saved
     * @throws com.l7tech.gateway.common.admin.UDDIRegistryAdmin.UDDIRegistryNotEnabledException if the registry is not enabled
     */
    @Secured(types = EntityType.SERVICE, stereotype = MethodStereotype.SAVE_OR_UPDATE, relevantArg = 0)
    void publishGatewayWsdl(PublishedService publishedService, long uddiRegistryOid, String uddiBusinessKey, String uddiBusinessName,
                            boolean updateWhenGatewayWsdlChanges, @Nullable SecurityZone securityZone)
            throws FindException, PublishProxiedServiceException, VersionException, UpdateException, SaveException, UDDIRegistryNotEnabledException;

    /**
     * Overwrite the Business Service in UDDI which we have a record for.
     *
     * @param publishedServiceIn PublishedService to publish a WSDL to an existing service in UDDI for
     * @param updateWhenGatewayWsdlChanges boolean if true, then the publish is configured to updated when the gateway wsdl changes
     * @param securityZone the SecurityZone to assign to the UDDIProxiedServiceInfo created on publish
     * @throws com.l7tech.objectmodel.FindException
     * @throws com.l7tech.objectmodel.SaveException
     */
    @Secured(types = EntityType.SERVICE, stereotype = MethodStereotype.SAVE_OR_UPDATE, relevantArg = 0)
    void overwriteBusinessServiceInUDDI(PublishedService publishedServiceIn, final boolean updateWhenGatewayWsdlChanges, @Nullable SecurityZone securityZone)
            throws SaveException, FindException;

    /**
     * Publish an endpoint to an existing service in UDDI.
     * <p/>
     * Gateway must have a UDDIServiceControl record for the original service, so that we know where (which UDDI Registry,
     * and which BusinessService) to add the endpoint to.
     * <p/>
     * Note: This method will never result in a GIF publish.
     *
     * @param publishedService the PublishedService who's endpoint will be published to UDDI
     * @param removeOthers     boolean if true, then all other bindingTemplates will be removed from the BusinessService
     * @param properties Map of properties
     * @param securityZone the SecurityZone to assign to the UDDIProxiedServiceInfo created on publish
     * @throws com.l7tech.gateway.common.admin.UDDIRegistryAdmin.UDDIRegistryNotEnabledException
     * @throws com.l7tech.objectmodel.FindException
     * @throws com.l7tech.objectmodel.SaveException
     *
     */
    @Secured(types = EntityType.SERVICE, stereotype = MethodStereotype.SAVE_OR_UPDATE, relevantArg = 0)
    void publishGatewayEndpoint(PublishedService publishedService,
                                boolean removeOthers,
                                Map<String, Object> properties,
                                @Nullable SecurityZone securityZone) throws FindException, SaveException, UDDIRegistryNotEnabledException;

    /**
     * Publish an endpoint to an existing Service in UDDI following the GIF. (Governance Interoperability Framework).
     *
     * @param publishedService the PublishedService who's endpoint will be published to UDDI
     * @param properties Map of properties for the UDDIProxiedServiceInfo being created. It must contain a property
     * called {@link com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo#GIF_SCHEME}, which is of type EndpointScheme
     * for what type of endpoint to publish HTTP or HTTPS
     * @param securityZone the SecurityZone to assign to the UDDIProxiedServiceInfo created on publish
     * @throws FindException
     * @throws SaveException
     * @throws UDDIRegistryNotEnabledException
     */
    @Secured(types = EntityType.SERVICE, stereotype = MethodStereotype.SAVE_OR_UPDATE, relevantArg = 0)
    void publishGatewayEndpointGif(PublishedService publishedService,
                                   Map<String, Object> properties,
                                   @Nullable SecurityZone securityZone) throws FindException, SaveException, UDDIRegistryNotEnabledException;

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
    @Secured(types = EntityType.UDDI_PROXIED_SERVICE_INFO, stereotype = MethodStereotype.SAVE_OR_UPDATE)
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
     * @throws FindException if there is a problem reading from the database or if the UDDIProxiedService is not found
     * @throws UpdateException any problems updating the database
     * @throws com.l7tech.gateway.common.admin.UDDIRegistryAdmin.UDDIRegistryNotEnabledException if the registry is not enabled
     * @throws com.l7tech.objectmodel.DeleteException
     */
    @Secured(types = EntityType.UDDI_PROXIED_SERVICE_INFO, stereotype = MethodStereotype.DELETE_ENTITY)
    void deleteGatewayWsdlFromUDDI(final UDDIProxiedServiceInfo uddiProxiedServiceInfo)
            throws FindException, UDDIRegistryNotEnabledException, UpdateException, DeleteException;

    /**
     * Delete the bindingTemplate which was previously published to an existing BusinessService in UDDI
     *
     * @param uddiProxiedServiceInfo UDDIProxiedServiceInfo which contains the information on the bindingTemplate
     */
    @Secured(types = EntityType.UDDI_PROXIED_SERVICE_INFO, stereotype = MethodStereotype.DELETE_ENTITY)
    void deleteGatewayEndpointFromUDDI(final UDDIProxiedServiceInfo uddiProxiedServiceInfo) throws FindException, UDDIRegistryNotEnabledException, UpdateException, DeleteException;

    /**
     * Allows for non final properties which do not rely on UDDI data like the UDDIProxiedServiceInfo's
     * 'updateProxyOnLocalChange' property to be updated without any any UDDI interaction.
     *
     * @param uddiProxiedServiceInfo UDDIProxiedServiceInfo to update
     * @throws com.l7tech.objectmodel.FindException any problems finding the entity
     * @throws com.l7tech.objectmodel.UpdateException any problems updating the database
     */
    @Secured(types = EntityType.UDDI_PROXIED_SERVICE_INFO, stereotype = MethodStereotype.SAVE_OR_UPDATE)
    void updateProxiedServiceOnly(final UDDIProxiedServiceInfo uddiProxiedServiceInfo)
            throws UpdateException, FindException;

    /**
     * Find the UDDIProxiedService for a service, if it exists
     *
     * @param publishedServiceGoid the service to get the UDDIProxiedService for
     * @return UDDIProxiedService or null if the service does not have one
     * @throws com.l7tech.objectmodel.FindException
     *          Any problem finding the proxied service
     */
    @Transactional(readOnly = true)
    @Secured(types = EntityType.SERVICE, stereotype = MethodStereotype.FIND_ENTITY)
    UDDIProxiedServiceInfo findProxiedServiceInfoForPublishedService(Goid publishedServiceGoid) throws FindException;

    /**
     * Get the UDDIPublishStatus for the UDDIProxiedServiceInfo. Allows the UI to display
     * the most current state of the UDDI publish / delete interaction to the user.
     * <p/>
     * This entity cannot be saved back by the user. There are no admin methods to do this and should not be
     *
     * @param uddiProxiedServiceInfo UDDIProxiedServiceInfo to get publish status information for
     * @param publishedServiceGoid Goid goid of the related published service. ONLY USED FOR RBAC. //todo refactor
     * @return UDDIPublishStatus representing the publish status
     * @throws FindException any problems searching the database
     */
    @Transactional(readOnly = true)
    @Secured(types = EntityType.SERVICE, stereotype = MethodStereotype.GET_PROPERTY_BY_ID, relevantArg = 1)
    UDDIPublishStatus getPublishStatusForProxy(long uddiProxiedServiceInfo, Goid publishedServiceGoid) throws FindException;

    /**
     * Find all UDDIProxiedServices which have been published to a UDDIRegistry
     *
     * @param registryOid the UDDIRegistry to search
     * @return Collection<UDDIProxiedService> of all proxied services published to the UDDI Registry
     * @throws FindException if any problems reading from the database or the UDDIRegistry cannot be found
     */
    @Transactional(readOnly = true)
    @Secured(types = EntityType.UDDI_REGISTRY, stereotype = MethodStereotype.FIND_ENTITIES)
    Collection<UDDIProxiedServiceInfo> getAllProxiedServicesForRegistry(long registryOid) throws FindException;

    /**
     * Save / Update UDDIServiceControl without changes to related UDDI.
     *
     * If the incoming UDDIServiceControl has not been modified, no update is performed. (version will not increment)
     *
     * @param uddiServiceControl the updated UDDIServiceControl
     * @param serviceEndPoint String endpoint of the protected service. If the entity is being saved for the first time
     * it must not be null, empty or contain only spaces. If the entity already exists, this parameter is ignored.
     * @param lastModifiedServiceTimeStamp Long last time the BusinessService or any of it's children were modified
     * in Uddi. Only required when the entity is being saved for the first time. Otherwise it is ignored
     * @return the unique object ID that was updated or created.
     * @throws UpdateException if the UDDIServiceControl cannot be updated
     * @throws SaveException   if the UDDIServiceControl cannot be saved
     * @throws FindException   any problems reading from the database
     * @throws UDDIRegistryAdmin.UDDIRegistryNotEnabledException
     *                         if the UDDI Registry is not enabled
     */
    @Secured(types = EntityType.UDDI_SERVICE_CONTROL, stereotype = MethodStereotype.SAVE_OR_UPDATE, relevantArg = 0)
    long saveUDDIServiceControlOnly(final UDDIServiceControl uddiServiceControl, String serviceEndPoint, Long lastModifiedServiceTimeStamp)
            throws UpdateException, SaveException, FindException, UDDIRegistryNotEnabledException;

    /**
     * Delete the UDDIServiceControl with id uddiServiceControlOid
     * <p/>
     * Cannot delete successfully if the UDDIServiceControl has had an endpoint added to it, or if it has been
     * overwritten
     *
     * @param uddiServiceControlOid long id of the UDDIServiceControl to delete
     * @throws FindException   if there is a problem reading from the database or if the UDDIServiceControl is not found
     * @throws DeleteException any problems deleting from the database
     * @throws UpdateException any problems updating the database
     */
    @Secured(types = EntityType.UDDI_SERVICE_CONTROL, stereotype = MethodStereotype.DELETE_BY_ID)
    void deleteUDDIServiceControl(final long uddiServiceControlOid) throws FindException, DeleteException, UpdateException;

    /**
     * Find the UDDIServiceControl for a service, if it exists
     *
     * @param serviceGoid the service to get the UDDIServiceControl for
     * @return UDDIServiceControl or null if the service does not have one
     * @throws com.l7tech.objectmodel.FindException Any problem finding the service control
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.SERVICE, stereotype= MethodStereotype.FIND_ENTITY)
    UDDIServiceControl getUDDIServiceControl(Goid serviceGoid) throws FindException;

    /**
     * Find the value of the endpoint of the Original Service. This value is used to populate the ${service.defaultRoutingURL}
     * context variable. This is the most upto date value of this value, which may be updated from UDDI.
     *
     * @param serviceControlOid the oid of UDDIServiceControl to retireve the original business service end point value
     * from.
     * @return String most uptodate value of the endPoint of the original business service. Never null or empty.  
     * @throws com.l7tech.objectmodel.FindException Any problem finding the end point
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.UDDI_SERVICE_CONTROL, stereotype= MethodStereotype.GET_PROPERTY_BY_ID)
    String getOriginalServiceEndPoint(long serviceControlOid) throws FindException;

    /**
     * Find all UDDIServiceControls for a UDDIRegistry
     *
     * @param registryOid the UDDIRegistry to search
     * @return Collection<UDDIServiceControl> of all service controls for the UDDI Registry
     * @throws FindException if any problems occur
     */
    @Transactional(readOnly=true)
    @Secured(types={EntityType.UDDI_REGISTRY}, stereotype= MethodStereotype.FIND_ENTITIES)
    Collection<UDDIServiceControl> getAllServiceControlsForRegistry(long registryOid) throws FindException;

    /**
     * Get the list of services headers from the supplied list which have information published to UDDI
     *
     * @param allServiceIds Collection Goid goids of services to query for. Required, cannot be null
     * @return Collection ServiceHeader of all services which have data in UDDI. Can be empty, never null
     * @throws FindException any db problems
     */
    @Transactional(readOnly = true)
    @Secured(types = EntityType.SERVICE, stereotype = MethodStereotype.FIND_ENTITIES)
    Collection<ServiceHeader> getServicesPublishedToUDDI(Collection<Goid> allServiceIds) throws FindException;

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
