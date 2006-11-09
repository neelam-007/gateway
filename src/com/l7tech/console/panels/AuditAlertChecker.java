package com.l7tech.console.panels;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.console.AuditWatcher;
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
        createTimer();
    }

    public void addWatcher(AuditWatcher watcher) {
        auditWatchers.add(watcher);
    }
    
    private void createTimer() {
        int interval = configBean.getAuditCheckInterval();
        timer = new Timer(interval*1000,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (configBean.isEnabled()) checkForNewAlerts();
                }
            });
    }

    public void checkForNewAlerts() {
        logger.info("Checking for new Audits");
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
                     e.printStackTrace();
                 }
            }
         } catch (RemoteException e1) {
            e1.printStackTrace();
        } catch (FindException e1) {
            e1.printStackTrace();
        } catch (SaveException e) {
             e.printStackTrace();
         } catch (DeleteException e) {
             e.printStackTrace();
         } catch (UpdateException e) {
             e.printStackTrace();
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
        if (!timer.isRunning()) {
            logger.info("Starting Audit Alert Timer (check interval = " + timer.getDelay() + ")");
            timer.start();
        }
    }

    private void stopTimer() {
        logger.info("Stopping Audit Alert Timer");
        if (timer.isRunning()) timer.stop();
    }

    public void updateAuditsAcknowledgedTime() {
        try {
            ClusterProperty prop = clusterAdmin.findPropertyByName(CLUSTER_PROP_LAST_AUDITACK_TIME);
            if (prop!=null) {
                prop.setValue(formatter.format(Calendar.getInstance().getTime()));
                clusterAdmin.saveProperty(prop);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (SaveException e) {
            e.printStackTrace();
        } catch (UpdateException e) {
            e.printStackTrace();
        } catch (DeleteException e) {
            e.printStackTrace();
        } catch (FindException e) {
            e.printStackTrace();
        }
    }
}
