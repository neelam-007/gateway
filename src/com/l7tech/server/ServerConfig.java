/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.logging.LogManager;
import com.l7tech.service.resolution.HttpUriResolver;
import com.l7tech.service.resolution.SoapActionResolver;
import com.l7tech.service.resolution.UrnResolver;
import com.l7tech.message.TransportProtocol;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Collections;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerConfig {
    private static final String PARAM_SERVICE_RESOLVERS = "ServiceResolvers";
    private static final String PARAM_SERVER_ID         = "ServerId";
    private static final String PARAM_KEYSTORE          = "KeystorePropertiesPath";
    private static final String PARAM_HIBERNATE         = "KeystorePropertiesPath";
    private static final String PARAM_IPS               = "IpAddresses";
    private static final String PARAM_HTTP_PORTS        = "HttpPorts";
    private static final String PARAM_HTTPS_PORTS       = "HttpsPorts";
    private static final String PARAM_HOSTNAME          = "Hostname";
    private static final String PARAM_SYSTEMPROPS       = "SystemPropertiesPath";
    private static final String JNDI_PREFIX             = "java:comp/env/";

    private static final String JNDI_SERVICE_RESOLVERS  = JNDI_PREFIX + PARAM_SERVICE_RESOLVERS;
    private static final String JNDI_SERVER_ID          = JNDI_PREFIX + PARAM_SERVER_ID;
    private static final String JNDI_KEYSTORE           = JNDI_PREFIX + PARAM_KEYSTORE;
    private static final String JNDI_HIBERNATE          = JNDI_PREFIX + PARAM_HIBERNATE;
    private static final String JNDI_IPS                = JNDI_PREFIX + PARAM_IPS;
    private static final String JNDI_HTTP_PORTS         = JNDI_PREFIX + PARAM_HTTP_PORTS;
    private static final String JNDI_HTTPS_PORTS        = JNDI_PREFIX + PARAM_HTTPS_PORTS;
    private static final String JNDI_HOSTNAME           = JNDI_PREFIX + PARAM_HOSTNAME;
    private static final String JNDI_SYSTEMPROPS        = JNDI_PREFIX + PARAM_SYSTEMPROPS;

    public static final String PROP_SERVER_ID = "com.l7tech.server.serverId";
    public static final String PROP_RESOLVERS = "com.l7tech.server.serviceResolvers";
    public static final String PROP_KEYSTORE_PROPS_PATH = "com.l7tech.server.keystorePropertiesPath";
    public static final String PROP_HIBERNATE_PROPS_PATH = "com.l7tech.server.hibernatePropertiesPath";
    public static final String PROP_IPS = "com.l7tech.server.ipAddresses";
    public static final String PROP_HTTP_PORTS = "com.l7tech.server.httpPorts";
    public static final String PROP_HTTPS_PORTS = "com.l7tech.server.httpsPorts";
    public static final String PROP_HOSTNAME = "com.l7tech.server.hostname";
    public static final String PROP_SYSTEMPROPS = "com.l7tech.server.systemPropertiesPath";

    public static final String DEFAULT_KEYSTORE_PROPS_PATH = "/ssg/etc/conf/keystore.properties";
    public static final String DEFAULT_HIBERNATE_PROPS_PATH = "/ssg/etc/conf/hibernate.properties";
    public static final String DEFAULT_SYSTEMPROPS_PATH = "/ssg/etc/conf/system.properties";
    public static final String DEFAULT_SERVICE_RESOLVERS =
        UrnResolver.class.getName() + " " +
        SoapActionResolver.class.getName() + " " +
        HttpUriResolver.class.getName();

    public static final String DEFAULT_HTTP_PORTS = "80,8080";
    public static final String DEFAULT_HTTPS_PORTS = "443,8443";

    public static ServerConfig getInstance() {
        if ( _instance == null ) _instance = new ServerConfig();
        return _instance;
    }

    private String getProperty( String systemPropName, String jndiName, String dflt ) {
        logger.fine( "Checking System property " + systemPropName );
        String value = System.getProperty( systemPropName );
        if ( value == null && jndiName != null ) {
            try {
                logger.fine( "Checking JNDI property " + jndiName );
                if ( _icontext == null ) _icontext = new InitialContext();
                value = (String)_icontext.lookup( jndiName );
            } catch ( NamingException ne ) {
                logger.fine( ne.getMessage() );
            }
        }

        if ( value == null ) {
            logger.fine( "Using default value " + dflt );
            value = dflt;
        }

        return value;
    }

    private ServerConfig() {
        _serverBootTime = System.currentTimeMillis();

        _serviceResolvers = getProperty( PROP_RESOLVERS, JNDI_SERVICE_RESOLVERS, DEFAULT_SERVICE_RESOLVERS );
        _keystorePropertiesPath = getProperty( PROP_KEYSTORE_PROPS_PATH, JNDI_KEYSTORE, DEFAULT_KEYSTORE_PROPS_PATH );
        _hibernatePropertiesPath = getProperty( PROP_HIBERNATE_PROPS_PATH, JNDI_HIBERNATE, DEFAULT_HIBERNATE_PROPS_PATH );
        _systemPropertiesPath = getProperty( PROP_SYSTEMPROPS, JNDI_SYSTEMPROPS, DEFAULT_SYSTEMPROPS_PATH );

        _hostname = getProperty( PROP_HOSTNAME, JNDI_HOSTNAME, null );

        if ( _hostname == null ) {
            try {
                _hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                logger.fine( "HostName parameter not set, assigning hostname " + _hostname );
                e.printStackTrace();  //To change body of catch statement use Options | File Templates.
            }
        }

        try {
            String sid = getProperty( PROP_SERVER_ID, JNDI_SERVER_ID, null );
            if ( sid != null && sid.length() > 0 )
                _serverId = new Byte( sid ).byteValue();
        } catch ( NumberFormatException nfe ) {
        }

        if ( _serverId == 0 ) {
            try {
                InetAddress localhost = InetAddress.getLocalHost();
                byte[] ip = localhost.getAddress();
                _serverId = ip[3] & 0xff;
                logger.info( "ServerId parameter not set, assigning server ID " + _serverId +
                              " from server's IP address");
            } catch ( UnknownHostException e ) {
                _serverId = 1;
                logger.severe( "Couldn't get server's local host!  Using server ID " + _serverId );
            }
        }

        logger.info( "KeystorePath = " + _keystorePropertiesPath );
        logger.info( "HibernatePath = " + _hibernatePropertiesPath );
        logger.info( "ServerId = " + _serverId );
        logger.info( "Hostname = " + _hostname );

        String[] sHttpPorts = getProperty( PROP_HTTP_PORTS, JNDI_HTTP_PORTS, DEFAULT_HTTP_PORTS ).trim().split(",\\s*");
        ArrayList httpPortsList = new ArrayList();
        for (int i = 0; i < sHttpPorts.length; i++) {
            String s = sHttpPorts[i];
            try {
                httpPortsList.add( new Integer(s) );
            } catch ( NumberFormatException nfe ) {
                logger.log( Level.SEVERE, "Couldn't parse HTTP port '" + s + "'" );
            }
        }

        int[] httpPorts = new int[httpPortsList.size()];
        int j = 0;
        for (Iterator i = httpPortsList.iterator(); i.hasNext();) {
            Integer integer = (Integer) i.next();
            httpPorts[j++] = integer.intValue();
        }

        String[] sHttpsPorts = getProperty( PROP_HTTPS_PORTS, JNDI_HTTPS_PORTS, DEFAULT_HTTPS_PORTS ).trim().split(",\\s*");
        ArrayList httpsPortsList = new ArrayList();
        for (int i = 0; i < sHttpsPorts.length; i++) {
            String s = sHttpsPorts[i];
            try {
                httpsPortsList.add( new Integer(s) );
            } catch ( NumberFormatException nfe ) {
                logger.log( Level.SEVERE, "Couldn't parse HTTPS port '" + s + "'" );
            }
        }

        int[] httpsPorts = new int[httpsPortsList.size()];
        j = 0;
        for (Iterator i = httpsPortsList.iterator(); i.hasNext();) {
            Integer integer = (Integer) i.next();
            httpsPorts[j++] = integer.intValue();
        }

        // Collect local IP addresses (default to all addresses on all interfaces)
        ArrayList localIps = new ArrayList();
        String ipAddressesProperty = getProperty( PROP_IPS, JNDI_IPS, null );
        if ( ipAddressesProperty != null ) {
            String[] sips = ipAddressesProperty.split( ",\\s*" );
            for (int i = 0; i < sips.length; i++) {
                String sip = sips[i];
                try {
                    InetAddress addr = InetAddress.getByName( sip );
                    logger.info( "Found IP address " + print(addr) + " from configuration. " );
                    localIps.add( addr );
                } catch ( UnknownHostException uhe ) {
                    logger.log( Level.WARNING, "Got UnknownHostException trying to resolve IP address" + sip, uhe );
                }
            }
        } else {
            try {
                Enumeration ints = NetworkInterface.getNetworkInterfaces();
                NetworkInterface net;
                InetAddress ip;

                while ( ints.hasMoreElements() ) {
                    net = (NetworkInterface)ints.nextElement();
                    Enumeration ips = net.getInetAddresses();
                    while ( ips.hasMoreElements() ) {
                        ip = (InetAddress)ips.nextElement();
                        if ( ( ip.getAddress()[0] & 0xff ) != 127 ) {
                            // Ignore localhost for autoconfigured IPs
                            logger.info( "Automatically found IP address " + print(ip) + " on network interface " + net.getName() );
                            localIps.add( ip );
                        }
                    }
                }
            } catch ( SocketException se ) {
                logger.log( Level.SEVERE, "Got SocketException while enumerating IP addresses!", se );
            }
        }

        for (Iterator i = localIps.iterator(); i.hasNext();) {
            InetAddress ip = (InetAddress) i.next();

            for (j = 0; j < httpPorts.length; j++) {
                _ipProtocolPorts.add( new HttpTransport.IpProtocolPort( ip, TransportProtocol.HTTP, httpPorts[j] ) );
                logger.info( "Configured HTTP on " + print(ip) + ":" + httpPorts[j] );
            }

            for (j = 0; j < httpsPorts.length; j++) {
                _ipProtocolPorts.add( new HttpTransport.IpProtocolPort( ip, TransportProtocol.HTTPS, httpsPorts[j] ) );
                logger.info( "Configured HTTPS on " + print(ip) + ":" + httpsPorts[j] );
            }
        }
    }

    private String print( InetAddress ip ) {
        StringBuffer result = new StringBuffer();
        byte[] addr = ip.getAddress();
        for (int i = 0; i < addr.length; i++) {
            result.append( addr[i] & 0xff );
            if ( i < addr.length-1 ) result.append( "." );
        }
        return result.toString();
    }

    public static void main(String[] args) {
        ServerConfig config = ServerConfig.getInstance();
    }

    public int getServerId() {
        return _serverId;
    }

    public String getServiceResolvers() {
        return _serviceResolvers;
    }

    public long getServerBootTime() {
        return _serverBootTime;
    }

    public String getKeystorePropertiesPath() {
        return _keystorePropertiesPath;
    }

    public String getHibernatePropertiesPath() {
        return _hibernatePropertiesPath;
    }

    public String getSystemPropertiesPath() {
        return _systemPropertiesPath;
    }

    public Iterator getIpProtocolPorts() {
        return Collections.unmodifiableList(_ipProtocolPorts).iterator();
    }

    public String getHostname() {
        return _hostname;
    }

    private int _serverId;
    private long _serverBootTime;
    private String _serviceResolvers;
    private String _keystorePropertiesPath;
    private String _hibernatePropertiesPath;
    private String _systemPropertiesPath;
    private ArrayList _ipProtocolPorts = new ArrayList();
    private String _hostname;

    private static ServerConfig _instance;
    private Logger logger = LogManager.getInstance().getSystemLogger();
    private InitialContext _icontext;
}
