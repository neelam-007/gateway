/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.example.manager.apidemo;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.ServiceUsage;
import com.l7tech.objectmodel.FindException;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that displays the service usage statistics for a SecureSpan Gateway cluster for
 * version 3.5.
 */
public class ShowClusterStatistics {
    private static Logger logger = Logger.getLogger(ShowClusterStatistics.class.getName());
    private SsgAdminSession session;

    public static void main(String[] args) throws Exception {
        final String[] fargs = args;
        Subject.doAsPrivileged(new Subject(), new PrivilegedAction() {
            public Object run() {
                try {
                    ShowClusterStatistics.run(fargs);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "exception running Identity Provider", e);
                }
                return null;
            }
        }, null);
    }

    public static void run(String[] args) throws Exception {
        ShowClusterStatistics me = new ShowClusterStatistics(Main.SSGHOST, Main.ADMINACCOUNT_NAME, Main.ADMINACCOUNT_PASSWD);
        String[] res = me.getClusterStatus();
        for (String s : res) {
            logger.info(s);
        }
        res = me.getServiceUsage();
        for (String s : res) {
            logger.info(s);
        }
    }

    public ShowClusterStatistics(SsgAdminSession session) {
        this.session = session;
    }

    public ShowClusterStatistics(String ssghost, String login, String passwd) throws MalformedURLException, LoginException, RemoteException {
        session = new SsgAdminSession(ssghost, login, passwd);
    }

    public String[] getClusterStatus() throws RemoteException, FindException {
        ClusterStatusAdmin stub = session.getClusterStatusAdmin();
        ClusterNodeInfo[] res = stub.getClusterStatus();
        String[] out = new String[res.length];
        for (int i = 0; i < res.length; i++) {
            long secs = System.currentTimeMillis() - res[i].getLastUpdateTimeStamp();
            secs /= 1000;
            out[i] = "Node " + res[i].getName() + " loaded " + res[i].getAvgLoad() + " last updated " + secs + " seconds ago.";
        }
        return out;
    }

    public String[] getServiceUsage() throws RemoteException, FindException {
        ClusterStatusAdmin stub = session.getClusterStatusAdmin();
        ServiceUsage[] res = stub.getServiceUsage();
        if (res == null) {
            res = new ServiceUsage[0];
        }
        String[] output = new String[res.length];
        for (int i = 0; i < res.length; i++) {
            output[i] = "Service " + res[i].getServiceid() + " got " + res[i].getRequests() + " requests.";
        }
        return output;
    }
}
