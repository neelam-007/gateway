package com.l7tech.server.uddi;

import com.l7tech.uddi.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.SyspropUtil;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;

import javax.wsdl.WSDLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * UDDITaskFactory for subscription tasks
 */
public class SubscriptionUDDITaskFactory extends UDDITaskFactory {

    //- PUBLIC

    public SubscriptionUDDITaskFactory(final UDDIRegistryManager uddiRegistryManager,
                                       final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                       final UDDIServiceControlManager uddiServiceControlManager,
                                       final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                                       final ServiceCache serviceCache,
                                       final UDDIHelper uddiHelper) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiRegistrySubscriptionManager = uddiRegistrySubscriptionManager;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.serviceCache = serviceCache;
        this.uddiHelper = uddiHelper;
    }

    @Override
    public UDDITask buildUDDITask( final UDDIEvent event ) {
        UDDITask task = null;

        if ( event instanceof TimerUDDIEvent ) {
            TimerUDDIEvent timerEvent = (TimerUDDIEvent) event;
            if ( timerEvent.getType() == TimerUDDIEvent.Type.SUBSCRIPTION_POLL ) {
                task = new SubscriptionPollUDDITask(
                        uddiRegistryManager,
                        uddiRegistrySubscriptionManager,
                        uddiServiceControlManager,
                        timerEvent.getRegistryOid() );
            }
        } else if ( event instanceof SubscribeUDDIEvent ) {
            SubscribeUDDIEvent subscribeEvent = (SubscribeUDDIEvent) event;
            switch ( subscribeEvent.getType() ) {
                case SUBSCRIBE:
                    task = new SubscribeUDDITask(
                            uddiRegistryManager,
                            uddiRegistrySubscriptionManager,
                            subscribeEvent.getRegistryOid());
                    break;
                case UNSUBSCRIBE:
                    task = new UnsubscribeUDDITask(
                            uddiRegistryManager,
                            uddiRegistrySubscriptionManager,
                            subscribeEvent.getRegistryOid() );
                    break;
            }
        } else if ( event instanceof NotificationUDDIEvent ) {
            NotificationUDDIEvent notificationEvent = (NotificationUDDIEvent) event;
            task = new SubscriptionNotificationUDDITask(
                    uddiRegistrySubscriptionManager,
                    uddiServiceControlManager,                                                          
                    notificationEvent.getMessage() );
        } else if( event instanceof PublishUDDIEvent){
            PublishUDDIEvent publishUDDIEvent = (PublishUDDIEvent) event;
            switch ( publishUDDIEvent.getType()){
                case CREATE_PROXY:
                    task = new PublishUDDITask(publishUDDIEvent.getUddiProxiedServiceInfo(), 
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

    private static final long SUBSCRIPTION_EXPIRY_INTERVAL = SyspropUtil.getLong( "com.l7tech.server.uddi.subscriptionExpiryInterval", TimeUnit.DAYS.toMillis( 1 ) );
    private static final long SUBSCRIPTION_RENEW_THRESHOLD = SyspropUtil.getLong( "com.l7tech.server.uddi.subscriptionRenewThreshold", TimeUnit.HOURS.toMillis( 12 ) );

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
    private final UDDIServiceControlManager uddiServiceControlManager;
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
        @Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
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
                    logger.log(Level.WARNING, "Could not update UDDIProxiedServiceInfo", e);
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

    private static final class SubscribeUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( SubscribeUDDITask.class.getName() );

        private final UDDIRegistryManager uddiRegistryManager;
        private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
        private final long registryOid;

        public SubscribeUDDITask( final UDDIRegistryManager uddiRegistryManager,
                                  final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                  final long registryOid ) {
            this.uddiRegistryManager = uddiRegistryManager;
            this.uddiRegistrySubscriptionManager = uddiRegistrySubscriptionManager;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) throws UDDIException {
            // TODO [steve] Allow renew of subscription rather than delete and replace?
            logger.info( "Subscribing to UDDI for registry "+registryOid+"." );
            try {
                UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    UDDIClient uddiClient = UDDIHelper.newUDDIClient( uddiRegistry );

                    UDDIRegistrySubscription uddiRegistrySubscription = uddiRegistrySubscriptionManager.findByUDDIRegistryOid( registryOid );
                    if ( uddiRegistrySubscription != null && uddiRegistrySubscription.getSubscriptionKey()!=null ) {
                        try {
                            uddiClient.deleteSubscription( uddiRegistrySubscription.getSubscriptionKey() );
                            uddiRegistrySubscription.setSubscriptionKey( null );
                        } catch ( UDDIException ue ) {
                            logger.log( Level.WARNING, "Unable to delete subscription '"+uddiRegistrySubscription.getSubscriptionKey()+"'.", ue );
                        }
                    }

                    String bindingKey = null;
                    long monitoringInterval = 0;
                    if ( uddiRegistry.isSubscribeForNotifications() ) {
                        bindingKey = context.getSubscriptionBindingKey( registryOid );
                        if ( bindingKey == null ) {
                            throw new UDDIException("Error subscribing for notifications, UDDI notification service missing or unpublished.");
                        }
                        monitoringInterval = uddiRegistry.getMonitoringFrequency();
                    }

                    final long expiryTime = System.currentTimeMillis() + SUBSCRIPTION_EXPIRY_INTERVAL;
                    final String subscriptionKey = uddiClient.subscribe(
                            expiryTime,
                            monitoringInterval ,
                            bindingKey );

                    if ( uddiRegistrySubscription == null ) {
                        uddiRegistrySubscription = new UDDIRegistrySubscription();
                        uddiRegistrySubscription.setUddiRegistryOid( registryOid );
                    }

                    uddiRegistrySubscription.setSubscriptionKey( subscriptionKey );
                    uddiRegistrySubscription.setSubscriptionExpiryTime( expiryTime );
                    // when polling set the last check time to the subscription start time
                    uddiRegistrySubscription.setSubscriptionCheckTime( bindingKey == null ? System.currentTimeMillis() : 0 );
                    uddiRegistrySubscription.setSubscriptionNotifiedTime( 0 );

                    try {
                        if ( uddiRegistrySubscription.getOid()==UDDIRegistrySubscription.DEFAULT_OID ) {
                            uddiRegistrySubscriptionManager.save( uddiRegistrySubscription );
                        } else {
                            uddiRegistrySubscriptionManager.update( uddiRegistrySubscription );
                        }
                    } catch ( Exception e ) {
                        logger.log( Level.WARNING, "Error persisting uddi subscription for registry "+uddiRegistry.getName()+" (#"+uddiRegistry.getOid()+").", e );
                        try {
                            uddiClient.deleteSubscription( subscriptionKey );
                        } catch ( UDDIException ue ) {
                            logger.log( Level.WARNING, "Unable to delete subscription '"+subscriptionKey+"'.", ue );
                        }
                        throw new UDDIException( "Error persisting subscription." );
                    }
                } else if ( uddiRegistry == null ) {
                    logger.log( Level.WARNING, "UDDIRegistry (#"+registryOid+") not found for subscription." );
                } else {
                    throw new UDDIException("UDDI Registry '"+uddiRegistry.getName()+"' is disabled.");
                }
            } catch (FindException e) {
                logger.log( Level.WARNING, "Error accessing UDDIRegistry", e );
            }
        }
    }

    private static final class UnsubscribeUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( UnsubscribeUDDITask.class.getName() );

        private final UDDIRegistryManager uddiRegistryManager;
        private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
        private final long registryOid;

        public UnsubscribeUDDITask( final UDDIRegistryManager uddiRegistryManager,
                                    final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                    final long registryOid ) {
            this.uddiRegistryManager = uddiRegistryManager;
            this.uddiRegistrySubscriptionManager = uddiRegistrySubscriptionManager;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) throws UDDIException {
            logger.info( "Unsubscribing from UDDI for registry "+registryOid+"." );
            try {
                UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    UDDIClient uddiClient = UDDIHelper.newUDDIClient( uddiRegistry );

                    UDDIRegistrySubscription uddiRegistrySubscription = uddiRegistrySubscriptionManager.findByUDDIRegistryOid( registryOid );
                    if ( uddiRegistrySubscription != null ) {
                        final String subscriptionKey = uddiRegistrySubscription.getSubscriptionKey();
                        if ( subscriptionKey != null ) {
                            uddiClient.deleteSubscription( subscriptionKey );
                        } else {
                            logger.log( Level.WARNING, "Missing subscription key for registry "+registryOid+", unsubscription not performed." );
                        }

                        try {
                            uddiRegistrySubscriptionManager.delete( uddiRegistrySubscription );
                        } catch (DeleteException e) {
                            logger.log( Level.WARNING, "Error deleting UDDI registry subscription.", e );
                        }
                    } else {
                        logger.log( Level.WARNING, "Cannot find subscription information for registry "+registryOid+", unsubscription not performed." );
                    }
                } else if ( uddiRegistry == null ) {
                    logger.log( Level.WARNING, "UDDIRegistry (#"+registryOid+") not found for unsubscription." );
                } else {
                    throw new UDDIException("UDDI Registry '"+uddiRegistry.getName()+"' is disabled.");
                }
            } catch (FindException e) {
                logger.log( Level.WARNING, "Error accessing UDDIRegistry", e );
            }
        }
    }

    private static abstract class SubscriptionProcessingUDDITask extends UDDITask {
        private final Logger logger;
        private final UDDIServiceControlManager uddiServiceControlManager;

        SubscriptionProcessingUDDITask( final Logger logger,
                                        final UDDIServiceControlManager uddiServiceControlManager ) {
            this.logger = logger;
            this.uddiServiceControlManager = uddiServiceControlManager;
        }

        void processSubscriptionResults( final UDDITaskContext context,
                                         final UDDIRegistrySubscription uddiRegistrySubscription,
                                         final UDDISubscriptionResults results ) throws FindException, UDDIException {
            final long registryOid = uddiRegistrySubscription.getUddiRegistryOid();
            for ( UDDISubscriptionResults.Result result : results.getResults() ) {
                Collection<UDDIServiceControl> controls = uddiServiceControlManager.findByUDDIRegistryAndServiceKey(
                        registryOid,
                        result.getEntityKey(),
                        true );

                if ( !controls.isEmpty() ) {
                    // Fire event for update from UDDI
                    context.notifyEvent( new BusinessServiceUpdateUDDIEvent(
                            registryOid,
                            result.getEntityKey(),
                            result.isDeleted() ) );
                }
            }

            if ( (uddiRegistrySubscription.getSubscriptionExpiryTime() - System.currentTimeMillis()) <
                 SUBSCRIPTION_RENEW_THRESHOLD ) {
                logger.info( "Notifying subscribe event for UDDI registry (#"+registryOid+")." );
                context.notifyEvent( new SubscribeUDDIEvent( registryOid, SubscribeUDDIEvent.Type.SUBSCRIBE ) );
            }
        }
    }

    private static final class SubscriptionPollUDDITask extends SubscriptionProcessingUDDITask {
        private static final Logger logger = Logger.getLogger( SubscriptionPollUDDITask.class.getName() );

        private final UDDIRegistryManager uddiRegistryManager;
        private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
        private final long registryOid;

        SubscriptionPollUDDITask( final UDDIRegistryManager uddiRegistryManager,
                                  final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                  final UDDIServiceControlManager uddiServiceControlManager,
                                  final long registryOid ) {
            super(logger,uddiServiceControlManager);
            this.uddiRegistryManager = uddiRegistryManager;
            this.uddiRegistrySubscriptionManager = uddiRegistrySubscriptionManager;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) throws UDDIException {
            logger.fine( "Polling UDDI registry subscriptions." );
            try {
                UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    UDDIClient uddiClient = UDDIHelper.newUDDIClient( uddiRegistry );

                    UDDIRegistrySubscription uddiRegistrySubscription = uddiRegistrySubscriptionManager.findByUDDIRegistryOid( registryOid );
                    if ( uddiRegistrySubscription != null ) {
                        final String subscriptionKey = uddiRegistrySubscription.getSubscriptionKey();
                        final long lastCheckTime = uddiRegistrySubscription.getSubscriptionCheckTime();
                        final long newCheckTime = System.currentTimeMillis();

                        UDDISubscriptionResults results = uddiClient.pollSubscription( lastCheckTime, newCheckTime, subscriptionKey );
                        processSubscriptionResults( context, uddiRegistrySubscription, results );

                        uddiRegistrySubscription.setSubscriptionCheckTime( newCheckTime );
                        uddiRegistrySubscriptionManager.update( uddiRegistrySubscription );
                    } else {
                        logger.log( Level.WARNING, "Cannot find subscription information for registry "+uddiRegistry.getName()+", subscription poll not performed." );                        
                    }
                } else if (uddiRegistry == null) {
                    logger.log( Level.WARNING, "UDDIRegistry (#"+registryOid+") not found for subscription poll." );
                }
            } catch (FindException e) {
                logger.log( Level.WARNING, "Error accessing UDDIRegistry", e );
            } catch (UpdateException e) {
                logger.log( Level.WARNING, "Error updating subscription for registry "+registryOid+".", e );
            }
        }
    }

    private static final class SubscriptionNotificationUDDITask extends SubscriptionProcessingUDDITask {
        private static final Logger logger = Logger.getLogger( SubscriptionNotificationUDDITask.class.getName() );

        private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
        private final UDDIServiceControlManager uddiServiceControlManager;
        private final String message;

        public SubscriptionNotificationUDDITask( final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                                 final UDDIServiceControlManager uddiServiceControlManager,
                                                 final String message ) {
            super(logger,uddiServiceControlManager);
            this.uddiRegistrySubscriptionManager = uddiRegistrySubscriptionManager;
            this.uddiServiceControlManager = uddiServiceControlManager;
            this.message = message;
        }

        @Override
        public void apply( final UDDITaskContext context ) throws UDDIException {
            logger.fine( "Processing UDDI subscription notification." );

            final UDDISubscriptionResults results = UDDISubscriptionResultFactory.buildResults( message );
            final String subscriptionKey = results.getSubscriptionKey();
            try {
                Collection<UDDIRegistrySubscription> subscriptions = uddiRegistrySubscriptionManager.findBySubscriptionKey( subscriptionKey );

                UDDIRegistrySubscription subscription = null;
                if ( subscriptions.size() > 1 ) {
                    // Then we have to look at the service keys to find which registry this is for.
                    outer:
                    for ( UDDISubscriptionResults.Result result : results.getResults() ) {
                        Collection<UDDIServiceControl> controls = uddiServiceControlManager.findByUDDIServiceKey( result.getEntityKey() );
                        for ( UDDIServiceControl control : controls ) {
                            for ( UDDIRegistrySubscription sub : subscriptions ) {
                                if ( sub.getUddiRegistryOid() == control.getUddiRegistryOid() ) {
                                    subscription = sub;
                                    break outer;
                                }
                            }
                        }
                    }
                } else if ( subscriptions.size()==1 ) {
                    subscription = subscriptions.iterator().next();
                } else {
                    logger.warning( "Ignoring unrecognised subscription key '"+subscriptionKey+"'." );
                }

                if ( subscription != null ) {
                    processSubscriptionResults( context, subscription, results );
                    subscription.setSubscriptionNotifiedTime( results.getEndTime() );
                    uddiRegistrySubscriptionManager.update( subscription );
                }
            } catch (FindException e) {
                logger.log( Level.WARNING, "Error finding subscription for "+subscriptionKey+", subscription notification ignored.", e );                        
            } catch (UpdateException e) {
                logger.log( Level.WARNING, "Error updating subscription for "+subscriptionKey+".", e );
            }
        }
    }
}
