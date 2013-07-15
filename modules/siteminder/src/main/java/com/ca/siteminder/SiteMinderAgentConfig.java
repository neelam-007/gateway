package com.ca.siteminder;

import netegrity.siteminder.javaagent.ServerDef;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/17/13
 */
public class SiteMinderAgentConfig {
    private static final Logger logger = Logger.getLogger(SiteMinderAgentConfig.class.getName());

    // agent config

    private static final String NAME_PROPNAME = "name";
    private static final String SECRET_PROPNAME = "secret";
    private static final String ADDRESS_PROPNAME = "address";
    private static final String IPCHECK_PROPNAME = "ipcheck";
    private static final String UPDATE_COOKIE_PROPNAME = "update_cookie";
    private static final String NONCLUSTER_FAILOVER_PROPNAME = "noncluster_failover";
    private static final String CLUSTER_THRESHOLD_PROPNAME = "cluster_threshold";
    private static final String HOSTNAME_PROPNAME = "hostname";
    private static final String FIPS_MODE_PROPNAME = "fipsmode";
    private static final String SERVER_PROPNAME = "server";
    private static final String SERVER_ADDRESS_PROPNAME = "address";
    private static final String SERVER_AUTHN_PORT_PROPNAME = "authentication.port";
    private static final String SERVER_AUTHZ_PORT_PROPNAME = "authorization.port";
    private static final String SERVER_ACCT_PORT_PROPNAME = "accounting.port";
    private static final String SERVER_CONN_MIN_PROPNAME = "connection.min";
    private static final String SERVER_CONN_MAX_PROPNAME = "connection.max";
    private static final String SERVER_CONN_STEP_PROPNAME = "connection.step";
    private static final String SERVER_TIMEOUT = "timeout";

    static final int DEFAULT_CONNECTION_MIN = 1;
    static final int DEFAULT_CONNECTION_MAX = 10;
    static final int DEFAULT_CONNECTION_STEP = 1;
    static final int DEFAULT_TIMEOUT = 75;
    static final int DEFAULT_CLUSTER_FAILOVER_THRESHOLD = 50;


    // <cluster_seq>.<server_seq>.<server_property>
    private static final Pattern SERVER_DEF_PATTERN = Pattern.compile("^([0-9]+)\\.([0-9]+)\\.(.*)");


    private final String agentID;
    private String agentName;
    private String agentSecret;
    private String agentAddress;
    private boolean agentIpCheck = false;

    private boolean updateCookie = false;


    // new in API v6
    private boolean nonClusterFailOver = false;

    private int clusterFailOverThreshold = DEFAULT_CLUSTER_FAILOVER_THRESHOLD;
    private String hostname; // The name of the registered trusted host
    private String fipsMode;
    // cluster_seq -> Map<server_seq, ServerDef>
    private Map clusters = new TreeMap();

    // - PUBLIC

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (agentID != null) sb.append(agentID).append(":\n");
        if (agentName != null) sb.append(agentID).append(".").append(NAME_PROPNAME).append("=").append(agentName).append("\n");
        if (agentSecret != null) sb.append(agentID).append(".").append(SECRET_PROPNAME).append("=").append(agentSecret).append("\n");
        if (agentAddress != null) sb.append(agentID).append(".").append(ADDRESS_PROPNAME).append("=").append(agentAddress).append("\n");
        if (hostname != null) sb.append(agentID).append(".").append(HOSTNAME_PROPNAME).append("=").append(hostname).append("\n");
        if (fipsMode != null) sb.append(agentID).append(".").append(FIPS_MODE_PROPNAME).append("=").append(fipsMode).append("\n");
        sb.append(agentID).append(".").append(IPCHECK_PROPNAME).append("=").append(agentIpCheck).append("\n");
        sb.append(agentID).append(".").append(UPDATE_COOKIE_PROPNAME).append("=").append(updateCookie).append("\n");
        sb.append(agentID).append(".").append(NONCLUSTER_FAILOVER_PROPNAME).append("=").append(nonClusterFailOver).append("\n");
        sb.append(agentID).append(".").append(CLUSTER_THRESHOLD_PROPNAME).append("=").append(clusterFailOverThreshold).append("\n");

