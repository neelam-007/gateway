package com.l7tech.server.uddi;

import com.l7tech.server.service.ServiceCache;
import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.WsdlToUDDIModelConverter;
import com.l7tech.uddi.BusinessServicePublisher;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIPublishStatus;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;

import javax.wsdl.WSDLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PublishingUDDITaskFactory extends UDDITaskFactory {

    //- PUBLIC

    public PublishingUDDITaskFactory( final UDDIRegistryManager uddiRegistryManager,
                                      final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                                      final ServiceCache serviceCache,
                                      final UDDIHelper uddiHelper ) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.serviceCache = serviceCache;
        this.uddiHelper = uddiHelper;
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
            }

        }

        return task;
    }

    //- PRIVATE

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager;
    private final ServiceCache serviceCache;
    private final UDDIHelper uddiHelper;

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
                //todo move most of this into UDDIHelper
                final PublishedService publishedService = serviceCache.getCachedService(proxiedServiceInfo.getPublishedServiceOid());

                final String protectedServiceExternalURL = uddiHelper.getExternalUrlForService(publishedService.getOid());
                //protected service gateway external wsdl url
                final String protectedServiceWsdlURL = uddiHelper.getExternalWsdlUrlForService(publishedService.getOid());

                final WsdlToUDDIModelConverter modelConverter;
                try {
                    modelConverter = new WsdlToUDDIModelConverter(publishedService.parsedWsdl(), protectedServiceWsdlURL,
                    protectedServiceExternalURL, proxiedServiceInfo.getUddiBusinessKey(), publishedService.getOid());
                } catch (WSDLException e) {
                    throw new UDDIException("Unable to parse WSDL from service (#" + publishedService.getOid()+")", e);
                }
                try {
                    modelConverter.convertWsdlToUDDIModel();
                } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
                    throw new UDDIException("Unable to convert WSDL from service (#" + publishedService.getOid()+") into UDDI object model.", e);
                }

                final List<BusinessService> wsdlBusinessServices = modelConverter.getBusinessServices();
                final Map<String, TModel> wsdlDependentTModels = modelConverter.getKeysToPublishedTModels();
                final Map<BusinessService, String> serviceToWsdlServiceName = modelConverter.getServiceKeyToWsdlServiceNameMap();

                BusinessServicePublisher businessServicePublisher = new BusinessServicePublisher();
                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey(proxiedServiceInfo.getUddiRegistryOid());
                if(!uddiRegistry.isEnabled()) throw new UDDIException("UDDI Registry '"+uddiRegistry.getName()+"' is disabled.");

                final UDDIClient uddiClient = UDDIHelper.newUDDIClient(uddiRegistry);
                //this is best effort commit / rollback
                businessServicePublisher.publishServicesToUDDIRegistry(uddiClient, wsdlBusinessServices, wsdlDependentTModels);

                //Create all required UDDIProxiedService
                for(BusinessService bs: wsdlBusinessServices){
                    final UDDIProxiedService proxiedService = new UDDIProxiedService(bs.getServiceKey(),
                            bs.getName().get(0).getValue(), serviceToWsdlServiceName.get(bs));
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
                        uddiClient.deleteBusinessServices(wsdlBusinessServices);
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
