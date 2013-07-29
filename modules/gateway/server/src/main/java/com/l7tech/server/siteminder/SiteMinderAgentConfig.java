package com.l7tech.server.siteminder;

import com.ca.siteminder.SiteMinderConfig;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import netegrity.siteminder.javaagent.ServerDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiteMinderAgentConfig implements SiteMinderConfig {

    //server.<cluster_seq>.<server_seq>.<server_property>
    private static final Pattern SERVER_DEF_PATTERN = Pattern.compile("^server\\.([0-9]+)\\.([0-9]+)\\.(.*)");
    private SiteMinderConfiguration config;
    private List<ServerDef> servers;
    private boolean isCluster = true;

    public SiteMinderAgentConfig(SiteMinderConfiguration config) {
        this.config = config;
        initServers();
    }

    private void initServers() {
        Map<Integer, TreeMap<Integer, ServerDef>> clusters = new TreeMap();

        for (Map.Entry<String, String> property : config.getProperties().entrySet()) {
            Matcher matcher = SERVER_DEF_PATTERN.matcher(property.getKey());
            if (matcher.matches()) {
                Integer clusterSeq = Integer.valueOf(matcher.group(1));
                Integer serverSeq = Integer.valueOf(matcher.group(2));

                if (clusterSeq == 0) {
                    isCluster = false;
                }

                TreeMap<Integer, ServerDef> cluster = clusters.get(clusterSeq);
                if (cluster == null) {
                    cluster = new TreeMap<Integer, ServerDef>();
                    clusters.put(clusterSeq, cluster);
                }
                ServerDef server = cluster.get(serverSeq);
                if (server == null) {
                    server = new ServerDef();
                    initServerDef(server, clusterSeq);
                    cluster.put(serverSeq, server);
                }
                setServerProperty(server, matcher.group(3), property.getValue());
            }
        }

        servers = new ArrayList<ServerDef>();
        for (Map<Integer, ServerDef> cluster : clusters.values()) {
            for (ServerDef server : cluster.values()) {
                servers.add(server);
            }
        }

    }

    private void setServerProperty(ServerDef server, String key, String value) {
        switch (key) {
            case SERVER_ADDRESS_PROPNAME:
                server.serverIpAddress = value;
                break;
            case SERVER_AUTHN_PORT_PROPNAME:
                server.authenticationPort = Integer.parseInt(value);
                break;
            case SERVER_AUTHZ_PORT_PROPNAME:
                server.authorizationPort = Integer.parseInt(value);
                break;
            case SERVER_ACCT_PORT_PROPNAME:
                server.accountingPort = Integer.parseInt(value);
                break;
            case SERVER_CONN_MAX_PROPNAME:
                server.connectionMax = Integer.parseInt(value);
                break;
            case SERVER_CONN_MIN_PROPNAME:
                server.connectionMin = Integer.parseInt(value);
                break;
            case SERVER_CONN_STEP_PROPNAME:
                server.connectionStep = Integer.parseInt(value);
                break;
            case SERVER_TIMEOUT:
                server.timeout = Integer.parseInt(value);
                break;
            default:
                throw new IllegalArgumentException("Invalid server configuration entry:  " + key);

        }
    }

    private void initServerDef(ServerDef server, Integer clusterSeq) {
        server.clusterSeq = clusterSeq;
        server.connectionMin = DEFAULT_CONNECTION_MIN;
        server.connectionMax = DEFAULT_CONNECTION_MAX;
        server.connectionStep = DEFAULT_CONNECTION_STEP;
        server.timeout = DEFAULT_TIMEOUT;
    }

    @Override
    public String getAgentName() {
        return config.getAgent_name();
    }

    @Override
    public String getAddress() {
        return config.getAddress();
    }

    @Override
    public String getSecret() {
        return config.getSecret();
    }

    @Override
    public boolean isIpcheck() {
        return config.isIpcheck();
    }

    @Override
    public String getHostname() {
        return config.getHostname();
    }

    @Override
    public int getFibsmode() {
        return config.getFipsmode();
    }

    @Override
    public boolean isNonClusterFailover() {
        return config.isNoncluster_failover();
    }

    @Override
    public int getClusterThreshold() {
        return config.getCluster_threshold();
    }

    @Override
    public boolean isUdpateCookie() {
        //TODO
        return true;
    }

    @Override
    public List<ServerDef> getServers() {
        return servers;
    }

    @Override
    public boolean isCluster() {
        return isCluster;
    }
}