        Iterator iter = clusters.entrySet().iterator();

        while(iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Integer cluster_seq = (Integer) entry.getKey();
            Map servers = (Map) entry.getValue();
            Iterator serverIterator = servers.values().iterator();

            while(serverIterator.hasNext()) {
                ServerDef server = (ServerDef)serverIterator.next();
                int clusterSeq = server.clusterSeq;

                sb.append(agentID).append(".").append(SERVER_PROPNAME).append(".").append(cluster_seq).append(".").append(clusterSeq).append(".")
                        .append(SERVER_ADDRESS_PROPNAME).append("=").append(server.serverIpAddress).append("\n");
                sb.append(agentID).append(".").append(SERVER_PROPNAME).append(".").append(cluster_seq).append(".").append(clusterSeq).append(".")
                        .append(SERVER_CONN_MIN_PROPNAME).append("=").append(server.connectionMin).append("\n");
                sb.append(agentID).append(".").append(SERVER_PROPNAME).append(".").append(cluster_seq).append(".").append(clusterSeq).append(".")
                        .append(SERVER_CONN_MAX_PROPNAME).append("=").append(server.connectionMax).append("\n");
                sb.append(agentID).append(".").append(SERVER_PROPNAME).append(".").append(cluster_seq).append(".").append(clusterSeq).append(".")
                        .append(SERVER_CONN_STEP_PROPNAME).append("=").append(server.connectionStep).append("\n");
                sb.append(agentID).append(".").append(SERVER_PROPNAME).append(".").append(cluster_seq).append(".").append(clusterSeq).append(".")
                        .append(SERVER_TIMEOUT).append("=").append(server.timeout).append("\n");
                sb.append(agentID).append(".").append(SERVER_PROPNAME).append(".").append(cluster_seq).append(".").append(clusterSeq).append(".")
                        .append(SERVER_AUTHN_PORT_PROPNAME).append("=").append(server.authenticationPort).append("\n");
                sb.append(agentID).append(".").append(SERVER_PROPNAME).append(".").append(cluster_seq).append(".").append(clusterSeq).append(".")
                        .append(SERVER_AUTHZ_PORT_PROPNAME).append("=").append(server.authorizationPort).append("\n");
                sb.append(agentID).append(".").append(SERVER_PROPNAME).append(".").append(cluster_seq).append(".").append(clusterSeq).append(".")
                        .append(SERVER_ACCT_PORT_PROPNAME).append("=").append(server.accountingPort).append("\n");
            }
        }

