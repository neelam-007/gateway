package com.l7tech.server.uddi;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.SyspropUtil;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
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
                                        final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager,
                                        final UDDIServiceControlManager uddiServiceControlManager ) {
        this.uddiRegistryManager = uddiRegistryManager;
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
        }

        return task;
    }

    //- PRIVATE

    private static final long SUBSCRIPTION_EXPIRY_INTERVAL = SyspropUtil.getLong( "com.l7tech.server.uddi.subscriptionExpiryInterval", TimeUnit.DAYS.toMillis( 1 ) );
    private static final long SUBSCRIPTION_RENEW_THRESHOLD = SyspropUtil.getLong( "com.l7tech.server.uddi.subscriptionRenewThreshold", TimeUnit.HOURS.toMillis( 12 ) );

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIRegistrySubscriptionManager uddiRegistrySubscriptionManager;
    private final UDDIServiceControlManager uddiServiceControlManager;

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
