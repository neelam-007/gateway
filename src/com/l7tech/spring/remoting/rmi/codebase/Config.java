/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.spring.remoting.rmi.codebase;

import sun.security.action.GetPropertyAction;

import java.security.AccessController;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Methods for configuring the codebase
 *
 * @author emil
 * @version Jan 17, 2005
 */
public final class Config {
    private static final Logger logger = Logger.getLogger(Config.class.getName());

    /** this class cannot be instantiated */
    private Config() {}

    /**
     * Configure the rmi codebase
     */
    public static void configureCodeBase() throws UnknownHostException {
        String serverCodebase = getRmiServerCodebase();
        if (serverCodebase !=null) {
            logger.fine("The 'java.rmi.server.codebase' is set, using "+serverCodebase);
        } else {
            String serverHostname = getRmiServerHostname();
            if (serverHostname == null) {
                serverHostname = determineHostname();
            }
            final String codebase = "http://"+serverHostname+":8080"+ClassServerServlet.URI_PREFIX;
            logger.fine("Setting the default 'java.rmi.server.codebase' to '"+codebase+"'");
            System.setProperty("java.rmi.server.codebase", codebase);
        }
    }

    private static String determineHostname() throws UnknownHostException {
        return InetAddress.getLocalHost().getCanonicalHostName();
    }

    private static String getRmiServerHostname() {
        String serverHostname = null;
        String prop = (String)AccessController.doPrivileged(new GetPropertyAction("java.rmi.server.hostname"));
        if (prop != null && prop.trim().length() > 0) {
            serverHostname = prop;
        }
        return serverHostname;
    }

    private static String getRmiServerCodebase() {
        String codebaseProperty = null;
        String prop = (String)java.security.AccessController.doPrivileged(new GetPropertyAction("java.rmi.server.codebase"));
        if (prop != null && prop.trim().length() > 0) {
            codebaseProperty = prop;
        }
        return codebaseProperty;
    }
}