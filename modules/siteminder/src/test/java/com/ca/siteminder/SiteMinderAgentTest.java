package com.ca.siteminder;

import com.l7tech.util.Pair;
import netegrity.siteminder.javaagent.ServerDef;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/21/13
 */
@Ignore("Requires connection to the SiteMinder Policy Server")
public class SiteMinderAgentTest {

    SiteMinderLowLevelAgent agent;
    SiteMinderHighLevelAgent fixture;

    @Before
    public void setUp() throws Exception {
       SiteMinderConfig config = new SiteMinderConfig() {
           @Override
           public String getAgentName() {
               return "layer7-agent";
           }

           @Override
           public String getAddress() {
               return "127.0.0.1";
           }

           @Override
           public String getSecret() {
               return "{RC2}kZiLwNZObmPrkGIxDIr+wjLr92OMIXTySRn0YWaQ8uMEHNgyN6BqDYMDq434h37fVCRBGT/nM1gDWZDq9SG/TAx12EAJbItKxyj7SH4Obq25R8gwxXl/Xh2WvoYOfpX0SbxyYAdiWyE1rwZVhpnmm1ToIEZjypTbNtM3VQS7PflzMeoXDMkNlP0Na9PGxB+X";
           }

           @Override
           public boolean isIpcheck() {
               return false;
           }

           @Override
           public String getHostname() {
               return "aw80Compat";
           }

           @Override
           public int getFibsmode() {
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
           public boolean isUdpateCookie() {
               return true;
           }

           @Override
           public List<ServerDef> getServers() {
               List<ServerDef> serverDefs = new ArrayList<ServerDef>();
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
       agent = new SiteMinderLowLevelAgent(config);
       fixture = new SiteMinderHighLevelAgent();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Ignore("Requires connection to the SiteMinder Policy Server")
    @Test
    public void testSiteMinderLowLeveAgent() throws Exception {
        assertTrue(agent.isInitialized());
    }


    @Ignore("Requires connection to the SiteMinder Policy Server")
    @Test
    public void testSiteMinderHighLevelAgent() throws Exception {
        SiteMinderContext context = new SiteMinderContext();
        context.setAgent(agent);

        assertTrue(fixture.checkProtected("127.0.0.1", "/resfilter*", "POST", context));

        SiteMinderCredentials testCredentials = new SiteMinderCredentials("wssker_tacoma", "7layer");
        assertEquals(1, fixture.processAuthenticationRequest(testCredentials, "127.0.0.1", null, context));
        for(Pair<String, Object> attr : context.getAttrList()) {
            System.out.println(attr.getKey() + ": " + attr.getValue());
        }
        String smsession = context.getSsoToken();
        assertEquals(1, fixture.processAuthenticationRequest(new SiteMinderCredentials(), "127.0.0.1", smsession, context));
        System.out.println("SMSESSION=" + context.getSsoToken());
    }

}
