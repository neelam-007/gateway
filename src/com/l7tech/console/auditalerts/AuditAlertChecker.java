package com.l7tech.console.auditalerts;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 8, 2006
 * Time: 2:51:08 PM
 */
public class AuditAlertChecker {
    private static final Logger logger = Logger.getLogger(AuditAlertChecker.class.getName());

    public static final String CLUSTER_PROP_LAST_AUDITACK_TIME = "audit.acknowledge.highestTime";

    private final SimpleDateFormat formatter = new SimpleDateFormat();

    AuditAlertConfigBean configBean;
    Timer timer;
    private ClusterStatusAdmin clusterAdmin;
    private AuditAdmin auditAdmin;
    private List<AuditWatcher> auditWatchers;

    public AuditAlertChecker(AuditAlertConfigBean configBean) {
        this.configBean = configBean;
        auditWatchers = new ArrayList<AuditWatcher>();        
    }

    public void addWatcher(AuditWatcher watcher) {
        auditWatchers.add(watcher);
    }

    private Timer getTimer() {
        if (timer == null) {
            int interval = configBean.getAuditCheckInterval();
            timer = new Timer(interval*1000,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (configBean.isEnabled()) checkForNewAlerts();
                    }
            });
        }
        return timer;
    }

    public void checkForNewAlerts() {
        logger.fine("Checking for new Audits");
        try {
             ClusterProperty lastAckedProperty = clusterAdmin.findPropertyByName(CLUSTER_PROP_LAST_AUDITACK_TIME);
             if (lastAckedProperty == null) {
                 String nowString = formatter.format(Calendar.getInstance().getTime());
                 logger.info("No \"" + CLUSTER_PROP_LAST_AUDITACK_TIME +"\" Cluster Property found, creating new one with date = " + nowString);
                 lastAckedProperty = new ClusterProperty(CLUSTER_PROP_LAST_AUDITACK_TIME, nowString);
                 clusterAdmin.saveProperty(lastAckedProperty);
                 startTimer();
             }
             else {
                 try {
                     Date lastAckedTime = formatter.parse(lastAckedProperty.getValue());
                     AuditSearchCriteria crit = getAuditSearchCriteria(lastAckedTime);
                     Collection coll = auditAdmin.find(crit);
                     if (coll.size() > 0) {
                         for (AuditWatcher auditWatcher : auditWatchers) {
                             auditWatcher.alertsAvailable(true);
                         }
                         stopTimer();
                     } else {
                        for (AuditWatcher auditWatcher : auditWatchers) {
                            auditWatcher.alertsAvailable(false);
                        }
                        startTimer();
                     }
                 } catch (ParseException e) {
                    logger.warning("Invalid date in " + CLUSTER_PROP_LAST_AUDITACK_TIME + " cluster property [" + e.getMessage() + "]");
                 }
            }
         } catch (RemoteException e) {
            logger.warning("Error while checking for new Audits: [" + e.getMessage() + "]");
        } catch (FindException e) {
            logger.warning("Error while checking for new Audits: [" + e.getMessage() + "]");
        } catch (SaveException e) {
             logger.warning("Error while checking for new Audits: [" + e.getMessage() + "]");
         } catch (DeleteException e) {
             logger.warning("Error while checking for new Audits: [" + e.getMessage() + "]");
         } catch (UpdateException e) {
             logger.warning("Error while checking for new Audits: [" + e.getMessage() + "]");
         }
    }

    private AuditSearchCriteria getAuditSearchCriteria(Date lastAckedTime) {
        Level currentLevel = configBean.getAuditAlertLevel();
        return new AuditSearchCriteria(lastAckedTime, null, currentLevel, null, null, null, 0,0,0);
    }

    public void start() {
        startTimer();
    }

    public void stop() {
        stopTimer();
    }

    public void setAuditAdmin(AuditAdmin auditAdmin) {
        this.auditAdmin = auditAdmin;
    }

    public void setClusterAdmin(ClusterStatusAdmin clusterStatusAdmin) {
        clusterAdmin = clusterStatusAdmin;
    }

    private void startTimer() {
        if (!getTimer().isRunning()) {
            logger.fine("Starting Audit Alert Timer (check interval = " + getTimer().getDelay() + ")");
            getTimer().start();
        }
    }

    private void stopTimer() {
        logger.fine("Stopping Audit Alert Timer");

        if (getTimer().isRunning())
            getTimer().stop();
    }

    public void updateAuditsAcknowledgedTime() {
        try {
            ClusterProperty prop = clusterAdmin.findPropertyByName(CLUSTER_PROP_LAST_AUDITACK_TIME);
            if (prop!=null) {
                prop.setValue(formatter.format(Calendar.getInstance().getTime()));
                clusterAdmin.saveProperty(prop);
            }
        } catch (RemoteException e) {
            logger.warning("Error while updating the " + CLUSTER_PROP_LAST_AUDITACK_TIME + " cluster property [" + e.getMessage() +"]");
        } catch (SaveException e) {
            logger.warning("Error while updating the " + CLUSTER_PROP_LAST_AUDITACK_TIME + " cluster property [" + e.getMessage() +"]");
        } catch (UpdateException e) {
            logger.warning("Error while updating the " + CLUSTER_PROP_LAST_AUDITACK_TIME + " cluster property [" + e.getMessage() +"]");
        } catch (DeleteException e) {
            logger.warning("Error while updating the " + CLUSTER_PROP_LAST_AUDITACK_TIME + " cluster property [" + e.getMessage() +"]");
        } catch (FindException e) {
            logger.warning("Error while updating the " + CLUSTER_PROP_LAST_AUDITACK_TIME + " cluster property [" + e.getMessage() +"]");
        }
    }

    public void updateSettings(boolean enabled, int checkInterval, Level checkLevel) {
        configBean.setEnabled(enabled);
        configBean.setAuditCheckInterval(checkInterval);
        configBean.setAuditAlertLevel(checkLevel);
        reset();
    }

    private void reset() {
        logger.fine("Audit alert options changed.");
        boolean wasRunning = getTimer().isRunning();

        if (wasRunning)
            stopTimer();

        logger.fine("setting audit alert timer to " + String.valueOf(configBean.getAuditCheckInterval()));
        getTimer().setDelay(configBean.getAuditCheckInterval());

        if (configBean.isEnabled())
            startTimer();
        else
            logger.fine("Audit alerts disabled");
    }
}
