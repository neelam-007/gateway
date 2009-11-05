package com.l7tech.server.uddi;

import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.util.Pair;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

/**
 * The UDDICoordinator handles events and task processing for UDDI
 */
public class UDDICoordinator implements ApplicationListener, InitializingBean {

    //- PUBLIC

    public UDDICoordinator( final PlatformTransactionManager transactionManager,
                            final UDDIHelper uddiHelper,
                            final UDDIRegistryManager uddiRegistryManager,
                            final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                            final UDDIServiceControlManager uddiServiceControlManager,
                            final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager,
                            final ServiceCache serviceCache,
                            final Timer timer,
                            final Collection<UDDITaskFactory> taskFactories ) {
        this.transactionManager = transactionManager;
        this.uddiHelper = uddiHelper;
        this.uddiRegistryManager = uddiRegistryManager;
        this.serviceCache = serviceCache;
        this.timer = timer;
        this.taskFactories = taskFactories;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiBusinessServiceStatusManager = uddiBusinessServiceStatusManager;
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
                task.apply( taskContext );
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
            } else if ( UDDIProxiedServiceInfo.class.equals(entityInvalidationEvent.getEntityClass()) ) {
                long[] entityIds = entityInvalidationEvent.getEntityIds();
                char[] entityOps = entityInvalidationEvent.getEntityOperations();

                for ( int i=0; i<entityIds.length; i++ ) {
                    long id = entityIds[i];
                    char op = entityOps[i];
                    if ( EntityInvalidationEvent.CREATE == op ) {
                        UDDIEvent uddiEvent = new PublishUDDIEvent(PublishUDDIEvent.Type.CREATE_PROXY, id);
                        notifyEvent(uddiEvent);
                        logger.log(Level.INFO, "Created event to publish service WSDL to UDDI");
                    }
                }

                timer.schedule( new BusinessServiceStatusTimerTask(this), 0 );
            } else if ( UDDIServiceControl.class.equals(entityInvalidationEvent.getEntityClass())) {
                timer.schedule( new BusinessServiceStatusTimerTask(this), 0 );
            }

        } else if ( applicationEvent instanceof ReadyForMessages ) {
            checkSubscriptions( Collections.<Long,UDDIRegistryRuntime>emptyMap(), registryRuntimes.get() );    
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( UDDICoordinator.class.getName() );

    private static final String SUBSCRIPTION_SERVICE_WSDL = "file://__ssginternal/uddi_subr_v3_service.wsdl";
    private static final long METRICS_CLEANUP_INTERVAL = SyspropUtil.getLong( "com.l7tech.server.uddi.metricsCleanupInterval", TimeUnit.MINUTES.toMillis(1) );

    private final PlatformTransactionManager transactionManager;
    private final UDDIHelper uddiHelper;
    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager;
    private final UDDIServiceControlManager uddiServiceControlManager;
    private final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager;
    private final ServiceCache serviceCache;
    private final Collection<UDDITaskFactory> taskFactories;
    private final Timer timer;
    private final UDDITaskFactory.UDDITaskContext taskContext;
    private final AtomicReference<Map<Long,UDDIRegistryRuntime>> registryRuntimes = // includes disabled registries
            new AtomicReference<Map<Long,UDDIRegistryRuntime>>( Collections.<Long,UDDIRegistryRuntime>emptyMap() );

    private PublishedService getServiceForHandlingUDDINotifications( final long registryOid ) {
        PublishedService notificationService = null;
        long notificationServiceOid = Long.MAX_VALUE; // find the matching service with the lowest oid

        for( PublishedService service : serviceCache.getInternalServices( )) {
            if ( SUBSCRIPTION_SERVICE_WSDL.equals(service.getWsdlUrl()) ) {
                UDDIProxiedServiceInfo uddiService = null;
                try {
                    uddiService = uddiProxiedServiceInfoManager.findByPublishedServiceOid( service.getOid() );
                } catch (FindException e) {
                    logger.log( Level.WARNING, "Error finding uddi proxied service", e );
                }
                boolean registryMatch = uddiService != null && uddiService.getUddiRegistryOid() == registryOid;

                if ( service.getOid() < notificationServiceOid && registryMatch ) {
                    notificationService = service;
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

        PublishedService service = getServiceForHandlingUDDINotifications( registryOid );
        if ( service != null ) {
            notificationUrl = uddiHelper.getExternalUrlForService( service.getOid() ); //TODO Add option to get secure URL once we publish SSL endpoint?
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
            PublishedService service = getServiceForHandlingUDDINotifications( registryOid );
            if ( service != null ) {
                try {
                    UDDIClient uddiClient = urr.getUDDIClient();
                    UDDIProxiedServiceInfo serviceInfo = uddiProxiedServiceInfoManager.findByPublishedServiceOid(service.getOid());
                    Set<UDDIProxiedService> proxiedServices = serviceInfo.getProxiedServices();
                    Set<String> serviceKeys = new HashSet<String>();
                    for(UDDIProxiedService ps: proxiedServices){
                        serviceKeys.add(ps.getUddiServiceKey());
                    }
                    List<BusinessService> services = uddiClient.getBusinessServices( serviceKeys );
                    if ( services.size() == 1 ) {
                        String serviceKey = services.get( 0 ).getServiceKey();
                        bindingKey = uddiClient.getBindingKeyForService( serviceKey );
                    } else {
                        logger.warning( "Could not find unique business service for notification listener service." );
                    }
                } catch (UDDIException e) {
                    logger.log( Level.WARNING,
                            "Could not get binding key for service from registry '"+urr.getDescription()+"': " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException( e ));
                } catch (FindException e) {
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

    /**
     * Ensures that the buiness service status reflects the current configuration.
     */
    private void checkBusinessServiceStatus() throws ObjectModelException {
        final Collection<UDDIProxiedServiceInfo> proxiedServices = uddiProxiedServiceInfoManager.findAll();
        final Collection<UDDIServiceControl> origServices = uddiServiceControlManager.findAll();
        final Collection<UDDIBusinessServiceStatus> status = uddiBusinessServiceStatusManager.findAll();

        // Build key set
        final Map<Pair<Long,String>,UDDIBusinessServiceStatus> registryServiceMap = new HashMap<Pair<Long,String>,UDDIBusinessServiceStatus>();
        for ( UDDIProxiedServiceInfo proxiedServiceInfo : proxiedServices ) {
            if ( proxiedServiceInfo.getProxiedServices() != null ) {
                for ( UDDIProxiedService proxiedService : proxiedServiceInfo.getProxiedServices() ) {
                    if ( proxiedService.getUddiServiceKey() != null ) {
                        registryServiceMap.put( new Pair<Long,String>( proxiedServiceInfo.getUddiRegistryOid(), proxiedService.getUddiServiceKey() ), null );
                    }
                }
            }
        }
        for ( UDDIServiceControl origService : origServices ) {
            registryServiceMap.put( new Pair<Long,String>( origService.getUddiRegistryOid(), origService.getUddiServiceKey() ), null );
        }

        // Delete stale entries
        final Set<Pair<Long,String>> registryServiceKeys = registryServiceMap.keySet();
        for ( UDDIBusinessServiceStatus serviceStatus : status ) {
            Pair<Long,String> key = new Pair<Long,String>( serviceStatus.getUddiRegistryOid(), serviceStatus.getUddiServiceKey() );
            if ( !registryServiceKeys.contains(key) ) {
                logger.info( "Deleting business service status for " + key + "." );
                uddiBusinessServiceStatusManager.delete( serviceStatus );
            } else {
                registryServiceMap.put( key, serviceStatus );
            }
        }

        // Update / create service status
        for ( UDDIProxiedServiceInfo proxiedServiceInfo : proxiedServices ) {
            if ( proxiedServiceInfo.getProxiedServices() != null ) {
                final long publishedServiceOid = proxiedServiceInfo.getPublishedServiceOid();
                final long uddiRegistryOid = proxiedServiceInfo.getUddiRegistryOid();
                final boolean isMetricsEnabled = proxiedServiceInfo.isMetricsEnabled();

                for ( UDDIProxiedService proxiedService : proxiedServiceInfo.getProxiedServices() ) {
                    if ( proxiedService.getUddiServiceKey() != null ) {
                        final String serviceKey = proxiedService.getUddiServiceKey();
                        final String serviceName = proxiedService.getUddiServiceName();

                        updateUDDIBusinessServiceStatus(
                                registryServiceMap,
                                publishedServiceOid,
                                uddiRegistryOid,
                                serviceKey,
                                serviceName,
                                isMetricsEnabled );
                    }
                }
            }
        }
        for ( UDDIServiceControl origService : origServices ) {
            final long publishedServiceOid = origService.getPublishedServiceOid();
            final long uddiRegistryOid = origService.getUddiRegistryOid();
            final String serviceKey = origService.getUddiServiceKey();
            final String serviceName = origService.getUddiServiceName();
            final boolean isMetricsEnabled = origService.isMetricsEnabled();

            updateUDDIBusinessServiceStatus(
                    registryServiceMap,
                    publishedServiceOid,
                    uddiRegistryOid,
                    serviceKey,
                    serviceName,
                    isMetricsEnabled );
        }
    }

    private void updateUDDIBusinessServiceStatus( final Map<Pair<Long,String>,UDDIBusinessServiceStatus> registryServiceMap,
                                                  final long publishedServiceOid,
                                                  final long uddiRegistryOid,
                                                  final String serviceKey,
                                                  final String serviceName,
                                                  final boolean metricsEnabled ) throws SaveException, UpdateException {
        final Pair<Long,String> key = new Pair<Long,String>( uddiRegistryOid, serviceKey );

        boolean updated = false;

        UDDIBusinessServiceStatus serviceStatus = registryServiceMap.get( key );
        if ( serviceStatus == null ) {
            updated = true;
            serviceStatus = buildUDDIBusinessServiceStatus(
                    publishedServiceOid,
                    uddiRegistryOid,
                    serviceKey,
                    serviceName );
        }

        if ( updateMetricsStatus( serviceStatus, metricsEnabled ) ) {
            updated = true;
        }

        if ( updated ) {
            if ( serviceStatus.getOid() == UDDIBusinessServiceStatus.DEFAULT_OID ) {
                logger.info( "Creating business service status for " + key + "." );
                uddiBusinessServiceStatusManager.save( serviceStatus );
            } else {
                logger.info( "Deleting business service status for " + key + "." );
                uddiBusinessServiceStatusManager.update( serviceStatus );
            }
        }
    }

    private boolean updateMetricsStatus( final UDDIBusinessServiceStatus serviceStatus, final boolean metricsEnabled ) {
        boolean updated = false;

        if ( metricsEnabled ) {
            if ( serviceStatus.getUddiMetricsReferenceStatus() != UDDIBusinessServiceStatus.Status.PUBLISHED &&
                 serviceStatus.getUddiMetricsReferenceStatus() != UDDIBusinessServiceStatus.Status.PUBLISH ) {
                updated = true;
                serviceStatus.setUddiMetricsReferenceStatus( UDDIBusinessServiceStatus.Status.PUBLISH );
            }
        } else {
            if ( serviceStatus.getUddiMetricsReferenceStatus() == UDDIBusinessServiceStatus.Status.PUBLISHED ) {
                updated = true;
                serviceStatus.setUddiMetricsReferenceStatus( UDDIBusinessServiceStatus.Status.DELETE );
            } else if ( serviceStatus.getUddiMetricsReferenceStatus() == UDDIBusinessServiceStatus.Status.PUBLISH ) {
                updated = true;
                serviceStatus.setUddiMetricsReferenceStatus( UDDIBusinessServiceStatus.Status.NONE );
            }
        }
        
        return updated;
    }

    private UDDIBusinessServiceStatus buildUDDIBusinessServiceStatus( final long publishedServiceOid,
                                                                      final long uddiRegistryOid,
                                                                      final String serviceKey,
                                                                      final String serviceName ) {
        UDDIBusinessServiceStatus serviceStatus = new UDDIBusinessServiceStatus();
        serviceStatus.setPublishedServiceOid( publishedServiceOid );
        serviceStatus.setUddiRegistryOid( uddiRegistryOid );
        serviceStatus.setUddiServiceKey( serviceKey );
        serviceStatus.setUddiServiceName( serviceName );
        serviceStatus.setUddiMetricsReferenceStatus( UDDIBusinessServiceStatus.Status.NONE );
        serviceStatus.setUddiPolicyStatus( UDDIBusinessServiceStatus.Status.NONE );
        return serviceStatus;
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
                    TimerUDDIEvent publishEvent = new TimerUDDIEvent( registry.getOid(), TimerUDDIEvent.Type.METRICS_PUBLISH );
                    tasks.add( new Pair<Long,TimerTask>(
                            registry.getMetricPublishFrequency(),
                            new UDDIEventTimerTask( coordinator, publishEvent ) ) );

                    TimerUDDIEvent cleanupEvent = new TimerUDDIEvent( registry.getOid(), TimerUDDIEvent.Type.METRICS_CLEANUP );
                    tasks.add( new Pair<Long,TimerTask>(
                            METRICS_CLEANUP_INTERVAL,
                            new UDDIEventTimerTask( coordinator, cleanupEvent ) ) );
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
            new TransactionTemplate(coordinator.transactionManager).execute( new TransactionCallbackWithoutResult(){
                @Override
                protected void doInTransactionWithoutResult( final TransactionStatus transactionStatus ) {
                    try {
                        coordinator.handleEvent( event ); //TODO [steve] ignore events on all but primary node?
                    } catch (UDDIException e) {
                        logger.log( Level.WARNING, "Error processing UDDI event", e );
                        transactionStatus.setRollbackOnly();
                    }
                }
            } );
        }
    }

    private static final class BusinessServiceStatusTimerTask extends ManagedTimerTask {
        private final UDDICoordinator coordinator;

        BusinessServiceStatusTimerTask( final UDDICoordinator coordinator  ) {
            this.coordinator = coordinator;
        }

        @Override
        public void doRun() {
            new TransactionTemplate(coordinator.transactionManager).execute( new TransactionCallbackWithoutResult(){
                @Override
                protected void doInTransactionWithoutResult( final TransactionStatus transactionStatus ) {
                    try {
                        coordinator.checkBusinessServiceStatus();
                    } catch (ObjectModelException ome) {
                        logger.log( Level.WARNING, "Error updating business services status.", ome );
                        transactionStatus.setRollbackOnly();
                    }
                }
            } );
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
