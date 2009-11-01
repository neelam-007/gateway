package com.l7tech.server.uddi;

import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.util.Pair;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.beans.factory.InitializingBean;

/**
 * The UDDICoordinator handles events and task processing for UDDI
 *
 * TODO [steve] transactions where appropriate
 */
public class UDDICoordinator implements ApplicationListener, InitializingBean {

    //- PUBLIC

    public UDDICoordinator( final UDDIHelper uddiHelper,
                            final UDDIRegistryManager uddiRegistryManager,
                            final UDDIProxiedServiceManager uddiProxiedServiceManager,
                            final ServiceCache serviceCache,
                            final Timer timer,
                            final Collection<UDDITaskFactory> taskFactories ) {
        this.uddiHelper = uddiHelper;
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiProxiedServiceManager = uddiProxiedServiceManager;
        this.serviceCache = serviceCache;
        this.timer = timer;
        this.taskFactories = taskFactories;
        this.taskContext = new UDDICoordinatorTaskContext( this );
    }

    public boolean isNotificationServiceAvailable( final long registryOid ) {
        return getServiceForHandlingUDDINotifications( registryOid ) != null;            
    }

    /**
     * Notification of a UDDIEvent for asynchronous processing.
     *
     * @param event The event
     */
    public void notifyEvent( final UDDIEvent event ) {
        timer.schedule( new UDDIEventTimerTask( this, event ), 0 );
    }

