/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.message.TransportProtocol;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerConfig implements ComponentConfig {
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

    public static final int DEFAULT_JMS_THREAD_POOL_SIZE = 200;

    public static final String PROPS_PATH_PROPERTY = "com.l7tech.server.serverConfigPropertiesPath";
    public static final String PROPS_PATH_DEFAULT = "/ssg/etc/conf/serverconfig.properties";
    public static final String PROPS_RESOURCE_PATH = "serverconfig.properties";

    private static final String SUFFIX_JNDI = ".jndi";
    private static final String SUFFIX_SYSPROP = ".systemProperty";
    private static final String SUFFIX_DESC = ".description";
    private static final String SUFFIX_DEFAULT = ".default";

    public static ServerConfig getInstance() {
        if (_instance == null) _instance = new ServerConfig();
        return _instance;
    }

    public String getProperty(String propName) {
        String sysPropProp = propName + SUFFIX_SYSPROP;
        String jndiProp = propName + SUFFIX_JNDI;
        String dfltProp = propName + SUFFIX_DEFAULT;

        String systemValue = getPropertyValue(sysPropProp);
        String jndiValue = getPropertyValue(jndiProp);
        String defaultValue = getPropertyValue(dfltProp);

        String value = null;

        if ( systemValue != null && systemValue.length() > 0 ) {
            logger.fine("Checking System property " + systemValue);
            value = System.getProperty(systemValue);
        }

        if (value == null && jndiValue != null && jndiValue.length() > 0 ) {
            try {
                logger.fine("Checking JNDI property " + jndiValue);
                if (_icontext == null) _icontext = new InitialContext();
                value = (String)_icontext.lookup(jndiValue);
            } catch (NamingException ne) {
                logger.fine(ne.getMessage());
            }
        }
        
        if ( value == null ) value = getPropertyValue( propName );

        if ( value == null ) {
            logger.fine("Using default value " + defaultValue);
            value = defaultValue;
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
                        String ref = getPropertyValue(prop2);
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

        try {
            InputStream propStream = null;

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
                                logger.info("Automatically found IP address " + print(ip) + " on network interface " + net.getName());
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
                    _ipProtocolPorts.add(new HttpTransport.IpProtocolPort(ip, TransportProtocol.HTTP, httpPorts[j]));
                    logger.info("Configured HTTP on " + print(ip) + ":" + httpPorts[j]);
                }

                for (j = 0; j < httpsPorts.length; j++) {
                    _ipProtocolPorts.add(new HttpTransport.IpProtocolPort(ip, TransportProtocol.HTTPS, httpsPorts[j]));
                    logger.info("Configured HTTPS on " + print(ip) + ":" + httpsPorts[j]);
                }
            }
        }

        return Collections.unmodifiableList(_ipProtocolPorts).iterator();
    }

    public String getHostname() {
        if (_hostname == null) {
            _hostname = getProperty(PARAM_HOSTNAME);

            if (_hostname == null) {
                try {
                    _hostname = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    logger.fine("HostName parameter not set, assigning hostname " + _hostname);
                }
            }
        }

        return _hostname;
    }

    private int _serverId;
    private final long _serverBootTime = System.currentTimeMillis();
    private ArrayList _ipProtocolPorts = new ArrayList();
    private String _hostname;
    private Properties _properties;

    private static ServerConfig _instance;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private InitialContext _icontext;
}
