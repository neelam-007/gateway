package com.l7tech.server.uddi;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.uddi.UDDISubscriptionResults;
import com.l7tech.uddi.UDDISubscriptionResultFactory;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;

/**
 * UDDITaskFactory for subscription tasks
 */
public class SubscriptionUDDITaskFactory extends UDDITaskFactory {

    //- PUBLIC

    public SubscriptionUDDITaskFactory( final UDDIRegistryManager uddiRegistryManager,
                                        final UDDIHelper uddiHelper,
                                        final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                        final UDDIServiceControlManager uddiServiceControlManager ) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiHelper = uddiHelper;
        this.uddiRegistrySubscriptionManager = uddiRegistrySubscriptionManager;
        this.uddiServiceControlManager = uddiServiceControlManager;
    }

    @Override
    public UDDITask buildUDDITask( final UDDIEvent event ) {
        UDDITask task = null;

        if ( event instanceof TimerUDDIEvent ) {
            TimerUDDIEvent timerEvent = (TimerUDDIEvent) event;
            if ( timerEvent.getType() == TimerUDDIEvent.Type.SUBSCRIPTION_POLL ) {
                task = new SubscriptionPollUDDITask(
                        uddiRegistryManager,
                        uddiHelper,
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
                            uddiHelper,
                            uddiRegistrySubscriptionManager,
                            subscribeEvent.getRegistryOid(),
                            subscribeEvent.isExpiredOnly() );
                    break;
                case UNSUBSCRIBE:
                    task = new UnsubscribeUDDITask(
                            uddiRegistryManager,
                            uddiHelper,
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
        }

        return task;
    }

    //- PRIVATE

    private static final long SUBSCRIPTION_EXPIRY_INTERVAL = SyspropUtil.getLong( "com.l7tech.server.uddi.subscriptionExpiryInterval", TimeUnit.DAYS.toMillis( 1 ) );
    private static final long SUBSCRIPTION_RENEW_THRESHOLD = SyspropUtil.getLong( "com.l7tech.server.uddi.subscriptionRenewThreshold", TimeUnit.HOURS.toMillis( 12 ) );

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIHelper uddiHelper;
    private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
    private final UDDIServiceControlManager uddiServiceControlManager;

    private static final class SubscribeUDDITask extends SubscriptionProcessingUDDITask {
        private static final Logger logger = Logger.getLogger( SubscribeUDDITask.class.getName() );

        private final UDDIRegistryManager uddiRegistryManager;
        private final UDDIHelper uddiHelper;
        private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
        private final long registryOid;
        private final boolean expiredOnly;

        public SubscribeUDDITask( final UDDIRegistryManager uddiRegistryManager,
                                  final UDDIHelper uddiHelper,
                                  final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                  final long registryOid,
                                  final boolean expiredOnly ) {
            super( logger, null );
            this.uddiRegistryManager = uddiRegistryManager;
            this.uddiHelper = uddiHelper;
            this.uddiRegistrySubscriptionManager = uddiRegistrySubscriptionManager;
            this.registryOid = registryOid;
            this.expiredOnly = expiredOnly;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            // TODO Renew subscription rather than delete and replace?
            logger.info( "Subscribing to UDDI for registry "+registryOid+"." );
            try {
                UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    UDDIClient uddiClient = uddiHelper.newUDDIClient( uddiRegistry );

                    UDDIRegistrySubscription uddiRegistrySubscription = uddiRegistrySubscriptionManager.findByUDDIRegistryOid( registryOid );
                    if ( uddiRegistrySubscription != null && uddiRegistrySubscription.getSubscriptionKey()!=null ) {
                        if ( expiredOnly && !isExpiring(uddiRegistrySubscription) ) {
                            return;    
                        }

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
                    throw new UDDIException("UDDI registry "+uddiRegistry.getName()+"(#"+registryOid+") is disabled.");
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

        private final UDDIRegistryManager uddiRegistryManager;
        private final UDDIHelper uddiHelper;
        private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
        private final long registryOid;

        public UnsubscribeUDDITask( final UDDIRegistryManager uddiRegistryManager,
                                    final UDDIHelper uddiHelper,
                                    final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                    final long registryOid ) {
            this.uddiRegistryManager = uddiRegistryManager;
            this.uddiHelper = uddiHelper;
            this.uddiRegistrySubscriptionManager = uddiRegistrySubscriptionManager;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            logger.info( "Unsubscribing from UDDI for registry "+registryOid+"." );
            try {
                UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    UDDIClient uddiClient = uddiHelper.newUDDIClient( uddiRegistry );

                    UDDIRegistrySubscription uddiRegistrySubscription = uddiRegistrySubscriptionManager.findByUDDIRegistryOid( registryOid );
                    if ( uddiRegistrySubscription != null ) {
                        final String subscriptionKey = uddiRegistrySubscription.getSubscriptionKey();
                        if ( subscriptionKey != null ) {
                            uddiClient.deleteSubscription( subscriptionKey );
                        } else {
                            logger.log( Level.WARNING, "Missing subscription key for registry "+registryOid+", unsubscription not performed." );
                        }

                        uddiRegistrySubscriptionManager.delete( uddiRegistrySubscription );
                    } else {
                        logger.log( Level.WARNING, "Cannot find subscription information for registry "+registryOid+", unsubscription not performed." );
                    }
                } else if ( uddiRegistry == null ) {
                    logger.log( Level.WARNING, "UDDIRegistry (#"+registryOid+") not found for unsubscription." );
                } else {
                    throw new UDDIException("UDDI registry "+uddiRegistry.getName()+"(#"+registryOid+") is disabled.");
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
                                         final UDDISubscriptionResults results ) throws FindException {
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

            if ( isExpiring( uddiRegistrySubscription ) ) {
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

        private final UDDIRegistryManager uddiRegistryManager;
        private final UDDIHelper uddiHelper;
        private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
        private final long registryOid;

        SubscriptionPollUDDITask( final UDDIRegistryManager uddiRegistryManager,
                                  final UDDIHelper uddiHelper,
                                  final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                  final UDDIServiceControlManager uddiServiceControlManager,
                                  final long registryOid ) {
            super(logger,uddiServiceControlManager);
            this.uddiRegistryManager = uddiRegistryManager;
            this.uddiHelper = uddiHelper;
            this.uddiRegistrySubscriptionManager = uddiRegistrySubscriptionManager;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            logger.fine( "Polling UDDI registry subscriptions." );
            try {
                UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    UDDIClient uddiClient = uddiHelper.newUDDIClient( uddiRegistry );

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
                        context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_POLL_FAILED, "Missing subscription for registry "+uddiRegistry.getName()+"(#"+registryOid+").");
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
        public void apply( final UDDITaskContext context ) {
            logger.fine( "Processing UDDI subscription notification." );

            String subscriptionKey = null;
            try {
                final UDDISubscriptionResults results = UDDISubscriptionResultFactory.buildResults( message );
                subscriptionKey = results.getSubscriptionKey();
                final Collection<UDDIRegistrySubscription> subscriptions = uddiRegistrySubscriptionManager.findBySubscriptionKey( subscriptionKey );

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
                    context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_NOTIFICATION_BADKEY, subscriptionKey );
                }

                if ( subscription != null ) {
                    processSubscriptionResults( context, subscription, results );
                    subscription.setSubscriptionNotifiedTime( results.getEndTime() );
                    uddiRegistrySubscriptionManager.update( subscription );
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_NOTIFICATION_FAILED, e, "Database error when processing subscription for "+subscriptionKey+".");
            } catch (UDDIException ue) {
                context.logAndAudit( SystemMessages.UDDI_SUBSCRIPTION_NOTIFICATION_FAILED, ue, ExceptionUtils.getMessage(ue));
            }
        }
    }
}
