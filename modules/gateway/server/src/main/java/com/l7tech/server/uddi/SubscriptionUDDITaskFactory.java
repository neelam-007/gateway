package com.l7tech.server.uddi;

import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.io.DocumentReferenceProcessor;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gateway.common.uddi.UDDIServiceControlRuntime;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.uddi.*;
import com.l7tech.util.*;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.wsdl.WsdlEntityResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UDDITaskFactory for subscription tasks
 */
public class SubscriptionUDDITaskFactory extends UDDITaskFactory {

    //- PUBLIC

    public SubscriptionUDDITaskFactory(final UDDIRegistryManager uddiRegistryManager,
                                       final UDDIHelper uddiHelper,
                                       final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                       final UDDIServiceControlManager uddiServiceControlManager,
                                       final UDDIServiceControlRuntimeManager uddiServiceControlRuntimeManager,
                                       final ServiceManager serviceManager,
                                       final ServiceDocumentManager serviceDocumentManager,
                                       final GenericHttpClientFactory httpClientFactory ) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiHelper = uddiHelper;
        this.uddiRegistrySubscriptionManager = uddiRegistrySubscriptionManager;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiServiceControlRuntimeManager = uddiServiceControlRuntimeManager;
        this.serviceManager = serviceManager;
        this.serviceDocumentManager = serviceDocumentManager;
        this.httpClientFactory = httpClientFactory;
        this.config = ConfigFactory.getCachedConfig();
    }

    @Override
    public UDDITask buildUDDITask( final UDDIEvent event ) {
        UDDITask task = null;

        if ( event instanceof TimerUDDIEvent ) {
            TimerUDDIEvent timerEvent = (TimerUDDIEvent) event;
            if ( timerEvent.getType() == TimerUDDIEvent.Type.SUBSCRIPTION_POLL ) {
                task = new SubscriptionPollUDDITask(
                        this,
                        timerEvent.getRegistryGoid(),
                        -1L,
                        -1L );
            }
        } else if ( event instanceof PollUDDIEvent ) {
            PollUDDIEvent pollEvent = (PollUDDIEvent) event;
            task = new SubscriptionPollUDDITask(
                    this,
                    pollEvent.getRegistryGoid(),
                    pollEvent.getStartTime(),
                    pollEvent.getEndTime());
        } else if ( event instanceof SubscribeUDDIEvent ) {
            SubscribeUDDIEvent subscribeEvent = (SubscribeUDDIEvent) event;
            switch ( subscribeEvent.getType() ) {
                case SUBSCRIBE:
                    task = new SubscribeUDDITask(
                            this,
                            subscribeEvent.getRegistryGoid(),
                            subscribeEvent.isExpiredOnly() );
                    break;
                case UNSUBSCRIBE:
                    task = new UnsubscribeUDDITask(
                            this,
                            subscribeEvent.getRegistryGoid() );
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
                    busEvent.getRegistryGoid(),
                    busEvent.isDeleted(),
                    busEvent.isForceUpdate());
        } else if(event instanceof UpdateAllMonitoredServicesUDDIEvent){
            UpdateAllMonitoredServicesUDDIEvent updateEvent = (UpdateAllMonitoredServicesUDDIEvent) event;
            task = new UpdateAllMonitoredServicesUDDITask(this, updateEvent.getRegistryGoid());
        }

        return task;
    }

    //- PRIVATE

    private static final long SUBSCRIPTION_EXPIRY_INTERVAL = ConfigFactory.getLongProperty( "com.l7tech.server.uddi.subscriptionExpiryInterval", TimeUnit.DAYS.toMillis( 5L ) );
    private static final long SUBSCRIPTION_RENEW_THRESHOLD = ConfigFactory.getLongProperty( "com.l7tech.server.uddi.subscriptionRenewThreshold", TimeUnit.DAYS.toMillis( 2L ) );

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIHelper uddiHelper;
    private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
    private final UDDIServiceControlManager uddiServiceControlManager;
    private final UDDIServiceControlRuntimeManager uddiServiceControlRuntimeManager;
    private final ServiceManager serviceManager;
    private final ServiceDocumentManager serviceDocumentManager;
    private final GenericHttpClientFactory httpClientFactory;
    private final Config config;

    private static String describe( final UDDIRegistry uddiRegistry ) {
        return uddiRegistry.getName()+" (#"+uddiRegistry.getGoid()+")";
    }

    private static final class SubscribeUDDITask extends SubscriptionProcessingUDDITask {
        private static final Logger logger = Logger.getLogger( SubscribeUDDITask.class.getName() );

        private final SubscriptionUDDITaskFactory factory;
        private final Goid registryGoid;
        private final boolean expiredOnly;

        private SubscribeUDDITask( final SubscriptionUDDITaskFactory factory,
                                   final Goid registryGoid,
                                   final boolean expiredOnly ) {
            super( logger, null );
            this.factory = factory;
            this.registryGoid = registryGoid;
            this.expiredOnly = expiredOnly;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            // TODO Renew subscription rather than delete and replace?
            logger.fine( "Checking subscription for UDDI registry "+registryGoid+"." );
            try {
                final UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey( registryGoid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    UDDIClient uddiClient = null;
                    try {
                        uddiClient = factory.uddiHelper.newUDDIClient( uddiRegistry );

                        UDDIRegistrySubscription uddiRegistrySubscription =
                                factory.uddiRegistrySubscriptionManager.findByUDDIRegistryGoid( registryGoid );
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
                                logger.log( Level.WARNING, "Unable to delete subscription '"+uddiRegistrySubscription.getSubscriptionKey()+"' for registry "+describe(uddiRegistry)+".", ExceptionUtils.getDebugException(ue) );
                            }
                        }

                        String bindingKey = null;
                        long monitoringInterval = 0L;
                        if ( uddiRegistry.isSubscribeForNotifications() ) {
                            bindingKey = context.getSubscriptionBindingKey( registryGoid );
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
                            uddiRegistrySubscription.setUddiRegistryGoid( registryGoid );
                        }

                        uddiRegistrySubscription.setSubscriptionKey( subscriptionKey );
                        uddiRegistrySubscription.setSubscriptionExpiryTime( expiryTime );
                        // when polling set the last check time to the subscription start time
                        uddiRegistrySubscription.setSubscriptionCheckTime( bindingKey == null ? System.currentTimeMillis() : 0L );
                        uddiRegistrySubscription.setSubscriptionNotifiedTime( 0L );

                        try {
                            if ( Goid.isDefault(uddiRegistrySubscription.getGoid()) ) {
                                factory.uddiRegistrySubscriptionManager.save( uddiRegistrySubscription );
                            } else {
                                factory.uddiRegistrySubscriptionManager.update( uddiRegistrySubscription );
                            }
                        } catch ( Exception e ) {
                            logger.log( Level.WARNING, "Error persisting uddi subscription for registry "+describe(uddiRegistry)+". Cause: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
                            try {
                                uddiClient.deleteSubscription( subscriptionKey );
                            } catch ( UDDIException ue ) {
                                logger.log( Level.WARNING, "Unable to delete subscription '"+subscriptionKey+"' for registry "+describe(uddiRegistry)+". Cause: " + ExceptionUtils.getMessage(ue), ExceptionUtils.getDebugException(ue) );
                            }
                            throw new UDDIException( "Error persisting subscription for registry "+describe(uddiRegistry)+"." );
                        }
                    } finally {
                        ResourceUtils.closeQuietly( uddiClient );
                    }
                } else if ( uddiRegistry == null ) {
                    logger.log( Level.WARNING, "UDDIRegistry (#"+registryGoid+") not found for subscription." );
                } else {
                    throw new UDDIException("UDDI registry "+describe(uddiRegistry)+" is disabled.");
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_SUBSCRIBE_FAILED, e, "Database error when polling subscription for registry #"+registryGoid+".");
            } catch (UDDIException ue) {
                context.logAndAudit(SystemMessages.UDDI_SUBSCRIPTION_SUBSCRIBE_FAILED, ExceptionUtils.getDebugException(ue), ExceptionUtils.getMessage(ue));
            }
        }
    }

    private static final class UnsubscribeUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( UnsubscribeUDDITask.class.getName() );

        private final SubscriptionUDDITaskFactory factory;
        private final Goid registryGoid;

        private UnsubscribeUDDITask( final SubscriptionUDDITaskFactory factory,
                                     final Goid registryGoid ) {
            this.factory = factory;
            this.registryGoid = registryGoid;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            logger.fine( "Processing unsubscribe for UDDI registry "+registryGoid+"." );
            try {
                UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey( registryGoid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    UDDIRegistrySubscription uddiRegistrySubscription = factory.uddiRegistrySubscriptionManager.findByUDDIRegistryGoid( registryGoid );
                    if ( uddiRegistrySubscription != null ) {
                        final String subscriptionKey = uddiRegistrySubscription.getSubscriptionKey();
                        if ( subscriptionKey != null ) {
                            logger.info( "Deleting subscription '"+subscriptionKey+"' for UDDI registry "+describe(uddiRegistry)+"." );
                            UDDIClient uddiClient = null;
                            try {
                                uddiClient = factory.uddiHelper.newUDDIClient( uddiRegistry );
                                uddiClient.deleteSubscription( subscriptionKey );
                            } finally {
                                ResourceUtils.closeQuietly( uddiClient );
                            }
                        } else {
                            logger.log( Level.WARNING, "Missing subscription key for registry "+describe(uddiRegistry)+", unsubscription not performed." );
                        }

                        factory.uddiRegistrySubscriptionManager.delete( uddiRegistrySubscription );
                    } else {
                        logger.log( Level.WARNING, "Cannot find subscription information for registry "+describe(uddiRegistry)+", unsubscription not performed." );
                    }
                } else if ( uddiRegistry == null ) {
                    logger.log( Level.WARNING, "UDDIRegistry (#"+registryGoid+") not found for unsubscription." );
                } else {
                    throw new UDDIException("UDDI registry "+describe(uddiRegistry)+" is disabled.");
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_UNSUBSCRIBE_FAILED, e, "Database error when polling subscription for registry #"+registryGoid+".");
            } catch (UDDIException ue) {
                context.logAndAudit(SystemMessages.UDDI_SUBSCRIPTION_UNSUBSCRIBE_FAILED, ExceptionUtils.getDebugException(ue), ExceptionUtils.getMessage(ue));
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
            final Goid registryGoid = uddiRegistrySubscription.getUddiRegistryGoid();
            for ( UDDISubscriptionResults.Result result : results.getResults() ) {
                Collection<UDDIServiceControl> controls = uddiServiceControlManager.findByUDDIRegistryAndServiceKey(
                        registryGoid,
                        result.getEntityKey(),
                        true );

                if ( !controls.isEmpty() ) {
                    // Fire event for update from UDDI
                    context.notifyEvent( new BusinessServiceUpdateUDDIEvent(
                            registryGoid,
                            result.getEntityKey(),
                            result.isDeleted(), false) );
                }
            }

            if ( renewIfExpiring && isExpiring( uddiRegistrySubscription ) ) {
                logger.info( "Notifying subscribe event for UDDI registry (#"+registryGoid+")." );
                context.notifyEvent( new SubscribeUDDIEvent( registryGoid, SubscribeUDDIEvent.Type.SUBSCRIBE ) );
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
        private final Goid registryGoid;
        private final long startTime;
        private final long endTime;

        SubscriptionPollUDDITask( final SubscriptionUDDITaskFactory factory,
                                  final Goid registryGoid,
                                  final long startTime,
                                  final long endTime ) {
            super(logger,factory.uddiServiceControlManager);
            this.factory = factory;
            this.registryGoid = registryGoid;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            logger.fine( "Polling UDDI registry subscriptions." );
            try {
                UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey( registryGoid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    UDDIRegistrySubscription uddiRegistrySubscription = factory.uddiRegistrySubscriptionManager.findByUDDIRegistryGoid( registryGoid );
                    if ( uddiRegistrySubscription != null ) {
                        final boolean isNotificationPoll = startTime > 0L;
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

                        UDDIClient uddiClient = null;
                        try {
                            uddiClient = factory.uddiHelper.newUDDIClient( uddiRegistry );
                            UDDISubscriptionResults results = uddiClient.pollSubscription( lastCheckTime, newCheckTime, subscriptionKey );
                            processSubscriptionResults( context, uddiRegistrySubscription, results, !isNotificationPoll );

                            if ( !isNotificationPoll ) {
                                uddiRegistrySubscription.setSubscriptionCheckTime( newCheckTime );
                                factory.uddiRegistrySubscriptionManager.update( uddiRegistrySubscription );
                            }
                        } finally {
                            ResourceUtils.closeQuietly( uddiClient );
                        }
                    } else {
                        context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_POLL_FAILED, "Missing subscription for registry "+describe(uddiRegistry)+".");
                    }
                } else if (uddiRegistry == null) {
                    logger.log( Level.WARNING, "UDDIRegistry (#"+registryGoid+") not found for subscription poll." );
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_POLL_FAILED, e, "Database error when polling subscription for registry #"+registryGoid+".");
            } catch (UDDIException ue) {
                context.logAndAudit(SystemMessages.UDDI_SUBSCRIPTION_POLL_FAILED, ExceptionUtils.getDebugException(ue), ExceptionUtils.getMessage(ue));
            }
        }
    }

    private static final class SubscriptionNotificationUDDITask extends SubscriptionProcessingUDDITask {
        private static final Logger logger = Logger.getLogger( SubscriptionNotificationUDDITask.class.getName() );

        private static final long SUBSCRIPTION_TOLERANCE = ConfigFactory.getLongProperty( "com.l7tech.server.uddi.subscriptionTolerance", TimeUnit.SECONDS.toMillis( 10L ) );

        private final SubscriptionUDDITaskFactory factory;
        private final String message;
        private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        private SubscriptionNotificationUDDITask( final SubscriptionUDDITaskFactory factory,
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
                                if ( Goid.equals(sub.getUddiRegistryGoid(), control.getUddiRegistryGoid()) ) {
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
                    UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey( subscription.getUddiRegistryGoid() );
                    if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                        processSubscriptionResults( context, subscription, results, true );
                        long lastEndTime = subscription.getSubscriptionNotifiedTime();
                        if ( lastEndTime < (results.getStartTime()-SUBSCRIPTION_TOLERANCE) ) {
                            logger.warning( "Missed subscription notifications for period "+formatDate(lastEndTime)+" to "+formatDate(results.getStartTime())+" for registry "+describe(uddiRegistry)+"." );
                            context.notifyEvent( new PollUDDIEvent( subscription.getUddiRegistryGoid(), lastEndTime, results.getStartTime() ) );
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
                context.logAndAudit(SystemMessages.UDDI_SUBSCRIPTION_NOTIFICATION_FAILED, ExceptionUtils.getDebugException(ue), ExceptionUtils.getMessage(ue));
            }
        }

        private String formatDate( final long time ) {
            return dateFormat.format(new Date(time));            
        }
    }

    private static final class UpdateAllMonitoredServicesUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( UpdateAllMonitoredServicesUDDITask.class.getName() );
        
        private final SubscriptionUDDITaskFactory factory;
        private final Goid registryGoid;

        private UpdateAllMonitoredServicesUDDITask(SubscriptionUDDITaskFactory factory, Goid registryGoid) {
            this.factory = factory;
            this.registryGoid = registryGoid;
        }

        @Override
        public void apply(UDDITaskContext context) throws UDDITaskException {
            try {
                final UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey(registryGoid);
                if(uddiRegistry == null) {
                    logger.log(Level.FINE, "UDDI Registry with id#(" + registryGoid+") not found");
                    return;
                }
                if(!uddiRegistry.isMonitoringEnabled()) {
                    logger.log(Level.FINE, "UDDI Registry " + describe(uddiRegistry) + " is not configured for monitoring. Not processing batch update");
                    return;
                }
                logger.log(Level.INFO, "Forcing refresh of all published services which are monitoring UDDI Registry " + describe(uddiRegistry));

                final Collection<UDDIServiceControl> allControlsForRegistry = factory.uddiServiceControlManager.findByUDDIRegistryGoid(registryGoid);
                for(UDDIServiceControl serviceControl: allControlsForRegistry){
                    if(!serviceControl.isMonitoringEnabled()) continue;
                    context.notifyEvent(new BusinessServiceUpdateUDDIEvent(registryGoid, serviceControl.getUddiServiceKey(), false, true));
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_NOTIFICATION_TRIGGERING_FAILED, e, Goid.toString(registryGoid));
            }
        }
    }

    private static final class BusinessServiceUpdateUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( BusinessServiceUpdateUDDITask.class.getName() );

        private final SubscriptionUDDITaskFactory factory;
        private final String serviceKey;
        private final Goid registryGoid;
        private final boolean isDeleted;
        private final boolean forceUpdate;

        private BusinessServiceUpdateUDDITask(final SubscriptionUDDITaskFactory factory,
                                              final String serviceKey,
                                              final Goid registryGoid,
                                              final boolean deleted,
                                              final boolean forceUpdate) {
            this.factory = factory;
            this.serviceKey = serviceKey;
            this.registryGoid = registryGoid;
            this.isDeleted = deleted;
            this.forceUpdate = forceUpdate;
        }

        @Override
        public void apply(UDDITaskContext context) throws UDDITaskException {
            //may just be url update or may be entire WSDL
            //if just URL - download the accessPoint and see what it's value is
            try {
                //each result is for a unique PublishedService. More than one results is when more than one
                //published service has the same original service in UDDI
                final Collection<UDDIServiceControl> allApplicableServiceControls =
                        factory.uddiServiceControlManager.findByUDDIRegistryAndServiceKey(registryGoid, serviceKey, null);

                logger.log(Level.FINE, "Processing business service update. UDDIRegistry #id("+registryGoid+") for serviceKey: " + serviceKey+" isDeleted = " + isDeleted);

                if ( isDeleted ) {
                    logger.log(Level.INFO, "Service with key: " + serviceKey +
                            " has been deleted from UDDI Registry #(" + registryGoid + "). Removing record of UDDI BusinessService.");
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
                    final UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey(registryGoid);
                    if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                        final List<UDDIHandledTaskException> caughtExceptions = new ArrayList<UDDIHandledTaskException>();
                        UDDIClient uddiClient = null;
                        try {
                            uddiClient = factory.uddiHelper.newUDDIClient(uddiRegistry);

                            final long uddiModifiedTime;
                            try {
                                UDDIOperationalInfo operationalInfo = uddiClient.getOperationalInfo(serviceKey);
                                uddiModifiedTime = operationalInfo.getModifiedIncludingChildrenTime();
                            } catch (UDDIException e) {
                                context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_PROCESSING_FAILED, ExceptionUtils.getDebugException(e),
                                        "Could not get operation information for serviceKey: " + serviceKey);
                                throw new UDDITaskException("Cannot find Operational Information for serviceKey: "
                                        + serviceKey + " from UDDIRegistry #(" + uddiRegistry.getGoid() + ")");
                            }

                            for(UDDIServiceControl serviceControl: allApplicableServiceControls) {
                                if (forceUpdate && !serviceControl.isUnderUddiControl()) {
                                    //if this is a force update the service must at least be under uddi control for
                                    //if we are going to check UDDI for accuracy.
                                    continue;
                                } else if (!forceUpdate && (!serviceControl.isUnderUddiControl() || !serviceControl.isMonitoringEnabled())) {
                                    //if this is a real notification from uddi, only process it if the uddi service control
                                    //is actually monitoring uddi.
                                    continue;
                                }

                                final PublishedService ps =
                                        factory.serviceManager.findByPrimaryKey(serviceControl.getPublishedServiceGoid());
                                final UDDIServiceControlRuntime monitorRuntime =
                                        factory.uddiServiceControlRuntimeManager.findByServiceControlGoid(serviceControl.getGoid());
                                if(!forceUpdate){
                                    if(monitorRuntime != null){
                                        final long lastKnownModificationTime = monitorRuntime.getLastUDDIModifiedTimeStamp();
                                        if (uddiModifiedTime <= lastKnownModificationTime) {
                                            logger.log(Level.FINE, "Received duplicate notification for serviceKey: " + serviceControl.getUddiServiceKey() +
                                                    " from registry " + describe(uddiRegistry) + ".");
                                            continue;
                                        }
                                    }else{
                                        logger.log(Level.WARNING, "Received notification for service for which we had no persisted runtime " +
                                                "information. BusinessService: " + serviceControl.getUddiServiceKey() +
                                                " from registry " + describe(uddiRegistry) + ". Related PublishedService #(" + serviceControl.getPublishedServiceGoid()+").");
                                        continue;
                                    }
                                }

                                //Now we start processing the update
                                UDDIUtilities.UDDIBindingImplementionInfo bindingImplInfo = null;
                                try {
                                    bindingImplInfo = UDDIUtilities.getUDDIBindingImplInfo(uddiClient,
                                            serviceControl.getUddiServiceKey(), serviceControl.getWsdlPortName(), serviceControl.getWsdlPortBinding(), serviceControl.getWsdlPortBindingNamespace());
                                } catch (UDDIException e) {
                                    context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_PROCESSING_FAILED, ExceptionUtils.getDebugException(e),
                                            "Could not find a UDDI bindingTemplate that implements wsdl:binding "
                                                    + serviceControl.getWsdlPortBinding() + " for serviceKey " +
                                                    "#(" + serviceControl.getUddiServiceKey() + "). " + ExceptionUtils.getMessage(e));
                                }

                                if (bindingImplInfo == null) {
                                    context.logAndAudit(
                                            SystemMessages.UDDI_NOTIFICATION_ENDPOINT_NOT_FOUND,
                                            serviceKey,
                                            serviceControl.getWsdlPortName(),
                                            serviceControl.getWsdlPortBinding(),
                                            (serviceControl.getWsdlPortBindingNamespace() != null) ? "namespace '" + serviceControl.getWsdlPortBindingNamespace() + "'" : "",
                                            describe(uddiRegistry));
                                    processInvalidOriginalService(factory, context, uddiRegistry, serviceControl.getGoid(), ps.getGoid());
                                    continue;
                                }

                                //only want changes to the accessPoint
                                //we do this regardless of configuration. UDDI is the authoritative source of info for the endPoint
                                boolean serviceUpdated = false;
                                //update serviceControl in case it has been updated
                                serviceControl = factory.uddiServiceControlManager.findByPublishedServiceGoid(serviceControl.getPublishedServiceGoid());
                                if (serviceControl == null) {
                                    continue;
                                }

                                final String endPoint;
                                try {
                                    endPoint = getUpdatedEndPoint(factory, bindingImplInfo, serviceControl, monitorRuntime.getAccessPointURL(), context, uddiRegistry, ps, forceUpdate);
                                } catch (UDDIHandledTaskException e) {
                                    caughtExceptions.add(e);
                                    continue;
                                }

                                if (endPoint != null && (monitorRuntime.getAccessPointURL() == null || !monitorRuntime.getAccessPointURL().equals(endPoint))) {
                                    //now we can update our records
                                    //if we need to extract a url from the wsdl see UDDIUtilities.extractEndPointFromWsdl
                                    serviceUpdated = true;
                                    ps.setDefaultRoutingUrl(endPoint);
                                    monitorRuntime.setAccessPointURL(endPoint);
                                    factory.uddiServiceControlRuntimeManager.update(monitorRuntime);
                                    context.logAndAudit(
                                            SystemMessages.UDDI_NOTIFICATION_ENDPOINT_UPDATED,
                                            String.valueOf(ps.getGoid()),
                                            endPoint,
                                            serviceKey,
                                            serviceControl.getWsdlPortName(),
                                            describe(uddiRegistry));
                                }

                                //do we need to update our serviceControl record?
                                if (!bindingImplInfo.getImplementingWsdlPort().equals(serviceControl.getWsdlPortName())) {
                                    factory.uddiServiceControlManager.update(serviceControl);
                                    serviceControl.setWsdlPortName(bindingImplInfo.getImplementingWsdlPort());
                                }

                                //if we didn't accept the endpoint above, we can still update our wsdl, which makes all available
                                //endpoints available on the client - manually
                                if (serviceControl.isUpdateWsdlOnChange() || serviceControl.isDisableServiceOnChange() || forceUpdate) {
                                    // Validate url
                                    String wsdlUrlStr = bindingImplInfo.getImplementingWsdlUrl();
                                    try {
                                        new URL(wsdlUrlStr);
                                    } catch (MalformedURLException e) {
                                        final String msg = "WSDL URL obtained from UDDI is not a valid URL (" +
                                                bindingImplInfo.getImplementingWsdlUrl() + ") for serviceKey: " +
                                                serviceControl.getUddiServiceKey() + " from registry " + describe(uddiRegistry) + ".";
                                        context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_PROCESSING_FAILED, ExceptionUtils.getDebugException(e),
                                                msg);
                                        throw new UDDITaskException(msg);
                                    }

                                    // Fetch wsdl
                                    final int maxSize = factory.config.getIntProperty( ServerConfigParams.PARAM_DOCUMENT_DOWNLOAD_MAXSIZE, HttpObjectCache.DEFAULT_DOWNLOAD_LIMIT);
                                    final SimpleHttpClient httpClient = new SimpleHttpClient(factory.httpClientFactory.createHttpClient(), maxSize);
                                    try {
                                        final RemoteEntityResolver resolver = new RemoteEntityResolver(httpClient, ps.getWsdlUrl());
                                        final DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
                                        final Map<String, String> contents = processor.processDocument(wsdlUrlStr, new DocumentReferenceProcessor.ResourceResolver() {
                                            @Override
                                            public String resolve(final String resourceUrl) throws IOException {
                                                String content = resolver.fetchResourceFromUrl(resourceUrl);
                                                return ResourceTrackingWSDLLocator.processResource(resourceUrl, content, resolver, false, true);
                                            }
                                        });

                                        final Collection<ResourceTrackingWSDLLocator.WSDLResource> sourceDocs =
                                                ResourceTrackingWSDLLocator.toWSDLResources(wsdlUrlStr, contents, false, false, false);

                                        final List<ServiceDocument> serviceDocuments = ServiceDocumentWsdlStrategy.fromWsdlResources(sourceDocs);

                                        // Check if WSDL has changed
                                        Collection<ServiceDocument> existingServiceDocuments = factory.serviceDocumentManager.findByServiceId(ps.getGoid());
                                        Wsdl wsdl = ServiceDocumentWsdlStrategy.parseWsdl(ps, existingServiceDocuments);
                                        Wsdl newWsdl = ServiceDocumentWsdlStrategy.parseWsdl(wsdlUrlStr, contents.get(wsdlUrlStr), existingServiceDocuments);

                                        if (!wsdl.getHash().equals(newWsdl.getHash())) {
                                            serviceUpdated = true;

                                            // Disable if so configured
                                            if (serviceControl.isDisableServiceOnChange()) {
                                                ps.setDisabled(true);
                                                context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_SERVICE_DISABLED, ps.getId());
                                            }

                                            if (serviceControl.isUpdateWsdlOnChange() || forceUpdate) {
                                                //if we need to extract a url from the wsdl see UDDIUtilities.extractEndPointFromWsdl
                                                ps.setWsdlUrl(resolver.addAuthentication(wsdlUrlStr));
                                                ps.setWsdlXml(contents.get(wsdlUrlStr));
                                                for (ServiceDocument serviceDocument : existingServiceDocuments) {
                                                    factory.serviceDocumentManager.delete(serviceDocument);
                                                }
                                                for (ServiceDocument serviceDocument : serviceDocuments) {
                                                    serviceDocument.setServiceId(ps.getGoid());
                                                    factory.serviceDocumentManager.save(serviceDocument);
                                                }

                                                context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_SERVICE_WSDL_UPDATE, ps.displayName() + " (#" + ps.getGoid() + ")");
                                            }
                                        } else {
                                            logger.info("Published Service #(" + ps.getGoid() + ") WSDL is not updated as it is already up to date for business service '" + serviceKey + "' for registry " + describe(uddiRegistry) + ".");
                                        }
                                    } catch (IOException ioe) {
                                        context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_SERVICE_WSDL_ERROR, ExceptionUtils.getDebugException(ioe), "Cause '" + ExceptionUtils.getMessage(ioe) + "'", ps.displayName() + " (#" + ps.getGoid() + ")");
                                    } catch (WSDLException we) {
                                        context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_SERVICE_WSDL_ERROR, ExceptionUtils.getDebugException(we), "Cause '" + ExceptionUtils.getMessage(we) + "'", ps.displayName() + " (#" + ps.getGoid() + ")");
                                    }
                                }

                                if (serviceUpdated) {
                                    factory.serviceManager.update(ps);
                                }

                                monitorRuntime.setLastUDDIModifiedTimeStamp(uddiModifiedTime);
                                factory.uddiServiceControlRuntimeManager.update(monitorRuntime);
                            }
                        } finally {
                            ResourceUtils.closeQuietly( uddiClient );
                        }
                        if(!caughtExceptions.isEmpty()){
                            throw new UDDIHandledTaskException(){
                                @Override
                                public void handleTaskError() {
                                    for (UDDIHandledTaskException caughtException : caughtExceptions) {
                                        caughtException.handleTaskError();
                                    }
                                }
                            };
                        }
                    }
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_NOTIFICATION_PROCESSING_FAILED, e,
                        "Database error when processing notification for registry #"+registryGoid+" and serviceKey: " + serviceKey+".");
            }
        }

        private void processInvalidOriginalService(final SubscriptionUDDITaskFactory factory,
                                                   final UDDITaskContext context,
                                                   final UDDIRegistry uddiRegistry,
                                                   final Goid serviceControlGoid,
                                                   final Goid serviceGoid) throws UpdateException, FindException {
            //set defaultRoutingURL to be empty
            final PublishedService ps = factory.serviceManager.findByPrimaryKey(serviceGoid);
            if(ps == null) return;

            ps.setDefaultRoutingUrl(null);
            factory.serviceManager.update(ps);
            logger.log(Level.INFO, "Cleared context variable ${service.defaultRoutingURL} of UDDI endpoint value for published service #(" + String.valueOf(ps.getGoid()) + ").");

            final UDDIServiceControl serviceControl = factory.uddiServiceControlManager.findByPrimaryKey(serviceControlGoid);
            if(serviceControl != null) {
                context.logAndAudit(SystemMessages.UDDI_ORIGINAL_SERVICE_INVALIDATED, serviceKey, describe(uddiRegistry), String.valueOf(ps.getGoid()) );
                serviceControl.setUnderUddiControl(false);
                serviceControl.setMonitoringEnabled(false);
                factory.uddiServiceControlManager.update(serviceControl);
            }
        }

        private void disableService(UDDITaskContext context, UDDIServiceControl serviceControl) throws FindException, UpdateException {
            final PublishedService ps = factory.serviceManager.findByPrimaryKey(serviceControl.getPublishedServiceGoid());
            ps.setDisabled(true);
            factory.serviceManager.update(ps);
            context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_SERVICE_DISABLED, ps.getGoid().toString());
        }

        /**
         * Get the endpoint from UDDI for this update. All auditing, logging and throwing is done here.
         *
         * @param existingEndPoint String Must not be null.
         * @return String endpoint. Null if not found. Never the empty string or only spaces.
         */
        private String getUpdatedEndPoint(final SubscriptionUDDITaskFactory factory,
                                          final UDDIUtilities.UDDIBindingImplementionInfo bindingImplInfo,
                                          final UDDIServiceControl serviceControl,
                                          final String existingEndPoint,
                                          final UDDITaskContext context,
                                          final UDDIRegistry uddiRegistry,
                                          final PublishedService ps,
                                          final boolean forceUpdate) throws UDDIHandledTaskException {

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

            if(!endPointValid){
                logger.log(Level.INFO, "accessPoint element in monitored bindingTemplate did not contain any value for an endpoint URL.");
                return null;
            }

            if(!existingEndPoint.equals(endPoint)){
                //check if it's a gateway endpoint
                if(factory.uddiHelper.isGatewayUrl(endPoint)){
                    final String msg = "Problem wih found endpoint from Original Business Service's " +
                            "bindingTemplate for wsdl:port '"+serviceControl.getWsdlPortName()+"' in UDDI for service" +
                            " '"+serviceControl.getUddiServiceKey()+"' in UDDIRegistry "+describe(uddiRegistry)+
                            " for Published Service #("+ps.getGoid()+"), it routes back to the Gateway: '" + endPoint + "'.";
                    if (!forceUpdate) {
                        context.logAndAudit(SystemMessages.UDDI_NOTIFICATION_PROCESSING_FAILED, msg);
                    } else {
                        logger.log(Level.WARNING, msg);
                    }

                    throw new UDDIHandledTaskException() {
                        @Override
                        public void handleTaskError() {
                            try {
                                processInvalidOriginalService(factory, context, uddiRegistry, serviceControl.getGoid(), ps.getGoid());
                            } catch (UpdateException e) {
                                context.logAndAudit(SystemMessages.DATABASE_ERROR, e,
                                        "Database error updating Published Service / UDDIServiceControl.");
                            } catch (FindException e) {
                                context.logAndAudit(SystemMessages.DATABASE_ERROR, e,
                                        "Database error finding Published Service / UDDIServiceControl.");
                            }
                        }
                    };
                }
            }

            return endPoint;
        }
    }

    private static final class RemoteEntityResolver implements EntityResolver {
        final WsdlEntityResolver catalogResolver = new WsdlEntityResolver(true);
        final SimpleHttpClient client;
        final Pair<String,Integer> authAddr;
        final PasswordAuthentication auth;

        RemoteEntityResolver( final SimpleHttpClient client, final String authUrl ) {
            this.client = client;
            this.auth = ResourceUtils.getPasswordAuthentication( authUrl );
            if ( this.auth != null ) {
                this.authAddr = getAddr( authUrl );
            } else {
                this.authAddr = null;
            }
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

        public String addAuthentication( final String resourceUrl ) {
            String authUrl = resourceUrl;

            if ( authAddr != null && authAddr.equals( getAddr(resourceUrl) )) {
                authUrl = ResourceUtils.addPasswordAuthentication( authUrl, auth );
            }

            return authUrl;
        }

        private String fetchResourceFromUrl( final String resourceUrl ) throws IOException {
            String resource = null;

            final boolean isHttp = resourceUrl.toLowerCase().startsWith("http:");
            final boolean isHttps = resourceUrl.toLowerCase().startsWith("https:");
            if ( isHttp || isHttps ) {
                URL url = new URL( resourceUrl );
                GenericHttpRequestParams params = new GenericHttpRequestParams( url );
                if ( authAddr != null && authAddr.equals( getAddr(resourceUrl) )) {
                    params.setPasswordAuthentication( auth );
                    params.setPreemptiveAuthentication( true );
                }
                SimpleHttpClient.SimpleHttpResponse response = client.get( params );
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
        
        private Pair<String, Integer> getAddr( final String authUrl ) {
            Pair<String, Integer> addr = null;

            try {
                URL url = new URL( authUrl );
                String host = url.getHost();
                int port = url.getPort();
                if ( port == -1 ) {
                    if ( authUrl.toLowerCase().startsWith("http:") ) {
                        port = 80;
                    } else if ( authUrl.toLowerCase().startsWith("https:") ) {
                        port = 443;
                    }
                }

                if ( host != null && port > -1  ) {
                    addr = new Pair<String, Integer>( host.toLowerCase(), port );
                }
            } catch (MalformedURLException e) {
                // no creds
            }

            return addr;
        }        
    }
}
