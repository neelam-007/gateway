package com.l7tech.server.uddi;

import com.l7tech.server.service.ServiceCache;
import com.l7tech.uddi.*;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.Config;
import com.l7tech.util.ResourceUtils;

import javax.wsdl.WSDLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

/**
 * No tasks are thread safe for business logic. All access to each task defined in this factory must be serialized.
 * All tasks should only ever run on the same timer task in UDDICoordinator, otherwise publish state will not
 * be managed correctly
 */
public class PublishingUDDITaskFactory extends UDDITaskFactory {
    //- PUBLIC

    public PublishingUDDITaskFactory(final UDDIRegistryManager uddiRegistryManager,
                                     final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                                     final ServiceCache serviceCache,
                                     final UDDIHelper uddiHelper,
                                     final UDDIServiceControlManager uddiServiceControlManager,
                                     final UDDIPublishStatusManager uddiPublishStatusManager,
                                     final Config config) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.serviceCache = serviceCache;
        this.uddiHelper = uddiHelper;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiPublishStatusManager = uddiPublishStatusManager;
        this.config = config;
    }

    @Override
    public UDDITask buildUDDITask(final UDDIEvent event) {
        UDDITask task = null;

        if (event instanceof PublishUDDIEvent) {
            PublishUDDIEvent publishUDDIEvent = (PublishUDDIEvent) event;
            final UDDIProxiedServiceInfo uddiProxiedServiceInfo = publishUDDIEvent.getUddiProxiedServiceInfo();
            final UDDIPublishStatus publishStatus = publishUDDIEvent.getUddiPublishStatus();
            switch (uddiProxiedServiceInfo.getPublishType()) {
                case PROXY:
                    if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH ||
                            publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED) {
                        final PublishedService service = serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceOid());
                        task = new UpdatePublishUDDITask(this, service);
                    } else if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE ||
                            publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                        task = new DeletePublishUDDITask(this, publishUDDIEvent.getUddiProxiedServiceInfo().getOid());
                    }

                    break;
                case ENDPOINT:
                    if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH ||
                            publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED) {
                        task = new PublishUDDIEndpointTask(this, publishUDDIEvent.getUddiProxiedServiceInfo().getOid());
                    } else if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE ||
                            publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                        task = new DeleteUDDIEndpointTask(this, publishUDDIEvent.getUddiProxiedServiceInfo().getOid());
                    }

                    break;
                case OVERWRITE:
                    final PublishedService ps = serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceOid());
                    final UDDIServiceControl serviceControl = publishUDDIEvent.getServiceControl();
                    if (serviceControl == null)
                        throw new IllegalStateException("No UDDIServiceControl found in PublishUDDIEvent");

                    if (!serviceControl.isHasBeenOverwritten()) {
                        task = new OverwriteServicePublishUDDITask(this, ps);
                    } else {
                        if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH ||
                                publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED) {
                            final PublishedService service = serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceOid());
                            task = new UpdatePublishUDDITask(this, service);
                        } else if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE ||
                                publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                            task = new DeletePublishUDDITask(this, publishUDDIEvent.getUddiProxiedServiceInfo().getOid());
                        }
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
    private final Config config;
    private static final Logger logger = Logger.getLogger(PublishingUDDITaskFactory.class.getName());

    private static final class PublishUDDIEndpointTask extends UDDITask {
        private static final Logger logger = Logger.getLogger(PublishUDDIEndpointTask.class.getName());

        private final PublishingUDDITaskFactory factory;
        private final long uddiProxiedServiceInfoOid;

        private PublishUDDIEndpointTask(final PublishingUDDITaskFactory factory,
                                        final long uddiProxiedServiceInfoOid) {
            this.factory = factory;
            this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
        }

        /**
         * This task must deal with both the first publish to UDDI and any subsequent updates to the binding template
         *
         * @param context The context for the task
         */
        @Override
        public void apply(final UDDITaskContext context) throws UDDITaskException {
            UDDIPublishStatus uddiPublishStatus = null;
            try {
                //only work with the most up to date values from the database
                final UDDIProxiedServiceInfo uddiProxiedServiceInfo =
                        factory.uddiProxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
                if (uddiProxiedServiceInfo == null) return;

                uddiPublishStatus =
                        factory.uddiPublishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                if (uddiPublishStatus == null) return;

                //Only work with published information in the publish state
                if (uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH
                        && uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH_FAILED) {
                    logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the publish state. Nothing to do");
                    return;
                }

                if (uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.ENDPOINT) {
                    throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type ENDPOINT");
                }

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry =
                        factory.uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                if (uddiRegistry == null)
                    throw new IllegalStateException("UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") not found.");
                if (!uddiRegistry.isEnabled()) {
                    logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

                final PublishedService publishedService =
                        factory.serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceOid());

                final UDDIServiceControl serviceControl =
                        factory.uddiServiceControlManager.findByPublishedServiceOid(uddiProxiedServiceInfo.getPublishedServiceOid());
                if (serviceControl == null)
                    throw new IllegalStateException("No UDDIServiceControl found for PublishedService with id #(" + uddiProxiedServiceInfo.getPublishedServiceOid() + ")");

                if (uddiProxiedServiceInfo.isRemoveOtherBindings() && serviceControl.isUnderUddiControl()) {
                    throw new IllegalStateException("Cannot remove other bindings when the WSDL is under UDDI control");
                }

                logger.log(Level.INFO, "Publishing endpoint from Published Service id #(" + publishedService.getOid() + ") to UDDI registry id #(" + uddiRegistry.getOid() + ")");

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service " + publishedService.getName() + "(#" + publishedService.getOid() + "). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));

                    uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                    factory.uddiPublishStatusManager.update(uddiPublishStatus);
                    return;
                }

                final Collection<Pair<String, String>> allEndpointPairs =
                        factory.uddiHelper.getAllExternalEndpointAndWsdlUrls(publishedService.getOid());
                
                final String publishedHostname = uddiProxiedServiceInfo.getPublishedHostname();
                BusinessServicePublisher businessServicePublisher = null;
                try {
                    businessServicePublisher = new BusinessServicePublisher(
                        wsdl,
                        publishedService.getOid(),
                        factory.uddiHelper.newUDDIClientConfig(uddiRegistry));

                    //provides best effort commit / rollback for all UDDI interactions
                    businessServicePublisher.publishBindingTemplate(
                            serviceControl.getUddiServiceKey(),
                            serviceControl.getWsdlPortName(),
                            serviceControl.getWsdlPortBinding(),
                            serviceControl.getWsdlPortBindingNamespace(), 
                            publishedHostname,
                            allEndpointPairs,
                            uddiProxiedServiceInfo.isRemoveOtherBindings()
                    );
                    
                } catch (UDDIException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e, ExceptionUtils.getMessage(e));
                    return;
                } finally {
                    ResourceUtils.closeQuietly( businessServicePublisher );
                }

                uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                factory.uddiPublishStatusManager.update(uddiPublishStatus);
                uddiProxiedServiceInfo.setPublishedHostname(publishedHostname);
                factory.uddiProxiedServiceInfoManager.update(uddiProxiedServiceInfo);
                if (uddiProxiedServiceInfo.isRemoveOtherBindings()) {
                    if (!serviceControl.isHasHadEndpointRemoved()) {
                        if (serviceControl.isUnderUddiControl()) {
                            serviceControl.setUnderUddiControl(false);//if this is actually done a coding error happened
                            logger.log(Level.WARNING, "Set UDDIServiceControl isUnderUDDIControl to be false");
                        }
                        serviceControl.setHasHadEndpointRemoved(true);
                        factory.uddiServiceControlManager.update(serviceControl);
                    }
                }
            } catch (ObjectModelException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e, "Database error when publishing proxy endpoint.");
                throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
            } catch (RuntimeException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_UNEXPECTED_ERROR, e, ExceptionUtils.getMessage(e));
                if(uddiPublishStatus != null){
                    throwHandledTaskException(context, e, uddiPublishStatus.getOid(), factory, true);
                }
            }
        }
    }

    /**
     * Publish to UDDI for the first time or update information already published
     */
    private static final class UpdatePublishUDDITask extends UDDITask {//todo [Donal] rename
        private static final Logger logger = Logger.getLogger(UpdatePublishUDDITask.class.getName());

        private final PublishingUDDITaskFactory factory;
        private final PublishedService publishedService;

        public UpdatePublishUDDITask(final PublishingUDDITaskFactory factory,
                                     final PublishedService publishedService) {
            this.factory = factory;
            this.publishedService = publishedService;
        }

        @Override
        public void apply(final UDDITaskContext context) throws UDDITaskException {
            UDDIPublishStatus uddiPublishStatus = null;
            try {
                //Get the most up to date version of the UDDIProxiedServiceInfo - the queue can be stale - will cause loop with eventual hibernate errors
                final UDDIProxiedServiceInfo uddiProxiedServiceInfo =
                        factory.uddiProxiedServiceInfoManager.findByPublishedServiceOid(publishedService.getOid());
                if (uddiProxiedServiceInfo == null) return;

                uddiPublishStatus =
                        factory.uddiPublishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                if (uddiPublishStatus == null) return;

                //we let a publish_failed through so it can be retried
                if (uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH_FAILED &&
                        uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH) {
                    logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the publish, publish_failed state. Nothing to do");
                    return;
                }

                if (uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY &&
                        uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.OVERWRITE) {
                    throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type PROXY or OVERWRITE");
                }

                final UDDIServiceControl serviceControl =
                        factory.uddiServiceControlManager.findByPublishedServiceOid(publishedService.getOid());
                if (serviceControl == null && uddiProxiedServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.OVERWRITE) {
                    throw new IllegalStateException("When a service has been overwritten, we must have a record of its UDDI information");
                }

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry =
                        factory.uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                if (uddiRegistry == null)
                    throw new IllegalStateException("UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") not found.");
                if (!uddiRegistry.isEnabled()) {
                    logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

                logger.log(Level.INFO,
                        "Publishing Gateway WSDL for Published Service id #(" + publishedService.getOid() + ") to UDDI Registry id #(" + uddiRegistry.getOid() + ")");

                final boolean serviceWasOverwritten;

                if (uddiProxiedServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.OVERWRITE) {
                    if (serviceControl == null) {
                        //this means that our service control was deleted, it contains the information we need
                        context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, "Our record of which UDDI BusinessService was overwriten is gone. Deleting UDDIProxiedServiceInfo");
                        factory.uddiProxiedServiceInfoManager.delete(uddiProxiedServiceInfo);
                        return;
                    }
                    if (!serviceControl.isHasBeenOverwritten()) {
                        throw new IllegalStateException("If we have an overwritten service, then we cannot update it unless it has been overwritten in UDDI");
                    }
                    serviceWasOverwritten = true;
                } else {
                    serviceWasOverwritten = false;
                }

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service " + publishedService.getName() + "(#" + publishedService.getOid() + "). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));
                    uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                    factory.uddiPublishStatusManager.update(uddiPublishStatus);
                    return;
                }

                //Get existing services
                final Set<String> serviceKeys = new HashSet<String>();
                final Set<UDDIProxiedService> allProxiedServices = uddiProxiedServiceInfo.getProxiedServices();
                switch (uddiProxiedServiceInfo.getPublishType()) {
                    case PROXY:
                        for (UDDIProxiedService ps : allProxiedServices) {
                            serviceKeys.add(ps.getUddiServiceKey());
                        }
                        break;
                    case OVERWRITE:
                        serviceKeys.add(serviceControl.getUddiServiceKey());//serviceControl cannot be null see above illegal state
                        break;
                    //no default, see above Illegal state
                }

                final UDDIRegistrySpecificMetaData registrySpecificMetaData =
                        PublishingUDDITaskFactory.getRegistrySpecificMetaData(uddiRegistry, serviceControl, factory);

                final Collection<Pair<String, String>> allEndpointPairs =
                        factory.uddiHelper.getAllExternalEndpointAndWsdlUrls(publishedService.getOid());

                final Pair<Set<String>, Set<UDDIBusinessService>> deletedAndNewServices;
                BusinessServicePublisher businessServicePublisher = null;
                try {
                    businessServicePublisher = new BusinessServicePublisher(
                            wsdl,
                            uddiProxiedServiceInfo.getPublishedServiceOid(),
                            factory.uddiHelper.newUDDIClientConfig(uddiRegistry));

                    deletedAndNewServices = businessServicePublisher.publishServicesToUDDIRegistry(
                            uddiProxiedServiceInfo.getUddiBusinessKey(),
                            serviceKeys, serviceWasOverwritten, registrySpecificMetaData, allEndpointPairs);
                } catch (UDDIException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, ExceptionUtils.getMessage(e));
                    return;
                } finally {
                    ResourceUtils.closeQuietly( businessServicePublisher );
                }

                if (uddiProxiedServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.PROXY) {
                    //now manage db entities
                    Set<String> deleteSet = deletedAndNewServices.left;
                    //update all child entities
                    Set<UDDIProxiedService> removeSet = new HashSet<UDDIProxiedService>();
                    for (String deleteServiceKey : deleteSet) {
                        for (UDDIProxiedService proxiedService : allProxiedServices) {
                            if (proxiedService.getUddiServiceKey().equals(deleteServiceKey)) {
                                removeSet.add(proxiedService);
                                logger.log(Level.FINE, "Deleting UDDIProxiedService for serviceKey: " + deleteServiceKey);
                            }
                        }
                    }

                    uddiProxiedServiceInfo.getProxiedServices().removeAll(removeSet);

                    //create required new UDDIProxiedServices
                    Set<UDDIBusinessService> newlyCreatedServices = deletedAndNewServices.right;
                    for (UDDIBusinessService bs : newlyCreatedServices) {
                        final UDDIProxiedService proxiedService = new UDDIProxiedService(
                                bs.getServiceKey(), bs.getServiceName(), bs.getWsdlServiceName(), bs.getWsdlServiceNamespace());
                        
                        proxiedService.setUddiProxiedServiceInfo(uddiProxiedServiceInfo);
                        uddiProxiedServiceInfo.getProxiedServices().add(proxiedService);
                    }
                }

                uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                factory.uddiPublishStatusManager.update(uddiPublishStatus);
                factory.uddiProxiedServiceInfoManager.update(uddiProxiedServiceInfo);
            } catch (ObjectModelException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, "Database error when publishing proxy services.");
                throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
            } catch (RuntimeException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_UNEXPECTED_ERROR, e, ExceptionUtils.getMessage(e));
                if(uddiPublishStatus != null){
                    throwHandledTaskException(context, e, uddiPublishStatus.getOid(), factory, true);
                }
            }
        }
    }

    /**
     * If any UDDI Registry has meta data requirements specific to it, then here is the current place to collect
     * that infomration from the UDDIServiceControl and UDDIRegistry. If the UDDIProxiedServiceInfo is required it
     * can also be added as an argument
     * 
     * @param uddiRegistry
     * @param serviceControl
     * @param uddiFactory
     * @return UDDIRegistrySpecificMetaData, not null if there are registry specific meta data requirements, null otherwise
     */
    private static UDDIRegistrySpecificMetaData getRegistrySpecificMetaData(final UDDIRegistry uddiRegistry,
                                                                            final UDDIServiceControl serviceControl,
                                                                            final PublishingUDDITaskFactory uddiFactory) {
        final boolean isActiveSOAVirtualService = serviceControl != null &&
                uddiRegistry.getUddiRegistryType().equals(UDDIRegistry.UDDIRegistryType.CENTRASITE_ACTIVE_SOA.toString()) &&
                serviceControl.getUddiRegistryOid() == uddiRegistry.getOid();
        //we know here whether the UDDIRegistry is Active SOA or not
        //true when both the original service and proxied service will be in the same registry

        if (isActiveSOAVirtualService) {
            return new UDDIRegistrySpecificMetaData() {
                @Override
                public Collection<UDDIClient.UDDIKeyedReference> getBusinessServiceKeyedReferences() {
                    final Collection<UDDIClient.UDDIKeyedReference> returnColl = new ArrayList<UDDIClient.UDDIKeyedReference>();

                    //download the tmodel key for active soa
                    final String virtualKey = uddiFactory.config.getProperty("uddi.activesoa.virtual.service.tmodelkey", "uddi:9de0173b-5117-11de-8cf9-da0192ff3739");

                    UDDIClient uddiClient = null;
                    try {
                        uddiClient = uddiFactory.uddiHelper.newUDDIClient(uddiRegistry);
                        uddiClient.getOperationalInfo( virtualKey );
                    } catch (UDDIInvalidKeyException e) {
                        logger.log(Level.INFO, "No virtual keyed reference will be added as no tModel can be found for tModelKey '" + virtualKey + "'.");
                        return null;
                    } catch (UDDINetworkException e) {
                        logger.log(Level.WARNING, "No virtual keyed reference will be added, network error checking for tModelKey '" + virtualKey + "', '"+ExceptionUtils.getMessage( e )+"'.", ExceptionUtils.getDebugException( e ));
                        return null;
                     } catch (UDDIException e) {
                        logger.log(Level.WARNING, "No virtual keyed reference will be added, error checking for tModelKey '" + virtualKey + "', '"+ExceptionUtils.getMessage( e )+"'.", ExceptionUtils.getDebugException( e ));
                        return null;
                    } finally {
                        ResourceUtils.closeQuietly( uddiClient );
                    }

                    final UDDIClient.UDDIKeyedReference kr =
                            new UDDIClient.UDDIKeyedReference(virtualKey, null, "Virtual service");
                    returnColl.add(kr);
                    return Collections.unmodifiableCollection(returnColl);
                }

                @Override
                public Collection<UDDIClient.UDDIKeyedReferenceGroup> getBusinessServiceKeyedReferenceGroups() {
                    final Collection<UDDIClient.UDDIKeyedReferenceGroup> returnColl = new ArrayList<UDDIClient.UDDIKeyedReferenceGroup>();
                    final UDDIClient.UDDIKeyedReference kr =
                            new UDDIClient.UDDIKeyedReference("uddi:uddi.org:categorization:general_keywords",
                                    "Contains", serviceControl.getUddiServiceKey());

                    final Collection<UDDIClient.UDDIKeyedReference> allRefs = new ArrayList<UDDIClient.UDDIKeyedReference>();
                    allRefs.add(kr);

                    final UDDIClient.UDDIKeyedReferenceGroup krg =
                            new UDDIClient.UDDIKeyedReferenceGroup("uddi:centrasite.com:attributes:relationship",
                                    Collections.unmodifiableCollection(allRefs));

                    returnColl.add(krg);
                    return Collections.unmodifiableCollection(returnColl);
                }
            };
        }

        return null;
    }

    /**
     * This task should only ever run once for a UDDIServiceControl in it's lifetime.
     * For udpates the normal udpate task should be used
     */
    private static final class OverwriteServicePublishUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger(UpdatePublishUDDITask.class.getName());

        private final PublishingUDDITaskFactory factory;
        private final PublishedService publishedService;

        public OverwriteServicePublishUDDITask(final PublishingUDDITaskFactory factory,
                                               final PublishedService publishedService) {
            this.factory = factory;
            this.publishedService = publishedService;
        }

        @Override
        public void apply(final UDDITaskContext context) throws UDDITaskException {
            UDDIPublishStatus uddiPublishStatus = null;
            try {
                //Get the most up to date version of the UDDIProxiedServiceInfo - the queue can be stale
                final UDDIProxiedServiceInfo uddiProxiedServiceInfo =
                        factory.uddiProxiedServiceInfoManager.findByPublishedServiceOid(publishedService.getOid());
                if (uddiProxiedServiceInfo == null) return;

                uddiPublishStatus =
                        factory.uddiPublishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                if (uddiPublishStatus == null) return;

                //we let a publish_failed through so it can be retried
                if (uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH_FAILED &&
                        uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH) {
                    logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the publish, publish_failed state. Nothing to do");
                    return;
                }

                if (uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.OVERWRITE) {
                    throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type OVERWRITE");
                }

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry =
                        factory.uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                if (uddiRegistry == null)
                    throw new IllegalStateException("UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") not found.");
                if (!uddiRegistry.isEnabled()) {
                    logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

                final PublishedService publishedService =
                        factory.serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceOid());

                final UDDIServiceControl serviceControl =
                        factory.uddiServiceControlManager.findByPublishedServiceOid(uddiProxiedServiceInfo.getPublishedServiceOid());
                if (serviceControl == null)//UDDIServiceControl delete logic does not allow this
                    throw new IllegalStateException("No UDDIServiceControl found for PublishedService with id #(" + uddiProxiedServiceInfo.getPublishedServiceOid() + ")");

                if (serviceControl.isHasBeenOverwritten()) {
                    logger.log(Level.FINER, "UDDIProxiedServiceInfo has already overwritten the BusinessService in UDDI. Nothing to do");
                    return;
                }

                logger.log(Level.INFO, "Overwriting BusinessService with Gateway WSDL from Published Service id #(" + publishedService.getOid() + ") to UDDI registry id #(" + uddiRegistry.getOid() + ")");

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service " + publishedService.getName() + "(#" + publishedService.getOid() + "). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));
                    uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                    factory.uddiPublishStatusManager.update(uddiPublishStatus);
                    return;
                }

                BusinessServicePublisher businessServicePublisher = null;
                try {
                    businessServicePublisher = new BusinessServicePublisher(
                        wsdl,
                        uddiProxiedServiceInfo.getPublishedServiceOid(),
                        factory.uddiHelper.newUDDIClientConfig(uddiRegistry));

                    final Collection<Pair<String, String>> allEndpointPairs =
                            factory.uddiHelper.getAllExternalEndpointAndWsdlUrls(publishedService.getOid());

                    businessServicePublisher.overwriteServiceInUDDI(serviceControl.getUddiServiceKey(),
                            serviceControl.getUddiBusinessKey(), allEndpointPairs);

                } catch (UDDIException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, ExceptionUtils.getMessage(e));
                    return;
                } finally {
                    ResourceUtils.closeQuietly( businessServicePublisher );
                }

                uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                factory.uddiPublishStatusManager.update(uddiPublishStatus);
                serviceControl.setHasBeenOverwritten(true);
                serviceControl.setUnderUddiControl(false);//can no longer be under UDDI control
                factory.uddiServiceControlManager.update(serviceControl);
            } catch (ObjectModelException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, "Database error when publishing proxy services.");
                throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
            } catch (RuntimeException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_UNEXPECTED_ERROR, e, ExceptionUtils.getMessage(e));
                if(uddiPublishStatus != null){
                    throwHandledTaskException(context, e, uddiPublishStatus.getOid(), factory, true);
                }
            }
        }
    }

    private static final class DeleteUDDIEndpointTask extends UDDITask {
        private static final Logger logger = Logger.getLogger(DeleteUDDIEndpointTask.class.getName());

        private final PublishingUDDITaskFactory factory;
        private final long uddiProxiedServiceInfoOid;

        public DeleteUDDIEndpointTask(final PublishingUDDITaskFactory factory,
                                      final long uddiProxiedServiceInfoOid) {
            this.factory = factory;
            this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
        }

        @Override
        public void apply(final UDDITaskContext context) throws UDDITaskException {
            UDDIPublishStatus uddiPublishStatus = null;
            try {
                //only work with the most up to date values from the database
                final UDDIProxiedServiceInfo uddiProxiedServiceInfo =
                        factory.uddiProxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
                if (uddiProxiedServiceInfo == null) return;

                uddiPublishStatus =
                        factory.uddiPublishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                if (uddiPublishStatus == null) return;

                if (uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE &&
                        uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                    logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the delete state. Nothing to do");
                    return;
                }
                if (uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.ENDPOINT) {
                    throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type ENDPOINT");
                }

                final UDDIServiceControl serviceControl =
                        factory.uddiServiceControlManager.findByPublishedServiceOid(uddiProxiedServiceInfo.getPublishedServiceOid());
                if (serviceControl == null)
                    throw new IllegalStateException("No UDDIServiceControl found for PublishedService with id #(" + uddiProxiedServiceInfo.getPublishedServiceOid() + ")");
                
                //publishing an endpoint requires that the service is under UDDI Control. However not enforcing here
                //as if we have a published endpoint and we have all the information to delete it, then we should

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry =
                        factory.uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                if (uddiRegistry == null)
                    throw new IllegalStateException("UDDI Registry #" + uddiProxiedServiceInfo.getUddiRegistryOid() + "' not found.");
                if (!uddiRegistry.isEnabled()) {
                    logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

                logger.log(Level.INFO, "Deleting gateway endpoints from BusinessService with serviceKey #(" + serviceControl.getUddiServiceKey() + " in UDDI Registry id #(" + uddiRegistry.getOid() + ")");

                final PublishedService publishedService =
                        factory.serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceOid());

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service " + publishedService.getName() + "(#" + publishedService.getOid() + "). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));

                    uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                    factory.uddiPublishStatusManager.update(uddiPublishStatus);
                    return;
                }

                //Only try and delete from UDDI if information was successfully published
                BusinessServicePublisher publisher = null;
                try {
                    publisher = new BusinessServicePublisher(wsdl, publishedService.getOid(), factory.uddiHelper.newUDDIClientConfig(uddiRegistry));
                    publisher.deleteGatewayBindingTemplates(serviceControl.getUddiServiceKey(), uddiProxiedServiceInfo.getPublishedHostname());
                    logger.log(Level.FINE, "Endpoints successfully deleted");
                } catch (UDDIException e) {
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_BINDING,
                            ExceptionUtils.getDebugException(e),
                            serviceControl.getUddiServiceKey());
                    PublishingUDDITaskFactory.handleUddiDeleteFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                    return;
                } finally {
                    ResourceUtils.closeQuietly( publisher );
                }

                factory.uddiProxiedServiceInfoManager.delete(uddiProxiedServiceInfo);
            } catch (ObjectModelException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_FAILED, e, "Database error when deleting proxy endpoint.");
                throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
            } catch (RuntimeException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_UNEXPECTED_ERROR, e, ExceptionUtils.getMessage(e));
                if(uddiPublishStatus != null){
                    throwHandledTaskException(context, e, uddiPublishStatus.getOid(), factory, false);
                }
            }
        }
    }

    private static final class DeletePublishUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger(DeletePublishUDDITask.class.getName());

        private final PublishingUDDITaskFactory factory;
        private final long uddiProxiedServiceInfoOid;

        public DeletePublishUDDITask(final PublishingUDDITaskFactory factory,
                                     final long uddiProxiedServiceInfoOid) {
            this.factory = factory;
            this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
        }

        @Override
        public void apply(final UDDITaskContext context) throws UDDITaskException {
            UDDIPublishStatus uddiPublishStatus = null;
            try {
                //only work with the most up to date values from the database
                final UDDIProxiedServiceInfo uddiProxiedServiceInfo =
                        factory.uddiProxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
                if (uddiProxiedServiceInfo == null) return;

                uddiPublishStatus =
                        factory.uddiPublishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                if (uddiPublishStatus == null) return;

                //Only work with published information in the publish state
                if (uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE
                        && uddiPublishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                    logger.log(Level.FINER, "UDDIProxiedServiceInfo is not in the delete state. Nothing to do");
                    return;
                }

                if (uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY &&
                        uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.OVERWRITE) {
                    throw new IllegalStateException("UDDIProxiedServiceInfo is the wrong type. Must be of type PROXY or OVERWRITE");
                }

                final UDDIServiceControl serviceControl =
                        factory.uddiServiceControlManager.findByPublishedServiceOid(uddiProxiedServiceInfo.getPublishedServiceOid());
                if (serviceControl == null && uddiProxiedServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.OVERWRITE) {
                    throw new IllegalStateException("When a service has been overwritten, we must have a record of its UDDI information");
                }

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry =
                        factory.uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                if (uddiRegistry == null)
                    throw new IllegalStateException("UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") not found.");
                if (!uddiRegistry.isEnabled()) {
                    logger.log(Level.WARNING, "Canot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

                final boolean serviceWasOverwritten;
                if (uddiProxiedServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.OVERWRITE) {
                    if (serviceControl == null) {
                        //this means that our service control was deleted, it contains the information we need
                        context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, "Our record of which UDDI BusinessService was overwriten is gone. Deleting UDDIProxiedServiceInfo");
                        factory.uddiProxiedServiceInfoManager.delete(uddiProxiedServiceInfo);
                        return;
                    }
                    if (!serviceControl.isHasBeenOverwritten()) {
                        throw new IllegalStateException("If we have an overwritten service, then we cannot update it unless it has been overwritten in UDDI");
                    }
                    serviceWasOverwritten = true;
                    logger.log(Level.INFO, "Deleting overwritten BusinessService with serviceKey #(" + serviceControl.getUddiServiceKey() + ") in UDDI Registry id #(" + uddiRegistry.getOid() + ")");
                } else {
                    serviceWasOverwritten = false;
                    logger.log(Level.INFO,
                            "Deleting published Gateway WSDL from Published Service id #(" + uddiProxiedServiceInfo.getPublishedServiceOid() + ")s in UDDI Registry id #(" + uddiRegistry.getOid() + ")");
                }

                final Set<UDDIProxiedService> proxiedServices = uddiProxiedServiceInfo.getProxiedServices();
                final Set<String> keysToDelete = new HashSet<String>();
                switch (uddiProxiedServiceInfo.getPublishType()) {
                    case PROXY:
                        for (UDDIProxiedService proxiedService : proxiedServices) {
                            keysToDelete.add(proxiedService.getUddiServiceKey());
                        }
                        break;
                    case OVERWRITE:
                        if (serviceWasOverwritten)
                            keysToDelete.add(serviceControl.getUddiServiceKey());//serviceControl cannot be null
                        break;
                    //no default, see illegal state above
                }

                UDDIClient uddiClient = null;
                try {
                    uddiClient = factory.uddiHelper.newUDDIClient(uddiRegistry);
                    for (String serviceKey : keysToDelete) {
                        try {
                            uddiClient.deleteBusinessServicesByKey(serviceKey);
                        } catch (UDDIException e) {
                            if (e instanceof UDDIInvalidKeyException) {
                                logger.log(Level.FINE, "Invalid serviceKey found #(" + serviceKey + "). Cannot delete from from UDDI Registry");
                                //move onto the next
                            } else {
                                throw e;
                            }
                        }
                    }
                    logger.log(Level.FINE, "Successfully deleted published Gateway WSDL from UDDI Registry");
                } catch (UDDIException e) {
                    context.logAndAudit(SystemMessages.UDDI_REMOVE_SERVICE_FAILED, e, ExceptionUtils.getMessage(e));
                    PublishingUDDITaskFactory.handleUddiDeleteFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                    return;
                } finally {
                    ResourceUtils.closeQuietly( uddiClient );
                }

                factory.uddiProxiedServiceInfoManager.delete(uddiProxiedServiceInfo.getOid());//cascade delete
                if (serviceWasOverwritten) {
                    //delete the service control
                    factory.uddiServiceControlManager.delete(serviceControl);
                }
                logger.log(Level.FINE, "Proxied BusinessService successfully deleted from UDDI");

            } catch (ObjectModelException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, e, "Database error when deleting proxy services.");
                throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
            } catch (RuntimeException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_UNEXPECTED_ERROR, e, ExceptionUtils.getMessage(e));
                if(uddiPublishStatus != null){
                    throwHandledTaskException(context, e, uddiPublishStatus.getOid(), factory, false);
                }
            }
        }
    }

    //-PRIVATE STATIC

    /**
     * If the UDDI publish has failed more than the allowable configured amount, it is set to the failedStatus,
     * othewise its status is set to the retry status
     *
     * @param retryOkStatus UDDIPublishStatus.PublishStatus the status to set on the UDDIPublishStatus if the publish
     * task can be retried
     * @param failedStatus UDDIPublishStatus.PublishStatus the status to set on the UDDIPublishStatus if the publish
     * task cannot be retried
     * @param uddiPublishStatusOid long oid of the UDDIPublishStatus to update.
     * @param taskContext UDDITaskContext to provide access to the max retry attempts
     * @param statusManager UDDIPublishStatusManager statusManager provide to look up the most up to date value for the
     * UDDIPublishStatus
     * @throws UpdateException any problems updating the db
     * @throws com.l7tech.objectmodel.FindException any problems searching the db
     */
    private static void handleUddiUpdateFailure(final long uddiPublishStatusOid,
                                                final UDDITaskContext taskContext,
                                                final UDDIPublishStatusManager statusManager,
                                                final UDDIPublishStatus.PublishStatus retryOkStatus,
                                                final UDDIPublishStatus.PublishStatus failedStatus)
            throws UpdateException, FindException {
        final UDDIPublishStatus uddiPublishStatus = statusManager.findByPrimaryKey(uddiPublishStatusOid);
        final int maxFailures = taskContext.getMaxRetryAttempts();
        final int numPreviousRetries = uddiPublishStatus.getFailCount();
        if (numPreviousRetries >= maxFailures) {
            uddiPublishStatus.setPublishStatus(failedStatus);
        } else {
            final int failCount = uddiPublishStatus.getFailCount() + 1;
            uddiPublishStatus.setFailCount(failCount);
            uddiPublishStatus.setPublishStatus(retryOkStatus);
        }
        statusManager.update(uddiPublishStatus);
    }
    
    private static void handleUddiPublishFailure(final long uddiPublishStatusOid,
                                                 final UDDITaskContext taskContext,
                                                 final UDDIPublishStatusManager uddiPublishStatusManager)
            throws UpdateException, FindException {
        handleUddiUpdateFailure(uddiPublishStatusOid, taskContext, uddiPublishStatusManager,
                UDDIPublishStatus.PublishStatus.PUBLISH_FAILED, UDDIPublishStatus.PublishStatus.CANNOT_PUBLISH);
    }

    private static void handleUddiDeleteFailure(final long uddiPublishStatusOid,
                                                final UDDITaskContext taskContext,
                                                final UDDIPublishStatusManager uddiPublishStatusManager) throws UpdateException, FindException {
        handleUddiUpdateFailure(uddiPublishStatusOid, taskContext, uddiPublishStatusManager,
                UDDIPublishStatus.PublishStatus.DELETE_FAILED, UDDIPublishStatus.PublishStatus.CANNOT_DELETE);
    }

    /**
     * Handle an unexpected error. Update the status of the publish / delete task so it gets retried again and avoids any 
     * task which causes a runtime exception from staying in the 'PUBLISH' status. We need users to see the problem in the SSM
     * @param context UDDITaskContext from the UDDITask
     * @param e Exception The unexpected runtime exception
     * @param publishStatusOid long oid of PublishStatus the status for the UDDIProxiedServiceInfo
     * @param factory PublishingUDDITaskFactory providing access to all required Managers
     * @param isPublishing boolean if true, then the UDDIPublishStatus will following lifecycle for PUBLISH->PUBLISH_FAILED->CANNOT_PUBLISH,
     * if false, then the UDDIPublishStatus will following lifecycle for DELETE->DELETE_FAILED->CANNOT_DELETE, 
     * @throws UDDIHandledTaskException
     */
    private static void throwHandledTaskException(final UDDITaskContext context,
                                                  final Exception e,
                                                  final long publishStatusOid,
                                                  final PublishingUDDITaskFactory factory,
                                                  final boolean isPublishing) throws UDDIHandledTaskException {
        throw new UDDIHandledTaskException(ExceptionUtils.getMessage(e), e) {
            @Override
            public void handleTaskError() {
                try {
                    if(isPublishing) handleUddiPublishFailure(publishStatusOid, context, factory.uddiPublishStatusManager);
                    else handleUddiDeleteFailure(publishStatusOid, context, factory.uddiPublishStatusManager);
                } catch (UpdateException e1) {
                    if(isPublishing) context.logAndAudit(SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e1, "Database error when updating database with publish fail stats.");
                    else context.logAndAudit(SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e1, "Database error when updating database with delete fail stats.");
                } catch (FindException e1) {
                    if(isPublishing) context.logAndAudit(SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e1, "Database error when updating database with publish fail stats.");
                    else context.logAndAudit(SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e1, "Database error when updating database with delete fail stats.");
                }
            }
        };
    }
}
