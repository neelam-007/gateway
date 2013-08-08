package com.l7tech.server.uddi;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.objectmodel.*;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.cluster.ClusterMaster;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.UDDISystemEvent;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.uddi.*;
import com.l7tech.util.*;
import com.l7tech.wsdl.Wsdl;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.*;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.core.Ordered;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The UDDICoordinator handles events and task processing for UDDI
 */
public class UDDICoordinator implements ApplicationContextAware, InitializingBean, Ordered {

    //- PUBLIC

    public UDDICoordinator(final PlatformTransactionManager transactionManager,
                           final UDDIHelper uddiHelper,
                           final UDDIRegistryManager uddiRegistryManager,
                           final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                           final UDDIPublishStatusManager uddiPublishStatusManager,
                           final UDDIServiceControlManager uddiServiceControlManager,
                           final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager,
                           final ServiceCache serviceCache,
                           final ClusterMaster clusterMaster,
                           final Timer timer,
                           final Collection<UDDITaskFactory> taskFactories,
                           final Config config,
                           final SessionFactory sessionFactory) {
        this.transactionManager = transactionManager;
        this.uddiHelper = uddiHelper;
        this.uddiRegistryManager = uddiRegistryManager;
        this.serviceCache = serviceCache;
        this.clusterMaster = clusterMaster;
        this.timer = timer;
        this.taskFactories = taskFactories;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiBusinessServiceStatusManager = uddiBusinessServiceStatusManager;
        this.uddiPublishStatusManager = uddiPublishStatusManager;
        this.sessionFactory = sessionFactory;
        this.taskContext = new UDDICoordinatorTaskContext( this, config );
    }

    public boolean isNotificationServiceAvailable( final Goid registryGoid ) {
        return getServiceForHandlingUDDINotifications( registryGoid ) != null;
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
     * <p>It is assumed that there is a current auditing context available for use.</p>
     *
     * @param event The event
     * @throws UDDIException if an error occurs handling the event
     */
    public void handleEvent( final UDDIEvent event ) throws UDDITaskFactory.UDDITaskException {
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

    public void onApplicationEvent( final ApplicationEvent applicationEvent ) {
        if ( applicationEvent instanceof GoidEntityInvalidationEvent ) {
            GoidEntityInvalidationEvent entityInvalidationEvent = (GoidEntityInvalidationEvent) applicationEvent;
            if ( UDDIRegistry.class.equals(entityInvalidationEvent.getEntityClass()) ) {
                loadUDDIRegistries(true);
            } else if (
                    UDDIPublishStatus.class.equals(entityInvalidationEvent.getEntityClass())) {

                Goid[] entityIds = entityInvalidationEvent.getEntityIds();
                char[] entityOps = entityInvalidationEvent.getEntityOperations();

                for ( int i=0; i<entityIds.length; i++ ) {
                    Goid id = entityIds[i];
                    char op = entityOps[i];
                    if(GoidEntityInvalidationEvent.CREATE == op || GoidEntityInvalidationEvent.UPDATE == op){
                        try {
                            final UDDIPublishStatus uddiPublishStatus = uddiPublishStatusManager.findByPrimaryKey(id);
                            if(uddiPublishStatus == null){
                                //this is an expected condition, we get more events than we need
                                return;
                            }

                            final UDDIProxiedServiceInfo uddiProxiedServiceInfo =
                                    uddiProxiedServiceInfoManager.findByPrimaryKey(uddiPublishStatus.getUddiProxiedServiceInfoGoid());

                            final UDDIPublishStatus.PublishStatus status = uddiPublishStatus.getPublishStatus();

                            if (status != UDDIPublishStatus.PublishStatus.PUBLISH &&
                                    status != UDDIPublishStatus.PublishStatus.DELETE) {
                                //this events are managed by the maintenance task
                                return;
                            }

                            final UDDIServiceControl serviceControl =
                                    uddiServiceControlManager.findByPublishedServiceGoid(uddiProxiedServiceInfo.getPublishedServiceGoid());
                            //ok if serviceControl is null
                            notifyPublishEvent(uddiProxiedServiceInfo, uddiPublishStatus, serviceControl);

                        } catch (FindException e) {
                            logger.log(Level.WARNING, "Could not find created UDDIPublishStatus with id #(" + id + ")");
                            return;
                        }
                    }
                }
            } else if ( UDDIProxiedServiceInfo.class.equals(entityInvalidationEvent.getEntityClass()) ) {
                timer.schedule( new BusinessServiceStatusTimerTask(this), 0 );
            } else if ( UDDIServiceControl.class.equals(entityInvalidationEvent.getEntityClass()) ) {
                timer.schedule( new BusinessServiceStatusTimerTask(this), 0 );
            } else if (PublishedService.class.equals(entityInvalidationEvent.getEntityClass())) {
                final boolean publishedServiceUpdate = PublishedService.class.equals(entityInvalidationEvent.getEntityClass());

                Goid[] entityIds = entityInvalidationEvent.getEntityIds();
                char[] entityOps = entityInvalidationEvent.getEntityOperations();

                for ( int i=0; i<entityIds.length; i++ ) {
                    Goid id = entityIds[i];
                    char op = entityOps[i];
                    if(GoidEntityInvalidationEvent.UPDATE == op){
                        final PublishedService service = serviceCache.getCachedService(id);
                        if(service == null) return;
                        final UDDIProxiedServiceInfo uddiProxiedServiceInfo;
                        final UDDIPublishStatus uddiPublishStatus;
                        try {
                            uddiProxiedServiceInfo = uddiProxiedServiceInfoManager.findByPublishedServiceGoid(service.getGoid());
                            if(uddiProxiedServiceInfo == null) return;

                            uddiPublishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoGoid(uddiProxiedServiceInfo.getGoid());
                            if(uddiPublishStatus == null) return;
                        } catch (FindException e) {
                            logger.log(Level.WARNING, "Problem looking up UDDIProxiedServiceInfo with service id #(" + service.getGoid()+") ");
                            return;
                        }

                        if(!uddiProxiedServiceInfo.isUpdateProxyOnLocalChange()) return;

                        if(uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH ||
                                uddiPublishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH_FAILED){
                            return;
                        }
                        timer.schedule( new PublishedServiceWsdlUpdatedTimerTask(this, service.getGoid()), 0 );
                        return;
                    }
                }
            } else if (ClusterProperty.class.equals(entityInvalidationEvent.getEntityClass())){
                final Object source = entityInvalidationEvent.getSource();
                if(source instanceof ClusterProperty){
                    ClusterProperty cp = (ClusterProperty) source;
                    if (cp.getName().equals("cluster.hostname") ||
                            cp.getName().equals("cluster.httpPort") ||
                            cp.getName().equals("cluster.httpsPort")) {
                        timer.schedule(new CheckPublishedEndpointsTimerTask(this), 0);
                    }
                }
            }
        }
        else if ( applicationEvent instanceof ReadyForMessages  ) {
            if (clusterMaster.isMaster()){
                checkSubscriptions( Collections.<Goid,UDDIRegistryRuntime>emptyMap(), registryRuntimes.get(), true );
            }

            final long maintenanceFrequency = ConfigFactory.getLongProperty( PROP_PUBLISH_PROXY_MAINTAIN_FREQUENCY, 900000 );
            timer.schedule(new PublishedProxyMaintenanceTimerTask(this), maintenanceFrequency, maintenanceFrequency);
            timer.schedule(new CheckPublishedEndpointsTimerTask(this), maintenanceFrequency, maintenanceFrequency);
        }
    }

    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
        eventPublisher = applicationContext;
        auditor = new Auditor( this, applicationContext, logger );
        ApplicationEventMulticaster eventMulticaster = applicationContext.getBean( "applicationEventMulticaster", ApplicationEventMulticaster.class );
        eventMulticaster.addApplicationListener( new ApplicationListener(){
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                UDDICoordinator.this.onApplicationEvent( event );
            }
        } );
    }

