package com.l7tech.server.uddi;

import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.uddi.*;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ConfigFactory;
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
                        final PublishedService service = serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceGoid());
                        task = new PublishGatewayWsdlUddiTask(this, service);
                    } else if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE ||
                            publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                        task = new DeletePublishUDDITask(this, publishUDDIEvent.getUddiProxiedServiceInfo().getOid());
                    }

                    break;
                case ENDPOINT:
                    //what type of endpoint is it?
                    final Boolean isGifPublish = uddiProxiedServiceInfo.getProperty("IS_GIF");
                    if(isGifPublish != null && isGifPublish){
                        if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH ||
                                publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED) {
                            task = new PublishUDDIGifEndpointTask(this, publishUDDIEvent.getUddiProxiedServiceInfo().getOid());
                        } else if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE ||
                                publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                            task = new DeleteUDDIGifEndpointTask(this, publishUDDIEvent.getUddiProxiedServiceInfo().getOid());
                        }
                    } else {
                        if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH ||
                                publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED) {
                            task = new PublishUDDIEndpointTask(this, publishUDDIEvent.getUddiProxiedServiceInfo().getOid());
                        } else if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE ||
                                publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED) {
                            task = new DeleteUDDIEndpointTask(this, publishUDDIEvent.getUddiProxiedServiceInfo().getOid());
                        }
                    }

                    break;
                case OVERWRITE:
                    final PublishedService ps = serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceGoid());
                    final UDDIServiceControl serviceControl = publishUDDIEvent.getServiceControl();
                    if (serviceControl == null){
                        try {
                            final UDDIPublishStatus status = uddiPublishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                            status.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                            uddiPublishStatusManager.update(status);
                        } catch (ObjectModelException e) {
                            logger.log(Level.WARNING, "Could not update status for UDDIProxiedServiceInfo id#(" + uddiProxiedServiceInfo.getOid()+").", e);
                        }
                        return null;
                    }

                    if (!serviceControl.isHasBeenOverwritten()) {
                        task = new OverwriteServicePublishUDDITask(this, ps);
                    } else {
                        if (publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH ||
                                publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED) {
                            final PublishedService service = serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceGoid());
                            task = new PublishGatewayWsdlUddiTask(this, service);
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
         * This task manages both the first publish to UDDI and any subsequent updates to the binding template
         *
         * @param context The context for the task
         */
        @Override
        public void apply(final UDDITaskContext context) throws UDDITaskException {
            UDDIPublishStatus uddiPublishStatus = null;
            try {
                //only work with the most up to date values from the database
                UDDIProxiedServiceInfo uddiProxiedServiceInfo = factory.uddiProxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
                if (uddiProxiedServiceInfo == null) return;

                uddiPublishStatus =
                        factory.uddiPublishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                if (uddiPublishStatus == null) return;

                //Only work with UDDIPublishStatus in a publish state
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
                    logger.log(Level.WARNING, "Cannot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

                final PublishedService publishedService =
                        factory.serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceGoid());

                UDDIServiceControl serviceControl =
                        factory.uddiServiceControlManager.findByPublishedServiceGoid(uddiProxiedServiceInfo.getPublishedServiceGoid());
                if (serviceControl == null)
                    throw new IllegalStateException("No UDDIServiceControl found for PublishedService with id #(" + uddiProxiedServiceInfo.getPublishedServiceGoid() + ")");

                if (uddiProxiedServiceInfo.isRemoveOtherBindings() && serviceControl.isUnderUddiControl()) {
                    throw new IllegalStateException("Cannot remove other bindings when the WSDL is under UDDI control");
                }

                logger.log(Level.INFO, "Publishing endpoint from Published Service id #(" + publishedService.getGoid() + ") to UDDI registry id #(" + uddiRegistry.getOid() + ")");

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service " + publishedService.getName() + "(#" + publishedService.getGoid() + "). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));

                    uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                    factory.uddiPublishStatusManager.update(uddiPublishStatus);
                    return;
                }

                final Set<EndpointPair> allEndpointPairs =
                        factory.uddiHelper.getAllExternalEndpointAndWsdlUrls(publishedService.getGoid());

                //this will be null on first publish
                final Set<EndpointPair> persistedEndpoints = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.ALL_ENDPOINT_PAIRS_KEY);
                //this will be null on first publish
                final Set<String> publishedBindingKeys = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.ALL_BINDING_TEMPLATE_KEYS);

                final Set<UDDIKeyedReference> configKeyedReferences = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_CONFIG);
                final Set<UDDIKeyedReference> runtimeKeyedReferences = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_RUNTIME);

                final Set<String> justPublishedBindingKeys;

                BusinessServicePublisher businessServicePublisher = null;
                try {
                    businessServicePublisher = new BusinessServicePublisher(
                        wsdl,
                        publishedService.getGoid().toHexString(),
                        factory.uddiHelper.newUDDIClientConfig(uddiRegistry));

                    //provides best effort commit / rollback for all UDDI interactions
                    justPublishedBindingKeys = businessServicePublisher.publishBindingTemplate(
                            serviceControl.getUddiServiceKey(),
                            serviceControl.getWsdlPortName(),
                            serviceControl.getWsdlPortBinding(),
                            serviceControl.getWsdlPortBindingNamespace(),
                            persistedEndpoints,
                            allEndpointPairs,
                            publishedBindingKeys,
                            uddiProxiedServiceInfo.isRemoveOtherBindings(),
                            configKeyedReferences,
                            runtimeKeyedReferences);

                } catch (UDDIException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e, ExceptionUtils.getMessage(e));
                    return;
                } finally {
                    ResourceUtils.closeQuietly( businessServicePublisher );
                }

                uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                factory.uddiPublishStatusManager.update(uddiPublishStatus);

                //reread in case it was modified via UI during the time taken to publish
                uddiProxiedServiceInfo = factory.uddiProxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);

                if(uddiProxiedServiceInfo != null){
                    //persist the endpoints published so we can detect when they change and a republish is required
                    uddiProxiedServiceInfo.setProperty(UDDIProxiedServiceInfo.ALL_ENDPOINT_PAIRS_KEY, allEndpointPairs);
                    if(publishedBindingKeys == null || !publishedBindingKeys.equals(justPublishedBindingKeys)){
                        uddiProxiedServiceInfo.setProperty(UDDIProxiedServiceInfo.ALL_BINDING_TEMPLATE_KEYS, justPublishedBindingKeys);
                    }

                    //ok if configKeyedReferences is null - property is removed.
                    uddiProxiedServiceInfo.setProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_RUNTIME, configKeyedReferences);

                    factory.uddiProxiedServiceInfoManager.update(uddiProxiedServiceInfo);

                    if (uddiProxiedServiceInfo.isRemoveOtherBindings()) {
                        //reread the UDDIServiceControl as it may have been updated via the UI during the time it takes to publish
                        serviceControl = factory.uddiServiceControlManager.findByPublishedServiceGoid(uddiProxiedServiceInfo.getPublishedServiceGoid());
                        if (serviceControl != null && !serviceControl.isHasHadEndpointRemoved()) {
                            if (serviceControl.isUnderUddiControl()) {
                                serviceControl.setUnderUddiControl(false);//if this is actually done a coding error happened
                                logger.log(Level.WARNING, "Set UDDIServiceControl isUnderUDDIControl to be false");
                            }
                            serviceControl.setHasHadEndpointRemoved(true);
                            factory.uddiServiceControlManager.update(serviceControl);
                        }
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

    private static final class PublishUDDIGifEndpointTask extends UDDITask {
        private static final Logger logger = Logger.getLogger(PublishUDDIEndpointTask.class.getName());

        private final PublishingUDDITaskFactory factory;
        private final long uddiProxiedServiceInfoOid;

        private PublishUDDIGifEndpointTask(final PublishingUDDITaskFactory factory,
                                        final long uddiProxiedServiceInfoOid) {
            this.factory = factory;
            this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
        }

        /**
         * This task manages both the first publish to UDDI and any subsequent updates to the GIF binding template.
         *
         * @param context The context for the task
         */
        @Override
        public void apply(final UDDITaskContext context) throws UDDITaskException {
            UDDIPublishStatus uddiPublishStatus = null;
            try {
                //work with the most up to date values from the database
                UDDIProxiedServiceInfo uddiProxiedServiceInfo = factory.uddiProxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);
                if (uddiProxiedServiceInfo == null) return;

                uddiPublishStatus =
                        factory.uddiPublishStatusManager.findByProxiedSerivceInfoOid(uddiProxiedServiceInfo.getOid());
                if (uddiPublishStatus == null) return;

                //Only work with UDDIPublishStatus in a publish state
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
                    logger.log(Level.WARNING, "Cannot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

                final PublishedService publishedService =
                        factory.serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceGoid());

                final UDDIServiceControl serviceControl =
                        factory.uddiServiceControlManager.findByPublishedServiceGoid(uddiProxiedServiceInfo.getPublishedServiceGoid());
                if (serviceControl == null)
                    throw new IllegalStateException("No UDDIServiceControl found for PublishedService with id #(" + uddiProxiedServiceInfo.getPublishedServiceGoid() + ")");

                if (serviceControl.isUnderUddiControl()) {
                    throw new IllegalStateException("Cannot publish GIF endpoint when service is under UDDI control.");
                }

                logger.log(Level.INFO, "Publishing endpoint from Published Service id #(" + publishedService.getGoid() + ") to UDDI registry id #(" + uddiRegistry.getOid() + ")");

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service " + publishedService.getName() + "(#" + publishedService.getGoid() + "). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));

                    uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                    factory.uddiPublishStatusManager.update(uddiPublishStatus);
                    return;
                }

                final UDDIRegistryAdmin.EndpointScheme endpointScheme = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.GIF_SCHEME);
                if(endpointScheme == null) {
                    throw new IllegalStateException("Property '" + UDDIProxiedServiceInfo.GIF_SCHEME + 
                            "' not found in UDDIProxiedServiceInfo #(" + uddiProxiedServiceInfo.getId() + ")");
                }

                final EndpointPair endPointPair;
                try {
                    endPointPair = factory.uddiHelper.getEndpointForScheme(endpointScheme, publishedService.getGoid());
                } catch (UDDIHelper.EndpointNotDefinedException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, ExceptionUtils.getDebugException(e), ExceptionUtils.getMessage(e));
                    return;
                }

                //this will be null on first publish
                final Set<EndpointPair> persistedEndpoints = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.ALL_ENDPOINT_PAIRS_KEY);
                //invariants - persistedEndpoints has 1 element. publishedBindingKeys has 1 element.
                if(persistedEndpoints != null && persistedEndpoints.size() != 1) throw new IllegalStateException("A GIF endpoint should only have a single endpoint pair");
                //this will be null on first publish
                final Set<String> publishedBindingKeys = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.ALL_BINDING_TEMPLATE_KEYS);
                if(publishedBindingKeys != null && publishedBindingKeys.size() != 1) throw new IllegalStateException("A GIF endpoint should only have a single bindingTemplate key");

                final String publishedBindingKey = (publishedBindingKeys != null) ? publishedBindingKeys.iterator().next() : null;
                final String functionalEndpointKey = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.FUNCTIONAL_ENDPOINT_KEY);

                final String functionalBindingKey;
                final String proxyBindingKey;
                BusinessServicePublisher businessServicePublisher = null;
                final Set<UDDIKeyedReference> configKeyedReferences = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_CONFIG);
                try {
                    businessServicePublisher = new BusinessServicePublisher(
                        wsdl,
                        publishedService.getGoid().toHexString(),
                        factory.uddiHelper.newUDDIClientConfig(uddiRegistry));

                    // what is the name of the WSMS for the required reference C.2?
                    final String keyValue = ConfigFactory.getProperty( "uddi.systinet.gif.management.system", null );

                    final Set<UDDIKeyedReference> runtimeKeyedReferences = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_RUNTIME);
                    //provides best effort commit / rollback for all UDDI interactions
                    final Pair<String, String> proxyFuncPair = businessServicePublisher.publishBindingTemplateGif(
                            serviceControl.getUddiServiceKey(),
                            serviceControl.getWsdlPortName(),
                            serviceControl.getWsdlPortBinding(),
                            serviceControl.getWsdlPortBindingNamespace(),
                            endPointPair,
                            publishedBindingKey,
                            functionalEndpointKey,
                            keyValue,
                            configKeyedReferences,
                            runtimeKeyedReferences);
                    proxyBindingKey = proxyFuncPair.left;
                    functionalBindingKey = proxyFuncPair.right;

                } catch (UDDIException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_ENDPOINT_FAILED, e, ExceptionUtils.getMessage(e));
                    return;
                } finally {
                    ResourceUtils.closeQuietly( businessServicePublisher );
                }

                uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                factory.uddiPublishStatusManager.update(uddiPublishStatus);

                //reread in case it was modified via UI during the time taken to publish
                uddiProxiedServiceInfo = factory.uddiProxiedServiceInfoManager.findByPrimaryKey(uddiProxiedServiceInfoOid);

                if(uddiProxiedServiceInfo != null){
                    //persist the endpoints published so we can detect when they change and a republish is required
                    uddiProxiedServiceInfo.setProperty(UDDIProxiedServiceInfo.ALL_ENDPOINT_PAIRS_KEY, new HashSet<EndpointPair>(Arrays.asList(endPointPair)));
                    if(publishedBindingKeys == null || !publishedBindingKeys.iterator().next().equals(proxyBindingKey)){
                        uddiProxiedServiceInfo.setProperty(UDDIProxiedServiceInfo.ALL_BINDING_TEMPLATE_KEYS, new HashSet<String>(Arrays.asList(proxyBindingKey)));
                    }
                    //save the functional endpoint
                    if(functionalEndpointKey == null){
                        uddiProxiedServiceInfo.setProperty(UDDIProxiedServiceInfo.FUNCTIONAL_ENDPOINT_KEY, functionalBindingKey);
                    }

                    //ok if configKeyedReferences is null - property is removed.
                    uddiProxiedServiceInfo.setProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_RUNTIME, configKeyedReferences);
                    
                    factory.uddiProxiedServiceInfoManager.update(uddiProxiedServiceInfo);
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
     * Publish the gateway WSDL to UDDI for the first time or update information already published
     */
    private static final class PublishGatewayWsdlUddiTask extends UDDITask {
        private static final Logger logger = Logger.getLogger(PublishGatewayWsdlUddiTask.class.getName());

        private final PublishingUDDITaskFactory factory;
        private final PublishedService publishedService;

        public PublishGatewayWsdlUddiTask(final PublishingUDDITaskFactory factory,
                                     final PublishedService publishedService) {
            this.factory = factory;
            this.publishedService = publishedService;
        }

        @Override
        public void apply(final UDDITaskContext context) throws UDDITaskException {
            UDDIPublishStatus uddiPublishStatus = null;
            try {
                //Get the most up to date version of the UDDIProxiedServiceInfo - the queue can be stale - will cause loop with eventual hibernate errors
                UDDIProxiedServiceInfo uddiProxiedServiceInfo =
                        factory.uddiProxiedServiceInfoManager.findByPublishedServiceGoid(publishedService.getGoid());
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
                        factory.uddiServiceControlManager.findByPublishedServiceGoid(publishedService.getGoid());
                if (serviceControl == null && uddiProxiedServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.OVERWRITE) {
                    throw new IllegalStateException("When a service has been overwritten, we must have a record of its UDDI information");
                }

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry =
                        factory.uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                if (uddiRegistry == null)
                    throw new IllegalStateException("UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") not found.");
                if (!uddiRegistry.isEnabled()) {
                    logger.log(Level.WARNING, "Cannot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

                final boolean serviceWasOverwritten;

                if (uddiProxiedServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.OVERWRITE) {
                    if (serviceControl == null) {
                        //this means that our service control was deleted, it contains the information we need
                        context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, "Gateway record of which UDDI BusinessService was overwriten was not found. Deleting UDDIProxiedServiceInfo");
                        factory.uddiProxiedServiceInfoManager.delete(uddiProxiedServiceInfo.getOid());
                        return;
                    }
                    if (!serviceControl.isHasBeenOverwritten()) {
                        throw new IllegalStateException("Service is not overwritten in UDDI. Cannot update.");
                    }
                    serviceWasOverwritten = true;
                } else {
                    serviceWasOverwritten = false;
                }

                logger.log(Level.INFO,
                        "Publishing Gateway WSDL " + ((serviceWasOverwritten) ? "for overwritten service " : "") + "for Published Service id #(" + publishedService.getGoid() + ") to UDDI Registry id #(" + uddiRegistry.getOid() + ")");

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service " + publishedService.getName() + "(#" + publishedService.getGoid() + "). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));
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

                final UDDIClient uddiClient = factory.uddiHelper.newUDDIClient(uddiRegistry);
                final UDDIRegistrySpecificMetaData registrySpecificMetaData =
                        PublishingUDDITaskFactory.getRegistrySpecificMetaData(uddiRegistry, serviceControl, uddiProxiedServiceInfo, factory, uddiClient);

                final Set<EndpointPair> allEndpointPairs =
                        factory.uddiHelper.getAllExternalEndpointAndWsdlUrls(publishedService.getGoid());

                final Pair<Set<String>, Set<UDDIBusinessService>> deletedAndNewServices;
                BusinessServicePublisher businessServicePublisher = null;
                try {
                    businessServicePublisher = new BusinessServicePublisher(
                            wsdl,
                            uddiProxiedServiceInfo.getPublishedServiceGoid().toHexString(),
                            uddiClient);

                    deletedAndNewServices = businessServicePublisher.publishServicesToUDDIRegistry(
                            uddiProxiedServiceInfo.getUddiBusinessKey(),
                            serviceKeys, serviceWasOverwritten, registrySpecificMetaData, allEndpointPairs);
                } catch (UDDIException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, ExceptionUtils.getDebugException(e), ExceptionUtils.getMessage(e));
                    return;
                } finally {
                    ResourceUtils.closeQuietly( businessServicePublisher );
                }

                //reread in case it was modified via UI during the time taken to publish
                uddiProxiedServiceInfo = factory.uddiProxiedServiceInfoManager.findByPublishedServiceGoid(publishedService.getGoid());

                if(uddiProxiedServiceInfo != null){
                    //persist the endpoints published so we can detect when they change and a republish is required
                    uddiProxiedServiceInfo.setProperty(UDDIProxiedServiceInfo.ALL_ENDPOINT_PAIRS_KEY, allEndpointPairs);
                    if(uddiProxiedServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.OVERWRITE){
                        uddiProxiedServiceInfo.setProperty(UDDIProxiedServiceInfo.ALL_BINDING_TEMPLATE_KEYS,
                                businessServicePublisher.getPublishedBindingTemplates());
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

                        if(!removeSet.isEmpty()){
                            uddiProxiedServiceInfo.getProxiedServices().removeAll(removeSet);
                            context.flushSession();
                        }

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
                }
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
     * that information from the UDDIServiceControl and UDDIRegistry. If the UDDIProxiedServiceInfo is required it
     * can also be added as an argument
     *
     * For ActiveSOA, if the Gateway WSDL is being published to the same instance as the original, then the returned
     * UDDIRegistrySpecificMetaData will contain the required meta data to make the published business services virtual
     * and to have associations to the original.
     * 
     * @param uddiRegistry UDDIRegistry the uddi registry a WSDL is being published to. Required.
     * @param serviceControl UDDIServiceControl which represents the original service. Required
     * @param uddiProxiedServiceInfo UDDIProxiedServiceInfo which represents the information being published to UDDI
     * @param uddiFactory PublishingUDDITaskFactory required for access to system properties
     * @param uddiClient UDDIClient if any information is required from UDDI. Required. Will not be closed by this method
     * @return UDDIRegistrySpecificMetaData, not null if there are registry specific meta data requirements, null otherwise
     */
    private static UDDIRegistrySpecificMetaData getRegistrySpecificMetaData(final UDDIRegistry uddiRegistry,
                                                                            final UDDIServiceControl serviceControl,
                                                                            final UDDIProxiedServiceInfo uddiProxiedServiceInfo,
                                                                            final PublishingUDDITaskFactory uddiFactory,
                                                                            final UDDIClient uddiClient) {
        final boolean isActiveSOAVirtualService = serviceControl != null &&
                uddiProxiedServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.PROXY &&
                uddiRegistry.getUddiRegistryType().equals(UDDIRegistry.UDDIRegistryType.CENTRASITE_ACTIVE_SOA.toString()) &&
                serviceControl.getUddiRegistryOid() == uddiRegistry.getOid();
        //we know here whether the UDDIRegistry is Active SOA or not
        //true when both the original service and proxied service will be in the same registry

        if (isActiveSOAVirtualService) {
            return new UDDIRegistrySpecificMetaData() {
                @Override
                public Collection<UDDIKeyedReference> getBusinessServiceKeyedReferences() {
                    final Collection<UDDIKeyedReference> returnColl = new ArrayList<UDDIKeyedReference>();

                    //download the tmodel key for active soa
                    final String virtualKey = uddiFactory.config.getProperty("uddi.centrasite.activesoa.virtual.service.tmodelkey", "uddi:9de0173b-5117-11de-8cf9-da0192ff3739");

                    try {
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
                    }

                    final UDDIKeyedReference kr =
                            new UDDIKeyedReference(virtualKey, null, "Virtual service");
                    returnColl.add(kr);
                    return Collections.unmodifiableCollection(returnColl);
                }

                @Override
                public Collection<UDDIClient.UDDIKeyedReferenceGroup> getBusinessServiceKeyedReferenceGroups() {
                    final Collection<UDDIClient.UDDIKeyedReferenceGroup> returnColl = new ArrayList<UDDIClient.UDDIKeyedReferenceGroup>();
                    final UDDIKeyedReference kr =
                            new UDDIKeyedReference("uddi:uddi.org:categorization:general_keywords",
                                    "Contains", serviceControl.getUddiServiceKey());

                    final Collection<UDDIKeyedReference> allRefs = new ArrayList<UDDIKeyedReference>();
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
     * For updates the normal udpate task should be used
     */
    private static final class OverwriteServicePublishUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger(PublishGatewayWsdlUddiTask.class.getName());

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
                        factory.uddiProxiedServiceInfoManager.findByPublishedServiceGoid(publishedService.getGoid());
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
                    logger.log(Level.WARNING, "Cannot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

                final PublishedService publishedService =
                        factory.serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceGoid());

                UDDIServiceControl serviceControl =
                        factory.uddiServiceControlManager.findByPublishedServiceGoid(uddiProxiedServiceInfo.getPublishedServiceGoid());
                if (serviceControl == null)//UDDIServiceControl delete logic does not allow this
                    throw new IllegalStateException("No UDDIServiceControl found for PublishedService with id #(" + uddiProxiedServiceInfo.getPublishedServiceGoid() + ")");

                if (serviceControl.isHasBeenOverwritten()) {
                    logger.log(Level.FINER, "UDDIProxiedServiceInfo has already overwritten the BusinessService in UDDI. Nothing to do");
                    return;
                }

                logger.log(Level.INFO, "Overwriting BusinessService with Gateway WSDL from Published Service id #(" + publishedService.getGoid() + ") to UDDI registry id #(" + uddiRegistry.getOid() + ")");

                final Wsdl wsdl;
                try {
                    wsdl = publishedService.parsedWsdl();
                } catch (WSDLException e) {
                    logger.log(Level.WARNING, "Unable to parse WSDL for service " + publishedService.getName() + "(#" + publishedService.getGoid() + "). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));
                    uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                    factory.uddiPublishStatusManager.update(uddiPublishStatus);
                    return;
                }

                final Set<EndpointPair> allEndpointPairs =
                        factory.uddiHelper.getAllExternalEndpointAndWsdlUrls(publishedService.getGoid());

                BusinessServicePublisher businessServicePublisher = null;
                final Set<String> publishedBindingKeys;
                try {
                    businessServicePublisher = new BusinessServicePublisher(
                        wsdl,
                        uddiProxiedServiceInfo.getPublishedServiceGoid().toHexString(),
                        factory.uddiHelper.newUDDIClientConfig(uddiRegistry));

                    publishedBindingKeys = businessServicePublisher.overwriteServiceInUDDI(serviceControl.getUddiServiceKey(),
                            serviceControl.getUddiBusinessKey(), allEndpointPairs);

                } catch (UDDIException e) {
                    PublishingUDDITaskFactory.handleUddiPublishFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                    context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, ExceptionUtils.getDebugException(e), ExceptionUtils.getMessage(e));
                    return;
                } finally {
                    ResourceUtils.closeQuietly( businessServicePublisher );
                }

                uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISHED);
                factory.uddiPublishStatusManager.update(uddiPublishStatus);
                //persist the endpoints published so we can detect when they change and a republish is required
                uddiProxiedServiceInfo.setProperty(UDDIProxiedServiceInfo.ALL_ENDPOINT_PAIRS_KEY, allEndpointPairs);
                //persist all published endpoints so we can delete by bindingKey
                uddiProxiedServiceInfo.setProperty(UDDIProxiedServiceInfo.ALL_BINDING_TEMPLATE_KEYS, publishedBindingKeys);
                
                factory.uddiProxiedServiceInfoManager.update(uddiProxiedServiceInfo);
                //reread the UDDIServiceControl as it may have been updated via the UI in the time it took to publish
                serviceControl =
                        factory.uddiServiceControlManager.findByPublishedServiceGoid(uddiProxiedServiceInfo.getPublishedServiceGoid());
                if(serviceControl != null){
                    serviceControl.setHasBeenOverwritten(true);
                    serviceControl.setUnderUddiControl(false);//can no longer be under UDDI control
                    factory.uddiServiceControlManager.update(serviceControl);
                }
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

    private static final class DeleteUDDIGifEndpointTask extends UDDITask {
        private static final Logger logger = Logger.getLogger(DeleteUDDIGifEndpointTask.class.getName());

        private final PublishingUDDITaskFactory factory;
        private final long uddiProxiedServiceInfoOid;

        private DeleteUDDIGifEndpointTask(final PublishingUDDITaskFactory factory,
                                          final long uddiProxiedServiceInfoOid) {
            this.factory = factory;
            this.uddiProxiedServiceInfoOid = uddiProxiedServiceInfoOid;
        }

        @Override
        public void apply(UDDITaskContext context) throws UDDITaskException {
            UDDIPublishStatus uddiPublishStatus = null;
            try {
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
                        factory.uddiServiceControlManager.findByPublishedServiceGoid(uddiProxiedServiceInfo.getPublishedServiceGoid());
                if (serviceControl == null)
                    throw new IllegalStateException("No UDDIServiceControl found for PublishedService with id #(" + uddiProxiedServiceInfo.getPublishedServiceGoid() + ")");

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry =
                        factory.uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                if (uddiRegistry == null)
                    throw new IllegalStateException("UDDI Registry #" + uddiProxiedServiceInfo.getUddiRegistryOid() + "' not found.");
                if (!uddiRegistry.isEnabled()) {
                    logger.log(Level.WARNING, "Cannot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

                logger.log(Level.INFO, "Deleting gateway GIF endpoint from BusinessService with serviceKey " +
                        "#(" + serviceControl.getUddiServiceKey() + ") " +
                        "for published service #(" + uddiProxiedServiceInfo.getPublishedServiceGoid() + ") " +
                        "in UDDI Registry id #(" + uddiRegistry.getOid() + ")");

                final PublishedService publishedService =
                        factory.serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceGoid());

                //Only try and delete from UDDI if information was successfully published
                BusinessServicePublisher publisher = null;
                try {
                    publisher = new BusinessServicePublisher(publishedService.getGoid().toHexString(), factory.uddiHelper.newUDDIClientConfig(uddiRegistry));
                    final Set<String> proxyBindingKeys = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.ALL_BINDING_TEMPLATE_KEYS);
                    final String functionalBindingKey = uddiProxiedServiceInfo.getProperty(UDDIProxiedServiceInfo.FUNCTIONAL_ENDPOINT_KEY);

                    publisher.deleteGatewayGifBindingTemplates(serviceControl.getUddiServiceKey(), proxyBindingKeys.iterator().next(), functionalBindingKey);
                    logger.log(Level.INFO, "GIF endpoint successfully deleted");
                } catch (UDDIException e) {
                    if (e instanceof UDDIInvalidKeyException) {
                        logger.log(Level.INFO, "BusinessService that endpoints were published to was not found #(" + serviceControl.getUddiServiceKey() + "). Cannot delete from UDDI Registry.");
                    } else if (e instanceof  UDDIUnpublishException){
                        logger.log(Level.INFO, e.getMessage() + " Cannot undo GIF publish.");
                    } else {
                        context.logAndAudit(SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_BINDING,
                                ExceptionUtils.getDebugException(e),
                                serviceControl.getUddiServiceKey(), ExceptionUtils.getMessage(e));
                        PublishingUDDITaskFactory.handleUddiDeleteFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                        return;
                    }
                } finally {
                    ResourceUtils.closeQuietly( publisher );
                }

                factory.uddiProxiedServiceInfoManager.delete(uddiProxiedServiceInfo.getOid());
            } catch (ObjectModelException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_FAILED, e, "Database error when deleting GIF proxy endpoint.");
                throw new UDDITaskException(ExceptionUtils.getMessage(e), e);//make db changes rollback
            } catch (RuntimeException e) {
                context.logAndAudit(SystemMessages.UDDI_PUBLISH_UNEXPECTED_ERROR, e, ExceptionUtils.getMessage(e));
                if(uddiPublishStatus != null){
                    throwHandledTaskException(context, e, uddiPublishStatus.getOid(), factory, false);
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
                        factory.uddiServiceControlManager.findByPublishedServiceGoid(uddiProxiedServiceInfo.getPublishedServiceGoid());
                if (serviceControl == null)
                    throw new IllegalStateException("No UDDIServiceControl found for PublishedService with id #(" +
                            uddiProxiedServiceInfo.getPublishedServiceGoid() + ")");
                
                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry =
                        factory.uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                if (uddiRegistry == null)
                    throw new IllegalStateException("UDDI Registry #" + uddiProxiedServiceInfo.getUddiRegistryOid() + "' not found.");
                if (!uddiRegistry.isEnabled()) {
                    logger.log(Level.WARNING, "Cannot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

                logger.log(Level.INFO, "Deleting gateway endpoints from BusinessService with serviceKey " +
                        "#(" + serviceControl.getUddiServiceKey() + ") " +
                        "for published service #(" + uddiProxiedServiceInfo.getPublishedServiceGoid() + ") " +
                        "in UDDI Registry id #(" + uddiRegistry.getOid() + ")");

                final PublishedService publishedService =
                        factory.serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceGoid());

                //Only try and delete from UDDI if information was successfully published
                BusinessServicePublisher publisher = null;
                try {
                    publisher = new BusinessServicePublisher(publishedService.getGoid().toHexString(), factory.uddiHelper.newUDDIClientConfig(uddiRegistry));
                    publisher.deleteGatewayBindingTemplates(serviceControl.getUddiServiceKey(),
                            uddiProxiedServiceInfo.<Set<EndpointPair>>getProperty(UDDIProxiedServiceInfo.ALL_ENDPOINT_PAIRS_KEY),
                            uddiProxiedServiceInfo.<Set<String>>getProperty(UDDIProxiedServiceInfo.ALL_BINDING_TEMPLATE_KEYS));
                    logger.log(Level.INFO, "Endpoints successfully deleted");
                } catch (UDDIException e) {
                    if (e instanceof UDDIInvalidKeyException) {
                        logger.log(Level.INFO, "BusinessService that endpoints were published to was not found #(" + serviceControl.getUddiServiceKey() + "). Cannot delete from UDDI Registry.");
                    } else {
                        context.logAndAudit(SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_BINDING,
                                ExceptionUtils.getDebugException(e),
                                serviceControl.getUddiServiceKey(), ExceptionUtils.getMessage(e));
                        PublishingUDDITaskFactory.handleUddiDeleteFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                        return;
                    }
                } finally {
                    ResourceUtils.closeQuietly( publisher );
                }

                factory.uddiProxiedServiceInfoManager.delete(uddiProxiedServiceInfo.getOid());
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
                        factory.uddiServiceControlManager.findByPublishedServiceGoid(uddiProxiedServiceInfo.getPublishedServiceGoid());
                if (serviceControl == null && uddiProxiedServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.OVERWRITE) {
                    throw new IllegalStateException("When a service has been overwritten, we must have a record of its UDDI information");
                }

                //this must be found due to db constraints
                final UDDIRegistry uddiRegistry =
                        factory.uddiRegistryManager.findByPrimaryKey(uddiProxiedServiceInfo.getUddiRegistryOid());
                if (uddiRegistry == null)
                    throw new IllegalStateException("UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") not found.");
                if (!uddiRegistry.isEnabled()) {
                    logger.log(Level.WARNING, "Cannot update UDDI. UDDI Registry #(" + uddiProxiedServiceInfo.getUddiRegistryOid() + ") is disabled");
                    return;
                }

//                final boolean serviceWasOverwritten;
                if (uddiProxiedServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.OVERWRITE) {
                    if (serviceControl == null) {
                        //this means that our service control was deleted, it contains the information we need
                        context.logAndAudit(SystemMessages.UDDI_PUBLISH_SERVICE_FAILED, "Our record of which UDDI BusinessService was overwriten is gone. Deleting UDDIProxiedServiceInfo");
                        factory.uddiProxiedServiceInfoManager.delete(uddiProxiedServiceInfo.getOid());
                        return;
                    }
                    if (!serviceControl.isHasBeenOverwritten()) {
                        throw new IllegalStateException("If we have an overwritten service, then we cannot update it unless it has been overwritten in UDDI");
                    }
//                    serviceWasOverwritten = true;
                    logger.log(Level.INFO, "Deleting overwritten BusinessService with serviceKey #(" + serviceControl.getUddiServiceKey() + ") in UDDI Registry id #(" + uddiRegistry.getOid() + ")");
                } else {
//                    serviceWasOverwritten = false;
                    logger.log(Level.INFO,
                            "Deleting published Gateway WSDL from Published Service id #(" + uddiProxiedServiceInfo.getPublishedServiceGoid() + ")s in UDDI Registry id #(" + uddiRegistry.getOid() + ")");
                }

                switch (uddiProxiedServiceInfo.getPublishType()) {
                    case PROXY:
                        UDDIClient uddiClient = null;
                        try {
                            uddiClient = factory.uddiHelper.newUDDIClient(uddiRegistry);
                            final Set<UDDIProxiedService> proxiedServices = uddiProxiedServiceInfo.getProxiedServices();
                            for (UDDIProxiedService proxiedService : proxiedServices) {
                                try {
                                    uddiClient.deleteBusinessServiceByKey(proxiedService.getUddiServiceKey());
                                } catch (UDDIException e) {
                                    if (e instanceof UDDIInvalidKeyException) {
                                        logger.log(Level.FINE, "Invalid serviceKey found #(" + proxiedService.getUddiServiceKey() + "). Cannot delete from UDDI Registry");
                                        //move onto the next
                                    } else {
                                        throw e;
                                    }
                                }
                            }
                            logger.log(Level.INFO, "Successfully deleted published Gateway WSDL from UDDI Registry");
                        } catch (UDDIException e) {
                            context.logAndAudit(SystemMessages.UDDI_REMOVE_SERVICE_FAILED, ExceptionUtils.getDebugException(e), ExceptionUtils.getMessage(e));
                            PublishingUDDITaskFactory.handleUddiDeleteFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                            return;
                        } finally {
                            ResourceUtils.closeQuietly(uddiClient);
                        }
                        break;
                    case OVERWRITE:
                        final PublishedService publishedService =
                                factory.serviceCache.getCachedService(uddiProxiedServiceInfo.getPublishedServiceGoid());
                        
                        final Wsdl wsdl;
                        try {
                            wsdl = publishedService.parsedWsdl();
                        } catch (WSDLException e) {
                            logger.log(Level.WARNING, "Unable to parse WSDL for service " + publishedService.getName() + "(#" + publishedService.getGoid() + "). Any previously published information will be delete from UDDI", ExceptionUtils.getDebugException(e));

                            uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
                            factory.uddiPublishStatusManager.update(uddiPublishStatus);
                            return;
                        }

                        BusinessServicePublisher publisher = null;
                        try {
                            publisher = new BusinessServicePublisher(wsdl, publishedService.getGoid().toHexString(), factory.uddiHelper.newUDDIClientConfig(uddiRegistry));
                            publisher.deleteGatewayBindingTemplates(serviceControl.getUddiServiceKey(),
                                    uddiProxiedServiceInfo.<Set<EndpointPair>>getProperty(UDDIProxiedServiceInfo.ALL_ENDPOINT_PAIRS_KEY),
                                    uddiProxiedServiceInfo.<Set<String>>getProperty(UDDIProxiedServiceInfo.ALL_BINDING_TEMPLATE_KEYS));
                            logger.log(Level.INFO, "Successfully deleted overwritten endpoints from UDDI Registry");
                        } catch (UDDIException e) {
                            if (e instanceof UDDIInvalidKeyException) {
                                logger.log(Level.INFO, "Overwritten serviceKey not found #(" + serviceControl.getUddiServiceKey() + "). Cannot delete from UDDI Registry.");
                            } else {
                                context.logAndAudit(SystemMessages.UDDI_PUBLISH_REMOVE_ENDPOINT_BINDING,
                                        ExceptionUtils.getDebugException(e),
                                        serviceControl.getUddiServiceKey(), ExceptionUtils.getMessage(e));
                                PublishingUDDITaskFactory.handleUddiDeleteFailure(uddiPublishStatus.getOid(), context, factory.uddiPublishStatusManager);
                                return;
                            }

                        }  finally {
                           ResourceUtils.closeQuietly( publisher );
                        }
                        break;
                    //no default, see illegal state above
                }

                factory.uddiProxiedServiceInfoManager.delete(uddiProxiedServiceInfo.getOid());//cascade delete
//                if (serviceWasOverwritten) {
//                    //delete the service control
//                    factory.uddiServiceControlManager.delete(serviceControl);
//                }
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
     * otherwise its status is set to the retry status
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
        if ( uddiPublishStatus != null ) {
            final int maxFailures = taskContext.getMaxRetryAttempts();
            final int numPreviousRetries = uddiPublishStatus.getFailCount();
            if (numPreviousRetries >= maxFailures) {
                uddiPublishStatus.setPublishStatus(failedStatus);
            } else {
                uddiPublishStatus.setPublishStatus(retryOkStatus);
            }
            final int failCount = uddiPublishStatus.getFailCount() + 1;
            uddiPublishStatus.setFailCount(failCount);

            statusManager.update(uddiPublishStatus);            
        }
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
