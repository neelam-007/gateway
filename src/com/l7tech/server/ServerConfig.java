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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerConfig {
    private static final String PARAM_LOG_LEVEL         = "SsgLogLevel";
    private static final String PARAM_SERVICE_RESOLVERS = "ServiceResolvers";
    private static final String PARAM_SERVER_ID         = "ServerId";
    private static final String PARAM_KEYSTORE          = "KeystorePropertiesPath";
    private static final String JNDI_PREFIX             = "java:comp/env/";

    private static final String JNDI_LOG_LEVEL          = JNDI_PREFIX + PARAM_LOG_LEVEL;
    private static final String JNDI_SERVICE_RESOLVERS  = JNDI_PREFIX + PARAM_SERVICE_RESOLVERS;
    private static final String JNDI_SERVER_ID          = JNDI_PREFIX + PARAM_SERVER_ID;
    private static final String JNDI_KEYSTORE           = JNDI_PREFIX + PARAM_KEYSTORE;

    public static final String PROP_LOG_LEVEL = "com.l7tech.server.logLevel";
    public static final String PROP_SERVER_ID = "com.l7tech.server.serverId";
    public static final String PROP_RESOLVERS = "com.l7tech.server.serviceResolvers";
    public static final String PROP_KEYSTORE_PROPS_PATH = "com.l7tech.server.keystorePropertiesPath";

    public static final String DEFAULT_LOG_LEVEL = "INFO";
    public static final String DEFAULT_KEYSTORE_PROPS_PATH = "/ssg/etc/conf/keystore.properties";
    public static final String DEFAULT_SERVICE_RESOLVERS =
        UrnResolver.class.getName() + " " +
        SoapActionResolver.class.getName() + " " +
        HttpUriResolver.class.getName();

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

        _logLevel = getProperty( PROP_LOG_LEVEL, JNDI_LOG_LEVEL, DEFAULT_LOG_LEVEL );
        logger.setLevel( Level.parse( _logLevel ) );
        _serviceResolvers = getProperty( PROP_RESOLVERS, JNDI_SERVICE_RESOLVERS, DEFAULT_SERVICE_RESOLVERS );
        _keystorePropertiesPath = getProperty( PROP_KEYSTORE_PROPS_PATH, JNDI_KEYSTORE, DEFAULT_KEYSTORE_PROPS_PATH );

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
                _serverId = (byte)(ip[3] & 0x7f);
                logger.info( "ServerId parameter not set, assigning server ID " + _serverId +
                              " from server's IP address");
            } catch ( UnknownHostException e ) {
                _serverId = 1;
                logger.severe( "Couldn't get server's local host!  Using server ID " + _serverId );
            }
        }

        logger.info( "LogLevel = " + _logLevel );
        logger.info( "KeystorePath = " + _keystorePropertiesPath );
        logger.info( "ServerId = " + _serverId );
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

    public String getLogLevel() {
        return _logLevel;
    }

    public String getKeystorePropertiesPath() {
        return _keystorePropertiesPath;
    }

    protected int _serverId;
    protected long _serverBootTime;
    protected String _serviceResolvers;
    protected String _logLevel;
    protected String _keystorePropertiesPath;

    private static ServerConfig _instance;
    private Logger logger = LogManager.getInstance().getSystemLogger();
    InitialContext _icontext;
}
