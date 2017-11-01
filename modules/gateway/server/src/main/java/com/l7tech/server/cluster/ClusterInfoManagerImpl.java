package com.l7tech.server.cluster;

import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.IOUtils;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
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
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class ClusterInfoManagerImpl extends HibernateDaoSupport implements ClusterInfoManager {

    //- PUBLIC

    public ClusterInfoManagerImpl( final String nodeid ) {
        this.nodeid = nodeid;
    }

    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    public void setServerConfig(Config config ) {
        this.config = config;
    }

    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    public void setClusterIDManager(ClusterIDManager clusterIDManager) {
        this.clusterIDManager = clusterIDManager;
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    public String thisNodeId() {
        return nodeid;
    }

    /**
     * allows a node to update its status in the cluster_info table
     *
     * @param avgLoad the average load for the last minute
     */
    @Override
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
            updateSelfStatus( selfCI );
        }
    }

    /**
     * Updates the specified {@link ClusterNodeInfo} to the database.
     */
    @Override
    public void updateSelfStatus( final ClusterNodeInfo selfCI ) throws UpdateException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                @Override
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

    @Override
    public String deleteNode(String nodeid) throws DeleteException {
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

        final String name = node.getName();
        try {
            getHibernateTemplate().delete(node);
        }  catch (HibernateException e) {
            String msg = "error deleting cluster status";
            logger.log(Level.WARNING, msg, e);
        }        
        return name;
    }

    @Override
    @Transactional
    public void removeStaleNodes(final long statusTimeStamp) {
        try {
            // Cannot use a lambda here since tis is loaded by spring.
            //noinspection Convert2Lambda
            getHibernateTemplate().execute(new HibernateCallback() {
                @Override
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    // The garbage collection query deletes all ClusterNodeInfo entities (rows from cluster_info) that were last updated more than `statusTimeStamp` ago
                    session.createQuery(HQL_DELETE_ALL_OLDER_THAN).setLong("statusTimeStamp", statusTimeStamp).executeUpdate();
                    return null;
                }
            });
        } catch (Exception e) {
            String msg = "error cleaning cluster statuses";
            logger.log(Level.WARNING, msg, e);
        }
    }

    @Override
    public String renameNode(String nodeid, String newnodename) throws UpdateException {
        ClusterNodeInfo node = getNodeStatusFromDB(nodeid);
        if (node == null) {
            String msg = "that node cannot be retrieved";
            logger.log(Level.WARNING, msg);
            throw new UpdateException(msg);
        }
        final String oldName = node.getName();
        node.setName(newnodename);
        try {
            updateSelfStatus(node);
        } catch (HibernateException e) {
            String msg = "error saving node's new name";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }
        return oldName;
    }

    /**
     * this should be called
     * when the server boots. it updates the boot time and update time in the cluster_status
     * table.
     */
    @Override
    public void updateSelfUptime() throws UpdateException {
        ClusterNodeInfo selfCI = getSelfNodeInf();
        if (selfCI != null) {
            selfCI.setBootTime(rememberedBootTime);
            selfCI.setLastUpdateTimeStamp(System.currentTimeMillis());
            updateSelfStatus( selfCI );
        } else {
            logger.warning("cannot retrieve db entry for this node.");
        }
    }

    /**
     * @return a collection containing ClusterNodeInfo objects. if the collection is empty, it means that
     * the SSG operated by itself outsides a cluster.
     */
    @Override
    @Transactional(readOnly=true)
    public Collection<ClusterNodeInfo> retrieveClusterStatus() throws FindException {
        // get all objects from that table
        try {
            //noinspection unchecked
            return (List<ClusterNodeInfo>) getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
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
    @Override
    public synchronized ClusterNodeInfo getSelfNodeInf() {
        return getSelfNodeInf( false );
    }

    @Override
    public synchronized ClusterNodeInfo getSelfNodeInf( boolean refresh ) {
        ClusterNodeInfo clusterNodeInfo;

        clusterNodeInfo = getNodeStatusFromDB(nodeid);
        if (clusterNodeInfo != null) {

            if ( !isInfoChecked() || refresh ) {
                if ( refresh ) thisNodeEsmIPAddress = null;
                final String newIpAddress = getIPAddress(null); // Load of IP is used to track if info is checked
                final String newEsmIpAddress = getEsmIpAddress(newIpAddress);
                final boolean updateIp = !isValidIPAddress(clusterNodeInfo.getAddress());
                final boolean updateEsmIp = newEsmIpAddress!=null && !newEsmIpAddress.equals( clusterNodeInfo.getEsmAddress() );
                if ( updateIp || updateEsmIp ) {
                    if (updateIp) clusterNodeInfo.setAddress(newIpAddress);
                    if (updateEsmIp) clusterNodeInfo.setEsmAddress(newEsmIpAddress);
                    try {
                        updateSelfStatus(clusterNodeInfo);
                    } catch (UpdateException e) {
                        String msg = "Error saving node's new ip '"+newIpAddress+"'.";
                        logger.log(Level.WARNING, msg, e);
                    }
                }

                logger.config( "Using server " + clusterNodeInfo.getName() +
                        " (Id:" + clusterNodeInfo.getNodeIdentifier() +
                        ", Ip:" + clusterNodeInfo.getAddress() +
                        ", EsmIp:" + clusterNodeInfo.getEsmAddress() + ")");
            }

            return clusterNodeInfo;
        } else {
            // no existing row for us. create one
            String mac = getMac();
            if ( mac != null ) {
                return selfPopulateClusterDB(nodeid, mac);
            } else {
                logger.severe("Should not get here. this server has no mac?");
                return null;
            }
        }
    }

    //- PRIVATE

    private static final String PROP_IP_ADDRESS = "com.l7tech.cluster.ipAddress";
    private static final String NODE_CLUSTER_IP_ADDRESS = "node.cluster.ip";

    private static final String TABLE_NAME = "cluster_info";
    private static final String NODEID_COLUMN_NAME = "nodeIdentifier";
    private static final String NAME_COLUMN_NAME = "name";
    private static final String STATUS_TIME_STAMP_COLUMN_NAME = "lastUpdateTimeStamp";

    private static Pattern ifconfigAddr4Pattern = Pattern.compile(".*inet addr:(\\d+\\.\\d+\\.\\d+\\.\\d+).*", Pattern.DOTALL);
    private static Pattern ifconfigAddr6Pattern = Pattern.compile(".*inet6 addr: ([^/]+)/\\d+ Scope:(Global|Site|Compat)", Pattern.DOTALL);

    private static final String HQL_FIND_ALL =
            "from " + TABLE_NAME +
                    " in class " + ClusterNodeInfo.class.getName();

    private static final String HQL_FIND_BY_NAME =
            "from " + TABLE_NAME +
                    " in class " + ClusterNodeInfo.class.getName() +
                    " where " + TABLE_NAME + "." + NAME_COLUMN_NAME + " = ?";

    private static final String HQL_FIND_BY_ID =
            "from " + TABLE_NAME +
                    " in class " + ClusterNodeInfo.class.getName() +
                    " where " + TABLE_NAME + "." + NODEID_COLUMN_NAME + " = ?";

    private static final String HQL_DELETE_BY_ID =
            "delete from " + ClusterNodeInfo.class.getName() + " as " + TABLE_NAME +
                    " where " + TABLE_NAME + "." + NODEID_COLUMN_NAME + " = :nodeid";

    private static final String HQL_DELETE_ALL_OLDER_THAN =
            "delete from " + ClusterNodeInfo.class.getName() + " as " + TABLE_NAME +
                    " where " + TABLE_NAME + "." + STATUS_TIME_STAMP_COLUMN_NAME + " < :statusTimeStamp";

    private static final Logger logger = Logger.getLogger(ClusterInfoManagerImpl.class.getName());
    private static final long rememberedBootTime = System.currentTimeMillis();
    private String thisNodeIPAddress;
    private String thisNodeMac;
    private String thisNodeEsmIPAddress;
    private final String nodeid;

    private Config config;
    private ClusterIDManager clusterIDManager;

    private ClusterNodeInfo recreateRow() {
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

    private ClusterNodeInfo selfPopulateClusterDB(String nodeid, String macid) {
        ClusterNodeInfo newClusterInfo = new ClusterNodeInfo();
        newClusterInfo.setAddress(getIPAddress(macid));
        newClusterInfo.setEsmAddress(getEsmIpAddress(newClusterInfo.getAddress()));
        newClusterInfo.setNodeIdentifier(nodeid);
        newClusterInfo.setMac(macid);
        newClusterInfo.setBootTime(rememberedBootTime);
        newClusterInfo.setLastUpdateTimeStamp(System.currentTimeMillis());
        // choose first available name
        String newnodename = null;
        for (int i = 1; i < 25; i++) {
            String maybenodename = "Gateway" + i;

            List hibResults = null;
            Session s = null;
            FlushMode old = null;
            try {
                s = getSession();
                old = s.getFlushMode();
                s.setFlushMode(FlushMode.MANUAL);
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

    private void recordNodeInDB(ClusterNodeInfo node) throws HibernateException {
        getHibernateTemplate().save(node);
    }

    private ClusterNodeInfo getNodeStatusFromDB(final String nodeIdentifer) {
        try {
            return (ClusterNodeInfo) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_BY_ID);
                    q.setString(0, nodeIdentifer);
                    return q.uniqueResult();
                }
            });
        }  catch (Exception e) {
            String msg = "error retrieving cluster status";
            logger.log(Level.WARNING, msg, e);
            return null;
        }
    }

    private String getIpAddress( final String mac ) {
        return ClusterIDManager.getIpForMac( mac );    
    }

    private String getIfConfigIpAddress() {
        Process up = null;
        InputStream got = null;
        String ifconfigOutput = null;
        try {
            try {
                up = Runtime.getRuntime().exec("/sbin/ifconfig eth0");
                got = new BufferedInputStream(up.getInputStream());
                byte[] buff = IOUtils.slurpStream(got);
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

        Pattern ifconfigPattern = InetAddressUtil.isUseIpv6() ? ifconfigAddr6Pattern : ifconfigAddr4Pattern;

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
        String[] splitted = ClusterIDManager.breakIntoLines(ifconfigOutput);
        for (String s : splitted) {
            Matcher matchr = ifconfigPattern.matcher(s);
            if (matchr.matches()) {
                return matchr.group(1);
            }
        }
        return null;
    }

    private boolean isInfoChecked() {
        return thisNodeIPAddress != null && thisNodeEsmIPAddress != null;
    }

    /**
     * This method gets this server's ip address by parsing ifconfig eth0.
     * If that does not work, then the ip address returned is the one given
     * by InetAddress.getLocalHost().getHostAddress(). (for cygwin support)
     *
     * If none of these methods work (is that possible?) then the output is null.
     *
     * @return IP address string, or null if we were unable to find one by any method.
     */
    private synchronized String getIPAddress( final String mac ) {
        if (thisNodeIPAddress == null) {
            thisNodeIPAddress = getSystemPropertyIPAddress();

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

            if (thisNodeIPAddress == null) {
                thisNodeIPAddress = getIpAddress(mac);
            }

            if (thisNodeIPAddress == null) {
                thisNodeIPAddress = getNodePropertiesIPAddress(); // last resort
            }

        }
        return thisNodeIPAddress;
    }

    private synchronized String getMac() {
        String mac = thisNodeMac;
        if ( mac == null ) {
            Collection<String> macs = clusterIDManager.getMacs();
            if ( !macs.isEmpty() ) {
                mac = macs.iterator().next();
                thisNodeMac = mac;
            }
        }

        return mac;
    }

    /**
     * Get the IP address from the system property (if any)
     *
     * @return The property or null if not set
     */
    private String getSystemPropertyIPAddress() {
        String configuredIp = config.getProperty( PROP_IP_ADDRESS );

        try {
            if (configuredIp!=null && !isValidIPAddress(InetAddress.getByName(configuredIp))) {
                logger.log(Level.WARNING, "IP address '"+configuredIp+"', is not present.");
            }
        } catch (UnknownHostException uhe) {
            logger.log(Level.WARNING, "Invalid IP address configured '"+configuredIp+"', ignoring.", uhe);
            configuredIp = null;
        }

        return configuredIp;
    }

    /**
     * Get the IP address from node.properties/cluster.node.ip.
     */
    private String getNodePropertiesIPAddress() {
        return clusterIDManager.loadNodeProperty(NODE_CLUSTER_IP_ADDRESS);
    }

    /**
     * Get the IP address to use for ESM administrative requests.
     *
     * @param defaultIpAddress The default address to use.
     * @return The ESM IP Address (only null if the default is null)
     */
    private String getEsmIpAddress( final String defaultIpAddress ) {
        String esmIpAddress = thisNodeEsmIPAddress;

        if ( esmIpAddress == null ) {
            esmIpAddress = defaultIpAddress;

            final String pcIp = config.getProperty( "com.l7tech.server.processControllerIpAddress" );
            if ( pcIp != null ) {
                if ( !InetAddressUtil.isAnyHostAddress( pcIp ) &&
                     !InetAddressUtil.isLoopbackAddress( pcIp ) ) {
                    esmIpAddress = pcIp;
                }
            }

            final String interfaceTag = config.getProperty( "admin.esmInterfaceTag" );
            if ( interfaceTag != null ) {
                final InterfaceTag tag = getInterfaceTagByName( interfaceTag );
                if ( tag != null ) {
                    List<InetAddress> localAddrs = Collections.emptyList();
                    try {
                        localAddrs = InetAddressUtil.findAllLocalInetAddresses();
                    } catch (SocketException e) {
                        logger.log( Level.WARNING, "Unable to list interfaces to check ESM interface tag: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
                    }

                    final Set<String> patterns = tag.getIpPatterns();
                    InetAddress match = null;
                    outer: for (InetAddress addr : localAddrs) {
                        for (String pattern : patterns) {
                            if (InetAddressUtil.patternMatchesAddress(pattern, addr)) {
                                if (match == null) {
                                    match = addr;
                                } else {
                                    logger.log(Level.WARNING, "Interface tag " + interfaceTag + " contains patterns matching more than one network addresses on this node.  Will use first match of " + match);
                                    break outer;
                                }
                            }
                        }
                    }

                    if ( match != null ) {
                        esmIpAddress = match.getHostAddress();
                    }
                }
            }

            thisNodeEsmIPAddress = esmIpAddress;
        }

        return esmIpAddress;
    }

    private InterfaceTag getInterfaceTagByName( final String desiredName ) {
        InterfaceTag interfaceTag = null;

        try {
            final String stringForm = config.getProperty( InterfaceTag.PROPERTY_NAME );
            final Set<InterfaceTag> tags = stringForm == null ?
                    Collections.<InterfaceTag>emptySet() :
                    InterfaceTag.parseMultiple(stringForm);
            for ( final InterfaceTag tag : tags ) {
                if ( tag.getName().equalsIgnoreCase(desiredName) ) {
                    interfaceTag = tag;
                    break;
                }
            }
        } catch ( ParseException e ) {
            logger.log( Level.WARNING, "Error loading interface tags: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
        }

        return interfaceTag;
    }

    /**
     * Check if the given IP address is a valid IP address for this system.
     *
     * Also check that if a system property is set it matches the given IP.
     */
    private boolean isValidIPAddress(final String address) {
        boolean valid = true;

        if (address == null) {
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
                String configuredIp = getSystemPropertyIPAddress();
                boolean configured = configuredIp != null;
                boolean matchesConfig = configured && configuredIp.equals(address);

                if (!found) {
                    valid = matchesConfig; // if it matches the config then treat as valid, you get what you ask for
                    logger.log(Level.WARNING, "Address does not resolve to any local address '"+address+"'.");
                }
                else if (configured && !matchesConfig) {
                    valid = false;
                    logger.log(Level.CONFIG, "Updating cluster IP address to match configuration '"+configuredIp+"' (was '"+address+"')");
                }
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
}
