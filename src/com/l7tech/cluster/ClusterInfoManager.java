package com.l7tech.cluster;

import com.l7tech.common.util.HexUtils;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.UpdateException;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hibernate layer over the cluster_info table.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Dec 17, 2003<br/>
 * $Id$
 *
 */
public class ClusterInfoManager {

    /**
     * determines whether this ssg belongs to a cluster or operates by itself
     * @return true if this ssg is a node inside a cluster
     */
    public boolean isCluster() {
        synchronized (this) {
            if (isCluster < 0) {
                ClusterInfo selfCI = getSelfNodeId();
                if (selfCI != null) {
                    isCluster = 1;
                } else {
                    isCluster = 0;
                }
            }
        }
        if (isCluster == 1) return true;
        if (isCluster == 0) return false;
        logger.warning("should not get here");
        return false;
    }

    /**
     * allows a node to update its status in the cluster_info table
     *
     * @param avgLoad the average load for the last minute
     */
    public void updateSelfStatus(double avgLoad) throws UpdateException {
        long now = System.currentTimeMillis();
        ClusterInfo selfCI = getSelfNodeId();
        if (selfCI != null) {
            selfCI.setAvgLoad(avgLoad);
            selfCI.setLastUpdateTimeStamp(now);
            try {
                HibernatePersistenceContext pc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
                Session session = pc.getSession();
                // update existing data
                session.update(selfCI);
            } catch (SQLException e) {
                String msg = "error updating db";
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            } catch (HibernateException e) {
                String msg = "error updating db";
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            }
        } else {
            logger.warning("cannot retrieve db entry for this node.");
        }
    }

    /**
     * allows a node to update its boot timestamp in the cluster_info table
     */
    public void updateSelfUptime() throws UpdateException {
        long newuptimevalue = System.currentTimeMillis();
        ClusterInfo selfCI = getSelfNodeId();
        if (selfCI != null) {
            selfCI.setUptime(newuptimevalue);
            selfCI.setLastUpdateTimeStamp(newuptimevalue);
            try {
                HibernatePersistenceContext pc = (HibernatePersistenceContext)PersistenceContext.getCurrent();
                Session session = pc.getSession();
                // update existing data
                session.update(selfCI);
            } catch (SQLException e) {
                String msg = "error updating db";
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            } catch (HibernateException e) {
                String msg = "error updating db";
                logger.log(Level.WARNING, msg, e);
                throw new UpdateException(msg, e);
            }
        } else {
            logger.warning("cannot retrieve db entry for this node.");
        }
    }

