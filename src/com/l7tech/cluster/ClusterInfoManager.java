package com.l7tech.cluster;

import com.l7tech.common.util.HexUtils;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.PersistenceContext;
import net.sf.hibernate.HibernateException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            // only do this once
            if (nrNodes < 0) {
                try {
                    Collection res = retrieveClusterStatus();
                    nrNodes = res.size();
                } catch (FindException e) {
                    logger.log(Level.WARNING, "error in retrieveClusterStatus", e);
                }
            }
            // more than one node == cluster
            if (nrNodes > 0) return true;
            return false;
        }
    }

    /**
     * allows a node to update its status in the cluster_info table
     *
     * @param avgLoad the average load for the last minute
     */
    public void updateSelfStatus(double avgLoad) {
        // determine which node id we belong to
        // todo
        // retrieve the ClusterInfo object
        // todo
        // update value in the ClusterInfo object and save object
        // todo
    }

    /**
     * allows a node to update its boot timestamp in the cluster_info table
     */
    public void updateSelfUptime() {
        long newuptimevalue = System.currentTimeMillis();
        // determine which node id we belong to
        // todo
        // retrieve the ClusterInfo object
        // todo
        // update value in the ClusterInfo object and save object
        // todo
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
    private long selfNodeId() {
        return 0;
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

    private  static final String TABLE_NAME = "cluster_info";
    private static Pattern ifconfigMacPattern = Pattern.compile(".*HWaddr\\s+(\\w\\w.\\w\\w.\\w\\w." +
                                                                "\\w\\w.\\w\\w.\\w\\w).*", Pattern.DOTALL);
    private static Pattern ipconfigMacPattern = Pattern.compile(".*Physical Address.*(\\w\\w.\\w\\w.\\w\\w." +
                                                                "\\w\\w.\\w\\w.\\w\\w).*", Pattern.DOTALL);
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    private int nrNodes = -1;
}
