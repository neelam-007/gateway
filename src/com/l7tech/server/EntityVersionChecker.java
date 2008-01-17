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
public class EntityVersionChecker implements ApplicationContextAware, ApplicationListener, InitializingBean, DisposableBean {
    
    //- PUBLIC

    /**
     * Set the list of managers whose entities should be checked.
     *
     * @param managers the List of HibernateEntityManagers
     * @throws IllegalStateException if the managers are already set
     * @throws ClassCastException if the list contains a non-HibernateEntityManager
     */
    public void setEntityManagers(List<EntityManager<PersistentEntity,EntityHeader>> managers) {
        if(btt!=null) throw new IllegalStateException("manager already set");
        if(managers!=null && !managers.isEmpty()) {
            List<EntityInvalidationVersionCheck> tasks = new ArrayList<EntityInvalidationVersionCheck>();
            for (EntityManager<PersistentEntity,EntityHeader> manager : managers) {
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
     * Listen for and asynchronously dispatch entity admin events.
     */
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof Created ) {
            handleAdminInvalidation((PersistenceEvent) event, new char[]{EntityInvalidationEvent.CREATE});
        } else if ( event instanceof Updated ) {
            handleAdminInvalidation((PersistenceEvent) event, new char[]{EntityInvalidationEvent.UPDATE});
        } else if ( event instanceof Deleted ) {
            handleAdminInvalidation((PersistenceEvent) event, new char[]{EntityInvalidationEvent.DELETE});
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

    private ApplicationEventPublisher eventSink;
    private Timer timer;
    private long interval = 15 * 1000;
    private BigTimerTask btt;

    private void handleAdminInvalidation(final PersistenceEvent event, char[] operation) {
        PersistentEntity entity = (PersistentEntity) event.getEntity();
        Class<? extends Entity> typeClass = null;

        for ( EntityInvalidationVersionCheck check : btt.tasks ) {
            if ( check.isOfInterest( entity ) ) {
                typeClass = check.entityType;
                break;
            }
        }

        if ( typeClass != null ) {
            enqueueInvalidation( new EntityInvalidationEvent( event.getSource(), typeClass, new long[]{entity.getOid()}, operation) );
        }
    }

    private void enqueueInvalidation( final EntityInvalidationEvent eie ) {
        if ( logger.isLoggable( Level.FINE ))
            logger.log( Level.FINE, "Queuing invalidation for entities: {0}", asList(eie.getEntityIds()) );

        DispatchingTransactionSynchronization dts = null;

        //noinspection unchecked
        for ( TransactionSynchronization ts : (List<TransactionSynchronization>) TransactionSynchronizationManager.getSynchronizations() ) {
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

        dts.events.add( eie );
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

        private EntityInvalidationVersionCheck(EntityManager<PersistentEntity, EntityHeader> manager) throws Exception {
            super(manager);
            entityType = manager.getInterfaceClass();
        }

        public boolean isOfInterest( Entity entity ) {
            return entityType.isInstance( entity );
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
        final List<EntityInvalidationEvent> events = new ArrayList<EntityInvalidationEvent>(); 

        @Override
        public void afterCompletion( final int status ) {
            if ( status == TransactionSynchronization.STATUS_COMMITTED ) {
                timer.schedule( new TimerTask() {
                    @Override
                    public void run() {
                        for ( EntityInvalidationEvent eie : events ) {
                            dispatchInvalidation(eie);
                        }
                    }
                }, 10L );
            }
        }
    }
}
