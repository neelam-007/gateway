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
import java.util.logging.Level;

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
            _logLevel = (String)(ic.lookup( JNDI_LOG_LEVEL ));
            _serviceResolvers = (String)ic.lookup( JNDI_SERVICE_RESOLVERS );
            String sid = (String)ic.lookup( JNDI_SERVER_ID );
            if ( sid != null && sid.length() > 0 )
                _serverId = new Byte( sid ).byteValue();
        } catch ( NamingException ne ) {
            LogManager.getInstance().getSystemLogger().log( Level.WARNING, "Parameter " + ne.getRemainingName() + " could not be read." );
        } catch ( NumberFormatException nfe ) {
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
                LogManager.getInstance().getSystemLogger().warning( "ServerId parameter not set, assigning server ID " + _serverId +
                              " from server's IP address");
            } catch ( UnknownHostException e ) {
                _serverId = 1;
                LogManager.getInstance().getSystemLogger().severe( "Couldn't get server's local host!  Using server ID " + _serverId );
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

    private static ServerConfig _instance;
}
