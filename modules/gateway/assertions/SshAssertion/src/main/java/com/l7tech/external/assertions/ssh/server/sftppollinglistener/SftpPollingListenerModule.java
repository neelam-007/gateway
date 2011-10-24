package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.LifecycleBean;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.Stopping;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SFTP polling listener module (aka boot process).
 */
public class SftpPollingListenerModule extends LifecycleBean implements PropertyChangeListener, ApplicationListener {
    private static final Logger logger = Logger.getLogger(SftpPollingListenerModule.class.getName());

    //Thread pool for tasks. Managed by this module.
    private final ThreadPoolBean threadPoolBean;

    /** The ClusterPropertyManager - used to determine whether the configurations have been modified*/
    private final ClusterPropertyManager clusterPropertyManager;

    /** Persisted/configured inbound config manager */
    private final SftpPollingListenerResourceManager sftpPollingResourceManager;

    /** Mutex */
    private final Object listenerLock = new Object();

    /** Set of all active listeners */
    private Set<SftpPollingListener> activeListeners = new HashSet<SftpPollingListener>();

    /** Boolean flag specifying whether the listeners have been started */
    private boolean started = false;

    /**
     * Single constructor for module.
     *
     * @param threadPoolBean inbound listener thread pool
     * @param cpManager cluster property manager
     * @param licenseManager license manager
     * @param resourceManager configuration manager (interfaces with cfg persistence store)
     */
    public SftpPollingListenerModule(final ThreadPoolBean threadPoolBean,
                                     final ClusterPropertyManager cpManager,
                                     final LicenseManager licenseManager,
                                     final SftpPollingListenerResourceManager resourceManager)
    {
        // set feature as modular assertion
        super("SFTP Polling Listener module", logger, "set:modularAssertions", licenseManager);

        this.threadPoolBean = threadPoolBean;
        this.clusterPropertyManager = cpManager;
        this.sftpPollingResourceManager = resourceManager;
    }

    @Override
    protected void init() {
        if(threadPoolBean == null) throw new IllegalStateException("threadPoolBean is required.");
        if(sftpPollingResourceManager == null) throw new IllegalStateException("resourceManager is required.");
    }

