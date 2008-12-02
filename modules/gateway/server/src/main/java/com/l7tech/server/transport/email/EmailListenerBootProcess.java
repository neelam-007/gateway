package com.l7tech.server.transport.email;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.*;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.transport.email.asynch.EmailListenerThreadPool;
import org.springframework.context.ApplicationEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starts up the email listeners and periodically checks for new, removed or modified email listeners.
 * This class is started during startup of the SSG.
 */
public class EmailListenerBootProcess extends LifecycleBean implements PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(EmailListenerBootProcess.class.getName());

    /** Server configuration properties */
    private final ServerConfig serverConfig;
    /** Persisted/configured email listener manager */
    private final EmailListenerManager emailListenerManager;
    private final String clusterNodeId;

    /** Mutex */
    private final Object listenerLock = new Object();
    /** Factory used to instantiate new PollingEmailListener objects */
    private PollingEmailListenerFactory pollingEmailListenerFactory;
    /** Set of all active inbound JMS listeners */
    private Set<PollingEmailListener> activeListeners = new HashSet<PollingEmailListener>();
    /** Background timer used to check for email listener updates */
    private final Timer backgroundTimer;
    /** Boolean flag specifying whether the email listeners have been started */
    private boolean started = false;
    /** Boolean flag specifying whether doStart() should start the listeners */
    private boolean canBeStarted = false;

    // it is expected that these checkers are run on the same thread
    private EmailListenerVersionChecker emailListenerChecker;

    /**
     * Constructor.
     *
     * @param serverConfig configuration properties
     * @param licenseManager licence manager
     * @param emailListenerManager Persisted email listener manager
     * @param timer timer object used by the connection/endpoint update checker
     */
    public EmailListenerBootProcess(ServerConfig serverConfig,
                          LicenseManager licenseManager,
                          EmailListenerManager emailListenerManager,
                          String clusterNodeId,
                          Timer timer)
    {
        super("Email Listener Boot Process", logger, GatewayFeatureSets.SERVICE_JMS_MESSAGE_INPUT, licenseManager);

        this.serverConfig = serverConfig;
        this.emailListenerManager = emailListenerManager;
        this.clusterNodeId = clusterNodeId;

        // create a timer if one is not supplied
        if (timer == null)
            this.backgroundTimer = new Timer("Email listener config refresh", true);
        else
            this.backgroundTimer = timer;

        this.backgroundTimer.schedule(new TimerTask() {
            public void run() {
                List<EmailListener> stolenListeners = EmailListenerBootProcess.this.emailListenerManager.stealSubscriptionsFromDeadNodes(EmailListenerBootProcess.this.clusterNodeId);
                for(EmailListener emailListener : stolenListeners) {
                    EmailListenerBootProcess.this.emailListenerUpdated(emailListener, true);
                }
            }
        }, 60 * 1000, 60 * 1000);
    }

    /**
     * For handling cluster property changes to the following properties.
     *
     * @param evt the change event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        synchronized(listenerLock) {
            for (PollingEmailListener listener : activeListeners) {
                listener.propertyChange(evt);
            }
        }
    }

    @Override
    protected void init() {
        if(serverConfig == null) throw new IllegalStateException("serverConfig is required.");
        if(emailListenerManager == null) throw new IllegalStateException("emailListenerManager is required.");
    }

    /**
     * Starts {@link com.l7tech.server.transport.email.PollingEmailListener}s for the initial configuration.
     * Also starts the EndpointVersionChecker and EmailListenerVersionChecker to periodically
     * check whether email listeners have been created, updated or deleted.
     */
    @Override
    protected void doStart() throws LifecycleException {
        if (isStarted())
            return;

        if(canBeStarted) {
            try {
                startListeners();
            } catch (LifecycleException e) {
                logger.log(Level.SEVERE, "Unable to start Email Listener", e);
            }
        } else {
            logger.info("The Email Listener subsystem will not start until the gateway is ready to process messages.");
        }
    }

    /**
     * Starts all configured EmailListeners.
     *
     * @throws LifecycleException when problems occur during Email Listeners subsystem startup
     */
    private void startListeners() throws LifecycleException {
        synchronized(listenerLock) {
            if (!started) {
                try {
                    started = true;  // "started" just means that we have already once attempted to start the email listener subsystem
                    logger.info("Email listeners starting.");

                    // Start up listeners for initial configuration
                    Collection<EmailListener> emailListeners = emailListenerManager.getEmailListenersForNode(clusterNodeId);
                    List<Long> staleListeners = new ArrayList<Long>();

                    for (EmailListener emailListener : emailListeners) {
                        if (!emailListener.isActive()) continue;

                        // need to check to see if it's Queue-based or pub/sub (topic)?
                        EmailListenerConfig emailListenerCfg = new EmailListenerConfig(emailListener, getApplicationContext());
                        PollingEmailListener pollingListener = pollingEmailListenerFactory.createListener(emailListenerCfg, emailListenerManager);

                        try {
                            pollingListener.start();
                            activeListeners.add(pollingListener);
                        } catch (LifecycleException e) {
                            logger.log(Level.WARNING, "Couldn't start email listener " + emailListener.getName()
                                    + ".  Will retry periodically", e);
                            staleListeners.add(emailListener.getOid());
                        }
                    }

                    // create the version checkers
                    emailListenerChecker = new EmailListenerVersionChecker(emailListenerManager);

                    for (Long oid : staleListeners) {
                        emailListenerChecker.markObjectAsStale(oid);
                    }

                } catch ( FindException e ) {
                    String msg = "Couldn't start email listener subsystem!  Email listener functionality will be disabled.";
                    logger.log( Level.SEVERE, msg, e );
                    throw new LifecycleException( msg, e );
                }

                // Start periodic check timer
                backgroundTimer.schedule( emailListenerChecker, emailListenerChecker.getFrequency() * 2,
                                        emailListenerChecker.getFrequency() );

                logger.info("Email listeners started.");
            }
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if ( isEventIgnorable(applicationEvent) ) {
            return;
        }

        super.onApplicationEvent(applicationEvent);

        if(applicationEvent instanceof ReadyForMessages) {
            canBeStarted = true;
        }
        
        if (!isStarted())
            return;

        if (applicationEvent instanceof ReadyForMessages) {
            try {
                startListeners();
            } catch (LifecycleException e) {
                logger.log(Level.SEVERE, "Unable to start Email Listener", e);
            }
        }
    }

    /**
     * Attempts to stop all running email listeners, then stops the EmailListenerVersionChecker
     */
    @Override
    protected void doStop() {
        synchronized(listenerLock) {
            if (emailListenerChecker != null)
                emailListenerChecker.cancel();

            if (backgroundTimer != null)
                backgroundTimer.cancel();

            for (PollingEmailListener listener : activeListeners) {
                logger.info("Stopping email listener '" + listener.toString() + "'");
                listener.stop();
            }
            for (PollingEmailListener listener : activeListeners) {
                logger.info("Waiting for email listener to stop '" + listener.toString() + "'");
                listener.ensureStopped();
            }
            activeListeners.clear();

            // shutdown the EmailListenerThreadPool (whether it's used or not)
            EmailListenerThreadPool.getInstance().shutdown();
        }
    }

    private boolean isEventIgnorable(ApplicationEvent applicationEvent) {
        return applicationEvent instanceof AuditDetailEvent ||
                applicationEvent instanceof MessageProcessed ||
                applicationEvent instanceof FaultProcessed;
    }

    /**
     * Handles the event generated by the discovery of the deletion of an EmailListener.
     *
     * @param deletedEmailListenerOid the OID of the email listener that has been deleted.
     */
    private void emailListenerDeleted( long deletedEmailListenerOid ) {
        synchronized(listenerLock) {
            /*
             * TODO: need to implement
             */
            PollingEmailListener listener = null;
            for (Iterator<PollingEmailListener> i = activeListeners.iterator(); i.hasNext();) {
                listener = i.next();
                if (listener.getEmailListenerOid() == deletedEmailListenerOid ) {
                    listener.stop();
                    i.remove();
                }
            }
        }
    }


    /**
     * Handles the event fired by the update or creation of a email listener.
     *
     * Calls emailListenerDeleted to shut down any PollingEmailListener objects that might already
     * be listening.
     *
     * @param updatedEmailListener the EmailListener that has been created or updated.
     */
    private void emailListenerUpdated( EmailListener updatedEmailListener, boolean notify ) {
        /*
         * TODO: need to implement
         */
        // Stop any existing PollingEmailListener objects for this email listener configuration.
        emailListenerDeleted( updatedEmailListener.getOid() );

        if (!updatedEmailListener.isActive() || !clusterNodeId.equals(updatedEmailListener.getEmailListenerState().getOwnerNodeId())) return;
        PollingEmailListener newListener = null;
        try {
            EmailListenerConfig newEmailListenerCfg = new EmailListenerConfig(updatedEmailListener, getApplicationContext());
            newListener = pollingEmailListenerFactory.createListener(newEmailListenerCfg, emailListenerManager);
            newListener.start();
            synchronized(listenerLock) {
                activeListeners.add(newListener);
            }
        } catch (LifecycleException e) {
            logger.warning("Exception while initializing email listener " + updatedEmailListener.getName() +
                            "; will try again later: " + e.toString());
            synchronized(listenerLock) {
                if (emailListenerChecker != null)
                    emailListenerChecker.markObjectAsStale( updatedEmailListener.getOid() );
            }
        }
    }


    /**
     * Sets the PollingEmailListenerFactory.
     *
     * @param newFactory the new factory
     */
    public void setPollingEmailListenerFactory(PollingEmailListenerFactory newFactory) {
        this.pollingEmailListenerFactory = newFactory;
    }

    /**
     * Periodically checks for new, updated or deleted email listener configurations
     */
    private class EmailListenerVersionChecker extends PeriodicVersionCheck {
        EmailListenerVersionChecker(EmailListenerManager mgr ) throws FindException {
            super(mgr);
        }

        @Override
        protected void onDelete( long removedOid ) {
            logger.info( "Email Listener " + removedOid + " deleted!" );
            emailListenerDeleted( removedOid );
        }

        @Override
        protected void onUpdate( PersistentEntity updatedEntity ) {
            logger.info( "EmailListener " + updatedEntity.getOid() + " updated!" );
            emailListenerUpdated( (EmailListener)updatedEntity, true );
        }

        @Override
        protected void onCreate(PersistentEntity createdEntity) {
            logger.info( "Email Listener " + createdEntity.getOid() + " created!" );
            emailListenerUpdated( (EmailListener)createdEntity, true );
        }

        @Override
        public boolean notifyUpdate(long updatedOid, int version) {
            return super.notifyUpdate(updatedOid, version);
        }
    }
}
