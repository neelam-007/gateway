package com.ca.siteminder;

import netegrity.siteminder.javaagent.ServerDef;

import java.util.List;

public interface SiteMinderConfig {

    public static final String SERVER_ADDRESS_PROPNAME = "address";
    public static final String SERVER_AUTHN_PORT_PROPNAME = "authentication.port";
    public static final String SERVER_AUTHZ_PORT_PROPNAME = "authorization.port";
    public static final String SERVER_ACCT_PORT_PROPNAME = "accounting.port";
    public static final String SERVER_CONN_MIN_PROPNAME = "connection.min";
    public static final String SERVER_CONN_MAX_PROPNAME = "connection.max";
    public static final String SERVER_CONN_STEP_PROPNAME = "connection.step";
    public static final String SERVER_TIMEOUT = "timeout";

    static final int DEFAULT_CONNECTION_MIN = 1;
    static final int DEFAULT_CONNECTION_MAX = 10;
    static final int DEFAULT_CONNECTION_STEP = 1;
    static final int DEFAULT_TIMEOUT = 75;

    public String getAgentName();

    public String getAddress();

    public String getSecret();

    public boolean isIpCheck();

    public String getHostname();

    public int getFipsMode();

    public boolean isNonClusterFailover();

    public int getClusterThreshold();

    public boolean isUpdateSSOToken();

    public List<ServerDef> getServers();

    public boolean isCluster();

}
