package com.l7tech.server.uddi;

import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.uddi.*;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.ServerConfig;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public class UDDIRegistryAdminImpl implements UDDIRegistryAdmin {
    protected static final Logger logger = Logger.getLogger(UDDIRegistryAdminImpl.class.getName());

    final private UDDIRegistryManager uddiRegistryManager;
    final private UDDIHelper uddiHelper;
    final private UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager;
    final private UDDIPublishStatusManager uddiPublishStatusManager;
    final private UDDIServiceControlManager uddiServiceControlManager;
    final private UDDICoordinator uddiCoordinator;
    final private ServiceCache serviceCache;

    public UDDIRegistryAdminImpl(final UDDIRegistryManager uddiRegistryManager,
                                 final UDDIHelper uddiHelper,
                                 final UDDIServiceControlManager uddiServiceControlManager,
                                 final UDDICoordinator uddiCoordinator,
                                 final ServiceCache serviceCache,
                                 final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                                 final UDDIPublishStatusManager uddiPublishStatusManager) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiHelper = uddiHelper;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiCoordinator = uddiCoordinator;
        this.serviceCache = serviceCache;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.uddiPublishStatusManager = uddiPublishStatusManager;
    }

    @Override
    public long saveUDDIRegistry(final UDDIRegistry uddiRegistry) throws SaveException, UpdateException {
        if(uddiRegistry.getOid() == PersistentEntity.DEFAULT_OID){
            logger.info("Saving UDDI Registry '" + uddiRegistry.getName()+"'");
            uddiRegistryManager.save(uddiRegistry);
        }else{
            logger.info("Updating UDDI Registry '" + uddiRegistry.getName()+"' oid = " + uddiRegistry.getOid());
            uddiRegistryManager.update(uddiRegistry);
        }

        return uddiRegistry.getOid();
    }

    @Override
    public void deleteUDDIRegistry(final long oid) throws DeleteException, FindException, UDDIException, UDDIRegistryNotEnabledException {

        UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(oid);
        if(uddiRegistry == null) throw new FindException("Could not find UDDI Registry to delete");

        //delete all UDDIProxiedService's which belong to this registry
//        Collection<UDDIProxiedServiceInfo> allProxiedServices = uddiRegistryManager.findAllByRegistryOid(uddiRegistry.getOid());
//        for(UDDIProxiedServiceInfo proxiedService: allProxiedServices){
//            proxiedService.getUddiPublishStatus().setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
//        }

        //TODO [Donal] implement clean up of UDDI registry
        //find all entities using this registry and delete
        logger.log(Level.INFO, "Deleting UDDI Registry oid = " + oid);
        uddiRegistryManager.delete(oid);
    }

    @Override
    public Collection<UDDIRegistry> findAllUDDIRegistries() throws FindException {
        return uddiRegistryManager.findAll();
    }

    @Override
    public UDDIRegistry findByPrimaryKey(long registryOid) throws FindException {
        return uddiRegistryManager.findByPrimaryKey(registryOid);
    }

    @Override
    public boolean subscriptionNotificationsAvailable( final long registryOid ) {
        return uddiCoordinator.isNotificationServiceAvailable( registryOid );
    }

    @Override
    public boolean metricsAvailable( final long registryOid ) throws FindException {
        boolean metrics = false;
        UDDIRegistry registry = findByPrimaryKey( registryOid );
        if ( registry != null ) {
            metrics = UDDIRegistry.UDDIRegistryType.CENTRASITE_ACTIVE_SOA.toString().equals(registry.getUddiRegistryType());
        }
        return metrics;
    }

    @Override
    public void testUDDIRegistryAuthentication(final UDDIRegistry uddiRegistry) throws UDDIException {
        final boolean loginSupplied = uddiRegistry.getRegistryAccountUserName() != null && !uddiRegistry.getRegistryAccountUserName().trim().isEmpty();
        final boolean passwordSupplied = uddiRegistry.getRegistryAccountPassword() != null && !uddiRegistry.getRegistryAccountPassword().trim().isEmpty();

        if(!loginSupplied || !passwordSupplied){
            throw new UDDIException("A username and password is required to test UDDI authentication");
        }

        final UDDIClient uddiClient = getUDDIClient(uddiRegistry);        
        try {
            uddiClient.authenticate();
        } catch (UDDIException e) {
            //the original exception may not be serializable
            throw new UDDIException(ExceptionUtils.getMessage(e));
        }
    }

    private UDDIClient getUDDIClient(final UDDIRegistry uddiRegistry) {
        return uddiHelper.newUDDIClient( uddiRegistry );
    }

    @Override
    public UDDIProxiedServiceInfo getUDDIProxiedServiceInfo(long serviceOid) throws FindException {
        return uddiProxiedServiceInfoManager.findByPublishedServiceOid(serviceOid);
    }

    @Override
    public UDDIPublishStatus getPublishStatusForProxy(final long uddiProxiedServiceInfoOid) throws FindException {
        final UDDIPublishStatus publishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfoOid);
        if(publishStatus == null)
            throw new FindException("Cannot find the UDDIPublishStatus for UDDIProxiedServiceInfo with id#(" + uddiProxiedServiceInfoOid+")");

        return publishStatus;
    }

    @Override
    public void deleteGatewayEndpointFromUDDI(final UDDIProxiedServiceInfo proxiedServiceInfo)
            throws FindException, UDDIRegistryNotEnabledException, UpdateException {
        if(proxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.ENDPOINT)
            throw new IllegalStateException("Cannot delete UDDIProxiedServiecInfo as gateway URL was not published as a bindingTemplate.");

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(proxiedServiceInfo.getUddiRegistryOid());
        if(!uddiRegistry.isEnabled()) throw new UDDIRegistryNotEnabledException("UDDIRegistry is not enabled. Cannot use");

        final UDDIPublishStatus uddiPublishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoOid(proxiedServiceInfo.getOid());
        if(uddiPublishStatus == null)
            throw new FindException("Cannot find UDDIPublishStatus for UDDIProxiedServiceInfo with id#(" + proxiedServiceInfo.getOid() + ")");

        uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
        /**
         * This triggers an entity invalidation event which the UDDICoordinator picks up
         */
        uddiPublishStatusManager.update(uddiPublishStatus);
    }

    @Override
    public void deleteGatewayWsdlFromUDDI(final UDDIProxiedServiceInfo proxiedServiceInfo)
            throws FindException, UDDIRegistryNotEnabledException, UpdateException, DeleteException {

        if(proxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY)
            throw new IllegalStateException("Cannot delete UDDIProxiedServiecInfo as gateway WSDL was not published.");

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(proxiedServiceInfo.getUddiRegistryOid());
        if(!uddiRegistry.isEnabled()) throw new UDDIRegistryNotEnabledException("UDDIRegistry is not enabled. Cannot use");

        final UDDIPublishStatus uddiPublishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoOid(proxiedServiceInfo.getOid());
        if(uddiPublishStatus == null)
            throw new FindException("Cannot find UDDIPublishStatus for UDDIProxiedServiceInfo with id#(" + proxiedServiceInfo.getOid() + ")");

        if (uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.CANNOT_DELETE ||
                uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.CANNOT_PUBLISH ||
                uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED ||
                uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED  ||
                uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.NONE) {

            //if we cannot delete or we have already tried, allow the user to stop any more attempts
            //this may cause queued tasks problems, but they will just fail
            if (uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                logger.log(Level.WARNING, "Stopping attept to delete from UDDI. Data may be orphaned in UDDI");
            }
            uddiProxiedServiceInfoManager.delete(proxiedServiceInfo);
        } else if (uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISHED) {
            uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
            uddiPublishStatusManager.update(uddiPublishStatus);
            logger.log(Level.INFO, "Set status to delete for published UDDI data");
        }else if(uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH){
            logger.log(Level.WARNING, "Cannot delete Gateway WSDL from UDDI while it is being published");
        }
    }

    @Override
    public void updateProxiedServiceOnly(UDDIProxiedServiceInfo proxiedServiceInfo) throws UpdateException, FindException {
        final UDDIProxiedServiceInfo original = uddiProxiedServiceInfoManager.findByPrimaryKey(proxiedServiceInfo.getOid());
        if(original == null) throw new FindException("Could not find UDDIProxiedServiceInfo with id: " + proxiedServiceInfo);

        proxiedServiceInfo.throwIfFinalPropertyModified(original);

        if(original.isUpdateProxyOnLocalChange() == proxiedServiceInfo.isUpdateProxyOnLocalChange() &&
           original.isMetricsEnabled() == proxiedServiceInfo.isMetricsEnabled() ) return;

        uddiProxiedServiceInfoManager.update(proxiedServiceInfo);
    }

    @Override
    public Collection<UDDIProxiedServiceInfo> getAllProxiedServicesForRegistry(long registryOid) throws FindException {
        return uddiRegistryManager.findAllByRegistryOid(registryOid);
    }

    @Override
    public long saveUDDIServiceControlOnly( final UDDIServiceControl uddiServiceControl )
            throws UpdateException, SaveException, FindException {
        if ( uddiServiceControl.getOid() == UDDIServiceControl.DEFAULT_OID ){
            final PublishedService service = serviceCache.getCachedService(uddiServiceControl.getPublishedServiceOid());

            //validate that an end point can be found
            final String endPoint = uddiServiceControl.getAccessPointUrl();
            //TODO [Donal] clean up this
//            try {
//                endPoint = UDDIUtilities.extractEndPointFromWsdl(wsdl, uddiServiceControl.getWsdlServiceName(),
//                        uddiServiceControl.getWsdlPortName(), uddiServiceControl.getWsdlPortBinding());
//            } catch (UDDIUtilities.WsdlEndPointNotFoundException e) {
//                final String msg = "Cannot save UDDIServiceControl as no valid endpoint can be found from the Published Service's WSDL: "
//                        + ExceptionUtils.getMessage(e);
//                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
//                throw new SaveException(msg);
//            }

            //make sure that we are not monitoring an endpoint of this SSG
            if(UDDIUtilities.isGatewayUrl(endPoint, ServerConfig.getInstance().getHostname())){
                final String msg = "Cannot save UDDIServiceControl as the WSDL endpoint routes back to the SecureSpan Gateway";
                logger.log(Level.WARNING, msg);
                throw new SaveException(msg);
            }

            final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiServiceControl.getUddiRegistryOid());
            if(uddiRegistry == null) throw new FindException("Cannot find UDDIRegistry");

            final UDDIClient uddiClient = getUDDIClient(uddiRegistry);
            try {
                final String businessName = uddiClient.getBusinessEntityName(uddiServiceControl.getUddiBusinessKey());
                uddiServiceControl.setUddiBusinessName(businessName);
            } catch (UDDIException e) {
                final String msg = "Cannot find BusinessEntity with businessKey: " + uddiServiceControl.getUddiBusinessKey();
                logger.log(Level.WARNING, msg, e);
                throw new SaveException(msg);
            }
            return uddiServiceControlManager.save(uddiServiceControl);
        }else{
            UDDIServiceControl original = uddiServiceControlManager.findByPrimaryKey(uddiServiceControl.getOid());
            if(original == null) throw new FindException("Cannot find UDDIServiceControl with oid: " + uddiServiceControl.getOid());
            uddiServiceControl.throwIfFinalPropertyModified(original);
            //make sure the wsdl under uddi control is not being set when it is not allowed
            if(uddiServiceControl.isHasHadEndpointRemoved() && uddiServiceControl.isUnderUddiControl()){
                throw new UpdateException("Cannot save UDDIServiceControl. Incorrect state. " +
                        "WSDL cannot be under UDDI control when the owning business service in UDDI has had its bindingTemplates removed");
            }
            uddiServiceControlManager.update( uddiServiceControl );
            return uddiServiceControl.getOid();
        }
    }

    @Override
    public void deleteUDDIServiceControl(long uddiServiceControlOid) throws FindException, DeleteException {
        uddiServiceControlManager.delete(uddiServiceControlOid);
    }

    @Override
    public UDDIServiceControl getUDDIServiceControl( final long serviceOid ) throws FindException {
        return uddiServiceControlManager.findByPublishedServiceOid( serviceOid );
    }

    @Override
    public Collection<UDDIServiceControl> getAllServiceControlsForRegistry( final long registryOid ) throws FindException {
        return uddiServiceControlManager.findByUDDIRegistryOid( registryOid );
    }

    @Override
    public void publishGatewayEndpoint(long publishedServiceOid, boolean removeOthers) throws FindException, SaveException, UDDIRegistryNotEnabledException {
        //todo [Donal] implement removeOthers
        if(publishedServiceOid < 1) throw new IllegalArgumentException("publishedServiceOid must be >= 1");

        final PublishedService service = serviceCache.getCachedService(publishedServiceOid);
        if(service == null) throw new SaveException("PublishedService with id #(" + publishedServiceOid + ") was not found");

        final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPublishedServiceOid(publishedServiceOid);
        if(serviceControl == null) throw new SaveException("PublishedService with id #("+publishedServiceOid+") was not created from UDDI (record may have been deleted)");

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(serviceControl.getUddiRegistryOid());
        if(uddiRegistry == null) throw new SaveException("UDDIRegistry with id #("+serviceControl.getUddiRegistryOid()+") was not found");
        if(!uddiRegistry.isEnabled()) throw new UDDIRegistryNotEnabledException("UDDIRegistry with id #("+serviceControl.getUddiRegistryOid()+") is not enabled");

        if(serviceControl.isUnderUddiControl() && removeOthers)
            throw new SaveException("Published service with id #("+publishedServiceOid+") is not under UDDI control so cannot remove existing bindingTemplates");

        final String wsdlHash;
        try {
            wsdlHash = service.parsedWsdl().getHash();
        } catch (Exception e) {
            final String msg = "Cannot get generate hash of WSDL";
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            throw new RuntimeException(msg, ExceptionUtils.getDebugException(e));
        }

        final UDDIProxiedServiceInfo serviceInfo = UDDIProxiedServiceInfo.getEndPointPublishInfo(service.getOid(),
                uddiRegistry.getOid(), serviceControl.getUddiBusinessKey(), serviceControl.getUddiBusinessName(),
                wsdlHash, removeOthers);

        final long oid = uddiProxiedServiceInfoManager.save(serviceInfo);
        final UDDIPublishStatus newStatus = new UDDIPublishStatus(oid, UDDIPublishStatus.PublishStatus.PUBLISH);
        uddiPublishStatusManager.save(newStatus);
        //the save event is picked up by the UDDICoordinator, which does the UDDI work
    }

    @Override
    public void publishGatewayWsdl(final long publishedServiceOid,
                                   final long uddiRegistryOid,
                                   final String uddiBusinessKey,
                                   final String uddiBusinessName,
                                   final boolean updateWhenGatewayWsdlChanges)
            throws FindException, SaveException, UDDIRegistryNotEnabledException {

        if(publishedServiceOid < 1) throw new IllegalArgumentException("publishedServiceOid must be >= 1");
        if(uddiRegistryOid < 1) throw new IllegalArgumentException("uddiRegistryOid must be > 1");
        if(uddiBusinessKey == null || uddiBusinessKey.trim().isEmpty()) throw new IllegalArgumentException("uddiBusinessKey cannot be null or empty");
        if(uddiBusinessName == null || uddiBusinessName.trim().isEmpty()) throw new IllegalArgumentException("uddiBusinessName cannot be null or empty");

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiRegistryOid);
        if(uddiRegistry == null) throw new IllegalArgumentException("Cannot find UDDIRegistry with oid: " + uddiRegistryOid);
        if(!uddiRegistry.isEnabled()) throw new UDDIRegistryNotEnabledException("UDDIRegistry with id #("+uddiRegistry.getOid()+") is not enabled");

        final PublishedService service = serviceCache.getCachedService(publishedServiceOid);
        if(service == null) throw new SaveException("PublishedService with id #(" + publishedServiceOid + ") was not found");

        final String wsdlHash;
        try {
            wsdlHash = service.parsedWsdl().getHash();
        } catch (Exception e) {
            final String msg = "Cannot get generate hash of WSDL";
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            throw new RuntimeException(msg, ExceptionUtils.getDebugException(e));
        }

        final UDDIProxiedServiceInfo uddiProxiedServiceInfo = UDDIProxiedServiceInfo.getProxyServicePublishInfo(service.getOid(),
                uddiRegistry.getOid(), uddiBusinessKey, uddiBusinessName,
                wsdlHash, updateWhenGatewayWsdlChanges);

        final long oid = uddiProxiedServiceInfoManager.save(uddiProxiedServiceInfo);
        final UDDIPublishStatus newStatus = new UDDIPublishStatus(oid, UDDIPublishStatus.PublishStatus.NONE);
        uddiPublishStatusManager.save(newStatus);
        //the save event is picked up by the UDDICoordinator, which does the UDDI work
    }

    @Override
    public void updatePublishedGatewayWsdl(long uddiProxiedServiceInfoOid)
            throws FindException, UDDIRegistryNotEnabledException, PublishProxiedServiceException, UpdateException,
            VersionException {
        final UDDIProxiedServiceInfo uddiProxiedServiceInfo = uddiProxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
        if(uddiProxiedServiceInfo == null) throw new FindException("Cannot find UDDIProxiedServiceInfo with id: " + uddiProxiedServiceInfoOid);

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
        if(!uddiRegistry.isEnabled()) throw new UDDIRegistryNotEnabledException("UDDIRegistry is not enabled. Cannot use");

        final UDDIPublishStatus uddiPublishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
        if(uddiPublishStatus == null)
            throw new FindException("Cannot find UDDIPublishStatus for UDDIProxiedServiceInfo with id#(" + uddiProxiedServiceInfo.getOid() + ")");

        uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISH);
        //the update event is picked up by the UDDICoordinator, which does the UDDI work
        uddiPublishStatusManager.update(uddiPublishStatus);
    }
}
