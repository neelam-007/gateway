package com.l7tech.console.auditalerts;

import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.objectmodel.FindException;
import com.l7tech.console.logging.ErrorManager;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

            AuditSearchCriteria crit = getAuditSearchCriteria(lastAcknowledged);
            Collection coll = admin.find(crit);
            boolean hasAlerts = !coll.isEmpty();
            for (AuditWatcher auditWatcher : auditWatchers) {
                auditWatcher.alertsAvailable(hasAlerts);
            }
            if (hasAlerts) {
                stopTimer();
            } else {
                startTimer();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (FindException e) {
            logger.warning("Error while checking for new Audits: [" + e.getMessage() + "]");
        }
    }

    public void start() {
        startTimer();
    }

    public void stop() {
        stopTimer();
    }

    public void updateAuditsAcknowledgedTime() {
        try {
            AuditAdmin admin = auditAdmin;
            if (admin != null) {
                lastAcknowledged = admin.getLastAcknowledgedAuditDate();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
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
            int interval = configBean.getAuditCheckInterval();
            timer = new Timer(interval*1000,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            if (configBean.isEnabled())
                                checkForNewAlerts();
                        } catch(Exception exception) {
                            ErrorManager.getDefault().notify(Level.WARNING, exception, "Error checking audit alerts.");
                        }
                    }
            });
        }
        return timer;
    }

    private AuditSearchCriteria getAuditSearchCriteria(Date lastAckedTime) {
        Level currentLevel = configBean.getAuditAlertLevel();
        return new AuditSearchCriteria(lastAckedTime, null, currentLevel, null, null, null, 0,0,1);
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
        getTimer().setDelay(configBean.getAuditCheckInterval() * 1000);

        if (configBean.isEnabled())
            startTimer();
        else
            logger.fine("Audit alerts disabled");
    }
}
