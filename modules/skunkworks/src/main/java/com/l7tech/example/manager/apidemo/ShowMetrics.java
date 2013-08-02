/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.example.manager.apidemo;

import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsSummaryBin;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that displays the service usage statistics for a SecureSpan Gateway cluster.
 */
public class ShowMetrics {
    private static Logger logger = Logger.getLogger(ShowMetrics.class.getName());
    private SsgAdminSession session;

    public static void main(String[] args) throws Exception {
        final String[] fargs = args;
        Subject.doAsPrivileged(new Subject(), new PrivilegedAction() {
            public Object run() {
                try {
                    ShowMetrics.run(fargs);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "exception running Identity Provider", e);
                }
                return null;
            }
        }, null);
    }

    public static void run(String[] args) throws Exception {
        ShowMetrics me = new ShowMetrics(Main.SSGHOST, Main.ADMINACCOUNT_NAME, Main.ADMINACCOUNT_PASSWD);
        me.outputMetrics();
    }

    public ShowMetrics(SsgAdminSession session) {
        this.session = session;
    }

    public ShowMetrics(String ssghost, String login, String passwd) throws MalformedURLException, LoginException, RemoteException {
        session = new SsgAdminSession(ssghost, login, passwd);
    }

    public void outputMetrics() throws RemoteException, FindException {
        ClusterStatusAdmin stub = session.getClusterStatusAdmin();
        // get metrics for periods of 5 seconds, for the last 15 seconds
        Collection<MetricsSummaryBin> newBins = stub.summarizeLatestByPeriod(null, null, MetricsBin.RES_FINE, 15000, true);

        System.out.println("Number of metrics bin: " + newBins.size());
        for (MetricsSummaryBin msb : newBins) {
            System.out.println("Service id: " + msb.getServiceGoid());

            System.out.println("Start time of metrics bin " + DateFormat.getTimeInstance(DateFormat.LONG).format(
                    new Date(msb.getStartTime()))
            );
            System.out.println("\tTotal requests: " + msb.getNumAttemptedRequest());
            System.out.println("\tAttempted Rate: " + msb.getActualAttemptedRate());
            System.out.println("\tCompleted Rate: " + msb.getActualCompletedRate());
            System.out.println("\tAvg Response Time: " + msb.getAverageFrontendResponseTime());
        }
    }
}
