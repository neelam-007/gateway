package com.l7tech.server.uddi;

import com.l7tech.server.service.ServiceCache;
import com.l7tech.uddi.*;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import javax.wsdl.WSDLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * No tasks are thread safe. All access to each task defined in this factory must be serialized. All tasks should
 * only ever run on the same timer task in UDDICoordinator
 */
public class PublishingUDDITaskFactory extends UDDITaskFactory {

    //- PUBLIC

    public PublishingUDDITaskFactory(final UDDIRegistryManager uddiRegistryManager,
                                     final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                                     final ServiceCache serviceCache,
                                     final UDDIHelper uddiHelper,
                                     final UDDIServiceControlManager uddiServiceControlManager,
                                     final UDDIPublishStatusManager uddiPublishStatusManager) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.serviceCache = serviceCache;
        this.uddiHelper = uddiHelper;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiPublishStatusManager = uddiPublishStatusManager;
    }

    @Override
    public UDDITask buildUDDITask( final UDDIEvent event ) {
        UDDITask task = null;

        if( event instanceof PublishUDDIEvent){
            PublishUDDIEvent publishUDDIEvent = (PublishUDDIEvent) event;
            final UDDIProxiedServiceInfo uddiProxiedServiceInfo = publishUDDIEvent.getUddiProxiedServiceInfo();
            final UDDIPublishStatus publishStatus = publishUDDIEvent.getUddiPublishStatus();
            switch ( uddiProxiedServiceInfo.getPublishType() ){
                case PROXY:
                    if(publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.NONE){
                        task = new PublishUDDITask(
                                publishUDDIEvent.getUddiProxiedServiceInfo().getOid(),
                                uddiProxiedServiceInfoManager,
                                serviceCache,
                                uddiHelper,
                                uddiRegistryManager,
                                uddiPublishStatusManager);
                    }else if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH ||
                            publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED){
                        final PublishedService service = serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceOid());
                        task = new UpdatePublishUDDITask(
                                service,
                                uddiProxiedServiceInfoManager,
                                uddiHelper,
                                uddiRegistryManager, uddiPublishStatusManager);
                    }else if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE ||
                            publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED){
                        task = new DeletePublishUDDITask(
                                publishUDDIEvent.getUddiProxiedServiceInfo().getOid(),
                                uddiProxiedServiceInfoManager,
                                uddiHelper,
                                uddiRegistryManager,
                                uddiPublishStatusManager);
                    }

                    break;
                case ENDPOINT:
                    if(publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH ||
                            publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED){
                        task = new PublishUDDIEndpointTask(
                                publishUDDIEvent.getUddiProxiedServiceInfo().getOid(), uddiProxiedServiceInfoManager,
                                uddiServiceControlManager, uddiRegistryManager, serviceCache, uddiHelper,
                                uddiPublishStatusManager);
                    } else if(publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE ||
                            publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED){
                        task = new DeleteUDDIEndpointTask(
                                publishUDDIEvent.getUddiProxiedServiceInfo().getOid(),
                                uddiProxiedServiceInfoManager,
                                uddiRegistryManager, uddiHelper, uddiPublishStatusManager);
                    }

                    break;
            }
        }

        return task;
    }

    //- PRIVATE

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager;
    private final UDDIPublishStatusManager uddiPublishStatusManager;
    private final ServiceCache serviceCache;
    private final UDDIHelper uddiHelper;
    private final UDDIServiceControlManager uddiServiceControlManager;

    private static final class PublishUDDIEndpointTask extends UDDITask {
        private static final Logger logger = Logger.getLogger(PublishUDDIEndpointTask.class.getName());

        private final long uddiProxiedServiceInfoOid;
        private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
        private final UDDIPublishStatusManager publishStatusManager;
        private final UDDIServiceControlManager uddiServiceControlManager;
        private final UDDIRegistryManager uddiRegistryManager;
        private final ServiceCache serviceCache;
        private final UDDIHelper uddiHelper;

        private PublishUDDIEndpointTask(final long uddiProxiedServiceInfoOid,
                                        final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                                        final UDDIServiceControlManager uddiServiceControlManager,
                                        final UDDIRegistryManager uddiRegistryManager,
                                        final ServiceCache serviceCache,
                                        final UDDIHelper uddiHelper,
                                        final UDDIPublishStatusManager publishStatusManager) {
            this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
            this.proxiedServiceInfoManager = proxiedServiceInfoManager;
            this.uddiServiceControlManager = uddiServiceControlManager;
            this.uddiRegistryManager = uddiRegistryManager;
            this.serviceCache = serviceCache;
            this.uddiHelper = uddiHelper;
            this.publishStatusManager = publishStatusManager;
        }

        /**
         * This task must deal with both the first publish to UDDI and any subsequent updates to the binding template
         * @param context The context for the task
         */
        @Override
        public void apply( final UDDITaskContext context ) throws UDDITaskException {
            try {
                //only work with the most up to date values from the database
                final UDDIProxiedServiceInfo uddiProxiedServiceInfo = proxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
                if(uddiProxiedServiceInfo == null) return;

                final UDDIPublishStatus uddiPublishStatus = publishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                if(uddiPublishStatus == null) return;

                //Only work with published information in the publish state
                if(uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH
                        && uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH_FAILED
                        && uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.NONE){
                    logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the publish state. Nothing to do");
                    return;
                }

                if(uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.ENDPOINT){
                    throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type ENDPOINT");
                }

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                if(uddiRegistry==null) throw new IllegalStateException("UDDI Registry #("+ uddiProxiedServiceInfo.getUddiRegistryOid()+") not found.");
                if(!uddiRegistry.isEnabled()){
                    logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #("+ uddiProxiedServiceInfo.getUddiRegistryOid()+") is disabled");
                    return;
                }

                final PublishedService publishedService = serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceOid());

                final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(publishedService.getOid());
                //protected service gateway external wsdl url
                final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(publishedService.getOid());

                final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPublishedServiceOid(uddiProxiedServiceInfo.getPublishedServiceOid());
                if(serviceControl == null)
                    throw new IllegalStateException("No UDDIServiceControl found for PublishedService with id #(" + uddiProxiedServiceInfo.getPublishedServiceOid()+")");

                if(uddiProxiedServiceInfo.isRemoveOtherBindings() && serviceControl.isUnderUddiControl()){
                    throw new IllegalStateException("Cannot remove other bindings when the WSDL is under UDDI control");
                }

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service "+publishedService.getName()+"(#" + publishedService.getOid()+"). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));

                    uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                    publishStatusManager.update(uddiPublishStatus);
                    return;
                }

                final UDDIClient uddiClient = uddiHelper.newUDDIClient(uddiRegistry);
                BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher(wsdl, publishedService.getOid(), uddiHelper.newUDDIClientConfig( uddiRegistry ));
                //provides best effort commit / rollback for all UDDI interactions
                final String bindingKey;
                try {
                    //delete the existing key from UDDI
                    final String bindingKeyToCheck = uddiProxiedServiceInfo.getProxyBindingKey();
                    final boolean bindingAlreadyPublished = bindingKeyToCheck != null && !bindingKeyToCheck.trim().isEmpty();
                    if(bindingAlreadyPublished){
                        try {
                                uddiClient.deleteBindingTemplate(bindingKeyToCheck);
                        } catch (UDDIException e) {
                            PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus, context, publishStatusManager);
                            context.logAndAudit( SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_BINDING, e, ExceptionUtils.getMessage(e), uddiProxiedServiceInfo.getProxyBindingKey());
                            return;
                        }
                    }
                    bindingKey = businessServicePublisher.publishBindingTemplate(serviceControl.getUddiServiceKey(),
                        serviceControl.getWsdlPortName(), serviceControl.getWsdlPortBinding(), protectedServiceExternalURL, protectedServiceWsdlURL,
                            uddiProxiedServiceInfo.isRemoveOtherBindings());
                } catch (UDDIException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus, context, publishStatusManager);
                    context.logAndAudit( SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e, ExceptionUtils.getMessage(e));
                    return;
                }

                uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                publishStatusManager.update(uddiPublishStatus);
                uddiProxiedServiceInfo.setProxyBindingKey(bindingKey);
                proxiedServiceInfoManager.update(uddiProxiedServiceInfo);
                if(uddiProxiedServiceInfo.isRemoveOtherBindings()){
                    if(!serviceControl.isHasHadEndpointRemoved()){
                        if(serviceControl.isUnderUddiControl()){
                            serviceControl.setUnderUddiControl(false);//if this is actually done a coding error happened
                            logger.log(Level.WARNING, "Set UDDIServiceControl isUnderUDDIControl to be false");
                        }
                        serviceControl.setHasHadEndpointRemoved(true);
                        uddiServiceControlManager.update(serviceControl);
                    }
                }

            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e, "Database error when publishing proxy endpoint.");
                throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
            }
        }
    }
    
    private static final class PublishUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger(PublishUDDITask.class.getName());

        private final long uddiProxiedServiceInfoOid;
        private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
        private final UDDIPublishStatusManager publishStatusManager;
        private final ServiceCache serviceCache;
        private final UDDIHelper uddiHelper;
        private final UDDIRegistryManager uddiRegistryManager;

        public PublishUDDITask(final long uddiProxiedServiceInfoOid,
                               final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                               final ServiceCache serviceCache,
                               final UDDIHelper uddiHelper,
                               final UDDIRegistryManager uddiRegistryManager,
                               final UDDIPublishStatusManager publishStatusManager) {
            this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
            this.proxiedServiceInfoManager = proxiedServiceInfoManager;
            this.serviceCache = serviceCache;
            this.uddiHelper = uddiHelper;
            this.uddiRegistryManager = uddiRegistryManager;
            this.publishStatusManager = publishStatusManager;
        }

        @Override
        public void apply( final UDDITaskContext context ) throws UDDITaskException {
            try {
                //only work with the most up to date values from the database
                final UDDIProxiedServiceInfo uddiProxiedServiceInfo = proxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
                if(uddiProxiedServiceInfo == null) return;

                final UDDIPublishStatus uddiPublishStatus = publishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfoOid);
                if(uddiPublishStatus == null) return;


                //Only work with published information in the publish state
                if(uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.NONE){
                    logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the waiting to publish state. Nothing to do");
                }

                if(uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY){
                    throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type PROXY");
                }

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                if(uddiRegistry==null) throw new IllegalStateException("UDDI Registry #("+uddiProxiedServiceInfo.getUddiRegistryOid()+") not found.");
                if(!uddiRegistry.isEnabled()){
                    logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #("+uddiProxiedServiceInfo.getUddiRegistryOid()+") is disabled");
                    return;
                }

                final PublishedService publishedService = serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceOid());

                final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(publishedService.getOid());
                //protected service gateway external wsdl url
                final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(publishedService.getOid());


                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service "+publishedService.getName()+"(#" + publishedService.getOid()+"). Any previously published information will be deleted from UDDI", ExceptionUtils.getDebugException(e));
                    uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                    publishStatusManager.update(uddiPublishStatus);
                    return;
                }

                BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher(wsdl, publishedService.getOid(), uddiHelper.newUDDIClientConfig( uddiRegistry ));
                //this is best effort commit / rollback
                final List<UDDIBusinessService> uddiBusinessServices;
                try {
                    uddiBusinessServices = businessServicePublisher.publishServicesToUDDIRegistry(
                        protectedServiceExternalURL, protectedServiceWsdlURL, uddiProxiedServiceInfo.getUddiBusinessKey());
                } catch (UDDIException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus, context, publishStatusManager);
                    context.logAndAudit( SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, ExceptionUtils.getMessage(e));
                    return;
                }
                
                //Create all required UDDIProxiedService
                for(UDDIBusinessService bs: uddiBusinessServices){
                    final UDDIProxiedService proxiedService = new UDDIProxiedService(bs.getServiceKey(),
                            bs.getServiceName(), bs.getWsdlServiceName());
                    //both parent and child records must be set before save
                    uddiProxiedServiceInfo.getProxiedServices().add(proxiedService);
                    proxiedService.setUddiProxiedServiceInfo(uddiProxiedServiceInfo);
                }

                uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                publishStatusManager.update(uddiPublishStatus);
                proxiedServiceInfoManager.update(uddiProxiedServiceInfo);
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, "Database error when publishing proxy services.");
                throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
            } //management of the publish state requires that all UDDIExceptions be caught where they can be thrown
        }
    }

    private static void handleUddiPublishFailure(final UDDIPublishStatus uddiPublishStatus,
                                                 final UDDITaskContext taskContext,
                                                 final UDDIPublishStatusManager uddiPublishStatusManager)
            throws UpdateException {
        handleUddiUpdateFailure(uddiPublishStatus, taskContext, uddiPublishStatusManager,
                UDDIPublishStatus.PublishStatus.PUBLISH_FAILED, UDDIPublishStatus.PublishStatus.CANNOT_PUBLISH);
    }

    private static void handleUddiDeleteFailure(final UDDIPublishStatus uddiPublishStatus,
                                                final UDDITaskContext taskContext,
                                                final UDDIPublishStatusManager uddiPublishStatusManager) throws UpdateException {
        handleUddiUpdateFailure(uddiPublishStatus, taskContext, uddiPublishStatusManager,
                UDDIPublishStatus.PublishStatus.DELETE_FAILED, UDDIPublishStatus.PublishStatus.CANNOT_DELETE);
    }

    /**
     * If the UDDI publish has failed more than the allowable configured amount, it is set to the failedStatus,
     * othewise its status is set to the retry status
     *
     * @param statusManager UDDIPublishStatusManager 
     * @param taskContext
     * @param statusManager
     * @throws UpdateException
     */
    private static void handleUddiUpdateFailure(final UDDIPublishStatus uddiPublishStatus,
                                                final UDDITaskContext taskContext,
                                                final UDDIPublishStatusManager statusManager,
                                                final UDDIPublishStatus.PublishStatus retryOkStatus,
                                                final UDDIPublishStatus.PublishStatus failedStatus)
            throws UpdateException {
        final int maxFailures = taskContext.getMaxRetryAttempts();
        final int numPreviousRetries = uddiPublishStatus.getFailCount();
        if(numPreviousRetries >= maxFailures){
            uddiPublishStatus.setPublishStatus(failedStatus);
        }else{
            final int failCount = uddiPublishStatus.getFailCount() + 1;
            uddiPublishStatus.setFailCount(failCount);
            uddiPublishStatus.setPublishStatus(retryOkStatus);
        }
        statusManager.update(uddiPublishStatus);
    }

    private static final class UpdatePublishUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger(PublishUDDITask.class.getName());

        private final PublishedService publishedService;
        private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
        private final UDDIPublishStatusManager publishStatusManager;
        private final UDDIHelper uddiHelper;
        private final UDDIRegistryManager uddiRegistryManager;

        public UpdatePublishUDDITask(final PublishedService publishedService,
                                     final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                                     final UDDIHelper uddiHelper,
                                     final UDDIRegistryManager uddiRegistryManager, UDDIPublishStatusManager publishStatusManager) {
            this.publishedService = publishedService;
            this.proxiedServiceInfoManager = proxiedServiceInfoManager;
            this.uddiHelper = uddiHelper;
            this.uddiRegistryManager = uddiRegistryManager;
            this.publishStatusManager = publishStatusManager;
        }

            @Override
            public void apply(final UDDITaskContext context) throws UDDITaskException {
                try {
                    //Get the most up to date version of the UDDIProxiedServiceInfo - the queue can be stale - will cause loop with eventual hibernate errors
                    final UDDIProxiedServiceInfo uddiProxiedServiceInfo = proxiedServiceInfoManager.findByPublishedServiceOid(publishedService.getOid());
                    if(uddiProxiedServiceInfo == null) return;

                    if(!uddiProxiedServiceInfo.isUpdateProxyOnLocalChange()) return;

                    final UDDIPublishStatus uddiPublishStatus = publishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                    if(uddiPublishStatus == null) return;

                    //Only work with published information in the publish state
                    if (uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH
                            && uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH_FAILED) {
                        logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the publish state. Nothing to do");
                        return;
                    }

                    if (uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY) {
                        throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type PROXY");
                    }

                    //this must be found due to db constraints
                    final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                    if (uddiRegistry == null)
                        throw new IllegalStateException("UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") not found.");
                    if (!uddiRegistry.isEnabled()) {
                        logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                        return;
                    }

                    final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(publishedService.getOid());
                    //protected service gateway external wsdl url
                    final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(publishedService.getOid());


                    final Wsdl wsdl;
                    try {
                        wsdl = publishedService.parsedWsdl();
                    } catch (WSDLException e) {
                        logger.log(Level.WARNING, "Unable to parse WSDL for service " + publishedService.getName() + "(#" + publishedService.getOid() + "). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));
                        uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                        publishStatusManager.update(uddiPublishStatus);
                        return;
                    }

                    //Get existing services
                    Set<UDDIProxiedService> allProxiedServices = uddiProxiedServiceInfo.getProxiedServices();
                    Set<String> serviceKeys = new HashSet<String>();
                    for (UDDIProxiedService ps : allProxiedServices) {
                        serviceKeys.add(ps.getUddiServiceKey());
                    }

                    BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher(wsdl, uddiProxiedServiceInfo.getPublishedServiceOid(), uddiHelper.newUDDIClientConfig(uddiRegistry));

                    final Pair<Set<String>, Set<UDDIBusinessService>> deletedAndNewServices;
                    try {
                        deletedAndNewServices = businessServicePublisher.updateServicesToUDDIRegistry(
                                protectedServiceExternalURL, protectedServiceWsdlURL, uddiProxiedServiceInfo.getUddiBusinessKey(), serviceKeys);

                    } catch (UDDIException e) {
                        PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus, context, publishStatusManager);
                        context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, ExceptionUtils.getMessage(e));
                        return;
                    }

                    //now manage db entities
                    Set<String> deleteSet = deletedAndNewServices.left;
                    //update all child entities
                    Set<UDDIProxiedService> removeSet = new HashSet<UDDIProxiedService>();
                    for (String deleteServiceKey : deleteSet) {
                        for (UDDIProxiedService proxiedService : allProxiedServices) {
                            if (proxiedService.getUddiServiceKey().equals(deleteServiceKey)) {
                                removeSet.add(proxiedService);
                                logger.log(Level.INFO, "Deleting UDDIProxiedService for serviceKey: " + deleteServiceKey);
                            }
                        }
                    }

                    uddiProxiedServiceInfo.getProxiedServices().removeAll(removeSet);

                    //create required new UDDIProxiedServices
                    Set<UDDIBusinessService> newlyCreatedServices = deletedAndNewServices.right;
                    for (UDDIBusinessService bs : newlyCreatedServices) {
                        final UDDIProxiedService proxiedService =
                                new UDDIProxiedService(bs.getServiceKey(), bs.getServiceName(), bs.getWsdlServiceName());
                        proxiedService.setUddiProxiedServiceInfo(uddiProxiedServiceInfo);
                        uddiProxiedServiceInfo.getProxiedServices().add(proxiedService);
                    }

                    uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                    publishStatusManager.update(uddiPublishStatus);
                    proxiedServiceInfoManager.update(uddiProxiedServiceInfo);
                } catch (ObjectModelException e) {
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, "Database error when publishing proxy services.");
                    throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
                }//management of the publish state requires that all UDDIExceptions be caught where they can be thrown
            }
        }

        private static final class DeleteUDDIEndpointTask extends UDDITask {
            private static final Logger logger = Logger.getLogger(DeleteUDDIEndpointTask.class.getName());

            private final long uddiProxiedServiceInfoOid;
            private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
            private final UDDIPublishStatusManager publishStatusManager;
            private final UDDIRegistryManager uddiRegistryManager;
            private final UDDIHelper uddiHelper;

            public DeleteUDDIEndpointTask(final long uddiProxiedServiceInfoOid,
                                          final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                                          final UDDIRegistryManager uddiRegistryManager,
                                          final UDDIHelper uddiHelper,
                                          final UDDIPublishStatusManager publishStatusManager) {
                this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
                this.proxiedServiceInfoManager = proxiedServiceInfoManager;
                this.uddiRegistryManager = uddiRegistryManager;
                this.uddiHelper = uddiHelper;
                this.publishStatusManager = publishStatusManager;
            }

            @Override
            public void apply(final UDDITaskContext context) throws UDDITaskException {
                try {
                    //only work with the most up to date values from the database
                    final UDDIProxiedServiceInfo uddiProxiedServiceInfo = proxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
                    if(uddiProxiedServiceInfo == null) return;

                    final UDDIPublishStatus uddiPublishStatus = publishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                    if(uddiPublishStatus == null) return;

                    if (uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE &&
                            uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                        logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the delete state. Nothing to do");
                        return;
                    }
                    if (uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.ENDPOINT) {
                        throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type ENDPOINT");
                    }

                    //publishing an endpoint requires that the service is under UDDI Control. However not enforcing here
                    //as if we have a published endpoint and we have all the information to delete it, then we should

                    //this must be found due to db constraints
                    final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                    if (uddiRegistry == null)
                        throw new IllegalStateException("UDDI Registry #" + uddiProxiedServiceInfo.getUddiRegistryOid() + "' not found.");
                    if (!uddiRegistry.isEnabled()) {
                        logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                        return;
                    }

                    //Only try and delete from UDDI if information was successfully published
                    if (uddiProxiedServiceInfo.getProxyBindingKey() != null
                            && !uddiProxiedServiceInfo.getProxyBindingKey().trim().isEmpty()) {
                        final UDDIClient uddiClient = uddiHelper.newUDDIClient(uddiRegistry);
                        try {
                            uddiClient.deleteBindingTemplate(uddiProxiedServiceInfo.getProxyBindingKey());
                            logger.log(Level.INFO, "Endpoint successfully deleted from UDDI");
                        } catch (UDDIException e) {
                            context.logAndAudit(SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_BINDING,
                                    ExceptionUtils.getDebugException(e),
                                    uddiProxiedServiceInfo.getProxyBindingKey());
                            PublishingUDDITaskFactory.handleUddiDeleteFailure(uddiPublishStatus, context, publishStatusManager);
                            return;
                        }
                    }

                    proxiedServiceInfoManager.delete(uddiProxiedServiceInfo);
                } catch (ObjectModelException e) {
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_FAILED, e, "Database error when deleting proxy endpoint.");
                    throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
                }
            }
        }

        private static final class DeletePublishUDDITask extends UDDITask {
            private static final Logger logger = Logger.getLogger(PublishUDDITask.class.getName());

            private final long uddiProxiedServiceInfoOid;
            private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
            private final UDDIHelper uddiHelper;
            private final UDDIRegistryManager uddiRegistryManager;
            private final UDDIPublishStatusManager publishStatusManager;

            public DeletePublishUDDITask(final long uddiProxiedServiceInfoOid,
                                         final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                                         final UDDIHelper uddiHelper,
                                         final UDDIRegistryManager uddiRegistryManager,
                                         final UDDIPublishStatusManager publishStatusManager) {
                this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
                this.proxiedServiceInfoManager = proxiedServiceInfoManager;
                this.uddiHelper = uddiHelper;
                this.uddiRegistryManager = uddiRegistryManager;
                this.publishStatusManager = publishStatusManager;
            }

            @Override
            public void apply(final UDDITaskContext context) throws UDDITaskException {
                try {
                    //only work with the most up to date values from the database
                    final UDDIProxiedServiceInfo uddiProxiedServiceInfo = proxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
                    if(uddiProxiedServiceInfo == null) return;

                    final UDDIPublishStatus uddiPublishStatus = publishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                    if(uddiPublishStatus == null) return;

                    //Only work with published information in the publish state
                    if (uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE
                            && uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                        logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the delete state. Nothing to do");
                        return;
                    }

                    if (uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY) {
                        throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type PROXY");
                    }

                    //this must be found due to db constraints
                    final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                    if (uddiRegistry == null)
                        throw new IllegalStateException("UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") not found.");
                    if (!uddiRegistry.isEnabled()) {
                        logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                        return;
                    }

                    final Set<UDDIProxiedService> proxiedServices = uddiProxiedServiceInfo.getProxiedServices();
                    final Set<String> keysToDelete = new HashSet<String>();
                    for (UDDIProxiedService proxiedService : proxiedServices) {
                        keysToDelete.add(proxiedService.getUddiServiceKey());
                    }
                    final UDDIClient uddiClient = uddiHelper.newUDDIClient(uddiRegistry);

                    try {
                        uddiClient.deleteBusinessServicesByKey(keysToDelete);
                        logger.log(Level.INFO, "Successfully deleted published Gateway WSDL from UDDI Registry");
                    } catch (UDDIException e) {
                        context.logAndAudit(SystemMessages.UDDI_REMOVE_SERVICE_FAILED, e, ExceptionUtils.getMessage(e));
                        PublishingUDDITaskFactory.handleUddiDeleteFailure(uddiPublishStatus, context, publishStatusManager);
                        return;
                    }

                    proxiedServiceInfoManager.delete(uddiProxiedServiceInfo.getOid());//cascade delete
                    logger.log(Level.INFO, "Proxied BusinessService successfully deleted from UDDI");

                } catch (ObjectModelException e) {
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, "Database error when deleting proxy services.");
                    throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
                } //management of the publish state requires that all UDDIExceptions be caught where they can be thrown
            }
        }

    }
