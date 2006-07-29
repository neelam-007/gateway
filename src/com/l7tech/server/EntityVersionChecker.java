package com.l7tech.server;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

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

    /**
     * Creates an uninitialized checker.
     */
    public EntityVersionChecker() {
    }

    /**
     * Set the list of managers whose entities should be checked.
     *
     * @param managers the List of HibernateEntityManagers
     * @throws IllegalStateException if the managers are already set
     * @throws ClassCastException if the list contains a non-HibernateEntityManager
     */
    public void setEntityManagers(List<HibernateEntityManager> managers) {
        if(btt!=null) throw new IllegalStateException("manager already set");
        if(managers!=null && !managers.isEmpty()) {
            List<EntityInvalidationVersionCheck> tasks = new ArrayList<EntityInvalidationVersionCheck>();
            for (HibernateEntityManager manager : managers) {
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
     * Initialize the component.
     */
    public void afterPropertiesSet() throws Exception {
        if(btt!=null) {
            timer = new Timer();
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
        if(this.applicationContext!=null) throw new IllegalStateException("applicationContext is already set");
        this.applicationContext = applicationContext;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(EntityVersionChecker.class.getName());

    private ApplicationContext applicationContext;
    private Timer timer;
    private long interval = 15 * 1000;
    private BigTimerTask btt;

    /**
     * Publish invalidation information for the given entityies
     */
    private void performInvalidation(Class entityType, List oids) {
        if(!oids.isEmpty()) {
            long[] oidArray = new long[oids.size()];
            Iterator oidIter = oids.iterator();
            for (int i = 0; i < oidArray.length; i++) {
                oidArray[i] = ((Long) oidIter.next()).longValue();
            }

            if(logger.isLoggable(Level.FINE))
                logger.fine("Invalidating entities of type '"+entityType.getName()+"' ids are "+oids+".");

            applicationContext.publishEvent(new EntityInvalidationEvent(this, entityType, oidArray));
        }
    }

    /**
     * Run a collection of TimerTasks as one task.
     */
    private class BigTimerTask extends TimerTask {
        private final List<? extends TimerTask> tasks;

        private BigTimerTask(List<? extends TimerTask> subTimerTasks) {
            this.tasks = subTimerTasks;
        }

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
     * Version check task for an HibernateEntityManager
     */
    private class EntityInvalidationVersionCheck extends PeriodicVersionCheck {

        private final Class entityType;
        private List<Long> invalidationList;

        private EntityInvalidationVersionCheck(EntityManager manager) throws Exception {
            super(manager);
            entityType = manager.getInterfaceClass();
        }

        public void run() {
            invalidationList = new ArrayList<Long>();
            super.run();
            performInvalidation(entityType, invalidationList);
            invalidationList = null;
        }

        protected void onDelete(long removedOid) {
            if(invalidationList!=null) {
                invalidationList.add(Long.valueOf(removedOid));
            }
        }

        protected boolean preSave(long updatedOid, int updatedVersion) {
            if(invalidationList!=null) {
                invalidationList.add(Long.valueOf(updatedOid));
            }
            return false;
        }
    }
}