    @Override
    public int getOrder() {
        return 0;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( UDDICoordinator.class.getName() );

    private static final String SUBSCRIPTION_SERVICE_WSDL = "file://__ssginternal/uddi_subr_v3_service.wsdl";
    private static final long METRICS_CLEANUP_INTERVAL = ConfigFactory.getLongProperty( "com.l7tech.server.uddi.metricsCleanupInterval", TimeUnit.MINUTES.toMillis( 1 ) );
    private static final String PROP_PUBLISH_PROXY_MAINTAIN_FREQUENCY = UDDICoordinator.class.getName() + ".publishedProxyMaintenanceFrequency";

    private final PlatformTransactionManager transactionManager;
    private final UDDIHelper uddiHelper;
    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager;
    private final UDDIPublishStatusManager uddiPublishStatusManager;
    private final UDDIServiceControlManager uddiServiceControlManager;
    private final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager;
    private final ServiceCache serviceCache;
    private final ClusterMaster clusterMaster;
    private final Collection<UDDITaskFactory> taskFactories;
    private final Timer timer;
    private final UDDITaskFactory.UDDITaskContext taskContext;
    private final SessionFactory sessionFactory;
    private final AtomicReference<Map<Goid,UDDIRegistryRuntime>> registryRuntimes = // includes disabled registries
            new AtomicReference<Map<Goid,UDDIRegistryRuntime>>( Collections.<Goid,UDDIRegistryRuntime>emptyMap() );
    private ApplicationEventPublisher eventPublisher;
    private Audit auditor;


    private PublishedService getServiceForHandlingUDDINotifications( final Goid registryGoid ) {
        PublishedService notificationService = null;

        for( PublishedService service : serviceCache.getInternalServices( )) {
            if ( SUBSCRIPTION_SERVICE_WSDL.equals(service.getWsdlUrl()) ) {
                UDDIProxiedServiceInfo uddiService = null;
                try {
                    uddiService = uddiProxiedServiceInfoManager.findByPublishedServiceGoid( service.getGoid() );
                } catch (FindException e) {
                    logger.log( Level.WARNING, "Error finding uddi proxied service", e );
                }
                boolean registryMatch = uddiService != null && Goid.equals(uddiService.getUddiRegistryGoid(), registryGoid);

                if ( registryMatch ) {
                    if(notificationService != null){
                        logger.log( Level.WARNING, "Found multiple services for the same UDDI registry: " + registryGoid );
                    }
                    notificationService = service;
                }
            }
        }

        return notificationService;

    }

    /**
     * Get the URL to use for Subscription Notifications or null if not found.
     */
    private String getSubscriptionNotificationUrl( final Goid registryGoid ) {
        String notificationUrl = null;

        PublishedService service = getServiceForHandlingUDDINotifications( registryGoid );
        if ( service != null ) {
            notificationUrl = uddiHelper.getExternalUrlForService( service.getGoid() ); //TODO Add option to get secure URL once we publish SSL endpoint?
        }

        return notificationUrl;
    }

    /**
     * Get the binding key to use for Subscription Notifications or null if not found.
     */
    private String getSubscriptionNotificationBindingKey( final Goid registryGoid ) {
        String bindingKey = null;

        UDDIRegistryRuntime urr = registryRuntimes.get().get( registryGoid );
        if ( urr != null && urr.registry.isEnabled() ) {
            PublishedService service = getServiceForHandlingUDDINotifications( registryGoid );
            if ( service != null ) {
                UDDIClient uddiClient = null;
                try {
                    uddiClient = urr.getUDDIClient();
                    UDDIProxiedServiceInfo serviceInfo = uddiProxiedServiceInfoManager.findByPublishedServiceGoid(service.getGoid());
                    Set<UDDIProxiedService> proxiedServices = serviceInfo.getProxiedServices();
                    Set<String> serviceKeys = new HashSet<String>();
                    for(UDDIProxiedService ps: proxiedServices){
                        serviceKeys.add(ps.getUddiServiceKey());
                    }
                    List<UDDIBusinessService> services = uddiClient.getUDDIBusinessServices( serviceKeys, false);
                    if ( services.size() == 1 ) {
                        String serviceKey = services.get( 0 ).getServiceKey();
                        bindingKey = uddiClient.getBindingKeyForService( serviceKey, Arrays.asList( "http", "https" ));
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
                } finally {
                    ResourceUtils.closeQuietly( uddiClient );
                }
            }
        }

        return bindingKey;
    }

    /**
     * Load the current registry settings and configure timer tasks for any
     * registries that need timed events.
     */
    private synchronized void loadUDDIRegistries( final boolean doUpdates ) {
        final Map<Goid,UDDIRegistryRuntime> registries = new HashMap<Goid,UDDIRegistryRuntime>();

        try {
            for ( UDDIRegistry registry : uddiRegistryManager.findAll() ) {
                registries.put( registry.getGoid(), new UDDIRegistryRuntime( this, uddiHelper, registry, timer ) );
            }
        } catch (FindException e) {
            logger.log( Level.WARNING, "Error loading UDDI registries.", e );
        }

        Map<Goid,UDDIRegistryRuntime> oldRegistries = registryRuntimes.get();
        for ( UDDIRegistryRuntime runtime : oldRegistries.values() ) {
            runtime.dispose();
        }

        if ( doUpdates && clusterMaster.isMaster() ) {
            checkSubscriptions( oldRegistries, registries, false );

            Collection<Goid> registryGoids = registriesUpdated( oldRegistries, registries );
            if ( !registryGoids.isEmpty() ) {
                timer.schedule( new PublishedProxyMaintenanceTimerTask(this), 0);
                for ( Goid uddiRegistryGoid : registryGoids ) {
                    notifyEvent(new WsPolicyUDDIEvent(uddiRegistryGoid));
                }
            }
        }

        registryRuntimes.set( Collections.unmodifiableMap( registries ) );
        for ( UDDIRegistryRuntime runtime : registries.values() ) {
            runtime.init();        
        }
    }

    private void checkSubscriptions( final Map<Goid,UDDIRegistryRuntime> oldRegistries,
                                     final Map<Goid,UDDIRegistryRuntime> newRegistries,
                                     final boolean checkExpired ) {
        for ( Goid registryGoid : newRegistries.keySet() ) {
            final UDDIRegistryRuntime oldurr = oldRegistries.get( registryGoid );
            final UDDIRegistryRuntime newurr = newRegistries.get( registryGoid );

            if ( newurr.registry.isMonitoringEnabled() &&
                 ( oldurr==null ||
                   !oldurr.registry.isMonitoringEnabled() ||
                   newurr.registry.getMonitoringFrequency() != oldurr.registry.getMonitoringFrequency() ||
                   newurr.registry.isSubscribeForNotifications() != oldurr.registry.isSubscribeForNotifications())) {
                notifyEvent( new SubscribeUDDIEvent( registryGoid, SubscribeUDDIEvent.Type.SUBSCRIBE, checkExpired ) );
            } else if ( !newurr.registry.isMonitoringEnabled() && (oldurr!=null && oldurr.registry.isMonitoringEnabled())) {
                notifyEvent( new SubscribeUDDIEvent( registryGoid, SubscribeUDDIEvent.Type.UNSUBSCRIBE ) );
            }
        }
    }

    /**
     * Check if any existing registry has been updated and is currently enabled, if so we should retry any UDDI updates.
     */
    private Collection<Goid> registriesUpdated( final Map<Goid,UDDIRegistryRuntime> oldr,
                                                final Map<Goid,UDDIRegistryRuntime> newr ) {
        Collection<Goid> goids = new ArrayList<Goid>();

        if ( !oldr.isEmpty() ) {
            for ( Map.Entry<Goid,UDDIRegistryRuntime> registryEntry : newr.entrySet() ) {
                UDDIRegistryRuntime oldRuntime = oldr.get(registryEntry.getKey());
                if ( oldRuntime != null ) {
                    if ( oldRuntime.registry.getVersion() != registryEntry.getValue().registry.getVersion() &&
                         registryEntry.getValue().registry.isEnabled() ) {
                        goids.add( registryEntry.getValue().registry.getGoid() );
                    }
                }
            }
        }

        return goids;
    }

    /**
     * Check if a Published Service needs to be republished to UDDI following a local WSDL change
     *
     * @throws ObjectModelException
     */
    private void checkPublishedServiceWithUpdatedWsdls(final Goid serviceGoid) throws ObjectModelException{
        final PublishedService service = serviceCache.getCachedService(serviceGoid);
        if(service == null) return;

        final UDDIProxiedServiceInfo uddiProxiedServiceInfo = uddiProxiedServiceInfoManager.findByPublishedServiceGoid(service.getGoid());
        if(uddiProxiedServiceInfo == null) return;

        final UDDIPublishStatus uddiPublishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoGoid(uddiProxiedServiceInfo.getGoid());
        if(uddiPublishStatus == null) return;
        
        final String updatedWsdlHash;
        try {
            final Wsdl wsdl = service.parsedWsdl();
            updatedWsdlHash = wsdl.getHash();
        } catch (Exception e) {//WsdlException or IOException
            logger.log(Level.WARNING, "Could not parse the WSDL for published service with id#(" + service.getGoid()+")", ExceptionUtils.getDebugException(e));
            uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.DELETE);
            uddiPublishStatusManager.update(uddiPublishStatus);
            return;
        }

        final String oldHash = uddiProxiedServiceInfo.getWsdlHash();
        if( oldHash == null || oldHash.trim().isEmpty() || !oldHash.equals(updatedWsdlHash)){
            uddiProxiedServiceInfo.setWsdlHash(updatedWsdlHash);
            uddiPublishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISH);
            try {
                uddiPublishStatusManager.update(uddiPublishStatus);
                uddiProxiedServiceInfoManager.update(uddiProxiedServiceInfo);
            } catch (UpdateException e) {
                logger.log(Level.WARNING, "Could not update UDDIProxiedServiceInfo. Cannot trigger UDDI required WSDL update", ExceptionUtils.getDebugException(e));
                throw e;//cause db to rollback
            }
        }
    }