    /**
     * Notification of a UDDIEvent for immediate processing.
     *
     * @param event The event
     * @throws UDDIException if an error occurs handling the event
     */
    public void handleEvent( final UDDIEvent event ) throws UDDIException {
        for ( UDDITaskFactory taskFactory : taskFactories ) {
            UDDITaskFactory.UDDITask task = taskFactory.buildUDDITask( event );
            if ( task != null ) {
                task.apply( taskContext ); //TODO [steve] revert on failure?
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        loadUDDIRegistries(false);
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent applicationEvent ) {
        if ( applicationEvent instanceof EntityInvalidationEvent ) {
            EntityInvalidationEvent entityInvalidationEvent = (EntityInvalidationEvent) applicationEvent;
            if ( UDDIRegistry.class.equals(entityInvalidationEvent.getEntityClass()) ) {
                loadUDDIRegistries(true);
            }
        } else if ( applicationEvent instanceof ReadyForMessages ) {
            checkSubscriptions( Collections.<Long,UDDIRegistryRuntime>emptyMap(), registryRuntimes.get() );    
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( UDDICoordinator.class.getName() );

    private static final String SUBSCRIPTION_SERVICE_WSDL = "file://__ssginternal/uddi_subr_v3_service.wsdl";

    private final UDDIHelper uddiHelper;
    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIProxiedServiceManager uddiProxiedServiceManager;
    private final ServiceCache serviceCache;
    private final Collection<UDDITaskFactory> taskFactories;
    private final Timer timer;
    private final UDDITaskFactory.UDDITaskContext taskContext;
    private final AtomicReference<Map<Long,UDDIRegistryRuntime>> registryRuntimes = // includes disabled registries
            new AtomicReference<Map<Long,UDDIRegistryRuntime>>( Collections.<Long,UDDIRegistryRuntime>emptyMap() );

    private Pair<String,PublishedService> getServiceForHandlingUDDINotifications( final long registryOid ) {
        Pair<String,PublishedService> notificationService = null;
        long notificationServiceOid = Long.MAX_VALUE; // find the matching service with the lowest oid

        for( PublishedService service : serviceCache.getInternalServices( )) {
            if ( SUBSCRIPTION_SERVICE_WSDL.equals(service.getWsdlUrl()) ) {
                UDDIProxiedService uddiService = null;
                try {
                    uddiService = uddiProxiedServiceManager.findByPublishedServiceOid( service.getOid() );
                } catch (FindException e) {
                    logger.log( Level.WARNING, "Error finding uddi proxied service", e );
                }
                boolean registryMatch = uddiService != null && uddiService.getUddiRegistryOid() == registryOid  && uddiService.getGeneralKeywordServiceIdentifier()!=null;

                if ( service.getOid() < notificationServiceOid && registryMatch ) {
                    notificationService = new Pair<String,PublishedService>(uddiService.getGeneralKeywordServiceIdentifier(),service);
                    notificationServiceOid = service.getOid();
                }
            }
        }

        return notificationService;

    }

    /**
     * Get the URL to use for Subscription Notifications or null if not found.
     */
    private String getSubscriptionNotificationUrl( final long registryOid ) {
        String notificationUrl = null;

        Pair<String,PublishedService> service = getServiceForHandlingUDDINotifications( registryOid );
        if ( service != null ) {
            notificationUrl = uddiHelper.getExternalUrlForService( service.right.getOid() ); //TODO [steve] option to get secure URL?
        }

        return notificationUrl;
    }

    /**
     * Get the binding key to use for Subscription Notifications or null if not found.
     */
    private String getSubscriptionNotificationBindingKey( final long registryOid ) {
        String bindingKey = null;

        UDDIRegistryRuntime urr = registryRuntimes.get().get( registryOid );
        if ( urr != null && urr.registry.isEnabled() ) {
            Pair<String,PublishedService> service = getServiceForHandlingUDDINotifications( registryOid );
            if ( service != null ) {
                //TODO [steve] remove use of generalKeywordServiceIdentifier
                try {
                    UDDIClient uddiClient = urr.getUDDIClient();
                    List<BusinessService> services = uddiClient.findMatchingBusinessServices( service.left );
                    if ( services.size() == 1 ) {
                        String serviceKey = services.get( 0 ).getServiceKey();
                        bindingKey = uddiClient.getBindingKeyForService( serviceKey );
                    } else {
                        logger.warning( "Could not find unique business service for identifier '"+service.left+"'." );
                    }
                } catch (UDDIException e) {
                    logger.log( Level.WARNING,
                            "Could not get binding key for service from registry '"+urr.getDescription()+"': " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException( e ));
                }
            }
        }

        return bindingKey;
    }

    /**
     * Load the current registry settings and configure timer tasks for any
     * registries that need timed events.
     */
    private synchronized void loadUDDIRegistries( final boolean updateSubscriptions ) {
        final Map<Long,UDDIRegistryRuntime> registries = new HashMap<Long,UDDIRegistryRuntime>();

        try {
            for ( UDDIRegistry registry : uddiRegistryManager.findAll() ) {
                registries.put( registry.getOid(), new UDDIRegistryRuntime( this, registry, timer ) );
            }
        } catch (FindException e) {
            logger.log( Level.WARNING, "Error loading UDDI registries.", e );
        }

        Map<Long,UDDIRegistryRuntime> oldRegistries = registryRuntimes.get();
        for ( UDDIRegistryRuntime runtime : oldRegistries.values() ) {
            runtime.dispose();
        }

        if ( updateSubscriptions ) {
            checkSubscriptions( oldRegistries, registries );
        }

        registryRuntimes.set( Collections.unmodifiableMap( registries ) );
        for ( UDDIRegistryRuntime runtime : registries.values() ) {
            runtime.init();        
        }
    }

    private void checkSubscriptions( final Map<Long,UDDIRegistryRuntime> oldRegistries,
                                     final Map<Long,UDDIRegistryRuntime> newRegistries ) {
        for ( long registryOid : newRegistries.keySet() ) {
            final UDDIRegistryRuntime oldurr = oldRegistries.get( registryOid );
            final UDDIRegistryRuntime newurr = newRegistries.get( registryOid );

            if ( newurr.registry.isMonitoringEnabled() && (oldurr==null || !oldurr.registry.isMonitoringEnabled() ||
                 newurr.registry.getMonitoringFrequency() != oldurr.registry.getMonitoringFrequency())) {
                notifyEvent( new SubscribeUDDIEvent( registryOid, SubscribeUDDIEvent.Type.SUBSCRIBE ) );
            } else if ( !newurr.registry.isMonitoringEnabled() && (oldurr!=null && oldurr.registry.isMonitoringEnabled())) {
                notifyEvent( new SubscribeUDDIEvent( registryOid, SubscribeUDDIEvent.Type.UNSUBSCRIBE ) );
            }
        }
    }

    private static final class UDDIRegistryRuntime {
        private final UDDIRegistry registry;
        private final Collection<Pair<Long,TimerTask>> timerTasks;
        private final Timer timer;

        UDDIRegistryRuntime( final UDDICoordinator coordinator,
                             final UDDIRegistry registry,
                             final Timer timer ) {
            this.registry = registry;
            this.timer = timer;
            this.timerTasks = buildTasks( coordinator, registry );
        }

        UDDIClient getUDDIClient() {
            return UDDIHelper.newUDDIClient( registry );
        }

        void init() {
            if ( !timerTasks.isEmpty() ) {
                logger.info( "Scheduling tasks for UDDI registry " + getDescription() + "." );
                for ( Pair<Long,TimerTask> task : timerTasks ) {
                    timer.schedule( task.right, task.left, task.left );
                }
            }
        }

        void dispose() {
            if ( !timerTasks.isEmpty() ) {
                logger.info( "Cancelling tasks for UDDI registry " + getDescription() + "." );
                for ( Pair<Long,TimerTask> task : timerTasks ) {
                    task.right.cancel();
                }
            }
        }

        String getDescription() {
            StringBuilder description = new StringBuilder();
            description.append( registry.getName() );
            description.append( "(#" );
            description.append( registry.getOid() );
            description.append( ',' );
            description.append( registry.getVersion() );
            description.append( ')' );
            return description.toString();
        }

        static Collection<Pair<Long,TimerTask>> buildTasks( final UDDICoordinator coordinator,
                                                            final UDDIRegistry registry ) {
            List<Pair<Long,TimerTask>> tasks = new ArrayList<Pair<Long,TimerTask>>();

            if ( registry.isEnabled() ) {
                if ( registry.isMonitoringEnabled() && !registry.isSubscribeForNotifications() &&
                        validInterval( "subscription poll", registry.getMonitoringFrequency() ) ) {
                    TimerUDDIEvent event = new TimerUDDIEvent( registry.getOid(), TimerUDDIEvent.Type.SUBSCRIPTION_POLL );
                    tasks.add( new Pair<Long,TimerTask>(
                            registry.getMonitoringFrequency(),
                            new UDDIEventTimerTask( coordinator, event ) ) );
                }

                if ( registry.isMetricsEnabled() && validInterval( "metrics publish", registry.getMetricPublishFrequency() ) ) {
                    TimerUDDIEvent event = new TimerUDDIEvent( registry.getOid(), TimerUDDIEvent.Type.METRICS );
                    tasks.add( new Pair<Long,TimerTask>(
                            registry.getMetricPublishFrequency(),
                            new UDDIEventTimerTask( coordinator, event ) ) );
                }
            }

            return Collections.unmodifiableCollection( tasks );
        }

        static boolean validInterval( final String label, final long interval ) {
            boolean valid = true;

            if ( interval < TimeUnit.SECONDS.toMillis(5) || interval > TimeUnit.DAYS.toMillis( 365 ) ) {
                valid = false;
                logger.log( Level.WARNING, "Ignoring invalid configuration for {0}: {1}ms.", new Object[]{label, interval} );                
            }

            return valid;
        }
    }

    private static final class UDDIEventTimerTask extends ManagedTimerTask {
        private final UDDICoordinator coordinator;
        private final UDDIEvent event;

        UDDIEventTimerTask( final UDDICoordinator coordinator, final UDDIEvent event ) {
            this.coordinator = coordinator;
            this.event = event;
        }

        @Override
        public void doRun() {
            try {                //TODO [steve] ignore events on all but primary node?
                coordinator.handleEvent( event );
            } catch (UDDIException e) {
                logger.log( Level.WARNING, "Error processing UDDI event", e );
            }
        }
    }

    private static final class UDDICoordinatorTaskContext implements UDDITaskFactory.UDDITaskContext {
        private final UDDICoordinator coordinator;

        UDDICoordinatorTaskContext( final UDDICoordinator coordinator ) {
            this.coordinator = coordinator;   
        }

        @Override
        public String getSubscriptionNotificationURL( final long registryOid ) {
            return this.coordinator.getSubscriptionNotificationUrl( registryOid );
        }

        @Override
        public String getSubscriptionBindingKey(final long registryOid) {
            return this.coordinator.getSubscriptionNotificationBindingKey( registryOid );
        }

        @Override
        public void notifyEvent( final UDDIEvent event ) {
            this.coordinator.notifyEvent( event );
        }
    }
}
