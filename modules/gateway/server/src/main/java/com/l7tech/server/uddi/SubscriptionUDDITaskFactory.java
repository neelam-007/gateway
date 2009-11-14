package com.l7tech.server.uddi;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.uddi.*;
import com.l7tech.server.util.HttpClientFactory;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.uddi.UDDIInvalidKeyException;
import com.l7tech.wsdl.WsdlEntityResolver;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.DocumentReferenceProcessor;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.HttpConstants;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;

/**
 * UDDITaskFactory for subscription tasks
 */
public class SubscriptionUDDITaskFactory extends UDDITaskFactory {

    //- PUBLIC

    public SubscriptionUDDITaskFactory(final UDDIRegistryManager uddiRegistryManager,
                                       final UDDIHelper uddiHelper,
                                       final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                       final UDDIServiceControlManager uddiServiceControlManager,
                                       final UDDIServiceControlMonitorRuntimeManager uddiServiceControlMonitorRuntimeManager,
                                       final ServiceManager serviceManager,
                                       final ServiceDocumentManager serviceDocumentManager,
                                       final HttpClientFactory httpClientFactory ) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiHelper = uddiHelper;
        this.uddiRegistrySubscriptionManager = uddiRegistrySubscriptionManager;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiServiceControlMonitorRuntimeManager = uddiServiceControlMonitorRuntimeManager;
        this.serviceManager = serviceManager;
        this.serviceDocumentManager = serviceDocumentManager;
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public UDDITask buildUDDITask( final UDDIEvent event ) {
        UDDITask task = null;

        if ( event instanceof TimerUDDIEvent ) {
            TimerUDDIEvent timerEvent = (TimerUDDIEvent) event;
            if ( timerEvent.getType() == TimerUDDIEvent.Type.SUBSCRIPTION_POLL ) {
                task = new SubscriptionPollUDDITask(
                        this,
                        timerEvent.getRegistryOid(),
                        -1,
                        -1 );
            }
        } else if ( event instanceof PollUDDIEvent ) {
            PollUDDIEvent pollEvent = (PollUDDIEvent) event;
            task = new SubscriptionPollUDDITask(
                    this,
                    pollEvent.getRegistryOid(),
                    pollEvent.getStartTime(),
                    pollEvent.getEndTime());
        } else if ( event instanceof SubscribeUDDIEvent ) {
            SubscribeUDDIEvent subscribeEvent = (SubscribeUDDIEvent) event;
            switch ( subscribeEvent.getType() ) {
                case SUBSCRIBE:
                    task = new SubscribeUDDITask(
                            this,
                            subscribeEvent.getRegistryOid(),
                            subscribeEvent.isExpiredOnly() );
                    break;
                case UNSUBSCRIBE:
                    task = new UnsubscribeUDDITask(
                            this,
                            subscribeEvent.getRegistryOid() );
                    break;
            }
        } else if ( event instanceof NotificationUDDIEvent ) {
            NotificationUDDIEvent notificationEvent = (NotificationUDDIEvent) event;
            task = new SubscriptionNotificationUDDITask(
                    this,
                    notificationEvent.getMessage() );
        } else if(event instanceof BusinessServiceUpdateUDDIEvent){
            BusinessServiceUpdateUDDIEvent busEvent = (BusinessServiceUpdateUDDIEvent) event;

            task = new BusinessServiceUpdateUDDITask(
                    this,
                    busEvent.getServiceKey(),
                    busEvent.getRegistryOid(),
                    busEvent.isDeleted() );
        }

        return task;
    }

    //- PRIVATE

