package com.l7tech.server.search;

import com.l7tech.gateway.common.licensing.LicenseDocument;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.PersistenceEvent;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.inject.Inject;
import javax.inject.Named;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.KeyStoreException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The dependency cache maintians a graph of the gateway dependencies. It is used to increase the speed of getting
 * dependencies and allows for reverse dependency searches (entity usages).
 *
 * @See https://wiki.l7tech.com/mediawiki/index.php/Reverse_Dependency_Analysis
 */
public class DependencyCacheImpl implements DependencyCache, PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(DependencyCacheImpl.class.getName());

    //These are the options used to create the dependency cache
    private static final Map<String, Object> dependencyCacheCreateOptions = CollectionUtils.<String, Object>mapBuilder()
            .put(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey, false)
            .map();

    @Inject
    private DependencyAnalyzerImpl dependencyAnalyzer;
    @Inject
    private EntityCrud entityCrud;
    @Inject
    private Config config;
    @Inject
    private ServiceManager serviceManager;
    @Inject
    private JmsEndpointManager jmsEndpointManager;
    @Inject
    private SsgKeyStoreManager ssgKeyStoreManager;
    @Inject
    @Named("applicationEventProxy")
    private ApplicationEventProxy applicationEventProxy;

    //the lock to make sure there are no cache issues accessing the cache from multiple threads.
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    //The current state of the cache. True for on false for off
    private final AtomicBoolean caching = new AtomicBoolean(false);

    /**
     * This is a map of usages. It keeps tract of which entities use a dependency.
     */
    private final Map<DependentObject, Set<DependentObject>> usages = new HashMap<>();
    /**
     * This map keeps tract of the dependencies of an object
     */
    private final Map<DependentObject, Dependency> dependencies = new HashMap<>();

    //This is the queue of events that need to be checked for cache modifications.
    private final LinkedBlockingQueue<ApplicationEvent> eventsQueue = new LinkedBlockingQueue<>();

    //This is the thread that is used to monitor for events in the events queue
    private Thread eventProcessingThread = null;
    //This latch is used to wait for the event thread to properly shutdown
    private CountDownLatch eventProcessingThreadCountDownLatch = new CountDownLatch(0);


    /**
     * Initialize the application event listener. This will listen to application events.
     */
    public void init() {
        applicationEventProxy.addApplicationListener(new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                if (event instanceof PersistenceEvent || event instanceof EntityInvalidationEvent) {
                    //don't lock on state. events need to execute quickly
                    // if the cache if on just add then to the events queue.
                    if (caching.get()) {
                        eventsQueue.offer(event);
                    }
                } else if (event instanceof Started) {
                    //the gateway has started so build cache
                    if(isCacheEnabled()) {
                        buildCache();
                    }
                }
            }
        });
    }

    /**
     * Builds the cache and started the thread to monitor for events.
     */
    private void buildCache() {
        //build the cache in a separate thread. This is done so that gateway startup is not interrupted.
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!caching.get()) {
                    cacheLock.writeLock().lock();
                    try {
                        if (caching.compareAndSet(false, true)) {
                            logger.log(Level.INFO, "Building Dependency Cache...");
                            //stop the event thread
                            if (eventProcessingThread != null) {
                                boolean eventThreadStopped = false;
                                //try nicely to stop the event thread
                                eventProcessingThread.interrupt();
                                try {
                                    eventThreadStopped = eventProcessingThreadCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                                } catch (InterruptedException e) {
                                    //unexpected exception. Continue.
                                }
                                if (!eventThreadStopped) {
                                    //could not nicely stop the thread so force it to stop
                                    //noinspection deprecation
                                    eventProcessingThread.stop();
                                }
                                eventProcessingThread = null;
                            }
                            //clear the eventsQueue again. There is a very small chance that an event could have been added during caching stop
                            eventsQueue.clear();
                            final List<Dependency> allDependencies;
                            try {
                                //get all gateway dependencies
                                allDependencies = DependencySearchResultsUtils.flattenDependencySearchResults(dependencyAnalyzer.getDependencies(Collections.<EntityHeader>emptyList(), dependencyCacheCreateOptions), true);
                            } catch (FindException | CannotRetrieveDependenciesException e) {
                                logger.log(Level.WARNING, "Error building dependency cache. Message: " + ExceptionUtils.getMessage(e), e);
                                caching.set(false);
                                return;
                            }
                            //Add all the dependencies to the cache.
                            for (final Dependency dependency : allDependencies) {
                                addDependencyToCache(dependency);
                            }

                            // process anything that is in the event queue now to make it so that dependency requests don't
                            // sneak in when the cache isn't completely up to date (mostly an issue in unit tests)
                            while (!eventsQueue.isEmpty()) {
                                ApplicationEvent event = eventsQueue.poll();
                                handlePersistenceOrInvalidationEvent(event);
                            }

                            logger.log(Level.INFO, "Dependency Cache built successfully");

                            //start the event processing thread.
                            //this countdown latch will reach 0 when the event processing thread has stopped.
                            eventProcessingThreadCountDownLatch = new CountDownLatch(1);
                            eventProcessingThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    //while caching
                                    while (caching.get()) {
                                        final ApplicationEvent event;
                                        try {
                                            //poll the events queue
                                            event = eventsQueue.poll(100, TimeUnit.MILLISECONDS);
                                            if (event != null) {
                                                handlePersistenceOrInvalidationEvent(event);
                                            }
                                        } catch (InterruptedException e) {
                                            //this could happen when we are attempting to stop the event processing thread.
                                        } catch (Throwable t) {
                                            logger.log(Level.WARNING, "Could not process event for dependency cache: Message: " + ExceptionUtils.getMessage(t));
                                        }
                                    }
                                    eventProcessingThreadCountDownLatch.countDown();
                                }
                            });
                            eventProcessingThread.start();
                        }
                    } finally {
                        cacheLock.writeLock().unlock();
                    }
                }
            }
        }).start();
    }

    /**
     * Handles the different events that will effect the cache
     *
     * @param event the event to handle
     */
    private void handlePersistenceOrInvalidationEvent(ApplicationEvent event) {
        if (event instanceof PersistenceEvent) {
            handlePersistenceEvent((PersistenceEvent) event);
        } else if (event instanceof EntityInvalidationEvent) {
            handleEntityInvalidationEvent((EntityInvalidationEvent) event);
        }
    }

    /**
     * Handles entity invalidation events
     *
     * @param entityInvalidationEvent The entity invalidation event to handle
     */
    private void handleEntityInvalidationEvent(EntityInvalidationEvent entityInvalidationEvent) {
        // for all entities in this event
        for (int i = 0; i < entityInvalidationEvent.getEntityIds().length; i++) {
            switch (entityInvalidationEvent.getEntityOperations()[i]) {
                case EntityInvalidationEvent.CREATE:
                case EntityInvalidationEvent.UPDATE:
                    //get the entity and handle the event
                    if (entityInvalidationEvent.getSource() instanceof Entity) {
                        handleEntityEvent((Entity) entityInvalidationEvent.getSource());
                    }
                    break;
                case EntityInvalidationEvent.DELETE:
                    if (entityInvalidationEvent.getSource() instanceof Entity) {
                        handleEntityDeleteEvent((Entity) entityInvalidationEvent.getSource());
                    }
                    break;
            }
        }
    }

    /**
     * Handles the persistence event
     *
     * @param event The persistence event
     */
    private void handlePersistenceEvent(PersistenceEvent event) {
        if (event instanceof Created || event instanceof Updated) {
            Entity entity = (event).getEntity();
            handleEntityEvent(entity);
        } else if (event instanceof Deleted) {
            Entity entity = event.getEntity();
            handleEntityDeleteEvent(entity);
        }
    }

    /**
     * handles an entity deletion event
     *
     * @param entity The entity that was deleted.
     */
    private void handleEntityDeleteEvent(Entity entity) {
        final DependentObject dependentObject;
        if (entity instanceof JmsConnection) {
            //do nothing
            return;
        } else if (entity instanceof SsgKeyMetadata) {
            //if it is a service policy we should instead update the published service in the cache
            SsgKeyEntry ssgKeyEntry = null;
            try {
                ssgKeyEntry = ssgKeyStoreManager.lookupKeyByKeyAlias(((SsgKeyMetadata) entity).getAlias(), ((SsgKeyMetadata) entity).getKeystoreGoid());
            } catch (FindException | KeyStoreException e) {
                //do nothing. Will log warning below.
            }
            if (ssgKeyEntry == null) {
                logger.log(Level.WARNING, "Trying to update dependency cache for ssg key metadata but could not find ssg key entry. alias: " + ((SsgKeyMetadata) entity).getAlias());
                return;
            } else {
                dependentObject = dependencyAnalyzer.createDependentObject(ssgKeyEntry);
            }
        } else {
            dependentObject = dependencyAnalyzer.createDependentObject(entity);
        }
        cacheLock.writeLock().lock();
        try {
            logger.log(Level.FINER, "Removing dependent object from dependency cache. name: " + dependentObject.getName() + " type: " + dependentObject.getDependencyType());
            dependencies.remove(dependentObject);
            usages.remove(dependentObject);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Handles an entity update or create event.
     *
     * @param entity The entity that was created or updated.
     */
    private void handleEntityEvent(@NotNull final Entity entity) {
        if (entity instanceof PolicyVersion || entity instanceof LicenseDocument) {
            //do nothing. These are not covered by the dependency analysis
            logger.log(Level.FINEST, "Not updating dependency cache for " + entity.getClass().getName() + " entities.");
        } else if ((entity instanceof Policy && PolicyType.PRIVATE_SERVICE.equals(((Policy) entity).getType()))) {
            //if it is a service policy we should instead update the published service in the cache
            List<PublishedService> services = null;
            try {
                services = serviceManager.findPagedMatching(0, 1, null, null, CollectionUtils.<String, List<Object>>mapBuilder().put("policy", Arrays.<Object>asList(entity)).map());
            } catch (FindException e) {
                //do nothing. Will log warning below.
            }
            if (services == null || services.isEmpty() || services.get(0) == null) {
                logger.log(Level.WARNING, "Trying to update dependency cache for private service policy, but could not find service. Policy name: " + ((Policy) entity).getName());
            } else {
                updateDependencyCache(services.get(0));
            }
        } else if (entity instanceof JmsConnection) {
            //if it is a service policy we should instead update the published service in the cache
            List<JmsEndpoint> jmsEndpoints = null;
            try {
                jmsEndpoints = jmsEndpointManager.findPagedMatching(0, 1, null, null, CollectionUtils.<String, List<Object>>mapBuilder().put("connectionGoid", Arrays.<Object>asList(((JmsConnection) entity).getGoid())).map());
            } catch (FindException e) {
                //do nothing. Will log warning below.
            }
            if (jmsEndpoints == null || jmsEndpoints.isEmpty() || jmsEndpoints.get(0) == null) {
                logger.log(Level.WARNING, "Trying to update dependency cache for jms connection, but could not find jms endpoint. jms connection name: " + ((JmsConnection) entity).getName());
            } else {
                updateDependencyCache(jmsEndpoints.get(0));
            }
        } else if (entity instanceof SsgKeyMetadata) {
            //if it is a service policy we should instead update the published service in the cache
            SsgKeyEntry ssgKeyEntry = null;
            try {
                ssgKeyEntry = ssgKeyStoreManager.lookupKeyByKeyAlias(((SsgKeyMetadata) entity).getAlias(), ((SsgKeyMetadata) entity).getKeystoreGoid());
            } catch (FindException | KeyStoreException e) {
                //do nothing. Will log warning below.
            }
            if (ssgKeyEntry == null) {
                logger.log(Level.WARNING, "Trying to update dependency cache for ssg key metadata but could not find ssg key entry. alias: " + ((SsgKeyMetadata) entity).getAlias());
            } else {
                updateDependencyCache(ssgKeyEntry);
            }
        } else {
            updateDependencyCache(entity);
        }
    }

    /**
     * This monitors cluster property changes and handles the events
     *
     * @param evt the property changed event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final String propertyName = evt.getPropertyName();
        switch (propertyName) {
            //This is the dependency cache property. It controls turning the cache on or off.
            case ServerConfigParams.PARAM_DEPENDENCY_CACHE_ENABLED:
                final boolean cacheEnabled = isCacheEnabled();
                if (cacheEnabled) {
                    //build the cache
                    buildCache();
                } else {
                    //stop cache. check that it is running first
                    if (caching.get()) {
                        cacheLock.writeLock().lock();
                        try {
                            if (caching.compareAndSet(true, false)) {
                                dependencies.clear();
                                usages.clear();
                                eventsQueue.clear();
                            }
                        } finally {
                            cacheLock.writeLock().unlock();
                        }
                    }
                }
                break;
        }
    }

    /**
     * Update the dependency cache with the given updated or newly create entity
     *
     * @param entity The entity to update the cache with.
     */
    private void updateDependencyCache(Entity entity) {
        DependencySearchResults dependencySearchResults;
        try {
            //get the entity dependencies with search depth 0 only
            dependencySearchResults = dependencyAnalyzer.getDependenciesFromEntities(Arrays.asList(entity), CollectionUtils.<String, Object>mapBuilder().put(DependencyAnalyzer.ReturnAssertionsAsDependenciesOptionKey, false).put(DependencyAnalyzer.SearchDepthOptionKey, 1).map()).get(0);
        } catch (CannotRetrieveDependenciesException | FindException e) {
            logger.log(Level.SEVERE, "Cannot update dependency cache: " + ExceptionUtils.getMessage(e), e);
            return;
        }
        cacheLock.writeLock().lock();
        try {
            final Dependency dependency = new Dependency(dependencySearchResults.getDependent());
            if (dependencySearchResults.getDependencies() != null) {
                dependency.setDependencies(new ArrayList<>(dependencySearchResults.getDependencies()));
            }
            final Dependency cacheDependency = addDependencyToCache(dependency);
            if (entity instanceof HasFolder) {
                //need to update the folder too.
                //TODO: need to handle folder changes
                Folder parentFolder = ((HasFolder) entity).getFolder();
                Dependency folderDependency = dependencies.get(dependencyAnalyzer.createDependentObject(parentFolder));
                if (folderDependency.getDependencies() != null) {
                    folderDependency.getDependencies().remove(cacheDependency);
                    folderDependency.getDependencies().add(cacheDependency);
                } else {
                    folderDependency.setDependencies(new ArrayList<Dependency>() {{
                        add(cacheDependency);
                    }});
                }
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Adds the dependency to the cache
     *
     * @param dependency The dependency to add to the cache
     * @return The dependency added to the cache.
     */
    private Dependency addDependencyToCache(final @NotNull Dependency dependency) {
        Dependency cachedDependency = dependencies.get(dependency.getDependent());
        final List<Dependency> previousDependencies;

        //the dependency hasn't been cached yet so add it to the cache.
        if (cachedDependency == null) {
            dependencies.put(dependency.getDependent(), dependency);
            previousDependencies = Collections.emptyList();
            cachedDependency = dependency;
        } else {
            //the dependency was already caches so just updated the cache version.
            cachedDependency.setDependent(dependency.getDependent());
            if (cachedDependency.getDependencies() == null) {
                previousDependencies = Collections.emptyList();
            } else {
                previousDependencies = new ArrayList<>(cachedDependency.getDependencies());
            }
        }

        //update the usage info
        if (dependency.getDependencies() != null) {
            List<Dependency> mappedDependencies = new ArrayList<>(dependency.getDependencies().size());
            for (Dependency subDependency : dependency.getDependencies()) {
                if (!dependencies.containsKey(subDependency.getDependent())) {
                    addDependencyToCache(subDependency);
                }
                Dependency cachedSubDependency = dependencies.get(subDependency.getDependent());
                mappedDependencies.add(cachedSubDependency);
            }
            cachedDependency.setDependencies(mappedDependencies);
        } else {
            if (cachedDependency.getDependencies() != null) {
                cachedDependency.getDependencies().clear();
            }
        }

        //find and update the usages.
        if (cachedDependency.getDependencies() != null && !cachedDependency.getDependencies().isEmpty()) {
            for (final Dependency usage : cachedDependency.getDependencies()) {
                Set<DependentObject> dependentUsages = usages.get(usage.getDependent());
                if (dependentUsages == null) {
                    dependentUsages = new HashSet<>();
                    usages.put(usage.getDependent(), dependentUsages);
                }
                dependentUsages.add(cachedDependency.getDependent());
                //remove the usage from the previous dependencies
                previousDependencies.remove(usage);
            }
        }
        //remove previous dependency usages
        for (final Dependency previousUsage : previousDependencies) {
            Set<DependentObject> dependentUsages = usages.get(previousUsage.getDependent());
            if (dependentUsages != null) {
                dependentUsages.remove(previousUsage.getDependent());
            }
        }

        return cachedDependency;
    }

    /**
     * Checks if the cache is enabled by looking at the dependency cache enabled cluster property.
     *
     * @return true if the cache is enabled. false otherwise
     */
    private boolean isCacheEnabled() {
        return config.getBooleanProperty(ServerConfigParams.PARAM_DEPENDENCY_CACHE_ENABLED, false);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public DependencySearchResults getDependencies(@NotNull final EntityHeader entityHeader) throws FindException, CannotRetrieveDependenciesException {
        return getDependencies(entityHeader, Collections.<String, Object>emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public DependencySearchResults getDependencies(@NotNull final EntityHeader entityHeader, @NotNull final Map<String, Object> searchOptions) throws FindException, CannotRetrieveDependenciesException {
        return getDependencies(Arrays.asList(entityHeader), searchOptions).get(0);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public List<DependencySearchResults> getDependencies(@NotNull final List<EntityHeader> entityHeaders) throws FindException, CannotRetrieveDependenciesException {
        return getDependencies(entityHeaders, Collections.<String, Object>emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public List<DependencySearchResults> getDependencies(@NotNull final List<EntityHeader> entityHeaders, @NotNull final Map<String, Object> searchOptions) throws FindException, CannotRetrieveDependenciesException {
        //check if caching is enabled
        if (caching.get()) {
            //get a read lock
            cacheLock.readLock().lock();
            try {
                //double check if caching is enabled
                if (caching.get()) {
                    //find the dependencies in the cache
                    return findDependenciesFromDependencyCache(entityHeaders, searchOptions);
                }
            } finally {
                cacheLock.readLock().unlock();
            }
        }
        //use the dependency analyzer
        return dependencyAnalyzer.getDependencies(entityHeaders, searchOptions);
    }

    @Override
    public <E extends Entity> void replaceDependencies(@NotNull E entity, @NotNull Map<EntityHeader, EntityHeader> replacementMap, boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        dependencyAnalyzer.replaceDependencies(entity, replacementMap, replaceAssertionsDependencies);
    }

    /**
     * This does the work of finding the dependencies in the cache
     *
     * @param entityHeaders The entity headers to find the dependencies for
     * @param searchOptions The search options
     * @return The dependencies.
     */
    private List<DependencySearchResults> findDependenciesFromDependencyCache(@NotNull final List<EntityHeader> entityHeaders, @NotNull final Map<String, Object> searchOptions) {
        //Create dependent objects for the entity headers
        final List<DependentObject> dependentObjects = new ArrayList<>(entityHeaders.size());
        for (final EntityHeader entityHeader : entityHeaders) {
            Entity entity;
            try {
                entity = entityCrud.find(entityHeader);
            } catch (FindException e) {
                throw new RuntimeException("Could not find Entity with header: " + entityHeader.toStringVerbose(), e);
            }
            if (entity == null) {
                throw new RuntimeException("Could not find Entity with header: " + entityHeader.toStringVerbose());
            }
            dependentObjects.add(dependencyAnalyzer.createDependentObject(entity));
        }
        //TODO: do the following two loops in a single command
        //find the items in the cache and create dependency search results from it.
        final List<DependencySearchResults> dependencySearchResultsList = new ArrayList<>(entityHeaders.size());
        for (DependentObject dependentObject : dependentObjects) {
            Dependency objectDependencies = dependencies.get(dependentObject);
            dependencySearchResultsList.add(new DependencySearchResults(dependentObject, (objectDependencies == null || objectDependencies.getDependencies() == null) ? null : Dependency.clone(objectDependencies.getDependencies()), searchOptions));
        }
        //need to filter the dependency list based on the search options
        for (DependencySearchResults dependencySearchResults : dependencySearchResultsList) {
            if (dependencySearchResults.getDependencies() != null) {
                dependencySearchResults.setDependencies(filter(dependencySearchResults.getDependencies(), searchOptions));
            }
        }
        return dependencySearchResultsList;
    }

    /**
     * Filters dependencies from the in the list.
     *
     * @param dependencyList The list to filter
     * @param searchOptions  The search options to filter by
     */
    private List<Dependency> filter(List<Dependency> dependencyList, @NotNull final Map<String, Object> searchOptions) {
        int searchDepth = PropertiesUtil.getOption(DependencyAnalyzer.SearchDepthOptionKey, Integer.class, -1, searchOptions);
        final List searchTypes = PropertiesUtil.getOption(DependencyAnalyzer.SearchEntityTypeOptionKey, List.class, (List) Collections.emptyList(), searchOptions);

        return filter(dependencyList, searchDepth, searchTypes, new HashSet<DependentObject>());
    }

    /**
     * Filters the dependencies in the list.
     *
     * @param dependencyList        The dependencies list to filter
     * @param searchDepth           The search depth to filter to
     * @param searchTypes           The types of entities to filter
     * @param processedDependencies The already filtered dependencies (this is needed for cyclical dependencies)
     * @return The filtered list
     */
    private List<Dependency> filter(List<Dependency> dependencyList, int searchDepth, List searchTypes, Set<DependentObject> processedDependencies) {
        if (searchDepth == 0) {
            return null;
        }
        Iterator<Dependency> iterator = dependencyList.iterator();
        while (iterator.hasNext()) {
            Dependency dependency = iterator.next();
            if (!processedDependencies.contains(dependency.getDependent())) {
                processedDependencies.add(dependency.getDependent());
                if (searchTypes.isEmpty() || (dependency.getDependent() instanceof DependentEntity && searchTypes.contains(((DependentEntity) dependency.getDependent()).getEntityHeader().getType()))) {
                    if (dependency.getDependencies() != null) {
                        dependency.setDependencies(filter(dependency.getDependencies(), --searchDepth, searchTypes, processedDependencies));
                    }
                } else {
                    iterator.remove();
                }
            }
        }
        return dependencyList;
    }

    /**
     * Returns the usages of the given entity.
     *
     * @param dependentObjectHeader The entity to find usages of.
     * @return The entities that use this entity.
     */
    @NotNull
    @Override
    public DependencySearchResults findUsages(@NotNull EntityHeader dependentObjectHeader) {
        if (caching.get()) {
            cacheLock.readLock().lock();
            try {
                if (caching.get()) {
                    return findUsagesFromDependencyCache(dependentObjectHeader);
                }
            } finally {
                cacheLock.readLock().unlock();
            }
        }
        throw new IllegalStateException("dependency cache is not started.");
    }

    private DependencySearchResults findUsagesFromDependencyCache(EntityHeader dependentObjectHeader) {
        final Entity entity;
        try {
            entity = entityCrud.find(dependentObjectHeader);
        } catch (FindException e) {
            throw new RuntimeException("Could not find Entity with header: " + dependentObjectHeader.toStringVerbose(), e);
        }
        if (entity == null) {
            throw new RuntimeException("Could not find Entity with header: " + dependentObjectHeader.toStringVerbose());
        }
        DependentObject dependentObject = dependencyAnalyzer.createDependentObject(entity);
        final Set<DependentObject> dependentObjectsList;
        dependentObjectsList = new HashSet<>(usages.get(dependentObject));
        return new DependencySearchResults(dependentObject, Functions.map(dependentObjectsList, new Functions.Unary<Dependency, DependentObject>() {
            @Override
            public Dependency call(DependentObject dependentObject) {
                return new Dependency(dependentObject);
            }
        }), Collections.<String, Object>emptyMap());
    }
}
