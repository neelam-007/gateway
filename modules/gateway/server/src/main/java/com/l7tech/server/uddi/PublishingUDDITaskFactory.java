package com.l7tech.server.uddi;

import com.l7tech.server.service.ServiceCache;
import com.l7tech.uddi.*;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.util.ExceptionUtils;

import javax.wsdl.WSDLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;

/**
 *
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
                    task = new PublishUDDITask(
                            publishUDDIEvent.getUddiProxiedServiceInfo(),
                            uddiProxiedServiceInfoManager,
                            serviceCache,
                            uddiHelper,
                            uddiRegistryManager);
                    break;
                case ENDPOINT:
                    if(publishStatus == UDDIPublishStatus.PublishStatus.PUBLISHING){
                        task = new PublishUDDIEndpointTask(
                                publishUDDIEvent.getUddiProxiedServiceInfo(),
                                uddiProxiedServiceInfoManager,
                                uddiServiceControlManager, uddiRegistryManager, serviceCache, uddiHelper);
                    } else if(publishStatus == UDDIPublishStatus.PublishStatus.DELETING){
                        task = new DeleteUDDIEndpointTask(
                                publishUDDIEvent.getUddiProxiedServiceInfo(),
                                uddiProxiedServiceInfoManager,
                                uddiRegistryManager, uddiHelper);
                    }
                    //it's ok when nothing is found. The status is PUBLISHED and the entity has been updated
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
        public void apply( final UDDITaskContext context ) throws UDDIException {
            try {
                if(uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETING){
                    throw new UDDIException("UDDIProxiedServiceInfo is in an incorrect state. Must be in deleting state");
                }
                if(uddiproxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.ENDPOINT){
                    throw new UDDIException("UDDIProxiedServiceInfo is the wrong type. Must be of type ENDPOINT");
                }

                //publishing an endpoint requires that the service is under UDDI Control. However not enforcing here
                //as if we have a published endpoint and we have all the information to delete it, then we should

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiproxiedServiceInfo.getUddiRegistryOid());
                if(uddiRegistry==null) throw new UDDIException("UDDI Registry #"+uddiproxiedServiceInfo.getUddiRegistryOid()+"' not found.");
                if(!uddiRegistry.isEnabled()) throw new UDDIException("UDDI Registry '"+uddiRegistry.getName()+"' is disabled.");

                final UDDIClient uddiClient = uddiHelper.newUDDIClient(uddiRegistry);
                try {
                    uddiClient.deleteBindingTemplate(uddiproxiedServiceInfo.getProxyBindingKey());
                } catch (UDDIException e) {
                    context.logAndAudit( SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_BINDING,
                            ExceptionUtils.getDebugException( e ),
                            uddiproxiedServiceInfo.getProxyBindingKey() );
                }

                try {
                    proxiedServiceInfoManager.delete(uddiproxiedServiceInfo);
                } catch (DeleteException e) {
                    //UDDI has been updated, delete can be retired. The fact that nothing will happen in UDDI will
                    //not stop the entity from being deleted
                    logger.log( Level.WARNING, "Could not delete UDDIProxiedServiceInfo", e);
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_FAILED, e, "Database error when deleting proxy endpoint.");
            } catch (UDDIException ue) {
                context.logAndAudit( SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_FAILED, ue, ExceptionUtils.getMessage(ue));
                throw ue;
            }
        }
    }

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

        @Override
        public void apply( final UDDITaskContext context ) throws UDDIException {
            try {
                if(uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISHING){
                    throw new UDDIException("UDDIProxiedServiceInfo is in an incorrect state. Must be in publishing state");
                }
                if(uddiproxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.ENDPOINT){
                    throw new UDDIException("UDDIProxiedServiceInfo is the wrong type. Must be of type ENDPOINT");
                }

                final PublishedService publishedService = serviceCache.getCachedService(uddiproxiedServiceInfo.getPublishedServiceOid());

                final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(publishedService.getOid());
                //protected service gateway external wsdl url
                final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(publishedService.getOid());

                final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPublishedServiceOid(uddiproxiedServiceInfo.getPublishedServiceOid());
                if(serviceControl == null)
                    throw new UDDIException("No UDDIServiceControl found for PublishedService with id #(" + uddiproxiedServiceInfo.getPublishedServiceOid()+")");

                if(!serviceControl.isUnderUddiControl())
                    throw new UDDIException("PublishedService with id #(" + uddiproxiedServiceInfo.getPublishedServiceOid()+") is not under UDDI control");

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiproxiedServiceInfo.getUddiRegistryOid());
                if(uddiRegistry==null) throw new UDDIException("UDDI Registry #"+uddiproxiedServiceInfo.getUddiRegistryOid()+"' not found.");
                if(!uddiRegistry.isEnabled()) throw new UDDIException("UDDI Registry '"+uddiRegistry.getName()+"' is disabled.");

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    throw new UDDIException("Unable to parse WSDL for service "+publishedService.getName()+"(#" + publishedService.getOid()+")", e);
                }

                final UDDIClient uddiClient = uddiHelper.newUDDIClient(uddiRegistry);
                BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher(wsdl, publishedService.getOid(), uddiHelper.newUDDIClientConfig( uddiRegistry ));
                //provides best effort commit / rollback for all UDDI interactions
                final String bindingKey = businessServicePublisher.publishEndPointToExistingService(serviceControl.getUddiServiceKey(),
                        serviceControl.getWsdlPortName(), protectedServiceExternalURL, protectedServiceWsdlURL );

                try {
                    uddiproxiedServiceInfo.getUddiPublishStatus().setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                    uddiproxiedServiceInfo.setProxyBindingKey(bindingKey);
                    proxiedServiceInfoManager.update(uddiproxiedServiceInfo);
                } catch (UpdateException e) {
                    logger.log( Level.WARNING, "Could not update UDDIProxiedServiceInfo", e);
                    try {
                        logger.log(Level.INFO, "Attempting to rollback UDDI updates");
                        uddiClient.deleteBindingTemplate(bindingKey);
                        logger.log(Level.INFO, "UDDI updates rolled back successfully");
                    } catch (UDDIException e1) {
                        context.logAndAudit(
                                SystemMessages.UDDI_PUBLISH_ENDPOINT_ROLLBACK_FAILED,
                                ExceptionUtils.getDebugException( e ),
                                bindingKey,
                                ExceptionUtils.getMessage(e1) );
                    }

                    //delete the entity
                    try {
                        proxiedServiceInfoManager.delete(uddiproxiedServiceInfo);
                    } catch (DeleteException e1) {
                        logger.log(Level.WARNING, "Could not delete UDDIProxiedServiceInfo (#"+uddiproxiedServiceInfo.getOid()+")", e1);
                    }
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e, "Database error when publishing proxy endpoint.");
            } catch (UDDIException ue) {
                context.logAndAudit( SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, ue, ExceptionUtils.getMessage(ue));
                throw ue;
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
        public void apply( final UDDITaskContext context ) throws UDDIException {
            try {
                if(uddiproxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISHING){
                    throw new UDDIException("UDDIProxiedServiceInfo is in an incorrect state. Must be in publishing state");
                }
                if(uddiproxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY){
                    throw new UDDIException("UDDIProxiedServiceInfo is the wrong type. Must be of type PROXY");
                }

                final PublishedService publishedService = serviceCache.getCachedService(uddiproxiedServiceInfo.getPublishedServiceOid());

                final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(publishedService.getOid());
                //protected service gateway external wsdl url
                final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(publishedService.getOid());

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(uddiproxiedServiceInfo.getUddiRegistryOid());
                if(uddiRegistry==null) throw new UDDIException("UDDI Registry #"+uddiproxiedServiceInfo.getUddiRegistryOid()+"' not found.");
                if(!uddiRegistry.isEnabled()) throw new UDDIException("UDDI Registry '"+uddiRegistry.getName()+"' is disabled.");

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    throw new UDDIException("Unable to parse WSDL for service "+publishedService.getName()+"(#" + publishedService.getOid()+")", e);
                }

                BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher(wsdl, publishedService.getOid(), uddiHelper.newUDDIClientConfig( uddiRegistry ));
                //this is best effort commit / rollback
                List<UDDIBusinessService> uddiBusinessServices = businessServicePublisher.publishServicesToUDDIRegistry(
                        protectedServiceExternalURL, protectedServiceWsdlURL, uddiproxiedServiceInfo.getUddiBusinessKey());
                //Create all required UDDIProxiedService
                for(UDDIBusinessService bs: uddiBusinessServices){
                    final UDDIProxiedService proxiedService = new UDDIProxiedService(bs.getServiceKey(),
                            bs.getServiceName(), bs.getWsdlServiceName());
                    //both parent and child records must be set before save
                    uddiproxiedServiceInfo.getProxiedServices().add(proxiedService);
                    proxiedService.setUddiProxiedServiceInfo(uddiproxiedServiceInfo);
                }

                try {
                    uddiproxiedServiceInfo.getUddiPublishStatus().setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                    proxiedServiceInfoManager.update(uddiproxiedServiceInfo);
                } catch (UpdateException e) {
                    logger.log( Level.WARNING, "Could not update UDDIProxiedServiceInfo", e);
                    try {
                        logger.log(Level.INFO, "Attempting to rollback UDDI updates");
                        //Attempt to roll back UDDI updates
                        UDDIClient uddiClient = uddiHelper.newUDDIClient( uddiRegistry );
                        uddiClient.deleteUDDIBusinessServices(uddiBusinessServices);
                        logger.log(Level.INFO, "UDDI updates rolled back successfully");
                    } catch (UDDIException e1) {
                        context.logAndAudit(
                                SystemMessages.UDDI_PUBLISH_SERVICE_ROLLBACK_FAILED,
                                ExceptionUtils.getDebugException( e ),
                                ExceptionUtils.getMessage(e1) );
                    }
                    //delete the entity //todo note - for udpate task, will need to use the api delete gateway wsdl method, so change state first to deleting
                    try {
                        proxiedServiceInfoManager.delete(uddiproxiedServiceInfo);
                    } catch (DeleteException e1) {
                        logger.log(Level.WARNING, "Could not delete UDDIProxiedServiceInfo (#"+ uddiproxiedServiceInfo.getOid()+")", e1);
                    }
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, "Database error when publishing proxy services.");
            } catch (UDDIException ue) {
                context.logAndAudit( SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, ue, ExceptionUtils.getMessage(ue));
                throw ue;
            }
        }
    }
}