        return sb.toString();
    }

    // - PACKAGE


    SiteMinderAgentConfig (String agentID) throws SiteMinderAgentConfigurationException {
        if (agentID == null || agentID.isEmpty())
            throw new SiteMinderAgentConfigurationException("Invalid agent name: " + agentID);
        this.agentID = agentID;
    }

    String getAgentID() {
        return agentID;
    }

    void setProperty(String propName, String value) throws SiteMinderAgentConfigurationException {

        if (value.startsWith("\"") && value.endsWith("\""))
            value = value.substring(1, value.length()-1);

        if (NAME_PROPNAME.equals(propName)) {
            this.agentName = value;
        } else if (SECRET_PROPNAME.equals(propName)) {
            this.agentSecret = value;
        } else if (ADDRESS_PROPNAME.equals(propName)) {
            this.agentAddress = value;
        } else if (IPCHECK_PROPNAME.equals(propName)) {
            this.agentIpCheck = Boolean.parseBoolean(value);
        } else if(UPDATE_COOKIE_PROPNAME.equals(propName)) {
          this.updateCookie = Boolean.parseBoolean(value);
        } else if (NONCLUSTER_FAILOVER_PROPNAME.equals(propName)) {
            this.nonClusterFailOver = Boolean.parseBoolean(value);
        } else if (CLUSTER_THRESHOLD_PROPNAME.equals(propName)) {
            this.clusterFailOverThreshold = Integer.parseInt(value);
        } else if (HOSTNAME_PROPNAME.equals(propName)) {
            this.hostname = value;
        } else if (FIPS_MODE_PROPNAME.equals(propName)) {
            this.fipsMode = value;
        } else if (propName.startsWith(SERVER_PROPNAME + ".")) {
            setServerProperty(propName.substring(SERVER_PROPNAME.length()+1), value);
        } else {
            throw new SiteMinderAgentConfigurationException("Invalid agent configuration entry: " + propName);
        }
    }

    void validate() throws SiteMinderAgentConfigurationException {
        if (agentName == null || agentName.isEmpty() )
            throw new SiteMinderAgentConfigurationException("Siteminder configuration error, agent: " + agentID + " : invalid agent name: " + agentName);

        if (agentSecret == null || agentSecret.isEmpty() )
            throw new SiteMinderAgentConfigurationException("Siteminder configuration error, agent: " + agentID + " : invalid agent secret : " + agentSecret);

        if (agentAddress == null || agentAddress.isEmpty() /* todo: || not valid ip address*/)
            throw new SiteMinderAgentConfigurationException("Siteminder configuration error, agent: " + agentID + " : invalid agent address: " + agentAddress);

        if (hostname == null || hostname.isEmpty())
            throw new SiteMinderAgentConfigurationException("Siteminder configuration error, agent: " + agentID + " : invalid hostname: " + hostname);

        if (fipsMode == null || fipsMode.isEmpty())
            throw new SiteMinderAgentConfigurationException("Siteminder configuration error, agent: " + agentID + " : invalid FIPS mode: " + fipsMode);

        if (clusters == null || clusters.size() <1)
            throw new SiteMinderAgentConfigurationException("Siteminder configuration error, agent: " + agentID + " : no clusters/servers defined");

        if ( (clusters.containsKey(Integer.valueOf(0))) && clusters.size() != 1 )
            throw new SiteMinderAgentConfigurationException("Siteminder configuration error: agent: " + agentID + " : both clustered (cluster_seq = 0) and non-clustered (cluster_seq != 0) servers defined");

        Iterator clusterIter = clusters.entrySet().iterator();

        while (clusterIter.hasNext()) {
            Map.Entry cluster = (Map.Entry) clusterIter.next();
            Integer cluster_seq = (Integer) cluster.getKey();
            Map servers = ((Map)cluster.getValue());
            Iterator serverIter = servers.entrySet().iterator();
            while (serverIter.hasNext()) {
                Map.Entry serverEntry = (Map.Entry) serverIter.next();
                Integer server_seq = (Integer) serverEntry.getKey();
                ServerDef server = (ServerDef)serverEntry.getValue();

                    if (server.serverIpAddress == null/*classHelper.getServerDef_serverIpAddress(server) == null*/ || server.serverIpAddress.isEmpty()/*classHelper.getServerDef_serverIpAddress(server).isEmpty()*/ /* todo: || not valid ip address */)
                        throw new SiteMinderAgentConfigurationException("Siteminder configuration error, agent: " + agentID + " : invalid address for cluster: " + cluster_seq + " server: " + server_seq);
                    if (server.authenticationPort == 0)
                        throw new SiteMinderAgentConfigurationException("Siteminder configuration error, agent: " + agentID + " : authentication port not defined.");
                    if (server.authorizationPort == 0)
                        throw new SiteMinderAgentConfigurationException("Siteminder configuration error, agent: " + agentID + " : authorization port not defined.");
                    if (server.accountingPort == 0)
                        throw new SiteMinderAgentConfigurationException("Siteminder configuration error, agent: " + agentID + " : accounting port not defined.");

            }
        }
    }

    String getAgentName() {
        return agentName;
    }

    String getAgentSecret() {
        return agentSecret;
    }

    String getAgentAddress() {
        return agentAddress;
    }

    boolean isAgentIpCheck() {
        return agentIpCheck;
    }

    public boolean isUpdateCookie() {
        return updateCookie;
    }

    /**
     * @return true if a cluster deployment is defined (servers with cluster sequence >=1), false otherwise (all servers have cluster sequence = 0)
     */
    boolean isCluster() {
        return !clusters.containsKey(Integer.valueOf(0));
    }

    /**
     * Gets the redundancy strategy (fail over or round-robin) for a group of servers in a  non-cluster deployment.
     *
     * A non-cluster deployment is either a single server, or a group of servers having the cluster sequence equal to zero.
     *
     * @return true for fail over, false for round-robin
     */
    boolean isNonClusterFailOver() {
        return nonClusterFailOver;
    }

    /**
     * Gets the fail over percentage for a cluster.
     * When the number of available servers in a cluster falls below the fail over percentage, fail over to the next cluster occurs.
     *
     * A cluster is defined if at least one group of server(s) is assigned a cluster sequence greater or equal than one.
     *
     * @return the percentage of servers within a cluster that must be available for the cluster to be sent requests;
     *         if the available servers percentage within a cluster drops below this threshold, requests fail over to the cluster with the next cluster sequence
     */
    int getClusterFailOverThreshold() {
        return clusterFailOverThreshold;
    }

    String getHostname() {
        return hostname;
    }

    String getFipsMode() {
        return fipsMode;
    }

    /**
     * @return a list with all ServerDef's, for all defined clusters.
     */
    List getServers() {
        List servers = new ArrayList();
        Iterator iter = clusters.values().iterator();
        while (iter.hasNext()) {
            Map serverEntry = (Map) iter.next();
            servers.addAll(serverEntry.values());
        }
        return servers;
    }

    // - PRIVATE

    private void setServerProperty(String propName, String value) throws SiteMinderAgentConfigurationException {
        Matcher matcher = SERVER_DEF_PATTERN.matcher(propName);
        if ( ! matcher.matches() ) {
            throw new SiteMinderAgentConfigurationException("Invalid server configuration entry: " + agentID + "." + SERVER_PROPNAME + "." + propName);
        }

        Integer clusterSeq = Integer.valueOf(matcher.group(1));

        Map cluster = (Map) clusters.get(clusterSeq);
        if (cluster == null) {
            cluster = new TreeMap();
            clusters.put(clusterSeq, cluster);
        }
        Integer serverSeq = Integer.valueOf(matcher.group(2));
        ServerDef server = (ServerDef)cluster.get(serverSeq);
        if (server == null) {
            try {
                server = new ServerDef();//classHelper.createServerDefClass();
                initServerDef(server, clusterSeq);
            } catch (SiteMinderApiClassException e) {
                throw new SiteMinderAgentConfigurationException("Unable to initialize ServerDef object", e);
            }
            cluster.put(serverSeq, server);
        }

        setServerProperty(server, matcher.group(3), value);
    }

    private void setServerProperty(ServerDef server, String serverPropName, String value) throws SiteMinderAgentConfigurationException {
        if (SERVER_ADDRESS_PROPNAME.equals(serverPropName)) {
            server.serverIpAddress = value;//classHelper.setServerDef_serverIpAddress(server, value);
        } else if (SERVER_AUTHN_PORT_PROPNAME.equals(serverPropName)) {
            server.authenticationPort = Integer.parseInt(value);//classHelper.setServerDef_authenticationPort(server, Integer.parseInt(value));
        } else if (SERVER_AUTHZ_PORT_PROPNAME.equals(serverPropName)) {
            server.authorizationPort = Integer.parseInt(value);//classHelper.setServerDef_authorizationPort(server, Integer.parseInt(value));
        } else if (SERVER_ACCT_PORT_PROPNAME.equals(serverPropName)) {
           server.accountingPort =  Integer.parseInt(value);//classHelper.setServerDef_accountingPort(server, Integer.parseInt(value));
        } else if (SERVER_CONN_MAX_PROPNAME.equals(serverPropName)) {
           server.connectionMax = Integer.parseInt(value);// classHelper.setServerDef_connectionMax(server, Integer.parseInt(value));
        } else if (SERVER_CONN_MIN_PROPNAME.equals(serverPropName)) {
            server.connectionMin = Integer.parseInt(value);//classHelper.setServerDef_connectionMin(server, Integer.parseInt(value));
        } else if (SERVER_CONN_STEP_PROPNAME.equals(serverPropName)) {
            server.connectionStep = Integer.parseInt(value);//classHelper.setServerDef_connectionStep(server, Integer.parseInt(value));
        } else if (SERVER_TIMEOUT.equals(serverPropName)) {
            server.timeout = Integer.parseInt(value);//classHelper.setServerDef_timeout(server, Integer.parseInt(value));
        } else {
            throw new SiteMinderAgentConfigurationException("Invalid server configuration entry: " + serverPropName);
        }
    }

    private void initServerDef(ServerDef server, Integer clusterSeq) throws SiteMinderApiClassException {
        server.clusterSeq = clusterSeq;//classHelper.setServerDef_clusterSeq(server, clusterSeq.intValue());
        server.connectionMin = DEFAULT_CONNECTION_MIN;//classHelper.setServerDef_connectionMin(server, DEFAULT_CONNECTION_MIN);
        server.connectionMax = DEFAULT_CONNECTION_MAX;//classHelper.setServerDef_connectionMax(server, DEFAULT_CONNECTION_MAX);
        server.connectionStep = DEFAULT_CONNECTION_STEP;//classHelper.setServerDef_connectionStep(server, DEFAULT_CONNECTION_STEP);
        server.timeout = DEFAULT_TIMEOUT;//classHelper.setServerDef_timeout(server, DEFAULT_TIMEOUT);
    }

    private static int getPositiveIntProperty(Properties props, String propName) throws SiteMinderAgentConfigurationException {
        int value = Integer.parseInt(getProperty(props, propName));
        if (value < 0) throw new SiteMinderAgentConfigurationException("Negative value for " + propName + " : " + value);
        return value;
    }

    /**
     * Gets the value of a required property, throws if not found.
     *
     * @return the property value
     * @throws SiteMinderAgentConfigurationException if the requested property is not found.
     */
    private static String getProperty(Properties props, String propName) throws SiteMinderAgentConfigurationException {
        String value = props.getProperty(propName);
        if (value == null) throw new SiteMinderAgentConfigurationException("Configuration value not found for: " + propName);
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SiteMinderAgentConfig)) return false;

        SiteMinderAgentConfig that = (SiteMinderAgentConfig) o;

        if (agentIpCheck != that.agentIpCheck) return false;
        if (clusterFailOverThreshold != that.clusterFailOverThreshold) return false;
        if (nonClusterFailOver != that.nonClusterFailOver) return false;
        if (updateCookie != that.updateCookie) return false;
        if (agentAddress != null ? !agentAddress.equals(that.agentAddress) : that.agentAddress != null) return false;
        if (agentID != null ? !agentID.equals(that.agentID) : that.agentID != null) return false;
        if (agentName != null ? !agentName.equals(that.agentName) : that.agentName != null) return false;
        if (agentSecret != null ? !agentSecret.equals(that.agentSecret) : that.agentSecret != null) return false;
        if (clusters != null ? !clusters.equals(that.clusters) : that.clusters != null) return false;
        if (fipsMode != null ? !fipsMode.equals(that.fipsMode) : that.fipsMode != null) return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = agentID != null ? agentID.hashCode() : 0;
        result = 31 * result + (agentName != null ? agentName.hashCode() : 0);
        result = 31 * result + (agentSecret != null ? agentSecret.hashCode() : 0);
        result = 31 * result + (agentAddress != null ? agentAddress.hashCode() : 0);
        result = 31 * result + (agentIpCheck ? 1 : 0);
        result = 31 * result + (updateCookie ? 1 : 0);
        result = 31 * result + (nonClusterFailOver ? 1 : 0);
        result = 31 * result + clusterFailOverThreshold;
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + (fipsMode != null ? fipsMode.hashCode() : 0);
        result = 31 * result + (clusters != null ? clusters.hashCode() : 0);
        return result;
    }

}