    private static final long SUBSCRIPTION_EXPIRY_INTERVAL = SyspropUtil.getLong( "com.l7tech.server.uddi.subscriptionExpiryInterval", TimeUnit.DAYS.toMillis( 5 ) );
    private static final long SUBSCRIPTION_RENEW_THRESHOLD = SyspropUtil.getLong( "com.l7tech.server.uddi.subscriptionRenewThreshold", TimeUnit.DAYS.toMillis( 2 ) );

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIHelper uddiHelper;
    private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
    private final UDDIServiceControlManager uddiServiceControlManager;
    private final UDDIServiceControlMonitorRuntimeManager uddiServiceControlMonitorRuntimeManager;
    private final ServiceManager serviceManager;
    private final ServiceDocumentManager serviceDocumentManager;
    private final HttpClientFactory httpClientFactory;

    private static String describe( final UDDIRegistry uddiRegistry ) {
        return uddiRegistry.getName()+" (#"+uddiRegistry.getOid()+")";
    }

    private static final class SubscribeUDDITask extends SubscriptionProcessingUDDITask {
        private static final Logger logger = Logger.getLogger( SubscribeUDDITask.class.getName() );

        private final SubscriptionUDDITaskFactory factory;
        private final long registryOid;
        private final boolean expiredOnly;

