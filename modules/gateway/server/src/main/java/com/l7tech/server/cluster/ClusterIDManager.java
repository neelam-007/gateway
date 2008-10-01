package com.l7tech.server.cluster;

import com.l7tech.common.io.IOUtils;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 */
public class ClusterIDManager extends HibernateDaoSupport {

    //- PUBLIC

    /**
     * returns the node id to which this server applies to
     */
    public String thisNodeId() {
        if (selfId != null) return selfId;

        String propertiesNodeId = loadNodeIdProperty();
        if ( propertiesNodeId != null ) {
            logger.config("Loaded node identifier is '"+propertiesNodeId+"'.");
        }

        try {
            // special query, dont do this everytime
            // (cache return value as this will not change while the server is up)
            Collection<String> macs = getMacs();
            List<String> nodeids = new ArrayList<String>();
            if ( propertiesNodeId != null ) {
                nodeids.add(propertiesNodeId);
            }

            String anymac = null;
            for ( String mac : macs ) {
                if (anymac == null) {
                    anymac = mac;
                }
                nodeids.add(toNodeId(mac));
            }

            // find out which id works for us
            for ( String nodeid : nodeids ) {
                ClusterNodeInfo output = getNodeStatusFromDB(nodeid);
                if (output != null) {
                    logger.config("Using server " + output.getName() +
                            " (Id:" + output.getNodeIdentifier() +
                            ", Ip:" + output.getAddress() + ")");
                    selfId = output.getNodeIdentifier();
                    break;
                }
            }

            selfId = propertiesNodeId;
            if ( selfId == null ) {
                selfId = generateNodeId();
            }

        } finally {
            if ( propertiesNodeId == null ) {
                String id = selfId;
                if ( id != null ) {
                    storeNodeIdProperty( id );
                }
            }
        }

        return selfId;
    }

    //- PACKAGE

    /**
     * get the mac addresses for the network adapters on this server
     *
     * @return a collection containing strings representing mac addresses in the following format:
     * XX:XX:XX:XX:XX:XX
     */
    static Collection<String> getMacs() {
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

    static String[] breakIntoLines(String in) {
        return Pattern.compile("$+", Pattern.MULTILINE).split(in);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ClusterIDManager.class.getName() );

    private static final String SYSPROP_CONFIG_HOME = "com.l7tech.server.configDirectory";
    private static final String NODE_ID_FILE = "node.properties";
    private static final String NODE_ID_PROPERTY = "node.id";    

    private static final String PROP_MAC_ADDRESS = "com.l7tech.cluster.macAddress";
    private static Pattern ifconfigMacPattern = Pattern.compile(".*HWaddr\\s+(\\w\\w.\\w\\w.\\w\\w." +
                                                                "\\w\\w.\\w\\w.\\w\\w).*", Pattern.DOTALL);

    private static Pattern ipconfigMacPattern = Pattern.compile(".+:\\s*(\\w\\w-\\w\\w-\\w\\w-\\w\\w-\\w\\w-\\w\\w).*", Pattern.DOTALL);

    private static final String TABLE_NAME = "cluster_info";
    private static final String NODEID_COLUMN_NAME = "nodeIdentifier";
    private static final String HQL_FIND_BY_ID =
            "from " + TABLE_NAME +
                    " in class " + ClusterNodeInfo.class.getName() +
                    " where " + TABLE_NAME + "." + NODEID_COLUMN_NAME + " = ?";

    private String selfId;

    /**
     * Generate a new nodeid
     */
    private static String generateNodeId( ) {
        return UUID.randomUUID().toString().replace("-","");
    }

    private static String toNodeId(String mac) {
        String identifierText = mac + "-default_"; // for compatibility with default_ partition
        byte[] identifierBytes = HexUtils.encodeUtf8(identifierText);
        byte[] md5IdentifierBytes = HexUtils.getMd5Digest(identifierBytes);
        return HexUtils.encodeMd5Digest(md5IdentifierBytes).toLowerCase();
    }    

    private ClusterNodeInfo getNodeStatusFromDB(final String nodeIdentifer) {
        try {
            return (ClusterNodeInfo) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
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

    /**
     * called by getMacs()
     *
     * tries to get mac addresses by running unix ifconfig command
     * @return  collection of mac address strings.  May be empty but never null.
     */
    private static Collection<String> getIfconfigMac() {
        Process up = null;
        InputStream got = null;
        String ifconfigOutput = null;
        ArrayList<String> output = new ArrayList<String>();
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
    
    /**
     * called by getMacs().
     *
     * tries to get mac addresses by running windows ipconfig command
     * @return collection of mac address strings.  May be empty but never null.
     */
    private static Collection<String> getIpconfigMac() {
        Process up = null;
        InputStream got = null;
        String ipconfigOutput = null;
        ArrayList<String> output = new ArrayList<String>();
        try {
            try {
                up = Runtime.getRuntime().exec("ipconfig /all");
                got = new BufferedInputStream(up.getInputStream());
                byte[] buff = IOUtils.slurpStream(got);
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

    /**
     * Load the nodes id from the properties file
     */
    private String loadNodeIdProperty() {
        String nodeid = null;
        String configDirectory = System.getProperty(SYSPROP_CONFIG_HOME);
        if ( configDirectory != null ) {
            File configDir = new File( configDirectory );
            File configProps = new File( configDir, NODE_ID_FILE );
            if ( configProps.isFile() ) {
                Properties properties = new Properties();
                InputStream in = null;
                try {
                    properties.load( in = new FileInputStream(configProps) );
                    nodeid = properties.getProperty( NODE_ID_PROPERTY );
                } catch ( IOException ioe ) {
                    logger.log( Level.WARNING, "Error loading node properties.", ioe);
                } finally {
                    ResourceUtils.closeQuietly(in);
                }
            }
        } else {
            logger.warning("Could not determine configuration directory.");
        }

        return nodeid;
    }

    /**
     * Store the node id to the properties file.
     */
    private void storeNodeIdProperty( final String nodeid ) {
        logger.config("Storing node identifier '"+nodeid+"'.");
        String configDirectory = System.getProperty(SYSPROP_CONFIG_HOME);
        if ( configDirectory != null ) {
            File configDir = new File( configDirectory );
            File configProps = new File( configDir, NODE_ID_FILE );
            if ( configDir.isDirectory() ) {
                OutputStream out = null;
                try {
                    PropertiesConfiguration newProps = new PropertiesConfiguration();
                    newProps.setAutoSave(false);
                    newProps.setListDelimiter((char)0);
                    if ( configProps.isFile() ) {
                        newProps.load(configProps);
                    }
                    newProps.setProperty( NODE_ID_PROPERTY, nodeid );

                    out = new FileOutputStream(configProps);
                    newProps.save(out, "iso-8859-1");
                } catch ( IOException ioe ) {
                    logger.log( Level.WARNING, "Error loading node properties.", ioe);
                } catch ( ConfigurationException ioe ) {
                    logger.log( Level.WARNING, "Error storing node properties.", ioe);
                } finally {
                    ResourceUtils.closeQuietly(out);
                }
            } else {
                logger.warning("Could not determine configuration directory to save nodeid.");
            }
        } else {
            logger.warning("Could not determine configuration directory to save nodeid.");
        }
    }    
}
