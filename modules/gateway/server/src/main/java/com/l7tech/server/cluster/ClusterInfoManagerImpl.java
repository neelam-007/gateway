package com.l7tech.server.cluster;

import com.l7tech.util.HexUtils;
import com.l7tech.common.io.IOUtils;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hibernate layer over the cluster_info table.
 *
 * This manager will look for a row in the cluster_info that represent this server using mac addresses.
 * If no row is found for this server, the manager will create one.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Dec 17, 2003<br/>
 * $Id$
 *
 */
public class ClusterInfoManagerImpl extends HibernateDaoSupport implements ClusterInfoManager {
    private static final String PROP_OLD_MULTICAST_GEN = "com.l7tech.cluster.macAddressOldGen"; // true for old
    private static final String PROP_MAC_ADDRESS = "com.l7tech.cluster.macAddress";
    private static final String PROP_IP_ADDRESS = "com.l7tech.cluster.ipAddress";

    private ServerConfig serverConfig;
    private KeystoreUtils keystore;

    private final String HQL_FIND_ALL =
            "from " + TABLE_NAME +
                    " in class " + ClusterNodeInfo.class.getName();

    private final String HQL_FIND_BY_NAME =
            "from " + TABLE_NAME +
                    " in class " + ClusterNodeInfo.class.getName() +
                    " where " + TABLE_NAME + "." + NAME_COLUMN_NAME + " = ?";

    private final String HQL_FIND_BY_ID =
            "from " + TABLE_NAME +
                    " in class " + ClusterNodeInfo.class.getName() +
                    " where " + TABLE_NAME + "." + NODEID_COLUMN_NAME + " = ?";

    private final String HQL_DELETE_BY_ID =
            "delete from " + ClusterNodeInfo.class.getName() + " as " + TABLE_NAME +
                    " where " + TABLE_NAME + "." + NODEID_COLUMN_NAME + " = :nodeid";

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void setKeystore(KeystoreUtils keystore) {
        this.keystore = keystore;
    }

