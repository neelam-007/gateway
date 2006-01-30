/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerConfig extends ApplicationObjectSupport {
    public static final String PARAM_SERVICE_RESOLVERS = "serviceResolvers";
    public static final String PARAM_SERVER_ID = "serverId";
    public static final String PARAM_KEYSTORE = "keystorePropertiesPath";
    public static final String PARAM_LDAP_TEMPLATES = "ldapTemplatesPath";
    public static final String PARAM_HIBERNATE = "hibernatePropertiesPath";
    public static final String PARAM_IPS = "ipAddresses";
    public static final String PARAM_HTTP_PORTS = "httpPorts";
    public static final String PARAM_HTTPS_PORTS = "httpsPorts";
    public static final String PARAM_HOSTNAME = "hostname";
    public static final String PARAM_SYSTEMPROPS = "systemPropertiesPath";
    public static final String PARAM_SERVERCOMPONENTS = "serverComponents";
    public static final String PARAM_JMS_THREAD_POOL_SIZE = "jmsThreadPoolSize";
    public static final String PARAM_MULTICAST_ADDRESS = "multicastAddress";
    public static final String PARAM_CONFIG_DIRECTORY = "configDirectory";
    public static final String PARAM_ATTACHMENT_DIRECTORY = "attachmentDirectory";
    public static final String PARAM_ATTACHMENT_DISK_THRESHOLD = "attachmentDiskThreshold";

    public static final String PARAM_AUDIT_MESSAGE_THRESHOLD = "auditMessageThreshold";
    public static final String PARAM_AUDIT_ADMIN_THRESHOLD = "auditAdminThreshold";
    public static final String PARAM_AUDIT_PURGE_MINIMUM_AGE = "auditPurgeMinimumAge";

    public static final String PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD = "auditAssociatedLogsThreshold";

    public static final String PARAM_ANTIVIRUS_ENABLED = "savseEnable";
    public static final String PARAM_ANTIVIRUS_HOST = "savseHost";
    public static final String PARAM_ANTIVIRUS_PORT = "savsePort";

    public static final String MAX_LDAP_SEARCH_RESULT_SIZE = "maxLdapSearchResultSize";

    public static final int DEFAULT_JMS_THREAD_POOL_SIZE = 200;

    public static final String PROPS_PATH_PROPERTY = "com.l7tech.server.serverConfigPropertiesPath";
    public static final String PROPS_PATH_DEFAULT = "/ssg/etc/conf/serverconfig.properties";
    public static final String PROPS_RESOURCE_PATH = "serverconfig.properties";

    public static final String PROPS_OVER_PATH_PROPERTY = "com.l7tech.server.serverConfigOverridePropertiesPath";
    public static final String PROPS_OVER_PATH_DEFAULT = "/ssg/etc/conf/serverconfig_override.properties";

    private static final String SUFFIX_JNDI = ".jndi";
    private static final String SUFFIX_SYSPROP = ".systemProperty";
    private static final String SUFFIX_SETSYSPROP = ".setSystemProperty";
    private static final String SUFFIX_DESC = ".description";
    private static final String SUFFIX_DEFAULT = ".default";

    public static ServerConfig getInstance() {
        if (_instance == null) _instance = new ServerConfig();
        return _instance;
    }

    public String getProperty(String propName) {
        String sysPropProp = propName + SUFFIX_SYSPROP;
        String setSysPropProp = propName + SUFFIX_SETSYSPROP;
        String jndiProp = propName + SUFFIX_JNDI;
        String dfltProp = propName + SUFFIX_DEFAULT;

        String systemValue = getPropertyValue(sysPropProp);
        String setSystemValue = getPropertyValue(setSysPropProp);
        String jndiValue = getPropertyValue(jndiProp);
        String defaultValue = getPropertyValue(dfltProp);

        String value = null;

        if ( systemValue != null && systemValue.length() > 0 ) {
            logger.finest("Checking System property " + systemValue);
            value = System.getProperty(systemValue);
        }

        if (value == null && jndiValue != null && jndiValue.length() > 0 ) {
            try {
                logger.finest("Checking JNDI property " + jndiValue);
                if (_icontext == null) _icontext = new InitialContext();
                value = (String)_icontext.lookup(jndiValue);
            } catch (NamingException ne) {
                logger.fine(ne.getMessage());
            }
        }
        
        if ( value == null ) value = getPropertyValue( propName );

        if ( value == null ) {
            logger.finest("Using default value " + defaultValue);
            value = defaultValue;
        }

        if (value != null && "true".equalsIgnoreCase(setSystemValue)) {
            System.setProperty(systemValue, value);
        }

        return value;
    }

    private String getPropertyValue(String prop) {
        String val = (String)_properties.get(prop);
        if (val == null) return null;
        if (val.length() == 0) return val;

        StringBuffer val2 = new StringBuffer();
        int pos = val.indexOf('$');
        if (pos >= 0) {
            while (pos >= 0) {
                if (val.charAt(pos + 1) == '{') {
                    int pos2 = val.indexOf('}', pos + 1);
                    if (pos2 >= 0) {
                        // there's a reference
                        String prop2 = val.substring(pos + 2, pos2);
                        String ref = getProperty(prop2);
                        if (ref == null) {
                            val2.append("${");
                            val2.append(prop2);
                            val2.append("}");
                        } else {
                            val2.append(ref);
                        }

                        pos = val.indexOf('$', pos + 1);
                        if (pos >= 0) {
                            val2.append(val.substring(pos2 + 1, pos));
                        } else {
                            val2.append(val.substring(pos2 + 1));
                        }
                    } else {
                        // there's no terminating }, pass it through literally
                        val2.append(val);
                        break;
                    }
                }
            }
        } else {
            val2.append(val);
        }
        return val2.toString();

    }

    private ServerConfig() {
        _properties = new Properties();

        String configPropertiesPath = System.getProperty(PROPS_PATH_PROPERTY);
        if (configPropertiesPath == null) configPropertiesPath = PROPS_PATH_DEFAULT;

        InputStream propStream = null;
        try {

            File file = new File(configPropertiesPath);
            if (file.exists())
                propStream = new FileInputStream(file);
            else
                propStream = getClass().getClassLoader().getResourceAsStream(PROPS_RESOURCE_PATH);

            if (propStream != null) {
                _properties.load(propStream);
            } else {
                logger.severe("Couldn't load serverconfig.properties!");
                throw new RuntimeException("Couldn't load serverconfig.properties!");
            }
        } catch (IOException ioe) {
            logger.severe("Couldn't load serverconfig.properties!");
            throw new RuntimeException("Couldn't load serverconfig.properties!");
        } finally {
            if (propStream != null) try { propStream.close(); } catch (IOException e) {}
        }

        // Find and process any override properties
        String overridePath = System.getProperty(PROPS_OVER_PATH_PROPERTY);
        if (overridePath == null) overridePath = PROPS_OVER_PATH_DEFAULT;

        InputStream overStream = null;
        try {
            if (overridePath != null) {
                propStream = new FileInputStream(overridePath);
                Properties op = new Properties();
                op.load(propStream);

                Set opKeys = op.keySet();
                for (Iterator i = opKeys.iterator(); i.hasNext();) {
                    Object s = i.next();
                    _properties.put(s, op.get(s));
                    logger.log(Level.FINE, "Overriding serverconfig property: " + s);
                }
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.INFO, "Couldn't find serverconfig_override.properties; continuing with no overrides");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading serverconfig_override.properties; continuing with no overrides", e);
        } finally {
            if (overStream != null) try { overStream.close(); } catch (IOException e) {}
        }

        // export as system property. This is required so custom assertions
        // do not need to import in the ServerConfig and the clases referred by it
        // (LogManager, TransportProtocol) to read the single property.  - em20040506
        String cfgDirectory = getProperty(PARAM_CONFIG_DIRECTORY);
        if (cfgDirectory !=null) {
            System.setProperty("ssg.config.dir", cfgDirectory);
        } else {
            logger.warning("The server config directory value is empty");
        }
    }

    private String print(InetAddress ip) {
        StringBuffer result = new StringBuffer();
        byte[] addr = ip.getAddress();
        for (int i = 0; i < addr.length; i++) {
            result.append(addr[i] & 0xff);
            if (i < addr.length - 1) result.append(".");
        }
        return result.toString();
    }

    public int getServerId() {
        if (_serverId == 0) {
            try {
                String sid = getProperty(PARAM_SERVER_ID);
                if (sid != null && sid.length() > 0)
                    _serverId = new Byte(sid).byteValue();
            } catch (NumberFormatException nfe) {
            }

            if (_serverId == 0) {
                try {
                    InetAddress localhost = InetAddress.getLocalHost();
                    byte[] ip = localhost.getAddress();
                    _serverId = ip[3] & 0xff;
                    logger.info("ServerId parameter not set, assigning server ID " + _serverId +
                      " from server's IP address");
                } catch (UnknownHostException e) {
                    _serverId = 1;
                    logger.severe("Couldn't get server's local host!  Using server ID " + _serverId);
                }
            }
        }
        return _serverId;
    }


    public long getServerBootTime() {
        return _serverBootTime;
    }

    public Iterator getIpProtocolPorts() {
        if (_ipProtocolPorts == null) {
            ArrayList ipps = new ArrayList();
            String[] sHttpPorts = getProperty(PARAM_HTTP_PORTS).trim().split(",\\s*");
            ArrayList httpPortsList = new ArrayList();
            for (int i = 0; i < sHttpPorts.length; i++) {
                String s = sHttpPorts[i];
                try {
                    httpPortsList.add(new Integer(s));
                } catch (NumberFormatException nfe) {
                    logger.log(Level.SEVERE, "Couldn't parse HTTP port '" + s + "'");
                }
            }

            int[] httpPorts = new int[httpPortsList.size()];
            int j = 0;
            for (Iterator i = httpPortsList.iterator(); i.hasNext();) {
                Integer integer = (Integer)i.next();
                httpPorts[j++] = integer.intValue();
            }

            String[] sHttpsPorts = getProperty(PARAM_HTTPS_PORTS).trim().split(",\\s*");
            ArrayList httpsPortsList = new ArrayList();
            for (int i = 0; i < sHttpsPorts.length; i++) {
                String s = sHttpsPorts[i];
                try {
                    httpsPortsList.add(new Integer(s));
                } catch (NumberFormatException nfe) {
                    logger.log(Level.SEVERE, "Couldn't parse HTTPS port '" + s + "'");
                }
            }

            int[] httpsPorts = new int[httpsPortsList.size()];
            j = 0;
            for (Iterator i = httpsPortsList.iterator(); i.hasNext();) {
                Integer integer = (Integer)i.next();
                httpsPorts[j++] = integer.intValue();
            }

            // Collect local IP addresses (default to all addresses on all interfaces)
            ArrayList localIps = new ArrayList();
            String ipAddressesProperty = getProperty(PARAM_IPS);
            if (ipAddressesProperty != null) {
                String[] sips = ipAddressesProperty.split(",\\s*");
                for (int i = 0; i < sips.length; i++) {
                    String sip = sips[i];
                    try {
                        InetAddress addr = InetAddress.getByName(sip);
                        logger.info("Found IP address " + print(addr) + " from configuration. ");
                        localIps.add(addr);
                    } catch (UnknownHostException uhe) {
                        logger.log(Level.WARNING, "Got UnknownHostException trying to resolve IP address" + sip, uhe);
                    }
                }
            } else {
                try {
                    Enumeration ints = NetworkInterface.getNetworkInterfaces();
                    NetworkInterface net;
                    InetAddress ip;

                    while (ints.hasMoreElements()) {
                        net = (NetworkInterface)ints.nextElement();
                        Enumeration ips = net.getInetAddresses();
                        while (ips.hasMoreElements()) {
                            ip = (InetAddress)ips.nextElement();
                            if (ip instanceof Inet4Address && (ip.getAddress()[0] & 0xff) != 127) {
                                // Ignore localhost for autoconfigured IPs
                                logger.info("Found IP address " + print(ip) + " on network interface " + net.getName());
                                localIps.add(ip);
                            }
                        }
                    }
                } catch (SocketException se) {
                    logger.log(Level.SEVERE, "Got SocketException while enumerating IP addresses!", se);
                }
            }

            for (Iterator i = localIps.iterator(); i.hasNext();) {
                InetAddress ip = (InetAddress)i.next();

                for (j = 0; j < httpPorts.length; j++) {
                    ipps.add(new HttpTransport.IpProtocolPort(ip, TransportProtocol.HTTP, httpPorts[j]));
                    logger.info("Configured HTTP on " + print(ip) + ":" + httpPorts[j]);
                }

                for (j = 0; j < httpsPorts.length; j++) {
                    ipps.add(new HttpTransport.IpProtocolPort(ip, TransportProtocol.HTTPS, httpsPorts[j]));
                    logger.info("Configured HTTPS on " + print(ip) + ":" + httpsPorts[j]);
                }
            }
            _ipProtocolPorts = Collections.unmodifiableList(ipps);
        }

        return _ipProtocolPorts.iterator();
    }

    public String getHostname() {
        if (_hostname == null) {
            _hostname = getProperty(PARAM_HOSTNAME);

            if (_hostname == null) {
                try {
                    _hostname = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    logger.info("HostName parameter not set, assigning hostname " + _hostname);
                }
            }
        }

        return _hostname;
    }

    /**
     * Get the attachment disk spooling threshold in bytes.
     * WARNING: This method is a tad slow and is not recommended to be called on the critical path.
     *
     * @return The theshold in bytes above which MIME parts will be spooled to disk.  Always nonnegative.
     */
    public int getAttachmentDiskThreshold() {
        String str = getProperty(PARAM_ATTACHMENT_DISK_THRESHOLD);

        int ret = 0;
        if (str != null && str.length() > 0) {
            try {
                ret = Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                // fallthrough
            }
        }

        if (ret < 1) {
            int def = 131071;
            String errorMsg = "The property " + PARAM_ATTACHMENT_DIRECTORY + " is undefined or invalid. Please ensure the SecureSpan " +
                    "Gateway is properly configured.  (Will use default of " + def + ")";
            logger.severe(errorMsg);
            return def;
        }

        return ret;
    }

    public File getAttachmentDirectory() {
        String attachmentsPath = getProperty(PARAM_ATTACHMENT_DIRECTORY);

        if (attachmentsPath == null || attachmentsPath.length() <= 0) {
            final String def = "/ssg/var/attachments";
            String errorMsg = "The property " + PARAM_ATTACHMENT_DIRECTORY + " is not defined. Please ensure the SecureSpan " +
                    "Gateway is properly configured.  (Will use default of " + def + ")";
            logger.severe(errorMsg);
            return new File(def);
        }

        File attachmentsDir = new File(attachmentsPath);
        if (!attachmentsDir.exists())
            attachmentsDir.mkdir();

        if (!attachmentsDir.exists()) {
            String errorMsg = "The property " + PARAM_ATTACHMENT_DIRECTORY + ", defined as the directory " + attachmentsPath +
                    ", is required for caching large attachments but was not found. Please ensure the " +
                    "SecureSpan Gateway is properly installed.";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (!attachmentsDir.canWrite()) {
            String errorMsg = "The property " + PARAM_ATTACHMENT_DIRECTORY + ", defined as the directory " + attachmentsPath +
                    ", is required for caching large attachments but was not writable by the Gateway process.  " +
                    "Please ensure the SecureSpan Gateway is properly installed.";
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        return attachmentsDir;
    }

    /**
     * shortcut
     */
    public final ApplicationContext getSpringContext() {
        return super.getApplicationContext();
    }

    private int _serverId;
    private final long _serverBootTime = System.currentTimeMillis();
    private List _ipProtocolPorts;
    private String _hostname;
    private Properties _properties;

    private static ServerConfig _instance;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private InitialContext _icontext;
}