    /**
     * Goes through all UDDIProxiedServiceInfo entities, and creates events to publish for events which are
     * waiting to publish, or those which have either failed to publish or to delete
     *
     * Note: this will retry tasks which are in the 'PUBLISH' state.
     * @throws com.l7tech.objectmodel.ObjectModelException if any db exception happens
     */
    private void fireMaintenancePublishEvents() throws ObjectModelException {
        final Collection<UDDIProxiedServiceInfo> proxiedServices = uddiProxiedServiceInfoManager.findAll();
        final Map<Goid, UDDIProxiedServiceInfo> oidToProxyServiceMap = new HashMap<Goid, UDDIProxiedServiceInfo>();
        for(UDDIProxiedServiceInfo serviceInfo: proxiedServices){
            oidToProxyServiceMap.put(serviceInfo.getGoid(), serviceInfo);
        }

        final Collection<UDDIPublishStatus> allPublishStatus = uddiPublishStatusManager.findAll();

        //move published info through its lifecycle
        for(UDDIPublishStatus publishStatus: allPublishStatus){
            final UDDIPublishStatus.PublishStatus status = publishStatus.getPublishStatus();

            //for every status other than cannot publish and delete, fire the event to ensure the publish status
            //works its way through its life cycle
            if(status != UDDIPublishStatus.PublishStatus.CANNOT_PUBLISH &&
                    status != UDDIPublishStatus.PublishStatus.CANNOT_DELETE &&
                    status != UDDIPublishStatus.PublishStatus.PUBLISHED){
                final UDDIProxiedServiceInfo info = oidToProxyServiceMap.get(publishStatus.getUddiProxiedServiceInfoGoid());
                logger.log(Level.FINE, "Creating event to update published proxy service info in UDDI. Service #("
                        + info.getPublishedServiceGoid()+") in status " + status.toString());
                final UDDIServiceControl serviceControl =
                        uddiServiceControlManager.findByPublishedServiceGoid(info.getPublishedServiceGoid());
                //ok if serviceControl is null

                notifyPublishEvent( info, publishStatus, serviceControl);
            }
        }
    }

