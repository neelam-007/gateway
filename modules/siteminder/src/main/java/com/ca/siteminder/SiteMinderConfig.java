package com.ca.siteminder;

import netegrity.siteminder.javaagent.ServerDef;

import java.util.List;

public interface SiteMinderConfig {

    String SERVER_ADDRESS_PROPNAME = "address";
    String SERVER_AUTHN_PORT_PROPNAME = "authentication.port";
    String SERVER_AUTHZ_PORT_PROPNAME = "authorization.port";
    String SERVER_ACCT_PORT_PROPNAME = "accounting.port";
    String SERVER_CONN_MIN_PROPNAME = "connection.min";
    String SERVER_CONN_MAX_PROPNAME = "connection.max";
    String SERVER_CONN_STEP_PROPNAME = "connection.step";
    String SERVER_TIMEOUT = "timeout";
    String SYSTEM_PROP_PREFIX = "siteminder.cache.";
    String AGENT_RESOURCE_CACHE_SIZE_PROPNAME = "resourceCache.size";
    String AGENT_RESOURCE_CACHE_MAX_AGE_PROPNAME = "resourceCache.maxAge";
    String AGENT_AUTHENTICATION_CACHE_SIZE_PROPNAME = "authenticationCache.size";
    String AGENT_AUTHENTICATION_CACHE_MAX_AGE_PROPNAME = "authenticationCache.maxAge";
    String AGENT_AUTHORIZATION_CACHE_SIZE_PROPNAME = "authorizationCache.size";
    String AGENT_AUTHORIZATION_CACHE_MAX_AGE_PROPNAME = "authorizationCache.maxAge";

    static final int DEFAULT_CONNECTION_MIN = 1;
    static final int DEFAULT_CONNECTION_MAX = 10;
    static final int DEFAULT_CONNECTION_STEP = 1;
    static final int DEFAULT_TIMEOUT = 75;


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
