package com.l7tech.console.util;

import com.l7tech.common.util.UptimeMetrics;
import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.ServiceStatistics;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.logging.LogAdmin;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;

/*
 * This class retrieves statistics from a node.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class StatisticsWorker extends SwingWorker {

    private UptimeMetrics metrics = null;
    private Vector statsList = new Vector();

    private LogAdmin logstub = null;
    private ServiceAdmin serviceManager = null;
    static Logger logger = Logger.getLogger(StatisticsWorker.class.getName());

    public StatisticsWorker(ServiceAdmin manager, LogAdmin log){
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
                        // todo: the line above invokes the method and the line below
                        // checks for null? The NPE clock is ticking.... em28102003
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
