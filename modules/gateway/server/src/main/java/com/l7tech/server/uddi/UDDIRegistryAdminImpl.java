package com.l7tech.server.uddi;

import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.objectmodel.*;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.UDDIKeyedReference;
import com.l7tech.uddi.UDDIUtilities;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.wsdl.Wsdl;
import org.apache.commons.lang.ObjectUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.wsdl.WSDLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    final private UDDIServiceControlRuntimeManager uddiServiceControlRuntimeManager;
    final private UDDICoordinator uddiCoordinator;
    final private ServiceCache serviceCache;
    final private UDDIBusinessServiceStatusManager businessServiceStatusManager;
    final private ServiceManager serviceManager;

    private static final String UDDI_ORG_SPECIFICATION_V3_POLICY = "uddi:uddi.org:specification:v3_policy";

    public UDDIRegistryAdminImpl(final UDDIRegistryManager uddiRegistryManager,
                                 final UDDIHelper uddiHelper,
                                 final UDDIServiceControlManager uddiServiceControlManager,
                                 final UDDICoordinator uddiCoordinator,
                                 final ServiceCache serviceCache,
                                 final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                                 final UDDIPublishStatusManager uddiPublishStatusManager,
                                 final UDDIServiceControlRuntimeManager uddiServiceControlRuntimeManager,
                                 final UDDIBusinessServiceStatusManager businessServiceStatusManager,
                                 final ServiceManager serviceManager) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiHelper = uddiHelper;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiCoordinator = uddiCoordinator;
        this.serviceCache = serviceCache;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.uddiPublishStatusManager = uddiPublishStatusManager;
        this.uddiServiceControlRuntimeManager = uddiServiceControlRuntimeManager;
        this.businessServiceStatusManager = businessServiceStatusManager;
        this.serviceManager = serviceManager;
    }

    @Override
    public Goid saveUDDIRegistry(final UDDIRegistry uddiRegistry) throws SaveException, UpdateException, FindException {
        if(Goid.isDefault(uddiRegistry.getGoid())){
            if(uddiRegistry.getName().trim().isEmpty()){
                throw new SaveException("Cannot save a UDDI Registry with an emtpy name (or only containing spaces)");
            }
            uddiRegistryManager.save(uddiRegistry);
            logger.info("Saved UDDI Registry '" + uddiRegistry.getName()+"'");
        }else{
            if(uddiRegistry.getName().trim().isEmpty()){
                throw new UpdateException("Cannot update a UDDI Registry to have an emtpy name (or only containing spaces)");
            }
            final UDDIRegistry original = uddiRegistryManager.findByPrimaryKey(uddiRegistry.getGoid());
            if(original == null) throw new FindException("Cannot find UDDIRegistry with id#(" + uddiRegistry.getGoid()+")");

            final boolean monitoringReenabled = !original.isMonitoringEnabled() && uddiRegistry.isMonitoringEnabled();

            final boolean methodChanged = original.isSubscribeForNotifications() != uddiRegistry.isSubscribeForNotifications();

            final String updateCtx = (monitoringReenabled)
                    ? "Monitoring of UDDI Registry has been reenabled."
                    : "Method of monitoring has changed.";

            final boolean registryReenabled = !original.isEnabled() && uddiRegistry.isEnabled();

            if (methodChanged || monitoringReenabled || registryReenabled) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == TransactionSynchronization.STATUS_COMMITTED) {
                            if(registryReenabled){
                                logger.log(Level.FINE, "UDDI Registry has been reenabled. Updating each published service monitoring UDDI Registry with id#(" + uddiRegistry.getGoid() + ")");
                            }else {
                                logger.log(Level.FINE, updateCtx + " Updating each published service monitoring UDDI Registry with id#(" + uddiRegistry.getGoid() + ")");
                            }
                            uddiCoordinator.notifyEvent(new UpdateAllMonitoredServicesUDDIEvent(uddiRegistry.getGoid()));
                        }
                    }
                });
            }

            uddiRegistryManager.update(uddiRegistry);
            logger.info("Updated UDDI Registry '" + uddiRegistry.getName()+"' goid = " + uddiRegistry.getGoid());
        }

        return uddiRegistry.getGoid();
    }

    @Override
    public void deleteUDDIRegistry(final Goid goid) throws DeleteException, FindException, UDDIException, UDDIRegistryNotEnabledException {

        UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(goid);
        if(uddiRegistry == null) throw new FindException("Could not find UDDI Registry to delete");

        logger.log(Level.INFO, "Deleting UDDI Registry goid = " + goid);
        uddiRegistryManager.delete(goid);
    }

    @Override
    public Collection<UDDIRegistry> findAllUDDIRegistries() throws FindException {
        return uddiRegistryManager.findAll();
    }

    @Override
    public UDDIRegistry findByPrimaryKey(Goid registryGoid) throws FindException {
        return uddiRegistryManager.findByPrimaryKey(registryGoid);
    }

    @Override
    public boolean subscriptionNotificationsAvailable( final Goid registryGoid ) {
        return uddiCoordinator.isNotificationServiceAvailable( registryGoid );
    }

    @Override
    public boolean metricsAvailable( final Goid registryGoid ) throws FindException {
        boolean metrics = false;
        UDDIRegistry registry = findByPrimaryKey( registryGoid );
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
        } catch (FindException e) {
            throw new UDDIException(e);
        } finally {
            ResourceUtils.closeQuietly( uddiClient );
        }
    }

    private UDDIClient getUDDIClient(final UDDIRegistry uddiRegistry) throws FindException {
        return uddiHelper.newUDDIClient( uddiRegistry );
    }

    @Override
    public UDDIProxiedServiceInfo findProxiedServiceInfoForPublishedService(Goid serviceGoid) throws FindException {
        return uddiProxiedServiceInfoManager.findByPublishedServiceGoid(serviceGoid);
    }

    @Override
    public UDDIPublishStatus getPublishStatusForProxy(final Goid uddiProxiedServiceInfoGoid, Goid publishedServiceGoid) throws FindException {
        final UDDIPublishStatus publishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoGoid(uddiProxiedServiceInfoGoid);
        if(publishStatus == null)
            throw new FindException("Cannot find the UDDIPublishStatus for UDDIProxiedServiceInfo with id#(" + uddiProxiedServiceInfoGoid+")");

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

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(proxiedServiceInfo.getUddiRegistryGoid());
        throwIfUddiRegistryNotEnabled(uddiRegistry);

        final UDDIPublishStatus uddiPublishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoGoid(proxiedServiceInfo.getGoid());
        if(uddiPublishStatus == null)
            throw new FindException("Cannot find UDDIPublishStatus for UDDIProxiedServiceInfo with id#(" + proxiedServiceInfo.getGoid() + ")");

        final UDDIPublishStatus.PublishStatus status = uddiPublishStatus.getPublishStatus();
        if (status == UDDIPublishStatus.PublishStatus.CANNOT_DELETE ||
                status == UDDIPublishStatus.PublishStatus.CANNOT_PUBLISH ||
                status == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED ||
                status == UDDIPublishStatus.PublishStatus.DELETE_FAILED ) {

            //if we cannot delete or we have already tried, allow the user to stop any more attempts
            if(status == UDDIPublishStatus.PublishStatus.DELETE_FAILED || status == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED){
                final boolean isDeleting = status == UDDIPublishStatus.PublishStatus.DELETE_FAILED;
                logger.log(Level.WARNING, "Stopping attempt to " + ((isDeleting) ? "delete from" : "publish to") + " UDDI. Data may be orphaned in UDDI");
            }
            uddiProxiedServiceInfoManager.delete(proxiedServiceInfo);
        } else if (status == UDDIPublishStatus.PublishStatus.PUBLISHED) {
            uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
            //This triggers an entity invalidation event which the UDDICoordinator picks up
            uddiPublishStatusManager.update(uddiPublishStatus);
            logger.log(Level.INFO, "Set status to delete for published UDDI data");
        }else if(status == UDDIPublishStatus.PublishStatus.PUBLISH){
            logger.log(Level.WARNING, "Cannot delete Gateway WSDL from UDDI while it is being published");
        }else if(status == UDDIPublishStatus.PublishStatus.DELETE){
            logger.log(Level.WARNING, "UDDI data is currently set to delete. Please wait for delete to complete");
        }
    }

    @Override
    public void updateProxiedServiceOnly(final UDDIProxiedServiceInfo proxiedServiceInfo) throws UpdateException, FindException {
        final UDDIProxiedServiceInfo original = uddiProxiedServiceInfoManager.findByPrimaryKey(proxiedServiceInfo.getGoid());
        if(original == null)
            throw new FindException("Could not find UDDIProxiedServiceInfo #(" + proxiedServiceInfo.getGoid() + ")");

        //if any of the above fields have changed, then we will copy them onto the entity we just retrieved from the db

        boolean updateRequired = original.isUpdateProxyOnLocalChange() != proxiedServiceInfo.isUpdateProxyOnLocalChange() ||
           original.isMetricsEnabled() != proxiedServiceInfo.isMetricsEnabled() ||
           original.isPublishWsPolicyEnabled() != proxiedServiceInfo.isPublishWsPolicyEnabled() ||
           original.isPublishWsPolicyFull() != proxiedServiceInfo.isPublishWsPolicyFull() ||
           original.isPublishWsPolicyInlined() != proxiedServiceInfo.isPublishWsPolicyInlined() ||
           !ObjectUtils.equals(original.getSecurityZone(), proxiedServiceInfo.getSecurityZone());

        final boolean synchronizeWsdl = proxiedServiceInfo.isUpdateProxyOnLocalChange() &&
                original.isUpdateProxyOnLocalChange() != proxiedServiceInfo.isUpdateProxyOnLocalChange();

        final boolean refsAreDifferent =
                original.areKeyedReferencesDifferent(
                        proxiedServiceInfo.<Set<UDDIKeyedReference>>getProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_CONFIG));
        updateRequired = updateRequired || refsAreDifferent;
        final boolean isRepublishRequired = synchronizeWsdl || refsAreDifferent;

        if (updateRequired) {
            //this comparison must be done before the update to hibernate below, as otherwise hibernate synchronizes both objects
            if (isRepublishRequired) {
                //this causes a republish
                final UDDIPublishStatus publishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoGoid(original.getGoid());
                if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISHED ||
                        publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED) {
                    publishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISH);
                    uddiPublishStatusManager.update(publishStatus);
                    if(synchronizeWsdl){
                        logger.log(Level.INFO, "Set gateway WSDL to update in UDDI as it will now be kept synchronized with the Gateway");
                    }
                }
            }

            //update the entity retrieved from the db
            original.copyConfigModifiableProperties(proxiedServiceInfo);
            uddiProxiedServiceInfoManager.update(original);
        }
    }

    @Override
    public Collection<UDDIProxiedServiceInfo> getAllProxiedServicesForRegistry(Goid registryGoid) throws FindException {
        return uddiRegistryManager.findAllByRegistryGoid(registryGoid);
    }

    @Override
    public Goid saveUDDIServiceControlOnly(final UDDIServiceControl uddiServiceControl,
                                           final String serviceEndPoint,
                                           final Long lastModifiedServiceTimeStamp)
            throws UpdateException, SaveException, FindException, UDDIRegistryNotEnabledException {
        if ( Goid.isDefault(uddiServiceControl.getGoid()) ){
            if(lastModifiedServiceTimeStamp == null || lastModifiedServiceTimeStamp < 0){
                throw new SaveException("lastModifiedServiceTimeStamp is required when saving a UDDIServiceControl. Cannot be null or negative");    
            }

            if(serviceEndPoint == null || serviceEndPoint.trim().isEmpty()){
                throw new SaveException("serviceEndPoint is required when saving a UDDIServiceControl. Cannot be null, empty or contain only spaces");
            }
            
            final PublishedService service = serviceManager.findByPrimaryKey(uddiServiceControl.getPublishedServiceGoid());

            final Wsdl wsdl;
            try {
                wsdl = service.parsedWsdl();
            } catch (WSDLException e) {
                throw new SaveException("Cannot parse services WSDL: " + ExceptionUtils.getDebugException(e));
            }
            //Namespace support is not needed for wsdl:service based on how we use them.
            final boolean wsdlImplementUddiWsdlPort = UDDIUtilities.validatePortBelongsToWsdl(wsdl, uddiServiceControl.getWsdlServiceName(), null,
                    uddiServiceControl.getWsdlPortName(), uddiServiceControl.getWsdlPortBinding(), uddiServiceControl.getWsdlPortBindingNamespace());

            if(!wsdlImplementUddiWsdlPort){
                throw new SaveException("The published service's WSDL does not contain the selected wsdl:port / wsdl:binding chosen from the original BusinessService in UDDI." +
                        "\nHint: if the gateway WSDL is stale, it can be updated from the WSDL tab");
            }
            
            //make sure that we are not monitoring an endpoint of this SSG
            if(uddiHelper.isGatewayUrl(serviceEndPoint)){
                final String msg = "WSDL endpoint routes back to the Gateway";
                logger.log(Level.WARNING, msg);
                throw new SaveException(msg);
            }

            final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiServiceControl.getUddiRegistryGoid());
            if(uddiRegistry == null) throw new FindException("Cannot find UDDIRegistry");
            throwIfUddiRegistryNotEnabled(uddiRegistry);

            Goid goid = uddiServiceControlManager.save(uddiServiceControl);
            //Create the monitor runtime record for this service control as it has just been created
            final UDDIServiceControlRuntime monitorRuntime = new UDDIServiceControlRuntime(goid, lastModifiedServiceTimeStamp, serviceEndPoint);
            uddiServiceControlRuntimeManager.save(monitorRuntime);
            service.setDefaultRoutingUrl(serviceEndPoint);
            serviceManager.save(service);
            logger.log(Level.INFO, "Set context variable ${service.defaultRoutingURL} with UDDI endpoint value for service #(" + service.getGoid()+")");

            //Has the published service been published to UDDI?
            final UDDIProxiedServiceInfo info = uddiProxiedServiceInfoManager.findByPublishedServiceGoid(uddiServiceControl.getPublishedServiceGoid());
            if(info != null && info.getPublishType() == UDDIProxiedServiceInfo.PublishType.PROXY){
                //we have published the gateway wsdl. We need to update UDDI in case extra meta data is required
                //now that we have the original service (this is really for activesoa to turn a service virtual)
                final UDDIPublishStatus status = uddiPublishStatusManager.findByProxiedSerivceInfoGoid(info.getGoid());
                status.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISH);
                uddiPublishStatusManager.update(status);
            }

            if(uddiServiceControl.isUnderUddiControl()){
                registerSynchronization(uddiServiceControl.getUddiRegistryGoid(),
                        uddiServiceControl.getUddiServiceKey(),
                        "WSDL is now under UDDI control. Creating task to refresh gateway's WSDL");
            }
            return goid;
        }else{
            UDDIServiceControl original = uddiServiceControlManager.findByPrimaryKey(uddiServiceControl.getGoid());
            if(original == null) throw new FindException("Cannot find UDDIServiceControl with oid: " + uddiServiceControl.getGoid());

            //do not save if nothing has changed
            if(original.equals(uddiServiceControl)) {
                return uddiServiceControl.getGoid();
            }

            final String wsdlRefreshRequiredMessage = isWsdlRefreshRequired(original, uddiServiceControl);
            final boolean clearServiceDefaultURL = original.isUnderUddiControl() && !uddiServiceControl.isUnderUddiControl();

            //make sure the wsdl under uddi control is not being set when it is not allowed
            if(uddiServiceControl.isHasHadEndpointRemoved() && uddiServiceControl.isUnderUddiControl()){
                throw new UpdateException("Cannot save UDDIServiceControl. Incorrect state. " +
                        "WSDL cannot be under UDDI control when the owning business service in UDDI has had its bindingTemplates removed");
            }

            final UDDIProxiedServiceInfo serviceInfo = uddiProxiedServiceInfoManager.findByPublishedServiceGoid(original.getPublishedServiceGoid());
            //this exception is for admin api users only, UI stops this from happening
            if(serviceInfo != null && uddiServiceControl.isUnderUddiControl() && serviceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY){
                throw new UpdateException("Cannot save UDDIServiceControl with isUnderUDDIControl = true, when either a bindingTemplate has been added to the original or if it has been overwritten");
            }

            uddiServiceControlManager.update( uddiServiceControl );
            if(clearServiceDefaultURL){
                final PublishedService service = serviceManager.findByPrimaryKey(uddiServiceControl.getPublishedServiceGoid());
                service.setDefaultRoutingUrl(null);
                serviceManager.update(service);
                logger.log(Level.INFO, "Cleared context variable ${service.defaultRoutingURL} of UDDI endpoint value for service #(" + service.getGoid()+")");
            }

            if(wsdlRefreshRequiredMessage != null){
                registerSynchronization(uddiServiceControl.getUddiRegistryGoid(), uddiServiceControl.getUddiServiceKey(), wsdlRefreshRequiredMessage);
            }
            return uddiServiceControl.getGoid();
        }
    }

    private void registerSynchronization(final Goid registryGoid, final String serviceKey, final String logMessage){
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    logger.log(Level.INFO, logMessage);
                    uddiCoordinator.notifyEvent(new BusinessServiceUpdateUDDIEvent(registryGoid, serviceKey, false, true));
                }
            }
        });
    }

    /**
     * Note: Make sure this is called before the updated object is updated, as they will then both be synchronized and
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
    public void deleteUDDIServiceControl(Goid uddiServiceControlGoid) throws FindException, DeleteException, UpdateException {

        final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPrimaryKey(uddiServiceControlGoid);
        if(serviceControl == null)
            throw new FindException("Cannot find UDDIServiceControl with id #(" + uddiServiceControlGoid + ")");


        final PublishedService service = serviceManager.findByPrimaryKey(serviceControl.getGoid());
        if(service != null){
            service.setDefaultRoutingUrl(null);
            serviceManager.update(service);
            logger.log(Level.INFO, "Cleared context variable ${service.defaultRoutingURL} of UDDI endpoint value for service #(" + service.getGoid()+")");
        }

        final UDDIProxiedServiceInfo serviceInfo = uddiProxiedServiceInfoManager.findByPublishedServiceGoid(serviceControl.getPublishedServiceGoid());
        if(serviceInfo != null && serviceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY){
            throw new DeleteException("Cannot delete record of how service was created until any published binding or overwritten service has been deleted");
        } else {
            uddiServiceControlManager.delete(uddiServiceControlGoid);
        }
    }

    @Override
    public UDDIServiceControl getUDDIServiceControl( final Goid serviceGoid ) throws FindException {
        return uddiServiceControlManager.findByPublishedServiceGoid( serviceGoid );
    }

    @Override
    public String getOriginalServiceEndPoint(Goid serviceControlGoid) throws FindException {
        final UDDIServiceControlRuntime monitorRuntime = uddiServiceControlRuntimeManager.findByServiceControlGoid(serviceControlGoid);
        if(monitorRuntime == null) throw new FindException("Could not find the runtime information for UDDIServiceControl with id#(" + serviceControlGoid+")");
        return monitorRuntime.getAccessPointURL();
    }

    @Override
    public Collection<UDDIServiceControl> getAllServiceControlsForRegistry( final Goid registryGoid ) throws FindException {
        return uddiServiceControlManager.findByUDDIRegistryGoid( registryGoid );
    }

    @Override
    public void publishGatewayEndpoint(final PublishedService publishedService,
                                       final boolean removeOthers,
                                       final Map<String, Object> properties,
                                       final @Nullable SecurityZone securityZone)
            throws FindException, SaveException, UDDIRegistryNotEnabledException {
        if (publishedService == null) throw new NullPointerException("publishedServiceGoid must not be null");

        final PublishedService service = serviceCache.getCachedService(publishedService.getGoid());
        if (service == null)
            throw new SaveException("PublishedService with id #(" + publishedService + ") was not found");

        final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPublishedServiceGoid(service.getGoid());
        if (serviceControl == null)
            throw new SaveException("PublishedService with id #(" + publishedService + ") was not created from UDDI (record may have been deleted)");

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(serviceControl.getUddiRegistryGoid());
        if (uddiRegistry == null)
            throw new SaveException("UDDIRegistry with id #(" + serviceControl.getUddiRegistryGoid() + ") was not found");
        throwIfUddiRegistryNotEnabled(uddiRegistry);

        if (serviceControl.isUnderUddiControl() && removeOthers)
            throw new SaveException("Published service with id #(" + publishedService + ") is not under UDDI control so cannot remove existing bindingTemplates");

        final String wsdlHash = getWsdlHash(service);

        final UDDIProxiedServiceInfo serviceInfo = UDDIProxiedServiceInfo.getEndPointPublishInfo(service.getGoid(),
                uddiRegistry.getGoid(), serviceControl.getUddiBusinessKey(), serviceControl.getUddiBusinessName(),
                wsdlHash, removeOthers);
        serviceInfo.setSecurityZone(securityZone);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            serviceInfo.setProperty(entry.getKey(), entry.getValue());
        }
        
        final Goid goid = uddiProxiedServiceInfoManager.save(serviceInfo);
        final UDDIPublishStatus newStatus = new UDDIPublishStatus(goid, UDDIPublishStatus.PublishStatus.PUBLISH);
        uddiPublishStatusManager.save(newStatus);
        //the save event is picked up by the UDDICoordinator, which does the UDDI work
    }

    @Override
    public void publishGatewayEndpointGif(final PublishedService publishedService,
                                          final Map<String, Object> properties,
                                          final @Nullable SecurityZone securityZone) throws FindException, SaveException, UDDIRegistryNotEnabledException {
        if(publishedService == null) throw new NullPointerException("publishedService must not be null");

        if(properties == null) throw new NullPointerException("properties cannot be null");

        final EndpointScheme scheme = (EndpointScheme) properties.get(UDDIProxiedServiceInfo.GIF_SCHEME);
        if(scheme == null)
            throw new IllegalArgumentException("properties is missing the '" + UDDIProxiedServiceInfo.GIF_SCHEME + "' property");

        final PublishedService service = serviceCache.getCachedService(publishedService.getGoid());
        if(service == null) throw new SaveException("PublishedService with id #(" + publishedService + ") was not found");

        final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPublishedServiceGoid(service.getGoid());
        if(serviceControl == null) throw new SaveException("PublishedService with id #("+ publishedService +") was not created from UDDI (record may have been deleted)");

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(serviceControl.getUddiRegistryGoid());
        if(uddiRegistry == null) throw new SaveException("UDDIRegistry with id #("+serviceControl.getUddiRegistryGoid()+") was not found");
        throwIfUddiRegistryNotEnabled(uddiRegistry);

        final String wsdlHash = getWsdlHash(service);

        final UDDIProxiedServiceInfo serviceInfo = UDDIProxiedServiceInfo.getGifEndPointPublishInfo(service.getGoid(),
                uddiRegistry.getGoid(), serviceControl.getUddiBusinessKey(), serviceControl.getUddiBusinessName(),
                wsdlHash);
        serviceInfo.setSecurityZone(securityZone);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            serviceInfo.setProperty(entry.getKey(), entry.getValue());
        }
        final Goid goid = uddiProxiedServiceInfoManager.save(serviceInfo);
        final UDDIPublishStatus newStatus = new UDDIPublishStatus(goid, UDDIPublishStatus.PublishStatus.PUBLISH);
        uddiPublishStatusManager.save(newStatus);
        //the save event is picked up by the UDDICoordinator, which does the UDDI work.
    }

    @Override
    public void overwriteBusinessServiceInUDDI(PublishedService publishedServiceIn, final boolean updateWhenGatewayWsdlChanges, final @Nullable SecurityZone securityZone)
            throws SaveException, FindException {
        final PublishedService publishedService = serviceCache.getCachedService(publishedServiceIn.getGoid());
        if(publishedService == null) throw new IllegalArgumentException("No PublishedService found for #(" + publishedServiceIn +")" );

        final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPublishedServiceGoid(publishedService.getGoid());
        if(serviceControl == null)
            throw new SaveException("Cannot overwrite service as there is no record of the BusinessService in UDDI from which the Published Service was created");

        final String wsdlHash = getWsdlHash(publishedService);

        final UDDIProxiedServiceInfo uddiProxiedServiceInfo =
                UDDIProxiedServiceInfo.getOverwriteProxyServicePublishInfo(publishedService.getGoid(),
                        serviceControl.getUddiRegistryGoid(), serviceControl.getUddiBusinessKey(),
                        serviceControl.getUddiBusinessName(), wsdlHash, updateWhenGatewayWsdlChanges);
        uddiProxiedServiceInfo.setSecurityZone(securityZone);

        final Goid goid = uddiProxiedServiceInfoManager.save(uddiProxiedServiceInfo);
        final UDDIPublishStatus newStatus = new UDDIPublishStatus(goid, UDDIPublishStatus.PublishStatus.PUBLISH);
        uddiPublishStatusManager.save(newStatus);
        //the save event is picked up by the UDDICoordinator, which does the UDDI work

    }

    @Override
    public void publishGatewayWsdl(final PublishedService publishedService,
                                   final Goid uddiRegistryGoid,
                                   final String uddiBusinessKey,
                                   final String uddiBusinessName,
                                   final boolean updateWhenGatewayWsdlChanges,
                                   final @Nullable SecurityZone securityZone)
            throws FindException, SaveException, UDDIRegistryNotEnabledException {

        if(publishedService == null) throw new NullPointerException("publishedService cannot be null");
        if(Goid.isDefault(uddiRegistryGoid)) throw new IllegalArgumentException("uddiRegistryGoid must not be default");
        if(uddiBusinessKey == null || uddiBusinessKey.trim().isEmpty()) throw new IllegalArgumentException("uddiBusinessKey cannot be null or empty");
        if(uddiBusinessName == null || uddiBusinessName.trim().isEmpty()) throw new IllegalArgumentException("uddiBusinessName cannot be null or empty");

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiRegistryGoid);
        if(uddiRegistry == null) throw new IllegalArgumentException("Cannot find UDDIRegistry with oid: " + uddiRegistryGoid);
        throwIfUddiRegistryNotEnabled(uddiRegistry);

        final PublishedService service = serviceCache.getCachedService(publishedService.getGoid());
        if(service == null) throw new SaveException("PublishedService with id #(" + publishedService + ") was not found");

        final String wsdlHash = getWsdlHash(service);

        final UDDIProxiedServiceInfo uddiProxiedServiceInfo = UDDIProxiedServiceInfo.getProxyServicePublishInfo(service.getGoid(),
                uddiRegistry.getGoid(), uddiBusinessKey, uddiBusinessName,
                wsdlHash, updateWhenGatewayWsdlChanges);
        uddiProxiedServiceInfo.setSecurityZone(securityZone);

        final Goid goid = uddiProxiedServiceInfoManager.save(uddiProxiedServiceInfo);
        final UDDIPublishStatus newStatus = new UDDIPublishStatus(goid, UDDIPublishStatus.PublishStatus.PUBLISH);
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
    public void updatePublishedGatewayWsdl(Goid uddiProxiedServiceInfoGoid)
            throws FindException, UDDIRegistryNotEnabledException, PublishProxiedServiceException, UpdateException,
            VersionException {
        final UDDIProxiedServiceInfo uddiProxiedServiceInfo = uddiProxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoGoid);
        if(uddiProxiedServiceInfo == null) throw new FindException("Cannot find UDDIProxiedServiceInfo with id: " + uddiProxiedServiceInfoGoid);

        final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryGoid());
        throwIfUddiRegistryNotEnabled(uddiRegistry);

        final UDDIPublishStatus uddiPublishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoGoid(uddiProxiedServiceInfo.getGoid());
        if(uddiPublishStatus == null)
            throw new FindException("Cannot find UDDIPublishStatus for UDDIProxiedServiceInfo with id#(" + uddiProxiedServiceInfo.getGoid() + ")");

        uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISH);
        //the update event is picked up by the UDDICoordinator, which does the UDDI work
        uddiPublishStatusManager.update(uddiPublishStatus);
    }

    private void throwIfUddiRegistryNotEnabled(UDDIRegistry uddiRegistry) throws UDDIRegistryNotEnabledException {
        if(!uddiRegistry.isEnabled())
            throw new UDDIRegistryNotEnabledException("UDDIRegistry with id #(" + uddiRegistry.getGoid() + ") is not enabled");
    }

    @Override
    public Collection<ServiceHeader> getServicesPublishedToUDDI(Collection<Goid> allServiceIds) throws FindException {
        if(allServiceIds == null) throw new NullPointerException("allServiceIds cannot be null");

        final Collection<ServiceHeader> returnColl = new HashSet<ServiceHeader>();
        for(Goid goid: allServiceIds) {
            if (uddiProxiedServiceInfoManager.findByPublishedServiceGoid(goid) != null) {
                final PublishedService publishedService = serviceCache.getCachedService(goid);
                final ServiceHeader serviceHeader = new ServiceHeader(publishedService);
                returnColl.add(serviceHeader);
            } else {
                final Collection<UDDIBusinessServiceStatus> serviceStatuses = businessServiceStatusManager.findByPublishedService(goid);
                for(UDDIBusinessServiceStatus status: serviceStatuses){
                    if(status.getUddiPolicyStatus() != UDDIBusinessServiceStatus.Status.NONE ||
                            status.getUddiMetricsReferenceStatus() != UDDIBusinessServiceStatus.Status.NONE){
                        final PublishedService publishedService = serviceCache.getCachedService(goid);
                        final ServiceHeader serviceHeader = new ServiceHeader(publishedService);
                        returnColl.add(serviceHeader);
                    }
                }
            }
        }

        return Collections.unmodifiableCollection(returnColl);
    }
}