    /**
     * Simply log the start, doesn't really do anything.  See onApplicationEvent(...) for start logic.
     */
    @Override
    protected void doStart() throws LifecycleException {
        if (isStarted())
            return;
        logger.info("The SFTP polling subsystem will not start until the gateway is ready to process messages.");
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if ( isEventIgnorable(event) ) {
            return;
        }

        super.onApplicationEvent(event);

        if (!isStarted())
            return;

        if (event instanceof Stopping) {
            // do stop?
        } else if (event instanceof ReadyForMessages) {
            try {
                threadPoolBean.start();
                startListeners();
            } catch (LifecycleException e) {
                logger.log(Level.SEVERE, "Unable to start SFTP polling Listener", e);
            }
        } else if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eiEvent = (EntityInvalidationEvent) event;
            if (ClusterProperty.class.equals(eiEvent.getEntityClass())) {
                handleClusterPropertyChange(eiEvent);
            }
        }
    }

    /**
     * Starts all configured listeners.
     *
     * @throws com.l7tech.server.LifecycleException when problems occur during subsystem startup
     */
    private void startListeners() throws LifecycleException {
        synchronized(listenerLock) {
            if (!started) {
                // this section needs to lookup all inbound settings and start the listeners
                try {
                    started = true;  // "started" just means that we have already once attempted to start the listener subsystem
                    logger.info("SFTP polling listener starting.");

                    // Start up listeners for initial configuration
                    List<Long> staleListeners = new ArrayList<Long>();
                    SftpPollingListenerConfig sftpPollingListenerCfg;

                    // initialize ResourceManager from cluster properties
                    sftpPollingResourceManager.init();

                    for (SftpPollingListenerResource sftpPollingCfg : sftpPollingResourceManager.getListenerConfigurations()) {
                        if (sftpPollingCfg.isActive()) {
                            sftpPollingListenerCfg = new SftpPollingListenerConfig(sftpPollingCfg, getApplicationContext());

                            logger.info("Instantiating SFTP polling listener for " + sftpPollingCfg.toString());
                            SftpPollingListener sftpPollListener = new SftpPollingListenerThreadPoolFileHandler(sftpPollingListenerCfg, threadPoolBean);

                            try {
                                sftpPollListener.start();
                                activeListeners.add(sftpPollListener);
                            } catch (LifecycleException e) {
                                logger.log(Level.WARNING, "Couldn't start listener for " + sftpPollingCfg.getName() + ".  Will retry periodically", e);

                                // what to do when we encounter bad stuff
                                staleListeners.add(sftpPollingCfg.getResId());
                            }
                        }
                    }
                } catch ( Exception e ) {
                    // this catch needs to be done to ensure we have all the configurations required for the listeners to function properly
                    String msg = "Couldn't start SFTP polling listener subsystem! This functionality will be disabled.";
                    logger.log( Level.SEVERE, msg, e );
                    throw new LifecycleException( msg, e );
                }

                logger.info("SFTP polling connector started.");

                // add debug as needed
            }
        }
    }

    /**
     * For handling cluster property changes to the following properties.
     *
     * @param evt the change event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        synchronized(listenerLock) {
            for (SftpPollingListener listener : activeListeners) {
                listener.propertyChange(evt);
            }
        }
    }

    private void handleClusterPropertyChange(EntityInvalidationEvent eiEvent) {
        for (long oid : eiEvent.getEntityIds()) {
            try {
                ClusterProperty cp = clusterPropertyManager.findByPrimaryKey(oid);
                if (cp != null && cp.getOid() == sftpPollingResourceManager.getConfigPropertyOid()) {
                    resourceUpdated();
                }
            } catch (FindException e) {
                logger.log(Level.FINE, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }
    
    /**
     * Attempts to stop all running SFTP polling listeners.
     */
    @Override
    protected void doStop() {
        synchronized(listenerLock) {
            for (SftpPollingListener listener : activeListeners) {
                logger.info("Stopping SFTP polling receiver '" + listener.toString() + "'");
                listener.stop();
            }
            for (SftpPollingListener listener : activeListeners) {
                logger.info("Waiting for SFTP polling receiver to stop '" + listener.toString() + "'");
                listener.ensureStopped();
            }
            activeListeners.clear();

            threadPoolBean.shutdown();
        }
    }

    private boolean isEventIgnorable(ApplicationEvent applicationEvent) {
        return applicationEvent instanceof AuditDetailEvent ||
                applicationEvent instanceof MessageProcessed ||
                applicationEvent instanceof FaultProcessed;
    }

    /**
     * Handles the event fired by update of a resource.
     */
    private void resourceUpdated() {
        synchronized(listenerLock) {
            try {
                SftpPollingListenerResourceManager.UpdateStatus[] changed = sftpPollingResourceManager.onUpdate();
                if (changed != null && changed.length > 0) {
                    StringBuffer sb = new StringBuffer("SFTP polling listener(s) changed: ");
                    for (SftpPollingListenerResourceManager.UpdateStatus resource : changed) {
                        sb.append(resource.status + " - ").append(resource.name + "(id ").append(resource.resId).append(").  ");
                    }
                    logger.info(sb.toString());

                    SftpPollingListenerResource res;
                    for (SftpPollingListenerResourceManager.UpdateStatus one : changed) {
                        if (one.status == SftpPollingListenerResourceManager.UpdateStatus.DELETE) {
                            deleteListener( one.resId );
                        } else {
                            res = sftpPollingResourceManager.getResourceByResId( one.resId );
                            updateListener(res);
                        }
                    }
                }
            } catch ( Exception e ) { // FindException
                logger.log( Level.SEVERE, "Caught exception finding a listener!", e );
            }
        }
    }

    /**
     * Handles the event generated by the discovery of the deletion of a listener.
     *
     * @param deletedOid the OID of the listener that has been deleted.
     */
    private void deleteListener( long deletedOid ) {
        synchronized(listenerLock) {
            SftpPollingListener listener;
            for (Iterator<SftpPollingListener> i = activeListeners.iterator(); i.hasNext();) {
                listener = i.next();
                if ( listener.getSftpPollingListenerResourceId() == deletedOid ) {
                    listener.stop();
                    i.remove();
                }
            }
        }
    }

    /**
     * Handles the event fired by the update or creation of a listener.
     *
     * Calls deleteListener to shut down any listener(s) that might already be listening to that configuration.
     *
     * @param updated the resource that has been created or updated.
     */
    private void updateListener( SftpPollingListenerResource updated ) {
        // Stop any existing listener for this configuration
        deleteListener( updated.getResId() );

        if (!updated.isActive())
            return;

        SftpPollingListener newListener = null;
        try {
            SftpPollingListenerConfig newCfg = new SftpPollingListenerConfig(updated, getApplicationContext());
            newListener = new SftpPollingListenerThreadPoolFileHandler(newCfg, threadPoolBean);

            SftpPollingListenerResource resource = newCfg.getSftpPollingListenerResource();
            logger.info("Starting SFTP polling listener " + resource.getName() + " on " + resource.getHostname());
            newListener.start();
            synchronized(listenerLock) {
                activeListeners.add(newListener);
            }
        } catch (LifecycleException e) {
            logger.warning("Exception while initializing receiver " + newListener + "; will try again later: " + e.toString());
        }
    }
}