    /**
     * See if we need to update UDDI following a change which may have caused the external endpoints for a cluster /
     * gateway to have changed
     * @throws ObjectModelException
     */
    private void checkPublishedEndpoints() throws ObjectModelException{
        final boolean autoRepublish = ConfigFactory.getBooleanProperty("uddi.autorepublish", true);
        if(!autoRepublish) return;

        final Collection<UDDIProxiedServiceInfo> infoCollection = uddiProxiedServiceInfoManager.findAll();
        for (UDDIProxiedServiceInfo serviceInfo : infoCollection) {
            final UDDIPublishStatus publishStatus = uddiPublishStatusManager.findByProxiedSerivceInfoGoid(serviceInfo.getGoid());
            if(publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISHED){

                final Set<EndpointPair> allEndpointPairs;
                final Boolean isGif = serviceInfo.getProperty(UDDIProxiedServiceInfo.IS_GIF);
                if(isGif != null && isGif){
                    UDDIRegistryAdmin.EndpointScheme endpointScheme = serviceInfo.getProperty(UDDIProxiedServiceInfo.GIF_SCHEME);
                    if(endpointScheme == null){
                        //should never happen if invariants of properties are maintained. Putting here to avoid a
                        //null pointer in the off chance the db is corrupted or there is a programming error.
                        logger.log(Level.WARNING, "Could not find endpoint type for GIF published endpoint for " +
                                "published service #("+serviceInfo.getPublishedServiceGoid()+"). ");
                        continue;
                    }
                    final EndpointPair endPointPair;
                    try {
                        endPointPair = uddiHelper.getEndpointForScheme(endpointScheme, serviceInfo.getPublishedServiceGoid());
                    } catch (UDDIHelper.EndpointNotDefinedException e) {
                        auditor.logAndAudit(SystemMessages.UDDI_GIF_SCHEME_NOT_AVAILABLE, endpointScheme.toString(), ExceptionUtils.getMessage(e));
                        continue;
                    }
                    allEndpointPairs = new HashSet<EndpointPair>(Arrays.asList(endPointPair));
                } else {
                    allEndpointPairs = uddiHelper.getAllExternalEndpointAndWsdlUrls(serviceInfo.getPublishedServiceGoid());
                }

                final Set<EndpointPair> persistedEndpoints = serviceInfo.getProperty(UDDIProxiedServiceInfo.ALL_ENDPOINT_PAIRS_KEY);

                if(!allEndpointPairs.equals(persistedEndpoints)){
                    logger.log(Level.INFO, "Setting service #(" + serviceInfo.getPublishedServiceGoid()+") to republish to UDDI following change to external endpoints");
                    publishStatus.setPublishStatus(UDDIPublishStatus.PublishStatus.PUBLISH);
                    uddiPublishStatusManager.update(publishStatus);
                }
            }
        }

        ApplicationEventPublisher publisher = eventPublisher;
        if ( publisher != null ) {
            publisher.publishEvent( new UDDISystemEvent( this, Component.GW_UDDI_SERVICE, UDDISystemEvent.Action.CHECKING_ENDPOINTS ) );
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
        final Map<Triple<Goid,Goid,String>,UDDIBusinessServiceStatus> registryServiceMap = new HashMap<Triple<Goid,Goid,String>,UDDIBusinessServiceStatus>();
        for ( UDDIProxiedServiceInfo proxiedServiceInfo : proxiedServices ) {
            if ( proxiedServiceInfo.getProxiedServices() != null ) {
                for ( UDDIProxiedService proxiedService : proxiedServiceInfo.getProxiedServices() ) {
                    if ( proxiedService.getUddiServiceKey() != null ) {
                        registryServiceMap.put( new Triple<Goid,Goid,String>(
                                proxiedServiceInfo.getUddiRegistryGoid(),
                                proxiedServiceInfo.getPublishedServiceGoid(),
                                proxiedService.getUddiServiceKey() ), null );
                    }
                }
            }
        }
        for ( UDDIServiceControl origService : origServices ) {
            registryServiceMap.put( new Triple<Goid,Goid,String>(
                    origService.getUddiRegistryGoid(),
                    origService.getPublishedServiceGoid(),
                    origService.getUddiServiceKey() ), null );
        }

        final Map<Goid,WsPolicyUDDIEvent> wsPolicyEventMap = new HashMap<Goid,WsPolicyUDDIEvent>(); // one event per registry

        // Delete stale entries
        final Set<Triple<Goid,Goid,String>> registryServiceKeys = registryServiceMap.keySet();
        for ( UDDIBusinessServiceStatus serviceStatus : status ) {
            Triple<Goid,Goid,String> key = new Triple<Goid,Goid,String>( serviceStatus.getUddiRegistryGoid(), serviceStatus.getPublishedServiceGoid(), serviceStatus.getUddiServiceKey() );
            if ( !registryServiceKeys.contains(key) ) {
                if ( serviceStatus.getUddiMetricsReferenceStatus() == UDDIBusinessServiceStatus.Status.NONE &&
                     serviceStatus.getUddiPolicyStatus() == UDDIBusinessServiceStatus.Status.NONE ) {
                    logger.info( "Deleting business service status for " + key + "." );
                    uddiBusinessServiceStatusManager.delete( serviceStatus );
                } else {
                    registryServiceMap.put( key, serviceStatus );
                    updateUDDIBusinessServiceStatus(
                            registryServiceMap,
                            wsPolicyEventMap,
                            serviceStatus.getPublishedServiceGoid(),
                            serviceStatus.getUddiRegistryGoid(),
                            serviceStatus.getUddiServiceKey(),
                            serviceStatus.getUddiServiceName(),
                            false,
                            false,
                            null );
                }
            } else {
                registryServiceMap.put( key, serviceStatus );
            }
        }

        final boolean autoRepublish = ConfigFactory.getBooleanProperty("uddi.autorepublish", true);
        // Update / create service status
        for ( UDDIProxiedServiceInfo proxiedServiceInfo : proxiedServices ) {
            if ( proxiedServiceInfo.getProxiedServices() != null ) {
                final Goid publishedServiceGoid = proxiedServiceInfo.getPublishedServiceGoid();
                final Goid uddiRegistryGoid = proxiedServiceInfo.getUddiRegistryGoid();
                final boolean isMetricsEnabled = proxiedServiceInfo.isMetricsEnabled();
                final boolean isPublishWsPolicyEnabled = proxiedServiceInfo.isPublishWsPolicyEnabled() && autoRepublish;
                final String wsPolicyUrl = uddiHelper.getExternalPolicyUrlForService(
                        proxiedServiceInfo.getPublishedServiceGoid(),
                        proxiedServiceInfo.isPublishWsPolicyFull(),
                        proxiedServiceInfo.isPublishWsPolicyInlined() );

                for ( UDDIProxiedService proxiedService : proxiedServiceInfo.getProxiedServices() ) {
                    if ( proxiedService.getUddiServiceKey() != null ) {
                        final String serviceKey = proxiedService.getUddiServiceKey();
                        final String serviceName = proxiedService.getUddiServiceName();

                        updateUDDIBusinessServiceStatus(
                                registryServiceMap,
                                wsPolicyEventMap,
                                publishedServiceGoid,
                                uddiRegistryGoid,
                                serviceKey,
                                serviceName,
                                isMetricsEnabled,
                                isPublishWsPolicyEnabled,
                                wsPolicyUrl );
                    }
                }
            }
        }
        for ( UDDIServiceControl origService : origServices ) {
            final Goid publishedServiceGoid = origService.getPublishedServiceGoid();
            final Goid uddiRegistryGoid = origService.getUddiRegistryGoid();
            final String serviceKey = origService.getUddiServiceKey();
            final String serviceName = origService.getUddiServiceName();
            final boolean isMetricsEnabled = origService.isMetricsEnabled();
            final boolean isPublishWsPolicyEnabled = origService.isPublishWsPolicyEnabled() && autoRepublish;
            final String wsPolicyUrl = uddiHelper.getExternalPolicyUrlForService(
                    origService.getPublishedServiceGoid(),
                    origService.isPublishWsPolicyFull(),
                    origService.isPublishWsPolicyInlined() );

            updateUDDIBusinessServiceStatus(
                    registryServiceMap,
                    wsPolicyEventMap,
                    publishedServiceGoid,
                    uddiRegistryGoid,
                    serviceKey,
                    serviceName,
                    isMetricsEnabled,
                    isPublishWsPolicyEnabled,
                    wsPolicyUrl );
        }

        for ( UDDIEvent event : wsPolicyEventMap.values() ) {
            notifyEvent( event );
        }
    }

    private void updateUDDIBusinessServiceStatus( final Map<Triple<Goid,Goid,String>,UDDIBusinessServiceStatus> registryServiceMap,
                                                  final Map<Goid,WsPolicyUDDIEvent> wsPolicyEventMap,
                                                  final Goid publishedServiceGoid,
                                                  final Goid uddiRegistryGoid,
                                                  final String serviceKey,
                                                  final String serviceName,
                                                  final boolean metricsEnabled,
                                                  final boolean publishWsPolicyEnabled,
                                                  final String publishWsPolicyUrl ) throws SaveException, UpdateException {
        final Triple<Goid,Goid,String> key = new Triple<Goid,Goid,String>( uddiRegistryGoid, publishedServiceGoid, serviceKey );

        boolean updated = false;

        UDDIBusinessServiceStatus serviceStatus = registryServiceMap.get( key );
        if ( serviceStatus == null ) {
            updated = true;
            serviceStatus = buildUDDIBusinessServiceStatus(
                    publishedServiceGoid,
                    uddiRegistryGoid,
                    serviceKey,
                    serviceName );
        }

        if ( updateMetricsStatus( serviceStatus, metricsEnabled ) ) {
            updated = true;
        }

        if ( updateWsPolicyStatus( serviceStatus, publishWsPolicyEnabled, publishWsPolicyUrl ) ) {
            updated = true;
            wsPolicyEventMap.put( uddiRegistryGoid, new WsPolicyUDDIEvent(uddiRegistryGoid) );
        }

        if ( updated ) {
            if ( Goid.isDefault(serviceStatus.getGoid()) ) {
                logger.info( "Creating business service status for " + key + "." );
                uddiBusinessServiceStatusManager.save( serviceStatus );
            } else {
                logger.info( "Updating business service status for " + key + "." );
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

    private boolean updateWsPolicyStatus( final UDDIBusinessServiceStatus serviceStatus,
                                          final boolean publishWsPolicyEnabled,
                                          final String wsPolicyUrl ) {
        boolean updated = false;

        if ( publishWsPolicyEnabled && wsPolicyUrl != null && !wsPolicyUrl.isEmpty() ) {
            if ( !((serviceStatus.getUddiPolicyStatus() == UDDIBusinessServiceStatus.Status.PUBLISHED &&
                    wsPolicyUrl.equals(serviceStatus.getUddiPolicyUrl())) ||
                   (serviceStatus.getUddiPolicyStatus() != UDDIBusinessServiceStatus.Status.PUBLISH &&
                    wsPolicyUrl.equals(serviceStatus.getUddiPolicyPublishUrl() ) ) ) ) {
                updated = true;
                serviceStatus.setUddiPolicyStatus( UDDIBusinessServiceStatus.Status.PUBLISH );
                serviceStatus.setUddiPolicyPublishUrl( wsPolicyUrl );
            }
        } else {
            if ( serviceStatus.getUddiPolicyStatus() == UDDIBusinessServiceStatus.Status.PUBLISHED ) {
                updated = true;
                serviceStatus.setUddiPolicyStatus( UDDIBusinessServiceStatus.Status.DELETE );
            } else if ( serviceStatus.getUddiPolicyStatus() == UDDIBusinessServiceStatus.Status.PUBLISH ) {
                updated = true;
                serviceStatus.setUddiPolicyStatus( UDDIBusinessServiceStatus.Status.NONE );
                serviceStatus.setUddiPolicyPublishUrl( null );
            }
        }

        return updated;
    }

    private UDDIBusinessServiceStatus buildUDDIBusinessServiceStatus( final Goid publishedServiceGoid,
                                                                      final Goid uddiRegistryGoid,
                                                                      final String serviceKey,
                                                                      final String serviceName ) {
        UDDIBusinessServiceStatus serviceStatus = new UDDIBusinessServiceStatus();
        serviceStatus.setPublishedServiceGoid( publishedServiceGoid );
        serviceStatus.setUddiRegistryGoid( uddiRegistryGoid );
        serviceStatus.setUddiServiceKey( serviceKey );
        serviceStatus.setUddiServiceName( serviceName );
        serviceStatus.setUddiMetricsReferenceStatus( UDDIBusinessServiceStatus.Status.NONE );
        serviceStatus.setUddiPolicyStatus( UDDIBusinessServiceStatus.Status.NONE );
        return serviceStatus;
    }

    /**
     *
     * @param uddiProxiedServiceInfo
     * @param uddiPublishStatus
     * @param serviceControl can be null
     */
    private void notifyPublishEvent(final UDDIProxiedServiceInfo uddiProxiedServiceInfo,
                                    final UDDIPublishStatus uddiPublishStatus,
                                    final UDDIServiceControl serviceControl) {
        //do not log event creation, as more than one event may be created per user interaction, duplicates are ignored later
        final UDDIEvent uddiEvent = new PublishUDDIEvent(uddiProxiedServiceInfo, uddiPublishStatus, serviceControl);
        notifyEvent(uddiEvent);
    }

    private static final class UDDIRegistryRuntime {
        private final UDDIHelper uddiHelper;
        private final UDDIRegistry registry;
        private final Collection<Pair<Long,TimerTask>> timerTasks;
        private final Timer timer;

        UDDIRegistryRuntime( final UDDICoordinator coordinator,
                             final UDDIHelper uddiHelper,
                             final UDDIRegistry registry,
                             final Timer timer ) {
            this.registry = registry;
            this.uddiHelper = uddiHelper;
            this.timer = timer;
            this.timerTasks = buildTasks( coordinator, registry );
        }

        /**
         * Get a new UDDIClient.
         *
         * <p>The caller is responsible for closing the client.</p>
         *
         * @return The UDDIClient
         * @throws com.l7tech.objectmodel.FindException if a password reference cannot be expanded
         */
        UDDIClient getUDDIClient() throws FindException {
            return uddiHelper.newUDDIClient( registry );
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
            description.append( registry.getGoid() );
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
                    TimerUDDIEvent event = new TimerUDDIEvent( registry.getGoid(), TimerUDDIEvent.Type.SUBSCRIPTION_POLL );
                    tasks.add( new Pair<Long,TimerTask>(
                            registry.getMonitoringFrequency(),
                            new UDDIEventTimerTask( coordinator, event ) ) );
                }

                if ( registry.isMetricsEnabled() && validInterval( "metrics publish", registry.getMetricPublishFrequency() ) ) {
                    TimerUDDIEvent publishEvent = new TimerUDDIEvent( registry.getGoid(), TimerUDDIEvent.Type.METRICS_PUBLISH );
                    tasks.add( new Pair<Long,TimerTask>(
                            registry.getMetricPublishFrequency(),
                            new UDDIEventTimerTask( coordinator, publishEvent ) ) );

                    TimerUDDIEvent cleanupEvent = new TimerUDDIEvent( registry.getGoid(), TimerUDDIEvent.Type.METRICS_CLEANUP );
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
            if ( coordinator.clusterMaster.isMaster() || !event.isMasterOnly() ) {
                AuditContextUtils.doAsSystem( new Runnable(){
                    @Override
                    public void run() {
                        final UDDITaskFactory.UDDIHandledTaskException uddiTaskException []= new UDDITaskFactory.UDDIHandledTaskException[1];
                        try {
                            final TransactionTemplate transactionTemplate = new TransactionTemplate(coordinator.transactionManager);
                            transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
                            transactionTemplate.execute( new TransactionCallbackWithoutResult(){
                                @Override
                                protected void doInTransactionWithoutResult( final TransactionStatus transactionStatus ) {
                                    try {
                                        coordinator.handleEvent( event );
                                        //RuntimeExceptions are caught by Spring and cause a rollback
                                    } catch (UDDITaskFactory.UDDITaskException e) {
                                        logger.log( Level.WARNING, "Error processing UDDI event", ExceptionUtils.getDebugException(e) );
                                        transactionStatus.setRollbackOnly();
                                        if(e != null && (e instanceof UDDITaskFactory.UDDIHandledTaskException)){
                                            uddiTaskException[0] = (UDDITaskFactory.UDDIHandledTaskException) e;
                                        }
                                    }
                                }
                            } );
                        } catch ( TransactionException te ) {
                            //te is a RuntimeException
                            logger.log( Level.WARNING, "Error processing transaction for UDDI event.", te );
                        }

                        //Run handleTaskError in a separate transaction so we can update the UDDIPublishStatus correctly
                        if(uddiTaskException[0] != null){
                            try {
                                final TransactionTemplate transactionTemplate = new TransactionTemplate(coordinator.transactionManager);
                                transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
                                transactionTemplate.execute( new TransactionCallbackWithoutResult(){
                                    @Override
                                    protected void doInTransactionWithoutResult( final TransactionStatus transactionStatus ) {
                                        uddiTaskException[0].handleTaskError();
                                    }
                                } );
                            } catch ( TransactionException te ) {
                                //te is a RuntimeException
                                logger.log( Level.WARNING, "Error handling exception caused by UDDI event.", te );
                            }
                        }

                        ApplicationEventPublisher publisher = coordinator.eventPublisher;
                        if ( publisher != null ) {
                            publisher.publishEvent( new UDDISystemEvent( coordinator, Component.GW_UDDI_SERVICE, UDDISystemEvent.Action.REGISTRY_UPDATE ) );
                        }
                    }
                } );
            }
        }
    }

    private static final class BusinessServiceStatusTimerTask extends ManagedTimerTask {
        private final UDDICoordinator coordinator;

        BusinessServiceStatusTimerTask( final UDDICoordinator coordinator  ) {
            this.coordinator = coordinator;
        }

        @Override
        public void doRun() {
            if ( coordinator.clusterMaster.isMaster() ) {
                AuditContextUtils.doAsSystem(new Runnable() {
                    @Override
                    public void run() {
                        new TransactionTemplate(coordinator.transactionManager).execute(new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
                                try {
                                    coordinator.checkBusinessServiceStatus();
                                } catch (ObjectModelException ome) {
                                    logger.log(Level.WARNING, "Error updating business services status.", ome);
                                    transactionStatus.setRollbackOnly();
                                }
                            }
                        });
                    }
                });
            }
        }
    }

    /**
     * Republish any published Business Services and bindingTemplates to UDDI
     */
    private static final class CheckPublishedEndpointsTimerTask extends ManagedTimerTask {
        private final UDDICoordinator coordinator;

        CheckPublishedEndpointsTimerTask(final UDDICoordinator coordinator) {
            this.coordinator = coordinator;
        }

        @Override
        public void doRun() {
            if ( coordinator.clusterMaster.isMaster() ) {
                AuditContextUtils.doAsSystem( new Runnable(){
                    @Override
                    public void run() {
                        new TransactionTemplate(coordinator.transactionManager).execute( new TransactionCallbackWithoutResult(){
                            @Override
                            protected void doInTransactionWithoutResult( final TransactionStatus transactionStatus ) {
                                try {
                                    coordinator.checkPublishedEndpoints();
                                    coordinator.checkBusinessServiceStatus();
                                } catch (ObjectModelException ome) {
                                    logger.log( Level.WARNING, "Error checking published endpoints for local changes", ome );
                                    transactionStatus.setRollbackOnly();
                                }
                            }
                        } );
                    }
                });
            }
        }
    }


    private static final class PublishedProxyMaintenanceTimerTask extends ManagedTimerTask {
        private final UDDICoordinator coordinator;

        PublishedProxyMaintenanceTimerTask( final UDDICoordinator coordinator  ) {
            this.coordinator = coordinator;
        }

        @Override
        public void doRun() {
            if ( coordinator.clusterMaster.isMaster() ) {
                AuditContextUtils.doAsSystem( new Runnable(){
                    @Override
                    public void run() {
                        new TransactionTemplate(coordinator.transactionManager).execute( new TransactionCallbackWithoutResult(){
                            @Override
                            protected void doInTransactionWithoutResult( final TransactionStatus transactionStatus ) {
                                try {
                                    coordinator.fireMaintenancePublishEvents();
                                } catch (ObjectModelException ome) {
                                    logger.log( Level.WARNING, "Error updating proxied business services status.", ome );
                                    transactionStatus.setRollbackOnly();
                                }
                            }
                        } );
                    }
                });
            }
        }
    }

    private static final class PublishedServiceWsdlUpdatedTimerTask extends ManagedTimerTask {
        private final UDDICoordinator coordinator;
        private final Goid serviceGoid;

        PublishedServiceWsdlUpdatedTimerTask( final UDDICoordinator coordinator, final Goid serviceGoid) {
            this.coordinator = coordinator;
            this.serviceGoid = serviceGoid;
        }

        @Override
        public void doRun() {
            if ( coordinator.clusterMaster.isMaster() ) {
                AuditContextUtils.doAsSystem( new Runnable(){
                    @Override
                    public void run() {
                        new TransactionTemplate(coordinator.transactionManager).execute( new TransactionCallbackWithoutResult(){
                            @Override
                            protected void doInTransactionWithoutResult( final TransactionStatus transactionStatus ) {
                                try {
                                    coordinator.checkPublishedServiceWithUpdatedWsdls(serviceGoid);
                                } catch (ObjectModelException ome) {
                                    logger.log( Level.WARNING, "Error updating proxied business service status.", ome );
                                    transactionStatus.setRollbackOnly();
                                }
                            }
                        } );
                    }
                });
            }
        }
    }


    private static final class UDDICoordinatorTaskContext implements UDDITaskFactory.UDDITaskContext {
        private final UDDICoordinator coordinator;
        private final Config config;
        private static final String UDDI_WSDL_PUBLISH_MAX_RETRIES = "uddi.wsdlpublish.maxretries"; 

        UDDICoordinatorTaskContext(final UDDICoordinator coordinator,
                                   final Config config) {
            this.coordinator = coordinator;
            this.config = config;
        }

        @Override
        public String getSubscriptionNotificationURL( final Goid registryGoid ) {
            return this.coordinator.getSubscriptionNotificationUrl( registryGoid );
        }

        @Override
        public String getSubscriptionBindingKey(final Goid registryGoid) {
            return this.coordinator.getSubscriptionNotificationBindingKey( registryGoid );
        }

        @Override
        public void notifyEvent( final UDDIEvent event ) {
            this.coordinator.notifyEvent( event );
        }

        @Override
        public void logAndAudit( final AuditDetailMessage msg, final Throwable e, final String... params ) {
            final Audit auditor = coordinator.auditor;
            if ( auditor != null ) {

                // Only audit a UDDIException if it has a cause
                Throwable auditThrowable = e;
                if( auditThrowable instanceof UDDINetworkException ){
                    //this is expected, dont log / audit stack trace
                    auditThrowable = null;
                } else if ( auditThrowable instanceof UDDIException ) {
                    if ( auditThrowable.getCause()==null ) {
                        auditThrowable = null;
                    }
                }

                auditor.logAndAudit( msg, params, auditThrowable );
            }
        }

        @Override
        public void logAndAudit( final AuditDetailMessage msg, final String... params ) {
            final Audit auditor = coordinator.auditor;
            if ( auditor != null ) {
                auditor.logAndAudit( msg, params );
            }
        }

        @Override
        public void logAndAudit( final AuditDetailMessage msg ) {
            final Audit auditor = coordinator.auditor;
            if ( auditor != null ) {
                auditor.logAndAudit( msg );
            }
        }

        @Override
        public int getMaxRetryAttempts() {
            return config.getIntProperty( UDDI_WSDL_PUBLISH_MAX_RETRIES, 3) - 1;//the first time is the first attempt
        }

        @Override
        public void flushSession() {
            new HibernateTemplate(coordinator.sessionFactory, false).execute(
                    new HibernateCallback<Void>(){
                        @Override
                        public Void doInHibernate(Session session) throws HibernateException, SQLException {
                            session.flush();
                            return null;
                        }
                    });
        }
    }
}
