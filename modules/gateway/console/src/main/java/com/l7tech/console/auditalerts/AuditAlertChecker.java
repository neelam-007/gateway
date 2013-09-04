package com.l7tech.console.auditalerts;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.console.logging.ErrorManager;

import java.util.*;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.springframework.remoting.RemoteAccessException;

import javax.swing.*;

/**
 * User: megery
 * Date: Nov 8, 2006
 * Time: 2:51:08 PM
 */
public class AuditAlertChecker {

    //- PUBLIC

    public AuditAlertChecker( final AuditAlertConfigBean configBean ) {
        this.configBean = configBean;
        auditWatchers = new ArrayList<AuditWatcher>();        
    }

    public void setAuditAdmin( final AuditAdmin auditAdmin ) {
        this.auditAdmin = auditAdmin;

        // reset for new gateway
        lastAcknowledged = null;
    }

    public void addWatcher( final AuditWatcher watcher ) {
        auditWatchers.add(watcher);
    }

    public void start() {
        startTimer();
    }

    public void stop() {
        stopTimer();
    }

    public void updateAuditsAcknowledgedTime() {
        AuditAdmin admin = auditAdmin;
        if (admin != null) {
            lastAcknowledged = admin.markLastAcknowledgedAuditDate();
        }
    }

    public void updateSettings(boolean enabled, int checkInterval, Level checkLevel) {
        configBean.setEnabled(enabled);
        configBean.setAuditCheckInterval(checkInterval);
        configBean.setAuditAlertLevel(checkLevel);
        reset();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AuditAlertChecker.class.getName());

    private static final String PROP_PREFIX = "com.l7tech.console";
    private static final long DELAY_INITIAL = ConfigFactory.getLongProperty(PROP_PREFIX + ".auditAlertChecker.serverSideDelay.initial", 50L);
    private static final long DELAY_CAP = ConfigFactory.getLongProperty(PROP_PREFIX + ".auditAlertChecker.serverSideDelay.maximum", 5000L);
    private static final double DELAY_MULTIPLIER = SyspropUtil.getDouble(PROP_PREFIX + ".auditAlertChecker.serverSideDelay.multiplier", 1.6);

    private final AuditAlertConfigBean configBean;
    private final List<AuditWatcher> auditWatchers;
    private AuditAdmin auditAdmin;
    private Timer timer;
    private TimerTask timerTask;
    private Date lastAcknowledged;

    private Timer getTimer() {
        if (timer == null) {
            timer = new Timer("Audit Alert Timer", true);
        }
        return timer;
    }

    private void startTimer() {
        if ( timerTask == null ) {
            logger.fine("Starting Audit Alert Timer (check interval = " + getDelay() + ")");
            rescheduleTask();
        }
    }

    private void stopTimer() {
        if ( timerTask != null ) {
            logger.fine("Stopping Audit Alert Timer");
            timerTask.cancel();
            timerTask = null;
        }
    }

    /**
     * Stop any current task and schedule a new one.
     */
    private void rescheduleTask() {
        final Timer timer = getTimer();

        stopTimer();

        timerTask = new TimerTask(){
            @Override
            public void run() {
                try {
                    if ( configBean.isEnabled() ) {
                        checkForNewAlerts();
                    }
                } catch ( final Throwable exception ) {
                    ErrorManager.getDefault().notify(Level.WARNING, exception, "Error checking audit alerts.");
                }
            }
        };

        timer.schedule( timerTask, 1000L, getDelay() );
    }

    private void reset() {
        logger.fine( "Audit alert options changed." );
        logger.fine("setting audit alert timer to " + String.valueOf(configBean.getAuditCheckInterval()));

        if ( configBean.isEnabled() ) {
            rescheduleTask();
        } else {
            logger.fine("Audit alerts disabled");
        }
    }

    private long getDelay() {
        return (long)configBean.getAuditCheckInterval() * 1000L;
    }

    private void checkForNewAlerts() {
        logger.fine("Checking for new Audits");
        try {
            final AuditAdmin admin = auditAdmin;
            if ( admin == null )
                return;

            if ( lastAcknowledged == null ) {
                lastAcknowledged = admin.getLastAcknowledgedAuditDate();

                if ( lastAcknowledged == null ) {
                    lastAcknowledged = new Date(0L);
                }
            }

            //check if there are new audits to grab
            final long alertTime = getHasNewAudits(admin);
            if ( alertTime > 0L ) {
                SwingUtilities.invokeLater( new Runnable(){
                    @Override
                    public void run() {
                        for ( final AuditWatcher auditWatcher : auditWatchers ) {
                            auditWatcher.alertsAvailable(alertTime!=0L, alertTime);
                        }
                    }
                } );
            }

            if ( alertTime != 0L ) {
                stopTimer();
            }
        } catch (RemoteAccessException e) {
            // bzilla #3741, we may no longer be connected
            logger.log(Level.WARNING, "Cannot connect to Gateway to check for audit alerts.", ExceptionUtils.getDebugException(e));
            //bug 9648 - allow handler chain to correctly manage this remote exception
            throw e;
        }
    }

    /**
     * @param admin  the audit API
     * @return  date of the first available audit or 0 if none are available or query error occurs
     */
    private long getHasNewAudits(AuditAdmin admin) {
        AsyncAdminMethods.JobId<Long> jobId = admin.hasNewAudits(lastAcknowledged,configBean.getAuditAlertLevel());
        double delay = DELAY_INITIAL;
        try{
            Thread.sleep((long)delay);
            while( true ) {
                final String status = admin.getNewAuditsJobStatus( jobId );
                if ( status == null ) {
                    logger.warning("Server could not find our new audits query job ID");
                    break;
                } else if ( !status.startsWith( "a" ) ) {
                    final AsyncAdminMethods.JobResult<Long> jobResult = admin.getNewAuditsJobResult( jobId );
                    if ( jobResult.result != null ) {
                        return  jobResult.result ;
                    } else {
                        logger.warning("Server returned a null job result");
                    }
                }
                delay = delay >= DELAY_CAP ? DELAY_CAP : delay * DELAY_MULTIPLIER;
                Thread.sleep((long)delay);
            }
        } catch (InterruptedException e) {
            // expected do nothing
        } catch (AsyncAdminMethods.JobStillActiveException e) {
            logger.warning("Server could not find our new audits query job ID");
        } catch (AsyncAdminMethods.UnknownJobException e) {
            logger.warning("Server could not find our new audits query job ID");
        }
        return 0;
    }
}
