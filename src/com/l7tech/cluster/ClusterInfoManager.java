package com.l7tech.cluster;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Hibernate layer over the cluster_info table.
 *
 * This manager will look for a row in the cluster_info that represent this server using mac addresses.
 * If no row is found for this server, the manager will create one.
 *
 * This is a singleton to avoid going through the process of finding which row applies to this server
 * using mac address every time.
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
     * get the manager
     */
    public static ClusterInfoManager getInstance() {
        return SingletonHolder.singleton;
    }

    /**
     * returns the node id to which this server applies to
     */
    public String thisNodeId() {
        if (selfId != null) return selfId;
        ClusterNodeInfo selfCI = getSelfNodeInf();
        if (selfCI != null) return selfCI.getMac();
        return null;
    }

    /**
     * allows a node to update its status in the cluster_info table
     *
     * @param avgLoad the average load for the last minute
     */
    public void updateSelfStatus(double avgLoad) throws UpdateException {
        long now = System.currentTimeMillis();
        ClusterNodeInfo selfCI = getSelfNodeInf();

        // recover from a table purge while a node is running (bugzilla #860)
        if (selfCI == null) {
            logger.warning("cannot retrieve record for this node. perhaps the table was cleared since last update." +
                           " attempting to recreate row");
            selfCI = recreateRow();
        }

        if (selfCI == null) {
            logger.warning("record for this node is not accessible and attempt to recreate it failed");
        } else {
            selfCI.setAvgLoad(avgLoad);
            selfCI.setLastUpdateTimeStamp(now);
            boolean isMaster = isMasterMode();
            selfCI.setIsMaster(isMaster);
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
        }
    }

    public void deleteNode(String nodeid) throws DeleteException {
        ClusterNodeInfo node = getNodeStatusFromDB(nodeid);
        if (node == null) {
            String msg = "that node cannot be retrieved";
            logger.log(Level.WARNING, msg);
            throw new DeleteException(msg);
        }
        // check that the node is indeed stale
        long lastUpdateForNodeToDelete = node.getLastUpdateTimeStamp();
        // node must be stale for at least 30 seconds to allow for delete
        if ((System.currentTimeMillis() - lastUpdateForNodeToDelete) < 30000) {
            String msg = "the node to delete has not been stale long enough to" +
                         "allow for deletion from database. Try again later.";
            logger.warning("admin trying to delete un-stale node " + msg);
            throw new DeleteException(msg);
        }
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            context.getSession().delete(node);
        } catch (SQLException e) {
            String msg = "error deleting cluster status";
            logger.log(Level.WARNING, msg, e);
        }  catch (HibernateException e) {
            String msg = "error deleting cluster status";
            logger.log(Level.WARNING, msg, e);
        }
    }

    public void renameNode(String nodeid, String newnodename) throws UpdateException {
        ClusterNodeInfo node = getNodeStatusFromDB(nodeid);
        if (node == null) {
            String msg = "that node cannot be retrieved";
            logger.log(Level.WARNING, msg);
            throw new UpdateException(msg);
        }
        node.setName(newnodename);
        try {
            recordNodeInDB(node);
        } catch (SQLException e) {
            String msg = "error saving node's new name";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        } catch (HibernateException e) {
            String msg = "error saving node's new name";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }
    }

    /**
     * this should be called
     * when the server boots. it updates the boot time and the ip address in the cluster_status
     * table.
     */
    public void updateSelfUptime() throws UpdateException {
        long newboottimevalue = System.currentTimeMillis();
        ClusterNodeInfo selfCI = getSelfNodeInf();
        if (selfCI != null) {
            selfCI.setBootTime(newboottimevalue);
            selfCI.setLastUpdateTimeStamp(newboottimevalue);
            try {
                String add = null;
                add = InetAddress.getLocalHost().getHostAddress();
                selfCI.setAddress(add);
            } catch (UnknownHostException e) {
                logger.warning("cannot get localhost address: " + e.getMessage());
            }
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
            rememberedBootTime = newboottimevalue;
        } else {
            logger.warning("cannot retrieve db entry for this node.");
        }
    }

    /**
     * @return a collection containing ClusterNodeInfo objects. if the collection is empty, it means that
     * the SSG operated by itself outsides a cluster.
     */
    public Collection retrieveClusterStatus() throws FindException {
        // get all objects from that table
        String queryall = "from " + TABLE_NAME + " in class " + ClusterNodeInfo.class.getName();
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
    public synchronized ClusterNodeInfo getSelfNodeInf() {
        if (selfId != null) {
            return getNodeStatusFromDB(selfId);
        } else {
            // special query, dont do this everytime
            // (cache return value as this will not change while the server is up)
            Iterator macs = getMacs().iterator();
            String anymac = null;
            // find out which mac works for us
            while (macs.hasNext()) {
                String mac = (String)macs.next();
                anymac = mac;
                ClusterNodeInfo output = getNodeStatusFromDB(mac);
                if (output != null) {
                    logger.finest("Using server " + output.getName() + " (" + output.getMac() + ")");
                    selfId = mac;
                    return output;
                }
            }

            // no existing row for us. create one
            if (anymac != null) {
                return selfPopulateClusterDB(anymac);
            }

            logger.severe("should not get here. this server has no mac?");
            return null;
        }
    }

    private ClusterNodeInfo recreateRow() {
        ClusterNodeInfo recreatedNodeInfo = selfPopulateClusterDB(selfId);
        if (recreatedNodeInfo == null) {
            logger.warning("failed to recreated node info row in table.");
            return null;
        } else {
            if (rememberedBootTime > -1) {
                recreatedNodeInfo.setBootTime(rememberedBootTime);
            }
            return recreatedNodeInfo;
        }
    }

    private ClusterNodeInfo selfPopulateClusterDB(String macid) {
        ClusterNodeInfo newClusterInfo = new ClusterNodeInfo();
        String add = null;
        try {
            add = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warning("cannot get localhost address: " + e.getMessage());
        }
        newClusterInfo.setAddress(add);
        boolean isMaster = isMasterMode();
        newClusterInfo.setIsMaster(isMaster);
        newClusterInfo.setMac(macid);
        // choose first available name
        String newnodename = null;
        for (int i = 1; i < 25; i++) {
            String maybenodename = "SSG" + i;
            String query = "from " + TABLE_NAME + " in class " + ClusterNodeInfo.class.getName() +
                           " where " + TABLE_NAME + "." + NAME_COLUMN_NAME + " = \'" + maybenodename + "\'";

            HibernatePersistenceContext context = null;
            List hibResults = null;
            try {
                context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
                hibResults = context.getSession().find(query);
            } catch (SQLException e) {
                String msg = "error looking for available node name";
                logger.log(Level.WARNING, msg, e);
            }  catch (HibernateException e) {
                String msg = "error looking for available node name";
                logger.log(Level.WARNING, msg, e);
            }
            if (hibResults == null || hibResults.isEmpty()) {
                newnodename = maybenodename;
                break;
            }
        }
        if (newnodename == null) {
            newnodename = "no_name";
        }
        newClusterInfo.setName(newnodename);
        selfId = macid;
        try {
            recordNodeInDB(newClusterInfo);
        } catch (SQLException e) {
            String msg = "cannot record new cluster node";
            logger.log(Level.SEVERE, msg, e);
        } catch (HibernateException e) {
            String msg = "cannot record new cluster node";
            logger.log(Level.SEVERE, msg, e);
        }
        logger.finest("added cluster status row for this server. mac= " + newClusterInfo.getMac() +
                      " name= " + newClusterInfo.getName());
        return newClusterInfo;
    }

    // returns true if this server has access to a root ca key
    private boolean isMasterMode() {
        String rootKeystorePath = KeystoreUtils.getInstance().getRootKeystorePath();
        if (rootKeystorePath != null) {
            if ((new File(rootKeystorePath)).exists()) return true;
        }
        return false;
    }

    private void recordNodeInDB(ClusterNodeInfo node) throws SQLException, HibernateException {
        HibernatePersistenceContext context = null;
        context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
        context.getSession().save(node);
    }

    private ClusterNodeInfo getNodeStatusFromDB(String mac) {
        String query = "from " + TABLE_NAME + " in class " + ClusterNodeInfo.class.getName() +
                       " where " + TABLE_NAME + "." + MAC_COLUMN_NAME + " = \'" + mac + "\'";
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
                return (ClusterNodeInfo)hibResults.get(0);
            default:
                logger.warning("this should not happen. more than one entry found" +
                                          "for mac: " + mac);
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
        if (output.isEmpty()) {
            output.addAll(getIpconfigMac());
        }
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
            logger.fine("error getting ipconfig: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.fine("error getting ipconfig: " + e.getMessage());
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
    private static final String NAME_COLUMN_NAME = "name";

    private static class SingletonHolder {
        private static final ClusterInfoManager singleton = new ClusterInfoManager();
    }

    private ClusterInfoManager() {
        // do nothing. just enforce the singleton pattern
    }

    private static Pattern ifconfigMacPattern = Pattern.compile(".*HWaddr\\s+(\\w\\w.\\w\\w.\\w\\w." +
                                                                "\\w\\w.\\w\\w.\\w\\w).*", Pattern.DOTALL);
    private static Pattern ipconfigMacPattern = Pattern.compile(".*Physical Address.*(\\w\\w.\\w\\w.\\w\\w." +
                                                                "\\w\\w.\\w\\w.\\w\\w).*", Pattern.DOTALL);
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    private String selfId = null;
    private long rememberedBootTime = -1;


}
