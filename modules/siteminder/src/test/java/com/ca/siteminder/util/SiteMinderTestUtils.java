package com.ca.siteminder.util;

import com.ca.siteminder.*;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.Goid;
import netegrity.siteminder.javaagent.ServerDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SiteMinderTestUtils {

    public static SiteMinderConfiguration getSiteMinderConfiguration() {
        return getSiteMinderConfiguration(10, 30 * 1000);
    }

    private static SiteMinderConfiguration getSiteMinderConfiguration(final int size, final int age) {
        final SiteMinderConfiguration config = new SiteMinderConfiguration();

        config.setGoid(Goid.DEFAULT_GOID);
        config.setProperties(new HashMap<>());

        config.getProperties().put(SiteMinderConfig.AGENT_AUTHENTICATION_CACHE_SIZE_PROPNAME, String.valueOf(size));
        config.getProperties().put(SiteMinderConfig.AGENT_AUTHORIZATION_CACHE_SIZE_PROPNAME, String.valueOf(size));
        config.getProperties().put(SiteMinderConfig.AGENT_RESOURCE_CACHE_SIZE_PROPNAME, String.valueOf(size));
        config.getProperties().put(SiteMinderConfig.AGENT_ACO_CACHE_SIZE_PROPNAME, String.valueOf(size));

        config.getProperties().put(SiteMinderConfig.AGENT_AUTHENTICATION_CACHE_MAX_AGE_PROPNAME, String.valueOf(age));
        config.getProperties().put(SiteMinderConfig.AGENT_AUTHORIZATION_CACHE_MAX_AGE_PROPNAME, String.valueOf(age));
        config.getProperties().put(SiteMinderConfig.AGENT_RESOURCE_CACHE_MAX_AGE_PROPNAME, String.valueOf(age));
        config.getProperties().put(SiteMinderConfig.AGENT_ACO_CACHE_MAX_AGE_PROPNAME, String.valueOf(age));

        return config;
    }

    public static SiteMinderConfig getSiteMinderConfig() {
        return new SiteMinderConfig() {

            @Override
            public String getAddress() {
                return "127.0.0.1";
            }

            @Override
            public String getSecret() {
                return "{RC2}AWKd1Ha8fZSLj6fMiOWQqX1d8AN5QGeeWKYpuaSFfKJRD6pg9nqUXP/lVuYI1Pm6rqYxpwHaeja24zrd60Zj4pCJmpTTItGtRFhzvxciEhunW9P8YjA/3Fu5XYg++Kagf7FHThTV5MdRrKx/QIV7i6y5gDp0YwAbQoibCw43SUGwXsPNC+zh5zM76kmVsmr/";
            }

            @Override
            public boolean isIpCheck() {
                return false;
            }

            @Override
            public String getHostname() {
                return "yuri-sm12sp3-native";
            }

            @Override
            public int getFipsMode() {
                return 1;//COMPACT
            }

            @Override
            public boolean isNonClusterFailover() {
                return false;
            }

            @Override
            public int getClusterThreshold() {
                return 50;
            }

            @Override
            public boolean isUpdateSSOToken() {
                return false;
            }

            @Override
            public List<ServerDef> getServers() {
                final List<ServerDef> serverDefs = new ArrayList<>();
                ServerDef serverDef = new ServerDef();

                serverDef.serverIpAddress = "10.7.34.32";
                serverDef.authenticationPort = 44442;
                serverDef.authorizationPort = 44443;
                serverDef.accountingPort = 44441;
                serverDef.connectionMin = 1;
                serverDef.connectionMax = 3;
                serverDef.connectionStep = 1;
                serverDef.timeout = 75;
                serverDefs.add(serverDef);

                return serverDefs;
            }

            @Override
            public boolean isCluster() {
                return false;
            }
        };
    }
}