    /**
     * returns the node id to which this server applies to
     */
    public String thisNodeId() {
        if (selfId != null) return selfId;
        ClusterNodeInfo selfCI = getSelfNodeInf();
        if (selfCI != null) return selfCI.getNodeIdentifier();
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
            updateSelfStatus( selfCI );
        }
    }

    /**
     * Updates the specified {@link ClusterNodeInfo} to the database.
     */
    public void updateSelfStatus( final ClusterNodeInfo selfCI ) throws UpdateException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(final Session session) throws HibernateException, SQLException {
                    // Use a bulk delete to ensure that a replicable SQL statement is run
                    // even if there is nothing in the table (see bug 4615)
                    session.createQuery( HQL_DELETE_BY_ID )
                            .setString("nodeid", selfCI.getNodeIdentifier() )
                            .executeUpdate();
                    if ( session.contains( selfCI ) ) { 
                        session.evict( selfCI );
                    }
                    session.save( selfCI );
                    return null;
                }
            });
        } catch (HibernateException e) {
            String msg = "error updating db";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
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

        try {
            getHibernateTemplate().delete(node);
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
        } catch (HibernateException e) {
            String msg = "error saving node's new name";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }
    }

    /**
     * this should be called
     * when the server boots. it updates the boot time and update time in the cluster_status
     * table.
     */
    public void updateSelfUptime() throws UpdateException {
        long newboottimevalue = System.currentTimeMillis();
        ClusterNodeInfo selfCI = getSelfNodeInf();
        if (selfCI != null) {
            selfCI.setBootTime(newboottimevalue);
            selfCI.setLastUpdateTimeStamp(newboottimevalue);
            updateSelfStatus( selfCI );
            rememberedBootTime = newboottimevalue;
        } else {
            logger.warning("cannot retrieve db entry for this node.");
        }
    }

    /**
     * @return a collection containing ClusterNodeInfo objects. if the collection is empty, it means that
     * the SSG operated by itself outsides a cluster.
     */
    public Collection<ClusterNodeInfo> retrieveClusterStatus() throws FindException {
        // get all objects from that table
        try {
            //noinspection unchecked
            return (List<ClusterNodeInfo>) getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    return session.createQuery(HQL_FIND_ALL).list();
                }
            });
        } catch (Exception e) {
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
            String partition = getPartitionName();
            Iterator macs = getMacs().iterator();
            String anymac = null;
            // find out which mac works for us
            while (macs.hasNext()) {
                String mac = (String)macs.next();
                anymac = mac;
                ClusterNodeInfo output = getNodeStatusFromDB(toNodeId(mac, partition));
                if (output != null) {
                    if (!isValidIPAddressAndClusterPort(output.getAddress(), output.getClusterPort())) {
                        String newIpAddress = getIPAddress();
                        int newClusterPort = getClusterPort();
                        output.setAddress(newIpAddress);
                        output.setClusterPort(newClusterPort);
                        try {
                            recordNodeInDB(output);
                        } catch (HibernateException e) {
                            String msg = "Error saving node's new ip '"+newIpAddress+"' or new cluster port '"+newClusterPort+"'.";
                            logger.log(Level.WARNING, msg, e);
                        }
                    }
                    logger.config("Using server " + output.getName() +
                            " (Id:" + output.getNodeIdentifier() +
                            ", Ip:" + output.getAddress() +
                            ", Port:" + output.getClusterPort() + ")");
                    selfId = output.getNodeIdentifier();

                    return output;
                }
            }

            // no existing row for us. create one
            if (anymac != null) {
                return selfPopulateClusterDB(anymac);
            }

            logger.severe("Should not get here. this server has no mac?");
            return null;
        }
    }

    private static String toNodeId(String mac, String partition) {
        String identifierText = mac + "-" + partition;
        byte[] identifierBytes = HexUtils.encodeUtf8(identifierText);
        byte[] md5IdentifierBytes = HexUtils.getMd5Digest(identifierBytes);
        String md5Identifier = HexUtils.encodeMd5Digest(md5IdentifierBytes).toLowerCase();

        return md5Identifier;
    }

    private ClusterNodeInfo recreateRow() {
        selfId = null;
        ClusterNodeInfo recreatedNodeInfo = getSelfNodeInf();
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
        newClusterInfo.setAddress(getIPAddress());
        newClusterInfo.setNodeIdentifier(toNodeId(macid, getPartitionName()));
        newClusterInfo.setPartitionName(getPartitionName());
        newClusterInfo.setClusterPort(getClusterPort());
        boolean isMaster = isMasterMode();
        newClusterInfo.setIsMaster(isMaster);
        newClusterInfo.setMac(macid);
        // choose first available name
        String newnodename = null;
        for (int i = 1; i < 25; i++) {
            String maybenodename = "SSG" + i;

            List hibResults = null;
            Session s = null;
            FlushMode old = null;
            try {
                s = getSession();
                old = s.getFlushMode();
                s.setFlushMode(FlushMode.NEVER);
                Query q = s.createQuery(HQL_FIND_BY_NAME);
                q.setString(0, maybenodename);
                hibResults = q.list();
            } catch (HibernateException e) {
                String msg = "error looking for available node name";
                logger.log(Level.WARNING, msg, e);
            } finally {
                if (old != null && s != null) s.setFlushMode(old);
                if (s != null) releaseSession(s);
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
        selfId = newClusterInfo.getNodeIdentifier();
        try {
            recordNodeInDB(newClusterInfo);
        } catch (HibernateException e) {
            String msg = "cannot record new cluster node";
            logger.log(Level.SEVERE, msg, e);
        }
        logger.finest("Added cluster status row for this server. identifier= " + newClusterInfo.getNodeIdentifier() +
                      " name= " + newClusterInfo.getName());
        return newClusterInfo;
    }

    // returns true if this server has access to a root ca key
    private boolean isMasterMode() {
        String rootKeystorePath = keystore.getRootKeystorePath();
        return rootKeystorePath != null && new File(rootKeystorePath).exists();
    }

    private void recordNodeInDB(ClusterNodeInfo node) throws HibernateException {
        getHibernateTemplate().save(node);
    }

    private ClusterNodeInfo getNodeStatusFromDB(final String nodeIdentifer) {
        try {
            return (ClusterNodeInfo) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_BY_ID);
                    q.setString(0, nodeIdentifer);
                    return (ClusterNodeInfo) q.uniqueResult();
                }
            });
        }  catch (Exception e) {
            String msg = "error retrieving cluster status";
            logger.log(Level.WARNING, msg, e);
            return null;
        }
    }

    /**
     * get the mac addresses for the network adapters on this server
     *
     * @return a collection containing strings representing mac addresses in the following format:
     * XX:XX:XX:XX:XX:XX
     */
    private Collection getMacs() {
        ArrayList<String> output = new ArrayList<String>();

        // try to get mac from system property
        String macproperty = System.getProperty(PROP_MAC_ADDRESS);
        if (macproperty != null && macproperty.length() > 0) {
            output.add(macproperty);
        } else {
            logger.fine("Cannot get mac address from system property ('"+PROP_MAC_ADDRESS+"').");
        }

        // try to get mac from ifconfig
        if (output.isEmpty()) {
            output.addAll(getIfconfigMac());
            if (output.isEmpty()) {
                logger.fine("Cannot get mac address from ifconfig.");
            }
        }

        // try to get mac from ipconfig
        if (output.isEmpty()) {
            output.addAll(getIpconfigMac());
            if (output.isEmpty()) {
                logger.fine("Cannot get mac address from ipconfig.");
            }
        }

        logger.config("Using mac addresses: " + output);

        return output;
    }

    /**
     * called by getMacs()
     *
     * tries to get mac addresses by running unix ifconfig command
     */
    private Collection<String> getIfconfigMac() {
        Process up = null;
        InputStream got = null;
        String ifconfigOutput = null;
        ArrayList<String> output = new ArrayList<String>();
        try {
            try {
                up = Runtime.getRuntime().exec("/sbin/ifconfig eth0");
                got = new BufferedInputStream(up.getInputStream());
                byte[] buff = IOUtils.slurpStreamLocalBuffer(got);
                ifconfigOutput = new String(buff);
                up.waitFor();
            } finally {
                if (got != null)
                    got.close();
                if (up != null)
                    up.destroy();
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "Error getting ifconfig - perhaps not running on linux.", e);
        } catch (InterruptedException e) {
            logger.log(Level.FINE, "Error getting ifconfig - perhaps not running on linux", e);
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
        if (ifconfigOutput == null) return Collections.emptyList();
        String[] splitted = breakIntoLines(ifconfigOutput);
        for (String s : splitted) {
            Matcher matchr = ifconfigMacPattern.matcher(s);
            if (matchr.matches()) {
                output.add(matchr.group(1));
            }
        }
        return output;
    }

    private String getIfConfigIpAddress() {
        Process up = null;
        InputStream got = null;
        String ifconfigOutput = null;
        try {
            try {
                up = Runtime.getRuntime().exec("/sbin/ifconfig eth0");
                got = new BufferedInputStream(up.getInputStream());
                byte[] buff = IOUtils.slurpStreamLocalBuffer(got);
                ifconfigOutput = new String(buff);
                up.waitFor();
            } finally {
                if (got != null)
                    got.close();
                if (up != null)
                    up.destroy();
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "error getting ifconfig - must not be running on linux", e);
        } catch (InterruptedException e) {
            logger.log(Level.FINE, "error getting ifconfig - must not be running on linux", e);
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
        if (ifconfigOutput == null) return null;
        String[] splitted = breakIntoLines(ifconfigOutput);
        for (String s : splitted) {
            Matcher matchr = ifconfigAddrPattern.matcher(s);
            if (matchr.matches()) {
                return matchr.group(1);
            }
        }
        return null;
    }

    /**
     * called by getMacs().
     *
     * tries to get mac addresses by running windows ipconfig command
     */
    private Collection<String> getIpconfigMac() {
        Process up = null;
        InputStream got = null;
        String ipconfigOutput = null;
        ArrayList<String> output = new ArrayList<String>();
        try {
            try {
                up = Runtime.getRuntime().exec("ipconfig /all");
                got = new BufferedInputStream(up.getInputStream());
                byte[] buff = IOUtils.slurpStreamLocalBuffer(got);
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
        if (ipconfigOutput == null) return Collections.emptyList();
        String[] splitted = breakIntoLines(ipconfigOutput);
        for (String s : splitted) {
            Matcher matchr = ipconfigMacPattern.matcher(s);
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

    /**
     * This method gets this server's ip address by parsing ifconfig eth0.
     * If that does not work, then the ip address returned is the one given
     * by InetAddress.getLocalHost().getHostAddress(). (for cygwin support)
     *
     * If none of these methods work (is that possible?) then the output is null.
     */
    private synchronized String getIPAddress() {
        if (thisNodeIPAddress == null) {            
            thisNodeIPAddress = getConfiguredIPAddress();

            if (thisNodeIPAddress == null) {
                thisNodeIPAddress = getIfConfigIpAddress();
            }

            if (thisNodeIPAddress == null) {
                try {
                    thisNodeIPAddress = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    logger.log(Level.FINEST, "problem getting address with InetAddress.getLocalHost().getHostAddress()", e);
                }
            }
        }
        return thisNodeIPAddress;
    }

    /**
     * Get the IP address from the system property (if any)
     *
     * @return The property or null if not set
     */
    private String getConfiguredIPAddress() {
        String configuredIp = System.getProperty(PROP_IP_ADDRESS);

        try {
            if (configuredIp!=null && !isValidIPAddress(InetAddress.getByName(configuredIp))) {
                logger.log(Level.WARNING, "IP address '"+configuredIp+"', is not present.");
            }
        }
        catch(UnknownHostException uhe) {
            logger.log(Level.WARNING, "Invalid IP address configured '"+configuredIp+"', ignoring.", uhe);
            configuredIp = null;
        }

        return configuredIp;
    }

    /**
     * Get this partitions name.
     */
    private String getPartitionName() {
        return serverConfig.getProperty("partitionName");        
    }

    /**
     * Get this partitions cluster port.
     */
    private int getClusterPort() {
        return serverConfig.getIntProperty("clusterPort", 2124);        
    }

    /**
     * Check if the given IP address is a valid IP address for this system.
     *
     * Also check that if a system property is set it matches the given IP.
     */
    private boolean isValidIPAddressAndClusterPort(final String address, final int clusterPort) {
        boolean valid = true;

        if (address == null || clusterPort <= 0) {
            valid = false;
        } else {
            InetAddress hostAddress = null;
            try {
                hostAddress = InetAddress.getByName(address);
            }
            catch(UnknownHostException uhe) {
                logger.log(Level.WARNING, "Cannot resolve address '"+address+"'", uhe);
            }

            if (hostAddress != null) {
                boolean found = isValidIPAddress(hostAddress);
                String configuredIp = getConfiguredIPAddress();
                boolean configured = configuredIp != null;
                boolean matchesConfig = configured && configuredIp.equals(address);

                if (!found) {
                    valid = matchesConfig; // if it matches the config then treat as valid, you get what you ask for
                    logger.log(Level.WARNING, "Address does not resolve to any local address '"+address+"'.");
                }
                else if (!matchesConfig) {
                    valid = false;    
                    logger.log(Level.CONFIG, "Updating cluster IP address to match configuration '"+configuredIp+"' (was '"+address+"')");
                }
            }

            if (clusterPort != getClusterPort()) {
                logger.log(Level.WARNING, "Cluster port is invalid '"+clusterPort+"'.");
                valid = false;
            }
        }
        return valid;
    }

    /**
     * Check if the given IP address is one of the IPs on the system.
     *
     * @param address The address to check
     * @return true if valid
     */
    private boolean isValidIPAddress(final InetAddress address) {
        boolean found = false;
        try {
            done:
            for(NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for(InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
                    if (inetAddress.equals(address)) {
                        found = true;
                        break done;
                    }
                }
            }
        } catch(SocketException se) {
            logger.log(Level.WARNING, "Cannot get network interfaces to address.", se);
        }

        return found;
    }

    public static String generateMulticastAddress() {
        StringBuffer addr = new StringBuffer();

        if (Boolean.getBoolean(PROP_OLD_MULTICAST_GEN)) {
            // old method ... not so random
            addr.append("224.0.7.");
            addr.append(Math.abs(random.nextInt() % 256));
        } else {
            // randomize an address from the 224.0.2.0 - 224.0.255.255 range
            addr.append("224.0.");
            int randomVal = 0;
            while (randomVal < 2) {
                randomVal = Math.abs(random.nextInt() % 256);
            }
            addr.append(randomVal);
            addr.append(".");
            addr.append(Math.abs(random.nextInt() % 256));
        }

        return addr.toString();
    }

    private static final String TABLE_NAME = "cluster_info";
    private static final String NODEID_COLUMN_NAME = "nodeIdentifier";
    private static final String NAME_COLUMN_NAME = "name";


    private static Pattern ifconfigMacPattern = Pattern.compile(".*HWaddr\\s+(\\w\\w.\\w\\w.\\w\\w." +
                                                                "\\w\\w.\\w\\w.\\w\\w).*", Pattern.DOTALL);

    private static Pattern ifconfigAddrPattern = Pattern.compile(".*inet addr:(\\d+\\.\\d+\\.\\d+\\.\\d+).*", Pattern.DOTALL);

    private static Pattern ipconfigMacPattern = Pattern.compile(".+:\\s*(\\w\\w-\\w\\w-\\w\\w-\\w\\w-\\w\\w-\\w\\w).*", Pattern.DOTALL);

    private final Logger logger = Logger.getLogger(getClass().getName());
    private String selfId = null;
    private long rememberedBootTime = -1;
    private String thisNodeIPAddress = null;


    private static Random random = new SecureRandom();
}