    /**
     *
     * @return a collection containing ClusterInfo objects. if the collection is empty, it means that
     * the SSG operated by itself outsides a cluster.
     */
    public Collection retrieveClusterStatus() throws FindException {
        // get all objects from that table
        String queryall = "from " + TABLE_NAME + " in class " + ClusterInfo.class.getName();
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            return context.getSession().find(queryall);
        } catch (SQLException e) {
            String msg = "error retrieving cluster status";
            logger.log(Level.WARNING, msg, e);
            throw new FindException(msg, e);
        }  catch (HibernateException e) {
            String msg = "error retrieving cluster status";
            logger.log(Level.WARNING, msg, e);
            throw new FindException(msg, e);
        }
    }

    /**
     * determines this node's nodeid value
     */
    private ClusterInfo getSelfNodeId() {
        synchronized (this) {
            // special query, dont do this everytime
            // (cache return value as this will not change while the server is up)
            if (selfId == null) {
                Iterator macs = getMacs().iterator();
                // find out which mac works for us
                while (macs.hasNext()) {
                    String mac = (String)macs.next();
                    ClusterInfo output = getNodeStatusFromDB(mac);
                    if (output != null) {
                        selfId = mac;
                        return output;
                    }
                }
            }
        }
        if (selfId != null) return getNodeStatusFromDB(selfId);
        else return null;
    }

    private ClusterInfo getNodeStatusFromDB(String mac) {
        String query = "from " + TABLE_NAME + " in class " + ClusterInfo.class.getName() +
                       " where " + TABLE_NAME + "." + MAC_COLUMN_NAME + " = " + mac;
        HibernatePersistenceContext context = null;
        List hibResults = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            hibResults = context.getSession().find(query);
        } catch (SQLException e) {
            String msg = "error retrieving cluster status";
            logger.log(Level.WARNING, msg, e);
        }  catch (HibernateException e) {
            String msg = "error retrieving cluster status";
            logger.log(Level.WARNING, msg, e);
        }
        if (hibResults == null || hibResults.isEmpty()) {
            return null;
        }
        switch (hibResults.size()) {
            case 0:
                break;
            case 1:
                return (ClusterInfo)hibResults.get(0);
            default:
                logger.warning("this should not happen. more than one entry found" +
                                          "for mac: " + selfId);
                break;
        }
        return null;
    }

    /**
     * get the mac addresses for the network adapters on this server
     *
     * @return a collection containing strings representing mac addresses in the following format:
     * XX:XX:XX:XX:XX:XX
     */
    private Collection getMacs() {
        ArrayList output = new ArrayList();
        output.addAll(getIfconfigMac());
        output.addAll(getIpconfigMac());
        return output;
    }

    /**
     * called by getMacs()
     *
     * tries to get mac addresses by running unix ifconfig command
     */
    private Collection getIfconfigMac() {
        Process up = null;
        InputStream got = null;
        String ifconfigOutput = null;
        ArrayList output = new ArrayList();
        try {
            try {
                up = Runtime.getRuntime().exec("/sbin/ifconfig");
                got = new BufferedInputStream(up.getInputStream());
                byte[] buff = HexUtils.slurpStream(got, 4096);
                ifconfigOutput = new String(buff);
                up.waitFor();
            } finally {
                if (got != null)
                    got.close();
                if (up != null)
                    up.destroy();
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "error getting ifconfig", e);
        } catch (InterruptedException e) {
            logger.log(Level.FINE, "error getting ifconfig", e);
        }
        // ifconfig output pattern
        //eth0    Link encap:Ethernet  HWaddr 00:0C:6E:69:8D:CA
        //        inet addr:192.168.1.227  Bcast:192.168.1.255  Mask:255.255.255.0
        //        UP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1
        //        RX packets:5015473 errors:2 dropped:1 overruns:0 frame:0
        //        TX packets:69559 errors:0 dropped:0 overruns:0 carrier:0
        //        collisions:0 txqueuelen:100
        //        RX bytes:357230125 (340.6 Mb)  TX bytes:6648567 (6.3 Mb)
        //        Interrupt:11 Base address:0x1000
        //
        //lo      Link encap:Local Loopback
        //        inet addr:127.0.0.1  Mask:255.0.0.0
        //        UP LOOPBACK RUNNING  MTU:16436  Metric:1
        //        RX packets:666667 errors:0 dropped:0 overruns:0 frame:0
        //        TX packets:666667 errors:0 dropped:0 overruns:0 carrier:0
        //        collisions:0 txqueuelen:0
        //        RX bytes:90058336 (85.8 Mb)  TX bytes:90058336 (85.8 Mb)
        if (ifconfigOutput == null) return Collections.EMPTY_LIST;
        String[] splitted = breakIntoLines(ifconfigOutput);
        for (int i = 0; i < splitted.length; i++) {
            Matcher matchr = ifconfigMacPattern.matcher(splitted[i]);
            if (matchr.matches()) {
                output.add(matchr.group(1));
            }
        }
        return output;
    }

    /**
     * called by getMacs().
     *
     * tries to get mac addresses by running windows ipconfig command
     */
    private Collection getIpconfigMac() {
        Process up = null;
        InputStream got = null;
        String ipconfigOutput = null;
        ArrayList output = new ArrayList();
        try {
            try {
                up = Runtime.getRuntime().exec("ipconfig /all");
                got = new BufferedInputStream(up.getInputStream());
                byte[] buff = HexUtils.slurpStream(got, 4096);
                ipconfigOutput = new String(buff);
                up.waitFor();
            } finally {
                if (got != null)
                    got.close();
                if (up != null)
                    up.destroy();
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "error getting ipconfig", e);
        } catch (InterruptedException e) {
            logger.log(Level.FINE, "error getting ipconfig", e);
        }
        // ipconfig output
        // ... stuff
        // Physical Address. . . . . . . . . . . : XX-XX-XX-XX-XX-XX
        // ... more stuff
        if (ipconfigOutput == null) return Collections.EMPTY_LIST;
        String[] splitted = breakIntoLines(ipconfigOutput);
        for (int i = 0; i < splitted.length; i++) {
            Matcher matchr = ipconfigMacPattern.matcher(splitted[i]);
            if (matchr.matches()) {
                String match = matchr.group(1);
                match = match.replace('-', ':');
                output.add(match);
            }
        }
        return output;
    }

    private static String[] breakIntoLines(String in) {
        return Pattern.compile("$+", Pattern.MULTILINE).split(in);
    }

    public static void main(String[] args) {
        ClusterInfoManager me = new ClusterInfoManager();
        for (Iterator i = me.getMacs().iterator(); i.hasNext();) {
            System.out.println("MAC Match: " + i.next());
        }
    }

    private static final String TABLE_NAME = "cluster_info";
    private static final String MAC_COLUMN_NAME = "mac";
    private static Pattern ifconfigMacPattern = Pattern.compile(".*HWaddr\\s+(\\w\\w.\\w\\w.\\w\\w." +
                                                                "\\w\\w.\\w\\w.\\w\\w).*", Pattern.DOTALL);
    private static Pattern ipconfigMacPattern = Pattern.compile(".*Physical Address.*(\\w\\w.\\w\\w.\\w\\w." +
                                                                "\\w\\w.\\w\\w.\\w\\w).*", Pattern.DOTALL);
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    private int isCluster = -1;
    private String selfId = null;
}
