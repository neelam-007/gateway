package com.l7tech.server;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.PersistenceEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Pair;
import com.l7tech.util.Functions;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.*;
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
        if (eventProxy != null) throw new IllegalStateException("applicationEventProxy already set");
        eventProxy = applicationEventProxy;
    }

    /**
     * Set the list of managers whose entities should be checked.
     *
     * @param managers the List of HibernateEntityManagers
     * @throws IllegalStateException if the managers are already set
     * @throws ClassCastException if the list contains a non-HibernateEntityManager
     */
    public void setEntityManagers(List<EntityManager<? extends PersistentEntity,? extends EntityHeader>> managers) {
        if(btt!=null) throw new IllegalStateException("manager already set");
        if(managers!=null && !managers.isEmpty()) {
            List<EntityInvalidationVersionCheck> tasks = new ArrayList<EntityInvalidationVersionCheck>();
            for (EntityManager<? extends PersistentEntity,? extends EntityHeader> manager : managers) {
                try {
                    tasks.add(new EntityInvalidationVersionCheck(manager));
                } catch (Exception e) {
                    logger.warning("Could not create invalidator for manager of '" + manager.getImpClass() + "'");
                }
            }
            btt = new BigTimerTask(tasks);
        }
    }

    /**
     * Set the invalidation check interval (default is 15000 milliseconds)
     *
     * @param interval the interval to use (minimum is 1000)
     */
    public void setInterval(long interval) {
        if(interval < 1000) throw new IllegalArgumentException("Interval must be at least 1000ms");
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

            if (timer == null)
                timer = new Timer("EntityVersionChecker");
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(this.eventSink!=null) throw new IllegalStateException("applicationContext is already set");
        this.eventSink = applicationContext;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(EntityVersionChecker.class.getName());

    private final ApplicationListener eventListener;
    private ApplicationEventPublisher eventSink;
    private ApplicationEventProxy eventProxy;
    private Timer timer;
    private long interval = 15 * 1000;
    private BigTimerTask btt;

    private void handleAdminInvalidation(final PersistenceEvent event, final char operation) {
        Entity entity = event.getEntity();

        if ( entity instanceof PersistentEntity ) {
            PersistentEntity persistentEntity = (PersistentEntity) entity;
            EntityInvalidationVersionCheck checker = null;

            for ( EntityInvalidationVersionCheck check : btt.tasks ) {
                if ( check.isOfInterest( persistentEntity ) ) {
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
        if ( logger.isLoggable( Level.FINE ))
            logger.log( Level.FINE, "Queuing invalidation for entity {0}", entity.getId() );

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

        EntityInvalidationEvent eie = new EntityInvalidationEvent( source, checker.entityType, new long[]{entity.getOid()}, new char[]{operation});

        dts.events.add( new Pair<EntityInvalidationEvent,Functions.Nullary<Boolean>>(eie, new Functions.Nullary<Boolean>(){
            public Boolean call() {
                return checker.isInvalidationRequired( entity, operation );
            }
        }));
    }

    /**
     * Publish invalidation information for the given entityies
     */
    private void performInvalidation(final Class<? extends Entity> entityType, final List<Long> oids, final List<Character> ops) {
        if(!oids.isEmpty()) {
            if (oids.size() != ops.size()) throw new IllegalArgumentException("oids and ops must be equal in size");
            long[] oidArray = new long[oids.size()];
            char[] opsArray = new char[oids.size()];
            for (int i = 0; i < oidArray.length; i++) {
                oidArray[i] = oids.get(i);
                opsArray[i] = ops.get(i);
            }

            dispatchInvalidation(new EntityInvalidationEvent(this, entityType, oidArray, opsArray));
        }
    }

    /**
     * Dispatch the given event using the application event sink.
     *
     * WARNING: only dispatch on a callback to ensure single threaded for any listeners that expect that
     */
    private void dispatchInvalidation( final EntityInvalidationEvent eie ) {

        if(logger.isLoggable(Level.FINE))
            logger.fine("Invalidating entities of type '"+eie.getEntityClass().getName()+"'; ids are "+asList(eie.getEntityIds())+".");

        eventSink.publishEvent(eie);
    }

    /**
     * Box array
     */
    private List<Long> asList( long[] items ) {
        List<Long> list = new ArrayList<Long>( items.length );
        for ( long item : items ) {
            list.add( item );
        }
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
        private final List<EntityInvalidationVersionCheck> tasks;

        private BigTimerTask(List<EntityInvalidationVersionCheck> subTimerTasks) {
            this.tasks = Collections.unmodifiableList( subTimerTasks );
        }

        @Override
        public void run() {
            if(logger.isLoggable(Level.FINE)) logger.fine("Running entity invalidation.");

            long startTime = System.currentTimeMillis();
            for (TimerTask task : tasks) {
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
            long checkTime = System.currentTimeMillis() - startTime;
            Level logLevel = checkTime > 1000 ? Level.INFO : Level.FINE;
            if(logger.isLoggable(logLevel)) {
                logger.log(logLevel, "Version checking took "+checkTime+"ms");
            }
        }
    }

    /**
     * Version check task for an EntityManager
     */
    private class EntityInvalidationVersionCheck extends PeriodicVersionCheck {
        private final Class<? extends Entity> entityType;
        private List<Long> invalidationOids;
        private List<Character> invalidationOps;

        private EntityInvalidationVersionCheck(EntityManager<? extends PersistentEntity, ? extends EntityHeader> manager) throws Exception {
            super(manager);
            entityType = manager.getInterfaceClass();
        }

        public boolean isOfInterest( Entity entity ) {
            return entityType.isInstance( entity );
        }

        public boolean isInvalidationRequired( PersistentEntity entity, char operation ) {
            boolean invalidationRequired;

            if ( operation == EntityInvalidationEvent.DELETE ) {
                invalidationRequired = notifyDelete( entity.getOid() );
            } else {
                invalidationRequired = notifyUpdate( entity.getOid(), entity.getVersion() );
            }

            return invalidationRequired;
        }

        @Override
        public void run() {
            invalidationOids = new ArrayList<Long>();
            invalidationOps = new ArrayList<Character>();
            super.run();
            performInvalidation(entityType, invalidationOids, invalidationOps);
            invalidationOids = null;
            invalidationOps = null;
        }

        @Override
        protected void onDelete(long removedOid) {
            if(invalidationOids !=null) {
                invalidationOids.add(removedOid);
                invalidationOps.add(EntityInvalidationEvent.DELETE);
            }
        }

        @Override
        protected void onUpdate(PersistentEntity updatedEntity) {
            if(invalidationOids !=null) {
                invalidationOids.add(updatedEntity.getOid());
                invalidationOps.add(EntityInvalidationEvent.UPDATE);
            }
        }

        @Override
        protected void onCreate(PersistentEntity createdEntity) {
            if(invalidationOids !=null) {
                invalidationOids.add(createdEntity.getOid());
                invalidationOps.add(EntityInvalidationEvent.CREATE);
            }
        }
    }

    /**
     * Dispatch invalidations for entity events on commit
     */
    private class DispatchingTransactionSynchronization extends TransactionSynchronizationAdapter {
        final List<Pair<EntityInvalidationEvent,Functions.Nullary<Boolean>>> events
                = new ArrayList<Pair<EntityInvalidationEvent, Functions.Nullary<Boolean>>>();

        @Override
        public void afterCompletion( final int status ) {
            if ( status == TransactionSynchronization.STATUS_COMMITTED ) {
                timer.schedule( new TimerTask() {
                    @Override
                    public void run() {
                        for ( Pair<EntityInvalidationEvent,Functions.Nullary<Boolean>> eventAndChecker : events ) {
                            EntityInvalidationEvent eie = eventAndChecker.left;
                            Functions.Nullary<Boolean> requiredCallback = eventAndChecker.right;

                            if ( requiredCallback.call() ) {
                                dispatchInvalidation(eie);
                            }
                        }
                    }
                }, 0L );
            }
        }
    }
}
