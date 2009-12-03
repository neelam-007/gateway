package com.l7tech.server.uddi;

import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.uddi.*;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.wsdl.Wsdl;

import javax.wsdl.WSDLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    final private UDDIServiceControlMonitorRuntimeManager uddiServiceControlMonitorRuntimeManager;
    final private UDDICoordinator uddiCoordinator;
    final private ServiceCache serviceCache;
    final private UDDIBusinessServiceStatusManager businessServiceStatusManager;

    private static final String UDDI_ORG_SPECIFICATION_V3_POLICY = "uddi:uddi.org:specification:v3_policy";

    public UDDIRegistryAdminImpl(final UDDIRegistryManager uddiRegistryManager,
                                 final UDDIHelper uddiHelper,
                                 final UDDIServiceControlManager uddiServiceControlManager,
                                 final UDDICoordinator uddiCoordinator,
                                 final ServiceCache serviceCache,
                                 final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                                 final UDDIPublishStatusManager uddiPublishStatusManager,
                                 final UDDIServiceControlMonitorRuntimeManager uddiServiceControlMonitorRuntimeManager,
                                 final UDDIBusinessServiceStatusManager businessServiceStatusManager) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiHelper = uddiHelper;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiCoordinator = uddiCoordinator;
        this.serviceCache = serviceCache;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.uddiPublishStatusManager = uddiPublishStatusManager;
        this.uddiServiceControlMonitorRuntimeManager = uddiServiceControlMonitorRuntimeManager;
        this.businessServiceStatusManager = businessServiceStatusManager;
    }

    @Override
    public long saveUDDIRegistry(final UDDIRegistry uddiRegistry) throws SaveException, UpdateException, FindException {
        if(uddiRegistry.getOid() == PersistentEntity.DEFAULT_OID){
            if(uddiRegistry.getName().trim().isEmpty()){
                throw new SaveException("Cannot save a UDDI Registry with an emtpy name (or only containing spaces)");
            }
            uddiRegistryManager.save(uddiRegistry);
            logger.info("Saved UDDI Registry '" + uddiRegistry.getName()+"'");
        }else{
            if(uddiRegistry.getName().trim().isEmpty()){
                throw new UpdateException("Cannot update a UDDI Registry to have an emtpy name (or only containing spaces)");
            }
            final UDDIRegistry original = uddiRegistryManager.findByPrimaryKey(uddiRegistry.getOid());
            if(original == null) throw new FindException("Cannot find UDDIRegistry with id#(" + uddiRegistry.getOid()+")");

            if(original.isSubscribeForNotifications() != uddiRegistry.isSubscribeForNotifications()){
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_COMMITTED) {
                            logger.log(Level.FINE, "Method of monitoring has changed. Firing events for each published service monitoring UDDI Registry with id#(" + uddiRegistry.getOid()+")");
                            uddiCoordinator.notifyEvent(new UpdateAllMonitoredServicesUDDIEvent(uddiRegistry.getOid()));
                        }
                    }
                });
            }

            uddiRegistryManager.update(uddiRegistry);
            logger.info("Updated UDDI Registry '" + uddiRegistry.getName()+"' oid = " + uddiRegistry.getOid());
        }

        return uddiRegistry.getOid();
    }

    @Override
    public void deleteUDDIRegistry(final long oid) throws DeleteException, FindException, UDDIException, UDDIRegistryNotEnabledException {

        UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(oid);
        if(uddiRegistry == null) throw new FindException("Could not find UDDI Registry to delete");

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
        if(uddiRegistry == null) throw new NullPointerException("uddiRegistry cannot be null");

        UDDIClient uddiClient = null;
        try {
            uddiClient = getUDDIClient(uddiRegistry);
            uddiClient.getOperationalInfo(UDDI_ORG_SPECIFICATION_V3_POLICY);
        } catch (UDDIException e) {
            //the original exception may not be serializable
            throw new UDDIException(ExceptionUtils.getMessage(e));
        } finally {
            ResourceUtils.closeQuietly( uddiClient );
        }
    }

    private UDDIClient getUDDIClient(final UDDIRegistry uddiRegistry) {
        return uddiHelper.newUDDIClient( uddiRegistry );
    }

    @Override
    public UDDIProxiedServiceInfo findProxiedServiceInfoForPublishedService(long serviceOid) throws FindException {
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
            throws FindException, UDDIRegistryNotEnabledException, UpdateException, DeleteException {
        if(proxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.ENDPOINT)
            throw new IllegalStateException("Cannot delete UDDIProxiedServiecInfo as gateway URL was not published as a bindingTemplate.");

        handleUddiProxiedServiceInfoDelete(proxiedServiceInfo);
    }

    @Override
    public void deleteGatewayWsdlFromUDDI(final UDDIProxiedServiceInfo proxiedServiceInfo)
            throws FindException, UDDIRegistryNotEnabledException, UpdateException, DeleteException {

        if(proxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY &&
                proxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.OVERWRITE)
            throw new IllegalStateException("Cannot delete UDDIProxiedServiecInfo as gateway WSDL was not published.");

        handleUddiProxiedServiceInfoDelete(proxiedServiceInfo);
    }

    private void handleUddiProxiedServiceInfoDelete(UDDIProxiedServiceInfo proxiedServiceInfo)
            throws DeleteException, UpdateException, FindException, UDDIRegistryNotEnabledException {

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(proxiedServiceInfo.getUddiRegistryOid());
        throwIfGatewayNotEnabled(uddiRegistry);

        final UDDIPublishStatus uddiPublishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoOid(proxiedServiceInfo.getOid());
        if(uddiPublishStatus == null)
            throw new FindException("Cannot find UDDIPublishStatus for UDDIProxiedServiceInfo with id#(" + proxiedServiceInfo.getOid() + ")");

        if (uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.CANNOT_DELETE ||
                uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.CANNOT_PUBLISH ||
                uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED ||
                uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED ) {

            //if we cannot delete or we have already tried, allow the user to stop any more attempts
            //this may cause queued tasks problems, but they will just fail
            if (uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                logger.log(Level.WARNING, "Stopping attempt to delete from UDDI. Data may be orphaned in UDDI");
            }
            uddiProxiedServiceInfoManager.delete(proxiedServiceInfo);
        } else if (uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISHED) {
            uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
            //This triggers an entity invalidation event which the UDDICoordinator picks up
            uddiPublishStatusManager.update(uddiPublishStatus);
            logger.log(Level.INFO, "Set status to delete for published UDDI data");
        }else if(uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH){
            logger.log(Level.WARNING, "Cannot delete Gateway WSDL from UDDI while it is being published");
        }else if(uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE){
            logger.log(Level.WARNING, "UDDI data is currently set to delete. Please wait for delete to complete");
        }
    }

    @Override
    public void updateProxiedServiceOnly(UDDIProxiedServiceInfo proxiedServiceInfo) throws UpdateException, FindException {
        final UDDIProxiedServiceInfo original = uddiProxiedServiceInfoManager.findByPrimaryKey(proxiedServiceInfo.getOid());
        if(original == null) throw new FindException("Could not find UDDIProxiedServiceInfo with id: " + proxiedServiceInfo);

        proxiedServiceInfo.throwIfFinalPropertyModified(original);

        if(original.isUpdateProxyOnLocalChange() == proxiedServiceInfo.isUpdateProxyOnLocalChange() &&
           original.isMetricsEnabled() == proxiedServiceInfo.isMetricsEnabled() &&
           original.isPublishWsPolicyEnabled() == proxiedServiceInfo.isPublishWsPolicyEnabled() &&
           original.isPublishWsPolicyFull() == proxiedServiceInfo.isPublishWsPolicyFull() &&
           original.isPublishWsPolicyInlined() == proxiedServiceInfo.isPublishWsPolicyInlined()) return;

        //check if we should trigger a publish to UDDI, but only when the check box has been checked and is being saved since it was unchecked
        if(proxiedServiceInfo.isUpdateProxyOnLocalChange() &&
                original.isUpdateProxyOnLocalChange() != proxiedServiceInfo.isUpdateProxyOnLocalChange()){
            final UDDIPublishStatus publishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoOid(proxiedServiceInfo.getOid());
            if(publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISHED){
                publishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISH);
                uddiPublishStatusManager.update(publishStatus);
                logger.log(Level.INFO, "Set gateway WSDL to update in UDDI as it will now be kept synchronized with the Gateway");
            }
        }

        //the above code must happen first, as hibernate is smart and will update the 'original' reference above
        //once we save the incoming entity with the same identity
        uddiProxiedServiceInfoManager.update(proxiedServiceInfo);

    }

    @Override
    public Collection<UDDIProxiedServiceInfo> getAllProxiedServicesForRegistry(long registryOid) throws FindException {
        return uddiRegistryManager.findAllByRegistryOid(registryOid);
    }

    @Override
    public long saveUDDIServiceControlOnly(final UDDIServiceControl uddiServiceControl, final Long lastModifiedServiceTimeStamp)
            throws UpdateException, SaveException, FindException, UDDIRegistryNotEnabledException {
        if ( uddiServiceControl.getOid() == UDDIServiceControl.DEFAULT_OID ){
            if(lastModifiedServiceTimeStamp == null || lastModifiedServiceTimeStamp < 0){
                throw new SaveException("lastModifiedServiceTimeStamp is required when saving a UDDIServiceControl. Cannot be null or negative");    
            }

            final PublishedService service = serviceCache.getCachedService(uddiServiceControl.getPublishedServiceOid());

            final Wsdl wsdl;
            try {
                wsdl = service.parsedWsdl();
            } catch (WSDLException e) {
                throw new SaveException("Cannot parse services WSDL: " + ExceptionUtils.getDebugException(e));
            }
            //validate that an end point can be found
            final String endPoint = uddiServiceControl.getAccessPointUrl();
            //TODO [Donal] Add namespace support for wsdl:service if needed
            final boolean wsdlImplementUddiWsdlPort = UDDIUtilities.validatePortBelongsToWsdl(wsdl, uddiServiceControl.getWsdlServiceName(), null,
                    uddiServiceControl.getWsdlPortName(), uddiServiceControl.getWsdlPortBinding(), uddiServiceControl.getWsdlPortBindingNamespace());

            if(!wsdlImplementUddiWsdlPort){
                throw new SaveException("The published service's WSDL does not contain the selected wsdl:port / wsdl:binding chosen from the original BusinessService in UDDI." +
                        "\nHint: if the gateway WSDL is stale, it can be updated from the WSDL tab");
            }
            
            //make sure that we are not monitoring an endpoint of this SSG
            if(uddiHelper.isGatewayUrl(endPoint)){
                final String msg = "WSDL endpoint routes back to the SecureSpan Gateway";
                logger.log(Level.WARNING, msg);
                throw new SaveException(msg);
            }

            final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiServiceControl.getUddiRegistryOid());
            if(uddiRegistry == null) throw new FindException("Cannot find UDDIRegistry");
            throwIfGatewayNotEnabled(uddiRegistry);

            long oid = uddiServiceControlManager.save(uddiServiceControl);
            //Create the monitor runtime record for this service control as it has just been created
            final UDDIServiceControlMonitorRuntime monitorRuntime = new UDDIServiceControlMonitorRuntime(oid, lastModifiedServiceTimeStamp);
            uddiServiceControlMonitorRuntimeManager.save(monitorRuntime);
            if(uddiServiceControl.isUnderUddiControl()){
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_COMMITTED) {
                            logger.log(Level.FINE, "WSDL is now under UDDI control. Creating task to refresh gateway's WSDL");
                            uddiCoordinator.notifyEvent(new BusinessServiceUpdateUDDIEvent(uddiRegistry.getOid(), uddiServiceControl.getUddiServiceKey(), false, true));
                        }
                    }
                });
            }
            return oid;
        }else{
            UDDIServiceControl original = uddiServiceControlManager.findByPrimaryKey(uddiServiceControl.getOid());
            if(original == null) throw new FindException("Cannot find UDDIServiceControl with oid: " + uddiServiceControl.getOid());
            final String wsdlRefreshRequiredMessage = isWsdlRefreshRequired(original, uddiServiceControl);
            uddiServiceControl.throwIfFinalPropertyModified(original);
            //make sure the wsdl under uddi control is not being set when it is not allowed
            if(uddiServiceControl.isHasHadEndpointRemoved() && uddiServiceControl.isUnderUddiControl()){
                throw new UpdateException("Cannot save UDDIServiceControl. Incorrect state. " +
                        "WSDL cannot be under UDDI control when the owning business service in UDDI has had its bindingTemplates removed");
            }
            uddiServiceControlManager.update( uddiServiceControl );
            if(wsdlRefreshRequiredMessage != null){
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_COMMITTED) {
                            logger.log(Level.FINE, wsdlRefreshRequiredMessage);
                            uddiCoordinator.notifyEvent(new BusinessServiceUpdateUDDIEvent(uddiServiceControl.getUddiRegistryOid(), uddiServiceControl.getUddiServiceKey(), false, true));
                        }
                    }
                });
            }
            return uddiServiceControl.getOid();
        }
    }

    /**
     * Note: Mke sure this is called before the updated object is updated, as they will then both be synchronized and
     * will both be the same object.
     *
     * @param original Most recent value from the database
     * @param updated Incoming potentially updated value from a client
     * @return String not null if any change in the incoming entity requires a WSDL refresh. Value can be used for
     * fine logging
     */
    private String isWsdlRefreshRequired(final UDDIServiceControl original, final UDDIServiceControl updated){

        if(!original.isUnderUddiControl() && updated.isUnderUddiControl()){
            return "WSDL is now under UDDI control. Creating task to refresh gateway's WSDL";
        }

        if(!original.isMonitoringEnabled() && updated.isMonitoringEnabled()){
            return "UDDI is now being monitored. Creating task to refresh gateway's WSDL";
        }

        if(!original.isUpdateWsdlOnChange() && updated.isUpdateWsdlOnChange()){
            return "WSDL is now configured to be updated when UDDI changes. Creating task to refresh gateway's WSDL";
        }

        if(!original.isDisableServiceOnChange() && updated.isDisableServiceOnChange()){
            return "Published Service is now configured to be disabled when monitored WSDL changes. Creating task to refresh gateway's WSDL";
        }

        return null;
    }

    @Override
    public void deleteUDDIServiceControl(long uddiServiceControlOid) throws FindException, DeleteException, UpdateException {

        final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPrimaryKey(uddiServiceControlOid);
        if(serviceControl == null)
            throw new FindException("Cannot find UDDIServiceControl with id #(" + uddiServiceControlOid + ")");

        final UDDIProxiedServiceInfo serviceInfo = uddiProxiedServiceInfoManager.findByPublishedServiceOid(serviceControl.getPublishedServiceOid());
        if(serviceInfo != null && serviceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY){
            throw new DeleteException("Cannot delete record of how service was created until any published binding or overwritten service has been deleted");
        } else {
            uddiServiceControlManager.delete(uddiServiceControlOid);
        }
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
        if(publishedServiceOid < 1) throw new IllegalArgumentException("publishedServiceOid must be >= 1");

        final PublishedService service = serviceCache.getCachedService(publishedServiceOid);
        if(service == null) throw new SaveException("PublishedService with id #(" + publishedServiceOid + ") was not found");

        final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPublishedServiceOid(publishedServiceOid);
        if(serviceControl == null) throw new SaveException("PublishedService with id #("+publishedServiceOid+") was not created from UDDI (record may have been deleted)");

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(serviceControl.getUddiRegistryOid());
        if(uddiRegistry == null) throw new SaveException("UDDIRegistry with id #("+serviceControl.getUddiRegistryOid()+") was not found");
        throwIfGatewayNotEnabled(uddiRegistry);

        if(serviceControl.isUnderUddiControl() && removeOthers)
            throw new SaveException("Published service with id #("+publishedServiceOid+") is not under UDDI control so cannot remove existing bindingTemplates");

        final String wsdlHash = getWsdlHash(service);

        final UDDIProxiedServiceInfo serviceInfo = UDDIProxiedServiceInfo.getEndPointPublishInfo(service.getOid(),
                uddiRegistry.getOid(), serviceControl.getUddiBusinessKey(), serviceControl.getUddiBusinessName(),
                wsdlHash, uddiHelper.getExternalHostName(), removeOthers);

        final long oid = uddiProxiedServiceInfoManager.save(serviceInfo);
        final UDDIPublishStatus newStatus = new UDDIPublishStatus(oid, UDDIPublishStatus.PublishStatus.PUBLISH);
        uddiPublishStatusManager.save(newStatus);
        //the save event is picked up by the UDDICoordinator, which does the UDDI work
    }

    @Override
    public void overwriteBusinessServiceInUDDI(long publishedServiceOid, final boolean updateWhenGatewayWsdlChanges)
            throws SaveException, FindException {
        final PublishedService publishedService = serviceCache.getCachedService(publishedServiceOid);
        if(publishedService == null) throw new IllegalArgumentException("No PublishedService found for #(" +publishedServiceOid+")" );

        final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPublishedServiceOid(publishedService.getOid());
        if(serviceControl == null)
            throw new SaveException("Cannot overwrite service as there is no record of the BusinessService in UDDI from which the Published Service was created");

        final String wsdlHash = getWsdlHash(publishedService);

        final UDDIProxiedServiceInfo uddiProxiedServiceInfo =
                UDDIProxiedServiceInfo.getOverwriteProxyServicePublishInfo(publishedService.getOid(),
                        serviceControl.getUddiRegistryOid(), serviceControl.getUddiBusinessKey(),
                        serviceControl.getUddiBusinessName(), wsdlHash, uddiHelper.getExternalHostName(), updateWhenGatewayWsdlChanges);

        final long oid = uddiProxiedServiceInfoManager.save(uddiProxiedServiceInfo);
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
        throwIfGatewayNotEnabled(uddiRegistry);

        final PublishedService service = serviceCache.getCachedService(publishedServiceOid);
        if(service == null) throw new SaveException("PublishedService with id #(" + publishedServiceOid + ") was not found");

        final String wsdlHash = getWsdlHash(service);

        final UDDIProxiedServiceInfo uddiProxiedServiceInfo = UDDIProxiedServiceInfo.getProxyServicePublishInfo(service.getOid(),
                uddiRegistry.getOid(), uddiBusinessKey, uddiBusinessName,
                wsdlHash, uddiHelper.getExternalHostName(), updateWhenGatewayWsdlChanges);

        final long oid = uddiProxiedServiceInfoManager.save(uddiProxiedServiceInfo);
        final UDDIPublishStatus newStatus = new UDDIPublishStatus(oid, UDDIPublishStatus.PublishStatus.PUBLISH);
        uddiPublishStatusManager.save(newStatus);
        //the save event is picked up by the UDDICoordinator, which does the UDDI work
    }

    private String getWsdlHash(PublishedService service) {
        final String wsdlHash;
        try {
            wsdlHash = service.parsedWsdl().getHash();
        } catch (Exception e) {
            final String msg = "Cannot get generate hash of WSDL";
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            throw new RuntimeException(msg, ExceptionUtils.getDebugException(e));
        }
        return wsdlHash;
    }

    @Override
    public void updatePublishedGatewayWsdl(long uddiProxiedServiceInfoOid)
            throws FindException, UDDIRegistryNotEnabledException, PublishProxiedServiceException, UpdateException,
            VersionException {
        final UDDIProxiedServiceInfo uddiProxiedServiceInfo = uddiProxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
        if(uddiProxiedServiceInfo == null) throw new FindException("Cannot find UDDIProxiedServiceInfo with id: " + uddiProxiedServiceInfoOid);

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
        throwIfGatewayNotEnabled(uddiRegistry);

        final UDDIPublishStatus uddiPublishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
        if(uddiPublishStatus == null)
            throw new FindException("Cannot find UDDIPublishStatus for UDDIProxiedServiceInfo with id#(" + uddiProxiedServiceInfo.getOid() + ")");

        uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISH);
        //the update event is picked up by the UDDICoordinator, which does the UDDI work
        uddiPublishStatusManager.update(uddiPublishStatus);
    }

    private void throwIfGatewayNotEnabled(UDDIRegistry uddiRegistry) throws UDDIRegistryNotEnabledException {
        if(!uddiRegistry.isEnabled())
            throw new UDDIRegistryNotEnabledException("UDDIRegistry with id #(" + uddiRegistry.getOid() + ") is not enabled");
    }

    @Override
    public Collection<ServiceHeader> getServicesPublishedToUDDI(Collection<Long> allServiceIds) throws FindException {
        if(allServiceIds == null) throw new NullPointerException("allServiceIds cannot be null");

        final Collection<ServiceHeader> returnColl = new HashSet<ServiceHeader>();
        for(Long oid: allServiceIds) {
            if (uddiProxiedServiceInfoManager.findByPublishedServiceOid(oid) != null) {
                final PublishedService publishedService = serviceCache.getCachedService(oid);
                final ServiceHeader serviceHeader = new ServiceHeader(publishedService);
                returnColl.add(serviceHeader);
            } else {
                final Collection<UDDIBusinessServiceStatus> serviceStatuses = businessServiceStatusManager.findByPublishedService(oid);
                for(UDDIBusinessServiceStatus status: serviceStatuses){
                    if(status.getUddiPolicyStatus() != UDDIBusinessServiceStatus.Status.NONE ||
                            status.getUddiMetricsReferenceStatus() != UDDIBusinessServiceStatus.Status.NONE){
                        final PublishedService publishedService = serviceCache.getCachedService(oid);
                        final ServiceHeader serviceHeader = new ServiceHeader(publishedService);
                        returnColl.add(serviceHeader);
                    }
                }
            }
        }

        return Collections.unmodifiableCollection(returnColl);
    }
}
