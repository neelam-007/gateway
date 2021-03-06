package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.PersistenceEvent;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Component that tracks Entity versions and publishes invalidation events.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class EntityVersionChecker implements ApplicationContextAware, InitializingBean, DisposableBean {
    //- PUBLIC

    public EntityVersionChecker() {
        eventListener = new ApplicationListener() {
            @Override
            public void onApplicationEvent(final ApplicationEvent applicationEvent) {
                handleApplicationEvent(applicationEvent);
            }
        };
    }

    /**
     * Set the event proxy.
     *
     * @param applicationEventProxy The proxy to use
     */
    public void setApplicationEventProxy( final ApplicationEventProxy applicationEventProxy ) {
        if (eventProxy != null) {
            throw new IllegalStateException("applicationEventProxy already set");
        }
        eventProxy = applicationEventProxy;
    }

    /**
     * Set the list of managers whose entities should be checked.
     *
     * @param managers the List of HibernateEntityManagers
     * @throws ClassCastException if the list contains a non-HibernateEntityManager
     */
    public void setEntityManagers(List<EntityManager<? extends Entity,? extends EntityHeader>> managers) {
        if(managers!=null && !managers.isEmpty()) {
            List<EntityInvalidationVersionCheck> tasks = new ArrayList<>();
            for (EntityManager<? extends Entity,? extends EntityHeader> manager : managers) {
                try {
                    tasks.add(new EntityInvalidationVersionCheck(manager));
                } catch (Exception e) {
                    logger.warning("Could not create invalidator for manager of '" + manager.getImpClass() + "'");
                }
            }
            btt.addTasks(tasks);
        }
    }

    /**
     * Adds a list of managers whose entities should be checked.
     *
     * @param managers the List of HibernateEntityManagers
     * @throws ClassCastException if the list contains a non-HibernateEntityManager
     */
     public void addEntityManagers(List<EntityManager<? extends Entity,? extends EntityHeader>> managers) {
        setEntityManagers(managers);
    }

    /**
     * Set the invalidation check interval (default is 15000 milliseconds)
     *
     * @param interval the interval to use (minimum is 1000)
     */
    public void setInterval(long interval) {
        if(interval < 1000) {
            throw new IllegalArgumentException("Interval must be at least 1000ms");
        }
        this.interval = interval;
    }

    /**
     * Set the timer.
     *
     * @param timer The timer to use.
     */
    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    /**
     * Initialize the component.
     */
    public void afterPropertiesSet() throws Exception {
        if(btt!=null) {
            if ( eventProxy != null ) {
                logger.fine( "Subscribing to application events." );
                eventProxy.addApplicationListener(eventListener);
            }

            if (timer == null) {
                timer = new Timer("EntityVersionChecker");
            }
            timer.schedule(btt, interval, interval);
        }
        else {
            logger.warning("No registered managers!");
        }
    }

    /**
     * Shutdown the component.
     */
    public void destroy() throws Exception {
        if (timer!=null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Set the application context.
     *
     * @param applicationContext the context to use.
     * @throws IllegalStateException if the context is already set.
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        if(this.applicationContext !=null) {
            throw new IllegalStateException("applicationContext is already set");
        }
        this.applicationContext = applicationContext;
    }

    /**
     * Set the entitiesProcessedInBatch controller.
     *
     * @param entitiesProcessedInBatch the controller that tracks entities already processed to avoid duplicate operations
     * @throws IllegalStateException if the entitiesProcessedInBatch is already set.
     */
    public void setEntitiesProcessedInBatch(EntitiesProcessedInBatch entitiesProcessedInBatch) {
        if(this.entitiesProcessedInBatch !=null) {
            throw new IllegalStateException("entitiesProcessedInBatch is already set");
        }
        this.entitiesProcessedInBatch = entitiesProcessedInBatch;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(EntityVersionChecker.class.getName());

    private final ApplicationListener eventListener;
    private ApplicationContext applicationContext;
    private ApplicationEventProxy eventProxy;
    private Timer timer;
    private long interval = 15L * 1000L;
    private BigTimerTask btt = new BigTimerTask();
    private EntitiesProcessedInBatch entitiesProcessedInBatch;

    private void handleAdminInvalidation(final PersistenceEvent event, final char operation) {
        Entity entity = event.getEntity();

        if ( entity instanceof PersistentEntity) {
            PersistentEntity persistentEntity = (PersistentEntity) entity;
            EntityInvalidationVersionCheck checker = null;

            for ( EntityInvalidationVersionCheck check : btt.tasks ) {
                if ( check.isOfInterest(persistentEntity) ) {
                    checker = check;
                    break;
                }
            }

            if ( checker != null ) {
                enqueueInvalidation( event.getSource(), persistentEntity, operation, checker );
            }

        }
    }

    private void enqueueInvalidation( final Object source,
                                      final PersistentEntity entity,
                                      final char operation,
                                      final EntityInvalidationVersionCheck checker ) {
        logger.log(Level.FINE, new Supplier<String>() {
            @Override
            public String get() {
                return MessageFormat.format("Queuing invalidation for entity {0}", entity.getId());
            }
        });

        DispatchingTransactionSynchronization dts = null;

        //noinspection unchecked
        for ( TransactionSynchronization ts : TransactionSynchronizationManager.getSynchronizations() ) {
            if ( ts instanceof DispatchingTransactionSynchronization ) {
                dts = (DispatchingTransactionSynchronization) ts;
                break;
            }
        }

        // Create sync if none registered
        if ( dts == null ) {
            dts = new DispatchingTransactionSynchronization();
            TransactionSynchronizationManager.registerSynchronization( dts );
        }

        EntityInvalidationEvent eie = new EntityInvalidationEvent( source, checker.entityType, new Goid[]{entity.getGoid()}, new char[]{operation});

        dts.events.add(new Pair<>(eie, new Functions.Nullary<Boolean>() {
            public Boolean call() {
                return checker.isInvalidationRequired(entity, operation);
            }
        }));
    }

    /**
     * Dispatch the given event using the application event sink.
     *
     * WARNING: only dispatch on a callback to ensure single threaded for any listeners that expect that
     */
    private void dispatchInvalidation( final EntityInvalidationEvent eie ) {
        logger.fine(new Supplier<String>() {
            @Override
            public String get() {
                return "Invalidating entities of type '"+eie.getEntityClass().getName()+"'; ids are "+asList(eie.getEntityIds())+".";
            }
        });

        applicationContext.publishEvent(eie);
    }

    /**
     * Box array
     */
    private List<Goid> asList( Goid[] items ) {
        List<Goid> list = new ArrayList<>(items.length);
        list.addAll(Arrays.asList(items));
        return list;
    }

    /**
     * Listen for and asynchronously dispatch entity admin events.
     */
    private void handleApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof Created ) {
            handleAdminInvalidation((PersistenceEvent) event, EntityInvalidationEvent.CREATE);
        } else if ( event instanceof Updated ) {
            handleAdminInvalidation((PersistenceEvent) event, EntityInvalidationEvent.UPDATE);
        } else if ( event instanceof Deleted ) {
            handleAdminInvalidation((PersistenceEvent) event, EntityInvalidationEvent.DELETE);
        }
    }

    /**
     * Run a collection of TimerTasks as one task.
     */
    private class BigTimerTask extends TimerTask {
        private final List<EntityInvalidationVersionCheck> tasks = new ArrayList<>();

        private BigTimerTask() {
        }

        private void addTasks(List<EntityInvalidationVersionCheck> subTimerTasks){
            tasks.addAll(subTimerTasks);
        }

        @Override
        public void run() {
            logger.fine(new Supplier<String>() {
                @Override
                public String get() {
                    return "Running entity invalidation.";
                }
            });

            long startTime = System.currentTimeMillis();

            entitiesProcessedInBatch.reset();

            //noinspection ForLoopReplaceableByForEach This is iterated in this way in case items are added to the list
            for (int i = 0; i < tasks.size(); i++) {
                TimerTask task = tasks.get(i);
                if (timer == null) {
                    logger.info("Version check task exiting due to shutdown.");
                    break; //check if cancelled
                }

                try {
                    task.run();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error running version check task", e);
                }
            }

            entitiesProcessedInBatch.reset();

            long checkTime = System.currentTimeMillis() - startTime;
            Level logLevel = checkTime > 1000 ? Level.INFO : Level.FINE;
            logger.log(logLevel, new Supplier<String>() {
                @Override
                public String get() {
                    return "Version checking took "+checkTime+"ms";
                }
            });
        }
    }

    /**
     * Version check task for an EntityManager
     */
    private class EntityInvalidationVersionCheck extends PeriodicVersionCheck {
        private final Class<? extends Entity> entityType;
        private List<Goid> invalidationGoids;
        private List<Character> invalidationOps;

        private EntityInvalidationVersionCheck(EntityManager<? extends PersistentEntity, ? extends EntityHeader> manager) throws FindException {
            super(manager);
            entityType = manager.getInterfaceClass();
        }

        boolean isOfInterest(Entity entity) {
            return entityType.isInstance( entity );
        }

        boolean isInvalidationRequired(PersistentEntity entity, char operation) {
            boolean invalidationRequired;

            if ( operation == EntityInvalidationEvent.DELETE ) {
                invalidationRequired = notifyDelete( entity.getGoid() );
            } else {
                invalidationRequired = notifyUpdate( entity.getGoid(), entity.getVersion() );
            }

            return invalidationRequired;
        }

        @Override
        public void run() {
            invalidationGoids = new ArrayList<>();
            invalidationOps = new ArrayList<>();
            super.run();
            performInvalidation(entityType, invalidationGoids, invalidationOps);
            invalidationGoids = null;
            invalidationOps = null;
        }

        @Override
        protected void onDelete(Goid removedGoid) {
            if(invalidationGoids !=null) {
                invalidationGoids.add(removedGoid);
                invalidationOps.add(EntityInvalidationEvent.DELETE);
            }
        }

        @Override
        protected void onUpdate(PersistentEntity updatedEntity) {
            if(invalidationGoids !=null) {
                invalidationGoids.add(updatedEntity.getGoid());
                invalidationOps.add(EntityInvalidationEvent.UPDATE);
            }
        }

        @Override
        protected void onCreate(PersistentEntity createdEntity) {
            if(invalidationGoids !=null) {
                invalidationGoids.add(createdEntity.getGoid());
                invalidationOps.add(EntityInvalidationEvent.CREATE);
            }
        }

        /**
         * Publish invalidation information for the given entityies
         */
        private void performInvalidation(final Class<? extends Entity> entityType, final List<Goid> goids, final List<Character> ops) {
            if(!goids.isEmpty()) {
                if (goids.size() != ops.size()) throw new IllegalArgumentException("oids and ops must be equal in size");
                Goid[] goidArray = new Goid[goids.size()];
                char[] opsArray = new char[goids.size()];
                for (int i = 0; i < goidArray.length; i++) {
                    goidArray[i] = goids.get(i);
                    opsArray[i] = ops.get(i);
                }

                dispatchInvalidation(new EntityInvalidationEvent(this, entityType, goidArray, opsArray));
            }
        }
    }

    /**
     * Dispatch invalidations for entity events on commit
     */
    private class DispatchingTransactionSynchronization extends TransactionSynchronizationAdapter {
        final List<Pair<EntityInvalidationEvent,Functions.Nullary<Boolean>>> events = new ArrayList<>();

        @Override
        public void afterCompletion( final int status ) {
            if ( status == STATUS_COMMITTED ) {
                timer.schedule( new TimerTask() {
                    @Override
                    public void run() {
                        long startTime = System.currentTimeMillis();
                        entitiesProcessedInBatch.reset();

                        for ( Pair<EntityInvalidationEvent,Functions.Nullary<Boolean>> eventAndChecker : events ) {
                            EntityInvalidationEvent eie = eventAndChecker.left;
                            Functions.Nullary<Boolean> requiredCallback = eventAndChecker.right;

                            if ( requiredCallback.call() ) {
                                dispatchInvalidation(eie);
                            }
                        }

                        entitiesProcessedInBatch.reset();
                        long endtime = System.currentTimeMillis();

                        long duration = endtime - startTime;
                        logger.fine(new Supplier<String>() {
                            @Override
                            public String get() {
                                return String.format("Entity Invalidation batch took %s millis", duration);
                            }
                        });

                    }
                }, 0L );
            }
        }
    }
}
