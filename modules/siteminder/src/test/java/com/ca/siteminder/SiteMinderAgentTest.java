package com.ca.siteminder;

import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.util.MockConfig;
import netegrity.siteminder.javaagent.ServerDef;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.ca.siteminder.SiteMinderContext.Attribute;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/21/13
 */
@Ignore("Requires connection to the SiteMinder Policy Server")
public class SiteMinderAgentTest {

    private static final String AGENT_NAME = "agent1";

    SiteMinderConfig config = getSiteMinderConfig();
    SiteMinderLowLevelAgent agent;
    SiteMinderHighLevelAgent fixture;
    SiteMinderConfiguration smConfig;
    SiteMinderAgentContextCacheManager cacheManager;

    @Before
    public void setUp() throws Exception {
        agent = new SiteMinderLowLevelAgent(config);
        cacheManager = new SiteMinderAgentContextCacheManagerImpl();
        fixture = new SiteMinderHighLevelAgent(new MockConfig(new HashMap<String, String>()), cacheManager);
        smConfig = new SiteMinderConfiguration();
        cacheManager.createCache(smConfig.getGoid(), AGENT_NAME,
                buildSubCaches(10, 300000,
                        10, 300000,
                        10, 300000,
                        10, 300000));
    }

    private List<SiteMinderAgentContextCache.AgentContextSubCache> buildSubCaches(int resMaxSize, long resMaxAge,
                                                                                  int authnMaxSize, long authnMaxAge,
                                                                                  int authzMaxSize, long authzMaxAge,
                                                                                  int acoMaxSize, long acoMaxAge) {
        List<SiteMinderAgentContextCache.AgentContextSubCache> subCaches = new ArrayList<>();

        subCaches.add(new SiteMinderAgentContextCache.AgentContextSubCache(null, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_RESOURCE,
                resMaxSize, resMaxAge));
        subCaches.add(new SiteMinderAgentContextCache.AgentContextSubCache(null, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHENTICATION,
                authnMaxSize, authnMaxAge));
        subCaches.add(new SiteMinderAgentContextCache.AgentContextSubCache(null, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_AUTHORIZATION,
                authzMaxSize, authzMaxAge));
        subCaches.add(new SiteMinderAgentContextCache.AgentContextSubCache(null, SiteMinderAgentContextCache.AgentContextSubCacheType.AGENT_CACHE_ACO,
                acoMaxSize, acoMaxAge));

        return subCaches;
    }

    @After
    public void tearDown() throws Exception {
       agent.unInitialize();
    }

    @Ignore("Requires connection to the SiteMinder Policy Server")
    @Test
    public void testSiteMinderLowLevelAgent() throws Exception {
        assertTrue(agent.isInitialized());
    }


    @Ignore("Requires connection to the SiteMinder Policy Server")
    @Test
    public void testSiteMinderHighLevelAgent() throws Exception {
        SiteMinderContext context = new SiteMinderContext();
        context.setAgent(agent);

        assertTrue(fixture.checkProtected("127.0.0.1", "layer7-agent", "", "/resfilter*", "POST", context));

        SiteMinderCredentials testCredentials = new SiteMinderCredentials("wssker_tacoma", "7layer");
        assertEquals(1, fixture.processAuthenticationRequest(testCredentials, "127.0.0.1", null, context, true));
        for(Attribute attr : context.getAttrList()) {
            System.out.println(attr.getName() + ": " + attr.getValue());
        }
        assertEquals(1, fixture.processAuthorizationRequest("127.0.0.1", null, context, false));
        System.out.println("SMSESSION=" + context.getSsoToken());
    }

    private static SiteMinderConfig getSiteMinderConfig() {
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
                List<ServerDef> serverDefs = new ArrayList<>();
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
