package com.l7tech.server.audit;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.cluster.ClusterLock;
import static com.l7tech.server.ServerConfig.*;
import com.l7tech.server.event.system.AuditArchiverEvent;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.audit.AuditRecord;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * Archives and deletes audit records off-box.
 *
 * The archive process is triggered when the configured limits for database disk space usage have been reached.
 *
 * @author jbufu
 */
public class AuditArchiver implements InitializingBean, ApplicationContextAware, PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(AuditArchiver.class.getName());


    // Filled in by Spring
    private final ServerConfig serverConfig;
    private final ClusterPropertyManager clusterPropertyManager;
    private PlatformTransactionManager transactionManager;
    private AuditRecordManager recordManager;

    private int shutdownThreshold;
    private int startThreshold;
    private int stopThreshold;
    private int batchSize;
    private static final int MAX_BATCH_SIZE = 10000;
    public static final int MYSQL_STATS_UPDATE_SLEEP_WAIT = 15000; // milliseconds

    private Timer timer;
    private Lock lock;
    private Auditor auditor;
    private ApplicationContext applicationContext;

    private long staleTimeout;
    private long timerPeriod;
    private TimerTask timerTask;

    private ArchiveReceiver archiveReceiver;

    public AuditArchiver(ServerConfig serverConfig, ClusterPropertyManager cpm,
                         PlatformTransactionManager tm, AuditRecordManager arm, ArchiveReceiver ar) throws FindException {
        if (serverConfig == null)
            throw new NullPointerException("ServerConfig parameter must not be null.");
        if (cpm == null)
            throw new NullPointerException("ClusterPropertyManager parameter must not be null.");
        if (arm == null)
            throw new NullPointerException("AuditRecordManager parameter must not be null.");
        if (ar == null)
            throw new NullPointerException("ArchiveReceiver parameter must not be null.");

        this.serverConfig = serverConfig;
        this.clusterPropertyManager = cpm;
        this.transactionManager = tm;
        this.recordManager = arm;
        this.archiveReceiver = ar;

        this.staleTimeout = this.serverConfig.getLongProperty(PARAM_AUDIT_ARCHIVER_STALE_TIMEOUT, 120);
        lock = getNewLock();

        timer = new Timer();
    }

    private Lock getNewLock() {
        // makes sure the same cluster property is used for each new lock
        return new ClusterLock(clusterPropertyManager, transactionManager, PARAM_AUDIT_ARCHIVER_IN_PROGRESS, staleTimeout);
    }

    public void afterPropertiesSet() throws Exception {
        this.auditor = new Auditor(this, getApplicationContext(), logger);
        reloadConfig();
        logger.info("Audit Archiver initialized.");
    }

    private void reschedule() {
        if (timer == null) {
            logger.warning("Timer is cancelled; unable to reschedule!");
            return;
        }

        logger.info("(Re)Scheduling Audit Archiver timer for " + timerPeriod / 1000 + " seconds.");

        if (timerTask != null)
            timerTask.cancel();

        timerTask = new AuditArchiverTimerTask();
        timer.schedule(timerTask, 0, timerPeriod);
    }

    private void reloadConfig() {
        logger.info("Reloading configuration.");

        shutdownThreshold = serverConfig.getIntProperty(PARAM_AUDIT_ARCHIVER_SHUTDOWN_THRESHOLD, 90);
        startThreshold = serverConfig.getIntProperty(PARAM_AUDIT_ARCHIVER_START_THRESHOLD, 75);
        stopThreshold = serverConfig.getIntProperty(PARAM_AUDIT_ARCHIVER_STOP_THRESHOLD, 50);
        batchSize = serverConfig.getIntProperty(PARAM_AUDIT_ARCHIVER_BATCH_SIZE, 100);

        long newStaleTimeout = serverConfig.getLongProperty(PARAM_AUDIT_ARCHIVER_STALE_TIMEOUT, 120);
        if (newStaleTimeout != staleTimeout) {
            logger.info("Setting new cluster lock stale timeout: " + newStaleTimeout + " minutes.");
            staleTimeout = newStaleTimeout;
            lock = getNewLock();
        }

        long newPeriod = serverConfig.getLongProperty(PARAM_AUDIT_ARCHIVER_TIMER_PERIOD, 10) * 1000;
        if (timer != null && newPeriod != timerPeriod) {
            timerPeriod = newPeriod;
            reschedule();
        }
    }

    /**
     * Checks audit records disk space usage and triggers the required actions:
     * <ul>
     * <li>stops the SSG from processing messages, if above stop_processing threshold</li>
     * <li>starts archive job, if above start_archive threshold</li>
     * </ul>
     *
     * <p>The actual archive job is started in a separate thread, with cluster-wide concurrency = 1</p>
     */
    private void trigger() {
        logger.info("Starting Audit Archiver check.");

        if (currentUsageCheck(0) < startThreshold) {
            logger.info("Below start_archive threshold, not starting archiver thread.");
            return;
        }

        // allow the above to be executed by subsequent timer invocations
        // even if one archive() run takes longer than the timer period
        Thread archiver = new Thread(new Runnable() {
            public void run() {
                boolean gotLock = false;
                try {
                    // do not allow more than one archive job
                    gotLock = lock.tryLock();
                    if (gotLock) {
                        if (logger.isLoggable(Level.FINE)) logger.fine("Got lock: " + lock);
                        archive();
                    } else {
                        logger.warning("NOT starting Audit Archiver job; could not get cluster lock for: " + lock);
                    }
                } finally {
                    // make sure the lock is released, no matter what happens with this thread
                    if (gotLock) lock.unlock();
                }
            }
        });
        archiver.start();
    }

    private void ssgRestart() {
        if (MessageProcessor.Lock.INSTANCE.isSuspended()) {
            logger.warning("Restarting SSG message processing");
            MessageProcessor.Lock.INSTANCE.resume();
            auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_MESSAGE_PROCESSING_RESTARTED);
            getApplicationContext().publishEvent(new AuditArchiverEvent(this));
        }
    }

    private void ssgSuspend(String reason) {
        if ( !MessageProcessor.Lock.INSTANCE.isSuspended() ) {
            logger.severe("Suspending SSG message processing: " + reason);
            MessageProcessor.Lock.INSTANCE.suspend(reason);
            auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_MESSAGE_PROCESSING_SUSPENDED, reason);
            getApplicationContext().publishEvent(new AuditArchiverEvent(this));
        }
    }

    /**
     * Checks the current database usage.
     *
     * @param adjustment   size (in bytes) to be subtracted from the currently used space.
     * @return             database usage %.
     */
    private int currentUsageCheck(long adjustment) {
        int usage;
        int adjustedUsage;

        try {
            long max = recordManager.getMaxTableSpace();

            if (max == -1) {
                auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_ERROR, "Max innodb tablespace size not defined.");
                stopTimer();

            } else if (max > 0) {
                usage = (int) (100L * recordManager.getCurrentUsage() / max);
                adjustedUsage = (int) (100L * (recordManager.getCurrentUsage() - adjustment) / max);

                if (adjustment > 0)
                    logger.info("Projected database space usage is " + adjustedUsage + "%");
                else
                    logger.info("Current database space usage is " + usage + "%");

                // use real usage for actions that modify the processing state
                if (usage >= shutdownThreshold )
                    ssgSuspend("Audit records database disk usage exceeded emergency limit (" + usage + " >= " + shutdownThreshold + ")");
                else
                    ssgRestart();

                return adjustment > 0 ? adjustedUsage : usage;
            } else {
                throw new FindException("Invalid value defined for max innodb tablespace: " + max);
            }
        } catch (FindException e) {
            auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_ERROR, new String[] {" could not retrieve current usage"}, e);
        } finally {
            getApplicationContext().publishEvent(new AuditArchiverEvent(this));
        }

        return 0; // don't do anything else for now
    }

    private void stopTimer() {
        if (timer != null) {
            auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_ERROR, "Cancelling Audit Archiver timer.");
            timer.cancel();
            timer = null;
        }
    }

    private void archive() {
        auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_JOB_STARTED);

        while (true) { // loop until the unadjusted (zipBytesArchived==0) usage check drops below the stop threshold
            long initialStartOid = Long.MIN_VALUE;
            long maxOidArchived = Long.MIN_VALUE;
            long zipBytesArchived = 0;

            while (stopThreshold <= (currentUsageCheck(zipBytesArchived))) {
                // get starting point
                long startOid;
                try {
                    startOid = recordManager.getMinOid(maxOidArchived);
                } catch (Exception e) {
                    auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_ERROR, new String[]{"Error getting lowest audit record object id."}, e);
                    break;
                }
                if (initialStartOid == Long.MIN_VALUE) initialStartOid = startOid;
                if (startOid == -1) {
                    auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_ERROR, "Error getting lowest audit record object id.");
                    break;
                }
                long endOid = startOid + (batchSize > MAX_BATCH_SIZE ? MAX_BATCH_SIZE : batchSize);

                // archive
                try {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Archiving audit records with objectid in [" + startOid + " : " + endOid + "]");

                    AuditExporter.ExportedInfo result = archiveReceiver.archiveRecords(startOid, endOid);
                    if (result == null) break;

                    maxOidArchived = result.getHighestId();
                    zipBytesArchived += result.getTransferredBytes(); // using this for heuristics, since req/resp message are archived in the DB as well

                } catch (Exception e) {
                    auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_ERROR, new String[]{"Error archiving audit records."}, e);
                    break;
                }
            }

            // flush & delete
            try {
                if (initialStartOid == Long.MIN_VALUE && maxOidArchived == Long.MIN_VALUE) {
                    break; // dropped below the start threshold at the previous iteration
                } else if (maxOidArchived < initialStartOid || maxOidArchived == Long.MIN_VALUE) {
                    auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_ERROR, "No records were saved by the configured archive receiver!");
                    break;
                } else if (archiveReceiver.flush()) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Deleting audit records with objectid in [" + initialStartOid + " : " + maxOidArchived + "]");
                    recordManager.deleteRangeByOid(initialStartOid, maxOidArchived);
                    auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_JOB_ARCHIVED, Long.toString(initialStartOid), Long.toString(maxOidArchived));
                    Thread.sleep(MYSQL_STATS_UPDATE_SLEEP_WAIT); // wait for the stats to update
                } else {
                    auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_ERROR, "Error flushing archive receiver; NOT deleting any records.");
                    break;
                }
            } catch (SQLException e) {
                auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_ERROR, new String[]{"Error deleting audit records."}, e);
            } catch (InterruptedException e) {
                logger.warning("Interrupted while waiting for MySQL stats to update.");
            }
        } // while (true)

        auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_JOB_COMPLETE);
        getApplicationContext().publishEvent(new AuditArchiverEvent(this));
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        reloadConfig();
    }

    public void runNow() {
        auditor.logAndAudit(SystemMessages.AUDIT_ARCHIVER_IMMEDIATE_TRIGGER);
        reschedule();
        getApplicationContext().publishEvent(new AuditArchiverEvent(this));
    }

    private class AuditArchiverTimerTask extends TimerTask {
        public void run() {
            trigger();
        }
    }
}
