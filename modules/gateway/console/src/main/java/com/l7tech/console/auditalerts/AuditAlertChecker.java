package com.l7tech.console.auditalerts;

import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.console.logging.ErrorManager;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.remoting.RemoteAccessException;

/**
 * User: megery
 * Date: Nov 8, 2006
 * Time: 2:51:08 PM
 */
public class AuditAlertChecker {

    //- PUBLIC

    public AuditAlertChecker(AuditAlertConfigBean configBean) {
        this.configBean = configBean;
        auditWatchers = new ArrayList<AuditWatcher>();        
    }

    public void setAuditAdmin(AuditAdmin auditAdmin) {
        this.auditAdmin = auditAdmin;

        // reset for new gateway
        lastAcknowledged = null;
    }

    public void addWatcher(AuditWatcher watcher) {
        auditWatchers.add(watcher);
    }

    public void checkForNewAlerts() {
        logger.fine("Checking for new Audits");
        try {
            AuditAdmin admin = auditAdmin;
            if (admin == null)
                return;

            if (lastAcknowledged == null) {
                lastAcknowledged = admin.getLastAcknowledgedAuditDate();

                if (lastAcknowledged == null) {
                    lastAcknowledged = new Date(0);
                }
            }

            //check if there are new audits to grab
            long alertTime = admin.hasNewAudits(lastAcknowledged, configBean.getAuditAlertLevel());
            if ( alertTime > 0 ) {
                for (AuditWatcher auditWatcher : auditWatchers) {
                    auditWatcher.alertsAvailable(alertTime!=0, alertTime);
                }
            }
            
            if (alertTime!=0) {
                stopTimer();
            } else {
                startTimer();
            }
        } catch (RemoteAccessException e) {
            // bzilla #3741, we may no longer be connected
            logger.log(Level.WARNING, "Could not access Gateway to update this. Perhaps the connection to the Gateway timed out.", e);
        }
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

    private final AuditAlertConfigBean configBean;
    private final List<AuditWatcher> auditWatchers;
    private AuditAdmin auditAdmin;
    private Timer timer;
    private Date lastAcknowledged;

    private Timer getTimer() {
        if (timer == null) {
            timer = new Timer(getDelay(),
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            if (configBean.isEnabled())
                                checkForNewAlerts();
                        } catch(Exception exception) {
                            ErrorManager.getDefault().notify(Level.WARNING, exception, "Error checking audit alerts.");
                        }
                    }
            });
            timer.setInitialDelay(1000);
        }
        return timer;
    }

    private void startTimer() {
        Timer timer = getTimer();

        if (!timer.isRunning()) {
            logger.fine("Starting Audit Alert Timer (check interval = " + timer.getDelay() + ")");
            timer.start();
        }
    }

    private void stopTimer() {
        Timer timer = getTimer();

        if (timer.isRunning()) {
            logger.fine("Stopping Audit Alert Timer");
            timer.stop();
        }
    }

    private void reset() {
        logger.fine("Audit alert options changed.");
        stopTimer();

        logger.fine("setting audit alert timer to " + String.valueOf(configBean.getAuditCheckInterval()));
        Timer timer = getTimer();
        int delay = getDelay();
        timer.setInitialDelay(delay);
        timer.setDelay(delay);

        if (configBean.isEnabled())
            startTimer();
        else
            logger.fine("Audit alerts disabled");
    }

    private int getDelay() {
        return configBean.getAuditCheckInterval() * 1000;
    }
}
