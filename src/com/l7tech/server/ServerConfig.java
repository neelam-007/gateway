/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.service.resolution.UrnResolver;
import com.l7tech.service.resolution.SoapActionResolver;
import com.l7tech.service.resolution.HttpUriResolver;
import com.l7tech.logging.LogManager;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerConfig {
    private static final String PARAM_LOG_LEVEL         = "SsgLogLevel";
    private static final String PARAM_SERVICE_RESOLVERS = "ServiceResolvers";
    private static final String PARAM_SERVER_ID         = "ServerId";
    private static final String JNDI_PREFIX             = "java:comp/env/";
    private static final String JNDI_LOG_LEVEL          = JNDI_PREFIX + PARAM_LOG_LEVEL;
    private static final String JNDI_SERVICE_RESOLVERS  = JNDI_PREFIX + PARAM_SERVICE_RESOLVERS;
    private static final String JNDI_SERVER_ID          = JNDI_PREFIX + PARAM_SERVER_ID;

    public static ServerConfig getInstance() {
        if ( _instance == null ) _instance = new ServerConfig();
        return _instance;
    }

    private ServerConfig() {
        _serverBootTime = System.currentTimeMillis();

        try {
            InitialContext ic = new InitialContext();
            _serviceResolvers = (String)ic.lookup( JNDI_SERVICE_RESOLVERS );
            _serverId = new Byte( (String)ic.lookup( JNDI_SERVER_ID ) ).byteValue();
            _logLevel = (String)(ic.lookup( JNDI_LOG_LEVEL ));

        } catch ( NamingException ne ) {
            _log.log( Level.WARNING, ne.getMessage(), ne );
        } catch ( NumberFormatException nfe ) {
            _log.log( Level.WARNING, nfe.getMessage(), nfe );
        }

        if ( _serviceResolvers == null ) {
            StringBuffer classnames = new StringBuffer();
            classnames.append( UrnResolver.class.getName() );
            classnames.append( " " );
            classnames.append( SoapActionResolver.class.getName() );
            classnames.append( " " );
            classnames.append( HttpUriResolver.class.getName() );
            _serviceResolvers = classnames.toString();
        }

        if ( _serverId == 0 ) {
            try {
                InetAddress localhost = InetAddress.getLocalHost();
                byte[] ip = localhost.getAddress();
                _serverId = ip[3];
                _log.warning( "ServerId parameter not set, assigning server ID " + _serverId +
                              " from server's IP address");
            } catch ( UnknownHostException e ) {
                _serverId = 1;
                _log.severe( "Couldn't get server's local host!  Using server ID " + _serverId );
            }
        }
    }

    public byte getServerId() {
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

    protected byte _serverId;
    protected long _serverBootTime;
    protected String _serviceResolvers;
    protected String _logLevel;

    protected static ServerConfig _instance;

    protected Logger _log = LogManager.getInstance().getSystemLogger();
}
