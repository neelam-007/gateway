package com.l7tech.console.util;

import com.l7tech.common.util.Locator;
import com.l7tech.common.util.UptimeMetrics;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.ServiceStatistics;
import com.l7tech.console.table.FilteredLogTableModel;
import com.l7tech.adminws.logging.Log;
import com.ibm.xml.policy.xacl.builtIn.provisional_action.log;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Oct 14, 2003
 * Time: 10:48:13 AM
 * To change this template use Options | File Templates.
 */
public class StatisticsWorker extends SwingWorker {

    private UptimeMetrics metrics = null;
    private Vector statsList = new Vector();

    private Log logstub = null;
    private com.l7tech.adminws.service.ServiceManager serviceManager = null;
    static Logger logger = Logger.getLogger(StatisticsWorker.class.getName());

    public StatisticsWorker(com.l7tech.adminws.service.ServiceManager manager, Log log){
        serviceManager = manager;
        logstub = log;
    }

    public Vector getStatsList(){
        return statsList;
    }

    public UptimeMetrics getMetrics(){
        return metrics;
    }

    public Object construct() {

        // retrieve server metrics
        try {
            metrics = logstub.getUptime();
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "Unable to retrieve server metrics from server", e);
            metrics = null;
        }


        // retrieve service statistics
        com.l7tech.objectmodel.EntityHeader[] entityHeaders = null;

        try {
            entityHeaders = serviceManager.findAllPublishedServices();

            EntityHeader header = null;
            for (int i = 0; i < entityHeaders.length; i++) {

                header = entityHeaders[i];
                if (header.getType().toString() == com.l7tech.objectmodel.EntityType.SERVICE.toString()) {

                    ServiceStatistics stats = null;

                    try {
                        stats = serviceManager.getStatistics(header.getOid());
                        stats.setServiceName(header.getName());

                        if (stats != null) {
                            statsList.add(stats);

                        }

                    } catch (RemoteException e) {
                        logger.log(Level.SEVERE, "Unable to retrieve statistics from server", e);
                    }

                }

            }
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "Remote exception when retrieving published services from server", e);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find all published services from server", e);
        }

        // return a dummy object
        return statsList;
    }
}