        public SubscribeUDDITask( final SubscriptionUDDITaskFactory factory,
                                  final long registryOid,
                                  final boolean expiredOnly ) {
            super( logger, null );
            this.factory = factory;
            this.registryOid = registryOid;
            this.expiredOnly = expiredOnly;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            // TODO Renew subscription rather than delete and replace?
            logger.fine( "Checking subscription for UDDI registry "+registryOid+"." );
            try {
                final UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    final UDDIClient uddiClient = factory.uddiHelper.newUDDIClient( uddiRegistry );

                    UDDIRegistrySubscription uddiRegistrySubscription =
                            factory.uddiRegistrySubscriptionManager.findByUDDIRegistryOid( registryOid );
                    if ( uddiRegistrySubscription != null && uddiRegistrySubscription.getSubscriptionKey()!=null ) {
                        if ( expiredOnly && !isExpiring(uddiRegistrySubscription) ) {
                            return;    
                        }

                        try {
                            uddiClient.deleteSubscription( uddiRegistrySubscription.getSubscriptionKey() );
                            uddiRegistrySubscription.setSubscriptionKey( null );
                        } catch ( UDDIInvalidKeyException ue ) {
                            logger.log( Level.WARNING, "Unable to delete subscription '"+uddiRegistrySubscription.getSubscriptionKey()+"' for registry "+describe(uddiRegistry)+", key is invalid (could be expired)." );
                        } catch ( UDDIException ue ) {
                            logger.log( Level.WARNING, "Unable to delete subscription '"+uddiRegistrySubscription.getSubscriptionKey()+"' for registry "+describe(uddiRegistry)+".", ue );
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

                    logger.info( "Subscribing for notifications from UDDI registry "+describe(uddiRegistry)+"." );
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
                            factory.uddiRegistrySubscriptionManager.save( uddiRegistrySubscription );
                        } else {
                            factory.uddiRegistrySubscriptionManager.update( uddiRegistrySubscription );
                        }
                    } catch ( Exception e ) {
                        logger.log( Level.WARNING, "Error persisting uddi subscription for registry "+describe(uddiRegistry)+".", e );
                        try {
                            uddiClient.deleteSubscription( subscriptionKey );
                        } catch ( UDDIException ue ) {
                            logger.log( Level.WARNING, "Unable to delete subscription '"+subscriptionKey+"' for registry "+describe(uddiRegistry)+".", ue );
                        }
                        throw new UDDIException( "Error persisting subscription for registry "+describe(uddiRegistry)+"." );
                    }
                } else if ( uddiRegistry == null ) {
                    logger.log( Level.WARNING, "UDDIRegistry (#"+registryOid+") not found for subscription." );
                } else {
                    throw new UDDIException("UDDI registry "+describe(uddiRegistry)+" is disabled.");
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_SUBSCRIBE_FAILED, e, "Database error when polling subscription for registry #"+registryOid+".");
            } catch (UDDIException ue) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_SUBSCRIBE_FAILED, ue, ExceptionUtils.getMessage(ue));
            }
        }
    }

    private static final class UnsubscribeUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( UnsubscribeUDDITask.class.getName() );

        private final SubscriptionUDDITaskFactory factory;
        private final long registryOid;

        public UnsubscribeUDDITask( final SubscriptionUDDITaskFactory factory,
                                    final long registryOid ) {
            this.factory = factory;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            logger.fine( "Processing unsubscribe for UDDI registry "+registryOid+"." );
            try {
                UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    UDDIClient uddiClient = factory.uddiHelper.newUDDIClient( uddiRegistry );

                    UDDIRegistrySubscription uddiRegistrySubscription = factory.uddiRegistrySubscriptionManager.findByUDDIRegistryOid( registryOid );
                    if ( uddiRegistrySubscription != null ) {
                        final String subscriptionKey = uddiRegistrySubscription.getSubscriptionKey();
                        if ( subscriptionKey != null ) {
                            logger.info( "Deleting subscription '"+subscriptionKey+"' for UDDI registry "+describe(uddiRegistry)+"." );
                            uddiClient.deleteSubscription( subscriptionKey );
                        } else {
                            logger.log( Level.WARNING, "Missing subscription key for registry "+describe(uddiRegistry)+", unsubscription not performed." );
                        }

                        factory.uddiRegistrySubscriptionManager.delete( uddiRegistrySubscription );
                    } else {
                        logger.log( Level.WARNING, "Cannot find subscription information for registry "+describe(uddiRegistry)+", unsubscription not performed." );
                    }
                } else if ( uddiRegistry == null ) {
                    logger.log( Level.WARNING, "UDDIRegistry (#"+registryOid+") not found for unsubscription." );
                } else {
                    throw new UDDIException("UDDI registry "+describe(uddiRegistry)+" is disabled.");
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_UNSUBSCRIBE_FAILED, e, "Database error when polling subscription for registry #"+registryOid+".");
            } catch (UDDIException ue) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_UNSUBSCRIBE_FAILED, ue, ExceptionUtils.getMessage(ue));
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
                                         final UDDISubscriptionResults results,
                                         final boolean renewIfExpiring ) throws FindException {
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

            if ( renewIfExpiring && isExpiring( uddiRegistrySubscription ) ) {
                logger.info( "Notifying subscribe event for UDDI registry (#"+registryOid+")." );
                context.notifyEvent( new SubscribeUDDIEvent( registryOid, SubscribeUDDIEvent.Type.SUBSCRIBE ) );
            }
        }

        protected boolean isExpiring( final UDDIRegistrySubscription uddiRegistrySubscription ) {
            return (uddiRegistrySubscription.getSubscriptionExpiryTime() - System.currentTimeMillis()) <
                 SUBSCRIPTION_RENEW_THRESHOLD;
        }
    }

    private static final class SubscriptionPollUDDITask extends SubscriptionProcessingUDDITask {
        private static final Logger logger = Logger.getLogger( SubscriptionPollUDDITask.class.getName() );

        private final SubscriptionUDDITaskFactory factory;
        private final long registryOid;
        private final long startTime;
        private final long endTime;

        SubscriptionPollUDDITask( final SubscriptionUDDITaskFactory factory,
                                  final long registryOid,
                                  final long startTime,
                                  final long endTime ) {
            super(logger,factory.uddiServiceControlManager);
            this.factory = factory;
            this.registryOid = registryOid;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            logger.fine( "Polling UDDI registry subscriptions." );
            try {
                UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    UDDIClient uddiClient = factory.uddiHelper.newUDDIClient( uddiRegistry );

                    UDDIRegistrySubscription uddiRegistrySubscription = factory.uddiRegistrySubscriptionManager.findByUDDIRegistryOid( registryOid );
                    if ( uddiRegistrySubscription != null ) {
                        final boolean isNotificationPoll = startTime > 0;
                        final String subscriptionKey = uddiRegistrySubscription.getSubscriptionKey();
                        final long lastCheckTime;
                        final long newCheckTime;

                        if ( isNotificationPoll ) {
                            lastCheckTime = startTime;
                            newCheckTime= endTime;
                            logger.info( "Polling subscription '"+subscriptionKey+"' for UDDI registry "+describe(uddiRegistry)+"." );
                        } else {
                            lastCheckTime = uddiRegistrySubscription.getSubscriptionCheckTime();
                            newCheckTime= System.currentTimeMillis();
                        }

                        UDDISubscriptionResults results = uddiClient.pollSubscription( lastCheckTime, newCheckTime, subscriptionKey );
                        processSubscriptionResults( context, uddiRegistrySubscription, results, !isNotificationPoll );

                        if ( !isNotificationPoll ) {
                            uddiRegistrySubscription.setSubscriptionCheckTime( newCheckTime );
                            factory.uddiRegistrySubscriptionManager.update( uddiRegistrySubscription );
                        }
                    } else {
                        context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_POLL_FAILED, "Missing subscription for registry "+describe(uddiRegistry)+".");
                    }
                } else if (uddiRegistry == null) {
                    logger.log( Level.WARNING, "UDDIRegistry (#"+registryOid+") not found for subscription poll." );
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_POLL_FAILED, e, "Database error when polling subscription for registry #"+registryOid+".");
            } catch (UDDIException ue) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_POLL_FAILED, ue, ExceptionUtils.getMessage(ue));
            }
        }
    }

    private static final class SubscriptionNotificationUDDITask extends SubscriptionProcessingUDDITask {
        private static final Logger logger = Logger.getLogger( SubscriptionNotificationUDDITask.class.getName() );

        private static final long SUBSCRIPTION_TOLERANCE = SyspropUtil.getLong( "com.l7tech.server.uddi.subscriptionTolerance", TimeUnit.SECONDS.toMillis(10) );

        private final SubscriptionUDDITaskFactory factory;
        private final String message;
        private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public SubscriptionNotificationUDDITask( final SubscriptionUDDITaskFactory factory,
                                                 final String message ) {
            super(logger,factory.uddiServiceControlManager);
            this.factory = factory;
            this.message = message;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            logger.fine( "Processing UDDI subscription notification." );

            String subscriptionKey = null;
            try {
                final UDDISubscriptionResults results = UDDISubscriptionResultFactory.buildResults( message );
                subscriptionKey = results.getSubscriptionKey();
                final Collection<UDDIRegistrySubscription> subscriptions = factory.uddiRegistrySubscriptionManager.findBySubscriptionKey( subscriptionKey );

                UDDIRegistrySubscription subscription = null;
                if ( subscriptions.size() > 1 ) {
                    // Then we have to look at the service keys to find which registry this is for.
                    outer:
                    for ( UDDISubscriptionResults.Result result : results.getResults() ) {
                        Collection<UDDIServiceControl> controls = factory.uddiServiceControlManager.findByUDDIServiceKey( result.getEntityKey() );
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
                    context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_NOTIFICATION_BADKEY, subscriptionKey );
                }

                if ( subscription != null ) {
                    UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey( subscription.getUddiRegistryOid() );
                    if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                        processSubscriptionResults( context, subscription, results, true );
                        long lastEndTime = subscription.getSubscriptionNotifiedTime();
                        if ( lastEndTime < (results.getStartTime()-SUBSCRIPTION_TOLERANCE) ) {
                            logger.warning( "Missed subscription notifications for period "+formatDate(lastEndTime)+" to "+formatDate(results.getStartTime())+" for registry "+describe(uddiRegistry)+"." );
                            context.notifyEvent( new PollUDDIEvent( subscription.getUddiRegistryOid(), lastEndTime, results.getStartTime() ) );
                        }
                        subscription.setSubscriptionNotifiedTime( results.getEndTime() );
                        factory.uddiRegistrySubscriptionManager.update( subscription );
                    } else if ( uddiRegistry != null ) {
                        logger.info( "UDDI registry "+describe(uddiRegistry)+" is disabled, ignoring subscription notification.");
                    }
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_NOTIFICATION_FAILED, e, "Database error when processing subscription for "+subscriptionKey+".");
            } catch (UDDIException ue) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_NOTIFICATION_FAILED, ue, ExceptionUtils.getMessage(ue));
            }
        }

        private String formatDate( final long time ) {
            return dateFormat.format(new Date(time));            
        }
    }

    private static final class BusinessServiceUpdateUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( BusinessServiceUpdateUDDITask.class.getName() );

        private final SubscriptionUDDITaskFactory factory;
        private final String serviceKey;
        private final long registryOid;
        private final boolean isDeleted;

        public BusinessServiceUpdateUDDITask( final SubscriptionUDDITaskFactory factory,
                                              final String serviceKey,
                                              final long registryOid,
                                              final boolean deleted ) {
            this.factory = factory;
            this.serviceKey = serviceKey;
            this.registryOid = registryOid;
            this.isDeleted = deleted;
        }

        @Override
        public void apply(UDDITaskContext context) throws UDDITaskException {
            //may just be url update or may be entire WSDL
            //if just URL - download the accessPoint and see what it's value is
            try {
                //each result is for a unique PublishedService. More than one results is when more than one
                //published service is monitoring the same WSDL in UDDI
                final Collection<UDDIServiceControl> allApplicableServiceControls =
                        factory.uddiServiceControlManager.findByUDDIRegistryAndServiceKey(registryOid, serviceKey, null);

                if ( isDeleted ) {
                    logger.log(Level.INFO, "Service with key: " + serviceKey +
                            " has been deleted from UDDI Registry #(" + registryOid + "). Removing record of UDDI BusinessService.");
                    //will not ignore reality, if we know the service is gone from uddi, then no point having a record of it

                    boolean deleted = false;
                    for(final UDDIServiceControl serviceControl: allApplicableServiceControls) {
                        deleted = true;
                        factory.uddiServiceControlManager.delete(serviceControl);
                        if (!serviceControl.isUnderUddiControl() || !serviceControl.isMonitoringEnabled()) continue;

                        if(serviceControl.isDisableServiceOnChange()) {
                            //take correct action -disable service if set up
                            disableService(context, serviceControl);
                        }
                    }

                    if ( deleted ) {
                        context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_SERVICE_DELETED, serviceKey);
                    }
                } else {
                    //Get the last modified time stamp
                    final UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey(registryOid);
                    if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                        final UDDIClient uddiClient = factory.uddiHelper.newUDDIClient(uddiRegistry);
                        final long uddiModifiedTime;
                        try {
                            UDDIOperationalInfo operationalInfo = uddiClient.getOperationalInfo(serviceKey);
                            uddiModifiedTime = operationalInfo.getModifiedIncludingChildrenTime();
                        } catch (UDDIException e) {
                            context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_PROCESSING_FAILED, e,
                                    "Could not get operation information for serviceKey: " + serviceKey);
                            throw new UDDITaskException("Cannot find Operational Information for serviceKey: "
                                    + serviceKey + " from UDDIRegistry #(" + uddiRegistry.getOid() + ")");
                        }

                        for(final UDDIServiceControl serviceControl: allApplicableServiceControls) {
                            if (!serviceControl.isUnderUddiControl() || !serviceControl.isMonitoringEnabled()) continue;

                            final PublishedService ps =
                                    factory.serviceManager.findByPrimaryKey(serviceControl.getPublishedServiceOid());
                            final UDDIServiceControlMonitorRuntime monitorRuntime =
                                    factory.uddiServiceControlMonitorRuntimeManager.findByServiceControlOid(serviceControl.getOid());
                            if (monitorRuntime == null) {
                                //this should never happen, (its a coding error managing UDDIServiceControl entities),
                                // if it does we will just create a record for it. If the db allows it, then proceed
                                final UDDIServiceControlMonitorRuntime monitorRuntimeNew = new UDDIServiceControlMonitorRuntime(serviceControl.getOid(), uddiModifiedTime);
                                factory.uddiServiceControlMonitorRuntimeManager.save(monitorRuntimeNew);
                                logger.log(Level.WARNING, "Recieved notification for service for which we had no persisted runtime " +
                                        "information. Created record for serviceKey: " + serviceControl.getUddiServiceKey() +
                                        " from registry " + describe(uddiRegistry) + ".");
                            } else {
                                final long lastKnownModificationTime = monitorRuntime.getLastUDDIModifiedTimeStamp();
                                if (uddiModifiedTime <= lastKnownModificationTime) {
                                    logger.log(Level.FINE, "Recieved duplicate notification for serviceKey: " + serviceControl.getUddiServiceKey() +
                                            " from registry " + describe(uddiRegistry) + ".");
                                    return;
                                }
                            }

                            //Now we start processing the update
                            UDDIUtilities.UDDIBindingImplementionInfo bindingImplInfo = null;
                            try {
                                bindingImplInfo = UDDIUtilities.getUDDIBindingImplInfo(uddiClient,
                                        serviceControl.getUddiServiceKey(), serviceControl.getWsdlPortName(), serviceControl.getWsdlPortBinding());
                            } catch (UDDIException e) {
                                context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_PROCESSING_FAILED, e,
                                        "Could not find UDDI bindingTemplate implement wsdl:binding "
                                                + serviceControl.getWsdlPortBinding() + " for serviceKey: " + serviceControl.getUddiServiceKey());
                            }

                            if ( bindingImplInfo == null ) {
                                context.logAndAudit(
                                        SystemMessages.UDDI_NOTIFICATION_ENDPOINT_NOT_FOUND,
                                        serviceKey,
                                        serviceControl.getWsdlPortName(),
                                        describe(uddiRegistry) );
                                continue;
                            }

                            //only want changes to the accessPoint
                            //we do this regardless of configuration. UDDI is the authorative source of info for the endPoint
                            boolean serviceUpdated = false;
                            boolean updated = false;
                            final String endPoint = getUpdatedEndPoint(bindingImplInfo, serviceControl, context, uddiRegistry);
                            if ( endPoint != null && (serviceControl.getAccessPointUrl()==null || !serviceControl.getAccessPointUrl().equals( endPoint ))) {
                                //now we can update our records
                                //if we need to extract a url from the wsdl see UDDIUtilities.extractEndPointFromWsdl
                                updated = true;
                                serviceUpdated = true;
                                ps.setDefaultRoutingUrl(endPoint);
                                serviceControl.setAccessPointUrl(endPoint);
                                context.logAndAudit(
                                        SystemMessages.UDDI_NOTIFICATION_ENDPOINT_UPDATED,
                                        endPoint,
                                        serviceKey,
                                        serviceControl.getWsdlPortName(),
                                        describe(uddiRegistry) );
                            }

                            //do we need to update our serviceControl record?
                            if(!bindingImplInfo.getImplementingWsdlPort().equals(serviceControl.getWsdlPortName())){
                                updated = true;
                                serviceControl.setWsdlPortName(bindingImplInfo.getImplementingWsdlPort());
                            }

                            if ( updated ) {
                                factory.uddiServiceControlManager.update(serviceControl);
                            }

                            //if we didn't accept the endpoint above, we can still update our wsdl, which makes all available
                            //endpoints available on the client - manually
                            if ( serviceControl.isUpdateWsdlOnChange() || serviceControl.isDisableServiceOnChange() ) {
                                // Validate url
                                String wsdlUrlStr = bindingImplInfo.getImplementingWsdlUrl();
                                try {
                                    new URL(wsdlUrlStr);
                                } catch (MalformedURLException e) {
                                    final String msg = "WSDL URL obtained from UDDI is not a valid URL (" +
                                            bindingImplInfo.getImplementingWsdlUrl() + ") for serviceKey: " +
                                            serviceControl.getUddiServiceKey()+" from registry " + describe(uddiRegistry)+".";
                                    context.logAndAudit( SystemMessages.UDDI_NOTIFICATION_PROCESSING_FAILED, e,
                                            msg);
                                    throw new UDDITaskException(msg);
                                }

                                // Fetch wsdl
                                final SimpleHttpClient httpClient = new SimpleHttpClient(factory.httpClientFactory.createHttpClient(), 10*1024*1024);
                                try {
                                    final RemoteEntityResolver resolver = new RemoteEntityResolver( httpClient );
                                    final DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
                                    final Map<String,String> contents = processor.processDocument( wsdlUrlStr, new DocumentReferenceProcessor.ResourceResolver(){
                                        @Override
                                        public String resolve( final String resourceUrl ) throws IOException {
                                            String content = resolver.fetchResourceFromUrl( resourceUrl );
                                            return ResourceTrackingWSDLLocator.processResource(resourceUrl, content, resolver, false, true);
                                        }
                                    } );

                                    final Collection<ResourceTrackingWSDLLocator.WSDLResource> sourceDocs =
                                            ResourceTrackingWSDLLocator.toWSDLResources(wsdlUrlStr, contents, false, false, false);

                                    final List<ServiceDocument> serviceDocuments = ServiceDocumentWsdlStrategy.fromWsdlResources( sourceDocs );

                                    // Check if WSDL has changed
                                    Collection<ServiceDocument> existingServiceDocuments = factory.serviceDocumentManager.findByServiceId(ps.getOid());
                                    Wsdl wsdl = ServiceDocumentWsdlStrategy.parseWsdl( ps, existingServiceDocuments );
                                    Wsdl newWsdl = ServiceDocumentWsdlStrategy.parseWsdl( wsdlUrlStr ,contents.get(wsdlUrlStr), existingServiceDocuments );

                                    if ( !wsdl.getHash().equals( newWsdl.getHash() ) ) {
                                        serviceUpdated = true;

                                        // Disable if so configured
                                        if ( serviceControl.isDisableServiceOnChange() ) {
                                            ps.setDisabled( true );
                                            context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_SERVICE_DISABLED, ps.getId());
                                        }

                                        if ( serviceControl.isUpdateWsdlOnChange() ) {
                                            //if we need to extract a url from the wsdl see UDDIUtilities.extractEndPointFromWsdl
                                            ps.setWsdlUrl( wsdlUrlStr );
                                            ps.setWsdlXml( contents.get(wsdlUrlStr) );
                                            for (ServiceDocument serviceDocument : existingServiceDocuments) {
                                                factory.serviceDocumentManager.delete(serviceDocument);
                                            }
                                            for (ServiceDocument serviceDocument : serviceDocuments) {
                                                serviceDocument.setServiceId(ps.getOid());
                                                factory.serviceDocumentManager.save(serviceDocument);
                                            }

                                            context.logAndAudit( SystemMessages.UDDI_NOTIFICATION_SERVICE_WSDL_UPDATE, ps.displayName() + " (#"+ps.getOid()+")" );
                                        }
                                    } else {
                                        logger.info( "WSDL is not updated for business service '"+serviceKey+"' for registry "+describe(uddiRegistry)+"." );
                                    }
                                } catch ( IOException ioe ) {
                                    context.logAndAudit( SystemMessages.UDDI_NOTIFICATION_SERVICE_WSDL_ERROR, ioe, ps.displayName() + " (#"+ps.getOid()+")" );
                                } catch ( WSDLException we ) {
                                    context.logAndAudit( SystemMessages.UDDI_NOTIFICATION_SERVICE_WSDL_ERROR, we, ps.displayName() + " (#"+ps.getOid()+")" );
                                }
                            }

                            if ( serviceUpdated ) {
                                factory.serviceManager.update(ps);
                            }

                            UDDIServiceControlMonitorRuntime monitorRuntimeToUpdate = factory.uddiServiceControlMonitorRuntimeManager.findByServiceControlOid(serviceControl.getOid());
                            monitorRuntimeToUpdate.setLastUDDIModifiedTimeStamp(uddiModifiedTime);
                            factory.uddiServiceControlMonitorRuntimeManager.update(monitorRuntimeToUpdate);
                        }
                    }
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_NOTIFICATION_PROCESSING_FAILED, e,
                        "Database error when processing notification for registry #"+registryOid+" and serviceKey: " + serviceKey+".");
            }
        }

        private void disableService(UDDITaskContext context, UDDIServiceControl serviceControl) throws FindException, UpdateException {
            final PublishedService ps = factory.serviceManager.findByPrimaryKey(serviceControl.getPublishedServiceOid());
            ps.setDisabled(true);
            factory.serviceManager.update(ps);
            context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_SERVICE_DISABLED, ps.getOidAsLong().toString());
        }

        /**
         * Get the endpoint from UDDI for this update. All auditing, logging and throwing is done here.
         * @return String endpoint. Null if not found
         */
        private String getUpdatedEndPoint(final UDDIUtilities.UDDIBindingImplementionInfo bindingImplInfo,
                                          final UDDIServiceControl serviceControl,
                                          final UDDITaskContext context,
                                          final UDDIRegistry uddiRegistry) throws UDDITaskException {

            final String endPoint = bindingImplInfo.getEndPoint();

            boolean endPointValid = true;
            if(endPoint == null || endPoint.trim().isEmpty()){
                endPointValid = false;
            }else{
                try {
                    new URL(endPoint);
                } catch (MalformedURLException e) {
                    endPointValid = false;
                }
            }

            if(endPointValid && !serviceControl.getAccessPointUrl().equals(endPoint)){
                //check if it's a gateway endpoint
                if(factory.uddiHelper.isGatewayUrl(endPoint)){
                    final String msg = "endPoint found which would route back to the gateway: " + endPoint+" for serviceKey: " + serviceControl.getUddiServiceKey();
                    context.logAndAudit( SystemMessages.UDDI_NOTIFICATION_PROCESSING_FAILED, msg);
                    throw new UDDITaskException("Problem with endPoint found for serviceKey: " + serviceControl.getUddiServiceKey()+
                            " for a bindingTemplate representing the wsdl:port '"+serviceControl.getWsdlPortName()+"'" +
                            " from UDDIRegistry #(" + uddiRegistry.getOid()+"): " + msg);
                }
            }else if(!endPointValid){
                logger.log(Level.INFO, "Found invalid URL from UDDI. Ignoring");
            }

            return endPoint;
        }
    }

    private static final class RemoteEntityResolver implements EntityResolver {
        final WsdlEntityResolver catalogResolver = new WsdlEntityResolver(true);
        final SimpleHttpClient client;

        RemoteEntityResolver( final SimpleHttpClient client ) {
            this.client = client;
        }

        @Override
        public InputSource resolveEntity( final String publicId, final String systemId ) throws SAXException, IOException {
            InputSource inputSource = catalogResolver.resolveEntity( publicId, systemId );

            if ( inputSource == null ) {
                String resource = fetchResourceFromUrl( systemId );

                inputSource = new InputSource();
                inputSource.setPublicId( publicId );
                inputSource.setSystemId( systemId );
                inputSource.setCharacterStream( new StringReader(resource) );
            }

            return inputSource;
        }

        private String fetchResourceFromUrl( final String resourceUrl ) throws IOException {
            String resource = null;

            if ( resourceUrl.toLowerCase().startsWith("http:") ||
                 resourceUrl.toLowerCase().startsWith("https:") ) {
                SimpleHttpClient.SimpleHttpResponse response = client.get( resourceUrl );
                if ( response.getStatus() == HttpConstants.STATUS_OK ) {
                    resource = response.getString();
                } else {
                    throw new IOException("Could not access resource '"+resourceUrl+"', http status "+response.getStatus()+".");
                }
            }

            if ( resource == null ) {
                throw new IOException("Could not access resource '"+resourceUrl+"'.");
            }

            return resource;
        }
    }
}
