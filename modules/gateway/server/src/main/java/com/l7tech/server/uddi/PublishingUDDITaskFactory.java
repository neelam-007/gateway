package com.l7tech.server.uddi;

import com.l7tech.server.service.ServiceCache;
import com.l7tech.uddi.*;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.wsdl.Wsdl;

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
            switch ( publishUDDIEvent.getType() ){
                case CREATE_PROXY:
                    task = new PublishUDDITask(
                            publishUDDIEvent.getUddiProxiedServiceInfo(),
                            uddiProxiedServiceInfoManager,
                            serviceCache,
                            uddiHelper,
                            uddiRegistryManager);
                    break;
                case ADD_BINDING:
                    task = new PublishUDDIEndpointTask(
                            publishUDDIEvent.getUddiProxiedServiceInfo(),
                            uddiProxiedServiceInfoManager,
                            uddiServiceControlManager, uddiRegistryManager, serviceCache, uddiHelper);
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

    private static final class PublishUDDIEndpointTask extends UDDITask{
        private static final Logger logger = Logger.getLogger(PublishUDDITask.class.getName());

        private final long uddiProxiedServiceInfoOid;
        private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
        private final UDDIServiceControlManager uddiServiceControlManager;
        private final UDDIRegistryManager uddiRegistryManager;
        private final ServiceCache serviceCache;
        private final UDDIHelper uddiHelper;

        private PublishUDDIEndpointTask(final long uddiProxiedServiceInfoOid,
                                        final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                                        final UDDIServiceControlManager uddiServiceControlManager, UDDIRegistryManager uddiRegistryManager, ServiceCache serviceCache, UDDIHelper uddiHelper) {
            this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
            this.proxiedServiceInfoManager = proxiedServiceInfoManager;
            this.uddiServiceControlManager = uddiServiceControlManager;
            this.uddiRegistryManager = uddiRegistryManager;
            this.serviceCache = serviceCache;
            this.uddiHelper = uddiHelper;
        }

        @Override
        public void apply(UDDITaskContext context) throws UDDIException {
            try {
                final UDDIProxiedServiceInfo proxiedServiceInfo = proxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
                //this is a coding error if we cannot find
                if(proxiedServiceInfo == null) throw new IllegalStateException("Cannot find UDDiProxiedServiceInfo with oid: " + uddiProxiedServiceInfoOid);
                if(proxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISHING.getStatus()){
                    throw new IllegalStateException("UDDIProxiedServiceInfo is in an incorrect state. Must be in publishing state");
                }
                if(proxiedServiceInfo.getType() != UDDIProxiedServiceInfo.PublishType.ENDPOINT){
                    throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type ENDPOINT");                    
                }

                final PublishedService publishedService = serviceCache.getCachedService(proxiedServiceInfo.getPublishedServiceOid());

                final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(publishedService.getOid());
                //protected service gateway external wsdl url
                final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(publishedService.getOid());

                final UDDIServiceControl serviceControl = uddiServiceControlManager.findByPublishedServiceOid(proxiedServiceInfo.getPublishedServiceOid());
                if(serviceControl == null)
                    throw new UDDIException("No UDDIServiceControl found for PublishedService with id #(" + proxiedServiceInfo.getPublishedServiceOid()+")");

                if(!serviceControl.isUnderUddiControl())
                    throw new UDDIException("PublishedService with id #(" + proxiedServiceInfo.getPublishedServiceOid()+") is not under UDDI control");

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(proxiedServiceInfo.getUddiRegistryOid());
                if(!uddiRegistry.isEnabled()) throw new UDDIException("UDDI Registry '"+uddiRegistry.getName()+"' is disabled.");

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    throw new UDDIException("Unable to parse WSDL from service (#" + publishedService.getOid()+")", e);
                }

                final UDDIClient uddiClient = uddiHelper.newUDDIClient(uddiRegistry);
                BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher(wsdl, publishedService.getOid(), uddiHelper.newUDDIClientConfig( uddiRegistry ));
                //provides best effort commit / rollback for all UDDI interactions
                final String bindingKey = businessServicePublisher.publishEndPointToExistingService(serviceControl.getUddiServiceKey(),
                        serviceControl.getWsdlPortName(), protectedServiceExternalURL, protectedServiceWsdlURL );

                try {
                    proxiedServiceInfo.getUddiPublishStatus().setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED.getStatus());
                    proxiedServiceInfo.setProxyBindingKey(bindingKey);
                    proxiedServiceInfoManager.update(proxiedServiceInfo);
                } catch (UpdateException e) {
                    logger.log( Level.WARNING, "Could not update UDDIProxiedServiceInfo", e);
                    try {
                        logger.log(Level.WARNING, "Attempting to rollback UDDI updates");
                        uddiClient.deleteBindingTemplate(bindingKey);
                        logger.log(Level.WARNING, "UDDI updates rolled back successfully");
                    } catch (UDDIException e1) {
                        logger.log(Level.WARNING, "Could not rollback UDDI updates", e1);
                    }

                    //delete the entity
                    try {
                        proxiedServiceInfoManager.delete(proxiedServiceInfo);
                    } catch (DeleteException e1) {
                        logger.log(Level.WARNING, "Could not delete UDDIProxiedServiceInfo (#"+proxiedServiceInfo.getOid()+")", e1);
                    }
                }
            } catch (FindException e) {
                logger.log( Level.WARNING, "Error accessing UDDIRegistry", e );
            }

        }
    }
    
    private static final class PublishUDDITask extends UDDITask{
        private static final Logger logger = Logger.getLogger(PublishUDDITask.class.getName());

        private final long uddiProxiedServiceInfoOid;
        private final UDDIProxiedServiceInfoManager proxiedServiceInfoManager;
        private final ServiceCache serviceCache;
        private final UDDIHelper uddiHelper;
        private final UDDIRegistryManager uddiRegistryManager;

        public PublishUDDITask(final long uddiProxiedServiceInfoOid,
                               final UDDIProxiedServiceInfoManager proxiedServiceInfoManager,
                               final ServiceCache serviceCache,
                               final UDDIHelper uddiHelper,
                               final UDDIRegistryManager uddiRegistryManager) {
            this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
            this.proxiedServiceInfoManager = proxiedServiceInfoManager;
            this.serviceCache = serviceCache;
            this.uddiHelper = uddiHelper;
            this.uddiRegistryManager = uddiRegistryManager;
        }

        @Override     //todo auditing
        public void apply(final UDDITaskContext context) throws UDDIException {
            try {
                final UDDIProxiedServiceInfo proxiedServiceInfo = proxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
                //this is a coding error if we cannot find
                if(proxiedServiceInfo == null) throw new IllegalStateException("Cannot find UDDiProxiedServiceInfo with oid: " + uddiProxiedServiceInfoOid);
                if(proxiedServiceInfo.getUddiPublishStatus().getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISHING.getStatus()){
                    throw new IllegalStateException("UDDIProxiedServiceInfo is in an incorrect state. Must be in publishing state");
                }
                if(proxiedServiceInfo.getType() != UDDIProxiedServiceInfo.PublishType.PROXY){
                    throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type PROXY");                    
                }

                final PublishedService publishedService = serviceCache.getCachedService(proxiedServiceInfo.getPublishedServiceOid());

                final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(publishedService.getOid());
                //protected service gateway external wsdl url
                final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(publishedService.getOid());

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(proxiedServiceInfo.getUddiRegistryOid());
                if(!uddiRegistry.isEnabled()) throw new UDDIException("UDDI Registry '"+uddiRegistry.getName()+"' is disabled.");

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    throw new UDDIException("Unable to parse WSDL from service (#" + publishedService.getOid()+")", e);
                }

                BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher(wsdl, publishedService.getOid(), uddiHelper.newUDDIClientConfig( uddiRegistry ));
                //this is best effort commit / rollback
                List<UDDIBusinessService> uddiBusinessServices = businessServicePublisher.publishServicesToUDDIRegistry(
                        protectedServiceExternalURL, protectedServiceWsdlURL, proxiedServiceInfo.getUddiBusinessKey());
                //Create all required UDDIProxiedService
                for(UDDIBusinessService bs: uddiBusinessServices){
                    final UDDIProxiedService proxiedService = new UDDIProxiedService(bs.getServiceKey(),
                            bs.getServiceName(), bs.getWsdlServiceName());
                    //both parent and child records must be set before save
                    proxiedServiceInfo.getProxiedServices().add(proxiedService);
                    proxiedService.setUddiProxiedServiceInfo(proxiedServiceInfo);
                }

                try {
                    proxiedServiceInfo.getUddiPublishStatus().setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED.getStatus());
                    proxiedServiceInfoManager.update(proxiedServiceInfo);
                } catch (UpdateException e) {
                    logger.log( Level.WARNING, "Could not update UDDIProxiedServiceInfo", e);
                    try {
                        logger.log(Level.WARNING, "Attempting to rollback UDDI updates");
                        //Attempt to roll back UDDI updates
                        UDDIClient uddiClient = uddiHelper.newUDDIClient( uddiRegistry );
                        uddiClient.deleteUDDIBusinessServices(uddiBusinessServices);
                        logger.log(Level.WARNING, "UDDI updates rolled back successfully");
                    } catch (UDDIException e1) {
                        logger.log(Level.WARNING, "Could not rollback UDDI updates", e1);
                    }
                    //delete the entity //todo note - for udpate task, will need to use the api delete gateway wsdl method, so change state first to deleting
                    try {
                        proxiedServiceInfoManager.delete(proxiedServiceInfo);
                    } catch (DeleteException e1) {
                        logger.log(Level.WARNING, "Could not delete UDDIProxiedServiceInfo (#"+proxiedServiceInfo.getOid()+")", e1);
                    }
                }
            } catch (FindException e) {
                logger.log( Level.WARNING, "Error accessing UDDIRegistry", e );
            }
        }
    }
}
