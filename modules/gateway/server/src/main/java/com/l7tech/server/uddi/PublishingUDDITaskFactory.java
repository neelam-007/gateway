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
                                     final UDDIServiceControlManager uddiServiceControlManager) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.serviceCache = serviceCache;
        this.uddiHelper = uddiHelper;
        this.uddiServiceControlManager = uddiServiceControlManager;
    }

    @Override
    public UDDITask buildUDDITask( final UDDIEvent event ) {
        UDDITask task = null;

        if( event instanceof PublishUDDIEvent){
            PublishUDDIEvent publishUDDIEvent = (PublishUDDIEvent) event;
            final UDDIProxiedServiceInfo uddiProxiedServiceInfo = publishUDDIEvent.getUddiProxiedServiceInfo();
            final UDDIPublishStatus.PublishStatus publishStatus = uddiProxiedServiceInfo.getUddiPublishStatus().getPublishStatus();
            switch ( publishUDDIEvent.getPublishType() ){
                case PROXY:
                    if(publishStatus == UDDIPublishStatus.PublishStatus.NONE){
                        task = new PublishUDDITask(
                                publishUDDIEvent.getUddiProxiedServiceInfo(),
                                uddiProxiedServiceInfoManager,
                                serviceCache,
                                uddiHelper,
                                uddiRegistryManager);
                    }else if (publishStatus == UDDIPublishStatus.PublishStatus.PUBLISH || publishStatus == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED){
                        final PublishedService service = serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceOid());
                        task = new UpdatePublishUDDITask(
                                service,
                                uddiProxiedServiceInfoManager,
                                uddiHelper,
                                uddiRegistryManager);
                    }else if (publishStatus == UDDIPublishStatus.PublishStatus.DELETE || publishStatus == UDDIPublishStatus.PublishStatus.DELETE_FAILED){
                        task = new DeletePublishUDDITask(
                                publishUDDIEvent.getUddiProxiedServiceInfo(),
                                uddiProxiedServiceInfoManager,
                                serviceCache,
                                uddiHelper,
                                uddiRegistryManager);
                    }

                    break;
                case ENDPOINT:
                    if(publishStatus == UDDIPublishStatus.PublishStatus.PUBLISH || publishStatus == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED){
                        task = new PublishUDDIEndpointTask(
                                publishUDDIEvent.getUddiProxiedServiceInfo(),
                                uddiProxiedServiceInfoManager,
                                uddiServiceControlManager, uddiRegistryManager, serviceCache, uddiHelper);
                    } else if(publishStatus == UDDIPublishStatus.PublishStatus.DELETE || publishStatus == UDDIPublishStatus.PublishStatus.DELETE_FAILED){
                        task = new DeleteUDDIEndpointTask(
                                publishUDDIEvent.getUddiProxiedServiceInfo(),
                                uddiProxiedServiceInfoManager,
                                uddiRegistryManager, uddiHelper);
                    }

                    break;
            }
        }

        return task;
    }

    //- PRIVATE

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager;
    private final ServiceCache serviceCache;
    private final UDDIHelper uddiHelper;
    private final UDDIServiceControlManager uddiServiceControlManager;

    private static final class PublishUDDIEndpointTask extends UDDITask {
        private static final Logger logger = Logger.getLogger(PublishUDDIEndpointTask.class.getName());

        private final UDDIProxiedServiceInfo uddiproxiedServiceInfo;
        private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
        private final UDDIServiceControlManager uddiServiceControlManager;
        private final UDDIRegistryManager uddiRegistryManager;
        private final ServiceCache serviceCache;
        private final UDDIHelper uddiHelper;

        private PublishUDDIEndpointTask(final UDDIProxiedServiceInfo uddiproxiedServiceInfo,
                                        final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                                        final UDDIServiceControlManager uddiServiceControlManager,
                                        final UDDIRegistryManager uddiRegistryManager,
                                        final ServiceCache serviceCache,
                                        final UDDIHelper uddiHelper) {
            this.uddiproxiedServiceInfo = uddiproxiedServiceInfo;
            this.proxiedServiceInfoManager = proxiedServiceInfoManager;
            this.uddiServiceControlManager = uddiServiceControlManager;
            this.uddiRegistryManager = uddiRegistryManager;
            this.serviceCache = serviceCache;
            this.uddiHelper = uddiHelper;
        }

        /**
         * This task must deal with both the first publish to UDDI and any subsequent updates to the binding template
         * @param context The context for the task
         */
        @Override
        public void apply( final UDDITaskContext context ) throws UDDITaskException {
            try {
                //Only work with published information in the publish state
                if(uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH
                        && uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH_FAILED
                        && uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.NONE){
                    logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the publish state. Nothing to do");
                    return;
                }

                if(uddiproxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.ENDPOINT){
                    throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type ENDPOINT");
                }

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiproxiedServiceInfo.getUddiRegistryOid());
                if(uddiRegistry==null) throw new IllegalStateException("UDDI Registry #("+uddiproxiedServiceInfo.getUddiRegistryOid()+") not found.");
                if(!uddiRegistry.isEnabled()){
                    logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #("+uddiproxiedServiceInfo.getUddiRegistryOid()+") is disabled");
                    return;
                }

                final PublishedService publishedService = serviceCache.getCachedService(uddiproxiedServiceInfo.getPublishedServiceOid());

                final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(publishedService.getOid());
                //protected service gateway external wsdl url
                final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(publishedService.getOid());

                final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPublishedServiceOid(uddiproxiedServiceInfo.getPublishedServiceOid());
                if(serviceControl == null)
                    throw new IllegalStateException("No UDDIServiceControl found for PublishedService with id #(" + uddiproxiedServiceInfo.getPublishedServiceOid()+")");

                if(!serviceControl.isUnderUddiControl()){
                    logger.log(Level.WARNING, "PublishedService with id #(" + uddiproxiedServiceInfo.getPublishedServiceOid()+") is not under UDDI control");
                    return;
                }

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service "+publishedService.getName()+"(#" + publishedService.getOid()+"). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));
                    uddiproxiedServiceInfo.getUddiPublishStatus().setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                    proxiedServiceInfoManager.update(uddiproxiedServiceInfo);
                    return;
                }

                final UDDIClient uddiClient = uddiHelper.newUDDIClient(uddiRegistry);
                BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher(wsdl, publishedService.getOid(), uddiHelper.newUDDIClientConfig( uddiRegistry ));
                //provides best effort commit / rollback for all UDDI interactions
                final String bindingKey;
                try {
                    if(uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.NONE){
                        //delete the existing key from UDDI
                        try {
                            uddiClient.deleteBindingTemplate(uddiproxiedServiceInfo.getProxyBindingKey());
                        } catch (UDDIException e) {
                            PublishingUDDITaskFactory.handleUddiPublishFailure(uddiproxiedServiceInfo, context, proxiedServiceInfoManager);
                            context.logAndAudit( SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_BINDING, e, ExceptionUtils.getMessage(e), uddiproxiedServiceInfo.getProxyBindingKey());
                            return;
                        }
                    }
                    bindingKey = businessServicePublisher.publishEndPointToExistingService(serviceControl.getUddiServiceKey(),
                        serviceControl.getWsdlPortName(), protectedServiceExternalURL, protectedServiceWsdlURL );
                } catch (UDDIException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiproxiedServiceInfo, context, proxiedServiceInfoManager);
                    context.logAndAudit( SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e, ExceptionUtils.getMessage(e));
                    return;
                }

                uddiproxiedServiceInfo.getUddiPublishStatus().setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                uddiproxiedServiceInfo.setProxyBindingKey(bindingKey);
                proxiedServiceInfoManager.update(uddiproxiedServiceInfo);
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e, "Database error when publishing proxy endpoint.");
                throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
            }
        }
    }
    
    private static final class PublishUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger(PublishUDDITask.class.getName());

        private final UDDIProxiedServiceInfo uddiproxiedServiceInfo;
        private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
        private final ServiceCache serviceCache;
        private final UDDIHelper uddiHelper;
        private final UDDIRegistryManager uddiRegistryManager;

        public PublishUDDITask(final UDDIProxiedServiceInfo uddiproxiedServiceInfo,
                               final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                               final ServiceCache serviceCache,
                               final UDDIHelper uddiHelper,
                               final UDDIRegistryManager uddiRegistryManager) {
            this.uddiproxiedServiceInfo = uddiproxiedServiceInfo;
            this.proxiedServiceInfoManager = proxiedServiceInfoManager;
            this.serviceCache = serviceCache;
            this.uddiHelper = uddiHelper;
            this.uddiRegistryManager = uddiRegistryManager;
        }

        @Override
        public void apply( final UDDITaskContext context ) throws UDDITaskException {
            try {
                //Only work with published information in the publish state
                if(uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.NONE){
                    logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the waiting to publish state. Nothing to do");
                }

                if(uddiproxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY){
                    throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type PROXY");
                }

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiproxiedServiceInfo.getUddiRegistryOid());
                if(uddiRegistry==null) throw new IllegalStateException("UDDI Registry #("+uddiproxiedServiceInfo.getUddiRegistryOid()+") not found.");
                if(!uddiRegistry.isEnabled()){
                    logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #("+uddiproxiedServiceInfo.getUddiRegistryOid()+") is disabled");
                    return;
                }

                final PublishedService publishedService = serviceCache.getCachedService(uddiproxiedServiceInfo.getPublishedServiceOid());

                final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(publishedService.getOid());
                //protected service gateway external wsdl url
                final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(publishedService.getOid());


                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service "+publishedService.getName()+"(#" + publishedService.getOid()+"). Any previously published information will be deleted from UDDI", ExceptionUtils.getDebugException(e));
                    uddiproxiedServiceInfo.getUddiPublishStatus().setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                    proxiedServiceInfoManager.update(uddiproxiedServiceInfo);
                    return;
                }

                BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher(wsdl, publishedService.getOid(), uddiHelper.newUDDIClientConfig( uddiRegistry ));
                //this is best effort commit / rollback
                final List<UDDIBusinessService> uddiBusinessServices;
                try {
                    uddiBusinessServices = businessServicePublisher.publishServicesToUDDIRegistry(
                        protectedServiceExternalURL, protectedServiceWsdlURL, uddiproxiedServiceInfo.getUddiBusinessKey());
                } catch (UDDIException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiproxiedServiceInfo, context, proxiedServiceInfoManager);
                    context.logAndAudit( SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, ExceptionUtils.getMessage(e));
                    return;
                }
                
                //Create all required UDDIProxiedService
                for(UDDIBusinessService bs: uddiBusinessServices){
                    final UDDIProxiedService proxiedService = new UDDIProxiedService(bs.getServiceKey(),
                            bs.getServiceName(), bs.getWsdlServiceName());
                    //both parent and child records must be set before save
                    uddiproxiedServiceInfo.getProxiedServices().add(proxiedService);
                    proxiedService.setUddiProxiedServiceInfo(uddiproxiedServiceInfo);
                }

                uddiproxiedServiceInfo.getUddiPublishStatus().setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                proxiedServiceInfoManager.update(uddiproxiedServiceInfo);
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, "Database error when publishing proxy services.");
                throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
            } //management of the publish state requires that all UDDIExceptions be caught where they can be thrown
        }
    }

    private static void handleUddiPublishFailure(final UDDIProxiedServiceInfo uddiProxiedServiceInfo,
                                                 final UDDITaskContext taskContext,
                                                 final UDDIProxiedServiceInfoManager proxiedServiceInfoManager)
            throws UpdateException {
        handleUddiUpdateFailure(uddiProxiedServiceInfo, taskContext, proxiedServiceInfoManager,
                UDDIPublishStatus.PublishStatus.PUBLISH_FAILED, UDDIPublishStatus.PublishStatus.CANNOT_PUBLISH);
    }

    private static void handleUddiDeleteFailure(final UDDIProxiedServiceInfo uddiProxiedServiceInfo,
                                                 final UDDITaskContext taskContext,
                                                 final UDDIProxiedServiceInfoManager proxiedServiceInfoManager) throws UpdateException {
        handleUddiUpdateFailure(uddiProxiedServiceInfo, taskContext, proxiedServiceInfoManager,
                UDDIPublishStatus.PublishStatus.DELETE_FAILED, UDDIPublishStatus.PublishStatus.CANNOT_DELETE);
    }

    /**
     * If the UDDI publish has failed more than the allowable configured amount, it is set to the failedStatus,
     * othewise its status is set to the retry status
     *
     * @param uddiProxiedServiceInfo
     * @param taskContext
     * @param proxiedServiceInfoManager
     * @throws UpdateException
     */
    private static void handleUddiUpdateFailure(final UDDIProxiedServiceInfo uddiProxiedServiceInfo,
                                                final UDDITaskContext taskContext,
                                                final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                                                final UDDIPublishStatus.PublishStatus retryOkStatus,
                                                final UDDIPublishStatus.PublishStatus failedStatus)
            throws UpdateException {
        final int maxFailures = taskContext.getMaxRetryAttempts();
        final int numPreviousRetries = uddiProxiedServiceInfo.getUddiPublishStatus().getFailCount();
        if(numPreviousRetries >= maxFailures){
            uddiProxiedServiceInfo.getUddiPublishStatus().setPublishStatus(failedStatus);
        }else{
            final int failCount = uddiProxiedServiceInfo.getUddiPublishStatus().getFailCount() + 1;
            uddiProxiedServiceInfo.getUddiPublishStatus().setFailCount(failCount);
            uddiProxiedServiceInfo.getUddiPublishStatus().setPublishStatus(retryOkStatus);
        }
        proxiedServiceInfoManager.update(uddiProxiedServiceInfo);
    }

    private static final class UpdatePublishUDDITask extends UDDITask {
            private static final Logger logger = Logger.getLogger(PublishUDDITask.class.getName());

            private final PublishedService publishedService;
            private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
            private final UDDIHelper uddiHelper;
            private final UDDIRegistryManager uddiRegistryManager;

        public UpdatePublishUDDITask(final PublishedService publishedService,
                                         final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                                         final UDDIHelper uddiHelper,
                                         final UDDIRegistryManager uddiRegistryManager) {
                this.publishedService = publishedService;
                this.proxiedServiceInfoManager = proxiedServiceInfoManager;
                this.uddiHelper = uddiHelper;
                this.uddiRegistryManager = uddiRegistryManager;
            }

            @Override
            public void apply(final UDDITaskContext context) throws UDDITaskException {
                try {
                    //Get the most up to date version of the UDDIProxiedServiceInfo - the queue can be stale - will cause loop with eventual hibernate errors
                    final UDDIProxiedServiceInfo uddiInfoToWorkWith = proxiedServiceInfoManager.findByPublishedServiceOid(publishedService.getOid());
                    //Only work with published information in the publish state
                    if (uddiInfoToWorkWith.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH
                            && uddiInfoToWorkWith.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH_FAILED) {
                        logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the publish state. Nothing to do");
                        return;
                    }

                    if (uddiInfoToWorkWith.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY) {
                        throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type PROXY");
                    }

                    //this must be found due to db constraints
                    final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiInfoToWorkWith.getUddiRegistryOid());
                    if (uddiRegistry == null)
                        throw new IllegalStateException("UDDI Registry #(" + uddiInfoToWorkWith.getUddiRegistryOid() + ") not found.");
                    if (!uddiRegistry.isEnabled()) {
                        logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #(" + uddiInfoToWorkWith.getUddiRegistryOid() + ") is disabled");
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
                        uddiInfoToWorkWith.getUddiPublishStatus().setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                        proxiedServiceInfoManager.update(uddiInfoToWorkWith);
                        return;
                    }

                    //Get existing services
                    Set<UDDIProxiedService> allProxiedServices = uddiInfoToWorkWith.getProxiedServices();
                    Set<String> serviceKeys = new HashSet<String>();
                    for (UDDIProxiedService ps : allProxiedServices) {
                        serviceKeys.add(ps.getUddiServiceKey());
                    }

                    BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher(wsdl, uddiInfoToWorkWith.getPublishedServiceOid(), uddiHelper.newUDDIClientConfig(uddiRegistry));

                    final Pair<Set<String>, Set<UDDIBusinessService>> deletedAndNewServices;
                    try {
                        deletedAndNewServices = businessServicePublisher.updateServicesToUDDIRegistry(
                                protectedServiceExternalURL, protectedServiceWsdlURL, uddiInfoToWorkWith.getUddiBusinessKey(), serviceKeys);

                    } catch (UDDIException e) {
                        PublishingUDDITaskFactory.handleUddiPublishFailure(uddiInfoToWorkWith, context, proxiedServiceInfoManager);
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

                    uddiInfoToWorkWith.getProxiedServices().removeAll(removeSet);

                    //create required new UDDIProxiedServices
                    Set<UDDIBusinessService> newlyCreatedServices = deletedAndNewServices.right;
                    for (UDDIBusinessService bs : newlyCreatedServices) {
                        final UDDIProxiedService proxiedService =
                                new UDDIProxiedService(bs.getServiceKey(), bs.getServiceName(), bs.getWsdlServiceName());
                        proxiedService.setUddiProxiedServiceInfo(uddiInfoToWorkWith);
                        uddiInfoToWorkWith.getProxiedServices().add(proxiedService);
                    }

                    uddiInfoToWorkWith.getUddiPublishStatus().setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                    proxiedServiceInfoManager.update(uddiInfoToWorkWith);
                } catch (ObjectModelException e) {
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, "Database error when publishing proxy services.");
                    throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
                }//management of the publish state requires that all UDDIExceptions be caught where they can be thrown
            }
        }

        private static final class DeleteUDDIEndpointTask extends UDDITask {
            private static final Logger logger = Logger.getLogger(DeleteUDDIEndpointTask.class.getName());

            private final UDDIProxiedServiceInfo uddiproxiedServiceInfo;
            private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
            private final UDDIRegistryManager uddiRegistryManager;
            private final UDDIHelper uddiHelper;

            public DeleteUDDIEndpointTask(final UDDIProxiedServiceInfo uddiproxiedServiceInfo,
                                          final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                                          final UDDIRegistryManager uddiRegistryManager,
                                          final UDDIHelper uddiHelper) {
                this.uddiproxiedServiceInfo = uddiproxiedServiceInfo;
                this.proxiedServiceInfoManager = proxiedServiceInfoManager;
                this.uddiRegistryManager = uddiRegistryManager;
                this.uddiHelper = uddiHelper;
            }

            @Override
            public void apply(final UDDITaskContext context) throws UDDITaskException {
                try {
                    if (uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE &&
                            uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                        logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the delete state. Nothing to do");
                        return;
                    }
                    if (uddiproxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.ENDPOINT) {
                        throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type ENDPOINT");
                    }

                    //publishing an endpoint requires that the service is under UDDI Control. However not enforcing here
                    //as if we have a published endpoint and we have all the information to delete it, then we should

                    //this must be found due to db constraints
                    final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiproxiedServiceInfo.getUddiRegistryOid());
                    if (uddiRegistry == null)
                        throw new IllegalStateException("UDDI Registry #" + uddiproxiedServiceInfo.getUddiRegistryOid() + "' not found.");
                    if (!uddiRegistry.isEnabled()) {
                        logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #(" + uddiproxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                        return;
                    }

                    //Only try and delete from UDDI if information was successfully published
                    if (uddiproxiedServiceInfo.getProxyBindingKey() != null
                            && !uddiproxiedServiceInfo.getProxyBindingKey().trim().isEmpty()) {
                        final UDDIClient uddiClient = uddiHelper.newUDDIClient(uddiRegistry);
                        try {
                            uddiClient.deleteBindingTemplate(uddiproxiedServiceInfo.getProxyBindingKey());
                            logger.log(Level.INFO, "Endpoint successfully deleted from UDDI");
                        } catch (UDDIException e) {
                            context.logAndAudit(SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_BINDING,
                                    ExceptionUtils.getDebugException(e),
                                    uddiproxiedServiceInfo.getProxyBindingKey());
                            PublishingUDDITaskFactory.handleUddiDeleteFailure(uddiproxiedServiceInfo, context, proxiedServiceInfoManager);
                            return;
                        }
                    }

                    proxiedServiceInfoManager.delete(uddiproxiedServiceInfo);
                } catch (ObjectModelException e) {
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_FAILED, e, "Database error when deleting proxy endpoint.");
                    throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
                }
            }
        }

        private static final class DeletePublishUDDITask extends UDDITask {
            private static final Logger logger = Logger.getLogger(PublishUDDITask.class.getName());

            private final UDDIProxiedServiceInfo uddiproxiedServiceInfo;
            private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
            private final ServiceCache serviceCache;
            private final UDDIHelper uddiHelper;
            private final UDDIRegistryManager uddiRegistryManager;

            public DeletePublishUDDITask(final UDDIProxiedServiceInfo uddiproxiedServiceInfo,
                                         final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                                         final ServiceCache serviceCache,
                                         final UDDIHelper uddiHelper,
                                         final UDDIRegistryManager uddiRegistryManager) {
                this.uddiproxiedServiceInfo = uddiproxiedServiceInfo;
                this.proxiedServiceInfoManager = proxiedServiceInfoManager;
                this.serviceCache = serviceCache;
                this.uddiHelper = uddiHelper;
                this.uddiRegistryManager = uddiRegistryManager;
            }

            @Override
            public void apply(final UDDITaskContext context) throws UDDITaskException {
                try {
                    //Only work with published information in the publish state
                    if (uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE
                            && uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                        logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the delete state. Nothing to do");
                        return;
                    }

                    if (uddiproxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY) {
                        throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type PROXY");
                    }

                    //this must be found due to db constraints
                    final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiproxiedServiceInfo.getUddiRegistryOid());
                    if (uddiRegistry == null)
                        throw new IllegalStateException("UDDI Registry #(" + uddiproxiedServiceInfo.getUddiRegistryOid() + ") not found.");
                    if (!uddiRegistry.isEnabled()) {
                        logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #(" + uddiproxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                        return;
                    }

                    final Set<UDDIProxiedService> proxiedServices = uddiproxiedServiceInfo.getProxiedServices();
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
                        PublishingUDDITaskFactory.handleUddiDeleteFailure(uddiproxiedServiceInfo, context, proxiedServiceInfoManager);                        
                        return;
                    }

                    proxiedServiceInfoManager.delete(uddiproxiedServiceInfo.getOid());//cascade delete
                    logger.log(Level.INFO, "Proxied BusinessService successfully deleted from UDDI");

                } catch (ObjectModelException e) {
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, "Database error when deleting proxy services.");
                    throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
                } //management of the publish state requires that all UDDIExceptions be caught where they can be thrown
            }
        }

    }
